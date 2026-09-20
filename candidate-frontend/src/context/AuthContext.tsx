// src/context/AuthContext.tsx
// Real auth context — delegates to authService and candidateService.
// JWT tokens stored via tokenManager.

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { candidateService } from '../services/candidateService';
import { tokenManager } from '../utils/tokenManager';
import { useUserActivity } from '../hooks/useUserActivity';
import type {
  CandidateProfileResponse,
  RegistrationRequest,
  VerificationStatusResponse,
} from '../types/api';

// ─── Context shape ───────────────────────────────────────────

interface AuthContextType {
  profile: CandidateProfileResponse | null;
  isAuthenticated: boolean;
  isVerified: boolean;
  pendingUserId: string | null;
  otpSentTo: { email: string; mobile: string } | null;
  profileLoading: boolean;

  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  register: (request: RegistrationRequest) => Promise<void>;
  verifyOtp: (otp: string) => Promise<boolean>;
  verifyEmailOtp: (otp: string) => Promise<VerificationStatusResponse>;
  verifyMobileOtp: (otp: string) => Promise<VerificationStatusResponse>;
  getVerificationStatus: () => Promise<VerificationStatusResponse | null>;
  resendEmailOtp: () => Promise<void>;
  resendSmsOtp: () => Promise<void>;
  resendOtp: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

// ─── Context & persistence keys ──────────────────────────────

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const PENDING_USER_KEY = 'nag_pending_user_id';
const PENDING_MOBILE_KEY = 'nag_pending_mobile';
const OTP_SENT_KEY = 'nag_otp_sent_to';

// ─── Provider ────────────────────────────────────────────────

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [profile, setProfile] = useState<CandidateProfileResponse | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(
    () => tokenManager.isAuthenticated(),
  );
  const [isVerified, setIsVerified] = useState<boolean>(() => {
    return tokenManager.isAuthenticated();
  });
  const [pendingUserId, setPendingUserId] = useState<string | null>(
    () => sessionStorage.getItem(PENDING_USER_KEY),
  );
  const [otpSentTo, setOtpSentTo] = useState<{ email: string; mobile: string } | null>(() => {
    const saved = sessionStorage.getItem(OTP_SENT_KEY);
    return saved ? (JSON.parse(saved) as { email: string; mobile: string }) : null;
  });
  const [profileLoading, setProfileLoading] = useState<boolean>(false);

  // Fetch profile from backend when authenticated
  const refreshProfile = useCallback(async () => {
    const userId = tokenManager.getUserId();
    if (!userId || userId === 'undefined' || userId === 'null') return;
    setProfileLoading(true);
    try {
      const p = await candidateService.getProfile(userId);
      setProfile(p);
    } catch (err) {
      console.error('Failed to load candidate profile', err);
    } finally {
      setProfileLoading(false);
    }
  }, []);

  // On mount: if we have a valid token, load the profile
  useEffect(() => {
    if (isAuthenticated) {
      void refreshProfile();
    }
  }, [isAuthenticated, refreshProfile]);

  // ─── Auth actions ───────────────────────────────────────────

