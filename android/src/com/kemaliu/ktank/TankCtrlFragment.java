package com.kemaliu.ktank;

import java.io.UnsupportedEncodingException;

import com.example.fragmentdemo.R;
import com.kemaliu.ktank.kTankDevice.KTANKCTRL;



import android.app.Fragment;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

public class TankCtrlFragment extends Fragment {
    /** 该页面代表的鱼缸的ID*/
    private int tankId;

    /** 主layout*/
    private View tankBaseLayout;
    /**页面中允许的最多设备数量*/
    final int MAX_DEVICE_NUM = 10;
    final int MAX_CTRL_NUM_OF_DEVICE = 10;

    /**每一个设备在该页面中有一个自己的layout用于显示设备名称/ID*/
    private LinearLayout[] deviceLayout = new LinearLayout[MAX_DEVICE_NUM];
    /**与deviceLayout对应的设备句柄*/
    public kTankDevice[] deviceList = new kTankDevice[MAX_DEVICE_NUM];
    private float status_temperature;
    private int status_pwm;
    /**
     * tank页面中， 每一个设备下面有2个LED控制器或4个开关控制器，共10个 0-1给LED用，2-9给开关用 
     ＊6个设备都是预先创建好 */
    private LinearLayout[][] ctrlLayout = new LinearLayout[MAX_DEVICE_NUM][MAX_CTRL_NUM_OF_DEVICE];
    /**与ctrlLayout对应的控制器句柄*/
    kTankDevice.KTANKCTRL[][] ctrlLists = new kTankDevice.KTANKCTRL[MAX_DEVICE_NUM][MAX_CTRL_NUM_OF_DEVICE];
    int [][] pause_value_list = new int[MAX_DEVICE_NUM][MAX_CTRL_NUM_OF_DEVICE];
    
