package com.jikexueyuan.mediaplayerdemo;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.TextView;

public class WidgetProviderClass extends AppWidgetProvider{

//    private ImageButton btnPre,btnPlay,btnNext;
//    private TextView tvTitle;
    public static String BROADCAST_MUSICSERVICE_CONTROL="MusicService.ACTION_CONTROL";
    public static String BROADCAST_MUSICSERVICE_UPDATE_STATUS="MusicService.ACTION_UPDATE";

    public static final int RequestCode_StartActivity=0;
    public static final int RequestCode_Play=1;
    public static final int RequestCode_Pause=2;
    public static final int RequestCode_Next=3;
    public static final int RequestCode_Previous=4;

    private RemoteViews remoteViews=null;
    private String musicName;
    private String musicArtist;
    private int status;

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        remoteViews=new RemoteViews(context.getPackageName(),R.layout.widget_layout);
        if (intent.getAction().equals(WidgetProviderClass.BROADCAST_MUSICSERVICE_UPDATE_STATUS)) {
            status = intent.getIntExtra("status", -1);
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    musicName = intent.getStringExtra("musicName");
                    musicArtist = intent.getStringExtra("musicArtist");
                    remoteViews.setTextViewText(R.id.widget_tv_title, musicName + "-" + musicArtist);
                    remoteViews.setImageViewResource(R.id.widget_btn_play, R.drawable.widget_pause);
                    //先准备好pendingIntent，当AppWidget按钮被点击时触发，因为它访问不到在这个应用中设定的onClick方法。
                    Intent intent_pause = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intent_pause.putExtra("command", MusicService.COMMAND_PAUSE);
                    PendingIntent pi_pause=PendingIntent.getBroadcast(context,RequestCode_Pause,intent_pause,PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_btn_play,pi_pause);
                    break;
                case MusicService.STATUS_PAUSED:
                    remoteViews.setImageViewResource(R.id.widget_btn_play, R.drawable.widget_play);
                    Intent intent_play = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intent_play.putExtra("command", MusicService.COMMAND_RESUME);
                    PendingIntent pi_play=PendingIntent.getBroadcast(context,RequestCode_Play,intent_play,PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_btn_play, pi_play);
                    break;
                case MusicService.STATUS_STOPPED:
                    remoteViews.setImageViewResource(R.id.widget_btn_play, R.drawable.widget_play);
                    remoteViews.setTextViewText(R.id.widget_tv_title,"Jason音乐");
                    break;
                default:
                    break;
            }
            AppWidgetManager appWidgetManager=AppWidgetManager.getInstance(context);
            ComponentName componentName=new ComponentName(context,WidgetProviderClass.class);
            appWidgetManager.updateAppWidget(componentName,remoteViews);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        remoteViews=new RemoteViews(context.getPackageName(),R.layout.widget_layout);

        Intent intent=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", MusicService.COMMAND_CHECK_PLAYING);
        context.sendBroadcast(intent);
        //标题（启动MainActivity???）
        Intent intent_title=new Intent();
        intent_title.setClass(context, MainActivity.class);
        PendingIntent pi_title=PendingIntent.getActivity(context,RequestCode_StartActivity,intent_title,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent_next=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent_next.putExtra("command", MusicService.COMMAND_PLAY_NEXT);
        PendingIntent pi_next=PendingIntent.getBroadcast(context, RequestCode_Next, intent_next, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent_pre=new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent_pre.putExtra("command",MusicService.COMMAND_PLAY_LAST);
        PendingIntent pi_pre=PendingIntent.getBroadcast(context, RequestCode_Previous, intent_pre, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.widget_tv_title,pi_title);
        remoteViews.setOnClickPendingIntent(R.id.widget_btn_next,pi_next);
        remoteViews.setOnClickPendingIntent(R.id.widget_btn_pre,pi_pre);

        appWidgetManager.updateAppWidget(appWidgetIds,remoteViews);
    }
}
