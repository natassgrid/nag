// src/pages/Profile.tsx
// Complete Candidate Profile Management integrated with candidate-service & asset-service.

import React, { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User,
  MapPin,
  ShieldCheck,
  Upload,
  CheckCircle,
  AlertCircle,
  Save,
  Loader2,
  Trash2,
  Lock,
  FileCheck,
  RefreshCw,
  AlertTriangle,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { candidateService } from '../services/candidateService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from '../components/Toast';
import type {
  CandidateProfileResponse,
  CreateCandidateProfileRequest,
  UpdateCandidateProfileRequest,
} from '../types/api';

// ─── Validation Schemas ───────────────────────────────────────────────────────

const personalSchema = z.object({
  fullName: z.string().min(2, 'Full name must be at least 2 characters'),
  dateOfBirth: z.string().min(1, 'Date of birth is required'),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'] as const),
  nationality: z.string().min(2, 'Nationality is required'),
  category: z.enum(['GENERAL', 'OBC', 'SC', 'ST', 'EWS'] as const),
  reservationCategory: z.string().optional(),
  identityDocNumber: z.string().optional(),
});

const contactSchema = z.object({
  mobile: z.string().min(10, 'Valid mobile number is required'),
  email: z.string().email('Valid email address is required'),
  address: z.string().min(5, 'Please provide full correspondence address'),
});

type PersonalForm = z.infer<typeof personalSchema>;
type ContactForm = z.infer<typeof contactSchema>;

// ─── Tabs Definition ─────────────────────────────────────────────────────────

type Tab = 'personal' | 'contact' | 'verification' | 'documents';

const TABS: { id: Tab; name: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'personal', name: 'Personal Details', icon: User },
  { id: 'contact', name: 'Contact & Address', icon: MapPin },
  { id: 'verification', name: 'KYC & DPDP Consent', icon: ShieldCheck },
  { id: 'documents', name: 'Document Uploads', icon: Upload },
];

