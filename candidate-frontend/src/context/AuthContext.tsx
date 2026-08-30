// src/context/AuthContext.tsx
// Real auth context — delegates to authService and candidateService.
// All mock setTimeout calls removed; JWT tokens stored via tokenManager.

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { candidateService } from '../services/candidateService';
import { tokenManager } from '../utils/tokenManager';
import type {
  CandidateProfileResponse,
  RegistrationRequest,
} from '../types/api';

// ─── Context shape ────────────────────────────────────────────────────────────

interface AuthContextType {
  /** Full candidate profile from the server. null when not logged in. */
  profile: CandidateProfileResponse | null;
  /** True after a successful login with valid JWT. */
  isAuthenticated: boolean;
  /** True after email+mobile OTP verification. */
  isVerified: boolean;
  /** userId returned by /register, persisted across page refreshes for OTP flow. */
  pendingUserId: string | null;
  /** Masked contact info shown on OTP screen. */
  otpSentTo: { email: string; mobile: string } | null;
  /** Whether the profile is currently being fetched from the backend. */
  profileLoading: boolean;

  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  register: (request: RegistrationRequest) => Promise<void>;
  verifyOtp: (otp: string) => Promise<boolean>;
  resendOtp: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

// ─── Context & persistence keys ──────────────────────────────────────────────

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const PENDING_USER_KEY = 'nag_pending_user_id';
const PENDING_MOBILE_KEY = 'nag_pending_mobile';
const OTP_SENT_KEY = 'nag_otp_sent_to';

// ─── Provider ─────────────────────────────────────────────────────────────────

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [profile, setProfile] = useState<CandidateProfileResponse | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(
    () => tokenManager.isAuthenticated(),
  );
  const [isVerified, setIsVerified] = useState<boolean>(() => {
    // User is verified if they have a valid token (OTP was verified to get it)
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

  // ── Auth actions ─────────────────────────────────────────────────────────────

  const login = useCallback(async (username: string, password: string): Promise<boolean> => {
    try {
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

  const register = useCallback(async (request: RegistrationRequest): Promise<void> => {
    const response = await authService.register(request);
    // Store pending userId, mobile, and registration data so VerifyOtp can auto-initialize profile
    setPendingUserId(response.userId);
    setOtpSentTo(response.otpSentTo);
    sessionStorage.setItem(PENDING_USER_KEY, response.userId);
    sessionStorage.setItem(PENDING_MOBILE_KEY, request.mobile);
    sessionStorage.setItem('nag_pending_registration', JSON.stringify(request));
    sessionStorage.setItem(OTP_SENT_KEY, JSON.stringify(response.otpSentTo));
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

      // Auto-create initial candidate profile in candidate-service using registration data
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
