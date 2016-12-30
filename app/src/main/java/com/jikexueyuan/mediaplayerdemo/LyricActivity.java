package com.jikexueyuan.mediaplayerdemo;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricActivity extends AppCompatActivity {


    private TextView tv_lrc;
    private LinearLayout layout_lrc;


    private int status;
    private StatusChangedReceiver receiver;

    private String musicName = null;
    private String singerName = null;
    private String lyric_data = null;
    private String fileName = null;
    private GestureDetector gestureDetector;

   private LrcFileLoader loader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric);
        android.support.v7.app.ActionBar actionBar=getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        tv_lrc = (TextView) findViewById(R.id.lrc_tv_lrc);
        layout_lrc = (LinearLayout) findViewById(R.id.lrc_layout);
        bindStatusChangedReceiver();

        getDataFromMainActivity();
        gestureDetector=new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e2.getX()-e1.getX()>150){
                    finish();
                    overridePendingTransition(R.anim.to_right_enter,R.anim.to_right_exit);
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector.onTouchEvent(ev)){
            ev.setAction(MotionEvent.ACTION_CANCEL);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void getDataFromMainActivity() {
        Intent intent = getIntent();
        musicName = intent.getStringExtra("musicName");
        singerName = intent.getStringExtra("musicArtist");
        LyricActivity.this.setTitle(musicName + "-" + singerName);
        try {
            get_lrc(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void get_lrc(Context context) throws Exception {
        String[] files=context.fileList();//手机存储
        if (musicName!=null){
            fileName=musicName+".lrc";
            List<String> fileList= Arrays.asList(files);//!!!!!!!相当有用
            if (fileList.contains(fileName)){
                Log.d("jason","has lrc file");

                FileInputStream fileInputStream=context.openFileInput(fileName);
                byte[] buffer=new byte[fileInputStream.available()];
                fileInputStream.read(buffer);
                String result=new String(buffer);
//                File file=new File(fileName);
//                BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//                String result = null;
//                String line;
//                while ((line=reader.readLine())!=null){
//                    result+=line;
//                }
                tv_lrc.setText(result);
            }else {
                ConnectivityManager manager= (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo info=manager.getActiveNetworkInfo();
                if (info!=null&&info.isAvailable()){
                    Log.d("jason","has not lrc file");
                    loader=new LrcFileLoader(LyricActivity.this,tv_lrc,fileName);
                    loader.startDownLoadLrc(URLEncoder.encode(musicName,"UTF-8"));

                }else {
                    Toast.makeText(LyricActivity.this,"网络出问题了，请检测网络",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    private void bindStatusChangedReceiver() {
        IntentFilter filter=new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        receiver=new StatusChangedReceiver();
        registerReceiver(receiver, filter);
    }

    private void sendBroadcastOnCommand(int command) {
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        sendBroadcast(intent);
    }


    private class StatusChangedReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            status=intent.getIntExtra("status",-1);
            switch (status){
                case MusicService.STATUS_PLAYING:
                    musicName=intent.getStringExtra("musicName");
                    singerName=intent.getStringExtra("musicArtist");
                    LyricActivity.this.setTitle(musicName + "-" + singerName);
                    try {
                        get_lrc(LyricActivity.this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }


}
