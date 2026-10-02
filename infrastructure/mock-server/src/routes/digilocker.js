/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { Router } from 'express';
import crypto from 'node:crypto';
import { DIGILOCKER_DOCUMENTS, AADHAAR_PERSONAS, generateMockPdf, testStore } from '../data/mock-data.js';

const router = Router();

// In-memory OAuth2 authorization codes and tokens store
const authCodes = new Map();

// Middleware for simulated delay or failure
router.use((req, res, next) => {
  const { delayMs, digilockerFailureStatus } = testStore.chaosConfig;
  if (digilockerFailureStatus) {
    return res.status(digilockerFailureStatus).json({
      error: 'Simulated DigiLocker Upstream Outage',
      status: digilockerFailureStatus
    });
  }
  if (delayMs > 0) {
    setTimeout(next, delayMs);
  } else {
    next();
  }
});

/**
 * 1. OpenID Connect Discovery
 */
router.get(['/.well-known/openid-configuration', '/oauth/.well-known/openid-configuration'], (req, res) => {
  const host = req.get('host') || 'localhost:8099';
  const protocol = req.protocol || 'http';
  const baseUrl = `${protocol}://${host}/digilocker`;

  res.json({
    issuer: `${baseUrl}/oauth`,
    authorization_endpoint: `${baseUrl}/oauth/authorize`,
    token_endpoint: `${baseUrl}/oauth/token`,
    userinfo_endpoint: `${baseUrl}/oauth/userinfo`,
    jwks_uri: `${baseUrl}/oauth/jwks.json`,
    response_types_supported: ['code', 'token', 'id_token'],
    subject_types_supported: ['public'],
    id_token_signing_alg_values_supported: ['RS256', 'HS256'],
    scopes_supported: ['openid', 'profile', 'email', 'digilocker:read', 'digilocker:write']
  });
});

/**
 * 2. Mock JWKS (JSON Web Key Set)
 */
router.get(['/oauth/jwks.json', '/oauth/certs'], (req, res) => {
  res.json({
    keys: [
      {
        kty: 'RSA',
        use: 'sig',
        alg: 'RS256',
        kid: 'digilocker-mock-key-2026',
        n: 'uMockDigiLockerKeyPublicKeyModulusForLocalDPIVerification...',
        e: 'AQAB'
      }
    ]
  });
});

/**
 * 3. OAuth2 Authorization Endpoint (Supports Interactive Browser UI & Direct Redirect / JSON)
 */
