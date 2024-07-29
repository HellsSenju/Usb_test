package com.dynamsoft.usbcamera;

import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Camera implements Runnable{
    private static final String TAG = "!!!";

    public Camera() {
    }

    @Override
    public void run() {
        Log.d(TAG, "RUN");

    }
}
