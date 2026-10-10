const fs=require('fs'),path=require('path'),assert=require('assert'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'..'),src=path.join(root,'src/com/lumivaraonline/fps'),build=path.join(root,'build/screen-off-test');
const service=fs.readFileSync(path.join(src,'FloatingGameService.java'),'utf8');
assert(service.includes('screenFilter.addAction(Intent.ACTION_SCREEN_OFF)')&&service.includes('screenFilter.addAction(Intent.ACTION_SCREEN_ON)'));
assert(service.includes('unregisterReceiver(screenReceiver)')&&service.includes('handler.removeCallbacks(framePump)'));
assert(service.includes('wakeLock.acquire(120000)')&&service.includes('wakeLock.release()'));
assert(!service.includes('addJavascriptInterface'));
const methods=service.slice(service.indexOf('    private void updateFrameMode(){'),service.indexOf('    private Button tool('));
assert(!/loadUrl|reload|onPause|pauseTimers|destroy|requestFocus|setTime|setCookie/.test(methods),'Screen-off scheduling must not navigate, pause, focus-steal or alter session data');
const harness=`import java.util.*;import com.lumivaraonline.fps.BackgroundFramePolicy;
public class ScreenOffHarness{
 static class Handler{ArrayDeque<Runnable> queue=new ArrayDeque<>();void post(Runnable r){queue.add(r);}void postDelayed(Runnable r,int delay){check(delay==17);queue.add(r);}void removeCallbacks(Runnable r){queue.removeIf(item->item==r);}}
 static class PowerManager{boolean interactive=true;boolean isInteractive(){return interactive;}}
 static class NavigationPolicy{enum Route{GAME,OTHER}static Route route(String u){return u.equals("game")?Route.GAME:Route.OTHER;}}
 interface Callback{void done(String result);}
 static class WebView{String url="game",mode;ArrayDeque<Callback> callbacks=new ArrayDeque<>();int pulses;String getUrl(){return url;}void evaluateJavascript(String s,Callback cb){if(cb==null){mode=s;}else{pulses++;callbacks.add(cb);}}}
 Handler handler=new Handler();PowerManager powerManager=new PowerManager();WebView game=new WebView();boolean expanded=true,running=true,frameFallback;Object fullscreenActivity;int frameGeneration;
 void updateRefreshRate(){}
 final Runnable framePump=()->pumpBackgroundFrame();
 ${methods}
 static void check(boolean c){if(!c)throw new AssertionError();}
 public static void main(String[] args){
  for(boolean interactive:new boolean[]{true,false})for(boolean expanded:new boolean[]{true,false})for(boolean full:new boolean[]{true,false})check(BackgroundFramePolicy.needsFallback(interactive,expanded,full)==(!interactive||(!expanded&&!full)));
  ScreenOffHarness a=new ScreenOffHarness();a.updateFrameMode();check(!a.frameFallback&&a.handler.queue.isEmpty());
  a.powerManager.interactive=false;a.updateFrameMode();check(a.frameFallback&&a.handler.queue.size()==1&&a.game.mode.contains("true"));
  a.handler.queue.remove().run();check(a.game.pulses==1&&a.handler.queue.isEmpty()&&a.game.callbacks.size()==1);
  a.game.callbacks.remove().done("null");check(a.handler.queue.size()==1);a.handler.queue.remove().run();check(a.game.callbacks.size()==1&&a.handler.queue.isEmpty());
  a.powerManager.interactive=true;a.updateFrameMode();check(!a.frameFallback);a.game.callbacks.remove().done("null");check(a.handler.queue.isEmpty());
  a.expanded=false;a.updateFrameMode();a.handler.queue.remove().run();check(a.frameFallback);a.running=false;a.game.callbacks.remove().done("null");check(a.handler.queue.isEmpty());
  a.running=true;a.fullscreenActivity=new Object();a.powerManager.interactive=false;a.updateFrameMode();check(a.frameFallback&&a.handler.queue.size()==1);
  a.powerManager.interactive=true;a.updateFrameMode();check(!a.frameFallback&&a.handler.queue.isEmpty());
  a.fullscreenActivity=null;a.game.url="external";a.updateFrameMode();check(a.handler.queue.isEmpty());
  System.out.println("PASS: production frame-mode/pump methods treat screen-off as background in mini/fullscreen; no async queue buildup, stale callbacks after foreground/stop ignored, external navigation not pulsed. Fake Android harness, not a device test.");
 }
}`;
fs.mkdirSync(build,{recursive:true});const file=path.join(build,'ScreenOffHarness.java');fs.writeFileSync(file,harness);
execFileSync('javac',['--release','8','-d',build,file,path.join(src,'BackgroundFramePolicy.java')],{stdio:'pipe'});
process.stdout.write(execFileSync('java',['-cp',build,'ScreenOffHarness'],{encoding:'utf8'}));
