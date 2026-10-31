/**
 * Candidate Mobile Profile Models & Indian States/Cities Dataset
 * Ported from nag-frontend-workspace candidate-delivery profile module.
 */

export type ProfileGender = 'MALE' | 'FEMALE' | 'OTHER';
export type ProfileCategory = 'GENERAL' | 'OBC_NCL' | 'SC' | 'ST' | 'EWS';
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

export interface EducationRecord {
  id: string;
  qualification: string;
  courseName?: string;
  boardOrUniversity: string;
  institutionName?: string;
  passingYear: number;
  scoreType: 'PERCENTAGE' | 'CGPA';
  percentageOrCgpa: string;
  gradeOrDivision?: string;
  specialization?: string;
  rollNumber?: string;
  certificateAssetId?: string;
  certificateFileName?: string;
  verifiedViaDigiLocker?: boolean;
}

export interface AddressDetails {
  addressLine: string;
  state: string;
  district: string;
  city: string;
  pinCode: string;
}

export interface IdentityDocumentRecord {
  docType: ProfileIdentityDocType;
  docNumber: string;
  verified: boolean;
  verifiedAt?: string;
  documentUrl?: string;
  aadhaarLastFour?: string;
  digiLockerLinked: boolean;
  photoUploaded: boolean;
  photoUrl?: string;
  signatureUploaded: boolean;
  signatureUrl?: string;
  biometricConsent: boolean;
}

export interface PwdAccommodationDetails {
  isPwd: boolean;
  disabilityCategory?: 'LOCOMOTOR' | 'VISUAL' | 'HEARING' | 'SPEECH' | 'AUTISM' | 'OTHER';
  disabilityPercentage?: number;
  requiresScribe: boolean;
  requiresCompensatoryTime: boolean;
  scribePreference?: 'OWN_SCRIBE' | 'NTA_SCRIBE';
  certificateNumber?: string;
}

export interface CandidateProfileData {
  candidateId: string;
  fullName: string;
  dateOfBirth: string; // YYYY-MM-DD
  gender: ProfileGender;
  nationality: string;
  category: ProfileCategory;
  reservationCategory?: string;
  mobile: string;
  email: string;
  mobileVerified: boolean;
  emailVerified: boolean;
  preferredLanguage: string;
  address: AddressDetails;
  identity: IdentityDocumentRecord;
  pwdDetails: PwdAccommodationDetails;
  education: EducationRecord[];
  digiLockerClaims?: DigiLockerClaim[];
  profileCompletionPercentage: number;
}

export interface StateDistrictMapping {
  state: string;
  districts: string[];
}

