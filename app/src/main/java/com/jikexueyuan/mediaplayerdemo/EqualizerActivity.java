package com.jikexueyuan.mediaplayerdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class EqualizerActivity extends AppCompatActivity {


    BassBoost boost;
    Equalizer equalizer;
    PresetReverb presetReverb;
    Visualizer visualizer;
    List<Short> reverbNames=new ArrayList<>();
    List<String> reverbVals=new ArrayList<>();
    LinearLayout layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout);


        setupVisualizer();
        setupEqualizer();
        setupBassBoost();
        setupPresetReverb();

    }
    private void setupPresetReverb() {
        try {
            presetReverb = MusicService.presetReverb;
            presetReverb.setEnabled(true);
        }catch (IllegalStateException e){
            MusicService.initPresetReverb();
            presetReverb = MusicService.presetReverb;
            presetReverb.setEnabled(true);
        }
        TextView tvTitle=new TextView(this);
        tvTitle.setText("音场");
        layout.addView(tvTitle);
        for (short i=0;i<equalizer.getNumberOfPresets();i++){
            reverbNames.add(i);
            reverbVals.add(equalizer.getPresetName(i));
        }
        Spinner spinner=new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(EqualizerActivity.this,
                android.R.layout.simple_spinner_item, reverbVals));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                presetReverb.setPreset(reverbNames.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        layout.addView(spinner);
    }



    private void setupBassBoost() {
        try {
            boost = MusicService.boost;
            boost.setEnabled(true);
        }catch (IllegalStateException e){
            MusicService.initBassBoost();
            boost = MusicService.boost;
            boost.setEnabled(true);
        }
        TextView tvTitle=new TextView(this);
        tvTitle.setText("重低音：");
        layout.addView(tvTitle);
        SeekBar bar=new SeekBar(this);
        bar.setMax(1000);
        bar.setProgress(0);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                boost.setStrength((short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        layout.addView(bar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        visualizer.setEnabled(false);
    }

    private void setupEqualizer() {
        try {
            equalizer = MusicService.equalizer;
            equalizer.setEnabled(true);
        }catch (IllegalStateException e){
            MusicService.initEqualizer();
            equalizer = MusicService.equalizer;
            equalizer.setEnabled(true);
        }
        TextView eqTitle=new TextView(this);
        eqTitle.setText("均衡器:");
        layout.addView(eqTitle);
        final short minEQlevel=equalizer.getBandLevelRange()[0];
        short maxEQlevel=equalizer.getBandLevelRange()[1];
        short bands=equalizer.getNumberOfBands();
        for (short i=0;i<bands;i++){
            TextView eqTextView=new TextView(this);
            eqTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            eqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            eqTextView.setText((equalizer.getCenterFreq(i) / 1000) + "Hz");
            layout.addView(eqTextView);
            LinearLayout linearLayout=new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            TextView minDbTextView=new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minEQlevel / 100) + "dB");
            TextView maxDbTextView=new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxEQlevel / 100) + "dB");
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.weight=1;
            SeekBar bar=new SeekBar(this);
            bar.setLayoutParams(params);
            bar.setMax(maxEQlevel - minEQlevel);
            bar.setProgress(equalizer.getBandLevel(i));
            final short brand=i;
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    equalizer.setBandLevel(brand, (short) (progress+minEQlevel));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            linearLayout.addView(minDbTextView);
            linearLayout.addView(bar);
            linearLayout.addView(maxDbTextView);
            layout.addView(linearLayout);
        }
    }

    private void setupVisualizer() {
        final MyVisualizerView myVisualizerView=new MyVisualizerView(this);
        myVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (120f * getResources().getDisplayMetrics().density)));
        layout.addView(myVisualizerView);
        visualizer=MusicService.visualizer;
        try {
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        }catch (IllegalStateException e){
            MusicService.initVisualizer();
            visualizer=MusicService.visualizer;
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        }
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                myVisualizerView.updateVisualizer(waveform);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);
        visualizer.setEnabled(true);
    }

    private class MyVisualizerView extends View{
        byte[] bytes;
        float[] points;
        Paint paint=new Paint();
        Rect rect=new Rect();
        byte type=0;

        public MyVisualizerView(Context context) {
            super(context);
            bytes=null;
            paint.setStrokeWidth(1f);
            paint.setAntiAlias(true);
            paint.setColor(Color.MAGENTA);
            paint.setStyle(Paint.Style.FILL);
        }
        public void updateVisualizer(byte[] ftt){
            bytes=ftt;
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction()!=MotionEvent.ACTION_DOWN){
                return false;
            }
            type++;
            if (type>=3) {
                type = 0;
            }
            return true;
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (bytes==null){
                return;
            }
            canvas.drawColor(Color.WHITE);
            rect.set(0,0,getWidth(),getHeight());
            switch (type){
                case 0:
                    for (int i=0;i<bytes.length-1;i++){
                        float left=rect.width()*i/(bytes.length-1);
                        float top=rect.height()-(byte)(bytes[i+1]+128)*rect.height()/128;
                        float right=left+1;
                        float bottom=rect.height();
                        canvas.drawRect(left,top,right,bottom,paint);
                    }
                    break;
                case 1:
                    for (int i=0;i<bytes.length-1;i+=18){
                        float left=rect.width()*i/(bytes.length-1);
                        float top=rect.height()-(byte)(bytes[i+1]+128)*rect.height()/128;
                        float right=left+6;
                        float bottom=rect.height();
                        canvas.drawRect(left,top,right,bottom,paint);
                    }
                    break;
                case 2:
                    if (points==null||points.length<bytes.length*4){
                        points=new float[bytes.length*4];
                    }
                    for (int i=0;i<bytes.length-1;i++){
                        points[i*4]=rect.width()*i/(bytes.length-1);
                        points[i*4+1]=rect.height()/2+((byte)(bytes[i]+128))*128/(rect.height()/2);
                        points[i*4+2]=rect.width()*(i+1)/(bytes.length-1);
                        points[i*4+3]=rect.height()/2+((byte)(bytes[i+1]+128))*128/(rect.height()/2);
                    }
                    canvas.drawLines(points, paint);
                    break;
            }
        }
    }
}
