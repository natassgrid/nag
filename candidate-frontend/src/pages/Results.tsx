import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Award, Download, Calendar, ShieldCheck, CheckCircle2, X } from 'lucide-react';

export const Results: React.FC = () => {
  const { user } = useAuth();
  const [selectedExamId, setSelectedExamId] = useState<string | null>(
    user?.completedExams && user.completedExams.length > 0 ? user.completedExams[0].examId : null
  );
  
  const [showCertificate, setShowCertificate] = useState(false);

  const activeExam = user?.completedExams.find(e => e.examId === selectedExamId);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-800">Exam Results</h1>
        <p className="text-sm text-slate-500">Access your assessment report cards, section analyses, and certificates.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Side: Exam List */}
        <div className="space-y-4">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5 space-y-4">
            <h2 className="text-xs font-bold text-slate-800 uppercase tracking-wider pb-2 border-b border-gray-100">
              Completed Assessments
            </h2>

            <div className="space-y-2">
              {user && user.completedExams.length > 0 ? (
                user.completedExams.map((exam) => {
                  const isActive = selectedExamId === exam.examId;
                  return (
                    <button
                      key={exam.examId}
                      onClick={() => setSelectedExamId(exam.examId)}
                      className={`w-full text-left p-4 rounded-xl border transition-all ${
                        isActive
                          ? 'border-indigo-600 bg-indigo-50/30'
                          : 'border-gray-200 hover:bg-slate-50'
                      }`}
                    >
                      <div className="flex justify-between items-start">
                        <span className="text-[10px] font-bold font-mono text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                          {exam.examId}
                        </span>
                        <span className="text-[10px] text-gray-400 font-medium flex items-center">
                          <Calendar className="h-3 w-3 mr-1" /> {exam.date}
                        </span>
                      </div>
                      <h3 className={`text-sm font-bold mt-2 truncate ${isActive ? 'text-indigo-900' : 'text-slate-800'}`}>
                        {exam.examName}
                      </h3>
                      <p className="text-xs text-gray-500 mt-1 font-semibold">Percentile: {exam.percentile}%</p>
                    </button>
                  );
                })
              ) : (
                <p className="text-sm text-gray-500 text-center py-4">No exams completed yet.</p>
              )}
            </div>
          </div>
        </div>

        {/* Right Side: Scorecard Detail */}
        <div className="lg:col-span-2">
          {activeExam ? (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
              {/* Exam Title Block */}
              <div className="flex flex-col sm:flex-row justify-between sm:items-center border-b border-gray-100 pb-5 gap-4">
                <div>
                  <span className="text-xs font-bold text-indigo-600 font-mono uppercase bg-indigo-50 px-2.5 py-1 rounded">
                    Scorecard: {activeExam.examId}
                  </span>
                  <h2 className="text-lg font-extrabold text-slate-800 mt-2">{activeExam.examName}</h2>
                  <p className="text-xs text-gray-500 mt-1">Conducted on {activeExam.date} • Computer Based Test</p>
                </div>
                
                <button
                  onClick={() => setShowCertificate(true)}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold flex items-center justify-center transition-colors shadow-sm"
                >
                  <Award className="h-4 w-4 mr-2" /> View Certificate
                </button>
              </div>

              {/* KPI Score Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="bg-gradient-to-br from-indigo-50/50 to-indigo-100/30 rounded-xl p-4 border border-indigo-100/60 flex flex-col justify-between">
                  <span className="text-xs font-semibold text-indigo-700 uppercase tracking-wide">National Rank</span>
                  <span className="text-2xl font-black text-indigo-900 mt-2">#{activeExam.rank}</span>
                  <span className="text-[10px] text-indigo-600 mt-1">All India Category</span>
                </div>
                <div className="bg-gradient-to-br from-emerald-50/50 to-emerald-100/30 rounded-xl p-4 border border-emerald-100/60 flex flex-col justify-between">
                  <span className="text-xs font-semibold text-emerald-700 uppercase tracking-wide">Percentile Score</span>
                  <span className="text-2xl font-black text-emerald-900 mt-2">{activeExam.percentile}%</span>
                  <span className="text-[10px] text-emerald-600 mt-1">Normalised Cumulative</span>
                </div>
                <div className="bg-gradient-to-br from-slate-50 to-slate-100 rounded-xl p-4 border border-slate-200/60 flex flex-col justify-between">
                  <span className="text-xs font-semibold text-slate-600 uppercase tracking-wide">Aggregate Mark</span>
                  <span className="text-2xl font-black text-slate-800 mt-2">{activeExam.score} / {activeExam.totalQuestions}</span>
                  <span className="text-[10px] text-slate-500 mt-1">Accuracy: {((activeExam.score / activeExam.totalQuestions) * 100).toFixed(0)}%</span>
                </div>
              </div>

              {/* Subject Breakdown Details */}
              <div className="space-y-4 pt-2">
                <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider pb-2 border-b border-gray-100">
                  Section-wise Performance
                </h3>

                <div className="space-y-4">
                  {/* Verbal */}
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs font-bold text-slate-700">
                      <span>Quantitative Aptitude</span>
                      <span>85% Accuracy</span>
                    </div>
                    <div className="w-full bg-gray-150 h-2.5 rounded-full bg-gray-100">
                      <div className="bg-indigo-600 h-2.5 rounded-full" style={{ width: '85%' }}></div>
                    </div>
                  </div>

                  {/* Logical */}
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs font-bold text-slate-700">
                      <span>Logical Reasoning</span>
                      <span>72% Accuracy</span>
                    </div>
                    <div className="w-full bg-gray-150 h-2.5 rounded-full bg-gray-100">
                      <div className="bg-indigo-500 h-2.5 rounded-full" style={{ width: '72%' }}></div>
                    </div>
                  </div>

                  {/* Technical */}
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs font-bold text-slate-700">
                      <span>Computer Science Concepts</span>
                      <span>90% Accuracy</span>
                    </div>
                    <div className="w-full bg-gray-150 h-2.5 rounded-full bg-gray-100">
                      <div className="bg-emerald-600 h-2.5 rounded-full" style={{ width: '90%' }}></div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Secure verification alert */}
              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl flex items-center space-x-3 text-xs text-slate-600">
                <ShieldCheck className="h-5 w-5 text-indigo-600 flex-shrink-0" />
                <p>
                  This scorecard is cryptographically signed by the **Next-generation Assessment Grid (NAG) Board**. Verifiers can query transaction hash `0x7ae...c69` on the state validation node for authenticity.
                </p>
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center text-gray-500">
              Please select a completed assessment from the left side panel to view detailed report card.
            </div>
          )}
        </div>
      </div>

      {/* Certificate Modal */}
      {showCertificate && activeExam && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full overflow-hidden border border-gray-200 relative flex flex-col">
            
            {/* Header */}
            <div className="p-4 border-b border-gray-100 flex justify-between items-center bg-indigo-50/50">
              <span className="text-xs font-bold text-indigo-800 flex items-center">
                <Award className="h-4.5 w-4.5 mr-1 text-indigo-600" /> Certificate Viewer
              </span>
              <button
                onClick={() => setShowCertificate(false)}
                className="text-gray-500 hover:text-gray-700 focus:outline-none"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Certificate Body (Prettified printable grid) */}
            <div className="p-6 md:p-10 flex-1 overflow-y-auto">
              <div className="border-[6px] border-double border-indigo-900/60 p-6 md:p-8 text-center space-y-6 bg-slate-50/50 relative rounded-lg">
                
                {/* Background Watermark Symbol */}
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none opacity-5">
                  <Award className="h-80 w-80 text-indigo-900" />
                </div>

                <div className="space-y-1.5 relative">
                  <span className="text-[10px] font-bold tracking-[0.25em] text-indigo-800">
                    NEXT-GENERATION ASSESSMENT GRID (NAG)
                  </span>
                  <h3 className="text-xl md:text-2xl font-black text-indigo-950">
                    CERTIFICATE OF MERIT
                  </h3>
                  <div className="w-24 h-0.5 bg-indigo-600 mx-auto mt-2"></div>
                </div>

                <p className="text-xs md:text-sm text-slate-600 font-medium">
                  This document serves to certify that candidate
                </p>

                <h4 className="text-lg md:text-xl font-bold uppercase tracking-wider text-slate-800 my-2">
                  {user?.name || 'Jane Doe'}
                </h4>

                <p className="text-xs md:text-sm text-slate-600 leading-relaxed max-w-lg mx-auto">
                  has successfully completed the online computer-based evaluation for <strong className="text-slate-800">{activeExam.examName}</strong> on <span className="font-semibold text-slate-800">{activeExam.date}</span>.
                </p>

                <p className="text-xs md:text-sm text-slate-600">
                  By securing a normalized percentile score of <strong className="text-indigo-700 text-base">{activeExam.percentile}%</strong>, the candidate is hereby declared qualified under Rank status <strong className="text-slate-800">#{activeExam.rank}</strong>.
                </p>

                {/* Footer Signatures */}
                <div className="pt-6 grid grid-cols-2 gap-4 border-t border-slate-200/80">
                  <div className="text-left space-y-1">
                    <p className="text-[10px] text-slate-400">Date Generated:</p>
                    <p className="text-xs font-bold text-slate-700">{new Date().toLocaleDateString()}</p>
                  </div>
                  <div className="text-right space-y-1">
                    <div className="inline-flex items-center text-xs font-bold text-green-700 bg-green-50 px-2 py-0.5 rounded border border-green-200">
                      <CheckCircle2 className="h-3 w-3 mr-1 text-green-600" /> Cryptographically Verified
                    </div>
                    <p className="text-[9px] text-gray-400 font-mono">NAG-VERIFY-ID: {activeExam.examId}-{activeExam.rank}</p>
                  </div>
                </div>

              </div>
            </div>

            {/* Actions Footer */}
            <div className="p-4 border-t border-gray-150 bg-gray-50 flex justify-end space-x-2">
              <button
                onClick={() => setShowCertificate(false)}
                className="px-4 py-2 border border-gray-300 hover:bg-gray-100 rounded-lg text-xs font-bold text-gray-700"
              >
                Close View
              </button>
              <button
                onClick={() => window.print()}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold flex items-center shadow-sm"
              >
                <Download className="h-4 w-4 mr-2" /> Print / Save PDF
              </button>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};
export default Results;
