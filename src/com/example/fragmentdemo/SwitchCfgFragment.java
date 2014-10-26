package com.example.fragmentdemo;

import java.util.Arrays;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SwitchCfgFragment extends Fragment {
	private kTankDevice switchDevice = null;
	LinearLayout[] switch_cfg_row = new LinearLayout[8];
	ToggleButton[] hourButton = new ToggleButton[24];
	byte[] switch_orginal_state = new byte[24];
	byte[] switch_current_state = new byte[24];

	private Button saveBtn;
	private Button cancelBtn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		int i, j;
		View baseLayout = inflater.inflate(R.layout.controller_cfg_layout,
				container, false);
		LinearLayout base = (LinearLayout) baseLayout
				.findViewById(R.id.Controller_cfg_base_Layout);

		saveBtn = (Button) baseLayout
				.findViewById(R.id.controller_cfg_save_btn);
		cancelBtn = (Button) baseLayout
				.findViewById(R.id.controller_cfg_cancel_btn);
		for (i = 0; i < 8; i++) {

			switch_cfg_row[i] = (LinearLayout) inflater.inflate(
					R.layout.device_ctrl_switch_cfg_row, container, false);
			base.addView(switch_cfg_row[i]);
			TextView tv;
			tv = (TextView) switch_cfg_row[i]
					.findViewById(R.id.swit_cfg_row_text0);
			tv.setText((i * 3 + 0) + ":00");
			tv = (TextView) switch_cfg_row[i]
					.findViewById(R.id.swit_cfg_row_text1);
			tv.setText((i * 3 + 1) + ":00");
			tv = (TextView) switch_cfg_row[i]
					.findViewById(R.id.swit_cfg_row_text2);
			tv.setText((i * 3 + 2) + ":00");

			hourButton[i * 3 + 0] = (ToggleButton) switch_cfg_row[i]
					.findViewById(R.id.swit_cfg_row_btn0);
			hourButton[i * 3 + 0]
					.setChecked((switch_orginal_state[i * 3 + 0] == 0) ? false
							: true);
			hourButton[i * 3 + 0]
					.setOnCheckedChangeListener(new SwitchChangeListener(
							i * 3 + 0));

			hourButton[i * 3 + 1] = (ToggleButton) switch_cfg_row[i]
					.findViewById(R.id.swit_cfg_row_btn1);
			hourButton[i * 3 + 1]
					.setChecked((switch_orginal_state[i * 3 + 1] == 0) ? false
							: true);
			hourButton[i * 3 + 1]
					.setOnCheckedChangeListener(new SwitchChangeListener(
							i * 3 + 1));

			hourButton[i * 3 + 2] = (ToggleButton) switch_cfg_row[i]
					.findViewById(R.id.swit_cfg_row_btn2);
			hourButton[i * 3 + 2]
					.setChecked((switch_orginal_state[i * 3 + 2] == 0) ? false
							: true);
			hourButton[i * 3 + 2]
					.setOnCheckedChangeListener(new SwitchChangeListener(
							i * 3 + 2));

		}
		updateViewFromCfg();
		saveBtn = (Button) baseLayout
				.findViewById(R.id.controller_cfg_save_btn);
		saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				System.arraycopy(switch_current_state, 0,
						onoffController.swi.hour, 0, 24);
				MainActivity ma = ((MainActivity) getActivity());
				byte[] buf = new byte[24];
				int i;
				for (i = 0; i < 24; i++) {
					buf[i] = (byte) onoffController.swi.hour[i];
				}
				int devId = onoffController.getCtrlId(onoffDevice, onoffController);
				ma.bt.remoteInfomationSave(onoffDevice.devId, devId,
						KTANK_CMD.KFISH_CMD_SET_CTRL_CFG, buf, 24);
				ma.setTabSelection(lastFragmentId);
			}
		});

		cancelBtn = (Button) baseLayout
				.findViewById(R.id.controller_cfg_cancel_btn);
		cancelBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				((MainActivity) getActivity()).setTabSelection(lastFragmentId);
			}
		});
		return baseLayout;
	}

	@Override
	public void onDestroyView() {
		int i;
		for (i = 0; i < 8; i++) {
			switch_cfg_row[i] = null;
		}
		for (i = 0; i < 24; i++)
			hourButton[i] = null;
		saveBtn = null;
		cancelBtn = null;
		super.onDestroyView();
		System.gc();
	}

	public void updateViewFromCfg() {
		int i;
		for (i = 0; i < 24; i++) {
			ToggleButton bt;
			if (switch_cfg_row[i / 3] == null)
				continue;
			switch (i % 3) {
			case 0:
				bt = (ToggleButton) switch_cfg_row[i / 3]
						.findViewById(R.id.swit_cfg_row_btn0);
				break;
			case 1:
				bt = (ToggleButton) switch_cfg_row[i / 3]
						.findViewById(R.id.swit_cfg_row_btn1);
				break;
			case 2:
			default:
				bt = (ToggleButton) switch_cfg_row[i / 3]
						.findViewById(R.id.swit_cfg_row_btn2);
			}
			bt.setChecked(switch_orginal_state[i] == 0 ? false : true);
		}
		if(saveBtn != null)
			saveBtn.setEnabled(false);
	}

	private kTankDevice onoffDevice = null;
	private kTankDevice.KTANKCTRL onoffController = null;
	private int lastFragmentId;

	public void updateSwitchCfg(kTankDevice device, kTankDevice.KTANKCTRL ctrl,
			int fromFrag) {
		int i;
		if (ctrl.controllerType != 1) {
			return;
		}
		onoffDevice = device;
		onoffController = ctrl;
		lastFragmentId = fromFrag;
		System.arraycopy(ctrl.swi.hour, 0, switch_orginal_state, 0, 24);
		System.arraycopy(ctrl.swi.hour, 0, switch_current_state, 0, 24);
		for (i = 0; i < 24; i++) {
			if (hourButton[i] == null)
				continue;
			hourButton[i].setChecked(switch_orginal_state[i] == 0 ? false : true);

		}
		
	}

	private class SwitchChangeListener implements OnCheckedChangeListener {

		private int switchId;

		public SwitchChangeListener(int id) {
			this.switchId = id;
		}

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked)
				switch_current_state[switchId] = 1;
			else
				switch_current_state[switchId] = 0;
			if (true == Arrays.equals(switch_current_state,
					switch_orginal_state)) {
				saveBtn.setEnabled(false);
				cancelBtn.setClickable(true);
			} else {
				saveBtn.setEnabled(true);
				cancelBtn.setClickable(true);
			}

		}

	}

}
