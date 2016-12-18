package com.jikexueyuan.mediaplayerdemo;


import java.util.ArrayList;

public class MusicList {

    private static ArrayList<Music> musicArray=new ArrayList<>();

    private MusicList(){

    }

    public static ArrayList<Music> getMusicList(){
        return musicArray;
    }
}
