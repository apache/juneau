/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * csrf-boundary.cjs - real-browser prober for a CSRF boundary filter in front of real pages.
 *
 * Never runs in a default build.  It is driven by CsrfBoundary_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node csrf-boundary.cjs <app> <appByName> <attacker> <loopbackByName> <site> <crossSiteAttacker>
 *
 *     <app>               the application, as http://127.0.0.1:<port>
 *     <appByName>         the same application, as http://localhost:<port>
 *     <attacker>          a second server on another 127.0.0.1 port, serving the attack pages
 *     <loopbackByName>    a server behind a loopback boundary, as http://localhost:<port>
 *     <site>              the application on a public host name, as http://<name>:<port>
 *     <crossSiteAttacker> the attacker on a host name of another site, as http://<name>:<port>
 *
 * The cross-site rows use a second browser, so the first one is launched exactly as before.  It resolves the host
 * names of <site> and <crossSiteAttacker> to 127.0.0.1 (--host-resolver-rules), so the cross-site requests stay on
 * this machine; no other name is remapped.  A browser sends fetch metadata (Sec-Fetch-Site) only to a trustworthy
 * origin, which plain http on a host name is not, so those two origins, and no others, are treated as secure
 * (--unsafely-treat-insecure-origin-as-secure), as they would be over https.  That switch is honoured by full
 * Chromium but not by the headless shell a bare launch() starts, hence channel 'chromium'.  If only the headless shell
 * is installed, that launch fails and so does the whole class.
 *
 * Drives headless Chromium through the application's own pages and the attacker's pages.  Nothing is stubbed: every
 * request is a real one the browser makes, with the headers the browser chooses.  Prints ONE JSON object to stdout.
 *
 * DIVISION OF LABOUR: this script only OBSERVES; every assertion lives in the Java test.  It never reports a token
 * value, only facts about tokens (present, different).
 */
'use strict';

const { chromium } = require('playwright');

const COUNT_PATH = /\/count\/([a-z0-9]+)$/;

/** Waits for the browser's POST to one counter, while running the action that should cause it. */
async function postTo(page, name, action) {
	const [response] = await Promise.all([
		page.waitForResponse(r => r.request().method() === 'POST' && COUNT_PATH.exec(new URL(r.url()).pathname)?.[1] === name),
		action()
	]);
	return response;
}

/*
 * Runs inside the application's page: fires one row action at a counter through the real juneau-views.js menu and
 * submit path, and returns once its outcome banner has rendered.
 */
const ROW_ACTION = function (name) {
	const table = document.getElementById('rows');
	table.dataset.juneauView = 'v';
	const tr = table.querySelector('tr');
	const td = tr.querySelector('.juneau-view-actions-cell');
	const old = td.querySelector('.juneau-view-action-outcome');
	if (old) old.remove();
	const action = { id: 'count', label: 'Count', endpoint: '/count/' + name, method: 'POST' };
	const menu = window.JuneauViews.init.buildRowActionMenu({ rowActions: [action] }, table, tr, {});
	td.appendChild(menu);
	menu.querySelector('.juneau-view-action-item').click();
	return new Promise(function (resolve) {
		const started = Date.now();
		(function poll() {
			const banner = td.querySelector('.juneau-view-action-outcome');
			if (banner || Date.now() - started > 15000) {
				const r = banner ? banner.getBoundingClientRect() : null;
				resolve({
					state: banner ? banner.dataset.state : null,
					text: banner ? banner.textContent : null,
					visible: !!r && r.width > 0 && r.height > 0,
					refusalBanner: !!td.querySelector('.juneau-view-action-refusal')
				});
				return;
			}
			setTimeout(poll, 20);
		})();
	});
};

function sessionCookie(cookies) {
	return cookies.find(c => c.name === 'JSESSIONID')?.value ?? null;
}

