export interface EducationFormState {
  id: string;
  qualification: string;
  courseName: string;
  boardOrUniversity: string;
  institutionName: string;
  passingYear: number;
  scoreType: 'PERCENTAGE' | 'CGPA';
  scoreValue: string;
  specialization: string;
  rollNumber: string;
  certificateAssetId?: string;
  certificateFileName?: string;
}

export const DEFAULT_QUALIFICATIONS: string[] = [
  '10th Standard / Secondary (SSC)',
  '12th Standard / Higher Secondary (HSC)',
  'Diploma / Polytechnic',
  'Bachelor Degree (B.Tech / B.E / B.Sc / B.Com / B.A / BBA)',
  'Master Degree (M.Tech / M.E / M.Sc / M.Com / MBA / MCA)',
  'Doctorate (Ph.D)',
  'Professional Certification / Other',
];
