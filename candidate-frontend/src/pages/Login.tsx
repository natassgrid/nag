// src/pages/Login.tsx
// Real login form with React Hook Form + Zod validation.
// Calls POST /api/v1/identity/auth/token; stores returned JWT via tokenManager.

import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LogIn, BookOpen, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { useToast } from '../components/Toast';

// ─── Zod schemas ──────────────────────────────────────────────────────────────

const loginSchema = z.object({
  username: z.string().min(1, 'Email or mobile is required'),
  password: z.string().min(1, 'Password is required'),
});

const forgotSchema = z.object({
  email: z.string().email('Enter a valid email address'),
});

type LoginForm = z.infer<typeof loginSchema>;
type ForgotForm = z.infer<typeof forgotSchema>;

// ─── Component ────────────────────────────────────────────────────────────────

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const { toast } = useToast();
  const [showPassword, setShowPassword] = useState(false);
  const [showForgot, setShowForgot] = useState(false);
  const [forgotSent, setForgotSent] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const {
    register: forgotRegister,
    handleSubmit: handleForgotSubmit,
    formState: { errors: forgotErrors, isSubmitting: forgotSubmitting },
  } = useForm<ForgotForm>({ resolver: zodResolver(forgotSchema) });

  const onSubmit = async (data: LoginForm) => {
    setLoginError(null);
    try {
      await login(data.username, data.password);
      toast.success('Welcome back!', 'You have logged in successfully.');
      navigate('/dashboard');
    } catch (err: unknown) {
      const errObj = err as {
        response?: {
          status?: number;
          data?: { detail?: string; message?: string; title?: string; error?: string };
        };
        message?: string;
      };

      let detail =
        errObj?.response?.data?.detail ||
        errObj?.response?.data?.message ||
        errObj?.response?.data?.error ||
        errObj?.response?.data?.title;

      if (!detail) {
        if (errObj?.response?.status === 401) {
          detail = 'Invalid email/mobile or password. Please verify your credentials.';
        } else if (errObj?.response?.status === 403) {
          detail = 'Access forbidden. Account may be locked or pending verification.';
        } else if (errObj?.response?.status === 500) {
          detail = 'Internal authentication error. Please try again.';
        } else if (errObj?.message) {
          detail = errObj.message;
        } else {
          detail = 'Invalid credentials. Please check and try again.';
        }
      }

      setLoginError(detail);
      toast.error('Login failed', detail);
    }
  };

  const onForgotSubmit = async (data: ForgotForm) => {
    try {
      await authService.forgotPassword({ email: data.email });
      setForgotSent(true);
      toast.info('OTP Sent', `Check your email ${data.email} for the reset code.`);
    } catch {
      toast.error('Failed to send OTP', 'Please check your email and try again.');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-3 mb-3">
            <div className="bg-indigo-500 p-3 rounded-xl">
              <BookOpen className="w-8 h-8 text-white" />
            </div>
            <div className="text-left">
              <h1 className="text-2xl font-bold text-white">NAG</h1>
              <p className="text-xs text-indigo-300 uppercase tracking-widest">
                National Assessment Grid
              </p>
            </div>
          </div>
          <p className="text-slate-400 text-sm">Candidate Portal</p>
        </div>

        <div className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-2xl p-8">
          {!showForgot ? (
            <>
              <h2 className="text-xl font-semibold text-white mb-6">Sign in to your account</h2>

              {loginError && (
                <div className="mb-6 p-4 bg-red-500/15 border border-red-500/40 rounded-xl text-red-200 text-sm flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-semibold text-red-300">Sign in failed</p>
                    <p className="text-xs text-red-200/90 mt-0.5 leading-relaxed">{loginError}</p>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1.5">
                    Email or Mobile Number
                  </label>
                  <input
                    {...register('username')}
                    type="text"
                    autoComplete="username"
                    placeholder="you@example.com or 9876543210"
                    aria-invalid={!!errors.username}
                    aria-describedby={errors.username ? 'username-error' : undefined}
                    className="w-full bg-white/10 border border-white/20 rounded-lg px-4 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                  />
                  {errors.username && (
                    <p id="username-error" className="text-red-400 text-xs mt-1">
                      {errors.username.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1.5">
                    Password
                  </label>
                  <div className="relative">
                    <input
                      {...register('password')}
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="current-password"
                      placeholder="••••••••"
                      aria-invalid={!!errors.password}
                      aria-describedby={errors.password ? 'password-error' : undefined}
                      className="w-full bg-white/10 border border-white/20 rounded-lg px-4 py-2.5 pr-10 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white"
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {errors.password && (
                    <p id="password-error" className="text-red-400 text-xs mt-1">
                      {errors.password.message}
                    </p>
                  )}
                </div>

                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={() => setShowForgot(true)}
                    className="text-indigo-400 text-xs hover:text-indigo-300"
                  >
                    Forgot password?
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-lg transition text-sm"
                >
                  {isSubmitting ? (
                    <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  ) : (
                    <LogIn className="w-4 h-4" />
                  )}
                  {isSubmitting ? 'Signing in…' : 'Sign In'}
                </button>
              </form>

              <p className="text-center text-sm text-slate-400 mt-6">
                Don't have an account?{' '}
                <Link to="/register" className="text-indigo-400 hover:text-indigo-300 font-medium">
                  Register here
                </Link>
              </p>
            </>
          ) : !forgotSent ? (
            <>
              <h2 className="text-xl font-semibold text-white mb-2">Forgot Password</h2>
              <p className="text-sm text-slate-400 mb-6">
                Enter your registered email address and we'll send you a reset code.
              </p>
              <form onSubmit={handleForgotSubmit(onForgotSubmit)} noValidate className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-slate-300 mb-1.5">
                    Registered Email
                  </label>
                  <input
                    {...forgotRegister('email')}
                    type="email"
                    placeholder="you@example.com"
                    aria-invalid={!!forgotErrors.email}
                    className="w-full bg-white/10 border border-white/20 rounded-lg px-4 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                  />
                  {forgotErrors.email && (
                    <p className="text-red-400 text-xs mt-1">{forgotErrors.email.message}</p>
                  )}
                </div>
                <button
                  type="submit"
                  disabled={forgotSubmitting}
                  className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-800 text-white font-semibold py-2.5 rounded-lg transition text-sm"
                >
                  {forgotSubmitting ? 'Sending…' : 'Send Reset Code'}
                </button>
                <button
                  type="button"
                  onClick={() => setShowForgot(false)}
                  className="w-full text-slate-400 text-sm hover:text-white"
                >
                  ← Back to login
                </button>
              </form>
            </>
          ) : (
            <div className="text-center py-4">
              <div className="text-5xl mb-4">📧</div>
              <h2 className="text-xl font-semibold text-white mb-2">Check your email</h2>
              <p className="text-slate-400 text-sm mb-6">
                A password reset link has been sent to your registered email address.
                <br />
                Redirecting you to{' '}
                <Link to="/reset-password" className="text-indigo-400">
                  reset password
                </Link>
                .
              </p>
              <button
                onClick={() => {
                  setShowForgot(false);
                  setForgotSent(false);
                }}
                className="text-indigo-400 text-sm hover:text-indigo-300"
              >
                ← Back to login
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Login;
