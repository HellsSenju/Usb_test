package com.dynamsoft.usbcamera;


public class GetterActivity {


    private Class<?> captureActivity;

    protected Class<?> getDefaultCaptureActivity() {
        return CaptureActivity.class;
    }

    public Class<?> getCaptureActivity() {
        if (captureActivity == null) {
            captureActivity = getDefaultCaptureActivity();
        }
        return captureActivity;
    }
}
