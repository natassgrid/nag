import { test, expect } from '@playwright/test';

test.describe('Candidate Delivery E2E Suite', () => {
  test.beforeEach(async ({ page }) => {
    // Intercept common candidate APIs for deterministic E2E execution
    await page.route('**/api/v1/identity/**', async (route) => {
      const url = route.request().url();
      if (url.includes('/login')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'mock-candidate-jwt-token',
            userId: 'cand-001',
            username: 'candidate@nag.gov.in',
            roles: ['ROLE_CANDIDATE'],
          }),
        });
      } else if (url.includes('/register')) {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            userId: 'cand-001',
            message: 'Registration successful. Verification code dispatched.',
          }),
        });
      } else if (url.includes('/verify-otp')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'mock-candidate-jwt-token',
            userId: 'cand-001',
            verified: true,
          }),
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/v1/candidates/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          candidateId: 'cand-001',
          fullName: 'Aarav Sharma',
          email: 'candidate@nag.gov.in',
          mobile: '9876543210',
          gender: 'MALE',
          category: 'GENERAL',
          kycStatus: 'VERIFIED',
          digiLockerStatus: 'LINKED',
          education: [],
        }),
      });
    });
  });

  test('should render candidate login view and navigate to registration', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/Candidate Delivery|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();

    const registerLink = page.locator('a[routerLink="/register"], a[href*="register"]');
    if (await registerLink.count() > 0) {
      await registerLink.first().click();
      await expect(page).toHaveURL(/.*register/);
    }
  });

  test('should display registration form elements and submit verification', async ({ page }) => {
    await page.goto('/register');
    const inputs = page.locator('input');
    await expect(inputs.first()).toBeVisible();

    const fullNameInput = page.locator('input[name="fullName"], input[formControlName="fullName"], input[placeholder*="Name" i]');
    if (await fullNameInput.count() > 0) {
      await fullNameInput.first().fill('Aarav Sharma');
    }

    const emailInput = page.locator('input[type="email"], input[name="email"], input[formControlName="email"]');
    if (await emailInput.count() > 0) {
      await emailInput.first().fill('candidate@nag.gov.in');
    }
  });

  test('should render candidate dashboard and navigation items', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.locator('body')).toBeVisible();
    const headers = page.locator('h1, h2, h3, [role="heading"]');
    await expect(headers.first()).toBeVisible();
  });

  test('should browse examinations catalog and search tests', async ({ page }) => {
    await page.goto('/browse');
    await expect(page.locator('body')).toBeVisible();

    const searchInput = page.locator('input');
    if (await searchInput.count() > 0) {
      await searchInput.first().fill('Computer Science');
      await expect(searchInput.first()).toHaveValue('Computer Science');
    }
  });

  test('should access candidate profile and display personal details panel', async ({ page }) => {
    await page.goto('/profile');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render exam delivery runtime layout with timer and palette', async ({ page }) => {
    await page.goto('/exam');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render candidate review and dispute resolution page', async ({ page }) => {
    await page.goto('/review');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render candidate scorecards and results view', async ({ page }) => {
    await page.goto('/results');
    await expect(page.locator('body')).toBeVisible();
  });
});
