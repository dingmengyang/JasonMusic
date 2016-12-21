package com.jikexueyuan.mediaplayerdemo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
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
    private MediaPlayer player = new MediaPlayer();
    private ArrayList<Music> musicArrayList = MusicList.getMusicList();
    CommandReceiver receiver;

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
        registerReceiver(receiver, filter);
        TelephonyManager telephonyManager= (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(new MyPhoneListener(),PhoneStateListener.LISTEN_CALL_STATE );
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
        if (player!=null){
            player.release();
        }
        super.onDestroy();
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
                    if (player!=null&&player.isPlaying()){
                        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
                    }
                    break;
                case COMMAND_SEEK_TO:
                    seekTo(intent.getIntExtra("time",0));
                case COMMAND_UNKNOWN:
                default:
                    break;
            }
        }
    }

    private void seekTo(int time){
        player.seekTo(time);
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
    }

    int position = 0;

    private void pause() {
        if (player.isPlaying()) {
            player.pause();
            status=MusicService.STATUS_PAUSED;
            position = player.getCurrentPosition();
            sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
        }
    }

    private void stop() {
        if (status!=MusicService.STATUS_STOPPED) {
            player.stop();
            position = 0;
            status=MusicService.STATUS_STOPPED;
            sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
        }
    }

    private void resume() {
        player.start();
        status=MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
    }

    private void replay() {
        player.start();
        status=MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
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
