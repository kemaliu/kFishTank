/* kTANK工作流
 * 全局存在两个中断
 * 1.硬件定时器中断 TIMER2，定时器频率为16M/64/256 = 976.5625Hz, 该定时器的主要作用为
 *  - 系统时间通过该定时器更新
 *  - 定时触发RF工作
 * 2.PCINT0_vect,RF芯片rx数据中断，中断中读取nrf24l01的数据
 * 除了NRF rx,定时器tx数据外，DS18B20的IO过程也加入到定时器回调，因为其收发1bit大约需要1ms
 * 其他i2c操作全部用轮循实现
 *
 * RF主管理程序 见rf_mng.c
 * 主设备同时使用串口和RF
 * RF与从设备通信
 * 串口与控制器(手机)通信
 * 
 */


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <avr/sleep.h>
#include <compat/deprecated.h>
#include <avr/wdt.h>
#include "uart.h"
#include "eeprom.h"
#include "i2c.h"
#include "pwm.h"
#include "rf.h"
#include "controllerDef.h"
#include "timer.h"
#include "switch.h"
#include "ds18b20.h"
#include "rf_mng.h"
#include "kTankCfg.h"
#include "debug.h"


void host_do_discover();
static void checkAndHandleUartCmd();


void uart_cmd_send(struct rf_cmd * pCmd)
{
    UINT8 * p = (UINT8 *)pCmd;
    UINT8 i, crc;
    put(0xfe);
    _delay_us(100);
    put(0x1c);
    /* calc and send crc */
    crc = 0;
    for(i=0; i<32; i++){
	crc += p[i];
    }
    put(crc);
    /* send data */
    for(i=0; i<32; i++){
	_delay_us(100);
	put(p[i]);
	
    }
}
/* 当前正在处理的命令 */
static struct rf_cmd cmd_in_progress;

void host_rf_to_uart_process()
{
    struct rf_cmd cmd;
    UINT8 i;
    if(RF_IS_PROCESSING_CMD()){
	/* 有消息正在处理 */
	INT8 ret = rf_cmd_rcv_ack(&cmd_in_progress);
	if(ret < 0){		/* 超时， 标记空闲 */
	    DBG_PRINT("rf rcv timeout\n");
	    RF_PROCESSING_CMD_CLR(); /* 标记应答已经收到,底层停止重发 */
	    return;
	}else if(ret == 0){	/* 应答没到 */
	    return;
	}else if(ret == 32){
	    /* 收到了应答并进行处理 */
#if 0
	    rf_ack_show(&cmd_in_progress);
#else
	    uart_cmd_send(&cmd_in_progress);
#endif
	    RF_PROCESSING_CMD_CLR(); /* 标记应答已经收到,底层停止重发 */
	    /* rf_ack_show(&cmd_in_progress); */
	}else{
	    DBG_PRINT("host_rf_to_uart_process bug %d\n", __LINE__);
	}
	return ;
    }
    /* 空闲，发送时间同步消息 */
    /* 分钟跳变， 更新时钟同步消息 */
    static UINT32 _last_sync_ms = 0;
    /* 每5s发送一次时间同步消息 */
    if(time_diff_ms(_last_sync_ms) > 5000){
	struct rtc_time time;
	local_time_get(&time);
	cmd.cmd = KFISH_CMD_TIME_NOTIFY;
	cmd.destId = 0xff;
	memcpy(cmd.data, (INT8*)&time, sizeof(struct rtc_time));
	DBG_PRINT("%d:%d:%d\n",time.hour, time.min, time.second);
	rf_cmd_snd(&cmd, 100); /* timeout 100ms */
	_last_sync_ms = timebase_get();
	RF_SENDING_TIME_SYNC_SET();
    }/* else if(RF_NOT_SENDING_TIME_SYNC()){ */
    /* 	/\* 时间同步发完了,发送心跳 *\/ */
    /* 	/\* 发送heart beat消息，超时10ms *\/ */
    /* 	cmd.cmd = KFISH_CMD_HEART_BEAT; */
    /* 	DBG_PRINT("%d:%d:%d\n",time.hour, time.min, time.second); */
    /* 	rf_cmd_snd(&cmd, 100); /\* timeout 100ms *\/ */
    /* } */
    
    return;
}

void host_main_process()
{

    /* 收取UART命令并处理，注意checkAndHandleUartCmd只发送rf命令，接收rf命令并回复UART由host_rf_to_uart_process()完成 */
    checkAndHandleUartCmd();	/* rcv and handle UART command */
    host_rf_to_uart_process();
}



/* 处理 host的 LISTEN/DISCOVER/NORMAL动作, normal模式下会检查串口命令 */
void main_process()
{
    switch(rf_mode_get()){
      case HOST_MODE_LISTEN:
	/* listen process */
	host_listen_process();
	break;
      case HOST_MODE_DISCOVER:
	host_discover_process();
	break;
      case HOST_MODE_NORMAL:
	host_main_process();
	break;
    }
}



