package com.jikexueyuan.mediaplayerdemo;

import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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

    ArrayList<Music> musicArrayList;
    MediaPlayer player= new MediaPlayer();;
    int number = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        registerListener();
        initMusicList();
        //initMusicListBySearch();
        initListView();
        checkMusicFile();
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
        for (int i=0;i<musicArrayList.size();i++){
            Music tempMusic=musicArrayList.get(i);
            for (int j=i+1;j<musicArrayList.size();j++){
                Music checkMusic=musicArrayList.get(j);
                if ((tempMusic.getMusicName().equals(checkMusic.getMusicName()))&&
                        (tempMusic.getMusicArtist().equals(checkMusic.getMusicArtist())))
                    musicArrayList.remove(j);
            }
        }
    }

    private void initMusicListBySearch(){
        File sdCard= Environment.getExternalStorageDirectory();
        musicArrayList=MusicList.getMusicList();
        if (sdCard.exists()){
            searchMusic(sdCard.listFiles());
        }else Toast.makeText(MainActivity.this,"SD卡不存在！",Toast.LENGTH_SHORT).show();
    }

    //一开始在mMusicCursor遍历时isAfterLast()没有加“！”（musicArrayList压根就没有加入数据）,所以自己写了这个方法...
    private void searchMusic(File[] files) {
        if (files.length > 0) {
            for (File tempFile : files) {
                if (tempFile.isDirectory()) {
                    searchMusic(tempFile.listFiles());
                }else {
                    checkAndAddFile(tempFile);
                }
            }
        }
    }

    private void checkAndAddFile(File file) {
        String str=file.getName();
        if ("mp3".equals(str.substring(str.length()-3))){
            musicArrayList.add(new Music(str,"jason",file.getAbsolutePath(),"1000"));
        }
    }

    private void registerListener() {
        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveNumberToPrevious();
                play(number);
                btnPlay.setBackgroundResource(R.drawable.pause);
            }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null && player.isPlaying()) {
                    pause();
                    btnPlay.setBackgroundResource(R.drawable.play);
                } else {
                    play(number);
                    btnPlay.setBackgroundResource(R.drawable.pause);
                }
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                btnPlay.setBackgroundResource(R.drawable.play);
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveNumberToNext();
                play(number);
                btnPlay.setBackgroundResource(R.drawable.pause);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                number = position;
                play(number);
                btnPlay.setBackgroundResource(R.drawable.pause);
            }
        });
    }

    private void findViews() {
        btnPre = (ImageButton) findViewById(R.id.ibtn1);
        btnPlay = (ImageButton) findViewById(R.id.ibtn2);
        btnStop = (ImageButton) findViewById(R.id.ibtn3);
        btnNext = (ImageButton) findViewById(R.id.ibtn4);
        listView = (ListView) findViewById(R.id.listView1);
    }

    private void play(int number) {
        if (player != null && player.isPlaying()) {
            player.stop();
        }
        load(number);
        player.seekTo(position);
        player.start();
    }

    int position=0;
    private void pause() {
        if (player.isPlaying()) {
            player.pause();
            position=player.getCurrentPosition();
        }
    }

    private void stop() {
        player.stop();
        position=0;
    }

    private void resume() {
        player.start();
    }

    private void replay() {
        player.start();
    }

    private void moveNumberToNext() {
        position=0;
        if (number == musicArrayList.size() - 1) {
            Toast.makeText(MainActivity.this, R.string.tip_reach_bottom, Toast.LENGTH_SHORT).show();
        } else {
            number++;
            play(number);
        }
    }

    private void moveNumberToPrevious() {
        position=0;
        if (number == 0) {
            Toast.makeText(MainActivity.this, R.string.tip_reach_top, Toast.LENGTH_SHORT).show();
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
    }
}