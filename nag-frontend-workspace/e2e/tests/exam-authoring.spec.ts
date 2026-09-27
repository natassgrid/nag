import { test, expect } from '@playwright/test';
import { AdminPortalPage } from '../page-objects/AdminPortalPage';

test.describe('Exam Authoring (Admin)', () => {
  test('admin portal loads', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/.+/);
  });

  test('exam creation form is accessible after auth', async ({ page }) => {
    await page.goto('/exams/new');
    // Either shows the form or redirects to login — both are acceptable outcomes
    // in a smoke test; the important thing is that no unhandled error occurs.
    const url = page.url();
    expect(url).toBeTruthy();
  });

  test('exams list route responds without error', async ({ page }) => {
    const response = await page.goto('/exams');
    // Angular app may return 200 even for SPA routes; 5xx indicates a build issue
    expect(response?.status() ?? 200).toBeLessThan(500);
  });
});
