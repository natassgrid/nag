import { Page, expect } from '@playwright/test';

/**
 * Page Object Model for the admin portal — exam management section.
 */
export class AdminPortalPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the admin portal root. */
  async goto(): Promise<void> {
    await this.page.goto('/');
  }

  /** Click the navigation link that leads to the exams section. */
  async navigateToExams(): Promise<void> {
    const examsLink = this.page
      .locator(
        '[data-testid="nav-exams"], nav a:has-text("Exam"), a[href*="exam"], a[href*="Exam"]',
      )
      .first();
    await examsLink.click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Fills and submits the new-exam form.
   * @param title    Title / name of the exam.
   * @param duration Duration in minutes.
   */
  async createNewExam(title: string, duration: number): Promise<void> {
    // Open creation form (button or link)
    const createBtn = this.page
      .locator(
        '[data-testid="create-exam-btn"], button:has-text("New Exam"),' +
          ' button:has-text("Create"), a:has-text("New Exam")',
      )
      .first();
    await createBtn.click();

    // Fill title
    const titleInput = this.page
      .locator('[data-testid="exam-title-input"], input[name="title"], input[placeholder*="title" i]')
      .first();
    await titleInput.fill(title);

    // Fill duration
    const durationInput = this.page
      .locator(
        '[data-testid="exam-duration-input"], input[name="duration"], input[placeholder*="duration" i]',
      )
      .first();
    await durationInput.fill(String(duration));

    // Submit the form
    const saveBtn = this.page
      .locator('[data-testid="save-exam-btn"], button[type="submit"], button:has-text("Save")')
      .first();
    await saveBtn.click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Asserts that an exam with the given title appears in the exam list. */
  async assertExamInList(title: string): Promise<void> {
    const listItem = this.page
      .locator(
        '[data-testid="exam-list-item"], tr, li, [class*="exam-item"], [class*="examItem"]',
      )
      .filter({ hasText: title });
    await expect(listItem.first()).toBeVisible({ timeout: 10_000 });
  }
}
