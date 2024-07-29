package com.example.testusb;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class Test  {
//    private var mViewBinding: FragmentUvcBinding? = null
//    private Activ
//
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        EdgeToEdge.enable(this);
////        setContentView(R.layout.activity_main);
////    }
//
//
//    @Nullable
//    @Override
//    protected IAspectRatio getCameraView() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    protected ViewGroup getCameraViewContainer() {
//        return null;
//    }
//
//    @Nullable
//    @Override
//    protected View getRootView(@NonNull LayoutInflater layoutInflater) {
//        return null;
//    }
//
//    @Override
//    public void onCameraState(@NonNull MultiCameraClient.ICamera iCamera, @NonNull State state, @Nullable String s) {
//
//    }
//
//    @Override
//    public void onPointerCaptureChanged(boolean hasCapture) {
//        super.onPointerCaptureChanged(hasCapture);
//    }
}
//
//class DemoFragment: CameraFragment() {
//    private var mViewBinding: FragmentUvcBinding? = null
//
//    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
//    if (mViewBinding == null) {
//        mViewBinding = FragmentUvcBinding.inflate(inflater, container, false)
//    }
//    return mViewBinding?.root
//    }
//
//    // if you want offscreen render
//    // please return null
//    override fun getCameraView(): IAspectRatio? {
//    return mViewBinding?.tvCameraRender
//    }
//
//    // if you want offscreen render
//    // please return null, the same as getCameraView()
//    override fun getCameraViewContainer(): ViewGroup? {
//    return mViewBinding?.container
//    }
//
//    // camera open status callback
//    override fun onCameraState(self: ICamera,
//            code: ICameraStateCallBack.State,
//            msg: String?) {
//        when (code) {
//            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
//            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
//            ICameraStateCallBack.State.ERROR -> handleCameraError()
//        }
//    }
//
//    override fun getGravity(): Int = Gravity.TOP
//}