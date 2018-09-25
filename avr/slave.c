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
#include "timer.h"
#include "nrf24l01.h"
#include <switch.h>
#include "controllerDef.h"
#include "debug.h"
#include "rf_mng.h"
#include "rf.h"
#include "kTankCfg.h"


void rf_config(UINT8 hostId, UINT8 devId);
UINT8 sndCmd(struct rf_cmd  * cmd);

void cfg_load(struct kfish_ctrl * pCtrl, UINT8 ctrlNum)
{
    UINT8 i;
    for(i=0; i<ctrlNum; i++){
        if(CTRL_CFG_AVAL(i)){
            eeprom_read(EEPROM_OFS_CTRL(i), (UINT8 *)&__local_ctrl[i], 
                        sizeof(struct kfish_ctrl));
        }
    }
}




void localDeviceProcess()
{
    int i;
    struct kfish_ctrl * pCtrl;
    for(i=0; i<__local_ctrl_num; i++){
        pCtrl = &__local_ctrl[i];
        switch(pCtrl->devType){
          case KFISH_LED_LIGHT:
            break;
          case KFISH_SWITCH:
            break;
          default:
            DBG_PRINT("bug! unrecognized dev type %d\n", pCtrl->devType);
        }
    }
}



void rf_process()
{
    struct rf_cmd cmd;
    UINT32 val32;
    UINT8 devId, hostId;
    INT8 ret;
    if(32 != rcvCmd(&cmd)){
        return;
    }
    switch(cmd.cmd){
      case KFISH_CMD_DISCOVER:
	if(rf_mode_get() != SLAVE_MODE_WAIT_DISCOVER)
	  break;
	memcpy(&val32, cmd.data, sizeof(UINT32));
        if(val32 == 0xffffffff){
            DBG_PRINT("devId full\n");
	    rf_mode_set(SLAVE_MODE_WAIT_DISCOVER);
            return;
        }
        for(devId = 0; devId < 0x20; devId++){
            if(((val32 >> devId) & 1) == 0)
              break;
        }
        /* printk("rcv discover from 0x%x->%x \n", cmd.srcId, cmd.destId); */
	/* printk("try devid 0x%x plane %d mode %d\n", devId, cmd.rfPlane, rf_mode_get()); */
        /* printk("list 0x%lx\n", val32); */
        rf_config(cmd.srcId, devId);
        cmd.cmd = KFISH_CMD_DISCOVER_ACK;
	val32 = val32 | (1 << devId);
	memcpy(cmd.data, &val32, sizeof(UINT32));
        sndCmd(&cmd);
	rf_mode_set(SLAVE_MODE_DISCOVER_GET);
        break;
      
      case KFISH_CMD_DEVCONFIRM: /* 4Byte dev status, 1B controlNum */
	if(rf_mode_get() != SLAVE_MODE_DISCOVER_GET)
	  break;
        hostId = cmd.srcId;
        devId = cmd.destId;
	memcpy(&val32, cmd.data, sizeof(UINT32));
	val32 |= (1 << devId);
	memcpy(cmd.data, &val32, sizeof(UINT32));
        cmd.data[4] = __local_ctrl_num; /* control number */
        cmd.cmd = KFISH_CMD_DEVCONFIRM_ACK;
        rf_mode_set(SLAVE_MODE_NORMAL);
        sndCmd(&cmd);
	printk("confirmed, use devId 0x%x\n", devId);
        if(hostId != EEPROM_get(EEPROM_OFS_HOSTID)){
            EEPROM_put(EEPROM_OFS_HOSTID, hostId);
        }
        if(devId != EEPROM_get(EEPROM_OFS_DEVID)){
            EEPROM_put(EEPROM_OFS_RFPLANE, devId);
        }
        if(0xa9 != EEPROM_get(EEPROM_OFS_RFAVAIL)){
            EEPROM_put(EEPROM_OFS_RFAVAIL, 0xa9);
        }
        break;
      default:
	ret = uartCmdLocalProcess(&cmd);
	if(CMD_PROCESS_ACK == ret){
	    DBG_PRINT("<cmd 0x%x ctrl:%d data:%x %x %x\n", 
		   cmd.cmd, cmd.ctrlId, cmd.data[0], cmd.data[1], cmd.data[2]
		  );
	    sndCmd(&cmd);	/* 合法命令,发送应答 */
	    DBG_PRINT(">cmd 0x%x ctrl %d %x %x %x\n", 
		      cmd.cmd, cmd.ctrlId, cmd.data[0], cmd.data[1], cmd.data[2]
		     );
	}else if(CMD_PROCESS_NO_ACK == ret){	/* 合法命令，但是不用应答 */
	    
	}else{			/* 不合法的命令 */
	    DBG_PRINT("illegal command\n");
	    DBG_PRINT("\tcmd 0x%x ctrlId %d", cmd.cmd, cmd.ctrlId);
	}
        break;
    }
}






volatile UINT8 tx_deliver = 0;
int main()
{
    UINT32 val;
    char c, forceClearCfg = 0;
    /* enable watchdog */
    wdt_enable(WDTO_8S);
#ifdef INCLUDE_KTANK_UART
    /* init UART */
    init_uart(9600);
    printk("\n\n\n!!!!!!!!!!ktank start!!!!!!!!!!!!!!\n\n\n");
#endif
    /* init PWM, local time */
    pwm_init();
    /* enable i2c bus, rtc need it  */
    I2C_init();
    /* init timer */
    timer_init();
    val = timebase_get();
#ifdef INCLUDE_KTANK_UART
    do{
        if(uart_poll_c((UINT8*)&c) > 0 && c == 'c'){
            forceClearCfg++;
        }
    }while(time_diff_ms(val) < 2000);
#endif
    if(forceClearCfg > 10){
        DBG_PRINT("slave force clear cfg..\n");
        EEPROM_put(EEPROM_OFS_RFAVAIL, 0xff);
    }
    local_device_info_load();
    local_device_info_show();
    if(RF_CFG_AVAL()){
        rf_init(NULL, SLAVE_MODE_NORMAL);

        rf_config(EEPROM_get(EEPROM_OFS_HOSTID), EEPROM_get(EEPROM_OFS_DEVID));
        DBG_PRINT("slave start hostid 0x%x devid 0x%x\n", 
               EEPROM_get(EEPROM_OFS_HOSTID), EEPROM_get(EEPROM_OFS_DEVID));
        nrf_enter_rx_mode();
    }else{
#if 0
        rf_init(NULL, SLAVE_MODE_WAIT_SYNC);
        DBG_PRINT("no cfg, force device id 1, host id 0xef\n");
        rf_config(0xef, 1);
        nrf_enter_rx_mode();
#else

        rf_init(NULL, SLAVE_MODE_WAIT_DISCOVER);
        rf_config(0xc0, 0x80);
        DBG_PRINT("slave start without rf cfg\n");
#endif
    }
    sei();
    while(1){
        wdt_reset();
        rf_process();
	local_device_update();
    }
}

