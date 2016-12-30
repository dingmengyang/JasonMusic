package com.jikexueyuan.mediaplayerdemo;


import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LrcFileLoader {

    public static final String LRC_SEARCH_URL = "http://geci.me/api/lyric/";
    public static final int REQUEST_TIMEOUT = 15 * 1000;
    public static final int SO_TIMEOUT = 15 * 1000;
    private Context context;
    private String fileName;

    public LrcFileLoader(Context context, TextView tv, String fileName) {
        tv_lyric = tv;
        this.context = context;
        this.fileName = fileName;
    }

    public void startDownLoadLrc(String name) {
        new AsyncDownLoad().execute(name);
    }

    private String getSongLRCUrl(String songName) throws Exception {
        String url;
        String str_json;
        if (songName == null) return null;

        str_json = getHtmlCode(LRC_SEARCH_URL + songName);
        if (str_json == null) return null;

        JSONObject object = new JSONObject(str_json);
        int count = object.getInt("count");
        if (count == 0) return null;

        JSONArray jsonArray = object.getJSONArray("result");
        JSONObject item = jsonArray.getJSONObject(0);
        url = item.getString("lrc");
        return url;
    }

    private HttpClient getHttpClient() {
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
        HttpClient client = new DefaultHttpClient(httpParams);
        return client;
    }

    private String getHtmlCode(String s) {
        String result = null;
        try {
            HttpClient httpClient = getHttpClient();
            HttpGet get = new HttpGet(s);
            HttpResponse response = httpClient.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(), "utf-8"));
                String line;
                result = "";
                while ((line = br.readLine()) != null) {
                    result += line + "\n";
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String lrc_data;
    private TextView tv_lyric;

    class AsyncDownLoad extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            String url;
            try {
                url = getSongLRCUrl(params[0]);
                lrc_data = getHtmlCode(url);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return lrc_data;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tv_lyric.setText("搜索歌词中...");
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                String newS=modifyLrcWord(s);
                tv_lyric.setText(newS);
                try {
                    FileOutputStream os = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                    os.write(newS.getBytes());
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else tv_lyric.setText("没有找到歌词！");

        }
    }

    private String modifyLrcWord(String line) {

        Pattern pattern = Pattern.compile("\\[\\d{2}:\\d{2}.\\d{2}\\]");


        Matcher m = pattern.matcher(line);
        line = m.replaceAll("").replace("[ti:", "").replace("[ar:", "").replace("[al:", "").replace("[by:", "").replace("[i:", "")
                .replace("]", "");
        line = line.contains("offset") ? "" : line;
        line = line.replace("url", "歌词来自").replace("null", "");


        return line;


    }

}
