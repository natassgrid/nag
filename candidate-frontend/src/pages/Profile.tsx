// src/pages/Profile.tsx
// Complete Candidate Profile Management integrated with candidate-service & asset-service.

import React, { useState, useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User,
  MapPin,
  Upload,
  CheckCircle,
  Save,
  Loader2,
  Trash2,
  Mail,
  FileText,
  ExternalLink,
  Lock,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { candidateService } from '../services/candidateService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from '../components/Toast';
import { DOC_TYPES, DOC_VALIDATION, type IdentityDocType } from './Register';
import type {
  CandidateProfileResponse,
  CreateCandidateProfileRequest,
  UpdateCandidateProfileRequest,
} from '../types/api';

// ─── Validation Schemas ───────────────────────────────────────────────────────

const personalSchema = z
  .object({
    fullName: z.string().min(2, 'Full name must be at least 2 characters'),
    dateOfBirth: z.string().min(1, 'Date of birth is required'),
    gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'] as const),
    nationality: z.string().min(2, 'Nationality is required'),
    category: z.enum(['GENERAL', 'OBC', 'SC', 'ST', 'EWS'] as const),
    reservationCategory: z.string().optional(),
    identityDocType: z.enum(['AADHAAR', 'PAN', 'PASSPORT', 'VOTER_ID', 'DL'] as const, {
      message: 'Select an identity document type',
    }),
    identityDocNumber: z.string().min(1, 'Identity document number is required'),
  })
  .superRefine((data, ctx) => {
    if (data.identityDocType && data.identityDocNumber) {
      const rule = DOC_VALIDATION[data.identityDocType];
      if (rule) {
        const cleanVal = rule.sanitize(data.identityDocNumber);
        if (cleanVal && !rule.pattern.test(cleanVal)) {
          ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: rule.errorMessage,
            path: ['identityDocNumber'],
          });
        }
      }
    }
  });

const contactSchema = z.object({
  mobile: z.string().min(10, 'Valid mobile number is required'),
  email: z.string().email('Valid email address is required'),
  address: z.string().min(5, 'Please provide full correspondence address'),
});

type PersonalForm = z.infer<typeof personalSchema>;
type ContactForm = z.infer<typeof contactSchema>;

// ─── Tabs Definition ─────────────────────────────────────────────────────────

type Tab = 'personal' | 'contact' | 'documents';

const TABS: { id: Tab; name: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'personal', name: 'Personal Details', icon: User },
  { id: 'contact', name: 'Contact & Address', icon: MapPin },
  { id: 'documents', name: 'Document Uploads', icon: Upload },
];

export interface UploadedDoc {
  id: string;
  filename: string;
  fileSize?: number;
  contentType?: string;
  uploadedAt: string;
}

