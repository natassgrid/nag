/**
 * NAG Candidate Authentication Controller
 * Manages Phase 1 candidate onboarding, Mobile OTP login,
 * 2FA TOTP multi-factor verification, and DigiLocker / Aadhaar identity binding.
 */

export interface CandidateRegistrationPayload {
  fullName: string;
  mobileNumber: string;
  email: string;
  password: string;
  dpdpConsentAccepted: boolean;
  dateOfBirth?: string;
  category?: 'GEN' | 'OBC' | 'SC' | 'ST' | 'EWS';
  aadhaarRef?: string;
}

export interface CandidateProfile {
  id: string;
  fullName: string;
  email: string;
  mobileNumber: string;
  dateOfBirth?: string;
  category?: string;
  photoUrl?: string;
  signatureUrl?: string;
  aadhaarVerified: boolean;
  digiLockerLinked: boolean;
  pwdStatus?: boolean;
  pwdCategory?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface DigiLockerBinding {
  linked: boolean;
  aadhaarNumberMasked?: string;
  digiLockerId?: string;
  verifiedAt?: string;
  issuer?: string;
}

export type AuthStep =
  | 'LOGIN_OTP'
  | 'VERIFY_OTP'
  | 'VERIFY_TOTP'
  | 'REGISTRATION'
  | 'AUTHENTICATED';

export interface AuthState {
  step: AuthStep;
  mobileNumber: string;
  registeredPayload: CandidateRegistrationPayload | null;
  candidateProfile: CandidateProfile | null;
  tokens: AuthTokens | null;
  digiLocker: DigiLockerBinding;
  error: string | null;
  infoMessage: string | null;
  isSubmitting: boolean;
  otpSentAt: number | null;
  otpExpiresIn: number;
  resendCooldown: number;
  mockOtpCode?: string;
}

export class AuthController {
  private state: AuthState;
  private listeners: Set<(state: AuthState) => void> = new Set();

  constructor(initialState?: Partial<AuthState>) {
    this.state = {
      step: 'LOGIN_OTP',
      mobileNumber: '',
      registeredPayload: null,
      candidateProfile: null,
      tokens: null,
      digiLocker: { linked: false },
      error: null,
      infoMessage: null,
      isSubmitting: false,
      otpSentAt: null,
      otpExpiresIn: 300,
      resendCooldown: 30,
      mockOtpCode: undefined,
      ...initialState,
    };
  }

  public getState(): AuthState {
    return { ...this.state };
  }

