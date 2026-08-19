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

test.describe('Home page', () => {
  test('boots and renders the workflow heading', async ({ page }) => {
    const response = await page.goto('/rest/home');
    expect(response?.status()).toBe(200);

    await expect(page.getByRole('heading', { name: 'Workflow' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Prerequisites' })).toBeVisible();

    // Home nav tab is marked active on its own page.
    await expect(page.getByRole('link', { name: 'Home' })).toHaveClass(/active/);
  });
});
