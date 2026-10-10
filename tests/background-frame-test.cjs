const fs=require('fs'),vm=require('vm'),assert=require('assert');
const script=fs.readFileSync(__dirname+'/../assets/background.js','utf8');
let clock=0,nextFrame=1,nextTimer=1,timerStarts=0,nativeCancels=0;
const frames=new Map(),timers=new Map();
const window={requestAnimationFrame(callback){const id=nextFrame++;frames.set(id,callback);return id;},cancelAnimationFrame(id){nativeCancels++;frames.delete(id);}};
const document={hidden:true,visibilityState:'hidden',hasFocus:()=>false};
const context={window,document,location:{origin:'https://lumivaraonline.com'},console,performance:{now:()=>clock},setTimeout(callback,delay){timerStarts++;const id=nextTimer++;timers.set(id,{callback,time:clock+delay});return id;},clearTimeout(id){timers.delete(id);}};
window.setTimeout=context.setTimeout;window.clearTimeout=context.clearTimeout;
window.setInterval=context.setTimeout;window.clearInterval=context.clearTimeout;
const nativeTick=()=>{clock+=16.67;for(const [id,callback] of [...frames]){frames.delete(id);callback(clock);}};
const timerTick=()=>{clock+=50;for(const [id,job] of [...timers])if(job.time<=clock){timers.delete(id);job.callback();}};
vm.runInNewContext(script,context);
let calls=0;
for(let i=0;i<120;i++){window.requestAnimationFrame(()=>calls++);nativeTick();}
assert.equal(calls,120);assert.equal(timerStarts,0,'foreground MUST allocate no fallback timer');assert.equal(nativeCancels,0,'foreground callbacks must not cancel completed native frames');
window.requestAnimationFrame(()=>calls++);
window.__lumivaraFrameControl.setBackground(true);
assert.equal(timers.size,1,'minimizing arms even a frame queued in foreground');
timerTick();assert.equal(calls,121);nativeTick();assert.equal(calls,121,'timer and native frame cannot double-fire');
window.requestAnimationFrame(()=>calls++);assert.equal(timers.size,1);
window.__lumivaraFrameControl.setBackground(false);assert.equal(timers.size,0,'returning foreground clears fallback');nativeTick();assert.equal(calls,122);
window.__lumivaraFrameControl.setBackground(true);const cancelled=window.requestAnimationFrame(()=>{throw new Error('cancelled callback fired');});
window.cancelAnimationFrame(cancelled);assert.equal(timers.size,0);assert.equal(frames.size,0);
const wrapper=window.requestAnimationFrame;vm.runInNewContext(script,context);assert.equal(window.requestAnimationFrame,wrapper,'idempotent injection');
// Suppress BOTH web timers and native paint frames, as a screen-off WebView may do.
let hiddenCalls=0,lastTimestamp=-1;
function loop(timestamp){hiddenCalls++;lastTimestamp=timestamp;window.requestAnimationFrame(loop);}
window.requestAnimationFrame(loop);
for(let i=0;i<100;i++){clock+=17;window.__lumivaraFrameControl.nativeTick();assert.equal(hiddenCalls,i+1);assert.equal(lastTimestamp,clock);assert.equal(frames.size,1);assert.equal(timers.size,1);}
// A long suspension gets one real-time callback, not a burst of fabricated frames.
clock+=600000;window.__lumivaraFrameControl.nativeTick();assert.equal(hiddenCalls,101);assert.equal(lastTimestamp,clock);
const staleFrames=[...frames.values()],staleTimers=[...timers.values()];
clock+=17;window.__lumivaraFrameControl.nativeTick();assert.equal(hiddenCalls,102);
for(const callback of staleFrames)callback(clock);for(const job of staleTimers)job.callback();assert.equal(hiddenCalls,102,'stale native/timer callbacks cannot deliver twice');
window.__lumivaraFrameControl.setBackground(false);window.__lumivaraFrameControl.nativeTick();assert.equal(hiddenCalls,102);assert.equal(timers.size,0);
nativeTick();assert.equal(hiddenCalls,103,'foreground paint resumes same loop');
window.__lumivaraFrameControl.setBackground(true);
const stop=window.requestAnimationFrame(()=>{throw new Error('native pulse fired a cancelled job');});window.cancelAnimationFrame(stop);clock+=17;window.__lumivaraFrameControl.nativeTick();assert.equal(hiddenCalls,104);
const otherWindow={requestAnimationFrame(){},cancelAnimationFrame(){}};vm.runInNewContext(script,{window:otherWindow,location:{origin:'https://example.test'}});assert(!otherWindow.__lumivaraFrameControl,'only the game origin may be patched');
console.log('PASS: zero foreground fallback timers; screen-off native pulses advance queued loops when paint/web timers stall; real timestamps after a 10-minute gap, no replay burst or duplicate callbacks; foreground return/cancel/origin guard. Deterministic scheduler tests, not device FPS or kill-count measurement.');
