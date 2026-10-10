(() => {
  if (location.origin !== 'https://lumivaraonline.com') return;
  if (window.__lumivaraBackgroundShim) return;

  const realRequest = window.requestAnimationFrame.bind(window);
  const realCancel = window.cancelAnimationFrame.bind(window);
  const realTimeout = window.setTimeout.bind(window);
  const realInterval = window.setInterval.bind(window);
  const realClearTimeout = window.clearTimeout.bind(window);
  const realClearInterval = window.clearInterval.bind(window);
  const pending = new Map();
  const webTimers = new Map();
  const stats = { frames: 0, timers: 0, pulseGapMax: 0, lastPulse: 0, lastFrame: 0,
    socketObserved: false, socketOpen: 0, socketCloses: 0, lastCloseCode: 0, packets: 0, lastPacket: 0 };
  let nextId = 1;
  let background = false;
  const arm = (id, job) => {
    if (background && job.timer === null) job.timer = realTimeout(() => complete(id, performance.now(), true), 17);
  };
  const complete = (id, timestamp, timerWon) => {
    const job = pending.get(id);
    if (!job) return;
    pending.delete(id);
    if (timerWon) realCancel(job.frame);
    if (job.timer !== null) realClearTimeout(job.timer);
    stats.frames++;stats.lastFrame=timestamp;
    job.callback(timestamp);
  };

  // Foreground stays synchronized to native paint frames with NO fallback timers.
  // Bubble and screen-off both need fallback when native paint frames stop.
  window.requestAnimationFrame = callback => {
    const id = nextId++;
    const job = { frame: 0, timer: null, callback };
    job.frame = realRequest(timestamp => complete(id, timestamp, false));
    pending.set(id, job);arm(id, job);
    return id;
  };
  window.cancelAnimationFrame = id => {
    const job = pending.get(id);
    if (!job) return;
    pending.delete(id);
    realCancel(job.frame);
    if (job.timer !== null) realClearTimeout(job.timer);
  };
  // Preserve normal scheduling. Only assist due function timers in background.
  // Connection pings/retries use timers too; rAF assistance alone is insufficient.
  const invokeTimer = (id, fromPulse) => {
    const job=webTimers.get(id);if(!job)return;
    const now=performance.now();
    if ((fromPulse || background) && now+1<job.due) return;
    if(job.repeat)job.due=now+job.delay;
    else {webTimers.delete(id);if(fromPulse)realClearTimeout(id);}
    stats.timers++;job.callback.apply(window,job.args);
  };
  const schedule = (callback, delay, repeat, args) => {
    const native=repeat?realInterval:realTimeout;
    const ms=Number(delay);
    // Short UI tasks/string timers/long jobs retain native behavior.
    if(typeof callback!=='function'||!Number.isFinite(ms)||ms<100||ms>30000)return native(callback,delay,...args);
    const job={callback,args,repeat,delay:ms,due:performance.now()+ms};
    const id=native(()=>invokeTimer(id,false),ms);webTimers.set(id,job);return id;
  };
  window.setTimeout=(callback,delay,...args)=>schedule(callback,delay,false,args);
  window.setInterval=(callback,delay,...args)=>schedule(callback,delay,true,args);
  // Browser timeout/interval IDs share one pool; either clear function cancels both.
  window.clearTimeout=id=>{webTimers.delete(Number(id));realClearTimeout(id);};
  window.clearInterval=id=>{webTimers.delete(Number(id));realClearInterval(id);};
  // Observe future game sockets without retaining addresses, payloads or sending packets.
  if(typeof window.WebSocket==='function'){
    const NativeSocket=window.WebSocket;
    window.WebSocket=new Proxy(NativeSocket,{construct(target,args,newTarget){
      const socket=Reflect.construct(target,args,newTarget);
      try{
        const url=new URL(String(args[0]),location.href);
        if(url.protocol==='wss:'&&url.hostname==='lumivaraonline.com'&&url.pathname==='/api/ws'){
          stats.socketObserved=true;let open=false;
          socket.addEventListener('open',()=>{open=true;stats.socketOpen++;});
          socket.addEventListener('message',()=>{stats.packets++;stats.lastPacket=performance.now();});
          socket.addEventListener('close',event=>{if(open)stats.socketOpen=Math.max(0,stats.socketOpen-1);open=false;stats.socketCloses++;stats.lastCloseCode=Number(event.code)||0;});
        }
      }catch{/* Observability must never interfere with connection creation. */}
      return socket;
    }});
  }
  Object.defineProperty(window, '__lumivaraFrameControl', { value: {
    nativeTick() {
      if (!background) return;
      // Android's bounded pump can still enter JS when web timers are throttled.
      // Use real elapsed time and a snapshot: never replay missed frames or speed time up.
      const timestamp = performance.now();
      if(stats.lastPulse)stats.pulseGapMax=Math.max(stats.pulseGapMax,timestamp-stats.lastPulse);
      stats.lastPulse=timestamp;
      for (const id of Array.from(pending.keys())) {
        try { complete(id, timestamp, true); }
        catch (error) { console.error(error); }
      }
      for(const [id,job] of Array.from(webTimers.entries()))if(timestamp+1>=job.due){
        try{invokeTimer(id,true);}catch(error){console.error(error);}
      }
    },
    snapshot() {
      const now=performance.now();
      // No account identity, callback URL, cookie, socket payload or chat text.
      const saver=document.getElementById?.('battery-saver');
      let gameCloseCount=0,gameCloseCode=0;
      try {
        const entries=window.__onlineDiagnostics?.().diag;
        if(Array.isArray(entries))for(const entry of entries){
          const count=String(entry).match(/หลุด #(\d+)/),code=String(entry).match(/code (\d+)/);
          if(count)gameCloseCount=Math.max(gameCloseCount,Number(count[1]));
          if(code)gameCloseCode=Number(code[1]);
        }
      }catch{/* Optional game hook: do not inspect its account field. */}
      return {...stats,ready:true,background,pendingFrames:pending.size,pendingTimers:webTimers.size,
        frameAgeMs:stats.lastFrame?Math.round(now-stats.lastFrame):-1,
        packetAgeMs:stats.lastPacket?Math.round(now-stats.lastPacket):-1,
        gameSaver:!!saver&&!saver.hidden,
        disconnectedUI:!!document.getElementById?.('online-status')?.classList.contains('disconnected'),
        gameCloseCount,gameCloseCode,
        saverIdle:document.getElementById?.('battery-saver-idle')?.value??'unknown'};
    },
    setBackground(value) {
      background = !!value;
      // This includes frames queued BEFORE collapse, so minimizing cannot strand a loop.
      for (const [id, job] of pending) {
        if (background) arm(id, job);
        else if (job.timer !== null) { realClearTimeout(job.timer); job.timer = null; }
      }
    }
  } });

  // The game and Phaser read these properties when the window is minimized.
  for (const [name, value] of [['hidden', false], ['visibilityState', 'visible']]) {
    try {
      Object.defineProperty(document, name, { configurable: true, get: () => value });
    } catch { /* A future browser may make this property unchangeable. */ }
  }
  try {
    Object.defineProperty(document, 'hasFocus', { configurable: true, value: () => true });
  } catch { /* Keep the native method if it cannot be replaced. */ }

  Object.defineProperty(window, '__lumivaraBackgroundShim', { value: true });
})();
