/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { Router } from 'express';
import crypto from 'node:crypto';
import { AADHAAR_PERSONAS, testStore } from '../data/mock-data.js';

const router = Router();

// Middleware to inject chaos delay or simulated failure
router.use((req, res, next) => {
  const { delayMs, aadhaarFailureStatus } = testStore.chaosConfig;
  if (aadhaarFailureStatus) {
    return res.status(aadhaarFailureStatus).json({
      error: 'Simulated UIDAI Aadhaar Upstream Service Unavailable',
      status: aadhaarFailureStatus
    });
  }
  if (delayMs > 0) {
    setTimeout(next, delayMs);
  } else {
    next();
  }
});

/**
 * Helper to build UIDAI e-KYC 2.5 XML string
 */
function buildUidaiKycXml(persona, txnId) {
  const ts = new Date().toISOString();
  return `<?xml version="1.0" encoding="UTF-8"?>
<KycRes xmlns="http://www.uidai.gov.in/kyc/uid-kyc-response/1.0" ret="Y" ts="${ts}" txn="${txnId}" ttl="2026-10-02T23:59:59">
  <Rar>MOCK_RAR_TOKEN_${crypto.randomBytes(8).toString('hex')}</Rar>
  <UidData tkn="${persona.aadhaarLast4}">
    <Poi dob="${persona.dob}" gender="${persona.gender}" name="${persona.fullName}" e="${persona.email}" m="${persona.maskedMobile}"/>
    <Poa co="${persona.careOf}" street="DPI Cyber Gateway" loc="${persona.city}" dist="${persona.city}" state="${persona.state}" pc="${persona.pincode}" country="India"/>
    <Pht>${persona.photoBase64}</Pht>
  </UidData>
  <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
    <SignedInfo>
      <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
      <SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha256"/>
    </SignedInfo>
    <SignatureValue>MOCK_UIDAI_ROOT_CA_DIGITAL_SIGNATURE_${crypto.randomBytes(16).toString('hex')}</SignatureValue>
  </Signature>
</KycRes>`;
}

/**
 * 1. Generate Aadhaar OTP Endpoint
 * Accepts 12-digit Aadhaar UID and issues a deterministic mock OTP.
 */
router.post(['/v1/otp/generate', '/api/v1/aadhaar/generate-otp', '/generate-otp'], (req, res) => {
  const aadhaarNumber = (req.body?.aadhaarNumber || req.body?.uid || req.query?.aadhaarNumber || '').toString().trim().replace(/[\s-]/g, '');

  if (!aadhaarNumber || aadhaarNumber.length !== 12 || !/^\d{12}$/.test(aadhaarNumber)) {
    return res.status(400).json({
      success: false,
      error: 'INVALID_AADHAAR_FORMAT',
      message: 'Aadhaar number must be a valid 12-digit numeric sequence.'
    });
  }

  const persona = AADHAAR_PERSONAS[aadhaarNumber] || AADHAAR_PERSONAS['123456789012'];

  if (persona.status === 'BIOMETRIC_LOCKED') {
    return res.status(423).json({
      success: false,
      error: 'BIOMETRIC_LOCKED',
      code: persona.errorCode,
      message: persona.errorMessage
    });
  }

  if (persona.status === 'DEACTIVATED') {
    return res.status(404).json({
      success: false,
      error: 'UID_NOT_FOUND',
      code: persona.errorCode,
      message: persona.errorMessage
    });
  }

  const txnId = 'TXN-UIDAI-' + crypto.randomBytes(8).toString('hex');
  const otp = '000000'; // Default predictable OTP for automated E2E suites

  testStore.aadhaarTxns.set(txnId, {
    txnId,
    aadhaarNumber,
    persona,
    otp,
    createdAt: Date.now(),
    expiresAt: Date.now() + 10 * 60 * 1000 // 10 minutes
  });

  // Also record mock SMS into outbox
  testStore.smsOutbox.unshift({
    id: 'SMS-UIDAI-' + crypto.randomBytes(6).toString('hex'),
    channel: 'SMS',
    mobile: persona.maskedMobile || 'XXXX-XXXX-1210',
    sender: 'UIDAI',
    otpCode: otp,
    message: `Your UIDAI Aadhaar verification OTP for National Assessment Grid (NAG) is ${otp}. Valid for 10 minutes. Do not share.`,
    sentAt: new Date().toISOString(),
    status: 'DELIVERED'
  });

  res.json({
    success: true,
    txnId,
    status: 'OTP_SENT',
    message: `Aadhaar OTP sent successfully to mobile ending with ${persona.maskedMobile || 'XXXX-XXXX-1210'}`,
    maskedMobile: persona.maskedMobile || 'XXXX-XXXX-1210',
    mockOtpHint: '000000',
    expiresInSeconds: 600
  });
});

