import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  User, 
  MapPin, 
  GraduationCap, 
  Upload, 
  CheckCircle, 
  AlertCircle, 
  Save,
  Loader2,
  FileCheck
} from 'lucide-react';

export const Profile: React.FC = () => {
  const { user, updateProfile } = useAuth();
  const [activeTab, setActiveTab] = useState<'personal' | 'contact' | 'education' | 'documents'>('personal');
  
  // Personal Details state
  const [name, setName] = useState(user?.name || '');
  const [dob, setDob] = useState(user?.dob || '');
  const [gender, setGender] = useState(user?.gender || '');
  const [category, setCategory] = useState(user?.category || '');

  // Contact Details (read-only from user)
  const email = user?.email || '';
  const mobile = user?.mobile || '';
  const [address, setAddress] = useState(user?.address || '');

  // Education state
  const [qualification, setQualification] = useState(user?.qualification || '');
  const [university, setUniversity] = useState(user?.university || '');
  const [passingYear, setPassingYear] = useState(user?.passingYear || '');
  const [percentage, setPercentage] = useState(user?.percentage || '');

  // Document states (Upload simulations)
  const [uploading, setUploading] = useState<{ [key: string]: boolean }>({});
  const [uploadProgress, setUploadProgress] = useState<{ [key: string]: number }>({});
  
  const [isSaving, setIsSaving] = useState(false);
  const [alertMsg, setAlertMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const tabs = [
    { id: 'personal', name: 'Personal Details', icon: User },
    { id: 'contact', name: 'Contact Details', icon: MapPin },
    { id: 'education', name: 'Education History', icon: GraduationCap },
    { id: 'documents', name: 'Document Uploads', icon: Upload },
  ] as const;

  const handleSavePersonal = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setAlertMsg(null);
    try {
      await new Promise((resolve) => setTimeout(resolve, 800));
      updateProfile({ name, dob, gender, category });
      setAlertMsg({ type: 'success', text: 'Personal details updated successfully.' });
    } catch {
      setAlertMsg({ type: 'error', text: 'Failed to update personal details.' });
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveContact = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setAlertMsg(null);
    try {
      await new Promise((resolve) => setTimeout(resolve, 800));
      updateProfile({ email, mobile, address });
      setAlertMsg({ type: 'success', text: 'Contact details updated successfully.' });
    } catch {
      setAlertMsg({ type: 'error', text: 'Failed to update contact details.' });
    } finally {
      setIsSaving(false);
    }
  };

  const handleSaveEducation = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setAlertMsg(null);
    try {
      await new Promise((resolve) => setTimeout(resolve, 800));
      updateProfile({ qualification, university, passingYear, percentage });
      setAlertMsg({ type: 'success', text: 'Educational history updated successfully.' });
    } catch {
      setAlertMsg({ type: 'error', text: 'Failed to update education details.' });
    } finally {
      setIsSaving(false);
    }
  };

  const simulateUpload = (docType: 'photoUploaded' | 'signatureUploaded' | 'idProofUploaded') => {
    setUploading(prev => ({ ...prev, [docType]: true }));
    setUploadProgress(prev => ({ ...prev, [docType]: 0 }));
    
    let progress = 0;
    const interval = setInterval(() => {
      progress += 20;
      setUploadProgress(prev => ({ ...prev, [docType]: progress }));
      
      if (progress >= 100) {
        clearInterval(interval);
        setUploading(prev => ({ ...prev, [docType]: false }));
        updateProfile({ [docType]: true });
        setAlertMsg({ type: 'success', text: `${docType.replace('Uploaded', '')} document uploaded and verified successfully.` });
      }
    }, 150);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between space-y-2 md:space-y-0">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-800">Profile Management</h1>
          <p className="text-sm text-slate-500">Provide verified educational, personal and ID details.</p>
        </div>
      </div>

      {/* Main Panel */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        {/* Navigation Tabs */}
        <div className="border-b border-gray-200 bg-gray-50/50">
          <nav className="flex flex-wrap -mb-px px-4" aria-label="Tabs">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => {
                    setActiveTab(tab.id);
                    setAlertMsg(null);
                  }}
                  className={`group flex items-center py-4 px-4 font-semibold text-sm border-b-2 transition-colors ${
                    isActive
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <Icon className={`mr-2 h-4 w-4 ${isActive ? 'text-indigo-600' : 'text-gray-400 group-hover:text-gray-500'}`} />
                  {tab.name}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Form Body */}
        <div className="p-6 md:p-8">
          {alertMsg && (
            <div className={`mb-6 p-4 rounded-lg border flex items-center ${
              alertMsg.type === 'success' 
                ? 'bg-green-50 border-green-200 text-green-800' 
                : 'bg-red-50 border-red-200 text-red-800'
            }`}>
              {alertMsg.type === 'success' ? (
                <CheckCircle className="h-5 w-5 mr-3 flex-shrink-0 text-green-600" />
              ) : (
                <AlertCircle className="h-5 w-5 mr-3 flex-shrink-0 text-red-600" />
              )}
              <span className="text-sm font-semibold">{alertMsg.text}</span>
            </div>
          )}

          {/* TAB 1: PERSONAL */}
          {activeTab === 'personal' && (
            <form onSubmit={handleSavePersonal} className="space-y-6 max-w-2xl">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Full Name (As per Matriculation)</label>
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Date of Birth</label>
                  <input
                    type="date"
                    required
                    value={dob}
                    onChange={(e) => setDob(e.target.value)}
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Gender</label>
                  <select
                    value={gender}
                    onChange={(e) => setGender(e.target.value)}
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  >
                    <option value="">Select Gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Category</label>
                  <select
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  >
                    <option value="">Select Category</option>
                    <option value="General">General / Unreserved</option>
                    <option value="OBC">OBC</option>
                    <option value="SC">SC</option>
                    <option value="ST">ST</option>
                    <option value="EWS">EWS</option>
                  </select>
                </div>
              </div>
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold text-sm flex items-center shadow transition-colors disabled:opacity-50"
                >
                  {isSaving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
                  Save Personal Details
                </button>
              </div>
            </form>
          )}

          {/* TAB 2: CONTACT */}
          {activeTab === 'contact' && (
            <form onSubmit={handleSaveContact} className="space-y-6 max-w-2xl">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Email Address (Verified)</label>
                  <input
                    type="email"
                    disabled
                    value={email}
                    className="mt-1 block w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-md text-gray-500 text-sm cursor-not-allowed"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Mobile Number (Verified)</label>
                  <input
                    type="tel"
                    disabled
                    value={mobile}
                    className="mt-1 block w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-md text-gray-500 text-sm cursor-not-allowed"
                  />
                </div>
                <div className="md:col-span-2">
                  <label className="block text-sm font-medium text-slate-700">Permanent Correspondence Address</label>
                  <textarea
                    required
                    rows={3}
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    placeholder="Enter flat/door no, street, locality, city, state, pin code"
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
              </div>
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold text-sm flex items-center shadow transition-colors disabled:opacity-50"
                >
                  {isSaving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
                  Save Contact Details
                </button>
              </div>
            </form>
          )}

          {/* TAB 3: EDUCATION */}
          {activeTab === 'education' && (
            <form onSubmit={handleSaveEducation} className="space-y-6 max-w-2xl">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Highest Qualification Degree</label>
                  <input
                    type="text"
                    required
                    value={qualification}
                    onChange={(e) => setQualification(e.target.value)}
                    placeholder="e.g. B.Tech / B.Sc / Class XII"
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">University / Board Name</label>
                  <input
                    type="text"
                    required
                    value={university}
                    onChange={(e) => setUniversity(e.target.value)}
                    placeholder="State University"
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Year of Passing</label>
                  <input
                    type="number"
                    required
                    min={2000}
                    max={2026}
                    value={passingYear}
                    onChange={(e) => setPassingYear(e.target.value)}
                    placeholder="2024"
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Aggregate Percentage / CGPA</label>
                  <input
                    type="text"
                    required
                    value={percentage}
                    onChange={(e) => setPercentage(e.target.value)}
                    placeholder="85.5% or 8.5 CGPA"
                    className="mt-1 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
                  />
                </div>
              </div>
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold text-sm flex items-center shadow transition-colors disabled:opacity-50"
                >
                  {isSaving ? <Loader2 className="h-4 w-4 mr-2 animate-spin" /> : <Save className="h-4 w-4 mr-2" />}
                  Save Education History
                </button>
              </div>
            </form>
          )}

          {/* TAB 4: DOCUMENTS */}
          {activeTab === 'documents' && (
            <div className="space-y-8 max-w-3xl">
              <p className="text-sm text-gray-500">
                Upload files in JPEG or PDF format. Maximum limit per file is 2MB. Pick a file to run the mock upload validation.
              </p>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Photo Upload */}
                <div className="border border-gray-200 rounded-xl p-5 flex flex-col justify-between items-center text-center bg-gray-50/20">
                  <div className="space-y-2">
                    <h3 className="font-bold text-sm text-slate-800">Recent Photograph</h3>
                    <p className="text-[10px] text-gray-400">Passport size (White background)</p>
                  </div>
                  
                  <div className="my-6">
                    {user?.photoUploaded ? (
                      <div className="flex flex-col items-center text-green-600 space-y-1">
                        <FileCheck className="h-10 w-10 text-green-500" />
                        <span className="text-xs font-bold">Uploaded</span>
                      </div>
                    ) : (
                      <span className="text-xs text-amber-500 font-semibold">Missing Photo</span>
                    )}
                  </div>

                  <div className="w-full">
                    {uploading.photoUploaded ? (
                      <div className="w-full space-y-1">
                        <div className="w-full bg-gray-200 h-1.5 rounded-full">
                          <div className="bg-indigo-600 h-1.5 rounded-full" style={{ width: `${uploadProgress.photoUploaded}%` }}></div>
                        </div>
                        <span className="text-[10px] text-gray-400 font-medium">Uploading {uploadProgress.photoUploaded}%</span>
                      </div>
                    ) : (
                      <button
                        onClick={() => simulateUpload('photoUploaded')}
                        className="w-full py-1.5 px-3 border border-indigo-600 text-indigo-600 hover:bg-indigo-50 rounded-lg text-xs font-bold transition-colors"
                      >
                        {user?.photoUploaded ? 'Re-upload Photo' : 'Upload Photo'}
                      </button>
                    )}
                  </div>
                </div>

                {/* Signature Upload */}
                <div className="border border-gray-200 rounded-xl p-5 flex flex-col justify-between items-center text-center bg-gray-50/20">
                  <div className="space-y-2">
                    <h3 className="font-bold text-sm text-slate-800">Candidate Signature</h3>
                    <p className="text-[10px] text-gray-400">Black/Blue ink on plain white paper</p>
                  </div>
                  
                  <div className="my-6">
                    {user?.signatureUploaded ? (
                      <div className="flex flex-col items-center text-green-600 space-y-1">
                        <FileCheck className="h-10 w-10 text-green-500" />
                        <span className="text-xs font-bold">Uploaded</span>
                      </div>
                    ) : (
                      <span className="text-xs text-amber-500 font-semibold">Missing Signature</span>
                    )}
                  </div>

                  <div className="w-full">
                    {uploading.signatureUploaded ? (
                      <div className="w-full space-y-1">
                        <div className="w-full bg-gray-200 h-1.5 rounded-full">
                          <div className="bg-indigo-600 h-1.5 rounded-full" style={{ width: `${uploadProgress.signatureUploaded}%` }}></div>
                        </div>
                        <span className="text-[10px] text-gray-400 font-medium">Uploading {uploadProgress.signatureUploaded}%</span>
                      </div>
                    ) : (
                      <button
                        onClick={() => simulateUpload('signatureUploaded')}
                        className="w-full py-1.5 px-3 border border-indigo-600 text-indigo-600 hover:bg-indigo-50 rounded-lg text-xs font-bold transition-colors"
                      >
                        {user?.signatureUploaded ? 'Re-upload Signature' : 'Upload Signature'}
                      </button>
                    )}
                  </div>
                </div>

                {/* ID Proof Upload */}
                <div className="border border-gray-200 rounded-xl p-5 flex flex-col justify-between items-center text-center bg-gray-50/20">
                  <div className="space-y-2">
                    <h3 className="font-bold text-sm text-slate-800">Govt ID Proof</h3>
                    <p className="text-[10px] text-gray-400">Aadhaar Card / PAN / Passport / DL</p>
                  </div>
                  
                  <div className="my-6">
                    {user?.idProofUploaded ? (
                      <div className="flex flex-col items-center text-green-600 space-y-1">
                        <FileCheck className="h-10 w-10 text-green-500" />
                        <span className="text-xs font-bold">Uploaded</span>
                      </div>
                    ) : (
                      <span className="text-xs text-amber-500 font-semibold">Missing ID Proof</span>
                    )}
                  </div>

                  <div className="w-full">
                    {uploading.idProofUploaded ? (
                      <div className="w-full space-y-1">
                        <div className="w-full bg-gray-200 h-1.5 rounded-full">
                          <div className="bg-indigo-600 h-1.5 rounded-full" style={{ width: `${uploadProgress.idProofUploaded}%` }}></div>
                        </div>
                        <span className="text-[10px] text-gray-400 font-medium">Uploading {uploadProgress.idProofUploaded}%</span>
                      </div>
                    ) : (
                      <button
                        onClick={() => simulateUpload('idProofUploaded')}
                        className="w-full py-1.5 px-3 border border-indigo-600 text-indigo-600 hover:bg-indigo-50 rounded-lg text-xs font-bold transition-colors"
                      >
                        {user?.idProofUploaded ? 'Re-upload ID' : 'Upload ID Proof'}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
export default Profile;
