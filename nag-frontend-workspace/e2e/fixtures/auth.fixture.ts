import { test as base, BrowserContext } from '@playwright/test';
import * as path from 'path';

type AuthFixtures = {
  adminContext: BrowserContext;
  candidateContext: BrowserContext;
};

/**
 * Extended test fixture that provides pre-authenticated browser contexts for
 * the admin and candidate roles.  When the stored auth state file is absent
 * (e.g. CI without a prior auth setup step) the fixture falls back to a
 * plain context so tests degrade gracefully instead of throwing.
 */
export const test = base.extend<AuthFixtures>({
  adminContext: async ({ browser }, use) => {
    const storageStatePath = path.join(__dirname, '../.auth/admin.json');
    let context: BrowserContext;
    try {
      context = await browser.newContext({ storageState: storageStatePath });
    } catch {
      context = await browser.newContext();
    }
    await use(context);
    await context.close();
  },

  candidateContext: async ({ browser }, use) => {
    const storageStatePath = path.join(__dirname, '../.auth/candidate.json');
    let context: BrowserContext;
    try {
      context = await browser.newContext({ storageState: storageStatePath });
    } catch {
      context = await browser.newContext();
    }
    await use(context);
    await context.close();
  },
});

export { expect } from '@playwright/test';
