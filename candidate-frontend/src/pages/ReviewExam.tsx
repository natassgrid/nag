// src/pages/ReviewExam.tsx
// Post-exam question review page — shows candidate's answers vs correct answers,
// step-by-step LaTeX solutions, time spent, and peer accuracy benchmarks.
// Route: /results/:examId/review

import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, ChevronLeft, ChevronRight, Clock,
  CheckCircle, XCircle, MinusCircle, Filter, Loader,
} from 'lucide-react';
import { MathRenderer } from '../components/MathRenderer';
import { resultService } from '../services/resultService';
import { tokenManager } from '../utils/tokenManager';
import { useToast } from '../components/Toast';
import type { ReviewQuestion, ExamReviewResponse } from '../types/api';

type FilterMode = 'ALL' | 'CORRECT' | 'INCORRECT' | 'UNATTEMPTED';

const ReviewExam: React.FC = () => {
  const { examId } = useParams<{ examId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const userId = tokenManager.getUserId();

  const [reviewData, setReviewData] = useState<ExamReviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [filter, setFilter] = useState<FilterMode>('ALL');
  const [showSolution, setShowSolution] = useState(false);

  useEffect(() => {
    if (!userId || !examId) return;
    setLoading(true);
    resultService.getReviewData(userId, examId)
      .then(setReviewData)
      .catch(() => toast.error('Failed to load review data'))
      .finally(() => setLoading(false));
  }, [userId, examId, toast]);

  const filteredQuestions: ReviewQuestion[] = (reviewData?.questions ?? []).filter((q) => {
    switch (filter) {
      case 'CORRECT': return q.isCorrect;
      case 'INCORRECT': return !q.isCorrect && q.candidateSelectedOptionIds.length > 0;
      case 'UNATTEMPTED': return q.candidateSelectedOptionIds.length === 0;
      default: return true;
    }
  });

  const currentQuestion = filteredQuestions[currentIndex] ?? null;

  const handlePrev = useCallback(() => {
    setCurrentIndex((i) => Math.max(0, i - 1));
    setShowSolution(false);
  }, []);

  const handleNext = useCallback(() => {
    setCurrentIndex((i) => Math.min(filteredQuestions.length - 1, i + 1));
    setShowSolution(false);
  }, [filteredQuestions.length]);

  const formatTime = (ms: number): string => {
    const secs = Math.floor(ms / 1000);
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
          <button
            onClick={() => navigate('/results')}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 transition"
          >
            <ArrowLeft className="w-4 h-4" />
            <span className="text-sm font-medium">Back to Results</span>
          </button>
          <h1 className="text-base font-semibold text-gray-800">Post-Exam Review</h1>
          <div className="text-sm text-gray-500">
            {filteredQuestions.length > 0
              ? `${currentIndex + 1} / ${filteredQuestions.length}`
              : '0 questions'}
          </div>
        </div>
      </div>

      <div className="max-w-5xl mx-auto px-4 py-6 space-y-4">
        {/* Filter Bar */}
        <div className="flex items-center gap-2 flex-wrap">
          <Filter className="w-4 h-4 text-gray-400" />
          {(['ALL', 'CORRECT', 'INCORRECT', 'UNATTEMPTED'] as FilterMode[]).map((f) => (
            <button
              key={f}
              onClick={() => { setFilter(f); setCurrentIndex(0); setShowSolution(false); }}
              className={`px-3 py-1 rounded-full text-xs font-medium transition ${
                filter === f
                  ? 'bg-indigo-600 text-white'
                  : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
              }`}
            >
              {f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
          <span className="ml-auto text-xs text-gray-400">
            Showing {filteredQuestions.length} question{filteredQuestions.length !== 1 ? 's' : ''}
          </span>
        </div>

        {filteredQuestions.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <MinusCircle className="w-10 h-10 mx-auto mb-3" />
            <p>No questions match this filter.</p>
          </div>
        ) : currentQuestion ? (
          <>
            {/* Question Card */}
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
              {/* Question meta */}
              <div className="px-6 py-3 bg-gray-50 border-b border-gray-100 flex items-center gap-2 flex-wrap">
                <span className="text-xs font-medium text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-full">
                  {currentQuestion.subject}
                </span>
                <span className="text-xs text-gray-500">{currentQuestion.topic}</span>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ml-auto ${
                  currentQuestion.difficulty === 'EASY' ? 'bg-green-50 text-green-700' :
                  currentQuestion.difficulty === 'HARD' ? 'bg-red-50 text-red-700' :
                  'bg-yellow-50 text-yellow-700'
                }`}>
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

              {/* Question content */}
              <div className="px-6 py-5">
                <p className="text-sm font-medium text-gray-500 mb-2">
                  Q{currentQuestion.questionNumber}.
                </p>
                <div className="text-gray-800 text-base leading-relaxed mb-5">
                  <MathRenderer content={currentQuestion.content} />
                </div>

                {/* Options */}
                <div className="space-y-2">
                  {currentQuestion.options.map((opt) => {
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
                        className={`flex items-start gap-3 px-4 py-3 rounded-xl border ${bg} transition`}
                      >
                        <span className="text-xs font-bold text-gray-500 mt-0.5 w-4 flex-shrink-0">
                          {opt.id.toUpperCase().slice(-1)}
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
                    {currentQuestion.marksAwarded >= 0 ? '+' : ''}{currentQuestion.marksAwarded}
                  </strong>
                </span>
              </div>
            </div>

            {/* Solution Accordion */}
            {currentQuestion.explanation && (
              <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
                <button
                  onClick={() => setShowSolution((s) => !s)}
                  className="w-full flex items-center justify-between px-6 py-4 text-left"
                >
                  <span className="text-sm font-semibold text-gray-800">📖 Step-by-step Solution</span>
                  <span className="text-xs text-indigo-600">{showSolution ? 'Hide ▲' : 'Show ▼'}</span>
                </button>
                {showSolution && (
                  <div className="px-6 pb-5 border-t border-gray-100">
                    <div className="pt-4 text-sm text-gray-700 leading-relaxed">
                      <MathRenderer content={currentQuestion.explanation} />
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Navigation */}
            <div className="flex items-center justify-between">
              <button
                onClick={handlePrev}
                disabled={currentIndex === 0}
                className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                <ChevronLeft className="w-4 h-4" /> Previous
              </button>
              <span className="text-xs text-gray-400">
                Question {currentIndex + 1} of {filteredQuestions.length}
              </span>
              <button
                onClick={handleNext}
                disabled={currentIndex === filteredQuestions.length - 1}
                className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-xl text-sm hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                Next <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
};

export default ReviewExam;
