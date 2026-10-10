package com.lumivaraonline.fps;

import android.content.Context;
import android.net.*;
import android.os.*;
import android.webkit.WebView;
import org.json.*;
import java.util.*;

/** Bounded local technical history. Never reads cookies, account data or packet bodies. */
final class GameDiagnostics {
    private final Context context;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayDeque<String> history=new ArrayDeque<>();
    private WebView current;
    private boolean stopped,pending;
    private long requested,lastReply;
    private final String preferences;
    private final Runnable sampler=()->sample();
    GameDiagnostics(Context context,String preferences){this.context=context;this.preferences=preferences;}
    void attach(WebView view){current=view;pending=false;}
    void capture(){sample();}
    void stop(){stopped=true;handler.removeCallbacksAndMessages(null);current=null;}
    private void sample(){
        if(stopped)return;
        long now=SystemClock.elapsedRealtime();
        if(pending){record("js_wait_ms="+(now-requested)+system());}
        else if(current!=null&&NavigationPolicy.route(current.getUrl())==NavigationPolicy.Route.GAME){
            pending=true;requested=now;final WebView view=current;
            view.evaluateJavascript("JSON.stringify(location.origin==='https://lumivaraonline.com'&&window.__lumivaraFrameControl?window.__lumivaraFrameControl.snapshot():{ready:false})",value->{
                if(stopped||view!=current)return;
                pending=false;lastReply=SystemClock.elapsedRealtime();
                try{
                    Object decoded=new JSONTokener(value==null?"null":value).nextValue();
                    JSONObject data=decoded instanceof String?new JSONObject((String)decoded):new JSONObject();
                    StringBuilder line=new StringBuilder("js_ms=").append(lastReply-requested);
                    for(String key:new String[]{"ready","background","frames","timers","pendingFrames","pendingTimers","frameAgeMs","pulseGapMax","socketObserved","socketOpen","socketCloses","lastCloseCode","packets","packetAgeMs","gameSaver","disconnectedUI","gameCloseCount","gameCloseCode"}){
                        Object field=data.opt(key);if(field instanceof Number||field instanceof Boolean)line.append(" ").append(key).append("=").append(field);
                    }
                    record(line.toString()+system());
                }catch(Exception invalid){record("js_reply_invalid"+system());}
            });
        }

    }
    private String system(){
        PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        ConnectivityManager net=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities caps=net.getNetworkCapabilities(net.getActiveNetwork());
        return " screen="+power.isInteractive()+" idle="+power.isDeviceIdleMode()+" exempt="+power.isIgnoringBatteryOptimizations(context.getPackageName())+" batterySaver="+power.isPowerSaveMode()+" networkValidated="+(caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
    }
    private void record(String value){
        history.addLast(SystemClock.elapsedRealtime()+" "+value);while(history.size()>180)history.removeFirst();
        context.getSharedPreferences(preferences,0).edit().putString("technical_history",String.join("\n",history)).apply();
    }
    String report(){
        String header="Lumivara Online FPS 1.0.0 · API "+Build.VERSION.SDK_INT+" · WebView "+(WebView.getCurrentWebViewPackage()==null?"unknown":WebView.getCurrentWebViewPackage().versionName)+"\n";
        header+="lastReplyAgeMs="+(lastReply==0?-1:SystemClock.elapsedRealtime()-lastReply)+" pending="+pending+system()+"\n";
        String data=history.isEmpty()?context.getSharedPreferences(preferences,0).getString("technical_history",""):String.join("\n",history);
        return header+data;
    }
}
