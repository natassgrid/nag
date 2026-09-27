import { test, expect } from '@playwright/test';

test.describe('Admin Portal E2E Suite', () => {
  test('should render admin login page', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/Admin Portal|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should display administrative credential fields', async ({ page }) => {
    await page.goto('/login');
    const inputElements = page.locator('input');
    await expect(inputElements.first()).toBeVisible();
  });
});
