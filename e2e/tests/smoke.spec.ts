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

/** Collects `console.error`-severity messages for the lifetime of a page navigation. */
function collectConsoleErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text());
  });
  page.on('pageerror', (err) => errors.push(err.message));
  return errors;
}

test.describe('Smoke: primary content renders without console errors', () => {
  test('New Release page renders its primary content', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    const response = await page.goto('/rest/runs');
    expect(response?.status()).toBe(200);

    // Input/Execution subtabs are always present regardless of whether a run is active.
    await expect(page.getByRole('tab', { name: 'Input' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Execution' })).toBeVisible();

    expect(errors, `console errors on /rest/runs: ${errors.join('; ')}`).toEqual([]);
  });

  test('Admin page renders its primary content', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    const response = await page.goto('/rest/admin');
    expect(response?.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'Admin' })).toBeVisible();
    // Composed tab bar (PageTable) — leaf tabs for the two dogfooded views. Scoped to .jc-tab-bar (the
    // PageTable-owned tablist) since the top nav also has same-named "Releases"/"Credentials" links.
    const adminTabBar = page.locator('.jc-tab-bar');
    await expect(adminTabBar.getByText('Releases', { exact: true })).toBeVisible();
    await expect(adminTabBar.getByText('Credentials', { exact: true })).toBeVisible();

    expect(errors, `console errors on /rest/admin: ${errors.join('; ')}`).toEqual([]);
  });

  test('Credentials page renders its primary content', async ({ page }) => {
    const errors = collectConsoleErrors(page);
    const response = await page.goto('/rest/credentials');
    expect(response?.status()).toBe(200);

    // One card per managed credential (Apache LDAP / GPG / GitHub) — assert on the label text rather than a
    // specific count, since the set of managed credentials could grow.
    await expect(page.getByText(/GitHub token/i)).toBeVisible();

    expect(errors, `console errors on /rest/credentials: ${errors.join('; ')}`).toEqual([]);
  });
});
