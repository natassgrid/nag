/// <reference path="../../../../../nag-frontend-workspace/node_modules/@types/jest/index.d.ts" />
/// <reference path="../../../../../nag-frontend-workspace/node_modules/@types/node/index.d.ts" />

/**
 * Unit Tests for @nag/candidate-core
 * Covers: Validation Schemas, NTA State Machine, Local Grading Engine,
 * Cryptographic Offline QR Verifier, and API Client Lifecycle.
 */

import {
  ApiClientError,
  DigitalAdmitCard,
  ExamItem,
  InMemoryTokenStorage,
  NagCandidateApiClient,
  NtaCbtStateMachine,
  computeCanonicalAdmitCardDigest,
  encodeAdmitCardQrPayload,
  generateAdmitCardSignature,
  gradeExamLocally,
  isAttemptedAnswer,
  parseAdmitCardQrPayload,
  sha256Hex,
  validateExamApplication,
  validateMobileOtp,
  validateRegistrationPayload,
  validateTotpCode,
  verifyOfflineAdmitCardQr,
} from '../index';

describe('@nag/candidate-core Shared Domain Library', () => {
  // ==========================================================================
  // 1. Validation Schemas Tests
  // ==========================================================================
  describe('Validation Schemas', () => {
    describe('validateRegistrationPayload', () => {
      const validPayload = {
        fullName: 'Aarav Sharma',
        mobileNumber: '9876543210',
        email: 'aarav.sharma@example.gov.in',
        password: 'Pass@word123',
        dpdpConsentAccepted: true,
        category: 'GEN',
      };

      it('should succeed with valid registration payload', () => {
        const result = validateRegistrationPayload(validPayload);
        expect(result.success).toBe(true);
        expect(result.data?.fullName).toBe('Aarav Sharma');
        expect(result.data?.mobileNumber).toBe('9876543210');
        expect(result.errors).toEqual({});
      });

      it('should fail when fullName is too short or empty', () => {
        const resEmpty = validateRegistrationPayload({ ...validPayload, fullName: ' ' });
        expect(resEmpty.success).toBe(false);
        expect(resEmpty.errors.fullName).toBeDefined();

        const resShort = validateRegistrationPayload({ ...validPayload, fullName: 'A' });
        expect(resShort.success).toBe(false);
        expect(resShort.errors.fullName).toBeDefined();
      });

      it('should fail with invalid Indian mobile numbers', () => {
        // Starts with 5 (invalid Indian mobile)
        const res5 = validateRegistrationPayload({ ...validPayload, mobileNumber: '5876543210' });
        expect(res5.success).toBe(false);
        expect(res5.errors.mobileNumber).toBeDefined();

        // 9 digits only
        const res9 = validateRegistrationPayload({ ...validPayload, mobileNumber: '987654321' });
        expect(res9.success).toBe(false);

        // Alpha characters
        const resAlpha = validateRegistrationPayload({ ...validPayload, mobileNumber: '987654321A' });
        expect(resAlpha.success).toBe(false);
      });

      it('should fail with invalid email', () => {
        const res = validateRegistrationPayload({ ...validPayload, email: 'invalid-email' });
        expect(res.success).toBe(false);
        expect(res.errors.email).toBeDefined();
      });

      it('should enforce password complexity (uppercase, lowercase, number, special char, min 8)', () => {
        // Short
        expect(validateRegistrationPayload({ ...validPayload, password: 'Pass1!' }).success).toBe(false);
        // Missing uppercase
        expect(validateRegistrationPayload({ ...validPayload, password: 'password123!' }).success).toBe(false);
        // Missing lowercase
        expect(validateRegistrationPayload({ ...validPayload, password: 'PASSWORD123!' }).success).toBe(false);
        // Missing digit
        expect(validateRegistrationPayload({ ...validPayload, password: 'Password!@#' }).success).toBe(false);
        // Missing special char
        expect(validateRegistrationPayload({ ...validPayload, password: 'Password1234' }).success).toBe(false);
      });

      it('should fail if DPDP consent is not accepted', () => {
        const res = validateRegistrationPayload({ ...validPayload, dpdpConsentAccepted: false });
        expect(res.success).toBe(false);
        expect(res.errors.dpdpConsentAccepted).toBeDefined();
      });

      it('should fail with non-object payload', () => {
        expect(validateRegistrationPayload(null).success).toBe(false);
        expect(validateRegistrationPayload('test').success).toBe(false);
      });
    });

    describe('validateMobileOtp', () => {
      it('should succeed with valid 10-digit mobile and 6-digit OTP', () => {
        const res = validateMobileOtp('9876543210', '123456');
        expect(res.success).toBe(true);
        expect(res.data?.otp).toBe('123456');
      });

      it('should fail with non-6-digit or non-numeric OTP', () => {
        expect(validateMobileOtp('9876543210', '12345').success).toBe(false);
        expect(validateMobileOtp('9876543210', '1234567').success).toBe(false);
        expect(validateMobileOtp('9876543210', '12a456').success).toBe(false);
      });
    });

    describe('validateTotpCode', () => {
      it('should succeed with valid 6-digit string or object', () => {
        expect(validateTotpCode('654321').success).toBe(true);
        expect(validateTotpCode({ totpCode: '654321' }).success).toBe(true);
      });

      it('should fail with invalid TOTP codes', () => {
        expect(validateTotpCode('65432').success).toBe(false);
        expect(validateTotpCode('6543210').success).toBe(false);
        expect(validateTotpCode('').success).toBe(false);
      });
    });

    describe('validateExamApplication (3 Distinct Centres Rule & PwD)', () => {
      const validApp = {
        examId: 'EXAM-2026-UPSC-CS',
        firstChoiceCentreId: 'CENTRE-DELHI-01',
        secondChoiceCentreId: 'CENTRE-MUMBAI-02',
        thirdChoiceCentreId: 'CENTRE-KOLKATA-03',
        preferredShiftId: 'SHIFT-MORNING',
        instructionsAccepted: true,
        pwdRequired: false,
      };

      it('should pass with 3 distinct centres and valid fields', () => {
        const res = validateExamApplication(validApp);
        expect(res.success).toBe(true);
        expect(res.data?.firstChoiceCentreId).toBe('CENTRE-DELHI-01');
      });

      it('should reject applications when any of the 3 centre choices are duplicate', () => {
        // First and second duplicate
        const dup1And2 = validateExamApplication({
          ...validApp,
          secondChoiceCentreId: 'CENTRE-DELHI-01',
        });
        expect(dup1And2.success).toBe(false);
        expect(dup1And2.errors.centreChoices).toBeDefined();

        // Second and third duplicate
        const dup2And3 = validateExamApplication({
          ...validApp,
          thirdChoiceCentreId: 'CENTRE-MUMBAI-02',
        });
        expect(dup2And3.success).toBe(false);
        expect(dup2And3.errors.centreChoices).toBeDefined();

        // First and third duplicate
        const dup1And3 = validateExamApplication({
          ...validApp,
          thirdChoiceCentreId: 'CENTRE-DELHI-01',
        });
        expect(dup1And3.success).toBe(false);
        expect(dup1And3.errors.centreChoices).toBeDefined();
      });

      it('should reject application without instructions accepted', () => {
        const res = validateExamApplication({ ...validApp, instructionsAccepted: false });
        expect(res.success).toBe(false);
        expect(res.errors.instructionsAccepted).toBeDefined();
      });

      it('should enforce PwD category when pwdRequired is true', () => {
        const resNoCat = validateExamApplication({
          ...validApp,
          pwdRequired: true,
        });
        expect(resNoCat.success).toBe(false);
        expect(resNoCat.errors.pwdCategory).toBeDefined();

        const resWithCat = validateExamApplication({
          ...validApp,
          pwdRequired: true,
          pwdCategory: 'Visually Impaired',
        });
        expect(resWithCat.success).toBe(true);
      });

      it('should enforce scribe qualification when scribe is requested for PwD', () => {
        const resNoScribeQual = validateExamApplication({
          ...validApp,
          pwdRequired: true,
          pwdCategory: 'Locomotor Disability',
          scribeRequired: true,
        });
        expect(resNoScribeQual.success).toBe(false);
        expect(resNoScribeQual.errors.scribeQualification).toBeDefined();

        const resWithScribeQual = validateExamApplication({
          ...validApp,
          pwdRequired: true,
          pwdCategory: 'Locomotor Disability',
          scribeRequired: true,
          scribeQualification: 'Graduate in Arts',
        });
        expect(resWithScribeQual.success).toBe(true);
      });

      it('should reject scribe request if candidate does not require PwD accommodation', () => {
        const res = validateExamApplication({
          ...validApp,
          pwdRequired: false,
          scribeRequired: true,
        });
        expect(res.success).toBe(false);
        expect(res.errors.scribeRequired).toBeDefined();
      });
    });
  });

  // ==========================================================================
  // 2. Cryptographic QR Code Verifier Tests
  // ==========================================================================
  describe('Cryptographic QR Code & Offline Admit Card Verifier', () => {
    const mockCard: DigitalAdmitCard = {
      hallTicketNumber: 'HT-2026-102948',
      candidateId: 'CAND-99881',
      candidateName: 'Priya Patel',
      examId: 'EXAM-NTA-JEE-2026',
      examTitle: 'Joint Entrance Examination Main 2026',
      examCode: 'JEE-MAIN',
      shiftId: 'SHIFT-01',
      shiftName: 'Morning Session',
      examDate: '2026-11-15',
      reportingTime: '07:30',
      gateClosingTime: '08:30',
      examStartTime: '09:00',
      examEndTime: '12:00',
      centreId: 'CENTRE-AHMEDABAD-04',
      centreName: 'National Test Centre Lab 4',
      centreAddress: 'Ring Road, Satellite',
      centreCity: 'Ahmedabad',
      centreState: 'Gujarat',
      centrePincode: '380015',
      instructions: ['Bring original Aadhaar card', 'No electronic devices permitted'],
      issuedAt: 1763190000000,
      expiresAt: 1763290000000,
    };

    const authorityKeyId = 'NTA-KMS-2026-PRIMARY';
    const authoritySecretKey = 'mock-secret-nta-key-2026';
    const trustedKeys = { [authorityKeyId]: authoritySecretKey };

    it('should calculate deterministic SHA-256 digest', () => {
      const digest1 = sha256Hex('NAG-CANDIDATE-CORE-TEST');
      const digest2 = sha256Hex('NAG-CANDIDATE-CORE-TEST');
      expect(digest1).toBe(digest2);
      expect(digest1).toHaveLength(64);
    });

    it('should encode and successfully verify valid offline Admit Card QR payload', () => {
      const unsignedDigest = computeCanonicalAdmitCardDigest({
        version: '1.0',
        keyId: authorityKeyId,
        algorithm: 'ECDSA_P256',
        hallTicketNumber: mockCard.hallTicketNumber,
        candidateId: mockCard.candidateId,
        candidateName: mockCard.candidateName,
        examId: mockCard.examId,
        shiftId: mockCard.shiftId,
        centreId: mockCard.centreId,
        issuedAt: Number(mockCard.issuedAt),
        expiresAt: Number(mockCard.expiresAt),
      });

      const signature = generateAdmitCardSignature(unsignedDigest, authoritySecretKey, 'ECDSA_P256');
      const rawQrData = encodeAdmitCardQrPayload(mockCard, signature, 'ECDSA_P256', authorityKeyId);

      const parsed = parseAdmitCardQrPayload(rawQrData);
      expect(parsed).not.toBeNull();
      expect(parsed?.hallTicketNumber).toBe('HT-2026-102948');

      // Verify with current timestamp before expiration
      const checkTimestamp = 1763200000000;
      const result = verifyOfflineAdmitCardQr(rawQrData, trustedKeys, checkTimestamp);

      expect(result.valid).toBe(true);
      expect(result.code).toBe('VALID');
      expect(result.payload?.hallTicketNumber).toBe('HT-2026-102948');
    });

    it('should fail with EXPIRED when current timestamp exceeds expiresAt', () => {
      const unsignedDigest = computeCanonicalAdmitCardDigest({
        version: '1.0',
        keyId: authorityKeyId,
        algorithm: 'ECDSA_P256',
        hallTicketNumber: mockCard.hallTicketNumber,
        candidateId: mockCard.candidateId,
        candidateName: mockCard.candidateName,
        examId: mockCard.examId,
        shiftId: mockCard.shiftId,
        centreId: mockCard.centreId,
        issuedAt: Number(mockCard.issuedAt),
        expiresAt: Number(mockCard.expiresAt),
      });

      const signature = generateAdmitCardSignature(unsignedDigest, authoritySecretKey);
      const rawQrData = encodeAdmitCardQrPayload(mockCard, signature, 'ECDSA_P256', authorityKeyId);

      // Verify at timestamp after expiresAt
      const lateTimestamp = 1763300000000;
      const result = verifyOfflineAdmitCardQr(rawQrData, trustedKeys, lateTimestamp);

      expect(result.valid).toBe(false);
      expect(result.code).toBe('EXPIRED');
    });

    it('should detect tampering and return INVALID_DIGEST', () => {
      const unsignedDigest = computeCanonicalAdmitCardDigest({
        version: '1.0',
        keyId: authorityKeyId,
        algorithm: 'ECDSA_P256',
        hallTicketNumber: mockCard.hallTicketNumber,
        candidateId: mockCard.candidateId,
        candidateName: mockCard.candidateName,
        examId: mockCard.examId,
        shiftId: mockCard.shiftId,
        centreId: mockCard.centreId,
        issuedAt: Number(mockCard.issuedAt),
        expiresAt: Number(mockCard.expiresAt),
      });

      const signature = generateAdmitCardSignature(unsignedDigest, authoritySecretKey);
      const rawQrData = encodeAdmitCardQrPayload(mockCard, signature, 'ECDSA_P256', authorityKeyId);

      // Tamper with candidateName in the JSON payload
      const tamperedObj = JSON.parse(rawQrData);
      tamperedObj.candidateName = 'Imposter Student';
      const tamperedRawQr = JSON.stringify(tamperedObj);

      const result = verifyOfflineAdmitCardQr(tamperedRawQr, trustedKeys, 1763200000000);
      expect(result.valid).toBe(false);
      expect(result.code).toBe('INVALID_DIGEST');
    });

    it('should reject untrusted keyId with UNTRUSTED_KEY', () => {
      const signature = 'sig_dummy';
      const rawQrData = encodeAdmitCardQrPayload(mockCard, signature, 'ECDSA_P256', 'ROGUE-KEY-99');

      const result = verifyOfflineAdmitCardQr(rawQrData, trustedKeys, 1763200000000);
      expect(result.valid).toBe(false);
      expect(result.code).toBe('UNTRUSTED_KEY');
    });

    it('should reject invalid hall ticket format with FORMAT_MISMATCH', () => {
      const invalidHtCard = { ...mockCard, hallTicketNumber: 'INVALID-HT-123' };
      const rawQrData = encodeAdmitCardQrPayload(invalidHtCard, 'sig_dummy', 'ECDSA_P256', authorityKeyId);

      const result = verifyOfflineAdmitCardQr(rawQrData, trustedKeys, 1763200000000);
      expect(result.valid).toBe(false);
      expect(result.code).toBe('FORMAT_MISMATCH');
    });

    it('should return MALFORMED for non-JSON or missing fields', () => {
      expect(verifyOfflineAdmitCardQr('not-json').code).toBe('MALFORMED');
      expect(verifyOfflineAdmitCardQr(JSON.stringify({ a: 1 })).code).toBe('MALFORMED');
    });
  });

  // ==========================================================================
  // 3. NTA 5-Status CBT State Machine Tests
  // ==========================================================================
  describe('NTA 5-Status CBT State Machine', () => {
    const mockItems: ExamItem[] = [
      {
        id: 'Q1',
        sectionId: 'SEC_PHYSICS',
        questionNumber: 1,
        topic: 'Kinematics',
        content: 'What is the acceleration due to gravity? $$\\approx 9.8\\text{ m/s}^2$$',
        options: [
          { id: 'A', text: '$$9.8\\text{ m/s}^2$$', isCorrect: true },
          { id: 'B', text: '$$12.5\\text{ m/s}^2$$', isCorrect: false },
          { id: 'C', text: '$$5.4\\text{ m/s}^2$$', isCorrect: false },
          { id: 'D', text: '$$0\\text{ m/s}^2$$', isCorrect: false },
        ],
        marks: 4,
        negativeMarks: 1,
      },
      {
        id: 'Q2',
        sectionId: 'SEC_PHYSICS',
        questionNumber: 2,
        topic: 'Optics',
        content: 'Focal length of a plane mirror is:',
        options: [
          { id: 'A', text: 'Zero', isCorrect: false },
          { id: 'B', text: 'Infinity', isCorrect: true },
          { id: 'C', text: '1 metre', isCorrect: false },
          { id: 'D', text: 'Negative', isCorrect: false },
        ],
        marks: 4,
        negativeMarks: 1,
      },
      {
        id: 'Q3',
        sectionId: 'SEC_CHEMISTRY',
        questionNumber: 3,
        topic: 'Equilibrium',
        content: 'Calculate pH of neutral water at $$25^\\circ\\text{C}$$.',
        options: [
          { id: 'A', text: '7', isCorrect: true },
          { id: 'B', text: '14', isCorrect: false },
          { id: 'C', text: '0', isCorrect: false },
          { id: 'D', text: '1', isCorrect: false },
        ],
        marks: 4,
        negativeMarks: 1,
      },
    ];

    const sessionMeta = {
      sessionId: 'SESSION-TEST-001',
      examId: 'EXAM-JEE-2026',
      candidateId: 'CAND-001',
    };

    it('should initialize all questions as NOT_VISITED except the first selected question (NOT_ANSWERED)', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);
      const palette = sm.getPaletteSummary();

      // Q1 is automatically selected on init, transitioning to NOT_ANSWERED (Red)
      expect(palette.notAnsweredCount).toBe(1);
      expect(palette.notVisitedCount).toBe(2);
      expect(palette.answeredCount).toBe(0);
      expect(palette.markedForReviewCount).toBe(0);
      expect(palette.answeredAndMarkedForReviewCount).toBe(0);
      expect(palette.totalQuestions).toBe(3);
    });

    it('should transition to ANSWERED (Green) when an option is selected and saveAndNext is called', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);

      sm.selectOption('A');
      const { current, next } = sm.saveAndNext();

      expect(current?.status).toBe('ANSWERED');
      expect(current?.selectedOptionId).toBe('A');
      expect(next?.id).toBe('Q2');

      // Next question (Q2) visited -> NOT_ANSWERED
      const q2State = sm.getQuestionState('Q2');
      expect(q2State?.status).toBe('NOT_ANSWERED');

      const palette = sm.getPaletteSummary();
      expect(palette.answeredCount).toBe(1);
      expect(palette.notAnsweredCount).toBe(1);
      expect(palette.notVisitedCount).toBe(1);
    });

    it('should remain NOT_ANSWERED (Red) when saveAndNext is called without selecting an option', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);

      const { current } = sm.saveAndNext();
      expect(current?.status).toBe('NOT_ANSWERED');
      expect(current?.selectedOptionId).toBeNull();
    });

    it('should transition to MARKED_FOR_REVIEW (Amber) when markForReviewAndNext is called without an option', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);

      const { current } = sm.markForReviewAndNext();
      expect(current?.status).toBe('MARKED_FOR_REVIEW');
      expect(current?.reviewFlag).toBe(true);
      expect(current?.selectedOptionId).toBeNull();
    });

    it('should transition to ANSWERED_AND_MARKED_FOR_REVIEW (Purple) when markForReviewAndNext is called WITH an option', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);

      sm.selectOption('B');
      const { current } = sm.markForReviewAndNext();

      expect(current?.status).toBe('ANSWERED_AND_MARKED_FOR_REVIEW');
      expect(current?.reviewFlag).toBe(true);
      expect(current?.selectedOptionId).toBe('B');

      const palette = sm.getPaletteSummary();
      expect(palette.answeredAndMarkedForReviewCount).toBe(1);
    });

    it('should clear response and revert to NOT_ANSWERED (Red) on clearResponse', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);

      sm.selectOption('A');
      sm.saveAndNext(); // Q1 is answered, now on Q2

      // Navigate back to Q1
      sm.selectQuestion('Q1');
      expect(sm.getQuestionState('Q1')?.status).toBe('ANSWERED');

      const cleared = sm.clearResponse();
      expect(cleared?.status).toBe('NOT_ANSWERED');
      expect(cleared?.selectedOptionId).toBeNull();
      expect(cleared?.reviewFlag).toBe(false);

      const palette = sm.getPaletteSummary();
      expect(palette.answeredCount).toBe(0);
      expect(palette.notAnsweredCount).toBe(2); // Q1 and Q2 are visited
    });

    it('should filter questions by section', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);
      const physicsQuestions = sm.getQuestionsForSection('SEC_PHYSICS');
      expect(physicsQuestions).toHaveLength(2);
      const chemQuestions = sm.getQuestionsForSection('SEC_CHEMISTRY');
      expect(chemQuestions).toHaveLength(1);
    });

    it('should produce monotonically incrementing offline packets with valid clientChecksum', () => {
      const sm = new NtaCbtStateMachine(mockItems, sessionMeta);

      const packet1 = sm.createOfflinePacket();
      expect(packet1.revision).toBe(1);
      expect(packet1.clientChecksum).toHaveLength(64);

      sm.selectOption('A');
      sm.saveAndNext();

      const packet2 = sm.createOfflinePacket();
      expect(packet2.revision).toBe(2);
      expect(packet2.revision).toBeGreaterThan(packet1.revision);
    });
  });

  // ==========================================================================
  // 4. Local Grading & Analytics Engine Tests
  // ==========================================================================
  describe('Local Grading & Analytics Engine', () => {
    const items: ExamItem[] = [
      {
        id: 'Q1',
        sectionId: 'PHYSICS',
        questionNumber: 1,
        topic: 'Mechanics',
        content: 'Question 1',
        options: [{ id: 'A', text: 'Opt A', isCorrect: true }, { id: 'B', text: 'Opt B' }],
        marks: 4,
        negativeMarks: 1,
      },
      {
        id: 'Q2',
        sectionId: 'PHYSICS',
        questionNumber: 2,
        topic: 'Mechanics',
        content: 'Question 2',
        options: [{ id: 'A', text: 'Opt A' }, { id: 'B', text: 'Opt B', isCorrect: true }],
        marks: 4,
        negativeMarks: 1,
      },
      {
        id: 'Q3',
        sectionId: 'CHEMISTRY',
        questionNumber: 3,
        topic: 'Organic',
        content: 'Question 3',
        options: [{ id: 'A', text: 'Opt A', isCorrect: true }, { id: 'B', text: 'Opt B' }],
        marks: 4,
        negativeMarks: 1,
      },
      {
        id: 'Q4',
        sectionId: 'CHEMISTRY',
        questionNumber: 4,
        topic: 'Inorganic',
        content: 'Question 4',
        options: [{ id: 'A', text: 'Opt A', isCorrect: true }, { id: 'B', text: 'Opt B' }],
        marks: 4,
        negativeMarks: 1,
      },
    ];

    const sessionMeta = {
      sessionId: 'SES-GRADE-01',
      examId: 'EXAM-01',
      candidateId: 'CAND-01',
    };

    it('should correctly evaluate attempted and unattempted responses including negative marking', () => {
      const states = {
        // Q1: ANSWERED correctly (+4)
        Q1: {
          questionId: 'Q1',
          status: 'ANSWERED' as const,
          selectedOptionId: 'A',
          timeSpentMs: 30000,
          reviewFlag: false,
        },
        // Q2: ANSWERED incorrectly (-1)
        Q2: {
          questionId: 'Q2',
          status: 'ANSWERED' as const,
          selectedOptionId: 'A', // Correct is B
          timeSpentMs: 25000,
          reviewFlag: false,
        },
        // Q3: ANSWERED_AND_MARKED_FOR_REVIEW correctly (+4) - must be evaluated in NTA standard!
        Q3: {
          questionId: 'Q3',
          status: 'ANSWERED_AND_MARKED_FOR_REVIEW' as const,
          selectedOptionId: 'A',
          timeSpentMs: 40000,
          reviewFlag: true,
        },
        // Q4: MARKED_FOR_REVIEW without selection (0 marks, no negative penalty)
        Q4: {
          questionId: 'Q4',
          status: 'MARKED_FOR_REVIEW' as const,
          selectedOptionId: null,
          timeSpentMs: 15000,
          reviewFlag: true,
        },
      };

      const breakdown = gradeExamLocally(items, states, sessionMeta);

      expect(breakdown.totalQuestions).toBe(4);
      expect(breakdown.totalAttempted).toBe(3); // Q1, Q2, Q3
      expect(breakdown.totalCorrect).toBe(2); // Q1 (+4), Q3 (+4)
      expect(breakdown.totalIncorrect).toBe(1); // Q2 (-1)
      expect(breakdown.totalUnattempted).toBe(1); // Q4 (0)

      // Raw score = 4 - 1 + 4 + 0 = 7
      expect(breakdown.rawScore).toBe(7);
      expect(breakdown.maxMarks).toBe(16); // 4 * 4
      expect(breakdown.accuracyPercentage).toBeCloseTo((2 / 3) * 100, 2);

      // Section breakdowns check
      const physicsSec = breakdown.sectionBreakdowns.find(s => s.sectionId === 'PHYSICS');
      expect(physicsSec?.scoreObtained).toBe(3); // 4 - 1
      expect(physicsSec?.attemptedQuestions).toBe(2);

      const chemSec = breakdown.sectionBreakdowns.find(s => s.sectionId === 'CHEMISTRY');
      expect(chemSec?.scoreObtained).toBe(4); // 4 + 0
      expect(chemSec?.attemptedQuestions).toBe(1);

      // Topic mastery check
      const mechanicsTopic = breakdown.topicBreakdowns.find(t => t.topic === 'Mechanics');
      expect(mechanicsTopic?.totalQuestions).toBe(2);
      expect(mechanicsTopic?.correctCount).toBe(1);

      // Deterministic receipt check
      expect(breakdown.receipt.sessionId).toBe('SES-GRADE-01');
      expect(breakdown.receipt.totalQuestionsAnswered).toBe(3);
      expect(breakdown.receipt.checksum).toHaveLength(64);
      expect(breakdown.receipt.localGradingCompleted).toBe(true);
    });

    it('should correctly identify isAttemptedAnswer', () => {
      expect(isAttemptedAnswer({ status: 'ANSWERED', selectedOptionId: 'A' } as any)).toBe(true);
      expect(isAttemptedAnswer({ status: 'ANSWERED_AND_MARKED_FOR_REVIEW', selectedOptionId: 'B' } as any)).toBe(true);
      expect(isAttemptedAnswer({ status: 'MARKED_FOR_REVIEW', selectedOptionId: null } as any)).toBe(false);
      expect(isAttemptedAnswer({ status: 'NOT_ANSWERED', selectedOptionId: null } as any)).toBe(false);
      expect(isAttemptedAnswer({ status: 'NOT_VISITED', selectedOptionId: null } as any)).toBe(false);
    });
  });

  // ==========================================================================
  // 5. API Client Lifecycle & Offline Queue Tests
  // ==========================================================================
  describe('NagCandidateApiClient Lifecycle', () => {
    it('should store, retrieve, and clear auth tokens using SecureTokenStorage', async () => {
      const storage = new InMemoryTokenStorage();
      const client = new NagCandidateApiClient({
        baseUrl: 'https://api.nag.gov.in',
        tokenStorage: storage,
        fetchFn: jest.fn(),
      });

      await client.setAuthTokens({
        accessToken: 'mock_access_token_123',
        refreshToken: 'mock_refresh_token_456',
        tokenType: 'Bearer',
        expiresIn: 3600,
      });

      expect(await client.getAccessToken()).toBe('mock_access_token_123');
      expect(await client.getRefreshToken()).toBe('mock_refresh_token_456');

      await client.clearAuthTokens();
      expect(await client.getAccessToken()).toBeNull();
      expect(await client.getRefreshToken()).toBeNull();
    });

    it('should inject Bearer token into request headers', async () => {
      const storage = new InMemoryTokenStorage();
      await storage.setItem('nag_access_token', 'valid-jwt-token');

      const mockFetch = jest.fn().mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => ({ centres: [] }),
      });

      const client = new NagCandidateApiClient({
        baseUrl: 'https://api.nag.gov.in',
        tokenStorage: storage,
        fetchFn: mockFetch as any,
      });

      await client.request('/api/v1/examinations/centres/public');

      expect(mockFetch).toHaveBeenCalledTimes(1);
      const callHeaders = mockFetch.mock.calls[0][1].headers;
      expect(callHeaders['Authorization']).toBe('Bearer valid-jwt-token');
    });

    it('should perform concurrent-safe 401 token refresh and retry request', async () => {
      const storage = new InMemoryTokenStorage();
      await storage.setItem('nag_access_token', 'expired-token');
      await storage.setItem('nag_refresh_token', 'valid-refresh-token');

      let requestCount = 0;
      let refreshCount = 0;

      const mockFetch = jest.fn().mockImplementation(async (url: string) => {
        if (url.includes('/identity/refresh')) {
          refreshCount++;
          return {
            ok: true,
            status: 200,
            json: async () => ({
              accessToken: 'new-fresh-access-token',
              refreshToken: 'new-fresh-refresh-token',
              tokenType: 'Bearer',
              expiresIn: 3600,
            }),
          };
        }

        requestCount++;
        // First attempt returns 401
        if (requestCount === 1) {
          return {
            ok: false,
            status: 401,
            statusText: 'Unauthorized',
            json: async () => ({ message: 'Token expired' }),
          };
        }

        // Second attempt after refresh succeeds
        return {
          ok: true,
          status: 200,
          json: async () => ({ data: 'secret exam data' }),
        };
      });

      const client = new NagCandidateApiClient({
        baseUrl: 'https://api.nag.gov.in',
        tokenStorage: storage,
        fetchFn: mockFetch as any,
      });

      const response = await client.request<{ data: string }>('/api/v1/protected/data');

      expect(response.data).toBe('secret exam data');
      expect(refreshCount).toBe(1);
      expect(await client.getAccessToken()).toBe('new-fresh-access-token');
    });

    it('should buffer offline response packets and flush sequentially', async () => {
      const client = new NagCandidateApiClient({
        baseUrl: 'https://api.nag.gov.in',
        fetchFn: jest.fn(),
      });

      const packet1 = {
        sessionId: 'SES-01',
        examId: 'EXAM-01',
        candidateId: 'CAND-01',
        revision: 1,
        timestamp: Date.now(),
        questionStates: {},
        clientChecksum: 'chk1',
      };

      const packet2 = {
        sessionId: 'SES-01',
        examId: 'EXAM-01',
        candidateId: 'CAND-01',
        revision: 2,
        timestamp: Date.now(),
        questionStates: {},
        clientChecksum: 'chk2',
      };

      client.enqueueResponsePacket(packet1);
      client.enqueueResponsePacket(packet2);

      expect(client.getOfflineQueue()).toHaveLength(2);

      // Flush queue with a custom save handler
      const flushedPackets: number[] = [];
      const result = await client.flushOfflineQueue(async packet => {
        flushedPackets.push(packet.revision);
        return true;
      });

      expect(result.flushed).toBe(2);
      expect(result.remaining).toBe(0);
      expect(flushedPackets).toEqual([1, 2]);
      expect(client.getOfflineQueue()).toHaveLength(0);
    });

    it('should calculate exponential backoff delay correctly', () => {
      const client = new NagCandidateApiClient({
        baseUrl: 'https://api.nag.gov.in',
        fetchFn: jest.fn(),
      });

      expect(client.calculateBackoffDelay(0, 1000, 30000)).toBe(1000);
      expect(client.calculateBackoffDelay(1, 1000, 30000)).toBe(2000);
      expect(client.calculateBackoffDelay(2, 1000, 30000)).toBe(4000);
      expect(client.calculateBackoffDelay(3, 1000, 30000)).toBe(8000);
      expect(client.calculateBackoffDelay(10, 1000, 30000)).toBe(30000); // capped at maxDelayMs
    });
  });
});
