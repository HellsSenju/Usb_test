package com.example.testusb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "!!!";

    private UsbManager mUsbManager;

    private HashMap<String, UsbDevice> devices;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        Log.i(TAG, "START");

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        devices = manager.getDeviceList();
        Log.d(TAG, "devices " + devices.size());


//        UsbDevice cam1 = deviceList.get("/dev/bus/usb/001/004");
//        UsbDevice cam2 = deviceList.get("/dev/bus/usb/001/003");

        UsbDevice mUsbDevice = findDevice();
        if(mUsbDevice == null){
            Log.d(TAG, "mUsbDevice is null");
            return;
        }

        Log.d(TAG, "interf count " + mUsbDevice.getInterfaceCount());

        UsbInterface mUsbInterface = findInterface(mUsbDevice);
        if(mUsbInterface == null) {
            Log.w(TAG, "mUsbInterface is null");
            return;
        }

        UsbEndpoint outEndpoint = null;
        UsbEndpoint inEndpoint = null;

        // Получаем endpoint’ы
        // 128 - USB_DIR_IN - device to host
        for (int nEp = 0; nEp < mUsbInterface.getEndpointCount(); nEp++) {
            UsbEndpoint tmpEndpoint = mUsbInterface.getEndpoint(nEp);
            Log.d(TAG, "tmpEndpoint type " + tmpEndpoint.getType());
            Log.d(TAG, "tmpEndpoint direction " + tmpEndpoint.getDirection());

//            if (tmpEndpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
//                continue;

            if ((outEndpoint == null)
                    && (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_OUT)) {
                outEndpoint = tmpEndpoint;
            } else if ((inEndpoint == null)
                    && (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_IN)) {
                inEndpoint = tmpEndpoint;
            }
        }

        if(inEndpoint == null) {
            Log.w(TAG, "inEndpoint is null");
        }

        UsbDeviceConnection mConnection = mUsbManager.openDevice(mUsbDevice);
        if (mConnection == null){
            Log.w(TAG, "mConnection is null");
            return;
        }
        mConnection.claimInterface (mUsbInterface, true);

        byte[] bytes = { 0 };
        int TIMEOUT = 0;

        final int receive_RequestType = (1 << 7) | (1 << 5) | (0); //USBRQ_DIR_DEVICE_TO_HOST USBRQ_TYPE_CLASS USBRQ_RCPT_DEVICE

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = { 0 };
                int a = mConnection.controlTransfer(receive_RequestType, 0x01, 0, 0, bytes, 1, 0);
                if (a > 0) {
                    Log.i(TAG, "SOME RES " + (bytes[0] & 0xFF));
                }
            }
        }, 20);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /*
        USB_CLASS_VIDEO = 14
        у нас класс девайса = 239, но он имеет несколько интерфейсов, один из которых с id = 0 и
        классом = 14 и есть камера
     */
    UsbDevice findDevice() {
        if(devices.isEmpty()){
            Log.d(TAG, "devices is empty");
            return null;
        }

        for (UsbDevice usbDevice: devices.values()) {
            if (usbDevice.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) {
                return usbDevice;
            } else {
                UsbInterface usbInterface = findInterface(usbDevice);
                if (usbInterface != null) return usbDevice;
            }
        }
        return null;
    }

    UsbInterface findInterface(UsbDevice usbDevice) {
        for (int nIf = 0; nIf < usbDevice.getInterfaceCount(); nIf++) {
            UsbInterface usbInterface = usbDevice.getInterface(nIf);
            if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) {
                return usbInterface;
            }
        }
        return null;
    }
}