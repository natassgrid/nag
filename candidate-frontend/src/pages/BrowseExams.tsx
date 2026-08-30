import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Search, Calendar, Check, AlertCircle } from 'lucide-react';

interface MockExam {
  id: string;
  name: string;
  code: string;
  eligibility: string;
  date: string;
  fee: string;
  duration: string;
  status: 'open' | 'closed' | 'upcoming';
}

const AVAILABLE_EXAMS: MockExam[] = [
  {
    id: 'EXAM001',
    name: 'National Entrance Examination (Graduate) 2026',
    code: 'NEE-G26',
    eligibility: 'Bachelor\'s Degree in any discipline',
    date: 'September 15, 2026',
    fee: '₹1000',
    duration: '180 mins',
    status: 'open'
  },
  {
    id: 'EXAM002',
    name: 'AI & Machine Learning Scholarship Test 2026',
    code: 'AIMLST-26',
    eligibility: 'Pursuing graduation or equivalent computer background',
    date: 'October 05, 2026',
    fee: 'Free',
    duration: '90 mins',
    status: 'open'
  },
  {
    id: 'EXAM003',
    name: 'Civil Services Preliminary Screening 2026',
    code: 'CSPS-26',
    eligibility: 'Graduate in any discipline',
    date: 'November 20, 2026',
    fee: '₹200',
    duration: '120 mins',
    status: 'open'
  },
  {
    id: 'EXAM004',
    name: 'Central Recruitment Officer Grade-A Assessment',
    code: 'CROA-26',
    eligibility: 'Post-graduation with 2 years experience',
    date: 'Closed',
    fee: '₹500',
    duration: '150 mins',
    status: 'closed'
  }
];

export const BrowseExams: React.FC = () => {
  const { user, applyForExam } = useAuth();
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | 'applied' | 'not-applied'>('all');
  
  const [alert, setAlert] = useState<{ examId: string; type: 'success'; text: string } | null>(null);

  const handleApply = (examId: string, examName: string) => {
    applyForExam(examId);
    setAlert({ examId, type: 'success', text: `You have successfully applied for ${examName}.` });
    setTimeout(() => setAlert(null), 4000);
  };

  const filteredExams = AVAILABLE_EXAMS.filter((exam) => {
    const matchesSearch = exam.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          exam.code.toLowerCase().includes(searchTerm.toLowerCase());
    
    const isApplied = user?.registeredExams.includes(exam.id) || false;
    
    if (filterStatus === 'applied') return matchesSearch && isApplied;
    if (filterStatus === 'not-applied') return matchesSearch && !isApplied;
    return matchesSearch;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-800">Browse Examinations</h1>
        <p className="text-sm text-slate-500">Explore open entrance and recruitment tests and register.</p>
      </div>

      {/* Filter / Search Bar */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-200 flex flex-col md:flex-row gap-4 justify-between items-center">
        {/* Search */}
        <div className="relative w-full md:w-96">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search className="h-4.5 w-4.5 text-gray-400" />
          </div>
          <input
            type="text"
            placeholder="Search by exam name or code..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm text-slate-900 focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 text-sm"
          />
        </div>

        {/* Filter status buttons */}
        <div className="flex space-x-2 w-full md:w-auto">
          {(['all', 'applied', 'not-applied'] as const).map((status) => (
            <button
              key={status}
              onClick={() => setFilterStatus(status)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold uppercase tracking-wider transition-colors flex-1 md:flex-none ${
                filterStatus === status
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 hover:bg-gray-200 text-gray-700'
              }`}
            >
              {status === 'all' ? 'All Exams' : status === 'applied' ? 'Applied Only' : 'Not Applied'}
            </button>
          ))}
        </div>
      </div>

      {/* Exam Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {filteredExams.map((exam) => {
          const isApplied = user?.registeredExams.includes(exam.id) || false;
          const isTaken = user?.completedExams.some((c) => c.examId === exam.id) || false;
          
          return (
            <div key={exam.id} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 flex flex-col justify-between space-y-4 hover:shadow-md transition-shadow relative">
              {alert?.examId === exam.id && (
                <div className="absolute top-2 left-2 right-2 bg-green-50 text-green-800 text-xs font-semibold p-2.5 rounded-lg border border-green-200 flex items-center z-10 animate-fade-in shadow">
                  <Check className="h-4 w-4 mr-2 text-green-600 flex-shrink-0" />
                  {alert.text}
                </div>
              )}

              <div className="space-y-2">
                <div className="flex justify-between items-start">
                  <span className="px-2.5 py-0.5 rounded text-xs font-bold font-mono bg-indigo-50 text-indigo-700 border border-indigo-100">
                    {exam.code}
                  </span>
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wide ${
                    exam.status === 'open' 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-red-100 text-red-800'
                  }`}>
                    {exam.status === 'open' ? 'Registration Open' : 'Closed'}
                  </span>
                </div>
                <h3 className="text-base font-extrabold text-slate-800 leading-snug">{exam.name}</h3>
                
                <div className="pt-2 text-xs space-y-1.5 text-gray-600">
                  <p><strong>Eligibility:</strong> {exam.eligibility}</p>
                  <p className="flex items-center"><Calendar className="h-3.5 w-3.5 mr-1 text-gray-400" /> <strong>Exam Date:</strong> {exam.date}</p>
                  <div className="flex space-x-4 pt-1 border-t border-gray-50 mt-2">
                    <p><strong>Duration:</strong> {exam.duration}</p>
                    <p><strong>Application Fee:</strong> {exam.fee}</p>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
                {isTaken ? (
                  <span className="text-xs font-bold text-slate-500 bg-slate-100 px-3 py-1.5 rounded-lg w-full text-center">
                    Assessment Completed
                  </span>
                ) : isApplied ? (
                  <span className="text-xs font-bold text-green-700 bg-green-50 border border-green-200 px-3 py-1.5 rounded-lg w-full text-center flex items-center justify-center">
                    <Check className="h-4 w-4 mr-1 text-green-600" /> Applied & Registered
                  </span>
                ) : exam.status === 'closed' ? (
                  <span className="text-xs font-bold text-red-700 bg-red-50 border border-red-200 px-3 py-1.5 rounded-lg w-full text-center flex items-center justify-center">
                    <AlertCircle className="h-4 w-4 mr-1" /> Closed
                  </span>
                ) : (
                  <button
                    onClick={() => handleApply(exam.id, exam.name)}
                    className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold transition-colors shadow-sm"
                  >
                    Apply Now
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
export default BrowseExams;
