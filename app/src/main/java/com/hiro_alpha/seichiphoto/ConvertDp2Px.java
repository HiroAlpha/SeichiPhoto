package com.hiro_alpha.seichiphoto;

import android.content.Context;
import android.util.DisplayMetrics;

//dp => px
public class ConvertDp2Px {

    public float convert(float dp, Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * metrics.density;
    }
}
