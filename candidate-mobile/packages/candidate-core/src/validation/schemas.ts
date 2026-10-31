/**
 * NAG Zero-Dependency Validation Engine
 * Pure TypeScript validators for Candidate Onboarding, OTP/TOTP, and 3-Step Exam Applications.
 */

import {
  ApplyExamPayload,
  CandidateRegistrationPayload,
  MobileOtpVerifyPayload,
  TotpVerifyPayload,
} from '../models';

export interface ValidationResult<T> {
  success: boolean;
  data?: T;
  errors: Record<string, string>;
}

const INDIAN_MOBILE_REGEX = /^[6-9]\d{9}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SIX_DIGIT_OTP_REGEX = /^\d{6}$/;
const PASSWORD_UPPERCASE_REGEX = /[A-Z]/;
const PASSWORD_LOWERCASE_REGEX = /[a-z]/;
const PASSWORD_DIGIT_REGEX = /\d/;
const PASSWORD_SPECIAL_REGEX = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/;

/**
 * Validates candidate self-registration payload according to DPDP & security standards.
 */
export function validateRegistrationPayload(
  payload: unknown
): ValidationResult<CandidateRegistrationPayload> {
  const errors: Record<string, string> = {};

  if (!payload || typeof payload !== 'object') {
    return {
      success: false,
      errors: { _payload: 'Registration payload must be a non-null object' },
    };
  }

  const p = payload as Record<string, unknown>;

  // 1. Full Name
  const fullName = typeof p.fullName === 'string' ? p.fullName.trim() : '';
  if (!fullName) {
    errors.fullName = 'Full name is required';
  } else if (fullName.length < 2) {
    errors.fullName = 'Full name must be at least 2 characters';
  } else if (fullName.length > 100) {
    errors.fullName = 'Full name must not exceed 100 characters';
  }

  // 2. Mobile Number (Indian 10-digit starting with 6-9)
  const mobileNumber = typeof p.mobileNumber === 'string' ? p.mobileNumber.trim() : '';
  if (!mobileNumber) {
    errors.mobileNumber = 'Mobile number is required';
  } else if (!INDIAN_MOBILE_REGEX.test(mobileNumber)) {
    errors.mobileNumber = 'Enter a valid 10-digit Indian mobile number starting with 6-9';
  }

  // 3. Email
  const email = typeof p.email === 'string' ? p.email.trim() : '';
  if (!email) {
    errors.email = 'Email address is required';
  } else if (!EMAIL_REGEX.test(email)) {
    errors.email = 'Enter a valid email address';
  }

  // 4. Password Complexity
  const password = typeof p.password === 'string' ? p.password : '';
  if (!password) {
    errors.password = 'Password is required';
  } else {
    if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters long';
    } else if (!PASSWORD_UPPERCASE_REGEX.test(password)) {
      errors.password = 'Password must contain at least one uppercase letter (A-Z)';
    } else if (!PASSWORD_LOWERCASE_REGEX.test(password)) {
      errors.password = 'Password must contain at least one lowercase letter (a-z)';
    } else if (!PASSWORD_DIGIT_REGEX.test(password)) {
      errors.password = 'Password must contain at least one numeric digit (0-9)';
    } else if (!PASSWORD_SPECIAL_REGEX.test(password)) {
      errors.password = 'Password must contain at least one special character (!@#$%^&*)';
    }
  }

  // 5. DPDP Consent
  if (p.dpdpConsentAccepted !== true) {
    errors.dpdpConsentAccepted = 'Consent under the Digital Personal Data Protection (DPDP) Act is required';
  }

  // Optional fields validation
  const dateOfBirth = typeof p.dateOfBirth === 'string' ? p.dateOfBirth.trim() : undefined;
  const category = typeof p.category === 'string' ? p.category.trim() : undefined;
  const aadhaarRef = typeof p.aadhaarRef === 'string' ? p.aadhaarRef.trim() : undefined;

  const success = Object.keys(errors).length === 0;

  if (!success) {
    return { success: false, errors };
  }

  return {
    success: true,
    data: {
      fullName,
      mobileNumber,
      email,
      password,
      dpdpConsentAccepted: true,
      ...(dateOfBirth ? { dateOfBirth } : {}),
      ...(category ? { category } : {}),
      ...(aadhaarRef ? { aadhaarRef } : {}),
    },
    errors: {},
  };
}

/**
 * Validates Indian Mobile OTP payload (6-digit numeric OTP).
 */
export function validateMobileOtp(
  mobile: unknown,
  otp: unknown
): ValidationResult<MobileOtpVerifyPayload> {
  const errors: Record<string, string> = {};

  const mobileNumber = typeof mobile === 'string' ? mobile.trim() : '';
  if (!mobileNumber) {
    errors.mobileNumber = 'Mobile number is required';
  } else if (!INDIAN_MOBILE_REGEX.test(mobileNumber)) {
    errors.mobileNumber = 'Enter a valid 10-digit Indian mobile number starting with 6-9';
  }

  const otpCode = typeof otp === 'string' ? otp.trim() : '';
  if (!otpCode) {
    errors.otp = 'OTP is required';
  } else if (!SIX_DIGIT_OTP_REGEX.test(otpCode)) {
    errors.otp = 'OTP must be exactly 6 numeric digits';
  }

  if (Object.keys(errors).length > 0) {
    return { success: false, errors };
  }

  return {
    success: true,
    data: {
      mobileNumber,
      otp: otpCode,
    },
    errors: {},
  };
}

/**
 * Validates 2FA TOTP code (6-digit numeric token).
 */
