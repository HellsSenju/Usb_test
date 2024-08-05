package com.dynamsoft.usbcamera;

import com.google.gson.annotations.SerializedName;

public class Image {
    @SerializedName("string_image")
    String stringImage;

    public Image(String stringImage) {
        this.stringImage = stringImage;
    }
}
