/**
 * Comprehensive Unit Tests for Candidate Mobile Features (Issue #158, ADR 0008)
 * Tests Formula Rendering Engine (LaTeX & SMILES), Auth Controller,
 * Hall Ticket Wallet & QR Matrix, Exam Catalog Filtering, Application Wizard,
 * and CBT Session Controller with Local Grading.
 */

import {
  normalizeLatexFormula,
  normalizeNewlinesPreservingLatex,
  repairEscapedArtifacts,
  renderSmilesToSvgDescriptor,
  parseFormulaText,
} from '../features/formula/formula-parser';

import {
  AuthController,
  CandidateRegistrationPayload,
} from '../features/auth/auth-controller';

import {
  WalletController,
  DigitalAdmitCard,
  SignedQrPayload,
  generateDeterministicQrMatrix,
} from '../features/wallet/wallet-controller';

import {
  CatalogController,
  CandidateEligibilityProfile,
  CatalogExam,
} from '../features/catalog/catalog-controller';

import {
  ApplicationWizardController,
} from '../features/application/application-wizard-controller';

import {
  CbtSessionController,
  ExamItem,
  ExamSection,
} from '../features/cbt/cbt-session-controller';

describe('1. Formula Rendering Engine (LaTeX/KaTeX & SMILES Chemistry)', () => {
  describe('Latex Normalization & Newline Preservation', () => {
    it('normalizes over-escaped backslashes (\\\\frac -> \\frac)', () => {
      const input = '\\\\frac{a}{b} + \\\\det(A)';
      const output = normalizeLatexFormula(input);
      expect(output).toBe('\\frac{a}{b} + \\det(A)');
    });

    it('normalizes \\ce\\{...\\} to standard \\ce{...}', () => {
      const input = '\\ce\\{H2SO4\\}';
      const output = normalizeLatexFormula(input);
      expect(output).toBe('\\ce{H2SO4}');
    });

    it('preserves \\begin environments without altering spacing', () => {
      const input = '\\begin{matrix} 1 & 0 \\\\ 0 & 1 \\end{matrix}';
      const output = normalizeLatexFormula(input);
      expect(output).toContain('\\begin{matrix}');
    });

    it('preserves lowercase LaTeX commands (\\neq, \\neg, \\rightarrow, \\theta, \\tau) without splitting \\n', () => {
      const input = 'Condition: \\neq 0 and \\neg L with angle \\theta and time \\tau';
      const output = normalizeNewlinesPreservingLatex(input);
      expect(output).toContain('\\neq 0');
      expect(output).toContain('\\neg L');
      expect(output).toContain('\\theta');
      expect(output).toContain('\\tau');
    });

    it('converts literal \\n to real newlines when followed by numbers or uppercase letters', () => {
      const input = '**Statements:**\\n1. First premise.\\n2. Second premise.\\n\\n**Conclusions:**\\nI. First conclusion.';
      const output = normalizeNewlinesPreservingLatex(input);
      expect(output).toContain('\n1. First premise.');
      expect(output).toContain('\n2. Second premise.');
      expect(output).toContain('\n\n**Conclusions:**');
      expect(output).toContain('\nI. First conclusion.');
    });

    it('repairs control character unescaping artifacts', () => {
      const input = '\x0crac{1}{2} + \ttheta';
      const repaired = repairEscapedArtifacts(input);
      expect(repaired).toBe('\\frac{1}{2} + \\theta');
    });
  });

  describe('SMILES 2D Coordinate & SVG Descriptor Generation', () => {
    it('computes 2D atom coordinates and SVG string for acyclic molecules (ethanol CCO)', () => {
      const descriptor = renderSmilesToSvgDescriptor('CCO');
      expect(descriptor.smiles).toBe('CCO');
      expect(descriptor.atoms.length).toBe(3);
      expect(descriptor.bonds.length).toBe(2);
      expect(descriptor.width).toBeGreaterThan(0);
      expect(descriptor.height).toBeGreaterThan(0);
      expect(descriptor.svgString).toContain('<svg');
      expect(descriptor.svgString).toContain('</svg>');
    });

    it('handles cyclic structures like benzene (c1ccccc1)', () => {
      const descriptor = renderSmilesToSvgDescriptor('c1ccccc1');
      expect(descriptor.atoms.length).toBe(6);
      expect(descriptor.bonds.length).toBe(6); // 5 chain bonds + 1 ring closure bond
      expect(descriptor.svgString).toContain('<line');
    });

    it('handles double bonds and carbonyl oxygen (acetic acid CC(=O)O)', () => {
      const descriptor = renderSmilesToSvgDescriptor('CC(=O)O');
      expect(descriptor.atoms.length).toBe(4);
      const doubleBond = descriptor.bonds.find((b) => b.order === 2);
      expect(doubleBond).toBeDefined();
    });

    it('handles heteroatoms and halogens with distinct element colors', () => {
      const descriptor = renderSmilesToSvgDescriptor('CCN(C)Cl');
      const chlorine = descriptor.atoms.find((a) => a.symbol === 'CL');
      expect(chlorine).toBeDefined();
      expect(chlorine?.color).toBe('#2F855A'); // Green for Chlorine
    });

    it('returns empty descriptor gracefully for blank input', () => {
      const descriptor = renderSmilesToSvgDescriptor('');
      expect(descriptor.atoms.length).toBe(0);
      expect(descriptor.bonds.length).toBe(0);
    });
  });

  describe('Mixed Text & Formula Parser (parseFormulaText)', () => {
    it('parses mixed content containing $$...$$ LaTeX, \\ce{...}, and [SMILES: ...]', () => {
      const rawText =
        'Given the reaction \\ce{CH3COOH + CH3OH -> CH3COOCH3 + H2O} with substrate [SMILES: CC(=O)O], compute the equilibrium constant $$K_c = \\frac{[P]}{[R]}$$.';

      const tokens = parseFormulaText(rawText);
      expect(tokens.length).toBeGreaterThan(3);

      const latexToken = tokens.find((t) => t.type === 'latex');
      expect(latexToken).toBeDefined();
      expect(latexToken?.type === 'latex' && latexToken.content).toContain('\\frac{[P]}{[R]}');

      const chemToken = tokens.find((t) => t.type === 'chemistry');
      expect(chemToken).toBeDefined();
      expect(chemToken?.type === 'chemistry' && chemToken.content).toContain('CH3COOH');

      const smilesToken = tokens.find((t) => t.type === 'smiles');
      expect(smilesToken).toBeDefined();
      expect(smilesToken?.type === 'smiles' && smilesToken.smiles).toBe('CC(=O)O');
    });
  });
});

