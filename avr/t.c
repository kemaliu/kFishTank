#include <stdlib.h>
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
#include "ktype.h"		/* ktype should be included as last head file */

#undef printf
#define printf uart_print


static unsigned long __timer_sec = 0;



volatile unsigned long num;
static UINT8 _snd_buf[128];

static UINT16 __snd_cnt = 0;
void mytimercb(char sec)
{
    __snd_cnt++;
    nrf_chan_set((UINT8)(__snd_cnt % 125));
    nrf_snd(_snd_buf, 32);
    if(__snd_cnt % 1000 == 0){
	printf("%d\n", __snd_cnt);
    }
}
#if 0
int main()
{
    struct rtc_time time;
    unsigned long cnt = 0;
    INT8 i, j;
#if 0
    init_uart(9600);
    /* periodly send data  */
    nrf_init();
#if 1
    timer_init();
    timer_cb_set(mytimercb);
    printf("...start\n");
    while(1);
#else
    timer_init();
    i = 0;
    nrf_enter_rx_mode();
    while(1){
	nrf_chan_set(i);
	_delay_ms(1000);
	nrf_snd(_snd_buf, 32);
	cnt++;
	/* if(cnt % 1000 == 0) */{
	    printf("%d\n", cnt);
	}
	i = (i + 1) % 125;
    }
#endif
#else
    timer_init();
    init_uart(9600);
    printf("begin rx\n");
    nrf_init();
    nrf_chan_set(24);
    nrf_enter_rx_mode();
    while(1){
	UINT8 len, j;
	/* for(i=0; i<5; i++) */{
	    len = nrf_rcv(1, _snd_buf, 32);
	    if(len > 0){
		cnt++;
		printf("%u pipe%d rcv %d bytes\n", (UINT16)cnt, i, len);
		/* for(j=0; j<len; j++){ */
		/*     printf("%.2x ", _snd_buf[j]); */
		/* } */
		/* printf("\n"); */
	    }
	    
	}
    }
#endif

#if 0
    spi_init();
    printf("spi inited\n");
    i=0;
    while(1){
	_delay_ms(1000);
	printf("g\n");
	_snd_buf[0] = i;
	spi_reg_write(0, _snd_buf, 1);
	printf("write 0x%x done\n", i);

	spi_reg_read(0, _snd_buf, 1);
	printf("read done, value 0x%x\n", _snd_buf[0]);
	spi_reg_read(5, _snd_buf, 1);
	printf("read done, reg 5 value 0x%x\n", _snd_buf[0]);
	memset(_snd_buf, 0, sizeof(_snd_buf));
	spi_reg_read(10, _snd_buf, 10);
	printf("addr %.2x-%.2x-%.2x-%.2x-%.2x\n", _snd_buf[0], _snd_buf[1], _snd_buf[2], _snd_buf[3], _snd_buf[4]);
	spi_reg_read(11, _snd_buf, 10);
	printf("addr %.2x-%.2x-%.2x-%.2x-%.2x\n", _snd_buf[0], _snd_buf[1], _snd_buf[2], _snd_buf[3], _snd_buf[4]);
	i++;
    }
#endif
#if 0				/* pwm test */
    pwm_init();
    while(1);
#endif
    
#if  0				/* timer test */
    timer_init();
    while(1){
	/* _delay_ms(1000);		/\* wait 1 second for power supply stable *\/ */
	/* printf("TCNT2 0x%x TIMSK 0x%x\n", TCNT2, TIMSK2); */
    }
#endif
    while(1){
	_delay_ms(1000);		/* wait 1 second for power supply stable */
	cnt++;
	uart_print("%d\n",cnt);
#if 0				/* eeprom test  */
	_snd_buf[0] = cnt & 0xff;
	eeprom_write(0 + cnt % 16, _snd_buf, 1);
	eeprom_read(0, _snd_buf, 16);
	for(i=0; i<16; i++){
	    printf("%.2x ", _snd_buf[i]);
	}
	printf("\n");
#endif
#if 0				/* RTC test */
	rtc_get(&time);
	if(time.mon > 12 ||  time.day > 31 || time.week > 7 || time.hour >= 24 || time.min >= 60 || time.second >= 60){
	    printf("time illegel, set time\n");
	    time.year = 13;
	    time.mon = 8;
	    time.day = 6;
	    time.week = 5;
	    time.hour = 20;
	    time.min = 25;
	    time.second = 0;
	    rtc_set(&time);
	    
	    rtc_get(&time);
	    printf("%d-%d-%d ", time.year, time.mon, time.day);
	    printf("%d:%d:%d\n", time.hour, time.min, time.second);
	}else{
	    rtc_get(&time);
	    printf("%d-%d-%d ", time.year, time.mon, time.day);
	    printf("%d:%d:%d\n", time.hour, time.min, time.second);
	}
#endif
    }
}
#else
int main()
{
    struct rtc_time time;
    unsigned long cnt = 0;
    INT8 i, j;
#if 0
    init_uart(9600);
    /* periodly send data  */
    nrf_init();
#if 1
    timer_init(mytimercb);
    printf("...start\n");
    while(1);
#else
    timer_init(0);
    i = 0;
    nrf_enter_rx_mode();
    while(1){
	nrf_chan_set(i);
	_delay_ms(1000);
	nrf_snd(_snd_buf, 32);
	cnt++;
	/* if(cnt % 1000 == 0) */{
	    printf("%d\n", cnt);
	}
	i = (i + 1) % 125;
    }
#endif
#else
    timer_init(0);
    init_uart(9600);
    printf("begin rx\n");
    nrf_init();
    nrf_chan_set(24);
    nrf_enter_rx_mode();
    while(1){
	UINT8 len, j;
	/* for(i=0; i<5; i++) */{
	    len = nrf_rcv(1, _snd_buf, 32);
	    if(len > 0){
		cnt++;
		printf("%u pipe%d rcv %d bytes\n", (UINT16)cnt, i, len);
		/* for(j=0; j<len; j++){ */
		/*     printf("%.2x ", _snd_buf[j]); */
		/* } */
		/* printf("\n"); */
	    }else{
		printf(".");
	    }
	    
	}
    }
#endif

#if 0
    spi_init();
    printf("spi inited\n");
    i=0;
    while(1){
	_delay_ms(1000);
	printf("g\n");
	_snd_buf[0] = i;
	spi_reg_write(0, _snd_buf, 1);
	printf("write 0x%x done\n", i);

	spi_reg_read(0, _snd_buf, 1);
	printf("read done, value 0x%x\n", _snd_buf[0]);
	spi_reg_read(5, _snd_buf, 1);
	printf("read done, reg 5 value 0x%x\n", _snd_buf[0]);
	memset(_snd_buf, 0, sizeof(_snd_buf));
	spi_reg_read(10, _snd_buf, 10);
	printf("addr %.2x-%.2x-%.2x-%.2x-%.2x\n", _snd_buf[0], _snd_buf[1], _snd_buf[2], _snd_buf[3], _snd_buf[4]);
	spi_reg_read(11, _snd_buf, 10);
	printf("addr %.2x-%.2x-%.2x-%.2x-%.2x\n", _snd_buf[0], _snd_buf[1], _snd_buf[2], _snd_buf[3], _snd_buf[4]);
	i++;
    }
#endif
#if 0				/* pwm test */
    pwm_init();
    while(1);
#endif
    
#if  0				/* timer test */
    timer_init(mytimercb);
    while(1){
	/* _delay_ms(1000);		/\* wait 1 second for power supply stable *\/ */
	/* printf("TCNT2 0x%x TIMSK 0x%x\n", TCNT2, TIMSK2); */
    }
#endif
    while(1){
	_delay_ms(1000);		/* wait 1 second for power supply stable */
	cnt++;
	uart_print("%d\n",cnt);
#if 0				/* eeprom test  */
	_snd_buf[0] = cnt & 0xff;
	eeprom_write(0 + cnt % 16, _snd_buf, 1);
	eeprom_read(0, _snd_buf, 16);
	for(i=0; i<16; i++){
	    printf("%.2x ", _snd_buf[i]);
	}
	printf("\n");
#endif
#if 0				/* RTC test */
	rtc_get(&time);
	if(time.mon > 12 ||  time.day > 31 || time.week > 7 || time.hour >= 24 || time.min >= 60 || time.second >= 60){
	    printf("time illegel, set time\n");
	    time.year = 13;
	    time.mon = 8;
	    time.day = 6;
	    time.week = 5;
	    time.hour = 20;
	    time.min = 25;
	    time.second = 0;
	    rtc_set(&time);
	    
	    rtc_get(&time);
	    printf("%d-%d-%d ", time.year, time.mon, time.day);
	    printf("%d:%d:%d\n", time.hour, time.min, time.second);
	}else{
	    rtc_get(&time);
	    printf("%d-%d-%d ", time.year, time.mon, time.day);
	    printf("%d:%d:%d\n", time.hour, time.min, time.second);
	}
#endif
    }
}

#endif
