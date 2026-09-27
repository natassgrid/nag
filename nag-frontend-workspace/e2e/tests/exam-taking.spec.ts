import { test, expect } from '@playwright/test';

test.describe('Exam Taking (Candidate)', () => {
  test('candidate portal loads', async ({ page }) => {
    const candidateBaseUrl =
      process.env['CANDIDATE_PORTAL_URL'] || 'http://localhost:4300';
    await page.goto(candidateBaseUrl);
    await expect(page).toHaveTitle(/.+/);
  });

  test('exam list page is accessible', async ({ page }) => {
    const candidateBaseUrl =
      process.env['CANDIDATE_PORTAL_URL'] || 'http://localhost:4300';
    await page.goto(candidateBaseUrl + '/exams');
    await expect(page).not.toHaveURL(/error|500/);
  });
});