describe('2. Candidate Authentication Controller (Phase 1)', () => {
  let controller: AuthController;

  beforeEach(() => {
    controller = new AuthController();
  });

  it('validates mobile number format according to Indian standards (10 digits starting 6-9)', () => {
    expect(controller.validateMobile('9876543210')).toBe(true);
    expect(controller.validateMobile('6123456789')).toBe(true);
    expect(controller.validateMobile('1234567890')).toBe(false); // invalid starting digit
    expect(controller.validateMobile('98765')).toBe(false); // too short
  });

  it('requires DPDP consent on candidate registration', async () => {
    const invalidPayload: CandidateRegistrationPayload = {
      fullName: 'Aarav Sharma',
      mobileNumber: '9876543210',
      email: 'aarav@example.com',
      password: 'SecurePassword123',
      dpdpConsentAccepted: false, // Rejected
    };

    const success = await controller.register(invalidPayload);
    expect(success).toBe(false);
    expect(controller.getState().error).toContain('DPDP');
  });

  it('registers successfully with valid details and DPDP consent', async () => {
    const validPayload: CandidateRegistrationPayload = {
      fullName: 'Aarav Sharma',
      mobileNumber: '9876543210',
      email: 'aarav@example.com',
      password: 'SecurePassword123',
      dpdpConsentAccepted: true,
      category: 'GEN',
    };

    const success = await controller.register(validPayload);
    expect(success).toBe(true);
    expect(controller.getState().candidateProfile).toBeDefined();
    expect(controller.getState().candidateProfile?.fullName).toBe('Aarav Sharma');
  });

  it('sends mobile OTP and transitions step to VERIFY_OTP', async () => {
    const success = await controller.sendOtp('9876543210', 'LOGIN');
    expect(success).toBe(true);
    const state = controller.getState();
    expect(state.step).toBe('VERIFY_OTP');
    expect(state.mobileNumber).toBe('9876543210');
    expect(state.mockOtpCode).toBe('123456');
  });

  it('verifies 6-digit OTP and authenticates candidate', async () => {
    await controller.sendOtp('9876543210', 'LOGIN');
    const success = await controller.verifyOtp('9876543210', '123456');
    expect(success).toBe(true);
    const state = controller.getState();
    expect(state.step).toBe('AUTHENTICATED');
    expect(state.tokens).toBeDefined();
    expect(state.tokens?.accessToken).toContain('nag_jwt_acc_');
  });

  it('transitions to VERIFY_TOTP when require2fa is enabled', async () => {
    await controller.sendOtp('9876543210', 'LOGIN');
    const success = await controller.verifyOtp('9876543210', '123456', true);
    expect(success).toBe(true);
    expect(controller.getState().step).toBe('VERIFY_TOTP');

    // Complete 2FA TOTP
    const totpSuccess = await controller.verifyTotp('654321');
    expect(totpSuccess).toBe(true);
    expect(controller.getState().step).toBe('AUTHENTICATED');
  });

  it('links DigiLocker and binds Aadhaar number', async () => {
    await controller.sendOtp('9876543210', 'LOGIN');
    await controller.verifyOtp('9876543210', '123456');

    const linkSuccess = await controller.linkDigiLocker('4321');
    expect(linkSuccess).toBe(true);
    const state = controller.getState();
    expect(state.digiLocker.linked).toBe(true);
    expect(state.digiLocker.aadhaarNumberMasked).toBe('XXXX-XXXX-4321');
    expect(state.candidateProfile?.aadhaarVerified).toBe(true);
  });

  it('authenticates via password login fallback and sets AUTHENTICATED step', async () => {
    const success = await controller.loginWithPassword('candidate@example.gov.in', 'Password123');
    expect(success).toBe(true);
    const state = controller.getState();
    expect(state.step).toBe('AUTHENTICATED');
    expect(state.tokens).toBeDefined();
    expect(state.infoMessage).toContain('Authenticated');
  });

  it('rejects password login with empty fields or invalid password length', async () => {
    const emptyId = await controller.loginWithPassword('', 'Password123');
    expect(emptyId).toBe(false);
    expect(controller.getState().error).toContain('registered email or username');

    const emptyPass = await controller.loginWithPassword('candidate@example.gov.in', '');
    expect(emptyPass).toBe(false);
    expect(controller.getState().error).toContain('password');

    const shortPass = await controller.loginWithPassword('candidate@example.gov.in', '123');
    expect(shortPass).toBe(false);
    expect(controller.getState().error).toContain('min 6 characters');
  });
});

