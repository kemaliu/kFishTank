package com.example.fragmentdemo;

import java.util.Map;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;

/**
 * 项目的主Activity，所有的Fragment都嵌入在这里。
 * 
 * @author guolin
 */
public class MainActivity extends Activity implements OnClickListener {
	/**
	 * 在线的所有设备 
	 */
	public kTankDevice[] device = new kTankDevice[32];
	public int tankNumber;
	public int deviceNum;
	private int clickCnt = 0;

	private TankCtrlFragment TankFragment[] = new TankCtrlFragment[3];
	private View tankLayout[] = new View[3];

	public LedCfgFragment LEDCfgFragment = null;
	public SwitchCfgFragment SwitchCfgFragment = null;
	/**
	 * 用于展示设置的Fragment
	 */
	public SettingFragment settingFragment;

	/**
	 * 设置界面布局
	 */
	private View settingLayout;

	private ImageView tankIconImage[] = new ImageView[3];

	/**
	 * 在Tab布局上显示设置图标的控件
	 */
	private ImageView settingImage;

	/**
	 * 在Tab布局上显示消息标题的控件
	 */
	private TextView messageText;

	/**
	 * 在Tab布局上显示联系人标题的控件
	 */
	private TextView contactsText;

	/**
	 * 在Tab布局上显示动态标题的控件
	 */
	private TextView newsText;

	/**
	 * 在Tab布局上显示设置标题的控件
	 */
	private TextView settingText;

	/**
	 * 用于对Fragment进行管理
	 */
	private FragmentManager fragmentManager;

	public kTankParam __params;
	public Map<String, ?> __param_map;

	BLUETOOTH_CTRL bt;

	private void gotoStartWindow() {
		setContentView(R.layout.welcome_layout);
	}

	public void startMainWindow() {
		loadSetting();
		setContentView(R.layout.activity_main);

		// 初始化布局元素
		initViews();
		fragmentManager = getFragmentManager();
		// 第一次启动时选中第0个tab
		setTabSelection(0);
	}

	final int BT_STATE_READY = 0x1010;

	final int BT_STATE_LOG = 0x4000;
	final int BT_STATE_LOG_RAW = 0x4001;


	final int BT_FATAL_ERROR = 0x8005;
	final int BT_STATE_FORCE_QUIT = 0xffff;

	public Handler msgHandler = new Handler() {
		public void handleMessage(Message msg) {
			Bundle bd;
			TextView tv = (TextView) (View) (getWindow().getDecorView())
					.findViewById(R.id.start_layout_text);
			switch (msg.what) {
			  case BT_FATAL_ERROR:
			    tv.append("\nfatal error occur, restart app or reset device then retry");
			    break;
			case BT_STATE_READY:
				tv.append("\nREADY, entering main app...");
				startMainWindow();
				break;
			case BT_STATE_LOG:// TestHandler是Activity的类名
				bd = msg.getData();
				bd.getString("t0");
				tv.append("\n" + bd.getString("t0"));
				break;
			case BT_STATE_LOG_RAW:// TestHandler是Activity的类名
				bd = msg.getData();
				bd.getString("t0");
				tv.append(bd.getString("t0"));
				break;
			case BT_STATE_FORCE_QUIT:
				finish();
				break;
			}
			super.handleMessage(msg);
		}
	};

	class BGTask implements Runnable {
		public void run() {
			bt.btInit();
		}
	}