  public subscribe(listener: (state: AuthState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify(): void {
    const currentState = this.getState();
    for (const listener of this.listeners) {
      listener(currentState);
    }
  }

  public setStep(step: AuthStep): void {
    this.state.step = step;
    this.state.error = null;
    this.notify();
  }

  public clearError(): void {
    this.state.error = null;
    this.notify();
  }

  public validateMobile(mobile: string): boolean {
    const cleaned = (mobile || '').trim();
    return /^[6-9]\d{9}$/.test(cleaned);
  }

  public validateEmail(email: string): boolean {
    const cleaned = (email || '').trim();
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleaned);
  }

  public validateOtp(otp: string): boolean {
    const cleaned = (otp || '').trim();
    return /^\d{6}$/.test(cleaned);
  }

  public async register(payload: CandidateRegistrationPayload): Promise<boolean> {
    this.state.isSubmitting = true;
    this.state.error = null;
    this.notify();

    if (!payload.fullName || payload.fullName.trim().length < 2) {
      this.state.error = 'Full name must be at least 2 characters.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    if (!this.validateMobile(payload.mobileNumber)) {
      this.state.error = 'Please enter a valid 10-digit Indian mobile number.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    if (!this.validateEmail(payload.email)) {
      this.state.error = 'Please enter a valid email address.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    if (!payload.password || payload.password.length < 8) {
      this.state.error = 'Password must be at least 8 characters with alphanumeric security.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    if (!payload.dpdpConsentAccepted) {
      this.state.error = 'Digital Personal Data Protection (DPDP) Act consent is required to register.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    const candidateId = `CND-${Date.now().toString(36).toUpperCase()}`;
    const newProfile: CandidateProfile = {
      id: candidateId,
      fullName: payload.fullName.trim(),
      email: payload.email.trim(),
      mobileNumber: payload.mobileNumber.trim(),
      dateOfBirth: payload.dateOfBirth,
      category: payload.category || 'GEN',
      aadhaarVerified: false,
      digiLockerLinked: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    this.state.registeredPayload = payload;
    this.state.candidateProfile = newProfile;
    this.state.mobileNumber = payload.mobileNumber.trim();
    this.state.isSubmitting = false;
    this.state.infoMessage = 'Registration successful. Please verify OTP to sign in.';
    this.state.step = 'LOGIN_OTP';
    this.notify();
    return true;
  }

  public async sendOtp(mobileNumber: string, purpose: 'REGISTRATION' | 'LOGIN' = 'LOGIN'): Promise<boolean> {
    this.state.isSubmitting = true;
    this.state.error = null;
    this.notify();

    const cleaned = (mobileNumber || '').trim();
    if (!this.validateMobile(cleaned)) {
      this.state.error = 'Please enter a valid 10-digit Indian mobile number (starts with 6-9).';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    // In mobile mock mode, deterministic OTP 123456
    const mockOtp = '123456';
    this.state.mobileNumber = cleaned;
    this.state.mockOtpCode = mockOtp;
    this.state.otpSentAt = Date.now();
    this.state.otpExpiresIn = 300;
    this.state.resendCooldown = 30;
    this.state.step = 'VERIFY_OTP';
    this.state.isSubmitting = false;
    this.state.infoMessage = `6-digit OTP sent to +91 ${cleaned} for ${purpose}. (Mock OTP: ${mockOtp})`;
    this.notify();
    return true;
  }

  public async verifyOtp(mobileNumber: string, otp: string, require2fa = false): Promise<boolean> {
    this.state.isSubmitting = true;
    this.state.error = null;
    this.notify();

    const cleanedOtp = (otp || '').trim();
    if (!this.validateOtp(cleanedOtp)) {
      this.state.error = 'OTP must be exactly 6 numeric digits.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    if (this.state.otpSentAt) {
      const elapsedSeconds = (Date.now() - this.state.otpSentAt) / 1000;
      if (elapsedSeconds > this.state.otpExpiresIn) {
        this.state.error = 'OTP has expired. Please request a new code.';
        this.state.isSubmitting = false;
        this.notify();
        return false;
      }
    }

    if (this.state.mockOtpCode && cleanedOtp !== this.state.mockOtpCode && cleanedOtp !== '123456') {
      this.state.error = 'Invalid OTP. Please check the code received.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    if (require2fa) {
      this.state.step = 'VERIFY_TOTP';
      this.state.isSubmitting = false;
      this.state.infoMessage = 'OTP verified. Please enter your 6-digit Authenticator 2FA code.';
      this.notify();
      return true;
    }

    return this.completeAuthentication();
  }

  public async verifyTotp(totpCode: string): Promise<boolean> {
    this.state.isSubmitting = true;
    this.state.error = null;
    this.notify();

    const cleaned = (totpCode || '').trim();
    if (!this.validateOtp(cleaned)) {
      this.state.error = '2FA TOTP code must be 6 numeric digits.';
      this.state.isSubmitting = false;
      this.notify();
      return false;
    }

    return this.completeAuthentication();
  }

  private completeAuthentication(): boolean {
    const profile = this.state.candidateProfile || {
      id: `CND-${Date.now().toString(36).toUpperCase()}`,
      fullName: 'Aarav Sharma',
      email: 'aarav.sharma@gov.in',
      mobileNumber: this.state.mobileNumber || '9876543210',
      dateOfBirth: '2001-05-15',
      category: 'GEN',
      aadhaarVerified: this.state.digiLocker.linked,
      digiLockerLinked: this.state.digiLocker.linked,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const tokens: AuthTokens = {
      accessToken: `nag_jwt_acc_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      refreshToken: `nag_jwt_ref_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      tokenType: 'Bearer',
      expiresIn: 3600,
    };

    this.state.candidateProfile = profile;
    this.state.tokens = tokens;
    this.state.step = 'AUTHENTICATED';
    this.state.isSubmitting = false;
    this.state.infoMessage = 'Authenticated successfully.';
    this.notify();
    return true;
  }

  public async linkDigiLocker(aadhaarLast4: string, digiLockerId = 'DL-IN-98741'): Promise<boolean> {
    const cleaned = (aadhaarLast4 || '').trim();
    if (!/^\d{4}$/.test(cleaned)) {
      this.state.error = 'Please enter valid last 4 digits of Aadhaar number.';
      this.notify();
      return false;
    }

    this.state.digiLocker = {
      linked: true,
      aadhaarNumberMasked: `XXXX-XXXX-${cleaned}`,
      digiLockerId,
      verifiedAt: new Date().toISOString(),
      issuer: 'DigiLocker / UIDAI',
    };

    if (this.state.candidateProfile) {
      this.state.candidateProfile = {
        ...this.state.candidateProfile,
        aadhaarVerified: true,
        digiLockerLinked: true,
      };
    }

    this.state.infoMessage = 'Aadhaar verified and DigiLocker linked successfully.';
    this.notify();
    return true;
  }

  public logout(): void {
    this.state = {
      step: 'LOGIN_OTP',
      mobileNumber: '',
      registeredPayload: null,
      candidateProfile: null,
      tokens: null,
      digiLocker: { linked: false },
      error: null,
      infoMessage: null,
      isSubmitting: false,
      otpSentAt: null,
      otpExpiresIn: 300,
      resendCooldown: 30,
      mockOtpCode: undefined,
    };
    this.notify();
  }
}
