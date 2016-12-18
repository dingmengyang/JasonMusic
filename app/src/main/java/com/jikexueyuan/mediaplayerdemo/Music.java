package com.jikexueyuan.mediaplayerdemo;


public class Music {
    private String musicName;
    private String musicArtist;
    private String musicPath;
    private String musicDuration;


    public Music(String musicName, String musicArtist, String musicPath, String musicDuration) {
        this.musicName = musicName;
        this.musicArtist = musicArtist;
        this.musicPath = musicPath;
        this.musicDuration = musicDuration;
    }

    public String getMusicName() {
        return musicName;
    }

    public String getMusicArtist() {
        return musicArtist;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public String getMusicDuration() {
        return musicDuration;
    }

}
