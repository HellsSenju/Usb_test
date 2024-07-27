package com.example.testusb;

import static android.hardware.usb.UsbConstants.USB_ENDPOINT_XFERTYPE_MASK;
import static android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_INT;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "!!!";

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";

    private static final int MY_CAMERA_REQUEST_CODE = 100;

    public int REPORT_SIZE = 0;

    private UsbManager manager;

    private UsbAccessory accessory;

    private UsbDevice device;

    private UsbSerialDevice serialPort;

    private UsbInterface usbIntr;

    private UsbEndpoint inEndpoint; //device to host

    private UsbDeviceConnection connection;

    private UsbRequest request = null;

    private HashMap<String, UsbDevice> devices;

    PendingIntent permissionIntent;

    Handler handler;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                Log.d(TAG, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            if(manager.hasPermission(device)){
                Log.d(TAG, "Права уже есть");
            }
            else if(ACTION_USB_PERMISSION.equals(intent.getAction())){
                boolean granted =
                        Objects.requireNonNull(intent.getExtras()).getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (!granted) {
                    Log.d(TAG, "Права не были получены");
                    return;
                }
            }

            //                serialSetup();
            try {
                setup();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

//                try {
//                    test();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
        }
    };

    // https://github.com/felHR85/UsbSerial
    public void serialSetup(){
        connection = manager.openDevice(device);
        if(connection == null)
            Log.d(TAG, "connection is null");
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialPort != null) {
            if (serialPort.open()) { //Set Serial Connection Parameters.

                serialPort.setBaudRate(9600);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialPort.read(mCallback);

            }
            else {
                Log.d(TAG, "PORT NOT OPEN");
            }
        }
        else {
            Log.d(TAG, "PORT IS NULL");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "START");

        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessoryList = manager.getAccessoryList();
        Log.d(TAG, "accessoryList " + Arrays.toString(accessoryList));

        devices = manager.getDeviceList();

        Log.d(TAG, "devices " + devices.size());

//        Log.d(TAG, String.valueOf(devices));

        permissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);

        setDevice();
        if(device == null){
            Log.w(TAG, "device is null");
            return;
        }

        manager.requestPermission(device, permissionIntent);
    }

    // https://github.com/mik3y/usb-serial-for-android/tree/master
    void test() throws IOException {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(7119, 8835, FtdiSerialDriver.class);

        UsbSerialProber prober = new UsbSerialProber(customTable);

        List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "availableDrivers is empty ");
            return;
        }
        Log.d(TAG, "availableDrivers: " + availableDrivers);

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return;
        }

        UsbSerialPort port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        Log.d(TAG, "ports: " + driver.getPorts());

