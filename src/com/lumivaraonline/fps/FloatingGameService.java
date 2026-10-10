package com.lumivaraonline.fps;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FloatingGameService extends Service {
    static final String EXPAND="expand",MINI="mini",COLLAPSE="collapse",STOP="stop",FULLSCREEN="fullscreen";
    static volatile boolean running=false;
    static FloatingGameService active;
    private OAuthHandshake pendingLogin;
    private int profileSlot(){return ProfileApplication.slot();}
    private String windowTitle(){return "Lumivara Online FPS";}
    private String toolbarTitle(int progress){return miniature?"FPS":progress<100?AppLanguage.text(this,R.string.loading_prefix)+progress+"%":windowTitle();}
    private String windowPreferences(){return ProfileIdentity.windowPrefs(profileSlot());}
    private int notificationId(){return ProfileIdentity.notificationId(profileSlot());}
    private final BroadcastReceiver menuLanguageReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){AppLanguage.acceptRemote(intent.getStringExtra(AppLanguage.KEY));refreshLanguage();}};
    private volatile String approvedCallback;
    private boolean loginActivityOpen=false;
    private boolean checkingLogin=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private FrameLayout root;
    private MiniGameLayout gameViewport;
    private TextView resizeGrip;
    private LinearLayout resizeFooter;
    private Button miniButton,helpButton,stopButton;
    private Button fullscreenButton,collapseButton,browserReturnButton;
    private SharedPreferences appSettings;
    private final SharedPreferences.OnSharedPreferenceChangeListener languageListener=(settings,key)->{
        if(AppLanguage.KEY.equals(key))handler.post(()->refreshLanguage());
        else if(PerformanceSettings.FPS.equals(key)||PerformanceSettings.SCALE.equals(key))handler.post(()->applyPerformance());
    };
    private WebView game;
    private MutableContextWrapper gameContext;
    private GameFullscreenActivity fullscreenActivity;
    private FrameLayout fullscreenHost;
    private WebView browser;
    private MutableContextWrapper browserContext;
    private final Set<WebView> releasedBrowsers=Collections.newSetFromMap(new WeakHashMap<WebView,Boolean>());
    private LinearLayout browserPanel;
    private TextView browserAddress;
    private AlertDialog authNotice;
    private AlertDialog gameDialog;
    private LinearLayout bar;
    private View bubble;
    private View profileBadge;
    private TextView title;
    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;
    private boolean frameFallback;
    private int frameGeneration;
    private final Runnable framePump=()->pumpBackgroundFrame();
    private boolean screenReceiverRegistered;
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){
            updateFrameMode();
            if(game!=null&&powerManager!=null&&powerManager.isInteractive()){game.onResume();game.resumeTimers();}
        }
    };
    private boolean expanded=false;
    private boolean miniature=false;
    private int miniWidth,miniHeight,miniX,miniY;
    private MiniGameLayout fullscreenViewport;
    private int bubbleX=12,bubbleY=160;
    private String backgroundScript="";
    private String selectScript="";
    private String performanceScript="";
    private Runnable heartbeat;
    private GameDiagnostics diagnostics;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private int width(){return Build.VERSION.SDK_INT>=30&&wm!=null?wm.getMaximumWindowMetrics().getBounds().width():getResources().getDisplayMetrics().widthPixels;}
    private int height(){return Build.VERSION.SDK_INT>=30&&wm!=null?wm.getMaximumWindowMetrics().getBounds().height():getResources().getDisplayMetrics().heightPixels;}
    @Override public void onCreate(){
        super.onCreate();
        active=this;
        AppLanguage.refreshRemote();
        if(Build.VERSION.SDK_INT>=33)registerReceiver(menuLanguageReceiver,new IntentFilter(AppLanguage.CHANGED),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(menuLanguageReceiver,new IntentFilter(AppLanguage.CHANGED));
        appSettings=AppLanguage.preferences(this);appSettings.registerOnSharedPreferenceChangeListener(languageListener);
        NotificationChannel channel=new NotificationChannel("floating_game",AppLanguage.text(FloatingGameService.this,R.string.floating_channel),NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(AppLanguage.text(FloatingGameService.this,R.string.channel_description));channel.setSound(null,null);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        if(Build.VERSION.SDK_INT>=34)startForeground(notificationId(),notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(notificationId(),notification());
        if(!Settings.canDrawOverlays(this)){Toast.makeText(this,AppLanguage.text(FloatingGameService.this,R.string.overlay_revoked),Toast.LENGTH_LONG).show();stopSelf();return;}
        try{
            wm=(WindowManager)getSystemService(WINDOW_SERVICE);
            bubbleX=getSharedPreferences(windowPreferences(),0).getInt("x",dp(12+profileSlot()*62));bubbleY=getSharedPreferences(windowPreferences(),0).getInt("y",dp(160+profileSlot()*62));
            miniWidth=getSharedPreferences(windowPreferences(),0).getInt("miniWidth",dp(280));miniHeight=getSharedPreferences(windowPreferences(),0).getInt("miniHeight",dp(190));miniX=getSharedPreferences(windowPreferences(),0).getInt("miniX",dp(12+profileSlot()*28));miniY=getSharedPreferences(windowPreferences(),0).getInt("miniY",dp(80+profileSlot()*70));
            try(InputStream in=getAssets().open("background.js")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);backgroundScript=new String(out.toByteArray(),StandardCharsets.UTF_8);}
            try(InputStream in=getAssets().open("select-picker.js")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);selectScript=new String(out.toByteArray(),StandardCharsets.UTF_8);}
            try(InputStream in=getAssets().open("performance.js")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);performanceScript=new String(out.toByteArray(),StandardCharsets.UTF_8);}
            diagnostics=new GameDiagnostics(this,windowPreferences());
            buildOverlay();
            powerManager=(PowerManager)getSystemService(POWER_SERVICE);
            IntentFilter screenFilter=new IntentFilter();screenFilter.addAction(Intent.ACTION_SCREEN_OFF);screenFilter.addAction(Intent.ACTION_SCREEN_ON);
            if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,screenFilter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screenReceiver,screenFilter);
            screenReceiverRegistered=true;
            wakeLock=powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Lumivara:FloatingGame");wakeLock.setReferenceCounted(false);
            heartbeat=new Runnable(){public void run(){if(!running)return;if(wakeLock!=null)wakeLock.acquire(120000);CookieManager.getInstance().flush();handler.postDelayed(this,60000);}};
            running=true;heartbeat.run();updateFrameMode();
            game.loadUrl("https://lumivaraonline.com/");
        }catch(Exception e){Toast.makeText(this,AppLanguage.text(FloatingGameService.this,R.string.overlay_failed)+e.getMessage(),Toast.LENGTH_LONG).show();stopSelf();}
    }
    private Notification notification(){
        Intent show=ProfileComponents.serviceIntent(this,profileSlot(),EXPAND),stop=ProfileComponents.serviceIntent(this,profileSlot(),STOP);
        PendingIntent open=PendingIntent.getService(this,ProfileIdentity.requestId(profileSlot(),1),show,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent end=PendingIntent.getService(this,ProfileIdentity.requestId(profileSlot(),2),stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent settings=PendingIntent.getActivity(this,ProfileIdentity.requestId(profileSlot(),3),new Intent(this,MainActivity.class).putExtra("profile",profileSlot()),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,"floating_game").setSmallIcon(R.drawable.notification_icon).setContentTitle(windowTitle())
            .setContentText(fullscreenActivity!=null?AppLanguage.text(FloatingGameService.this,R.string.notification_fullscreen):expanded?(miniature?AppLanguage.text(FloatingGameService.this,R.string.notification_mini):AppLanguage.text(FloatingGameService.this,R.string.notification_window)):AppLanguage.text(FloatingGameService.this,R.string.notification_bubble))
            .setContentIntent(settings).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(null,AppLanguage.text(FloatingGameService.this,R.string.open_game),open).build()).addAction(new Notification.Action.Builder(null,AppLanguage.text(FloatingGameService.this,R.string.stop_mode),end).build()).build();
    }
    private void buildOverlay(){
        root=new FrameLayout(this);root.setClipChildren(true);root.setClipToPadding(true);root.setBackgroundColor(Color.rgb(20,35,71));
        params=new WindowManager.LayoutParams(dp(56),dp(56),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;
        params.x=Math.max(0,Math.min(bubbleX,width()-dp(56)));params.y=Math.max(0,Math.min(bubbleY,height()-dp(56)));
        gameViewport=new MiniGameLayout(this);root.addView(gameViewport,0,new FrameLayout.LayoutParams(Math.max(dp(240),width()-dp(24)),Math.max(dp(240),height()-dp(120))));
        createGame();
        bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setBackgroundColor(Color.rgb(26,46,87));
        title=new TextView(this);title.setText(windowTitle());title.setTextColor(Color.rgb(243,211,139));title.setTextSize(13);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setPadding(dp(8),0,0,0);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        miniButton=tool("▣",v->toggleMini());miniButton.setContentDescription(AppLanguage.text(FloatingGameService.this,R.string.toggle_mini));helpButton=tool("?",v->{if(miniature)windowMenu();else help();});fullscreenButton=tool("⛶",v->openFullscreen());collapseButton=tool("−",v->collapse());stopButton=tool("×",v->confirmStop());stopButton.setTextColor(Color.WHITE);stopButton.setBackgroundColor(Color.rgb(133,44,47));
        FrameLayout.LayoutParams barLp=new FrameLayout.LayoutParams(-1,dp(40));root.addView(bar,barLp);drag(title,false);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.game_icon);icon.setPadding(dp(5),dp(5),dp(5),dp(5));
        GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(Color.rgb(26,46,87));circle.setStroke(dp(2),Color.rgb(219,183,99));icon.setBackground(circle);icon.setElevation(dp(5));icon.setContentDescription(AppLanguage.text(FloatingGameService.this,R.string.bubble_description));bubble=icon;
        root.addView(bubble,new FrameLayout.LayoutParams(dp(56),dp(56)));drag(bubble,true);
        TextView badge=new TextView(this);profileBadge=badge;badge.setText(String.valueOf(profileSlot()+1));badge.setTextColor(Color.rgb(243,211,139));badge.setTextSize(13);badge.setGravity(Gravity.CENTER);badge.setBackgroundColor(Color.rgb(20,35,71));root.addView(badge,new FrameLayout.LayoutParams(dp(18),dp(18),Gravity.TOP|Gravity.RIGHT));badge.setClickable(false);
        resizeFooter=new LinearLayout(this);resizeFooter.setGravity(Gravity.CENTER_VERTICAL);resizeFooter.setBackgroundColor(Color.rgb(26,46,87));
        TextView hint=new TextView(this);hint.setText(AppLanguage.text(FloatingGameService.this,R.string.resize));hint.setTextSize(10);hint.setTextColor(Color.rgb(243,211,139));hint.setPadding(dp(6),0,0,0);resizeFooter.addView(hint,new LinearLayout.LayoutParams(0,-1,1));
        resizeControl("↔",AppLanguage.text(FloatingGameService.this,R.string.resize_width),true,false);resizeControl("↕",AppLanguage.text(FloatingGameService.this,R.string.resize_height),false,true);resizeGrip=resizeControl("◢",AppLanguage.text(FloatingGameService.this,R.string.resize_both),true,true);
        root.addView(resizeFooter,new FrameLayout.LayoutParams(-1,dp(32),Gravity.BOTTOM));
        wm.addView(root,params);collapse();refreshLanguage();
    }
    private void createGame(){
        gameContext=new MutableContextWrapper(fullscreenActivity!=null?fullscreenActivity:this);
        game=new WebView(gameContext);if(diagnostics!=null)diagnostics.attach(game);game.setBackgroundColor(Color.rgb(20,35,71));WebSettings settings=game.getSettings();
        settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(true);
        settings.setMediaPlaybackRequiresUserGesture(true);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setSupportZoom(false);settings.setBuiltInZoomControls(false);settings.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(game,true);
        game.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT,false);
        game.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                if(!request.isForMainFrame())return false;
                String address=request.getUrl().toString();
                if(address.equals(approvedCallback))return false;
                NavigationPolicy.Route route=NavigationPolicy.route(address);
                if(route==NavigationPolicy.Route.GAME)return false;
                if(route==NavigationPolicy.Route.GOOGLE_AUTH){if(OAuthHandshake.isStart(address))startGoogleLogin(address);else googleAuthNotice();return true;}
                if(route==NavigationPolicy.Route.IN_APP){openBrowser(address);return true;}
                Toast.makeText(FloatingGameService.this,AppLanguage.text(FloatingGameService.this,R.string.https_only),Toast.LENGTH_SHORT).show();return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){return blockGoogleAuth(req);}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){inject();}
            @Override public void onPageFinished(WebView view,String url){inject();CookieManager.getInstance().flush();if(approvedCallback!=null&&NavigationPolicy.route(url)==NavigationPolicy.Route.GAME)checkLoginResult();}
            @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){if(req.isForMainFrame())Toast.makeText(FloatingGameService.this,AppLanguage.text(FloatingGameService.this,R.string.connection_failed),Toast.LENGTH_LONG).show();}
            @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail){
                detach(view);if(game==view)game=null;view.destroy();handler.post(()->{if(!running)return;createGame();layoutGame();game.loadUrl("https://lumivaraonline.com/");Toast.makeText(FloatingGameService.this,AppLanguage.text(FloatingGameService.this,R.string.renderer_recovered),Toast.LENGTH_LONG).show();});return true;
            }
        });
        game.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView v,int p){if(title!=null)title.setText(toolbarTitle(p));}
            @Override public boolean onCreateWindow(WebView view,boolean dialog,boolean userGesture,Message result){return createPopup(userGesture,result);}
            @Override public boolean onJsAlert(WebView view,String url,String message,JsResult result){return showGameDialog(url,message,result,null,false);}
            @Override public boolean onJsConfirm(WebView view,String url,String message,JsResult result){return showGameDialog(url,message,result,null,true);}
            @Override public boolean onJsPrompt(WebView view,String url,String message,String defaultValue,JsPromptResult result){return showGameDialog(url,message,result,defaultValue,true);}
        });
        attachGame(fullscreenHost!=null?fullscreenHost:root);
        game.onResume();game.resumeTimers();
    }
    private boolean showGameDialog(String url,String message,JsResult result,String defaultValue,boolean cancellable){
        if(NavigationPolicy.route(url)!=NavigationPolicy.Route.GAME){result.cancel();return true;}
        dismissGameDialog();final boolean[] resolved={false};
        Context context=fullscreenActivity!=null?fullscreenActivity:this;
        AlertDialog.Builder builder=new AlertDialog.Builder(context).setTitle("Lumivara Online · lumivaraonline.com").setMessage(message==null?"":message.substring(0,Math.min(message.length(),6000)));
        EditText input=result instanceof JsPromptResult?new EditText(context):null;
        if(input!=null){input.setText(defaultValue==null?"":defaultValue);builder.setView(input);}
        builder.setPositiveButton(AppLanguage.text(FloatingGameService.this,R.string.ok),(dialog,which)->{resolved[0]=true;if(input!=null)((JsPromptResult)result).confirm(input.getText().toString());else result.confirm();});
        if(cancellable)builder.setNegativeButton(AppLanguage.text(FloatingGameService.this,R.string.cancel),(dialog,which)->{resolved[0]=true;result.cancel();});
        gameDialog=builder.create();gameDialog.setOnDismissListener(dialog->{if(!resolved[0]){resolved[0]=true;result.cancel();}});
        try{if(fullscreenActivity==null)gameDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);gameDialog.show();}
        catch(Exception error){dismissGameDialog();if(!resolved[0]){resolved[0]=true;result.cancel();}}
        return true;
    }
    private void dismissGameDialog(){if(gameDialog!=null){gameDialog.dismiss();gameDialog=null;}}
    private WebResourceResponse blockGoogleAuth(WebResourceRequest request){
        if(request.getUrl().toString().equals(approvedCallback))return null;
        if(request.isForMainFrame() && NavigationPolicy.route(request.getUrl().toString())==NavigationPolicy.Route.GOOGLE_AUTH){
            String address=request.getUrl().toString();handler.post(()->{if(OAuthHandshake.isStart(address))startGoogleLogin(address);else googleAuthNotice();});
            return new WebResourceResponse("text/html","UTF-8",new ByteArrayInputStream("<!doctype html><title>Google Login</title>".getBytes(StandardCharsets.UTF_8)));
        }
        return null;
    }
    private void googleAuthNotice(){
        if(authNotice!=null&&authNotice.isShowing())return;
        authNotice=new AlertDialog.Builder(this).setTitle(AppLanguage.text(FloatingGameService.this,R.string.google_notice_title))
            .setMessage(AppLanguage.text(FloatingGameService.this,R.string.google_notice_body))
            .setPositiveButton(AppLanguage.text(FloatingGameService.this,R.string.back_to_game),(dialog,which)->{closeBrowser();if(game!=null&&NavigationPolicy.route(game.getUrl())!=NavigationPolicy.Route.GAME)game.loadUrl("https://lumivaraonline.com/");})
            .setNegativeButton(AppLanguage.text(FloatingGameService.this,R.string.dismiss_message),null).create();
        overlayDialog(authNotice);
    }
    private void startGoogleLogin(String address){
        if(!OAuthHandshake.isStart(address)||loginActivityOpen)return;
        loginActivityOpen=true;pendingLogin=null;approvedCallback=null;
        closeBrowser();collapse();
        try{startActivity(new Intent(this,ProfileComponents.login(profileSlot())).putExtra("auth_start",address).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(Exception error){loginActivityOpen=false;Toast.makeText(this,AppLanguage.text(FloatingGameService.this,R.string.login_launch_failed),Toast.LENGTH_LONG).show();}
    }
    void loginActivityClosed(){loginActivityOpen=false;}
    void setPendingLogin(OAuthHandshake pending){pendingLogin=pending;}
    boolean acceptGoogleCallback(String address){
        if(!running||game==null||pendingLogin==null||!pendingLogin.accept(address,SystemClock.elapsedRealtime()))return false;
        pendingLogin=null;approvedCallback=address;loginActivityOpen=false;closeBrowser();expand();game.loadUrl(address);return true;
    }
    private void checkLoginResult(){
        if(checkingLogin)return;checkingLogin=true;
        String cookies=CookieManager.getInstance().getCookie("https://lumivaraonline.com/");
        new Thread(()->{
            boolean signedIn=false;java.net.HttpURLConnection connection=null;
            try{
                connection=(java.net.HttpURLConnection)new java.net.URL("https://lumivaraonline.com/api/me").openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setRequestProperty("Cache-Control","no-cache");if(cookies!=null)connection.setRequestProperty("Cookie",cookies);
                try(InputStream in=connection.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[2048];int n;while((n=in.read(bytes))!=-1&&out.size()<65536)out.write(bytes,0,n);org.json.JSONObject result=new org.json.JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));signedIn=result.optBoolean("signedIn",false)&&"google".equals(result.optString("kind"));}
            }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
            final boolean success=signedIn;
            handler.post(()->{checkingLogin=false;approvedCallback=null;if(!running)return;Toast.makeText(this,success?AppLanguage.text(FloatingGameService.this,R.string.google_success):AppLanguage.text(FloatingGameService.this,R.string.google_no_session),Toast.LENGTH_LONG).show();});
        },"LumivaraAuthCheck").start();
    }
    private boolean createPopup(boolean userGesture,Message result){
        if(!userGesture)return false;
        closeBrowser();
        openBrowser(null);
        WebView.WebViewTransport transport=(WebView.WebViewTransport)result.obj;
        transport.setWebView(browser);result.sendToTarget();return true;
    }
    private void openBrowser(String address){
        expand();
        if(browser==null){
            browserPanel=new LinearLayout(this);browserPanel.setOrientation(LinearLayout.VERTICAL);browserPanel.setBackgroundColor(Color.rgb(20,35,71));
            LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackgroundColor(Color.rgb(26,46,87));
            Button back=new Button(this);back.setText("‹");back.setTextColor(Color.rgb(248,241,224));back.setBackgroundColor(Color.TRANSPARENT);nav.addView(back,new LinearLayout.LayoutParams(dp(40),dp(40)));
            browserAddress=new TextView(this);browserAddress.setText(AppLanguage.text(FloatingGameService.this,R.string.in_app_browser));browserAddress.setTextColor(Color.rgb(243,211,139));browserAddress.setSingleLine(true);browserAddress.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);nav.addView(browserAddress,new LinearLayout.LayoutParams(0,dp(40),1));
            Button close=new Button(this);browserReturnButton=close;close.setText(AppLanguage.text(FloatingGameService.this,R.string.return_game));close.setTextColor(Color.rgb(248,241,224));close.setTextSize(12);close.setBackgroundColor(Color.TRANSPARENT);nav.addView(close,new LinearLayout.LayoutParams(dp(90),dp(40)));
            browserPanel.addView(nav,new LinearLayout.LayoutParams(-1,dp(40)));
            browserContext=new MutableContextWrapper(fullscreenActivity!=null?fullscreenActivity:this);
            browser=new WebView(browserContext);WebSettings prefs=browser.getSettings();prefs.setJavaScriptEnabled(true);prefs.setDomStorageEnabled(true);prefs.setUseWideViewPort(true);prefs.setLoadWithOverviewMode(true);prefs.setAllowFileAccess(false);prefs.setAllowContentAccess(false);prefs.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);prefs.setSupportMultipleWindows(true);prefs.setMediaPlaybackRequiresUserGesture(true);
            browser.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT,false);
            browser.setWebViewClient(new WebViewClient(){
                @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){if(!req.isForMainFrame())return false;String address=req.getUrl().toString();NavigationPolicy.Route route=NavigationPolicy.route(address);if(route==NavigationPolicy.Route.GOOGLE_AUTH){if(OAuthHandshake.isStart(address))startGoogleLogin(address);else googleAuthNotice();return true;}return route==NavigationPolicy.Route.BLOCKED;}
                @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){return blockGoogleAuth(req);}
                @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap favicon){if(v==browser&&browserAddress!=null)browserAddress.setText(Uri.parse(url).getHost());}
                @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){
                    boolean current=v==browser;releaseBrowser(v);
                    if(current){if(game!=null&&expanded)game.requestFocus();Toast.makeText(FloatingGameService.this,AppLanguage.text(FloatingGameService.this,R.string.browser_renderer_closed),Toast.LENGTH_LONG).show();}
                    return true;
                }
            });
            browser.setWebChromeClient(new WebChromeClient(){@Override public void onCloseWindow(WebView window){if(window==browser)closeBrowser();}});
            browserPanel.addView(browser,new LinearLayout.LayoutParams(-1,0,1));
            attachBrowser();
            back.setOnClickListener(v->{if(browser!=null&&browser.canGoBack())browser.goBack();else closeBrowser();});close.setOnClickListener(v->closeBrowser());
            browser.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP){if(browser!=null&&browser.canGoBack())browser.goBack();else closeBrowser();return true;}return false;});
        }
        if(address!=null)browser.loadUrl(address);
        browser.requestFocus();
    }
    private void closeBrowser(){
        WebView closing=browser;
        if(closing!=null){closing.stopLoading();releaseBrowser(closing);}
        else{detach(browserPanel);browserPanel=null;browserContext=null;browserAddress=null;browserReturnButton=null;}
        if(game!=null&&expanded)game.requestFocus();
    }
    private void releaseBrowser(WebView closing){
        if(closing==null||!releasedBrowsers.add(closing))return;
        // Renderer callbacks can arrive for an older popup. Release only that view.
        if(closing==browser){
            browser=null;detach(browserPanel);browserPanel=null;
            if(browserContext!=null)browserContext.setBaseContext(this);
            browserContext=null;browserAddress=null;browserReturnButton=null;
        }
        detach(closing);closing.destroy();
    }
    private void attachBrowser(){
        if(browserPanel==null||browser==null)return;
        FrameLayout host=fullscreenHost!=null?fullscreenHost:root;
        if(host==null)return;
        if(browserContext!=null)browserContext.setBaseContext(fullscreenActivity!=null?fullscreenActivity:this);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);
        lp.topMargin=fullscreenHost==null?toolbarHeight():0;
        lp.bottomMargin=fullscreenHost==null?dp(32):0;
        if(browserPanel.getParent()!=host){detach(browserPanel);host.addView(browserPanel,lp);}
        else browserPanel.setLayoutParams(lp);
        showBrowser();
    }
    private void showBrowser(){
        if(browserPanel==null||browser==null)return;
        boolean visible=fullscreenHost!=null||expanded;
        browserPanel.setVisibility(visible?View.VISIBLE:View.GONE);
        if(visible){browser.onResume();browser.resumeTimers();browser.requestFocus();}
    }
    private void inject(){if(game!=null&&game.getUrl()!=null&&NavigationPolicy.route(game.getUrl())==NavigationPolicy.Route.GAME){game.evaluateJavascript(backgroundScript,null);updateFrameMode();updatePickerLanguage();game.evaluateJavascript(selectScript,null);applyPerformance();}}
    private void applyPerformance(){
        if(game!=null&&game.getUrl()!=null&&NavigationPolicy.route(game.getUrl())==NavigationPolicy.Route.GAME){
            game.evaluateJavascript("window.__lumivaraFpsLimit="+PerformanceSettings.limit(this)+";"+performanceScript,null);
        }
        if(gameViewport!=null)gameViewport.setRenderScale(PerformanceSettings.scale(this)/100f);
        if(fullscreenViewport!=null)fullscreenViewport.setRenderScale(PerformanceSettings.scale(this)/100f);
        updateRefreshRate();
    }
    private void updateRefreshRate(){
        boolean interactive=powerManager==null||powerManager.isInteractive();
        if(root!=null&&params!=null&&wm!=null){
            PerformanceSettings.request(this,root.getDisplay(),params,interactive&&expanded&&fullscreenActivity==null);
            if(root.isAttachedToWindow())try{wm.updateViewLayout(root,params);}catch(Exception ignored){}
        }
        if(fullscreenActivity!=null)fullscreenActivity.requestRefreshRate(interactive);
    }
    private void updatePickerLanguage(){
        if(game!=null&&game.getUrl()!=null&&NavigationPolicy.route(game.getUrl())==NavigationPolicy.Route.GAME){
            // Only labels owned by our dropdown helper. Do not change game text/storage.
            String locale=AppLanguage.effective(this);
            game.evaluateJavascript("window.__lumivaraSelectPickerLanguage="+org.json.JSONObject.quote(locale)+";if(window.__lumivaraPickerLocale)window.__lumivaraPickerLocale.setLocale(window.__lumivaraSelectPickerLanguage);",null);
        }
    }
    private void refreshLanguage(){
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel=manager.getNotificationChannel("floating_game");
        if(channel!=null){channel.setName(AppLanguage.text(this,R.string.floating_channel));channel.setDescription(AppLanguage.text(this,R.string.channel_description));manager.createNotificationChannel(channel);}
        if(miniButton!=null)miniButton.setContentDescription(AppLanguage.text(this,R.string.toggle_mini));
        if(helpButton!=null)helpButton.setContentDescription(AppLanguage.text(this,miniature?R.string.window_actions:R.string.help_description));
        if(fullscreenButton!=null)fullscreenButton.setContentDescription(AppLanguage.text(this,R.string.fullscreen_description));
        if(collapseButton!=null)collapseButton.setContentDescription(AppLanguage.text(this,R.string.collapse_description));
        if(stopButton!=null)stopButton.setContentDescription(AppLanguage.text(this,R.string.close_description));
        if(bubble!=null)bubble.setContentDescription(AppLanguage.text(this,R.string.bubble_description));
        if(title!=null){int progress=game==null?100:game.getProgress();title.setText(toolbarTitle(progress));title.setContentDescription(windowTitle());}
        if(resizeFooter!=null){((TextView)resizeFooter.getChildAt(0)).setText(AppLanguage.text(this,R.string.resize));int[] ids={R.string.resize_width,R.string.resize_height,R.string.resize_both};for(int i=0;i<ids.length;i++)resizeFooter.getChildAt(i+1).setContentDescription(AppLanguage.text(this,ids[i]));}
        if(browserReturnButton!=null)browserReturnButton.setText(AppLanguage.text(this,R.string.return_game));
        updatePickerLanguage();if(running)updateNotification();
    }
    private void updateFrameMode(){
        updateRefreshRate();
        handler.removeCallbacks(framePump);frameGeneration++;
        frameFallback=BackgroundFramePolicy.needsFallback(powerManager==null||powerManager.isInteractive(),expanded,fullscreenActivity!=null);
        if(game!=null&&NavigationPolicy.route(game.getUrl())==NavigationPolicy.Route.GAME){
            game.evaluateJavascript("if(location.origin==='https://lumivaraonline.com'&&window.__lumivaraFrameControl)window.__lumivaraFrameControl.setBackground("+frameFallback+");",null);
            if(running&&frameFallback)handler.post(framePump);
        }
    }
    private void pumpBackgroundFrame(){
        if(!running||!frameFallback||game==null||NavigationPolicy.route(game.getUrl())!=NavigationPolicy.Route.GAME)return;
        final int generation=frameGeneration;
        // Schedule the next pulse only after this one completes: no JS queue buildup.
        game.evaluateJavascript("if(location.origin==='https://lumivaraonline.com'&&window.__lumivaraFrameControl)window.__lumivaraFrameControl.nativeTick();",result->{
            if(running&&frameFallback&&generation==frameGeneration)handler.postDelayed(framePump,BackgroundFramePolicy.PULSE_INTERVAL_MS);
        });
    }
    private Button tool(String text,View.OnClickListener listener){Button b=new Button(this);b.setText(text);b.setTextSize(17);b.setTextColor(Color.rgb(248,241,224));b.setBackgroundColor(Color.TRANSPARENT);b.setPadding(0,0,0,0);b.setMinWidth(0);b.setMinimumWidth(0);bar.addView(b,new LinearLayout.LayoutParams(dp(48),-1));b.setOnClickListener(listener);return b;}
    private static void detach(View view){if(view!=null&&view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);}
    private void attachGame(FrameLayout host){
        dismissGameDialog();
        detach(game);
        game.setScaleX(1);game.setScaleY(1);game.setTranslationX(0);game.setTranslationY(0);
        gameContext.setBaseContext(fullscreenActivity!=null?fullscreenActivity:this);
        if(host==fullscreenHost){
            if(fullscreenViewport!=null)detach(fullscreenViewport);
            fullscreenViewport=new MiniGameLayout(this);fullscreenViewport.setRenderScale(PerformanceSettings.scale(this)/100f);
            host.addView(fullscreenViewport,0,new FrameLayout.LayoutParams(-1,-1));fullscreenViewport.addView(game,new ViewGroup.LayoutParams(-1,-1));
        }
        else{gameViewport.addView(game,new ViewGroup.LayoutParams(-1,-1));layoutGame();}
        game.requestLayout();
        attachBrowser();
    }
    private void openFullscreen(){
        if(game==null)return;
        ProfileComponents.minimizeOthers(this,profileSlot());
        try{startActivity(new Intent(this,ProfileComponents.fullscreen(profileSlot())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
        catch(Exception error){Toast.makeText(this,AppLanguage.text(FloatingGameService.this,R.string.fullscreen_failed),Toast.LENGTH_LONG).show();}
    }
    boolean attachFullscreen(GameFullscreenActivity owner,FrameLayout host){
        if(!running||game==null||root==null)return false;
        if(fullscreenActivity!=null&&fullscreenActivity!=owner)return false;
        collapse();fullscreenActivity=owner;fullscreenHost=host;attachGame(host);
        root.setVisibility(View.GONE);game.onResume();game.resumeTimers();game.requestFocus();showBrowser();updateFrameMode();updateNotification();return true;
    }
    void returnToOverlay(GameFullscreenActivity owner,boolean minimized){
        if(fullscreenActivity!=owner)return;
        fullscreenActivity=null;fullscreenHost=null;fullscreenViewport=null;
        if(!running||game==null||root==null)return;
        attachGame(root);root.setVisibility(View.VISIBLE);owner.finish();
        if(minimized)collapse();else expand();
    }
    void returnToMini(GameFullscreenActivity owner){miniature=true;returnToOverlay(owner,false);}
    private int toolbarHeight(){return dp(48);}
    private void clampMini(){miniWidth=FitGeometry.boundedSize(miniWidth,dp(180),width());miniHeight=FitGeometry.boundedSize(miniHeight,dp(144),height());miniX=FitGeometry.position(miniX,miniWidth,width());miniY=FitGeometry.position(miniY,miniHeight,height());}
    private void saveMini(){getSharedPreferences(windowPreferences(),0).edit().putInt("miniWidth",miniWidth).putInt("miniHeight",miniHeight).putInt("miniX",miniX).putInt("miniY",miniY).apply();}
    private void windowMenu(){
        String[] choices={AppLanguage.text(this,R.string.normal_window),AppLanguage.text(this,R.string.fullscreen_description),AppLanguage.text(this,R.string.help_title),AppLanguage.text(this,R.string.diagnostics)};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(windowTitle()).setItems(choices,(menu,choice)->{
            if(choice==0){miniature=false;expand();}else if(choice==1)openFullscreen();else if(choice==2)help();else showDiagnostics();
        }).create();overlayDialog(dialog);
    }
    private void toggleMini(){if(expanded&&miniature){miniX=params.x;miniY=params.y;saveMini();}miniature=!miniature;expand();}
    private void layoutGame(){if(game==null||fullscreenHost!=null)return;clampMini();FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)gameViewport.getLayoutParams();lp.width=miniature?miniWidth:Math.max(dp(240),width()-dp(24));lp.height=Math.max(1,(miniature?miniHeight:Math.max(dp(280),height()-dp(80)))-toolbarHeight()-dp(32));lp.topMargin=toolbarHeight();gameViewport.setMiniature(miniature);gameViewport.setRenderScale(PerformanceSettings.scale(this)/100f);gameViewport.setLayoutParams(lp);}
    private void expand(){
        if(root==null||game==null)return;
        if(fullscreenActivity!=null){returnToOverlay(fullscreenActivity,false);return;}
        if(!expanded){bubbleX=params.x;bubbleY=params.y;}
        expanded=true;layoutGame();params.width=miniature?miniWidth:Math.max(dp(240),width()-dp(24));params.height=miniature?miniHeight:Math.max(dp(280),height()-dp(80));
        title.setText(toolbarTitle(game.getProgress()));title.setContentDescription(windowTitle());helpButton.setContentDescription(AppLanguage.text(this,miniature?R.string.window_actions:R.string.help_description));
        if(profileBadge!=null)profileBadge.setVisibility(View.GONE);
        params.x=miniature?miniX:dp(12);params.y=miniature?miniY:dp(24);params.flags=WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        FrameLayout.LayoutParams barLp=(FrameLayout.LayoutParams)bar.getLayoutParams();barLp.height=toolbarHeight();bar.setLayoutParams(barLp);helpButton.setVisibility(View.VISIBLE);helpButton.setText(miniature?"⋮":"?");miniButton.setVisibility(miniature?View.GONE:View.VISIBLE);fullscreenButton.setVisibility(miniature?View.GONE:View.VISIBLE);stopButton.setVisibility(View.VISIBLE);miniButton.setText(miniature?"▢":"▣");resizeFooter.setVisibility(View.VISIBLE);
        bar.setVisibility(View.VISIBLE);bubble.setVisibility(View.GONE);root.setDescendantFocusability(android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS);
        try{wm.updateViewLayout(root,params);game.onResume();game.resumeTimers();game.requestFocus();showBrowser();updateFrameMode();updateNotification();}catch(Exception e){stopSelf();}
    }
    void minimizeForPage(){collapse();}
    private void collapse(){
        dismissGameDialog();
        if(fullscreenActivity!=null){returnToOverlay(fullscreenActivity,true);return;}
        if(root==null)return;if(expanded&&miniature){miniX=params.x;miniY=params.y;saveMini();}expanded=false;
        // Keep the SAME live, attached WebView at its full layout size under the bubble.
        // No GONE, onPause, pauseTimers, detach, destroy, or navigation on collapse.
        params.width=dp(56);params.height=dp(56);params.x=Math.max(0,Math.min(bubbleX,width()-dp(56)));params.y=Math.max(0,Math.min(bubbleY,height()-dp(56)));
        params.flags=WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        bar.setVisibility(View.GONE);bubble.setVisibility(View.VISIBLE);root.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        resizeFooter.setVisibility(View.GONE);
        if(profileBadge!=null)profileBadge.setVisibility(View.GONE);
        showBrowser();
        try{wm.updateViewLayout(root,params);game.onResume();game.resumeTimers();updateFrameMode();CookieManager.getInstance().flush();updateNotification();}catch(Exception e){stopSelf();}
    }
    private void drag(View handle,boolean clickable){handle.setOnTouchListener(new View.OnTouchListener(){float x,y;int ox,oy;boolean moved;public boolean onTouch(View v,android.view.MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN){x=event.getRawX();y=event.getRawY();ox=params.x;oy=params.y;moved=false;return true;}
        if(event.getAction()==MotionEvent.ACTION_MOVE){float dx=event.getRawX()-x,dy=event.getRawY()-y;if(Math.abs(dx)+Math.abs(dy)>dp(6))moved=true;if(moved){params.x=Math.max(0,Math.min(width()-params.width,ox+(int)dx));params.y=Math.max(0,Math.min(height()-params.height,oy+(int)dy));try{wm.updateViewLayout(root,params);}catch(Exception ignored){}}return true;}
        if(event.getAction()==MotionEvent.ACTION_UP){if(clickable&&!moved)expand();else if(!expanded){bubbleX=params.x;bubbleY=params.y;getSharedPreferences(windowPreferences(),0).edit().putInt("x",bubbleX).putInt("y",bubbleY).apply();}else if(miniature){miniX=params.x;miniY=params.y;saveMini();}return true;}return true;
    }});}
    private TextView resizeControl(String label,String description,boolean horizontal,boolean vertical){
        TextView control=new TextView(this);control.setText(label);control.setTextColor(Color.rgb(243,211,139));control.setTextSize(20);control.setGravity(Gravity.CENTER);control.setContentDescription(description);resizeFooter.addView(control,new LinearLayout.LayoutParams(dp(36),-1));
        control.setOnTouchListener(new View.OnTouchListener(){float startX,startY;int startWidth,startHeight;public boolean onTouch(View view,MotionEvent event){
            if(!expanded)return false;
            if(event.getAction()==MotionEvent.ACTION_DOWN){startX=event.getRawX();startY=event.getRawY();startWidth=params.width;startHeight=params.height;miniWidth=startWidth;miniHeight=startHeight;miniX=params.x;miniY=params.y;miniature=true;return true;}
            if(event.getAction()==MotionEvent.ACTION_MOVE){miniWidth=FitGeometry.boundedSize(startWidth+(horizontal?(int)(event.getRawX()-startX):0),dp(180),width()-params.x);miniHeight=FitGeometry.boundedSize(startHeight+(vertical?(int)(event.getRawY()-startY):0),dp(144),height()-params.y);layoutGame();params.width=miniWidth;params.height=miniHeight;helpButton.setVisibility(View.VISIBLE);helpButton.setText("⋮");miniButton.setVisibility(View.GONE);fullscreenButton.setVisibility(View.GONE);stopButton.setVisibility(View.VISIBLE);miniButton.setText("▢");try{wm.updateViewLayout(root,params);}catch(Exception ignored){}return true;}
            if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){saveMini();expand();return true;}return true;
        }});return control;
    }
    private void updateNotification(){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(notificationId(),notification());}
    private void overlayDialog(AlertDialog dialog){dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);dialog.show();}
    private void help(){AlertDialog dialog=new AlertDialog.Builder(this).setTitle(AppLanguage.text(FloatingGameService.this,R.string.help_title)).setMessage(AppLanguage.text(FloatingGameService.this,R.string.overlay_help)).setPositiveButton(AppLanguage.text(FloatingGameService.this,R.string.ok),null).setNeutralButton(AppLanguage.text(FloatingGameService.this,R.string.reload_game),(d,w)->game.reload()).create();overlayDialog(dialog);}
    private void showDiagnostics(){
        if(diagnostics==null)return;diagnostics.capture();
        TextView text=new TextView(this);text.setText(AppLanguage.text(this,R.string.diagnostics_help)+"\n\n"+diagnostics.report());text.setTextSize(12);text.setTextIsSelectable(true);text.setPadding(dp(16),dp(12),dp(16),dp(12));
        ScrollView scroll=new ScrollView(this);scroll.addView(text);scroll.setLayoutParams(new ViewGroup.LayoutParams(-1,dp(360)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(AppLanguage.text(this,R.string.diagnostics)+" · "+ProfileComponents.label(this,profileSlot())).setView(scroll)
            .setPositiveButton(AppLanguage.text(this,R.string.ok),null).setNeutralButton(AppLanguage.text(this,R.string.copy_report),(d,w)->{
                ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Lumivara diagnostics",diagnostics.report()));
                Toast.makeText(this,AppLanguage.text(this,R.string.report_copied),Toast.LENGTH_SHORT).show();
            }).create();overlayDialog(dialog);
    }
    private void disableGameSaver(){
        if(game==null)return;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(AppLanguage.text(this,R.string.saver_setting)).setMessage(AppLanguage.text(this,R.string.saver_explanation))
            .setNegativeButton(AppLanguage.text(this,R.string.cancel),null).setPositiveButton(AppLanguage.text(this,R.string.saver_apply),(d,w)->{
                game.evaluateJavascript("(function(){if(location.origin!=='https://lumivaraonline.com')return false;const s=document.getElementById('battery-saver-idle');if(!s||![...s.options].some(o=>o.value==='off'))return false;s.value='off';s.dispatchEvent(new Event('change',{bubbles:true}));return true;})()",value->Toast.makeText(this,AppLanguage.text(this,"true".equals(value)?R.string.saver_applied:R.string.saver_unavailable),Toast.LENGTH_LONG).show());
            }).create();overlayDialog(dialog);
    }
    private void confirmStop(){overlayDialog(new AlertDialog.Builder(this).setTitle(AppLanguage.text(FloatingGameService.this,R.string.stop_confirm_title)+" · "+ProfileComponents.label(this,profileSlot())).setMessage(AppLanguage.text(FloatingGameService.this,R.string.stop_confirm_body)).setNegativeButton(AppLanguage.text(FloatingGameService.this,R.string.cancel),null).setPositiveButton(AppLanguage.text(FloatingGameService.this,R.string.close_game),(d,w)->stopSelf()).create());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(root==null)return START_NOT_STICKY;
        String action=intent==null?COLLAPSE:intent.getAction();if(STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(FULLSCREEN.equals(action)){expand();openFullscreen();}else if(MINI.equals(action)){miniature=true;expand();}else if(EXPAND.equals(action))expand();else collapse();return START_STICKY;
    }
    @Override public void onTaskRemoved(Intent rootIntent){super.onTaskRemoved(rootIntent);/* Service owns the WebView; task removal does not close it. */}
    @Override public android.os.IBinder onBind(Intent intent){return new Messenger(new Handler(Looper.getMainLooper(),message->{
        if(message.sendingUid!=android.os.Process.myUid())return true;
        boolean accepted=false;
        if(message.what==ProfileCommands.CALLBACK){String candidate=message.getData().getString("address");accepted=CallbackCandidate.valid(candidate)&&acceptGoogleCallback(candidate);}
        else if(message.what==ProfileCommands.MINIMIZE){if(running){collapse();accepted=true;}}
        else if(message.what==ProfileCommands.PING){accepted=running&&root!=null&&game!=null;}
        else if(message.what==ProfileCommands.DIAGNOSTICS){accepted=running&&diagnostics!=null;if(accepted)showDiagnostics();}
        else if(message.what==ProfileCommands.SAVER){accepted=running&&game!=null;if(accepted)disableGameSaver();}
                if(message.replyTo!=null){try{Message reply=Message.obtain(null,message.what);reply.arg1=accepted?1:0;message.replyTo.send(reply);}catch(RemoteException ignored){}}
        return true;
    })).getBinder();}
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);refreshLanguage();if(fullscreenHost!=null){if(game!=null)game.requestLayout();return;}if(expanded)expand();else{layoutGame();collapse();}}
    @Override public void onDestroy(){running=false;if(diagnostics!=null)diagnostics.stop();frameGeneration++;handler.removeCallbacks(framePump);if(screenReceiverRegistered){try{unregisterReceiver(screenReceiver);}catch(Exception ignored){}screenReceiverRegistered=false;}active=null;try{unregisterReceiver(menuLanguageReceiver);}catch(Exception ignored){}pendingLogin=null;approvedCallback=null;if(appSettings!=null)appSettings.unregisterOnSharedPreferenceChangeListener(languageListener);handler.removeCallbacksAndMessages(null);dismissGameDialog();if(fullscreenActivity!=null)fullscreenActivity.finish();fullscreenActivity=null;fullscreenHost=null;if(authNotice!=null)authNotice.dismiss();closeBrowser();if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();CookieManager.getInstance().flush();if(root!=null&&wm!=null)try{wm.removeView(root);}catch(Exception ignored){}if(game!=null){detach(game);game.stopLoading();game.destroy();game=null;}stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
