import { Page } from '@playwright/test';

/**
 * Page Object Model for the login screen.
 *
 * Selector strategy: prefers explicit `data-testid` attributes; falls back to
 * semantic HTML selectors so the POM works even before testid attributes are
 * wired up in the Angular templates.
 */
export class LoginPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the login route. */
  async goto(): Promise<void> {
    await this.page.goto('/login');
  }

  /** Fill the email / username field. */
  async fillEmail(email: string): Promise<void> {
    const locator = this.page
      .locator('[data-testid="email-input"], input[type="email"], input[name="email"]')
      .first();
    await locator.fill(email);
  }

  /** Fill the password field. */
  async fillPassword(password: string): Promise<void> {
    const locator = this.page
      .locator('[data-testid="password-input"], input[type="password"], input[name="password"]')
      .first();
    await locator.fill(password);
  }

  /** Click the submit / login button. */
  async submit(): Promise<void> {
    const locator = this.page
      .locator('[data-testid="login-button"], button[type="submit"]')
      .first();
    await locator.click();
  }

  /**
   * Full login flow: navigate → fill credentials → submit → wait for the URL
   * to change away from the login page.
   */
  async loginAs(email: string, password: string): Promise<void> {
    await this.goto();
    await this.fillEmail(email);
    await this.fillPassword(password);
    await Promise.all([
      this.page.waitForNavigation({ waitUntil: 'networkidle', timeout: 30_000 }),
      this.submit(),
    ]);
  }
}
