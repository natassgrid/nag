import { Page, Locator, expect } from '@playwright/test';

/**
 * Page Object Model for the candidate dashboard / exam list screen.
 */
export class CandidateDashboardPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the candidate dashboard. */
  async goto(): Promise<void> {
    await this.page.goto('/candidate/dashboard');
  }

  /**
   * Returns a locator for the exam list container.
   * Tries explicit testid first, then common CSS class patterns.
   */
  getExamList(): Locator {
    return this.page.locator(
      '[data-testid="exam-list"], .exam-list, [class*="exam-list"], [class*="examList"]',
    );
  }

  /**
   * Finds an exam card whose text content includes `examTitle` and clicks its
   * start button.
   */
  async clickStartExam(examTitle: string): Promise<void> {
    const card = this.page
      .locator('[data-testid="exam-card"], .exam-card, [class*="exam-card"], [class*="examCard"]')
      .filter({ hasText: examTitle });
    const startBtn = card
      .locator('[data-testid="start-exam-btn"], button')
      .filter({ hasText: /start/i });
    await startBtn.click();
  }

  /** Asserts that an exam card with the given title is visible on the page. */
  async assertExamVisible(title: string): Promise<void> {
    const card = this.page
      .locator('[data-testid="exam-card"], .exam-card, [class*="exam-card"], [class*="examCard"]')
      .filter({ hasText: title });
    await expect(card).toBeVisible({ timeout: 10_000 });
  }
}
