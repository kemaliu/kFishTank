
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
#include "pwm.h"
#include "rf.h"
#include "kTankDev.h"
#undef printk
#define printk uart_print

const UINT8 __ctrl_num = 2;
struct kfish_ctrl __ctrl[2] = {
  {
    .devType = KFISH_LED_LIGHT,
    .ctrl.led.name = "LED0",
    .ctrl.led.lowest_pwm = 0,
    .ctrl.led.watt = 30,
    .ctrl.led.pwm = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
		     0x00, 0x10, 0x40, 0x80, 0xc0, 0xe0, 
		     0xe0, 0xc0, 0xc0, 0x80, 0x40, 0x20, 
		     0x10, 0x08, 0x04, 0x00, 0x00, 0x00}
  },
  {
    .devType = KFISH_LED_LIGHT,
    .ctrl.led.name = "LED1",
    .ctrl.led.lowest_pwm = 0,
    .ctrl.led.watt = 30,
    .ctrl.led.pwm = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
		     0x00, 0x10, 0x40, 0x80, 0xc0, 0xe0, 
		     0xe0, 0xc0, 0xc0, 0x80, 0x40, 0x20, 
		     0x10, 0x08, 0x04, 0x00, 0x00, 0x00}
  }
};


void cfg_load(struct kfish_ctrl * pCtrl, UINT8 ctrlNum)
{
    UINT8 i;
    for(i=0; i<ctrlNum; i++){
	if(CTRL_CFG_AVAL(i)){
	    eeprom_read(EEPROM_OFS_CTRL(i), (UINT8 *)&__ctrl[i], 
			sizeof(struct kfish_ctrl));
	}
    }
}

void rxCmdProcess(struct rf_cmd * pCmd, INT8 broadcast)
{
    struct rtc_time *pTime, time;
    INT16 time0, time1;
    switch(pCmd->cmd){
      case KFISH_CMD_TIME_NOTIRY:
	pTime = (struct rtc_time *)pCmd->data;
	time0 = (INT16)pTime->hour*60 + pTime->min;
	/* get current time */
	local_time_get(&time);
	time1 = (INT16)time.hour*60 + time.min;
	if(abs(time1 - time0) > 3){ /* time diff >3 minitues */
	    local_time_set(pTime);
	    printk("update time %.2d-%.2d-%.2d ", pTime->year, pTime->mon, pTime->day);
	    printk("%.2d:%.2d:%.2d\n", pTime->hour, pTime->min, pTime->second);
	}
	break;
      case KFISH_CMD_CTRL_SET:
	if(pCmd->ctrlId >= __ctrl_num){
	    printk("ctrl id %d illegal\n", pCmd->ctrlId);
	    break;
	}
	if(pCmd->ctrlType != __ctrl[pCmd->ctrlId].devType){
	    
	}
	break;
    }
}








void localDeviceProcess()
{
    int i;
    struct kfish_ctrl * pCtrl;
    for(i=0; i<__ctrl_num; i++){
	pCtrl = &__ctrl[i];
	switch(pCtrl->devType){
	  case KFISH_LED_LIGHT:
	    break;
	  case KFISH_SWITCH:
	    break;
	  default:
	    printk("bug! unrecognized dev type %d\n", pCtrl->devType);
	}
    }
}






volatile UINT8 tx_deliver = 0;
int main()
{
    struct rtc_time time;
    
    /* enable watchdog */
    wdt_enable(WDTO_8S);
    /* init UART */
    init_uart(57600);
    printk("\n\n\n!!!!!!!!!!ktank start!!!!!!!!!!!!!!\n\n\n");
    /* init PWM, local time */
    pwm_init();
    /* fan full */
    pwm_set(0, 0xff);
    /* led 0 */
    pwm_set(1, 0x0);
    pwm_set(2, 0x0);
    /* enable i2c bus, rtc need it  */
    I2C_init();
    
    /* get time from rtc */
    rtc_get(&time);
    if(time.year  != 14){
	struct rtc_time time1 = {14, 9, 22, 1, 20, 16, 50};
	rtc_set(&time1);	
	printk("default time\n");
	rtc_get(&time);
    }
    printk("current time %.2d-%.2d-%.2d  ", time.year, time.mon, time.day);
    printk("%.2d:%.2d:%.2d\n", time.hour, time.min, time.second);
#ifdef KTANK_HOST
    extern void master_rf_start();
    master_rf_start();
#else
    UINT8 hostId, devId, sync_ok = 0;
    if(RF_CFG_AVAL()){
	hostId = EEPROM_get(EEPROM_OFS_HOSTID);
	devId = EEPROM_get(EEPROM_OFS_RFPLANE);
	if(hostId >= 0xe0 && hostId < 0xf0 && devId < 0x40){
	    printk("load rf cfg ok\n");
	}
	sync_ok = 1;
	rf_init(NULL, hostId, devId);
    }else{
	hostId = 0xff;
	devId = 0xff;
	rf_init(NULL, hostId, devId);
	printk("slave need handshake\n");
	rfSlaveDoSync();
	sync_ok = 0;
    }
#endif
    sei();
    while(1){
	wdt_reset();
#ifdef KTANK_HOST

	if((tx_deliver)){
	    printk("tx %.3u\n", tx_deliver & 0x7f);
	    tx_deliver = 0;
	}
#else
	UINT8 last;
	struct rf_cmd cmd;
	
	if(!sync_ok){
	    if(OK != rfSlaveSyncOk(&hostId, &devId)){
		continue;
	    }    
	    sync_ok = 1;
	}
	
	/* sync status OK, do cmd IO */
	if(32 == rcvCmd(&cmd)){
	    if(((last+11) % 125) != cmd.rfChan)
	      printk("miss\n");
	    printk("rcv chan %u\n", cmd.rfChan);
	    last = cmd.rfChan;
	    
	}
#endif
    }
}

