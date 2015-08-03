package com.kemaliu.ktank;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.kemaliu.ktank.BLUETOOTH_CTRL;
import com.kemaliu.ktank.KTANK_CMD;
import com.kemaliu.ktank.kTankDevice;
import com.kemaliu.ktank.kTankDevice.KTANKCTRL;

public class BLUETOOTH_CTRL {
	Handler msgHandler;
	private Object bt_sem_obj = new Object();
	private void notifyActivity(int state) {
		Message m = new Message();
		m.what = state;
		msgHandler.sendMessage(m);
	}

	private void notifyActivity(int state, String str) {
		Message m = new Message();
		m.what = state;
		if (str != null) {
			Bundle bundle = new Bundle();
			bundle.putString("t0", str);
			m.setData(bundle);
		}
		msgHandler.sendMessage(m);
	}

	MainActivity mainActivity;

	public BLUETOOTH_CTRL(MainActivity act,//主窗口句柄 
			Handler handler//住窗口消息处理单元句柄
			) {
		mainActivity = act;
		msgHandler = handler;
	}

	/**
	 * 与设备配对 参考源码：platform/packages/apps/Settings.git
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	static public boolean createBond(Class btClass, BluetoothDevice btDevice)
			throws Exception {
		Method createBondMethod = btClass.getMethod("createBond");
		Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	/**
	 * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
	 * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
	 */
	static public boolean removeBond(Class btClass, BluetoothDevice btDevice)
			throws Exception {
		Method removeBondMethod = btClass.getMethod("removeBond");
		Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
		return returnValue.booleanValue();
	}

	static public boolean setPin(Class btClass, BluetoothDevice btDevice,
			String str) throws Exception {
		try {
			Method removeBondMethod = btClass.getDeclaredMethod("setPin",
					new Class[] { byte[].class });
			Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice,
					new Object[] { str.getBytes() });
			Log.e("returnValue", "" + returnValue);
		} catch (SecurityException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;

	}

	// 取消用户输入
	static public boolean cancelPairingUserInput(Class btClass,
			BluetoothDevice device)

	throws Exception {
		Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
		// cancelBondProcess()
		Boolean returnValue = (Boolean) createBondMethod.invoke(device);
		return returnValue.booleanValue();
	}

	// 取消配对
	static public boolean cancelBondProcess(Class btClass,
			BluetoothDevice device)

	throws Exception {
		Method createBondMethod = btClass.getMethod("cancelBondProcess");
		Boolean returnValue = (Boolean) createBondMethod.invoke(device);
		return returnValue.booleanValue();
	}

	/**
	 * 
	 * @param clsShow
	 */
	static public void printAllInform(Class clsShow) {
		try {
			// 取得所有方法
			Method[] hideMethod = clsShow.getMethods();
			int i = 0;
			for (; i < hideMethod.length; i++) {
				Log.e("method name", hideMethod[i].getName() + ";and the i is:"
						+ i);
			}
			// 取得所有常量
			Field[] allFields = clsShow.getFields();
			for (i = 0; i < allFields.length; i++) {
				Log.e("Field name", allFields[i].getName());
			}
		} catch (SecurityException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// throw new RuntimeException(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static public boolean pair(String strAddr, String strPsw) {
		boolean result = false;
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		bluetoothAdapter.cancelDiscovery();

		if (!bluetoothAdapter.isEnabled()) {
			bluetoothAdapter.enable();
		}

		if (!BluetoothAdapter.checkBluetoothAddress(strAddr)) { // 检查蓝牙地址是否有效

			Log.d("mylog", "devAdd un effient!");
		}

		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(strAddr);

		if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
			try {
				Log.d("mylog", "NOT BOND_BONDED");
				BLUETOOTH_CTRL.setPin(device.getClass(), device, strPsw); // 手机和蓝牙采集器配对
				BLUETOOTH_CTRL.createBond(device.getClass(), device);
				// remoteDevice = device; // 配对完毕就把这个设备对象传给全局的remoteDevice
				result = true;
			} catch (Exception e) {
				// TODO Auto-generated catch block

				Log.d("mylog", "setPiN failed!");
				e.printStackTrace();
			} //

		} else {
			Log.d("mylog", "HAS BOND_BONDED");
			try {
				BLUETOOTH_CTRL.createBond(device.getClass(), device);
				BLUETOOTH_CTRL.setPin(device.getClass(), device, strPsw); // 手机和蓝牙采集器配对
				BLUETOOTH_CTRL.createBond(device.getClass(), device);
				// remoteDevice = device; // 如果绑定成功，就直接把这个设备对象传给全局的remoteDevice
				result = true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.d("mylog", "setPiN failed!");
				e.printStackTrace();
			}
		}
		return result;
	}

