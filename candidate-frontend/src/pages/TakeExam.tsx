// src/pages/TakeExam.tsx
// Full-screen CBT — wired to real delivery and response service APIs.
// Start session → navigate → save per question → submit.

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, AlertTriangle, CheckSquare, Square } from 'lucide-react';
import { sessionService } from '../services/sessionService';
import { responseService } from '../services/responseService';
import { useToast } from '../components/Toast';
import { offlineQueue } from '../utils/offlineQueue';
import type { QuestionDto, SessionStartResponse } from '../types/api';

// ─── Types ────────────────────────────────────────────────────────────────────

type AnswerMap = Record<string, { optionIndex: number | null; markedForReview: boolean; revSeq: number }>;

// ─── Component ────────────────────────────────────────────────────────────────

const TakeExam: React.FC = () => {
  const { examId = '', shiftId = '' } = useParams<{ examId: string; shiftId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [session, setSession] = useState<SessionStartResponse | null>(null);
  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<AnswerMap>({});
  const [timeLeft, setTimeLeft] = useState(0);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [online, setOnline] = useState(navigator.onLine);
  const sessionIdRef = useRef<string>('');

  // ── Session start ──────────────────────────────────────────────────────────
  useEffect(() => {
    const startSession = async () => {
      try {
        const s = await sessionService.startSession({ examId, shiftId });
        setSession(s);
        sessionIdRef.current = s.sessionId;
        setQuestions(s.questions);
        // Sync timer to server time
        const serverExpiry = new Date(s.expiresAt).getTime();
        setTimeLeft(Math.max(0, Math.floor((serverExpiry - Date.now()) / 1000)));
      } catch {
        toast.error('Cannot start exam', 'Failed to initialize session. Please try again.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    };
    void startSession();
  }, [examId, shiftId, toast, navigate]);

  // ── Timer countdown ────────────────────────────────────────────────────────
  useEffect(() => {
    if (!session || timeLeft <= 0) return;
    const interval = setInterval(() => {
      setTimeLeft((t) => {
        if (t <= 1) {
          clearInterval(interval);
          void handleSubmit(true);
          return 0;
        }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [session]); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Fullscreen enforcement ─────────────────────────────────────────────────
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden && sessionIdRef.current) {
        void sessionService.recordFullScreenExit(sessionIdRef.current);
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, []);

  // ── Online/offline detection ───────────────────────────────────────────────
  useEffect(() => {
    const onOnline = async () => {
      setOnline(true);
      if (sessionIdRef.current && offlineQueue.hasItems(sessionIdRef.current)) {
        toast.info('Back online', 'Syncing your saved answers…');
        await responseService.flushOfflineQueue(sessionIdRef.current);
        toast.success('Synced!', 'All offline responses have been saved.');
      }
    };
    const onOffline = () => {
      setOnline(false);
      toast.warning('You are offline', 'Answers will be saved locally and synced when back online.');
    };
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, [toast]);

  // ── Answer a question ──────────────────────────────────────────────────────
  const handleAnswer = useCallback(
    async (questionId: string, optionIndex: number) => {
      const prev = answers[questionId];
      const revSeq = (prev?.revSeq ?? 0) + 1;
      const newAnswer = { optionIndex, markedForReview: prev?.markedForReview ?? false, revSeq };

      setAnswers((a) => ({ ...a, [questionId]: newAnswer }));

      // Auto-save to backend
      if (sessionIdRef.current) {
        await responseService.saveResponse(sessionIdRef.current, {
          questionId,
          responseType: 'MCQ',
          selectedOptionIndex: optionIndex,
          markedForReview: newAnswer.markedForReview,
          timeTakenSeconds: 0,
          revisionSequence: revSeq,
        });
      }
    },
    [answers],
  );

  const toggleReview = (questionId: string) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        markedForReview: !prev[questionId]?.markedForReview,
      },
    }));
  };

  // ── Navigate between questions ─────────────────────────────────────────────
  const goToQuestion = useCallback(
    async (index: number) => {
      if (!session) return;
      setCurrentIndex(index);
      try {
        await sessionService.navigate(session.sessionId, { targetQuestionIndex: index });
      } catch {
        // Non-critical — just a navigation record
      }
    },
    [session],
  );

  // ── Submit ─────────────────────────────────────────────────────────────────
  const handleSubmit = async (autoSubmit = false) => {
    if (!session) return;
    setSubmitting(true);
    try {
      await responseService.submitSession(session.sessionId);
      toast.success(autoSubmit ? 'Time up — submitted!' : 'Exam submitted!', 'Your responses have been recorded.');
      navigate('/results');
    } catch {
      toast.error('Submission failed', 'Please try again immediately.');
    } finally {
      setSubmitting(false);
      setShowConfirm(false);
    }
  };

  // ── Helpers ───────────────────────────────────────────────────────────────
  const formatTime = (s: number) => {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
      : `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  };

  const getQuestionStatus = (q: QuestionDto) => {
    const ans = answers[q.id];
    if (ans?.markedForReview) return 'review';
    if (ans?.optionIndex !== null && ans?.optionIndex !== undefined) return 'answered';
    return 'unanswered';
  };

  const statusColors: Record<string, string> = {
    answered: 'bg-green-500 text-white',
    review: 'bg-amber-400 text-white',
    unanswered: 'bg-gray-200 text-gray-600',
    current: 'bg-indigo-600 text-white ring-2 ring-indigo-300',
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-900 text-white">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-indigo-400 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p>Initializing exam session…</p>
        </div>
      </div>
    );
  }

  if (!session || questions.length === 0) return null;

  const currentQuestion = questions[currentIndex];
  const currentAnswer = answers[currentQuestion?.id];

  const answeredCount = Object.values(answers).filter(
    (a) => a.optionIndex !== null && a.optionIndex !== undefined,
  ).length;
  const reviewCount = Object.values(answers).filter((a) => a.markedForReview).length;

  return (
    <div className="min-h-screen bg-gray-900 flex flex-col">
      {/* Header */}
      <header className="bg-gray-800 border-b border-gray-700 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-indigo-600 px-3 py-1.5 rounded-lg text-white text-sm font-semibold">
            NAG
          </div>
          <div className="text-white text-sm">
            Q {currentIndex + 1}/{questions.length}
          </div>
        </div>

        {/* Offline indicator */}
        {!online && (
          <div className="flex items-center gap-1.5 bg-amber-600 text-white text-xs px-3 py-1 rounded-full">
            <AlertTriangle className="w-3 h-3" /> Offline — saving locally
          </div>
        )}

        {/* Timer */}
        <div className={`flex items-center gap-2 px-4 py-2 rounded-lg font-mono text-lg font-bold ${
          timeLeft < 300 ? 'bg-red-600 text-white' : 'bg-gray-700 text-white'
        }`}>
          <Clock className="w-5 h-5" />
          {formatTime(timeLeft)}
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Main question area */}
        <main className="flex-1 overflow-y-auto p-6">
          <div className="max-w-3xl mx-auto">
            {/* Question */}
            <div className="bg-gray-800 rounded-xl p-6 mb-4">
              <div className="flex items-center gap-2 mb-4">
                <span className="bg-indigo-600 text-white text-xs px-2 py-0.5 rounded font-medium">
                  Q{currentIndex + 1}
                </span>
                <span className="text-gray-400 text-xs">{currentQuestion.sectionName}</span>
                <span className="ml-auto text-xs text-gray-400">
                  +{currentQuestion.marks} / -{currentQuestion.negativeMarks}
                </span>
              </div>
              <p className="text-white text-base leading-relaxed">{currentQuestion.text}</p>
            </div>

            {/* Options */}
            <div className="space-y-3 mb-6">
              {currentQuestion.options.map((opt) => (
                <button
                  key={opt.index}
                  onClick={() => void handleAnswer(currentQuestion.id, opt.index)}
                  className={`w-full text-left p-4 rounded-xl border transition ${
                    currentAnswer?.optionIndex === opt.index
                      ? 'border-indigo-500 bg-indigo-900/50 text-indigo-200'
                      : 'border-gray-700 bg-gray-800 text-gray-300 hover:border-gray-500'
                  }`}
                >
                  <span className="inline-flex items-center gap-3">
                    <span className={`w-7 h-7 rounded-full border flex items-center justify-center text-sm font-bold shrink-0 ${
                      currentAnswer?.optionIndex === opt.index
                        ? 'bg-indigo-500 border-indigo-500 text-white'
                        : 'border-gray-600 text-gray-400'
                    }`}>
                      {String.fromCharCode(65 + opt.index)}
                    </span>
                    {opt.text}
                  </span>
                </button>
              ))}
            </div>

            {/* Action buttons */}
            <div className="flex items-center gap-3 flex-wrap">
              <button
                onClick={() => toggleReview(currentQuestion.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition ${
                  currentAnswer?.markedForReview
                    ? 'bg-amber-500 text-white'
                    : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                }`}
              >
                {currentAnswer?.markedForReview ? (
                  <CheckSquare className="w-4 h-4" />
                ) : (
                  <Square className="w-4 h-4" />
                )}
                Mark for Review
              </button>

              <div className="flex items-center gap-2 ml-auto">
                <button
                  onClick={() => goToQuestion(Math.max(0, currentIndex - 1))}
                  disabled={currentIndex === 0}
                  className="px-4 py-2 bg-gray-700 text-gray-300 rounded-lg text-sm disabled:opacity-40 hover:bg-gray-600"
                >
                  Previous
                </button>
                {currentIndex < questions.length - 1 ? (
                  <button
                    onClick={() => goToQuestion(currentIndex + 1)}
                    className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm hover:bg-indigo-700"
                  >
                    Save &amp; Next
                  </button>
                ) : (
                  <button
                    onClick={() => setShowConfirm(true)}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 font-medium"
                  >
                    Submit Exam
                  </button>
                )}
              </div>
            </div>
          </div>
        </main>

        {/* Question palette sidebar */}
        <aside className="w-64 bg-gray-800 border-l border-gray-700 flex flex-col overflow-y-auto">
          <div className="p-4 border-b border-gray-700">
            <h3 className="text-white text-sm font-semibold mb-3">Question Palette</h3>
            <div className="space-y-1.5 text-xs text-gray-400">
              {[
                { color: 'bg-green-500', label: `Answered (${answeredCount})` },
                { color: 'bg-amber-400', label: `Review (${reviewCount})` },
                { color: 'bg-gray-600', label: `Not attempted (${questions.length - answeredCount - reviewCount})` },
              ].map((l) => (
                <div key={l.label} className="flex items-center gap-2">
                  <span className={`w-3 h-3 rounded-full shrink-0 ${l.color}`} />
                  {l.label}
                </div>
              ))}
            </div>
          </div>

          <div className="p-4 grid grid-cols-5 gap-1.5">
            {questions.map((q, i) => {
              const status = i === currentIndex ? 'current' : getQuestionStatus(q);
              return (
                <button
                  key={q.id}
                  onClick={() => goToQuestion(i)}
                  className={`w-9 h-9 text-xs font-bold rounded-lg transition ${statusColors[status]}`}
                  aria-label={`Question ${i + 1}, ${status}`}
                >
                  {i + 1}
                </button>
              );
            })}
          </div>

          <div className="mt-auto p-4">
            <button
              onClick={() => setShowConfirm(true)}
              className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2.5 rounded-lg text-sm transition"
            >
              Submit Exam
            </button>
          </div>
        </aside>
      </div>

      {/* Submit confirmation modal */}
      {showConfirm && (
        <div
          className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-title"
        >
          <div className="bg-white rounded-2xl p-6 max-w-md w-full">
            <AlertTriangle className="w-10 h-10 text-amber-500 mx-auto mb-3" />
            <h3 id="confirm-title" className="text-lg font-bold text-gray-800 text-center mb-2">
              Submit Examination?
            </h3>
            <div className="text-sm text-gray-600 text-center mb-6 space-y-1">
              <p>Answered: <strong>{answeredCount}</strong> / {questions.length}</p>
              <p>Marked for review: <strong>{reviewCount}</strong></p>
              <p className="text-amber-600 font-medium">This action cannot be undone.</p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => setShowConfirm(false)}
                className="flex-1 border border-gray-300 text-gray-700 py-2.5 rounded-lg hover:bg-gray-50 font-medium"
              >
                Continue Exam
              </button>
              <button
                onClick={() => void handleSubmit(false)}
                disabled={submitting}
                className="flex-1 bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white py-2.5 rounded-lg font-medium"
              >
                {submitting ? 'Submitting…' : 'Yes, Submit'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TakeExam;
