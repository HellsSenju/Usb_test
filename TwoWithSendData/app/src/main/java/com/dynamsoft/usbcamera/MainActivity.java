package com.dynamsoft.usbcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GetterActivity options = new GetterActivity();
        Intent intent = new Intent(this, options.getCaptureActivity());
        startActivity(intent);

    }

}