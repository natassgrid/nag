import {
  NativeKioskLockdown,
  NativeScreenSecurity,
  NativeHardwareKeystore,
  NativeProctoringCompanion,
  DEFAULT_AUDIO_THRESHOLD_DB,
  DEFAULT_ORIENTATION_THRESHOLD_DEG,
} from '../native';

import {
  FcmNotificationService,
  fcmNotificationService,
  NOTIFICATION_CHANNELS,
} from '../services/fcm-notification.service';

import {
  SecureWalletStorage,
  secureWalletStorage,
  AdmitCardRecord,
} from '../services/secure-wallet.storage';

describe('Phase 3 Bridgeless TurboModules & Security Layer', () => {
  describe('NativeKioskLockdown', () => {
    it('should query initial lock task mode state', async () => {
      const status = await NativeKioskLockdown.getLockTaskModeState();
      expect(status).toBeDefined();
      expect(typeof status.isLockdownActive).toBe('boolean');
      expect(['NONE', 'LOCKED', 'PINNED']).toContain(status.lockTaskMode);
    });

    it('should activate kiosk lockdown with single-app mode and overlay suppression', async () => {
      const result = await NativeKioskLockdown.startLockdown();
      expect(result.isLockdownActive).toBe(true);
      expect(result.lockTaskMode).toBe('LOCKED');
      expect(result.overlaySuppressed).toBe(true);

      const stateAfter = await NativeKioskLockdown.getLockTaskModeState();
      expect(stateAfter.isLockdownActive).toBe(true);
    });

    it('should deactivate kiosk lockdown and restore default state', async () => {
      const result = await NativeKioskLockdown.stopLockdown();
      expect(result.isLockdownActive).toBe(false);
      expect(result.lockTaskMode).toBe('NONE');
      expect(result.overlaySuppressed).toBe(false);
    });

    it('should verify Device Owner status and overlay suppression', async () => {
      const isOwner = await NativeKioskLockdown.isDeviceOwner();
      expect(typeof isOwner).toBe('boolean');

      const suppressed = await NativeKioskLockdown.suppressOverlays();
      expect(suppressed).toBe(true);
    });
  });

  describe('NativeScreenSecurity (FLAG_SECURE)', () => {
    it('should enable FLAG_SECURE on the UI window', async () => {
      const res = await NativeScreenSecurity.enableFlagSecure();
      expect(res.secure).toBe(true);
      expect(res.timestamp).toBeGreaterThan(0);

      const status = await NativeScreenSecurity.getScreenSecurityStatus();
      expect(status.secure).toBe(true);
    });

    it('should disable FLAG_SECURE on the UI window', async () => {
      const res = await NativeScreenSecurity.disableFlagSecure();
      expect(res.secure).toBe(false);
      expect(res.timestamp).toBeGreaterThan(0);

      const status = await NativeScreenSecurity.getScreenSecurityStatus();
      expect(status.secure).toBe(false);
    });
  });

  describe('NativeHardwareKeystore (StrongBox & ECDSA P-256)', () => {
    const TEST_ALIAS = 'NAG_CBT_TEST_KEY';

    it('should ensure hardware key pair generation in Android KeyStore', async () => {
      const keyInfo = await NativeHardwareKeystore.ensureHardwareKeyPair(TEST_ALIAS);
      expect(keyInfo.alias).toBe(TEST_ALIAS);
      expect(keyInfo.algorithm).toBe('EC/secp256r1');
      expect(keyInfo.publicKeyBase64).toBeDefined();
      expect(keyInfo.isHardwareBacked).toBe(true);
      expect(keyInfo.isStrongBoxBacked).toBe(true);
      expect(keyInfo.certificateChainLength).toBeGreaterThanOrEqual(1);
    });

    it('should retrieve existing key information', async () => {
      const info = await NativeHardwareKeystore.getHardwareKeyInfo(TEST_ALIAS);
      expect(info).not.toBeNull();
      expect(info?.alias).toBe(TEST_ALIAS);
      expect(info?.algorithm).toBe('EC/secp256r1');
    });

    it('should sign CBT exam submission payload with SHA256withECDSA', async () => {
      const payloadHash = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
      const signatureResult = await NativeHardwareKeystore.signExamSubmission(
        payloadHash,
        TEST_ALIAS
      );

      expect(signatureResult.alias).toBe(TEST_ALIAS);
      expect(signatureResult.payloadHash).toBe(payloadHash);
      expect(signatureResult.algorithm).toBe('SHA256withECDSA');
      expect(signatureResult.signatureBase64).toBeDefined();
      expect(signatureResult.signatureBase64.length).toBeGreaterThan(20);
      expect(signatureResult.strongBoxBacked).toBe(true);
    });
  });

  describe('NativeProctoringCompanion (Audio dB & Posture Anomaly Detection)', () => {
    beforeAll(async () => {
      await NativeProctoringCompanion.startSensorMonitoring(
        DEFAULT_AUDIO_THRESHOLD_DB,
        DEFAULT_ORIENTATION_THRESHOLD_DEG
      );
    });

    afterAll(async () => {
      await NativeProctoringCompanion.stopSensorMonitoring();
    });

    it('should report no violations for quiet ambient noise and upright posture', async () => {
      const sample = await NativeProctoringCompanion.evaluateSensorSample({
        ambientNoiseDb: 42.0,
        pitchDeg: 12.0,
        rollDeg: 6.0,
        azimuthDeg: 90.0,
      });

      expect(sample.noiseViolation).toBe(false);
      expect(sample.postureViolation).toBe(false);
      expect(sample.ambientNoiseDb).toBe(42.0);
    });

    it('should flag noise violation when ambient decibels exceed 65 dB', async () => {
      const sample = await NativeProctoringCompanion.evaluateSensorSample({
        ambientNoiseDb: 76.5,
        pitchDeg: 15.0,
        rollDeg: 8.0,
      });

      expect(sample.noiseViolation).toBe(true);
      expect(sample.postureViolation).toBe(false);
    });

    it('should flag posture violation when candidate tilts device beyond 45 degrees pitch', async () => {
      const sample = await NativeProctoringCompanion.evaluateSensorSample({
        ambientNoiseDb: 40.0,
        pitchDeg: 55.0,
        rollDeg: 10.0,
      });

      expect(sample.noiseViolation).toBe(false);
      expect(sample.postureViolation).toBe(true);
    });

    it('should flag posture violation when candidate rolls device beyond 45 degrees roll', async () => {
      const sample = await NativeProctoringCompanion.evaluateSensorSample({
        ambientNoiseDb: 40.0,
        pitchDeg: 10.0,
        rollDeg: -52.0,
      });

      expect(sample.postureViolation).toBe(true);
    });
  });
});

