package com.lumivaraonline.fps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private TextView status;
    private ScrollView scroll;
    private String renderedLanguage;
    private int selectedProfile;
    private final int GOLD=Color.rgb(219,183,99), WHITE=Color.rgb(248,241,224);
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        int requested=getIntent().getIntExtra("profile",getPreferences(0).getInt("profile",0));
        selectedProfile=requested>=0&&requested<ProfileIdentity.supportedCount(Build.VERSION.SDK_INT)?requested:0;
        renderPage();
    }
    private void renderPage(){
        final int position=scroll==null?0:scroll.getScrollY();
        renderedLanguage=AppLanguage.effective(this);
        scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(24),dp(32),dp(24),dp(24));
        page.setBackgroundColor(Color.rgb(20,35,71));scroll.addView(page);setContentView(scroll);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.game_icon);page.addView(icon,new LinearLayout.LayoutParams(dp(76),dp(76)));
        text(page,"Lumivara Online FPS",28,GOLD,true);
        text(page,AppLanguage.text(MainActivity.this,R.string.app_subtitle),15,WHITE,false);
        button(page,AppLanguage.text(this,R.string.app_language)+": "+AppLanguage.label(this),v->chooseLanguage());
        text(page,AppLanguage.text(this,R.string.language_scope),13,GOLD,false);
        text(page,AppLanguage.text(this,R.string.fps_intro),14,WHITE,false);
        button(page,AppLanguage.text(this,R.string.fps_setting)+": "+PerformanceSettings.fpsLabel(this),v->chooseFps());
        button(page,AppLanguage.text(this,R.string.render_setting)+": "+PerformanceSettings.scale(this)+"%",v->chooseScale());
        text(page,AppLanguage.text(this,R.string.render_help),13,GOLD,false);
        text(page,AppLanguage.text(MainActivity.this,R.string.setup_intro),16,WHITE,false);
        button(page,AppLanguage.text(this,R.string.installation_video),v->{
            if(FloatingGameService.active!=null)FloatingGameService.active.minimizeForPage();
            startActivity(new Intent(this,InstallationVideoActivity.class));
        });
        status=text(page,"",14,GOLD,false);
        button(page,AppLanguage.text(MainActivity.this,R.string.permission_overlay),v->open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        button(page,AppLanguage.text(MainActivity.this,R.string.permission_notifications),v->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);else Toast.makeText(this,AppLanguage.text(MainActivity.this,R.string.notifications_already_available),Toast.LENGTH_SHORT).show();});
        button(page,AppLanguage.text(MainActivity.this,R.string.permission_battery),v->{
            PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
            if(power.isIgnoringBatteryOptimizations(getPackageName())){Toast.makeText(this,AppLanguage.text(MainActivity.this,R.string.battery_already_exempt),Toast.LENGTH_SHORT).show();return;}
            new AlertDialog.Builder(this).setTitle(AppLanguage.text(MainActivity.this,R.string.battery_dialog_title)).setMessage(AppLanguage.text(MainActivity.this,R.string.battery_dialog_body)).setNegativeButton(AppLanguage.text(MainActivity.this,R.string.cancel),null).setPositiveButton(AppLanguage.text(MainActivity.this,R.string.open_settings),(d,w)->open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())))).show();
        });
        button(page,AppLanguage.text(MainActivity.this,R.string.permission_login_links),v->GoogleLoginActivity.openLinkSettings(this));
        button(page,AppLanguage.text(this,R.string.fps_play),v->startFloating(FloatingGameService.FULLSCREEN));
        button(page,AppLanguage.text(MainActivity.this,R.string.start_bubble),v->startFloating(FloatingGameService.COLLAPSE));
        button(page,AppLanguage.text(MainActivity.this,R.string.resume_game),v->startFloating(FloatingGameService.EXPAND));
        button(page,AppLanguage.text(MainActivity.this,R.string.start_mini),v->startFloating(FloatingGameService.MINI));
        button(page,AppLanguage.text(MainActivity.this,R.string.stop_game_service),v->{stopService(ProfileComponents.serviceIntent(this,selectedProfile,FloatingGameService.STOP));refresh();});
        button(page,AppLanguage.text(this,R.string.donate_button),v->{
            if(FloatingGameService.active!=null)FloatingGameService.active.minimizeForPage();
            startActivity(new Intent(this,DonateActivity.class));
        });
        text(page,AppLanguage.text(MainActivity.this,R.string.performance_note),14,GOLD,false);
        text(page,AppLanguage.text(MainActivity.this,R.string.main_help),15,WHITE,false);
        text(page,AppLanguage.text(MainActivity.this,R.string.oem_battery_note),14,WHITE,false);
        text(page,AppLanguage.text(MainActivity.this,R.string.engine_auth_note),13,WHITE,false);
        refresh();
        scroll.post(()->scroll.scrollTo(0,position));
    }
    private void chooseLanguage(){
        String[] names={AppLanguage.text(this,R.string.language_thai),AppLanguage.text(this,R.string.language_english),AppLanguage.text(this,R.string.language_system)};
        new AlertDialog.Builder(this).setTitle(AppLanguage.text(this,R.string.language_picker_title))
            .setSingleChoiceItems(names,LanguageChoice.index(AppLanguage.choice(this)),(dialog,which)->{
                if(which<0||which>=LanguageChoice.MODES.length)return;
                AppLanguage.set(this,LanguageChoice.MODES[which]);dialog.dismiss();renderPage();
            }).setNegativeButton(AppLanguage.text(this,R.string.cancel),null).show();
    }
    private void chooseFps(){
        String[] names={AppLanguage.text(this,R.string.fps_max),"120 FPS","60 FPS"};
        int choice=PerformanceSettings.limit(this),index=choice==120?1:choice==60?2:0;
        new AlertDialog.Builder(this).setTitle(AppLanguage.text(this,R.string.fps_setting)).setSingleChoiceItems(names,index,(dialog,which)->{
            AppLanguage.preferences(this).edit().putInt(PerformanceSettings.FPS,new int[]{0,120,60}[which]).apply();dialog.dismiss();renderPage();
        }).setNegativeButton(AppLanguage.text(this,R.string.cancel),null).show();
    }
    private void chooseScale(){
        int[] scales={100,85,70};String[] names={"100%","85%","70%"};
        int value=PerformanceSettings.scale(this),index=value==100?0:value==70?2:1;
        new AlertDialog.Builder(this).setTitle(AppLanguage.text(this,R.string.render_setting)).setSingleChoiceItems(names,index,(dialog,which)->{
            AppLanguage.preferences(this).edit().putInt(PerformanceSettings.SCALE,scales[which]).apply();dialog.dismiss();renderPage();
        }).setNegativeButton(AppLanguage.text(this,R.string.cancel),null).show();
    }
    private TextView text(LinearLayout p,String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(10),0,dp(10));if(bold)t.setTypeface(null,Typeface.BOLD);p.addView(t);return t;}
    private void button(LinearLayout p,String s,View.OnClickListener listener){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(WHITE);b.setTextSize(15);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(38,63,112));bg.setCornerRadius(dp(10));bg.setStroke(dp(1),GOLD);b.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(8),0,dp(4));p.addView(b,lp);b.setOnClickListener(listener);}
    private void open(Intent i){try{startActivity(i);}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception ignored){Toast.makeText(this,AppLanguage.text(MainActivity.this,R.string.open_settings_manually),Toast.LENGTH_LONG).show();}}}
    private void startFloating(String action){

        if(!Settings.canDrawOverlays(this)){new AlertDialog.Builder(this).setMessage(AppLanguage.text(MainActivity.this,R.string.overlay_required)).setPositiveButton(AppLanguage.text(MainActivity.this,R.string.ok),null).show();return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);Toast.makeText(this,AppLanguage.text(MainActivity.this,R.string.notifications_required),Toast.LENGTH_LONG).show();return;}
        try{
            startForegroundService(ProfileComponents.serviceIntent(this,0,action));
            moveTaskToBack(true);
        }catch(Exception e){new AlertDialog.Builder(this).setMessage(AppLanguage.text(MainActivity.this,R.string.service_start_failed)+e.getMessage()).setPositiveButton(AppLanguage.text(MainActivity.this,R.string.ok),null).show();}
    }
    @Override protected void onResume(){
        super.onResume();
        // Settings must remain reachable even when several overlay windows were expanded.
        for(int slot=0;slot<ProfileIdentity.supportedCount(Build.VERSION.SDK_INT);slot++)ProfileCommands.send(this,slot,ProfileCommands.MINIMIZE,null,accepted->{});
        if(renderedLanguage!=null&&!renderedLanguage.equals(AppLanguage.effective(this)))renderPage();else refresh();
    }
    private void refresh(){if(status==null)return;PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);boolean notices=Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;status.setText(AppLanguage.text(MainActivity.this,R.string.status_overlay)+(Settings.canDrawOverlays(this)?AppLanguage.text(MainActivity.this,R.string.ready):AppLanguage.text(MainActivity.this,R.string.not_allowed))+AppLanguage.text(MainActivity.this,R.string.status_notifications)+(notices?AppLanguage.text(MainActivity.this,R.string.ready):AppLanguage.text(MainActivity.this,R.string.not_allowed))+AppLanguage.text(MainActivity.this,R.string.status_battery)+(p.isIgnoringBatteryOptimizations(getPackageName())?AppLanguage.text(MainActivity.this,R.string.ready):AppLanguage.text(MainActivity.this,R.string.not_exempt))+AppLanguage.text(MainActivity.this,R.string.status_service)+ProfileComponents.label(this,selectedProfile)+": "+(ProfileComponents.isRunning(this,selectedProfile)?AppLanguage.text(MainActivity.this,R.string.running):AppLanguage.text(MainActivity.this,R.string.not_started)));}
}