describe('3. Offline Hall Ticket Wallet & Signed QR Code (Phase 1)', () => {
  let walletController: WalletController;

  beforeEach(() => {
    walletController = new WalletController();
  });

  it('initializes with cached sample admit cards in offline mode', () => {
    const state = walletController.getState();
    expect(state.admitCards.length).toBeGreaterThan(0);
    expect(state.isOfflineMode).toBe(true);
    const active = walletController.getSelectedAdmitCard();
    expect(active).toBeDefined();
    expect(active?.hallTicketNumber).toMatch(/^HT-\d{4}-\d{6}$/);
  });

  it('generates a 29x29 deterministic QR matrix with finder and timing patterns', () => {
    const matrix = generateDeterministicQrMatrix('HT-2026-882194', 29);
    expect(matrix.length).toBe(29);
    expect(matrix[0].length).toBe(29);

    // Top-left finder pattern center (row 3, col 3) must be black/true
    expect(matrix[3][3]).toBe(true);

    // Top-right finder pattern center (row 3, col 25) must be black/true
    expect(matrix[3][25]).toBe(true);

    // Bottom-left finder pattern center (row 25, col 3) must be black/true
    expect(matrix[25][3]).toBe(true);
  });

  it('verifies valid offline signed QR payload', () => {
    const active = walletController.getSelectedAdmitCard()!;
    const verification = walletController.verifyOfflinePayload(active.signedQrPayload);
    expect(verification.valid).toBe(true);
    expect(verification.code).toBe('VALID');
    expect(verification.message).toContain('verified');
  });

  it('flags expired or malformed payloads', () => {
    const expiredPayload: SignedQrPayload = {
      version: '1.0',
      keyId: 'KEY-01',
      algorithm: 'ECDSA_P256',
      hallTicketNumber: 'HT-2026-112233',
      candidateId: 'CND-1',
      candidateName: 'Test',
      examId: 'EXAM-1',
      shiftId: 'SHIFT-1',
      centreId: 'CENTRE-1',
      issuedAt: Date.now() - 100000,
      expiresAt: Date.now() - 1000, // Expired
      digest: 'a'.repeat(64),
      signature: 'b'.repeat(64),
    };

    const verification = walletController.verifyOfflinePayload(expiredPayload);
    expect(verification.valid).toBe(false);
    expect(verification.code).toBe('EXPIRED');
  });

  it('calculates gate reporting countdown accurately', () => {
    const examDate = '2026-11-15';
    const gateClosingTime = '08:45';
    // Test with mock time: 2026-11-15 07:45 (exactly 1 hour before)
    const mockNow = new Date('2026-11-15T07:45:00').getTime();

    const countdown = walletController.calculateGateCountdown(examDate, gateClosingTime, mockNow);
    expect(countdown.isClosed).toBe(false);
    expect(countdown.hours).toBe(1);
    expect(countdown.minutes).toBe(0);
    expect(countdown.formatted).toContain('01h 00m');
  });

  it('flags gate as closed when closing time has passed', () => {
    const examDate = '2026-11-15';
    const gateClosingTime = '08:45';
    const mockNowAfter = new Date('2026-11-15T09:00:00').getTime();

    const countdown = walletController.calculateGateCountdown(examDate, gateClosingTime, mockNowAfter);
    expect(countdown.isClosed).toBe(true);
    expect(countdown.formatted).toBe('Gate Closed');
  });
});

