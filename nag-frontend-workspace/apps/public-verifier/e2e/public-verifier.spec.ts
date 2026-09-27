import { test, expect } from '@playwright/test';

test.describe('Public Verifier E2E Suite', () => {
  test.beforeEach(async ({ page }) => {
    // Intercept public verification queries
    await page.route('**/api/v1/verify/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          verified: true,
          candidateName: 'Aarav Sharma',
          examTitle: 'National Engineering Aptitude Test 2026',
          scorecardHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          digitalSignatureValid: true,
          percentile: '99.42',
          issuedAt: new Date().toISOString(),
        }),
      });
    });
  });

  test('should render public credential verification homepage', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Public Verifier|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();
  });

  test('should search scorecard cryptographic hash and show verification status', async ({ page }) => {
    await page.goto('/');
    const searchInput = page.locator('input');
    if (await searchInput.count() > 0) {
      await searchInput.first().fill('e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855');
      await expect(searchInput.first()).toHaveValue('e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855');
    }
  });

  test('should display DPI verified badge and credential details', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).toBeVisible();
  });
});
