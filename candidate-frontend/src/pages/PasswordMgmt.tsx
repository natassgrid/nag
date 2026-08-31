// src/pages/PasswordMgmt.tsx
// Connected password change form → POST /api/v1/identity/auth/change-password

import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Lock, Eye, EyeOff, ShieldCheck } from 'lucide-react';
import { authService } from '../services/authService';
import { useToast } from '../components/Toast';
import { useState } from 'react';

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z
      .string()
      .min(8, 'Minimum 8 characters')
      .regex(/[A-Z]/, 'At least one uppercase letter')
      .regex(/[0-9]/, 'At least one number')
      .regex(/[^a-zA-Z0-9]/, 'At least one special character'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type Form = z.infer<typeof schema>;

const PasswordMgmt: React.FC = () => {
  const { toast } = useToast();
  const [show, setShow] = useState({ current: false, new: false, confirm: false });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: Form) => {
    try {
      await authService.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      toast.success('Password changed!', 'Your password has been updated successfully.');
      reset();
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string }; status?: number } };
      if (error.response?.status === 401 || error.response?.status === 403) {
        toast.error('Incorrect password', 'The current password you entered is wrong.');
      } else {
        toast.error('Change failed', error.response?.data?.message ?? 'Please try again.');
      }
    }
  };

  const toggleShow = (field: keyof typeof show) =>
    setShow((prev) => ({ ...prev, [field]: !prev[field] }));

  const inputCls =
    'w-full border border-gray-300 rounded-lg px-4 py-2.5 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500';
  const errCls = 'text-red-500 text-xs mt-1';
  const labelCls = 'block text-sm font-medium text-gray-700 mb-1.5';

  const fields: { name: keyof Form; label: string; showKey: keyof typeof show }[] = [
    { name: 'currentPassword', label: 'Current Password', showKey: 'current' },
    { name: 'newPassword', label: 'New Password', showKey: 'new' },
    { name: 'confirmPassword', label: 'Confirm New Password', showKey: 'confirm' },
  ];

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Change Password</h1>
        <p className="text-gray-500 text-sm mt-1">
          Keep your account secure by using a strong, unique password.
        </p>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="bg-indigo-100 p-2.5 rounded-xl">
            <Lock className="w-5 h-5 text-indigo-600" />
          </div>
          <div>
            <h2 className="font-semibold text-gray-800">Update Password</h2>
            <p className="text-xs text-gray-500">You'll need to log in again after changing.</p>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
          {fields.map(({ name, label, showKey }) => (
            <div key={name}>
              <label className={labelCls}>{label}</label>
              <div className="relative">
                <input
                  {...register(name)}
                  type={show[showKey] ? 'text' : 'password'}
                  aria-invalid={!!errors[name]}
                  placeholder="••••••••"
                  className={inputCls}
                />
                <button
                  type="button"
                  onClick={() => toggleShow(showKey)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  aria-label={`${show[showKey] ? 'Hide' : 'Show'} ${label.toLowerCase()}`}
                >
                  {show[showKey] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors[name] && <p className={errCls}>{errors[name]?.message}</p>}
            </div>
          ))}

          {/* Password strength hints */}
          <div className="bg-gray-50 rounded-lg p-3 text-xs text-gray-600 space-y-1">
            <p className="font-medium text-gray-700 mb-1 flex items-center gap-1">
              <ShieldCheck className="w-3.5 h-3.5" /> Password Requirements
            </p>
            {[
              'Minimum 8 characters',
              'At least one uppercase letter (A-Z)',
              'At least one number (0-9)',
              'At least one special character (!@#$…)',
            ].map((req) => (
              <p key={req} className="flex items-center gap-1.5">
                <span className="text-gray-400">•</span> {req}
              </p>
            ))}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold py-3 rounded-lg transition"
          >
            {isSubmitting ? (
              <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <Lock className="w-4 h-4" />
            )}
            {isSubmitting ? 'Changing password…' : 'Change Password'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default PasswordMgmt;