const Profile: React.FC = () => {
  const { profile, refreshProfile, logout } = useAuth();
  const { toast } = useToast();
  const userId = tokenManager.getUserId();

  const [activeTab, setActiveTab] = useState<Tab>('personal');
  const [loading, setLoading] = useState(!profile);
  const [isNewProfile, setIsNewProfile] = useState(false);
  const [currentProfile, setCurrentProfile] = useState<CandidateProfileResponse | null>(profile);

  // Document uploads & KYC states
  const [uploading, setUploading] = useState<Record<string, boolean>>({});
  const [uploadedFiles, setUploadedFiles] = useState<Record<string, string>>({});
  const [digiLockerLoading, setDigiLockerLoading] = useState(false);
  const [consentLoading, setConsentLoading] = useState(false);
  const [eraseModalOpen, setEraseModalOpen] = useState(false);
  const [erasingPii, setErasingPii] = useState(false);

  const photoRef = useRef<HTMLInputElement>(null);
  const sigRef = useRef<HTMLInputElement>(null);
  const idRef = useRef<HTMLInputElement>(null);

  // Load profile on mount
  useEffect(() => {
    const uid = userId || tokenManager.getUserId();
    if (!uid || uid === 'undefined' || uid === 'null') {
      setLoading(false);
      setIsNewProfile(true);
      return;
    }
    setLoading(true);
    candidateService.getProfile(uid)
      .then((p) => {
        setCurrentProfile(p);
        setIsNewProfile(false);
      })
      .catch((err: unknown) => {
        // 404 means candidate has registered but not yet created profile entity
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 404) {
          setIsNewProfile(true);
        } else {
          toast.error('Failed to load profile', 'Please check network connection.');
        }
      })
      .finally(() => setLoading(false));
  }, [userId, toast]);

  // ── Personal Form ──────────────────────────────────────────────────────────
  const {
    register: personalRegister,
    handleSubmit: handlePersonalSubmit,
    formState: { errors: personalErrors, isSubmitting: personalSaving },
    reset: resetPersonal,
  } = useForm<PersonalForm>({
    resolver: zodResolver(personalSchema),
    defaultValues: {
      fullName: currentProfile?.fullName ?? '',
      dateOfBirth: currentProfile?.dateOfBirth ?? '',
      gender: (currentProfile?.gender as PersonalForm['gender']) ?? 'MALE',
      nationality: currentProfile?.nationality ?? 'INDIAN',
      category: (currentProfile?.category as PersonalForm['category']) ?? 'GENERAL',
      reservationCategory: currentProfile?.reservationCategory ?? '',
      identityDocNumber: currentProfile?.identityDocNumber ?? '',
    },
  });

  useEffect(() => {
    if (currentProfile) {
      resetPersonal({
        fullName: currentProfile.fullName ?? '',
        dateOfBirth: currentProfile.dateOfBirth ?? '',
        gender: (currentProfile.gender as PersonalForm['gender']) ?? 'MALE',
        nationality: currentProfile.nationality ?? 'INDIAN',
        category: (currentProfile.category as PersonalForm['category']) ?? 'GENERAL',
        reservationCategory: currentProfile.reservationCategory ?? '',
        identityDocNumber: currentProfile.identityDocNumber ?? '',
      });
    }
  }, [currentProfile, resetPersonal]);

  const onSavePersonal = async (data: PersonalForm) => {
    const uid = userId || tokenManager.getUserId();
    if (!uid || uid === 'undefined') {
      toast.error('Authentication Error', 'Please login again to update profile.');
      return;
    }
    try {
      if (isNewProfile) {
        // Create initial profile with required fields
        const createReq: CreateCandidateProfileRequest = {
          userId: uid,
          fullName: data.fullName,
          dateOfBirth: data.dateOfBirth,
          gender: data.gender,
          nationality: data.nationality,
          category: data.category,
          reservationCategory: data.reservationCategory || undefined,
          identityDocNumber: data.identityDocNumber || 'NOT_SPECIFIED',
          mobile: currentProfile?.mobile ?? '9999999999',
          email: currentProfile?.email ?? `${uid}@candidate.nag.gov.in`,
          address: currentProfile?.address ?? '',
        };
        const created = await candidateService.createProfile(createReq);
        setCurrentProfile(created);
        setIsNewProfile(false);
        await refreshProfile();
        toast.success('Profile created successfully!');
      } else {
        const updateReq: UpdateCandidateProfileRequest = {
          fullName: data.fullName,
          dateOfBirth: data.dateOfBirth,
          gender: data.gender,
          nationality: data.nationality,
          category: data.category,
          reservationCategory: data.reservationCategory || undefined,
          identityDocNumber: data.identityDocNumber || undefined,
        };
        const updated = await candidateService.updateProfile(uid, updateReq);
        setCurrentProfile(updated);
        await refreshProfile();
        toast.success('Personal details saved!');
      }
    } catch {
      toast.error('Save failed', 'Please verify your details and try again.');
    }
  };

  // ── Contact Form ───────────────────────────────────────────────────────────
  const {
    register: contactRegister,
    handleSubmit: handleContactSubmit,
    formState: { errors: contactErrors, isSubmitting: contactSaving },
    reset: resetContact,
  } = useForm<ContactForm>({
    resolver: zodResolver(contactSchema),
    defaultValues: {
      mobile: currentProfile?.mobile ?? '',
      email: currentProfile?.email ?? '',
      address: currentProfile?.address ?? '',
    },
  });

  useEffect(() => {
    if (currentProfile) {
      resetContact({
        mobile: currentProfile.mobile ?? '',
        email: currentProfile.email ?? '',
        address: currentProfile.address ?? '',
      });
    }
  }, [currentProfile, resetContact]);

  const onSaveContact = async (data: ContactForm) => {
    const uid = userId || tokenManager.getUserId();
    if (!uid || uid === 'undefined') {
      toast.error('Authentication Error', 'Please login again.');
      return;
    }
    try {
      if (isNewProfile) {
        const createReq: CreateCandidateProfileRequest = {
          userId: uid,
          fullName: currentProfile?.fullName ?? 'Candidate',
          dateOfBirth: currentProfile?.dateOfBirth ?? '2000-01-01',
          gender: currentProfile?.gender ?? 'MALE',
          nationality: currentProfile?.nationality ?? 'INDIAN',
          category: currentProfile?.category ?? 'GENERAL',
          mobile: data.mobile,
          email: data.email,
          address: data.address,
          identityDocNumber: 'NOT_SPECIFIED',
        };
        const created = await candidateService.createProfile(createReq);
        setCurrentProfile(created);
        setIsNewProfile(false);
      } else {
        const updateReq: UpdateCandidateProfileRequest = {
          address: data.address,
        };
        // If mobile / email are not masked values (user actually edited them)
        if (data.mobile && !data.mobile.startsWith('****')) {
          updateReq.mobile = data.mobile;
        }
        if (data.email && !data.email.startsWith('**')) {
          updateReq.email = data.email;
        }
        const updated = await candidateService.updateProfile(uid, updateReq);
        setCurrentProfile(updated);
      }
      await refreshProfile();
      toast.success('Contact details saved!');
    } catch {
      toast.error('Save failed', 'Please try again.');
    }
  };

  // ── DigiLocker Verification ────────────────────────────────────────────────
  const handleVerifyDigiLocker = async () => {
    const uid = userId || tokenManager.getUserId();
    if (!uid) return;
    setDigiLockerLoading(true);
    try {
      const res = await candidateService.verifyDigiLocker(uid);
      toast.success('DigiLocker Verification', `Status: ${res.status}`);
      // Refresh profile to reflect verified status
      const updated = await candidateService.getProfile(uid);
      setCurrentProfile(updated);
      await refreshProfile();
    } catch {
      toast.error('DigiLocker Verification Failed', 'Could not verify document.');
    } finally {
      setDigiLockerLoading(false);
    }
  };

  // ── DPDP Consent Recording ─────────────────────────────────────────────────
  const handleRecordConsent = async () => {
    const uid = userId || tokenManager.getUserId();
    if (!uid) return;
    setConsentLoading(true);
    try {
      await candidateService.recordConsent(uid, { consentGiven: true, consentVersion: 'v1.0' });
      toast.success('Consent Recorded', 'Biometric & identity DPDP consent has been recorded.');
      const updated = await candidateService.getProfile(uid);
      setCurrentProfile(updated);
      await refreshProfile();
    } catch {
      toast.error('Consent recording failed', 'Please try again.');
    } finally {
      setConsentLoading(false);
    }
  };

  // ── DPDP PII Erasure (Right to be Forgotten) ───────────────────────────────
  const handleErasePii = async () => {
    const uid = userId || tokenManager.getUserId();
    if (!uid) return;
    setErasingPii(true);
    try {
      await candidateService.erasePii(uid);
      toast.success('Data Erased', 'Your PII has been erased as per DPDP Act.');
      setEraseModalOpen(false);
      // Log user out as their profile is wiped
      await logout();
    } catch {
      toast.error('Erasure failed', 'Could not process erasure request.');
    } finally {
      setErasingPii(false);
    }
  };

  // ── Document Upload ────────────────────────────────────────────────────────
  const handleFileUpload = async (
    file: File,
    type: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF',
  ) => {
    setUploading((prev) => ({ ...prev, [type]: true }));
    try {
      const asset = await candidateService.uploadDocument(file, type);
      setUploadedFiles((prev) => ({ ...prev, [type]: asset.originalFilename }));
      toast.success(`${type.replace('_', ' ')} uploaded!`, `Asset ID: ${asset.id}`);
    } catch {
      toast.error('Upload failed', 'Ensure file is under 5MB (JPG, PNG, PDF).');
    } finally {
      setUploading((prev) => ({ ...prev, [type]: false }));
    }
  };

  // ── Calculate Profile Completeness ─────────────────────────────────────────
  const calculateCompleteness = () => {
    if (!currentProfile) return isNewProfile ? 10 : 0;
    let score = 0;
    if (currentProfile.fullName) score += 20;
    if (currentProfile.dateOfBirth) score += 15;
    if (currentProfile.gender) score += 10;
    if (currentProfile.category) score += 10;
    if (currentProfile.address) score += 15;
    if (currentProfile.mobile) score += 10;
    if (currentProfile.email) score += 10;
    if (currentProfile.consentRecorded) score += 10;
    return Math.min(100, score);
  };

  const completeness = calculateCompleteness();

  // ── Styling constants ──────────────────────────────────────────────────────
  const inputCls = 'w-full border border-gray-300 rounded-lg px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white transition';
  const errCls = 'text-red-500 text-xs mt-1';
  const labelCls = 'block text-sm font-medium text-gray-700 mb-1';

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24">
        <Loader2 className="w-10 h-10 animate-spin text-indigo-600 mb-3" />
        <p className="text-sm text-gray-500 font-medium">Loading candidate profile…</p>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-gray-900">
              {currentProfile?.fullName || 'Candidate Profile'}
            </h1>
            {currentProfile?.digiLockerVerified === 'VERIFIED' && (
              <span className="inline-flex items-center gap-1 bg-green-50 border border-green-200 text-green-700 text-xs px-2.5 py-1 rounded-full font-medium">
                <CheckCircle className="w-3.5 h-3.5" /> DigiLocker Verified
              </span>
            )}
          </div>
          <p className="text-gray-500 text-sm mt-1">
            User ID: <span className="font-mono text-xs text-gray-600">{userId || 'Not Logged In'}</span>
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <p className="text-xs text-gray-500">Profile Completeness</p>
            <p className="text-sm font-bold text-gray-800">{completeness}%</p>
          </div>
          <div className="w-28 bg-gray-100 rounded-full h-3 overflow-hidden border border-gray-200">
            <div
              className={`h-full transition-all duration-500 ${
                completeness >= 80 ? 'bg-green-500' : completeness >= 50 ? 'bg-amber-500' : 'bg-indigo-500'
              }`}
              style={{ width: `${completeness}%` }}
            />
          </div>
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="border-b border-gray-200 bg-white rounded-xl shadow-sm px-4">
        <nav className="flex gap-2 overflow-x-auto">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-3.5 text-sm font-semibold border-b-2 whitespace-nowrap transition ${
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-800 hover:border-gray-300'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.name}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Contents */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">

        {/* ── TAB 1: Personal Details ──────────────────────────────────────── */}
        {activeTab === 'personal' && (
          <form onSubmit={handlePersonalSubmit(onSavePersonal)} noValidate className="space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Personal & Identity Information</h2>
                <p className="text-xs text-gray-500 mt-0.5">Encrypted with per-candidate AES-256 keys</p>
              </div>
              <span className="flex items-center gap-1 text-xs text-indigo-700 bg-indigo-50 px-2.5 py-1 rounded-md">
                <Lock className="w-3.5 h-3.5" /> End-to-End Encrypted PII
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label className={labelCls}>Full Name *</label>
                <input
                  {...personalRegister('fullName')}
                  placeholder="e.g. Rahul Sharma"
                  className={inputCls}
                />
                {personalErrors.fullName && <p className={errCls}>{personalErrors.fullName.message}</p>}
              </div>

              <div>
                <label className={labelCls}>Date of Birth *</label>
                <input
                  {...personalRegister('dateOfBirth')}
                  type="date"
                  className={inputCls}
                />
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
                <label className={labelCls}>Nationality *</label>
                <input
                  {...personalRegister('nationality')}
                  placeholder="INDIAN"
                  className={inputCls}
                />
                {personalErrors.nationality && <p className={errCls}>{personalErrors.nationality.message}</p>}
              </div>

              <div>
                <label className={labelCls}>Category *</label>
                <select {...personalRegister('category')} className={inputCls}>
                  <option value="GENERAL">General (Unreserved)</option>
                  <option value="OBC">OBC (Other Backward Classes)</option>
                  <option value="SC">SC (Scheduled Caste)</option>
                  <option value="ST">ST (Scheduled Tribe)</option>
                  <option value="EWS">EWS (Economically Weaker Section)</option>
                </select>
              </div>

              <div>
                <label className={labelCls}>Reservation / Sub-Category (Optional)</label>
                <input
                  {...personalRegister('reservationCategory')}
                  placeholder="e.g. PwD-VI, Ex-Serviceman"
                  className={inputCls}
                />
              </div>

              <div className="sm:col-span-2">
                <label className={labelCls}>Identity Document Number</label>
                <input
                  {...personalRegister('identityDocNumber')}
                  placeholder="Identity Document Number (e.g. Aadhaar / PAN / Passport)"
                  className={inputCls}
                />
                <p className="text-xs text-gray-400 mt-1">
                  Stored as SHA-256 HMAC hash for verification without plain-text exposure.
                </p>
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-gray-100">
              <button
                type="submit"
                disabled={personalSaving}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-6 py-2.5 rounded-lg text-sm font-semibold shadow-sm transition"
              >
                {personalSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {personalSaving ? 'Saving…' : 'Save Personal Details'}
              </button>
            </div>
          </form>
        )}

        {/* ── TAB 2: Contact & Address ─────────────────────────────────────── */}
        {activeTab === 'contact' && (
          <form onSubmit={handleContactSubmit(onSaveContact)} noValidate className="space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Contact & Correspondence Address</h2>
                <p className="text-xs text-gray-500 mt-0.5">Used for exam admit card & delivery notifications</p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label className={labelCls}>Mobile Number *</label>
                <input
                  {...contactRegister('mobile')}
                  placeholder="+91 9876543210"
                  className={inputCls}
                />
                {contactErrors.mobile && <p className={errCls}>{contactErrors.mobile.message}</p>}
              </div>

              <div>
                <label className={labelCls}>Email Address *</label>
                <input
                  {...contactRegister('email')}
                  type="email"
                  placeholder="candidate@example.com"
                  className={inputCls}
                />
                {contactErrors.email && <p className={errCls}>{contactErrors.email.message}</p>}
              </div>

              <div className="sm:col-span-2">
                <label className={labelCls}>Correspondence Address *</label>
                <textarea
                  {...contactRegister('address')}
                  rows={4}
                  placeholder="Flat / House No., Building Name, Street / Road, Area / Locality, City, State, PIN Code"
                  className={inputCls}
                />
                {contactErrors.address && <p className={errCls}>{contactErrors.address.message}</p>}
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-gray-100">
              <button
                type="submit"
                disabled={contactSaving}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-6 py-2.5 rounded-lg text-sm font-semibold shadow-sm transition"
              >
                {contactSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {contactSaving ? 'Saving…' : 'Save Address Details'}
              </button>
            </div>
          </form>
        )}

        {/* ── TAB 3: KYC & DPDP Consent ────────────────────────────────────── */}
        {activeTab === 'verification' && (
          <div className="space-y-8">
            <div>
              <h2 className="text-lg font-bold text-gray-900">Identity Verification & DPDP Compliance</h2>
              <p className="text-xs text-gray-500 mt-0.5">
                Digital Personal Data Protection (DPDP) Act 2023 controls and automated DigiLocker verification
              </p>
            </div>

            {/* DigiLocker Section */}
            <div className="border border-gray-200 rounded-2xl p-5 bg-gradient-to-r from-blue-50/50 to-indigo-50/50">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-start gap-3.5">
                  <div className="p-2.5 bg-blue-100 text-blue-700 rounded-xl mt-0.5">
                    <FileCheck className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900">DigiLocker Identity Verification</h3>
                    <p className="text-xs text-gray-600 mt-1 max-w-xl">
                      Instantly verify candidate identity document credentials against the National DigiLocker repository.
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-xs text-gray-500">Current Status:</span>
                      {currentProfile?.digiLockerVerified === 'VERIFIED' ? (
                        <span className="text-xs font-semibold text-green-700 bg-green-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <CheckCircle className="w-3 h-3" /> Verified
                        </span>
                      ) : (
                        <span className="text-xs font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <AlertCircle className="w-3 h-3" /> {currentProfile?.digiLockerVerified || 'Pending Verification'}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleVerifyDigiLocker}
                  disabled={digiLockerLoading}
                  className="flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm transition whitespace-nowrap"
                >
                  {digiLockerLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
                  {digiLockerLoading ? 'Verifying…' : 'Verify with DigiLocker'}
                </button>
              </div>
            </div>

            {/* DPDP Biometric & Identity Consent Section */}
            <div className="border border-gray-200 rounded-2xl p-5 bg-gray-50/70">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-start gap-3.5">
                  <div className="p-2.5 bg-emerald-100 text-emerald-700 rounded-xl mt-0.5">
                    <ShieldCheck className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-900">Biometric & Identity Consent (DPDP Section 6)</h3>
                    <p className="text-xs text-gray-600 mt-1 max-w-xl">
                      Explicit consent for proctoring snapshots, face verification, and exam validation under the DPDP Act.
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-xs text-gray-500">Status:</span>
                      {currentProfile?.consentRecorded ? (
                        <span className="text-xs font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <CheckCircle className="w-3 h-3" /> Consent Recorded
                        </span>
                      ) : (
                        <span className="text-xs font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <AlertCircle className="w-3 h-3" /> Not Yet Given
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleRecordConsent}
                  disabled={consentLoading || currentProfile?.consentRecorded}
                  className="flex items-center justify-center gap-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-300 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm transition whitespace-nowrap"
                >
                  {consentLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
                  {currentProfile?.consentRecorded ? 'Consent Active' : 'Give DPDP Consent'}
                </button>
              </div>
            </div>

            {/* DPDP Section: Right to be Forgotten (Danger Zone) */}
            <div className="border border-red-200 rounded-2xl p-5 bg-red-50/50">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-start gap-3.5">
                  <div className="p-2.5 bg-red-100 text-red-700 rounded-xl mt-0.5">
                    <Trash2 className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-bold text-red-900">Right to be Forgotten (DPDP Section 12)</h3>
                    <p className="text-xs text-red-700 mt-1 max-w-xl">
                      Erase all personally identifiable information (PII) from NAG and revoke encryption keys. This will permanently deactivate your profile.
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setEraseModalOpen(true)}
                  className="flex items-center justify-center gap-2 bg-red-600 hover:bg-red-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold shadow-sm transition whitespace-nowrap"
                >
                  <Trash2 className="w-4 h-4" /> Request PII Erasure
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ── TAB 4: Document Uploads ──────────────────────────────────────── */}
        {activeTab === 'documents' && (
          <div className="space-y-6">
            <div className="pb-3 border-b border-gray-100">
              <h2 className="text-lg font-bold text-gray-900">Candidate Document Uploads</h2>
              <p className="text-xs text-gray-500 mt-0.5">
                Upload passport photo, signature, and government ID proof (JPG, PNG, or PDF up to 5MB)
              </p>
            </div>

            <div className="grid grid-cols-1 gap-4">
              {[
                {
                  key: 'PHOTO' as const,
                  label: 'Passport Photograph',
                  desc: 'Recent color photograph with white or light background (JPG/PNG, max 5MB)',
                  ref: photoRef,
                  accept: 'image/jpeg,image/png',
                },
                {
                  key: 'SIGNATURE' as const,
                  label: 'Official Signature',
                  desc: 'Clear signature on white paper with dark ink (JPG/PNG, max 2MB)',
                  ref: sigRef,
                  accept: 'image/jpeg,image/png',
                },
                {
                  key: 'ID_PROOF' as const,
                  label: 'Government Identity Proof',
                  desc: 'Scanned copy of Aadhaar / PAN / Passport / Voter ID (JPG/PNG/PDF, max 5MB)',
                  ref: idRef,
                  accept: 'image/jpeg,image/png,application/pdf',
                },
              ].map(({ key, label, desc, ref, accept }) => (
                <div
                  key={key}
                  className="flex flex-col sm:flex-row sm:items-center justify-between p-5 border border-gray-200 hover:border-indigo-200 rounded-2xl bg-white transition gap-4"
                >
                  <div className="space-y-1">
                    <p className="text-sm font-bold text-gray-900">{label}</p>
                    <p className="text-xs text-gray-500">{desc}</p>
                    {uploadedFiles[key] ? (
                      <p className="text-xs text-green-600 font-medium flex items-center gap-1.5 pt-1">
                        <CheckCircle className="w-3.5 h-3.5" /> Uploaded: {uploadedFiles[key]}
                      </p>
                    ) : (
                      <p className="text-xs text-gray-400 pt-1">No file uploaded yet</p>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
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
                      className="flex items-center gap-2 bg-indigo-50 hover:bg-indigo-100 disabled:bg-gray-50 text-indigo-700 text-sm font-semibold px-4 py-2.5 rounded-xl transition border border-indigo-200"
                    >
                      {uploading[key] ? (
                        <Loader2 className="w-4 h-4 animate-spin text-indigo-600" />
                      ) : (
                        <Upload className="w-4 h-4 text-indigo-600" />
                      )}
                      {uploading[key] ? 'Uploading…' : 'Choose File'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* DPDP Erasure Confirmation Modal */}
      {eraseModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fadeIn">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 border border-gray-200">
            <div className="flex items-center gap-3 text-red-600">
              <div className="p-2 bg-red-100 rounded-full">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-gray-900">Confirm PII Erasure</h3>
            </div>

            <p className="text-sm text-gray-600 leading-relaxed">
              Under Section 12 of the DPDP Act 2023, this action will permanently zero out your personal details, revoke your encryption key, and delete your candidate profile record.
            </p>

            <div className="bg-red-50 p-3 rounded-xl border border-red-200 text-xs text-red-800 font-medium">
              ⚠️ Warning: You will be immediately logged out and will lose eligibility for registered exams.
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setEraseModalOpen(false)}
                disabled={erasingPii}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleErasePii}
                disabled={erasingPii}
                className="flex items-center gap-2 px-4 py-2 text-sm font-semibold text-white bg-red-600 hover:bg-red-700 disabled:bg-red-400 rounded-lg transition"
              >
                {erasingPii ? <Loader2 className="w-4 h-4 animate-spin" /> : <Trash2 className="w-4 h-4" />}
                {erasingPii ? 'Erasing…' : 'Yes, Erase My Data'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profile;
