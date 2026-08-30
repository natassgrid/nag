import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  Calendar, 
  CheckCircle2, 
  AlertTriangle, 
  ArrowRight, 
  BookOpen, 
  Award, 
  BellRing,
  HelpCircle,
  FileText
} from 'lucide-react';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();

  // Mock Announcements
  const announcements = [
    { id: 1, title: 'Admit Card released for National Entrance Exam 2026', date: 'August 28, 2026', priority: 'high' },
    { id: 2, title: 'Exam Guidelines: Do not carry electronic items to CBT centers', date: 'August 25, 2026', priority: 'medium' },
    { id: 3, title: 'Helpdesk available 24/7 for Technical support during Take Exam phase', date: 'August 20, 2026', priority: 'low' },
  ];

  // Calculate profile completeness
  const calculateCompleteness = () => {
    if (!user) return 0;
    let score = 0;
    if (user.name) score += 10;
    if (user.email) score += 10;
    if (user.mobile) score += 10;
    if (user.dob) score += 10;
    if (user.gender) score += 10;
    if (user.address) score += 10;
    if (user.qualification) score += 15;
    if (user.photoUploaded) score += 10;
    if (user.signatureUploaded) score += 10;
    if (user.idProofUploaded) score += 5;
    return score;
  };

  const profileScore = calculateCompleteness();

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl shadow-xl p-6 md:p-8 text-white relative overflow-hidden border border-slate-800">
        <div className="relative z-10 max-w-2xl">
          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
            Welcome back, {user?.name}!
          </h1>
          <p className="mt-2 text-indigo-200 text-sm md:text-base leading-relaxed">
            Monitor your exam registrations, verify your application profiles, complete pending documents, and access your test interfaces securely here.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-green-950/60 text-green-300 border border-green-500/30">
              <CheckCircle2 className="h-3.5 w-3.5 mr-1" /> Verified Candidate Account
            </span>
            <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-indigo-950/60 text-indigo-300 border border-indigo-500/30">
              ID: NAG-2026-{user?.mobile ? user.mobile.slice(-4) : '0000'}
            </span>
          </div>
        </div>
        <div className="absolute right-0 bottom-0 opacity-10 transform translate-x-10 translate-y-10 scale-150 hidden md:block">
          <BookOpen className="h-48 w-48 text-indigo-400" />
        </div>
      </div>

      {/* Warning/Alerts */}
      {profileScore < 85 && (
        <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-lg shadow-sm">
          <div className="flex">
            <AlertTriangle className="h-5 w-5 text-amber-600 flex-shrink-0" />
            <div className="ml-3 flex-1 md:flex md:justify-between items-center">
              <div>
                <p className="text-sm font-semibold text-amber-800">
                  Your registration profile is incomplete ({profileScore}% complete)
                </p>
                <p className="text-xs text-amber-700 mt-0.5">
                  Complete your details and upload required documents to qualify for upcoming schedules.
                </p>
              </div>
              <p className="mt-3 md:mt-0 text-sm">
                <Link
                  to="/profile"
                  className="font-bold text-amber-800 hover:text-amber-900 inline-flex items-center"
                >
                  Edit Profile <ArrowRight className="ml-1 h-4 w-4" />
                </Link>
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Grid: Main Info */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Registered & Completed Exams */}
        <div className="lg:col-span-2 space-y-6">
          {/* Registered Exams */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="px-5 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
              <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider flex items-center">
                <BookOpen className="h-5 w-5 mr-2 text-indigo-600" />
                Registered Examinations
              </h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-100 text-indigo-800">
                {user?.registeredExams.length} Active
              </span>
            </div>
            
            <div className="divide-y divide-gray-100">
              {user && user.registeredExams.length > 0 ? (
                user.registeredExams.map((examId) => {
                  // In real app, load details. Here we mock
                  const examDetails = {
                    id: examId,
                    name: examId === 'EXAM001' ? 'National Entrance Examination (Graduate) 2026' : 'AI & Machine Learning Scholarship Test',
                    date: 'September 15, 2026',
                    time: '10:00 AM - 01:00 PM',
                    duration: '180 mins',
                    status: 'Admit Card Generated'
                  };
                  return (
                    <div key={examId} className="p-5 flex flex-col sm:flex-row justify-between sm:items-center hover:bg-slate-50/40 transition-colors">
                      <div className="space-y-1">
                        <div className="flex items-center space-x-2">
                          <span className="px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 text-xs font-mono font-bold">
                            {examDetails.id}
                          </span>
                          <span className="text-xs font-semibold text-green-700 bg-green-50 px-2 py-0.5 rounded">
                            {examDetails.status}
                          </span>
                        </div>
                        <h3 className="text-base font-bold text-slate-800">{examDetails.name}</h3>
                        <div className="flex flex-wrap text-xs text-gray-500 gap-x-4 gap-y-1">
                          <span className="flex items-center"><Calendar className="h-3.5 w-3.5 mr-1" /> {examDetails.date}</span>
                          <span>Duration: {examDetails.duration}</span>
                        </div>
                      </div>
                      <div className="mt-4 sm:mt-0 flex space-x-2">
                        <Link
                          to={`/take-exam/${examDetails.id}`}
                          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-lg shadow-sm transition-colors text-center inline-flex items-center"
                        >
                          Take Test Screen
                        </Link>
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="p-8 text-center">
                  <p className="text-gray-500 text-sm">You haven't registered for any exams yet.</p>
                  <Link to="/exams" className="text-indigo-600 font-semibold text-sm hover:underline mt-2 inline-block">
                    Browse and Apply for Exams &rarr;
                  </Link>
                </div>
              )}
            </div>
          </div>

          {/* Completed Exams */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            <div className="px-5 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
              <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider flex items-center">
                <Award className="h-5 w-5 mr-2 text-indigo-600" />
                Completed Assessments
              </h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-green-100 text-green-800">
                {user?.completedExams.length} Complete
              </span>
            </div>
            
            <div className="divide-y divide-gray-100">
              {user && user.completedExams.length > 0 ? (
                user.completedExams.map((exam) => (
                  <div key={exam.examId} className="p-5 flex justify-between items-center hover:bg-slate-50/40 transition-colors">
                    <div className="space-y-1">
                      <h3 className="text-sm font-bold text-slate-800">{exam.examName}</h3>
                      <p className="text-xs text-gray-500">Taken on {exam.date}</p>
                    </div>
                    <div className="flex items-center space-x-4">
                      <div className="text-right">
                        <p className="text-sm font-extrabold text-indigo-600">{exam.percentile}%ile</p>
                        <p className="text-[10px] text-gray-400">Score: {exam.score}/{exam.totalQuestions}</p>
                      </div>
                      <Link
                        to="/results"
                        className="p-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg transition-colors"
                        title="View Report Card"
                      >
                        <FileText className="h-4.5 w-4.5" />
                      </Link>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-8 text-center text-gray-500 text-sm">
                  No completed exams on record.
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Notices / Board and Helpdesk */}
        <div className="space-y-6">
          {/* Announcement Board */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5 space-y-4">
            <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider flex items-center pb-2 border-b border-gray-100">
              <BellRing className="h-5 w-5 mr-2 text-indigo-600" />
              Important Notices
            </h2>
            <div className="space-y-4">
              {announcements.map((ann) => (
                <div key={ann.id} className="text-sm group border-b border-gray-50 pb-3 last:border-0 last:pb-0">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-semibold text-gray-400">{ann.date}</span>
                    <span className={`h-1.5 w-1.5 rounded-full ${
                      ann.priority === 'high' ? 'bg-red-500' : ann.priority === 'medium' ? 'bg-amber-500' : 'bg-slate-400'
                    }`}></span>
                  </div>
                  <h4 className="font-bold text-slate-700 mt-1 hover:text-indigo-600 cursor-pointer transition-colors leading-tight">
                    {ann.title}
                  </h4>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Support / Center Desk */}
          <div className="bg-gradient-to-br from-indigo-50 to-indigo-100 rounded-xl shadow-sm border border-indigo-200/50 p-5 space-y-4">
            <h3 className="text-sm font-bold text-indigo-900 uppercase tracking-wider flex items-center">
              <HelpCircle className="h-5 w-5 mr-2 text-indigo-600" />
              NAG Helpdesk Desk
            </h3>
            <p className="text-xs text-indigo-800 leading-relaxed">
              Facing difficulties during registration or uploading documents? Contact candidate cell:
            </p>
            <div className="text-xs space-y-1.5 text-indigo-900 bg-white/60 p-3 rounded-lg border border-indigo-200/40">
              <p><strong>Email:</strong> candidate.support@nag.gov.in</p>
              <p><strong>Toll Free:</strong> 1800-345-NAG (624)</p>
              <p><strong>Timings:</strong> 09:00 AM - 06:00 PM IST</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
export default Dashboard;
