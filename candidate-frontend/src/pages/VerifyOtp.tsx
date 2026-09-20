/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { Mail, Phone, RefreshCw, CheckCircle, AlertTriangle, ArrowRight, ShieldCheck, RotateCcw } from 'lucide-react';
import { authService } from '../services/authService';
import { tokenManager } from '../utils/tokenManager';

const OTP_LENGTH = 6;
const COOLDOWN_SECONDS = 30;

export const VerifyOtp: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  // State from navigation or local storage
  const stateData = location.state as {
    email?: string;
    mobile?: string;
    userId?: string;
    requiresVerification?: boolean;
    activeTab?: 'email' | 'mobile';
  } | null;

  const email = stateData?.email || localStorage.getItem('pending_email') || '';
  const mobile = stateData?.mobile || localStorage.getItem('pending_mobile') || '';
  const userId =
    stateData?.userId ||
    sessionStorage.getItem('nag_pending_user_id') ||
    tokenManager.getUserId() ||
    '';

  const [activeTab, setActiveTab] = useState<'email' | 'mobile'>(
    stateData?.activeTab || 'email'
  );

  const [emailDigits, setEmailDigits] = useState<string[]>(Array(OTP_LENGTH).fill(''));
  const [mobileDigits, setMobileDigits] = useState<string[]>(Array(OTP_LENGTH).fill(''));

  const [emailVerified, setEmailVerified] = useState(false);
  const [mobileVerified, setMobileVerified] = useState(false);

  const [emailCooldown, setEmailCooldown] = useState(COOLDOWN_SECONDS);
  const [smsCooldown, setSmsCooldown] = useState(COOLDOWN_SECONDS);

  const [smsRemaining, setSmsRemaining] = useState<number>(3);
  const [rateLimitMessage, setRateLimitMessage] = useState<string | null>(null);

  const [isVerifying, setIsVerifying] = useState(false);
  const [isResendingEmail, setIsResendingEmail] = useState(false);
  const [isResendingSms, setIsResendingSms] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [otpSentTo, setOtpSentTo] = useState<{ email?: string; mobile?: string }>({
    email,
    mobile,
  });

  const emailInputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const mobileInputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Smooth completion handler
  const handleProceed = useCallback(() => {
    // Clear pending registration markers
    localStorage.removeItem('pending_email');
    localStorage.removeItem('pending_mobile');
    sessionStorage.removeItem('nag_pending_user_id');
    sessionStorage.removeItem('nag_pending_mobile');

    if (tokenManager.isAuthenticated()) {
      navigate('/dashboard', { replace: true });
    } else {
      navigate('/login', {
        state: { message: 'Account verified successfully! Please sign in to proceed.' },
        replace: true,
      });
    }
  }, [navigate]);

  // Load verification status on mount
  useEffect(() => {
    if (!userId && !email) {
      navigate('/register', { replace: true });
      return;
    }

    const checkStatus = async () => {
      if (!userId) return;
      try {
        const status = await authService.getVerificationStatus(userId);
        setEmailVerified(status.emailVerified);
        setMobileVerified(status.mobileVerified);
        setSmsRemaining(status.smsRemainingThisWeek ?? 3);

        if (status.emailVerified && status.mobileVerified) {
          handleProceed();
        } else if (status.emailVerified && !status.mobileVerified) {
          setActiveTab('mobile');
        }
      } catch {
        // Status fetch failed; continue with defaults
      }
    };

    checkStatus();
  }, [userId, email, navigate, handleProceed]);

  // Cooldown Timers
  useEffect(() => {
    if (emailCooldown <= 0) return;
    const timer = setInterval(() => setEmailCooldown((prev) => prev - 1), 1000);
    return () => clearInterval(timer);
  }, [emailCooldown]);

  useEffect(() => {
    if (smsCooldown <= 0) return;
    const timer = setInterval(() => setSmsCooldown((prev) => prev - 1), 1000);
    return () => clearInterval(timer);
  }, [smsCooldown]);

  // Input Handling
  const handleDigitChange = (
    channel: 'email' | 'mobile',
    index: number,
    value: string
  ) => {
    // Handle paste of full 6-digit OTP
    if (value.length > 1) {
      const digits = value.replace(/\D/g, '').slice(0, OTP_LENGTH).split('');
      if (channel === 'email') {
        const newDigits = [...emailDigits];
        digits.forEach((d, i) => { if (i < OTP_LENGTH) newDigits[i] = d; });
        setEmailDigits(newDigits);
        const nextIndex = Math.min(digits.length, OTP_LENGTH - 1);
        emailInputRefs.current[nextIndex]?.focus();
      } else {
        const newDigits = [...mobileDigits];
        digits.forEach((d, i) => { if (i < OTP_LENGTH) newDigits[i] = d; });
        setMobileDigits(newDigits);
        const nextIndex = Math.min(digits.length, OTP_LENGTH - 1);
        mobileInputRefs.current[nextIndex]?.focus();
      }
      return;
    }

    const digit = value.replace(/\D/g, '');
    if (channel === 'email') {
      const newDigits = [...emailDigits];
      newDigits[index] = digit;
      setEmailDigits(newDigits);
      if (digit && index < OTP_LENGTH - 1) {
        emailInputRefs.current[index + 1]?.focus();
      }
    } else {
      const newDigits = [...mobileDigits];
      newDigits[index] = digit;
      setMobileDigits(newDigits);
      if (digit && index < OTP_LENGTH - 1) {
        mobileInputRefs.current[index + 1]?.focus();
      }
    }
  };

  const handleKeyDown = (
    channel: 'email' | 'mobile',
    index: number,
    e: React.KeyboardEvent<HTMLInputElement>
  ) => {
    const digits = channel === 'email' ? emailDigits : mobileDigits;
    const refs = channel === 'email' ? emailInputRefs : mobileInputRefs;

    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      refs.current[index - 1]?.focus();
    }
  };

  const handleRetryInput = (channel: 'email' | 'mobile') => {
    setError(null);
    if (channel === 'email') {
      setEmailDigits(Array(OTP_LENGTH).fill(''));
      emailInputRefs.current[0]?.focus();
    } else {
      setMobileDigits(Array(OTP_LENGTH).fill(''));
      mobileInputRefs.current[0]?.focus();
    }
  };

  // Verify Email OTP
  const handleVerifyEmail = async () => {
    const otp = emailDigits.join('');
    if (otp.length < OTP_LENGTH) {
      setError('Please enter the complete 6-digit email OTP.');
      return;
    }

    if (!userId) {
      setError('Session expired. Please register or log in again.');
      return;
    }

    setIsVerifying(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const res = await authService.verifyEmail({ userId, otp });
      if (res.emailVerified) {
        setEmailVerified(true);
        setSuccessMessage('Email verified successfully!');
        if (res.fullyVerified || !mobile) {
          setTimeout(() => handleProceed(), 1500);
        } else {
          setTimeout(() => {
            setActiveTab('mobile');
            setSuccessMessage(null);
          }, 1200);
        }
      } else {
        setError('Invalid email OTP. Please try again.');
        handleRetryInput('email');
      }
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      const msg = e?.response?.data?.message || e?.message || 'Verification failed. Please check the OTP and try again.';
      setError(msg);
      handleRetryInput('email');
    } finally {
      setIsVerifying(false);
    }
  };

  // Verify Mobile OTP
  const handleVerifyMobile = async () => {
    const otp = mobileDigits.join('');
    if (otp.length < OTP_LENGTH) {
      setError('Please enter the complete 6-digit mobile OTP.');
      return;
    }

    if (!userId) {
      setError('Session expired. Please register or log in again.');
      return;
    }

    setIsVerifying(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const res = await authService.verifyMobile({ userId, otp });
      if (res.mobileVerified) {
        setMobileVerified(true);
        setSuccessMessage('Mobile number verified successfully!');
        setTimeout(() => handleProceed(), 1500);
      } else {
        setError('Invalid mobile OTP. Please try again.');
        handleRetryInput('mobile');
      }
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      const msg = e?.response?.data?.message || e?.message || 'Verification failed. Please check the OTP and try again.';
      setError(msg);
      handleRetryInput('mobile');
    } finally {
      setIsVerifying(false);
    }
  };

  // Resend Email OTP
  const handleResendEmail = async () => {
    if (emailCooldown > 0 || isResendingEmail || !userId) return;
    setIsResendingEmail(true);
    setError(null);

    try {
      await authService.resendEmailOtp(userId);
      setEmailCooldown(COOLDOWN_SECONDS);
      setEmailDigits(Array(OTP_LENGTH).fill(''));
      setSuccessMessage('A fresh verification code has been sent to your email.');
      setOtpSentTo((prev) => ({ ...prev, email }));
      emailInputRefs.current[0]?.focus();
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e?.response?.data?.message || 'Failed to resend email OTP. Please try again.');
    } finally {
      setIsResendingEmail(false);
    }
  };

  // Resend SMS OTP
  const handleResendSms = async () => {
    if (smsCooldown > 0 || isResendingSms || smsRemaining <= 0 || !userId) return;
    setIsResendingSms(true);
    setError(null);
    setRateLimitMessage(null);

    try {
      await authService.resendSmsOtp(userId);
      setSmsCooldown(COOLDOWN_SECONDS);
      setMobileDigits(Array(OTP_LENGTH).fill(''));
      setSuccessMessage('A fresh OTP has been sent via SMS.');
      mobileInputRefs.current[0]?.focus();
    } catch (err: unknown) {
      const e = err as { response?: { status?: number; data?: { message?: string; smsRemainingThisWeek?: number } }; message?: string };
      if (e?.response?.status === 429) {
        setSmsRemaining(0);
        setRateLimitMessage(
          e.response.data?.message ||
          'Weekly SMS OTP limit reached (3/week). You can verify your email to complete registration.'
        );
      } else {
        setError(e?.response?.data?.message || 'Failed to resend SMS OTP. Please try again.');
      }
    } finally {
      setIsResendingSms(false);
    }
  };

  const allVerified = emailVerified && (mobileVerified || !mobile);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl p-8 shadow-2xl text-white text-center">
        {/* Header Icon */}
        <div className="w-16 h-16 bg-indigo-500/20 border border-indigo-400/40 rounded-full flex items-center justify-center mx-auto mb-4">
          <ShieldCheck className="w-8 h-8 text-indigo-400" />
        </div>

        <h1 className="text-2xl font-bold mb-1">Verify Your Account</h1>
        <p className="text-sm text-slate-300 mb-6">
          Complete the verification steps below to activate your National Assessment Grid candidate profile.
        </p>

        {/* Tab Selection */}
        <div className="flex bg-slate-800/80 rounded-xl p-1 mb-6 gap-1 border border-slate-700">
          <button
            type="button"
            onClick={() => { setActiveTab('email'); setError(null); }}
            className={`flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg text-xs font-semibold transition ${
              activeTab === 'email'
                ? 'bg-indigo-600 text-white shadow'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Mail className="w-3.5 h-3.5" />
            <span>Email (Required)</span>
            {emailVerified && <CheckCircle className="w-3.5 h-3.5 text-green-400 ml-1" />}
          </button>

          <button
            type="button"
            onClick={() => { setActiveTab('mobile'); setError(null); }}
            className={`flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg text-xs font-semibold transition ${
              activeTab === 'mobile'
                ? 'bg-indigo-600 text-white shadow'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Phone className="w-3.5 h-3.5" />
            <span>Mobile SMS (Optional)</span>
            {mobileVerified && <CheckCircle className="w-3.5 h-3.5 text-green-400 ml-1" />}
          </button>
        </div>

        {/* Alerts */}
        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 mb-4 text-left flex items-start justify-between gap-2.5 text-xs text-red-300 animate-shake">
            <div className="flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
            <button
              type="button"
              onClick={() => handleRetryInput(activeTab)}
              className="text-red-200 hover:text-white font-medium flex items-center gap-1 flex-shrink-0 underline text-xs ml-2"
            >
              <RotateCcw className="w-3 h-3" />
              Retry
            </button>
          </div>
        )}

        {successMessage && (
          <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-3 mb-4 text-left flex gap-2.5 text-xs text-green-300">
            <CheckCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            <span>{successMessage}</span>
          </div>
        )}

        {/* Tab Contents */}
        <div className="mb-6">
          {/* Email Tab */}
          {activeTab === 'email' && (
            <div>
              <p className="text-xs text-slate-300 mb-2">
                Enter the 6-digit code sent to:
              </p>
              <div className="text-xs font-mono text-indigo-300 mb-4 bg-indigo-950/40 py-1.5 px-3 rounded inline-block">
                ✉️ {otpSentTo.email || email}
              </div>

              {emailVerified ? (
                <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-4 mb-4 flex flex-col items-center justify-center gap-2 text-green-400 text-sm">
                  <div className="flex items-center gap-2">
                    <CheckCircle className="w-5 h-5" />
                    <span>Email address is verified</span>
                  </div>
                  <button
                    onClick={handleProceed}
                    className="mt-2 bg-green-600 hover:bg-green-700 text-white text-xs font-semibold py-2 px-4 rounded-lg flex items-center gap-1.5 transition"
                  >
                    <span>Proceed to Dashboard</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              ) : (
                <>
                  <div className="flex justify-center gap-2 mb-3" role="group" aria-label="Email OTP input">
                    {emailDigits.map((digit, i) => (
                      <input
                        key={i}
                        ref={(el) => { emailInputRefs.current[i] = el; }}
                        type="text"
                        inputMode="numeric"
                        maxLength={6}
                        value={digit}
                        onChange={(e) => handleDigitChange('email', i, e.target.value)}
                        onKeyDown={(e) => handleKeyDown('email', i, e)}
                        onFocus={(e) => e.target.select()}
                        aria-label={`Email OTP digit ${i + 1}`}
                        className="w-11 h-14 text-center text-xl font-bold bg-white/10 border border-white/20 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 caret-indigo-400"
                      />
                    ))}
                  </div>

                  <div className="flex items-center justify-end mb-4 px-1">
                    <button
                      type="button"
                      onClick={() => handleRetryInput('email')}
                      className="text-xs text-slate-400 hover:text-white flex items-center gap-1 transition"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>Clear / Retry</span>
                    </button>
                  </div>

                  <button
                    onClick={handleVerifyEmail}
                    disabled={isVerifying || emailDigits.join('').length < OTP_LENGTH}
                    className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-lg transition mb-3"
                  >
                    {isVerifying ? 'Verifying Email…' : 'Verify Email OTP'}
                  </button>

                  <button
                    onClick={handleResendEmail}
                    disabled={emailCooldown > 0 || isResendingEmail}
                    className="flex items-center justify-center gap-2 w-full text-xs text-slate-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-50 transition"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 ${isResendingEmail ? 'animate-spin' : ''}`} />
                    {emailCooldown > 0 ? `Resend Email OTP in ${emailCooldown}s` : 'Resend Email OTP'}
                  </button>
                </>
              )}
            </div>
          )}

          {/* Mobile Tab */}
          {activeTab === 'mobile' && (
            <div>
              <p className="text-xs text-slate-300 mb-2">
                Mobile SMS OTP verification is optional:
              </p>
              {otpSentTo?.mobile && (
                <div className="text-xs font-mono text-indigo-300 mb-2 bg-indigo-950/40 py-1.5 px-3 rounded inline-block">
                  📱 {otpSentTo.mobile}
                </div>
              )}

              {/* Weekly SMS Quota Banner */}
              <div className="bg-slate-800/60 border border-slate-700/60 rounded-lg py-1.5 px-3 mb-4 text-xs text-slate-300 flex items-center justify-between">
                <span>SMS OTPs remaining this week:</span>
                <span className={`font-bold font-mono ${smsRemaining > 0 ? 'text-amber-400' : 'text-red-400'}`}>
                  {smsRemaining} / 3
                </span>
              </div>

              {rateLimitMessage && (
                <div className="bg-amber-500/10 border border-amber-500/30 rounded-lg p-3 mb-4 text-left flex gap-2.5 text-xs text-amber-300">
                  <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                  <div>
                    <div className="font-semibold mb-0.5">Rate Limit Notice</div>
                    <div>{rateLimitMessage} You can verify via email OTP to complete registration.</div>
                  </div>
                </div>
              )}

              {mobileVerified ? (
                <div className="bg-green-500/10 border border-green-500/30 rounded-lg p-4 mb-4 flex items-center justify-center gap-2 text-green-400 text-sm">
                  <CheckCircle className="w-5 h-5" />
                  <span>Mobile number is verified</span>
                </div>
              ) : (
                <>
                  <div className="flex justify-center gap-2 mb-3" role="group" aria-label="Mobile OTP input">
                    {mobileDigits.map((digit, i) => (
                      <input
                        key={i}
                        ref={(el) => { mobileInputRefs.current[i] = el; }}
                        type="text"
                        inputMode="numeric"
                        maxLength={6}
                        value={digit}
                        onChange={(e) => handleDigitChange('mobile', i, e.target.value)}
                        onKeyDown={(e) => handleKeyDown('mobile', i, e)}
                        onFocus={(e) => e.target.select()}
                        aria-label={`Mobile OTP digit ${i + 1}`}
                        className="w-11 h-14 text-center text-xl font-bold bg-white/10 border border-white/20 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 caret-indigo-400"
                      />
                    ))}
                  </div>

                  <div className="flex items-center justify-end mb-4 px-1">
                    <button
                      type="button"
                      onClick={() => handleRetryInput('mobile')}
                      className="text-xs text-slate-400 hover:text-white flex items-center gap-1 transition"
                    >
                      <RotateCcw className="w-3 h-3" />
                      <span>Clear / Retry</span>
                    </button>
                  </div>

                  <button
                    onClick={handleVerifyMobile}
                    disabled={isVerifying || mobileDigits.join('').length < OTP_LENGTH}
                    className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-lg transition mb-3"
                  >
                    {isVerifying ? 'Verifying Mobile…' : 'Verify Mobile OTP'}
                  </button>

                  <button
                    onClick={handleResendSms}
                    disabled={smsCooldown > 0 || isResendingSms || smsRemaining <= 0}
                    className="flex items-center justify-center gap-2 w-full text-xs text-slate-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-50 transition mb-3"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 ${isResendingSms ? 'animate-spin' : ''}`} />
                    {smsCooldown > 0
                      ? `Resend SMS OTP in ${smsCooldown}s`
                      : smsRemaining <= 0
                      ? 'Weekly SMS Quota Reached'
                      : 'Resend SMS OTP'}
                  </button>

                  {emailVerified && (
                    <button
                      onClick={handleProceed}
                      className="w-full bg-slate-700/60 hover:bg-slate-700 text-slate-200 font-semibold py-2.5 rounded-lg text-xs transition"
                    >
                      Skip Mobile Verification & Proceed →
                    </button>
                  )}
                </>
              )}
            </div>
          )}

          {/* All Verified Success / Proceed */}
          {allVerified && (
            <div className="mt-4 pt-4 border-t border-white/10">
              <button
                onClick={handleProceed}
                className="w-full bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 text-white font-bold py-3 px-4 rounded-xl flex items-center justify-center gap-2 transition shadow-lg"
              >
                <span>Complete Registration & Proceed</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>

        {/* Back to Login */}
        <div className="pt-2 border-t border-white/10 text-xs text-slate-400">
          Already verified or want to sign in later?{' '}
          <Link to="/login" className="text-indigo-400 hover:text-indigo-300 underline font-medium">
            Go to Login
          </Link>
        </div>
      </div>
    </div>
  );
};

export default VerifyOtp;