describe('4. Exam Catalog Discovery & Real-Time Eligibility Filters (Phase 2)', () => {
  let catalog: CatalogController;

  beforeEach(() => {
    catalog = new CatalogController();
  });

  it('filters exams by search query across title, code, and conducting body', () => {
    catalog.setSearchQuery('Civil Services');
    const results = catalog.getFilteredExams();
    expect(results.length).toBe(1);
    expect(results[0].code).toBe('UPSC-CSP-2026');
  });

  it('filters exams by category (UPSC, NTA, SSC, BANKING, STATE_PSC)', () => {
    catalog.setCategory('NTA');
    const results = catalog.getFilteredExams();
    expect(results.every((e) => e.category === 'NTA')).toBe(true);
  });

  it('evaluates candidate age and qualification eligibility', () => {
    const exam = catalog.getState().exams.find((e) => e.id === 'EXAM-UPSC-2026')!;

    // Candidate too young (age 19, min is 21)
    const youngProfile: CandidateEligibilityProfile = {
      age: 19,
      qualification: 'GRADUATE',
      category: 'GEN',
      pwd: false,
    };
    const youngResult = catalog.checkEligibility(exam, youngProfile);
    expect(youngResult.eligible).toBe(false);
    expect(youngResult.reasons[0]).toContain('Minimum age is 21');

    // Candidate with insufficient qualification (12th, requires Graduate)
    const underqualifiedProfile: CandidateEligibilityProfile = {
      age: 24,
      qualification: '12TH',
      category: 'GEN',
      pwd: false,
    };
    const qualResult = catalog.checkEligibility(exam, underqualifiedProfile);
    expect(qualResult.eligible).toBe(false);
    expect(qualResult.reasons[0]).toContain('Minimum qualification is GRADUATE');
  });

  it('applies category fee waiver for SC, ST, and PwD candidates', () => {
    const exam = catalog.getState().exams.find((e) => e.id === 'EXAM-NTA-JEE-2026')!;
    expect(exam.feeAmount).toBe(1000);

    const scProfile: CandidateEligibilityProfile = {
      age: 18,
      qualification: '12TH',
      category: 'SC',
      pwd: false,
    };
    const result = catalog.checkEligibility(exam, scProfile);
    expect(result.feeWaiverApplicable).toBe(true);
    expect(result.effectiveFee).toBe(0); // 100% Fee waiver
  });

  it('sorts exams by fee ascending and descending', () => {
    catalog.setSort('FEE_ASC');
    const ascResults = catalog.getFilteredExams();
    expect(ascResults[0].feeAmount).toBeLessThanOrEqual(ascResults[ascResults.length - 1].feeAmount);

    catalog.setSort('FEE_DESC');
    const descResults = catalog.getFilteredExams();
    expect(descResults[0].feeAmount).toBeGreaterThanOrEqual(descResults[descResults.length - 1].feeAmount);
  });
});

