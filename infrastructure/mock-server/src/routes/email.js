/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { Router } from 'express';
import crypto from 'node:crypto';
import { testStore } from '../data/mock-data.js';

const router = Router();

/**
 * Dispatch / send email via mock REST gateway.
 * POST /email/send or POST /api/v1/email/send
 */
router.post(['/send', '/api/v1/email/send', '/v1/send'], (req, res) => {
  // Check chaos failure status
  if (testStore.chaosConfig.emailFailureStatus) {
    return res.status(testStore.chaosConfig.emailFailureStatus).json({
      type: 'error',
      message: `Simulated Mock Email Gateway Failure (${testStore.chaosConfig.emailFailureStatus})`
    });
  }

  const { to, recipient, email, subject, text, html, otp, otpCode, templateId } = req.body || {};
  const targetEmail = to || recipient || email || req.query.to || req.query.email;

  if (!targetEmail) {
    return res.status(400).json({
      type: 'error',
      message: 'Recipient email address is required.'
    });
  }

  // Extract OTP if not explicitly passed
  let resolvedOtp = otp || otpCode;
  if (!resolvedOtp && (text || html)) {
    const combined = `${text || ''} ${html || ''}`;
    const otpMatch = combined.match(/\b\d{6}\b/);
    if (otpMatch) {
      resolvedOtp = otpMatch[0];
    }
  }

  const messageId = `mock-mail-${crypto.randomBytes(8).toString('hex')}`;
  const record = {
    messageId,
    to: targetEmail.toString().toLowerCase().trim(),
    subject: subject || 'NAG DPI Verification',
    otpCode: resolvedOtp || '000000',
    text: text || '',
    html: html || '',
    templateId: templateId || 'default-otp',
    sentAt: new Date().toISOString()
  };

  testStore.emailOutbox.unshift(record);

  res.status(200).json({
    type: 'success',
    messageId,
    recipient: record.to,
    otpCode: record.otpCode,
    sentAt: record.sentAt
  });
});

export default router;
