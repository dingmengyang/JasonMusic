package com.jikexueyuan.mediaplayerdemo;


import android.app.Activity;
import android.os.Bundle;

public class CloseActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.exit(0);
    }
}