struct rf_cmd * rcvUartCmd()
{
    static UINT8 rxBuf[40];
    struct rf_cmd * pCmd= (struct rf_cmd *)&rxBuf[3];
    static UINT8 pos = 0;
    static UINT32 lastRcvMs = 0;
    UINT8 i, crc;
    
    /* poll uart rx command*/
    while(uart_poll_c(&rxBuf[pos]) > 0){
	if(time_diff_ms(lastRcvMs) > 100){
	    pos = 0;
	}
	lastRcvMs = timebase_get();
	/* put(rxBuf[pos]); */
	pos++;
	/* find the head */
	if(pos == 1){
	  if(rxBuf[0] == 0xfe){
	  }else{
	      pos = 0;
	  }
	}
	if(pos == 2){
	    if(rxBuf[1] == 0x1c){
		
	    }else if(rxBuf[1] == 0xfe){
		rxBuf[0] = 0xfe;
		pos = 1;
	    }else{
		pos = 0;
	    }
	}
	if(pos >= 11 && pCmd->cmd >= 0x80){/* command  request info*/
	    pos = 0;
	    /* 计算CRC */
	    crc = 0;
	    for(i=3; i<11; i++){
		crc += rxBuf[i];
	    }
	    if(crc == rxBuf[2])
	      return pCmd;
	    else
	      DBG_PRINT("crc error\n");
	}else if(pos >= 35){
	    pos = 0;
	    /* 计算CRC */
	    crc = 0;
	    for(i=3; i<35; i++){
		crc += rxBuf[i];
	    }
	    if(crc == rxBuf[2])
	      return pCmd;
	    else
	      DBG_PRINT("crc error\n");

	}
    }
    return NULL;
}

/* UART命令中继为RF命令发送出去 */
static void cmd_relay_uart2rf(struct rf_cmd * pCmd)
{
    pCmd->destId = 1;
    pCmd->devId = 1;
    rf_cmd_snd(pCmd, 400);
    /* 发送消息后标记正在处理命令 */
    RF_PROCESSING_CMD_SET();
    DBG_PRINT("relay to 0x%x cmd 0x%x\n", pCmd->destId, pCmd->cmd);
}


/* 查询uart命令并处理 */
static void checkAndHandleUartCmd()
{
    struct rf_cmd * pCmd = rcvUartCmd();
    if(pCmd == NULL)
      return;
    /* 收到uart命令, idle置为0 */
    if(pCmd->devId == 0xff && pCmd->cmd == KFISH_CMD_GET_DEVICES_INFO){
	/* get devices list */
	memcpy((char *)pCmd->data, (char *)__deviceList, 24);
	uart_cmd_send(pCmd);
    }else if(pCmd->devId == 0xff && pCmd->cmd == KFISH_CMD_GET_DEVICE_TIME){ /* 获取设备时间 */
	rtc_get((struct rtc_time *)pCmd->data);
	uart_cmd_send(pCmd);
	/* 顺手同步下本地时间 */
	local_time_set((struct rtc_time *)pCmd->data);
    }else if(pCmd->devId == 0xff && pCmd->cmd == KFISH_CMD_SET_TIME){ /* 设置时间 */
	rtc_set((struct rtc_time *)pCmd->data);
	uart_cmd_send(pCmd);
    }else if(pCmd->devId == 0){	/* host本地设备 */
	INT8 ret = uartCmdLocalProcess(pCmd);
	if(CMD_PROCESS_ACK == ret){
	    /* 发送应答 */
	    uart_cmd_send(pCmd);
	}else if(CMD_PROCESS_NO_ACK == ret){
	      /* 不发送应答 */
	}else{
	    
	}
    }else{			/* remote device process */
	cmd_relay_uart2rf(pCmd);
    }
}

/* 每隔15min和RTC校准一次本地时间 */
void local_time_adjust()
{
    static UINT32 __last_time_entry = 0;
    static UINT16 __time_entry_cnt = 0;
    struct rtc_time time;
    if(local_time_diff_ms(__last_time_entry) >= 1000){
	__last_time_entry = local_ms_get();
	__time_entry_cnt++;
	if(__time_entry_cnt > 900){
	    rtc_get(&time);
	    local_time_set(&time);
	}
    }
}



volatile UINT8 tx_deliver = 0;
#include "ds18b20.h"

