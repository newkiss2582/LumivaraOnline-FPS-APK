const fs=require('fs'),path=require('path'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'..'),dir=path.join(root,'build/display-request-test');fs.mkdirSync(dir,{recursive:true});
const files={
 'android/content/Context.java':`package android.content;public class Context{public static final String WINDOW_SERVICE="window";public Object getSystemService(String s){return new android.view.WindowManager();}}`,
 'android/os/Build.java':`package android.os;public class Build{public static class VERSION{public static int SDK_INT=36;}}`,
 'android/view/WindowManager.java':`package android.view;public class WindowManager{public Display getDefaultDisplay(){return new Display();}public static class LayoutParams{public int preferredDisplayModeId;public float preferredRefreshRate;}}`,
 'android/view/Display.java':`package android.view;public class Display{
 public Mode getMode(){return new Mode(1,1080,2400,60);}public Mode[] getSupportedModes(){return new Mode[]{getMode(),new Mode(2,1080,2400,120),new Mode(3,1440,3200,165)};}
 public static class Mode{int id,w,h;float hz;Mode(int i,int x,int y,float r){id=i;w=x;h=y;hz=r;}public int getModeId(){return id;}public int getPhysicalWidth(){return w;}public int getPhysicalHeight(){return h;}public float getRefreshRate(){return hz;}}
 }`,
 'com/lumivaraonline/fps/AppLanguage.java':`package com.lumivaraonline.fps;import android.content.Context;class AppLanguage{static int fps=0,scale=85;static AppLanguage preferences(Context c){return new AppLanguage();}int getInt(String k,int fallback){return k.equals("fps_limit")?fps:scale;}static String text(Context c,int id){return "max";}}`,
 'com/lumivaraonline/fps/R.java':`package com.lumivaraonline.fps;class R{static class string{static int fps_max=1;}}`,
 'com/lumivaraonline/fps/DisplayRequestHarness.java':`package com.lumivaraonline.fps;import android.content.Context;import android.os.Build;import android.view.*;
 public class DisplayRequestHarness{static void check(boolean c){if(!c)throw new AssertionError();}public static void main(String[] args){Context c=new Context();WindowManager.LayoutParams p=new WindowManager.LayoutParams();
 Build.VERSION.SDK_INT=36;PerformanceSettings.request(c,new Display(),p,true);check(p.preferredDisplayModeId==0&&p.preferredRefreshRate==120);
 AppLanguage.fps=60;PerformanceSettings.request(c,new Display(),p,true);check(p.preferredRefreshRate==60);
 Build.VERSION.SDK_INT=33;AppLanguage.fps=0;PerformanceSettings.request(c,new Display(),p,true);check(p.preferredDisplayModeId==2&&p.preferredRefreshRate==0);
 PerformanceSettings.request(c,new Display(),p,false);check(p.preferredDisplayModeId==0&&p.preferredRefreshRate==0);
 Build.VERSION.SDK_INT=26;PerformanceSettings.request(c,null,p,true);check(p.preferredDisplayModeId==2);
 PerformanceSettings.request(c,new Display(){public Mode getMode(){throw new IllegalStateException();}},p,true);check(p.preferredDisplayModeId==0&&p.preferredRefreshRate==0);
 AppLanguage.fps=999;AppLanguage.scale=-1;check(PerformanceSettings.limit(c)==0&&PerformanceSettings.scale(c)==85);
 System.out.println("PASS: production display requests on API 26/33/36, max/60, supported resolution, release in hidden state, null/error display fallback and invalid preferences. Fake Android; not measured device refresh.");}}
`};
for(const [name,text]of Object.entries(files)){const file=path.join(dir,name);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,text);}
execFileSync('javac',['--release','8','-d',dir,...Object.keys(files).map(name=>path.join(dir,name)),path.join(root,'src/com/lumivaraonline/fps/PerformanceSettings.java'),path.join(root,'src/com/lumivaraonline/fps/DisplayRefreshPolicy.java')],{stdio:'inherit'});
execFileSync('java',['-cp',dir,'com.lumivaraonline.fps.DisplayRequestHarness'],{stdio:'inherit'});
