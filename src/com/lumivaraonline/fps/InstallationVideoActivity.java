package com.lumivaraonline.fps;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;

/** Plays the bundled tutorial without a browser or storage permission. */
public class InstallationVideoActivity extends Activity {
    private VideoView video;
    private MediaController controls;
    private TextView error;
    private int position;
    private boolean prepared,resumed,playWhenReady=true,failed;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private String text(int id){return AppLanguage.text(this,id);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(state!=null){position=state.getInt("video_position",0);playWhenReady=state.getBoolean("video_playing",true);}
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(Color.rgb(20,35,71));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(4),0,dp(8),0);
        Button back=button("‹");back.setTextSize(28);back.setContentDescription(text(R.string.installation_video_back));back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView title=new TextView(this);title.setText(text(R.string.installation_video_title));title.setTextColor(Color.rgb(219,183,99));title.setTextSize(18);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button replay=button(text(R.string.installation_video_replay));replay.setOnClickListener(v->{position=0;playWhenReady=true;if(failed){loadVideo();}else if(prepared){video.seekTo(0);video.start();controls.show();}});header.addView(replay,new LinearLayout.LayoutParams(-2,dp(48)));page.addView(header);
        TextView hint=new TextView(this);hint.setText(text(R.string.installation_video_hint));hint.setTextColor(Color.rgb(248,241,224));hint.setTextSize(12);hint.setPadding(dp(12),dp(4),dp(12),dp(6));page.addView(hint);
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.BLACK);page.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        video=new VideoView(this);video.setKeepScreenOn(true);frame.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        error=new TextView(this);error.setText(text(R.string.installation_video_error));error.setTextColor(Color.WHITE);error.setTextSize(17);error.setGravity(Gravity.CENTER);error.setPadding(dp(24),dp(24),dp(24),dp(24));error.setVisibility(View.GONE);frame.addView(error,new FrameLayout.LayoutParams(-1,-1));
        controls=new MediaController(this);controls.setAnchorView(frame);video.setMediaController(controls);
        video.setOnPreparedListener(player->{prepared=true;failed=false;error.setVisibility(View.GONE);int duration=player.getDuration();video.seekTo(Math.min(position,Math.max(0,duration-1)));if(resumed&&playWhenReady)video.start();controls.show();});
        video.setOnCompletionListener(player->{position=0;playWhenReady=false;controls.show(0);});
        video.setOnErrorListener((player,what,extra)->{prepared=false;failed=true;controls.hide();error.setVisibility(View.VISIBLE);return true;});
        setContentView(page);
        page.setOnApplyWindowInsetsListener((view,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=safe.left;top=safe.top;right=safe.right;bottom=safe.bottom;}
            else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}
            page.setPadding(left,top,right,bottom);return insets;
        });page.requestApplyInsets();
    }
    private Button button(String label){Button button=new Button(this);button.setText(label);button.setAllCaps(false);button.setTextColor(Color.rgb(219,183,99));button.setBackgroundColor(Color.TRANSPARENT);button.setMinWidth(0);button.setMinimumWidth(0);return button;}
    private void loadVideo(){prepared=false;failed=false;error.setVisibility(View.GONE);video.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.install_tutorial));}
    @Override protected void onStart(){super.onStart();loadVideo();}
    @Override protected void onResume(){super.onResume();resumed=true;if(prepared&&playWhenReady)video.start();}
    @Override protected void onPause(){resumed=false;if(prepared){position=video.getCurrentPosition();playWhenReady=video.isPlaying();video.pause();}controls.hide();super.onPause();}
    @Override protected void onStop(){video.stopPlayback();prepared=false;super.onStop();}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("video_position",prepared?video.getCurrentPosition():position);state.putBoolean("video_playing",prepared&&resumed?video.isPlaying():playWhenReady);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){controls.hide();video.stopPlayback();super.onDestroy();}
}
