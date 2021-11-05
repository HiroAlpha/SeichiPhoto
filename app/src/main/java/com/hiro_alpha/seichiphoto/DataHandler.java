package com.hiro_alpha.seichiphoto;

import android.app.Application;
import android.graphics.Bitmap;

public class DataHandler extends Application {

    private Bitmap overlayImage = null;
    private int camRatio = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //オーバーレイ画像
    public void setOverlayImage(Bitmap overlayImage) {
        this.overlayImage = overlayImage;
    }

    public Bitmap getOverlayImage() {
        return overlayImage;
    }

    public void cleanOverlayImage(){
        overlayImage = null;
    }

    //アスペクト比

    public void setCamRatio(int camRatio) {
        this.camRatio = camRatio;
    }

    public int getCamRatio() {
        return camRatio;
    }
}