    tankButtonClickListener tankBtnClickListen = new tankButtonClickListener();
    /** tank页面按键监听 */
    private class tankButtonClickListener implements OnClickListener {
        @Override
          public void onClick(View v) {
            switch (v.getId()) {
              case R.id.tankPauseBtn://暂停按钮
            	  if(pause_resume_btn_state == 0){
            		  /*暂停*/
            		  int i, j;
            		  byte [] buf = new byte[24];
            		  int pauseMin = Integer.parseInt(((EditText)tankBaseLayout.findViewById(R.id.tank_pause_min)).getText().toString());

                	  for(i=0; i<MAX_DEVICE_NUM; i++){
                		  kTankDevice device = deviceList[i];
                		  kTankDevice.KTANKCTRL ctrl;
                		  if(device == null)
                			  continue;
                		  
                		  for(j=0; j<24; j++)
                			  buf[j] = 0;
                		  
                		  buf[0] = (byte)pauseMin;
                		  //获取配置的暂停的数值
                		  for(j=0; j<MAX_CTRL_NUM_OF_DEVICE; j++){
                			  if(ctrlLists[i][j] == null)
                				  continue;
                			  ctrl = ctrlLists[i][j];
                			  if(ctrl.controllerType == kTankDevice.TANK_DEV_LED){
                				  buf[1 + j] = (byte)pause_value_list[i][j]; 
                			  }else if(ctrl.controllerType == kTankDevice.TANK_DEV_ONOFF){
                				  buf[1 + j - 2] = (byte)pause_value_list[i][j]; 
                			  }else{
                				  
                			  }
                		  }
                		  if(device.controller[0].controllerType == kTankDevice.TANK_DEV_LED)
                			  continue;
                		  for(j=0; j<3; j++){
                			  if(24 == ((MainActivity) getActivity()).ctrl_setting_save(device.devId, 
                				  0, KTANK_CMD.KFISH_CMD_DEV_PAUSE, buf, 24)){
                				  
                				  break;
                			  }
                		  }
                		  if(j >= 3){
                			  //失败
                			  pause_resume_btn.setText("暂停(上次失败,重试)");
                		  }else{
                			  pause_resume_btn_state = 1;
                    		  pause_resume_btn.setText("恢复");
                		  }
                	  }
            	  }else{
            		  pause_resume_btn_state = 0;
            		  pause_resume_btn.setText("暂停");
            		  byte [] buf = new byte[24];
            		  int i, j;
            		  int pauseMin = Integer.parseInt(((EditText)tankBaseLayout.findViewById(R.id.tank_pause_min)).getText().toString());
                	  for(i=0; i<MAX_DEVICE_NUM; i++){
                		  kTankDevice device = deviceList[i];
                		  kTankDevice.KTANKCTRL ctrl;
                		  if(device == null)
                			  continue;
                		  for(j=0; j<24; j++)
                			  buf[j] = 0;
                		  if(device.controller[0].controllerType == kTankDevice.TANK_DEV_LED)
                			  continue;
                		  for(j=0; j<3; j++){
                			  if(24 == ((MainActivity) getActivity()).bluetooth_controller.remoteInfomationSave(device.devId, 
                				  0, KTANK_CMD.KFISH_CMD_DEV_PAUSE, buf, 24))
                				  break;
                		  }
                		  if(j >= 3){
                			  //失败
                			  pause_resume_btn.setText("恢复(上次失败,重试)");
                		  }else{
                			  pause_resume_btn_state = 0;
                    		  pause_resume_btn.setText("暂停");
                		  }

                	  }
            	  }
            	  
                break;
              default:
                if (renameView == null) {
                    Toast.makeText(((MainActivity) getActivity()),
                                   "illegal button, renameView is null",
                                   Toast.LENGTH_SHORT).show();
                    break;
                }

                if (v.getId() == R.id.dev_rename_ok_btn) {
                    EditText et;
                    String newName = ((EditText) renameView
                                      .findViewById(R.id.dev_rename_edit)).getText()
                                     .toString();
                    // update deviceList
                    kTankDevice.KTANKCTRL ctrl = ctrlLists[popupDevId][popupCtrlId];
                    kTankDevice device = deviceList[popupDevId];
                    ctrl.name = newName;

                    ((TextView) ctrlLayout[popupDevId][popupCtrlId]
                     .findViewById(R.id.ctrl_name))
                      .setText(newName);

                    byte[] buf = new byte[24];
                    byte[] namebuf;
                    //kTankDevice device = ((MainActivity) getActivity()).device[popupDevId];
                    
                    try {
                        int len;
                        namebuf = ctrl.name.getBytes("gb2312");
                        // 获取namebuf字符串长度
                        for (len = 0; len < 24; len++) {
                            if (buf[len] == 0)
                              break;
                        }
                        System.arraycopy(namebuf, 0, buf, 0, namebuf.length>23?23:namebuf.length);
                        ((MainActivity) getActivity()).bluetooth_controller.remoteInfomationSave(
                            device.devId, ctrl.getCtrlId(device, ctrl),
                            KTANK_CMD.KFISH_CMD_SET_CTRL_NAME, buf, 24);
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    popupWindow.dismiss();
                } else if (v.getId() == R.id.dev_rename_cancel_btn) {
                    popupWindow.dismiss();
                } else {
                    Toast.makeText(((MainActivity) getActivity()),
                                   "illegal button", Toast.LENGTH_SHORT).show();
                }

                break;
            }
        }
    }

    private int move = 0;

    private class CFG_BTN_LISTEN implements OnClickListener {
        private kTankDevice.KTANKCTRL controller = null;
        private kTankDevice device = null;

        /**
         * 监听配置页面中的按键事件
         * 
         * @param device
         *            设备句柄
         * @param ctrl
         *            控制器句柄
         * */
        public CFG_BTN_LISTEN(kTankDevice device, kTankDevice.KTANKCTRL ctrl) {
            controller = ctrl;
            this.device = device;
        }

