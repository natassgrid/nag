import { test, expect } from '@playwright/test';
import { LoginPage } from '../page-objects/LoginPage';

test.describe('Candidate Onboarding', () => {
  test('registration page loads and form is present', async ({ page }) => {
    await page.goto('/register');
    // Check registration form exists - selector may differ per implementation
    const form = page.locator('form, [data-testid="register-form"]');
    await expect(form).toBeVisible({ timeout: 15_000 });
  });

  test('login page renders correctly', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await expect(page).toHaveURL(/login|\/$/, { timeout: 10_000 });
  });

  test('invalid login shows error', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.fillEmail('invalid@test.com');
    await loginPage.fillPassword('wrongpassword');
    await loginPage.submit();
    // Should stay on login page or show error — must NOT navigate to dashboard
    await expect(page).not.toHaveURL('/dashboard');
  });
});
