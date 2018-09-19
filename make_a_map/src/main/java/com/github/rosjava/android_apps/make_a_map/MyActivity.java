package com.github.rosjava.android_apps.make_a_map;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.laifeng.sopcastdemo.ui.MultiToggleImageButton;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.stream.packer.rtmp.RtmpPacker;
import com.laifeng.sopcastsdk.stream.sender.rtmp.RtmpSender;
import com.laifeng.sopcastsdk.ui.CameraLivingView;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLOnAudioFrameListener;
import com.pili.pldroid.player.PLOnBufferingUpdateListener;
import com.pili.pldroid.player.PLOnCompletionListener;
import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnInfoListener;
import com.pili.pldroid.player.PLOnVideoFrameListener;
import com.pili.pldroid.player.PLOnVideoSizeChangedListener;
import com.pili.pldroid.player.widget.PLVideoView;

import org.ros.android.RosActivity;
import org.ros.node.NodeMainExecutor;

import java.util.Arrays;

public class MyActivity extends MainActivity {

    private static final String TAG = MyActivity.class.getSimpleName();

    private CameraLivingView mLFLiveView;
    private MultiToggleImageButton mMicBtn;
    private ImageButton mRecordBtn;
    private boolean isRecording;
    private ProgressBar mProgressConnecting;
    private RtmpSender mRtmpSender;

