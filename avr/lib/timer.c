#include <stdio.h>
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <avr/sleep.h>
#include <string.h>
#include <stdlib.h>
#include <compat/deprecated.h>
#include "timer.h"
#include "rtc.h"
#include "ktype.h"

static volatile UINT32 __isr_cnt = 0;
void timer_clear()
{
    __isr_cnt = 0;
}

static TIMECB __timer_cb = NULL;
SIGNAL(TIMER2_OVF_vect)
{
    __isr_cnt++;
    if((__timer_cb)){
	__timer_cb(1);
    }
}


void timer_init()
{
    /* config internal timer interrupt, when TCNT2 reach max value,
       it will trigger a internal interrupt */

    TCCR2A = 0 | 		/* OC2A/OC2B disconnected */
	     0;			/* normal mode */
    TCCR2B = 0x4; 		/* 16M div 64 = 250K*/ 
    TCNT2=0;                    /* orignal count = 0 */
    timer_enable_int (_BV (TOIE2));
    TIMSK2 = 0x1;
    sei();
}

void timer_cb_set(TIMECB cb)
{
    __timer_cb = cb;    
}

UINT32 timebase_get()
{
    UINT32 time = __isr_cnt;
    UINT8 low;
    do{
	time = __isr_cnt;
	low = TCNT2;
    }while(__isr_cnt != time);
    return (time << 8)|low;
}


UINT32 time_diff_us(UINT32 last)
{
    UINT32 now = timebase_get();
    now = (now - last);
    now = now << 2;
    return now;
}

UINT32 time_diff_ms(UINT32 last)
{
    UINT32 now = timebase_get();
    now = (now - last);
    now = (now << 2)/1000L;
    return now;
}

    /* while(1){ */
    /* now = timebase_get(); */
    /* _delay_ms(6600); */
    /* now = time_diff_us(now); */
    /* printf("delay 10ms cost %d.%.3dms\n", (UINT16)(now/1000), (UINT16)(now%1000)); */
    /* } */
