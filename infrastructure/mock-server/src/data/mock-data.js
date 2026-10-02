/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import crypto from 'node:crypto';

/**
 * Generates a minimal valid PDF byte buffer for mock document downloads.
 */
export function generateMockPdf(title, subject, candidateName, regNo) {
  const content = `BT /F1 18 Tf 50 750 Td (${title}) Tj ET\n` +
    `BT /F1 12 Tf 50 720 Td (Issued to: ${candidateName} | Reg/Roll: ${regNo}) Tj ET\n` +
    `BT /F1 12 Tf 50 700 Td (Document Subject: ${subject}) Tj ET\n` +
    `BT /F1 10 Tf 50 670 Td (DigiLocker Cryptographically Verified by National Assessment Grid) Tj ET\n` +
    `BT /F1 10 Tf 50 650 Td (Digital Signature: SHA256withRSA-${crypto.randomBytes(16).toString('hex')}) Tj ET\n`;

  const streamLength = Buffer.byteLength(content, 'utf8');

  const pdfString = `%PDF-1.4
1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj
4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
5 0 obj << /Length ${streamLength} >>
stream
${content}
endstream
endobj
xref
0 6
0000000000 65535 f
0000000009 00000 n
0000000058 00000 n
0000000115 00000 n
0000000244 00000 n
0000000318 00000 n
trailer << /Size 6 /Root 1 0 R >>
startxref
${400 + streamLength}
%%EOF`;

  return Buffer.from(pdfString, 'utf8');
}

/**
 * Pre-seeded Aadhaar resident personas for deterministic e-KYC test scenarios.
 */
export const AADHAAR_PERSONAS = {
  // Standard Default Verified Candidate
  '123456789012': {
    aadhaarNumber: '123456789012',
    aadhaarLast4: '9012',
    fullName: 'Aditya Sharma',
    dob: '1998-05-15',
    gender: 'M',
    careOf: 'S/O Rajesh Sharma',
    address: 'Flat 402, Block B, Green Heights, Sector 62, Noida',
    city: 'Noida',
    state: 'Uttar Pradesh',
    pincode: '201309',
    maskedMobile: 'XXXX-XXXX-1210',
    email: 'aditya.sharma@example.gov.in',
    status: 'ACTIVE',
    photoBase64: 'data:image/svg+xml;base64,' + Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="#2563eb"/><text x="50" y="55" fill="white" font-size="20" text-anchor="middle">AS</text></svg>').toString('base64')
  },
  // Female Candidate Profile
  '111122223333': {
    aadhaarNumber: '111122223333',
    aadhaarLast4: '3333',
    fullName: 'Priya Patel',
    dob: '2000-11-20',
    gender: 'F',
    careOf: 'D/O Kirit Patel',
    address: '14 Shantivan Society, Satellite Road, Ahmedabad',
    city: 'Ahmedabad',
    state: 'Gujarat',
    pincode: '380015',
    maskedMobile: 'XXXX-XXXX-3333',
    email: 'priya.patel@example.gov.in',
    status: 'ACTIVE',
    photoBase64: 'data:image/svg+xml;base64,' + Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="#db2777"/><text x="50" y="55" fill="white" font-size="20" text-anchor="middle">PP</text></svg>').toString('base64')
  },
  // Reserved / EWS Category Candidate
  '777788889999': {
    aadhaarNumber: '777788889999',
    aadhaarLast4: '9999',
    fullName: 'Rahul Verma',
    dob: '1999-03-10',
    gender: 'M',
    careOf: 'S/O Sunil Verma',
    address: 'House 88, Civil Lines, Prayagraj',
    city: 'Prayagraj',
    state: 'Uttar Pradesh',
    pincode: '211001',
    maskedMobile: 'XXXX-XXXX-9999',
    email: 'rahul.verma@example.gov.in',
    status: 'ACTIVE',
    photoBase64: 'data:image/svg+xml;base64,' + Buffer.from('<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="#059669"/><text x="50" y="55" fill="white" font-size="20" text-anchor="middle">RV</text></svg>').toString('base64')
  },
  // Error Scenario 1: Biometric Locked resident
  '333333333333': {
    aadhaarNumber: '333333333333',
    aadhaarLast4: '3333',
    fullName: 'Locked Biometrics Resident',
    status: 'BIOMETRIC_LOCKED',
    errorCode: 'K-200',
    errorMessage: 'Aadhaar resident biometrics / authentication temporarily locked by resident.'
  },
  // Error Scenario 2: Invalid / Deactivated UID
  '999999999999': {
    aadhaarNumber: '999999999999',
    aadhaarLast4: '9999',
    fullName: 'Unknown Citizen',
    status: 'DEACTIVATED',
    errorCode: 'K-100',
    errorMessage: 'Aadhaar number does not exist or has been suspended/deactivated.'
  }
};

/**
 * Pre-seeded DigiLocker document records.
 */
