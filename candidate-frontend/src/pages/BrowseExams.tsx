// src/pages/BrowseExams.tsx
// Connected exam listing → GET /api/v1/examinations/public (paginated)
// Apply Now → POST /api/v1/examinations/{examId}/apply

import React, { useEffect, useState, useCallback } from 'react';
import { Search, BookOpen, Calendar, Clock, Award, CheckCircle, Loader } from 'lucide-react';
import { examService } from '../services/examService';
import { useToast } from '../components/Toast';
import type { ExaminationResponse } from '../types/api';

const BrowseExams: React.FC = () => {
  const { toast } = useToast();
  const [exams, setExams] = useState<ExaminationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [applying, setApplying] = useState<string | null>(null);
  const [appliedIds, setAppliedIds] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchExams = useCallback(async (q: string, p: number) => {
    setLoading(true);
    try {
      const result = await examService.listPublishedExams({ search: q || undefined, page: p, size: 12 });
      setExams(result.content);
      setTotalPages(result.totalPages);
    } catch {
      toast.error('Failed to load exams', 'Please refresh and try again.');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    // Load already-applied exams on mount
    examService.getMyExams()
      .then((apps) => setAppliedIds(new Set(apps.map((a) => a.examId))))
      .catch(() => {});
  }, []);

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      void fetchExams(search, 0);
    }, 400);
    return () => clearTimeout(timer);
  }, [search, fetchExams]);

  useEffect(() => {
    void fetchExams(search, page);
  }, [page, fetchExams, search]);

  const handleApply = async (exam: ExaminationResponse) => {
    setApplying(exam.id);
    try {
      await examService.applyForExam({ examId: exam.id });
      setAppliedIds((prev) => new Set(prev).add(exam.id));
      toast.success('Application submitted!', `You've applied for "${exam.title}".`);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string }; status?: number } };
      if (error.response?.status === 409) {
        toast.warning('Already applied', 'You have already applied for this exam.');
        setAppliedIds((prev) => new Set(prev).add(exam.id));
      } else {
        toast.error('Application failed', error.response?.data?.message ?? 'Please try again.');
      }
    } finally {
      setApplying(null);
    }
  };

  const formatDate = (d: string) =>
    new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Browse Examinations</h1>
        <p className="text-gray-500 text-sm mt-1">Find and apply for upcoming government examinations.</p>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4" />
        <input
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by exam name, department…"
          className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
        />
      </div>

      {/* Exam grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100 animate-pulse">
              <div className="h-5 bg-gray-200 rounded w-3/4 mb-3" />
              <div className="h-3 bg-gray-100 rounded w-full mb-2" />
              <div className="h-3 bg-gray-100 rounded w-2/3 mb-4" />
              <div className="h-9 bg-gray-200 rounded-lg" />
            </div>
          ))}
        </div>
      ) : exams.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <BookOpen className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="font-medium text-gray-500">No examinations found</p>
          <p className="text-sm">
            {search ? 'Try a different search term.' : 'Check back later for new exam announcements.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {exams.map((exam) => {
            const isApplied = appliedIds.has(exam.id);
            const isApplying = applying === exam.id;
            const appOpen = new Date(exam.applicationEndDate) > new Date();

            return (
              <div key={exam.id} className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100 flex flex-col">
                {/* Header */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div className="bg-indigo-100 p-2 rounded-lg shrink-0">
                    <BookOpen className="w-5 h-5 text-indigo-600" />
                  </div>
                  <span className={`text-xs px-2 py-1 rounded-full font-medium ${
                    exam.status === 'PUBLISHED' ? 'bg-green-100 text-green-700' :
                    exam.status === 'ACTIVE' ? 'bg-blue-100 text-blue-700' :
                    'bg-gray-100 text-gray-600'
                  }`}>
                    {exam.status}
                  </span>
                </div>

                <h3 className="font-semibold text-gray-800 text-sm mb-1 line-clamp-2">{exam.title}</h3>
                <p className="text-xs text-gray-500 line-clamp-2 mb-3">{exam.description}</p>

                {/* Details */}
                <div className="space-y-1.5 text-xs text-gray-500 mb-4">
                  <p className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-indigo-400" />
                    Exam: {formatDate(exam.examDate)}
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-indigo-400" />
                    Duration: {exam.durationMinutes} min
                  </p>
                  <p className="flex items-center gap-1.5">
                    <Award className="w-3.5 h-3.5 text-indigo-400" />
                    Total Marks: {exam.totalMarks}
                  </p>
                  {exam.applicationFee > 0 && (
                    <p className="flex items-center gap-1.5">
                      <span className="text-indigo-400 font-bold">₹</span>
                      Application Fee: ₹{exam.applicationFee}
                    </p>
                  )}
                </div>

                {/* Apply deadline */}
                {appOpen && (
                  <p className="text-xs text-amber-600 bg-amber-50 rounded-lg px-2 py-1 mb-3 text-center">
                    Apply by: {formatDate(exam.applicationEndDate)}
                  </p>
                )}

                {/* CTA */}
                <div className="mt-auto">
                  {isApplied ? (
                    <div className="flex items-center justify-center gap-2 w-full py-2.5 bg-green-50 text-green-700 rounded-lg text-sm font-medium border border-green-200">
                      <CheckCircle className="w-4 h-4" /> Applied
                    </div>
                  ) : !appOpen ? (
                    <div className="w-full py-2.5 bg-gray-100 text-gray-400 rounded-lg text-sm font-medium text-center">
                      Applications Closed
                    </div>
                  ) : (
                    <button
                      onClick={() => void handleApply(exam)}
                      disabled={isApplying}
                      className="w-full flex items-center justify-center gap-2 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white rounded-lg text-sm font-medium transition"
                    >
                      {isApplying ? <Loader className="w-4 h-4 animate-spin" /> : null}
                      {isApplying ? 'Applying…' : 'Apply Now'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 border border-gray-300 rounded-lg text-sm disabled:opacity-40 hover:bg-gray-50"
          >
            Previous
          </button>
          <span className="px-4 py-2 text-sm text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 border border-gray-300 rounded-lg text-sm disabled:opacity-40 hover:bg-gray-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default BrowseExams;
