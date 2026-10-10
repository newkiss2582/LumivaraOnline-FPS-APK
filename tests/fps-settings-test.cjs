const fs=require('fs'),assert=require('assert'),{chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const script=fs.readFileSync(__dirname+'/../assets/performance.js','utf8');
const fixture='<!doctype html><canvas></canvas><div id="controls"></div>';
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.TEST_BROWSER?{executablePath:process.env.TEST_BROWSER}:{})});
 try{
  const context=await browser.newContext();await context.route('**/*',route=>route.fulfill({contentType:'text/html',body:fixture}));
  const page=await context.newPage();await page.goto('https://lumivaraonline.com/fps-fixture');
  await page.evaluate(()=>{localStorage.setItem('pixelrpg.language','th');window.__lumivaraFpsLimit=0;});await page.addScriptTag({content:script});
  assert.equal(await page.evaluate(()=>localStorage.getItem('pixelrpg.fps-limit')),'0');
  assert.equal(await page.evaluate(()=>localStorage.getItem('pixelrpg.battery-saver')),'off');
  await page.evaluate(()=>{window.changes=[];for(const [id,values]of [['fps-limit',['0','30','60','120']],['battery-saver-idle',['off','1','3','5']]]){let s=document.createElement('select');s.id=id;for(const value of values)s.add(new Option(value,value));s.value=id==='fps-limit'?'30':'3';s.onchange=()=>window.changes.push([id,s.value]);document.querySelector('#controls').append(s);}});
  assert.equal(await page.locator('#fps-limit').inputValue(),'0');
  assert.equal(await page.locator('#battery-saver-idle').inputValue(),'off');
  assert.deepEqual(await page.evaluate(()=>window.changes),[['fps-limit','0'],['battery-saver-idle','off']]);
  await page.addScriptTag({content:script});assert.equal(await page.evaluate(()=>window.changes.length),2,'reinject must not restart an unchanged game loop');
  for(const value of [120,60,0]){await page.evaluate(v=>window.__lumivaraFpsLimit=v,value);await page.addScriptTag({content:script});assert.equal(await page.locator('#fps-limit').inputValue(),String(value));}
  await page.evaluate(()=>{localStorage.setItem('pixelrpg.battery-saver','3');document.querySelector('#battery-saver-idle').value='3';});
  await page.addScriptTag({content:script});assert.equal(await page.locator('#battery-saver-idle').inputValue(),'3','later game saver selection respected');
  assert.equal(await page.evaluate(()=>localStorage.getItem('pixelrpg.language')),'th','game language untouched');
  assert.equal(await page.locator('canvas').evaluate(n=>getComputedStyle(n).touchAction),'none');
  await page.goto('https://example.com/');await page.addScriptTag({content:script});assert.equal(await page.evaluate(()=>window.__lumivaraFpsEdition),undefined,'only game origin');
  console.log('PASS: native max/120/60 reach real-style game setting handlers; dynamic controls, no duplicate loop restart, saver choice/language retained, game-origin isolation. Browser fixture; not measured game FPS.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