router.get(['/oauth/authorize', '/public/oauth2/1/authorize'], (req, res) => {
  const responseType = req.query.response_type || 'code';
  const clientId = req.query.client_id || 'mock-client';
  const redirectUri = req.query.redirect_uri;
  const state = req.query.state || 'nag_oauth_state';
  const codeChallenge = req.query.code_challenge;
  const codeChallengeMethod = req.query.code_challenge_method || 'S256';
  const personaUid = req.query.persona || '123456789012';
  const autoConsent = req.query.auto_consent === 'true' || !req.headers.accept?.includes('text/html');

  const code = 'dl_auth_code_' + crypto.randomBytes(12).toString('hex');
  const persona = AADHAAR_PERSONAS[personaUid] || AADHAAR_PERSONAS['123456789012'];

  authCodes.set(code, {
    code,
    clientId,
    redirectUri,
    codeChallenge,
    codeChallengeMethod,
    persona,
    expiresAt: Date.now() + 10 * 60 * 1000
  });

  // If client wants JSON or automated redirect
  if (autoConsent && redirectUri) {
    const url = new URL(redirectUri);
    url.searchParams.set('code', code);
    if (state) url.searchParams.set('state', state);
    return res.redirect(url.toString());
  }

  if (autoConsent) {
    return res.json({
      status: 'AUTHORIZED',
      code,
      state,
      clientId,
      persona: persona.fullName,
      expiresIn: 600
    });
  }

  // Serve authentic DigiLocker Consent Web Screen for Interactive / E2E Playwright sessions
  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>DigiLocker DPI — Candidate Identity Consent</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f0f4f8; margin: 0; padding: 40px 20px; display: flex; justify-content: center; }
    .card { background: white; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); width: 100%; max-width: 480px; padding: 32px; box-sizing: border-box; }
    .logo-badge { background: #004085; color: white; padding: 6px 14px; border-radius: 6px; font-weight: 700; font-size: 14px; display: inline-block; margin-bottom: 20px; }
    h2 { color: #1e293b; margin: 0 0 8px 0; font-size: 22px; }
    p { color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 20px 0; }
    .consent-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 24px; font-size: 13px; color: #334155; }
    .consent-box ul { margin: 8px 0 0 20px; padding: 0; }
    .persona-select { width: 100%; padding: 10px; border-radius: 6px; border: 1px solid #cbd5e1; margin-bottom: 20px; font-size: 14px; }
    .btn-group { display: flex; gap: 12px; }
    button { flex: 1; padding: 12px; font-size: 15px; font-weight: 600; border-radius: 6px; cursor: pointer; border: none; }
    .btn-allow { background: #0284c7; color: white; }
    .btn-allow:hover { background: #0369a1; }
    .btn-deny { background: #e2e8f0; color: #475569; }
    .btn-deny:hover { background: #cbd5e1; }
  </style>
</head>
<body>
  <div class="card">
    <div class="logo-badge">🏛️ DigiLocker DPI Gateway</div>
    <h2>Consent for Profile & Document Verification</h2>
    <p>National Assessment Grid (NAG) is requesting verified identity and document access for examination enrollment.</p>

    <div class="consent-box">
      <strong>Permissions requested:</strong>
      <ul>
        <li>Read full resident identity & demographic profile</li>
        <li>Access verified 10th/12th marksheets & academic degree</li>
        <li>Anchor exam scorecards directly to your DigiLocker</li>
      </ul>
    </div>

    <form method="POST" action="/digilocker/oauth/consent">
      <input type="hidden" name="code" value="${code}">
      <input type="hidden" name="redirect_uri" value="${redirectUri || ''}">
      <input type="hidden" name="state" value="${state}">

      <label style="font-size: 13px; font-weight: 600; color: #475569; display: block; margin-bottom: 6px;">
        Select Mock Candidate Persona:
      </label>
      <select name="personaUid" class="persona-select">
        <option value="123456789012" selected>Aditya Sharma (General Category, Male - 123456789012)</option>
        <option value="111122223333">Priya Patel (Female Candidate - 111122223333)</option>
        <option value="777788889999">Rahul Verma (OBC/EWS Category - 777788889999)</option>
      </select>

      <div class="btn-group">
        <button type="submit" name="action" value="deny" class="btn-deny">Deny</button>
        <button type="submit" name="action" value="allow" class="btn-allow">Allow Access</button>
      </div>
    </form>
  </div>
</body>
</html>`);
});

/**
 * 4. OAuth2 Interactive Consent Form Submission
 */
router.post('/oauth/consent', (req, res) => {
  const { code, redirect_uri, state, action, personaUid } = req.body;
  const authRecord = authCodes.get(code);

  if (action === 'deny') {
    if (redirect_uri) {
      const url = new URL(redirect_uri);
      url.searchParams.set('error', 'access_denied');
      url.searchParams.set('error_description', 'The candidate denied consent in DigiLocker.');
      if (state) url.searchParams.set('state', state);
      return res.redirect(url.toString());
    }
    return res.status(403).json({ error: 'access_denied', message: 'User denied authorization' });
  }

  if (authRecord && personaUid) {
    authRecord.persona = AADHAAR_PERSONAS[personaUid] || authRecord.persona;
  }

  if (redirect_uri) {
    const url = new URL(redirect_uri);
    url.searchParams.set('code', code);
    if (state) url.searchParams.set('state', state);
    return res.redirect(url.toString());
  }

  res.json({
    status: 'AUTHORIZED',
    code,
    state,
    persona: authRecord?.persona?.fullName
  });
});

/**
 * 5. OAuth2 Token Endpoint (Supports Authorization Code with PKCE, Refresh Token, Client Credentials)
 */
router.post(['/oauth/token', '/public/oauth2/2/token', '/oauth2/2/token'], (req, res) => {
  const grantType = req.body?.grant_type || req.query?.grant_type || 'authorization_code';
  const code = req.body?.code || req.query?.code;
  const codeVerifier = req.body?.code_verifier || req.query?.code_verifier;
  const clientId = req.body?.client_id || req.headers['client_id'];

  let persona = AADHAAR_PERSONAS['123456789012'];

  if (grantType === 'authorization_code') {
    if (code && authCodes.has(code)) {
      const record = authCodes.get(code);
      persona = record.persona;

      // Validate PKCE code_verifier if code_challenge was used during authorization
      if (record.codeChallenge) {
        const computedChallenge = crypto.createHash('sha256').update(codeVerifier || '').digest('base64url');
        if (computedChallenge !== record.codeChallenge && codeVerifier !== record.codeChallenge) {
          return res.status(400).json({
            error: 'invalid_grant',
            error_description: 'PKCE code_verifier mismatch.'
          });
        }
      }
      authCodes.delete(code); // Single-use authorization code
    }
  }

  const accessToken = 'dl_token_' + crypto.randomBytes(24).toString('hex');
  const refreshToken = 'dl_refresh_' + crypto.randomBytes(24).toString('hex');

  // JWT-like payload simulation
  const idTokenHeader = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const idTokenPayload = Buffer.from(JSON.stringify({
    iss: 'http://localhost:8099/digilocker/oauth',
    sub: `DL-IND-${persona.aadhaarLast4}-2026`,
    aud: clientId || 'exam-platform',
    name: persona.fullName,
    gender: persona.gender,
    dob: persona.dob,
    email: persona.email,
    mobile: persona.maskedMobile,
    eaadhaar: 'Y',
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 3600
  })).toString('base64url');
  const mockSignature = crypto.createHmac('sha256', 'mock_dl_secret').update(`${idTokenHeader}.${idTokenPayload}`).digest('base64url');
  const idToken = `${idTokenHeader}.${idTokenPayload}.${mockSignature}`;

  res.json({
    access_token: accessToken,
    token_type: 'Bearer',
    expires_in: 3600,
    refresh_token: refreshToken,
    id_token: idToken,
    scope: 'openid profile email read:documents write:scorecard',
    digilocker_id: `DL-IND-${persona.aadhaarLast4}-2026`,
    issued_at: new Date().toISOString()
  });
});

/**
 * 6. User Info Endpoint
 */
router.get(['/oauth/userinfo', '/public/oauth2/1/user'], (req, res) => {
  const authHeader = req.headers.authorization || '';
  res.json({
    sub: 'DL-IND-9012-2026',
    digilocker_id: 'DL-IND-9012-2026',
    name: 'Aditya Sharma',
    dob: '1998-05-15',
    gender: 'M',
    mobile: '+919876543210',
    email: 'aditya.sharma@example.gov.in',
    eaadhaar: 'Y',
    issuer: 'in.gov.digilocker',
    authenticated: true,
    authType: authHeader.startsWith('Bearer') ? 'BEARER_TOKEN' : 'SESSION'
  });
});

/**
 * 7. List User Documents
 */
router.get(['/v1/user/documents', '/public/oauth2/1/xml/eaadhaar'], (req, res) => {
  const documents = Object.values(DIGILOCKER_DOCUMENTS).map(doc => ({
    docId: doc.docId,
    docType: doc.docType,
    name: doc.name,
    issuerId: doc.issuerId,
    issuerName: doc.issuerName,
    issueDate: doc.issueDate,
    status: doc.status,
    uri: doc.uri
  }));

  res.json({
    status: 'SUCCESS',
    count: documents.length,
    documents
  });
});

/**
 * 8. Download Document / Marksheet / Certificate (PDF or XML)
 */
router.get(['/v1/document/download/:docId', '/v1/file/:docId', '/public/oauth2/1/file/:docId'], (req, res) => {
  const docId = req.params.docId;
  const format = req.query.format || (req.headers.accept?.includes('application/pdf') ? 'pdf' : 'pdf');

  const doc = DIGILOCKER_DOCUMENTS[docId] || DIGILOCKER_DOCUMENTS['DOC-CBSE-10TH'];

  if (format === 'xml') {
    res.setHeader('Content-Type', 'application/xml');
    return res.send(doc.xmlData);
  }

  const pdfBuffer = generateMockPdf(doc.name, doc.docType, doc.candidateName, doc.regNo);
  res.setHeader('Content-Type', 'application/pdf');
  res.setHeader('Content-Disposition', `attachment; filename="${docId}.pdf"`);
  res.setHeader('Content-Length', pdfBuffer.length);
  res.send(pdfBuffer);
});

/**
 * 9. Document Verification API (Validates cryptographic signatures)
 */
router.post(['/v1/verify', '/api/v1/candidate/digilocker/verify'], (req, res) => {
  const { docType, token, userId } = req.body || {};
  const requestedType = (docType || 'AADHAAR').toUpperCase();

  const matchingDoc = Object.values(DIGILOCKER_DOCUMENTS).find(
    d => d.docType.toUpperCase() === requestedType
  ) || DIGILOCKER_DOCUMENTS['DOC-AADHAAR-CLAIM'];

  res.json({
    status: 'SUCCESS',
    documentData: Buffer.from(matchingDoc.xmlData).toString('base64'),
    issuerId: matchingDoc.issuerId,
    docType: matchingDoc.docType,
    verified: true,
    signatureTimestamp: new Date().toISOString(),
    message: 'DigiLocker document signature validated successfully against Root Trust Anchor.'
  });
});

/**
 * 10. Partner HMAC Signature Verification API
 */
router.post('/v1/hmac/verify', (req, res) => {
  const hmacHeader = req.headers['x-digilocker-hmac'] || req.body?.hmac;
  const payload = req.body?.payload || '';

  res.json({
    valid: true,
    algorithm: 'HMAC-SHA256',
    verifiedTimestamp: new Date().toISOString(),
    partnerId: req.headers['x-partner-id'] || 'NAG-DPI-ASSESSMENT'
  });
});

/**
 * 11. Scorecard / Credential Push API
 */
router.post(['/v1/credential/push', '/v1/documents/push'], (req, res) => {
  const { candidateId, pdfRef, examId, score, percentile } = req.body || {};
  const docId = 'NAG-SCORECARD-' + crypto.randomUUID();
  const uri = `in.gov.natassgrid-SCORECARD-${candidateId || 'anon'}`;

  const record = {
    docId,
    uri,
    candidateId,
    pdfRef,
    examId,
    score,
    percentile,
    pushedAt: new Date().toISOString(),
    status: 'ISSUED_IN_DIGILOCKER'
  };

  testStore.pushedScorecards.push(record);

  res.status(201).json({
    status: 'SUCCESS',
    message: 'Candidate examination scorecard successfully published and anchored to DigiLocker.',
    docId,
    uri,
    timestamp: record.pushedAt
  });
});

export default router;
