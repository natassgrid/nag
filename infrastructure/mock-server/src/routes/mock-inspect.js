/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { Router } from 'express';
import { AADHAAR_PERSONAS, DIGILOCKER_DOCUMENTS, testStore } from '../data/mock-data.js';

const router = Router();

/**
 * 1. Healthcheck Endpoint
 */
router.get(['/health', '/actuator/health'], (req, res) => {
  res.json({
    status: 'UP',
    server: 'NAG Mock Gateway Server',
    version: '1.0.0',
    services: {
      digilocker: 'HEALTHY',
      aadhaarKyc: 'HEALTHY',
      msg91Sms: 'HEALTHY',
      emailGateway: 'HEALTHY'
    },
    smsOutboxCount: testStore.smsOutbox.length,
    emailOutboxCount: testStore.emailOutbox.length,
    activeAadhaarTxns: testStore.aadhaarTxns.size,
    pushedScorecardsCount: testStore.pushedScorecards.length,
    timestamp: new Date().toISOString()
  });
});

/**
 * 2. Get latest SMS sent to a mobile number (for automated E2E assertions)
 */
router.get('/sms/latest', (req, res) => {
  const { mobile } = req.query;

  if (mobile) {
    const cleaned = mobile.toString().replace(/[\s\-\+]/g, '');
    const found = testStore.smsOutbox.find(s =>
      s.mobile.replace(/[\s\-\+]/g, '').includes(cleaned)
    );
    if (found) {
      return res.json({ success: true, message: found });
    }
    return res.status(404).json({
      success: false,
      message: `No SMS message found for mobile pattern: ${mobile}`
    });
  }

  // Return the very latest message if no mobile specified
  if (testStore.smsOutbox.length > 0) {
    return res.json({ success: true, message: testStore.smsOutbox[0] });
  }

  res.status(404).json({ success: false, message: 'SMS outbox is currently empty.' });
});

/**
 * 3. Get all sent SMS messages
 */
router.get('/sms/all', (req, res) => {
  res.json({
    count: testStore.smsOutbox.length,
    messages: testStore.smsOutbox
  });
});

/**
 * 4. Clear SMS outbox
 */
router.delete('/sms/clear', (req, res) => {
  testStore.smsOutbox = [];
  res.json({ success: true, message: 'SMS outbox cleared successfully.' });
});

/**
 * 5. Get latest Email sent to an email address (for automated E2E assertions)
 */
router.get('/email/latest', (req, res) => {
  const { email } = req.query;

  if (email) {
    const cleaned = email.toString().toLowerCase().trim();
    const found = testStore.emailOutbox.find(e =>
      e.to.toLowerCase().includes(cleaned)
    );
    if (found) {
      return res.json({ success: true, message: found });
    }
    return res.status(404).json({
      success: false,
      message: `No email message found for recipient: ${email}`
    });
  }

  // Return the very latest message if no recipient specified
  if (testStore.emailOutbox.length > 0) {
    return res.json({ success: true, message: testStore.emailOutbox[0] });
  }

  res.status(404).json({ success: false, message: 'Email outbox is currently empty.' });
});

/**
 * 6. Get all sent Email messages
 */
router.get('/email/all', (req, res) => {
  res.json({
    count: testStore.emailOutbox.length,
    messages: testStore.emailOutbox
  });
});

/**
 * 7. Clear Email outbox
 */
router.delete('/email/clear', (req, res) => {
  testStore.emailOutbox = [];
  res.json({ success: true, message: 'Email outbox cleared successfully.' });
});

/**
 * 8. Get pushed scorecards
 */
router.get('/digilocker/scorecards', (req, res) => {
  res.json({
    count: testStore.pushedScorecards.length,
    scorecards: testStore.pushedScorecards
  });
});

/**
 * 9. Get sample Aadhaar personas
 */
router.get('/aadhaar/personas', (req, res) => {
  res.json({
    count: Object.keys(AADHAAR_PERSONAS).length,
    personas: AADHAAR_PERSONAS
  });
});

/**
 * 10. Get sample DigiLocker documents
 */
router.get('/digilocker/documents', (req, res) => {
  res.json({
    count: Object.keys(DIGILOCKER_DOCUMENTS).length,
    documents: DIGILOCKER_DOCUMENTS
  });
});

/**
 * 11. Chaos & Fault Injection Configuration
 * Allows E2E test suites to simulate upstream network timeouts, 503 outages, or latency.
 */
router.post('/chaos', (req, res) => {
  const {
    reset,
    delayMs,
    msg91FailureStatus,
    aadhaarFailureStatus,
    digilockerFailureStatus,
    emailFailureStatus
  } = req.body || {};

  if (reset) {
    testStore.reset();
    return res.json({
      success: true,
      message: 'Chaos configuration and mock stores reset to initial clean state.'
    });
  }

  if (typeof delayMs === 'number') {
    testStore.chaosConfig.delayMs = delayMs;
  }
  if (msg91FailureStatus !== undefined) {
    testStore.chaosConfig.msg91FailureStatus = msg91FailureStatus;
  }
  if (aadhaarFailureStatus !== undefined) {
    testStore.chaosConfig.aadhaarFailureStatus = aadhaarFailureStatus;
  }
  if (digilockerFailureStatus !== undefined) {
    testStore.chaosConfig.digilockerFailureStatus = digilockerFailureStatus;
  }
  if (emailFailureStatus !== undefined) {
    testStore.chaosConfig.emailFailureStatus = emailFailureStatus;
  }

  res.json({
    success: true,
    message: 'Chaos configuration updated.',
    chaosConfig: testStore.chaosConfig
  });
});

/**
 * 12. Reset entire mock server state
 */
router.post('/reset', (req, res) => {
  testStore.reset();
  res.json({
    success: true,
    message: 'All mock stores, transactions, outboxes, and chaos settings have been reset.'
  });
});

export default router;
