const fs=require('fs'),path=require('path'),assert=require('assert'),{execFileSync}=require('child_process');
const root=path.join(__dirname,'..'),code=fs.readFileSync(path.join(root,'src/com/lumivaraonline/fps/FloatingGameService.java'),'utf8');
const slice=(start,end)=>code.slice(code.indexOf(start),code.indexOf(end,code.indexOf(start)));
for(const segment of [slice('private void collapse(){','private void drag('),slice('private void toggleMini(){','private void layoutGame('),slice('private void openFullscreen(){','boolean attachFullscreen(')]){
 assert(!/closeBrowser\(|releaseBrowser\(|browser\.(destroy|loadUrl|reload|stopLoading|pauseTimers)\(/.test(segment),'View-mode changes cannot close or navigate the link page');
}
assert(code.includes('if(window==browser)closeBrowser();'),'Stale popup close must not close the active page');
assert(code.includes('boolean current=v==browser;releaseBrowser(v);'),'Renderer cleanup must target the callback view synchronously');
const methods=slice('private void closeBrowser(){','private void inject(){');
const harness=`import java.util.*;
public class BrowserRetentionHarness extends Context {
 static class View {static final int VISIBLE=0,GONE=8;ViewGroup parent;int visibility;Object getParent(){return parent;}void setVisibility(int value){visibility=value;}}
 static class ViewGroup extends View {void removeView(View child){if(child.parent==this)child.parent=null;}}
 static class FrameLayout extends ViewGroup {static class LayoutParams{int topMargin,bottomMargin;LayoutParams(int width,int height){}} void addView(LinearLayout child,LayoutParams lp){if(child.parent!=null)throw new AssertionError("two parents");child.parent=this;child.layout=lp;}}
 static class LinearLayout extends View {FrameLayout.LayoutParams layout;void setLayoutParams(FrameLayout.LayoutParams lp){layout=lp;}}
 static class WebView extends View {int destroys,stops,resumes,focus;String url="https://example.test/second";List<String> history=new ArrayList<>(Arrays.asList("first","second"));void stopLoading(){stops++;}void destroy(){destroys++;}void onResume(){if(destroys>0)throw new AssertionError("dead page");resumes++;}void resumeTimers(){}void requestFocus(){focus++;}}
 static class MutableContextWrapper{Context base;void setBaseContext(Context value){base=value;}}
 private WebView browser,game;private LinearLayout browserPanel;private MutableContextWrapper browserContext;
 private Object browserAddress,browserReturnButton;private FrameLayout root,fullscreenHost;private Context fullscreenActivity;private boolean expanded;
 private final Set<WebView> releasedBrowsers=Collections.newSetFromMap(new WeakHashMap<WebView,Boolean>());
 private int toolbarHeight(){return 44;}private int dp(int n){return n;}
 private static void detach(View view){if(view!=null&&view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);}
 ${methods}
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  BrowserRetentionHarness app=new BrowserRetentionHarness();app.root=new FrameLayout();app.game=new WebView();app.browser=new WebView();WebView original=app.browser;
  app.browserPanel=new LinearLayout();app.browserContext=new MutableContextWrapper();app.expanded=true;app.attachBrowser();
  check(app.browserPanel.parent==app.root&&app.browserPanel.visibility==View.VISIBLE,"initial browser placement");
  check(app.browserPanel.layout.topMargin==44&&app.browserPanel.layout.bottomMargin==32,"overlay controls have separate space");
  for(int n=0;n<50;n++){
   app.expanded=false;app.showBrowser();check(app.browserPanel.visibility==View.GONE,"bubble hides panel");
   app.expanded=true;app.showBrowser();
   check(app.browser==original&&original.destroys==0&&original.stops==0,"bubble keeps browser");
   app.expanded=false;app.fullscreenHost=new FrameLayout();app.fullscreenActivity=new Context();app.attachBrowser();
   check(app.browserPanel.parent==app.fullscreenHost&&app.browserPanel.visibility==View.VISIBLE,"fullscreen keeps same page");
   check(app.browserPanel.layout.topMargin==0&&app.browserPanel.layout.bottomMargin==0,"fullscreen viewport has its own controls");
   check(app.browserContext.base==app.fullscreenActivity,"browser uses current fullscreen host");
   app.fullscreenHost=null;app.fullscreenActivity=null;app.attachBrowser();
   check(app.browserPanel.parent==app.root&&app.browserContext.base==app,"Home transfers ownership to service");
   app.expanded=true;app.showBrowser();
   check(original.url.equals("https://example.test/second")&&original.history.equals(Arrays.asList("first","second")),"URL and history preserved");
  }
  WebView stale=new WebView();app.releaseBrowser(stale);app.releaseBrowser(stale);
  check(stale.destroys==1&&app.browser==original&&original.destroys==0,"stale renderer callbacks cannot close current page");
  MutableContextWrapper closingContext=app.browserContext;app.closeBrowser();app.releaseBrowser(original);
  check(app.browser==null&&app.browserPanel==null&&original.destroys==1&&original.stops==1,"explicit close releases once");
  check(closingContext.base==app,"closed browser releases Activity context");
  app.closeBrowser();
  System.out.println("PASS: 50 bubble/fullscreen/Home cycles preserve link page identity, URL and history; host margins/context are correct; stale callbacks and explicit close release each view once. Production-method harness with fake Android hosts, not a device test.");
 }
}
class Context {}`;
const build=path.join(root,'build','browser-retention-test');fs.mkdirSync(build,{recursive:true});
const source=path.join(build,'BrowserRetentionHarness.java');fs.writeFileSync(source,harness);
process.stdout.write(execFileSync('javac',['--release','8','-encoding','UTF-8','-d',build,source],{encoding:'utf8'}));
process.stdout.write(execFileSync('java',['-cp',build,'BrowserRetentionHarness'],{encoding:'utf8'}));
