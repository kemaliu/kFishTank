#include <string.h>
#include <stdlib.h>
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include "eeprom.h"
#include "i2c.h"
#include "pwm.h"
#include "rf.h"
#include "controllerDef.h"
#include "timer.h"
#include "switch.h"
#include "ds18b20.h"
#include "uart.h"
#include "controllerDef.h"
#include "ds18b20.h"
#include "kTankCfg.h"
#include "rf.h"
#include "rf_mng.h"
#include "debug.h"


/* RF主管理程序 
 *  RF工作流程
 * 角色分为主设备和从设备
 *  - 主设备依次查询从设备的状态或者下发命令
 *  - 从设备不会主动发出数据，而是根据主设备的请求发送应答数据
 * 定时器每秒触发976.5625次中断,每个终断代表一个时隙, 每5(RF_FRAME_LEN)个时隙是一个帧
 * NRF24L01共有125个频段，主从设备每个帧都会使用不同的频段以避免干扰
 * 跳频算法见rf_channel_inc
 * 每一次host主动发起的数据发送和从设备应答需要在一个帧当中完成
 * #define RF_FRAME_LEN 5
 * #define HOST_SND_FRAME 0
 * #define SLAVE_SND_FRAME 4
 * RF_FRAME_LEN为帧长度
 * HOST_SND_FRAME为主设备发送数据使用的时隙
 * SLAVE_SND_FRAME为从设备应答数据使用的时隙
 * 时隙0，完成跳频和数据发送
 * 时隙HOST_SND_FRAME，host发送数据，并等待数据发送结束，进入接收模式
 * 发送的数据带一个目的设备ID和sequence号码
 * NRF24L01中断异步工作，当接收到数据并且和当前host发送的devID/sequence匹配的包认为是应答数据，放到buffer当中
 * 主设备提供的通信rf接口有两个:
 *  - rfSendCmd，发送命令，该命令是异步的，只是把数据放到tx buffer中，host RF会在每一帧发送
 *  - rcvCmd,查询是否有应答回来，如果有，进行相应处理
 *基于上面两个接口又封装了两个接口完成超时处理，这两个API都是供轮循使用的
 *   - INT8 rf_cmd_snd(struct rf_cmd *pCmd, UINT32 timeout) 发送数据，并等待应答超时，在未超时的所有帧都会重复发送数据
 *   - rf_cmd_rcv_ack:检查是否有应答数据
 *   


 * 设备发现
 * 主设备第一次上电时，需要决定本机tx/rx使用的ID，监听网络中的数据包HOST_LIST_TIMEOUT_MS,如果存在源为0xe0-0xef的报文，说明该ID已经被使用，标记为被占用
 * 确定本机ID后，保存并会使用该ID进行通信
 * 
 *
 */



/* 设备列表，可用的设备ID是0-0x1f， 
 * __deviceList的每一个元素记录这对应ID的设备的controller个数
 * 0xff/0xfe为两个特殊标志，供设备发现流程使用
 * 0xfe表示设备发现流程过程中预分配一个id给从设备
 * 高4位表示是否active(host与之通信正常)，每次heart beat或通信成功后都会变为0xf,每2s减1.当active < 2时，会自动发送heart beat
 */
#define __DEV_ACTIVE_MASK 0xF0
#define __DEV_CTRL_NUM_MASK 0x0F
#define MAX_DEVICE_NUM 0x20
volatile UINT8 __deviceList[MAX_DEVICE_NUM] = {0, 0, 0, 0, 0, 0, 0, 0, /* device ID0 is host's controller, force device 1 */
					       0, 0, 0, 0, 0, 0, 0, 0,
					       0, 0, 0, 0, 0, 0, 0, 0,
					       0, 0, 0, 0, 0, 0, 0, 0};

/* 生成描述__deviceList的一个32bit描述，如果该设备ID已经被使用，bit为1
 * 未被使用bit为0*/
