#include <string.h>
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include "eeprom.h"
#include "i2c.h"
#include "pwm.h"
#include "rf.h"
#include "controllerDef.h"
#include "timer.h"
#include "switch.h"
#include "ds18b20.h"
#include "uart.h"
#include "controllerDef.h"
#include "ds18b20.h"
#include "kTankCfg.h"
#include "rf.h"
#include "debug.h"


#define DEVICE_INFO_MAGIC 0xb5


/* 保存本地设备信息 */
void local_device_info_save()
{
    UINT8 i, ret;
    struct kfish_ctrl info;
    /* cfg avail */
    /* get device name */
    ret = eeprom_write(EEPROM_OFS_DEV_NAME, (UINT8 *)__device_name, 
		       strlen(__device_name) + 1);
    DBG_PRINT("mod dev name %s, modify %d bytes\n", __device_name, ret);
    
    /* get controller info */
    for(i=0; i<__local_ctrl_num; i++){
        DBG_PRINT("save %d\n", i);
        memcpy((char *)&info, (char *)&__local_ctrl[i], sizeof(struct kfish_ctrl));
        /* clear dynamic */
        memset((char *)info.ctrl.swi.rsv, 0, 8);
        ret = eeprom_write(EEPROM_OFS_CTRL(i), (UINT8*)&info, sizeof(struct kfish_ctrl));
        if(ret > 0){
            DBG_PRINT("save, modify %d bytes\n", ret);
        }else{
            DBG_PRINT("save, info not changed\n");
        }
    }
    EEPROM_put(EEPROM_OFS_DEV_INFOAVAIL, DEVICE_INFO_MAGIC);
}

/* 从eeprom加载本地设备信息 */
void local_device_info_load()
{
    UINT8 i;
    if(EEPROM_get(EEPROM_OFS_DEV_INFOAVAIL) == DEVICE_INFO_MAGIC){
        /* cfg avail */
        /* get device name */
        eeprom_read(EEPROM_OFS_DEV_NAME, (UINT8 *)__device_name, 24);
        /* get controller info */
        for(i=0; i<__local_ctrl_num; i++){
            UINT8 buf[8];
            memcpy(buf, __local_ctrl[i].ctrl.swi.rsv, 8);
            memset(&__local_ctrl[i], 0, sizeof(struct kfish_ctrl));
            eeprom_read(EEPROM_OFS_CTRL(i), (UINT8 *)&__local_ctrl[i], sizeof(struct kfish_ctrl));
            DBG_PRINT("read ctrl %d from 0x%x size %x to %x\n", i, EEPROM_OFS_CTRL(i), sizeof(struct kfish_ctrl), &__local_ctrl[i]);
            memcpy(__local_ctrl[i].ctrl.swi.rsv, buf, 8);
        }
    }else{
        /* cfg not avail */
        local_device_info_save();
    }
    
    
}




/* 显示本地设备信息 */
void local_device_info_show()
{
    UINT8 i, j;
    printk("device name <%s> ", __device_name);
    printk("controller num %d\n", __local_ctrl_num);
    for(i=0; i<__local_ctrl_num; i++){
        printk("\tctrl%d\n", i);
        printk("\t\t name %s\n", __local_ctrl[i].name);
        printk("\t\t type %s\n", __local_ctrl[i].devType == 0 ?"LED":"ONOFF");
        printk("\t\t configuration:\n\t\t ");
        for(j=0; j<24; j++){
            printk("%.2x ", __local_ctrl[i].ctrl.led.pwm[j]);
        }
        printk("\n");
    }
}



/* 暂停时间，秒 */
static UINT16 __device_pause_second;
/* 保存暂停命令的各个控制器的值的数组 */
static UINT8 __local_ctrl_pause_value[8];


/* 获取某个控制器暂停的时间 */
static INT8 get_pause_value(UINT8 id, UINT8 * value)
{
    *value = __local_ctrl_pause_value[id];
    return (__device_pause_second)>0?1:0;
}

