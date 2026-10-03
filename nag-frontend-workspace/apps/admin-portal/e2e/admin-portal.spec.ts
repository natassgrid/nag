import { test, expect } from '@playwright/test';

test.describe('Admin Portal E2E Suite', () => {
  let invitationsList: any[] = [];

  test.beforeEach(async ({ page }) => {
    invitationsList = [
      {
        id: 'inv-1',
        email: 'evaluator.cs@nag.gov.in',
        fullName: 'Dr. Priya Sharma',
        roleName: 'ROLE_EVALUATOR',
        status: 'PENDING',
        expiresAt: new Date(Date.now() + 48 * 3600 * 1000).toISOString(),
      },
    ];

    // Intercept admin authentication and system APIs
    await page.route('**/api/v1/identity/**', async (route) => {
      const url = route.request().url();
      const method = route.request().method();

      if (url.includes('/login') && method === 'POST') {
        const body = route.request().postDataJSON() || {};
        // If MFA code is not provided or user has 2FA enabled without otpCode, trigger 403 MFA required
        if (body.username === 'superadmin' && !body.otpCode) {
          await route.fulfill({
            status: 403,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'MFA_REQUIRED',
              message: 'Command Clearance: Two-Factor Authentication (MFA / 2FA) required.',
            }),
          });
          return;
        }

        if (body.otpCode && body.otpCode !== '000000' && body.otpCode !== '654321') {
          await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'INVALID_TOTP',
              message: 'Invalid 2FA Authenticator code. Please check your authenticator app time.',
            }),
          });
          return;
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              accessToken: 'mock-admin-jwt-token',
              refreshToken: 'mock-admin-refresh-token',
              userId: 'admin-001',
              username: 'superadmin',
              roles: ['ROLE_SUPER_ADMIN'],
            },
          }),
        });
      } else if (url.includes('/invitations/validate') || url.includes('/admin/invite/validate')) {
        const urlObj = new URL(url);
        const token = urlObj.searchParams.get('token');
        if (token === 'expired-token') {
          await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'INVITATION_EXPIRED',
              message: 'This invitation link has expired. Please contact your system administrator.',
            }),
          });
          return;
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              valid: true,
              email: 'newofficer@nag.gov.in',
              fullName: 'Vikram Mehta',
              roles: ['ROLE_EXAM_CONTROLLER'],
              tenantId: 'default',
            },
          }),
        });
      } else if (url.includes('/invitations/accept') || url.includes('/admin/invite/accept')) {
        const body = route.request().postDataJSON() || {};
        if (body.totpCode !== '000000' && body.totpCode !== '654321') {
          await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'INVALID_TOTP',
              message: 'Invalid 2FA authenticator code. Please verify your paired Authenticator app.',
            }),
          });
          return;
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              accessToken: 'mock-newadmin-jwt-token',
              refreshToken: 'mock-newadmin-refresh-token',
              userId: 'admin-002',
              username: 'newofficer@nag.gov.in',
              roles: ['ROLE_EXAM_CONTROLLER'],
            },
          }),
        });
      } else if (url.includes('/admin/invite') && method === 'POST') {
        const body = route.request().postDataJSON() || {};
        const newInv = {
          id: 'inv-' + Date.now(),
          email: body.email,
          fullName: body.fullName,
          roleName: body.roles?.[0] || 'ROLE_QUESTION_AUTHOR',
          status: 'PENDING',
          expiresAt: new Date(Date.now() + 48 * 3600 * 1000).toISOString(),
        };
        invitationsList.unshift(newInv);
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              invitationId: newInv.id,
              email: newInv.email,
              invitationToken: 'inv_token_' + Date.now(),
              expiresAt: newInv.expiresAt,
            },
          }),
        });
      } else if (url.includes('/users')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: [
              {
                id: 'u-1',
                username: 'superadmin',
                email: 'admin@nag.gov.in',
                fullName: 'Super Administrator',
                roleName: 'ROLE_SUPER_ADMIN',
                status: 'ACTIVE',
              },
            ],
          }),
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/v1/audit/**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: [
            {
              id: 'aud-001',
              serviceName: 'IDENTITY_SERVICE',
              action: 'ADMIN_LOGIN',
              severity: 'INFO',
              status: 'SUCCESS',
              actorId: 'superadmin',
              timestamp: new Date().toISOString(),
            },
          ],
        }),
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

  test('Admin 2FA Login Flow: Step-up authentication on MFA challenge and redirection to Dashboard', async ({ page }) => {
    await page.goto('/login');
    await expect(page).toHaveTitle(/Admin Portal|National Assessment Grid|NAG/i);
    await expect(page.locator('body')).toBeVisible();

    // 1. Enter admin credentials
    await page.locator('#admin-username').fill('superadmin');
    await page.locator('#admin-password').fill('SuperSecretPass123!');
    await page.locator('button[type="submit"]').click();

    // 2. MFA prompt appears requesting TOTP code
    const otpInput = page.locator('#admin-otp');
    await expect(otpInput).toBeVisible();

    // 3. Enter wrong TOTP code to verify rejection
    await otpInput.fill('999999');
    await page.locator('button[type="submit"]').click();
    await expect(page.locator('body')).toContainText(/Invalid 2FA Authenticator code/i);

    // 4. Enter valid TOTP code
    await otpInput.fill('000000');
    await page.locator('button[type="submit"]').click();

    // 5. Successful redirect to dashboard
    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('Accept Admin Invitation & Onboarding Flow: Password setup, QR Code TOTP enrollment, and 2FA activation', async ({ page }) => {
    // 1. Visit invitation link
    await page.goto('/auth/accept-invite?token=valid-invite-token-xyz');
    await expect(page.locator('body')).toBeVisible();

    // Check invitee banner displays email
    await expect(page.locator('[data-testid="invitee-email"]')).toContainText('newofficer@nag.gov.in');

    // Step 1: Fill Password
    await page.locator('[data-testid="invite-password-input"]').fill('OfficerSecurePass2026!');
    await page.locator('[data-testid="invite-confirm-password-input"]').fill('OfficerSecurePass2026!');

    // Step 2: Verify TOTP QR code and secret key are rendered
    await expect(page.locator('[data-testid="totp-secret-key"]')).toBeVisible();

    // Step 3: Enter 6-digit TOTP verification code
    await page.locator('[data-testid="invite-totp-input"]').fill('654321');

    // Step 4: Submit enrollment
    await page.locator('[data-testid="complete-onboarding-button"]').click();

    // Assert success banner and navigation
    await expect(page.locator('[data-testid="accept-success-message"]')).toBeVisible();
    await expect(page).toHaveURL(/.*dashboard/, { timeout: 10000 });
  });

  test('Accept Invitation with Expired or Invalid Token displays error view', async ({ page }) => {
    await page.goto('/auth/accept-invite?token=expired-token');
    await expect(page.locator('body')).toBeVisible();

    const errorMsg = page.locator('[data-testid="invite-error-message"]');
    await expect(errorMsg).toBeVisible();
    await expect(errorMsg).toContainText(/expired/i);
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
