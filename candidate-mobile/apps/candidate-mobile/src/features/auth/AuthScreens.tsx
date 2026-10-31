import React, { useState, useEffect } from 'react';
import {
  AuthController,
  AuthState,
  CandidateRegistrationPayload,
} from './auth-controller';

export interface AuthScreenProps {
  controller: AuthController;
  onAuthenticated?: () => void;
  onNavigateToRegister?: () => void;
  onNavigateToLogin?: () => void;
}

/**
 * Mobile OTP Login Screen (Phase 1)
 * 10-digit Indian Mobile entry, OTP trigger, 6-digit verification & Resend counter.
 */
export const MobileOtpLoginScreen: React.FC<AuthScreenProps> = ({
  controller,
  onAuthenticated,
  onNavigateToRegister,
}) => {
  const [state, setState] = useState<AuthState>(controller.getState());
  const [mobileInput, setMobileInput] = useState<string>(state.mobileNumber || '');
  const [otpInput, setOtpInput] = useState<string>('');
  const [require2fa, setRequire2fa] = useState<boolean>(false);

  useEffect(() => {
    return controller.subscribe((s: AuthState) => {
      setState(s);
      if (s.step === 'AUTHENTICATED' && onAuthenticated) {
        onAuthenticated();
      }
    });
  }, [controller, onAuthenticated]);

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    await controller.sendOtp(mobileInput, 'LOGIN');
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    await controller.verifyOtp(state.mobileNumber, otpInput, require2fa);
  };

  return (
    <div
      data-testid="mobile-otp-login-screen"
      style={{
        maxWidth: '420px',
        margin: '20px auto',
        padding: '24px',
        borderRadius: '12px',
        backgroundColor: '#FFFFFF',
        boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A365D', margin: '0 0 6px 0' }}>
          NAG Candidate Mobile
        </h2>
        <p style={{ fontSize: '13px', color: '#718096', margin: 0 }}>
          Digital Public Infrastructure for National Assessments
        </p>
      </div>

      {state.error && (
        <div
          data-testid="auth-error-banner"
          style={{
            padding: '10px 14px',
            marginBottom: '16px',
            backgroundColor: '#FFF5F5',
            color: '#C53030',
            borderRadius: '6px',
            fontSize: '13px',
            border: '1px solid #FEB2B2',
          }}
        >
          {state.error}
        </div>
      )}

      {state.infoMessage && !state.error && (
        <div
          data-testid="auth-info-banner"
          style={{
            padding: '10px 14px',
            marginBottom: '16px',
            backgroundColor: '#F0FFF4',
            color: '#276749',
            borderRadius: '6px',
            fontSize: '13px',
            border: '1px solid #9AE6B4',
          }}
        >
          {state.infoMessage}
        </div>
      )}

      {state.step === 'LOGIN_OTP' && (
        <form onSubmit={handleSendOtp} data-testid="send-otp-form">
          <div style={{ marginBottom: '16px' }}>
            <label
              htmlFor="mobile-input"
              style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '6px' }}
            >
              Registered Mobile Number
            </label>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <span
                style={{
                  padding: '10px 12px',
                  backgroundColor: '#EDF2F7',
                  border: '1px solid #CBD5E0',
                  borderRight: 'none',
                  borderRadius: '6px 0 0 6px',
                  fontSize: '14px',
                  color: '#4A5568',
                }}
              >
                +91
              </span>
              <input
                id="mobile-input"
                data-testid="mobile-number-input"
                type="tel"
                maxLength={10}
                placeholder="10-digit mobile number"
                value={mobileInput}
                onChange={(e) => setMobileInput(e.target.value.replace(/\D/g, ''))}
                style={{
                  flex: 1,
                  padding: '10px 12px',
                  border: '1px solid #CBD5E0',
                  borderRadius: '0 6px 6px 0',
                  fontSize: '14px',
                  outline: 'none',
                }}
              />
            </div>
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'flex', alignItems: 'center', fontSize: '13px', color: '#4A5568', cursor: 'pointer' }}>
              <input
                type="checkbox"
                data-testid="require-2fa-toggle"
                checked={require2fa}
                onChange={(e) => setRequire2fa(e.target.checked)}
                style={{ marginRight: '8px' }}
              />
              Require 2FA Authenticator TOTP after OTP
            </label>
          </div>

          <button
            data-testid="get-otp-button"
            type="submit"
            disabled={state.isSubmitting || mobileInput.length !== 10}
            style={{
              width: '100%',
              padding: '12px',
              backgroundColor: mobileInput.length === 10 ? '#2B6CB0' : '#A0AEC0',
              color: '#FFFFFF',
              fontWeight: '600',
              fontSize: '14px',
              border: 'none',
              borderRadius: '6px',
              cursor: mobileInput.length === 10 ? 'pointer' : 'not-allowed',
            }}
          >
            {state.isSubmitting ? 'Sending OTP...' : 'Get Mobile OTP'}
          </button>
        </form>
      )}

      {state.step === 'VERIFY_OTP' && (
        <form onSubmit={handleVerifyOtp} data-testid="verify-otp-form">
          <div style={{ marginBottom: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
              <label htmlFor="otp-input" style={{ fontSize: '13px', fontWeight: '600', color: '#2D3748' }}>
                Enter 6-Digit OTP
              </label>
              <span style={{ fontSize: '12px', color: '#718096' }}>
                Sent to +91 {state.mobileNumber}
              </span>
            </div>
            <input
              id="otp-input"
              data-testid="otp-code-input"
              type="text"
              maxLength={6}
              placeholder="123456"
              value={otpInput}
              onChange={(e) => setOtpInput(e.target.value.replace(/\D/g, ''))}
              style={{
                width: '100%',
                padding: '12px',
                border: '1px solid #CBD5E0',
                borderRadius: '6px',
                fontSize: '18px',
                letterSpacing: '6px',
                textAlign: 'center',
                outline: 'none',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <button
            data-testid="verify-otp-button"
            type="submit"
            disabled={state.isSubmitting || otpInput.length !== 6}
            style={{
              width: '100%',
              padding: '12px',
              backgroundColor: otpInput.length === 6 ? '#22543D' : '#A0AEC0',
              color: '#FFFFFF',
              fontWeight: '600',
              fontSize: '14px',
              border: 'none',
              borderRadius: '6px',
              cursor: otpInput.length === 6 ? 'pointer' : 'not-allowed',
              marginBottom: '12px',
            }}
          >
            {state.isSubmitting ? 'Verifying...' : 'Verify & Continue'}
          </button>

          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
            <button
              type="button"
              data-testid="change-mobile-button"
              onClick={() => controller.setStep('LOGIN_OTP')}
              style={{ background: 'none', border: 'none', color: '#2B6CB0', cursor: 'pointer', padding: 0 }}
            >
              Change Mobile
            </button>
            <button
              type="button"
              data-testid="resend-otp-button"
              onClick={() => controller.sendOtp(state.mobileNumber, 'LOGIN')}
              style={{ background: 'none', border: 'none', color: '#2B6CB0', cursor: 'pointer', padding: 0 }}
            >
              Resend OTP
            </button>
          </div>
        </form>
      )}

      {onNavigateToRegister && state.step === 'LOGIN_OTP' && (
        <div style={{ marginTop: '20px', textAlign: 'center', borderTop: '1px solid #E2E8F0', paddingTop: '16px' }}>
          <span style={{ fontSize: '13px', color: '#718096' }}>New Candidate? </span>
          <button
            type="button"
            data-testid="navigate-to-register-button"
            onClick={onNavigateToRegister}
            style={{
              background: 'none',
              border: 'none',
              color: '#2B6CB0',
              fontWeight: '600',
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            Register Here
          </button>
        </div>
      )}
    </div>
  );
};

/**
 * 2FA TOTP Verification Screen (Phase 1)
 */
export const TotpVerificationScreen: React.FC<AuthScreenProps> = ({
  controller,
  onAuthenticated,
}) => {
  const [state, setState] = useState<AuthState>(controller.getState());
  const [totpInput, setTotpInput] = useState<string>('');

  useEffect(() => {
    return controller.subscribe((s: AuthState) => {
      setState(s);
      if (s.step === 'AUTHENTICATED' && onAuthenticated) {
        onAuthenticated();
      }
    });
  }, [controller, onAuthenticated]);

  const handleVerifyTotp = async (e: React.FormEvent) => {
    e.preventDefault();
    await controller.verifyTotp(totpInput);
  };

  return (
    <div
      data-testid="totp-verification-screen"
      style={{
        maxWidth: '420px',
        margin: '20px auto',
        padding: '24px',
        borderRadius: '12px',
        backgroundColor: '#FFFFFF',
        boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#1A365D', margin: '0 0 6px 0' }}>
          Two-Factor Authentication
        </h2>
        <p style={{ fontSize: '13px', color: '#718096', margin: 0 }}>
          Enter the 6-digit TOTP code from your Authenticator app
        </p>
      </div>

      {state.error && (
        <div
          data-testid="totp-error-banner"
          style={{
            padding: '10px 14px',
            marginBottom: '16px',
            backgroundColor: '#FFF5F5',
            color: '#C53030',
            borderRadius: '6px',
            fontSize: '13px',
            border: '1px solid #FEB2B2',
          }}
        >
          {state.error}
        </div>
      )}

      <form onSubmit={handleVerifyTotp} data-testid="totp-form">
        <div style={{ marginBottom: '20px' }}>
          <input
            data-testid="totp-code-input"
            type="text"
            maxLength={6}
            placeholder="000000"
            value={totpInput}
            onChange={(e) => setTotpInput(e.target.value.replace(/\D/g, ''))}
            style={{
              width: '100%',
              padding: '14px',
              border: '1px solid #CBD5E0',
              borderRadius: '6px',
              fontSize: '22px',
              letterSpacing: '8px',
              textAlign: 'center',
              outline: 'none',
              boxSizing: 'border-box',
            }}
          />
        </div>

        <button
          data-testid="submit-totp-button"
          type="submit"
          disabled={state.isSubmitting || totpInput.length !== 6}
          style={{
            width: '100%',
            padding: '12px',
            backgroundColor: totpInput.length === 6 ? '#2B6CB0' : '#A0AEC0',
            color: '#FFFFFF',
            fontWeight: '600',
            fontSize: '14px',
            border: 'none',
            borderRadius: '6px',
            cursor: totpInput.length === 6 ? 'pointer' : 'not-allowed',
          }}
        >
          {state.isSubmitting ? 'Verifying 2FA...' : 'Verify & Access Grid'}
        </button>
      </form>
    </div>
  );
};

/**
 * Candidate Registration Screen (Phase 1)
 * Self-registration with DPDP Act 2023 Consent and Aadhaar/DigiLocker integration.
 */
export const CandidateRegistrationScreen: React.FC<AuthScreenProps> = ({
  controller,
  onNavigateToLogin,
}) => {
  const [state, setState] = useState<AuthState>(controller.getState());
  const [form, setForm] = useState<CandidateRegistrationPayload>({
    fullName: '',
    mobileNumber: '',
    email: '',
    password: '',
    dpdpConsentAccepted: false,
    category: 'GEN',
    dateOfBirth: '2002-01-01',
  });

  useEffect(() => {
    return controller.subscribe((s: AuthState) => {
      setState(s);
    });
  }, [controller]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const success = await controller.register(form);
    if (success && onNavigateToLogin) {
      onNavigateToLogin();
    }
  };

  return (
    <div
      data-testid="candidate-registration-screen"
      style={{
        maxWidth: '460px',
        margin: '20px auto',
        padding: '24px',
        borderRadius: '12px',
        backgroundColor: '#FFFFFF',
        boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A365D', margin: '0 0 6px 0' }}>
          Candidate Self-Registration
        </h2>
        <p style={{ fontSize: '13px', color: '#718096', margin: 0 }}>
          Create your verified NAG profile for national examination access
        </p>
      </div>

      {state.error && (
        <div
          data-testid="registration-error-banner"
          style={{
            padding: '10px 14px',
            marginBottom: '16px',
            backgroundColor: '#FFF5F5',
            color: '#C53030',
            borderRadius: '6px',
            fontSize: '13px',
            border: '1px solid #FEB2B2',
          }}
        >
          {state.error}
        </div>
      )}

      <form onSubmit={handleSubmit} data-testid="registration-form">
        <div style={{ marginBottom: '14px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '4px' }}>
            Full Legal Name (as per 10th / Matriculation Certificate)
          </label>
          <input
            data-testid="reg-fullname-input"
            type="text"
            required
            placeholder="e.g. Aarav Sharma"
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            style={{ width: '100%', padding: '10px', border: '1px solid #CBD5E0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ marginBottom: '14px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '4px' }}>
            Mobile Number (for OTP & Admit Card SMS)
          </label>
          <input
            data-testid="reg-mobile-input"
            type="tel"
            maxLength={10}
            required
            placeholder="10-digit mobile number"
            value={form.mobileNumber}
            onChange={(e) => setForm({ ...form, mobileNumber: e.target.value.replace(/\D/g, '') })}
            style={{ width: '100%', padding: '10px', border: '1px solid #CBD5E0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ marginBottom: '14px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '4px' }}>
            Email Address
          </label>
          <input
            data-testid="reg-email-input"
            type="email"
            required
            placeholder="candidate@example.com"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            style={{ width: '100%', padding: '10px', border: '1px solid #CBD5E0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ marginBottom: '14px' }}>
          <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '4px' }}>
            Account Password
          </label>
          <input
            data-testid="reg-password-input"
            type="password"
            required
            placeholder="Minimum 8 characters"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            style={{ width: '100%', padding: '10px', border: '1px solid #CBD5E0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ display: 'flex', gap: '12px', marginBottom: '14px' }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '4px' }}>
              Category
            </label>
            <select
              data-testid="reg-category-select"
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value as any })}
              style={{ width: '100%', padding: '10px', border: '1px solid #CBD5E0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
            >
              <option value="GEN">GEN (General)</option>
              <option value="OBC">OBC (Other Backward Class)</option>
              <option value="SC">SC (Scheduled Caste)</option>
              <option value="ST">ST (Scheduled Tribe)</option>
              <option value="EWS">EWS (Economically Weaker)</option>
            </select>
          </div>

          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#2D3748', marginBottom: '4px' }}>
              Date of Birth
            </label>
            <input
              data-testid="reg-dob-input"
              type="date"
              value={form.dateOfBirth}
              onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
              style={{ width: '100%', padding: '9px', border: '1px solid #CBD5E0', borderRadius: '6px', fontSize: '14px', boxSizing: 'border-box' }}
            />
          </div>
        </div>

        <div style={{ marginBottom: '18px', padding: '12px', backgroundColor: '#F7FAFC', borderRadius: '6px', border: '1px solid #E2E8F0' }}>
          <label style={{ display: 'flex', alignItems: 'flex-start', fontSize: '12px', color: '#4A5568', cursor: 'pointer' }}>
            <input
              type="checkbox"
              data-testid="dpdp-consent-checkbox"
              checked={form.dpdpConsentAccepted}
              onChange={(e) => setForm({ ...form, dpdpConsentAccepted: e.target.checked })}
              style={{ marginTop: '2px', marginRight: '8px' }}
            />
            <span>
              I accept the terms under the <strong>Digital Personal Data Protection (DPDP) Act 2023</strong> and authorize NAG to verify my credentials via DigiLocker / UIDAI Aadhaar for examination issuance.
            </span>
          </label>
        </div>

        <button
          data-testid="reg-submit-button"
          type="submit"
          disabled={state.isSubmitting}
          style={{
            width: '100%',
            padding: '12px',
            backgroundColor: '#2B6CB0',
            color: '#FFFFFF',
            fontWeight: '600',
            fontSize: '14px',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
          }}
        >
          {state.isSubmitting ? 'Registering...' : 'Complete Registration'}
        </button>
      </form>

      {onNavigateToLogin && (
        <div style={{ marginTop: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '13px', color: '#718096' }}>Already registered? </span>
          <button
            type="button"
            data-testid="back-to-login-button"
            onClick={onNavigateToLogin}
            style={{
              background: 'none',
              border: 'none',
              color: '#2B6CB0',
              fontWeight: '600',
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            Sign In with OTP
          </button>
        </div>
      )}
    </div>
  );
};
