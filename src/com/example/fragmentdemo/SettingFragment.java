package com.example.fragmentdemo;

import android.app.Fragment;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;          
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;  
import java.util.List;  
import java.util.Map;
import java.util.StringTokenizer;





public class SettingFragment extends Fragment {
	private byte remote_dev_hour;
	private byte remote_dev_min;
	private byte remote_dev_second;
	private EditText time_edittext;

	public void dev_time_update(byte hour, byte min, byte sec){
		remote_dev_hour = (byte)hour;
		remote_dev_min = (byte)min;
		remote_dev_second = sec;
		if(time_edittext == null)
			return;
        time_edittext.setText(remote_dev_hour + ":" + remote_dev_min + ":" + remote_dev_second);
	}
	
    /** click listen for save/restore button*/
    private class SettingButtonClickListener implements OnClickListener {
        @Override  
          public void onClick(View v) {
        	byte [] buf = new byte[24];
        	int retry;
            switch (v.getId()){
              case R.id.setting_save_btn:
                saveSetting(((MainActivity)getActivity()).__params.getEditor());
                ((MainActivity)getActivity()).__params.save();
                break;
              case R.id.setting_restore_btn:
                break;
                
              case R.id.time_set_btn:
            	  
                  String timeStr = time_edittext.getText().toString();
                  StringTokenizer st = new StringTokenizer  (timeStr , ":") ; 
                  buf[0] = 13;
                  buf[1] = 8;
                  buf[2] = 7;
                  buf[3] = 0;
                  if(st.hasMoreTokens())
                    buf[4] = (byte)(Integer.parseInt(st.nextToken()) % 24);
                  else{
                      Toast.makeText(getActivity(), "time " + timeStr + " format error",1000).show();
                      return;
                  }
                  if(st.hasMoreTokens()){
                	  buf[5] = (byte)Integer.parseInt(st.nextToken());
                  }else{
                      Toast.makeText(getActivity(), "time " + timeStr + " format error",1000).show();
                      return;
                  }
                  if(st.hasMoreTokens()){
                	  buf[6] = (byte)Integer.parseInt(st.nextToken());
                  }else{
                      Toast.makeText(getActivity(), "time " + timeStr + " format error",1000).show();
                      return;
                  }
                  
                  for(retry = 0; retry < 3; retry++){
                  if(24 == ((MainActivity)getActivity()).bt.remoteInfomationSave(0xff, 0,
          				KTANK_CMD.KFISH_CMD_SET_DEV_TIME, buf, 24)){
                  	
                  	break;
                  }
                  }
                  
            	  break;
              case R.id.time_get_btn:
                  
                  for(retry = 0; retry < 3; retry++){
                  if(24 == ((MainActivity)getActivity()).bt.remoteInfomationRequest(0xff, 0,
          				KTANK_CMD.KFISH_CMD_GET_DEV_TIME, buf, 24)){
                  	dev_time_update(buf[4], buf[5], buf[6]);
                  	break;
                  }
                  }
            	  break;
              default:
                if(renameView == null){
                    Toast.makeText(((MainActivity)getActivity()), "illegal button, renameView is null", Toast.LENGTH_SHORT).show();
                    break;
                }
                          
                if(v.getId() == R.id.dev_rename_ok_btn){
                    EditText et;
                    ((MainActivity)getActivity()).device[popupDevId].name = ((EditText)renameView.findViewById(R.id.dev_rename_edit)).getText().toString();
                    String newName = ((EditText)renameView.findViewById(R.id.dev_rename_edit)).getText().toString();
                    ((TextView)deviceRow[popupDevId].findViewById(R.id.setting_deviceName)).setText(newName);
                    
                    
                    
                    byte [] namebuf;
                    kTankDevice device = ((MainActivity)getActivity()).device[popupDevId];
                    device.name = newName;
                    try {
                    	int len;
                        namebuf = device.name.getBytes("gb2312");
                        // 获取namebuf字符串长度
                        for (len = 0; len < 24; len++) {
                            if (buf[len] == 0)
                              break;
                        }
                        System.arraycopy(namebuf, 0, buf, 0, namebuf.length>23?23:namebuf.length);
    					((MainActivity)getActivity()).bt.remoteInfomationSave(device.devId, 0,
    							KTANK_CMD.KFISH_CMD_SET_DEVICE_NAME, buf, 24);
    				} catch (UnsupportedEncodingException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
                    
                    popupWindow.dismiss();
                }else if(v.getId() == R.id.dev_rename_cancel_btn){
                    popupWindow.dismiss();
                }else{
                    Toast.makeText(((MainActivity)getActivity()), "illegal button", Toast.LENGTH_SHORT).show();
                }
                  
                break;
            }
        }
    }

    
    private class TankNumSelectLIsten implements OnItemSelectedListener {
        //@Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            String str=parent.getItemAtPosition(position).toString();
            tankNumber = Integer.parseInt(str);
            int  i;
            tankLists = new ArrayList<String>();
                        