/**
 * 2. Verify Aadhaar OTP & Return Deterministic e-KYC Response
 */
router.post(['/v1/otp/verify', '/api/v1/aadhaar/verify-otp', '/verify-otp'], (req, res) => {
  const { txnId, otp, aadhaarNumber } = req.body || {};
  const cleanedUid = (aadhaarNumber || '').toString().trim().replace(/[\s-]/g, '');

  let txn = txnId ? testStore.aadhaarTxns.get(txnId) : null;
  let persona = txn ? txn.persona : (AADHAAR_PERSONAS[cleanedUid] || AADHAAR_PERSONAS['123456789012']);

  const providedOtp = (otp || '').toString().trim();

  // Accept static '000000' or matching transaction OTP
  const isValidOtp = providedOtp === '000000' || (txn && txn.otp === providedOtp);

  if (!isValidOtp) {
    return res.status(401).json({
      success: false,
      error: 'INVALID_OTP',
      code: 'K-500',
      message: 'The Aadhaar OTP entered is invalid or expired. Test E2E OTP is 000000.'
    });
  }

  const kycPayload = {
    verified: true,
    aadhaarNumber: persona.aadhaarNumber,
    aadhaarLast4: persona.aadhaarLast4,
    fullName: persona.fullName,
    dob: persona.dob,
    gender: persona.gender,
    careOf: persona.careOf,
    address: persona.address,
    city: persona.city,
    state: persona.state,
    pincode: persona.pincode,
    email: persona.email,
    photoBase64: persona.photoBase64,
    verificationTimestamp: new Date().toISOString(),
    digitalSignature: 'SHA256withRSA:MOCK_UIDAI_TRUST_ANCHOR_' + crypto.randomBytes(12).toString('hex')
  };

  res.json({
    success: true,
    status: 'KYC_VERIFIED',
    message: 'Aadhaar e-KYC authentication successful.',
    data: kycPayload
  });
});

/**
 * 3. UIDAI Authentication 2.5 Protocol Endpoint (/aadhaar/v2.5/auth)
 * Supports Demographic (DEMO), OTP (OTP), and Biometric (BIO/FACE) authentication modes.
 */
