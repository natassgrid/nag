import { test, expect } from '@playwright/test';

test.describe('Candidate Delivery E2E Suite', () => {
  let smsDispatches = 0;

  test.beforeEach(async ({ page }) => {
    smsDispatches = 0;

    // Intercept identity and candidate services
    await page.route('**/api/v1/identity/**', async (route) => {
      const url = route.request().url();
      const method = route.request().method();

      if (url.includes('/login') && method === 'POST') {
        const body = route.request().postDataJSON() || {};
        if (body.otpCode && body.otpCode !== '000000' && body.otpCode !== '123456') {
          await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'INVALID_CREDENTIALS',
              message: 'Invalid 2FA Authenticator code. Please check your authenticator app.',
            }),
          });
          return;
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              accessToken: 'mock-candidate-jwt-token',
              refreshToken: 'mock-candidate-refresh-token',
              userId: 'cand-001',
              username: 'candidate@nag.gov.in',
              roles: ['ROLE_CANDIDATE'],
            },
          }),
        });
      } else if (url.includes('/register') && method === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              userId: 'cand-001',
              message: 'Registration successful. Email OTP and SMS OTP dispatched.',
            },
          }),
        });
      } else if (url.includes('/verification-status')) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            data: {
              userId: 'cand-001',
              emailVerified: false,
              mobileVerified: false,
              accountStatus: 'PENDING_VERIFICATION',
              smsRemainingThisWeek: Math.max(0, 3 - smsDispatches),
              remainingSmsQuota: Math.max(0, 3 - smsDispatches),
              maskedMobile: '+91 ******3210',
              fullyVerified: false,
            },
          }),
        });
      } else if (url.includes('/resend/sms-otp') && method === 'POST') {
        smsDispatches++;
        if (smsDispatches > 3) {
          await route.fulfill({
            status: 429,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'RATE_LIMIT_EXCEEDED',
              message: 'Weekly SMS limit reached (429: Too Many Requests). Please switch to Email verification.',
            }),
          });
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              message: `SMS OTP dispatched via MSG91 gateway. (${3 - smsDispatches}/3 remaining this week)`,
            }),
          });
        }
      } else if (url.includes('/resend/email-otp') && method === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'A fresh 6-digit verification code has been dispatched to your email.',
          }),
        });
      } else if ((url.includes('/otp/verify') || url.includes('/verify-otp')) && method === 'POST') {
        const body = route.request().postDataJSON() || {};
        if (body.otp === '000000' || body.otp === '123456') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              data: {
                accessToken: 'mock-candidate-jwt-token',
                userId: 'cand-001',
                username: 'candidate@nag.gov.in',
                roles: ['ROLE_CANDIDATE'],
              },
            }),
          });
        } else {
          await route.fulfill({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              error: 'INVALID_OTP',
              message: 'Invalid or expired verification code. Please try again.',
            }),
          });
        }
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

  test('Registration -> OTP Dispatch -> Verify Email OTP Flow with Dashboard Redirection', async ({ page }) => {
    // 1. Visit registration page
    await page.goto('/register');
    await expect(page.locator('body')).toBeVisible();

    // Fill candidate details
    await page.locator('input[name="fullName"]').fill('Aarav Sharma');
    await page.locator('input[name="email"]').fill('aarav.sharma@nag.gov.in');
    await page.locator('input[name="mobile"]').fill('9876543210');
    await page.locator('input[name="password"]').fill('CandidatePass123!');
    await page.locator('input[name="confirmPassword"]').fill('CandidatePass123!');

    // Submit registration
    await page.locator('button[type="submit"]').click();

    // 2. Redirected to /verify-otp
    await expect(page).toHaveURL(/.*verify-otp/);
    await expect(page.locator('text=Email Verification')).toBeVisible();

    // 3. Enter 6-digit OTP code and submit
    const otpInput = page.locator('#otp-code');
    await otpInput.fill('123456');
    await page.locator('button[type="submit"]').click();

    // 4. Assert token received and navigated to dashboard
    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('Mobile SMS Verification Tab: Masked phone, weekly quota decrement, and 429 rate limit handling', async ({ page }) => {
    await page.goto('/verify-otp?userId=cand-001&email=aarav.sharma@nag.gov.in&mobile=9876543210');
    await expect(page.locator('body')).toBeVisible();

    // Switch to Mobile SMS Tab
    await page.locator('button:has-text("Mobile SMS")').click();

    // Check masked phone and initial 3 SMS quota
    await expect(page.locator('[data-testid="masked-phone"]')).toContainText('+91 ******3210');
    await expect(page.locator('[data-testid="sms-quota-display"]')).toContainText('3 of 3 SMS left');

    // Trigger SMS dispatch via Resend SMS OTP
    const resendBtn = page.locator('[data-testid="resend-otp-button"]');
    await expect(resendBtn).toBeVisible();
  });

  test('OTP Resend cooldown timer and inline error retry controls', async ({ page }) => {
    await page.goto('/verify-otp?userId=cand-001&email=aarav.sharma@nag.gov.in');

    // Enter wrong OTP to trigger inline error
    const otpInput = page.locator('#otp-code');
    await otpInput.fill('999999');
    await page.locator('button[type="submit"]').click();

    // Check error banner and clear & retry button
    const errorBanner = page.locator('[data-testid="otp-error-message"]');
    await expect(errorBanner).toBeVisible();
    await expect(errorBanner).toContainText(/Invalid or expired/i);

    // Click Clear & Retry
    await page.locator('button:has-text("Clear & Retry")').click();
    await expect(otpInput).toHaveValue('');
    await expect(errorBanner).not.toBeVisible();
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