export function validateTotpCode(totpCode: unknown): ValidationResult<TotpVerifyPayload> {
  const errors: Record<string, string> = {};

  let code = '';
  let sessionToken: string | undefined;
  let candidateId: string | undefined;

  if (typeof totpCode === 'string') {
    code = totpCode.trim();
  } else if (totpCode && typeof totpCode === 'object') {
    const obj = totpCode as Record<string, unknown>;
    code = typeof obj.totpCode === 'string' ? obj.totpCode.trim() : '';
    if (typeof obj.sessionToken === 'string') sessionToken = obj.sessionToken.trim();
    if (typeof obj.candidateId === 'string') candidateId = obj.candidateId.trim();
  }

  if (!code) {
    errors.totpCode = 'TOTP verification code is required';
  } else if (!SIX_DIGIT_OTP_REGEX.test(code)) {
    errors.totpCode = 'TOTP code must be exactly 6 numeric digits';
  }

  if (Object.keys(errors).length > 0) {
    return { success: false, errors };
  }

  return {
    success: true,
    data: {
      totpCode: code,
      ...(sessionToken ? { sessionToken } : {}),
      ...(candidateId ? { candidateId } : {}),
    },
    errors: {},
  };
}

/**
 * Validates 3-step examination application payload.
 * Enforces instructions acceptance, 3 mutually distinct centre preferences, shift, and PwD/scribe rules.
 */
export function validateExamApplication(
  payload: unknown
): ValidationResult<ApplyExamPayload> {
  const errors: Record<string, string> = {};

  if (!payload || typeof payload !== 'object') {
    return {
      success: false,
      errors: { _payload: 'Application payload must be a non-null object' },
    };
  }

  const p = payload as Record<string, unknown>;

  // Step 0: Exam ID
  const examId = typeof p.examId === 'string' ? p.examId.trim() : '';
  if (!examId) {
    errors.examId = 'Examination ID is required';
  }

  // Step 1: Instructions & Eligibility Declaration
  if (p.instructionsAccepted !== true) {
    errors.instructionsAccepted = 'You must read and accept the instructions and eligibility declaration';
  }

  // Step 2: Centre Preferences & Shift Preference
  const firstChoiceCentreId =
    typeof p.firstChoiceCentreId === 'string' ? p.firstChoiceCentreId.trim() : '';
  const secondChoiceCentreId =
    typeof p.secondChoiceCentreId === 'string' ? p.secondChoiceCentreId.trim() : '';
  const thirdChoiceCentreId =
    typeof p.thirdChoiceCentreId === 'string' ? p.thirdChoiceCentreId.trim() : '';

  if (!firstChoiceCentreId) {
    errors.firstChoiceCentreId = 'First choice examination centre is required';
  }
  if (!secondChoiceCentreId) {
    errors.secondChoiceCentreId = 'Second choice examination centre is required';
  }
  if (!thirdChoiceCentreId) {
    errors.thirdChoiceCentreId = 'Third choice examination centre is required';
  }

  // Enforce 3 mutually distinct centres
  if (firstChoiceCentreId && secondChoiceCentreId && thirdChoiceCentreId) {
    const centres = [firstChoiceCentreId, secondChoiceCentreId, thirdChoiceCentreId];
    const uniqueCentres = new Set(centres);
    if (uniqueCentres.size !== 3) {
      errors.centreChoices = 'All 3 examination centre preferences must be distinct';
      if (firstChoiceCentreId === secondChoiceCentreId) {
        errors.secondChoiceCentreId = 'Second choice centre must differ from first choice';
      }
      if (firstChoiceCentreId === thirdChoiceCentreId || secondChoiceCentreId === thirdChoiceCentreId) {
        errors.thirdChoiceCentreId = 'Third choice centre must differ from first and second choices';
      }
    }
  }

  const preferredShiftId =
    typeof p.preferredShiftId === 'string' ? p.preferredShiftId.trim() : '';
  if (!preferredShiftId) {
    errors.preferredShiftId = 'Preferred examination shift must be selected';
  }

  // Step 3: PwD & Scribe Accommodations
  const pwdRequired = Boolean(p.pwdRequired);
  let scribeRequired: boolean | undefined =
    p.scribeRequired !== undefined ? Boolean(p.scribeRequired) : undefined;
  const pwdCategory =
    typeof p.pwdCategory === 'string' ? p.pwdCategory.trim() : undefined;
  const scribeQualification =
    typeof p.scribeQualification === 'string' ? p.scribeQualification.trim() : undefined;

  if (pwdRequired) {
    if (!pwdCategory) {
      errors.pwdCategory = 'PwD category is required when disability accommodation is requested';
    }
    if (scribeRequired) {
      if (!scribeQualification) {
        errors.scribeQualification =
          'Scribe qualification details must be provided when scribe assistance is requested';
      }
    }
  } else {
    // If pwdRequired is false, scribe cannot be requested
    if (scribeRequired === true) {
      errors.scribeRequired = 'Scribe assistance is only available for candidates requiring PwD accommodations';
    }
    scribeRequired = false;
  }

  if (Object.keys(errors).length > 0) {
    return { success: false, errors };
  }

  return {
    success: true,
    data: {
      examId,
      firstChoiceCentreId,
      secondChoiceCentreId,
      thirdChoiceCentreId,
      preferredShiftId,
      instructionsAccepted: true,
      pwdRequired,
      scribeRequired,
      ...(pwdCategory ? { pwdCategory } : {}),
      ...(scribeQualification ? { scribeQualification } : {}),
    },
    errors: {},
  };
}
