package com.dynamsoft.usbcamera;

import android.util.Log;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApi {
    private static final String BASE_URL = "http://192.168.1.57:5000/v1/android/";
    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private static final Api api = retrofit.create(Api.class);

    public static void sendImageToServer(String scannedString) {
        Image image = new Image(scannedString);
        Call<Void> call = api.sendImage(image);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("!!!", "String sent successfully");
                } else {
                    Log.e("!!!", "Failed to send image. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
