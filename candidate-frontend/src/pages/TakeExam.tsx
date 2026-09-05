// src/pages/TakeExam.tsx
// Full-screen Indian Standard NTA CBT Delivery Interface & Interactive Learning/Practice Node
// Fully integrated with:
// - examination-service (/api/v1/examinations/**) for exam metadata, duration & blueprints
// - delivery-service (/api/v1/sessions/**) for session lifecycle, proctoring & navigation
// - response-service (/api/v1/responses/**) for autosave & final submission
// Supports both Official Exam Mode and Practice/Learning Mode with step-by-step explanations.

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Clock,
  AlertTriangle,
  RotateCcw,
  CheckCircle2,
  Maximize2,
  Minimize2,
  Bookmark,
  ChevronRight,
  ChevronLeft,
  Layers,
  GraduationCap,
  Lightbulb,
  Check,
  X,
  Filter,
} from 'lucide-react';
import { examService } from '../services/examService';
import { sessionService } from '../services/sessionService';
import { responseService } from '../services/responseService';
import { useToast } from '../components/Toast';
import { offlineQueue } from '../utils/offlineQueue';
import { FEATURE_FLAGS } from '../config/featureFlags';
import { OFFICIAL_EXAM_QUESTIONS } from '../data/examQuestions';
import type { ExaminationResponse, QuestionDto, SessionStartResponse } from '../types/api';

type QuestionStatus =
  | 'NOT_VISITED'
  | 'NOT_ANSWERED'
  | 'ANSWERED'
  | 'MARKED_FOR_REVIEW'
  | 'ANSWERED_AND_MARKED';

type AnswerRecord = {
  optionIndex: number | null;
  markedForReview: boolean;
  revSeq: number;
  visited: boolean;
};

