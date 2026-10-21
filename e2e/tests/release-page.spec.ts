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

const ANSWERS = path.join(RELEASE_HOME, 'fake-answers.jsonl');

function script(cfg: object) {
  fs.writeFileSync(path.join(RELEASE_HOME, 'fake-script.json'), JSON.stringify(cfg));
  fs.rmSync(ANSWERS, { force: true });
}

function answers(): any[] {
  return fs.existsSync(ANSWERS)
    ? fs.readFileSync(ANSWERS, 'utf8').trim().split('\n').filter(Boolean).map((l) => JSON.parse(l))
    : [];
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

const dialog = (page: Page) => page.locator('#rl-dialog');

test.describe.configure({ mode: 'serial' });

test.beforeAll(() => {
  test.skip(!!process.env.JRM_E2E_BASE_URL || process.env.JRM_E2E_REUSE === '1',
    'needs the suite-booted app with the fixture release.py');
});

test('dialog_renders_each_kind', async ({ page }) => {
  script({
    prompts: [
      { kind: 'text', key: 'release.rc', title: 'Release candidate', message: 'Release candidate number', default: '1' },
      { kind: 'secret', key: 'release.test-secret', title: 'Test secret', message: 'A secret' },
      { kind: 'confirm', key: 'release.nexus-release', message: 'Type 9.2.1 to release', expect: '9.2.1', danger: true },
      { kind: 'choice', key: 'vote.outcome', message: 'Outcome',
        options: [{ value: 'passed', label: 'Passed' }, { value: 'failed', label: 'Failed' }] },
      { kind: 'form', key: 'release.details', title: 'New release',
        fields: [{ name: 'version', label: 'Version', kind: 'text', default: '9.2.1', required: true },
                 { name: 'highlights', label: 'Highlights', kind: 'multiline', default: '' }] },
    ],
  });
  await startDryRun(page);

  await expect(dialog(page).getByRole('heading', { name: 'Release candidate' })).toBeVisible();
  await expect(dialog(page).locator('input')).toHaveValue('1');
  await dialog(page).locator('input').fill('2');
  await dialog(page).getByRole('button', { name: 'OK' }).click();

  await expect(dialog(page).getByRole('heading', { name: 'Test secret' })).toBeVisible();
  await expect(dialog(page).locator('input')).toHaveAttribute('type', 'password');
  await expect(dialog(page).locator('input')).toHaveAttribute('autocomplete', 'off');
  await dialog(page).locator('input').fill('hunter2');
  await dialog(page).getByRole('button', { name: 'OK' }).click();

  const ok = dialog(page).getByRole('button', { name: 'OK' });
  await expect(dialog(page).locator('input')).toBeVisible();
  await expect(ok).toBeDisabled();
  await dialog(page).locator('input').fill('9.2.');
  await expect(ok).toBeDisabled();
  await dialog(page).locator('input').fill('9.2.1');
  await ok.click();

  await dialog(page).getByLabel('Failed').check();
  await dialog(page).getByRole('button', { name: 'OK' }).click();

  await expect(dialog(page).getByRole('heading', { name: 'New release' })).toBeVisible();
  await dialog(page).getByLabel('Highlights').fill('Faster');
  await dialog(page).getByRole('button', { name: 'OK' }).click();

  await waitIdle(page);
  expect(answers().map((a) => a.value)).toEqual(['2', 'hunter2', '9.2.1', 'failed', { version: '9.2.1', highlights: 'Faster' }]);
  expect(answers().every((a) => a.source === 'entered')).toBe(true);
  // The terminal transcript never shows the secret: check the log on disk (the page may not have rendered it all),
  // and that the log really was read by finding the masked prompt line in it.
  const attempts = path.join(RELEASE_HOME, 'attempts');
  const newest = fs.readdirSync(attempts).sort().pop()!;
  const log = fs.readFileSync(path.join(attempts, newest, 'terminal.log'), 'utf8');
  expect(log).toContain('A secret: ********');
  expect(log).not.toContain('hunter2');
  await expect(page.locator('#rl-terminal')).not.toContainText('hunter2');
});

test('email_copy_buttons', async ({ page, context }) => {
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  const body = 'I am pleased to be calling this vote for the release of Apache Juneau 9.2.1.';
  script({
    prompts: [{
      kind: 'email', key: 'email.vote',
      email: { to: ['dev@juneau.apache.org'], cc: [], subject: '[VOTE] Release Apache Juneau 9.2.1 RC1', body,
               mailto: 'mailto:dev@juneau.apache.org?subject=%5BVOTE%5D' },
    }],
  });
  await startDryRun(page);

  const rows = dialog(page).locator('.rl-email-row');
  await expect(rows).toHaveCount(3); // no Cc row when cc is empty
  await rows.nth(1).getByRole('button', { name: 'Copy' }).click();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe('[VOTE] Release Apache Juneau 9.2.1 RC1');
  await rows.nth(2).getByRole('button', { name: 'Copy' }).click();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(body);
  await expect(dialog(page).getByRole('link', { name: 'Open in mail client' })).toHaveAttribute('href', /^mailto:dev@juneau/);

  await dialog(page).getByRole('button', { name: 'Not yet' }).click();
  await waitIdle(page);
  expect(answers().map((a) => a.value)).toEqual(['not-yet']);
});

test('confirm_yes_no_without_expect', async ({ page }) => {
  script({
    prompts: [
      { kind: 'confirm', key: 'c.one', message: 'Proceed one?', default: false },
      { kind: 'confirm', key: 'c.two', message: 'Proceed two?', default: true },
    ],
  });
  await startDryRun(page);
  await expect(dialog(page).getByRole('button', { name: 'Yes' })).toBeVisible();
  await dialog(page).getByRole('button', { name: 'Yes' }).click();
  await expect(dialog(page).getByRole('heading', { name: 'Proceed two?' })).toBeVisible();
  await dialog(page).getByRole('button', { name: 'No' }).click();
  await waitIdle(page);
  expect(answers().map((a) => a.value)).toEqual([true, false]);
});

test('prompt_error_is_displayed', async ({ page }) => {
  script({ prompts: [{ kind: 'text', key: 'e.one', message: 'Version', error: 'Not a valid version: x' }] });
  await startDryRun(page);
  await expect(dialog(page).locator('.rl-error')).toHaveText('Not a valid version: x');
  await dialog(page).getByRole('button', { name: 'Cancel' }).click();
  await waitIdle(page);
});

test('escape_cancels_a_prompt_but_not_an_email', async ({ page }) => {
  script({
    prompts: [
      { kind: 'text', key: 'esc.one', message: 'Ask me' },
      { kind: 'email', key: 'email.vote',
        email: { to: ['dev@juneau.apache.org'], cc: [], subject: 'S', body: 'B', mailto: 'mailto:dev@juneau.apache.org' } },
    ],
  });
  await startDryRun(page);
  await expect(dialog(page).getByRole('heading', { name: 'Ask me' })).toBeVisible();
  await page.keyboard.press('Escape');
  // Esc is Cancel: the script gets a cancel answer and the next prompt (the email) opens.
  await expect(dialog(page).getByRole('link', { name: 'Open in mail client' })).toBeVisible();
  expect(answers()[0].cancel).toBeTruthy();
  // Esc on the email prompt would abort the step, so it must leave the dialog open.
  await page.keyboard.press('Escape');
  await expect(dialog(page).getByRole('button', { name: 'Not yet' })).toBeVisible();
  await dialog(page).getByRole('button', { name: 'Not yet' }).click();
  await waitIdle(page);
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