	private boolean bluetooth_original_status;
	private BluetoothAdapter btAdapt = null;

	public void btInit() {

		btAdapt = BluetoothAdapter.getDefaultAdapter();
		bluetooth_original_status = btAdapt.isEnabled();
		btAdapt.isEnabled();
		if (!bluetooth_original_status) {
			btAdapt.enable();
		}
		int cnt = 0;
		while (cnt < 5000) {
			if (btAdapt.isEnabled())
				break;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			cnt++;
		}
		if (btAdapt.isEnabled() == false) {
			notifyActivity(mainActivity.BT_STATE_LOG, "打开蓝牙fail");
			notifyActivity(mainActivity.BT_FATAL_ERROR);
			return;
		}
		// 注册Receiver来获取蓝牙设备相关的结果
		String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
		intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		intent.addAction(ACTION_PAIRING_REQUEST);
		intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		mainActivity.registerReceiver(searchDevices, intent);

		if (btAdapt.getState() == BluetoothAdapter.STATE_OFF) {// 如果蓝牙还没开启
			notifyActivity(mainActivity.BT_STATE_LOG, "请先打开蓝牙");
			return;
		}
		if (btAdapt.isDiscovering())
			btAdapt.cancelDiscovery();
		// lstDevices.clear();
		Object[] lstDevice = btAdapt.getBondedDevices().toArray();
		int findMark = 0;
		BluetoothDevice device = null;
		// search blue tooth device
		for (int i = 0; i < lstDevice.length; i++) {
			device = (BluetoothDevice) lstDevice[i];
			String nameStr = null;
			nameStr = device.getName()// .substring(0, 6)
			;
			if (0 != "kfish-2".compareTo(nameStr)) {
				continue;
			}
			nameStr = null;
			findMark = 1;
			break;
		}
		if (findMark == 0 || device == null) {
			// found none, report error
			notifyActivity(mainActivity.BT_STATE_LOG, "蓝牙连接KFISH失败");
			notifyActivity(mainActivity.BT_FATAL_ERROR);

			// this.setTitle("discovering");
			// ((MainActivity)getActivity()).Toast.makeText(this,
			// "discover ",Toast.LENGTH_SHORT).show();
			/* btAdapt.startDiscovery(); */
			return;
		}

		// try connect
		if (0 != connect(device)) {
			notifyActivity(mainActivity.BT_STATE_LOG, "程序将会在三秒内退出");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			notifyActivity(mainActivity.BT_STATE_FORCE_QUIT);
			return;
		}
		notifyActivity(mainActivity.BT_STATE_LOG, "获取设备列表...");

		if (0 != kTankGetDevicesInfo()) {
			notifyActivity(mainActivity.BT_STATE_LOG_RAW, "失败!!!!");
			notifyActivity(mainActivity.BT_FATAL_ERROR);
			return;
		}
		notifyActivity(mainActivity.BT_STATE_LOG, "设备数:"
				+ mainActivity.deviceNum + " :)");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		notifyActivity(mainActivity.BT_STATE_READY);

		
		/*while (!Thread.interrupted()) {
			byte[] buf = new byte[100];
			try {
				Thread.sleep(1000);
				int retry;
				for (retry = 0; retry < 3; retry++) {
					if (24 == remoteInfomationRequest(0xff, 0,
									KTANK_CMD.KFISH_CMD_GET_DEV_TIME, buf, 24)) {
						if(mainActivity.settingFragment != null){
							mainActivity.settingFragment.dev_time_update(buf[4], buf[5], buf[6]);
						}
						break;
					}
				}
				
				
			} catch (InterruptedException e) { // TODO
				
			}
		}*/
		Log.d("kTANK", "thread quit");
		
		return;

	}

