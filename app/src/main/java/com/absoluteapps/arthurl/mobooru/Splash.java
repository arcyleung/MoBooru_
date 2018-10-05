package com.absoluteapps.arthurl.mobooru;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by pspka on 12/27/2017.
 */

public class Splash extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
        finish();
    }
}
