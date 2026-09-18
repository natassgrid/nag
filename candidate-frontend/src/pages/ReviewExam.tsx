// src/pages/ReviewExam.tsx
// Issue #101: Post-Exam Review Screen
// Detailed per-question analysis: answer correctness, time spent, peer accuracy, explanations.
// Full keyboard navigation, filter by status / section, question palette.

import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  CheckCircle,
  XCircle,
  Clock,
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Filter,
  Layers,
  BookOpen,
  Award,
} from 'lucide-react';
import { resultService } from '../services/resultService';
import { MathRenderer } from '../components/MathRenderer';
import { useToast } from '../components/Toast';
import type { ExamReviewResponse, ReviewQuestion } from '../types/api';

type FilterStatus = 'ALL' | 'CORRECT' | 'INCORRECT' | 'UNATTEMPTED';

export const ReviewExam: React.FC = () => {
  const { examId = '' } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [review, setReview] = useState<ExamReviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [statusFilter, setStatusFilter] = useState<FilterStatus>('ALL');
  const [subjectFilter, setSubjectFilter] = useState<string>('ALL');

  useEffect(() => {
    if (!examId) return;
    setLoading(true);
    resultService
      .getExamReview(examId)
      .then((data: ExamReviewResponse) => {
        setReview(data);
      })
      .catch((err: any) => {
        toast.error('Failed to load review', err?.message || 'Could not fetch review data.');
      })
      .finally(() => setLoading(false));
  }, [examId, toast]);

  const questions: ReviewQuestion[] = review?.questions ?? [];

  // Unique subjects for filter
  const subjects = Array.from(new Set(questions.map((q) => q.subject).filter(Boolean)));

  // Filtered question list
  const filteredQuestions = questions.filter((q) => {
    if (statusFilter === 'CORRECT' && !q.isCorrect) return false;
    if (statusFilter === 'INCORRECT' && (q.isCorrect || q.candidateSelectedOptionIds.length === 0)) return false;
    if (statusFilter === 'UNATTEMPTED' && q.candidateSelectedOptionIds.length > 0) return false;
    if (subjectFilter !== 'ALL' && q.subject !== subjectFilter) return false;
    return true;
  });

  const currentQuestion: ReviewQuestion | undefined = filteredQuestions[currentIndex];

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'ArrowRight' || e.key === 'n') {
        setCurrentIndex((prev) => Math.min(filteredQuestions.length - 1, prev + 1));
      } else if (e.key === 'ArrowLeft' || e.key === 'p') {
        setCurrentIndex((prev) => Math.max(0, prev - 1));
      }
    },
    [filteredQuestions.length]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Reset current index when filter changes
  const handleFilterChange = (filter: FilterStatus) => {
    setStatusFilter(filter);
    setCurrentIndex(0);
  };

  const handleSubjectChange = (subj: string) => {
    setSubjectFilter(subj);
    setCurrentIndex(0);
  };

  const formatTime = (ms: number) => {
    const totalSecs = Math.round(ms / 1000);
    const m = Math.floor(totalSecs / 60);
    const s = totalSecs % 60;
    if (m === 0) return `${s}s`;
    return `${m}m ${s}s`;
  };

  // Summary counts
  const correctCount = questions.filter((q) => q.isCorrect).length;
  const incorrectCount = questions.filter((q) => !q.isCorrect && q.candidateSelectedOptionIds.length > 0).length;
  const unattemptedCount = questions.filter((q) => q.candidateSelectedOptionIds.length === 0).length;

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-4 border-teal-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-gray-500">Loading exam review...</p>
        </div>
      </div>
    );
  }

  if (!review || questions.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow p-8 max-w-md w-full text-center">
          <BookOpen className="w-12 h-12 text-gray-400 mx-auto mb-3" />
          <h2 className="text-lg font-bold text-gray-900 mb-1">No Review Data Available</h2>
          <p className="text-sm text-gray-500 mb-6">
            Review is available after the exam results are published.
          </p>
          <button
            onClick={() => navigate('/results')}
            className="w-full py-2.5 bg-teal-600 text-white font-medium rounded-xl hover:bg-teal-700 transition"
          >
            Back to Results
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Top navbar */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-10 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/results')}
            className="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 transition"
            aria-label="Back to results"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-base font-bold text-gray-900 leading-tight">Exam Review & Analysis</h1>
            <p className="text-xs text-gray-500">
              {correctCount} Correct &bull; {incorrectCount} Incorrect &bull; {unattemptedCount} Unattempted
            </p>
          </div>
        </div>

        {/* Quick summary badges */}
        <div className="hidden sm:flex items-center gap-2 text-xs">
          <span className="px-2.5 py-1 bg-green-50 text-green-700 font-semibold rounded-full border border-green-200">
            {correctCount} / {questions.length} Correct
          </span>
          <span className="px-2.5 py-1 bg-teal-50 text-teal-700 font-semibold rounded-full border border-teal-200">
            {((correctCount / questions.length) * 100).toFixed(0)}% Accuracy
          </span>
        </div>
      </header>

      {/* Main container */}
      <div className="flex-1 max-w-7xl w-full mx-auto p-4 grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* Left / Main Question Area (3 cols) */}
        <div className="lg:col-span-3 flex flex-col gap-4">
          {/* Filters bar */}
          <div className="bg-white rounded-xl border border-gray-200 p-3 flex flex-wrap items-center gap-2">
            <Filter className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-semibold text-gray-500 mr-1">Status:</span>
            {(['ALL', 'CORRECT', 'INCORRECT', 'UNATTEMPTED'] as FilterStatus[]).map((f) => (
              <button
                key={f}
                onClick={() => handleFilterChange(f)}
                className={`text-xs px-2.5 py-1 rounded-lg font-medium transition ${
                  statusFilter === f
                    ? 'bg-teal-600 text-white'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {f.charAt(0) + f.slice(1).toLowerCase()}
                {f === 'CORRECT' && ` (${correctCount})`}
                {f === 'INCORRECT' && ` (${incorrectCount})`}
                {f === 'UNATTEMPTED' && ` (${unattemptedCount})`}
              </button>
            ))}

            {subjects.length > 1 && (
              <>
                <span className="text-gray-300 mx-1">|</span>
                <span className="text-xs font-semibold text-gray-500">Subject:</span>
                <select
                  value={subjectFilter}
                  onChange={(e) => handleSubjectChange(e.target.value)}
                  className="text-xs border border-gray-200 rounded-lg px-2 py-1 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-teal-500"
                >
                  <option value="ALL">All Subjects</option>
                  {subjects.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </>
            )}
          </div>

          {/* Question Card */}
          {currentQuestion ? (
            <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
              {/* Question metadata header */}
              <div className="px-6 py-4 bg-gray-50 border-b border-gray-200 flex items-center justify-between flex-wrap gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-gray-600 bg-white border border-gray-200 px-2 py-0.5 rounded-md">
                    {currentQuestion.subject}
                  </span>
                  {currentQuestion.topic && (
                    <span className="text-xs text-gray-500">&bull; {currentQuestion.topic}</span>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span
                    className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                      currentQuestion.difficulty === 'EASY'
                        ? 'bg-emerald-50 text-emerald-700'
                        : currentQuestion.difficulty === 'HARD'
                        ? 'bg-rose-50 text-rose-700'
                        : 'bg-amber-50 text-amber-700'
                    }`}
                  >
                    {currentQuestion.difficulty}
                  </span>
                  {currentQuestion.bloomsLevel && (
                    <span className="text-xs text-gray-400">{currentQuestion.bloomsLevel}</span>
                  )}
                  {/* Result badge */}
                  {currentQuestion.candidateSelectedOptionIds.length === 0 ? (
                    <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full">Unattempted</span>
                  ) : currentQuestion.isCorrect ? (
                    <span className="text-xs bg-green-50 text-green-700 px-2 py-0.5 rounded-full flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" /> Correct
                    </span>
                  ) : (
                    <span className="text-xs bg-red-50 text-red-700 px-2 py-0.5 rounded-full flex items-center gap-1">
                      <XCircle className="w-3 h-3" /> Incorrect
                    </span>
                  )}
                </div>
              </div>

              {/* Question content */}
              <div className="px-6 py-5">
                <p className="text-sm font-medium text-gray-500 mb-2">
                  Q{currentQuestion.questionNumber}.
                </p>
                <div className="text-gray-800 text-base leading-relaxed mb-5">
                  <MathRenderer content={currentQuestion.content} />
                </div>

                {currentQuestion.imageUrl && (
                  <div className="mb-5">
                    <img
                      src={currentQuestion.imageUrl}
                      alt={currentQuestion.imageAltText || `Question ${currentQuestion.questionNumber} Figure`}
                      className="max-h-60 rounded-lg border object-contain bg-white"
                    />
                  </div>
                )}

                {/* Options */}
                <div className="space-y-2">
                  {currentQuestion.options.map((opt, optDisplayIdx) => {
                    const wasSelected = currentQuestion.candidateSelectedOptionIds.includes(opt.id);
                    const isCorrectOpt = opt.isCorrect;

                    let bg = 'bg-gray-50 border-gray-200';
                    let icon = null;
                    if (isCorrectOpt) {
                      bg = 'bg-green-50 border-green-300';
                      icon = <CheckCircle className="w-4 h-4 text-green-600 flex-shrink-0" />;
                    } else if (wasSelected && !isCorrectOpt) {
                      bg = 'bg-red-50 border-red-300';
                      icon = <XCircle className="w-4 h-4 text-red-500 flex-shrink-0" />;
                    }

                    return (
                      <div
                        key={opt.id}
                        className={`flex flex-col gap-2 px-4 py-3 rounded-xl border ${bg} transition`}
                      >
                        <div className="flex items-start gap-3">
                          <span className="text-xs font-bold text-gray-500 mt-0.5 w-4 flex-shrink-0">
                            {String.fromCharCode(65 + optDisplayIdx)}
                          </span>
                          <div className="flex-1 text-sm text-gray-800">
                            <MathRenderer content={opt.text} />
                          </div>
                          <div className="flex items-center gap-1">
                            {wasSelected && !isCorrectOpt && (
                              <span className="text-xs text-red-500">Your answer</span>
                            )}
                            {isCorrectOpt && (
                              <span className="text-xs text-green-600">Correct</span>
                            )}
                            {icon}
                          </div>
                        </div>
                        {opt.imageUrl && (
                          <div className="relative mt-1 ml-7">
                            <img
                              src={opt.imageUrl}
                              alt={opt.imageAltText || `Option ${String.fromCharCode(65 + optDisplayIdx)}`}
                              className="max-h-40 rounded border object-contain bg-white"
                            />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Stats bar */}
              <div className="px-6 py-3 bg-gray-50 border-t border-gray-100 flex items-center gap-6 flex-wrap text-xs text-gray-500">
                <span className="flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" />
                  Time spent: <strong>{formatTime(currentQuestion.timeSpentMs)}</strong>
                </span>
                <span>
                  Peer accuracy: <strong>{currentQuestion.peerAccuracyPct.toFixed(1)}%</strong>
                </span>
                <span>
                  Marks: <strong className={currentQuestion.marksAwarded >= 0 ? 'text-green-600' : 'text-red-500'}>
                    {currentQuestion.marksAwarded > 0 ? `+${currentQuestion.marksAwarded}` : currentQuestion.marksAwarded}
                  </strong>
                </span>
              </div>

              {/* Explanation Section */}
              {currentQuestion.explanation && (
                <div className="px-6 py-4 bg-teal-50/50 border-t border-teal-100">
                  <p className="text-xs font-bold text-teal-800 uppercase tracking-wide mb-1 flex items-center gap-1">
                    <BookOpen className="w-3.5 h-3.5" /> Explanation
                  </p>
                  <div className="text-sm text-gray-700 leading-relaxed">
                    <MathRenderer content={currentQuestion.explanation} />
                  </div>
                </div>
              )}

              {/* Navigation buttons */}
              <div className="px-6 py-3 bg-white border-t border-gray-200 flex items-center justify-between">
                <button
                  onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
                  disabled={currentIndex === 0}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-gray-600 bg-gray-100 hover:bg-gray-200 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  <ChevronLeft className="w-4 h-4" /> Previous
                </button>
                <span className="text-xs text-gray-400">
                  {currentIndex + 1} of {filteredQuestions.length}
                </span>
                <button
                  onClick={() => setCurrentIndex((prev) => Math.min(filteredQuestions.length - 1, prev + 1))}
                  disabled={currentIndex === filteredQuestions.length - 1}
                  className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-semibold text-white bg-teal-600 hover:bg-teal-700 disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Next <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-2xl border border-gray-200 p-8 text-center text-gray-500">
              No questions match the selected filter.
            </div>
          )}
        </div>

        {/* Right sidebar: Question palette (1 col) */}
        <div className="lg:col-span-1 flex flex-col gap-4">
          <div className="bg-white rounded-2xl border border-gray-200 p-4 shadow-sm">
            <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wide mb-3 flex items-center gap-1.5">
              <Layers className="w-4 h-4 text-teal-600" /> Question Palette
            </h3>
            <div className="grid grid-cols-5 gap-1.5 max-h-96 overflow-y-auto pr-1">
              {filteredQuestions.map((q, idx) => {
                const wasSelected = q.candidateSelectedOptionIds.length > 0;
                let btnColor = 'bg-gray-100 text-gray-500 hover:bg-gray-200 border-gray-200';
                if (q.isCorrect) {
                  btnColor = 'bg-green-100 text-green-800 border-green-300 hover:bg-green-200';
                } else if (wasSelected && !q.isCorrect) {
                  btnColor = 'bg-red-100 text-red-800 border-red-300 hover:bg-red-200';
                }

                const isCurrent = idx === currentIndex;

                return (
                  <button
                    key={q.questionId}
                    onClick={() => setCurrentIndex(idx)}
                    className={`h-8 rounded-lg text-xs font-bold border transition ${btnColor} ${
                      isCurrent ? 'ring-2 ring-teal-600 ring-offset-1 font-extrabold' : ''
                    }`}
                  >
                    {q.questionNumber}
                  </button>
                );
              })}
            </div>

            {/* Legend */}
            <div className="mt-4 pt-3 border-t border-gray-100 space-y-1.5 text-xs text-gray-500">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-green-100 border border-green-300 inline-block" />
                <span>Correct ({correctCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-red-100 border border-red-300 inline-block" />
                <span>Incorrect ({incorrectCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-gray-100 border border-gray-200 inline-block" />
                <span>Unattempted ({unattemptedCount})</span>
              </div>
            </div>
          </div>

          {/* Quick diagnostic tip */}
          <div className="bg-amber-50 rounded-2xl border border-amber-200 p-4 text-xs text-amber-900">
            <p className="font-bold mb-1 flex items-center gap-1">
              <Award className="w-3.5 h-3.5 text-amber-600" /> Review Tip
            </p>
            <p className="leading-relaxed">
              Use arrow keys or <strong>[N]</strong> / <strong>[P]</strong> on your keyboard to navigate between questions.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReviewExam;
