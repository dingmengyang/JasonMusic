package com.jikexueyuan.mediaplayerdemo;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    ImageButton btnPre, btnPlay, btnStop, btnNext;
    ListView listView;
    TextView tv_current, tv_total, tv_name, tv_sleep;
    SeekBar seekBar;
    RelativeLayout root_layout;
    Handler seekBarHandler;
    Handler updateNameHandler;
    private int duration = 0;
    private int time = 0;

    private static final int PROGRESS_INCREASE = 0;
    private static final int PROGRESS_PAUSE = 1;
    private static final int PROGRESS_RESET = 2;
    private static final int UPDATE_NAME = 3;
    private static final int LENGTH = 20;

    private static final int MODE_LIST_SEQUENCE = 0;
    private static final int MODE_SINGLE_CYCLE = 1;
    private static final int MODE_LIST_CYCLE = 2;
    private int playMode;

    private static boolean isExit = false;
    ArrayList<Music> musicArrayList;
    AudioManager audioManager;

    private static final boolean NOTSLEEP = false;
    private static final boolean ISSLEEP = true;
    private AlarmManager alarmManager;
    private int sleepMinute = 20;
    private static boolean sleepMode;

    //MediaPlayer player = new MediaPlayer();
    ;
    int number = 0;
    private int status = MusicService.COMMAND_UNKNOWN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setActionBarView();
        startService(new Intent(MainActivity.this, MusicService.class));
        findViews();
        registerListener();
        initMusicList();
        registerStatusReceiver();
        //initMusicListBySearch();
        initListView();
        checkMusicFile();
        initSeekBarHandler();
        initUpdateNameHandler();
        playMode = MODE_LIST_SEQUENCE;
        sleepMode = NOTSLEEP;
    }

    TextView audio_tv;
    SeekBar audio_bar;

    private void setActionBarView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View root = inflater.inflate(R.layout.audio_seekbar, null);
        audio_tv = (TextView) root.findViewById(R.id.audio_percent);
        audio_bar = (SeekBar) root.findViewById(R.id.audio_seekbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(root, new android.support.v7.app.ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                exitByDoubleCheck();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                int progress = audio_bar.getProgress();
                if (progress + 5 < audio_bar.getMax()) {
                    progress += 5;
                    audio_bar.setProgress(progress);
                } else audio_bar.setProgress(audio_bar.getMax());
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                int progress1 = audio_bar.getProgress();
                if (progress1 - 5 > 0) {
                    progress1 -= 5;
                    audio_bar.setProgress(progress1);
                } else audio_bar.setProgress(0);
                return true;
        }
        return false;
    }

    private void audioControl() {
        final int max_progress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audio_bar.setMax(max_progress);
        int progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio_bar.setProgress(progress);
        audio_tv.setText("" + progress * 100 / max_progress + "%");
        audio_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND);
                audio_tv.setText("" + progress * 100 / max_progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void exitByDoubleCheck() {
        Timer timer;
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次将退出程序！", Toast.LENGTH_SHORT).show();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            System.exit(0);
        }
    }

    int update_position = 0;
    String song_name;
    int update_divide = 0;
    int update_total = 0;

    private void initUpdateNameHandler() {
        updateNameHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == UPDATE_NAME) {
                    try {
                        String str;
                        if (update_position < update_divide) {
                            str = song_name.substring(update_position, update_position + LENGTH);
                        } else {
                            str = song_name.substring(update_position, update_total);
                        }
                        tv_name.setText(str);
                        update_position++;
                        if (update_position > update_total) update_position = 0;
                        updateNameHandler.sendEmptyMessageDelayed(UPDATE_NAME, 300);
                    } catch (StringIndexOutOfBoundsException e) {
                        tv_name.setText(song_name);
                    }
                }
            }
        };
    }

    private void initSeekBarHandler() {
        seekBarHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case PROGRESS_INCREASE:
                        if (seekBar.getProgress() < duration) {
                            seekBar.setProgress(time);
                            seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                            tv_current.setText(formatTime(time));
                            time += 1000;
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
        PropertyBean bean = new PropertyBean(MainActivity.this);
        setTheme(bean.getTheme());
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_PLAYING);
        audioControl();
        if (sleepMode == ISSLEEP) {
            tv_sleep.setVisibility(View.VISIBLE);
        } else tv_sleep.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        if (status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(MainActivity.this, MusicService.class));
        }

        super.onDestroy();
    }

    StatusReceiver receiver;

    private void registerStatusReceiver() {
        receiver = new StatusReceiver();
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, filter);
    }

    private void sendBroadcastOnCommand(int command) {
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                intent.putExtra("time", time);
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
                switch (status) {
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
                if (number==musicArrayList.size()-1){
                    if (playMode==MODE_LIST_CYCLE){
                        number=0;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }else {
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY_NEXT);
                    }
                }else
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
                if (status != MusicService.STATUS_STOPPED) {
                    time = seekBar.getProgress();
                    tv_current.setText(formatTime(time));
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                }
                if (status == MusicService.STATUS_PLAYING) {
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
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tv_current = (TextView) findViewById(R.id.tv1);
        tv_total = (TextView) findViewById(R.id.tv2);
        tv_name = (TextView) findViewById(R.id.tv3);
        tv_sleep = (TextView) findViewById(R.id.main_tv_sleep);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        root_layout = (RelativeLayout) findViewById(R.id.relative);
    }

    class StatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            status = intent.getIntExtra("status", -1);
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);
                    updateNameHandler.removeMessages(UPDATE_NAME);
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    number = intent.getIntExtra("number", 0);
                    listView.setSelection(number);
                    seekBar.setProgress(time);
                    seekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessage(PROGRESS_INCREASE);
                    tv_total.setText(formatTime(duration));
                    tv_name.setText("");
                    j = 0;
                    showSongName(intent.getStringExtra("musicName"), intent.getStringExtra("musicArtist"));
                    btnPlay.setBackgroundResource(R.drawable.pause);
                    break;
                case MusicService.STATUS_PAUSED:
                    seekBarHandler.removeMessages(PROGRESS_INCREASE);
                    btnPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_STOPPED:
                    time = 0;
                    duration = 0;
                    tv_name.setText("");
                    tv_current.setText(formatTime(time));
                    tv_total.setText(formatTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    btnPlay.setBackgroundResource(R.drawable.play);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number = intent.getIntExtra("number", 0);
                    tv_name.setText("");
                    if (playMode == MainActivity.MODE_LIST_SEQUENCE) {
                        if (number == musicArrayList.size() - 1) {
                            sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                        } else {
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY_NEXT);
                        }
                    } else if (playMode == MainActivity.MODE_SINGLE_CYCLE) {
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    } else if (playMode == MainActivity.MODE_LIST_CYCLE) {
                        if (number == musicArrayList.size() - 1) {
                            number = 0;
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        } else {
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY_NEXT);
                        }
                    }

                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    btnPlay.setBackgroundResource(R.drawable.play);
                    break;
                default:
                    break;
            }
        }
    }

    int j = 0;

    private void showSongName(String musicName, String musicArtist) {

        song_name = "  " + musicName + " - " + musicArtist;
        if (song_name.length() > LENGTH) {
            update_divide = song_name.length() - LENGTH;
            update_total = song_name.length();

            updateNameHandler.sendEmptyMessage(UPDATE_NAME);
        } else {
            tv_name.setText(song_name);
        }
    }

    private String formatTime(int msec) {
        int minute = (msec / 1000) / 60;
        int second = (msec / 1000) % 60;
        String minuteString;
        String secondString;
        if (minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }
        if (second < 10) {
            secondString = "0" + second;
        } else {
            secondString = "" + second;
        }
        return minuteString + ":" + secondString;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_theme:
                new AlertDialog.Builder(this).setTitle("请选择主题").setItems(R.array.theme, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String theme = PropertyBean.THEMES[which];
                        setTheme(theme);
                        PropertyBean bean = new PropertyBean(MainActivity.this);
                        bean.setAndSaveTheme(theme);
                    }
                }).show();
                break;
            case R.id.menu_playMode:
                String[] modes = new String[]{"顺序播放", "单曲循环", "列表循环"};
                new AlertDialog.Builder(this).setTitle("播放模式").setSingleChoiceItems(modes, playMode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playMode = which;
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (playMode) {
                            case 0:
                                playMode = MainActivity.MODE_LIST_SEQUENCE;
                                Toast.makeText(MainActivity.this, R.string.sequence, Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                playMode = MainActivity.MODE_SINGLE_CYCLE;
                                Toast.makeText(MainActivity.this, R.string.singlecycle, Toast.LENGTH_SHORT).show();
                                break;
                            case 2:
                                playMode = MainActivity.MODE_LIST_CYCLE;
                                Toast.makeText(MainActivity.this, R.string.listcycle, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    }
                }).show();
                break;
            case R.id.menu_sleep:
                showSleepDialog();
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
                        }).setNegativeButton("取消", null).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSleepDialog() {
        View root = LayoutInflater.from(this).inflate(R.layout.sleep_dialog, null);
        Switch sleep_switch = (Switch) root.findViewById(R.id.dialog_switch);
        final TextView sleep_tv = (TextView) root.findViewById(R.id.dialog_tv);
        SeekBar sleep_bar = (SeekBar) root.findViewById(R.id.dialog_seekbar);

        sleep_bar.setMax(60);
        sleep_tv.setText("睡眠于:" + sleepMinute + "分钟");
        if (sleepMode == NOTSLEEP) sleep_switch.setChecked(false);
        sleep_bar.setProgress(sleepMinute);
        sleep_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sleepMode = isChecked;
            }
        });
        sleep_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sleepMinute = progress;
                sleep_tv.setText("睡眠于:" + progress + "分钟");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        new AlertDialog.Builder(this).setTitle("请选择睡眠时间(0～60分钟)").setView(root)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (sleepMode == ISSLEEP) {
                            alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                            Intent intent = new Intent(MainActivity.this, CloseActivity.class);
                            PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                            alarmManager.set(AlarmManager.ELAPSED_REALTIME, sleepMinute * 60 * 1000, pi);
                            tv_sleep.setVisibility(View.VISIBLE);
                        } else {
                            if (alarmManager != null) {
                                Intent intent = new Intent(MainActivity.this, CloseActivity.class);
                                PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                                alarmManager.cancel(pi);
                            }
                            dialog.dismiss();
                            tv_sleep.setVisibility(View.INVISIBLE);
                        }
                    }
                }).setNeutralButton("重置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sleepMode = NOTSLEEP;
                tv_sleep.setVisibility(View.INVISIBLE);
                sleepMinute = 20;
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }


    private void setTheme(String theme) {
        if ("pink_hot".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.sky);
        } else if ("demon".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.monster);
        } else if ("nature".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.tree);
        } else if ("love".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.love);
        }
    }

}
