// src/pages/Register.tsx
// Self-registration form with React Hook Form + Zod.
// Calls POST /api/v1/identity/register — includes dynamic identity document validation.

import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, BookOpen, UserPlus, Info } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import type { RegistrationRequest } from '../types/api';

// ─── Identity Document Validation Rules ──────────────────────────────────────

export type IdentityDocType = 'AADHAAR' | 'PAN' | 'PASSPORT' | 'VOTER_ID' | 'DL';

interface DocRule {
  label: string;
  placeholder: string;
  hint: string;
  pattern: RegExp;
  errorMessage: string;
  maxLength: number;
  sanitize: (val: string) => string;
}

export const DOC_VALIDATION: Record<IdentityDocType, DocRule> = {
  AADHAAR: {
    label: 'Aadhaar Card',
    placeholder: '12-digit number (e.g. 123456789012)',
    hint: 'Format: 12 numeric digits without spaces or hyphens',
    pattern: /^\d{12}$/,
    errorMessage: 'Aadhaar number must be exactly 12 numeric digits (e.g. 123456789012)',
    maxLength: 14, // Allows typing with spaces
    sanitize: (val: string) => val.replace(/[\s-]/g, ''),
  },
  PAN: {
    label: 'PAN Card',
    placeholder: '10-character alphanumeric (e.g. ABCDE1234F)',
    hint: 'Format: 5 uppercase letters + 4 digits + 1 uppercase letter',
    pattern: /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/,
    errorMessage: 'PAN must be in standard format: 5 letters, 4 digits, 1 letter (e.g. ABCDE1234F)',
    maxLength: 10,
    sanitize: (val: string) => val.trim().toUpperCase(),
  },
  PASSPORT: {
    label: 'Passport',
    placeholder: '8-character alphanumeric (e.g. A1234567)',
    hint: 'Format: 1 uppercase letter + 7 digits',
    pattern: /^[A-Z][0-9]{7}$/,
    errorMessage: 'Passport number must start with 1 letter followed by 7 digits (e.g. A1234567)',
    maxLength: 8,
    sanitize: (val: string) => val.trim().toUpperCase(),
  },
  VOTER_ID: {
    label: 'Voter ID (EPIC)',
    placeholder: '10-character alphanumeric (e.g. ABC1234567)',
    hint: 'Format: 3 uppercase letters + 7 digits',
    pattern: /^[A-Z]{3}[0-9]{7}$/,
    errorMessage: 'Voter ID must start with 3 letters followed by 7 digits (e.g. ABC1234567)',
    maxLength: 16,
    sanitize: (val: string) => val.trim().toUpperCase(),
  },
  DL: {
    label: "Driver's Licence",
    placeholder: 'Licence number (e.g. DL0120110012345)',
    hint: 'Format: State code + RTO + Year + Unique number (e.g. DL0120110012345)',
    pattern: /^[A-Z]{2}[0-9A-Z\s/-]{8,18}$/i,
    errorMessage: "Enter a valid Driver's Licence number (e.g. DL0120110012345)",
    maxLength: 20,
    sanitize: (val: string) => val.trim().toUpperCase(),
  },
};

export const DOC_TYPES: { value: IdentityDocType; label: string }[] = [
  { value: 'AADHAAR', label: 'Aadhaar Card' },
  { value: 'PAN', label: 'PAN Card' },
  { value: 'PASSPORT', label: 'Passport' },
  { value: 'VOTER_ID', label: 'Voter ID' },
  { value: 'DL', label: "Driver's Licence" },
];

// ─── Zod schema ───────────────────────────────────────────────────────────────

const registerSchema = z
  .object({
    fullName: z.string().min(3, 'Full name must be at least 3 characters'),
    email: z.string().email('Enter a valid email address'),
    mobile: z
      .string()
      .regex(/^[6-9]\d{9}$/, 'Enter a valid 10-digit Indian mobile number'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Must contain at least one uppercase letter')
      .regex(/[0-9]/, 'Must contain at least one number')
      .regex(/[^a-zA-Z0-9]/, 'Must contain at least one special character'),
    confirmPassword: z.string(),
    identityDocType: z.enum(['AADHAAR', 'PAN', 'PASSPORT', 'VOTER_ID', 'DL'] as const, {
      message: 'Select an identity document type',
    }),
    identityDocNumber: z.string().min(1, 'Document number is required'),
    declaration: z.literal<boolean>(true, {
      message: 'You must accept the declaration to proceed',
    }),
  })
  .superRefine((data, ctx) => {
    if (data.password !== data.confirmPassword) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Passwords do not match',
        path: ['confirmPassword'],
      });
    }

    if (data.identityDocType && data.identityDocNumber) {
      const rule = DOC_VALIDATION[data.identityDocType];
      if (rule) {
        const cleanVal = rule.sanitize(data.identityDocNumber);
        if (!rule.pattern.test(cleanVal)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: rule.errorMessage,
            path: ['identityDocNumber'],
          });
        }
      }
    }
  });

type RegisterForm = z.infer<typeof registerSchema>;

// ─── Component ────────────────────────────────────────────────────────────────