	Thread backGroundThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		__params = new kTankParam(MainActivity.this);
		__param_map = __params.getPreferences();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		gotoStartWindow();
		bt = new BLUETOOTH_CTRL(this, msgHandler);
		backGroundThread = new Thread(new BGTask());
		backGroundThread.start();
	}

	/**
	 * 在这里获取到每个需要用到的控件的实例，并给它们设置好必要的点击事件。
	 */
	private void initViews() {
		tankLayout[0] = findViewById(R.id.tank1_layout);
		tankLayout[1] = findViewById(R.id.tank2_layout);
		tankLayout[2] = findViewById(R.id.tank3_layout);

		settingLayout = findViewById(R.id.setting_layout);
		tankIconImage[0] = (ImageView) findViewById(R.id.tank1_image);
		tankIconImage[1] = (ImageView) findViewById(R.id.tank2_image);
		tankIconImage[2] = (ImageView) findViewById(R.id.tank3_image);
		settingImage = (ImageView) findViewById(R.id.setting_image);
		messageText = (TextView) findViewById(R.id.message_text);
		contactsText = (TextView) findViewById(R.id.contacts_text);
		newsText = (TextView) findViewById(R.id.news_text);
		settingText = (TextView) findViewById(R.id.setting_text);
		tankLayout[0].setOnClickListener(this);
		tankLayout[1].setOnClickListener(this);
		tankLayout[2].setOnClickListener(this);
		settingLayout.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		clickCnt++;
		switch (v.getId()) {
		case R.id.tank1_layout:
			// 当点击了消息tab时，选中第1个tab
			setTabSelection(0);
			break;
		case R.id.tank2_layout:
			// 当点击了联系人tab时，选中第2个tab
			setTabSelection(1);
			break;
		case R.id.tank3_layout:
			// 当点击了动态tab时，选中第3个tab

			// myt.notifyAll();

			setTabSelection(2);
			break;
		case R.id.setting_layout:
			// 当点击了设置tab时，选中第4个tab
			setTabSelection(3);
			// settingFragment.deviceListUpdate(deviceNum, device);
			break;
		default:
			break;
		}
	}

	/**
	 * 根据传入的index参数来设置选中的tab页。
	 * 
	 * @param index
	 *            每个tab页对应的下标。0表示消息，1表示联系人，2表示动态，3表示设置。
	 */
	public void setTabSelection(int index) {
		// 每次选中之前先清楚掉上次的选中状态
		clearSelection();
		// 开启一个Fragment事务
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		// 先隐藏掉所有的Fragment，以防止有多个Fragment显示在界面上的情况

		hideFragments(transaction);
		switch (index) {
		case 0:
		case 1:
		case 2:
			// 当点击了消息tab时，改变控件的图片和文字颜色
			tankIconImage[index].setImageResource(R.drawable.fish);
			tankIconImage[index].setColorFilter(0);
			messageText.setTextColor(Color.WHITE);
			if (TankFragment[index] == null) {
				// 如果MessageFragment为空，则创建一个并添加到界面上
				TankFragment[index] = new TankCtrlFragment();
				TankFragment[index].setTankId(index);
				transaction.add(R.id.content, TankFragment[index]);
				// ((TextView)TankFragment[index].getView().findViewById(R.id.mycont)).setText("tank1");
			} else {
				// 如果MessageFragment不为空，则直接将它显示出来
				transaction.show(TankFragment[index]);
			}
			break;
		case 3:
		default:
			// 当点击了设置tab时，改变控件的图片和文字颜色
			settingImage.setImageResource(R.drawable.setting_selected);
			settingText.setTextColor(Color.WHITE);
			if (settingFragment == null) {
				// 如果SettingFragment为空，则创建一个并添加到界面上
				settingFragment = new SettingFragment();

				transaction.add(R.id.content, settingFragment);
			} else {
				// 如果SettingFragment不为空，则直接将它显示出来

				transaction.show(settingFragment);
			}
			break;
		case 4: // led cfg
			if (LEDCfgFragment == null) {
				LEDCfgFragment = new LedCfgFragment();
				transaction.add(R.id.content, LEDCfgFragment);
			} else {
				transaction.show(LEDCfgFragment);
			}
			break;
		case 5: // switch cfg
			if (LEDCfgFragment == null) {
				SwitchCfgFragment = new SwitchCfgFragment();
				transaction.add(R.id.content, SwitchCfgFragment);
			} else {
				transaction.show(SwitchCfgFragment);
			}
			break;
		}

		transaction.commit();
	}

	/**
	 * 清除掉所有的选中状态。
	 */
	private void clearSelection() {
		tankIconImage[0].setImageResource(R.drawable.fish);
		tankIconImage[0].setColorFilter(Color.GRAY);
		messageText.setTextColor(Color.parseColor("#82858b"));
		tankIconImage[1].setImageResource(R.drawable.fish);
		tankIconImage[1].setColorFilter(Color.GRAY);
		contactsText.setTextColor(Color.parseColor("#82858b"));
		tankIconImage[2].setImageResource(R.drawable.fish);
		tankIconImage[2].setColorFilter(Color.GRAY);
		newsText.setTextColor(Color.parseColor("#82858b"));
		settingImage.setImageResource(R.drawable.setting_unselected);
		settingText.setTextColor(Color.parseColor("#82858b"));
	}

	/**
	 * 将所有的Fragment都置为隐藏状态。
	 * 
	 * @param transaction
	 *            用于对Fragment执行操作的事务
	 */
	private void hideFragments(FragmentTransaction transaction) {
		if (TankFragment[0] != null) {
			transaction.hide(TankFragment[0]);
		}
		if (TankFragment[1] != null) {
			transaction.hide(TankFragment[1]);
		}
		if (TankFragment[2] != null) {
			transaction.hide(TankFragment[2]);
		}
		if (settingFragment != null) {
			transaction.hide(settingFragment);
		}
		if (LEDCfgFragment != null) {
			transaction.hide(LEDCfgFragment);
		}
		if (SwitchCfgFragment != null) {
			transaction.hide(SwitchCfgFragment);
		}
	}

	public void loadSetting() {
		Object tObj = __param_map.get("tank_number");
		if (tObj == null) {
			/* no configuration */
			tankNumber = 1;
		} else {
			String tStr = tObj.toString();
			tankNumber = Integer.parseInt(tStr);
		}
		int i;
		for (i = 0; i < device.length; i++) {
			if (device[i] == null)
				continue;
			// find the cfg for the device
			tObj = __param_map.get("" + i);
			device[i].tankId = 0;
			if (tObj != null && 0 == tObj.toString().compareTo("y")) {
				tObj = __param_map.get("devId" + i);
				if (tObj != null) {
					device[i].tankId = Integer.parseInt(tObj.toString());
				}
				/*tObj = __param_map.get("devName" + i);
				if (tObj != null) {
					device[i].name = tObj.toString();
				}*/
			}

		}
	}

	public void saveSetting() {
		int i;
		Editor edit = __params.getEditor();
		for (i = 0; i < deviceNum; i++) {
			edit.putString("" + device[i].devId, "y");
			edit.putInt("devId" + device[i].devId, device[i].tankId);
			edit.putString("devName" + device[i].devId, device[i].name);
		}
		edit.commit();
	}

	private long exitTime = 0;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), "再按一次退出程序",
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				// backGroundThread.interrupt();
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		Log.e("", "onCreate method is onDestroy");
		backGroundThread.interrupt();
		super.onDestroy();
	}
}