//        port.read()
//        port.open(connection);
//        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
    }

    void setup_test(){
        setInterface();
        if(usbIntr == null) {
            Log.w(TAG, "mUsbInterface is null");
            return;
        }

        inEndpoint = null;

        // Получаем endpoint’ы
        // 128 - USB_DIR_IN - device to host
        for (int nEp = 0; nEp < usbIntr.getEndpointCount(); nEp++) {
            UsbEndpoint tmpEndpoint = usbIntr.getEndpoint(nEp);
//            Log.d(TAG, "tmpEndpoint type " + tmpEndpoint.getType());
//            Log.d(TAG, "tmpEndpoint direction " + tmpEndpoint.getDirection());

//            if (tmpEndpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK)
//                continue;

            if ((inEndpoint == null)
                    && (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_IN)) {
                inEndpoint = tmpEndpoint;
            }
        }

        if(inEndpoint == null) {
            Log.w(TAG, "inEndpoint is null");
        }


        REPORT_SIZE = inEndpoint.getMaxPacketSize();

        connection = manager.openDevice(device);
        if (connection == null){
            Log.w(TAG, "mConnection is null");
            return;
        }

        connection.claimInterface (usbIntr, true);
        request = new UsbRequest();
        request.initialize(connection, inEndpoint);

//        byte[] res = get();
        byte[] data = new byte[19];
        int size = Math.min(data.length, inEndpoint.getMaxPacketSize());
//        int result = mConnection.controlTransfer(
//                UsbConstants.USB_DIR_IN,
//                );
////        int result = mConnection.bulkTransfer(inEndpoint, data, size, 300);
//
//        Log.w(TAG, "REPORT : " + result);
    }

    void setup() throws InterruptedException {

        connection = manager.openDevice(device);
        if (connection == null){
            Log.w(TAG, "mConnection is null");
            return;
        }
        openDevice();

//        setInterface();
//        if(usbIntr == null) {
//            Log.w(TAG, "mUsbInterface is null");
//            return;
//        }
//
//        Log.w(TAG, "intr  " + usbIntr);
//        inEndpoint = null;
//
//        // Получаем endpoint’ы
//        // 128 - USB_DIR_IN - device to host
//        for (int nEp = 0; nEp < usbIntr.getEndpointCount(); nEp++) {
//            UsbEndpoint tmpEndpoint = usbIntr.getEndpoint(nEp);
//
//            if ((inEndpoint == null)
//                    && (tmpEndpoint.getDirection() == UsbConstants.USB_DIR_IN)) {
//                inEndpoint = tmpEndpoint;
//            }
//        }
//
//        if(inEndpoint == null) {
//            Log.w(TAG, "inEndpoint is null");
//        }
//
//        boolean r = inEndpoint.getType() == USB_ENDPOINT_XFERTYPE_MASK;
//        Log.d(TAG, "inEndpoint type " + r);
//
//        REPORT_SIZE = inEndpoint.getMaxPacketSize();
//        Log.d(TAG, "REPORT_SIZE: " + String.valueOf(REPORT_SIZE));
//

//
//        connection.claimInterface (usbIntr, true);
//        request = new UsbRequest();
//        request.initialize(connection, inEndpoint);

//        new Thread(new UsbCameraReader(connection, inEndpoint)).start();

//        byte[] res = get();
//        byte[] data;
//        Log.w(TAG, "data : " + Arrays.toString(data));

//        int size = Math.min(data.length, inEndpoint.getMaxPacketSize());
//        int result = connection.controlTransfer(
//                UsbConstants.USB_DIR_IN,
//                1,
//                0,
//                0,
//                data,
//                inEndpoint.getMaxPacketSize(),
//                400
//                );
//        int result = connection.bulkTransfer(inEndpoint, data, inEndpoint.getMaxPacketSize(), 0);
//        Log.d(TAG, "res1 : " + result);
//
//
//        Thread.sleep(10000);
//        result = connection.bulkTransfer(inEndpoint, data, size, 0);
//        Log.d(TAG, "res2 : " + result);
//
//
//        Log.w(TAG, "REPORT : " + result);
//        Log.w(TAG, "res : " + Arrays.toString(data));
    }

    private void openDevice() {
        Log.d(TAG, "openDevice");

//        Log.d(TAG, "getInterfaceCount " + device.getInterfaceCount());

        for (int i = 0; i < device.getInterfaceCount(); i++) {

            UsbInterface usbInterface = device.getInterface(i);

//            Log.d(TAG, "getEndpointCount " + usbInterface.getEndpointCount());

            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(j);
                Log.d(TAG, "Type " + endpoint.getType());


                if (endpoint.getType() == USB_ENDPOINT_XFER_INT) {
                    Log.d(TAG, "find");
                    new Thread(new UsbCameraReader(connection, endpoint)).start();
                }
            }

        }
    }

    byte[] get(){
        Log.w(TAG, "GET");
        ByteBuffer buffer = ByteBuffer.allocate(REPORT_SIZE);
        byte[] report = new byte[buffer.remaining()];

        if (request.queue(buffer)) {
            Log.w(TAG, "1");
            connection.requestWait();
            Log.w(TAG, "2");
            buffer.rewind();
            buffer.get(report, 0, report.length);
            buffer.clear();
        }
        return report;
    }

    /*
        USB_CLASS_VIDEO = 14
        у нас класс девайса = 239, но он имеет несколько интерфейсов, один из которых с id = 0 и
        классом = 14 и есть камера
     */
    void setDevice() {
        if(devices.isEmpty()){
            Log.w(TAG, "devices is empty");
            return;
        }

        for (UsbDevice usbDevice: devices.values()) {
            if (usbDevice.getDeviceClass()
                    == 239
//                    == 0
            ) {
                device = usbDevice;
            }
//            else {
//                UsbInterface usbInterface = setInterface(usbDevice);
//                if (usbInterface != null) return usbDevice;
//            }
        }
    }

    void setInterface() {
        if(device == null) {
            Log.w(TAG, "device is null");
            return;
        }
        for (int nIf = 0; nIf < device.getInterfaceCount(); nIf++) {
            UsbInterface tmp = device.getInterface(nIf);
            if (tmp.getInterfaceClass()
//                    == 3
                    == 14
//                    && tmp.getAlternateSetting() == 0
            ) {
                usbIntr = tmp;
                return;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    protected void onStart() {
//        mUsbManager.requestPermission(mUsbDevice, permissionIntent);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "camera permission granted ");
            } else {
                Log.d(TAG, "camera permission denied ");
            }
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        request.close();
        super.onDestroy();
    }
}

class UsbCameraReader implements Runnable {
    private UsbDeviceConnection connection;
    private UsbEndpoint endpoint;

    public static final String TAG = "!!!";

    public UsbCameraReader(UsbDeviceConnection connection, UsbEndpoint endpoint) {
        this.connection = connection;
        this.endpoint = endpoint;
    }

    @Override
    public void run() {
        Log.d(TAG, "RUN");
        ByteBuffer buffer = ByteBuffer.allocate(endpoint.getMaxPacketSize());

        UsbRequest request = new UsbRequest();
        request.initialize(connection, endpoint);

        while (true) {
            request.queue(buffer);

            UsbRequest waited = connection.requestWait();

            if (waited == request) {
                Log.d(TAG, "Buffer : " + Arrays.toString(buffer.array()));
                // Обработка данных изображения
                processFrame(buffer.array());
            }
        }
    }

    private void processFrame(byte[] data) {
        // Обработка и отображение данных изображения

        Log.d(TAG, Arrays.toString(data));
    }
}