describe('Mobile Services Layer', () => {
  describe('FcmNotificationService', () => {
    it('should initialize high-priority notification channels', async () => {
      await expect(
        fcmNotificationService.initializeNotificationChannels()
      ).resolves.not.toThrow();

      expect(NOTIFICATION_CHANNELS.EXAM_GATE_ALERTS).toBe('exam-gate-alerts');
      expect(NOTIFICATION_CHANNELS.ADMIT_CARD_WALLET).toBe('admit-card-wallet');
      expect(NOTIFICATION_CHANNELS.APPLICATION_DEADLINES).toBe('application-deadlines');
    });

    it('should schedule gate closing countdown alert', async () => {
      const gateClosingTime = new Date(Date.now() + 45 * 60 * 1000);
      const alertId = await fcmNotificationService.scheduleGateClosingCountdown({
        examName: 'UPSC CSE Prelims 2026',
        gateClosingTime,
        venueName: 'Kendriya Vidyalaya No. 1, New Delhi',
        minutesBeforeGateClose: 30,
      });

      expect(alertId).toBeDefined();
      expect(alertId).toContain('gate-alert-');
    });

    it('should send urgent shift change alert', async () => {
      const alertId = await fcmNotificationService.sendShiftChangeAlert({
        examName: 'SSC CGL Tier 1',
        oldShift: 'Shift 1 (09:00 AM)',
        newShift: 'Shift 2 (12:30 PM)',
        reason: 'Administrative centre reallocation',
      });

      expect(alertId).toBeDefined();
      expect(alertId).toContain('shift-change-');
    });

    it('should send admit card issued alert and handle cancellation', async () => {
      const alertId = await fcmNotificationService.sendAdmitCardIssuedAlert(
        'JEE Advanced 2026',
        '2601004589'
      );
      expect(alertId).toBeDefined();

      await expect(fcmNotificationService.cancelAlert(alertId)).resolves.not.toThrow();
    });
  });

  describe('SecureWalletStorage (Encrypted Hall Ticket Wallet)', () => {
    const mockAdmitCard: AdmitCardRecord = {
      admitCardId: 'AC-2026-NAG-99881',
      candidateId: 'CAND-00192',
      candidateName: 'Aarav Sharma',
      rollNumber: '2604001923',
      examCode: 'NTA-NEET-UG-2026',
      examTitle: 'National Eligibility cum Entrance Test (UG) 2026',
      examDate: '2026-05-03',
      shift: 'Morning (09:30 AM - 12:30 PM)',
      reportingTime: '08:00 AM',
      gateClosingTime: '09:00 AM',
      centreCode: 'DL0104',
      centreName: 'Delhi Technological University Examination Centre',
      centreAddress: 'Shahbad Daulatpur, Main Bawana Road, Delhi 110042',
      qrPayload: 'NAG:AC:2026-NAG-99881:2604001923:DL0104',
      qrDigitalSignature: 'MEYCIQDx8493821039801823091238091283019283012983109283019238',
      photoUrl: 'https://cdn.nag.gov.in/photos/cand-00192.jpg',
      storedAt: Date.now(),
    };

    it('should save admit card offline in encrypted wallet', async () => {
      await expect(
        secureWalletStorage.saveAdmitCardOffline(mockAdmitCard)
      ).resolves.not.toThrow();
    });

    it('should retrieve all offline admit cards', async () => {
      const cards = await secureWalletStorage.getOfflineAdmitCards();
      expect(cards.length).toBeGreaterThanOrEqual(1);
      const found = cards.find((c) => c.admitCardId === mockAdmitCard.admitCardId);
      expect(found).toBeDefined();
      expect(found?.candidateName).toBe('Aarav Sharma');
      expect(found?.rollNumber).toBe('2604001923');
    });

    it('should retrieve admit card by ID', async () => {
      const card = await secureWalletStorage.getOfflineAdmitCardById(
        mockAdmitCard.admitCardId
      );
      expect(card).not.toBeNull();
      expect(card?.admitCardId).toBe(mockAdmitCard.admitCardId);
      expect(card?.centreCode).toBe('DL0104');
    });

    it('should cryptographically verify stored genuine admit card', async () => {
      const verification = await secureWalletStorage.verifyStoredAdmitCard(
        mockAdmitCard.admitCardId
      );
      expect(verification.valid).toBe(true);
      expect(verification.admitCard?.rollNumber).toBe('2604001923');
    });

    it('should fail verification for non-existent admit card', async () => {
      const verification = await secureWalletStorage.verifyStoredAdmitCard(
        'NON-EXISTENT-CARD'
      );
      expect(verification.valid).toBe(false);
      expect(verification.reason).toContain('not found');
    });

    it('should remove admit card from offline wallet and clean up keys', async () => {
      const removed = await secureWalletStorage.removeAdmitCard(
        mockAdmitCard.admitCardId
      );
      expect(removed).toBe(true);

      const cardAfter = await secureWalletStorage.getOfflineAdmitCardById(
        mockAdmitCard.admitCardId
      );
      expect(cardAfter).toBeNull();
    });
  });
});