const Register: React.FC = () => {
  const navigate = useNavigate();
  const { register: authRegister } = useAuth();
  const { toast } = useToast();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
  });

  const selectedDocType = watch('identityDocType');
  const activeDocConfig = selectedDocType ? DOC_VALIDATION[selectedDocType] : null;

  const onSubmit = async (data: RegisterForm) => {
    try {
      const docRule = DOC_VALIDATION[data.identityDocType];
      const sanitizedDocNumber = docRule ? docRule.sanitize(data.identityDocNumber) : data.identityDocNumber.trim();

      const request: RegistrationRequest = {
        fullName: data.fullName,
        email: data.email,
        mobile: data.mobile,
        password: data.password,
        identityDocType: data.identityDocType,
        identityDocNumber: sanitizedDocNumber,
      };
      await authRegister(request);
      toast.success('Registration initiated!', 'OTP has been sent to your email and mobile.');
      navigate('/verify');
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string } } };
      if (error.response?.status === 409) {
        toast.error('Already registered', 'An account with this email or mobile already exists.');
      } else {
        toast.error('Registration failed', error.response?.data?.message ?? 'Please try again.');
      }
    }
  };

  const inputClass =
    'w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent';
  const errorClass = 'text-red-500 text-xs mt-1';
  const labelClass = 'block text-sm font-medium text-gray-700 mb-1.5';

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        {/* Header */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center gap-3 mb-2">
            <div className="bg-indigo-500 p-2.5 rounded-xl">
              <BookOpen className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-bold text-white">NAG Candidate Registration</span>
          </div>
          <p className="text-slate-400 text-sm">Create your account to access examinations</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
            {/* Row 1: Full Name + Email */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>Full Name *</label>
                <input
                  {...register('fullName')}
                  type="text"
                  placeholder="As per identity document"
                  aria-invalid={!!errors.fullName}
                  className={inputClass}
                />
                {errors.fullName && <p className={errorClass}>{errors.fullName.message}</p>}
              </div>
              <div>
                <label className={labelClass}>Email Address *</label>
                <input
                  {...register('email')}
                  type="email"
                  placeholder="you@example.com"
                  aria-invalid={!!errors.email}
                  className={inputClass}
                />
                {errors.email && <p className={errorClass}>{errors.email.message}</p>}
              </div>
            </div>

            {/* Row 2: Mobile */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>Mobile Number *</label>
                <input
                  {...register('mobile')}
                  type="tel"
                  placeholder="10-digit mobile (e.g. 9876543210)"
                  aria-invalid={!!errors.mobile}
                  maxLength={10}
                  className={inputClass}
                />
                {errors.mobile && <p className={errorClass}>{errors.mobile.message}</p>}
              </div>
            </div>

            {/* Row 3: Identity Document */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>Identity Document Type *</label>
                <select
                  {...register('identityDocType')}
                  aria-invalid={!!errors.identityDocType}
                  className={inputClass}
                >
                  <option value="">Select document type</option>
                  {DOC_TYPES.map((d) => (
                    <option key={d.value} value={d.value}>
                      {d.label}
                    </option>
                  ))}
                </select>
                {errors.identityDocType && (
                  <p className={errorClass}>{errors.identityDocType.message}</p>
                )}
              </div>
              <div>
                <label className={labelClass}>
                  Document Number *{' '}
                  {activeDocConfig && (
                    <span className="text-xs font-normal text-indigo-600">
                      ({activeDocConfig.label})
                    </span>
                  )}
                </label>
                <input
                  {...register('identityDocNumber')}
                  type="text"
                  placeholder={activeDocConfig?.placeholder ?? 'Select document type first'}
                  maxLength={activeDocConfig?.maxLength ?? 25}
                  aria-invalid={!!errors.identityDocNumber}
                  className={`${inputClass} uppercase tracking-wider font-mono`}
                />
                {errors.identityDocNumber ? (
                  <p className={errorClass}>{errors.identityDocNumber.message}</p>
                ) : activeDocConfig ? (
                  <p className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                    <Info className="w-3 h-3 text-indigo-500 shrink-0" />
                    {activeDocConfig.hint}
                  </p>
                ) : null}
              </div>
            </div>

            {/* Row 4: Passwords */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelClass}>Password *</label>
                <div className="relative">
                  <input
                    {...register('password')}
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Min 8 chars, 1 uppercase, 1 number, 1 special"
                    aria-invalid={!!errors.password}
                    className={`${inputClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    aria-label="Toggle password visibility"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.password && <p className={errorClass}>{errors.password.message}</p>}
              </div>
              <div>
                <label className={labelClass}>Confirm Password *</label>
                <div className="relative">
                  <input
                    {...register('confirmPassword')}
                    type={showConfirm ? 'text' : 'password'}
                    placeholder="Re-enter password"
                    aria-invalid={!!errors.confirmPassword}
                    className={`${inputClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirm(!showConfirm)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    aria-label="Toggle confirm password visibility"
                  >
                    {showConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <p className={errorClass}>{errors.confirmPassword.message}</p>
                )}
              </div>
            </div>

            {/* Declaration */}
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  {...register('declaration')}
                  type="checkbox"
                  className="mt-0.5 w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                />
                <span className="text-xs text-gray-700">
                  I hereby declare that all information provided is true, complete, and accurate to
                  the best of my knowledge. I understand that providing false information may lead to
                  cancellation of my candidature.
                </span>
              </label>
              {errors.declaration && (
                <p className={`${errorClass} mt-1`}>{errors.declaration.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-lg transition text-sm"
            >
              {isSubmitting ? (
                <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                <UserPlus className="w-4 h-4" />
              )}
              {isSubmitting ? 'Creating account…' : 'Create Account'}
            </button>

            <p className="text-center text-sm text-gray-500">
              Already registered?{' '}
              <Link to="/login" className="text-indigo-600 font-medium hover:text-indigo-700">
                Sign in
              </Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Register;