        @Override
          public void onClick(View arg0) {
            // TODO Auto-generated method stub
            if (controller == null) {
                return;
            }
            switch(arg0.getId()){
            	
            	case R.id.ctrl_led_status:
            	{ /* 更新状态*/
            		int retry;
            		int ctrlId;
            		byte[] info = new byte[24];
            		for(ctrlId=0; ctrlId< MAX_CTRL_NUM_OF_DEVICE; ctrlId++){
            			if(device.controller[ctrlId] == controller)
            				break;
            		}
            	    if(ctrlId >= MAX_CTRL_NUM_OF_DEVICE){
            	    	return;            	    	
            	    }
            		for (retry = 0; retry < 3; retry++) {
        				if (24 == ((MainActivity) getActivity()).bluetooth_controller.remoteInfomationRequest(device.devId, ctrlId,
        						KTANK_CMD.KFISH_CMD_GET_CTRL_STATUS, info, 24)) {
					    controller.updateLocalStatus(info);
					    /*TextView status_text = (TextView)arg0.findViewById(R.id.ctrl_led_status);
					    status_text.setText("状态： 温度"+controller.led.temperature + ", 亮度" + 
					    		controller.led.pwm_now + ", 风扇"+controller.led.fanPwm);*/
					    break;
        				}
        			}
            	}
	            break;
            	case R.id.ctrl_onoff_status:
            	{ /* 更新状态*/
                      		int retry;
                      		int ctrlId;
                      		byte[] info = new byte[24];
                      		for(ctrlId=0; ctrlId< MAX_CTRL_NUM_OF_DEVICE; ctrlId++){
                      			if(device.controller[ctrlId] == controller)
                      				break;
                      		}
                      	    if(ctrlId >= MAX_CTRL_NUM_OF_DEVICE){
                      	    	return;            	    	
                      	    }
                      		for (retry = 0; retry < 3; retry++) {
                  				if (24 == ((MainActivity) getActivity()).bluetooth_controller.remoteInfomationRequest(device.devId, ctrlId,
                  						KTANK_CMD.KFISH_CMD_GET_CTRL_STATUS, info, 24)) {
          					    controller.updateLocalStatus(info);
          					    /*TextView status_text = (TextView)arg0.findViewById(R.id.ctrl_led_status);
          					    status_text.setText("状态： 温度"+controller.led.temperature + ", 亮度" + 
          					    		controller.led.pwm_now + ", 风扇"+controller.led.fanPwm);*/
          					    break;
                  				}
                  			}
            	}
          	         break;
            case R.id.ctrl_onoff_btn:
            	//this btn for switch row only
            	int i = 0, j = 0, found = 0;
            	for(i=0; i<MAX_DEVICE_NUM; i++){
            		for(j=0; j<MAX_CTRL_NUM_OF_DEVICE; j++){
            			if(ctrlLists[i][j] == null)
            				continue;
            			if(ctrlLists[i][j] == controller){
            				found = 1;
            				break;
            			}
            		}
            		if(found != 0)
            			break;
            	}
            	
            	if(found == 0){
            		return;
            	}
            	pause_value_list[i][j] =  ((ToggleButton)arg0).isChecked() == true?1:0;
            	break;
            }
        }

    }

