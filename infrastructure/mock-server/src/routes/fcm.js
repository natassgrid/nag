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
  const { delayMs, pushFailureStatus } = testStore.chaosConfig;
  if (pushFailureStatus) {
    return res.status(pushFailureStatus).json({
      error: {
        code: pushFailureStatus,
        message: 'Simulated Push / FCM Gateway Connectivity Failure',
        status: 'UNAVAILABLE'
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
 * 1. FCM Legacy / Standard Send Endpoint
 * POST /fcm/send or /send (when mounted under /fcm)
 */
router.post(['/fcm/send', '/send-fcm'], (req, res) => {
  const { to, registration_ids, token, notification, data } = req.body || {};
  const targetToken = to || token || (Array.isArray(registration_ids) ? registration_ids[0] : null);

  if (!targetToken && (!registration_ids || registration_ids.length === 0)) {
    return res.status(400).json({
      error: 'Missing registration token (to or registration_ids)'
    });
  }

  const messageId = 'fcm_' + crypto.randomBytes(10).toString('hex');
  const title = notification?.title || data?.title || 'NAG Push Notification';
  const body = notification?.body || data?.body || 'New notification received';

  const pushRecord = {
    id: messageId,
    channel: 'FCM_PUSH',
    targetToken: targetToken || 'MULTICAST',
    registrationIds: registration_ids || (targetToken ? [targetToken] : []),
    title,
    body,
    data: data || {},
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  };

  testStore.pushOutbox.unshift(pushRecord);

  res.status(200).json({
    multicast_id: Math.floor(Math.random() * 1000000000000),
    success: 1,
    failure: 0,
    canonical_ids: 0,
    results: [{ message_id: messageId }]
  });
});

/**
 * 2. Firebase Cloud Messaging HTTP v1 API Send Endpoint
 * POST /v1/projects/:projectId/messages:send
 */
router.post('/v1/projects/:projectId/messages:send', (req, res) => {
  const { projectId } = req.params;
  const message = req.body?.message || {};
  const token = message.token || message.topic;

  if (!token) {
    return res.status(400).json({
      error: {
        code: 400,
        message: 'The request message must contain a valid token or topic.',
        status: 'INVALID_ARGUMENT'
      }
    });
  }

  const messageId = crypto.randomBytes(12).toString('hex');
  const title = message.notification?.title || message.data?.title || 'NAG Push Notification';
  const body = message.notification?.body || message.data?.body || '';

  const pushRecord = {
    id: messageId,
    channel: 'FCM_HTTP_V1',
    projectId,
    targetToken: token,
    title,
    body,
    data: message.data || {},
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  };

  testStore.pushOutbox.unshift(pushRecord);

  res.status(200).json({
    name: `projects/${projectId}/messages/${messageId}`
  });
});

/**
 * 3. Generic Push Send Endpoint
 * POST /api/v1/push/send or /send
 */
router.post(['/api/v1/push/send', '/send', '/send-generic'], (req, res) => {
  // If request is targeted directly at FCM endpoint with FCM legacy schema
  if (req.originalUrl?.includes('/fcm') || req.body?.registration_ids) {
    const { to, registration_ids, token, notification, data } = req.body || {};
    const targetToken = to || token || (Array.isArray(registration_ids) ? registration_ids[0] : null);

    if (!targetToken && (!registration_ids || registration_ids.length === 0)) {
      return res.status(400).json({
        error: 'Missing registration token (to or registration_ids)'
      });
    }

    const messageId = 'fcm_' + crypto.randomBytes(10).toString('hex');
    const title = notification?.title || data?.title || 'NAG Push Notification';
    const body = notification?.body || data?.body || 'New notification received';

    const pushRecord = {
      id: messageId,
      channel: 'FCM_PUSH',
      targetToken: targetToken || 'MULTICAST',
      registrationIds: registration_ids || (targetToken ? [targetToken] : []),
      title,
      body,
      data: data || {},
      sentAt: new Date().toISOString(),
      status: 'DELIVERED'
    };

    testStore.pushOutbox.unshift(pushRecord);

    return res.status(200).json({
      multicast_id: Math.floor(Math.random() * 1000000000000),
      success: 1,
      failure: 0,
      canonical_ids: 0,
      results: [{ message_id: messageId }]
    });
  }

  const { fcmToken, token, deviceToken, to, title, body, subject, message, payload, data } = req.body || {};
  const targetToken = fcmToken || token || deviceToken || to;

  if (!targetToken) {
    return res.status(400).json({
      success: false,
      error: 'MISSING_DEVICE_TOKEN',
      message: 'FCM or device token is required.'
    });
  }

  const id = 'PUSH-' + crypto.randomBytes(8).toString('hex');
  const pushTitle = title || subject || 'NAG Push Notification';
  const pushBody = body || message || '';

  const pushRecord = {
    id,
    channel: 'GENERIC_PUSH',
    targetToken,
    title: pushTitle,
    body: pushBody,
    payload: payload || data || {},
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  };

  testStore.pushOutbox.unshift(pushRecord);

  res.status(200).json({
    success: true,
    messageId: id,
    status: 'DELIVERED'
  });
});

export default router;
