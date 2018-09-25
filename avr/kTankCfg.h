#ifndef __KTANK_CFG_H__
#define __KTANK_CFG_H__

/* 命令处理的结果 */
#define CMD_PROCESS_ACK 0	/* 成功，发送应答数据 */
#define CMD_PROCESS_NO_ACK 1	/* 不发送应答 */
#define CMD_PROCESS_ERROR -1	/* 错误 */

/* 保存本地设备信息 */
void local_device_info_save();

/* 从eeprom加载本地设备信息 */
void local_device_info_load();


void local_device_update();

/* 本地设备命令处理函数 */
INT8 uartCmdLocalProcess(struct rf_cmd * pCmd);

/* 显示本地设备信息 */
void local_device_info_show();

#endif