/* 暂停， 命令格式
 * byte0:  时间，单位分钟
 * byte1-n:控制器0-n的值
 */
static void device_pause(struct rf_cmd * pCmd)
{
    UINT8 i;
    __device_pause_second = (UINT16)pCmd->data[0] * 60;
    DBG_PRINT("do pause, %d seconds\n", __device_pause_second);
    if(__device_pause_second > 7200){ /* max 120min, 7200seconds */
	__device_pause_second = 7200;
    }
    for(i=0; i<__local_ctrl_num; i++){
	__local_ctrl_pause_value[i] = pCmd->data[1 + i];
	DBG_PRINT("ctrl %d pause value 0x%x\n", i, __local_ctrl_pause_value[i]);
    }
}
#ifdef INCLUDE_KTANK_LED

/* LED数量 */
#define TEMPERATURE_SENSOR_NUM 2
struct __led_temperature_info{
    UINT8 sensor_num;
    INT16 temperature[TEMPERATURE_SENSOR_NUM];
    UINT8 led_num;
    UINT32 lastTime[TEMPERATURE_SENSOR_NUM]; /* 上一次获取温度的时间 */
}temperature_info[1];

/* 获取当前所有温度传感器最高的温度 */
static INT8 local_max_temperature_get()
{
    UINT8 sensor_id, max = 0, i;
    INT16 temperature;
    /* 获取温度 */
    temperature = ds_get_id_temperaturex16_unblock(&sensor_id);
    if(temperature > -1000 && sensor_id < TEMPERATURE_SENSOR_NUM){	/* 成功获取温度 */
	temperature_info[0].temperature[sensor_id] = temperature / 16;
	temperature_info[0].lastTime[sensor_id] = timebase_get();
    }else{			/* 有错误发生 */
	
    }
    for(i=0; i<1; i++){
	UINT32 diff = time_diff_ms(temperature_info[0].lastTime[i]);
	if(diff > 60000){	/* 60秒没有成功获取温度 */
	    temperature_info[0].temperature[i] = 120;
	}
	if(diff > 10000){	/* 十秒没有成功获取温度,温度+10度 */
	    temperature_info[0].lastTime[i] = local_ms_get();
	    temperature_info[0].temperature[i] += 10;
	    if(temperature_info[0].temperature[i] > 120)
	      temperature_info[0].temperature[i] = 120;
	}
	if(max < temperature_info[0].temperature[i])
	  max = temperature_info[0].temperature[i];
    }
    return max;
}



static void dualLedProcess(struct kfish_led_info * led1, struct kfish_led_info * led2)
{
#define PWM_INC(pwm, inc) do{if(255 - pwm < inc){pwm = 255;}else{pwm=pwm+inc;}}while(0)
#define PWM_DEC(pwm, dec) do{			\
	if(pwm < dec){				\
	    pwm = 0;				\
	}else{					\
	    pwm=pwm-dec;			\
	}					\
    }while(0)