router.post(['/v2.5/auth', '/v1/auth', '/auth'], (req, res) => {
  const uid = (req.body?.uid || req.body?.aadhaarNumber || '').toString().trim().replace(/[\s-]/g, '');
  const authType = (req.body?.authType || req.body?.type || 'DEMO').toUpperCase();
  const txnId = req.body?.txn || 'TXN-' + crypto.randomBytes(8).toString('hex');
  const asXml = req.headers.accept?.includes('application/xml');

  const persona = AADHAAR_PERSONAS[uid] || AADHAAR_PERSONAS['123456789012'];

  if (persona.status === 'BIOMETRIC_LOCKED' && (authType === 'BIO' || authType === 'FACE')) {
    if (asXml) {
      res.setHeader('Content-Type', 'application/xml');
      return res.status(423).send(`<?xml version="1.0" encoding="UTF-8"?>
<AuthRes xmlns="http://www.uidai.gov.in/authentication/uid-auth-response/1.0" ret="n" err="K-200" txn="${txnId}" ts="${new Date().toISOString()}"/>`);
    }
    return res.status(423).json({ ret: 'N', err: 'K-200', message: 'Biometrics locked by resident.' });
  }

  let isMatch = true;
  let errorMsg = null;

  if (authType === 'DEMO') {
    const demo = req.body?.demographic || req.body?.demo || {};
    if (demo.name && !persona.fullName.toLowerCase().includes(demo.name.toLowerCase())) {
      isMatch = false;
      errorMsg = 'Demographic Name mismatch';
    }
    if (demo.gender && persona.gender.toUpperCase() !== demo.gender.toUpperCase()) {
      isMatch = false;
      errorMsg = 'Demographic Gender mismatch';
    }
  } else if (authType === 'OTP') {
    const otp = req.body?.otp || req.body?.otpValue;
    if (otp !== '000000') {
      isMatch = false;
      errorMsg = 'Invalid OTP value. Use deterministic test OTP 000000.';
    }
  } else if (authType === 'BIO' || authType === 'FACE') {
    // Biometric match simulation for candidate verification & exam entrance
    const matchScore = Number(req.body?.matchScore || 98.5);
    isMatch = matchScore >= 70.0;
  }

  const ts = new Date().toISOString();

  if (asXml) {
    res.setHeader('Content-Type', 'application/xml');
    return res.send(`<?xml version="1.0" encoding="UTF-8"?>
<AuthRes xmlns="http://www.uidai.gov.in/authentication/uid-auth-response/1.0" ret="${isMatch ? 'y' : 'n'}" code="${isMatch ? '00' : 'K-100'}" txn="${txnId}" ts="${ts}" info="MOCK-UIDAI-AUTH-2.5"/>`);
  }

  res.json({
    ret: isMatch ? 'Y' : 'N',
    code: isMatch ? '00' : 'K-100',
    txn: txnId,
    timestamp: ts,
    authType,
    authenticated: isMatch,
    message: isMatch ? 'UIDAI 2.5 Authentication Successful.' : (errorMsg || 'Authentication Failed.')
  });
});

/**
 * 4. UIDAI e-KYC 2.5 XML/JSON Protocol Endpoint (/aadhaar/v2.5/kyc)
 */
router.post(['/v2.5/kyc', '/v1/kyc', '/kyc'], (req, res) => {
  const uid = (req.body?.uid || req.body?.aadhaarNumber || '123456789012').toString().trim().replace(/[\s-]/g, '');
  const txnId = req.body?.txn || 'TXN-KYC-' + crypto.randomBytes(8).toString('hex');
  const otp = req.body?.otp;
  const persona = AADHAAR_PERSONAS[uid] || AADHAAR_PERSONAS['123456789012'];

  if (otp && otp !== '000000') {
    return res.status(401).json({ ret: 'N', err: 'K-500', message: 'Invalid e-KYC OTP' });
  }

  const asXml = req.headers.accept?.includes('application/xml') || req.query.format === 'xml';

  if (asXml) {
    res.setHeader('Content-Type', 'application/xml');
    return res.send(buildUidaiKycXml(persona, txnId));
  }

  res.json({
    ret: 'Y',
    txn: txnId,
    timestamp: new Date().toISOString(),
    xmlPayload: buildUidaiKycXml(persona, txnId),
    residentData: {
      aadhaarLast4: persona.aadhaarLast4,
      fullName: persona.fullName,
      dob: persona.dob,
      gender: persona.gender,
      careOf: persona.careOf,
      address: persona.address,
      city: persona.city,
      state: persona.state,
      pincode: persona.pincode,
      email: persona.email,
      maskedMobile: persona.maskedMobile,
      photoBase64: persona.photoBase64
    }
  });
});

/**
 * 5. Personas Listing Helper
 */
router.get(['/v1/personas', '/personas'], (req, res) => {
  res.json({
    personas: Object.values(AADHAAR_PERSONAS).map(p => ({
      aadhaarNumber: p.aadhaarNumber,
      fullName: p.fullName,
      gender: p.gender,
      dob: p.dob,
      state: p.state,
      status: p.status,
      description: p.errorMessage || 'Standard active verified resident'
    }))
  });
});

export default router;
