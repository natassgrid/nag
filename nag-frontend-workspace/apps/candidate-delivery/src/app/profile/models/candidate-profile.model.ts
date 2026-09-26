export interface EducationEntry {
  id: string;
  qualification: string;
  boardOrUniversity: string;
  passingYear: number;
  percentageOrCgpa: string;
}

export type ProfileGender = 'MALE' | 'FEMALE' | 'OTHER';
export type ProfileCategory = 'GENERAL' | 'OBC' | 'SC' | 'ST' | 'EWS';
export type ProfileIdentityDocType = 'AADHAAR' | 'PAN' | 'PASSPORT' | 'VOTER_ID' | 'DRIVING_LICENSE';
export type ProfileKycStatus = 'VERIFIED' | 'PENDING' | 'REJECTED';

export interface CandidateProfile {
  candidateId: string;
  fullName: string;
  dateOfBirth: string;
  gender: ProfileGender;
  nationality: string;
  category: ProfileCategory;
  identityDocType: ProfileIdentityDocType;
  identityDocNumber: string;
  mobile: string;
  email: string;
  address: string;
  state: string;
  pinCode: string;
  kycStatus: ProfileKycStatus;
  education: EducationEntry[];
}

export type ProfileTab = 'personal' | 'contact' | 'education' | 'documents';

export interface ProfileTabOption {
  id: ProfileTab;
  label: string;
  icon: string;
}