	final int MAX_RETRY_NUMBER = 3;

	private int kTankGetDevInfo(kTankDevice device) {
		int ctrl_i, retry;
		byte[] info = new byte[24];
		// get device name
		for (retry = 0; retry < MAX_RETRY_NUMBER; retry++) {
			if (24 == remoteInfomationRequest(device.devId, 0,
					KTANK_CMD.KFISH_CMD_GET_DEVICE_NAME, info, 24)) {

				try {
					int byteLen = 0;
					while (info[byteLen] != 0) {
						byteLen++;
					}
					device.name = new String(info, 0, byteLen, "gb2312");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		if (retry >= MAX_RETRY_NUMBER) {
			return -1;
		}
		// get every controller's info
		for (ctrl_i = 0; ctrl_i < device.getCtrlNum(); ctrl_i++) {
			notifyActivity(mainActivity.BT_STATE_LOG, "控制器" + ctrl_i);
			KTANKCTRL ctrl = null;
			// get controller base info
			for (retry = 0; retry < MAX_RETRY_NUMBER; retry++) {
				if (24 == remoteInfomationRequest(device.devId, ctrl_i,
						KTANK_CMD.KFISH_CMD_GET_CTRL_STATUS, info, 24)) {
					ctrl = device.new KTANKCTRL(info);
					device.controller[ctrl_i] = ctrl;
					ctrl.updateLocalStatus(info);
					break;
				}
			}
			if (retry >= MAX_RETRY_NUMBER) {
				return -1;
			}
			if (ctrl == null) {
				String str = "获取设备ID(" + device.devId + ")控制器 " + ctrl_i
						+ "信息失败";
				notifyActivity(mainActivity.BT_STATE_LOG, str);
				continue;
			} else {
				notifyActivity(mainActivity.BT_STATE_LOG_RAW,
						" 类型:" + ctrl.ctrlTypeName());
			}
			// get controller name
			for (retry = 0; retry < MAX_RETRY_NUMBER; retry++) {
				if (24 == remoteInfomationRequest(device.devId, ctrl_i,
						KTANK_CMD.KFISH_CMD_GET_CTRL_NAME, info, 24)) {
					try {
						int byteLen = 0;
						while (info[byteLen] != 0) {
							byteLen++;
						}
						ctrl.name = new String(info, 0, byteLen, "gb2312");
						notifyActivity(mainActivity.BT_STATE_LOG_RAW, " 名:"
								+ ctrl.name);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
			if (retry >= MAX_RETRY_NUMBER) {
				return -1;
			}
			// get controller's cfg
			for (retry = 0; retry < MAX_RETRY_NUMBER; retry++) {
				if (24 == remoteInfomationRequest(device.devId, ctrl_i,
						KTANK_CMD.KFISH_CMD_GET_CTRL_CFG, info, 24)) {
					ctrl.updateLocalCfg(info);
					break;
				}
			}

			if (retry >= MAX_RETRY_NUMBER) {
				return -1;
			}

		}
		return 0;
	}

	private int kTankGetDevicesInfo() {
		int retry, ret = 0, i;
		byte[] info = new byte[24];
		// get device lists
		for (retry = 0; retry < MAX_RETRY_NUMBER; retry++) {
			ret = remoteInfomationRequest(0, 0,
					KTANK_CMD.KFISH_CMD_GET_DEVICES_INFO, info, 24);
			if (ret == 24) {
				break;
			}

		}
		if (retry >= MAX_RETRY_NUMBER) {
			notifyActivity(mainActivity.BT_STATE_LOG, "重试超时");
			return -1;
		}
		if (ret != 24) {
			notifyActivity(mainActivity.BT_STATE_LOG, "设备列表长度非法");
			return -1;
		}
		if (ret == 24) {
			for (i = 0; i < 24; i++) {
				if (info[i] == 0) {
					continue;
				}
				notifyActivity(mainActivity.BT_STATE_LOG, "读取设备ID:" + i
						+ " 详细信息");
				kTankDevice device = new kTankDevice(info[i]);
				device.devId = i;
				if (0 == kTankGetDevInfo(device)) {
					/*获取设备信息成功，添加到设备列表*/
					mainActivity.device[mainActivity.deviceNum] = device;
					mainActivity.deviceNum++;
					notifyActivity(mainActivity.BT_STATE_LOG, "设备名<"
							+ device.name + ">");
				} else {
					notifyActivity(mainActivity.BT_STATE_LOG, "获取设备ID(" + i
							+ ")信息失败");
				}
			}
		}
		return 0;
	}

	private BroadcastReceiver searchDevices = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			Message m;
			String action = intent.getAction();
			Bundle b = intent.getExtras();
			Object[] lstName = b.keySet().toArray();

			// 显示所有收到的消息及其细节
			for (int i = 0; i < lstName.length; i++) {
				String keyName = lstName[i].toString();
				Log.e(keyName, String.valueOf(b.get(keyName)));
			}
			BluetoothDevice device = null;
			// 搜索设备时，取得设备的MAC地址
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {

				device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				String nameStr = null;
				nameStr = device.getName();// .substring(0, 5)

				if (0 != "kfish-2".compareTo(nameStr)) {
					return;
				}
				nameStr = null;

				if (device.getBondState() == BluetoothDevice.BOND_NONE) {

					String str = "未配对|" + device.getName() + "|"
							+ device.getAddress();
					Toast.makeText(mainActivity, str, Toast.LENGTH_SHORT)
							.show();
					// if (lstDevices.indexOf(str) == -1)// 防止重复添加
					// lstDevices.add(str); // 获取设备名称和mac地址

					try {
						Toast.makeText(mainActivity,
								"try 配对......" + device.getName(),
								Toast.LENGTH_SHORT).show();
						BLUETOOTH_CTRL
								.setPin(device.getClass(), device, "8410"); // 手机和蓝牙采集器配对
						BLUETOOTH_CTRL.createBond(device.getClass(), device);
						BLUETOOTH_CTRL.cancelPairingUserInput(
								device.getClass(), device);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					String str = "已配对|" + device.getName() + "|"
							+ device.getAddress();
					Toast.makeText(mainActivity, str, Toast.LENGTH_SHORT)
							.show();
					// connect(device);//连接设备
					// lstDevices.add(str); // 获取设备名称和mac地址
				}
				// adtDevices.notifyDataSetChanged();
				// if(adtDevices.getCount() > 0)
				// btDevListView.setVisibility(View.VISIBLE);
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
				device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				switch (device.getBondState()) {
				case BluetoothDevice.BOND_BONDING:
					Toast.makeText(mainActivity,
							"正在配对......" + device.getName(), Toast.LENGTH_SHORT)
							.show();
					Log.d("BlueToothTestActivity", "正在配对......");
					// String str = "正在配对|" + device.getName() + "|"
					// + device.getAddress();
					break;
				case BluetoothDevice.BOND_BONDED:
					Toast.makeText(mainActivity,
							"完成配对......" + device.getName(), Toast.LENGTH_SHORT)
							.show();
					Log.d("BlueToothTestActivity", "完成配对");
					// connect(device);//连接设备
					break;
				case BluetoothDevice.BOND_NONE:
					Log.d("BlueToothTestActivity", "取消配对");
				default:
					break;
				}
			}

			if (intent.getAction().equals(
					"android.bluetooth.device.action.PAIRING_REQUEST")) {

				Log.e("tag11111111111111111111111", "ddd");
				BluetoothDevice btDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				// byte[] pinBytes = BluetoothDevice.convertPinToBytes("8410");
				// device.setPin(pinBytes);
				int retry;
				/* while(retry++ < 5) */{
					try {
						BLUETOOTH_CTRL.setPin(btDevice.getClass(), btDevice,
								"8410"); // 手机和蓝牙采集器配对
						BLUETOOTH_CTRL
								.createBond(btDevice.getClass(), btDevice);
						BLUETOOTH_CTRL.cancelPairingUserInput(
								btDevice.getClass(), btDevice);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	};
	public OutputStream outStream;
	public InputStream inStream;
	public static BluetoothSocket btSocket;

	private int connect(BluetoothDevice btDev) {
		int retry;
		final UUID SPP_UUID = UUID
				.fromString("00001101-0000-1000-8000-00805F9B34FB");
		UUID uuid = SPP_UUID;
		// this.setTitle("connection");
		retry = 0;
		while (retry++ < 5) {
			notifyActivity(mainActivity.BT_STATE_LOG, "try connect device "
					+ btDev.getName());
			try {
				btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
				Log.d("BlueToothTestActivity", "开始连接...");
				btSocket.connect();
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// Toast.makeText(mainActivity,
				// "socket connect failed",Toast.LENGTH_SHORT).show();
			}
		}
		if (retry >= 5) {
			notifyActivity(mainActivity.BT_STATE_LOG,
					"connect device " + btDev.getName() + " failed!!!");
			return -1;
		}
		try {
			outStream = btSocket.getOutputStream();
			inStream = btSocket.getInputStream(); // 可在TextView里显示
			// cmdOpr.setStream(outStream, inStream);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(mainActivity, "get out/in stream failed",
					Toast.LENGTH_SHORT).show();
			return -1;
		}

		return 0;
	}

	int __cmdSeq = 0;

    /**等待应答
     * @param buf 接受数据的缓冲
     * @param waitLen 等待的数据长度
     * @param timeoutMs 等待的时间长度，单位ms.如果超时，返回读取的长度
     * @return 获取的数据长度，如果CRC有错误，返回-1
     */
	private int waitACK(byte[] buf, int waitLen, int timeoutMs) {
		long entry_time = System.currentTimeMillis();
		int len, pos = 0, sync = 0;
		byte[] rxBuf = new byte[64];
		try {
			do {
				len = inStream.available();
				if (len <= 0)
					continue;
				if (sync == 1)
					pos += inStream.read(rxBuf, pos,
							((35 - pos) < len) ? (35 - pos) : len);
				else
					pos += inStream.read(rxBuf, pos, 1);
				if (sync == 0) {
					if (pos == 1) {
						if (rxBuf[0] == (byte) 0xfe) {
							pos = 1;
						} else {
							pos = 0;
						}
					}
					if (pos == 2) {
						if (rxBuf[1] == (byte) 0x1c) {
							sync = 1;
						} else if (rxBuf[1] == (byte) 0xfe) {
							pos = 1;
						} else {
							pos = 0;
						}
					}
				} else {
					if (pos >= waitLen) {
						byte crc = 0;
						int i;
						for(i=3; i<waitLen; i++)
							crc += rxBuf[i];
						if(crc == rxBuf[2]){
							System.arraycopy(rxBuf, 3, buf, 0, waitLen - 3);
							return waitLen - 3;
						}else{
							return -1;
						}
								
					}
				}
			} while (System.currentTimeMillis() - entry_time < (long) timeoutMs);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		return pos - 3;
	}
//   struct rf_cmd{
//   UINT8 srcId;                /* 源设备ID */
//    UINT8 destId;               /* 目的设备ID */
//    UINT8 rfPlane:4;            /* 跳频方案编号 */
//    UINT8 ctrlId:4;             /* ctrl ID，命令发向的控制器ID */
//    UINT8 cmd;                  /* KFISH_CMD_T */
//    union{
//        struct{
//            UINT8 magic; /* 随机数,主机发送的每一个消息都有一个唯一的随机数 */
//            UINT8 seqNum;               /* sequence number */
//        };
//        UINT16 identification;  /* magic和seq合并在一起构成一个识别码 */
//    };
//    UINT8 rsv[2];               /* device */
//    UINT8 data[24];             /* cmd data */
//   };
/* UART发送的数据除了struct rf_cmd外，前面加了3Bytes的头
 * 前两个字节为0xfe,0x1c, 第三个字节为CRC
 * */
	public int remoteInfomationRequest(int devId, int ctrlId, int cmdType,
			byte[] buf, int bufLen) {
		synchronized(bt_sem_obj){
		byte[] cmd = new byte[20];
		byte[] rxBuf = new byte[64];
		int i;
		int cmdLen, rxLen;
		cmd[0] = (byte) 0xfe;//src
		cmd[1] = (byte) 0x1c;//src
		cmd[2] = 0;//CRC后面再计算
		cmd[3] = 0;//srcId,串口通信中无用
		cmd[4] = (byte) devId;//destID, 目的设备ID
		cmd[5] = (byte)(ctrlId << 4);//高4bit, plane(串口通信中无用),低4bit controller ID 
		cmd[6] = (byte) cmdType;// src id ,ignore
		cmd[7] = (byte) devId;// magic， 串口通信中无用
		cmd[8] = 0;// seq, 串口通信中无用
		cmd[9] = 0;// rsv, 保留
		cmd[10] = (byte) cmdType;// rsv, 保留

		//计算CRC
		cmd[2] = 0;
		for(i=3; i<11; i++)
			cmd[2] += cmd[i];
		cmdLen = 11;
		try {
			outStream.flush();
			inStream.skip(inStream.available());
			outStream.write(cmd, 0, cmdLen);
			rxLen = waitACK(rxBuf, 35, 500);
			if (rxLen >= 32) {
				for (i = 0; i < 24; i++)
					buf[i] = rxBuf[8 + i];
				return 24;
			}
			return 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		}
	}

	public int remoteInfomationSave(int devId, int ctrlId, int cmdType,
			byte[] buf, int bufLen) {
		synchronized(bt_sem_obj){
		int i;
		byte[] cmd = new byte[40];
		byte[] rxBuf = new byte[64];
		int cmdLen, rxLen;
		cmd[0] = (byte) 0xfe;//src
		cmd[1] = (byte) 0x1c;//src
		cmd[2] = 0;//CRC后面再计算
		cmd[3] = 0;//srcId,串口通信中无用
		cmd[4] = (byte) devId;//destID, 目的设备ID
		cmd[5] = (byte)(ctrlId << 4);//高4bit, plane(串口通信中无用),低4bit controller ID 
		cmd[6] = (byte) cmdType;// src id ,ignore
		cmd[7] = (byte) devId;// magic， 串口通信中无用
		cmd[8] = 0;// seq, 串口通信中无用
		cmd[9] = 0;// rsv, 保留
		cmd[10] = (byte) cmdType;// rsv, 保留
		// fill data
		System.arraycopy(buf, 0, cmd, 11, 24);
		//CRC
		cmd[2] = 0;
		for(i=3; i<35; i++)
			cmd[2] += cmd[i];
		cmdLen = 35;
		try {
			outStream.write(cmd, 0, cmdLen);
			rxLen = waitACK(rxBuf, 35, 500);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		if (rxLen >= 32) {
			for (i = 0; i < 24; i++)
				buf[i] = rxBuf[8 + i];
		}
		return 24;
	}
	}
}
