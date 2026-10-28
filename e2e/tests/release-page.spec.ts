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

import { test, expect, Page } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { RELEASE_HOME } from './release-home';

const RUNS = path.join(RELEASE_HOME, 'runs');

function script(cfg: object) {
  fs.writeFileSync(path.join(RELEASE_HOME, 'fake-script.json'), JSON.stringify(cfg));
}

/** Clicks Start (dry run) once the page has loaded its state; resolves when the attempt has a History row. */
async function startDryRun(page: Page) {
  await page.goto('/rest/runs');
  await page.locator('#rl-dry-run').check();
  await clickStart(page);
}

async function clickStart(page: Page) {
  const start = page.locator('#rl-start');
  await expect(start).toBeVisible(); // hidden until the first /current has rendered
  const rows = page.locator('#rl-history tbody tr');
  const before = await rows.count();
  await start.click();
  await expect(rows).toHaveCount(before + 1);
  await expect(newestOutcome(page)).toBeVisible();
}

const newestOutcome = (page: Page) => page.locator('#rl-history tbody tr').first().locator('td').nth(4);

/** Idle: Start is offered and the newest attempt is no longer running (or flagged interrupted). */
async function waitIdle(page: Page) {
  await expect(page.locator('#rl-start')).toBeVisible({ timeout: 20_000 });
  await expect(newestOutcome(page)).not.toHaveText(/^(running|interrupted)/, { timeout: 20_000 });
}

test.describe.configure({ mode: 'serial' });

test.beforeAll(() => {
  test.skip(!!process.env.JRM_E2E_BASE_URL || process.env.JRM_E2E_REUSE === '1',
    'needs the suite-booted app with the fixture release.py');
});

test('history_replay_rebinds_terminal', async ({ page }) => {
  const rows = page.locator('#rl-history tbody tr');
  script({ say: 'FIRST-ATTEMPT' });
  await startDryRun(page);
  await waitIdle(page);
  script({ say: 'SECOND-ATTEMPT' });
  await clickStart(page);
  await waitIdle(page);
  await page.reload();

  await expect(rows.first()).toContainText('dry run');
  await rows.first().getByRole('button', { name: 'Replay' }).click();
  await expect(page.locator('#rl-terminal')).toContainText('SECOND-ATTEMPT');
  await rows.nth(1).getByRole('button', { name: 'Replay' }).click();
  await expect(page.locator('#rl-terminal')).toContainText('FIRST-ATTEMPT');
  await expect(page.locator('#rl-terminal')).not.toContainText('SECOND-ATTEMPT');
  await expect(rows.nth(1)).toHaveClass(/selected/);
});

test.describe('pause banner', () => {
  // A leftover run.json would hide Start for the tests that follow.
  test.afterEach(() => fs.rmSync(RUNS, { recursive: true, force: true }));

  test('pause_banner_shows_and_resume_continues', async ({ page }) => {
    script({ pause: { step: 'vote-gate',
      message: 'The vote is open.  When it closes, run: python3 scripts/release.py --resume --version 9.2.1' } });
    await page.goto('/rest/runs');
    await clickStart(page);
    await expect(page.locator('#rl-pause')).toContainText('--resume --version');
    await expect(page.locator('#rl-resume')).toBeVisible();

    script({ say: 'RESUMED' });
    await page.locator('#rl-resume').click();
    await expect(page.locator('#rl-terminal')).toContainText('RESUMED');
    await expect(page.locator('#rl-pause')).toBeHidden();
  });

  test('pause_banner_for_a_pending_mail_without_pause', async ({ page }) => {
    const dir = path.join(RUNS, '9.2.1');
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, 'run.json'), JSON.stringify({
      schema: 1, details: { version: '9.2.1', rc: 1 }, phase: 'rc', emailPending: 'propose', attempts: [] }));
    await page.goto('/rest/runs');
    await expect(page.locator('#rl-pause')).toContainText('propose');
    await expect(page.locator('#rl-resume')).toBeVisible();
  });
});