  const login = useCallback(async (username: string, password: string): Promise<boolean> => {
    try {
      tokenManager.clearTokens();
      const tokens = await authService.login({ username, password });
      tokenManager.setTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresIn, tokens.userId);
      setIsAuthenticated(true);
      setIsVerified(true);
      try {
        await refreshProfile();
      } catch (profileErr) {
        console.warn('Profile load on login:', profileErr);
      }
      return true;
    } catch (err) {
      console.error('Login failed', err);
      throw err;
    }
  }, [refreshProfile]);

  const logout = useCallback(async (): Promise<void> => {
    try {
      await authService.logout();
    } finally {
      tokenManager.clearTokens();
      setIsAuthenticated(false);
      setIsVerified(false);
      setProfile(null);
      setPendingUserId(null);
      setOtpSentTo(null);
      sessionStorage.removeItem(PENDING_USER_KEY);
      sessionStorage.removeItem(PENDING_MOBILE_KEY);
      sessionStorage.removeItem(OTP_SENT_KEY);
    }
  }, []);

  // Monitor user activity and proactive refresh
  useUserActivity(isAuthenticated, logout);

  const register = useCallback(async (request: RegistrationRequest): Promise<void> => {
    const response = await authService.register(request);
    setPendingUserId(response.userId);
    setOtpSentTo(response.otpSentTo || { email: request.email, mobile: request.mobile });
    sessionStorage.setItem(PENDING_USER_KEY, response.userId);
    sessionStorage.setItem(PENDING_MOBILE_KEY, request.mobile);
    sessionStorage.setItem('nag_pending_registration', JSON.stringify(request));
    if (response.otpSentTo) {
      sessionStorage.setItem(OTP_SENT_KEY, JSON.stringify(response.otpSentTo));
    }
  }, []);

  const verifyOtp = useCallback(async (otp: string): Promise<boolean> => {
    const pendingMobile = sessionStorage.getItem(PENDING_MOBILE_KEY);
    if (!pendingUserId && !pendingMobile) return false;
    try {
      const tokens = await authService.verifyOtp({
        userId: pendingUserId ?? undefined,
        mobile: pendingMobile ?? undefined,
        otp,
      });
      tokenManager.setTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresIn, tokens.userId);
      setIsAuthenticated(true);
      setIsVerified(true);
      setPendingUserId(null);
      setOtpSentTo(null);
      sessionStorage.removeItem(PENDING_USER_KEY);
      sessionStorage.removeItem(PENDING_MOBILE_KEY);
      sessionStorage.removeItem(OTP_SENT_KEY);

      // Auto-create initial candidate profile in candidate-service
      const pendingRegStr = sessionStorage.getItem('nag_pending_registration');
      if (pendingRegStr) {
        try {
          const regData = JSON.parse(pendingRegStr) as RegistrationRequest;
          const resolvedId = tokens.userId || tokenManager.getUserId();
          if (resolvedId) {
            await candidateService.createProfile({
              userId: resolvedId,
              fullName: regData.fullName,
              dateOfBirth: '2000-01-01',
              gender: 'PREFER_NOT_TO_SAY',
              nationality: 'INDIAN',
              category: 'GENERAL',
              mobile: regData.mobile,
              email: regData.email,
              identityDocNumber: regData.identityDocNumber,
              address: '',
            });
            sessionStorage.removeItem('nag_pending_registration');
          }
        } catch (profileCreateErr) {
          console.warn('Auto-create profile during OTP verification:', profileCreateErr);
        }
      }

      await refreshProfile();
      return true;
    } catch (err) {
      console.error('OTP verification failed', err);
      return false;
    }
  }, [pendingUserId, refreshProfile]);

  const verifyEmailOtp = useCallback(async (otp: string): Promise<VerificationStatusResponse> => {
    if (!pendingUserId) throw new Error('No pending registration found');
    return await authService.verifyEmail({ userId: pendingUserId, otp });
  }, [pendingUserId]);

  const verifyMobileOtp = useCallback(async (otp: string): Promise<VerificationStatusResponse> => {
    if (!pendingUserId) throw new Error('No pending registration found');
    return await authService.verifyMobile({ userId: pendingUserId, otp });
  }, [pendingUserId]);

  const getVerificationStatus = useCallback(async (): Promise<VerificationStatusResponse | null> => {
    if (!pendingUserId) return null;
    return await authService.getVerificationStatus(pendingUserId);
  }, [pendingUserId]);

  const resendEmailOtp = useCallback(async (): Promise<void> => {
    if (!pendingUserId) return;
    await authService.resendEmailOtp(pendingUserId);
  }, [pendingUserId]);

  const resendSmsOtp = useCallback(async (): Promise<void> => {
    if (!pendingUserId) return;
    await authService.resendSmsOtp(pendingUserId);
  }, [pendingUserId]);

  const resendOtp = useCallback(async (): Promise<void> => {
    if (!pendingUserId) return;
    await authService.resendOtp({ userId: pendingUserId });
  }, [pendingUserId]);

  return (
    <AuthContext.Provider
      value={{
        profile,
        isAuthenticated,
        isVerified,
        pendingUserId,
        otpSentTo,
        profileLoading,
        login,
        logout,
        register,
        verifyOtp,
        verifyEmailOtp,
        verifyMobileOtp,
        getVerificationStatus,
        resendEmailOtp,
        resendSmsOtp,
        resendOtp,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
