package com.lumivaraonline.fps;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Display host only: the foreground service continues to own the game WebView. */
public class GameFullscreenActivity extends Activity {
    private FloatingGameService service;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams p=getWindow().getAttributes();p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(p);}
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.HORIZONTAL);page.setBackgroundColor(Color.rgb(20,35,71));
        FrameLayout host=new FrameLayout(this);page.addView(host,new LinearLayout.LayoutParams(0,-1,1));setContentView(page);
        service=FloatingGameService.active;
        if(service==null||!service.attachFullscreen(this,host)){Toast.makeText(this,AppLanguage.text(GameFullscreenActivity.this,R.string.start_before_fullscreen),Toast.LENGTH_SHORT).show();finish();return;}
        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.VERTICAL);tools.setBackgroundColor(Color.rgb(26,46,87));
        LinearLayout.LayoutParams dockLp=new LinearLayout.LayoutParams(dp(44),-1);page.addView(tools,dockLp);
        Button fold=control(tools,"›",AppLanguage.text(GameFullscreenActivity.this,R.string.fold_dock),null);
        control(tools,"▣",AppLanguage.text(GameFullscreenActivity.this,R.string.fullscreen_to_mini),v->service.returnToMini(this));
        control(tools,"−",AppLanguage.text(GameFullscreenActivity.this,R.string.fullscreen_to_bubble),v->service.returnToOverlay(this,true));
        final boolean[] folded={false};fold.setOnClickListener(v->{folded[0]=!folded[0];dockLp.width=dp(folded[0]?20:44);tools.setLayoutParams(dockLp);fold.setText(folded[0]?"‹":"›");for(int i=1;i<tools.getChildCount();i++)tools.getChildAt(i).setVisibility(folded[0]?View.GONE:View.VISIBLE);});
        // The dock has its OWN width; no app controls overlap the game's viewport.
        page.setOnApplyWindowInsetsListener((view,insets)->{
            if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout cut=insets.getDisplayCutout();page.setPadding(cut.getSafeInsetLeft(),cut.getSafeInsetTop(),cut.getSafeInsetRight(),cut.getSafeInsetBottom());}
            else page.setPadding(0,0,0,0);return insets;
        });
        page.requestApplyInsets();immersive();
    }
    private Button control(LinearLayout row,String label,String description,View.OnClickListener click){
        Button button=new Button(this);button.setText(label);button.setContentDescription(description);button.setTextSize(21);button.setTextColor(Color.rgb(243,211,139));button.setBackgroundColor(Color.TRANSPARENT);button.setPadding(0,0,0,0);button.setMinWidth(0);button.setMinimumWidth(0);row.addView(button,new LinearLayout.LayoutParams(-1,dp(48)));button.setOnClickListener(click);return button;
    }
    void requestRefreshRate(boolean visible){
        WindowManager.LayoutParams params=getWindow().getAttributes();
        PerformanceSettings.request(this,getWindow().getDecorView().getDisplay(),params,visible);
        getWindow().setAttributes(params);
    }
    @Override protected void onResume(){super.onResume();requestRefreshRate(true);}
    private void immersive(){
        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null){controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);controller.hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());}
        }else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus){immersive();requestRefreshRate(true);}}
    @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);immersive();requestRefreshRate(true);}
    @Override public void onBackPressed(){if(service!=null)service.returnToOverlay(this,false);else finish();}
    @Override protected void onStop(){requestRefreshRate(false);if(service!=null)service.returnToOverlay(this,true);super.onStop();}
    @Override protected void onDestroy(){if(service!=null)service.returnToOverlay(this,true);service=null;super.onDestroy();}
}
