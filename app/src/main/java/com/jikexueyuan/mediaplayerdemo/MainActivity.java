package com.jikexueyuan.mediaplayerdemo;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ImageButton btnPre, btnPlay, btnStop, btnNext;
    ListView listView;
    TextView tv_current,tv_total;
    SeekBar seekBar;
    RelativeLayout root_layout;
    Handler seekBarHandler;
    private int duration=0;
    private int time=0;

    private static final int PROGRESS_INCREASE=0;
    private static final int PROGRESS_PAUSE=1;
    private static final int PROGRESS_RESET=2;
    ArrayList<Music> musicArrayList;
    //MediaPlayer player = new MediaPlayer();
    ;
    int number = 0;
    private int status=MusicService.COMMAND_UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService(new Intent(MainActivity.this, MusicService.class));
        findViews();
        registerListener();
        initMusicList();
        registerStatusReceiver();
        //initMusicListBySearch();
        initListView();
        checkMusicFile();
        initSeekBarHandler();

    }

    private void initSeekBarHandler() {
        seekBarHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case PROGRESS_INCREASE:
                        if (seekBar.getProgress()<duration){
                            seekBar.setProgress(time);
                            seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                            tv_current.setText(formatTime(time));
                            time+=1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        seekBar.setProgress(0);
                        tv_current.setText("00:00");
                        break;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        PropertyBean bean=new PropertyBean(MainActivity.this);
        setTheme(bean.getTheme());
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_PLAYING);

    }

    @Override
    protected void onDestroy() {
        if (status==MusicService.STATUS_STOPPED){
            stopService(new Intent(MainActivity.this,MusicService.class));
        }

        super.onDestroy();
    }

    StatusReceiver receiver;
    private void registerStatusReceiver() {
        receiver=new StatusReceiver();
        IntentFilter filter=new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, filter);
    }

    private void sendBroadcastOnCommand(int command) {
        Intent intent=new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number",number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                intent.putExtra("time",time);
                break;
            case MusicService.COMMAND_PAUSE:
            case MusicService.COMMAND_PLAY_LAST:
            case MusicService.COMMAND_PLAY_NEXT:
            case MusicService.COMMAND_STOP:
            case MusicService.COMMAND_RESUME:
            default:
                break;
        }
        sendBroadcast(intent);
    }

    private void checkMusicFile() {
        if (musicArrayList.isEmpty()) {
            btnNext.setEnabled(false);
            btnPre.setEnabled(false);
            btnPlay.setEnabled(false);
            btnStop.setEnabled(false);
            Toast.makeText(MainActivity.this, "当前没有歌曲文件！", Toast.LENGTH_SHORT).show();
        } else {
            btnNext.setEnabled(true);
            btnPre.setEnabled(true);
            btnPlay.setEnabled(true);
            btnStop.setEnabled(true);
        }
    }

    private void initListView() {
        List<Map<String, String>> maps = new ArrayList<>();
        HashMap<String, String> map;
        SimpleAdapter adapter;
        for (Music music : musicArrayList) {
            map = new HashMap<>();
            map.put("musicName", music.getMusicName());
            map.put("singerName", music.getMusicArtist());
            maps.add(map);
        }
        String[] from = new String[]{"musicName", "singerName"};
        int[] to = new int[]{R.id.musicName, R.id.singerName};
        adapter = new SimpleAdapter(this, maps, R.layout.item, from, to);
        listView.setAdapter(adapter);
    }

    private void initMusicList() {
        musicArrayList = MusicList.getMusicList();
        if (musicArrayList.isEmpty()) {
            Cursor mMusicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            int indexTitle = mMusicCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE);
            int indexArtist = mMusicCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST);
            int indexTotalTime = mMusicCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION);
            int indexPath = mMusicCursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA);

            //虽然看起来比以前直接用mMusicCursor.getString(mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE););
            //要繁琐，但是减少了getColumnIndex(...)的使用次数
            for (mMusicCursor.moveToFirst(); !mMusicCursor.isAfterLast(); mMusicCursor.moveToNext()) {
                String title = mMusicCursor.getString(indexTitle);
                String artist = mMusicCursor.getString(indexArtist);
                String duration = mMusicCursor.getString(indexTotalTime);
                String path = mMusicCursor.getString(indexPath);

                if (artist.equals("<unknown>")) artist = "无艺术家";
                Music music = new Music(title, artist, path, duration);
                musicArrayList.add(music);
            }
            mMusicCursor.close();
        }
        removeSameMusic();
    }

    //虽然列表显示有重复的，但是还是有微小的区别，可能要通过判断歌曲名和歌手名来去掉重复
    private void removeSameMusic() {
        for (int i = 0; i < musicArrayList.size(); i++) {
            Music tempMusic = musicArrayList.get(i);
            for (int j = i + 1; j < musicArrayList.size(); j++) {
                Music checkMusic = musicArrayList.get(j);
                if ((tempMusic.getMusicName().equals(checkMusic.getMusicName())) &&
                        (tempMusic.getMusicArtist().equals(checkMusic.getMusicArtist())))
                    musicArrayList.remove(j);
            }
        }
    }

    private void initMusicListBySearch() {
        File sdCard = Environment.getExternalStorageDirectory();
        musicArrayList = MusicList.getMusicList();
        if (sdCard.exists()) {
            searchMusic(sdCard.listFiles());
        } else Toast.makeText(MainActivity.this, "SD卡不存在！", Toast.LENGTH_SHORT).show();
    }

    //一开始在mMusicCursor遍历时isAfterLast()没有加“！”（musicArrayList压根就没有加入数据）,所以自己写了这个方法...
    private void searchMusic(File[] files) {

        if (files.length > 0) {
            for (File tempFile : files) {
                if (tempFile.isDirectory()) {
                    searchMusic(tempFile.listFiles());
                } else {
                    checkAndAddFile(tempFile);
                }
            }
        }
    }

    private void checkAndAddFile(File file) {
        String str = file.getName();
        if ("mp3".equals(str.substring(str.length() - 3))) {
            musicArrayList.add(new Music(str, "jason", file.getAbsolutePath(), "1000"));
        }
    }


    private void registerListener() {
        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                moveNumberToPrevious();
//                play(number);
//                btnPlay.setBackgroundResource(R.drawable.pause);

                sendBroadcastOnCommand(MusicService.COMMAND_PLAY_LAST);

            }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (player != null && player.isPlaying()) {
//                    pause();
//                    btnPlay.setBackgroundResource(R.drawable.play);
//                } else {
//                    play(number);
//                    btnPlay.setBackgroundResource(R.drawable.pause);
//                }
                switch (status){
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                stop();
//                btnPlay.setBackgroundResource(R.drawable.play);
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                moveNumberToNext();
//                play(number);
//                btnPlay.setBackgroundResource(R.drawable.pause);
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY_NEXT);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                number = position;
//                play(number);
//                btnPlay.setBackgroundResource(R.drawable.pause);
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (status!=MusicService.STATUS_STOPPED){
                    time=seekBar.getProgress();
                    tv_current.setText(formatTime(time));
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                }
                if (status==MusicService.STATUS_PLAYING){
                    seekBarHandler.sendEmptyMessage(PROGRESS_INCREASE);
                }
            }
        });
    }

    private void findViews() {
        btnPre = (ImageButton) findViewById(R.id.ibtn1);
        btnPlay = (ImageButton) findViewById(R.id.ibtn2);
        btnStop = (ImageButton) findViewById(R.id.ibtn3);
        btnNext = (ImageButton) findViewById(R.id.ibtn4);
        listView = (ListView) findViewById(R.id.listView1);
        seekBar= (SeekBar) findViewById(R.id.seekBar);
        tv_current= (TextView) findViewById(R.id.tv1);
        tv_total= (TextView) findViewById(R.id.tv2);
        root_layout= (RelativeLayout) findViewById(R.id.relative);
    }

    class StatusReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            status=intent.getIntExtra("status",-1);
            switch (status){
                case MusicService.STATUS_PLAYING:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);
                    time=intent.getIntExtra("time",0);
                    duration=intent.getIntExtra("duration",0);
                    number=intent.getIntExtra("number",0);
                    listView.setSelection(number);
                    seekBar.setProgress(time);
                    seekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessage(PROGRESS_INCREASE);
                    tv_total.setText(formatTime(duration));
                    btnPlay.setBackgroundResource(R.drawable.pause);
                    break;
                case MusicService.STATUS_PAUSED:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);
                    btnPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_STOPPED:
                    time=0;
                    duration=0;
                    tv_current.setText(formatTime(time));
                    tv_total.setText(formatTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    btnPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number=intent.getIntExtra("number",0);
                    if (number==musicArrayList.size()-1){
                        sendBroadcastOnCommand(MusicService.STATUS_STOPPED);
                    }else {
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY_NEXT);
                    }
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    btnPlay.setBackgroundResource(R.drawable.play);
                    break;
                default:
                    break;
            }
        }
    }

    private String formatTime(int msec){
        int minute=(msec/1000)/60;
        int second=(msec/1000)%60;
        String minuteString;
        String secondString;
        if (minute<10){
            minuteString="0"+minute;
        }else {
            minuteString=""+minute;
        }
        if (second<10){
            secondString="0"+second;
        }else {
            secondString=""+second;
        }
        return minuteString+":"+secondString;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        for (int i=0;i<menu.size();i++){
            MenuItem item=menu.getItem(i);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_theme:
                new AlertDialog.Builder(this).setTitle("请选择主题").setItems(R.array.theme, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String theme=PropertyBean.THEMES[which];
                        setTheme(theme);
                        PropertyBean bean=new PropertyBean(MainActivity.this);
                        bean.setAndSaveTheme(theme);
                    }
                }).show();
                break;
            case R.id.menu_about:
                new AlertDialog.Builder(this).setTitle(R.string.app_name).setMessage(R.string.about_us).show();
                break;
            case R.id.menu_quit:
                new AlertDialog.Builder(this).setTitle("提示").setMessage(R.string.quit_message)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        }).setNegativeButton("取消",null).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    private void setTheme(String theme){
        if ("pink_hot".equals(theme)){
            root_layout.setBackgroundResource(R.drawable.sky);
        }else if ("demon".equals(theme)){
            root_layout.setBackgroundResource(R.drawable.monster);
        }else if ("nature".equals(theme)){
            root_layout.setBackgroundResource(R.drawable.tree);
        }else if ("love".equals(theme)){
            root_layout.setBackgroundResource(R.drawable.love);
        }
    }


}