static UINT32 dev_list_used_flag()
{
    UINT8 i;
    UINT32 value = 0;
    for(i=0; i<sizeof(__deviceList); i++){
	if(__deviceList[i] > 0 && __deviceList[i] != 0xff){
	    value |= 1 << i;
	}
    }
    return value;
}

static UINT32 devStateListClrIncomplete()
{
    UINT8 i;
    UINT32 value = 0;
    for(i=0; i<sizeof(__deviceList); i++){
	if(__deviceList[i] >= 0xfe)
	  __deviceList[i] = 0;
    }
    __deviceList[0] = __local_ctrl_num;
    return value;
}

/* 保存设备列表信息 */
void devListSave()
{
    UINT8 buf[MAX_DEVICE_NUM], i;
    /* devStateListClrIncomplete(); */
    /* eeprom_read(DEV_CTRL_NUM_ADDR(0), (UINT8 *)buf, 0x20); */
    /* if(memcmp(buf, (char *)__deviceList, 0x20) != 0) */{
#if 1
	DBG_PRINT("update device list\n");    
#else
	{
	    static const prog_char mbuf[] = "update device list\n";
	    strcpy_P(realTimeFmtBuf, mbuf);
	    DBG_PRINT(realTimeFmtBuf);
	}
	{
	    const prog_char mbuf[] = "update device list\n";
	    strcpy_P(realTimeFmtBuf, mbuf);
	    DBG_PRINT(realTimeFmtBuf);
	} 
#endif
	memcpy(buf, __deviceList, MAX_DEVICE_NUM);
	/* 不保存active flag */
	for(i=0; i<MAX_DEVICE_NUM; i++){
	    buf[i] &= ~__DEV_ACTIVE_MASK;
	    
	}
	eeprom_write(DEV_CTRL_NUM_ADDR(0), (UINT8 *)buf, 
		     0x20);
    }
}
/* 从eeprom加载设备列表信息 */
void devListLoad()
{
    UINT8 i;
    eeprom_read(DEV_CTRL_NUM_ADDR(0), (UINT8 *)&__deviceList, 
		0x20);
    for(i=0; i<MAX_DEVICE_NUM; i++){
	__deviceList[i] &= ~__DEV_ACTIVE_MASK;
    }
    __deviceList[0] = __local_ctrl_num | __DEV_ACTIVE_MASK;
}

/* host设备的设备发现流程
 * 整个工作流程基于轮循，
 */


static UINT16 __list_start_ms;
static UINT16 __host_table, __rf_plane_table;
void host_listen_start()
{
    __host_table =  __rf_plane_table = 0;
    rf_mode_set(HOST_MODE_LISTEN);
    __list_start_ms = local_ms_get();
}

#define HOST_LIST_TIMEOUT_MS 3000
INT8 host_listen_timeout()
{
    if(local_time_diff_ms(__list_start_ms) > HOST_LIST_TIMEOUT_MS){ /*  */
	return 1;
    }else{
	return 0;
    }
}

void rf_start()
{
    UINT8 hostId, rfPlane;
    rf_init(NULL);
    if(RF_CFG_AVAL()){
	hostId = EEPROM_get(EEPROM_OFS_HOSTID);
	rfPlane = EEPROM_get(EEPROM_OFS_RFPLANE);
	rf_config(hostId, rfPlane);
	rf_mode_set(HOST_MODE_NORMAL);
	printk("RF start, hostID 0x%x rfPlane %d\n", hostId, rfPlane);
	host_do_discover();
    }else{
	printk("no rf config,listen start\n");
	host_listen_start();
	
    }
}


static UINT32 __cmd_snd_ms;	/* 命令开始发送的时间点 */
static UINT32  __cmd_snd_timeout; /* 命令超时时间, ms */
INT8 rf_cmd_snd(struct rf_cmd *pCmd, UINT32 timeout)
{
    rfSendCmd(pCmd);
    __cmd_snd_ms = local_ms_get();
    __cmd_snd_timeout = timeout;
    return OK;
}


/**
 * 检查数据应答
 * return -1:超时， 0:无应答  >0:收到的应答数据长度
 */
