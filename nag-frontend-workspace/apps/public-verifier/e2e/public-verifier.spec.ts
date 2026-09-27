import { test, expect } from '@playwright/test';

test.describe('Public Verifier E2E Suite', () => {
  test('should render public credential verification homepage', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Public Verifier|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should have search query input for credential / scorecard verification', async ({ page }) => {
    await page.goto('/');
    const searchInput = page.locator('input');
    await expect(searchInput.first()).toBeVisible();
  });
});
