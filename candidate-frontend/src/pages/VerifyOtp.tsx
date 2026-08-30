// src/pages/VerifyOtp.tsx
// OTP verification — real call to POST /api/v1/identity/otp/verify.
// Stores JWT tokens returned by backend via tokenManager through AuthContext.

import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, RefreshCw, CheckCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60; // seconds

const VerifyOtp: React.FC = () => {
  const navigate = useNavigate();
  const { verifyOtp, resendOtp, otpSentTo, isAuthenticated, isVerified } = useAuth();
  const { toast } = useToast();

  const [digits, setDigits] = useState<string[]>(Array(OTP_LENGTH).fill(''));
  const [isVerifying, setIsVerifying] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cooldown, setCooldown] = useState(RESEND_COOLDOWN);
  const [verified, setVerified] = useState(false);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated && isVerified) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, isVerified, navigate]);

  // Countdown timer for resend button
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => setCooldown((c) => c - 1), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  const otp = digits.join('');

  const handleDigitChange = (index: number, value: string) => {
    // Handle paste of full OTP
    if (value.length > 1) {
      const pasted = value.replace(/\D/g, '').slice(0, OTP_LENGTH);
      const newDigits = [...digits];
      pasted.split('').forEach((ch, i) => {
        if (index + i < OTP_LENGTH) newDigits[index + i] = ch;
      });
      setDigits(newDigits);
      const nextIndex = Math.min(index + pasted.length, OTP_LENGTH - 1);
      inputRefs.current[nextIndex]?.focus();
      return;
    }

    const char = value.replace(/\D/g, '');
    const newDigits = [...digits];
    newDigits[index] = char;
    setDigits(newDigits);
    if (char && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
    setError(null);
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleVerify = async () => {
    if (otp.length < OTP_LENGTH) {
      setError('Please enter all 6 digits');
      return;
    }
    setIsVerifying(true);
    setError(null);
    try {
      const success = await verifyOtp(otp);
      if (success) {
        setVerified(true);
        toast.success('Verified!', 'Your account has been activated.');
        setTimeout(() => navigate('/dashboard'), 1500);
      } else {
        setError('Invalid OTP. Please check and try again.');
        setDigits(Array(OTP_LENGTH).fill(''));
        inputRefs.current[0]?.focus();
      }
    } finally {
      setIsVerifying(false);
    }
  };

  const handleResend = async () => {
    setIsResending(true);
    try {
      await resendOtp();
      setCooldown(RESEND_COOLDOWN);
      setDigits(Array(OTP_LENGTH).fill(''));
      inputRefs.current[0]?.focus();
      toast.info('OTP Resent', 'A new code has been sent to your email and mobile.');
    } catch {
      toast.error('Failed to resend', 'Please try again in a moment.');
    } finally {
      setIsResending(false);
    }
  };

  if (verified) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-800 flex items-center justify-center">
        <div className="text-center">
          <CheckCircle className="w-20 h-20 text-green-400 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-white mb-2">Verification Successful!</h2>
          <p className="text-slate-400">Redirecting to your dashboard…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-2xl p-8 text-center">
          <div className="bg-indigo-500/20 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShieldCheck className="w-8 h-8 text-indigo-400" />
          </div>

          <h2 className="text-xl font-bold text-white mb-2">Verify Your Account</h2>
          <p className="text-slate-400 text-sm mb-2">
            Enter the 6-digit OTP sent to:
          </p>
          {otpSentTo && (
            <div className="flex justify-center gap-4 text-xs mb-6">
              <span className="text-indigo-300">📧 {otpSentTo.email}</span>
              <span className="text-indigo-300">📱 {otpSentTo.mobile}</span>
            </div>
          )}

          {/* OTP digit inputs */}
          <div className="flex justify-center gap-2 mb-3" role="group" aria-label="OTP input">
            {digits.map((digit, i) => (
              <input
                key={i}
                ref={(el) => { inputRefs.current[i] = el; }}
                type="text"
                inputMode="numeric"
                maxLength={6}
                value={digit}
                onChange={(e) => handleDigitChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                onFocus={(e) => e.target.select()}
                aria-label={`OTP digit ${i + 1}`}
                className="w-11 h-14 text-center text-xl font-bold bg-white/10 border border-white/20 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 caret-indigo-400"
              />
            ))}
          </div>

          <div className="mb-4">
            <button
              type="button"
              onClick={() => {
                setDigits(['0', '0', '0', '0', '0', '0']);
                setError(null);
              }}
              className="text-xs text-indigo-300 hover:text-indigo-200 underline transition"
            >
              💡 Testing mode: Fill test OTP (000000)
            </button>
          </div>

          {error && (
            <p className="text-red-400 text-sm mb-4" role="alert">
              {error}
            </p>
          )}

          <button
            onClick={handleVerify}
            disabled={isVerifying || otp.length < OTP_LENGTH}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-lg transition mb-4"
          >
            {isVerifying ? (
              <span className="flex items-center justify-center gap-2">
                <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Verifying…
              </span>
            ) : (
              'Verify OTP'
            )}
          </button>

          <button
            onClick={handleResend}
            disabled={cooldown > 0 || isResending}
            className="flex items-center justify-center gap-2 w-full text-sm text-slate-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-50 transition"
          >
            <RefreshCw className={`w-4 h-4 ${isResending ? 'animate-spin' : ''}`} />
            {cooldown > 0 ? `Resend OTP in ${cooldown}s` : 'Resend OTP'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default VerifyOtp;