INT8 rf_cmd_rcv_ack(struct rf_cmd *pCmd)
{
    int ret = rcvCmd(pCmd);
    if(32 != ret){
	if(local_time_diff_ms(__cmd_snd_ms) < __cmd_snd_timeout){ 
	    /* 接受到的命令没有超时， 提交给上层程序 */
	    return 0;
	}else{			/* 超时，丢掉 */
	    return -1;
	}
    }else{
	return 32;
    }
}

#define DISCOVER_TIMEOUT_MS 3000
static UINT32 __discover_enter_ms;
static UINT8 __discover_state;	/* 0: discover started, 
				 * 1: discover sent
				 * 2: do confirm
				 */
void host_do_discover()
{
    printk("---start discover\n");
    rf_mode_set(HOST_MODE_DISCOVER);
    __discover_enter_ms = local_ms_get();
    __discover_state = 0;
}
/* 发现网络中的其他从设备 */
void host_discover_process()
{
    struct rf_cmd cmd;
    INT8 ret;
    UINT32 val32;
    if(local_time_diff_ms(__discover_enter_ms) > DISCOVER_TIMEOUT_MS){
	flushSendCmd();
	__discover_state = 0xff; /* 摆脱discover模式 */
	printk("---enter normal mode\n");
	rf_mode_set(HOST_MODE_NORMAL);
	return;
    }
    switch(__discover_state){
      default:
      case 0:			/* discover模式，发送discover命令 */
	cmd.cmd = KFISH_CMD_DISCOVER;
	devStateListClrIncomplete();
	val32 = dev_list_used_flag();
	memcpy(cmd.data, &val32, sizeof(UINT32));
	cmd.destId = 0xff;
	
	rf_cmd_snd(&cmd, 1000);
	RF_PROCESSING_CMD_SET();
	__discover_state = 1;
	DBG_PRINT("send discovery %d\n", __discover_state);
	break;
      case 1:			/* discover 已经发出去，等待应答 */
	ret = rf_cmd_rcv_ack(&cmd);
	if(ret == 32){		/* 收到了应答 */
	    if(cmd.cmd == KFISH_CMD_DISCOVER_ACK){
		UINT8 newDevId = cmd.srcId;
		if((__deviceList[newDevId] != 0xff && __deviceList[newDevId] != 0)){
		    /* illegal dev, restart discover */
		    DBG_PRINT("busy dev 0x%x discover ack in\n", newDevId);
		    __discover_state = 2;
		}else{
		    DBG_PRINT("rcv discover ack %x->%x\n", newDevId, cmd.destId);
		    __deviceList[newDevId] = 0xfe;
		    val32 = dev_list_used_flag();
		    memcpy(cmd.data, &val32, sizeof(UINT32));
		    cmd.cmd = KFISH_CMD_DEVCONFIRM;
		    cmd.destId = newDevId;
		    rf_cmd_snd(&cmd, 2000); /* timeout 200 */
		    RF_PROCESSING_CMD_SET();
		    __discover_state = 2;
		    /* DBG_PRINT("dev request 0x%x\n", newDevId); */
		}
	    }else{
		/* not discover ack */
		DBG_PRINT("not discover ack, redo discover\n");
		__discover_state = 2;
	    }
	}else if(ret == 0){	/* nothing rcv */
	    break;
	}else if(ret < 0){
	    DBG_PRINT("timeout\n");
	    /* timeout, go back to state 0 */
	    __discover_state = 0;
	}
	break;
      case 2:
	ret = rf_cmd_rcv_ack(&cmd);
	if(ret == 32){		/* recv confirm response */
	    if(cmd.cmd == KFISH_CMD_DEVCONFIRM_ACK){
		if(__deviceList[cmd.srcId] != 0xfe){
		    DBG_PRINT("illegal confirm ack dev 0x%x\n", cmd.srcId);
		    __discover_state = 0;
		    break;
		}
		__deviceList[cmd.srcId] = cmd.data[4];
		__discover_state = 0;
		DBG_PRINT("new dev 0x%x online\n", cmd.srcId);
		devListSave();
	    }else{
		__discover_state = 0;
	    }
	}else if(ret == 0){
	    /* continue wait */
	}else{
	    /* timeout, back to discover */
	    __discover_state = 0;
	}
	break;
    }
}



