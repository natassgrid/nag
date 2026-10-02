/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { test, describe, before, after, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import crypto from 'node:crypto';
import { createApp } from '../src/app.js';
import { testStore } from '../src/data/mock-data.js';

describe('NAG Mock Third-Party API Server Test Suite', () => {
  let server;
  let baseUrl;

  before(async () => {
    process.env.NODE_ENV = 'test';
    const app = createApp();
    await new Promise((resolve) => {
      server = http.createServer(app).listen(0, '127.0.0.1', () => {
        const port = server.address().port;
        baseUrl = `http://127.0.0.1:${port}`;
        resolve();
      });
    });
  });

  after(async () => {
    await new Promise((resolve) => {
      if (server) {
        server.close(resolve);
      } else {
        resolve();
      }
    });
  });

  beforeEach(() => {
    testStore.reset();
  });

  // =========================================================================
  // 1. Health & Server Info
  // =========================================================================
  test('GET /health returns UP status and active components', async () => {
    const res = await fetch(`${baseUrl}/health`);
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.status, 'UP');
    assert.strictEqual(body.services.digilocker, 'HEALTHY');
    assert.strictEqual(body.services.aadhaarKyc, 'HEALTHY');
    assert.strictEqual(body.services.msg91Sms, 'HEALTHY');
    assert.strictEqual(body.services.emailGateway, 'HEALTHY');
  });

  // =========================================================================
  // 2. MSG91 SMS Gateway Mock Tests
  // =========================================================================
  describe('MSG91 SMS Gateway Mock Endpoints', () => {
    test('POST /api/v5/otp sends SMS with custom or default OTP 000000', async () => {
      const res = await fetch(`${baseUrl}/api/v5/otp?template_id=NAG_OTP&mobile=9876543210&authkey=testkey&otp=000000`, {
        method: 'POST'
      });
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.type, 'success');
      assert.strictEqual(body.otp_code, '000000');
      assert.ok(body.request_id);

      // Verify outbox
      assert.strictEqual(testStore.smsOutbox.length, 1);
      assert.strictEqual(testStore.smsOutbox[0].mobile, '9876543210');
      assert.strictEqual(testStore.smsOutbox[0].otpCode, '000000');
    });

    test('GET /api/v5/otp/verify validates default static OTP 000000', async () => {
      const res = await fetch(`${baseUrl}/api/v5/otp/verify?mobile=9876543210&otp=000000`);
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.type, 'success');
      assert.strictEqual(body.message, 'OTP verified success');
    });

    test('POST /api/v5/otp/verify fails for non-zero unrecorded OTP', async () => {
      const res = await fetch(`${baseUrl}/api/v5/otp/verify?mobile=9876543210&otp=999888`, {
        method: 'POST'
      });
      assert.strictEqual(res.status, 400);
      const body = await res.json();
      assert.strictEqual(body.type, 'error');
    });

    test('GET /mock/sms/latest inspects outbox for automated E2E assertion', async () => {
      await fetch(`${baseUrl}/api/v5/otp?template_id=NAG_OTP&mobile=919988776655&authkey=testkey&otp=123456`, {
        method: 'POST'
      });

      const inspectRes = await fetch(`${baseUrl}/mock/sms/latest?mobile=9988776655`);
      assert.strictEqual(inspectRes.status, 200);
      const inspectBody = await inspectRes.json();
      assert.strictEqual(inspectBody.success, true);
      assert.strictEqual(inspectBody.message.otpCode, '123456');
    });

    test('DELETE /mock/sms/clear empties the outbox', async () => {
      await fetch(`${baseUrl}/api/v5/otp?mobile=9876543210&otp=000000`, { method: 'POST' });
      assert.strictEqual(testStore.smsOutbox.length, 1);

      const clearRes = await fetch(`${baseUrl}/mock/sms/clear`, { method: 'DELETE' });
      assert.strictEqual(clearRes.status, 200);
      assert.strictEqual(testStore.smsOutbox.length, 0);
    });
  });

  // =========================================================================
  // 3. Email Gateway Mock Tests
  // =========================================================================
  describe('Email Gateway Mock Endpoints', () => {
    test('POST /email/send saves email with extracted OTP code to outbox', async () => {
      const res = await fetch(`${baseUrl}/email/send`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: 'candidate@natassgrid.gov.in',
          subject: 'Your Verification Code',
          text: 'Your 6-digit verification code is 654321. Valid for 10 minutes.'
        })
      });
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.type, 'success');
      assert.strictEqual(body.recipient, 'candidate@natassgrid.gov.in');
      assert.strictEqual(body.otpCode, '654321');

      assert.strictEqual(testStore.emailOutbox.length, 1);
      assert.strictEqual(testStore.emailOutbox[0].to, 'candidate@natassgrid.gov.in');
      assert.strictEqual(testStore.emailOutbox[0].otpCode, '654321');
    });

    test('GET /mock/email/latest retrieves dispatched email for verification', async () => {
      await fetch(`${baseUrl}/email/send`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: 'recovery@example.com',
          subject: 'NAG OTP',
          otp: '887766'
        })
      });

      const inspectRes = await fetch(`${baseUrl}/mock/email/latest?email=recovery@example.com`);
      assert.strictEqual(inspectRes.status, 200);
      const inspectBody = await inspectRes.json();
      assert.strictEqual(inspectBody.success, true);
      assert.strictEqual(inspectBody.message.otpCode, '887766');
    });

    test('DELETE /mock/email/clear empties the email outbox', async () => {
      await fetch(`${baseUrl}/email/send`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ to: 'temp@example.com', otp: '112233' })
      });
      assert.strictEqual(testStore.emailOutbox.length, 1);

      const clearRes = await fetch(`${baseUrl}/mock/email/clear`, { method: 'DELETE' });
      assert.strictEqual(clearRes.status, 200);
      assert.strictEqual(testStore.emailOutbox.length, 0);
    });
  });

  // =========================================================================
  // 4. Aadhaar e-KYC & UIDAI 2.5 Auth Mock Tests
  // =========================================================================
  describe('Aadhaar e-KYC & UIDAI 2.5 Auth Mock Endpoints', () => {
    test('POST /aadhaar/v1/otp/generate triggers OTP generation with test hint 000000', async () => {
      const res = await fetch(`${baseUrl}/aadhaar/v1/otp/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ aadhaarNumber: '123456789012' })
      });
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.success, true);
      assert.strictEqual(body.mockOtpHint, '000000');
      assert.ok(body.txnId);
    });

    test('POST /aadhaar/v1/otp/generate returns 400 for invalid Aadhaar length', async () => {
      const res = await fetch(`${baseUrl}/aadhaar/v1/otp/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ aadhaarNumber: '123' })
      });
      assert.strictEqual(res.status, 400);
      const body = await res.json();
      assert.strictEqual(body.error, 'INVALID_AADHAAR_FORMAT');
    });

    test('POST /aadhaar/v1/otp/generate handles locked biometric persona (333333333333)', async () => {
      const res = await fetch(`${baseUrl}/aadhaar/v1/otp/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ aadhaarNumber: '333333333333' })
      });
      assert.strictEqual(res.status, 423);
      const body = await res.json();
      assert.strictEqual(body.error, 'BIOMETRIC_LOCKED');
      assert.strictEqual(body.code, 'K-200');
    });

    test('POST /aadhaar/v1/otp/verify validates OTP 000000 and returns full deterministic e-KYC', async () => {
      const genRes = await fetch(`${baseUrl}/aadhaar/v1/otp/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ aadhaarNumber: '123456789012' })
      });
      const genBody = await genRes.json();
      const txnId = genBody.txnId;

      const verifyRes = await fetch(`${baseUrl}/aadhaar/v1/otp/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          txnId,
          otp: '000000',
          aadhaarNumber: '123456789012'
        })
      });

      assert.strictEqual(verifyRes.status, 200);
      const verifyBody = await verifyRes.json();
      assert.strictEqual(verifyBody.success, true);
      assert.strictEqual(verifyBody.data.fullName, 'Aditya Sharma');
      assert.strictEqual(verifyBody.data.gender, 'M');
      assert.strictEqual(verifyBody.data.dob, '1998-05-15');
      assert.strictEqual(verifyBody.data.aadhaarLast4, '9012');
      assert.strictEqual(verifyBody.data.state, 'Uttar Pradesh');
      assert.ok(verifyBody.data.photoBase64.startsWith('data:image/svg+xml;base64,'));
    });

    test('POST /aadhaar/v2.5/auth performs demographic matching', async () => {
      const res = await fetch(`${baseUrl}/aadhaar/v2.5/auth`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          uid: '123456789012',
          authType: 'DEMO',
          demographic: { name: 'Aditya', gender: 'M' }
        })
      });
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.ret, 'Y');
      assert.strictEqual(body.authenticated, true);
    });

    test('POST /aadhaar/v2.5/kyc returns signed XML e-KYC response', async () => {
      const res = await fetch(`${baseUrl}/aadhaar/v2.5/kyc?format=xml`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/xml' },
        body: JSON.stringify({ uid: '123456789012', otp: '000000' })
      });
      assert.strictEqual(res.status, 200);
      assert.strictEqual(res.headers.get('content-type'), 'application/xml; charset=utf-8');
      const xml = await res.text();
      assert.ok(xml.includes('<KycRes'));
      assert.ok(xml.includes('<UidData'));
      assert.ok(xml.includes('<Signature'));
    });
  });

  // =========================================================================
  // 5. DigiLocker Mock Tests
  // =========================================================================
  describe('DigiLocker Mock & Authentication Endpoints', () => {
    test('GET /.well-known/openid-configuration returns DigiLocker OIDC discovery doc', async () => {
      const res = await fetch(`${baseUrl}/digilocker/.well-known/openid-configuration`);
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.ok(body.authorization_endpoint);
      assert.ok(body.token_endpoint);
      assert.ok(body.jwks_uri);
    });

    test('GET /digilocker/oauth/jwks.json returns mock signature keys', async () => {
      const res = await fetch(`${baseUrl}/digilocker/oauth/jwks.json`);
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.ok(Array.isArray(body.keys));
      assert.strictEqual(body.keys[0].kty, 'RSA');
    });

    test('GET /digilocker/oauth/authorize serves HTML consent UI for browsers', async () => {
      const res = await fetch(`${baseUrl}/digilocker/oauth/authorize?response_type=code&client_id=nag-portal&redirect_uri=http://localhost:4300/callback`, {
        headers: { 'Accept': 'text/html' }
      });
      assert.strictEqual(res.status, 200);
      const html = await res.text();
      assert.ok(html.includes('DigiLocker DPI Gateway'));
      assert.ok(html.includes('Consent for Profile & Document Verification'));
    });

    test('POST /digilocker/oauth/token with PKCE returns JWT id_token and access_token', async () => {
      const verifier = 'test_code_verifier_12345678901234567890';
      const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');

      const authRes = await fetch(`${baseUrl}/digilocker/oauth/authorize?response_type=code&client_id=nag-portal&code_challenge=${challenge}&auto_consent=true`);
      const authBody = await authRes.json();
      const code = authBody.code;

      const tokenRes = await fetch(`${baseUrl}/digilocker/oauth/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          grant_type: 'authorization_code',
          code,
          code_verifier: verifier,
          client_id: 'nag-portal'
        })
      });
      assert.strictEqual(tokenRes.status, 200);
      const tokenBody = await tokenRes.json();
      assert.ok(tokenBody.access_token);
      assert.ok(tokenBody.id_token);
      assert.strictEqual(tokenBody.token_type, 'Bearer');
      assert.ok(tokenBody.digilocker_id.startsWith('DL-IND-'));
    });

    test('GET /digilocker/oauth/userinfo returns mock citizen profile', async () => {
      const res = await fetch(`${baseUrl}/digilocker/oauth/userinfo`, {
        headers: { 'Authorization': 'Bearer dl_token_sample' }
      });
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.name, 'Aditya Sharma');
      assert.strictEqual(body.eaadhaar, 'Y');
      assert.strictEqual(body.authType, 'BEARER_TOKEN');
    });

    test('GET /digilocker/v1/user/documents returns list of pre-seeded test certificates', async () => {
      const res = await fetch(`${baseUrl}/digilocker/v1/user/documents`);
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.status, 'SUCCESS');
      assert.ok(body.documents.length >= 4);
      assert.ok(body.documents.some(d => d.docType === '10TH_MARKSHEET'));
      assert.ok(body.documents.some(d => d.docType === '12TH_MARKSHEET'));
    });

    test('GET /digilocker/v1/document/download/:docId returns valid PDF binary', async () => {
      const res = await fetch(`${baseUrl}/digilocker/v1/document/download/DOC-CBSE-10TH`);
      assert.strictEqual(res.status, 200);
      assert.strictEqual(res.headers.get('content-type'), 'application/pdf');
      const arrayBuffer = await res.arrayBuffer();
      const textPrefix = Buffer.from(arrayBuffer).toString('utf8', 0, 8);
      assert.ok(textPrefix.startsWith('%PDF-1.4'));
    });

    test('POST /digilocker/v1/verify returns SUCCESS status and document data', async () => {
      const res = await fetch(`${baseUrl}/digilocker/v1/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ docType: '10TH_MARKSHEET', token: 'mock-token' })
      });
      assert.strictEqual(res.status, 200);
      const body = await res.json();
      assert.strictEqual(body.status, 'SUCCESS');
      assert.strictEqual(body.issuerId, 'in.gov.cbse');
      assert.ok(body.documentData);
    });

    test('POST /digilocker/v1/credential/push stores pushed scorecard record', async () => {
      const payload = {
        candidateId: '018f4e2b-0050-7000-8000-000000000001',
        examId: 'EXAM-2026-NAG-01',
        pdfRef: 'https://s3.nag.gov.in/scorecards/018f4e2b.pdf',
        score: 184,
        percentile: 99.4
      };

      const res = await fetch(`${baseUrl}/digilocker/v1/credential/push`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      assert.strictEqual(res.status, 201);
      const body = await res.json();
      assert.strictEqual(body.status, 'SUCCESS');
      assert.ok(body.docId.startsWith('NAG-SCORECARD-'));

      assert.strictEqual(testStore.pushedScorecards.length, 1);
      assert.strictEqual(testStore.pushedScorecards[0].candidateId, payload.candidateId);
    });
  });

  // =========================================================================
  // 6. Chaos and Failure Injection Tests
  // =========================================================================
  describe('Chaos Injection Endpoints', () => {
    test('POST /mock/chaos simulates upstream 503 outage on DigiLocker', async () => {
      await fetch(`${baseUrl}/mock/chaos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ digilockerFailureStatus: 503 })
      });

      const res = await fetch(`${baseUrl}/digilocker/v1/user/documents`);
      assert.strictEqual(res.status, 503);

      // Reset
      await fetch(`${baseUrl}/mock/chaos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reset: true })
      });

      const resAfter = await fetch(`${baseUrl}/digilocker/v1/user/documents`);
      assert.strictEqual(resAfter.status, 200);
    });
  });
});
