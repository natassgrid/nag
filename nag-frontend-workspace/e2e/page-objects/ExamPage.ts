import { Page, expect } from '@playwright/test';

/**
 * Page Object Model for the active exam-taking screen.
 */
export class ExamPage {
  constructor(private readonly page: Page) {}

  /** Waits until the question content area is visible, indicating the exam has loaded. */
  async waitForLoad(): Promise<void> {
    await this.page
      .locator(
        '[data-testid="question-content"], [class*="question-content"], [class*="questionContent"]',
      )
      .first()
      .waitFor({ state: 'visible', timeout: 30_000 });
  }

  /**
   * Selects the answer option identified by its letter (A–D).
   * Targets radio inputs or labelled buttons with the matching letter.
   */
  async selectOption(optionLetter: 'A' | 'B' | 'C' | 'D'): Promise<void> {
    const option = this.page
      .locator(
        `[data-testid="option-${optionLetter}"],` +
          ` [data-option="${optionLetter}"],` +
          ` label:has-text("${optionLetter}"),` +
          ` button:has-text("${optionLetter}")`,
      )
      .first();
    await option.click();
  }

  /** Advances to the next question. */
  async nextQuestion(): Promise<void> {
    const btn = this.page
      .locator(
        '[data-testid="next-question-btn"], button:has-text("Next"), button:has-text("next")',
      )
      .first();
    await btn.click();
  }

  /**
   * Clicks the submit exam button and confirms via any confirmation modal
   * that may appear.
   */
  async submitExam(): Promise<void> {
    const submitBtn = this.page
      .locator(
        '[data-testid="submit-exam-btn"], button:has-text("Submit"), button:has-text("submit")',
      )
      .first();
    await submitBtn.click();

    // Handle optional confirmation dialog
    const confirmBtn = this.page.locator(
      '[data-testid="confirm-submit-btn"], button:has-text("Confirm"), button:has-text("Yes")',
    );
    if (await confirmBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await confirmBtn.click();
    }
  }

  /**
   * Asserts that a success / results screen is shown after exam submission,
   * or that the URL has navigated to a results page.
   */
  async assertSubmitted(): Promise<void> {
    const successLocator = this.page.locator(
      '[data-testid="exam-submitted"], [data-testid="results-page"],' +
        ' [class*="success"], [class*="result"]',
    );
    await expect(successLocator.first()).toBeVisible({ timeout: 30_000 });
  }
}
