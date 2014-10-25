package com.example.fragmentdemo;

import com.example.fragmentdemo.kTankDevice.KTANKCTRL;

import android.app.Fragment;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

public class TankCtrlFragment extends Fragment {

    private int move = 0;
    
    
    
    private class CFG_BTN_LISTEN implements OnClickListener{
	private kTankDevice.KTANKCTRL controller = null;
	private kTankDevice  device = null;、
	/**
	 * 监听配置页面中的按键事件
	 * @param device 设备句柄
	 * @param ctrl 控制器句柄
	 * */
	public CFG_BTN_LISTEN(kTankDevice device, kTankDevice.KTANKCTRL ctrl){
	    controller = ctrl;
	    this.device = device;
	}
	@Override
	  public void onClick(View arg0) {
	    // TODO Auto-generated method stub
		if(controller == null){
			return;
		}
		if(controller.controllerType == 0){//led
			((MainActivity)getActivity()).setTabSelection(4);
			((MainActivity)getActivity()).LEDCfgFragment.updateLEDCfg(device, controller, tankId);
		}else{
			((MainActivity)getActivity()).setTabSelection(5);
			((MainActivity)getActivity()).SwitchCfgFragment.updateSwitchCfg(device, controller, tankId);
		}
	       
	}
    	
    }
    private class seekBarChangeListen implements SeekBar.OnSeekBarChangeListener {

        private LinearLayout parentLayout;

        public seekBarChangeListen(LinearLayout ctrlLayout) {
            parentLayout = ctrlLayout;
        }

        /**
         * 拖动条停止拖动的时候调用
         */
        @Override
          public void onStopTrackingTouch(SeekBar seekBar) {
            move = 0;
        }

        /**
         * 拖动条开始拖动的时候调用
         */
        @Override
          public void onStartTrackingTouch(SeekBar seekBar) {
            move = 1;
        }

        /**
         * 拖动条进度改变的时候调用
         */
        @Override
          public void onProgressChanged(SeekBar seekBar, int progress,
                                        boolean fromUser) {
            if (move == 1) {
                int i, j;
                EditText et = (EditText) parentLayout
                              .findViewById(R.id.ctrl_led_pwm_value);
                et.setText("" + progress);
            }
        }
    }

    private class pwmEditWatcher implements TextWatcher {
        private LinearLayout parentLayout;
        public pwmEditWatcher(LinearLayout base_layout){
            parentLayout = base_layout;
        }
        public void afterTextChanged(Editable s) {
            SeekBar bar = (SeekBar) parentLayout
                          .findViewById(R.id.ctrl_led_bar);
            if(s.length() < 1){
                bar.setProgress(0);
                return;
            }
                        
            if(move == 0){
                String mystr = s.toString();
                int value = Integer.parseInt(mystr);
                int mod = 0;
                if(value < 0){
                    value = 0;
                    mod = 1;
                }if(value >255){
                    value = 255;
                    mod = 1;
                }else{
                }
                bar.setProgress(value);
                if(mod != 0){
                    EditText et = ((EditText)parentLayout.findViewById(R.id.ctrl_led_pwm_value));
                    String str = ""+value; 
                    et.setText(str);
                    et.setSelection(str.length());
                }
                                
            }
                        
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }
    }

    private View tankBaseLayout;
    private int tankId;
    private int deviceNum;
    final int MAX_DEVICE_NUM = 8;
    private LinearLayout[] deviceLayout = new LinearLayout[MAX_DEVICE_NUM];
    
    /** tank页面中， 每一个设备下面有2个LED控制器或4个开关控制器，共6个
     * 0-1给LED用，2-5给开关用*/
    private LinearLayout[][] ctrlLayout = new LinearLayout[MAX_DEVICE_NUM][6];

    public kTankDevice[] deviceList = new kTankDevice[MAX_DEVICE_NUM];
    LayoutInflater frag_inflater = null;

    public void setTankId(int tankId) {
        this.tankId = tankId;
    }