void tempearure_test()
{
        /*温度传感器测试程序*/
    init_uart(9600);
    printk("..................\n");
    timer_init();
    printk("init\n");
    while(1){
    ds_init();
    printk("do reset\n");
    if (0 != ds_reset()){
	printk("reset failed\n");
    }else{
	printk("reset ok\n");
    }

    printk("sensor num %d\n", ds_identify());
    _delay_ms(2000);
    printk("temperature %d\n", ds_get_temperature());
/*     while(1){ */
/* 	UINT8 i; */
/* 	INT16 temperature; */
/* #if 0 */
/* 	UINT8 devNum */
/* 	devNum = ds_identify(); */
/* 	printk("found %d ds18b20\n", devNum); */
/* 	for(i=0; i<devNum; i++){ */
/* 	    UINT32 tt = timebase_get(); */
/* 	    temperature = ds_get_id_temperaturex16(i); */
/* 	    tt = time_diff_us(tt); */
/* 	    printk("dev %d temperature %d.%.3d size %d\n",  */
/* 		   i, temperature/16, (temperature & 0xf)*1000/16, */
/* 		   sizeof(temperature)); */
/* 	    printk("cost %d us\n", tt); */
/* 	} */
/* 	_delay_ms(2000); */
/* #else */
/* 	temperature = ds_get_id_temperaturex16_unblock(&i); */
/* 	if(temperature == -1001){ /\* 未完成 *\/ */
/* 	}else if(temperature == -1000){ /\* io错误 *\/ */
/* 	    printk("\nerror\n"); */
/* 	}else{ */
/* 	    printk("sensor %d temperature %d.%.3d\n", i,  */
/* 		   temperature/16, (temperature & 0xf)*1000/16); */
/* 	} */
/* 	ds_sample_check(); */
/* 	_delay_ms(1); */
	
/* #endif */
/*     } */
    }
}

int main()
{
#if 1
    UINT32 cnt = 0, now;
    char buf[32];

    tempearure_test();
    /* rf_tx_test(); */
#else

    struct rtc_time time;
    UINT8 i;
    UINT32 val;
    char c, forceClearCfg = 0;
    /* enable watchdog */
    wdt_enable(WDTO_8S);

    /* init UART */
    init_uart(9600);
    printk("host..............\n");
    /* DBG_PRINT("WDTCSR 0x%x\n", WDTCSR); */
    printk("\n\n\n!!!!!!!!!!ktank start!!!!!!!!!!!!!!\n\n\n");
    /* timer_init */
    timer_init();
    val = timebase_get();
    /* init PWM, local time */
    pwm_init();
    pwm_set(0, 0xff);
    pwm_set(1, 0x00);
    pwm_set(2, 0x00);
    
    do{
        if(uart_poll_c((UINT8*)&c) > 0 && c == 'c'){
	    printk(".");
            forceClearCfg++;
        }
    }while(time_diff_ms(val) < 2000);

    printk("mycnt %d\n", forceClearCfg);
    if(forceClearCfg > 10){
        printk("host force clear cfg..\n");
	devListSave();		/* 保存设备列表默认值 */
	local_device_info_save(); /* 保存设备配置默认值 */
    }
    local_device_info_load();
    local_device_info_show();
    devListLoad();
    
    /* enable i2c bus, rtc need it  */
    I2C_init();
    printk("温传%d\n", ds_identify());
    /* get time from rtc */
    rtc_get(&time);
    if(time.year  < 15 || time.year  >= 60){
	time.year = 15;
	time.mon = 7;
	time.day = 22;
	time.week = 3;
	time.hour = 21;
	time.min = 05;
	time.second = 13;
	rtc_set(&time);	
	printk("default time %.2d-%.2d-%.2d  ", time.year, time.mon, time.day);
	printk("%.2d:%.2d:%.2d\n", time.hour, time.min, time.second);
	rtc_get(&time);
    }
    uart_print("current time %.2d-%.2d-%.2d  ", time.year, time.mon, time.day);
    uart_print("%.2d:%.2d:%.2d\n", time.hour, time.min, time.second);
    local_time_set(&time);
    srand((UINT16)time.second << 8 | time.second);
    /* load device list from eeprom */
    
    printk("device list \n");
    for(i=0; i<32; i++){
	if(__deviceList[i] != 0 && __deviceList[i] < 0xfe){
	    printk("0x%x: controller num %d\n", i, __deviceList[i] & 0xf/* __DEV_CTRL_NUM_MASK */);
	}else{
	    printk("0x%x: NA\n", i);
	}
    }
    /* load local device cfg */
    rf_start();
    sei();
#if 0

    while(1){
	wdt_reset();

	
	nrf_enter_rx_mode();
	nrf_write_tx_buffer(buf, 32);
	
	now = timebase_get();	
	PORTD &= ~8;			/* high */	
	nrf_tx_start(TX_BLOCK);
	PORTD |= 8;			/* high */
	now = time_diff_us(now);
	cnt++;
	_delay_us(1000);
	if(cnt % 100 == 0)
	  printk("%lusend block cost %luus\n", cnt++, now);
    }
#else
    while(1){
	wdt_reset();
	/* 命令的处理 */
	main_process();
	/* 运行时配置本地设备 */
	/* local_device_update(); */
	local_time_adjust();
	
    }
#endif
#endif
}
