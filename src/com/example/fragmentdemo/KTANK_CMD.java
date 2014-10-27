package com.example.fragmentdemo;

public class KTANK_CMD {
	final static int KFISH_CMD_TIME_NOTIRY = 0; /*
												 * master notify current time
												 * {1B hour 1Bmin; 4Byte dev
												 * status}
												 */
	final static int KFISH_CMD_DISCOVER = 1; /* 4Byte dev status */
	final static int KFISH_CMD_DISCOVER_ACK = 2; /* 1B devId; 1B ctrlNum */
	final static int KFISH_CMD_DEVCONFIR = 3; /* 4Byte dev status */
	final static int KFISH_CMD_DEVCONFIRM_ACK = 4; /*
													 * 4Byte dev status; 1B
													 * controlNum
													 */

	final static int KFISH_HEART_BEAT = 0x70; /* master setup controller; */
	final static int KFISH_HEART_BEAT_ACK = 0x71; /* master setup controller */

	final static int KFISH_CMD_CTRL_SET = 0x10; /* master setup controller */
	final static int KFISH_CMD_CTRL_ACK = 0x11; /* master setup controller */

	final static int KFISH_CMD_CTRLS_GET = 0x12; /* request controllers' info */
	final static int KFISH_CMD_CTRLS_ACK = 0x13; /*
												 * all control info {1B
												 * ctrl_num; ctrl0 type; ctrl1
												 * type...ctrl14 type}
												 */
	final static int KFISH_CMD_CTRL_INFO_GET = 0x14; /*
													 * master get controller
													 * info
													 */
	final static int KFISH_CMD_CTRL_INFO_RESP = 0x15; /*
													 * controller info; first 24
													 * bytes of kfish_xxx_info
													 */

	final static int KFISH_CMD_CTRL_CFG_GET = 0x16; /*
													 * master get controller
													 * info
													 */
	final static int KFISH_CMD_CTRL_CFG_RESP = 0x17; /* 24Bytes cfg */

	final static int KFISH_CMD_DISCOVER_HOST = 0x18; /*
													 * host broadcast device
													 * infomation
													 */

	final static int KFISH_CMD_SET_DEVICES_INFO = 0x60;
	final static int KFISH_CMD_SET_DEVICE_NAME = 0x61;
	final static int KFISH_CMD_SET_CTRL_INFO = 0x62;
	final static int KFISH_CMD_SET_CTRL_NAME = 0x63;
	final static int KFISH_CMD_SET_CTRL_CFG = 0x64;
	final static int KFISH_CMD_DEV_PAUSE = 0x65;
	final static int KFISH_CMD_SET_DEV_TIME = 0x66;

	final static int KFISH_CMD_GET_DEVICES_INFO = 0x80;
	final static int KFISH_CMD_GET_DEVICE_NAME = 0x81;
	final static int KFISH_CMD_GET_CTRL_INFO = 0x82;
	final static int KFISH_CMD_GET_CTRL_NAME = 0x83;
	final static int KFISH_CMD_GET_CTRL_CFG = 0x84;
	final static int KFISH_CMD_GET_DEV_TIME = 0x85;

}
