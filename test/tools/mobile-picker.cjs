const fs = require('fs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
(async () => {
 const browser = await chromium.launch();
 try {
  for (const width of [360, 390, 500, 1024, 1280, 1366]) {
   const page = await browser.newPage({viewport: {width, height: 844}});
   const css = ['style', 'equipment-workflows'].map(name => fs.readFileSync(`code/src/main/resources/static/css/${name}.css`, 'utf8')).join('\n');
   await page.setContent(`<style>${css}</style><label>Request status<select data-mobile-request-filter><option value="ALL">All requests</option><option value="PENDING">Pending</option></select></label>`);
   await page.addScriptTag({content: fs.readFileSync('code/src/main/resources/static/js/form-pickers.js', 'utf8')});
   await page.locator('.picker-trigger').click();
   await page.locator('[role=option]').filter({hasText: 'Pending'}).click();
   if (await page.locator('select').inputValue() !== 'PENDING') throw Error('Selection failed');
   await page.locator('select').evaluate(select => { select.value = 'ALL'; document.dispatchEvent(new Event('picker:sync')); });
   if (!(await page.locator('.picker-trigger').textContent()).includes('All requests')) throw Error('Sync failed');
   await page.locator('.picker-trigger').click();
   await page.keyboard.press('Escape');
   if (await page.locator('.picker-panel').isVisible()) throw Error('Escape failed');
   console.log(`${width}px: dropdown selection, sync and Escape PASS`);
   await page.close();
  }
 } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exit(1); });
