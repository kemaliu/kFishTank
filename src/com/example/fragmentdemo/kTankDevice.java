package com.example.fragmentdemo;

import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class kTankDevice {
	private int controllerNum;
	private int ctrlType;
	public String name;
	public int devId;
	public int tankId;
	public View tankLayout;

	public class KTANKCTRL {

		public View tankLayout;

		public class KTANKCTRL_LED {
			String name;
			int[] hour = new int[24];
			int temperature;
			int fanPwm;
		}

		public class KTANKCTRL_SWITCH {
			byte[] hour = new byte[24];
			int temperature;
			int fanPwm;
		}

		int controllerType;
		KTANKCTRL_SWITCH swi = null;
		KTANKCTRL_LED led = null;
		String name;

		public KTANKCTRL(byte[] info) {

			controllerType = info[0];
			if (controllerType == 0) { // LED
				led = new KTANKCTRL_LED();
				led.temperature = info[1];
				led.fanPwm = (info[2] >= 0) ? (int) info[2]
						: (int) info[2] + 256;
			} else {
				swi = new KTANKCTRL_SWITCH();
			}
		}

		void updateLocalCfg(byte[] info) {
			int i;
			if (controllerType == 0) { // LED
				for (i = 0; i < 24; i++) {
					if (info[i] >= 0) {
						led.hour[i] = info[i];
					} else {
						led.hour[i] = (int) info[i] + 256;
					}
				}
			} else { // switch
				for (i = 0; i < 24; i++) {
					swi.hour[i] = info[i];
				}
			}
		}

		void updateLocalCfg(int hour, int value) {

		}
		
		public int getCtrlId(kTankDevice dev, kTankDevice.KTANKCTRL ctrl){
			int i;
			for(i=0; i<dev.controllerNum; i++){
				if(ctrl == dev.controller[i])
					return i;
			}
			return -1;
		}
	}

	public KTANKCTRL[] controller;

	public kTankDevice(int controllerNum) {
		this.controllerNum = controllerNum;
		controller = new KTANKCTRL[controllerNum];
	}

	public int getCtrlNum() {
		return this.controllerNum;
	}

	public String getCtrlName(int id) {
		if (id >= this.controllerNum)
			return "overwhelming";
		return this.controller[id].name;
	}
}
