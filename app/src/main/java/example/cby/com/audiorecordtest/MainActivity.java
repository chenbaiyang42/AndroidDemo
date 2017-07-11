package example.cby.com.audiorecordtest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Button initRecord;
    private Button startRecord;
    private Button stop;
    private Button release;
    private TextView attitude;

    int sampleRate = 16000;
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;// 单声道
    AudioRecord	mAudioRecord;
    int			mBufSize;
    int			mCurAmplitude = 0;
    RecordThread mRecordThread;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int AUDIO_FORMAT_IN_BYTE = 2;
    private int bs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();

    }

    private void initLayout() {
        initRecord = (Button) findViewById(R.id.initRecord);
        startRecord = (Button) findViewById(R.id.startRecord);
        stop = (Button) findViewById(R.id.stop);
        release = (Button) findViewById(R.id.release);
        attitude = (TextView) findViewById(R.id.amtitude);

        initRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initRecord();
            }
        });

        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecordThread();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });

        release.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioRecord.release();
            }
        });
    }

    private void startRecordThread() {
        initRecord();
        if(mRecordThread == null){
            mRecordThread = new RecordThread();
        }
        mRecordThread.start();
    }

    public void initRecord() {
        int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AUDIO_FORMAT);
        mBufSize = sampleRate * 20 / 1000 * channelConfig * AUDIO_FORMAT_IN_BYTE;
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, AUDIO_FORMAT, 8 * minBufSize);
        Log.e("", "state: " + mAudioRecord.getState());
    }
    public void stopRecord(){
        mRecordThread.exit = true;
        try {
            mRecordThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mRecordThread = null;
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    class RecordThread extends Thread {
        public volatile boolean exit = false;
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            while (!exit) {
                if (mAudioRecord == null) {
                   initRecord();
                }
                if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    mAudioRecord.startRecording();
                }
                byte[] buffer = new byte[mBufSize];
                int len = mAudioRecord.read(buffer, 0, buffer.length);
                setCurAmplitude(buffer, len);
                Log.i("Amplitude", "===== " + getAmplitude());
            }
        }

    }

    private void setCurAmplitude(byte[] readBuf, int read) {
        mCurAmplitude = 0;
        for (int i = 0; i < read / 2; i++) {
            short curSample = (short) ((readBuf[i * 2] & 0xFF) | (readBuf[i * 2 + 1] << 8));
            if (curSample > mCurAmplitude) {
                mCurAmplitude = curSample;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                attitude.setText(mCurAmplitude+"");
            }
        });

    }
    public int getAmplitude() {
        return mCurAmplitude;
    }
}
