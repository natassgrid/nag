// src/pages/Profile.tsx
// Connected profile management — GET on mount + PUT on save via candidateService.

import React, { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User, MapPin, GraduationCap, Upload, CheckCircle, AlertCircle, Save, Loader2,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { candidateService } from '../services/candidateService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from '../components/Toast';
import type { CandidateProfileResponse } from '../types/api';

// ─── Schemas ──────────────────────────────────────────────────────────────────

const personalSchema = z.object({
  firstName: z.string().min(1, 'First name is required'),
  lastName: z.string().min(1, 'Last name is required'),
  dateOfBirth: z.string().min(1, 'Date of birth is required'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'] as const),
  category: z.enum(['GENERAL', 'OBC', 'SC', 'ST', 'EWS'] as const),
});

const addressSchema = z.object({
  street: z.string().min(1, 'Street is required'),
  city: z.string().min(1, 'City is required'),
  district: z.string().min(1, 'District is required'),
  state: z.string().min(1, 'State is required'),
  pincode: z.string().regex(/^\d{6}$/, '6-digit pincode required'),
  country: z.string().min(1, 'Country is required'),
});

type PersonalForm = z.infer<typeof personalSchema>;
type AddressForm = z.infer<typeof addressSchema>;

// ─── Component ────────────────────────────────────────────────────────────────

type Tab = 'personal' | 'contact' | 'education' | 'documents';

const TABS: { id: Tab; name: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'personal', name: 'Personal Details', icon: User },
  { id: 'contact', name: 'Contact & Address', icon: MapPin },
  { id: 'education', name: 'Education', icon: GraduationCap },
  { id: 'documents', name: 'Documents', icon: Upload },
];

const Profile: React.FC = () => {
  const { profile, refreshProfile } = useAuth();
  const { toast } = useToast();
  const userId = tokenManager.getUserId();

  const [activeTab, setActiveTab] = useState<Tab>('personal');
  const [loading, setLoading] = useState(!profile);
  const [currentProfile, setCurrentProfile] = useState<CandidateProfileResponse | null>(profile);
  const [uploading, setUploading] = useState<Record<string, boolean>>({});
  const [uploadedFiles, setUploadedFiles] = useState<Record<string, string>>({});

  const photoRef = useRef<HTMLInputElement>(null);
  const sigRef = useRef<HTMLInputElement>(null);
  const idRef = useRef<HTMLInputElement>(null);

  // Load profile on mount
  useEffect(() => {
    if (profile) {
      setCurrentProfile(profile);
      setLoading(false);
      return;
    }
    if (!userId) return;
    setLoading(true);
    candidateService.getProfile(userId)
      .then((p) => { setCurrentProfile(p); })
      .catch(() => toast.error('Failed to load profile'))
      .finally(() => setLoading(false));
  }, [profile, userId, toast]);

  // ── Personal form ──────────────────────────────────────────────────────────
  const {
    register: personalRegister,
    handleSubmit: handlePersonalSubmit,
    formState: { errors: personalErrors, isSubmitting: personalSaving },
    reset: resetPersonal,
  } = useForm<PersonalForm>({
    resolver: zodResolver(personalSchema),
    defaultValues: {
      firstName: currentProfile?.firstName ?? '',
      lastName: currentProfile?.lastName ?? '',
      dateOfBirth: currentProfile?.dateOfBirth ?? '',
      gender: currentProfile?.gender ?? 'PREFER_NOT_TO_SAY',
      category: currentProfile?.category ?? 'GENERAL',
    },
  });

  useEffect(() => {
    if (currentProfile) {
      resetPersonal({
        firstName: currentProfile.firstName,
        lastName: currentProfile.lastName,
        dateOfBirth: currentProfile.dateOfBirth,
        gender: currentProfile.gender,
        category: currentProfile.category,
      });
    }
  }, [currentProfile, resetPersonal]);

  const onSavePersonal = async (data: PersonalForm) => {
    if (!userId) return;
    try {
      const updated = await candidateService.updateProfile(userId, data);
      setCurrentProfile(updated);
      await refreshProfile();
      toast.success('Personal details saved!');
    } catch {
      toast.error('Save failed', 'Please try again.');
    }
  };

  // ── Address form ───────────────────────────────────────────────────────────
  const {
    register: addressRegister,
    handleSubmit: handleAddressSubmit,
    formState: { errors: addressErrors, isSubmitting: addressSaving },
    reset: resetAddress,
  } = useForm<AddressForm>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      street: currentProfile?.address?.street ?? '',
      city: currentProfile?.address?.city ?? '',
      district: currentProfile?.address?.district ?? '',
      state: currentProfile?.address?.state ?? '',
      pincode: currentProfile?.address?.pincode ?? '',
      country: currentProfile?.address?.country ?? 'India',
    },
  });

  useEffect(() => {
    if (currentProfile?.address) {
      resetAddress(currentProfile.address);
    }
  }, [currentProfile, resetAddress]);

  const onSaveAddress = async (data: AddressForm) => {
    if (!userId) return;
    try {
      const updated = await candidateService.updateProfile(userId, { address: data });
      setCurrentProfile(updated);
      await refreshProfile();
      toast.success('Address saved!');
    } catch {
      toast.error('Save failed', 'Please try again.');
    }
  };

  // ── Document upload ────────────────────────────────────────────────────────
  const handleFileUpload = async (
    file: File,
    type: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF',
  ) => {
    setUploading((prev) => ({ ...prev, [type]: true }));
    try {
      const asset = await candidateService.uploadDocument(file, type);
      setUploadedFiles((prev) => ({ ...prev, [type]: asset.originalFilename }));
      toast.success(`${type.replace('_', ' ')} uploaded!`, asset.originalFilename);
    } catch {
      toast.error('Upload failed', 'Check file size (max 5MB) and format.');
    } finally {
      setUploading((prev) => ({ ...prev, [type]: false }));
    }
  };

  // ── Helpers ────────────────────────────────────────────────────────────────
  const inputCls = 'w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500';
  const errCls = 'text-red-500 text-xs mt-1';
  const labelCls = 'block text-sm font-medium text-gray-700 mb-1';

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  const completeness = currentProfile?.completionPercentage ?? 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Profile Management</h1>
          <p className="text-gray-500 text-sm mt-1">
            Keep your profile updated to maintain exam eligibility.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {completeness < 100 ? (
            <span className="flex items-center gap-1 text-amber-700 bg-amber-50 border border-amber-200 text-xs px-3 py-1.5 rounded-full">
              <AlertCircle className="w-3.5 h-3.5" /> {completeness}% complete
            </span>
          ) : (
            <span className="flex items-center gap-1 text-green-700 bg-green-50 border border-green-200 text-xs px-3 py-1.5 rounded-full">
              <CheckCircle className="w-3.5 h-3.5" /> Profile complete
            </span>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-1 overflow-x-auto">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 whitespace-nowrap transition ${
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.name}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab panels */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">

        {/* Personal */}
        {activeTab === 'personal' && (
          <form onSubmit={handlePersonalSubmit(onSavePersonal)} noValidate className="space-y-5">
            <h2 className="font-semibold text-gray-800 mb-4">Personal Information</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>First Name *</label>
                <input {...personalRegister('firstName')} className={inputCls} />
                {personalErrors.firstName && <p className={errCls}>{personalErrors.firstName.message}</p>}
              </div>
              <div>
                <label className={labelCls}>Last Name *</label>
                <input {...personalRegister('lastName')} className={inputCls} />
                {personalErrors.lastName && <p className={errCls}>{personalErrors.lastName.message}</p>}
              </div>
              <div>
                <label className={labelCls}>Date of Birth *</label>
                <input {...personalRegister('dateOfBirth')} type="date" className={inputCls} />
                {personalErrors.dateOfBirth && <p className={errCls}>{personalErrors.dateOfBirth.message}</p>}
              </div>
              <div>
                <label className={labelCls}>Gender *</label>
                <select {...personalRegister('gender')} className={inputCls}>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                  <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
              </div>
              <div>
                <label className={labelCls}>Category *</label>
                <select {...personalRegister('category')} className={inputCls}>
                  {['GENERAL', 'OBC', 'SC', 'ST', 'EWS'].map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
            </div>
            <button
              type="submit"
              disabled={personalSaving}
              className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-5 py-2.5 rounded-lg text-sm font-medium transition"
            >
              {personalSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              {personalSaving ? 'Saving…' : 'Save Personal Details'}
            </button>
          </form>
        )}

        {/* Contact / Address */}
        {activeTab === 'contact' && (
          <form onSubmit={handleAddressSubmit(onSaveAddress)} noValidate className="space-y-5">
            <h2 className="font-semibold text-gray-800 mb-4">Contact & Address</h2>

            {/* Read-only contact info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className={labelCls}>Email (read-only)</label>
                <input
                  type="email"
                  value={currentProfile?.userId ?? ''}
                  readOnly
                  className={`${inputCls} bg-gray-50 text-gray-500`}
                />
                <p className="text-xs text-gray-400 mt-1">Change via identity service.</p>
              </div>
            </div>

            <h3 className="font-medium text-gray-700 mt-4">Correspondence Address</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <label className={labelCls}>Street / Door No. *</label>
                <input {...addressRegister('street')} className={inputCls} />
                {addressErrors.street && <p className={errCls}>{addressErrors.street.message}</p>}
              </div>
              <div>
                <label className={labelCls}>City *</label>
                <input {...addressRegister('city')} className={inputCls} />
                {addressErrors.city && <p className={errCls}>{addressErrors.city.message}</p>}
              </div>
              <div>
                <label className={labelCls}>District *</label>
                <input {...addressRegister('district')} className={inputCls} />
                {addressErrors.district && <p className={errCls}>{addressErrors.district.message}</p>}
              </div>
              <div>
                <label className={labelCls}>State *</label>
                <input {...addressRegister('state')} className={inputCls} />
                {addressErrors.state && <p className={errCls}>{addressErrors.state.message}</p>}
              </div>
              <div>
                <label className={labelCls}>PIN Code *</label>
                <input {...addressRegister('pincode')} maxLength={6} className={inputCls} />
                {addressErrors.pincode && <p className={errCls}>{addressErrors.pincode.message}</p>}
              </div>
            </div>

            <button
              type="submit"
              disabled={addressSaving}
              className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-5 py-2.5 rounded-lg text-sm font-medium transition"
            >
              {addressSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              {addressSaving ? 'Saving…' : 'Save Address'}
            </button>
          </form>
        )}

        {/* Education */}
        {activeTab === 'education' && (
          <div className="space-y-5">
            <h2 className="font-semibold text-gray-800 mb-4">Education History</h2>
            {currentProfile?.education ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { label: 'Qualification', value: currentProfile.education.qualification },
                  { label: 'Board / University', value: currentProfile.education.boardOrUniversity },
                  { label: 'Passing Year', value: String(currentProfile.education.passingYear) },
                  { label: 'Percentage / CGPA', value: String(currentProfile.education.percentage) },
                ].map((f) => (
                  <div key={f.label}>
                    <label className={labelCls}>{f.label}</label>
                    <input value={f.value} readOnly className={`${inputCls} bg-gray-50 text-gray-500`} />
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-400 text-sm">No education details on file. Contact support to update.</p>
            )}
            <p className="text-xs text-gray-400">
              Education records are set at registration. Contact support to amend.
            </p>
          </div>
        )}

        {/* Documents */}
        {activeTab === 'documents' && (
          <div className="space-y-5">
            <h2 className="font-semibold text-gray-800 mb-2">Document Uploads</h2>
            <p className="text-sm text-gray-500 mb-4">
              Upload passport-size photo, signature, and ID proof. Accepted: JPG, PNG, PDF. Max 5MB each.
            </p>
            {[
              { key: 'PHOTO' as const, label: 'Passport Photo', ref: photoRef, accept: 'image/jpeg,image/png' },
              { key: 'SIGNATURE' as const, label: 'Signature (white background)', ref: sigRef, accept: 'image/jpeg,image/png' },
              { key: 'ID_PROOF' as const, label: 'ID Proof Document', ref: idRef, accept: 'image/jpeg,image/png,application/pdf' },
            ].map(({ key, label, ref, accept }) => (
              <div key={key} className="flex items-center justify-between p-4 border border-gray-200 rounded-xl">
                <div>
                  <p className="text-sm font-medium text-gray-800">{label}</p>
                  {uploadedFiles[key] ? (
                    <p className="text-xs text-green-600 flex items-center gap-1 mt-0.5">
                      <CheckCircle className="w-3 h-3" /> {uploadedFiles[key]}
                    </p>
                  ) : (
                    <p className="text-xs text-gray-400 mt-0.5">Not uploaded</p>
                  )}
                </div>
                <div>
                  <input
                    ref={ref}
                    type="file"
                    accept={accept}
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) void handleFileUpload(file, key);
                    }}
                  />
                  <button
                    type="button"
                    onClick={() => ref.current?.click()}
                    disabled={uploading[key]}
                    className="flex items-center gap-2 bg-gray-100 hover:bg-gray-200 disabled:bg-gray-50 text-gray-700 text-sm px-4 py-2 rounded-lg transition"
                  >
                    {uploading[key] ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <Upload className="w-4 h-4" />
                    )}
                    {uploading[key] ? 'Uploading…' : 'Upload'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
