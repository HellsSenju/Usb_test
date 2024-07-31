package com.dynamsoft.usbcamera;

import android.content.Context;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScanContract extends ActivityResultContract<GetterActivity, ScanIntentResult> {
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, GetterActivity input) {
        return input.createScanIntent(context);
    }

    @Override
    public ScanIntentResult parseResult(int resultCode, @Nullable Intent intent) {
        return ScanIntentResult.parseActivityResult(resultCode, intent);
    }
}