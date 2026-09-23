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

function topNav(page: Page) {
  return page.locator('nav.juneau-page-nav');
}

function pageTab(page: Page, name: string) {
  return topNav(page).locator('a.juneau-page-nav-section', { hasText: name });
}

function pageSubtab(page: Page, name: string) {
  return topNav(page).locator('a.juneau-page-nav-child', { hasText: name });
}

test.describe('New Release Page Subtabs', () => {
  test('page-tab row is only Setup, Releases, New Release', async ({ page }) => {
    await page.goto('/rest/runs');
    const sections = topNav(page).locator('.juneau-page-nav-section');
    await expect(sections).toHaveCount(3);
    await expect(sections.nth(0)).toHaveText('Setup');
    await expect(sections.nth(1)).toHaveText('Releases');
    await expect(sections.nth(2)).toHaveText('New Release');
  });

  test('Input and Execution are page-nav children on a second row under New Release', async ({ page }) => {
    await page.goto('/rest/runs');
    await expect(pageSubtab(page, 'Input')).toBeVisible();
    await expect(pageSubtab(page, 'Execution')).toBeVisible();
    await expect(pageSubtab(page, 'Input')).toHaveAttribute('href', '/rest/runs?tab=input');
    await expect(pageSubtab(page, 'Execution')).toHaveAttribute('href', '/rest/runs?tab=exec');

    const newRelease = await pageTab(page, 'New Release').boundingBox();
    const input = await pageSubtab(page, 'Input').boundingBox();
    expect(newRelease, 'New Release page tab should be laid out').toBeTruthy();
    expect(input, 'Input subtab should be laid out').toBeTruthy();
    expect(input!.y).toBeGreaterThan(newRelease!.y + newRelease!.height - 1);
  });

  test('subtabs are hidden on Setup and Releases', async ({ page }) => {
    await page.goto('/rest/setup');
    await expect(pageSubtab(page, 'Input')).toHaveCount(0);
    await expect(pageSubtab(page, 'Execution')).toHaveCount(0);
    await expect(topNav(page).locator('.juneau-page-nav-section')).toHaveCount(3);

    await page.goto('/rest/releases');
    await expect(pageSubtab(page, 'Input')).toHaveCount(0);
    await expect(pageSubtab(page, 'Execution')).toHaveCount(0);
  });

  test('explicit tab= selects Input vs Execution', async ({ page }) => {
    await page.goto('/rest/runs?tab=input');
    await expect(pageSubtab(page, 'Input')).toHaveAttribute('aria-current', 'page');
    await expect(page.locator('#nr-panel-input')).toBeVisible();

    await page.goto('/rest/runs?tab=exec');
    await expect(pageSubtab(page, 'Execution')).toHaveAttribute('aria-current', 'page');
    await expect(page.locator('#nr-panel-exec')).toBeVisible();
  });

  test('there is no SAFE / simulate-mode banner or copy', async ({ page }) => {
    await page.goto('/rest/runs');
    await expect(page.getByText(/SAFE MODE/i)).toHaveCount(0);
    await expect(page.getByText(/simulate/i)).toHaveCount(0);
    await page.goto('/rest/runs?tab=exec');
    await expect(page.getByText(/SAFE MODE/i)).toHaveCount(0);
  });
});
