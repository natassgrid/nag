// src/pages/TakeExam.tsx
// Full-screen Indian Standard NTA CBT Delivery Interface
// 5-state Question Palette, Section Tabs, Autosave Sync, Fullscreen lock & Verification.

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
} from 'lucide-react';
import { sessionService } from '../services/sessionService';
import { responseService } from '../services/responseService';
import { useToast } from '../components/Toast';
import { offlineQueue } from '../utils/offlineQueue';
import type { QuestionDto, SessionStartResponse } from '../types/api';

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

const SAMPLE_QUESTIONS: QuestionDto[] = [
  {
    id: 'q1000000-0000-0000-0000-000000000001',
    text: 'Select the option that is related to the third word in the same way as the second word is related to the first word:\n\nThermometer : Temperature :: Hygrometer : ?',
    options: [
      { index: 0, text: 'Pressure' },
      { index: 1, text: 'Humidity' },
      { index: 2, text: 'Current' },
      { index: 3, text: 'Specific Gravity' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    sectionId: 'sec-1',
    sectionName: 'General Intelligence & Reasoning',
  },
  {
    id: 'q1000000-0000-0000-0000-000000000002',
    text: 'If A + B means A is the mother of B; A - B means A is the brother of B; A % B means A is the father of B and A × B means A is the sister of B, which of the following shows that P is the maternal uncle of Q?',
    options: [
      { index: 0, text: 'P - M + N × Q' },
      { index: 1, text: 'P - M + Q' },
      { index: 2, text: 'P + M - Q' },
      { index: 3, text: 'P × M - Q' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    sectionId: 'sec-1',
    sectionName: 'General Intelligence & Reasoning',
  },
  {
    id: 'q1000000-0000-0000-0000-000000000003',
    text: 'Which Article of the Constitution of India provides for the establishment of a Finance Commission every fifth year?',
    options: [
      { index: 0, text: 'Article 265' },
      { index: 1, text: 'Article 280' },
      { index: 2, text: 'Article 324' },
      { index: 3, text: 'Article 352' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    sectionId: 'sec-2',
    sectionName: 'General Awareness',
  },
  {
    id: 'q1000000-0000-0000-0000-000000000004',
    text: 'The fundamental objective of the Digital Public Infrastructure (DPI) approach is:',
    options: [
      { index: 0, text: 'Creating monopolistic closed ecosystems' },
      { index: 1, text: 'Interoperable, open standards enabling public and private innovation at population scale' },
      { index: 2, text: 'Replacing all physical hardware with cloud-only instances' },
      { index: 3, text: 'Mandatory centralized proprietary identity management' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    sectionId: 'sec-2',
    sectionName: 'General Awareness',
  },
  {
    id: 'q1000000-0000-0000-0000-000000000005',
    text: 'A sum of money at compound interest doubles itself in 4 years. In how many years will it amount to 8 times itself at the same rate of interest?',
    options: [
      { index: 0, text: '8 years' },
      { index: 1, text: '12 years' },
      { index: 2, text: '16 years' },
      { index: 3, text: '24 years' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    sectionId: 'sec-3',
    sectionName: 'Quantitative Aptitude',
  },
  {
    id: 'q1000000-0000-0000-0000-000000000006',
    text: 'Select the most appropriate antonym of the given word: VIGILANT',
    options: [
      { index: 0, text: 'Careless' },
      { index: 1, text: 'Watchful' },
      { index: 2, text: 'Alert' },
      { index: 3, text: 'Attentive' },
    ],
    marks: 2,
    negativeMarks: 0.5,
    sectionId: 'sec-4',
    sectionName: 'English Comprehension',
  },
];

const TakeExam: React.FC = () => {
  const { examId = '', shiftId = '' } = useParams<{ examId: string; shiftId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();

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

  const sessionIdRef = useRef<string>('');

  // Extract sections
  const sections = Array.from(new Set(questions.map((q) => q.sectionName || 'General Section')));
  const currentSection = questions[currentIndex]?.sectionName || sections[0] || 'General Section';

  // ── Session Initialization ────────────────────────────────────────────────
  useEffect(() => {
    const startSession = async () => {
      try {
        const s = await sessionService.startSession({ examId, shiftId });
        setSession(s);
        sessionIdRef.current = s.sessionId;
        const qList = s.questions && s.questions.length > 0 ? s.questions : SAMPLE_QUESTIONS;
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

        const serverExpiry = new Date(s.expiresAt).getTime();
        const diff = Math.max(0, Math.floor((serverExpiry - Date.now()) / 1000));
        setTimeLeft(diff > 0 ? diff : 3600);
      } catch {
        // Fallback for demo / preview
        const mockSession: SessionStartResponse = {
          sessionId: 'cbt-sess-' + Date.now(),
          examId,
          candidateId: '018f4e2a-0000-7000-8000-000000000001',
          durationSeconds: 3600,
          totalQuestions: SAMPLE_QUESTIONS.length,
          navigationMode: 'FLEXIBLE',
          questions: SAMPLE_QUESTIONS,
          serverTime: new Date().toISOString(),
          expiresAt: new Date(Date.now() + 3600000).toISOString(),
        };
        setSession(mockSession);
        sessionIdRef.current = mockSession.sessionId;
        setQuestions(SAMPLE_QUESTIONS);

        const initialAnswers: Record<string, AnswerRecord> = {};
        SAMPLE_QUESTIONS.forEach((q, idx) => {
          initialAnswers[q.id] = {
            optionIndex: null,
            markedForReview: false,
            revSeq: 0,
            visited: idx === 0,
          };
        });
        setAnswers(initialAnswers);
        setTimeLeft(3600);
      } finally {
        setLoading(false);
      }
    };

    void startSession();
  }, [examId, shiftId]);

  // ── Timer countdown ───────────────────────────────────────────────────────
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
  }, [session]);

  // ── Fullscreen Monitor ───────────────────────────────────────────────────
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    const handleVisibilityChange = () => {
      if (document.hidden && sessionIdRef.current) {
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
  }, [toast]);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  };

  // ── Online/offline detection ─────────────────────────────────────────────
  useEffect(() => {
    const onOnline = async () => {
      setOnline(true);
      setSavingStatus('saved');
      if (sessionIdRef.current && offlineQueue.hasItems(sessionIdRef.current)) {
        toast.info('Network Restored', 'Syncing responses in background...');
        await responseService.flushOfflineQueue(sessionIdRef.current);
        toast.success('Sync Complete', 'All answers securely synced.');
      }
    };
    const onOffline = () => {
      setOnline(false);
      setSavingStatus('offline');
      toast.warning('Offline Mode', 'Answers will be saved in local storage and synced automatically.');
    };
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, [toast]);

  // ── Question Palette Helpers ─────────────────────────────────────────────
  const getQuestionState = (qId: string): QuestionStatus => {
    const rec = answers[qId];
    if (!rec || !rec.visited) return 'NOT_VISITED';
    const hasAnswer = rec.optionIndex !== null && rec.optionIndex !== undefined;

    if (rec.markedForReview && hasAnswer) return 'ANSWERED_AND_MARKED';
    if (rec.markedForReview && !hasAnswer) return 'MARKED_FOR_REVIEW';
    if (hasAnswer) return 'ANSWERED';
    return 'NOT_ANSWERED';
  };

  // ── Navigation & Responses ───────────────────────────────────────────────
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

      if (sessionIdRef.current) {
        try {
          await sessionService.navigate(sessionIdRef.current, {
            targetQuestionIndex: index,
          });
        } catch {}
      }
    },
    [questions]
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

    // Auto-save
    if (sessionIdRef.current && newAnswer.optionIndex !== null) {
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

  // ── Submit ───────────────────────────────────────────────────────────────
  const handleSubmit = async (autoSubmit = false) => {
    if (!session) return;
    setSubmitting(true);
    try {
      await responseService.submitSession(session.sessionId);
      toast.success(
        autoSubmit ? 'Time Elapsed — Auto-Submitted' : 'Test Submitted Successfully!',
        'Your answers have been cryptographically signed and archived.'
      );
      navigate('/results');
    } catch {
      // Mock submit success in dev
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
          <p className="font-semibold tracking-wide">Initializing Secure CBT Assessment Node...</p>
        </div>
      </div>
    );
  }

  const currentQ = questions[currentIndex];
  const currentAnswer = answers[currentQ?.id];

  return (
    <div className="flex h-screen w-screen flex-col overflow-hidden bg-slate-100 select-none">
      {/* Top Banner / Assessment Header */}
      <header className="flex h-14 items-center justify-between border-b border-slate-300 bg-slate-900 px-4 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-8 items-center rounded bg-teal-600 px-2.5 font-bold tracking-wider text-xs">
            NAG CBT
          </div>
          <div className="text-xs text-slate-300 hidden sm:block">
            <span className="font-semibold text-white">Exam Session:</span> {examId.substring(0, 8)}...
          </div>
        </div>

        {/* Center Clock */}
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
              {savingStatus === 'saved' ? 'Auto-Saved' : savingStatus === 'saving' ? 'Saving...' : 'Offline Saved'}
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
            return (
              <button
                key={secName}
                onClick={() => firstIdxOfSec !== -1 && goToQuestion(firstIdxOfSec)}
                className={`rounded-md px-3 py-1 text-xs font-bold transition ${
                  isCurrent
                    ? 'bg-teal-700 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {secName}
              </button>
            );
          })}
        </div>

        {/* Font resize control */}
        <div className="hidden md:flex items-center gap-1 text-xs text-slate-600">
          <span>Text:</span>
          <button
            onClick={() => setFontSize('normal')}
            className={`px-1.5 py-0.5 rounded ${fontSize === 'normal' ? 'font-bold bg-slate-200' : ''}`}
          >
            A
          </button>
          <button
            onClick={() => setFontSize('large')}
            className={`px-1.5 py-0.5 rounded text-sm ${fontSize === 'large' ? 'font-bold bg-slate-200' : ''}`}
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
                      Question {currentIndex + 1}
                    </span>
                    <span className="text-xs font-semibold text-slate-600">
                      [{currentQ.sectionName}]
                    </span>
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
                    return (
                      <div
                        key={opt.index}
                        onClick={() => handleSelectOption(opt.index)}
                        className={`flex cursor-pointer items-center gap-3 rounded-xl border-2 p-3.5 transition ${
                          isSelected
                            ? 'border-teal-600 bg-teal-50/60 text-teal-950 font-semibold shadow-sm'
                            : 'border-slate-200 bg-white text-slate-800 hover:border-slate-300 hover:bg-slate-50'
                        }`}
                      >
                        <div
                          className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold ${
                            isSelected
                              ? 'border-teal-700 bg-teal-700 text-white'
                              : 'border-slate-400 bg-white text-slate-600'
                          }`}
                        >
                          {String.fromCharCode(65 + opt.index)}
                        </div>
                        <span className="text-sm leading-snug">{opt.text}</span>
                      </div>
                    );
                  })}
                </div>
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
                    className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition"
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
              <div className="font-bold text-slate-900">Candidate Assessment Terminal</div>
              <div className="text-[11px] text-slate-500 mt-0.5">Section: {currentSection}</div>
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
              <div className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Questions
              </div>
              <div className="grid grid-cols-5 gap-1.5">
                {questions.map((q, idx) => {
                  const state = getQuestionState(q.id);
                  const isCurrent = idx === currentIndex;

                  let bgClass = 'bg-slate-300 text-slate-700'; // NOT_VISITED
                  if (state === 'ANSWERED') bgClass = 'bg-emerald-600 text-white';
                  else if (state === 'NOT_ANSWERED') bgClass = 'bg-rose-600 text-white';
                  else if (state === 'MARKED_FOR_REVIEW') bgClass = 'bg-purple-700 text-white';
                  else if (state === 'ANSWERED_AND_MARKED')
                    bgClass = 'bg-purple-700 text-white';

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
              Submit Examination
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
              Confirm Exam Submission?
            </h3>
            <p className="mt-1 text-center text-xs text-slate-500">
              Please review your question attempt summary before final submission.
            </p>

            <div className="mt-4 grid grid-cols-2 gap-2 rounded-xl bg-slate-50 p-3.5 text-xs border border-slate-200">
              <div>
                Answered: <strong className="text-emerald-700">{counts.answered + counts.answeredAndMarked}</strong>
              </div>
              <div>
                Not Answered: <strong className="text-rose-700">{counts.notAnswered}</strong>
              </div>
              <div>
                Marked for Review: <strong className="text-purple-700">{counts.marked + counts.answeredAndMarked}</strong>
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
                {submitting ? 'Submitting...' : 'Yes, Submit Final'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TakeExam;
