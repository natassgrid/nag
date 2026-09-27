import { test, expect } from '@playwright/test';

test.describe('Result Verification', () => {
  test('public verifier portal loads', async ({ page }) => {
    const verifierBaseUrl =
      process.env['VERIFIER_PORTAL_URL'] || 'http://localhost:4400';
    await page.goto(verifierBaseUrl);
    await expect(page).toHaveTitle(/.+/);
  });

  test('public verify endpoint responds', async ({ request }) => {
    const apiBase =
      process.env['API_BASE_URL'] || 'http://localhost:9000';
    const response = await request.get(
      `${apiBase}/api/v1/public/verify/test-scorecard-id`,
    );
    // 404 for a non-existent scorecard is acceptable; 5xx is not
    expect(response.status()).toBeLessThan(500);
  });
});
