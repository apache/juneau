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

test.describe('step console output', () => {
  test('execution page loads the console-output region and none of the old renderer', async ({ page }) => {
    await page.goto('/rest/runs?tab=exec');
    await expect(page.locator('#nr-panel-exec')).toBeAttached();
    const html = await page.content();
    expect(html).toContain('juneau-console-output.js');
    expect(html).not.toContain('rm-console');
  });

  test('lines endpoint answers 404 for an unknown run and step', async ({ request }) => {
    const res = await request.get('/rest/runs/0.0.0-none/steps/no-such-step/output/lines');
    expect(res.status()).toBe(404);
  });
});
