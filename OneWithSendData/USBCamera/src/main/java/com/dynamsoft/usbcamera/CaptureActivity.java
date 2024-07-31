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
    private static final int PREVIEW_MODE = 1;

    private USBMonitor mUSBMonitor;

    private UVCCameraHandler mCameraHandler;

    private CameraViewInterface mUVCCameraView;



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate:");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_capture);

        mUVCCameraView = (CameraViewInterface)findViewById(R.id.camera_view);
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (double)PREVIEW_HEIGHT);
        ((UVCCameraTextureView)mUVCCameraView).setOnClickListener(mOnClickListener);

        synchronized (mSync) {
            mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
            mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                    USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DEBUG) Log.i(TAG, "onStart");

        synchronized (mSync) {
            mUSBMonitor.register();
        }
        if (mUVCCameraView != null) {
            mUVCCameraView.onResume();
        }
    }

    @Override
    protected void onStop() {
        if(DEBUG) Log.i(TAG, "onStop");

        synchronized (mSync) {
            mCameraHandler.close();
            mUSBMonitor.unregister();
        }
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {
            if (mCameraHandler != null) {
                mCameraHandler.setPreviewCallback(null);
                mCameraHandler.release();
                mCameraHandler = null;
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
            if (view.getId() == R.id.camera_view) {
                if (mCameraHandler != null) {
                    if (!mCameraHandler.isOpened()) {
                        CameraDialog.showDialog(CaptureActivity.this);
                    } else {
                        mCameraHandler.close();
                    }
                }
            }
        }
    };

    private void startConnectedCamera(){
        if(mUSBMonitor.getDeviceList().isEmpty()){
            if (DEBUG) Log.v(TAG, "no device");
            return;
        }
        try{
            UsbDevice device = mUSBMonitor.getDeviceList().get(0);
            if(!mUSBMonitor.hasPermission(device))
                mUSBMonitor.requestPermission(device);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(CaptureActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            startConnectedCamera();
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");

            synchronized (mSync) {
                if (mCameraHandler != null) {
                    mCameraHandler.open(ctrlBlock);
                    if (mCameraHandler != null) {
                        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                        mCameraHandler.setPreviewCallback(mIFrameCallback);
                        mCameraHandler.startPreview(new Surface(st));
                    }
                }
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");

            synchronized (mSync) {
                if (mCameraHandler != null) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                // maybe throw java.lang.IllegalStateException: already released
                                mCameraHandler.setPreviewCallback(null); //zhf
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                            mCameraHandler.close();
                        }
                    }, 0);
                }
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


    private boolean done = false;

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            if(!done) {
                Log.d(TAG, "OnFrame");
                done = true;


                byte[] data = new byte[frame.remaining()];
                frame.get(data);

                String s = Base64.encodeToString(data, Base64.DEFAULT);


                Log.d("???", s);
            }
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