    /**设置该页面对应的鱼缸ID*/
    public void setTankId(int tankId) {
        this.tankId = tankId;
    }
    /**
     * 向该页面添加设备
     * @param device 添加的设备的句柄
     */
    public void addDevice(kTankDevice device) {
        int i, j;
        // make sure the device has not been added
        for (i = 0; i < MAX_DEVICE_NUM; i++) {
            if (deviceList[i] != null && deviceList[i].devId == device.devId) {
                return;
            }
        }
        /* 在tank页面中找一个没用的设备列表 */
        for (i = 0; i < MAX_DEVICE_NUM; i++) {
            if (deviceList[i] == null) {
                deviceList[i] = device;
                if (device.tankId != this.tankId)
                  return;
                /* 在页面中显示该列表 */
                deviceLayout[i].setVisibility(View.VISIBLE);
                 
                TextView tv = (TextView) deviceLayout[i]
                              .findViewById(R.id.device_desc_text);
                
                tv.setText(device.name + "(" + device.devId + ")");

                for (j = 0; j < device.getCtrlNum(); j++) {
		    CTRL_LONGCLICK_LISTEN longClickListen = new CTRL_LONGCLICK_LISTEN(
			deviceList[i], deviceList[i].controller[j]);
		    CFG_BTN_LISTEN clickListen = new CFG_BTN_LISTEN(
			deviceList[i], deviceList[i].controller[j]);
                    if (device.controller[j].controllerType == kTankDevice.TANK_DEV_LED) {
                        LinearLayout layout = ctrlLayout[i][j];
                        device.controller[j].ctrl_layout = layout;
                        ctrlLists[i][j] = device.controller[j];
                        tv = (TextView) layout.findViewById(R.id.ctrl_name);
                        tv.setText(device.getCtrlName(j));
                        tv.setOnLongClickListener(longClickListen);
                        TextView status_text;
                        status_text = (TextView)layout.findViewById(R.id.ctrl_led_status);
                        status_text.setText("状态： 温度"+deviceList[i].controller[j].led.temperature + ", 亮度" + 
					    deviceList[i].controller[j].led.pwm_now + ", 风扇"+deviceList[i].controller[j].led.fanPwm);
                        status_text.setClickable(true);
                        status_text.setLongClickable(true);
                        status_text.setOnLongClickListener(longClickListen);
                        status_text.setOnClickListener(clickListen);
                        layout.setVisibility(View.VISIBLE);                        
                    } else/* if (device.controller[j].controllerType == kTankDevice.TANK_DEV_ONOFF) */{
                        LinearLayout layout = ctrlLayout[i][j + 2];
                        ctrlLists[i][j + 2] = device.controller[j];
                        device.controller[j].ctrl_layout = layout;
                        tv = (TextView) layout.findViewById(R.id.ctrl_name);
                        tv.setText(device.getCtrlName(j));
                        tv.setOnLongClickListener(longClickListen);
                        
                        /*Button btn = (Button) layout
                                     .findViewById(R.id.ctrl_led_cfg_btn);
                        CFG_BTN_LISTEN listen = new CFG_BTN_LISTEN(deviceList[i], deviceList[i].controller[j]);
                        btn.setOnClickListener(listen);*/
                        ToggleButton t_btn = (ToggleButton) layout
                                .findViewById(R.id.ctrl_onoff_btn);
                        t_btn.setChecked(device.controller[j].swi.on_now==0?false:true);
			pause_value_list[i][j+2] = device.controller[j].swi.on_now;
			
			t_btn.setOnClickListener(clickListen);
                        TextView status_text;
                        status_text = (TextView)layout.findViewById(R.id.ctrl_onoff_status);
                        if(deviceList[i].controller[j].swi.on_now > 0)
                        	status_text.setText("状态： 开");
                        else
                        	status_text.setText("状态： 关");
                        status_text.setClickable(true);
                        status_text.setLongClickable(true);
                        status_text.setOnLongClickListener(longClickListen);
			status_text.setOnClickListener(clickListen);
			// t_btn.setOnLongClickListener(longClickListen);
			// t_btn.setOnClickListener(new CFG_BTN_LISTEN(
			// 			     deviceList[i], deviceList[i].controller[j]));
                        layout.setVisibility(View.VISIBLE);
                    	}
                }
                break;
            }
        }
    }

    private View renameView;
    private PopupWindow popupWindow;
    /** 弹出窗口的设备ID */
    private int popupDevId;
    /** 弹出窗口的controller ID */
    private int popupCtrlId;

    private class CTRL_LONGCLICK_LISTEN implements OnLongClickListener {
        private kTankDevice.KTANKCTRL controller = null;
        private kTankDevice device = null;

	public CTRL_LONGCLICK_LISTEN(kTankDevice device, kTankDevice.KTANKCTRL ctrl) {
            controller = ctrl;
            this.device = device;
        }
        @Override
          public boolean onLongClick(View v) {

	    if(v.getId() == R.id.ctrl_led_status || v.getId() == R.id.ctrl_onoff_status){
			if (controller.controllerType == 0) {// led
			    ((MainActivity) getActivity()).setTabSelection(4);
			    //根据老的配置信息更新界面的显示信息
			    ((MainActivity) getActivity()).LEDCfgFragment.updateLEDCfg(
				device, controller, tankId);
			} else {
			    ((MainActivity) getActivity()).setTabSelection(5);
			    ((MainActivity) getActivity()).SwitchCfgFragment
			      .updateSwitchCfg(device, controller, tankId);
			}
			return true;
	    }
	    
            int i, j;
            int foundId, foundJd;
            foundId = -1;
            foundJd = -1;
            for (i = 0; i < MAX_DEVICE_NUM; i++)
              for (j = 0; j < MAX_CTRL_NUM_OF_DEVICE; j++) {
                  if (v.getParent() == ctrlLayout[i][j]) {
                      foundId = i;
                      foundJd = j;
                      break;
                  }

              }
            if (foundId < 0) {
                Toast.makeText(((MainActivity) getActivity()),
                               "illegal device", Toast.LENGTH_SHORT).show();
            } else {
                if (popupWindow == null) {
                    popupWindow = new PopupWindow(renameView, 400, 350);
                }
                popupDevId = foundId;
                popupCtrlId = foundJd;
                // 使其聚集
                popupWindow.setFocusable(true);
                // 设置允许在外点击消失
                popupWindow.setOutsideTouchable(true);
                kTankDevice device = deviceList[foundId];
                kTankDevice.KTANKCTRL ctrl = ctrlLists[foundId][foundJd];
                TextView tv = (TextView) renameView
                              .findViewById(R.id.dev_rename_text);
                tv.setTextColor(Color.BLUE);
                tv.setText("输入控制器<" + ctrl.name + ">的新名称\n最大11个中文字符/23英文字符)");
                ((EditText) renameView.findViewById(R.id.dev_rename_edit))
                  .setText(ctrl.name);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
            }
            return true;
        }
    }
    
