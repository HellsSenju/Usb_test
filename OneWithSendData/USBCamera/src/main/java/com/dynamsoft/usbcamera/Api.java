package com.dynamsoft.usbcamera;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface Api {
    @POST("receive_image")
    Call<Void> sendImage(@Body Image image);

}
