/*
 * Copyright (C) 2013 OSRF.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava.android_apps.make_a_map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.rosjava.android_remocons.common_tools.apps.RosAppActivity;
import com.google.common.collect.Lists;
import com.laifeng.sopcastdemo.ui.MultiToggleImageButton;
import com.laifeng.sopcastsdk.camera.CameraListener;
import com.laifeng.sopcastsdk.configuration.AudioConfiguration;
import com.laifeng.sopcastsdk.configuration.CameraConfiguration;
import com.laifeng.sopcastsdk.configuration.VideoConfiguration;
import com.laifeng.sopcastsdk.entity.Watermark;
import com.laifeng.sopcastsdk.entity.WatermarkPosition;
import com.laifeng.sopcastsdk.stream.packer.rtmp.RtmpPacker;
import com.laifeng.sopcastsdk.stream.sender.rtmp.RtmpSender;
import com.laifeng.sopcastsdk.ui.CameraLivingView;
import com.laifeng.sopcastsdk.utils.SopCastLog;
import com.laifeng.sopcastsdk.video.effect.GrayEffect;
import com.laifeng.sopcastsdk.video.effect.NullEffect;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLOnAudioFrameListener;
import com.pili.pldroid.player.PLOnBufferingUpdateListener;
import com.pili.pldroid.player.PLOnCompletionListener;
import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnInfoListener;
import com.pili.pldroid.player.PLOnVideoFrameListener;
import com.pili.pldroid.player.PLOnVideoSizeChangedListener;
import com.pili.pldroid.player.widget.PLVideoView;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;
import org.ros.android.view.VirtualJoystickView;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.time.NtpTimeProvider;
import org.ros.time.TimeProvider;
import org.ros.time.WallTimeProvider;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import sensor_msgs.CompressedImage;
import world_canvas_msgs.SaveMapResponse;

import static com.laifeng.sopcastsdk.constant.SopCastConstant.TAG;

/**
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MainActivity extends RosAppActivity {

    private static final int NAME_MAP_DIALOG_ID = 0;

    private RosImageView<sensor_msgs.CompressedImage> cameraView;
    private RosImageView<sensor_msgs.CompressedImage> cameraView1;
    private RosImageView<sensor_msgs.CompressedImage> cameraView2;
    private RosImageView<sensor_msgs.CompressedImage> cameraView3;
    private RosImageView<sensor_msgs.CompressedImage> cameraView4;
    private RosImageView<sensor_msgs.CompressedImage> cameraView5;

    private VirtualJoystickView virtualJoystickView;
    private VisualizationView mapView;
    private ViewGroup mainLayout;
    private ViewGroup sideLayout;
    private ImageButton refreshButton;
    private ImageButton saveButton;
    //    private Button backButton;
    private NodeMainExecutor nodeMainExecutor;
    private NodeConfiguration nodeConfiguration;
    private ProgressDialog waitingDialog;
    private AlertDialog notiDialog;


    private OccupancyGridLayer occupancyGridLayer = null;
    private LaserScanLayer laserScanLayer = null;
    private RobotLayer robotLayer = null;

    private static final String TAG = MainActivity.class.getSimpleName();

    private CameraLivingView mLFLiveView;
    private MultiToggleImageButton mMicBtn;
    private ImageButton mRecordBtn;
    private boolean isRecording;
    private ProgressBar mProgressConnecting;
    private RtmpSender mRtmpSender;

    private PLVideoView plVideoView;
    private MediaController mMediaController;


    public MainActivity() {
        // The RosActivity constructor configures the notification title and
        // ticker
        // messages.
        super("Make a map", "Make a map");

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        String defaultRobotName = getString(R.string.default_robot);
        String defaultAppName = getString(R.string.default_app);
        setDefaultMasterName(defaultRobotName);
        setDefaultAppName(defaultAppName);
        setDashboardResource(R.id.top_bar);
        setMainWindowResource(R.layout.my_layout);

        super.onCreate(savedInstanceState);

        cameraView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
        cameraView.setMessageType(sensor_msgs.CompressedImage._TYPE);
        cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        cameraView1 = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image1);
        cameraView1.setMessageType(sensor_msgs.CompressedImage._TYPE);
        cameraView1.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        cameraView2 = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image2);
        cameraView2.setMessageType(sensor_msgs.CompressedImage._TYPE);
        cameraView2.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        cameraView3 = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image3);
        cameraView3.setMessageType(sensor_msgs.CompressedImage._TYPE);
        cameraView3.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        cameraView4 = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image4);
        cameraView4.setMessageType(sensor_msgs.CompressedImage._TYPE);
        cameraView4.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        cameraView5 = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image5);
        cameraView5.setMessageType(sensor_msgs.CompressedImage._TYPE);
        cameraView5.setMessageToBitmapCallable(new BitmapFromCompressedImage());

//        initRosImageView(cameraView, R.id.image);
//        initRosImageView(cameraView1, R.id.image1);
//        initRosImageView(cameraView2, R.id.image2);
//        initRosImageView(cameraView3, R.id.image3);
//        initRosImageView(cameraView4, R.id.image4);
//        initRosImageView(cameraView5, R.id.image5);

//        cameraView1 = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image1);
//        cameraView1.setMessageType(sensor_msgs.CompressedImage._TYPE);
//        cameraView1.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        virtualJoystickView = (VirtualJoystickView) findViewById(R.id.virtual_joystick);
        refreshButton = (ImageButton) findViewById(R.id.refresh_button);
        saveButton = (ImageButton) findViewById(R.id.save_map);
//        backButton = (Button) findViewById(R.id.back_button);

        mapView = (VisualizationView) findViewById(R.id.map_view);
        mapView.onCreate(Lists.<Layer>newArrayList());

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
                Toast.makeText(MainActivity.this, "refreshing map...",
                        Toast.LENGTH_SHORT).show();
                mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(NAME_MAP_DIALOG_ID);

            }

        });

//        backButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onBackPressed();
//            }
//        });

        mapView.getCamera().jumpToFrame((String) params.get("map_frame", getString(R.string.map_frame)));

        mainLayout = (ViewGroup) findViewById(R.id.main_layout);
        sideLayout = (ViewGroup) findViewById(R.id.side_layout);

        initViews();
        initListeners();
        initLiveView();
        initRtmpView();
    }

//    private void initRosImageView(RosImageView<CompressedImage> rosImageView, int imageId) {
//        rosImageView = (RosImageView<sensor_msgs.CompressedImage>) findViewById(imageId);
//        rosImageView.setMessageType(sensor_msgs.CompressedImage._TYPE);
//        rosImageView.setMessageToBitmapCallable(new BitmapFromCompressedImage());
//    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        Button button;

        switch (id) {
            case NAME_MAP_DIALOG_ID:
                dialog = new Dialog(this);
                dialog.setContentView(R.layout.name_map_dialog);
                dialog.setTitle("Save Map");
                final EditText nameField = (EditText) dialog
                        .findViewById(R.id.name_editor);

                nameField.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(final View view, int keyCode,
                                         KeyEvent event) {
                        if (event.getAction() == KeyEvent.ACTION_DOWN
                                && keyCode == KeyEvent.KEYCODE_ENTER) {
                            safeShowWaitingDialog("Saving map...");
                            try {
                                final MapManager mapManager = new MapManager(MainActivity.this, remaps);
                                String name = nameField.getText().toString();
                                if (name != null) {
                                    mapManager.setMapName(name);
                                }
                                mapManager.setNameResolver(getMasterNameSpace());
                                mapManager.registerCallback(new MapManager.StatusCallback() {
                                    @Override
                                    public void timeoutCallback() {
                                        safeDismissWaitingDialog();
                                        safeShowNotiDialog("Error", "Timeout");
                                    }

                                    @Override
                                    public void onSuccessCallback(SaveMapResponse arg0) {
                                        safeDismissWaitingDialog();
                                        safeShowNotiDialog("Success", "Map saving success!");
                                    }

                                    @Override
                                    public void onFailureCallback(Exception e) {
                                        safeDismissWaitingDialog();
                                        safeShowNotiDialog("Error", e.getMessage());
                                    }
                                });

                                nodeMainExecutor.execute(mapManager,
                                        nodeConfiguration.setNodeName("android/save_map"));

                            } catch (Exception e) {
                                e.printStackTrace();
                                safeShowNotiDialog("Error", "Error during saving: " + e.toString());
                            }

                            removeDialog(NAME_MAP_DIALOG_ID);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                button = (Button) dialog.findViewById(R.id.cancel_button);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeDialog(NAME_MAP_DIALOG_ID);
                    }
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    private void safeDismissWaitingDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (waitingDialog != null) {
                    waitingDialog.dismiss();
                    waitingDialog = null;
                }
            }
        });
    }

    private void safeShowWaitingDialog(final CharSequence message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (waitingDialog != null) {
                    waitingDialog.dismiss();
                    waitingDialog = null;
                }
                waitingDialog = ProgressDialog.show(MainActivity.this, "",
                        message, true);
            }
        });
    }

    private void safeShowNotiDialog(final String title, final CharSequence message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (notiDialog != null) {
                    notiDialog.dismiss();
                    notiDialog = null;
                }
                if (waitingDialog != null) {
                    waitingDialog.dismiss();
                    waitingDialog = null;
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(
                        MainActivity.this);
                dialog.setTitle(title);
                dialog.setMessage(message);
                dialog.setNeutralButton("Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dlog, int i) {
                                dlog.dismiss();
                            }
                        });
                notiDialog = dialog.show();
            }
        });
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        super.init(nodeMainExecutor);
        this.nodeMainExecutor = nodeMainExecutor;

        nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory
                .newNonLoopback().getHostAddress(), getMasterUri());

        String joyTopic = remaps.get(getString(R.string.joystick_topic));

        String camTopic = remaps.get(getString(R.string.camera_topic));
        String camTopic1 = remaps.get(getString(R.string.camera_topic1));
        String camTopic2 = remaps.get(getString(R.string.camera_topic2));
        String camTopic3 = remaps.get(getString(R.string.camera_topic3));
        String camTopic4 = remaps.get(getString(R.string.camera_topic4));
        String camTopic5 = remaps.get(getString(R.string.camera_topic5));

        NameResolver appNameSpace = getMasterNameSpace();
        joyTopic = appNameSpace.resolve(joyTopic).toString();

        camTopic = appNameSpace.resolve(camTopic).toString();
        camTopic1 = appNameSpace.resolve(camTopic1).toString();
        camTopic2 = appNameSpace.resolve(camTopic2).toString();
        camTopic3 = appNameSpace.resolve(camTopic3).toString();
        camTopic4 = appNameSpace.resolve(camTopic4).toString();
        camTopic5 = appNameSpace.resolve(camTopic5).toString();

        cameraView.setTopicName(camTopic);
        cameraView1.setTopicName(camTopic1);
        cameraView2.setTopicName(camTopic2);
        cameraView3.setTopicName(camTopic3);
        cameraView4.setTopicName(camTopic4);
        cameraView5.setTopicName(camTopic5);

        virtualJoystickView.setTopicName(joyTopic);

        nodeMainExecutor.execute(cameraView, nodeConfiguration.setNodeName("android/camera_view"));
        nodeMainExecutor.execute(cameraView1, nodeConfiguration.setNodeName("android/camera_view1"));
        nodeMainExecutor.execute(cameraView2, nodeConfiguration.setNodeName("android/camera_view2"));
        nodeMainExecutor.execute(cameraView3, nodeConfiguration.setNodeName("android/camera_view3"));
        nodeMainExecutor.execute(cameraView4, nodeConfiguration.setNodeName("android/camera_view4"));
        nodeMainExecutor.execute(cameraView5, nodeConfiguration.setNodeName("android/camera_view5"));

        nodeMainExecutor.execute(virtualJoystickView, nodeConfiguration.setNodeName("android/virtual_joystick"));

        ViewControlLayer viewControlLayer = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView,
                mapView, mainLayout, sideLayout, params);

        ViewControlLayer viewControlLayer1 = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView1,
                mapView, mainLayout, sideLayout, params);

        ViewControlLayer viewControlLayer2 = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView2,
                mapView, mainLayout, sideLayout, params);

        ViewControlLayer viewControlLayer3 = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView3,
                mapView, mainLayout, sideLayout, params);

        ViewControlLayer viewControlLayer4 = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView4,
                mapView, mainLayout, sideLayout, params);

        ViewControlLayer viewControlLayer5 = new ViewControlLayer(this,
                nodeMainExecutor.getScheduledExecutorService(), cameraView5,
                mapView, mainLayout, sideLayout, params);


        String mapTopic = remaps.get(getString(R.string.map_topic));
        String scanTopic = remaps.get(getString(R.string.scan_topic));
        String robotFrame = (String) params.get("robot_frame", getString(R.string.robot_frame));

        occupancyGridLayer = new OccupancyGridLayer(appNameSpace.resolve(mapTopic).toString());
        laserScanLayer = new LaserScanLayer(appNameSpace.resolve(scanTopic).toString());
        robotLayer = new RobotLayer(robotFrame);

        mapView.addLayer(viewControlLayer);
        mapView.addLayer(viewControlLayer1);
        mapView.addLayer(viewControlLayer2);
        mapView.addLayer(viewControlLayer3);
        mapView.addLayer(viewControlLayer4);
        mapView.addLayer(viewControlLayer5);

        mapView.addLayer(occupancyGridLayer);
        mapView.addLayer(laserScanLayer);
        mapView.addLayer(robotLayer);

        mapView.init(nodeMainExecutor);
        viewControlLayer.addListener(new CameraControlListener() {
            @Override
            public void onZoom(float focusX, float focusY, float factor) {
            }

            @Override
            public void onDoubleTap(float x, float y) {
            }

            @Override
            public void onTranslate(float distanceX, float distanceY) {
            }

            @Override
            public void onRotate(float focusX, float focusY, double deltaAngle) {
            }
        });

        TimeProvider timeProvider = null;
        try {
            NtpTimeProvider ntpTimeProvider = new NtpTimeProvider(
                    InetAddressFactory.newFromHostString("pool.ntp.org"),
                    nodeMainExecutor.getScheduledExecutorService());
            ntpTimeProvider.startPeriodicUpdates(1, TimeUnit.MINUTES);
            timeProvider = ntpTimeProvider;
        } catch (Throwable t) {
            Log.w("MakeAMap", "Unable to use NTP provider, using Wall Time. Error: " + t.getMessage(), t);
            timeProvider = new WallTimeProvider();
        }
        nodeConfiguration.setTimeProvider(timeProvider);

        nodeMainExecutor.execute(mapView, nodeConfiguration.setNodeName("android/map_view"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.stop_app);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case 0:
                onDestroy();
                break;
        }
        return true;
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
                    Toast.makeText(MainActivity.this, "stop living", Toast.LENGTH_SHORT).show();
                    mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
                    mLFLiveView.stop();
                    isRecording = false;
                } else {
                    mRtmpSender.setAddress("rtmp://192.168.0.104:1935/live/rtmpdemo");
                    mProgressConnecting.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "start connecting", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MainActivity.this, "start living fail", Toast.LENGTH_SHORT).show();
                mLFLiveView.stop();
            }

            @Override
            public void startSuccess() {
                Toast.makeText(MainActivity.this, "start living", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MainActivity.this, "fail to live", Toast.LENGTH_SHORT).show();
            mRecordBtn.setBackgroundResource(R.mipmap.ic_record_start);
            mLFLiveView.stop();
            isRecording = false;
        }

        @Override
        public void onPublishFail() {
            mProgressConnecting.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, "fail to publish stream", Toast.LENGTH_SHORT).show();
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
                    showToastTips(MainActivity.this, "first video render time: " + extra + "ms");
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
                    showToastTips(MainActivity.this, "failed to open player !");
                    break;
                case PLOnErrorListener.ERROR_CODE_SEEK_FAILED:
                    showToastTips(MainActivity.this, "failed to seek !");
                    return true;
                case PLOnErrorListener.ERROR_CODE_CACHE_FAILED:
                    showToastTips(MainActivity.this, "failed to cache url !");
                    break;
                default:
                    showToastTips(MainActivity.this, "unknown error !");
                    break;
            }
            return true;
        }
    };

    private PLOnCompletionListener mOnCompletionListener = new PLOnCompletionListener() {
        @Override
        public void onCompletion() {
            Log.i(TAG, "Play Completed !");
            showToastTips(MainActivity.this, "Play Completed !");
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
