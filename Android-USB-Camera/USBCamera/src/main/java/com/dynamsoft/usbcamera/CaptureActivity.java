package com.dynamsoft.usbcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dynamsoft.dbr.BarcodeReader;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CaptureActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "!!!";

    private List<UsbDevice> devices = null;

    private boolean one = false;

    private boolean two = false;

    /**
     * lock
     */
    private final Object mSync = new Object();

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     *  by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
//    private static final int PREVIEW_WIDTH = 640; // 640
    private static final int PREVIEW_WIDTH = 1920;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
//    private static final int PREVIEW_HEIGHT = 480; //480
    private static final int PREVIEW_HEIGHT = 1080;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1; // YUV

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;

//    private USBMonitor mUSBMonitor2;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    private UVCCameraHandler mCameraHandler2;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    private CameraViewInterface mUVCCameraView2;
    /**
     * for open&start / stop&close camera preview
     */
    private ImageButton mCameraButton;
    private ImageButton captureButton;
    private ImageButton captureButton2;

    private TextView resultTextView;

    private ImageView canvasImageView;

    private Timer timer = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate:");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_capture);

        mCameraButton = findViewById(R.id.imageButton);
//        canvasImageView = findViewById(R.id.canvasImageView);
//        canvasImageView.setAdjustViewBounds(true);
//        canvasImageView.setScaleType(ImageView.ScaleType.CENTER);
        resultTextView = findViewById(R.id.resultTextView);
        mCameraButton.setOnClickListener(mOnClickListener);


        //for first device
        mUVCCameraView = (CameraViewInterface)findViewById(R.id.camera_view);
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (double)PREVIEW_HEIGHT);
        ((UVCCameraTextureView)mUVCCameraView).setOnClickListener(mOnClickListener);

        captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(mOnClickListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);

        //for second device
        mUVCCameraView2 = (CameraViewInterface)findViewById(R.id.camera_view2);
        mUVCCameraView2.setAspectRatio(PREVIEW_WIDTH / (double)PREVIEW_HEIGHT);
        ((UVCCameraTextureView)mUVCCameraView2).setOnClickListener(mOnClickListener);

        captureButton2 = findViewById(R.id.capture_button_2);
        captureButton2.setOnClickListener(mOnClickListener);
        mCameraHandler2 = UVCCameraHandler.createHandler(this, mUVCCameraView2,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);


        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG) Log.i(TAG, "onStart");

        mUSBMonitor.register();
        devices = mUSBMonitor.getDeviceList();

        if (mUVCCameraView != null)
            mUVCCameraView.onResume();
        if (mUVCCameraView2 != null)
            mUVCCameraView2.onResume();

