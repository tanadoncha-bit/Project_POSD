const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const base = process.argv[2] || 'http://127.0.0.1:8089';
if (!['127.0.0.1', 'localhost'].includes(new URL(base).hostname)) throw new Error('Use a disposable localhost app.');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const results = [], errors = [];
  fs.mkdirSync('tmp/ui-results', { recursive: true });
  try {
    for (const viewport of [{ width: 360, height: 640 }, { width: 390, height: 844 }, { width: 768, height: 1024 }, { width: 1280, height: 900 }]) {
      const context = await browser.newContext({ viewport });
      const page = await context.newPage();
      page.on('pageerror', error => errors.push(error.message));
      await page.goto(base, { waitUntil: 'domcontentloaded' });
      await page.locator('[data-open-login]').click();
      await page.locator('#login-modal').waitFor({ state: 'visible' });
      await page.locator('[data-open-register]').click();
      const modal = page.locator('#register-modal .auth-modal');
      await modal.waitFor({ state: 'visible' });
      if (!(await page.evaluate(() => document.body.classList.contains('modal-open')))) throw new Error('Modal does not lock background');
      let box = await modal.boundingBox();
      if (box.x < -1 || box.x + box.width > viewport.width + 1 || box.y < -1 || box.height > viewport.height + 1) throw new Error('Registration modal exceeds viewport ' + JSON.stringify({ viewport, box }));
      await page.screenshot({ path: `tmp/ui-results/signup-${viewport.width}.png` });
      await page.setViewportSize({ width: viewport.width, height: Math.min(430, viewport.height) });
      const password = page.locator('#register-modal [name=password]');
      await password.focus();
      const submit = page.locator('#register-modal button[type=submit]');
      await submit.scrollIntoViewIfNeeded();
      box = await submit.boundingBox();
      if (box.y < -1 || box.y + box.height > Math.min(430, viewport.height) + 1) throw new Error('Submit unreachable with short viewport');
      await context.close();
      results.push({ viewport, registrationModalFits: true, submitReachableAfterHeightReduction: true });
    }
    const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(base);
    await page.locator('[data-open-login]').click();
    await page.locator('#preview-login-form [name=username]').fill('admin');
    await page.locator('#preview-login-form [name=password]').fill('Password123!');
    await Promise.all([page.waitForResponse(response => response.url() === base + '/login' && response.request().method() === 'POST'), page.locator('#preview-login-form button[type=submit]').click()]);
    await page.waitForLoadState('domcontentloaded');
    const response = await page.goto(base + '/profile/change-email');
    if (response.status() !== 200) throw new Error('Change-email page failed');
    await page.locator('#new-email').waitFor({ state: 'visible' });
    if (await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1)) throw new Error('Email form has horizontal overflow');
    await page.screenshot({ path: 'tmp/ui-results/change-email-mobile.png', fullPage: true });
    await page.goto(base + '/profile');
    await page.locator('#profile-edit-button').click();
    if (!(await page.locator('#profile-email').getAttribute('readonly') !== null)) throw new Error('Profile email became editable');
    const csrf = await page.locator('meta[name=_csrf]').getAttribute('content');
    const inventory = await (await context.request.get(base + '/api/v1/equipment?size=1')).json();
    const upload = await context.request.post(base + '/api/v1/equipment/' + inventory.content[0].id + '/image', {
      headers: { 'X-CSRF-TOKEN': csrf },
      multipart: { image: { name: 'too-large.png', mimeType: 'image/png', buffer: Buffer.alloc(5 * 1024 * 1024) } }
    });
    if (upload.status() !== 413) throw new Error('Oversized transport upload returned ' + upload.status());
    await context.close();
    if (errors.length) throw new Error('Browser errors: ' + errors.join('; '));
    fs.writeFileSync('tmp/ui-results/result.json', JSON.stringify({ results, emailChangeMobileFits: true, profileEmailRemainsReadonly: true, oversizedApiUploadStatus: 413, browserErrors: errors }, null, 2));
    console.log('UI smoke passed: 4 viewport sizes, reduced-height modal, email page, readonly profile email.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exit(1); });
