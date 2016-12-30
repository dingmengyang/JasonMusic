package com.jikexueyuan.mediaplayerdemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Visualizer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service {

    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    public static final int COMMAND_RESUME = 2;
    public static final int COMMAND_PLAY_LAST = 3;
    public static final int COMMAND_PLAY_NEXT = 4;
    public static final int COMMAND_STOP = 5;
    public static final int COMMAND_CHECK_PLAYING = 6;
    public static final int COMMAND_SEEK_TO = 7;



    public static final int STATUS_PLAYING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_STOPPED = 2;
    public static final int STATUS_COMPLETED = 3;

    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";

    private boolean phone=false;
    private int number = 0;
    private int status;
    private static MediaPlayer player = new MediaPlayer();

    public static BassBoost boost;
    public static Equalizer equalizer;
    public static PresetReverb presetReverb;
    public static Visualizer visualizer;



    private ArrayList<Music> musicArrayList = MusicList.getMusicList();
    CommandReceiver receiver;
    private NotificationManager notificationManager;

    public MusicService() {

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        receiver=new CommandReceiver();
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        filter.addAction("CLOSE");
        filter.addAction("NEXT");
        filter.addAction("PAUSE");
        filter.addAction("RESUME");
        filter.addAction("PREVIOUS");

        registerReceiver(receiver, filter);
        TelephonyManager telephonyManager= (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(new MyPhoneListener(),PhoneStateListener.LISTEN_CALL_STATE );

        getEqualizer();
    }


    private void getEqualizer() {
        presetReverb=new PresetReverb(0,player.getAudioSessionId());
        boost=new BassBoost(0,player.getAudioSessionId());
        equalizer=new Equalizer(0,player.getAudioSessionId());
        visualizer=new Visualizer(player.getAudioSessionId());
    }

    private void updateNotification() {
        notificationManager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this);
        RemoteViews remoteViews=new RemoteViews(getPackageName(),R.layout.notification);
        Music music=musicArrayList.get(number);
        remoteViews.setTextViewText(R.id.notify_tv_song,music.getMusicName());

        remoteViews.setTextViewText(R.id.notify_tv_singer,music.getMusicArtist());

        if (status==MusicService.STATUS_PLAYING){
            remoteViews.setViewVisibility(R.id.notify_btn_play, View.GONE);
            remoteViews.setViewVisibility(R.id.notify_btn_pause, View.VISIBLE);
        }else {
            remoteViews.setViewVisibility(R.id.notify_btn_play, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.notify_btn_pause, View.GONE);
        }
/*
在Intent中通过putExtra添加参数区分控件是行不通的，用action可以区分,这样直接复用之前的receiver，指定command就不是那么合适了，
 */
//        Intent intent_pre=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
//        intent_pre.putExtra("command",MusicService.COMMAND_PLAY_LAST);
//        PendingIntent pi_pre=PendingIntent.getBroadcast(getApplicationContext(), 0, intent_pre, 0);
//
//        Intent intent_play=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
//        intent_play.putExtra("command",MusicService.COMMAND_PLAY);
//        PendingIntent pi_play=PendingIntent.getBroadcast(getApplicationContext(), 0, intent_play, 0);
//
//        Intent intent_pause=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
//        intent_pause.putExtra("command",MusicService.COMMAND_PAUSE);
//        PendingIntent pi_pause=PendingIntent.getBroadcast(getApplicationContext(), 0, intent_pause, 0);
//
//        Intent intent_next=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
//        intent_next.putExtra("command",MusicService.COMMAND_PLAY_NEXT);
//        PendingIntent pi_next=PendingIntent.getBroadcast(getApplicationContext(), 0, intent_next, 0);
//
//        Intent intent_close=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
//        intent_close.putExtra("command", MusicService.COMMAND_CLOSE);
//        PendingIntent pi_close=PendingIntent.getBroadcast(getApplicationContext(), 0, intent_close, 0);


        remoteViews.setOnClickPendingIntent(R.id.notify_btn_play,getPendingIntent("RESUME"));
        remoteViews.setOnClickPendingIntent(R.id.notify_btn_pause,getPendingIntent("PAUSE"));
        remoteViews.setOnClickPendingIntent(R.id.notify_btn_close,getPendingIntent("CLOSE"));
        remoteViews.setOnClickPendingIntent(R.id.notify_btn_next,getPendingIntent("NEXT"));
        remoteViews.setOnClickPendingIntent(R.id.notify_btn_pre, getPendingIntent("PREVIOUS"));

        Intent intent=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this, 0, intent, 0);
        /*这三个参数必须设置，否则没有效果:
        小图标，使用setSamllIcon()方法设置。
        标题，使用setContentTitle()方法设置。
        文本内容，使用setContentText()方法设置。
         */
        builder.setContent(remoteViews).setContentIntent(pi).setPriority(Notification.PRIORITY_MAX).setOngoing(true).setSmallIcon(R.drawable.notify_icon);

        Notification notification=builder.build();
        notification.flags=Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(0x123,notification);
    }

    private PendingIntent getPendingIntent(String params) {
        Intent intent=new Intent(params);
        return PendingIntent.getBroadcast(this,0,intent,0);
    }

    class MyPhoneListener extends PhoneStateListener{
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state){
                case TelephonyManager.CALL_STATE_RINGING:
                    if (status==MusicService.STATUS_PLAYING){
                        pause();
                        phone=true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (phone){
                        resume();
                        phone=false;
                    }
                    break;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
        if (player!=null){
            player.release();
        }
        notificationManager.cancel(0x123);
        unregisterReceiver(receiver);
        if (player!=null){
            visualizer.release();
            equalizer.release();
            presetReverb.release();
            boost.release();
            player.release();
        }
        super.onDestroy();
    }

    public static void initVisualizer(){
        visualizer=new Visualizer(player.getAudioSessionId());
    }
    public static void initEqualizer(){
        equalizer=new Equalizer(0,player.getAudioSessionId());
    }
    public static void initBassBoost(){
        boost=new BassBoost(0,player.getAudioSessionId());
    }
    public static void initPresetReverb(){
        presetReverb=new PresetReverb(0,player.getAudioSessionId());
    }
    private void sendBroadcastOnStatusChanged(int status){
        Intent intent=new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        intent.putExtra("status",status);
        if (status!=STATUS_STOPPED){
            intent.putExtra("time",player.getCurrentPosition());
            intent.putExtra("duration",player.getDuration());
            intent.putExtra("number",number);
            intent.putExtra("musicName",musicArrayList.get(number).getMusicName());
            intent.putExtra("musicArtist",musicArrayList.get(number).getMusicArtist());

        }
        sendBroadcast(intent);
    }

    class CommandReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String ctrl_code=intent.getAction();
            if (BROADCAST_MUSICSERVICE_CONTROL.equals(ctrl_code)) {
                switch (intent.getIntExtra("command", COMMAND_UNKNOWN)) {
                    case COMMAND_PLAY:
                        number = intent.getIntExtra("number", 0);
                        play(number);
                        break;
                    case COMMAND_PLAY_LAST:
                        moveNumberToPrevious();
                        break;
                    case COMMAND_PLAY_NEXT:
                        moveNumberToNext();
                        break;
                    case COMMAND_PAUSE:
                        pause();
                        break;
                    case COMMAND_STOP:
                        stop();
                        break;
                    case COMMAND_RESUME:
                        resume();
                        break;
                    case COMMAND_CHECK_PLAYING:
                        if (player != null && player.isPlaying()) {
                            sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
                        }
                        break;
                    case COMMAND_SEEK_TO:
                        seekTo(intent.getIntExtra("time", 0));
                    case COMMAND_UNKNOWN:
                    default:
                        break;
                }
            }else if ("RESUME".equals(ctrl_code)){
                resume();
            }else if ("CLOSE".equals(ctrl_code)){
                notificationManager.cancel(0x123);
                sendBroadcastOnStatusChanged(STATUS_STOPPED);
                System.exit(0);
            }else if ("PAUSE".equals(ctrl_code)){
                pause();
            }else if ("PREVIOUS".equals(ctrl_code)){
                moveNumberToPrevious();
            }else if ("NEXT".equals(ctrl_code)){
                moveNumberToNext();
            }
        }
    }

    private void seekTo(int time){
        player.seekTo(time);
        updateNotification();
    }

    private void play(int number) {
        if (player != null && player.isPlaying()) {
            player.stop();
        }
        load(number);
        player.seekTo(position);
        player.start();
        status=MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        updateNotification();
    }

    int position = 0;

    private void pause() {
        if (player.isPlaying()) {
            player.pause();
            status=MusicService.STATUS_PAUSED;
            position = player.getCurrentPosition();
            sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
            updateNotification();
        }
    }

    private void stop() {
        if (status!=MusicService.STATUS_STOPPED) {
            player.stop();
            position = 0;
            status=MusicService.STATUS_STOPPED;
            sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
            updateNotification();
        }
    }

    private void resume() {
        player.start();
        status=MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        updateNotification();
    }

    private void replay() {
        player.start();
        status=MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
        updateNotification();
    }

    private void moveNumberToNext() {
        position = 0;
        if (number == musicArrayList.size() - 1) {
            Toast.makeText(getApplicationContext(), R.string.tip_reach_top, Toast.LENGTH_SHORT).show();
        } else {
            number++;
            play(number);
        }
    }

    private void moveNumberToPrevious() {
        position = 0;
        if (number == 0) {
            Toast.makeText(getApplicationContext(), R.string.tip_reach_bottom, Toast.LENGTH_SHORT).show();
        } else {
            number--;
            play(number);
        }
    }

    private void load(int number) {
        Music music = musicArrayList.get(number);
        try {
            player.reset();
            player.setDataSource(music.getMusicPath());
            player.prepare();
        } catch (IOException e) {
            Log.d("jason", "load方法出错！" + e.toString());
            e.printStackTrace();
        }

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mp.isLooping()){
                    replay();
                }else {
                    sendBroadcastOnStatusChanged(MusicService.STATUS_COMPLETED);
                }
            }
        });
    }

}
