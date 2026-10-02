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

// Middleware to inject chaos delay or simulated failure
router.use((req, res, next) => {
  const { delayMs, msg91FailureStatus } = testStore.chaosConfig;
  if (msg91FailureStatus) {
    return res.status(msg91FailureStatus).json({
      type: 'error',
      message: 'Simulated MSG91 Gateway Connectivity Failure',
      status: msg91FailureStatus
    });
  }
  if (delayMs > 0) {
    setTimeout(next, delayMs);
  } else {
    next();
  }
});

/**
 * 1. MSG91 OTP Dispatch Endpoint (Supports both GET and POST with Query Params / JSON Body)
 * Matches Msg91SmsService.java RestClient post calls.
 */
router.all(['/api/v5/otp', '/otp'], (req, res) => {
  const templateId = req.query.template_id || req.body?.template_id;
  const mobile = req.query.mobile || req.body?.mobile;
  const authKey = req.query.authkey || req.body?.authkey || req.headers['authkey'];
  const otp = req.query.otp || req.body?.otp || '000000';
  const sender = req.query.sender || req.body?.sender || 'NAGGov';

  if (!mobile) {
    return res.status(400).json({
      type: 'error',
      message: 'Missing mandatory parameter: mobile'
    });
  }

  const requestId = 'msg91_' + crypto.randomBytes(12).toString('hex');

  const smsRecord = {
    id: requestId,
    channel: 'SMS_MSG91',
    mobile: mobile.toString(),
    sender: sender.toString(),
    templateId: templateId?.toString() || 'DEFAULT_OTP_TEMPLATE',
    otpCode: otp.toString(),
    message: `Your National Assessment Grid (NAG) verification code is ${otp}. Valid for 10 minutes.`,
    sentAt: new Date().toISOString(),
    status: 'DELIVERED',
    authKeyProvided: !!authKey
  };

  testStore.smsOutbox.unshift(smsRecord);

  res.json({
    type: 'success',
    message: 'OTP sent successfully',
    request_id: requestId,
    otp_code: otp.toString()
  });
});

/**
 * 2. MSG91 OTP Verify Endpoint
 */
router.all(['/api/v5/otp/verify', '/otp/verify'], (req, res) => {
  const mobile = req.query.mobile || req.body?.mobile;
  const otp = req.query.otp || req.body?.otp;

  if (!otp) {
    return res.status(400).json({
      type: 'error',
      message: 'Missing OTP code to verify.'
    });
  }

  // Accept predictable E2E test OTP '000000' or matching outbox OTP
  const isZeroOtp = otp.toString() === '000000';
  const matchingMessage = testStore.smsOutbox.find(
    s => (!mobile || s.mobile.includes(mobile.toString())) && s.otpCode === otp.toString()
  );

  if (isZeroOtp || matchingMessage) {
    return res.json({
      type: 'success',
      message: 'OTP verified success'
    });
  }

  res.status(400).json({
    type: 'error',
    message: 'Invalid or expired OTP. (Hint: Use static test OTP 000000 for E2E suites)'
  });
});

/**
 * 3. MSG91 Flow / Campaign SMS Endpoint
 */
router.post(['/api/v5/flow', '/flow'], (req, res) => {
  const { flow_id, recipients } = req.body || {};
  const requestId = 'flow_' + crypto.randomBytes(12).toString('hex');

  if (Array.isArray(recipients)) {
    for (const r of recipients) {
      testStore.smsOutbox.unshift({
        id: 'flow_' + crypto.randomBytes(8).toString('hex'),
        channel: 'MSG91_FLOW',
        mobile: r.mobiles || r.mobile || 'UNKNOWN',
        sender: 'NAGGov',
        templateId: flow_id || 'FLOW_DEFAULT',
        otpCode: r.otp || '000000',
        message: `Notification from NAG Exam Portal. Flow: ${flow_id}`,
        sentAt: new Date().toISOString(),
        status: 'DELIVERED'
      });
    }
  }

  res.json({
    type: 'success',
    message: 'Flow initiated successfully',
    request_id: requestId
  });
});

/**
 * 4. Generic SMS Send Endpoint
 */
router.post(['/api/v1/sms/send', '/send'], (req, res) => {
  const { to, message, otp } = req.body || {};
  const id = 'SMS-' + crypto.randomBytes(8).toString('hex');

  testStore.smsOutbox.unshift({
    id,
    channel: 'GENERIC_SMS',
    mobile: to || 'UNKNOWN',
    sender: 'NAGGov',
    otpCode: otp || '000000',
    message: message || `Verification OTP: ${otp || '000000'}`,
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  });

  res.json({
    success: true,
    messageId: id,
    status: 'QUEUED_FOR_DELIVERY'
  });
});

export default router;
