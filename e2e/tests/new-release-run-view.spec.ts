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

import { test, expect } from '@playwright/test';

test.describe('run progress region', () => {
  test('execution page loads the run-view module', async ({ page }) => {
    await page.goto('/rest/runs?tab=exec');
    await expect(page.locator('#nr-panel-exec')).toBeAttached();
    const html = await page.content();
    expect(html).toContain('juneau-run-view.js');
  });

  test('events endpoint answers 404 for an unknown run', async ({ request }) => {
    const res = await request.get('/rest/runs/juneau-run-view/0_0_0-none/events');
    expect(res.status()).toBe(404);
  });
});
