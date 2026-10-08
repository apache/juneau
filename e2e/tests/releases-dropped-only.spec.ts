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
 * The 'Dropped only' ribbon option is a column-scoped filter.  It must reach the server in the JSON request body as the
 * BeanQuery expression `$eq(DROPPED)`, and the table must then show only DROPPED rows.  Before the ribbon encoding was
 * shared with the Java decoder through a corpus, this was the filter that silently stopped applying.
 */
const TABLE_SELECTOR = '#releases';

function dataRows(page: Page) {
  return page.locator(TABLE_SELECTOR).locator('tbody tr');
}

test.describe('releases: dropped-only ribbon option', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/rest/releases');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(page.locator(TABLE_SELECTOR)).toBeVisible();
    await expect(dataRows(page).first()).toBeVisible();
  });

  test('turning the option on sends $eq(DROPPED) and shows only DROPPED rows', async ({ page }) => {
    const [req] = await Promise.all([
      page.waitForRequest(r => r.method() === 'POST' && (r.postData() ?? '').includes('$eq(DROPPED)')),
      page.getByTestId('ribbon').getByRole('button', { name: 'Dropped only' }).click(),
    ]);
    expect(req.postDataJSON().columns.some((c: any) => c?.search?.value === '$eq(DROPPED)')).toBe(true);

    await page.waitForLoadState('networkidle');
    // The fixture is real tag history, so it may hold no DROPPED release: then the table is empty and the loop is
    // vacuous, but any status pill that IS shown must be DROPPED.
    const statuses = await page.locator(TABLE_SELECTOR).locator('.tag.status').allInnerTexts();
    for (const s of statuses) expect(s.trim()).toBe('DROPPED');
  });
});