describe('5. 3-Step Exam Application Wizard Controller (Phase 2)', () => {
  let wizard: ApplicationWizardController;

  beforeEach(() => {
    wizard = new ApplicationWizardController();
  });

  it('enforces 3 distinct examination centres in Step 1', () => {
    // Select duplicate centres
    wizard.setCentrePreference(1, 'CENTRE-DEL-01');
    wizard.setCentrePreference(2, 'CENTRE-DEL-01'); // Duplicate!
    wizard.setCentrePreference(3, 'CENTRE-MUM-01');

    const isValid = wizard.validateStep1();
    expect(isValid).toBe(false);
    expect(wizard.getState().error).toContain('must be mutually distinct');

    // Fix to 3 distinct centres
    wizard.setCentrePreference(2, 'CENTRE-DEL-02');
    const isNowValid = wizard.validateStep1();
    expect(isNowValid).toBe(true);
    expect(wizard.getState().error).toBeNull();
  });

  it('advances through 3 steps and records shift & PwD assistance', () => {
    // Step 1
    wizard.setCentrePreference(1, 'CENTRE-DEL-01');
    wizard.setCentrePreference(2, 'CENTRE-DEL-02');
    wizard.setCentrePreference(3, 'CENTRE-BLR-01');
    expect(wizard.nextStep()).toBe(true);
    expect(wizard.getState().currentStep).toBe(2);

    // Step 2
    wizard.setShiftPreference('SHIFT-MORNING');
    wizard.setPwdAccommodations(true, 'LOCOMOTOR', true, 'HINDI');
    expect(wizard.nextStep()).toBe(true);
    expect(wizard.getState().currentStep).toBe(3);

    // Step 3
    wizard.setInstructionsAccepted(true);
    expect(wizard.validateStep3()).toBe(true);
  });

  it('submits application, generates ApplicationReceipt, and issues Hall Ticket (HT-2026-XXXXXX)', async () => {
    wizard.setCentrePreference(1, 'CENTRE-DEL-01');
    wizard.setCentrePreference(2, 'CENTRE-DEL-02');
    wizard.setCentrePreference(3, 'CENTRE-BLR-01');
    wizard.nextStep();

    wizard.setShiftPreference('SHIFT-MORNING');
    wizard.nextStep();

    wizard.setInstructionsAccepted(true);

    const receipt = await wizard.submitApplication();
    expect(receipt).not.toBeNull();
    expect(receipt?.applicationNumber).toMatch(/^APP-2026-\d{6}$/);
    expect(receipt?.hallTicketNumber).toMatch(/^HT-2026-\d{6}$/);
    expect(wizard.getState().currentStep).toBe('SUCCESS');
    expect(wizard.getState().issuedHallTicket).toBeDefined();
  });
});

