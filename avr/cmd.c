/* cmd format
 * cmd             byte0       byte1           byte2             reply   byte0       byte1           byte2
 * 0x0  get time    na           na             na               0       hour        min             seconds
 * 0x1  set time    hour         min            second           0       0           0               0
 * 0x2  get switch  index        hour           na               0       index       hour            1(on)/0(off)
 *      info
 * 0x3  set switch  index        time           1(on)/0(off)     0       0           0              0
 * 0x4  get light   hour         na             na               0       hour        pwm value       0
 * 0x5  set light   hour         pwm value      na               0       0           0               0
 * other                                                         'e'     'r'         'r'             0xff
 * reply format
 */


#include <stdio.h>
#include "uart.h"
#include <stdlib.h>
#include "event.h"
#include "rtc.h"

extern unsigned char time_hour;
extern unsigned int time_second;
extern char rcv_cmd[16];
extern char rcv_cmd_len;
extern unsigned char __isr_cnt;
extern unsigned char switch_mode[24<<2];
extern volatile unsigned char trig_mark;
void cmdProcess()
{
    char * end;
    char arg[3];
    if(0 == memcmp(rcv_cmd, "time", 4)){ /* get time */
	uart_print("%u:%u:", time_hour, time_second/60);
	uart_print("%u\n", time_second%60);
    }else if(0 == memcmp(rcv_cmd, "tims", 4)){ /* set time */ 
	time_hour = strtol(rcv_cmd+4, &end, 10);
	if(*end !=':'){
	    return;
	}
	time_second = strtol(end+1, NULL, 10);
	__isr_cnt = 0;
	uart_print("new time %u:%u:", time_hour, time_second/60);
	uart_print("%u\n", time_second%60);
	rtc_set(time_hour, time_second/60, time_second%60);
	trig_mark |= EVENT_PERIOD_OPR;
	trig_mark |= EVENT_HOUR_PASS;
	return;
    }else if(0 == memcmp(rcv_cmd, "swig", 4)){ /* get swith info */
	arg[0] = strtol(rcv_cmd+4, &end, 10);  /* switch index */
	if(*end !=':'){
	    goto swi_get_failed;
	}
	arg[1] = strtol(end+1, NULL, 10);
	uart_print("switch %u %u:00   ", arg[0], arg[1]);
	uart_print("%s\n", (1 & (switch_mode[arg[1]] >> arg[0])) ? "on":"off");
	return;
    swi_get_failed:
	uart_print("get swi failed");
    }else if(0 == memcmp(rcv_cmd, "swis", 4)){ /* set swith info */
	arg[0] = strtol(rcv_cmd+4, &end, 10);  /* switch index */
	if(*end !=':'){
	    goto swi_set_failed;
	}
	arg[1] = strtol(end+1, &end, 10);
	if(*end !=':'){
	    goto swi_set_failed;
	}
	arg[2] = strtol(end+1, NULL, 10);
	if(!arg[2]){
	    switch_mode[arg[1]] &= ~(1 << arg[0]);
	}else{
	    switch_mode[arg[1]] |= (1 << arg[0]);
	}
	uart_print("set switch %u %u:00 ", arg[0], arg[1]);
	uart_print("%s\n", arg[2] ? "on":"off");
	return;
    swi_set_failed:
	uart_print("set time failed");
    }else if(0 == memcmp(rcv_cmd, "paus", 4)){ /* stop pump for 10 min */
	extern int decrease_timer;
	extern void switchctrl(char index, char enable);
	decrease_timer = strtol(rcv_cmd+4, &end, 10);
	/* turn off all the pump */
	switchctrl(2, 0);
	switchctrl(3, 0);
    }
    
}