const Profile: React.FC = () => {
  const { profile, refreshProfile } = useAuth();
  const { toast } = useToast();
  const userId = tokenManager.getUserId();

  const [activeTab, setActiveTab] = useState<Tab>('personal');
  const [loading, setLoading] = useState(!profile);
  const [isNewProfile, setIsNewProfile] = useState(false);
  const [currentProfile, setCurrentProfile] = useState<CandidateProfileResponse | null>(profile);

  // Extract candidate name and email from profile, local storage, or JWT claims
  const jwtPayload = tokenManager.decodePayload();
  const storedName = localStorage.getItem('nag_candidate_name') || '';
  const tokenName = (jwtPayload?.name as string) || '';
  const candidateName =
    currentProfile?.fullName ||
    storedName ||
    (tokenName && !tokenName.includes('@') ? tokenName : '') ||
    'Candidate';

  const tokenEmail = (jwtPayload?.preferred_username as string)?.includes('@')
    ? (jwtPayload?.preferred_username as string)
    : (jwtPayload?.email as string) || (jwtPayload?.preferred_username as string) || '';
  const displayEmail = currentProfile?.email || tokenEmail;

  // Document uploads integrated with asset-service
  const [uploading, setUploading] = useState<Record<string, boolean>>({});
  const [uploadedDocs, setUploadedDocs] = useState<Record<string, UploadedDoc>>(() => {
    try {
      const saved = localStorage.getItem('nag_uploaded_docs');
      return saved ? JSON.parse(saved) : {};
    } catch {
      return {};
    }
  });

  const photoRef = useRef<HTMLInputElement>(null);
  const sigRef = useRef<HTMLInputElement>(null);
  const idRef = useRef<HTMLInputElement>(null);
  const fetchedUserIdRef = useRef<string | null>(null);

  // Load profile on mount
  useEffect(() => {
    const uid = userId || tokenManager.getUserId();
    const UUID_REGEX = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
    if (!uid || !UUID_REGEX.test(uid)) {
      setLoading(false);
      setIsNewProfile(true);
      return;
    }
    if (fetchedUserIdRef.current === uid) {
      return;
    }
    fetchedUserIdRef.current = uid;
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
  }, [userId]);

  // Identity document stored defaults
  const storedDocType = (localStorage.getItem('nag_candidate_doc_type') as IdentityDocType) || 'AADHAAR';
  const storedDocNum = localStorage.getItem('nag_candidate_doc_num') || '';

  // ── Personal Form ──────────────────────────────────────────────────────────
  const {
    register: personalRegister,
    handleSubmit: handlePersonalSubmit,
    watch: personalWatch,
    formState: { errors: personalErrors, isSubmitting: personalSaving },
    reset: resetPersonal,
  } = useForm<PersonalForm>({
    resolver: zodResolver(personalSchema),
    mode: 'onTouched',
    defaultValues: {
      fullName: currentProfile?.fullName || storedName || (tokenName && !tokenName.includes('@') ? tokenName : '') || '',
      dateOfBirth: currentProfile?.dateOfBirth ?? '',
      gender: (currentProfile?.gender as PersonalForm['gender']) ?? 'MALE',
      nationality: currentProfile?.nationality ?? 'INDIAN',
      category: (currentProfile?.category as PersonalForm['category']) ?? 'GENERAL',
      reservationCategory: currentProfile?.reservationCategory ?? '',
      identityDocType: storedDocType,
      identityDocNumber: storedDocNum,
    },
  });

  const selectedPersonalDocType = personalWatch('identityDocType') || 'AADHAAR';
  const activePersonalDocConfig = DOC_VALIDATION[selectedPersonalDocType];

  useEffect(() => {
    resetPersonal({
      fullName: currentProfile?.fullName || storedName || (tokenName && !tokenName.includes('@') ? tokenName : '') || '',
      dateOfBirth: currentProfile?.dateOfBirth ?? '',
      gender: (currentProfile?.gender as PersonalForm['gender']) ?? 'MALE',
      nationality: currentProfile?.nationality ?? 'INDIAN',
      category: (currentProfile?.category as PersonalForm['category']) ?? 'GENERAL',
      reservationCategory: currentProfile?.reservationCategory ?? '',
      identityDocType: storedDocType,
      identityDocNumber: storedDocNum,
    });
  }, [currentProfile, storedName, tokenName, storedDocType, storedDocNum, resetPersonal]);

  const onSavePersonal = async (data: PersonalForm) => {
    const uid = userId || tokenManager.getUserId();
    if (!uid || uid === 'undefined') {
      toast.error('Authentication Error', 'Please login again to update profile.');
      return;
    }
    try {
      const docRule = data.identityDocType ? DOC_VALIDATION[data.identityDocType] : null;
      const sanitizedDoc = docRule ? docRule.sanitize(data.identityDocNumber) : data.identityDocNumber.trim();
      localStorage.setItem('nag_candidate_name', data.fullName);
      localStorage.setItem('nag_candidate_doc_type', data.identityDocType);
      localStorage.setItem('nag_candidate_doc_num', sanitizedDoc);

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
          identityDocNumber: sanitizedDoc || 'NOT_SPECIFIED',
          mobile: currentProfile?.mobile ?? '9999999999',
          email: currentProfile?.email ?? `${uid}@candidate.nag.gov.in`,
          address: currentProfile?.address ?? '',
        };
        const created = await candidateService.createProfile(createReq);
        setCurrentProfile(created);
        setIsNewProfile(false);
        await refreshProfile();
        toast.success('Profile created successfully!', 'Your personal details have been saved.');
      } else {
        const updateReq: UpdateCandidateProfileRequest = {
          fullName: data.fullName,
          dateOfBirth: data.dateOfBirth,
          gender: data.gender,
          nationality: data.nationality,
          category: data.category,
          reservationCategory: data.reservationCategory || undefined,
          identityDocNumber: sanitizedDoc || undefined,
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
      email: displayEmail ?? '',
      address: currentProfile?.address ?? '',
    },
  });

  useEffect(() => {
    resetContact({
      mobile: currentProfile?.mobile ?? '',
      email: displayEmail ?? '',
      address: currentProfile?.address ?? '',
    });
  }, [currentProfile, displayEmail, resetContact]);

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

  // ── Document Upload with Asset Management Service ───────────────────────────
  const handleFileUpload = async (
    file: File,
    type: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF',
  ) => {
    if (file.size > 5 * 1024 * 1024) {
      toast.error('File too large', 'Maximum allowed file size is 5MB.');
      return;
    }
    setUploading((prev) => ({ ...prev, [type]: true }));
    try {
      const asset = await candidateService.uploadDocument(file);
      const docInfo: UploadedDoc = {
        id: String(asset.id),
        filename: asset.originalFilename || file.name,
        fileSize: asset.fileSize || file.size,
        contentType: asset.contentType || file.type,
        uploadedAt: new Date().toLocaleDateString('en-IN', {
          day: 'numeric',
          month: 'short',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        }),
      };
      setUploadedDocs((prev) => {
        const next = { ...prev, [type]: docInfo };
        localStorage.setItem('nag_uploaded_docs', JSON.stringify(next));
        return next;
      });
      toast.success(`${type.replace('_', ' ')} Uploaded!`, `${file.name} saved to Asset Service`);
    } catch (err: unknown) {
      console.error('Document upload error:', err);
      toast.error('Upload failed', 'Ensure asset-service is running and file format is valid.');
    } finally {
      setUploading((prev) => ({ ...prev, [type]: false }));
    }
  };

  const handleRemoveDoc = (type: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF') => {
    setUploadedDocs((prev) => {
      const next = { ...prev };
      delete next[type];
      localStorage.setItem('nag_uploaded_docs', JSON.stringify(next));
      return next;
    });
    toast.info('Document Removed', `${type.replace('_', ' ')} has been removed.`);
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
    if (uploadedDocs['PHOTO']) score += 10;
    if (uploadedDocs['SIGNATURE']) score += 10;
    if (uploadedDocs['ID_PROOF']) score += 10;
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
              {candidateName}
            </h1>
            {currentProfile?.digiLockerVerified === 'VERIFIED' && (
              <span className="inline-flex items-center gap-1 bg-green-50 border border-green-200 text-green-700 text-xs px-2.5 py-1 rounded-full font-medium">
                <CheckCircle className="w-3.5 h-3.5" /> DigiLocker Verified
              </span>
            )}
          </div>
          {displayEmail && (
            <div className="flex items-center gap-2 text-xs text-gray-500 mt-2">
              <span className="inline-flex items-center gap-1.5 font-semibold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2.5 py-1 rounded-lg">
                <Mail className="w-3.5 h-3.5 text-indigo-600" /> {displayEmail}
              </span>
            </div>
          )}
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

              <div>
                <label className={labelCls}>Identity Document Type *</label>
                <select {...personalRegister('identityDocType')} className={inputCls}>
                  {DOC_TYPES.map((doc) => (
                    <option key={doc.value} value={doc.value}>
                      {doc.label}
                    </option>
                  ))}
                </select>
                {personalErrors.identityDocType && (
                  <p className={errCls}>{personalErrors.identityDocType.message}</p>
                )}
              </div>

              <div>
                <label className={labelCls}>
                  Identity Document Number ({activePersonalDocConfig?.label || 'Document'}) *
                </label>
                <input
                  {...personalRegister('identityDocNumber')}
                  placeholder={activePersonalDocConfig?.placeholder || 'Document Number'}
                  maxLength={activePersonalDocConfig?.maxLength || 30}
                  className={inputCls}
                />
                {personalErrors.identityDocNumber && (
                  <p className={errCls}>{personalErrors.identityDocNumber.message}</p>
                )}
                {activePersonalDocConfig && (
                  <p className="text-xs text-gray-400 mt-1">
                    {activePersonalDocConfig.hint} • Stored as SHA-256 HMAC hash.
                  </p>
                )}
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

        {/* ── TAB 3: Document Uploads (Asset Management Service) ──────────── */}
        {activeTab === 'documents' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-gray-100 gap-2">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Candidate Document Uploads</h2>
                <p className="text-xs text-gray-500 mt-0.5">
                  Secure multimedia asset storage for passport photograph, official signature, and government identity proof
                </p>
              </div>
              <div className="flex items-center gap-2 text-xs font-semibold text-indigo-700 bg-indigo-50 px-3 py-1.5 rounded-lg border border-indigo-100 w-fit">
                <FileText className="w-3.5 h-3.5 text-indigo-600" />
                <span>Asset Management Service Active</span>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-5">
              {[
                {
                  key: 'PHOTO' as const,
                  label: 'Passport Photograph',
                  desc: 'Recent color photograph with white or light background (JPG/PNG, max 5MB)',
                  ref: photoRef,
                  accept: 'image/jpeg,image/png',
                  icon: '📸',
                },
                {
                  key: 'SIGNATURE' as const,
                  label: 'Official Candidate Signature',
                  desc: 'Clear signature on white paper with dark ink (JPG/PNG, max 2MB)',
                  ref: sigRef,
                  accept: 'image/jpeg,image/png',
                  icon: '✍️',
                },
                {
                  key: 'ID_PROOF' as const,
                  label: 'Government Identity Proof',
                  desc: 'Scanned copy of Aadhaar / PAN / Passport / Voter ID / DL (JPG/PNG/PDF, max 5MB)',
                  ref: idRef,
                  accept: 'image/jpeg,image/png,application/pdf',
                  icon: '🪪',
                },
              ].map(({ key, label, desc, ref, accept, icon }) => {
                const doc = uploadedDocs[key];
                return (
                  <div
                    key={key}
                    className="p-5 border border-gray-200 hover:border-indigo-300 rounded-2xl bg-white transition shadow-sm"
                  >
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                      <div className="flex items-start gap-4">
                        <div className="w-12 h-12 rounded-xl bg-gray-50 border border-gray-100 flex items-center justify-center text-2xl shrink-0">
                          {icon}
                        </div>
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-bold text-gray-900">{label}</p>
                            {doc ? (
                              <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                                <CheckCircle className="w-3 h-3" /> Uploaded
                              </span>
                            ) : (
                              <span className="text-[11px] font-semibold text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                                Required
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-gray-500">{desc}</p>
                          {doc && (
                            <div className="flex flex-wrap items-center gap-2 text-xs text-gray-600 pt-1">
                              <span className="font-medium text-gray-800 bg-gray-100 px-2 py-0.5 rounded-md border border-gray-200">
                                {doc.filename}
                              </span>
                              {doc.fileSize && (
                                <span className="text-gray-400">
                                  {(doc.fileSize / 1024).toFixed(1)} KB
                                </span>
                              )}
                              <span className="text-gray-400">• Uploaded on {doc.uploadedAt}</span>
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-2.5 self-end md:self-center shrink-0">
                        {doc && (
                          <>
                            <a
                              href={candidateService.getAssetDownloadUrl(doc.id)}
                              target="_blank"
                              rel="noreferrer"
                              className="flex items-center gap-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold px-3 py-2 rounded-xl transition"
                            >
                              <ExternalLink className="w-3.5 h-3.5" /> View
                            </a>
                            <button
                              type="button"
                              onClick={() => handleRemoveDoc(key)}
                              className="p-2 text-gray-400 hover:text-red-600 rounded-xl hover:bg-red-50 transition"
                              title="Remove document"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        )}
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
                          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white text-xs font-semibold px-4 py-2 rounded-xl transition shadow-sm"
                        >
                          {uploading[key] ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <Upload className="w-3.5 h-3.5" />
                          )}
                          {uploading[key] ? 'Uploading…' : doc ? 'Replace File' : 'Upload File'}
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Profile;
