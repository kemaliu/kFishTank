#ifndef __KTANKDEV_H__
#define __KTANKDEV_H__
#include "ktype.h"
#include "eeprom.h"


/* device type */
typedef enum{
  KFISH_LED_LIGHT = 0, 
  KFISH_SWITCH
}KFISH_DEV_T;

struct kfish_led_info{
    char temperature;		/* led temperature */
    UINT8 fanPWM;		/* led temperature */
    UINT8 pwm_now;		/* LED PWM */
    unsigned char watt;		/* 最大功耗 */
    UINT32 lastTime;		/* 上一次设置pwm的时间 */
    UINT8 pwm[24];	/* pwm value for every hour */
};


struct kfish_switch_info{
    char isOn;
    char rsv[7];
    UINT8 onoff[24];	/* on/off value for every hour, >0:on  0:off*/
};

/* every light/switch is a controller */
typedef union{
    struct kfish_led_info led;
    struct kfish_switch_info swi;
}kTankCtrlInfo_t;



struct kfish_ctrl{
    UINT8 devType;		/* KFISH_DEV_T */
    UINT8 rsv[7];
    char name[24];
    kTankCtrlInfo_t ctrl;
};



/* EEPROM definition
 * OFS 0: RF info magic , 0xa9
 * OFS 1: host ID
 * OFS 2: device ID
 * OFS 0x10- 0x2f:device list
 * OFS 0x30-0x3f:cfg avail magic, 0xa9 mean cfg available
 * OFS 0x40: controller 0
 * OFS 0x80: controller 1
 * OFS 0xc0: controller 2
 * ....
 * OFS 0x3c: controller 14
 */
#define RF_CFG_AVAL() (EEPROM_get(0) == 0xa9)
#define CTRL_CFG_AVAL(ctrlId) (EEPROM_get(0x10 + ctrlId) == 0xa9)

#define DEV_CTRL_NUM_ADDR(devId) (0x10 + devId)


#define EEPROM_OFS_DEV_INFOAVAIL (0x40)
#define EEPROM_OFS_DEV_NAME (0x50)


#define EEPROM_OFS_CTRL(ctrlId) (0x80 + 0x40 * ctrlId)
#define EEPROM_OFS_RFAVAIL (0)
#define EEPROM_OFS_HOSTID (1)
/* host use rfplane */
#define EEPROM_OFS_RFPLANE (2)
/* slave use DEVID */
#define EEPROM_OFS_DEVID (2)






#endif	/* #ifndef __KTANKDEV_H__ */