    private PLVideoView plVideoView;
    private MediaController mMediaController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_layout);
        initViews();
        initListeners();
        initLiveView();
        initRtmpView();
    }

    private void initRtmpView() {
        String videoPath = "rtmp://192.168.0.104:1935/live/rtmpstream";

        plVideoView = (PLVideoView) findViewById(R.id.pl_video_view);

        View loadingView = findViewById(R.id.progressConnecting);
//        plVideoView.setBufferingIndicator(loadingView);

        //mediaCodec
        int codec = AVOptions.MEDIA_CODEC_HW_DECODE;
        AVOptions options = new AVOptions();
        // the unit of timeout is ms
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        // 1 -> hw codec enable, 0 -> disable [recommended]
        options.setInteger(AVOptions.KEY_MEDIACODEC, codec);
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1);
//        options.setString(AVOptions.KEY_DNS_SERVER, "127.0.0.1");
        options.setInteger(AVOptions.KEY_LOG_LEVEL, 0);

        plVideoView.setAVOptions(options);

        // Set some listeners
        plVideoView.setOnInfoListener(mOnInfoListener);
        plVideoView.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        plVideoView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        plVideoView.setOnCompletionListener(mOnCompletionListener);
        plVideoView.setOnErrorListener(mOnErrorListener);
        plVideoView.setOnVideoFrameListener(mOnVideoFrameListener);
        plVideoView.setOnAudioFrameListener(mOnAudioFrameListener);

        plVideoView.setVideoPath(videoPath);
        plVideoView.setLooping(false);

        // You can also use a custom `MediaController` widget
        mMediaController = new MediaController(this, false, true);
        mMediaController.setOnClickSpeedAdjustListener(mOnClickSpeedAdjustListener);
        plVideoView.setMediaController(mMediaController);

    }

    private void initViews() {
        mLFLiveView = new CameraLivingView(this);
        mMicBtn = (MultiToggleImageButton) findViewById(R.id.record_mic_button);
        mRecordBtn = (ImageButton) findViewById(R.id.btnRecord);
        mProgressConnecting = (ProgressBar) findViewById(R.id.progressConnecting);
    }

    private void initListeners() {
        mMicBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mLFLiveView.mute(true);
            }
        });
        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    mProgressConnecting.setVisibility(View.GONE);
                    Toast.makeText(MyActivity.this, "stop living", Toast.LENGTH_SHORT).show();
                    mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
                    mLFLiveView.stop();
                    isRecording = false;
                } else {
                    mRtmpSender.setAddress("rtmp://192.168.0.104:1935/live/rtmpdemo");
                    mProgressConnecting.setVisibility(View.VISIBLE);
                    Toast.makeText(MyActivity.this, "start connecting", Toast.LENGTH_SHORT).show();
                    mRecordBtn.setBackgroundResource(R.mipmap.ic_record_stop);
                    mRtmpSender.connect();
                    isRecording = true;
                }
            }
        });
    }

    private void initLiveView() {
        SopCastLog.isOpen(true);
        mLFLiveView.init();

        //初始化flv打包器
        RtmpPacker packer = new RtmpPacker();
        packer.initAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mLFLiveView.setPacker(packer);
        //设置发送器
        mRtmpSender = new RtmpSender();
        mRtmpSender.setVideoParams(640, 360);
        mRtmpSender.setAudioParams(AudioConfiguration.DEFAULT_FREQUENCY, 16, false);
        mRtmpSender.setSenderListener(mSenderListener);
        mLFLiveView.setSender(mRtmpSender);
        mLFLiveView.setLivingStartListener(new CameraLivingView.LivingStartListener() {
            @Override
            public void startError(int error) {
                Toast.makeText(MyActivity.this, "start living fail", Toast.LENGTH_SHORT).show();
                mLFLiveView.stop();
            }

            @Override
            public void startSuccess() {
                Toast.makeText(MyActivity.this, "start living", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RtmpSender.OnSenderListener mSenderListener = new RtmpSender.OnSenderListener() {
        @Override
        public void onConnecting() {

        }

        @Override
        public void onConnected() {
            mProgressConnecting.setVisibility(View.GONE);
            mLFLiveView.start();
        }

        @Override
        public void onDisConnected() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(MyActivity.this, "fail to live", Toast.LENGTH_SHORT).show();
            mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
            mLFLiveView.stop();
            isRecording = false;
        }

        @Override
        public void onPublishFail() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(MyActivity.this, "fail to publish stream", Toast.LENGTH_SHORT).show();
            mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
            isRecording = false;
        }

        @Override
        public void onNetGood() {
        }

        @Override
        public void onNetBad() {
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mLFLiveView.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        plVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMediaController.getWindow().dismiss();
        plVideoView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLFLiveView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLFLiveView.stop();
        mLFLiveView.release();
        plVideoView.stopPlayback();
    }


    private PLOnInfoListener mOnInfoListener = new PLOnInfoListener() {
        @Override
        public void onInfo(int what, int extra) {
            Log.i(TAG, "OnInfo, what = " + what + ", extra = " + extra);
            switch (what) {
                case PLOnInfoListener.MEDIA_INFO_BUFFERING_START:
                    break;
                case PLOnInfoListener.MEDIA_INFO_BUFFERING_END:
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_RENDERING_START:
                    showToastTips(MyActivity.this, "first video render time: " + extra + "ms");
                    Log.i(TAG, "Response: " + plVideoView.getResponseInfo());
                    break;
                case PLOnInfoListener.MEDIA_INFO_AUDIO_RENDERING_START:
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_FRAME_RENDERING:
                    Log.i(TAG, "video frame rendering, ts = " + extra);
                    break;
                case PLOnInfoListener.MEDIA_INFO_AUDIO_FRAME_RENDERING:
                    Log.i(TAG, "audio frame rendering, ts = " + extra);
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_GOP_TIME:
                    Log.i(TAG, "Gop Time: " + extra);
                    break;
                case PLOnInfoListener.MEDIA_INFO_SWITCHING_SW_DECODE:
                    Log.i(TAG, "Hardware decoding failure, switching software decoding!");
                    break;
                case PLOnInfoListener.MEDIA_INFO_METADATA:
                    Log.i(TAG, plVideoView.getMetadata().toString());
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_BITRATE:
                case PLOnInfoListener.MEDIA_INFO_VIDEO_FPS:
                    updateStatInfo();
                    break;
                case PLOnInfoListener.MEDIA_INFO_CONNECTED:
                    Log.i(TAG, "Connected !");
                    break;
                case PLOnInfoListener.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                    Log.i(TAG, "Rotation changed: " + extra);
                    break;
                case PLOnInfoListener.MEDIA_INFO_LOOP_DONE:
                    Log.i(TAG, "Loop done");
                    break;
                case PLOnInfoListener.MEDIA_INFO_CACHE_DOWN:
                    Log.i(TAG, "Cache done");
                    break;
                default:
                    break;
            }
        }
    };

    private PLOnErrorListener mOnErrorListener = new PLOnErrorListener() {
        @Override
        public boolean onError(int errorCode) {
            Log.e(TAG, "Error happened, errorCode = " + errorCode);
            switch (errorCode) {
                case PLOnErrorListener.ERROR_CODE_IO_ERROR:
                    /**
                     * SDK will do reconnecting automatically
                     */
                    Log.e(TAG, "IO Error!");
                    return false;
                case PLOnErrorListener.ERROR_CODE_OPEN_FAILED:
                    showToastTips(MyActivity.this, "failed to open player !");
                    break;
                case PLOnErrorListener.ERROR_CODE_SEEK_FAILED:
                    showToastTips(MyActivity.this, "failed to seek !");
                    return true;
                case PLOnErrorListener.ERROR_CODE_CACHE_FAILED:
                    showToastTips(MyActivity.this, "failed to cache url !");
                    break;
                default:
                    showToastTips(MyActivity.this, "unknown error !");
                    break;
            }
            return true;
        }
    };

    private PLOnCompletionListener mOnCompletionListener = new PLOnCompletionListener() {
        @Override
        public void onCompletion() {
            Log.i(TAG, "Play Completed !");
            showToastTips(MyActivity.this, "Play Completed !");
            //finish();
        }
    };

    private PLOnBufferingUpdateListener mOnBufferingUpdateListener = new PLOnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(int precent) {
            Log.i(TAG, "onBufferingUpdate: " + precent);
        }
    };

    private PLOnVideoSizeChangedListener mOnVideoSizeChangedListener = new PLOnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(int width, int height) {
            Log.i(TAG, "onVideoSizeChanged: width = " + width + ", height = " + height);
        }
    };

    private PLOnVideoFrameListener mOnVideoFrameListener = new PLOnVideoFrameListener() {
        @Override
        public void onVideoFrameAvailable(byte[] data, int size, int width, int height, int format, long ts) {
            Log.i(TAG, "onVideoFrameAvailable: " + size + ", " + width + " x " + height + ", " + format + ", " + ts);
            if (format == PLOnVideoFrameListener.VIDEO_FORMAT_SEI && bytesToHex(Arrays.copyOfRange(data, 19, 23)).equals("74733634")) {
                // If the RTMP stream is from Qiniu
                // Add &addtssei=true to the end of URL to enable SEI timestamp.
                // Format of the byte array:
                // 0:       SEI TYPE                    This is part of h.264 standard.
                // 1:       unregistered user data      This is part of h.264 standard.
                // 2:       payload length              This is part of h.264 standard.
                // 3-18:    uuid                        This is part of h.264 standard.
                // 19-22:   ts64                        Magic string to mark this stream is from Qiniu
                // 23-30:   timestamp                   The timestamp
                // 31:      0x80                        Magic hex in ffmpeg
                Log.i(TAG, " timestamp: " + Long.valueOf(bytesToHex(Arrays.copyOfRange(data, 23, 31)), 16));
            }
        }
    };

    private PLOnAudioFrameListener mOnAudioFrameListener = new PLOnAudioFrameListener() {
        @Override
        public void onAudioFrameAvailable(byte[] data, int size, int samplerate, int channels, int datawidth, long ts) {
            Log.i(TAG, "onAudioFrameAvailable: " + size + ", " + samplerate + ", " + channels + ", " + datawidth + ", " + ts);
        }
    };

    private MediaController.OnClickSpeedAdjustListener mOnClickSpeedAdjustListener = new MediaController.OnClickSpeedAdjustListener() {
        @Override
        public void onClickNormal() {
            // 0x0001/0x0001 = 2
            plVideoView.setPlaySpeed(0X00010001);
        }

        @Override
        public void onClickFaster() {
            // 0x0002/0x0001 = 2
            plVideoView.setPlaySpeed(0X00020001);
        }

        @Override
        public void onClickSlower() {
            // 0x0001/0x0002 = 0.5
            plVideoView.setPlaySpeed(0X00010002);
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void updateStatInfo() {
        long bitrate = plVideoView.getVideoBitrate() / 1024;
        final String stat = bitrate + "kbps, " + plVideoView.getVideoFps() + "fps";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mStatInfoTextView.setText(stat);
                Log.e(TAG, stat);
            }
        });
    }

    private void showToastTips(final Context context, final String tips) {
        Toast.makeText(context, tips, Toast.LENGTH_SHORT).show();
    }
}