            tankListAdapter.clear();
            for(i=1; i<=tankNumber; i++)
              tankListAdapter.add("Tank"+ i);
        }

        @Override
          public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
                                
        }
    }
    
    private class TankBelongToSelectedListener implements OnItemSelectedListener {
        //@Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            int  i;
            for(i=0; i<24; i++){
                if(deviceRow[i] == view.getParent().getParent()){
                    ((MainActivity)getActivity()).device[i].tankId = position;
                }
            }
        }

        @Override
          public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
                        
        }
    }
    private PopupWindow popupWindow;
    /** 弹出窗口的设备ID*/
    private int popupDevId;
    private class DevRenameListen implements OnLongClickListener{
        @Override  
          public boolean onLongClick(View v) {
            int i;
            int foundId;
            foundId = -1;
            for(i=0; i<deviceRow.length; i++){
                if(v.getParent() == deviceRow[i]){
                    foundId = i;
                    break;
                }
            }
            if(foundId < 0){
                Toast.makeText(((MainActivity)getActivity()), "illegal device", Toast.LENGTH_SHORT).show(); 
            }else{
                if(popupWindow == null){
                    popupWindow = new PopupWindow(renameView, 400, 350);
                }
                popupDevId = foundId;
                // 使其聚集
                popupWindow.setFocusable(true);
                // 设置允许在外点击消失
                popupWindow.setOutsideTouchable(true);
                kTankDevice device = ((MainActivity)getActivity()).device[popupDevId];
                TextView tv = (TextView)renameView.findViewById(R.id.dev_rename_text);
                tv.setTextColor(Color.BLUE);
                tv.setText("输入设备<"+ device.name + ">id" +device.devId+"的新名称\n最大11个中文字符/23英文字符)");
                ((EditText)renameView.findViewById(R.id.dev_rename_edit)).setText(device.name);
                popupWindow.showAtLocation(v,  Gravity.CENTER, 0, 0);
            }
            return true;
        }
    }
    DevRenameListen devRenameListen = new DevRenameListen();
    
    TankBelongToSelectedListener tankSelectListen = new TankBelongToSelectedListener();
    
    TankNumSelectLIsten tankNumSelectListen = new TankNumSelectLIsten();

    SettingButtonClickListener settingBtnOnClkLIsten = new SettingButtonClickListener();

    private View [] deviceRow = new View[32];
    
    private List<String> tankLists = new ArrayList<String>();
        
    private ArrayAdapter<String> tankListAdapter;
        
    private List<String> tankNumList = new ArrayList<String>();

    private ArrayAdapter<String> tankNumAdapter;
    
    private int tankNumber;
    
    private View renameView;
    @Override  
      public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub  
        super.onCreate(savedInstanceState);
        tankNumber = ((MainActivity)this.getActivity()).tankNumber;
      
        int i;
        for(i=0; i<tankNumber; i++){
            tankLists.add("Tank"+(i+1));
        }
        this.tankListAdapter = new ArrayAdapter<String>(this.getActivity(),android.R.layout.simple_spinner_item, tankLists);
        this.tankListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      
        tankNumList.add("1");
        tankNumList.add("2");
        tankNumList.add("3");
        this.tankNumAdapter = new ArrayAdapter<String>(this.getActivity(),android.R.layout.simple_spinner_item, 
                                                       tankNumList);
        this.tankNumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      
    }  

    
    @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
                               Bundle savedInstanceState) {
        View settingLayout = inflater.inflate(R.layout.fragment_setting_layout,
                                              container, false);
                
                
        Button bt = (Button) settingLayout.findViewById(R.id.setting_save_btn);
        bt.setOnClickListener(settingBtnOnClkLIsten);
        bt = (Button) settingLayout.findViewById(R.id.setting_restore_btn);
        bt.setOnClickListener(settingBtnOnClkLIsten);
                
                
        settingLayout.setScrollbarFadingEnabled(false);
        Spinner num_sp = (Spinner)settingLayout.findViewById(R.id.tank_num_spinner);
        num_sp.setAdapter(tankNumAdapter);

        num_sp.setSelection(tankNumber-1);
                
                
        num_sp.setOnItemSelectedListener(tankNumSelectListen);
                
        LinearLayout layout = (LinearLayout)settingLayout.findViewById(R.id.SettingTopLayout);
        int i;
                
                
        for(i=0; i<deviceRow.length; i++){
                        
            deviceRow[i] = inflater.inflate(R.layout.device_setting_layout, container, false);
            Spinner sp; // devTankSelSp[i]
            TextView tv;
            sp = (Spinner)deviceRow[i].findViewById(R.id.dev_belongto_spinner);
            sp.setAdapter(tankListAdapter);
            sp.setPrompt(tankLists.get(0));
            sp.setOnItemSelectedListener(tankSelectListen);
            
            tv = (TextView)deviceRow[i].findViewById(R.id.setting_deviceName);
            tv.setOnLongClickListener(devRenameListen);
                        
            layout.addView(deviceRow[i]);
            if (((MainActivity)this.getActivity()).device[i] == null)
              deviceRow[i].setVisibility(View.GONE);
            else{
                TextView text = (TextView)deviceRow[i].findViewById(R.id.setting_deviceName);
                text.setText(((MainActivity)this.getActivity()).device[i].name);
                deviceRow[i].setVisibility(View.VISIBLE);
                sp.setSelection(((MainActivity)this.getActivity()).device[i].tankId);
            }
        }
        //deviceListUpdate(((MainActivity)this.getActivity()).deviceNum, ((MainActivity)this.getActivity()).device);
        renameView = inflater.inflate(R.layout.subwindow_rename_layout, container, false);
        ((Button)renameView.findViewById(R.id.dev_rename_ok_btn)).setOnClickListener(settingBtnOnClkLIsten);
        ((Button)renameView.findViewById(R.id.dev_rename_cancel_btn)).setOnClickListener(settingBtnOnClkLIsten);
        time_edittext = (EditText)settingLayout.findViewById(R.id.time_edit_text);
        byte [] buf = new byte[24];
        int retry;
        for(retry = 0; retry < 3; retry++){
        if(24 == ((MainActivity)getActivity()).bt.remoteInfomationRequest(0xff, 0,
				KTANK_CMD.KFISH_CMD_GET_DEV_TIME, buf, 24)){
        	dev_time_update(buf[4], buf[5], buf[6]);
        	break;
        }
        }
        Button btn = (Button)settingLayout.findViewById(R.id.time_get_btn);
        btn.setOnClickListener(settingBtnOnClkLIsten);
        btn = (Button)settingLayout.findViewById(R.id.time_set_btn);
        btn.setOnClickListener(settingBtnOnClkLIsten);
        return settingLayout;
    }
        
        
    public void saveSetting(Editor edit){
        int i;
        edit.clear();
        edit.putInt("tank_number", tankNumber);
        ((MainActivity)this.getActivity()).saveSetting();
        
    }
        
    public void loadSetting(){
        Object tObj = ((MainActivity)this.getActivity()).__param_map.get("tank_number");
        if(tObj == null){
            /* no configuration*/
            tankNumber = 1;
        }else{
            String tStr = tObj.toString();
            tankNumber = Integer.parseInt(tStr);
        }
        int i;
        for(i=0; i<((MainActivity)this.getActivity()).deviceNum; i++){
            tObj = ((MainActivity)this.getActivity()).__param_map.get(""+i);
            ((MainActivity)this.getActivity()).device[i].tankId = 0;
            if(tObj != null && 0 == tObj.toString().compareTo("y")){
                tObj = ((MainActivity)this.getActivity()).__param_map.get("devId"+i);
                if(tObj != null){
                    ((MainActivity)this.getActivity()).device[i].tankId = Integer.parseInt(tObj.toString());
                }
                                
            }
                        
        }
        
    }
        
}
