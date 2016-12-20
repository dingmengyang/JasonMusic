package com.jikexueyuan.mediaplayerdemo;


import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Properties;

public class PropertyBean {
    public static  String[] THEMES;
    private static String DEFAULT_THEME;
    private Context context;

    private String theme;
    public PropertyBean(Context context){
        this.context=context;
        THEMES=context.getResources().getStringArray(R.array.theme);
        DEFAULT_THEME=THEMES[0];
        loadTheme();
    }

    public String getTheme() {
        return theme;
    }

    public void setAndSaveTheme(String theme){
        this.theme=theme;
        saveTheme(theme);
    }

    private void loadTheme(){
        Properties properties=new Properties();
        try {
            FileInputStream fis=context.openFileInput("configuration.cfg");
            properties.load(fis);
            theme= properties.getProperty("theme");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean saveTheme(String theme){
        Properties properties=new Properties();
        properties.put("theme",theme);
        try {
            FileOutputStream fos=context.openFileOutput("configuration.cfg",Context.MODE_PRIVATE);
            properties.store(fos,"");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
