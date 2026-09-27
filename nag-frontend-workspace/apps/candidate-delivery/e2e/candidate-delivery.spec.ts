import { test, expect } from '@playwright/test';

test.describe('Candidate Delivery E2E Suite', () => {
  test('should render candidate authentication login page', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/Candidate Delivery|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should display login input fields for credentials or OTP', async ({ page }) => {
    await page.goto('/login');
    const inputElements = page.locator('input');
    await expect(inputElements.first()).toBeVisible();
  });
});