export const DIGILOCKER_DOCUMENTS = {
  'DOC-CBSE-10TH': {
    docId: 'DOC-CBSE-10TH',
    docType: '10TH_MARKSHEET',
    name: 'Class X Secondary School Marksheet & Passing Certificate',
    issuerId: 'in.gov.cbse',
    issuerName: 'Central Board of Secondary Education (CBSE)',
    issueDate: '2014-05-20',
    candidateName: 'Aditya Sharma',
    regNo: 'CBSE10-8492021',
    status: 'VERIFIED',
    uri: 'in.gov.cbse-SECMK-8492021',
    xmlData: `<Certificate xmlns="http://digilocker.gov.in/schema">
      <Issuer id="in.gov.cbse" name="Central Board of Secondary Education"/>
      <DocDetails type="10TH_MARKSHEET" number="CBSE10-8492021" year="2014"/>
      <Candidate name="Aditya Sharma" roll="8492021" dob="1998-05-15"/>
      <Result status="PASS" total="475" max="500" percentage="95.0"/>
    </Certificate>`
  },
  'DOC-CBSE-12TH': {
    docId: 'DOC-CBSE-12TH',
    docType: '12TH_MARKSHEET',
    name: 'Class XII Senior Secondary School Marksheet',
    issuerId: 'in.gov.cbse',
    issuerName: 'Central Board of Secondary Education (CBSE)',
    issueDate: '2016-05-22',
    candidateName: 'Aditya Sharma',
    regNo: 'CBSE12-9842104',
    status: 'VERIFIED',
    uri: 'in.gov.cbse-HSCMK-9842104',
    xmlData: `<Certificate xmlns="http://digilocker.gov.in/schema">
      <Issuer id="in.gov.cbse" name="Central Board of Secondary Education"/>
      <DocDetails type="12TH_MARKSHEET" number="CBSE12-9842104" year="2016"/>
      <Candidate name="Aditya Sharma" roll="9842104" dob="1998-05-15"/>
      <Result status="PASS" total="482" max="500" percentage="96.4"/>
    </Certificate>`
  },
  'DOC-DEGREE-BTECH': {
    docId: 'DOC-DEGREE-BTECH',
    docType: 'DEGREE_CERTIFICATE',
    name: 'Bachelor of Technology in Computer Science & Engineering Degree',
    issuerId: 'in.ac.iitd',
    issuerName: 'Indian Institute of Technology Delhi',
    issueDate: '2020-07-15',
    candidateName: 'Aditya Sharma',
    regNo: '2016CS10849',
    status: 'VERIFIED',
    uri: 'in.ac.iitd-DEG-2016CS10849',
    xmlData: `<Certificate xmlns="http://digilocker.gov.in/schema">
      <Issuer id="in.ac.iitd" name="IIT Delhi"/>
      <DocDetails type="DEGREE" number="2016CS10849" year="2020"/>
      <Candidate name="Aditya Sharma" cgpa="9.4" division="FIRST_CLASS_HONOURS"/>
    </Certificate>`
  },
  'DOC-CASTE-OBC': {
    docId: 'DOC-CASTE-OBC',
    docType: 'CASTE_CERTIFICATE',
    name: 'Other Backward Classes (OBC-NCL) Community Certificate',
    issuerId: 'in.gov.up.edistrict',
    issuerName: 'Revenue Department, Govt of Uttar Pradesh',
    issueDate: '2023-01-10',
    candidateName: 'Aditya Sharma',
    regNo: 'UP-EDIST-2023-9941',
    status: 'VERIFIED',
    uri: 'in.gov.up.edistrict-CASTE-2023-9941',
    xmlData: `<Certificate xmlns="http://digilocker.gov.in/schema">
      <Issuer id="in.gov.up.edistrict" name="Govt of UP"/>
      <DocDetails type="CASTE_CERTIFICATE" category="OBC_NCL" validUntil="2027-01-10"/>
      <Candidate name="Aditya Sharma" father="Rajesh Sharma"/>
    </Certificate>`
  },
  'DOC-AADHAAR-CLAIM': {
    docId: 'DOC-AADHAAR-CLAIM',
    docType: 'AADHAAR',
    name: 'UIDAI Aadhaar e-KYC Identity Document',
    issuerId: 'in.gov.uidai',
    issuerName: 'Unique Identification Authority of India',
    issueDate: '2024-01-01',
    candidateName: 'Aditya Sharma',
    regNo: 'UIDAI-XXXX-XXXX-9012',
    status: 'VERIFIED',
    uri: 'in.gov.uidai-EAA-9012',
    xmlData: `<Eaadhhar xmlns="http://uidai.gov.in/aadhaar">
      <UidData uid="XXXX-XXXX-9012">
        <Poi name="Aditya Sharma" dob="1998-05-15" gender="M"/>
        <Poa co="S/O Rajesh Sharma" street="Sector 62" dist="Gautam Buddha Nagar" state="Uttar Pradesh" pc="201309"/>
      </UidData>
    </Eaadhhar>`
  }
};

/**
 * In-memory store for SMS messages, Email messages, OTP tokens, and chaos configuration.
 */
export const testStore = {
  smsOutbox: [],
  emailOutbox: [],
  aadhaarTxns: new Map(),
  pushedScorecards: [],
  chaosConfig: {
    msg91FailureStatus: null,
    aadhaarFailureStatus: null,
    digilockerFailureStatus: null,
    emailFailureStatus: null,
    delayMs: 0
  },
  reset() {
    this.smsOutbox = [];
    this.emailOutbox = [];
    this.aadhaarTxns.clear();
    this.pushedScorecards = [];
    this.chaosConfig = {
      msg91FailureStatus: null,
      aadhaarFailureStatus: null,
      digilockerFailureStatus: null,
      emailFailureStatus: null,
      delayMs: 0
    };
  }
};