    private class ledConfigListen implements OnLongClickListener {
        @Override
          public boolean onLongClick(View v) {
            int i, j;
            int foundId, foundJd;
            foundId = -1;
            foundJd = -1;
            for (i = 0; i < MAX_DEVICE_NUM; i++)
              for (j = 0; j < MAX_CTRL_NUM_OF_DEVICE; j++) {
                  if (v.getParent() == ctrlLayout[i][j]) {
                      foundId = i;
                      foundJd = j;
                      break;
                  }

              }
            if (foundId < 0) {
                Toast.makeText(((MainActivity) getActivity()),
                               "illegal device", Toast.LENGTH_SHORT).show();
            } else {
                if (popupWindow == null) {
                    popupWindow = new PopupWindow(renameView, 400, 350);
                }
                popupDevId = foundId;
                popupCtrlId = foundJd;
                // 使其聚集
                popupWindow.setFocusable(true);
                // 设置允许在外点击消失
                popupWindow.setOutsideTouchable(true);
                kTankDevice device = deviceList[foundId];
                kTankDevice.KTANKCTRL ctrl = ctrlLists[foundId][foundJd];
                TextView tv = (TextView) renameView
                              .findViewById(R.id.dev_rename_text);
                tv.setTextColor(Color.BLUE);
                tv.setText("输入控制器<" + ctrl.name + ">的新名称\n最大11个中文字符/23英文字符)");
                ((EditText) renameView.findViewById(R.id.dev_rename_edit))
                  .setText(ctrl.name);
                popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
            }
            return true;
        }
    }
    
    
    /**暂停/恢复按键状态  0:暂停  1:恢复*/
    private int pause_resume_btn_state;
    private Button pause_resume_btn;
    private EditText pause_min_et;
    @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
                               Bundle savedInstanceState) {
        int i;
        tankBaseLayout = inflater.inflate(R.layout.framgment_tank_layout,
                                          container, false);
        TextView tv = ((TextView) tankBaseLayout.findViewById(R.id.mycont));
        tv.setText("TANK" + (tankId + 1));

        LinearLayout dev_list_layout = (LinearLayout) tankBaseLayout
                                       .findViewById(R.id.TankListLayout);
        // create all the device/ctrl view
        for (i = 0; i < MAX_DEVICE_NUM; i++) {
            int j;
            deviceLayout[i] = (LinearLayout) inflater.inflate(
                R.layout.fragment_tank_device_layout, container, false);
            for (j = 0; j < MAX_CTRL_NUM_OF_DEVICE; j++) {
                if (j < 2) {
                    ctrlLayout[i][j] = (LinearLayout) inflater.inflate(
                        R.layout.fragment_tank_led_cfg_layout, container,
                        false);
                } else {
                    ctrlLayout[i][j] = (LinearLayout) inflater.inflate(
                        R.layout.fragment_tank_onoff_cfg_layout, container,
                        false);
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
        renameView = inflater.inflate(R.layout.subwindow_rename_layout,
                                      container, false);
        ((Button)renameView.findViewById(R.id.dev_rename_ok_btn)).setOnClickListener(tankBtnClickListen);
        ((Button)renameView.findViewById(R.id.dev_rename_cancel_btn)).setOnClickListener(tankBtnClickListen);
        pause_resume_btn = (Button)tankBaseLayout.findViewById(R.id.tankPauseBtn);
        pause_resume_btn.setText("暂停");
        pause_resume_btn_state = 0;
        pause_resume_btn.setOnClickListener(tankBtnClickListen);
        pause_min_et = (EditText)tankBaseLayout.findViewById(R.id.tank_pause_min);
        pause_min_et.setText("15");
        return tankBaseLayout;
    }

    @Override
      public void onDestroyView() {
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


}
