#include <stdlib.h>
#include <avr/wdt.h>
#include <stdio.h>
#include <string.h>
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <avr/sleep.h>
#include <compat/deprecated.h>
#include "uart.h"
#include "eeprom.h"
#include "i2c.h"
#include "rtc.h"
#include "spi.h"
#include "nrf24l01.h"
#include "timer.h"
#include "rf.h"
#include "ktype.h"		/* ktype should be included as last head file */
#include "debug.h"


static void deliverRxCmd();



struct rf_ctrl{
    UINT8 rfChan;		/* current channel */

    UINT8 tick;			/* 当前时隙 */
    UINT16 frameId;		/* 当前帧ID */
    UINT16 lastRxFrameId;	/* 最近收到的数据的所属帧 */
    UINT8 rx_err;		/* err event cnt */
    UINT8 tx_err;		/* err event cnt */
    UINT8 hostId;		/* host id, 0xe0-0xfe */
    UINT8 devId;		/* device id, 0x0-0x3f, 0xff mean broadcast msg */
    UINT8 rfPlane;		/* rf channel jump plane */
    UINT16 lastRcv;
    struct rf_cmd rxCmd;	/* ping-pong buffer for rx cmd */
    UINT8 rxCmdAvail;
    struct rf_cmd txCmd;	/* ping-pong buffer for rx cmd */
    UINT8 txCmdFull;
    UINT8 mode;			/* listen mode */
    UINT8 sync;			/* listen mode */
    UINT8 rxMode;
    struct rf_cmd tmpRxCmdBuf;	/* 临时rx命令buffer */
};

volatile struct rf_ctrl rf;


volatile static UINT16 __1k_timer_cnt = 0;		/*  */


/* 16M crystal, 976.5626HZ
 * cnt to 15625 = 16s
 */
void slave_timer_cb(UINT8 second)
{
    UINT16 diff;
    if(!rf.sync){
	/* 没同步上 */
	/* 每2000次中断跳频一下，大约是2秒钟跳频一次 */
	if(__1k_timer_cnt > 500){
	    __1k_timer_cnt = 0;
	    rf.rfChan = rf_channel_inc(rf.rfChan, rf.rfPlane);
	    nrf_chan_set(rf.rfChan);
	    nrf_enter_rx_mode();
	    
	}
    }else{
	/* 同步上了 */
	/* change rf channel */
	rf.tick++;
	if(rf.tick >= RF_TICKS_PER_FRAME){
	    /* 新的一帧 */
	    rf.frameId++;
	    DDRC |= 0x2;
	    PORTC |= 0x2;
	    rf.tick = 0;
	    rf.rfChan = rf_channel_inc(rf.rfChan, rf.rfPlane);
	    /* use new channel */
	    nrf_chan_set(rf.rfChan);
	    nrf_enter_rx_mode();
	    PORTC &= 0xfd;
	}
	/* 检查同步状态,如果两秒钟没收到数据，认为失去同步 */
	diff = __1k_timer_cnt - rf.lastRcv;
	if(diff > 500){
	    printk("sync lost\n");
	    rf.sync = 0;
	    return;
	}
	/* 发送时隙，并且上一次接受到的包和当前需要发送的包的seq相同,并且是同一个帧 */
	if(rf.tick == RF_SLAVE_FRAME_SND_TICK && /* slave发送时隙 */
	   rf.lastRxFrameId == rf.frameId && 	 /* 在同一帧当中 */
	   rf.txCmd.seqNum == rf.tmpRxCmdBuf.seqNum){ /* seq与请求seq相同 */
	    nrf_snd_block((UINT8*)&rf.txCmd, 32);
	    nrf_enter_rx_mode();
	    /* printk(">>>>>>>%d dest %x src %x\n", rf.frameId, rf.txCmd.destId, rf.txCmd.srcId); */
	}else{
	    
	    /* printk("seq %d %d, frame %d %d\n",  */
	    /* 	   rf.txCmd.seqNum, rf.tmpRxCmdBuf.seqNum,  */
	    /* 	   rf.lastRxFrameId,rf.frameId); */
	}
    }
    __1k_timer_cnt++;
}


void frame_reset(struct rf_cmd * pCmd)
{
    #define TIME_OFS 170
    TCNT2 = TIME_OFS;
    rf.tick = RF_HOST_FRAME_SND_TICK;
    rf.rfChan = pCmd->rfChan;
    rf.rfPlane = pCmd->rfPlane;
    if(!rf.sync){
	printk("sync ok\n");
    }
    rf.sync = 1;
}

void ignore_rx_cmd(struct rf_cmd * pCmd)
{
    printk("i src:%x cmd:%d\n", pCmd->srcId, pCmd->cmd);
}


