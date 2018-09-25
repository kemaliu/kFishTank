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


void get_led_ctrl_num()
{
}

void led_cfg_set()
{
    
}


void led_pwm_update(struct rtc_time * time)
{
    UINT8 i;
    UINT8 currentPwm = time->hour, nextPwm= time->hour == 23?24:time->hour + 1;
    INT16 diff;
    for(i=0; i<__ctrl_num; i++){
	currentPwm = __ctrl[i].ctrl.led.pwm[time->hour];
	nextPwm= time->hour == 23?24:time->hour + 1;
	nextPwm = __ctrl[i].ctrl.led.pwm[nextPwm];
	diff = (INT16)nextPwm - (INT16)currentPwm;
	diff = diff * (INT16)time->min / 60;
	diff += (INT16)currentPwm;
	pwm_set(i + 1, (UINT8)diff);
    }
    
    
    
}


void led_pwm_update(struct rtc_time * time)
{
    UINT16 min = time->min;
}
