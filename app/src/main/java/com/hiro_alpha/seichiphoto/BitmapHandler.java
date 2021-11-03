package com.hiro_alpha.seichiphoto;

import android.app.Application;
import android.graphics.Bitmap;

public class BitmapHandler extends Application {

    private Bitmap obj;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    //Bitmap受け渡し用
    public void setObj(Bitmap bitmap){
        obj = bitmap;
    }

    public Bitmap getObj(){
        return obj;
    }

    public void cleanObj(){
        obj = null;
    }
}
