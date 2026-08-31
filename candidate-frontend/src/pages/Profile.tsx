// src/pages/Profile.tsx
// Complete Candidate Profile Management integrated with candidate-service & asset-service.

import React, { useState, useEffect, useRef, useCallback } from 'react';
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
  Lock,
  Camera,
  Eye,
  Download,
  X,
  Info,
  GraduationCap,
  Plus,
  Edit2,
  Calendar,
  Building,
  Award,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { candidateService } from '../services/candidateService';
import { assetService } from '../services/assetService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from '../components/Toast';
import { CandidateAvatar } from '../components/CandidateAvatar';
import { DOC_TYPES, DOC_VALIDATION, type IdentityDocType } from './Register';
import type {
  CandidateEducation,
  CandidateEducationRequest,
  CandidateProfileResponse,
  CreateCandidateProfileRequest,
  UpdateCandidateProfileRequest,
} from '../types/api';

// ─── Validation Schemas ─────────────────────────────────────────────────────

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

const educationSchema = z.object({
  qualification: z.string().min(1, 'Please select qualification'),
  courseName: z.string().optional(),
  boardOrUniversity: z.string().min(2, 'Board or University is required'),
  institutionName: z.string().optional(),
  passingYear: z
    .number({ message: 'Passing year is required' })
    .min(1950, 'Year must be 1950 or later')
    .max(new Date().getFullYear() + 1, 'Passing year cannot be in far future'),
  percentageOrCgpa: z.string().optional(),
  gradeOrDivision: z.string().optional(),
  specialization: z.string().optional(),
  rollNumber: z.string().optional(),
});

type PersonalForm = z.infer<typeof personalSchema>;
type ContactForm = z.infer<typeof contactSchema>;
type EducationForm = z.infer<typeof educationSchema>;

// ─── Tabs Definition ────────────────────────────────────────────────────────

type Tab = 'personal' | 'contact' | 'education' | 'documents';

const TABS: { id: Tab; name: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: 'personal', name: 'Personal Details', icon: User },
  { id: 'contact', name: 'Contact & Address', icon: MapPin },
  { id: 'education', name: 'Educational Details', icon: GraduationCap },
  { id: 'documents', name: 'Document Uploads', icon: Upload },
];

export const QUALIFICATION_OPTIONS: { value: string; label: string }[] = [
  { value: '10TH', label: '10th / Matriculation / Secondary' },
  { value: '12TH', label: '12th / Intermediate / Higher Secondary' },
  { value: 'DIPLOMA', label: 'Diploma (Polytechnic / ITI)' },
  { value: 'GRADUATE', label: "Graduation / Bachelor's Degree (B.A., B.Sc., B.Tech, etc.)" },
  { value: 'POST_GRADUATE', label: "Post Graduation / Master's Degree (M.A., M.Sc., M.Tech, etc.)" },
  { value: 'PHD', label: 'Doctorate (Ph.D. / M.Phil)' },
  { value: 'OTHER', label: 'Other Professional / Technical Certification' },
];

export interface UploadedDoc {
  id: string;
  filename: string;
  fileSize?: number;
  contentType?: string;
  uploadedAt: string;
  localPreviewUrl?: string;
}

