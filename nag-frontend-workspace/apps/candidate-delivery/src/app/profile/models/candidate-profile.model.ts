export interface EducationEntry {
  id: string;
  qualification: string;
  boardOrUniversity: string;
  passingYear: number;
  percentageOrCgpa: string;
  certificateAssetId?: string;
}

export type ProfileGender = 'MALE' | 'FEMALE' | 'OTHER';
export type ProfileCategory = 'GENERAL' | 'OBC' | 'SC' | 'ST' | 'EWS';
export type ProfileIdentityDocType = 'AADHAAR' | 'PAN' | 'PASSPORT' | 'VOTER_ID' | 'DRIVING_LICENSE';
export type ProfileKycStatus = 'VERIFIED' | 'PENDING' | 'REJECTED';
export type DigiLockerStatus = 'VERIFIED' | 'LINKED' | 'NOT_LINKED';

export interface DigiLockerClaim {
  id: string;
  docType: string;
  docName: string;
  issuerName: string;
  docNumber: string;
  issuedDate: string;
  verifiedAt: string;
  status: 'VERIFIED' | 'PENDING';
  hashDigest?: string;
}

export interface CandidateProfile {
  candidateId: string;
  fullName: string;
  dateOfBirth: string;
  gender: ProfileGender;
  nationality: string;
  category: ProfileCategory;
  reservationCategory?: string;
  identityDocType: ProfileIdentityDocType;
  identityDocNumber: string;
  mobile: string;
  email: string;
  address: string;
  state: string;
  pinCode: string;
  preferredRegionalLanguage?: string;
  kycStatus: ProfileKycStatus;
  photoUrl?: string;
  photoAssetId?: string;
  signatureUrl?: string;
  signatureAssetId?: string;
  idProofUrl?: string;
  idProofAssetId?: string;
  digiLockerStatus?: DigiLockerStatus;
  digiLockerUri?: string;
  digiLockerClaims?: DigiLockerClaim[];
  education: EducationEntry[];
}

export type ProfileTab = 'personal' | 'contact' | 'education' | 'documents' | 'digilocker';

export interface ProfileTabOption {
  id: ProfileTab;
  label: string;
  icon: string;
  badge?: string;
}
