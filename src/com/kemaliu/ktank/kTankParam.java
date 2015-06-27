package com.kemaliu.ktank;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class kTankParam {
	private Context context;  
    //构造方法中传入上下文对象  
    public kTankParam(Context context) {  
        super();  
        this.context = context;  
    }  
    public Editor getEditor(){
    	SharedPreferences sharedPreferences = context.getSharedPreferences("kTankParam", Context.MODE_PRIVATE);  
        Editor editor = sharedPreferences.edit();
        return editor;
    }
    
    public Map<String,?> getPreferences(){  
        SharedPreferences sharedPreferences = context.getSharedPreferences("kTankParam", Context.MODE_PRIVATE);  
        return sharedPreferences.getAll();
    }  
    public SharedPreferences getHandle(){
    	SharedPreferences sharedPreferences = context.getSharedPreferences("kTankParam", Context.MODE_PRIVATE);
        return sharedPreferences;
    }
    public void save(){
    	SharedPreferences sharedPreferences = context.getSharedPreferences("kTankParam", Context.MODE_PRIVATE);  
        Editor editor = sharedPreferences.edit();
        editor.commit();
    }
    
}