/* 监听端口，决定本机host ID */
void host_listen_process()
{
    struct rf_cmd cmd;
    UINT8 hostId, rfPlane;
    if(host_listen_timeout()){
	/* timeout */
	if(__host_table == 0xffff){
	    DBG_PRINT("host table full\n");
	    return;
	}
#if 0				/* rand a hostId */
	do{
	    hostId = rand() & 0xf;
	}while(((__host_table >> hostId) & 1)!= 0);
#else  /* get hostId from 0xef to 0xe0 */
	hostId = 0xf;
	while(((__host_table >> hostId) & 1)!= 0){
	    break;
	    hostId--;
	}
#endif
	hostId += 0xe0;
	do{
	    rfPlane = rand() & 0xf;
	}while(((__rf_plane_table >> rfPlane) & 1)!= 0);
	EEPROM_put(EEPROM_OFS_HOSTID, hostId);
	EEPROM_put(EEPROM_OFS_RFPLANE, rfPlane);
	EEPROM_put(EEPROM_OFS_RFAVAIL, 0xa9);
	printk("use host 0x%x rfPlane %d\n", hostId, rfPlane);
	rf_config(hostId, rfPlane);
	rf_mode_set(HOST_MODE_NORMAL);
    }else{
	/* listen other host's id */
	if(32 != rcvCmd(&cmd)){
	    return;
	}
	if(cmd.srcId >= 0xe0 && cmd.srcId < 0xf0){
	    DBG_PRINT("other host use0x%x, %d\n", cmd.srcId, cmd.rfPlane);
	    __host_table |= 1 << (cmd.srcId - 0xe0);
	    __rf_plane_table |= 1 << cmd.rfPlane;
	}
    }
}

void rf_ack_show(struct rf_cmd * pCmd)
{
    switch(pCmd->cmd){
      case KFISH_CMD_GET_DEVICES_INFO:
	printk("device 0x%x info ack, num %d\n", pCmd->srcId, pCmd->data[0]);
	break;
      case KFISH_CMD_GET_DEVICE_NAME:
	printk("device %d name ack, ", pCmd->srcId);
	printk("name <%s>\n", pCmd->data);
	break;
      case KFISH_CMD_GET_CTRL_STATUS:
	printk("dev 0x%x ctrl %d info ack, ctrl type %d \n", 
	       pCmd->srcId, pCmd->ctrlId, pCmd->data[0]);
	break;
      case KFISH_CMD_GET_CTRL_NAME:
	printk("ctrl %d name ack,", pCmd->ctrlId);
	printk("name %s\n", pCmd->data);
	break;
      case KFISH_CMD_GET_CTRL_CFG:
	printk("ctrl %d cfg ack ", pCmd->ctrlId);
	printk("%x %x %x %x %x %x\n", pCmd->data[0], pCmd->data[1], pCmd->data[2],
	       pCmd->data[3], pCmd->data[4], pCmd->data[5]);
	break;
    }
}


/* void heart_beat_process(struct rf_cmd * pCmd) */
/* { */
/*     UINT8 i; */
/*     /\* 时间同步发完了,发送心跳 *\/ */
/*     /\* 发送heart beat消息，超时10ms *\/ */
/*     cmd.cmd = KFISH_CMD_HEART_BEAT; */
/*     for(i=0; i<MAX_DEVICE_NUM; i++){ */
/* 	if((__deviceList[i] & __DEV_CTRL_NUM_MASK) > 0 && (__deviceList[i] & __DEV_CTRL_NUM_MASK) < 3){ */
/* 	    break; */
/* 	} */
/*     } */
/*     cmd.destId = i; */
    
    
/* } */

UINT8 cmd_process_flag = 0;	/* bit 0: rf command processing */
