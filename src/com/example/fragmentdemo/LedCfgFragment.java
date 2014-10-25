package com.example.fragmentdemo;



import java.util.Arrays;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.text.TextWatcher;

public class LedCfgFragment extends Fragment {
	private LinearLayout[] ledrow = new LinearLayout[24];
	ScrollView pageScroll = null;
	private Button saveBtn;
	private Button cancelBtn;
	int[] original_pwm_hour = new int[24];
	int[] current_pwm_hour = new int[24];

	private int move;

	private class seekBarChangeListen implements
			SeekBar.OnSeekBarChangeListener {

		private LinearLayout parentLayout;
		private int hour;

		public seekBarChangeListen(LinearLayout ctrlLayout, int id) {
			parentLayout = ctrlLayout;
			hour = id;
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
						.findViewById(R.id.led_cfg_value);
				et.setText("" + progress);
				current_pwm_hour[hour] = progress;
				if(true == Arrays.equals(current_pwm_hour, original_pwm_hour)){
					saveBtn.setEnabled(false);
					cancelBtn.setEnabled(false);
				}else{
					saveBtn.setEnabled(true);
					cancelBtn.setEnabled(true);
				}
			}
		}
	}

	private class pwmEditWatcher implements TextWatcher {
		private LinearLayout parentLayout;

		public pwmEditWatcher(LinearLayout base_layout) {
			parentLayout = base_layout;
		}

		public void afterTextChanged(Editable s) {
			SeekBar bar = (SeekBar) parentLayout.findViewById(R.id.led_cfg_bar);
			if (s.length() < 1) {
				bar.setProgress(0);
				return;
			}

			if (move == 0) {
				String mystr = s.toString();
				int value = Integer.parseInt(mystr);
				int mod = 0;
				if (value < 0) {
					value = 0;
					mod = 1;
				}
				if (value > 255) {
					value = 255;
					mod = 1;
				} else {
				}
				bar.setProgress(value);
				if (mod != 0) {
					EditText et = ((EditText) parentLayout
							.findViewById(R.id.led_cfg_value));
					String str = "" + value;
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		int i, j;
		View baseLayout = inflater.inflate(R.layout.controller_cfg_layout,
				container, false);
		pageScroll = (ScrollView)baseLayout.findViewById(R.id.controll_cfg_scroll);
		LinearLayout base = (LinearLayout) baseLayout
				.findViewById(R.id.Controller_cfg_base_Layout);

		saveBtn = (Button) baseLayout
				.findViewById(R.id.controller_cfg_save_btn);
		saveBtn.setOnClickListener(new OnClickListener(){  
			  public void onClick(View arg0) {
			    System.arraycopy(current_pwm_hour, 0, ledController.led.hour, 0, 24);
			    MainActivity ma = ((MainActivity)getActivity());
			    byte [] buf = new byte[24];
			    int i;
			    for(i=0; i<24; i++){
			    	buf[i] = (byte)ledController.led.hour[i];
			    }
			    int devId = ledController.getCtrlId(ledDevice, ledController);
			    ma.bt.remoteInfomationSave(ledDevice.devId, devId, 
			    		KTANK_CMD.KFISH_CMD_SET_CTRL_CFG, buf, 24);
			    ma.setTabSelection(lastFragmentId);
			}
		});
		
		cancelBtn = (Button) baseLayout
				.findViewById(R.id.controller_cfg_cancel_btn);
		cancelBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
			    ((MainActivity)getActivity()).setTabSelection(lastFragmentId);
			}
		});
		
		for (i = 0; i < 24; i++) {
			ledrow[i] = (LinearLayout) inflater.inflate(R.layout.device_ctrl_led_cfg_row,
					container, false);
			base.addView(ledrow[i]);
			TextView tv = (TextView) ledrow[i]
					.findViewById(R.id.led_cfg_hour_text);
			//tv.setText(i + "点");
			EditText et = (EditText) ledrow[i].findViewById(R.id.led_cfg_value);
			//et.setText("" + current_pwm_hour[i]);
			et.addTextChangedListener(new pwmEditWatcher(ledrow[i]));
			SeekBar bar = (SeekBar) ledrow[i].findViewById(R.id.led_cfg_bar);
			//bar.setProgress(current_pwm_hour[i]);
			bar.setOnSeekBarChangeListener(new seekBarChangeListen(ledrow[i], i));

		}
		updateViewFromCfg();
		if(saveBtn != null)
		  saveBtn.setEnabled(false);
		if(pageScroll != null)
		  pageScroll.smoothScrollTo(0, pageScroll.getBaseline());
		return baseLayout;
	}
	
    @Override  
    public void onDestroyView() {
    	int  i;
    	saveBtn = null;
    	cancelBtn = null;
	pageScroll = null;
	for(i=0; i<24; i++)
	  ledrow [i] = null;
    	super.onDestroyView();
    	System.gc();
    }
  
	
	
	private void updateViewFromCfg(){
		int i;
		for (i = 0; i < 24; i++) {
			if(ledrow[i] == null)
				continue;
			TextView tv = (TextView) ledrow[i]
					.findViewById(R.id.led_cfg_hour_text);
			tv.setText(i + "点");
			EditText et = (EditText) ledrow[i].findViewById(R.id.led_cfg_value);
			et.setText("" + current_pwm_hour[i]);
			et.addTextChangedListener(new pwmEditWatcher(ledrow[i]));
			SeekBar bar = (SeekBar) ledrow[i].findViewById(R.id.led_cfg_bar);
			bar.setProgress(current_pwm_hour[i]);
			bar.setOnSeekBarChangeListener(new seekBarChangeListen(ledrow[i], i));

		}
		if(pageScroll != null)
		  pageScroll.smoothScrollTo(0, pageScroll.getBaseline());
		if(saveBtn != null)
			saveBtn.setEnabled(false);
	}
	private kTankDevice ledDevice = null;
	private kTankDevice.KTANKCTRL ledController = null;
	private int lastFragmentId; 
	public void updateLEDCfg(kTankDevice device, kTankDevice.KTANKCTRL ctrl, int fromFrag){
		int i;
		if(ctrl.controllerType != 0){
			return;
		}
		if(ctrl.led == null)
			return;
		ledDevice = device;
		ledController = ctrl;
		lastFragmentId = fromFrag;
		System.arraycopy(ctrl.led.hour, 0, original_pwm_hour, 0, 24);
		System.arraycopy(ctrl.led.hour, 0, current_pwm_hour, 0, 24);
		updateViewFromCfg();

	}
	
}
