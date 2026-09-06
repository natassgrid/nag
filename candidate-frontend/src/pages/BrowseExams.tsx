// src/pages/BrowseExams.tsx
// Connected exam listing with multi-step application modal and instant Admit Card download.

import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Search,
  BookOpen,
  Calendar,
  Clock,
  Award,
  FileCheck2,
  Building2,
  ExternalLink,
} from 'lucide-react';
import { examService } from '../services/examService';
import { useToast } from '../components/Toast';
import type { ExaminationResponse } from '../types/api';
import { ApplyExamModal } from '../components/ApplyExamModal';
import { AdmitCardModal } from '../components/AdmitCardModal';

const BrowseExams: React.FC = () => {
  const { toast } = useToast();
  const [exams, setExams] = useState<ExaminationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [appliedIds, setAppliedIds] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Modal State
  const [selectedExamForApply, setSelectedExamForApply] = useState<ExaminationResponse | null>(null);
  const [selectedExamForAdmitCard, setSelectedExamForAdmitCard] = useState<string | null>(null);

  const fetchExams = useCallback(
    async (q: string, p: number) => {
      setLoading(true);
      try {
        const result = await examService.listPublishedExams({
          search: q || undefined,
          page: p,
          size: 12,
        });
        setExams(result.content);
        setTotalPages(result.totalPages);
      } catch {
        toast.error('Failed to load exams', 'Please refresh and try again.');
      } finally {
        setLoading(false);
      }
    },
    [toast]
  );

  const loadMyApplications = useCallback(() => {
    examService
      .getMyExams()
      .then((apps) => setAppliedIds(new Set(apps.map((a) => a.examId))))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadMyApplications();
  }, [loadMyApplications]);

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0);
      void fetchExams(search, 0);
    }, 350);
    return () => clearTimeout(timer);
  }, [search, fetchExams]);

  useEffect(() => {
    void fetchExams(search, page);
  }, [page, fetchExams, search]);

  const formatDate = (d?: string) => {
    if (!d) return 'Schedule TBA';
    return new Date(d).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Browse Examinations
          </h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Official All-India CBT examination notices and registration portal.
          </p>
        </div>

        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search exam name, authority..."
            className="w-full rounded-xl border border-slate-300 bg-white pl-9 pr-4 py-2 text-sm shadow-sm focus:border-teal-600 focus:outline-none focus:ring-1 focus:ring-teal-600 dark:bg-slate-800 dark:border-slate-700 dark:text-white"
          />
        </div>
      </div>

      {/* Exam grid */}
      {loading ? (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm animate-pulse dark:bg-slate-800 dark:border-slate-700"
            >
              <div className="h-5 bg-slate-200 rounded w-3/4 mb-3 dark:bg-slate-700" />
              <div className="h-3 bg-slate-100 rounded w-full mb-2 dark:bg-slate-700/50" />
              <div className="h-3 bg-slate-100 rounded w-2/3 mb-4 dark:bg-slate-700/50" />
              <div className="h-10 bg-slate-200 rounded-xl dark:bg-slate-700" />
            </div>
          ))}
        </div>
      ) : exams.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white py-16 text-center text-slate-400 dark:bg-slate-800/40 dark:border-slate-700">
          <BookOpen className="mx-auto mb-3 h-12 w-12 text-slate-300 dark:text-slate-600" />
          <p className="font-semibold text-slate-700 dark:text-slate-200">No examinations found</p>
          <p className="text-xs text-slate-500 mt-1 dark:text-slate-400">
            {search ? 'Try adjusting your search criteria.' : 'Check back later for new examination notifications.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {exams.map((exam) => {
            const isApplied = appliedIds.has(exam.id);

            return (
              <div
                key={exam.id}
                className="flex flex-col justify-between rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:shadow-md dark:bg-slate-800 dark:border-slate-700"
              >
                <div>
                  {/* Category & Authority Badge */}
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <span className="rounded-md bg-teal-50 px-2 py-0.5 text-[11px] font-bold text-teal-700 border border-teal-200 dark:bg-teal-950/50 dark:border-teal-900 dark:text-teal-300 uppercase tracking-wider">
                      {exam.category || 'RECRUITMENT'}
                    </span>
                    <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400">
                      {exam.examinationMode || 'CBT'}
                    </span>
                  </div>

                  <h3 className="text-sm font-bold text-slate-900 line-clamp-2 dark:text-white leading-snug">
                    {exam.title || exam.code}
                  </h3>

                  {exam.conductingAuthority && (
                    <p className="mt-1 flex items-center gap-1 text-xs text-slate-600 font-medium dark:text-slate-300">
                      <Building2 className="h-3.5 w-3.5 text-slate-400" />
                      <span>{exam.conductingAuthority}</span>
                    </p>
                  )}

                  {exam.description && (
                    <p className="mt-2 text-xs text-slate-500 line-clamp-2 dark:text-slate-400 leading-relaxed">
                      {exam.description}
                    </p>
                  )}

                  {/* Highlights Grid */}
                  <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-2.5 text-xs dark:bg-slate-900/60 border border-slate-100 dark:border-slate-800">
                    <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                      <Clock className="h-3.5 w-3.5 text-teal-600" />
                      <span>{exam.durationMinutes} Mins</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300">
                      <Award className="h-3.5 w-3.5 text-indigo-600" />
                      <span>{exam.totalMarks} Marks</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-600 dark:text-slate-300 col-span-2">
                      <Calendar className="h-3.5 w-3.5 text-amber-600" />
                      <span>Exam Date: {formatDate(exam.examDate)}</span>
                    </div>
                  </div>
                </div>

                {/* Card CTA */}
                <div className="mt-5 pt-3 border-t border-slate-100 dark:border-slate-700/60">
                  {isApplied ? (
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setSelectedExamForAdmitCard(exam.id)}
                        className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl bg-teal-50 border border-teal-300 px-3 py-2 text-xs font-bold text-teal-800 hover:bg-teal-100 dark:bg-teal-950/40 dark:border-teal-800 dark:text-teal-200 transition"
                      >
                        <FileCheck2 className="h-4 w-4" />
                        <span>Admit Card</span>
                      </button>
                      <Link
                        to={`/take-exam/${exam.id}`}
                        className="inline-flex items-center justify-center gap-1 rounded-xl bg-teal-700 px-3 py-2 text-xs font-bold text-white hover:bg-teal-800 transition"
                      >
                        <span>Start</span>
                        <ExternalLink className="h-3.5 w-3.5" />
                      </Link>
                    </div>
                  ) : (
                    <button
                      onClick={() => setSelectedExamForApply(exam)}
                      className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2.5 text-xs font-bold text-white shadow-sm hover:bg-teal-800 transition active:scale-[0.99]"
                    >
                      <span>Apply for Examination</span>
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
        <div className="flex justify-center items-center gap-2 pt-4">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-1.5 text-xs font-medium text-slate-700 disabled:opacity-40 hover:bg-slate-50 dark:bg-slate-800 dark:border-slate-700 dark:text-slate-300"
          >
            Previous
          </button>
          <span className="text-xs text-slate-600 dark:text-slate-400">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="rounded-lg border border-slate-300 bg-white px-3.5 py-1.5 text-xs font-medium text-slate-700 disabled:opacity-40 hover:bg-slate-50 dark:bg-slate-800 dark:border-slate-700 dark:text-slate-300"
          >
            Next
          </button>
        </div>
      )}

      {/* Apply Multi-Step Modal */}
      <ApplyExamModal
        exam={selectedExamForApply}
        isOpen={!!selectedExamForApply}
        onClose={() => setSelectedExamForApply(null)}
        onSuccess={() => {
          loadMyApplications();
        }}
        onViewAdmitCard={(examId) => setSelectedExamForAdmitCard(examId)}
      />

      {/* Admit Card Modal */}
      <AdmitCardModal
        examId={selectedExamForAdmitCard}
        isOpen={!!selectedExamForAdmitCard}
        onClose={() => setSelectedExamForAdmitCard(null)}
      />
    </div>
  );
};

export default BrowseExams;
