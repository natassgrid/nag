import React, { useState, useEffect } from 'react';
import {
  ProfileController,
  ProfileState,
  EducationRecord,
  ProfileCategory,
  ProfileGender,
  INDIAN_STATES_AND_CITIES,
  DEFAULT_QUALIFICATIONS,
} from './profile-controller';

export interface CandidateProfileScreenProps {
  controller: ProfileController;
  onProfileSaved?: () => void;
}

type ProfileSectionTab = 'PERSONAL' | 'ADDRESS' | 'EDUCATION' | 'IDENTITY' | 'CATEGORY';

export const CandidateProfileScreen: React.FC<CandidateProfileScreenProps> = ({
  controller,
  onProfileSaved,
}) => {
  const [profileState, setProfileState] = useState<ProfileState>(controller.getState());
  const [activeTab, setActiveTab] = useState<ProfileSectionTab>('PERSONAL');

  // Education Form Modal State
  const [showEduModal, setShowEduModal] = useState<boolean>(false);
  const [editingEduId, setEditingEduId] = useState<string | null>(null);
  const [eduQualification, setEduQualification] = useState<string>(DEFAULT_QUALIFICATIONS[0]);
  const [eduBoard, setEduBoard] = useState<string>('');
  const [eduInstitution, setEduInstitution] = useState<string>('');
  const [eduYear, setEduYear] = useState<number>(2023);
  const [eduScoreType, setEduScoreType] = useState<'PERCENTAGE' | 'CGPA'>('PERCENTAGE');
  const [eduScore, setEduScore] = useState<string>('85.0');
  const [eduError, setEduError] = useState<string | null>(null);

  // DigiLocker Input State
  const [showDigiLockerInput, setShowDigiLockerInput] = useState<boolean>(false);
  const [aadhaarInput, setAadhaarInput] = useState<string>('');

  useEffect(() => {
    return controller.subscribe((state: ProfileState) => {
      setProfileState(state);
    });
  }, [controller]);

  const profile = profileState.profile;

  const handleOpenAddEdu = () => {
    setEditingEduId(null);
    setEduQualification(DEFAULT_QUALIFICATIONS[0]);
    setEduBoard('');
    setEduInstitution('');
    setEduYear(2023);
    setEduScoreType('PERCENTAGE');
    setEduScore('85.0');
    setEduError(null);
    setShowEduModal(true);
  };

  const handleOpenEditEdu = (record: EducationRecord) => {
    setEditingEduId(record.id);
    setEduQualification(record.qualification);
    setEduBoard(record.boardOrUniversity);
    setEduInstitution(record.institutionName || '');
    setEduYear(record.passingYear);
    setEduScoreType(record.scoreType);
    setEduScore(record.percentageOrCgpa);
    setEduError(null);
    setShowEduModal(true);
  };

  const handleSaveEdu = () => {
    setEduError(null);
    const entryData = {
      qualification: eduQualification,
      boardOrUniversity: eduBoard,
      institutionName: eduInstitution,
      passingYear: Number(eduYear),
      scoreType: eduScoreType,
      percentageOrCgpa: eduScore,
    };

    if (editingEduId) {
      const res = controller.updateEducation(editingEduId, entryData);
      if (!res.success) {
        setEduError(res.error || 'Failed to update qualification.');
        return;
      }
    } else {
      const res = controller.addEducation(entryData);
      if (!res.success) {
        setEduError(res.error || 'Failed to add qualification.');
        return;
      }
    }

    setShowEduModal(false);
  };

  const handleDigiLockerLink = () => {
    if (aadhaarInput.trim().length >= 4) {
      controller.linkDigiLocker(aadhaarInput.trim());
      setShowDigiLockerInput(false);
      setAadhaarInput('');
    }
  };

  const handleSaveAll = () => {
    controller.saveProfile();
    if (onProfileSaved) {
      onProfileSaved();
    }
  };

  return (
    <div
      data-testid="candidate-profile-screen"
      style={{
        padding: '16px',
        paddingBottom: '80px',
        backgroundColor: '#F7FAFC',
        minHeight: '100%',
      }}
    >
      {/* 1. Profile Completion Header Banner */}
      <div
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          padding: '16px',
          marginBottom: '16px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 2px 4px rgba(0,0,0,0.04)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '800', color: '#1A365D' }}>
              {profile.fullName}
            </h2>
            <div style={{ fontSize: '12px', color: '#718096', marginTop: '2px' }}>
              NAG ID: {profile.candidateId} • {profile.category}
            </div>
          </div>
          <div
            data-testid="profile-completion-text"
            style={{
              fontSize: '14px',
              fontWeight: '800',
              color: profile.profileCompletionPercentage === 100 ? '#38A169' : '#DD6B20',
            }}
          >
            {profile.profileCompletionPercentage}% Complete
          </div>
        </div>

        {/* Progress Bar */}
        <div
          data-testid="profile-completion-bar"
          style={{
            height: '8px',
            backgroundColor: '#EDF2F7',
            borderRadius: '4px',
            overflow: 'hidden',
            marginTop: '12px',
          }}
        >
          <div
            style={{
              height: '100%',
              width: `${profile.profileCompletionPercentage}%`,
              backgroundColor:
                profile.profileCompletionPercentage === 100 ? '#38A169' : '#3182CE',
              transition: 'width 0.3s ease',
            }}
          />
        </div>
      </div>

      {/* 2. Section Navigation Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '6px',
          overflowX: 'auto',
          paddingBottom: '8px',
          marginBottom: '16px',
        }}
      >
        {(
          [
            { id: 'PERSONAL', label: 'Personal', testId: 'tab-personal' },
            { id: 'ADDRESS', label: 'Address', testId: 'tab-address' },
            { id: 'EDUCATION', label: 'Education', testId: 'tab-education' },
            { id: 'IDENTITY', label: 'KYC & Aadhaar', testId: 'tab-identity' },
            { id: 'CATEGORY', label: 'Category & PwD', testId: 'tab-category' },
          ] as const
        ).map((tab) => {
          const active = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              data-testid={tab.testId}
              onClick={() => setActiveTab(tab.id)}
              style={{
                backgroundColor: active ? '#2B6CB0' : '#EDF2F7',
                color: active ? '#FFFFFF' : '#4A5568',
                border: 'none',
                borderRadius: '8px',
                padding: '8px 12px',
                fontSize: '12px',
                fontWeight: active ? '700' : '600',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* 3. Tab Contents */}
      <div
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          padding: '16px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
        }}
      >
        {/* Tab 1: Personal Details */}
        {activeTab === 'PERSONAL' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '15px', color: '#2D3748' }}>
              Personal & Contact Information
            </h3>

            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                Full Name (As per Matriculation Certificate)
              </label>
              <input
                data-testid="input-full-name"
                type="text"
                value={profile.fullName}
                onChange={(e) => controller.updatePersonalDetails({ fullName: e.target.value })}
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E0',
                  marginTop: '4px',
                  fontSize: '13px',
                }}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                  Date of Birth
                </label>
                <input
                  data-testid="input-dob"
                  type="date"
                  value={profile.dateOfBirth}
                  onChange={(e) =>
                    controller.updatePersonalDetails({ dateOfBirth: e.target.value })
                  }
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    marginTop: '4px',
                    fontSize: '13px',
                  }}
                />
              </div>

              <div>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                  Gender
                </label>
                <select
                  data-testid="select-gender"
                  value={profile.gender}
                  onChange={(e) =>
                    controller.updatePersonalDetails({
                      gender: e.target.value as ProfileGender,
                    })
                  }
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    marginTop: '4px',
                    fontSize: '13px',
                    backgroundColor: '#FFFFFF',
                  }}
                >
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
            </div>

            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                Mobile Number
              </label>
              <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                <input
                  data-testid="input-mobile"
                  type="tel"
                  value={profile.mobile}
                  onChange={(e) => controller.updateContactDetails(e.target.value, profile.email)}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '13px',
                  }}
                />
                {profile.mobileVerified ? (
                  <span
                    style={{
                      fontSize: '11px',
                      color: '#276749',
                      backgroundColor: '#C6F6D5',
                      padding: '6px 8px',
                      borderRadius: '4px',
                      fontWeight: '700',
                      alignSelf: 'center',
                    }}
                  >
                    ✓ Verified
                  </span>
                ) : (
                  <button
                    onClick={() => controller.verifyMobileOtp('123456')}
                    style={{
                      backgroundColor: '#3182CE',
                      color: '#FFFFFF',
                      border: 'none',
                      borderRadius: '6px',
                      padding: '6px 10px',
                      fontSize: '11px',
                      fontWeight: '700',
                      cursor: 'pointer',
                    }}
                  >
                    Verify OTP
                  </button>
                )}
              </div>
            </div>

            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                Email Address
              </label>
              <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                <input
                  data-testid="input-email"
                  type="email"
                  value={profile.email}
                  onChange={(e) => controller.updateContactDetails(profile.mobile, e.target.value)}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '13px',
                  }}
                />
                {profile.emailVerified ? (
                  <span
                    style={{
                      fontSize: '11px',
                      color: '#276749',
                      backgroundColor: '#C6F6D5',
                      padding: '6px 8px',
                      borderRadius: '4px',
                      fontWeight: '700',
                      alignSelf: 'center',
                    }}
                  >
                    ✓ Verified
                  </span>
                ) : (
                  <button
                    onClick={() => controller.verifyEmailOtp('123456')}
                    style={{
                      backgroundColor: '#3182CE',
                      color: '#FFFFFF',
                      border: 'none',
                      borderRadius: '6px',
                      padding: '6px 10px',
                      fontSize: '11px',
                      fontWeight: '700',
                      cursor: 'pointer',
                    }}
                  >
                    Verify OTP
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Residential Address */}
        {activeTab === 'ADDRESS' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '15px', color: '#2D3748' }}>
              Permanent & Correspondence Address
            </h3>

            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                Address Line / House No. / Street
              </label>
              <textarea
                data-testid="input-address-line"
                rows={2}
                value={profile.address.addressLine}
                onChange={(e) => controller.updateAddress({ addressLine: e.target.value })}
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E0',
                  marginTop: '4px',
                  fontSize: '13px',
                }}
              />
            </div>

            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                State / Union Territory
              </label>
              <select
                data-testid="select-state"
                value={profile.address.state}
                onChange={(e) => controller.onStateChange(e.target.value)}
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E0',
                  marginTop: '4px',
                  fontSize: '13px',
                  backgroundColor: '#FFFFFF',
                }}
              >
                {INDIAN_STATES_AND_CITIES.map((s) => (
                  <option key={s.state} value={s.state}>
                    {s.state}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                  City / District
                </label>
                <select
                  data-testid="select-city"
                  value={profile.address.city}
                  onChange={(e) =>
                    controller.updateAddress({ city: e.target.value, district: e.target.value })
                  }
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    marginTop: '4px',
                    fontSize: '13px',
                    backgroundColor: '#FFFFFF',
                  }}
                >
                  {profileState.availableCities.map((city) => (
                    <option key={city} value={city}>
                      {city}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                  PIN Code
                </label>
                <input
                  data-testid="input-pincode"
                  type="text"
                  maxLength={6}
                  value={profile.address.pinCode}
                  onChange={(e) => controller.updateAddress({ pinCode: e.target.value })}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    marginTop: '4px',
                    fontSize: '13px',
                  }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Educational Qualifications */}
        {activeTab === 'EDUCATION' && (
          <div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '12px',
              }}
            >
              <h3 style={{ margin: 0, fontSize: '15px', color: '#2D3748' }}>
                Qualifications ({profile.education.length})
              </h3>
              <button
                data-testid="btn-add-education"
                onClick={handleOpenAddEdu}
                style={{
                  backgroundColor: '#2B6CB0',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '6px 12px',
                  fontSize: '12px',
                  fontWeight: '700',
                  cursor: 'pointer',
                }}
              >
                + Add Qualification
              </button>
            </div>

            <div
              data-testid="education-list"
              style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}
            >
              {profile.education.map((edu) => (
                <div
                  key={edu.id}
                  data-testid={`education-card-${edu.id}`}
                  style={{
                    border: '1px solid #E2E8F0',
                    borderRadius: '8px',
                    padding: '12px',
                    backgroundColor: '#F7FAFC',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'flex-start',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: '700', fontSize: '13px', color: '#1A365D' }}>
                        {edu.qualification}
                      </div>
                      <div style={{ fontSize: '12px', color: '#4A5568', marginTop: '2px' }}>
                        {edu.boardOrUniversity} • {edu.passingYear}
                      </div>
                      <div style={{ fontSize: '11px', color: '#718096', marginTop: '2px' }}>
                        Score: <strong>{edu.percentageOrCgpa}</strong> ({edu.scoreType})
                      </div>
                      {edu.verifiedViaDigiLocker && (
                        <span
                          style={{
                            display: 'inline-block',
                            marginTop: '4px',
                            fontSize: '10px',
                            fontWeight: '700',
                            color: '#22543D',
                            backgroundColor: '#C6F6D5',
                            padding: '2px 6px',
                            borderRadius: '4px',
                          }}
                        >
                          ✓ DigiLocker Verified
                        </span>
                      )}
                    </div>
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        data-testid={`btn-edit-education-${edu.id}`}
                        onClick={() => handleOpenEditEdu(edu)}
                        style={{
                          backgroundColor: '#EDF2F7',
                          border: 'none',
                          borderRadius: '4px',
                          padding: '4px 8px',
                          fontSize: '11px',
                          fontWeight: '600',
                          color: '#2B6CB0',
                          cursor: 'pointer',
                        }}
                      >
                        Edit
                      </button>
                      <button
                        data-testid={`btn-delete-education-${edu.id}`}
                        onClick={() => controller.deleteEducation(edu.id)}
                        style={{
                          backgroundColor: '#FED7D7',
                          border: 'none',
                          borderRadius: '4px',
                          padding: '4px 8px',
                          fontSize: '11px',
                          fontWeight: '600',
                          color: '#9B2C2C',
                          cursor: 'pointer',
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab 4: Identity & DigiLocker */}
        {activeTab === 'IDENTITY' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <h3 style={{ margin: 0, fontSize: '15px', color: '#2D3748' }}>
              DigiLocker & Official Identity Verification
            </h3>

            {/* Aadhaar Card Status */}
            <div
              data-testid="digilocker-kyc-card"
              style={{
                border: '1px solid #9AE6B4',
                backgroundColor: '#F0FFF4',
                borderRadius: '8px',
                padding: '14px',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div
                    data-testid="badge-aadhaar-verified"
                    style={{
                      fontSize: '11px',
                      fontWeight: '700',
                      color: '#22543D',
                      textTransform: 'uppercase',
                    }}
                  >
                    ✓ Aadhaar e-KYC Verified
                  </div>
                  <div style={{ fontSize: '14px', fontWeight: '700', marginTop: '2px' }}>
                    {profile.identity.docNumber}
                  </div>
                </div>
                <div style={{ fontSize: '24px' }}>🆔</div>
              </div>
            </div>

            {/* Link / Re-link DigiLocker */}
            {!showDigiLockerInput ? (
              <button
                data-testid="btn-link-digilocker"
                onClick={() => setShowDigiLockerInput(true)}
                style={{
                  backgroundColor: '#2B6CB0',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '8px 12px',
                  fontSize: '12px',
                  fontWeight: '700',
                  cursor: 'pointer',
                }}
              >
                Link / Re-link DigiLocker Aadhaar
              </button>
            ) : (
              <div style={{ display: 'flex', gap: '8px' }}>
                <input
                  data-testid="input-aadhaar-number"
                  type="text"
                  placeholder="Enter 12-digit Aadhaar / Virtual ID"
                  value={aadhaarInput}
                  onChange={(e) => setAadhaarInput(e.target.value)}
                  style={{
                    flex: 1,
                    padding: '8px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                  }}
                />
                <button
                  onClick={handleDigiLockerLink}
                  style={{
                    backgroundColor: '#38A169',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    padding: '8px 12px',
                    fontSize: '12px',
                    fontWeight: '700',
                    cursor: 'pointer',
                  }}
                >
                  Verify
                </button>
              </div>
            )}

            {/* Biometric & Photograph Check */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '10px',
                marginTop: '6px',
              }}
            >
              <div
                style={{
                  border: '1px solid #E2E8F0',
                  borderRadius: '8px',
                  padding: '10px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '11px', color: '#718096' }}>Candidate Photo</div>
                <div
                  style={{
                    fontSize: '12px',
                    fontWeight: '700',
                    color: profile.identity.photoUploaded ? '#276749' : '#C53030',
                    marginTop: '4px',
                  }}
                >
                  {profile.identity.photoUploaded ? '✓ Uploaded' : 'Missing'}
                </div>
              </div>

              <div
                style={{
                  border: '1px solid #E2E8F0',
                  borderRadius: '8px',
                  padding: '10px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '11px', color: '#718096' }}>Candidate Signature</div>
                <div
                  style={{
                    fontSize: '12px',
                    fontWeight: '700',
                    color: profile.identity.signatureUploaded ? '#276749' : '#C53030',
                    marginTop: '4px',
                  }}
                >
                  {profile.identity.signatureUploaded ? '✓ Uploaded' : 'Missing'}
                </div>
              </div>
            </div>

            {/* Biometric Attendance Consent */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                marginTop: '8px',
                padding: '8px',
                backgroundColor: '#EDF2F7',
                borderRadius: '6px',
              }}
            >
              <input
                data-testid="toggle-biometric-consent"
                type="checkbox"
                checked={profile.identity.biometricConsent}
                onChange={(e) => controller.setBiometricConsent(e.target.checked)}
              />
              <span style={{ fontSize: '11px', color: '#4A5568' }}>
                I give consent for biometric facial verification and attendance scanning at the
                examination venue.
              </span>
            </div>
          </div>
        )}

        {/* Tab 5: Category & PwD Preferences */}
        {activeTab === 'CATEGORY' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <h3 style={{ margin: 0, fontSize: '15px', color: '#2D3748' }}>
              Reservation Category & PwD Accommodation
            </h3>

            <div>
              <label style={{ fontSize: '12px', fontWeight: '600', color: '#4A5568' }}>
                Reservation Category
              </label>
              <select
                data-testid="select-category"
                value={profile.category}
                onChange={(e) => controller.setCategory(e.target.value as ProfileCategory)}
                style={{
                  width: '100%',
                  boxSizing: 'border-box',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E0',
                  marginTop: '4px',
                  fontSize: '13px',
                  backgroundColor: '#FFFFFF',
                }}
              >
                <option value="GENERAL">General (Unreserved)</option>
                <option value="OBC_NCL">OBC (Non-Creamy Layer)</option>
                <option value="SC">Scheduled Caste (SC)</option>
                <option value="ST">Scheduled Tribe (ST)</option>
                <option value="EWS">Economically Weaker Section (EWS)</option>
              </select>
            </div>

            <div
              style={{
                border: '1px solid #E2E8F0',
                borderRadius: '8px',
                padding: '12px',
                backgroundColor: '#F7FAFC',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div style={{ fontSize: '13px', fontWeight: '700', color: '#2D3748' }}>
                    Person with Benchmark Disability (PwD)
                  </div>
                  <div style={{ fontSize: '11px', color: '#718096' }}>
                    Eligibility for scribe and compensatory time
                  </div>
                </div>
                <input
                  data-testid="toggle-pwd-status"
                  type="checkbox"
                  checked={profile.pwdDetails.isPwd}
                  onChange={(e) =>
                    controller.updatePwdPreferences({ isPwd: e.target.checked })
                  }
                />
              </div>

              {profile.pwdDetails.isPwd && (
                <div
                  style={{
                    marginTop: '12px',
                    paddingTop: '10px',
                    borderTop: '1px dashed #CBD5E0',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '8px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input
                      data-testid="toggle-requires-scribe"
                      type="checkbox"
                      checked={profile.pwdDetails.requiresScribe}
                      onChange={(e) =>
                        controller.updatePwdPreferences({
                          requiresScribe: e.target.checked,
                        })
                      }
                    />
                    <span style={{ fontSize: '12px', color: '#4A5568' }}>
                      Require Scribe assistance during examination
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <input
                      type="checkbox"
                      checked={profile.pwdDetails.requiresCompensatoryTime}
                      onChange={(e) =>
                        controller.updatePwdPreferences({
                          requiresCompensatoryTime: e.target.checked,
                        })
                      }
                    />
                    <span style={{ fontSize: '12px', color: '#4A5568' }}>
                      Require 20 minutes/hour compensatory extra time
                    </span>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Global Save Button */}
        <div style={{ marginTop: '16px', borderTop: '1px solid #E2E8F0', paddingTop: '12px' }}>
          <button
            data-testid="btn-save-profile"
            onClick={handleSaveAll}
            style={{
              width: '100%',
              backgroundColor: '#2B6CB0',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '8px',
              padding: '10px',
              fontSize: '13px',
              fontWeight: '700',
              cursor: 'pointer',
            }}
          >
            💾 Save Profile Details
          </button>
        </div>
      </div>

      {/* 4. Education Add / Edit Form Modal */}
      {showEduModal && (
        <div
          data-testid="modal-education-form"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '16px',
          }}
        >
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '12px',
              maxWidth: '440px',
              width: '100%',
              padding: '20px',
              boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '12px',
              }}
            >
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1A365D' }}>
                {editingEduId ? 'Edit Qualification' : 'Add New Qualification'}
              </h3>
              <button
                data-testid="btn-cancel-education"
                onClick={() => setShowEduModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '20px',
                  color: '#A0AEC0',
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {eduError && (
              <div
                style={{
                  backgroundColor: '#FED7D7',
                  color: '#9B2C2C',
                  fontSize: '12px',
                  padding: '8px',
                  borderRadius: '6px',
                  marginBottom: '10px',
                }}
              >
                {eduError}
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div>
                <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                  Qualification Level
                </label>
                <select
                  data-testid="input-edu-qualification"
                  value={eduQualification}
                  onChange={(e) => setEduQualification(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                  }}
                >
                  {DEFAULT_QUALIFICATIONS.map((q) => (
                    <option key={q} value={q}>
                      {q}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                  Board / University Name *
                </label>
                <input
                  data-testid="input-edu-board"
                  type="text"
                  placeholder="e.g. CBSE / Delhi University"
                  value={eduBoard}
                  onChange={(e) => setEduBoard(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                  }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <div>
                  <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                    Passing Year *
                  </label>
                  <input
                    data-testid="input-edu-year"
                    type="number"
                    value={eduYear}
                    onChange={(e) => setEduYear(Number(e.target.value))}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      padding: '8px',
                      borderRadius: '6px',
                      border: '1px solid #CBD5E0',
                      fontSize: '12px',
                    }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                    Score Type
                  </label>
                  <select
                    data-testid="select-edu-score-type"
                    value={eduScoreType}
                    onChange={(e) =>
                      setEduScoreType(e.target.value as 'PERCENTAGE' | 'CGPA')
                    }
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      padding: '8px',
                      borderRadius: '6px',
                      border: '1px solid #CBD5E0',
                      fontSize: '12px',
                    }}
                  >
                    <option value="PERCENTAGE">Percentage (%)</option>
                    <option value="CGPA">CGPA (10 Point Scale)</option>
                  </select>
                </div>
              </div>

              <div>
                <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                  Score Value ({eduScoreType === 'PERCENTAGE' ? '%' : 'CGPA'}) *
                </label>
                <input
                  data-testid="input-edu-score"
                  type="text"
                  placeholder={eduScoreType === 'PERCENTAGE' ? 'e.g. 85.5' : 'e.g. 8.5'}
                  value={eduScore}
                  onChange={(e) => setEduScore(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                  }}
                />
              </div>

              <button
                data-testid="btn-save-education"
                onClick={handleSaveEdu}
                style={{
                  width: '100%',
                  marginTop: '8px',
                  backgroundColor: '#2B6CB0',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '10px',
                  fontSize: '13px',
                  fontWeight: '700',
                  cursor: 'pointer',
                }}
              >
                {editingEduId ? 'Update Record' : 'Add Record'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
