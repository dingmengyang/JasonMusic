package com.jikexueyuan.mediaplayerdemo;


import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LyricLoader {

    private TextView tv_lyric;
    public static final String LRC_SEARCH_URL="http://geci.me/api/lyric/";

    public LyricLoader(TextView tv){
        tv_lyric=tv;
    }

    public void startDownLoad(String songName){
        new LyricDownLoadTask().execute(LRC_SEARCH_URL+songName);
    }
    class LyricDownLoadTask extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... params) {
            return getJsonData(params[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            String result=getLyric(s);
            if (result!=null) {
                tv_lyric.setText(result);
            }else tv_lyric.setText("未找到歌词！");
            super.onPostExecute(s);
        }

        @Override
        protected void onPreExecute() {
            tv_lyric.setText("正在寻找歌词...");
            super.onPreExecute();
        }
    }

    private String getData(InputStream is) {
        String result="";
        try {
            InputStreamReader isr=new InputStreamReader(is,"utf-8");
            BufferedReader br=new BufferedReader(isr);
            String line;
            while((line=br.readLine())!=null){
                result+=line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getJsonData(String url) {
        String lyric;
        try {
            JSONObject jsonObject=new JSONObject(getData(new URL(url).openStream()));
            JSONArray jsonArray=jsonObject.getJSONArray("result");
            lyric=jsonArray.getJSONObject(0).getString("lrc");
            return lyric;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }




    private String getLyric(String url) {
        String result=null;
        try {
            URL downUrl=new URL(url);
            HttpURLConnection connection= (HttpURLConnection) downUrl.openConnection();
            //之前的ImageLoader中没有这些，难道是因为 Bitmap bitmap= BitmapFactory.decodeStream(connection.getInputStream());比较简单？？？
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15*1000);

            result="";
            BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"utf-8"));
            String line;
            while ((line=reader.readLine())!=null){
                result+=line+"\n";
            }
            connection.disconnect();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