    public void addDevice(kTankDevice device) {
        int i, j;
        // make sure the device has not been added
        for (i = 0; i < MAX_DEVICE_NUM; i++) {
            if (deviceList[i] != null && deviceList[i].devId == device.devId) {
                return;
            }
        }
        /*在tank页面中找一个没用的设备列表*/
        for (i = 0; i < MAX_DEVICE_NUM; i++) {
            if (deviceList[i] == null) {
                deviceList[i] = device;
                if (device.tankId != this.tankId)
                  return;
                /*在页面中显示该列表*/
                deviceLayout[i].setVisibility(View.VISIBLE);
                TextView tv = (TextView) deviceLayout[i]
                              .findViewById(R.id.device_desc_text);
				tv.setText(device.name + "(" + device.devId + ")");
				
				for (j = 0; j < device.getCtrlNum(); j++) {
					
					if (device.controller[j].controllerType == kTankDevice.TANK_DEV_LED) {
						LinearLayout layout =  ctrlLayout[i][j];
						tv = (TextView) layout.findViewById(R.id.ctrl_led_name);
						tv.setText(device.getCtrlName(j));
						((SeekBar)layout.findViewById(R.id.ctrl_led_bar))
								.setOnSeekBarChangeListener(new seekBarChangeListen(layout));
						((EditText)layout.findViewById(R.id.ctrl_led_pwm_value))
								.addTextChangedListener(new pwmEditWatcher(layout));
						layout.setVisibility(View.VISIBLE);
						Button btn = (Button) layout.findViewById(R.id.ctrl_led_cfg_btn);
						btn.setOnClickListener(new CFG_BTN_LISTEN(
								deviceList[i], deviceList[i].controller[j]));
					} else if (device.controller[j].controllerType == kTankDevice.TANK_DEV_ONOFF) {
						LinearLayout layout =  ctrlLayout[i][j + 2];
						tv = (TextView)layout.findViewById(R.id.ctrl_led_name);
						tv.setText(device.getCtrlName(j));
						layout.setVisibility(View.VISIBLE);
						Button btn = (Button)layout.findViewById(R.id.ctrl_led_cfg_btn);
						btn.setOnClickListener(new CFG_BTN_LISTEN(
								deviceList[i], deviceList[i].controller[j]));
					}
				}
                break;
            }
        }
    }

    @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
                               Bundle savedInstanceState) {
        int i;
        frag_inflater = inflater;
        tankBaseLayout = inflater.inflate(R.layout.framgment_tank_layout, container,
                                          false);
        TextView tv = ((TextView) tankBaseLayout.findViewById(R.id.mycont));
        tv.setText("TANK" + (tankId + 1));

        LinearLayout dev_list_layout = (LinearLayout) tankBaseLayout
                                       .findViewById(R.id.TankListLayout);
        // create all the device/ctrl view
        for (i = 0; i < MAX_DEVICE_NUM; i++) {
            int j;
            deviceLayout[i] = (LinearLayout) inflater.inflate(
                R.layout.fragment_tank_device_layout, container, false);
            for (j = 0; j < 6; j++) {
            	if(j < 2){
            		ctrlLayout[i][j] = (LinearLayout) inflater.inflate(
            				R.layout.fragment_tank_led_cfg_layout, container, false);
            	}else{
            		ctrlLayout[i][j] = (LinearLayout) inflater.inflate(
            				R.layout.fragment_tank_onoff_cfg_layout, container, false);
            	}
                deviceLayout[i].addView(ctrlLayout[i][j]);
                ctrlLayout[i][j].setVisibility(View.GONE);
            }

            dev_list_layout.addView(deviceLayout[i]);
            deviceLayout[i].setVisibility(View.GONE);
        }

        for (i = 0; i < ((MainActivity) getActivity()).deviceNum; i++) {
            addDevice(((MainActivity) getActivity()).device[i]);
        }

        return tankBaseLayout;
    }

    @Override
      public void onDestroyView() {
        frag_inflater = null;
        super.onDestroyView();
        Log.d("Fragment 1", "onDestroyView");
    }

    @Override
      public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.tankId = tankId;
        System.out.println("ExampleFragment--onCreate");
    }

    @Override
      public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        System.out.println("ExampleFragment--onPause");
    }

    @Override
      public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("Fragment 1", "onAttach");
    }

    @Override
      public void onStart() {
        super.onStart();
        Log.d("Fragment 1", "onStart");
    }

    @Override
      public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        System.out.println("ExampleFragment--onResume");
    }

    @Override
      public void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        System.out.println("ExampleFragment--onStop");
    }

    @Override
      public void onDestroy() {
        super.onDestroy();
        Log.d("Fragment 1", "onDestroy");
    }

    @Override
      public void onDetach() {
        super.onDetach();
        Log.d("Fragment 1", "onDetach");
    }

    public void textUpdate(int cnt) {
        TextView myt = (TextView) this.getView().findViewById(R.id.mycont);
        myt.setText("contact" + cnt);
    }
}
