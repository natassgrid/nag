/**
 * Comprehensive Unit Tests for Candidate Mobile Dashboard, Profile, Review, and Results
 * Covers state controllers, data models, validation logic, cascading selectors,
 * cryptographic verification, and answer key objection workflows.
 */

import {
  DashboardController,
  EnrolledExam,
  DEFAULT_ENROLLED_EXAMS,
} from '../features/dashboard/dashboard-controller';

import {
  ProfileController,
  DEFAULT_CANDIDATE_PROFILE,
  INDIAN_STATES_AND_CITIES,
} from '../features/profile/profile-controller';

import {
  ReviewController,
  DEFAULT_REVIEW_QUESTIONS,
  AnswerKeyChallengePayload,
} from '../features/review/review-controller';

import {
  ResultsController,
  DEFAULT_EXAM_RESULTS,
} from '../features/results/results-controller';

describe('Candidate Mobile Features Suite: Dashboard, Profile, Review & Results', () => {
  // =========================================================================
  // 1. DASHBOARD CONTROLLER TESTS
  // =========================================================================
  describe('1. Dashboard Controller (Enrolled Assessments, KPIs & Shortcuts)', () => {
    let controller: DashboardController;

    beforeEach(() => {
      controller = new DashboardController();
    });

    it('initializes with default enrolled exams and accurate KPI metrics', () => {
      const state = controller.getState();
      expect(state.enrolledExams.length).toBe(DEFAULT_ENROLLED_EXAMS.length);
      expect(state.candidateName).toBe('Aryan Sharma');
      expect(state.kpis.totalRegistered).toBe(5);
      expect(state.kpis.liveCount).toBe(1); // NES-2026-S1 is LIVE
      expect(state.kpis.upcomingCount).toBe(2); // GATE & RBI are UPCOMING non-practice
      expect(state.kpis.practiceCount).toBe(4); // exams with practiceAvailable or isPractice
      expect(state.kpis.scorecardsCount).toBe(1); // CSE-PRE-2025 is COMPLETED
    });

    it('filters enrolled examinations by status correctly', () => {
      controller.setFilter('LIVE');
      const live = controller.getFilteredExams();
      expect(live.length).toBe(1);
      expect(live[0].code).toBe('NES-2026-S1');

      controller.setFilter('UPCOMING');
      const upcoming = controller.getFilteredExams();
      expect(upcoming.every((e) => !e.isPractice && (e.status === 'UPCOMING' || e.status === 'SCHEDULED'))).toBe(true);
      expect(upcoming.some((e) => e.code === 'GATE-DPI-2026')).toBe(true);

      controller.setFilter('PRACTICE');
      const practice = controller.getFilteredExams();
      expect(practice.some((e) => e.isPractice || e.practiceAvailable)).toBe(true);

      controller.setFilter('COMPLETED');
      const completed = controller.getFilteredExams();
      expect(completed.length).toBe(1);
      expect(completed[0].code).toBe('CSE-PRE-2025');

      controller.setFilter('ALL');
      expect(controller.getFilteredExams().length).toBe(5);
    });

    it('generates upcoming alerts for live sessions and released admit cards', () => {
      const state = controller.getState();
      expect(state.alerts.length).toBeGreaterThan(0);

      const criticalAlert = state.alerts.find((a) => a.severity === 'CRITICAL');
      expect(criticalAlert).toBeDefined();
      expect(criticalAlert?.actionType).toBe('LAUNCH_LIVE');

      const admitAlert = state.alerts.find((a) => a.actionType === 'DOWNLOAD_ADMIT_CARD');
      expect(admitAlert).toBeDefined();
    });

    it('generates digital admit card with QR verification token', () => {
      const card = controller.openAdmitCard('exam-1');
      expect(card).not.toBeNull();
      expect(card?.examCode).toBe('NES-2026-S1');
      expect(card?.rollNumber).toBe('849202');
      expect(card?.qrVerificationToken).toContain('849202');
      expect(card?.instructions.length).toBeGreaterThan(0);

      const parsedToken = JSON.parse(card!.qrVerificationToken);
      expect(parsedToken.nagId).toBe('exam-1');
      expect(parsedToken.roll).toBe('849202');

      controller.closeAdmitCard();
      expect(controller.getState().selectedAdmitCard).toBeNull();
    });

    it('manages practice paper retrieval and picker dialog', () => {
      const papers = controller.getPracticePapers('exam-1');
      expect(papers.length).toBe(2);
      expect(papers[0].name).toContain('Mock Paper A');

      controller.selectPracticeExam('exam-1');
      expect(controller.getState().selectedPracticeExam?.papers.length).toBe(2);

      const launch = controller.startPracticePaper('exam-1', 'pp-nes-cs-01');
      expect(launch).toEqual({ examId: 'exam-1', paperId: 'pp-nes-cs-01' });
      expect(controller.getState().selectedPracticeExam).toBeNull();
    });

    it('handles live exam launch and scorecard navigation actions', () => {
      const liveLaunch = controller.startLiveExam('exam-1');
      expect(liveLaunch).not.toBeNull();
      expect(liveLaunch?.sessionId).toBe('sess-nes-2026-live-01');

      // Non-live exam should return null
      const invalidLaunch = controller.startLiveExam('exam-2');
      expect(invalidLaunch).toBeNull();

      // Scorecard for completed exam
      const sc = controller.viewScorecard('exam-prev-1');
      expect(sc).toBe('result-exam-prev-1');

      const nonCompletedSc = controller.viewScorecard('exam-1');
      expect(nonCompletedSc).toBeNull();
    });

    it('adds new enrolled exam and dynamically recalculates KPIs', () => {
      const newExam: EnrolledExam = {
        id: 'exam-test-new',
        applicationId: 'APP-TEST-99',
        code: 'ISRO-SC-2026',
        title: 'ISRO Scientist/Engineer Assessment',
        conductingAuthority: 'Indian Space Research Organisation',
        scheduledDate: '2026-12-20',
        scheduledTime: '10:00 AM - 01:00 PM',
        durationMinutes: 180,
        totalMarks: 240,
        centerName: 'ISRO Telemetry Center',
        centerAddress: 'Peenya Industrial Area',
        centerCity: 'Bengaluru',
        centerState: 'Karnataka',
        shiftName: 'Morning Shift',
        rollNumber: '990142',
        status: 'UPCOMING',
        admitCardReady: false,
        daysRemaining: 50,
        isPractice: false,
        practiceAvailable: true,
      };

      controller.addEnrolledExam(newExam);
      const state = controller.getState();
      expect(state.kpis.totalRegistered).toBe(6);
      expect(state.kpis.upcomingCount).toBe(3);
    });

    it('subscribes to state updates and cleanly unsubscribes', () => {
      let callCount = 0;
      const unsub = controller.subscribe(() => {
        callCount++;
      });
      expect(callCount).toBe(1);

      controller.setFilter('COMPLETED');
      expect(callCount).toBe(2);

      unsub();
      controller.setFilter('ALL');
      expect(callCount).toBe(2); // no further calls after unsubscribe
    });
  });

  // =========================================================================
  // 2. PROFILE CONTROLLER TESTS
  // =========================================================================
  describe('2. Profile Controller (Personal, Cascading City, Education CRUD, DigiLocker)', () => {
    let controller: ProfileController;

    beforeEach(() => {
      controller = new ProfileController();
    });

    it('initializes with default candidate profile and calculates 100% completion for full profile', () => {
      const state = controller.getState();
      expect(state.profile.fullName).toBe('Aryan Sharma');
      expect(state.profile.category).toBe('GENERAL');
      expect(state.profile.profileCompletionPercentage).toBe(100);
      expect(state.availableCities.length).toBeGreaterThan(0);
    });

    it('updates personal details and updates state', () => {
      controller.updatePersonalDetails({
        fullName: 'Aryan Sharma',
        gender: 'MALE',
        preferredLanguage: 'Hindi',
      });
      const state = controller.getState();
      expect(state.profile.preferredLanguage).toBe('Hindi');
    });

    it('updates contact details and enforces OTP verification reset', () => {
      controller.updateContactDetails('9999888877', 'new.aryan@gov.in');
      let state = controller.getState();
      expect(state.profile.mobile).toBe('9999888877');
      expect(state.profile.mobileVerified).toBe(false);
      expect(state.profile.emailVerified).toBe(false);

      // Verify OTP
      const mobileSuccess = controller.verifyMobileOtp('123456');
      expect(mobileSuccess).toBe(true);
      expect(controller.getState().profile.mobileVerified).toBe(true);

      const emailSuccess = controller.verifyEmailOtp('123456');
      expect(emailSuccess).toBe(true);
      expect(controller.getState().profile.emailVerified).toBe(true);
    });

    it('cascades available cities when state is changed using INDIAN_STATES_AND_CITIES', () => {
      // Test Maharashtra cascade
      controller.onStateChange('Maharashtra');
      let state = controller.getState();
      expect(state.profile.address.state).toBe('Maharashtra');
      expect(state.availableCities).toContain('Mumbai City');
      expect(state.availableCities).toContain('Pune');
      expect(state.availableCities).toContain('Nagpur');
      expect(state.profile.address.city).toBe(state.availableCities[0]);

      // Test Karnataka cascade
      controller.onStateChange('Karnataka');
      state = controller.getState();
      expect(state.profile.address.state).toBe('Karnataka');
      expect(state.availableCities).toContain('Bengaluru Urban');
      expect(state.availableCities).toContain('Mysuru');

      // Test Delhi cascade
      controller.onStateChange('Delhi (NCT)');
      state = controller.getState();
      expect(state.profile.address.state).toBe('Delhi (NCT)');
      expect(state.availableCities).toContain('New Delhi');
    });

    it('validates education entries strictly on board name, passing year, and score ranges', () => {
      // Missing board name
      const res1 = controller.validateEducationEntry({
        boardOrUniversity: '',
        passingYear: 2020,
        scoreType: 'PERCENTAGE',
        percentageOrCgpa: '85',
      });
      expect(res1.valid).toBe(false);
      expect(res1.errors.some((e) => e.includes('Board or University'))).toBe(true);

      // Invalid passing year (future / out of range)
      const res2 = controller.validateEducationEntry({
        boardOrUniversity: 'Delhi University',
        passingYear: 2045,
        scoreType: 'PERCENTAGE',
        percentageOrCgpa: '85',
      });
      expect(res2.valid).toBe(false);
      expect(res2.errors.some((e) => e.includes('Passing year'))).toBe(true);

      // Invalid percentage score (> 100)
      const res3 = controller.validateEducationEntry({
        boardOrUniversity: 'Delhi University',
        passingYear: 2022,
        scoreType: 'PERCENTAGE',
        percentageOrCgpa: '105',
      });
      expect(res3.valid).toBe(false);
      expect(res3.errors.some((e) => e.includes('between 0% and 100%'))).toBe(true);

      // Invalid CGPA score (> 10)
      const res4 = controller.validateEducationEntry({
        boardOrUniversity: 'IIT Delhi',
        passingYear: 2023,
        scoreType: 'CGPA',
        percentageOrCgpa: '11.5',
      });
      expect(res4.valid).toBe(false);
      expect(res4.errors.some((e) => e.includes('between 0.0 and 10.0'))).toBe(true);

      // Valid entry
      const res5 = controller.validateEducationEntry({
        boardOrUniversity: 'IIT Delhi',
        passingYear: 2023,
        scoreType: 'CGPA',
        percentageOrCgpa: '9.45',
      });
      expect(res5.valid).toBe(true);
    });

    it('executes Education CRUD operations (add, update, delete)', () => {
      const initialCount = controller.getState().profile.education.length;

      // 1. Add
      const addRes = controller.addEducation({
        qualification: 'Master Degree (M.Tech / M.E / M.Sc / M.Com / MBA / MCA)',
        boardOrUniversity: 'IISc Bangalore',
        passingYear: 2025,
        scoreType: 'CGPA',
        percentageOrCgpa: '9.8',
        specialization: 'Quantum Computing',
      });
      expect(addRes.success).toBe(true);
      expect(addRes.id).toBeDefined();
      expect(controller.getState().profile.education.length).toBe(initialCount + 1);

      // 2. Update
      const updateRes = controller.updateEducation(addRes.id!, {
        percentageOrCgpa: '9.9',
        gradeOrDivision: 'Gold Medalist',
      });
      expect(updateRes.success).toBe(true);
      const updated = controller.getState().profile.education.find((e) => e.id === addRes.id);
      expect(updated?.percentageOrCgpa).toBe('9.9');
      expect(updated?.gradeOrDivision).toBe('Gold Medalist');

      // 3. Delete
      const deleteSuccess = controller.deleteEducation(addRes.id!);
      expect(deleteSuccess).toBe(true);
      expect(controller.getState().profile.education.length).toBe(initialCount);
    });

    it('links DigiLocker and issues verified claim', () => {
      const linked = controller.linkDigiLocker('123456784920');
      expect(linked).toBe(true);
      const state = controller.getState();
      expect(state.profile.identity.digiLockerLinked).toBe(true);
      expect(state.profile.identity.aadhaarLastFour).toBe('4920');
      expect(state.profile.digiLockerClaims?.some((c) => c.docType === 'AADHAAR')).toBe(true);
    });

    it('updates Category & PwD accommodation preferences', () => {
      controller.setCategory('OBC_NCL');
      expect(controller.getState().profile.category).toBe('OBC_NCL');
      expect(controller.getState().profile.reservationCategory).toContain('Non-Creamy');

      controller.updatePwdPreferences({
        isPwd: true,
        disabilityCategory: 'LOCOMOTOR',
        disabilityPercentage: 45,
        requiresScribe: true,
        requiresCompensatoryTime: true,
      });

      const pwd = controller.getState().profile.pwdDetails;
      expect(pwd.isPwd).toBe(true);
      expect(pwd.requiresScribe).toBe(true);
      expect(pwd.requiresCompensatoryTime).toBe(true);
    });

    it('accurately calculates completion score for partial profiles', () => {
      const minimalProfile = {
        ...DEFAULT_CANDIDATE_PROFILE,
        mobileVerified: false,
        emailVerified: false,
        education: [],
        identity: {
          ...DEFAULT_CANDIDATE_PROFILE.identity,
          verified: false,
          digiLockerLinked: false,
          photoUploaded: false,
          signatureUploaded: false,
        },
      };

      const completion = controller.calculateCompletionPercentage(minimalProfile);
      expect(completion).toBeLessThan(50);
    });
  });

  // =========================================================================
  // 3. REVIEW CONTROLLER TESTS
  // =========================================================================
  describe('3. Review Controller (Post-Exam Responses & Answer Key Challenges)', () => {
    let controller: ReviewController;

    beforeEach(() => {
      controller = new ReviewController();
    });

    it('initializes with default STEM exam questions and accurate scoring stats', () => {
      const state = controller.getState();
      expect(state.attempt.questions.length).toBe(DEFAULT_REVIEW_QUESTIONS.length);
      expect(state.stats.totalQuestions).toBe(5);
      expect(state.stats.correct).toBe(3); // Q1 (Tarjan), Q3 (Matrix), Q5 (Haber)
      expect(state.stats.incorrect).toBe(1); // Q2 (Pipeline)
      expect(state.stats.unattempted).toBe(1); // Q4 (Crypto)
      expect(state.stats.rawScore).toBe(11.0); // 4 + (-1) + 4 + 0 + 4 = 11.0
      expect(state.stats.accuracyRate).toBe(75); // 3 correct out of 4 attempted = 75%
    });

    it('filters review questions by status (CORRECT, INCORRECT, UNATTEMPTED, CHALLENGED)', () => {
      controller.setStatusFilter('CORRECT');
      const correct = controller.getFilteredQuestions();
      expect(correct.length).toBe(3);
      expect(correct.every((q) => q.isCorrect)).toBe(true);

      controller.setStatusFilter('INCORRECT');
      const incorrect = controller.getFilteredQuestions();
      expect(incorrect.length).toBe(1);
      expect(incorrect[0].id).toBe('q-2');

      controller.setStatusFilter('UNATTEMPTED');
      const unattempted = controller.getFilteredQuestions();
      expect(unattempted.length).toBe(1);
      expect(unattempted[0].id).toBe('q-4');

      controller.setStatusFilter('CHALLENGED');
      expect(controller.getFilteredQuestions().length).toBe(0);
    });

    it('navigates next and previous questions safely', () => {
      controller.setStatusFilter('ALL');
      expect(controller.getState().currentIndex).toBe(0);

      controller.nextQuestion();
      expect(controller.getState().currentIndex).toBe(1);
      expect(controller.getCurrentQuestion()?.id).toBe('q-2');

      controller.prevQuestion();
      expect(controller.getState().currentIndex).toBe(0);
      expect(controller.getCurrentQuestion()?.id).toBe('q-1');

      // Prev at 0 should stay at 0
      controller.prevQuestion();
      expect(controller.getState().currentIndex).toBe(0);
    });

    it('calculates answer key challenge fees based on question count', () => {
      expect(controller.calculateChallengeFee(1)).toBe(200);
      expect(controller.calculateChallengeFee(3)).toBe(600);
      expect(controller.calculateChallengeFee(0)).toBe(0);
    });

    it('validates objection payload strictly before submission', () => {
      // 1. Missing justification
      const res1 = controller.submitChallenge({
        questionId: 'q-2',
        candidateId: '849202',
        reasonType: 'INCORRECT_PROVISIONAL_KEY',
        justificationText: 'too short',
        bookCitation: {
          bookTitle: 'Computer Architecture: A Quantitative Approach',
          author: 'Hennessy & Patterson',
          pageNumber: '142',
        },
        feeAmount: 200,
      });
      expect(res1.success).toBe(false);
      expect(res1.error).toContain('at least 10 characters');

      // 2. Missing textbook citation
      const res2 = controller.submitChallenge({
        questionId: 'q-2',
        candidateId: '849202',
        reasonType: 'INCORRECT_PROVISIONAL_KEY',
        justificationText: 'Operand forwarding eliminates RAW hazard without pipeline stalls.',
        bookCitation: {
          bookTitle: '',
          author: 'Hennessy & Patterson',
          pageNumber: '',
        },
        feeAmount: 200,
      });
      expect(res2.success).toBe(false);
      expect(res2.error).toContain('Standard textbook title');
    });

    it('successfully submits an answer key challenge and updates question & stats', () => {
      const payload: AnswerKeyChallengePayload = {
        questionId: 'q-2',
        candidateId: '849202',
        claimedCorrectOptionId: '3',
        reasonType: 'INCORRECT_PROVISIONAL_KEY',
        justificationText:
          'Hennessy and Patterson Section 3.2 clearly states RAW hazard occurs when operand forwarding is disabled.',
        bookCitation: {
          bookTitle: 'Computer Architecture: A Quantitative Approach',
          author: 'John L. Hennessy and David A. Patterson',
          editionOrYear: '6th Edition',
          pageNumber: '184',
        },
        feeAmount: 200,
      };

      const res = controller.submitChallenge(payload);
      expect(res.success).toBe(true);
      expect(res.record).toBeDefined();
      expect(res.record?.transactionReference).toContain('TXN-OBJ-');
      expect(res.record?.status).toBe('SUBMITTED');

      // Question should be marked challenged
      const question = controller.getState().attempt.questions.find((q) => q.id === 'q-2');
      expect(question?.isChallenged).toBe(true);
      expect(question?.challengeDetails?.transactionReference).toBe(res.record?.transactionReference);

      // Stats should reflect challengedCount
      expect(controller.getState().stats.challengedCount).toBe(1);

      // Filtering by CHALLENGED should return 1 question
      controller.setStatusFilter('CHALLENGED');
      expect(controller.getFilteredQuestions().length).toBe(1);
    });
  });

  // =========================================================================
  // 4. RESULTS CONTROLLER TESTS
  // =========================================================================
  describe('4. Results Controller (Score Normalization, AIR/Category Rank, Cutoffs, Ledger Verification)', () => {
    let controller: ResultsController;

    beforeEach(() => {
      controller = new ResultsController();
    });

    it('initializes with published scorecards and selects top scorecard', () => {
      const state = controller.getState();
      expect(state.resultsList.length).toBe(DEFAULT_EXAM_RESULTS.length);
      expect(state.selectedResult).not.toBeNull();
      expect(state.selectedResult?.examCode).toBe('NES-2026-S1');
      expect(state.selectedResult?.allIndiaRank).toBe(142);
      expect(state.selectedResult?.categoryRank).toBe(88);
      expect(state.selectedResult?.percentile).toBe(99.64);
      expect(state.selectedResult?.qualifyingStatus).toBe('QUALIFIED');
    });

    it('provides multi-shift score normalization data and section breakdown', () => {
      const result = controller.getState().selectedResult!;
      expect(result.isMultiShiftNormalized).toBe(true);
      expect(result.rawTotalMarks).toBe(268);
      expect(result.normalizedTotalMarks).toBe(271.45);
      expect(result.zScore).toBe(2.78);

      expect(result.sectionScores.length).toBe(3);
      expect(result.sectionScores[0].subject).toBe('Algorithms & Data Structures');
      expect(result.sectionScores[0].status).toBe('CLEARED');
      expect(result.sectionScores[0].accuracyRate).toBe(94);
    });

    it('evaluates category-wise qualifying cutoffs accurately', () => {
      const result = controller.getState().selectedResult!;

      // General category check (Score: 271.45 vs Cutoff: 180) -> Qualified by +91.45
      const genCheck = controller.checkCategoryQualification(result, 'GENERAL');
      expect(genCheck.qualified).toBe(true);
      expect(genCheck.cutoffRequired).toBe(180);
      expect(genCheck.marginMarks).toBeGreaterThan(90);

      // OBC check (Score: 271.45 vs Cutoff: 165)
      const obcCheck = controller.checkCategoryQualification(result, 'OBC_NCL');
      expect(obcCheck.qualified).toBe(true);
      expect(obcCheck.cutoffRequired).toBe(165);
    });

    it('verifies tamper-evident cryptographic ledger proof and Merkle root', () => {
      const result = controller.getState().selectedResult!;
      const verification = controller.verifyScorecardIntegrity(result);

      expect(verification.valid).toBe(true);
      expect(verification.algorithm).toBe('ECDSA_P256');
      expect(verification.digest).toContain('0x7f83b165');
      expect(verification.message).toContain('Merkle proof verified');
    });

    it('generates deterministic JSON verification QR payload for SignedQrCodeView', () => {
      const result = controller.getState().selectedResult!;
      const qrPayload = controller.generateSignedQrPayload(result);
      expect(qrPayload).toBeDefined();

      const parsed = JSON.parse(qrPayload);
      expect(parsed.id).toBe('res-nes-2026-01');
      expect(parsed.roll).toBe('849202');
      expect(parsed.air).toBe(142);
      expect(parsed.score).toBe(271.45);
      expect(parsed.pctl).toBe(99.64);
    });

    it('saves scorecard to offline wallet and updates state', () => {
      const result = controller.getState().selectedResult!;
      expect(result.savedToOfflineWallet).toBe(false);

      const saved = controller.saveScorecardToOfflineWallet(result.id);
      expect(saved).toBe(true);
      expect(controller.getState().selectedResult?.savedToOfflineWallet).toBe(true);
      expect(controller.getState().isSavedToWallet).toBe(true);
    });

    it('deposits academic scorecard credential to DigiLocker and issues receipt', async () => {
      const result = controller.getState().selectedResult!;
      const receipt = await controller.pushToDigiLocker(result.id);

      expect(receipt.status).toBe('ISSUED');
      expect(receipt.docId).toContain('DL-SCORECARD-');
      expect(receipt.txnId).toContain('TXN-DL-');
      expect(controller.getState().selectedResult?.digiLockerPushed).toBe(true);
      expect(controller.getState().digiLockerReceipt).toEqual(receipt);
    });

    it('switches between multiple published examination scorecards', () => {
      controller.selectResult('res-cse-2025-02');
      const selected = controller.getState().selectedResult;
      expect(selected?.examCode).toBe('CSE-PRE-2025');
      expect(selected?.conductingAuthority).toContain('UPSC');
      expect(selected?.allIndiaRank).toBe(1204);
      expect(selected?.percentile).toBe(98.85);
    });
  });
});
