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
  const { delayMs, whatsappFailureStatus } = testStore.chaosConfig;
  if (whatsappFailureStatus) {
    return res.status(whatsappFailureStatus).json({
      error: {
        message: 'Simulated WhatsApp Business API Gateway Connectivity Failure',
        type: 'OAuthException',
        code: whatsappFailureStatus
      }
    });
  }
  if (delayMs > 0) {
    setTimeout(next, delayMs);
  } else {
    next();
  }
});

/**
 * 1. WhatsApp Business Cloud API Message Send Endpoint
 * Standard Meta / WhatsApp Cloud API: POST /v1/messages or /whatsapp/v1/messages
 */
router.post(['/v1/messages', '/messages'], (req, res) => {
  const { messaging_product, to, type, template, text } = req.body || {};
  const recipient = to || req.query.to;

  if (!recipient) {
    return res.status(400).json({
      error: {
        message: 'Missing required field: to',
        type: 'OAuthException',
        code: 100
      }
    });
  }

  const messageId = 'wamid.' + crypto.randomBytes(16).toString('hex');
  const templateName = template?.name || 'GENERIC_WHATSAPP_TEMPLATE';
  const bodyText = text?.body || template?.components?.[0]?.parameters?.[0]?.text || 'NAG Notification via WhatsApp';

  const waRecord = {
    id: messageId,
    channel: 'WHATSAPP',
    recipient: recipient.toString(),
    type: type || 'template',
    templateName,
    templateComponents: template?.components || [],
    text: bodyText,
    messagingProduct: messaging_product || 'whatsapp',
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  };

  testStore.whatsappOutbox.unshift(waRecord);

  res.status(200).json({
    messaging_product: messaging_product || 'whatsapp',
    contacts: [{
      input: recipient.toString(),
      wa_id: recipient.toString()
    }],
    messages: [{
      id: messageId,
      message_status: 'accepted'
    }]
  });
});

/**
 * 2. Generic WhatsApp Send Endpoint
 * POST /api/v1/whatsapp/send
 */
router.post(['/api/v1/whatsapp/send', '/send'], (req, res) => {
  const { to, phone, recipientPhone, message, body, templateId, templateParams } = req.body || {};
  const targetPhone = to || phone || recipientPhone;

  if (!targetPhone) {
    return res.status(400).json({
      success: false,
      error: 'MISSING_RECIPIENT_PHONE',
      message: 'Recipient phone number is required.'
    });
  }

  const id = 'WA-' + crypto.randomBytes(8).toString('hex');
  const messageText = message || body || `Notification: ${templateId || 'NAG Notification'}`;

  const waRecord = {
    id,
    channel: 'WHATSAPP_DIRECT',
    recipient: targetPhone.toString(),
    templateId: templateId || 'DEFAULT_TEMPLATE',
    templateParams: templateParams || {},
    text: messageText,
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  };

  testStore.whatsappOutbox.unshift(waRecord);

  res.status(200).json({
    success: true,
    messageId: id,
    status: 'DELIVERED'
  });
});

export default router;
