package com.lumivaraonline.fps;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.pm.verify.domain.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class GoogleLoginActivity extends Activity {
    private TextView message;
    private Button begin;
    private boolean busy=false;
    private boolean waiting=false;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    static boolean callbackEnabled(Context context){
        return ReturnLinkSettings.ready(context);
    }
    static void openLinkSettings(Activity activity){
        ReturnLinkSettings.openGuide(activity);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);waiting=state!=null&&state.getBoolean("waiting",false);ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(24),dp(40),dp(24),dp(24));page.setBackgroundColor(Color.rgb(20,35,71));scroll.addView(page);setContentView(scroll);
        message=new TextView(this);message.setTextColor(Color.rgb(248,241,224));message.setTextSize(17);page.addView(message);
        Button settings=new Button(this);settings.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_link_settings));page.addView(settings);settings.setOnClickListener(v->openLinkSettings(this));
        begin=new Button(this);begin.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.begin_google_login));page.addView(begin);begin.setOnClickListener(v->begin());
        Button back=new Button(this);back.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_back));page.addView(back);back.setOnClickListener(v->finish());
        message.setText(AppLanguage.text(GoogleLoginActivity.this,waiting?R.string.login_waiting:R.string.login_instructions));
    }
    @Override protected void onResume(){super.onResume();if(message!=null&&!busy&&!waiting)message.setText(AppLanguage.text(this,callbackEnabled(this)?R.string.login_instructions:R.string.login_links_disabled));}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("waiting",waiting);super.onSaveInstanceState(state);}
    private void begin(){
        if(busy)return;
        if(!callbackEnabled(this)){message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_links_disabled));ReturnLinkSettings.openGuide(this);return;}
        FloatingGameService service=FloatingGameService.active;
        String start=getIntent().getStringExtra("auth_start");
        if(service==null||!FloatingGameService.running||!OAuthHandshake.isStart(start)){message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_start_from_game));return;}
        busy=true;waiting=false;begin.setEnabled(false);message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_preparing));
        String cookies=CookieManager.getInstance().getCookie("https://lumivaraonline.com/");
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(start).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
                if(cookies!=null)connection.setRequestProperty("Cookie",cookies);
                int code=connection.getResponseCode();if(code!=302&&code!=303)throw new Exception();
                String google=connection.getHeaderField("Location");OAuthHandshake pending=new OAuthHandshake(google,SystemClock.elapsedRealtime());
                List<String> newCookies=new ArrayList<>();for(Map.Entry<String,List<String>> header:connection.getHeaderFields().entrySet())if(header.getKey()!=null&&header.getKey().equalsIgnoreCase("Set-Cookie"))newCookies.addAll(header.getValue());
                if(newCookies.isEmpty())throw new Exception();
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed()||FloatingGameService.active!=service)return;
                    CookieManager manager=CookieManager.getInstance();
                    applyCookies(manager,newCookies,0,()->{manager.flush();service.setPendingLogin(pending);launchTab(google);});
                });
            }catch(Exception error){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;busy=false;begin.setEnabled(true);message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_prepare_failed));});}
            finally{if(connection!=null)connection.disconnect();}
        },"LumivaraAuthStart").start();
    }
    private void applyCookies(CookieManager manager,List<String> cookies,int index,Runnable done){
        if(isFinishing()||isDestroyed())return;
        if(index>=cookies.size()){done.run();return;}
        manager.setCookie("https://lumivaraonline.com/auth/google",cookies.get(index),ok->{if(!Boolean.TRUE.equals(ok)){busy=false;begin.setEnabled(true);message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_cookie_failed));return;}applyCookies(manager,cookies,index+1,done);});
    }
    private void launchTab(String address){
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(address));Bundle extras=new Bundle();extras.putBinder("android.support.customtabs.extra.SESSION",null);intent.putExtras(extras);intent.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR",Color.rgb(20,35,71));intent.putExtra("android.support.customtabs.extra.TITLE_VISIBILITY",1);
        List<ResolveInfo> providers=getPackageManager().queryIntentServices(new Intent("android.support.customtabs.action.CustomTabsService"),0);
        ResolveInfo defaultBrowser=getPackageManager().resolveActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://accounts.google.com/")),PackageManager.MATCH_DEFAULT_ONLY);
        String selected=null;for(ResolveInfo provider:providers){String name=provider.serviceInfo.packageName;if(selected==null)selected=name;if(defaultBrowser!=null&&name.equals(defaultBrowser.activityInfo.packageName)){selected=name;break;}}
        if(selected!=null)intent.setPackage(selected);
        try{startActivity(intent);waiting=true;message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_waiting));busy=false;begin.setEnabled(true);}
        catch(Exception error){busy=false;begin.setEnabled(true);message.setText(AppLanguage.text(GoogleLoginActivity.this,R.string.login_browser_missing));}
    }
    @Override protected void onDestroy(){if(FloatingGameService.active!=null)FloatingGameService.active.loginActivityClosed();super.onDestroy();}
}
