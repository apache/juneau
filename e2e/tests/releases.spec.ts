import { test, expect, type Locator, type Page } from '@playwright/test';

/**
 * The Releases tab renders a rich-view toolkit table: `table#releases` / `table[data-juneau-view="releases"]`,
 * hydrated client-side from `/rest/releases/data` by juneau-views.js. Row content comes from real `git tag
 * juneau-*` history in the local apache/juneau checkout (rm.repo.dir), so "9.2.0" is expected to always be
 * present as a released version in this environment.
 */
const TABLE_SELECTOR = '#releases, [data-juneau-view="releases"]';

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
    // "tag:status" rendering keeps the raw status value as display text (e.g. RELEASED for tag-derived rows).
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
