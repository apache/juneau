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