export const INDIAN_STATES_AND_CITIES: StateDistrictMapping[] = [
  {
    state: 'Andhra Pradesh',
    districts: [
      'Visakhapatnam', 'Vijayawada', 'Guntur', 'Nellore', 'Kurnool', 'Tirupati',
      'Kakinada', 'Rajahmundry', 'Anantapur', 'Eluru', 'Kadapa', 'Srikakulam'
    ],
  },
  {
    state: 'Arunachal Pradesh',
    districts: ['Itanagar', 'Naharlagun', 'Pasighat', 'Tawang', 'Ziro', 'Tezu'],
  },
  {
    state: 'Assam',
    districts: ['Guwahati', 'Silchar', 'Dibrugarh', 'Jorhat', 'Nagaon', 'Tezpur', 'Tinsukia'],
  },
  {
    state: 'Bihar',
    districts: ['Patna', 'Gaya', 'Bhagalpur', 'Muzaffarpur', 'Darbhanga', 'Purnia', 'Bihar Sharif'],
  },
  {
    state: 'Chhattisgarh',
    districts: ['Raipur', 'Bhilai', 'Bilaspur', 'Korba', 'Rajnandgaon', 'Jagdalpur', 'Ambikapur'],
  },
  {
    state: 'Goa',
    districts: ['Panaji', 'Margao', 'Vasco da Gama', 'Mapusa', 'Ponda'],
  },
  {
    state: 'Gujarat',
    districts: ['Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Bhavnagar', 'Jamnagar', 'Gandhinagar', 'Junagadh'],
  },
  {
    state: 'Haryana',
    districts: ['Gurugram', 'Faridabad', 'Panipat', 'Ambala', 'Yamunanagar', 'Rohtak', 'Hisar', 'Karnal'],
  },
  {
    state: 'Himachal Pradesh',
    districts: ['Shimla', 'Dharamshala', 'Mandi', 'Solan', 'Kullu', 'Hamirpur'],
  },
  {
    state: 'Jharkhand',
    districts: ['Ranchi', 'Jamshedpur', 'Dhanbad', 'Bokaro', 'Deoghar', 'Hazaribagh'],
  },
  {
    state: 'Karnataka',
    districts: ['Bengaluru Urban', 'Mysuru', 'Hubballi-Dharwad', 'Mangaluru', 'Belagavi', 'Kalaburagi', 'Ballari'],
  },
  {
    state: 'Kerala',
    districts: ['Thiruvananthapuram', 'Kochi', 'Kozhikode', 'Kollam', 'Thrissur', 'Kannur', 'Alappuzha'],
  },
  {
    state: 'Madhya Pradesh',
    districts: ['Bhopal', 'Indore', 'Jabalpur', 'Gwalior', 'Ujjain', 'Sagar', 'Dewas', 'Satna'],
  },
  {
    state: 'Maharashtra',
    districts: ['Mumbai City', 'Mumbai Suburban', 'Pune', 'Nagpur', 'Thane', 'Nashik', 'Chhatrapati Sambhajinagar', 'Solapur'],
  },
  {
    state: 'Manipur',
    districts: ['Imphal East', 'Imphal West', 'Thoubal', 'Bishnupur', 'Churachandpur'],
  },
  {
    state: 'Meghalaya',
    districts: ['Shillong', 'Tura', 'Jowai', 'Nongpoh'],
  },
  {
    state: 'Mizoram',
    districts: ['Aizawl', 'Lunglei', 'Champhai', 'Serchhip'],
  },
  {
    state: 'Nagaland',
    districts: ['Kohima', 'Dimapur', 'Mokokchung', 'Tuensang', 'Wokha'],
  },
  {
    state: 'Odisha',
    districts: ['Bhubaneswar', 'Cuttack', 'Rourkela', 'Berhampur', 'Sambalpur', 'Puri', 'Balasore'],
  },
  {
    state: 'Punjab',
    districts: ['Ludhiana', 'Amritsar', 'Jalandhar', 'Patiala', 'Bathinda', 'Mohali (SAS Nagar)'],
  },
  {
    state: 'Rajasthan',
    districts: ['Jaipur', 'Jodhpur', 'Kota', 'Bikaner', 'Ajmer', 'Udaipur', 'Bhilwara', 'Alwar'],
  },
  {
    state: 'Sikkim',
    districts: ['Gangtok', 'Namchi', 'Gyalshing', 'Mangan'],
  },
  {
    state: 'Tamil Nadu',
    districts: ['Chennai', 'Coimbatore', 'Madurai', 'Tiruchirappalli', 'Salem', 'Tirunelveli', 'Erode', 'Vellore'],
  },
  {
    state: 'Telangana',
    districts: ['Hyderabad', 'Warangal', 'Nizamabad', 'Karimnagar', 'Khammam', 'Ramagundam'],
  },
  {
    state: 'Tripura',
    districts: ['Agartala', 'Dharmanagar', 'Udaipur', 'Kailashahar'],
  },
  {
    state: 'Uttar Pradesh',
    districts: ['Lucknow', 'Kanpur', 'Varanasi', 'Prayagraj', 'Agra', 'Noida (GB Nagar)', 'Ghaziabad', 'Meerut', 'Bareilly'],
  },
  {
    state: 'Uttarakhand',
    districts: ['Dehradun', 'Haridwar', 'Roorkee', 'Haldwani', 'Rishikesh', 'Nainital'],
  },
  {
    state: 'West Bengal',
    districts: ['Kolkata', 'Howrah', 'Durgapur', 'Asansol', 'Siliguri', 'Kharagpur'],
  },
  {
    state: 'Delhi (NCT)',
    districts: ['New Delhi', 'Central Delhi', 'South Delhi', 'North Delhi', 'East Delhi', 'West Delhi', 'Shahdara'],
  },
  {
    state: 'Jammu and Kashmir',
    districts: ['Srinagar', 'Jammu', 'Anantnag', 'Baramulla', 'Udhampur'],
  },
  {
    state: 'Ladakh',
    districts: ['Leh', 'Kargil'],
  },
  {
    state: 'Chandigarh',
    districts: ['Chandigarh'],
  },
  {
    state: 'Puducherry',
    districts: ['Puducherry', 'Karaikal', 'Mahe', 'Yanam'],
  },
];

export const DEFAULT_QUALIFICATIONS: string[] = [
  '10th Standard / Secondary (SSC)',
  '12th Standard / Higher Secondary (HSC)',
  'Diploma / Polytechnic',
  'Bachelor Degree (B.Tech / B.E / B.Sc / B.Com / B.A / BBA)',
  'Master Degree (M.Tech / M.E / M.Sc / M.Com / MBA / MCA)',
  'Doctorate (Ph.D)',
  'Professional Certification / Other',
];
