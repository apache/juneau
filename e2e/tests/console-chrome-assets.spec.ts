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

import { test, expect, type Page } from '@playwright/test';

/**
 * Nested-path console chrome CSS. A relative or page-prefixed
 * `juneau-console/chrome.css` href on `/rest/setup` 404s (the browser asks for
 * `/rest/setup/juneau-console/...`) and the page paints as unstyled run-on tabs
 * ("SetupReleasesNew Release"). Root-absolute `/juneau-console/chrome.css` is
 * 200 and the chrome paints. This spec fails if that regression ships again.
 */

type CssHit = { pathname: string; status: number; contentType: string };

function collectConsoleCss(page: Page): CssHit[] {
  const hits: CssHit[] = [];
  page.on('response', (res) => {
    let pathname: string;
    try {
      pathname = new URL(res.url()).pathname;
    } catch {
      return;
    }
    if (
      pathname.includes('/juneau-console/chrome.css') ||
      pathname.includes('/juneau-console/themes/juneau-theme-')
    ) {
      hits.push({
        pathname,
        status: res.status(),
        contentType: res.headers()['content-type'] ?? '',
      });
    }
  });
  return hits;
}

test.describe('Console chrome assets on a nested page', () => {
  test('/rest/setup loads root-absolute juneau-console CSS and paints the nav', async ({ page }) => {
    const hits = collectConsoleCss(page);
    const response = await page.goto('/rest/setup');
    expect(response?.status()).toBe(200);

    const chrome = hits.filter((h) => h.pathname.endsWith('/juneau-console/chrome.css'));
    const theme = hits.filter((h) =>
      /\/juneau-console\/themes\/juneau-theme-[a-z0-9-]+\.css$/.test(h.pathname),
    );
    expect(chrome, `chrome.css requests on /rest/setup: ${JSON.stringify(hits)}`).toHaveLength(1);
    expect(theme, `theme CSS requests on /rest/setup: ${JSON.stringify(hits)}`).toHaveLength(1);
    expect(chrome[0].pathname, 'chrome.css must be site-root, not under /rest/setup').toBe(
      '/juneau-console/chrome.css',
    );
    expect(theme[0].pathname, 'theme CSS must be site-root, not under /rest/setup').toMatch(
      /^\/juneau-console\/themes\/juneau-theme-[a-z0-9-]+\.css$/,
    );
    expect(chrome[0].status).toBe(200);
    expect(theme[0].status).toBe(200);
    expect(chrome[0].contentType).toMatch(/text\/css/i);
    expect(theme[0].contentType).toMatch(/text\/css/i);

    const header = page.locator('header.jc-header');
    await expect(header).toBeVisible();
    expect(
      await header.evaluate((el) => getComputedStyle(el).display),
      'header.jc-header must pick up chrome.css flex layout, not the unstyled block default',
    ).toBe('flex');

    const nav = page.locator('nav.juneau-page-nav').first();
    await expect(nav.getByRole('link', { name: 'Setup', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Releases', exact: true })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'New Release', exact: true })).toBeVisible();

    const sections = nav.locator('.juneau-page-nav-sections').first();
    expect(
      await sections.evaluate((el) => getComputedStyle(el).display),
      'page-tab row must be flex from chrome.css, not inline run-on tabs',
    ).toBe('flex');
    expect(
      await sections.evaluate((el) => getComputedStyle(el).paddingLeft),
      'page-tab row must have chrome.css padding, not the unstyled 0px default',
    ).not.toBe('0px');
  });
});
