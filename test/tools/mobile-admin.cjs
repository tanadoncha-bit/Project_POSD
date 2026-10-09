const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('fs');
const base=process.argv[2]||'http://127.0.0.1:8089';
if(!['localhost','127.0.0.1'].includes(new URL(base).hostname))throw Error('Use a disposable localhost app.');
(async()=>{
const browser=await chromium.launch();const results=[];fs.mkdirSync('tmp/mobile-fix-results',{recursive:true});
try{
 for(const width of [360,390,500,1280]){
  const page=await browser.newPage({viewport:{width,height:844},reducedMotion:'reduce'});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base);await page.locator('[data-open-login]').click();
  await page.locator('#preview-login-form [name=username]').fill('admin');await page.locator('#preview-login-form [name=password]').fill('Password123!');
  await Promise.all([page.waitForResponse(r=>r.url()===base+'/login'&&r.request().method()==='POST'),page.locator('#preview-login-form button[type=submit]').click()]);
  for(const view of ['users','requests','dashboard','inventory']){
   await page.goto(base+'/admin?ui='+view+'#'+view);
   if(view==='users'){await page.locator('.user-accounts-table tbody').waitFor({state:'visible'});await page.locator('.user-history-panel').evaluate(e=>e.open=true);}
   if(view==='requests')await page.locator('#admin-request-list .admin-request-card').first().waitFor({state:'visible'});
   if(view==='dashboard')await page.locator('.management-shelf').first().waitFor({state:'visible'});
   if(view==='inventory')await page.locator('[data-open-equipment-admin]').first().waitFor({state:'visible'});
   const info=await page.evaluate(()=>({width:innerWidth,scrollWidth:document.documentElement.scrollWidth,scrollbar:getComputedStyle(document.documentElement).scrollbarWidth}));
   if(info.scrollWidth>width+1)throw Error('Horizontal overflow '+view+' '+JSON.stringify(info));
   if(info.scrollbar!=='none')throw Error('Root scrollbar not hidden');
   await page.screenshot({path:`tmp/mobile-fix-results/${view}-${width}.png`});
   results.push({width,view,...info});
  }
  if(width<=600){
   await page.goto(base+'/admin#requests');
   await page.locator('#admin-request-list .admin-request-card').first().waitFor();
   const mobileFilter=page.locator('.mobile-request-filter .picker-trigger');
   await mobileFilter.click();
   await page.locator('.picker-panel:popover-open [role=option]').filter({hasText:'Pending'}).click();
   await page.waitForFunction(()=>!document.querySelector('#admin-request-list .admin-request-card:not([hidden])[data-request-status]:not([data-request-status="PENDING"])'));
   if(await page.locator('[data-mobile-request-filter]').inputValue()!=='PENDING')throw Error('Mobile request filter failed');
   await page.locator('[data-admin-attention]').click();
   await page.locator('[data-admin-panel=attention]').waitFor({state:'visible'});
   if(await page.locator('dialog[open]').count())throw Error('Attention still opens a modal');
   await page.locator('.attention-back').click();
   await page.locator('[data-admin-panel=requests]').waitFor({state:'visible'});
   await page.locator('.admin-navigation [data-admin-nav=inventory]').click();
   const card=page.locator('.inventory-tile').first();await card.waitFor();
   await card.locator('h2').click();
   await page.locator('#equipment-admin-modal').waitFor({state:'visible'});
   await page.locator('#equipment-admin-modal header [data-close-admin-modal]').click();
  }
  await page.goto(base+'/my-requests');
  if(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1))throw Error('Borrower history overflow');
  await page.screenshot({path:`tmp/mobile-fix-results/history-${width}.png`});
  await page.route('**/api/v1/borrow-requests/999999/return',route=>route.fulfill({json:{borrowRequestId:999999,returnDate:'2026-10-09',fineAmount:0,damageAmount:0,totalAmount:0,items:Array.from({length:8},(_,i)=>({equipmentName:'Test item '+i,condition:'NORMAL',damageAmount:0}))}}));
  await page.evaluate(()=>{const b=document.createElement('button');b.dataset.returnReceipt='999999';b.textContent='Test receipt';document.body.prepend(b);b.click();});
  const receipt=page.locator('.return-receipt');await receipt.waitFor({state:'visible'});await receipt.locator('.receipt-item').first().waitFor();
  if(!(await page.locator('body').getAttribute('class')).includes('modal-open'))throw Error('Receipt does not lock background');
  await page.screenshot({path:`tmp/mobile-fix-results/receipt-${width}.png`});
  const close=receipt.locator('footer button');await close.scrollIntoViewIfNeeded();await close.click();await receipt.waitFor({state:"detached"});
  if((await page.locator('body').getAttribute('class')).includes('modal-open'))throw Error('Receipt did not restore scrolling');
  const badId=await page.request.get(base+'/api/v1/equipment/not-a-number');
  if(badId.status()!==400)throw Error('Invalid API id is not 400');
  const csrf=await page.locator('meta[name=_csrf]').getAttribute('content');
  const methodError=await page.request.patch(base+'/api/v1/equipment',{headers:{'X-CSRF-TOKEN':csrf}});
  if(methodError.status()!==405||!methodError.headers()['allow'])throw Error('405/Allow not preserved');
  if(errors.length)throw Error(errors.join(';'));
  await page.close();
 }
 fs.writeFileSync('tmp/mobile-fix-results/results.json',JSON.stringify(results,null,2));console.log('Mobile checks passed: 4 widths, admin users/history/requests/dashboard/inventory, borrower history, hidden scrollbars, scrollable receipt and restored background.');
}finally{await browser.close()}
})().catch(e=>{console.error(e.message);process.exit(1)});
