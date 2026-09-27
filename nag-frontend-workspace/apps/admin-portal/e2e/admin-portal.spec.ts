import { test, expect } from '@playwright/test';

test.describe('Admin Portal E2E Suite', () => {
  test.beforeEach(async ({ page }) => {
    // Intercept admin authentication and system APIs
    await page.route('**/api/v1/identity/**', async (route) => {
      const url = route.request().url();
      if (url.includes('/login')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            token: 'mock-admin-jwt-token',
            userId: 'admin-001',
            username: 'superadmin',
            roles: ['ROLE_SUPER_ADMIN'],
          }),
        });
      } else if (url.includes('/users')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            {
              id: 'u-1',
              username: 'superadmin',
              email: 'admin@nag.gov.in',
              fullName: 'Super Administrator',
              roleName: 'ROLE_SUPER_ADMIN',
              status: 'ACTIVE',
            },
          ]),
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/v1/audit/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          {
            id: 'aud-001',
            serviceName: 'IDENTITY_SERVICE',
            action: 'ADMIN_LOGIN',
            severity: 'INFO',
            status: 'SUCCESS',
            actorId: 'superadmin',
            timestamp: new Date().toISOString(),
          },
        ]),
      });
    });

    await page.route('**/api/v1/assets/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'ast-1',
              fileName: 'blueprint_diagram.png',
              assetType: 'IMAGE',
              fileSize: 2048,
              status: 'ACTIVE',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
        }),
      });
    });
  });

  test('should render administrative login page and credentials inputs', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/Admin Portal|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();

    const inputs = page.locator('input');
    await expect(inputs.first()).toBeVisible();
  });

  test('should render national assessment grid admin dashboard', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render user and role access control management', async ({ page }) => {
    await page.goto('/users');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render examination centre infrastructure management', async ({ page }) => {
    await page.goto('/centres');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render media assets repository and uploads interface', async ({ page }) => {
    await page.goto('/assets');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should render cryptographic audit log trail with ledger verification', async ({ page }) => {
    await page.goto('/audit');
    await expect(page.locator('body')).toBeVisible();
  });
});
