package com.lumivaraonline.fps;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.*;
import java.io.InputStream;
import java.io.OutputStream;

/** Local QR screen; image saving is explicit and never initiates a bank transaction. */
public class DonateActivity extends Activity {
    private static final String QR_ASSET="donate_qr.png";
    private static final int CREATE_QR=18;
    private static final int GOLD=Color.rgb(219,183,99),WHITE=Color.rgb(248,241,224);
    private Button save;
    private boolean saving;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private String text(int id){return AppLanguage.text(this,id);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(20,35,71));
        FrameLayout container=new FrameLayout(this);scroll.addView(container);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(16),dp(20),dp(16),dp(24));
        int width=Math.min(dp(480),getResources().getDisplayMetrics().widthPixels);
        container.addView(page,new FrameLayout.LayoutParams(width,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=safe.left;top=safe.top;right=safe.right;bottom=safe.bottom;}
            else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}
            scroll.setPadding(left,top,right,bottom);
            FrameLayout.LayoutParams layout=(FrameLayout.LayoutParams)page.getLayoutParams();layout.width=Math.max(1,Math.min(dp(480),getResources().getDisplayMetrics().widthPixels-left-right));page.setLayoutParams(layout);return insets;
        });scroll.requestApplyInsets();
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setText("‹");back.setTextSize(28);back.setTextColor(GOLD);back.setBackgroundColor(Color.TRANSPARENT);back.setPadding(0,0,0,0);back.setMinWidth(0);back.setMinimumWidth(0);back.setContentDescription(text(R.string.donate_back));back.setOnClickListener(v->finish());
        header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView title=new TextView(this);title.setText(text(R.string.donate_title));title.setTextColor(GOLD);title.setTextSize(25);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));page.addView(header);
        TextView welcome=paragraph(text(R.string.donate_welcome),18,GOLD);welcome.setGravity(Gravity.CENTER);page.addView(welcome);
        page.addView(paragraph(text(R.string.donate_scan_instructions),14,WHITE));
        save=button(text(R.string.donate_save));save.setOnClickListener(v->saveQr());page.addView(save);
        ImageView qr=new ImageView(this);qr.setAdjustViewBounds(true);qr.setScaleType(ImageView.ScaleType.FIT_CENTER);qr.setBackgroundColor(Color.WHITE);qr.setContentDescription(text(R.string.donate_qr_description));
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,-2);imageParams.setMargins(0,dp(16),0,dp(16));page.addView(qr,imageParams);
        try(InputStream source=getAssets().open(QR_ASSET)){
            Bitmap bitmap=BitmapFactory.decodeStream(source);if(bitmap==null)throw new IllegalStateException();
            BitmapDrawable drawable=new BitmapDrawable(getResources(),bitmap);drawable.setFilterBitmap(false);qr.setImageDrawable(drawable);
        }catch(Exception error){save.setEnabled(false);page.addView(paragraph(text(R.string.donate_image_failed),15,WHITE));}
        Button close=button(text(R.string.donate_back));close.setOnClickListener(v->finish());page.addView(close);
    }
    private TextView paragraph(String content,int size,int color){TextView view=new TextView(this);view.setText(content);view.setTextColor(color);view.setTextSize(size);view.setPadding(0,dp(10),0,dp(10));return view;}
    private Button button(String label){Button view=new Button(this);view.setText(label);view.setAllCaps(false);view.setTextColor(WHITE);view.setTextSize(15);GradientDrawable background=new GradientDrawable();background.setColor(Color.rgb(38,63,112));background.setCornerRadius(dp(10));background.setStroke(dp(1),GOLD);view.setBackground(background);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,dp(52));layout.setMargins(0,dp(6),0,dp(6));view.setLayoutParams(layout);return view;}
    private void saveQr(){
        if(saving)return;saving=true;save.setEnabled(false);
        if(Build.VERSION.SDK_INT>=29){saveToPictures();return;}
        Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/png").putExtra(Intent.EXTRA_TITLE,"Lumivara-coffee-QR.png");
        try{startActivityForResult(create,CREATE_QR);}catch(Exception error){saved(false,false);}
    }
    private void saveToPictures(){
        new Thread(()->{
            Uri created=null;boolean success=false;
            try{
                ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"Lumivara-coffee-QR.png");values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/Lumivara");values.put(MediaStore.Images.Media.IS_PENDING,1);
                created=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(created==null)throw new IllegalStateException();
                copyOriginal(created);values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0);
                if(getContentResolver().update(created,values,null,null)==0)throw new IllegalStateException();success=true;
            }catch(Exception error){
                // Remove only our newly-created incomplete MediaStore item.
                if(created!=null)try{getContentResolver().delete(created,null,null);}catch(Exception ignored){}
            }
            final boolean done=success;runOnUiThread(()->saved(done,false));
        },"LumivaraQrSave").start();
    }
    private void copyOriginal(Uri destination)throws Exception{
        if(!"content".equals(destination.getScheme()))throw new IllegalArgumentException();
        try(InputStream source=getAssets().open(QR_ASSET);OutputStream target=getContentResolver().openOutputStream(destination,"w")){
            if(target==null)throw new IllegalStateException();byte[] buffer=new byte[8192];int size;while((size=source.read(buffer))!=-1)target.write(buffer,0,size);
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(request!=CREATE_QR)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){saved(false,true);return;}
        final Uri destination=data.getData();
        new Thread(()->{boolean success=false;try{copyOriginal(destination);success=true;}catch(Exception ignored){}final boolean done=success;runOnUiThread(()->saved(done,false));},"LumivaraQrDocument").start();
    }
    private void saved(boolean success,boolean cancelled){
        saving=false;if(isFinishing()||isDestroyed())return;save.setEnabled(true);
        if(!cancelled)Toast.makeText(this,text(success?R.string.donate_saved:R.string.donate_save_failed),Toast.LENGTH_LONG).show();
    }
}