/* 设备发现流程下的处理函数 */
INT8 rf_rcv_isr_discover(struct rf_cmd * pCmd)
{
    INT8 illegal = 0;
    switch(rf.mode){
      case SLAVE_MODE_WAIT_DISCOVER:
	if(pCmd->cmd == KFISH_CMD_DISCOVER){
	    /* wait discover模式只接收KFISH_CMD_DISCOVER命令，忽略其他命令 */
	    rf.hostId = pCmd->srcId;
	    illegal = 0;
	}else{
	    illegal = 1;
	    /* wait discover mode ignore not discover command */
	}
	break;
      case SLAVE_MODE_DISCOVER_GET:
	/* discover get模式只接收KFISH_CMD_DEVCONFIRM命令，忽略其他命令 */
	if(pCmd->cmd == KFISH_CMD_DEVCONFIRM && rf.hostId == pCmd->srcId){
	    illegal = 0;
	}else{
	    illegal = 1;
	}
	break;
      default:
	illegal = 1;
    }
    if(!illegal){
	frame_reset(pCmd);
	rf.lastRcv = __1k_timer_cnt;
	rf.lastRxFrameId = rf.frameId;
	return 1;
    }else{
	return 0;
    }
}

/* 正常工作流程,返回1表示命令需要外部处理 */
INT8 rf_rcv_isr_normal(struct rf_cmd * pCmd)
{
    if(rf.hostId != pCmd->srcId){
	return 0;
    }
    frame_reset(pCmd);		/* 收到主机 发来的消息，做帧同步 */
    TCNT2 = TIME_OFS;
    /* adjust clock */
    rf.lastRcv = __1k_timer_cnt;
    if(pCmd->destId == rf.devId){
	/* 记录发送到本地且需要应答的命令的frameId */
	rf.lastRxFrameId = rf.frameId;
	return 1;
    }
    return 0;
}

/* 无线接收的终端处理程序 */

void rf_rcv_isr()
{
    UINT8 len, pipe;
    struct rf_cmd * pCmd = &rf.tmpRxCmdBuf;
    PCIFR = 0x1;
    /* adjust local 976.5626HZ timer  */
    len = nrf_int_rcv(&pipe, (UINT8*)pCmd, 32);
    
    if(len != 32){
	printk("e");
	rf.rx_err++;
	return;
    }
    if(rf.mode < SLAVE_MODE_NORMAL){ /* 设备发现流程 */
	len = rf_rcv_isr_discover(pCmd);
    }else{			/* 正常工作流程 */
	len = rf_rcv_isr_normal(pCmd);
    }
    if(len > 0){
	deliverRxCmd();
    }
    deliverRxCmd();
}



void rf_config(UINT8 hostId, UINT8 devId)
{
    rf.hostId = hostId;
    rf.devId = devId;
    
}

/* init rf tx/rx, timer, spi, nrf2401 */
INT8 rf_init(struct rtc_time * time, UINT8 mode)
{
    memset((char *)&rf, 0, sizeof(struct rf_ctrl));
    if(mode == SLAVE_MODE_WAIT_DISCOVER){
	rf.hostId = 0xc0;
	rf.devId = 0;
    }
    /* enable 976HZ timer */
    timer_init();
    nrf_init();
    /* enable slave timer */
    nrf_enter_rx_mode();
    rf.mode = mode;	/* default in listen mode */
    rf.rfChan = 50;
    rf.sync = 0;
    timer_cb_set(slave_timer_cb);
    nrf_enter_rx_mode();
    return 0;
}




static void deliverRxCmd()
{
    if((rf.rxCmdAvail)){
	/* 之前的rx命令还没有处理完，什么事都不干 */
    }else{
	memcpy(&rf.rxCmd, &rf.tmpRxCmdBuf, sizeof(struct rf_cmd));
	rf.rxCmdAvail = 1;
    }
}

UINT8 rcvCmd(struct rf_cmd  * cmd)
{
    if((rf.rxCmdAvail)){
	memcpy((char *)cmd, (char *)&rf.rxCmd, sizeof(struct rf_cmd));
	rf.rxCmdAvail = 0;
	return sizeof(struct rf_cmd);
    }else{
	return 0;
    }
}

UINT8 sndCmd(struct rf_cmd  * cmd)
{
    cmd->srcId = rf.devId;
    cmd->destId = rf.hostId;
    memcpy((INT8*)&rf.txCmd, (INT8*)cmd, sizeof(struct rf_cmd));
    return 32;
}


UINT8 rf_mode_get()
{
    return rf.mode;
}

void rf_mode_set(UINT8 mode)
{
    printk("(set mode %d\n)", mode);
    rf.mode = mode;
}
