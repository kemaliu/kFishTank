#ifndef __RF_MNG_H__
#define __RF_MNG_H__

extern UINT8 cmd_process_flag;	/* bit 0: rf command processing */
/* 查询RF模块是否忙 */
#define RF_IS_PROCESSING_CMD() ((cmd_process_flag & 1) != 0)

/* 查询RF模块是否空闲 */
#define RF_IS_NOT_PROCESSING_CMD() ((cmd_process_flag & 1) == 0)

/* 标记RF模块为非空闲状态 */
#define RF_PROCESSING_CMD_SET() do{cmd_process_flag = 1;}while(0)
/* 标记RF模块为空闲状态 */
#define RF_PROCESSING_CMD_CLR() do{cmd_process_flag = 0;}while(0)

#define RF_IS_SENDING_TIME_SYNC() ((cmd_process_flag & 2) != 0)
#define RF_NOT_SENDING_TIME_SYNC() ((cmd_process_flag & 2) == 0)
#define RF_SENDING_TIME_SYNC_SET() do{cmd_process_flag = 2;}while(0)


extern volatile UINT8 __deviceList[0x20];

INT8 rf_cmd_snd(struct rf_cmd *pCmd, UINT32 timeout);

/**
 * 检查数据应答
 * return -1:超时， 0:无应答  >0:收到的应答数据长度
 */
INT8 rf_cmd_rcv_ack(struct rf_cmd *pCmd);

/* 监听端口，决定本机host ID */
void host_listen_process();

void rf_ack_show(struct rf_cmd * pCmd);

/* 发现网络中的其他从设备 */
void host_discover_process();

void host_do_discover();

/* 保存设备列表信息 */
void devListSave();

/* 从eeprom加载设备列表信息 */
void devListLoad();

void rf_start();

#endif
