
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



#undef printk
#define printk uart_print

int main()
{
    init_uart(57600);
    /* printk("\n\n\n!!!!!!!!!!ktank start!!!!!!!!!!!!!!\n\n\n"); */
}