#define LED_PWM_SET(id, pwm) pwm_set(id+1, pwm)
#define LED_PWM_GET(id) pwm_get(id+1)
    UINT8 beginVal, endVal;
    UINT8 fan_pwm, led_pwm;
    INT16 temperature;
    UINT8 i;
    struct rtc_time time;
    INT16 diff;
    temperature = local_max_temperature_get();
    /* 每2秒检查一次 */
    if(local_time_diff_ms(led1->lastTime) < 2000){
	return;
    }
    led1->lastTime = local_ms_get();
    
    local_time_get(&time);
    
    /* led需要时刻检查温度是否正常，需要动态调整风扇转速度
     * 如果温度超高，还需要降低led功率
     */
    
    
    /* 计算新的风扇PWM值 */
    fan_pwm = pwm_get(0);
    if(temperature > 60){ /* 大于60度，风扇全速 */
	fan_pwm = 255;
	
    }else if(temperature < 30){ /* 35度以下停止转动 */
	fan_pwm = 0;
    }else if(temperature < 40){/* 40度以下开始降低转速 */
	PWM_DEC(fan_pwm, 5);
    }else if(temperature > 50){/* 50度以下开始增加转速 */
	PWM_INC(fan_pwm, 5);
    }
    for(i=0; i<2; i++){
	if(temperature > 65){	/* 温度失控，降低灯亮度 */
	    if(__local_ctrl[i].ctrl.led.pwm_now > 50)
	      PWM_DEC(__local_ctrl[i].ctrl.led.pwm_now, 30);
	}else if(get_pause_value(i, &diff) > 0){ /* 暂停状态，使用暂停的配置 */
	    __local_ctrl[i].ctrl.led.pwm_now = (UINT8)diff;
	}else{		/* 根据分钟数算一个pwm值 */
	    beginVal = __local_ctrl[i].ctrl.led.pwm[time.hour];
	    endVal = __local_ctrl[i].ctrl.led.pwm[(time.hour + 1) % 24];
	    diff = (INT16)endVal - (INT16)beginVal;
	    
	    diff = diff * time.min / 60;
	    diff += (INT16)beginVal;
	    /* 渐变，不一下子亮度跳变 */
	    beginVal = LED_PWM_GET(i);
	    DBG_PRINT("led %d %d->%d\n", i, beginVal, diff);
	    if(beginVal > diff){
		__local_ctrl[i].ctrl.led.pwm_now = beginVal - 1;
	    }else if(beginVal < diff){
		__local_ctrl[i].ctrl.led.pwm_now = beginVal + 1;
	    }else{
		/* 相同，不处理 */
		__local_ctrl[i].ctrl.led.pwm_now = beginVal;
	    }
	}
	LED_PWM_SET(i, __local_ctrl[i].ctrl.led.pwm_now);
	DBG_PRINT("led %d pwm %d fan %d temp %d\n", i, LED_PWM_GET(i), fan_pwm, temperature);
    }
    pwm_set(0, fan_pwm);	/* 设置风扇 */
    printk("%x %x %x\n", pwm_get(0), pwm_get(1), pwm_get(2));
}
#endif	/* #ifdef INCLUDE_KTANK_LED */


/* 本地设备监测API */
void local_device_update()
{
    UINT8 i, val;
    static UINT32 __last_entry_time = 0;
    /* 对暂停命令的处理 */
    if(__device_pause_second > 0 && local_time_diff_ms(__last_entry_time) >= 1000){
	__device_pause_second--;
	__last_entry_time = local_ms_get();
	DBG_PRINT("pause left %d seconds\n", __device_pause_second);
    }
    for(i=0; i<__local_ctrl_num; i++){
	switch(__local_ctrl[i].devType){
#ifdef INCLUDE_KTANK_LED
	  case KFISH_LED_LIGHT:
	    dualLedProcess(&__local_ctrl[0].ctrl.led, &__local_ctrl[1].ctrl.led);
	    break;
#endif
#ifdef INCLUDE_KTANK_SWITCH
	  case KFISH_SWITCH:
	    if(get_pause_value(i, &val) > 0){ /* 暂停状态，使用暂停的配置 */
		switch_set(i, val);
		__local_ctrl[i].ctrl.swi.isOn = val;
	    }else{
		switch_set(i, __local_ctrl[i].ctrl.swi.onoff[i]);
		__local_ctrl[i].ctrl.swi.isOn = __local_ctrl[i].ctrl.swi.onoff[i];
	    }
	    break;
#endif
	  default:
	    DBG_PRINT("err!!%d\n", __LINE__);
	    break;
	    
	}
    }
}


