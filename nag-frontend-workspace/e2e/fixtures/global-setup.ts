import { chromium } from '@playwright/test';

/**
 * Global setup: verifies Keycloak is reachable before the test run.
 * If Keycloak is unavailable (e.g. running tests against a pre-built app without
 * the full stack), the warning is logged and the function returns so tests can
 * still execute with whatever auth state is already present in .auth/.
 */
export default async function globalSetup(): Promise<void> {
  const keycloakBase =
    process.env['KEYCLOAK_URL'] || 'http://localhost:8081';
  const authUrl =
    `${keycloakBase}/realms/exam-realm/protocol/openid-connect/auth` +
    `?client_id=admin-portal-client&response_type=code`;

  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    await page.goto(authUrl, { waitUntil: 'domcontentloaded', timeout: 15_000 });
    console.log('[global-setup] Keycloak reachable at', keycloakBase);
  } catch (err) {
    console.warn(
      '[global-setup] Keycloak unreachable — skipping auth warm-up.',
      (err as Error).message,
    );
  } finally {
    await browser.close();
  }
}
