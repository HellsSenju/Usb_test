package com.dynamsoft.usbcamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class CaptureActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "!!!";

    private List<UsbDevice> devices = null;

    private final Object mSync = new Object();

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     *  by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    private static final int PREVIEW_WIDTH = 1920;

    private static final int PREVIEW_HEIGHT = 1080;

    /**
     * preview mode
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1; // YUV

    private USBMonitor mUSBMonitor;

    private UVCCameraHandler mCameraHandler,
            mCameraHandler2;

    private CameraViewInterface mUVCCameraView, mUVCCameraView2;

//    private SimpleUVCCameraTextureView view;

    private UVCCamera camera;

    private ImageView imageView;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate:");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_capture);
        imageView = (ImageView) findViewById(R.id.frame_image_R);


        //for first device
        mUVCCameraView = (CameraViewInterface)findViewById(R.id.camera_view);
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (double)PREVIEW_HEIGHT);
        ((UVCCameraTextureView)mUVCCameraView).setOnClickListener(mOnClickListener);

        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);

        //for second device
//        mUVCCameraView2 = (CameraViewInterface)findViewById(R.id.camera_view2);
//        mUVCCameraView2.setAspectRatio(PREVIEW_WIDTH / (double)PREVIEW_HEIGHT);
//        ((UVCCameraTextureView)mUVCCameraView2).setOnClickListener(mOnClickListener);
//
//
//        mCameraHandler2 = UVCCameraHandler.createHandler(this, mUVCCameraView2,
//                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);


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
            }
//            else if (id == R.id.camera_view2) {
//                if (mCameraHandler2 != null) {
//                    if (!mCameraHandler2.isOpened()) {
//                        CameraDialog.showDialog(CaptureActivity.this);
//                    } else {
//                        mCameraHandler2.close();
//                    }
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

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(CaptureActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            startConnectedCamera(device.getDeviceName());
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");

            if (!mCameraHandler.isOpened()) {
                Log.d(TAG, "1");
                mCameraHandler.open(ctrlBlock);

                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                Surface s = new Surface(st);
                mCameraHandler.setPreviewCallback(mIFrameCallback);
                mCameraHandler.startPreview(s);
            }
//            else if (!mCameraHandler2.isOpened()) {
//                Log.d(TAG, "2");
//
//                mCameraHandler2.open(ctrlBlock);
//
//                final SurfaceTexture st = mUVCCameraView2.getSurfaceTexture();
//
//                Surface s = new Surface(st);
//
//                mCameraHandler2.setPreviewCallback(mIFrameCallback2);
//                mCameraHandler2.startPreview(s);
//
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

    private final Bitmap bitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.RGB_565);

    private boolean done = false;

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            if(!done) {
                done = true;
                byte[] data = new byte[frame.limit()];

                String s = Base64.encodeToString(data, Base64.DEFAULT);
                Log.d(TAG, s);

//                frame.clear();
//                synchronized (bitmap) {
//                    bitmap.copyPixelsFromBuffer(frame);
//                }
//                imageView.post(mUpdateImageTask); // this is just for showing result
            }
        }
    };

    private final Runnable mUpdateImageTask = new Runnable() {
        @Override
        public void run() {
            synchronized (bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        }
    };


    private final IFrameCallback mIFrameCallback2 = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            Log.i(TAG, "2: " + Arrays.toString(frame.array()));

        }
    };

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
}