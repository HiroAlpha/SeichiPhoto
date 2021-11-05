package com.hiro_alpha.seichiphoto;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

public class OnTextChange extends Activity implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        Log.d("Activity.OnTextChange", "Ratio Changed : " + charSequence.toString());

        String ratio = charSequence.toString();
        int ratioId = 0;
        if (ratio.equals("16:9")) {
            ratioId = 1;
        }

        new DataHandler().setCamRatio(ratioId);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
}
