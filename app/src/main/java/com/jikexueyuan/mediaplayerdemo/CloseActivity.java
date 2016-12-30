package com.jikexueyuan.mediaplayerdemo;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class CloseActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent=new Intent(CloseActivity.this,MusicService.class);
        stopService(intent);
        finish();
    }
}