//        openDevices();
    }

    @Override
    protected void onStop() {
        if(DEBUG) Log.i(TAG, "onStop");

        mCameraHandler.close();
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();

        mCameraHandler2.close();
        if (mUVCCameraView2 != null)
            mUVCCameraView2.onPause();

        mUSBMonitor.unregister();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {
            if (mCameraHandler != null) {
                mCameraHandler.setPreviewCallback(null); //zhf
                mCameraHandler.release();
                mCameraHandler = null;
            }

            if (mCameraHandler2 != null) {
                mCameraHandler2.setPreviewCallback(null); //zhf
                mCameraHandler2.release();
                mCameraHandler2 = null;
            }

            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        super.onDestroy();
    }

    private void openDevices(){
        USBMonitor.UsbControlBlock ctrlBlock = mUSBMonitor.openDevice(devices.get(0));
        mCameraHandler.open(ctrlBlock);
//        mUVCCameraView.hasSurface();
//        Log.d(TAG, String.valueOf(mUVCCameraView.hasSurface()));

//        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
//        mCameraHandler.startPreview(new Surface(st));
    }

    /**
     * event handler when click camera / capture button
     */
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            int id = view.getId();
            if (id == R.id.camera_view) {
                if (mCameraHandler != null) {
                    if (!mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(CaptureActivity.this);
                    } else {
                        mCameraHandler.close();
                    }
                }
            } else if (id == R.id.camera_view2) {
                if (mCameraHandler2 != null) {
                    if (!mCameraHandler2.isOpened()) {
                        CameraDialog.showDialog(CaptureActivity.this);
                    } else {
                        mCameraHandler2.close();
                    }
                }
            }
//            synchronized (mSync) {
//                if ((mCameraHandler != null) && !mCameraHandler.isOpened()) {
//                    CameraDialog.showDialog(CaptureActivity.this);
//                } else {
//                    mCameraHandler.close();
//                }
//
//                if ((mCameraHandler2 != null) && !mCameraHandler2.isOpened()) {
//                    CameraDialog.showDialog(CaptureActivity.this);
//                } else {
//                    mCameraHandler2.close();
//                }
//            }
        }
    };

    private void startConnectedCamera(String devName){
        if(devices.isEmpty()){
            if (DEBUG) Log.v(TAG, "devices is empty");
            return;
        }
        try{
            for(int i = 0; i < mUSBMonitor.getDeviceList().size(); i++){
                UsbDevice tmp = devices.get(i);
                if(tmp.getDeviceName().equals(devName) && !mUSBMonitor.hasPermission(tmp)){
                    mUSBMonitor.requestPermission(tmp);
                    break;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    private void startPreview() {
        synchronized (mSync) {
            if (mCameraHandler != null && mCameraHandler2 != null) {
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();

                mCameraHandler.setPreviewCallback(mIFrameCallback);

                mCameraHandler.startPreview(new Surface(st));
            }
        }
    }

    private void startPreview2() {
        synchronized (mSync) {
            if (mCameraHandler2 != null) {
                final SurfaceTexture st = mUVCCameraView2.getSurfaceTexture();

                mCameraHandler2.setPreviewCallback(mIFrameCallback2);

                mCameraHandler2.startPreview(new Surface(st));
            }
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(CaptureActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            if (getIntent().getBooleanExtra(Intents.ScanOptions.MANUAL,false) == false){
                startConnectedCamera(device.getDeviceName());
            }
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");

            if (!mCameraHandler.isOpened()) {
                Log.d(TAG, "1");
                Log.d(TAG, "dev " + device.getDeviceName());
                Log.d(TAG, "ctrlBlock " + ctrlBlock);
                Log.d(TAG, "ctrlBlock.getDeviceName() " + ctrlBlock.getDeviceName());
                mCameraHandler.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                mCameraHandler.startPreview(new Surface(st));
            }
            else if (!mCameraHandler2.isOpened()) {
                Log.d(TAG, "2");
                Log.d(TAG, "dev " + device.getDeviceName());
                Log.d(TAG, "ctrlBlock " + ctrlBlock);
                Log.d(TAG, "ctrlBlock.getDeviceName() " + ctrlBlock.getDeviceName());

                mCameraHandler2.open(ctrlBlock);
                final SurfaceTexture st = mUVCCameraView2.getSurfaceTexture();
                mCameraHandler2.startPreview(new Surface(st));
            }
//            synchronized (mSync) {
//                if (mCameraHandler != null && mCameraHandler2 != null) {
//                    USBMonitor.UsbControlBlock ctrlBlock2 = null;
//                    if(!devices.get(0).getDeviceName().equals(ctrlBlock.getDeviceName())){
//                        ctrlBlock2 = mUSBMonitor.openDevice(devices.get(0));
//                    }
//                    else if(!devices.get(1).getDeviceName().equals(ctrlBlock.getDeviceName())){
//                        ctrlBlock2 = mUSBMonitor.openDevice(devices.get(1));
//                    }
//
//                    mCameraHandler.open(ctrlBlock);
//
//                    mCameraHandler2.open(ctrlBlock2);
//
//                    startPreview();
//                }
//            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            if(mCameraHandler != null){
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            // maybe throw java.lang.IllegalStateException: already released
                            mCameraHandler.setPreviewCallback(null);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                        mCameraHandler.close();
                    }
                }, 0);
            }

            if(mCameraHandler2 != null){
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            // maybe throw java.lang.IllegalStateException: already released
                            mCameraHandler2.setPreviewCallback(null);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                        mCameraHandler2.close();
                    }
                }, 0);
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(CaptureActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        synchronized (mSync) {
            return mUSBMonitor;
        }
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
    }

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            Log.i(TAG, String.valueOf(frame));
        }
    };

    private final IFrameCallback mIFrameCallback2 = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            Log.i(TAG, String.valueOf(frame));

        }
    };
}