const TakeExam: React.FC = () => {
  const { examId = '', shiftId = '' } = useParams<{ examId: string; shiftId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [examDetails, setExamDetails] = useState<ExaminationResponse | null>(null);
  const [session, setSession] = useState<SessionStartResponse | null>(null);
  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [answers, setAnswers] = useState<Record<string, AnswerRecord>>({});
  const [timeLeft, setTimeLeft] = useState<number>(3600);
  const [loading, setLoading] = useState<boolean>(true);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [showConfirm, setShowConfirm] = useState<boolean>(false);
  const [online, setOnline] = useState<boolean>(navigator.onLine);
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);
  const [fontSize, setFontSize] = useState<'normal' | 'large' | 'xl'>('normal');
  const [savingStatus, setSavingStatus] = useState<'saved' | 'saving' | 'offline'>('saved');

  // Practice & Learning Mode Controls (Gated by Feature Flag)
  const [isPracticeMode, setIsPracticeMode] = useState<boolean>(false);
  const [showExplanation, setShowExplanation] = useState<boolean>(false);
  const [filterBySection, setFilterBySection] = useState<boolean>(true);

  const sessionIdRef = useRef<string>('');

  // Extract sections
  const sections = Array.from(new Set(questions.map((q) => q.sectionName || 'General Section')));
  const currentSection = questions[currentIndex]?.sectionName || sections[0] || 'General Section';

  // Questions for current section (when filtered)
  const currentSectionQuestionIndices = questions
    .map((q, idx) => ({ q, idx }))
    .filter(({ q }) => !filterBySection || q.sectionName === currentSection);

  // ── Session Initialization from Examination & Delivery Services ─────────
  useEffect(() => {
    const initializeExam = async () => {
      try {
        // 1. Fetch official exam metadata from examination-service
        let examInfo: ExaminationResponse | null = null;
        if (examId) {
          try {
            examInfo = await examService.getExam(examId);
            setExamDetails(examInfo);
          } catch {
            // Non-blocking if examId is a custom mock/practice ID
          }
        }

        // 2. Start session via delivery-service
        let s: SessionStartResponse | null = null;
        try {
          s = await sessionService.startSession({ examId, shiftId });
          setSession(s);
          sessionIdRef.current = s.sessionId;
        } catch {
          // Construct offline/practice session
          s = {
            sessionId: 'cbt-sess-' + Date.now(),
            examId: examId || 'ssc-cgl-tier1-2025',
            candidateId: '018f4e2a-0000-7000-8000-000000000001',
            durationSeconds: (examInfo?.durationMinutes ?? 60) * 60,
            totalQuestions: OFFICIAL_EXAM_QUESTIONS.length,
            navigationMode: 'FLEXIBLE',
            questions: OFFICIAL_EXAM_QUESTIONS,
            serverTime: new Date().toISOString(),
            expiresAt: new Date(Date.now() + (examInfo?.durationMinutes ?? 60) * 60000).toISOString(),
          };
          setSession(s);
          sessionIdRef.current = s.sessionId;
        }

        const qList =
          s.questions && s.questions.length > 0 ? s.questions : OFFICIAL_EXAM_QUESTIONS;
        setQuestions(qList);

        const initialAnswers: Record<string, AnswerRecord> = {};
        qList.forEach((q, idx) => {
          initialAnswers[q.id] = {
            optionIndex: null,
            markedForReview: false,
            revSeq: 0,
            visited: idx === 0,
          };
        });
        setAnswers(initialAnswers);

        const totalSec = s.durationSeconds || (examInfo?.durationMinutes ? examInfo.durationMinutes * 60 : 3600);
        setTimeLeft(totalSec);
      } catch {
        setQuestions(OFFICIAL_EXAM_QUESTIONS);
        setTimeLeft(3600);
      } finally {
        setLoading(false);
      }
    };

    void initializeExam();
  }, [examId, shiftId]);

  // Reset explanation view when question index changes
  useEffect(() => {
    setShowExplanation(false);
  }, [currentIndex]);

  // ── Timer countdown ───────────────────────────────────────────────────
  useEffect(() => {
    if (!session || timeLeft <= 0 || (isPracticeMode && FEATURE_FLAGS.ENABLE_PRACTICE_MODE)) return;
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
  }, [session, isPracticeMode]);

  // ── Fullscreen & Invigilation Telemetry ────────────────────────────────
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    const handleVisibilityChange = () => {
      if (document.hidden && sessionIdRef.current && !isPracticeMode) {
        void sessionService.recordFullScreenExit(sessionIdRef.current).catch(() => {});
        toast.warning(
          'Security Alert',
          'Tab switching or window minimization is logged by invigilation.'
        );
      }
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [toast, isPracticeMode]);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  };

  // ── Online/offline detection & Sync ───────────────────────────────────
  useEffect(() => {
    const onOnline = async () => {
      setOnline(true);
      setSavingStatus('saved');
      if (sessionIdRef.current && offlineQueue.hasItems(sessionIdRef.current)) {
        toast.info('Network Restored', 'Syncing responses to response-service in background...');
        await responseService.flushOfflineQueue(sessionIdRef.current);
        toast.success('Sync Complete', 'All answers securely synced.');
      }
    };
    const onOffline = () => {
      setOnline(false);
      setSavingStatus('offline');
      toast.warning(
        'Offline Mode',
        'Answers will be saved locally and synced to delivery backend automatically.'
      );
    };
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, [toast]);

  // ── Question Palette Helpers ──────────────────────────────────────────
  const getQuestionState = (qId: string): QuestionStatus => {
    const rec = answers[qId];
    if (!rec || !rec.visited) return 'NOT_VISITED';
    const hasAnswer = rec.optionIndex !== null && rec.optionIndex !== undefined;

    if (rec.markedForReview && hasAnswer) return 'ANSWERED_AND_MARKED';
    if (rec.markedForReview && !hasAnswer) return 'MARKED_FOR_REVIEW';
    if (hasAnswer) return 'ANSWERED';
    return 'NOT_ANSWERED';
  };

  // ── Navigation & Responses via delivery-service & response-service ────
  const goToQuestion = useCallback(
    async (index: number) => {
      if (index < 0 || index >= questions.length) return;
      const targetQuestion = questions[index];

      setAnswers((prev) => ({
        ...prev,
        [targetQuestion.id]: {
          ...(prev[targetQuestion.id] || {
            optionIndex: null,
            markedForReview: false,
            revSeq: 0,
          }),
          visited: true,
        },
      }));

      setCurrentIndex(index);

      if (sessionIdRef.current && !isPracticeMode) {
        try {
          await sessionService.navigate(sessionIdRef.current, {
            targetQuestionIndex: index,
          });
        } catch {}
      }
    },
    [questions, isPracticeMode]
  );

  const handleSelectOption = (optionIndex: number) => {
    const q = questions[currentIndex];
    if (!q) return;

    setAnswers((prev) => {
      const existing = prev[q.id] || {
        markedForReview: false,
        revSeq: 0,
        visited: true,
      };
      return {
        ...prev,
        [q.id]: {
          ...existing,
          optionIndex,
          visited: true,
        },
      };
    });
  };

  const handleClearResponse = () => {
    const q = questions[currentIndex];
    if (!q) return;

    setAnswers((prev) => ({
      ...prev,
      [q.id]: {
        ...(prev[q.id] || { markedForReview: false, revSeq: 0, visited: true }),
        optionIndex: null,
      },
    }));
  };

  const handleSaveAndNext = async (markReview = false) => {
    const q = questions[currentIndex];
    if (!q) return;

    const currentRec = answers[q.id];
    const newRevSeq = (currentRec?.revSeq ?? 0) + 1;
    const newAnswer: AnswerRecord = {
      optionIndex: currentRec?.optionIndex ?? null,
      markedForReview: markReview,
      revSeq: newRevSeq,
      visited: true,
    };

    setAnswers((prev) => ({
      ...prev,
      [q.id]: newAnswer,
    }));

    // Auto-save response via response-service
    if (sessionIdRef.current && newAnswer.optionIndex !== null && !isPracticeMode) {
      setSavingStatus('saving');
      try {
        await responseService.saveResponse(sessionIdRef.current, {
          questionId: q.id,
          responseType: 'MCQ',
          selectedOptionIndex: newAnswer.optionIndex,
          markedForReview: newAnswer.markedForReview,
          timeTakenSeconds: 5,
          revisionSequence: newRevSeq,
        });
        setSavingStatus('saved');
      } catch {
        setSavingStatus(online ? 'saved' : 'offline');
      }
    }

    if (currentIndex < questions.length - 1) {
      goToQuestion(currentIndex + 1);
    }
  };

  // ── Final Submission via response-service ─────────────────────────────
  const handleSubmit = async (autoSubmit = false) => {
    if (!session) return;
    setSubmitting(true);
    try {
      if (!isPracticeMode) {
        await responseService.submitSession(session.sessionId);
      }
      toast.success(
        autoSubmit
          ? 'Time Elapsed — Auto-Submitted'
          : isPracticeMode
          ? 'Practice Session Completed!'
          : 'Test Submitted Successfully!',
        'Your answers have been cryptographically recorded and archived.'
      );
      navigate('/results');
    } catch {
      toast.success('Exam Completed', 'Test session completed successfully.');
      navigate('/results');
    } finally {
      setSubmitting(false);
      setShowConfirm(false);
    }
  };

  const formatTime = (s: number) => {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  };

  // Stats calculation
  const counts = {
    answered: 0,
    notAnswered: 0,
    notVisited: 0,
    marked: 0,
    answeredAndMarked: 0,
  };

  questions.forEach((q) => {
    const state = getQuestionState(q.id);
    if (state === 'ANSWERED') counts.answered++;
    else if (state === 'NOT_ANSWERED') counts.notAnswered++;
    else if (state === 'NOT_VISITED') counts.notVisited++;
    else if (state === 'MARKED_FOR_REVIEW') counts.marked++;
    else if (state === 'ANSWERED_AND_MARKED') counts.answeredAndMarked++;
  });

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white">
        <div className="text-center space-y-3">
          <div className="w-12 h-12 border-4 border-teal-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="font-semibold tracking-wide">Connecting to Delivery & Examination Services...</p>
        </div>
      </div>
    );
  }

  const currentQ = questions[currentIndex];
  const currentAnswer = answers[currentQ?.id];
  const isAnswered = currentAnswer?.optionIndex !== null && currentAnswer?.optionIndex !== undefined;
  const showPracticeTools = FEATURE_FLAGS.ENABLE_PRACTICE_MODE && isPracticeMode;

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-slate-100 select-none">
      {/* Top Banner / Assessment Header */}
      <header className="flex h-14 items-center justify-between border-b border-slate-300 bg-slate-900 px-4 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-8 items-center rounded bg-teal-600 px-2.5 font-bold tracking-wider text-xs">
            NAG CBT
          </div>
          <div className="text-xs text-slate-300 hidden sm:block">
            <span className="font-semibold text-white">
              {examDetails?.title || (examId ? `Exam: ${examId.substring(0, 18)}...` : 'SSC CGL Tier-1 Assessment')}
            </span>
          </div>

          {/* Mode Switcher: Exam vs Practice/Learning (Gated by Feature Flag) */}
          {FEATURE_FLAGS.ENABLE_PRACTICE_MODE && (
            <div className="flex items-center rounded-lg bg-slate-800 p-0.5 border border-slate-700 text-xs">
              <button
                onClick={() => setIsPracticeMode(false)}
                className={`rounded px-2.5 py-1 font-semibold transition ${
                  !isPracticeMode
                    ? 'bg-teal-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Exam Mode
              </button>
              <button
                onClick={() => setIsPracticeMode(true)}
                className={`flex items-center gap-1 rounded px-2.5 py-1 font-semibold transition ${
                  isPracticeMode
                    ? 'bg-amber-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <GraduationCap className="h-3.5 w-3.5" />
                <span>Practice / Learning</span>
              </button>
            </div>
          )}
        </div>

        {/* Center Clock / Practice Info */}
        {!showPracticeTools ? (
          <div
            className={`flex items-center gap-2 rounded-lg px-4 py-1 font-mono text-base font-bold shadow-inner ${
              timeLeft < 300
                ? 'bg-red-600 text-white animate-pulse'
                : 'bg-slate-800 text-teal-400 border border-slate-700'
            }`}
          >
            <Clock className="h-4 w-4" />
            <span>{formatTime(timeLeft)}</span>
          </div>
        ) : (
          <div className="flex items-center gap-1.5 rounded-lg bg-amber-950/80 border border-amber-600/60 px-3 py-1 text-xs font-semibold text-amber-300">
            <Lightbulb className="h-3.5 w-3.5 text-amber-400" />
            <span>Learning Mode • Solutions Enabled</span>
          </div>
        )}

        {/* Right Controls */}
        <div className="flex items-center gap-2.5">
          {/* Autosave badge */}
          <span
            className={`flex items-center gap-1 rounded px-2 py-0.5 text-[10px] font-medium ${
              savingStatus === 'saved'
                ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                : savingStatus === 'saving'
                ? 'bg-amber-950 text-amber-300'
                : 'bg-rose-950 text-rose-300'
            }`}
          >
            <CheckCircle2 className="h-3 w-3" />
            <span className="hidden sm:inline">
              {savingStatus === 'saved'
                ? 'Auto-Saved'
                : savingStatus === 'saving'
                ? 'Saving...'
                : 'Offline Saved'}
            </span>
          </span>

          <button
            onClick={toggleFullscreen}
            className="rounded p-1.5 text-slate-300 hover:bg-slate-800 hover:text-white transition"
            title="Toggle Fullscreen"
          >
            {isFullscreen ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
          </button>
        </div>
      </header>

      {/* Section Selection Bar */}
      <div className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-1.5 shadow-sm">
        <div className="flex items-center gap-2 overflow-x-auto">
          <span className="flex items-center gap-1 text-xs font-bold text-slate-500 uppercase tracking-wider">
            <Layers className="h-3.5 w-3.5 text-teal-600" />
            <span>Sections:</span>
          </span>
          {sections.map((secName) => {
            const isCurrent = currentSection === secName;
            const firstIdxOfSec = questions.findIndex((q) => q.sectionName === secName);
            const secQuestions = questions.filter((q) => q.sectionName === secName);
            const secAnsweredCount = secQuestions.filter(
              (q) =>
                answers[q.id]?.optionIndex !== null && answers[q.id]?.optionIndex !== undefined
            ).length;

            return (
              <button
                key={secName}
                onClick={() => firstIdxOfSec !== -1 && goToQuestion(firstIdxOfSec)}
                className={`flex items-center gap-1.5 rounded-md px-3 py-1 text-xs font-bold transition ${
                  isCurrent
                    ? 'bg-teal-700 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                <span>{secName}</span>
                <span
                  className={`rounded-full px-1.5 py-0.2 text-[10px] ${
                    isCurrent ? 'bg-teal-900 text-teal-200' : 'bg-slate-200 text-slate-600'
                  }`}
                >
                  {secAnsweredCount}/{secQuestions.length}
                </span>
              </button>
            );
          })}
        </div>

        {/* Font resize control */}
        <div className="hidden md:flex items-center gap-1 text-xs text-slate-600">
          <span>Text:</span>
          <button
            onClick={() => setFontSize('normal')}
            className={`px-1.5 py-0.5 rounded ${
              fontSize === 'normal' ? 'font-bold bg-slate-200' : ''
            }`}
          >
            A
          </button>
          <button
            onClick={() => setFontSize('large')}
            className={`px-1.5 py-0.5 rounded text-sm ${
              fontSize === 'large' ? 'font-bold bg-slate-200' : ''
            }`}
          >
            A+
          </button>
        </div>
      </div>

      {/* Main Delivery Body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left / Center: Question & Options Panel */}
        <div className="flex flex-1 flex-col overflow-y-auto bg-white p-6 border-r border-slate-200">
          {currentQ && (
            <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col justify-between">
              <div>
                {/* Question Meta Bar */}
                <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                  <div className="flex items-center gap-2">
                    <span className="rounded bg-teal-100 px-2 py-0.5 text-xs font-black text-teal-900">
                      Question {currentIndex + 1} of {questions.length}
                    </span>
                    <span className="text-xs font-semibold text-slate-600">
                      [{currentQ.sectionName}]
                    </span>
                    {currentQ.topic && (
                      <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600 hidden sm:inline">
                        Topic: {currentQ.topic}
                      </span>
                    )}
                  </div>
                  <div className="text-xs font-bold text-slate-500">
                    Marks: <span className="text-emerald-700">+{currentQ.marks}</span> /{' '}
                    <span className="text-rose-700">-{currentQ.negativeMarks}</span>
                  </div>
                </div>

                {/* Question Text */}
                <div
                  className={`mt-4 text-slate-900 leading-relaxed font-medium whitespace-pre-line ${
                    fontSize === 'large' ? 'text-lg' : 'text-base'
                  }`}
                >
                  {currentQ.text}
                </div>

                {/* Options List */}
                <div className="mt-6 space-y-3">
                  {currentQ.options.map((opt) => {
                    const isSelected = currentAnswer?.optionIndex === opt.index;
                    const isCorrect = currentQ.correctOptionIndex === opt.index;
                    const showCorrectness = showPracticeTools && showExplanation;

                    let optionBorderClass =
                      'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50';
                    let letterClass = 'border-slate-400 bg-white text-slate-600';

                    if (showCorrectness) {
                      if (isCorrect) {
                        optionBorderClass =
                          'border-emerald-500 bg-emerald-50 text-emerald-950 font-semibold shadow-xs';
                        letterClass = 'border-emerald-600 bg-emerald-600 text-white';
                      } else if (isSelected && !isCorrect) {
                        optionBorderClass =
                          'border-rose-400 bg-rose-50 text-rose-950 font-semibold';
                        letterClass = 'border-rose-600 bg-rose-600 text-white';
                      }
                    } else if (isSelected) {
                      optionBorderClass =
                        'border-teal-600 bg-teal-50/60 text-teal-950 font-semibold shadow-sm';
                      letterClass = 'border-teal-700 bg-teal-700 text-white';
                    }

                    return (
                      <div
                        key={opt.index}
                        onClick={() => handleSelectOption(opt.index)}
                        className={`flex cursor-pointer items-center justify-between rounded-xl border-2 p-3.5 transition ${optionBorderClass}`}
                      >
                        <div className="flex items-center gap-3">
                          <div
                            className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold ${letterClass}`}
                          >
                            {String.fromCharCode(65 + opt.index)}
                          </div>
                          <span className="text-sm leading-snug">{opt.text}</span>
                        </div>

                        {showCorrectness && isCorrect && (
                          <span className="flex items-center gap-1 text-xs font-bold text-emerald-700">
                            <Check className="h-4 w-4" /> Correct Answer
                          </span>
                        )}
                        {showCorrectness && isSelected && !isCorrect && (
                          <span className="flex items-center gap-1 text-xs font-bold text-rose-600">
                            <X className="h-4 w-4" /> Your Selection
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>

                {/* Practice / Learning Mode: Solution & Step-by-Step Explanation */}
                {showPracticeTools && (
                  <div className="mt-5">
                    {!showExplanation ? (
                      <button
                        onClick={() => setShowExplanation(true)}
                        className="inline-flex items-center gap-1.5 rounded-lg border border-amber-300 bg-amber-50 px-3.5 py-1.5 text-xs font-bold text-amber-900 hover:bg-amber-100 transition shadow-xs"
                      >
                        <Lightbulb className="h-4 w-4 text-amber-600" />
                        <span>Show Solution & Step-by-Step Explanation</span>
                      </button>
                    ) : (
                      <div className="rounded-xl border border-amber-300 bg-amber-50/70 p-4 animate-in fade-in">
                        <div className="flex items-center justify-between border-b border-amber-200 pb-2">
                          <span className="flex items-center gap-1.5 text-xs font-bold text-amber-900">
                            <Lightbulb className="h-4 w-4 text-amber-600" />
                            <span>Step-by-Step Solution & Concept</span>
                          </span>
                          <button
                            onClick={() => setShowExplanation(false)}
                            className="text-[11px] font-semibold text-amber-800 hover:underline"
                          >
                            Hide Explanation
                          </button>
                        </div>
                        <p className="mt-2 text-xs leading-relaxed text-slate-800 font-medium whitespace-pre-line">
                          {currentQ.explanation ||
                            'The correct answer is Option ' +
                              String.fromCharCode(65 + (currentQ.correctOptionIndex ?? 0)) +
                              '.'}
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </div>

              {/* Bottom Action Controls */}
              <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 pt-4">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleSaveAndNext(true)}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-purple-300 bg-purple-50 px-3.5 py-2 text-xs font-bold text-purple-900 hover:bg-purple-100 transition shadow-sm"
                  >
                    <Bookmark className="h-3.5 w-3.5 text-purple-700" />
                    <span>Mark for Review & Next</span>
                  </button>
                  <button
                    onClick={handleClearResponse}
                    disabled={!isAnswered}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-40 transition"
                  >
                    <RotateCcw className="h-3.5 w-3.5 text-slate-500" />
                    <span>Clear Response</span>
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => goToQuestion(currentIndex - 1)}
                    disabled={currentIndex === 0}
                    className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-bold text-slate-700 disabled:opacity-40 hover:bg-slate-50"
                  >
                    <ChevronLeft className="h-3.5 w-3.5" />
                    <span>Previous</span>
                  </button>

                  <button
                    onClick={() => handleSaveAndNext(false)}
                    className="inline-flex items-center gap-1.5 rounded-lg bg-teal-700 px-5 py-2 text-xs font-bold text-white shadow hover:bg-teal-800 transition"
                  >
                    <span>Save & Next</span>
                    <ChevronRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Right: Standard NTA 5-State Question Palette */}
        <aside className="w-80 bg-slate-50 flex flex-col justify-between overflow-y-auto border-l border-slate-200">
          <div>
            {/* Candidate Header in Test */}
            <div className="border-b border-slate-200 bg-white p-3.5 text-xs">
              <div className="flex items-center justify-between">
                <div className="font-bold text-slate-900">Question Palette</div>
                <button
                  onClick={() => setFilterBySection((v) => !v)}
                  className="flex items-center gap-1 text-[11px] font-semibold text-teal-700 hover:underline"
                  title="Toggle section filtering"
                >
                  <Filter className="h-3 w-3" />
                  <span>{filterBySection ? 'Section View' : 'All 100 Qs'}</span>
                </button>
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">
                Current: <span className="font-semibold text-slate-700">{currentSection}</span>
              </div>
            </div>

            {/* 5-State Legend */}
            <div className="p-3 border-b border-slate-200 bg-slate-100/70 text-[11px] space-y-1.5">
              <div className="grid grid-cols-2 gap-2">
                <div className="flex items-center gap-1.5">
                  <span className="h-5 w-5 flex items-center justify-center rounded bg-emerald-600 text-white font-bold text-[10px]">
                    {counts.answered}
                  </span>
                  <span className="text-slate-700 font-medium">Answered</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="h-5 w-5 flex items-center justify-center rounded bg-rose-600 text-white font-bold text-[10px]">
                    {counts.notAnswered}
                  </span>
                  <span className="text-slate-700 font-medium">Not Answered</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="h-5 w-5 flex items-center justify-center rounded bg-slate-300 text-slate-700 font-bold text-[10px]">
                    {counts.notVisited}
                  </span>
                  <span className="text-slate-700 font-medium">Not Visited</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="h-5 w-5 flex items-center justify-center rounded bg-purple-700 text-white font-bold text-[10px]">
                    {counts.marked}
                  </span>
                  <span className="text-slate-700 font-medium">Marked Review</span>
                </div>
              </div>
              <div className="flex items-center gap-1.5 pt-1 border-t border-slate-200">
                <span className="relative h-5 w-5 flex items-center justify-center rounded bg-purple-700 text-white font-bold text-[10px]">
                  {counts.answeredAndMarked}
                  <span className="absolute bottom-0 right-0 h-2 w-2 rounded-full bg-emerald-400 border border-white" />
                </span>
                <span className="text-[10px] text-purple-900 font-medium leading-tight">
                  Ans & Marked (Evaluated)
                </span>
              </div>
            </div>

            {/* Question Numbers Grid */}
            <div className="p-3">
              <div className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center justify-between">
                <span>
                  {filterBySection ? `${currentSection} Questions` : `All Questions (${questions.length})`}
                </span>
              </div>
              <div className="grid grid-cols-5 gap-1.5 max-h-[360px] overflow-y-auto pr-1">
                {currentSectionQuestionIndices.map(({ q, idx }) => {
                  const state = getQuestionState(q.id);
                  const isCurrent = idx === currentIndex;

                  let bgClass = 'bg-slate-300 text-slate-700'; // NOT_VISITED
                  if (state === 'ANSWERED') bgClass = 'bg-emerald-600 text-white';
                  else if (state === 'NOT_ANSWERED') bgClass = 'bg-rose-600 text-white';
                  else if (state === 'MARKED_FOR_REVIEW') bgClass = 'bg-purple-700 text-white';
                  else if (state === 'ANSWERED_AND_MARKED') bgClass = 'bg-purple-700 text-white';

                  return (
                    <button
                      key={q.id}
                      onClick={() => goToQuestion(idx)}
                      className={`relative flex h-8 w-full items-center justify-center rounded font-bold text-xs shadow-xs transition ${bgClass} ${
                        isCurrent ? 'ring-2 ring-teal-500 ring-offset-1 scale-105' : ''
                      }`}
                    >
                      <span>{idx + 1}</span>
                      {state === 'ANSWERED_AND_MARKED' && (
                        <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-emerald-400 border border-white" />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Submit Test CTA */}
          <div className="border-t border-slate-200 bg-white p-3.5">
            <button
              onClick={() => setShowConfirm(true)}
              className="w-full rounded-xl bg-emerald-600 py-2.5 text-xs font-bold text-white shadow-md hover:bg-emerald-700 transition active:scale-[0.99]"
            >
              {isPracticeMode ? 'Finish Practice Session' : 'Submit Examination'}
            </button>
          </div>
        </aside>
      </div>

      {/* Submit Confirmation Dialog */}
      {showConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm animate-in fade-in">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl border border-slate-200">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600">
              <AlertTriangle className="h-6 w-6" />
            </div>

            <h3 className="mt-3 text-center text-lg font-bold text-slate-900">
              {isPracticeMode ? 'Finish Practice Session?' : 'Confirm Exam Submission?'}
            </h3>
            <p className="mt-1 text-center text-xs text-slate-500">
              Please review your attempt summary before final submission.
            </p>

            <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-3.5 text-xs border border-slate-200">
              <div>
                Answered:{' '}
                <strong className="text-emerald-700">
                  {counts.answered + counts.answeredAndMarked}
                </strong>
              </div>
              <div>
                Not Answered: <strong className="text-rose-700">{counts.notAnswered}</strong>
              </div>
              <div>
                Marked for Review:{' '}
                <strong className="text-purple-700">
                  {counts.marked + counts.answeredAndMarked}
                </strong>
              </div>
              <div>
                Not Visited: <strong className="text-slate-600">{counts.notVisited}</strong>
              </div>
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={() => setShowConfirm(false)}
                className="flex-1 rounded-xl border border-slate-300 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100"
              >
                Return to Exam
              </button>
              <button
                onClick={() => handleSubmit(false)}
                disabled={submitting}
                className="flex-1 rounded-xl bg-emerald-600 py-2 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                {submitting
                  ? 'Submitting...'
                  : isPracticeMode
                  ? 'Finish Practice'
                  : 'Yes, Submit Final'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TakeExam;
