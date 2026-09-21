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
 * Each top-nav tab points at a real /rest/* route (base.ftlh) — this table is the single source of truth for
 * which route each tab should land on and what landmark proves the right page rendered. Kept independent of the
 * control-row/ribbon markup (which is in flux) — these are page-level landmarks only.
 */
const NAV_TABS: { name: string; path: string; heading: string | RegExp | null }[] = [
  { name: 'Setup', path: '/rest/setup', heading: 'Probes' },
  { name: 'Releases', path: '/rest/releases', heading: 'All Releases' },
  // New Release has no single fixed heading — its Input-vs-Execution subtab starts on whichever one matches
  // current run state (state-dependent), so it's asserted separately below via the always-present subtab bar.
  { name: 'New Release', path: '/rest/runs', heading: null },
];

function topNav(page: Page) {
  return page.locator('nav.juneau-page-nav');
}

test.describe('Top navigation', () => {
  for (const tab of NAV_TABS) {
    test(`"${tab.name}" tab is present and navigates to a working page`, async ({ page }) => {
      await page.goto('/rest/setup');

      const link = topNav(page).getByRole('link', { name: tab.name, exact: true });
      await expect(link).toBeVisible();
      await expect(link).toHaveAttribute('href', tab.path);

      const response = await page.goto(tab.path);
      expect(response?.status()).toBe(200);

      if (tab.name === 'New Release') {
        await expect(page.getByRole('tab', { name: 'Input' })).toBeVisible();
        await expect(page.getByRole('tab', { name: 'Execution' })).toBeVisible();
      } else if (tab.heading instanceof RegExp) {
        await expect(page.getByText(tab.heading).first()).toBeVisible();
      } else if (tab.heading) {
        await expect(page.getByRole('heading', { name: tab.heading })).toBeVisible();
      }
    });
  }

  test('all three nav tabs are present on every page', async ({ page }) => {
    for (const startTab of NAV_TABS) {
      await page.goto(startTab.path);
      for (const tab of NAV_TABS) {
        await expect(topNav(page).getByRole('link', { name: tab.name, exact: true })).toBeVisible();
      }
      await expect(topNav(page).getByRole('link', { name: 'Home', exact: true })).toHaveCount(0);
      await expect(topNav(page).getByRole('link', { name: 'Credentials', exact: true })).toHaveCount(0);
      await expect(topNav(page).getByRole('link', { name: 'Admin', exact: true })).toHaveCount(0);
    }
  });
});
