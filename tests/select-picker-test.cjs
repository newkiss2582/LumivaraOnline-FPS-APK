const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const script=fs.readFileSync(path.join(__dirname,'../assets/select-picker.js'),'utf8');
const fixture=`<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{background:#142347;color:white;font:16px sans-serif}select{display:block;width:240px;height:44px;margin:16px}</style>
<label for="language">ภาษา / Language</label><select id="language"><option value="en">English</option><option value="th">ไทย</option><optgroup label="Unavailable" disabled><option value="disabled">Disabled</option></optgroup><option value="safe">&lt;img src=x onerror=alert(1)&gt;</option></select>
<select id="channels" multiple aria-label="Channels"><option value="world" selected>World</option><option value="trade">Trade</option></select>
<select id="empty" aria-label="Empty"></select><select id="disabled" disabled><option>Disabled</option></select>`;
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.TEST_BROWSER?{executablePath:process.env.TEST_BROWSER}:{})});
 try{
  const context=await browser.newContext({viewport:{width:960,height:432},isMobile:true,hasTouch:true});
  await context.route('**/*',route=>route.fulfill({contentType:'text/html',body:fixture}));
  const page=await context.newPage();
  const pageErrors=[];page.on('pageerror',error=>pageErrors.push(error.message));
  await page.goto('https://lumivaraonline.com/test-picker');
  await page.evaluate(()=>{window.events=[];window.gameClicks=0;document.addEventListener('click',()=>window.gameClicks++);document.querySelector('#language').onchange=e=>window.events.push('language:'+e.target.value);document.addEventListener('input',e=>window.events.push('input:'+e.target.id));document.querySelector('#channels').onchange=()=>window.events.push('channels');});
  await page.evaluate(()=>{window.rootTouchHandlers=[];const original=EventTarget.prototype.addEventListener;EventTarget.prototype.addEventListener=function(type,callback,options){if(['touchstart','touchmove','touchend'].includes(type)&&[window,document,document.body].includes(this)&&new Error().stack.includes('lumivara-select-picker.js'))window.rootTouchHandlers.push(type);return original.call(this,type,callback,options);};});
  await page.addScriptTag({content:script+'\n//# sourceURL=lumivara-select-picker.js'});await page.addScriptTag({content:script+'\n//# sourceURL=lumivara-select-picker.js'});
  const picker=page.locator('[data-lumivara-picker]');
  const touch=async locator=>{await locator.scrollIntoViewIfNeeded();const box=await locator.boundingBox();assert(box,'touch target has bounds');await page.touchscreen.tap(box.x+box.width/2,box.y+box.height/2);};
  await touch(page.locator('#language'));await picker.waitFor();
  assert.equal(await picker.locator('button.option').count(),4);
  assert.equal(await picker.locator('button.option').nth(2).isDisabled(),true);
  assert.equal(await picker.locator('img').count(),0,'option labels must be text, never HTML');
  await touch(picker.getByRole('option',{name:'ไทย',exact:true}));
  assert.equal(await page.locator('#language').inputValue(),'th');
  assert.deepEqual(await page.evaluate(()=>window.events),['input:language','language:th']);
  assert.equal(await page.evaluate(()=>window.gameClicks),0,'picker must not click the underlying game');
  await touch(page.locator('#language'));await touch(picker.getByRole('button',{name:'ยกเลิก / Cancel'}));
  assert.equal(await page.locator('#language').inputValue(),'th');assert.equal(await page.evaluate(()=>window.events.length),2);
  await page.locator('#language').focus();await page.keyboard.press('Enter');await picker.waitFor();await page.keyboard.press('Escape');assert.equal(await picker.count(),0);
  await touch(page.locator('#channels'));await touch(picker.getByRole('option',{name:'Trade',exact:true}));await touch(picker.getByRole('button',{name:'ยกเลิก / Cancel'}));
  assert.deepEqual(await page.locator('#channels').evaluate(select=>Array.from(select.selectedOptions,o=>o.value)),['world']);
  await touch(page.locator('#channels'));await touch(picker.getByRole('option',{name:'Trade',exact:true}));await touch(picker.getByRole('button',{name:'ตกลง / Apply'}));
  assert.deepEqual(await page.locator('#channels').evaluate(select=>Array.from(select.selectedOptions,o=>o.value)),['world','trade']);
  await touch(page.locator('#empty'));await picker.waitFor();await touch(picker.getByRole('button',{name:'ยกเลิก / Cancel'}));
  await touch(page.locator('#language'));await page.locator('#language').evaluate(select=>select.remove());await picker.waitFor({state:'detached'});
  await page.setViewportSize({width:360,height:780});await touch(page.locator('#channels'));await picker.waitFor();
  const bounds=await picker.boundingBox();assert(bounds.x>=0&&bounds.y>=0&&bounds.x+bounds.width<=360.1&&bounds.y+bounds.height<=780.1,'portrait picker fits viewport');
  await touch(picker.getByRole('button',{name:'ยกเลิก / Cancel'}));
  const foreign=await context.newPage();await foreign.goto('https://example.org/test-picker');await foreign.addScriptTag({content:script});
  assert.equal(await foreign.evaluate(()=>window.__lumivaraSelectPicker),undefined,'untrusted origin must not be modified');
  assert.deepEqual(await page.evaluate(()=>window.rootTouchHandlers),[],'app picker must not install page-wide touch blockers');
  await page.evaluate(()=>{const select=document.createElement('select');select.id='dynamic';select.innerHTML='<option value="a">Alpha</option><option value="b">Beta</option>';document.body.append(select);});
  await touch(page.locator('#dynamic'));await picker.waitFor();await touch(picker.getByRole('option',{name:'Beta',exact:true}));assert.equal(await page.locator('#dynamic').inputValue(),'b','later widgets receive element-scoped handlers');
  assert.deepEqual(pageErrors,[],'picker should not throw browser errors');
  console.log('PASS: actual browser touch selects language, emits input/change once, grouped disabled choices, safe option text, no clicks leaking to game, keyboard/cancel, multiple Apply/cancel, empty select, removal cleanup, portrait fit, origin guard. Desktop Chromium with mobile emulation; NOT Android WebView execution.');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});

