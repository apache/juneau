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

import { test, expect, type Locator, type Page } from '@playwright/test';

/**
 * The Releases tab renders a `<@card type="datatables" id="releases">`: `juneau-console.js` mounts a
 * DataTable inside the `#releases` card body from the card's entry in the `#juneau-page` contract, hydrating rows
 * client-side from `/rest/releases/data`. Row content comes from real `git tag juneau-*` history in the local
 * apache/juneau checkout (rm.repo.dir), so "9.2.0" is expected to always be present as a released version in this
 * environment.
 */
const TABLE_SELECTOR = '#releases';

function releasesTable(page: Page): Locator {
  return page.locator(TABLE_SELECTOR);
}

function dataRows(page: Page): Locator {
  // Scope to tbody rows only — the header row (with any per-column search inputs) lives in <thead>.
  return releasesTable(page).locator('tbody tr');
}

test.describe('Releases table', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/rest/releases');
    await expect(releasesTable(page)).toBeVisible();
    // Wait for the real ajax-loaded data (not DataTables' transient "loading"/"no data" placeholder row) before
    // any test measures row counts, so a race against the initial draw can't be mistaken for a filter effect.
    await expect(releasesTable(page).getByText('9.2.0', { exact: true }).first()).toBeVisible();
  });

  test('loads more than zero data rows', async ({ page }) => {
    await expect(dataRows(page).first()).toBeVisible();
    const count = await dataRows(page).count();
    expect(count).toBeGreaterThan(0);
  });

  test('a known released version appears in the table', async ({ page }) => {
    await expect(releasesTable(page).getByText('9.2.0', { exact: true }).first()).toBeVisible();
  });

  test('status pills render with expected text', async ({ page }) => {
    // The "pill:status" renderer keeps the raw status value as display text (e.g. RELEASED for tag-derived rows)
    // and reuses the shared `.tag.<domain>.<value>` classes (so the span carries `jc-pill tag status released`).
    // Other statuses (DRAFT/DISTRIBUTED/FAILED) are state-dependent and not asserted here since they depend on
    // whatever in-progress runs happen to exist locally.
    await expect(releasesTable(page).locator('.tag.status').filter({ hasText: 'RELEASED' }).first()).toBeVisible();
  });

  test('search filters the visible row count down', async ({ page }) => {
    const initialCount = await dataRows(page).count();
    expect(initialCount).toBeGreaterThan(0);

    const searchBox = page.getByRole('searchbox').or(page.getByPlaceholder(/search/i)).first();
    await expect(searchBox).toBeVisible();
    await searchBox.fill('9.0');

    // expect.poll auto-retries until DataTables' redraw settles.
    await expect
      .poll(async () => dataRows(page).count(), { message: 'row count should drop below the pre-filter count' })
      .toBeLessThan(initialCount);

    // "9.0" should isolate the 9.0.x releases specifically.
    await expect(releasesTable(page).getByText('9.0.0', { exact: true }).first()).toBeVisible();
  });

  test.describe('ribbon (right actions cluster — data-testid="ribbon")', () => {
    // Accessible-name-based assertions only — the control row's exact grouping/order isn't asserted here,
    // just that the expected actions exist, are reachable by accessible name, and live in the right cluster.
    function ribbon(page: Page): Locator {
      return page.getByTestId('ribbon');
    }

    test('export (copy/csv) and refresh actions are present and enabled', async ({ page }) => {
      const copyBtn = ribbon(page).getByRole('button', { name: /copy/i });
      const csvBtn = ribbon(page).getByRole('button', { name: /csv/i });
      const refreshBtn = ribbon(page).getByRole('button', { name: /refresh/i });

      await expect(copyBtn).toBeVisible();
      await expect(copyBtn).toBeEnabled();
      await expect(csvBtn).toBeVisible();
      await expect(csvBtn).toBeEnabled();
      await expect(refreshBtn).toBeVisible();
      await expect(refreshBtn).toBeEnabled();
    });

    test('refresh re-draws the table without erroring', async ({ page }) => {
      const refreshBtn = ribbon(page).getByRole('button', { name: /refresh/i });
      await refreshBtn.click();
      // The table should still be present and populated after a refresh.
      await expect(dataRows(page).first()).toBeVisible();
    });
  });

  test.describe('paging (unified ribbon — single control, data-testid="paging")', () => {
    // Paging now exists in exactly ONE place: the unified segmented ribbon rendered with data-testid="paging"
    // (juneau-views.js buildPagingPill). Scoping to it is no longer strictly required for disambiguation (the
    // old redundant right-side compact prev/next ribbon is gone), but keeping the scope is cheap and future-proof.
    function paging(page: Page): Locator {
      return page.getByTestId('paging');
    }

    test('paging ribbon controls are present by accessible name', async ({ page }) => {
      // aria-labels per juneau-views.js's buildPagingPill: "First page"/"Previous page"/"Next page"/"Last page".
      const pill = paging(page);
      await expect(pill.getByRole('button', { name: 'First page' })).toBeVisible();
      await expect(pill.getByRole('button', { name: 'Previous page' })).toBeVisible();
      await expect(pill.getByRole('button', { name: 'Next page' })).toBeVisible();
      await expect(pill.getByRole('button', { name: 'Last page' })).toBeVisible();
    });

    test('first/prev/next/last are correctly enabled/disabled at the boundaries', async ({ page }) => {
      const totalRows = await dataRows(page).count();
      test.skip(totalRows <= 25, 'not enough rows in this environment to exercise multi-page paging');

      const pill = paging(page);
      const firstBtn = pill.getByRole('button', { name: 'First page' });
      const prevBtn = pill.getByRole('button', { name: 'Previous page' });
      const nextBtn = pill.getByRole('button', { name: 'Next page' });
      const lastBtn = pill.getByRole('button', { name: 'Last page' });

      // On page 1: First/Prev disabled, Next/Last enabled (assuming more than one page of data).
      await expect(firstBtn).toBeDisabled();
      await expect(prevBtn).toBeDisabled();
      await expect(nextBtn).toBeEnabled();
      await expect(lastBtn).toBeEnabled();

      await lastBtn.click();
      // On the last page: First/Prev enabled, Next/Last disabled.
      await expect(nextBtn).toBeDisabled();
      await expect(lastBtn).toBeDisabled();
      await expect(firstBtn).toBeEnabled();
      await expect(prevBtn).toBeEnabled();

      await firstBtn.click();
      await expect(firstBtn).toBeDisabled();
      await expect(prevBtn).toBeDisabled();
    });

    test('when there are enough rows, paging to the last page changes the displayed rows', async ({ page }) => {
      const totalRows = await dataRows(page).count();
      // Default page size is 25 rows (juneau-views.js PAGE_SIZE_OPTIONS[0]); only meaningful to page if there's
      // more than one page's worth of data. Skip gracefully otherwise rather than asserting a false negative.
      test.skip(totalRows <= 25, 'not enough rows in this environment to exercise multi-page paging');

      const firstRowBefore = await dataRows(page).first().innerText();
      await paging(page).getByRole('button', { name: 'Last page' }).click();
      await expect
        .poll(async () => dataRows(page).first().innerText())
        .not.toBe(firstRowBefore);

      // Explicit assertion (the poll above exists only to retry until DataTables' redraw settles): the
      // last page's first row must genuinely differ from the first page's first row.
      const firstRowAfter = await dataRows(page).first().innerText();
      expect(firstRowAfter).not.toBe(firstRowBefore);
    });

    test('the range segment doubles as a page-size menu button', async ({ page }) => {
      const menuBtn = paging(page).locator('.juneau-view-pagingpill-menubtn');
      await expect(menuBtn).toBeVisible();
      await expect(menuBtn).toHaveAttribute('aria-haspopup', 'listbox');
      await expect(menuBtn).toHaveAttribute('aria-expanded', 'false');

      await menuBtn.click();
      await expect(menuBtn).toHaveAttribute('aria-expanded', 'true');

      const menu = paging(page).getByRole('listbox');
      await expect(menu).toBeVisible();
      const options = menu.getByRole('option');
      // "25 rows" / "100 rows" / "All rows", per juneau-views.js PAGE_SIZE_OPTIONS.
      await expect(options).toHaveCount(3);

      // Escape closes the menu and returns focus to the button, without changing the page size.
      await page.keyboard.press('Escape');
      await expect(menuBtn).toHaveAttribute('aria-expanded', 'false');
      await expect(menuBtn).toBeFocused();
    });

    test('picking a larger page size from the menu grows the visible row count and updates the range text', async ({ page }) => {
      const totalRows = await dataRows(page).count();
      test.skip(totalRows <= 25, 'not enough rows in this environment to observe a row-count increase at size 100');

      const menuBtn = paging(page).locator('.juneau-view-pagingpill-menubtn');
      const initialRangeText = await menuBtn.innerText();

      await menuBtn.click();
      await paging(page).getByRole('option', { name: '100 rows' }).click();

      // expect.poll auto-retries until DataTables' redraw settles.
      await expect
        .poll(async () => dataRows(page).count(), { message: 'row count should grow once page size is 100' })
        .toBeGreaterThan(25);
      await expect
        .poll(async () => menuBtn.innerText())
        .not.toBe(initialRangeText);
      await expect(menuBtn).toHaveAttribute('aria-expanded', 'false');
    });
  });
});

