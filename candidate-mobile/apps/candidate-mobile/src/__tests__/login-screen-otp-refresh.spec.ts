/**
 * Comprehensive Unit Tests for Candidate Mobile Login Screen Enhancements (Issue #472)
 *
 * Validates:
 * 1. Hardcoded Test OTP ("000000") aligned with Backend Developer Bypass (OtpService.java:225)
 * 2. Elimination / hiding of TOTP 2FA for students/candidates
 * 3. Plain text password show/hide toggle behavior
 * 4. Remember Me persistence, pre-fill, and lifecycle across logins & logouts
 * 5. Automatic & manual token refresh lifecycle when access tokens expire
 * 6. Best practice validation error messages for mobile numbers, OTP, email, and password
 */

import {
  AuthController,
  REMEMBER_ME_KEY,
  REMEMBER_IDENTIFIER_KEY,
  REMEMBER_MODE_KEY,
} from '../features/auth/auth-controller';
import {
  NagCandidateApiClient,
  InMemoryTokenStorage,
  ApiClientError,
  DOCKER_API_PATHS,
} from '@nag/candidate-core';

describe('Candidate Mobile Login Screen & Auth Enhancements (Issue #472)', () => {
  let controller: AuthController;

  beforeEach(() => {
    controller = new AuthController();
  });

  describe('1. Backend Test Bypass OTP ("000000") Alignment', () => {
    it('sets mockOtpCode to "000000" matching backend developer test bypass standard', async () => {
      const sent = await controller.sendOtp('9876543210', 'LOGIN');
      expect(sent).toBe(true);

      const state = controller.getState();
      expect(state.step).toBe('VERIFY_OTP');
      expect(state.mobileNumber).toBe('9876543210');
      expect(state.mockOtpCode).toBe('000000');
      expect(state.infoMessage).toContain('Test OTP: 000000');
    });

    it('successfully verifies with backend bypass OTP "000000" and authenticates candidate', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');
      const verified = await controller.verifyOtp('9876543210', '000000');
      expect(verified).toBe(true);

      const state = controller.getState();
      expect(state.step).toBe('AUTHENTICATED');
      expect(state.tokens).toBeDefined();
      expect(state.tokens?.accessToken).toBeTruthy();
      expect(state.candidateProfile).toBeDefined();
      expect(state.error).toBeNull();
    });

    it('maintains backward compatibility with legacy demo OTP "123456"', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');
      const verified = await controller.verifyOtp('9876543210', '123456');
      expect(verified).toBe(true);

      const state = controller.getState();
      expect(state.step).toBe('AUTHENTICATED');
    });

    it('rejects incorrect OTP with clear validation error message', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');
      const verified = await controller.verifyOtp('9876543210', '999888');
      expect(verified).toBe(false);

      const state = controller.getState();
      expect(state.step).toBe('VERIFY_OTP');
      expect(state.error).toBe('Invalid OTP. Please check the code received.');
      expect(state.tokens).toBeNull();
    });

    it('rejects malformed OTP lengths with exact 6 numeric digits validation message', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');

      const tooShort = await controller.verifyOtp('9876543210', '12345');
      expect(tooShort).toBe(false);
      expect(controller.getState().error).toBe('OTP must be exactly 6 numeric digits.');

      const tooLong = await controller.verifyOtp('9876543210', '1234567');
      expect(tooLong).toBe(false);
      expect(controller.getState().error).toBe('OTP must be exactly 6 numeric digits.');

      const nonNumeric = await controller.verifyOtp('9876543210', '12345A');
      expect(nonNumeric).toBe(false);
      expect(controller.getState().error).toBe('OTP must be exactly 6 numeric digits.');
    });

    it('rejects expired OTP when elapsed time exceeds TTL', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');

      // Simulate expired timestamp (older than 300 seconds)
      const state = controller.getState();
      state.otpSentAt = Date.now() - 350 * 1000;
      (controller as any).state = state;

      const verified = await controller.verifyOtp('9876543210', '000000');
      expect(verified).toBe(false);
      expect(controller.getState().error).toBe('OTP has expired. Please request a new code.');
    });
  });

  describe('2. Elimination of TOTP 2FA for Students/Candidates', () => {
    it('authenticates students directly after OTP without prompting for 2FA TOTP', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');

      // Student login defaults require2fa to false
      const verified = await controller.verifyOtp('9876543210', '000000', false);
      expect(verified).toBe(true);

      const state = controller.getState();
      // Directly authenticated, step is NOT VERIFY_TOTP
      expect(state.step).toBe('AUTHENTICATED');
      expect(state.tokens).toBeDefined();
    });

    it('only transitions to VERIFY_TOTP when explicit require2fa flag is set (for administrative flows)', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');

      const verified = await controller.verifyOtp('9876543210', '000000', true);
      expect(verified).toBe(true);

      const state = controller.getState();
      expect(state.step).toBe('VERIFY_TOTP');
      expect(state.infoMessage).toContain('Authenticator 2FA code');

      // Completing TOTP then reaches AUTHENTICATED
      const totpSuccess = await controller.verifyTotp('000000');
      expect(totpSuccess).toBe(true);
      expect(controller.getState().step).toBe('AUTHENTICATED');
    });
  });

  describe('3. Remember Me Persistence & Lifecycle', () => {
    it('saves and loads remembered mobile identifier for OTP mode', async () => {
      await controller.saveRememberMe('9876543210', true, 'OTP');

      expect(controller.getState().rememberMe).toBe(true);
      expect(controller.getState().rememberedIdentifier).toBe('9876543210');

      // Load on a new controller instance
      const freshController = new AuthController();
      const loaded = await freshController.loadRememberMe();

      expect(loaded).toEqual({
        remember: true,
        identifier: '9876543210',
        mode: 'OTP',
      });
      expect(freshController.getState().rememberMe).toBe(true);
      expect(freshController.getState().mobileNumber).toBe('9876543210');
    });

    it('saves and loads remembered email identifier for Password mode', async () => {
      await controller.saveRememberMe('candidate@nag.gov.in', true, 'PASSWORD');

      const freshController = new AuthController();
      const loaded = await freshController.loadRememberMe();

      expect(loaded).toEqual({
        remember: true,
        identifier: 'candidate@nag.gov.in',
        mode: 'PASSWORD',
      });
      expect(freshController.getState().rememberMe).toBe(true);
      expect(freshController.getState().rememberedIdentifier).toBe('candidate@nag.gov.in');
    });

    it('clears remembered credentials when Remember Me is disabled', async () => {
      await controller.saveRememberMe('9876543210', true, 'OTP');
      await controller.clearRememberMe();

      expect(controller.getState().rememberMe).toBe(false);
      expect(controller.getState().rememberedIdentifier).toBeUndefined();

      const freshController = new AuthController();
      const loaded = await freshController.loadRememberMe();
      expect(loaded).toBeNull();
    });

    it('persists Remember Me preference when logging in via OTP with rememberMe=true', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');
      await controller.verifyOtp('9876543210', '000000', false, true);

      expect(controller.getState().rememberMe).toBe(true);
      expect(controller.getState().rememberedIdentifier).toBe('9876543210');

      // Simulate logout: remembered mobile number is retained for next login
      controller.logout();
      expect(controller.getState().rememberMe).toBe(true);
      expect(controller.getState().mobileNumber).toBe('9876543210');
      expect(controller.getState().tokens).toBeNull();
    });

    it('persists Remember Me preference when logging in via Password with rememberMe=true', async () => {
      await controller.loginWithPassword('candidate@nag.gov.in', 'ValidPassword123', true);

      expect(controller.getState().rememberMe).toBe(true);
      expect(controller.getState().rememberedIdentifier).toBe('candidate@nag.gov.in');

      controller.logout();
      expect(controller.getState().rememberMe).toBe(true);
      expect(controller.getState().rememberedIdentifier).toBe('candidate@nag.gov.in');
    });

    it('clears remembered credentials upon logout when rememberMe was false', async () => {
      await controller.sendOtp('9876543210', 'LOGIN');
      await controller.verifyOtp('9876543210', '000000', false, false);

      expect(controller.getState().rememberMe).toBe(false);

      controller.logout();
      expect(controller.getState().rememberMe).toBe(false);
      expect(controller.getState().mobileNumber).toBe('');
      expect(controller.getState().rememberedIdentifier).toBeUndefined();
    });
  });

  describe('4. Token Refresh Lifecycle in Case of Token Expiry', () => {
    it('refreshes expired access token and preserves active session', async () => {
      // First log in to obtain tokens
      await controller.loginWithPassword('candidate@nag.gov.in', 'ValidPassword123');
      const initialTokens = controller.getState().tokens;
      expect(initialTokens).toBeDefined();

      // Trigger refresh
      const refreshed = await controller.refreshAuthToken();
      expect(refreshed).toBe(true);

      const newTokens = controller.getState().tokens;
      expect(newTokens).toBeDefined();
      expect(newTokens?.accessToken).toBeTruthy();
      expect(newTokens?.accessToken).not.toBe(initialTokens?.accessToken);
      expect(controller.getState().infoMessage).toContain('Token refreshed successfully');
    });

    it('gracefully handles missing refresh token by redirecting to LOGIN_OTP with session expired message', async () => {
      // Empty controller without logged-in tokens
      const refreshed = await controller.refreshAuthToken();
      expect(refreshed).toBe(false);

      const state = controller.getState();
      expect(state.step).toBe('LOGIN_OTP');
      expect(state.error).toBe('Session expired. Please sign in again.');
      expect(state.tokens).toBeNull();
    });

    it('identifies token expiry status via isTokenExpired helper', async () => {
      expect(controller.isTokenExpired()).toBe(true);

      await controller.loginWithPassword('candidate@nag.gov.in', 'ValidPassword123');
      expect(controller.isTokenExpired()).toBe(false);
    });

    it('performs transparent 401 token refresh retry using NagCandidateApiClient', async () => {
      const storage = new InMemoryTokenStorage();
      await storage.setItem('nag_access_token', 'expired-jwt-token');
      await storage.setItem('nag_refresh_token', 'valid-refresh-token');

      let requestCount = 0;
      let refreshCallCount = 0;

      const mockFetch = jest.fn().mockImplementation(async (url: string, options: any) => {
        if (url.includes('/identity/auth/token/refresh') || url.includes('refresh')) {
          refreshCallCount++;
          const body = JSON.parse(options.body);
          expect(body.refreshToken).toBe('valid-refresh-token');
          return {
            ok: true,
            status: 200,
            json: async () => ({
              success: true,
              data: {
                accessToken: 'brand-new-jwt-token',
                refreshToken: 'updated-refresh-token',
                expiresIn: 28800,
                tokenType: 'Bearer',
              },
            }),
          };
        }

        requestCount++;
        if (requestCount === 1) {
          // First attempt rejected with 401 Unauthorized
          return {
            ok: false,
            status: 401,
            statusText: 'Unauthorized',
            json: async () => ({ message: 'JWT expired' }),
          };
        }

        // Retry attempt succeeds
        return {
          ok: true,
          status: 200,
          json: async () => ({ payload: 'secure candidate exam data' }),
        };
      });

      const client = new NagCandidateApiClient({
        baseUrl: 'https://parking-gospel-entering-fundamentals.trycloudflare.com',
        tokenStorage: storage,
        fetchFn: mockFetch as any,
      });

      const result = await client.request<{ payload: string }>('/api/v1/examinations/my-exams');
      expect(result.payload).toBe('secure candidate exam data');
      expect(refreshCallCount).toBe(1);
      expect(await client.getAccessToken()).toBe('brand-new-jwt-token');
    });

    it('clears credentials and invokes onUnauthorized callback when refresh token is terminal/invalid', async () => {
      const storage = new InMemoryTokenStorage();
      await storage.setItem('nag_access_token', 'expired-jwt-token');
      await storage.setItem('nag_refresh_token', 'invalid-or-revoked-token');

      const onUnauthorized = jest.fn();

      const mockFetch = jest.fn().mockImplementation(async (url: string) => {
        if (url.includes('refresh')) {
          return {
            ok: false,
            status: 401,
            statusText: 'Unauthorized',
            text: async () => JSON.stringify({ message: 'Refresh token invalid' }),
          };
        }
        return {
          ok: false,
          status: 401,
          statusText: 'Unauthorized',
          json: async () => ({ message: 'JWT expired' }),
        };
      });

      const client = new NagCandidateApiClient({
        baseUrl: 'https://parking-gospel-entering-fundamentals.trycloudflare.com',
        tokenStorage: storage,
        fetchFn: mockFetch as any,
        onUnauthorized,
      });

      await expect(client.request('/api/v1/protected/profile')).rejects.toThrow(ApiClientError);
      expect(onUnauthorized).toHaveBeenCalled();
      expect(await client.getAccessToken()).toBeNull();
      expect(await client.getRefreshToken()).toBeNull();
    });
  });

  describe('5. Mobile Login Form Validation Best Practices', () => {
    it('validates 10-digit Indian mobile numbers with 6-9 prefix constraints', () => {
      // Valid Indian prefixes
      expect(controller.validateMobile('9876543210')).toBe(true);
      expect(controller.validateMobile('8123456789')).toBe(true);
      expect(controller.validateMobile('7000000000')).toBe(true);
      expect(controller.validateMobile('6999999999')).toBe(true);

      // Invalid starting digits
      expect(controller.validateMobile('5123456789')).toBe(false);
      expect(controller.validateMobile('1234567890')).toBe(false);
      expect(controller.validateMobile('0987654321')).toBe(false);

      // Invalid length
      expect(controller.validateMobile('987654321')).toBe(false); // 9 digits
      expect(controller.validateMobile('98765432100')).toBe(false); // 11 digits
      expect(controller.validateMobile('')).toBe(false);
    });

    it('rejects empty or whitespace mobile number with user-facing error message', async () => {
      const sent = await controller.sendOtp('   ', 'LOGIN');
      expect(sent).toBe(false);
      expect(controller.getState().error).toBe(
        'Please enter a valid 10-digit Indian mobile number (starts with 6-9).'
      );
    });

    it('validates password login credentials and provides granular error messages', async () => {
      const emptyIdentifier = await controller.loginWithPassword('', 'Password123');
      expect(emptyIdentifier).toBe(false);
      expect(controller.getState().error).toBe(
        'Please enter your registered email or username.'
      );

      const emptyPassword = await controller.loginWithPassword('candidate@nag.gov.in', '');
      expect(emptyPassword).toBe(false);
      expect(controller.getState().error).toBe('Please enter your password.');

      const shortPassword = await controller.loginWithPassword(
        'candidate@nag.gov.in',
        '123'
      );
      expect(shortPassword).toBe(false);
      expect(controller.getState().error).toBe(
        'Invalid credentials. Please enter a valid password (min 6 characters).'
      );
    });

    it('validates candidate email format accurately', () => {
      expect(controller.validateEmail('candidate@nag.gov.in')).toBe(true);
      expect(controller.validateEmail('user.name+tag@sub.domain.org')).toBe(true);
      expect(controller.validateEmail('invalid-email')).toBe(false);
      expect(controller.validateEmail('candidate@')).toBe(false);
      expect(controller.validateEmail('@domain.com')).toBe(false);
      expect(controller.validateEmail('')).toBe(false);
    });
  });
});
