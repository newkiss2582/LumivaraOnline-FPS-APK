package com.lumivaraonline.fps;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

public class ProfileApplication extends Application {
    private static int slot;
    public static int slot(){return slot;}
    @Override protected void attachBaseContext(Context base){
        super.attachBaseContext(base);
        if(Build.VERSION.SDK_INT>=28){
            slot=ProfileIdentity.fromProcess(base.getPackageName(),Application.getProcessName());
            String suffix=ProfileIdentity.suffix(slot);
            // Must precede any WebView or CookieManager initialization. Slot 0 retains existing data.
            if(suffix!=null)WebView.setDataDirectorySuffix(suffix);
        }
    }
}
