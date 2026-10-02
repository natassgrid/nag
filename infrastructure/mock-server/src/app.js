/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import express from 'express';
import cors from 'cors';
import digilockerRoutes from './routes/digilocker.js';
import aadhaarRoutes from './routes/aadhaar.js';
import msg91Routes from './routes/msg91.js';
import emailRoutes from './routes/email.js';
import inspectRoutes from './routes/mock-inspect.js';
import { testStore } from './data/mock-data.js';

export function createApp() {
  const app = express();

  // Standard middleware
  app.use(cors());
  app.use(express.json({ limit: '10mb' }));
  app.use(express.urlencoded({ extended: true, limit: '10mb' }));

  // Request logger for debugging E2E interactions
  app.use((req, res, next) => {
    if (process.env.NODE_ENV !== 'test' && !req.url.includes('/health')) {
      const timestamp = new Date().toISOString();
      console.log(`[MOCK-SERVER] ${timestamp} ${req.method} ${req.originalUrl}`);
    }
    next();
  });

  // Health check & Server info
  app.get(['/', '/health', '/actuator/health'], (req, res) => {
    res.json({
      status: 'UP',
      service: 'nag-mock-api-server',
      version: '1.0.0',
      description: 'Mock DPI Third-Party Gateway Server (DigiLocker, Aadhaar e-KYC, MSG91 SMS, Email)',
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
      endpoints: {
        digilocker: '/digilocker/... or /public/oauth2/...',
        aadhaar: '/aadhaar/... or /api/v1/aadhaar/...',
        msg91: '/msg91/... or /api/v5/otp',
        email: '/email/... or /mock/email/...',
        mockInspector: '/mock/...'
      },
      timestamp: new Date().toISOString()
    });
  });

  // Mount DigiLocker routes (both prefixed and root alias)
  app.use('/digilocker', digilockerRoutes);
  app.use('/public/oauth2', digilockerRoutes);
  app.use('/oauth', digilockerRoutes);

  // Mount Aadhaar routes (both prefixed and root alias)
  app.use('/aadhaar', aadhaarRoutes);
  app.use('/api/v1/aadhaar', aadhaarRoutes);

  // Mount MSG91 SMS routes (both prefixed and root alias)
  app.use('/msg91', msg91Routes);
  app.use('/api/v5', msg91Routes);
  app.use('/sms', msg91Routes);

  // Mount Email routes
  app.use('/email', emailRoutes);
  app.use('/api/v1/email', emailRoutes);

  // Mount Mock Inspection / Control routes
  app.use('/mock', inspectRoutes);

  // 404 Handler
  app.use((req, res) => {
    res.status(404).json({
      error: 'NOT_FOUND',
      message: `Mock endpoint ${req.method} ${req.originalUrl} is not registered.`,
      availableRoutes: [
        '/digilocker/oauth/authorize',
        '/digilocker/oauth/token',
        '/digilocker/v1/user/documents',
        '/digilocker/v1/document/download/:docId',
        '/digilocker/v1/credential/push',
        '/aadhaar/v1/otp/generate',
        '/aadhaar/v1/otp/verify',
        '/msg91/api/v5/otp',
        '/api/v5/otp',
        '/api/v5/otp/verify',
        '/email/send',
        '/mock/sms/latest',
        '/mock/email/latest',
        '/mock/chaos'
      ]
    });
  });

  // Global error handler
  app.use((err, req, res, next) => {
    console.error('[MOCK-SERVER ERROR]', err);
    res.status(500).json({
      error: 'INTERNAL_MOCK_ERROR',
      message: err.message || 'An unexpected mock server error occurred.'
    });
  });

  return app;
}
