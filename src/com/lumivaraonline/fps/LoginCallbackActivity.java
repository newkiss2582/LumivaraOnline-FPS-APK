package com.lumivaraonline.fps;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.widget.Toast;

/** The exported entry point forwards only a validated candidate to running profiles.
 * Each profile alone verifies its pending OAuth state before accepting it. */
public class LoginCallbackActivity extends Activity {
    private int generation;
    @Override public void onCreate(Bundle state){super.onCreate(state);receive(getIntent());}
    @Override public void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);receive(intent);}
    private void receive(Intent intent){
        int request=++generation;String address=intent==null?null:intent.getDataString();
        if(!CallbackCandidate.valid(address)){rejected(request);return;}
        dispatch(address,0,request);
    }
    private void dispatch(String address,int slot,int request){
        if(request!=generation||isFinishing()||isDestroyed())return;
        if(slot>=ProfileIdentity.supportedCount(Build.VERSION.SDK_INT)){rejected(request);return;}
        ProfileCommands.send(this,slot,ProfileCommands.CALLBACK,address,accepted->{
            if(request!=generation||isFinishing()||isDestroyed())return;
            if(accepted)finish();else dispatch(address,slot+1,request);
        });
    }
    private void rejected(int request){if(request!=generation)return;Toast.makeText(this,AppLanguage.text(this,R.string.login_callback_rejected),Toast.LENGTH_LONG).show();finish();}
    @Override protected void onDestroy(){generation++;super.onDestroy();}
}
