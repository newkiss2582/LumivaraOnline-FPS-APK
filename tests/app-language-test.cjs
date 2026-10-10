const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const root=path.join(__dirname,'..'),script=fs.readFileSync(path.join(root,'assets/select-picker.js'),'utf8');
const catalog=JSON.parse(fs.readFileSync(path.join(root,'translations.json'),'utf8'));
assert.equal(new Set(catalog.map(entry=>entry.name)).size,catalog.length);
for(const entry of catalog){assert(entry.th.trim()&&entry.en.trim(),entry.name+' is missing a translation');}
for(const [locale,folder]of [['en','values'],['th','values-th']]){
 const resources=fs.readFileSync(path.join(root,'res',folder,'strings.xml'),'utf8');
 const keys=[...resources.matchAll(/<string name="([^"]+)"/g)].map(match=>match[1]);
 assert.deepEqual(keys,catalog.map(entry=>entry.name),'resource language coverage must match');
}
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.TEST_BROWSER?{executablePath:process.env.TEST_BROWSER}:{})});
 try{
  const context=await browser.newContext({viewport:{width:640,height:420},isMobile:true,hasTouch:true});
  const fixture='<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><style>select{width:240px;height:48px}</style><select id="choices" multiple><option value="world" selected>World</option><option value="trade">Trade</option></select>';
  await context.route('**/*',route=>route.fulfill({contentType:'text/html',body:fixture}));
  const page=await context.newPage();let requests=0;page.on('request',()=>requests++);
  await page.goto('https://lumivaraonline.com/app-language-fixture');
  await page.evaluate(()=>{localStorage.setItem('pixelrpg.language','th');window.changes=0;document.querySelector('#choices').onchange=()=>window.changes++;window.__lumivaraSelectPickerLanguage='th';});
  await page.addScriptTag({content:script});
  const before=await page.evaluate(()=>({gameLanguage:localStorage.getItem('pixelrpg.language'),navigatorLanguage:navigator.language,gameValue:document.querySelector('#choices').value}));
  const requestCount=requests;
  await page.locator('#choices').tap();
  const picker=page.locator('[data-lumivara-picker]');await picker.waitFor();
  assert.equal(await picker.locator('h2').innerText(),'เลือก');
  await picker.getByRole('button',{name:'ยกเลิก',exact:true}).waitFor();await picker.getByRole('button',{name:'ตกลง',exact:true}).waitFor();
  await page.evaluate(()=>window.__lumivaraPickerLocale.setLocale('en'));
  assert.equal(await picker.locator('h2').innerText(),'Choose');
  await picker.getByRole('button',{name:'Cancel',exact:true}).waitFor();await picker.getByRole('button',{name:'Apply',exact:true}).waitFor();
  assert.equal(await picker.getByRole('option',{name:'World',exact:true}).count(),1,'game option text stays untouched');
  assert.equal(await picker.getByRole('option',{name:'Trade',exact:true}).count(),1);
  await page.evaluate(()=>window.__lumivaraPickerLocale.setLocale('invalid'));
  assert.equal(await picker.locator('h2').innerText(),'Choose','invalid locale does not change UI');
  await picker.getByRole('button',{name:'Cancel',exact:true}).tap();
  assert.deepEqual(await page.evaluate(()=>({gameLanguage:localStorage.getItem('pixelrpg.language'),navigatorLanguage:navigator.language,gameValue:document.querySelector('#choices').value})),before,'app language leaves game/navigator settings intact');
  assert.equal(await page.evaluate(()=>window.changes),0,'switching app language cannot dispatch a game selection change');
  assert.equal(requests,requestCount,'switching app language cannot reload or navigate');
  await page.evaluate(()=>window.__lumivaraPickerLocale.setLocale('th'));await page.locator('#choices').tap();await picker.getByRole('button',{name:'ยกเลิก',exact:true}).waitFor();
  console.log('PASS: all translated messages have Thai/English resources; live helper labels switch while preserving game language, options, selection, navigator and navigation. Browser fixture, not a device test.');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