/**
 * Column headers of the first `<thead>` row (the second row, when present, is the per-column search row). Used to
 * locate a column by its visible title rather than by a brittle positional index.
 */
function headerCells(page: Page): Locator {
  return releasesTable(page).locator('thead tr').first().locator('th');
}

async function openViewSettings(page: Page): Promise<Locator> {
  // juneau-config.js mountChooser: a "Columns" toolbar button (aria-label "Columns") opens a role=dialog titled
  // "View Settings" (h2#juneau-config-title) with one `.juneau-config-col-row[data-col=<data>]` per catalog column.
  await page.getByRole('button', { name: 'Columns', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'View Settings' });
  await expect(dialog).toBeVisible();
  return dialog;
}

test.describe('Releases View Settings', () => {
  // Isolation: each Playwright test gets a fresh browser context, so localStorage-persisted View Settings from one
  // test never leak into another.
  test.beforeEach(async ({ page }) => {
    await page.goto('/rest/releases');
    await expect(releasesTable(page).getByText('9.2.0', { exact: true }).first()).toBeVisible();
  });

  test('unchecking a column and applying hides it, and the choice survives a reload', async ({ page }) => {
    await expect(headerCells(page).filter({ hasText: /^Stage$/ })).toHaveCount(1);

    const dialog = await openViewSettings(page);
    await dialog.locator('.juneau-config-col-row[data-col="stage"] input.juneau-config-col-vis').uncheck();
    await dialog.getByRole('button', { name: 'Apply', exact: true }).click();

    await expect(headerCells(page).filter({ hasText: /^Stage$/ })).toHaveCount(0);
    // Other default-visible columns are untouched.
    await expect(headerCells(page).filter({ hasText: /^Status$/ })).toHaveCount(1);

    // View Settings are browser-local (localStorage), deliberately NOT carried in the shareable ?state= link
    // (juneau-urlstate.js), so a plain reload must still come back with Stage hidden.
    await page.reload();
    await expect(releasesTable(page).getByText('9.2.0', { exact: true }).first()).toBeVisible();
    await expect(headerCells(page).filter({ hasText: /^Stage$/ })).toHaveCount(0);
    await expect(headerCells(page).filter({ hasText: /^Status$/ })).toHaveCount(1);
  });

  test('a default-hidden column (GitHub) can be shown from View Settings', async ({ page }) => {
    await expect(headerCells(page).filter({ hasText: /^GitHub$/ })).toHaveCount(0);

    const dialog = await openViewSettings(page);
    await dialog.locator('.juneau-config-col-row[data-col="githubReleaseUrl"] input.juneau-config-col-vis').check();
    await dialog.getByRole('button', { name: 'Apply', exact: true }).click();

    await expect(headerCells(page).filter({ hasText: /^GitHub$/ })).toHaveCount(1);
  });
});