const Profile: React.FC = () => {
  const { profile, refreshProfile } = useAuth();
  const { toast } = useToast();
  const userId = tokenManager.getUserId();

  const [activeTab, setActiveTab] = useState<Tab>('personal');
  const [loading, setLoading] = useState(!profile);
  const [isNewProfile, setIsNewProfile] = useState(false);
  const [currentProfile, setCurrentProfile] = useState<CandidateProfileResponse | null>(profile);
  const [previewModalUrl, setPreviewModalUrl] = useState<{ url: string; title: string } | null>(null);
  const [photoUploading, setPhotoUploading] = useState(false);

  // Education state
  const [educationList, setEducationList] = useState<CandidateEducation[]>([]);
  const [educationLoading, setEducationLoading] = useState(false);
  const [showEducationModal, setShowEducationModal] = useState(false);
  const [editingEducation, setEditingEducation] = useState<CandidateEducation | null>(null);
  const [educationCertUploading, setEducationCertUploading] = useState(false);
  const [selectedCertAssetId, setSelectedCertAssetId] = useState<string | null>(null);
  const [selectedCertFilename, setSelectedCertFilename] = useState<string>('');
  const eduCertInputRef = useRef<HTMLInputElement>(null);

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

  const mainPhotoRef = useRef<HTMLInputElement>(null);
  const photoRef = useRef<HTMLInputElement>(null);
  const sigRef = useRef<HTMLInputElement>(null);
  const idRef = useRef<HTMLInputElement>(null);
  const fetchedUserIdRef = useRef<string | null>(null);

  // Load profile and educational details
  const fetchEducation = useCallback(async (uid: string) => {
    setEducationLoading(true);
    try {
      const list = await candidateService.getEducation(uid);
      setEducationList(list);
    } catch (err) {
      console.warn('Could not load education records:', err);
    } finally {
      setEducationLoading(false);
    }
  }, []);

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
    candidateService
      .getProfile(uid)
      .then((p) => {
        setCurrentProfile(p);
        setIsNewProfile(false);
      })
      .catch((err: unknown) => {
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 404) {
          setIsNewProfile(true);
        } else {
          toast.error('Failed to load profile', 'Please check network connection.');
        }
      })
      .finally(() => setLoading(false));

    void fetchEducation(uid);
  }, [userId, toast, fetchEducation]);

  // Identity document stored defaults
  const storedDocType = (localStorage.getItem('nag_candidate_doc_type') as IdentityDocType) || 'AADHAAR';
  const storedDocNum = localStorage.getItem('nag_candidate_doc_num') || '';

  // ── Personal Form ─────────────────────────────────────────────────────────
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
  }, [currentProfile, resetPersonal, storedDocNum, storedDocType, storedName, tokenName]);

  const onSavePersonal = async (data: PersonalForm) => {
    const uid = userId || tokenManager.getUserId();
    if (!uid) {
      toast.error('Session Error', 'Please log in again.');
      return;
    }

    try {
      let updated: CandidateProfileResponse;
      if (isNewProfile) {
        const createReq: CreateCandidateProfileRequest = {
          userId: uid,
          fullName: data.fullName,
          dateOfBirth: data.dateOfBirth,
          gender: data.gender,
          nationality: data.nationality,
          category: data.category,
          mobile: currentProfile?.mobile || localStorage.getItem('nag_pending_mobile') || '9876543210',
          email: displayEmail || 'candidate@exam.gov.in',
          address: currentProfile?.address || 'Correspondence Address Pending',
          reservationCategory: data.reservationCategory || undefined,
          identityDocNumber: data.identityDocNumber,
        };
        updated = await candidateService.createProfile(createReq);
        setIsNewProfile(false);
      } else {
        const updateReq: UpdateCandidateProfileRequest = {
          fullName: data.fullName,
          dateOfBirth: data.dateOfBirth,
          gender: data.gender,
          nationality: data.nationality,
          category: data.category,
          reservationCategory: data.reservationCategory || undefined,
          identityDocNumber: data.identityDocNumber,
        };
        updated = await candidateService.updateProfile(uid, updateReq);
      }

      localStorage.setItem('nag_candidate_name', data.fullName);
      localStorage.setItem('nag_candidate_doc_type', data.identityDocType);
      localStorage.setItem('nag_candidate_doc_num', data.identityDocNumber);

      setCurrentProfile(updated);
      await refreshProfile();
      toast.success('Personal details saved!', 'Candidate record updated successfully.');
    } catch (err: unknown) {
      console.error('Failed to save personal profile:', err);
      toast.error('Save failed', 'Please verify your information and try again.');
    }
  };

  // ── Contact & Address Form ────────────────────────────────────────────────
  const {
    register: contactRegister,
    handleSubmit: handleContactSubmit,
    formState: { errors: contactErrors, isSubmitting: contactSaving },
    reset: resetContact,
  } = useForm<ContactForm>({
    resolver: zodResolver(contactSchema),
    mode: 'onTouched',
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
    if (!uid) {
      toast.error('Session Error', 'Please log in again.');
      return;
    }

    try {
      const updated = await candidateService.updateProfile(uid, {
        mobile: data.mobile,
        email: data.email,
        address: data.address,
      });
      setCurrentProfile(updated);
      await refreshProfile();
      toast.success('Contact & address saved!', 'Communication details updated.');
    } catch (err: unknown) {
      console.error('Failed to save contact details:', err);
      toast.error('Save failed', 'Unable to update contact information.');
    }
  };

  // ── Education Form & Handlers ─────────────────────────────────────────────
  const {
    register: educationRegister,
    handleSubmit: handleEducationSubmit,
    reset: resetEducationForm,
    formState: { errors: educationErrors, isSubmitting: educationSaving },
  } = useForm<EducationForm>({
    resolver: zodResolver(educationSchema),
    mode: 'onTouched',
    defaultValues: {
      qualification: '10TH',
      courseName: '',
      boardOrUniversity: '',
      institutionName: '',
      passingYear: 2020,
      percentageOrCgpa: '',
      gradeOrDivision: '',
      specialization: '',
      rollNumber: '',
    },
  });

  const openAddEducationModal = () => {
    setEditingEducation(null);
    setSelectedCertAssetId(null);
    setSelectedCertFilename('');
    resetEducationForm({
      qualification: '10TH',
      courseName: '',
      boardOrUniversity: '',
      institutionName: '',
      passingYear: 2020,
      percentageOrCgpa: '',
      gradeOrDivision: '',
      specialization: '',
      rollNumber: '',
    });
    setShowEducationModal(true);
  };

  const openEditEducationModal = (item: CandidateEducation) => {
    setEditingEducation(item);
    setSelectedCertAssetId(item.certificateAssetId || null);
    setSelectedCertFilename(item.certificateAssetId ? 'Uploaded Certificate' : '');
    resetEducationForm({
      qualification: item.qualification,
      courseName: item.courseName || '',
      boardOrUniversity: item.boardOrUniversity,
      institutionName: item.institutionName || '',
      passingYear: Number(item.passingYear),
      percentageOrCgpa: item.percentageOrCgpa ? String(item.percentageOrCgpa) : '',
      gradeOrDivision: item.gradeOrDivision || '',
      specialization: item.specialization || '',
      rollNumber: item.rollNumber || '',
    });
    setShowEducationModal(true);
  };

  const handleEducationCertUpload = async (file: File) => {
    if (file.size > 5 * 1024 * 1024) {
      toast.error('File too large', 'Certificate must be less than 5MB.');
      return;
    }
    setEducationCertUploading(true);
    try {
      const asset = await candidateService.uploadDocument(file);
      setSelectedCertAssetId(String(asset.id));
      setSelectedCertFilename(asset.originalFilename || file.name);
      toast.success('Certificate uploaded!', `${file.name} attached.`);
    } catch (err) {
      console.error('Certificate upload failed:', err);
      toast.error('Upload failed', 'Unable to upload education certificate.');
    } finally {
      setEducationCertUploading(false);
    }
  };

  const onSaveEducation = async (data: EducationForm) => {
    const uid = userId || tokenManager.getUserId();
    if (!uid) {
      toast.error('Session Error', 'Please log in again.');
      return;
    }

    try {
      const payload: CandidateEducationRequest = {
        qualification: data.qualification,
        courseName: data.courseName?.trim() || undefined,
        boardOrUniversity: data.boardOrUniversity.trim(),
        institutionName: data.institutionName?.trim() || undefined,
        passingYear: Number(data.passingYear),
        percentageOrCgpa: data.percentageOrCgpa?.trim() || undefined,
        gradeOrDivision: data.gradeOrDivision?.trim() || undefined,
        specialization: data.specialization?.trim() || undefined,
        rollNumber: data.rollNumber?.trim() || undefined,
        certificateAssetId: selectedCertAssetId || undefined,
      };

      if (editingEducation && editingEducation.id) {
        await candidateService.updateEducation(uid, editingEducation.id, payload);
        toast.success('Education Updated', 'Academic qualification details updated.');
      } else {
        await candidateService.addEducation(uid, payload);
        toast.success('Education Added', 'Academic qualification successfully recorded.');
      }

      setShowEducationModal(false);
      setEditingEducation(null);
      void fetchEducation(uid);
    } catch (err) {
      console.error('Failed to save education:', err);
      toast.error('Save Failed', 'Could not save educational details.');
    }
  };

  const handleDeleteEducation = async (educationId?: string) => {
    if (!educationId) return;
    const uid = userId || tokenManager.getUserId();
    if (!uid) return;

    if (!window.confirm('Are you sure you want to delete this educational record?')) {
      return;
    }

    try {
      await candidateService.deleteEducation(uid, educationId);
      toast.info('Education Deleted', 'The academic record was removed.');
      void fetchEducation(uid);
    } catch (err) {
      console.error('Failed to delete education record:', err);
      toast.error('Delete Failed', 'Unable to delete academic record.');
    }
  };

  // ── Document & Profile Picture Upload via Asset Service ────────────────────
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
      const localUrl = URL.createObjectURL(file);

      const docInfo: UploadedDoc = {
        id: String(asset.id),
        filename: asset.originalFilename || file.name,
        fileSize: asset.fileSize || file.size,
        contentType: asset.contentType || file.type,
        localPreviewUrl: localUrl,
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

      // Persist asset ID on candidate profile
      const uid = userId || tokenManager.getUserId();
      if (uid && currentProfile) {
        const patch: UpdateCandidateProfileRequest = {};
        if (type === 'PHOTO') patch.photoAssetId = String(asset.id);
        if (type === 'SIGNATURE') patch.signatureAssetId = String(asset.id);
        if (type === 'ID_PROOF') patch.idProofAssetId = String(asset.id);

        const updated = await candidateService.updateProfile(uid, patch);
        setCurrentProfile(updated);
        await refreshProfile();
      }

      toast.success(`${type.replace('_', ' ')} Uploaded!`, `${file.name} saved to Asset Service`);
    } catch (err: unknown) {
      console.error('Document upload error:', err);
      toast.error('Upload failed', 'Ensure asset-service is running and file format is valid.');
    } finally {
      setUploading((prev) => ({ ...prev, [type]: false }));
    }
  };

  const handleProfilePhotoChange = useCallback(
    async (file: File) => {
      const validation = assetService.validateProfilePhoto(file);
      if (!validation.valid) {
        toast.error('Invalid Photo', validation.error || 'Please select a valid image file.');
        return;
      }
      setPhotoUploading(true);
      try {
        await handleFileUpload(file, 'PHOTO');
      } finally {
        setPhotoUploading(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [userId, currentProfile, refreshProfile, toast],
  );

  const handleRemoveDoc = async (type: 'PHOTO' | 'SIGNATURE' | 'ID_PROOF') => {
    setUploadedDocs((prev) => {
      const next = { ...prev };
      delete next[type];
      localStorage.setItem('nag_uploaded_docs', JSON.stringify(next));
      return next;
    });

    const uid = userId || tokenManager.getUserId();
    if (uid && currentProfile) {
      try {
        const patch: UpdateCandidateProfileRequest = {};
        if (type === 'PHOTO') patch.photoAssetId = undefined;
        if (type === 'SIGNATURE') patch.signatureAssetId = undefined;
        if (type === 'ID_PROOF') patch.idProofAssetId = undefined;

        const updated = await candidateService.updateProfile(uid, patch);
        setCurrentProfile(updated);
        await refreshProfile();
      } catch {
        // non-critical
      }
    }

    toast.info('Document Removed', `${type.replace('_', ' ')} has been removed.`);
  };

  // ── Calculate Profile Completeness ─────────────────────────────────────────
  const calculateCompleteness = () => {
    if (!currentProfile) return isNewProfile ? 10 : 0;
    let score = 0;
    if (currentProfile.fullName) score += 15;
    if (currentProfile.dateOfBirth) score += 10;
    if (currentProfile.gender) score += 10;
    if (currentProfile.category) score += 10;
    if (currentProfile.address) score += 15;
    if (educationList.length > 0) score += 20;
    if (uploadedDocs['PHOTO'] || currentProfile.photoAssetId) score += 10;
    if (uploadedDocs['SIGNATURE'] || currentProfile.signatureAssetId) score += 5;
    if (uploadedDocs['ID_PROOF'] || currentProfile.idProofAssetId) score += 5;
    return Math.min(100, score);
  };

  const completeness = calculateCompleteness();

  // ── Styling constants ──────────────────────────────────────────────────────
  const inputCls =
    'w-full border border-gray-300 rounded-lg px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white transition';
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

  const activePhotoAssetId = currentProfile?.photoAssetId || uploadedDocs['PHOTO']?.id;
  const activePhotoUrl = uploadedDocs['PHOTO']?.localPreviewUrl;

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* ── Header Banner with Avatar & Profile Picture Controls ─────────── */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5">
          {/* Avatar with Camera Trigger Overlay */}
          <div className="relative group shrink-0">
            <CandidateAvatar
              photoAssetId={activePhotoAssetId}
              photoUrl={activePhotoUrl}
              name={candidateName}
              size="xl"
              shape="circle"
              bordered={true}
              showVerifiedBadge={currentProfile?.digiLockerVerified === 'VERIFIED'}
              className="shadow-md ring-4 ring-indigo-50"
            />
            <input
              ref={mainPhotoRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) void handleProfilePhotoChange(file);
                e.target.value = '';
              }}
            />
            <button
              type="button"
              onClick={() => mainPhotoRef.current?.click()}
              disabled={photoUploading}
              className="absolute inset-0 bg-black/50 rounded-full opacity-0 group-hover:opacity-100 flex flex-col items-center justify-center text-white transition-opacity duration-200 cursor-pointer"
              title="Change profile picture"
            >
              {photoUploading ? (
                <Loader2 className="w-6 h-6 animate-spin" />
              ) : (
                <>
                  <Camera className="w-5 h-5 mb-0.5 text-indigo-200" />
                  <span className="text-[10px] font-semibold">Change</span>
                </>
              )}
            </button>
          </div>

          {/* Identity details */}
          <div className="text-center sm:text-left space-y-2">
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
              <h1 className="text-2xl font-bold text-gray-900">{candidateName}</h1>
              {currentProfile?.digiLockerVerified === 'VERIFIED' && (
                <span className="inline-flex items-center gap-1 bg-green-50 border border-green-200 text-green-700 text-xs px-2.5 py-1 rounded-full font-medium">
                  <CheckCircle className="w-3.5 h-3.5" /> DigiLocker Verified
                </span>
              )}
            </div>

            {displayEmail && (
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 text-xs text-gray-500">
                <span className="inline-flex items-center gap-1.5 font-semibold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2.5 py-1 rounded-lg">
                  <Mail className="w-3.5 h-3.5 text-indigo-600" /> {displayEmail}
                </span>
              </div>
            )}

            {/* Quick Actions */}
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
              <button
                type="button"
                onClick={() => mainPhotoRef.current?.click()}
                disabled={photoUploading}
                className="inline-flex items-center gap-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold px-3 py-1.5 rounded-lg transition cursor-pointer"
              >
                {photoUploading ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Camera className="w-3.5 h-3.5" />
                )}
                {activePhotoAssetId ? 'Update Photo' : 'Upload Photo'}
              </button>

              {activePhotoAssetId && (
                <button
                  type="button"
                  onClick={() => void handleRemoveDoc('PHOTO')}
                  className="inline-flex items-center gap-1 text-gray-500 hover:text-red-600 text-xs font-medium px-2 py-1.5 rounded-lg hover:bg-red-50 transition cursor-pointer"
                >
                  <Trash2 className="w-3.5 h-3.5" /> Remove
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Completeness Card */}
        <div className="flex items-center justify-center sm:justify-end gap-3 self-center shrink-0">
          <div className="text-right">
            <p className="text-xs text-gray-500">Profile Completeness</p>
            <p className="text-sm font-bold text-gray-800">{completeness}%</p>
          </div>
          <div className="w-28 bg-gray-100 rounded-full h-3 overflow-hidden border border-gray-200">
            <div
              className={`h-full transition-all duration-500 ${
                completeness >= 80
                  ? 'bg-green-500'
                  : completeness >= 50
                  ? 'bg-amber-500'
                  : 'bg-indigo-500'
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
              className={`flex items-center gap-2 px-4 py-3.5 text-sm font-semibold border-b-2 whitespace-nowrap transition cursor-pointer ${
                activeTab === tab.id
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-800 hover:border-gray-300'
              }`}
            >
              <tab.icon className="w-4 h-4" />
              {tab.name}
              {tab.id === 'education' && educationList.length > 0 && (
                <span className="ml-1 px-1.5 py-0.5 bg-indigo-100 text-indigo-700 text-xs rounded-full font-bold">
                  {educationList.length}
                </span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Contents */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
        {/* ── TAB 1: Personal Details ─────────────────────────────────────── */}
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
              {/* Full Name */}
              <div className="sm:col-span-2">
                <label className={labelCls}>Full Name (as on Identity Document) *</label>
                <input
                  {...personalRegister('fullName')}
                  placeholder="e.g. Ramesh Kumar Sharma"
                  className={inputCls}
                />
                {personalErrors.fullName && <p className={errCls}>{personalErrors.fullName.message}</p>}
              </div>

              {/* Date of Birth */}
              <div>
                <label className={labelCls}>Date of Birth *</label>
                <input
                  {...personalRegister('dateOfBirth')}
                  type="date"
                  max={new Date().toISOString().split('T')[0]}
                  className={inputCls}
                />
                {personalErrors.dateOfBirth && (
                  <p className={errCls}>{personalErrors.dateOfBirth.message}</p>
                )}
              </div>

              {/* Gender */}
              <div>
                <label className={labelCls}>Gender *</label>
                <select {...personalRegister('gender')} className={inputCls}>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                  <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
                {personalErrors.gender && <p className={errCls}>{personalErrors.gender.message}</p>}
              </div>

              {/* Nationality */}
              <div>
                <label className={labelCls}>Nationality *</label>
                <input
                  {...personalRegister('nationality')}
                  placeholder="INDIAN"
                  className={inputCls}
                />
                {personalErrors.nationality && (
                  <p className={errCls}>{personalErrors.nationality.message}</p>
                )}
              </div>

              {/* Social Category */}
              <div>
                <label className={labelCls}>Social Category *</label>
                <select {...personalRegister('category')} className={inputCls}>
                  <option value="GENERAL">General / Unreserved (UR)</option>
                  <option value="OBC">Other Backward Class (OBC)</option>
                  <option value="SC">Scheduled Caste (SC)</option>
                  <option value="ST">Scheduled Tribe (ST)</option>
                  <option value="EWS">Economically Weaker Section (EWS)</option>
                </select>
                {personalErrors.category && (
                  <p className={errCls}>{personalErrors.category.message}</p>
                )}
              </div>

              {/* Reservation Category */}
              <div className="sm:col-span-2">
                <label className={labelCls}>Special Reservation / Sub-Category (Optional)</label>
                <input
                  {...personalRegister('reservationCategory')}
                  placeholder="e.g. Person with Benchmark Disability (PwBD), Ex-Serviceman"
                  className={inputCls}
                />
              </div>

              {/* Identity Document Type */}
              <div>
                <label className={labelCls}>Identity Document Type *</label>
                <select {...personalRegister('identityDocType')} className={inputCls}>
                  {DOC_TYPES.map((dt) => (
                    <option key={dt.value} value={dt.value}>
                      {dt.label}
                    </option>
                  ))}
                </select>
                {personalErrors.identityDocType && (
                  <p className={errCls}>{personalErrors.identityDocType.message}</p>
                )}
              </div>

              {/* Identity Document Number */}
              <div>
                <label className={labelCls}>Identity Document Number *</label>
                <input
                  {...personalRegister('identityDocNumber')}
                  placeholder={activePersonalDocConfig?.placeholder || 'Document Number'}
                  className={`${inputCls} uppercase font-mono`}
                />
                {personalErrors.identityDocNumber ? (
                  <p className={errCls}>{personalErrors.identityDocNumber.message}</p>
                ) : (
                  <p className="text-xs text-gray-400 mt-1">
                    {activePersonalDocConfig?.hint || 'Official ID number'}
                  </p>
                )}
              </div>
            </div>

            <div className="flex justify-end pt-4 border-t border-gray-100">
              <button
                type="submit"
                disabled={personalSaving}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-6 py-2.5 rounded-lg text-sm font-semibold shadow-sm transition cursor-pointer"
              >
                {personalSaving ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
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
                <h2 className="text-lg font-bold text-gray-900">Communication & Address</h2>
                <p className="text-xs text-gray-500 mt-0.5">Used for dispatch of admit cards & alerts</p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <div>
                <label className={labelCls}>Registered Mobile Number *</label>
                <input
                  {...contactRegister('mobile')}
                  placeholder="e.g. 9876543210"
                  className={inputCls}
                />
                {contactErrors.mobile && <p className={errCls}>{contactErrors.mobile.message}</p>}
              </div>

              <div>
                <label className={labelCls}>Registered Email Address *</label>
                <input
                  {...contactRegister('email')}
                  type="email"
                  placeholder="candidate@example.com"
                  className={inputCls}
                />
                {contactErrors.email && <p className={errCls}>{contactErrors.email.message}</p>}
              </div>

              <div className="sm:col-span-2">
                <label className={labelCls}>Full Correspondence Address *</label>
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
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-6 py-2.5 rounded-lg text-sm font-semibold shadow-sm transition cursor-pointer"
              >
                {contactSaving ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                {contactSaving ? 'Saving…' : 'Save Address Details'}
              </button>
            </div>
          </form>
        )}

        {/* ── TAB 3: Educational Details ──────────────────────────────────── */}
        {activeTab === 'education' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-gray-100 gap-3">
              <div>
                <h2 className="text-lg font-bold text-gray-900">Educational Qualifications</h2>
                <p className="text-xs text-gray-500 mt-0.5">
                  Manage your academic records, degrees, boards, marks, and supporting certificates
                </p>
              </div>
              <button
                type="button"
                onClick={openAddEducationModal}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold px-4 py-2.5 rounded-lg shadow-sm transition cursor-pointer w-fit shrink-0"
              >
                <Plus className="w-4 h-4" /> Add Qualification
              </button>
            </div>

            {educationLoading ? (
              <div className="flex flex-col items-center justify-center py-12 text-gray-500">
                <Loader2 className="w-8 h-8 animate-spin text-indigo-600 mb-2" />
                <p className="text-xs">Loading qualifications…</p>
              </div>
            ) : educationList.length === 0 ? (
              <div className="text-center py-12 px-4 border-2 border-dashed border-gray-200 rounded-2xl bg-gray-50/50">
                <GraduationCap className="w-12 h-12 text-gray-300 mx-auto mb-3 stroke-1" />
                <h3 className="text-sm font-semibold text-gray-800">No Educational Qualifications Added</h3>
                <p className="text-xs text-gray-500 max-w-md mx-auto mt-1 mb-4">
                  Add your 10th, 12th, graduation, and other academic records to complete your profile and meet exam eligibility requirements.
                </p>
                <button
                  type="button"
                  onClick={openAddEducationModal}
                  className="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold px-4 py-2 rounded-lg transition cursor-pointer shadow-sm"
                >
                  <Plus className="w-3.5 h-3.5" /> Add Qualification
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 gap-4">
                {educationList.map((edu) => {
                  const qualLabel =
                    QUALIFICATION_OPTIONS.find((q) => q.value === edu.qualification)?.label ||
                    edu.qualification;
                  const certUrl = edu.certificateAssetId
                    ? candidateService.getAssetDownloadUrl(edu.certificateAssetId)
                    : null;

                  return (
                    <div
                      key={edu.id || edu.qualification}
                      className="p-5 border border-gray-200 hover:border-indigo-200 rounded-2xl bg-white hover:bg-indigo-50/20 transition shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4"
                    >
                      <div className="space-y-2 flex-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="inline-flex items-center gap-1.5 font-bold text-sm text-indigo-900 bg-indigo-50 border border-indigo-100 px-3 py-1 rounded-lg">
                            <Award className="w-3.5 h-3.5 text-indigo-600" />
                            {qualLabel}
                          </span>
                          {edu.courseName && (
                            <span className="text-sm font-semibold text-gray-800">
                              {edu.courseName}
                            </span>
                          )}
                          {edu.specialization && (
                            <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                              {edu.specialization}
                            </span>
                          )}
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-y-1.5 gap-x-4 text-xs text-gray-600 pt-1">
                          <div className="flex items-center gap-1.5">
                            <Building className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                            <span className="truncate">
                              <strong className="text-gray-700">Board/Univ:</strong> {edu.boardOrUniversity}
                            </span>
                          </div>

                          {edu.institutionName && (
                            <div className="flex items-center gap-1.5">
                              <span className="text-gray-400 font-bold shrink-0">•</span>
                              <span className="truncate">
                                <strong className="text-gray-700">Institute:</strong> {edu.institutionName}
                              </span>
                            </div>
                          )}

                          <div className="flex items-center gap-1.5">
                            <Calendar className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                            <span>
                              <strong className="text-gray-700">Year:</strong> {edu.passingYear}
                            </span>
                          </div>

                          {edu.percentageOrCgpa != null && (
                            <div>
                              <strong className="text-gray-700">Score:</strong>{' '}
                              <span className="font-semibold text-emerald-700">
                                {edu.percentageOrCgpa}%
                              </span>
                            </div>
                          )}

                          {edu.gradeOrDivision && (
                            <div>
                              <strong className="text-gray-700">Division:</strong> {edu.gradeOrDivision}
                            </div>
                          )}

                          {edu.rollNumber && (
                            <div>
                              <strong className="text-gray-700">Roll No:</strong>{' '}
                              <span className="font-mono text-gray-600">{edu.rollNumber}</span>
                            </div>
                          )}
                        </div>

                        {/* Certificate badge */}
                        {certUrl && (
                          <div className="pt-1">
                            <button
                              type="button"
                              onClick={() =>
                                setPreviewModalUrl({
                                  url: certUrl,
                                  title: `${qualLabel} Certificate`,
                                })
                              }
                              className="inline-flex items-center gap-1 text-xs text-indigo-600 hover:text-indigo-800 font-medium hover:underline cursor-pointer"
                            >
                              <FileText className="w-3.5 h-3.5" /> View Certificate
                            </button>
                          </div>
                        )}
                      </div>

                      {/* Action buttons */}
                      <div className="flex items-center gap-2 self-end md:self-center shrink-0">
                        <button
                          type="button"
                          onClick={() => openEditEducationModal(edu)}
                          className="flex items-center gap-1 text-xs font-medium text-gray-700 hover:text-indigo-600 bg-gray-50 hover:bg-indigo-50 border border-gray-200 hover:border-indigo-200 px-3 py-1.5 rounded-lg transition cursor-pointer"
                          title="Edit qualification"
                        >
                          <Edit2 className="w-3.5 h-3.5" /> Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteEducation(edu.id)}
                          className="flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-700 bg-red-50 hover:bg-red-100 border border-red-200 px-2.5 py-1.5 rounded-lg transition cursor-pointer"
                          title="Delete qualification"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* ── TAB 4: Document Uploads (3-Column Preview Grid) ─────────────── */}
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

            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              {[
                {
                  key: 'PHOTO' as const,
                  label: 'Passport Photo',
                  desc: 'Recent color photograph with white or light background (JPG/PNG, max 5MB)',
                  ref: photoRef,
                  accept: 'image/jpeg,image/png,image/webp',
                  assetId: currentProfile?.photoAssetId || uploadedDocs['PHOTO']?.id,
                  isImage: true,
                },
                {
                  key: 'SIGNATURE' as const,
                  label: 'Candidate Signature',
                  desc: 'Clear signature on white paper with dark ink (JPG/PNG, max 2MB)',
                  ref: sigRef,
                  accept: 'image/jpeg,image/png,image/webp',
                  assetId: currentProfile?.signatureAssetId || uploadedDocs['SIGNATURE']?.id,
                  isImage: true,
                },
                {
                  key: 'ID_PROOF' as const,
                  label: 'Identity Proof',
                  desc: 'Scanned copy of Aadhaar / PAN / Passport / Voter ID / DL (JPG/PNG/PDF, max 5MB)',
                  ref: idRef,
                  accept: 'image/jpeg,image/png,application/pdf',
                  assetId: currentProfile?.idProofAssetId || uploadedDocs['ID_PROOF']?.id,
                  isImage: false,
                },
              ].map(({ key, label, desc, ref, accept, assetId, isImage }) => {
                const doc = uploadedDocs[key];
                const previewUrl =
                  doc?.localPreviewUrl ||
                  (assetId ? candidateService.getAssetDownloadUrl(assetId) : null);
                const hasAsset = Boolean(doc || assetId);
                const filename = doc?.filename || (assetId ? 'Uploaded Document' : '');

                return (
                  <div
                    key={key}
                    className="flex flex-col justify-between p-5 border border-gray-200 hover:border-indigo-300 rounded-2xl bg-slate-50/50 hover:bg-slate-50 transition shadow-sm"
                  >
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-sm text-gray-800">{label}</span>
                        {hasAsset ? (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-100 border border-emerald-200 px-2 py-0.5 rounded-full">
                            <CheckCircle className="w-3 h-3" /> Uploaded
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[11px] font-medium text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
                            Required
                          </span>
                        )}
                      </div>

                      <p className="text-xs text-gray-500 min-h-[32px]">{desc}</p>

                      {/* Dedicated Preview Box */}
                      <div className="w-full h-36 rounded-xl bg-white border border-dashed border-gray-300 flex items-center justify-center overflow-hidden relative group">
                        {previewUrl && isImage ? (
                          <>
                            <img
                              src={previewUrl}
                              alt={label}
                              className="w-full h-full object-contain p-2"
                            />
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center gap-2 transition backdrop-blur-xs">
                              <button
                                type="button"
                                onClick={() =>
                                  setPreviewModalUrl({ url: previewUrl, title: label })
                                }
                                className="p-2 bg-white text-gray-800 rounded-lg hover:bg-gray-100 transition shadow cursor-pointer"
                                title="Preview document"
                              >
                                <Eye className="w-4 h-4 text-indigo-600" />
                              </button>
                              <button
                                type="button"
                                onClick={() => void handleRemoveDoc(key)}
                                className="p-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition shadow cursor-pointer"
                                title="Remove document"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            </div>
                          </>
                        ) : previewUrl && !isImage ? (
                          <div className="flex flex-col items-center gap-2 text-indigo-600 p-3 text-center">
                            <FileText className="w-10 h-10 text-indigo-500" />
                            <span className="text-xs font-semibold text-gray-700 truncate max-w-[180px]">
                              {filename}
                            </span>
                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                onClick={() =>
                                  setPreviewModalUrl({ url: previewUrl, title: label })
                                }
                                className="text-xs text-indigo-600 hover:underline flex items-center gap-1 cursor-pointer font-medium"
                              >
                                <Eye className="w-3.5 h-3.5" /> View
                              </button>
                              <button
                                type="button"
                                onClick={() => void handleRemoveDoc(key)}
                                className="text-xs text-red-500 hover:underline flex items-center gap-1 cursor-pointer font-medium"
                              >
                                <Trash2 className="w-3.5 h-3.5" /> Remove
                              </button>
                            </div>
                          </div>
                        ) : (
                          <div className="flex flex-col items-center text-gray-400 gap-1.5">
                            <Upload className="w-8 h-8 stroke-1 text-gray-300" />
                            <span className="text-xs">No file uploaded</span>
                          </div>
                        )}
                      </div>

                      {filename && (
                        <p className="text-[11px] text-gray-500 truncate" title={filename}>
                          File: <span className="font-mono">{filename}</span>
                        </p>
                      )}
                    </div>

                    {/* Upload button */}
                    <div className="mt-4 pt-3 border-t border-gray-200">
                      <input
                        ref={ref}
                        type="file"
                        accept={accept}
                        className="hidden"
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) void handleFileUpload(file, key);
                          e.target.value = '';
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => ref.current?.click()}
                        disabled={uploading[key]}
                        className="w-full flex items-center justify-center gap-2 bg-white hover:bg-indigo-50 border border-gray-300 hover:border-indigo-300 text-indigo-700 text-xs font-semibold py-2 rounded-xl transition cursor-pointer"
                      >
                        {uploading[key] ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        ) : (
                          <Upload className="w-3.5 h-3.5" />
                        )}
                        {uploading[key] ? 'Uploading…' : hasAsset ? 'Replace File' : 'Upload File'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Asset Service DPDP and Security note */}
            <div className="flex items-start gap-3 p-4 bg-indigo-50/70 border border-indigo-100 rounded-2xl text-xs text-indigo-900">
              <Info className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold block mb-0.5">Asset Service Security & DPDP Compliance</span>
                <span>
                  All profile media is validated against magic number byte signatures, virus scanned,
                  and encrypted with AES-256 before persisting to storage. You may exercise your DPDP Right to Erasure
                  at any time from account settings.
                </span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* ── Add / Edit Education Modal ───────────────────────────────────── */}
      {showEducationModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto"
          onClick={() => setShowEducationModal(false)}
        >
          <div
            className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl overflow-hidden my-8"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-5 border-b border-gray-100 bg-gray-50">
              <div className="flex items-center gap-2">
                <GraduationCap className="w-5 h-5 text-indigo-600" />
                <h3 className="font-bold text-gray-900 text-base">
                  {editingEducation ? 'Edit Educational Qualification' : 'Add Educational Qualification'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowEducationModal(false)}
                className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-200 rounded-lg transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleEducationSubmit(onSaveEducation)} noValidate className="p-6 space-y-4 max-h-[75vh] overflow-y-auto">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Qualification */}
                <div className="sm:col-span-2">
                  <label className={labelCls}>Qualification Level *</label>
                  <select {...educationRegister('qualification')} className={inputCls}>
                    {QUALIFICATION_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                  {educationErrors.qualification && (
                    <p className={errCls}>{educationErrors.qualification.message}</p>
                  )}
                </div>

                {/* Course Name */}
                <div>
                  <label className={labelCls}>Degree / Course Name</label>
                  <input
                    {...educationRegister('courseName')}
                    placeholder="e.g. B.Tech Computer Science / CBSE Class 10"
                    className={inputCls}
                  />
                  {educationErrors.courseName && (
                    <p className={errCls}>{educationErrors.courseName.message}</p>
                  )}
                </div>

                {/* Board / University */}
                <div>
                  <label className={labelCls}>Board / University *</label>
                  <input
                    {...educationRegister('boardOrUniversity')}
                    placeholder="e.g. CBSE / Delhi University"
                    className={inputCls}
                  />
                  {educationErrors.boardOrUniversity && (
                    <p className={errCls}>{educationErrors.boardOrUniversity.message}</p>
                  )}
                </div>

                {/* Institution Name */}
                <div>
                  <label className={labelCls}>School / College / Institution Name</label>
                  <input
                    {...educationRegister('institutionName')}
                    placeholder="e.g. ABC Public School / XYZ Institute"
                    className={inputCls}
                  />
                </div>

                {/* Passing Year */}
                <div>
                  <label className={labelCls}>Year of Passing *</label>
                  <input
                    {...educationRegister('passingYear', { valueAsNumber: true })}
                    type="number"
                    min={1950}
                    max={new Date().getFullYear() + 1}
                    placeholder="e.g. 2022"
                    className={inputCls}
                  />
                  {educationErrors.passingYear && (
                    <p className={errCls}>{educationErrors.passingYear.message}</p>
                  )}
                </div>

                {/* Percentage / CGPA */}
                <div>
                  <label className={labelCls}>Percentage / CGPA</label>
                  <input
                    {...educationRegister('percentageOrCgpa')}
                    placeholder="e.g. 85.50 or 8.5"
                    className={inputCls}
                  />
                </div>

                {/* Grade / Division */}
                <div>
                  <label className={labelCls}>Grade / Division</label>
                  <input
                    {...educationRegister('gradeOrDivision')}
                    placeholder="e.g. First Class / Distinction"
                    className={inputCls}
                  />
                </div>

                {/* Specialization / Stream */}
                <div>
                  <label className={labelCls}>Stream / Specialization</label>
                  <input
                    {...educationRegister('specialization')}
                    placeholder="e.g. Science (PCM) / CSE"
                    className={inputCls}
                  />
                </div>

                {/* Roll Number */}
                <div>
                  <label className={labelCls}>Roll Number / Reg Number</label>
                  <input
                    {...educationRegister('rollNumber')}
                    placeholder="e.g. 12345678"
                    className={inputCls}
                  />
                </div>

                {/* Certificate Upload */}
                <div className="sm:col-span-2 pt-2 border-t border-gray-100">
                  <label className={labelCls}>Educational Certificate / Marksheet (Optional)</label>
                  <input
                    ref={eduCertInputRef}
                    type="file"
                    accept="image/jpeg,image/png,application/pdf"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) void handleEducationCertUpload(file);
                      e.target.value = '';
                    }}
                  />
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() => eduCertInputRef.current?.click()}
                      disabled={educationCertUploading}
                      className="inline-flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-semibold px-3 py-2 rounded-lg transition cursor-pointer"
                    >
                      {educationCertUploading ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Upload className="w-3.5 h-3.5" />
                      )}
                      {selectedCertAssetId ? 'Change Certificate' : 'Upload Certificate (PDF/IMG)'}
                    </button>
                    {selectedCertAssetId && (
                      <span className="inline-flex items-center gap-1 text-xs text-emerald-700 font-medium">
                        <CheckCircle className="w-3.5 h-3.5" /> {selectedCertFilename || 'Certificate Attached'}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowEducationModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-lg transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={educationSaving}
                  className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white px-5 py-2 rounded-lg text-xs font-semibold shadow-sm transition cursor-pointer"
                >
                  {educationSaving ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Save className="w-3.5 h-3.5" />
                  )}
                  {educationSaving ? 'Saving…' : editingEducation ? 'Update Qualification' : 'Save Qualification'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Document Lightbox / Preview Modal ────────────────────────────── */}
      {previewModalUrl && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={previewModalUrl.title}
          className="fixed inset-0 z-50 bg-black/80 backdrop-blur-xs flex items-center justify-center p-4"
          onClick={() => setPreviewModalUrl(null)}
        >
          <div
            className="bg-white rounded-2xl max-w-2xl w-full overflow-hidden shadow-2xl relative"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-4 border-b border-gray-100 bg-gray-50">
              <h3 className="font-semibold text-gray-800 text-sm">{previewModalUrl.title}</h3>
              <div className="flex items-center gap-2">
                <a
                  href={previewModalUrl.url}
                  download="document-preview"
                  className="p-1.5 text-gray-600 hover:text-indigo-600 hover:bg-gray-200 rounded-lg transition"
                  title="Download File"
                >
                  <Download className="w-4 h-4" />
                </a>
                <button
                  type="button"
                  onClick={() => setPreviewModalUrl(null)}
                  className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-200 rounded-lg transition cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            <div className="p-4 max-h-[75vh] flex items-center justify-center overflow-auto bg-slate-900">
              {previewModalUrl.url.startsWith('data:application/pdf') ||
              previewModalUrl.url.includes('.pdf') ? (
                <iframe
                  src={previewModalUrl.url}
                  className="w-full h-96 border-none rounded-lg"
                  title="Document Preview"
                />
              ) : (
                <img
                  src={previewModalUrl.url}
                  alt={previewModalUrl.title}
                  className="max-h-[65vh] w-auto object-contain rounded-lg shadow-md"
                />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Profile;
