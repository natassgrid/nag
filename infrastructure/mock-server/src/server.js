/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { createApp } from './app.js';

const app = createApp();
const PORT = process.env.MOCK_SERVER_PORT || process.env.PORT || 8099;

app.listen(PORT, '0.0.0.0', () => {
  console.log(`================================================================`);
  console.log(`🚀 NAG Mock Third-Party API Server listening on port ${PORT}`);
  console.log(`   - DigiLocker:  http://localhost:${PORT}/digilocker`);
  console.log(`   - Aadhaar KYC: http://localhost:${PORT}/aadhaar`);
  console.log(`   - MSG91 SMS:   http://localhost:${PORT}/msg91 or /api/v5/otp`);
  console.log(`   - Mock Outbox: http://localhost:${PORT}/mock/sms/latest`);
  console.log(`   - Health:      http://localhost:${PORT}/health`);
  console.log(`================================================================`);
});