describe('6. Mobile CBT Mock Engine & Instant Local Grading (Phase 2 & 3)', () => {
  let cbt: CbtSessionController;

  beforeEach(() => {
    cbt = new CbtSessionController();
  });

  it('initializes question palette with Question 1 as NOT_ANSWERED and others NOT_VISITED', () => {
    const summary = cbt.getPaletteSummary();
    expect(summary.totalQuestions).toBe(6);
    expect(summary.notAnsweredCount).toBe(1);
    expect(summary.notVisitedCount).toBe(5);
  });

  it('selects option and records ANSWERED on Save & Next', () => {
    cbt.selectOption('B');
    expect(cbt.getActiveQuestionState()?.selectedOptionId).toBe('B');

    cbt.saveAndNext();
    const prevEntry = cbt.getState().questionStates['Q-PHY-01'];
    expect(prevEntry.status).toBe('ANSWERED');
    expect(cbt.getState().activeQuestionIndex).toBe(1);
  });

  it('records ANSWERED_AND_MARKED_FOR_REVIEW when marked with option selected', () => {
    cbt.selectOption('A');
    cbt.markForReviewAndNext();
    const entry = cbt.getState().questionStates['Q-PHY-01'];
    expect(entry.status).toBe('ANSWERED_AND_MARKED_FOR_REVIEW');
    expect(entry.reviewFlag).toBe(true);
  });

  it('clears selected response', () => {
    cbt.selectOption('C');
    expect(cbt.getActiveQuestionState()?.selectedOptionId).toBe('C');

    cbt.clearResponse();
    expect(cbt.getActiveQuestionState()?.selectedOptionId).toBeNull();
    expect(cbt.getActiveQuestionState()?.status).toBe('NOT_ANSWERED');
  });

  it('tracks FLAG_SECURE, LockTaskMode kiosk status, and proctoring anomalies', () => {
    const state = cbt.getState();
    expect(state.security.isFlagSecureActive).toBe(true);
    expect(state.security.isLockTaskPinned).toBe(true);

    cbt.recordAnomaly();
    expect(cbt.getState().security.anomalyCount).toBe(1);
  });

  it('performs instant local grading with positive and negative marking calculation', () => {
    // Answer Q1 correctly (B) -> +4 marks
    cbt.jumpToQuestion(0);
    cbt.selectOption('B');
    cbt.saveAndNext();

    // Answer Q2 incorrectly (C instead of A) -> -1 mark
    cbt.jumpToQuestion(1);
    cbt.selectOption('C');
    cbt.saveAndNext();

    // Leave Q3 to Q6 unattempted (0 marks)
    const breakdown = cbt.submitExam();

    expect(breakdown.totalQuestions).toBe(6);
    expect(breakdown.totalAttempted).toBe(2);
    expect(breakdown.totalCorrect).toBe(1);
    expect(breakdown.totalIncorrect).toBe(1);
    expect(breakdown.totalUnattempted).toBe(4);
    expect(breakdown.positiveMarksObtained).toBe(4.0);
    expect(breakdown.negativeMarksDeducted).toBe(1.0);
    expect(breakdown.rawScore).toBe(3.0); // 4.0 - 1.0 = 3.0
    expect(breakdown.accuracyPercentage).toBe(50.0);

    // Section performance
    expect(breakdown.sectionBreakdowns.length).toBe(3);
    const physicsSec = breakdown.sectionBreakdowns.find((s) => s.sectionId === 'SEC-PHYSICS');
    expect(physicsSec).toBeDefined();
    expect(physicsSec?.attemptedQuestions).toBe(2);
    expect(physicsSec?.scoreObtained).toBe(3.0);

    // Hardware signature receipt
    expect(breakdown.receipt.isHardwareSigned).toBe(true);
    expect(breakdown.receipt.signerKeyAlias).toBe('NAG_KEYSTORE_STRONGBOX_ECDSA_P256');
  });
});