test.describe('Releases Copy link', () => {
  test.use({ permissions: ['clipboard-read', 'clipboard-write'] });

  // PENDING UPSTREAM: the framework exposes JuneauViews.init.copyShareableUrl(table, ctx) but does not yet ship the
  // "Copy link" toolbar button (juneau-views.js buildShareableUrl / juneau-config.js copyShareLink comments: the
  // toolbar button is a follow-up). Un-fixme once the button lands upstream (or JRM wires its own).
  // When enabled (once the upstream Copy link toolbar button lands), poll the clipboard with expect.poll rather than reading it once, since the write may
  // land asynchronously relative to the click.
  test.fixme('Copy link puts a ?state= URL on the clipboard', async ({ page }) => {
    await page.goto('/rest/releases');
    await expect(releasesTable(page).getByText('9.2.0', { exact: true }).first()).toBeVisible();

    await page.getByRole('button', { name: /copy link/i }).click();
    const clip = await page.evaluate(() => navigator.clipboard.readText());
    expect(clip).toContain('?state=');
  });
});

test.describe('Releases server-side search', () => {
  test('column header search on Status issues a POST to /rest/releases/data and only RELEASED rows remain', async ({ page }) => {
    await page.goto('/rest/releases');
    await expect(releasesTable(page).getByText('9.2.0', { exact: true }).first()).toBeVisible();

    const statusHeader = headerCells(page).filter({ hasText: /^Status$/ });
    await expect(statusHeader).toHaveCount(1);

    // Status column index is computed from the live header row each time it is used (never a stale early snapshot).
    const statusCells = async (): Promise<string[]> => {
      const idx = (await headerCells(page).allInnerTexts()).findIndex(t => t.trim() === 'Status');
      expect(idx).toBeGreaterThanOrEqual(0);
      return (await dataRows(page).locator(`td:nth-child(${idx + 1})`).allInnerTexts()).map(t => t.trim());
    };

    // Precondition: the unfiltered table has at least one non-RELEASED Status cell, so the filtered assertion below
    // cannot pass vacuously.
    await expect
      .poll(async () => (await statusCells()).some(t => t !== '' && t !== 'RELEASED'),
        { message: 'unfiltered table should contain a non-RELEASED Status cell' })
      .toBe(true);

    // The data endpoint is POST-only JSON (DataTablesQuery); server-mode search must go over POST. Create the wait
    // promises before the triggering action and await them via Promise.all so a rejection is never left unhandled.
    const isSearchPost = (r: { method(): string; url(): string; postData(): string | null }) =>
      r.method() === 'POST' && r.url().includes('/rest/releases/data') && (r.postData() ?? '').includes('RELEASED');
    const reqPromise = page.waitForRequest(r => isSearchPost(r));
    const respPromise = page.waitForResponse(resp => isSearchPost(resp.request()));
    // If a step before the Promise.all throws, these promises would otherwise reject unhandled.
    reqPromise.catch(() => {});
    respPromise.catch(() => {});

    // juneau-views.js renderHeaderSearchIcon / openColumnSearchPopover: a glyph in the header opens a popover whose
    // input is labelled "Search <title>"; Enter commits (server tables do not fetch per keystroke).
    await statusHeader.locator('.juneau-view-col-search-icon').click();
    const input = page.getByRole('textbox', { name: 'Search Status', exact: true });
    await expect(input).toBeVisible();
    await input.fill('RELEASED');
    const [req, resp] = await Promise.all([reqPromise, respPromise, input.press('Enter')]);
    expect(req.method()).toBe('POST');
    expect(resp.ok()).toBe(true);

    // Every visible Status cell must say RELEASED once the filtered draw settles, and at least one row remains.
    await expect
      .poll(async () => {
        const texts = await statusCells();
        return texts.length > 0 && texts.every(t => t === 'RELEASED');
      }, { message: 'all visible Status cells should read RELEASED' })
      .toBe(true);
  });
});