(async () => {
	const [app, appByName, attacker, loopbackByName, site, crossSiteAttacker] = process.argv.slice(2);
	if (!crossSiteAttacker) {
		process.stderr.write('usage: node csrf-boundary.cjs <app> <appByName> <attacker> <loopbackByName> <site> <crossSiteAttacker>\n');
		process.exit(2);
	}
	const names = [...new Set([site, crossSiteAttacker].map(u => new URL(u).hostname))];
	const resolverRules = names.map(n => 'MAP ' + n + ' 127.0.0.1').join(', ');
	const secureOrigins = [site, crossSiteAttacker].map(u => new URL(u).origin).join(',');

	const report = {};
	const failures = [];
	const watch = page => {
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => {
			// A refused request is logged by the browser itself; that is the expected outcome, not a page error.
			if (m.type() === 'error' && !/Failed to load resource|CORS policy|net::ERR_FAILED/.test(m.text()))
				failures.push(m.text());
		});
		return page;
	};

	const browser = await chromium.launch();
	try {
		// The application reached by host name rather than by address, which a loopback boundary refuses.
		{
			const context = await browser.newContext();
			const page = watch(await context.newPage());
			const row = {};
			const loaded = await page.goto(appByName + '/page');
			row.pageStatus = loaded.status();
			row.action = await page.evaluate(ROW_ACTION, 'a01');
			const loopback = await page.goto(loopbackByName + '/');
			row.loopbackStatus = loopback.status();
			row.loopbackReason = loopback.headers()['x-request-boundary'] ?? null;
			report.a01 = row;
			await context.close();
		}

		const context = await browser.newContext();
		const page = watch(await context.newPage());

		// The token page, as a navigation.
		{
			const loaded = await page.goto(app + '/page');
			const headers = loaded.headers();
			report.a10 = {
				status: loaded.status(),
				cacheControl: headers['cache-control'] ?? null,
				expires: headers['expires'] ?? null,
				sessionCookie: sessionCookie(await context.cookies(app)) != null
			};
		}

		// The two embedded copies of the token: different strings, both accepted.
		report.a09 = await page.evaluate(async function () {
			const header = document.body.dataset.juneauCsrfHeader;
			const meta = document.querySelector('meta[name="csrf-token"]')?.content ?? '';
			const body = document.body.dataset.juneauCsrf ?? '';
			const post = token => fetch('/count/a09', {
				method: 'POST',
				credentials: 'same-origin',
				headers: { 'Content-Type': 'application/json', Accept: 'application/json', [header]: token },
				body: '{}'
			}).then(r => r.status);
			return {
				present: meta.length > 0 && body.length > 0,
				differ: meta !== body,
				metaStatus: await post(meta),
				bodyStatus: await post(body)
			};
		});

		// A row action, then the session id changes under the open page, then the same row action again.
		{
			const row = {};
			row.before = await page.evaluate(ROW_ACTION, 'a07');
			const cookieBefore = sessionCookie(await context.cookies(app));
			row.rotateStatus = await page.evaluate(() => fetch('/rotate', { credentials: 'same-origin' }).then(r => r.status));
			const cookieAfter = sessionCookie(await context.cookies(app));
			row.sessionChanged = cookieBefore != null && cookieAfter != null && cookieBefore !== cookieAfter;
			row.after = await page.evaluate(ROW_ACTION, 'a07');
			report.a07 = row;
		}

		// A plain HTML form on the application's own page carries no token.
		{
			await page.goto(app + '/page');
			const response = await postTo(page, 'a08', () => page.evaluate(() => document.getElementById('plain-form').submit()));
			await page.waitForURL(u => u.pathname === '/count/a08');
			report.a08 = { status: response.status() };
		}

		// A console card's write through the shell's own fetch helper.
		{
			await page.goto(app + '/console');
			const mounted = await page.waitForFunction(() => !!window.cardContext, null, { timeout: 15000 }).then(() => true, () => false);
			const row = { mounted };
			if (mounted) {
				Object.assign(row, await page.evaluate(() => window.cardContext.fetchJson('/count/a02', {
					method: 'POST',
					headers: { 'Content-Type': 'application/json' },
					body: '{}'
				}).then(v => ({ ok: true, count: v?.count ?? null }), e => ({ ok: false, error: String(e) }))));
			}
			report.a02 = row;
		}

		// The attacker's pages, opened in the browser that holds the application's session cookie.
		{
			const response = await postTo(page, 'a03', () => page.goto(attacker + '/attack/form'));
			await page.waitForURL(u => u.pathname === '/count/a03');
			report.a03 = { status: response.status() };
		}
		{
			await page.goto(attacker + '/attack/no-cors');
			await page.waitForFunction(() => window.attackDone != null, null, { timeout: 15000 });
			report.a04 = { result: await page.evaluate(() => window.attackDone) };
		}
		{
			const response = await postTo(page, 'a05', () => page.goto(attacker + '/attack/sandboxed'));
			report.a05 = { status: response.status() };
		}
		{
			await page.goto(attacker + '/attack/json');
			await page.waitForFunction(() => window.attackDone != null, null, { timeout: 15000 });
			report.a06 = { result: await page.evaluate(() => window.attackDone) };
		}

		// The application on its own site, then the attacker on another site, in one browser context.
		const sites = await chromium.launch({ channel: 'chromium', args: [
			'--host-resolver-rules=' + resolverRules,
			'--unsafely-treat-insecure-origin-as-secure=' + secureOrigins
		] });
		try {
			const context = await sites.newContext();
			const page = watch(await context.newPage());
			const row = {};
			const loaded = await page.goto(site + '/page');
			row.pageStatus = loaded.status();
			row.action = await page.evaluate(ROW_ACTION, 'a11');
			report.a11 = row;

			const response = await postTo(page, 'a12', () => page.goto(crossSiteAttacker + '/attack/cross-site-form'));
			await page.waitForURL(u => u.pathname === '/count/a12');
			report.a12 = { status: response.status() };
			await context.close();
		} finally {
			await sites.close();
		}

		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
