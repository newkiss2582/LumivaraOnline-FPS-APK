package com.lumivaraonline.fps;

import android.content.Context;
import android.os.Build;
import android.view.*;

final class PerformanceSettings {
    static final String FPS="fps_limit",SCALE="render_scale";
    private PerformanceSettings(){}
    static int limit(Context context){int v=AppLanguage.preferences(context).getInt(FPS,0);return v==60||v==120?v:0;}
    static int scale(Context context){int v=AppLanguage.preferences(context).getInt(SCALE,85);return v==100||v==70?v:85;}
    static String fpsLabel(Context context){int v=limit(context);return v==0?AppLanguage.text(context,R.string.fps_max):v+" FPS";}
    static void request(Context context,Display display,WindowManager.LayoutParams params,boolean visible){
        params.preferredDisplayModeId=0;params.preferredRefreshRate=0;
        if(!visible)return;
        try{
            if(display==null)display=((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Display.Mode current=display.getMode();Display.Mode[] supported=display.getSupportedModes();
            int[][] modes=new int[supported.length][3];float[] rates=new float[supported.length];
            for(int i=0;i<supported.length;i++){Display.Mode mode=supported[i];modes[i]=new int[]{mode.getModeId(),mode.getPhysicalWidth(),mode.getPhysicalHeight()};rates[i]=mode.getRefreshRate();}
            int index=DisplayRefreshPolicy.choose(current.getPhysicalWidth(),current.getPhysicalHeight(),limit(context),modes,rates);
            if(index>=0){
                if(Build.VERSION.SDK_INT>=34)params.preferredRefreshRate=rates[index];
                else params.preferredDisplayModeId=modes[index][0];
            }
        }catch(RuntimeException unavailable){/* Retain system defaults if display data is unavailable. */}
    }
}