static INT8 cmdSetProcess(struct rf_cmd * pCmd)
{
    switch(pCmd->cmd){
      case KFISH_CMD_SET_CTRL_CFG:
	if(pCmd->ctrlId >= __local_ctrl_num){
	    return CMD_PROCESS_ERROR;
	}
	memcpy(__local_ctrl[pCmd->ctrlId].ctrl.led.pwm, pCmd->data, 24);
	local_device_info_save();
	return CMD_PROCESS_ACK;
      case KFISH_CMD_SET_CTRL_NAME:
	if(pCmd->ctrlId >= __local_ctrl_num){
	    return CMD_PROCESS_ERROR;
	}
	memcpy(__local_ctrl[pCmd->ctrlId].name, pCmd->data, 24);
	__local_ctrl[pCmd->ctrlId].name[23] = 0;
	local_device_info_save();
	DBG_PRINT("\nset  ctrl%d name %s\n", pCmd->ctrlId, __local_ctrl[pCmd->ctrlId].name);
	return CMD_PROCESS_ACK;
      case KFISH_CMD_SET_DEVICE_NAME:
	memcpy(__device_name, pCmd->data, 24);
	__device_name[23] = 0;
	DBG_PRINT("new dev name %s\n", __device_name);
	local_device_info_save();
	return CMD_PROCESS_ACK;
      case KFISH_CMD_TIME_NOTIFY:
	/* 时间同步是一个特殊的消息，主同一个同步命令只需要理会一次 */
        /* slave time process */{
          static UINT8 __last_time_seq = 0;
          if(__last_time_seq != pCmd->seqNum){
              DBG_PRINT("time sync %d-%d-%d %d:%d:%d\n",
                     pCmd->data[0], pCmd->data[1], pCmd->data[2],
                     pCmd->data[4], pCmd->data[5], pCmd->data[6]);
              __last_time_seq = pCmd->seqNum;
          }
	  return CMD_PROCESS_NO_ACK;
      }
      case KFISH_CMD_PAUSE_DEVICE:
	device_pause(pCmd);
	return CMD_PROCESS_ACK;
      default:
	return CMD_PROCESS_ERROR;
    }
}


static INT8 cmdGetProcess(struct rf_cmd * pCmd)
{
    switch(pCmd->cmd){
      case KFISH_CMD_GET_DEVICES_CTRL_NUM: /* 获取设备的控制器数量 */
	pCmd->data[0] = __local_ctrl_num;
	return CMD_PROCESS_ACK;
      case KFISH_CMD_GET_DEVICE_NAME:
	memcpy((char *)pCmd->data, __device_name, 24);
	pCmd->data[23] = 0;
	return CMD_PROCESS_ACK;
      case KFISH_CMD_GET_CTRL_STATUS:/* 获取控制器状态*/
	if(pCmd->ctrlId >= __local_ctrl_num)
	  return CMD_PROCESS_ERROR;
	pCmd->data[0] = __local_ctrl[pCmd->ctrlId].devType;
	memcpy(&pCmd->data[1], &__local_ctrl[pCmd->ctrlId].ctrl, 8);
	DBG_PRINT("ctrl %d info ack\n");
	return CMD_PROCESS_ACK;
      case KFISH_CMD_GET_CTRL_NAME: /* 获取控制器名称 */
	if(pCmd->ctrlId >= __local_ctrl_num)
	  return CMD_PROCESS_ERROR;
	memcpy((char *)pCmd->data, __local_ctrl[pCmd->ctrlId].name, 24);
	pCmd->data[23] = 0;
	return CMD_PROCESS_ACK;
      case KFISH_CMD_GET_CTRL_CFG:/* 获取控制器定时配置 */
	memcpy(pCmd->data, __local_ctrl[pCmd->ctrlId].ctrl.led.pwm, 24);
	return CMD_PROCESS_ACK;
      case KFISH_CMD_GET_DEVICE_TIME:
	local_time_get((struct rtc_time *)pCmd->data);
	return CMD_PROCESS_ACK;
      default:
	/* unknown command */
	return CMD_PROCESS_ERROR;
    }
}



/* 本地设备命令处理函数 */
INT8 uartCmdLocalProcess(struct rf_cmd * pCmd)
{
    if(pCmd->cmd>= 0x80){/* command  request info*/
	return cmdGetProcess(pCmd);
    }else{
	return cmdSetProcess(pCmd);
    }
}
