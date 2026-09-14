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
  Languages,
} from 'lucide-react';
import { examService } from '../services/examService';
import { sessionService } from '../services/sessionService';
import { responseService } from '../services/responseService';
import { useToast } from '../components/Toast';
import { MathRenderer } from '../components/MathRenderer';
import { ImageZoomModal } from '../components/ImageZoomModal';
import { ALL_EXAM_LANGUAGES } from '../components/LanguageSelector';
import { ExamLanguageBanner } from '../components/ExamLanguageBanner';
import { offlineQueue } from '../utils/offlineQueue';
import {
  saveLanguagePreference,
  loadLanguagePreference,
  clearLanguagePreference,
  getCandidatePreferredRegionalLanguage,
} from '../utils/languagePreference';
import { FEATURE_FLAGS } from '../config/featureFlags';
import { OFFICIAL_EXAM_QUESTIONS } from '../data/examQuestions';
import type { ExaminationResponse, ExamLanguage, QuestionDto, QuestionOption, SessionStartResponse } from '../types/api';

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

const UUID_REGEX = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

const KNOWN_EXAM_TITLES: Record<string, string> = {
  'e1000000-0000-0000-0000-000000000001':
    'Staff Selection Commission Combined Graduate Level (SSC CGL) Tier-1 Examination 2026',
  'e1000000-0000-0000-0000-000000000002':
    'IBPS Probationary Officer (PO) Preliminary Examination 2026',
  'e1000000-0000-0000-0000-000000000003':
    'UPSC Civil Services (Preliminary) Examination 2026 - Paper 1 (General Studies)',
  'e1000000-0000-0000-0000-000000000004':
    'Railway Recruitment Board Non-Technical Popular Categories (RRB NTPC) CBT-1 2026',
  'e1000000-0000-0000-0000-000000000005':
    'Central Teacher Eligibility Test (CTET) Paper-1 (Primary Teacher) 2026',
  'e2000000-0000-0000-0000-000000000002':
    'Union Public Service Commission Civil Services Examination (Prelims) 2026',
  'e3000000-0000-0000-0000-000000000003':
    'Railway Recruitment Board Non-Technical Popular Categories (RRB NTPC) 2026',
  'e4000000-0000-0000-0000-000000000004':
    'IBPS Probationary Officers (PO) Preliminary Examination 2026',
  'e5000000-0000-0000-0000-000000000005':
    'National Eligibility cum Entrance Test (NEET UG) 2026',
};

const generateUUID = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const TakeExam: React.FC = () => {
  const { examId = '', shiftId = '' } = useParams<{ examId: string; shiftId: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [zoomImage, setZoomImage] = useState<{src: string; alt: string} | null>(null);
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
  const [isOfflineSession, setIsOfflineSession] = useState<boolean>(false);

  // Practice & Learning Mode Controls (Gated by Feature Flag)
  const [isPracticeMode, setIsPracticeMode] = useState<boolean>(false);
  const [showExplanation, setShowExplanation] = useState<boolean>(false);
  const [filterBySection, setFilterBySection] = useState<boolean>(true);
  // --- Candidate Language Medium State (English + 1 Preferred Indian Language) ---
  const [preferredRegionalCode] = useState<string>(() =>
    getCandidatePreferredRegionalLanguage()
  );

  const preferredRegionalLang =
    ALL_EXAM_LANGUAGES.find((l) => l.code === preferredRegionalCode) ??
    ALL_EXAM_LANGUAGES.find((l) => l.code === 'hi') ??
    ALL_EXAM_LANGUAGES[1];

  // activeLanguage: current language view ('en' or preferredRegionalLang)
  const [activeLanguage, setActiveLanguage] = useState<ExamLanguage>(preferredRegionalLang);
  // isBilingual: show English master reference + regional translation
  const [isBilingual, setIsBilingual] = useState<boolean>(true);

  // --- Concurrent Active Session Conflict State ---
  const [concurrentConflict, setConcurrentConflict] = useState<boolean>(false);
  const [conflictLoading, setConflictLoading] = useState<boolean>(false);

  const sessionIdRef = useRef<string>('');
  const isOfflineSessionRef = useRef<boolean>(false);


  // ─── Multilingual Content Resolvers ────────────────────────────
  const hasTranslation = useCallback(
    (q?: QuestionDto, langCode?: string): boolean => {
      if (!q || !q.translations) return false;
      const code = langCode || activeLanguage.code;
      if (code === 'en') return false;
      const trans = q.translations[code];
      if (!trans) return false;
      return Boolean(trans.content || trans.text || (trans.options && trans.options.length > 0));
    },
    [activeLanguage.code]
  );

  const getEnglishContent = (q?: QuestionDto): string => {
    if (!q) return '';
    return q.content || q.text || '';
  };

  const getTranslatedContent = useCallback(
    (q?: QuestionDto, langCode?: string): string => {
      if (!q || !q.translations) return '';
      const code = langCode || activeLanguage.code;
      const trans = q.translations[code];
      return trans?.content || trans?.text || '';
    },
    [activeLanguage.code]
  );

  const getEnglishOptionText = (opt?: QuestionOption): string => {
    if (!opt) return '';
    return opt.text || opt.content || '';
  };

  const getTranslatedOptionText = useCallback(
    (q: QuestionDto, opt: QuestionOption, optIndex: number, langCode?: string): string => {
      if (!q || !q.translations) return '';
      const code = langCode || activeLanguage.code;
      const trans = q.translations[code];
      if (!trans || !trans.options) return '';

      const optionId = opt.id || String.fromCharCode(65 + optIndex);
      const match = trans.options.find(
        (o, idx) => (o.id && o.id === optionId) || o.index === optIndex || idx === optIndex
      );
      return match?.content || match?.text || '';
    },
    [activeLanguage.code]
  );

  const resolveExplanation = useCallback(
    (q: QuestionDto): string => {
      if (activeLanguage.code !== 'en' && q.translations?.[activeLanguage.code]?.explanation) {
        return q.translations[activeLanguage.code].explanation!;
      }
      return q.explanation || '';
    },
    [activeLanguage.code]
  );

  // Extract sections
  const sections = Array.from(new Set(questions.map((q) => q.sectionName || 'General Section')));
  const currentSection = questions[currentIndex]?.sectionName || sections[0] || 'General Section';

  // Questions for current section (when filtered)
  const currentSectionQuestionIndices = questions
    .map((q, idx) => ({ q, idx }))
    .filter(({ q }) => !filterBySection || q.sectionName === currentSection);

  // ─── Session Initialization from Examination & Delivery Services ─────────
  const initializeExamSession = useCallback(async (forceNew = false) => {
    try {
      setLoading(true);
      // 1. Fetch official exam metadata from examination-service
      let examInfo: ExaminationResponse | null = null;
      if (examId && UUID_REGEX.test(examId)) {
        try {
          examInfo = await examService.getExam(examId);
          setExamDetails(examInfo);
        } catch {
          // Non-blocking if examId is a custom mock/practice ID
        }
      }

      // Determine effective shift ID from candidate application / admit card
      let effectiveShiftId = shiftId && UUID_REGEX.test(shiftId) ? shiftId : undefined;
      if (!effectiveShiftId && examId && UUID_REGEX.test(examId)) {
        try {
          const app = await examService.getApplicationStatus(examId);
          if (app.allocatedShiftId && UUID_REGEX.test(app.allocatedShiftId)) {
            effectiveShiftId = app.allocatedShiftId;
          } else if (app.preferredShiftId && UUID_REGEX.test(app.preferredShiftId)) {
            effectiveShiftId = app.preferredShiftId;
          }
        } catch {
          // Non-blocking
        }
      }
      if (!effectiveShiftId && examId && UUID_REGEX.test(examId)) {
        try {
          const card = await examService.getAdmitCard(examId);
          if (card?.shiftId && UUID_REGEX.test(card.shiftId)) {
            effectiveShiftId = card.shiftId;
          }
        } catch {
          // Non-blocking
        }
      }

      // 2. Start session or resume existing active session via delivery-service
      let s: SessionStartResponse | null = null;
      try {
        const targetExamId =
          examId && UUID_REGEX.test(examId)
            ? examId
            : 'e1000000-0000-0000-0000-000000000001';
        s = await sessionService.startSession({
          examId: targetExamId,
          ...(effectiveShiftId ? { shiftId: effectiveShiftId } : {}),
          languageCode: activeLanguage.code || 'en',
          ...(forceNew ? { forceNewSession: true, terminateExisting: true } : {}),
        });
        const resolvedTitle =
          examInfo?.name || examInfo?.title ||
          (targetExamId && KNOWN_EXAM_TITLES[targetExamId]) ||
          s.examTitle ||
          'National Assessment Grid Examination';
        s.examTitle = resolvedTitle;
        if (examInfo?.durationMinutes) {
          s.durationSeconds = examInfo.durationMinutes * 60;
        }
        setSession(s);
        sessionIdRef.current = s.sessionId;
        setIsOfflineSession(false);
        isOfflineSessionRef.current = false;
        setConcurrentConflict(false);
      } catch (err: any) {
        const errMsg = err?.response?.data?.error?.message || err?.response?.data?.detail || err?.message || '';
        const status = err?.response?.status;
        if (
          status === 409 ||
          errMsg.includes('already has an active session') ||
          errMsg.includes('ConcurrentSessionException') ||
          errMsg.includes('CONCURRENT_SESSION')
        ) {
          setConcurrentConflict(true);
          setLoading(false);
          return;
        }

        // Construct offline/practice session with a valid UUID
        const fallbackExamId =
          examId && UUID_REGEX.test(examId)
            ? examId
            : 'e1000000-0000-0000-0000-000000000001';
        const mockSessionId = generateUUID();
        s = {
          sessionId: mockSessionId,
          examId: fallbackExamId,
          examTitle:
            examInfo?.name || examInfo?.title ||
            (fallbackExamId && KNOWN_EXAM_TITLES[fallbackExamId]) ||
            'National Assessment Grid Examination',
          candidateId: '018f4e2a-0000-7000-8000-000000000001',
          durationSeconds: (examInfo?.durationMinutes ?? 60) * 60,
          totalQuestions: OFFICIAL_EXAM_QUESTIONS.length,
          navigationMode: 'FLEXIBLE',
          questions: OFFICIAL_EXAM_QUESTIONS,
          serverTime: new Date().toISOString(),
          expiresAt: new Date(Date.now() + (examInfo?.durationMinutes ?? 60) * 60000).toISOString(),
        };
        setSession(s);
        sessionIdRef.current = mockSessionId;
        setIsOfflineSession(true);
        isOfflineSessionRef.current = true;
      }

      const qList =
        s.questions && s.questions.length > 0 ? s.questions : OFFICIAL_EXAM_QUESTIONS;
      setQuestions(qList);

      // ── Multilingual: resolve candidate preferred language ──
      if (FEATURE_FLAGS.ENABLE_MULTILINGUAL) {
        const persisted = loadLanguagePreference(s.sessionId);
        const candidatePref = getCandidatePreferredRegionalLanguage();
        const defaultCode = persisted ?? (s.defaultLanguageCode && s.defaultLanguageCode !== 'en' ? s.defaultLanguageCode : candidatePref) ?? 'hi';
        const resolvedLang =
          ALL_EXAM_LANGUAGES.find((l) => l.code === defaultCode) ?? ALL_EXAM_LANGUAGES[0];
        setActiveLanguage(resolvedLang);
      }

      // Initialize question state dictionary
      const initialAnswers: Record<string, AnswerRecord> = {};
      qList.forEach((q, idx) => {
        initialAnswers[q.id] = {
          optionIndex: null,
          markedForReview: false,
          revSeq: 0,
          visited: idx === 0,
        };
      });

      // 3. Restore previously saved answers if resuming an existing active session
      if (s.sessionId && UUID_REGEX.test(s.sessionId) && !isOfflineSessionRef.current) {
        try {
          const savedResponses = await responseService.getSessionResponses(s.sessionId);
          if (savedResponses && savedResponses.length > 0) {
            savedResponses.forEach((resp) => {
              if (resp.questionId && initialAnswers[resp.questionId]) {
                initialAnswers[resp.questionId] = {
                  optionIndex: resp.selectedOptionIndex !== undefined ? resp.selectedOptionIndex : null,
                  markedForReview: !!resp.markedForReview,
                  revSeq: resp.revisionSequence || 1,
                  visited: true,
                };
              }
            });
            toast.info('Session Resumed', `Restored ${savedResponses.length} previously saved answer(s).`);
          }
        } catch {
          // Non-blocking response restoration
        }
      }

      setAnswers(initialAnswers);

      // Compute remaining duration accurately based on scheduledEndAt
      let totalSec = s.durationSeconds || (examInfo?.durationMinutes ? examInfo.durationMinutes * 60 : 3600);
      if (s.scheduledEndAt) {
        const endMs = new Date(s.scheduledEndAt).getTime();
        const serverMs = s.serverTime ? new Date(s.serverTime).getTime() : Date.now();
        const diffSec = Math.floor((endMs - serverMs) / 1000);
        if (diffSec > 0 && diffSec < totalSec) {
          totalSec = diffSec;
        }
      }
      setTimeLeft(totalSec);
    } catch {
      setQuestions(OFFICIAL_EXAM_QUESTIONS);
    } finally {
      setLoading(false);
    }
  }, [examId, shiftId, activeLanguage.code, toast]);

  useEffect(() => {
    void initializeExamSession(false);
  }, [initializeExamSession]);

  // Reset explanation view when question index changes
  useEffect(() => {
    setShowExplanation(false);
  }, [currentIndex]);

  // ─── Timer countdown ──────────────────────────────────────────────────────
  useEffect(() => {
    if (!session || timeLeft <= 0 || (isPracticeMode && FEATURE_FLAGS.ENABLE_PRACTICE_MODE)) return;
    const interval = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          void handleSubmit(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [session, isPracticeMode]);

  // ─── Fullscreen & Invigilation Telemetry ──────────────────────────────────
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    const handleVisibilityChange = () => {
      if (
        document.hidden &&
        sessionIdRef.current &&
        !isPracticeMode &&
        !isOfflineSessionRef.current &&
        UUID_REGEX.test(sessionIdRef.current)
      ) {
        void sessionService.recordFullScreenExit(sessionIdRef.current).catch(() => {});
        toast.warning(
          'Security Alert',
          'Tab switch detected. This event has been logged to the invigilation audit grid.'
        );
      }
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [isPracticeMode, toast]);

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      void document.documentElement.requestFullscreen().catch(() => {});
    } else {
      void document.exitFullscreen().catch(() => {});
    }
  };

  // ─── Online/offline detection & Sync ──────────────────────────────────────
  useEffect(() => {
    const onOnline = async () => {
      setOnline(true);
      setSavingStatus('saved');
      if (
        sessionIdRef.current &&
        !isOfflineSessionRef.current &&
        UUID_REGEX.test(sessionIdRef.current) &&
        offlineQueue.hasItems(sessionIdRef.current)
      ) {
        toast.info('Network Restored', 'Syncing responses to response-service in background...');
        await responseService.flushOfflineQueue(sessionIdRef.current);
        toast.success('Sync Complete', 'All answers securely synced.');
      }
    };
    const onOffline = () => {
      setOnline(false);
      setSavingStatus('offline');
      toast.warning('Network Lost', 'Working in secure offline buffer mode. Answers are safely held locally.');
    };

    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);

    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, [toast]);

  // ─── Question Palette Helpers ─────────────────────────────────────────────
  const getQuestionState = (qId: string): QuestionStatus => {
    const rec = answers[qId];
    if (!rec || !rec.visited) return 'NOT_VISITED';
    if (rec.optionIndex !== null && rec.markedForReview) return 'ANSWERED_AND_MARKED';
    if (rec.markedForReview) return 'MARKED_FOR_REVIEW';
    if (rec.optionIndex !== null) return 'ANSWERED';
    return 'NOT_ANSWERED';
  };

  // ─── Navigation & Responses via delivery-service & response-service ───────
  const goToQuestion = useCallback(
    async (index: number) => {
      if (index < 0 || index >= questions.length) return;
      const targetQuestion = questions[index];

      // Mark question visited
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

      if (
        sessionIdRef.current &&
        !isPracticeMode &&
        !isOfflineSessionRef.current &&
        UUID_REGEX.test(sessionIdRef.current)
      ) {
        try {
          await sessionService.navigate(sessionIdRef.current, {
            targetQuestionIndex: index,
          });
        } catch {
          // Non-blocking navigation telemetry
        }
      }
    },
    [questions, isPracticeMode]
  );

  const handleSelectOption = (optionIndex: number) => {
    const q = questions[currentIndex];
    if (!q) return;

    const currentRecord = answers[q.id] || {
      optionIndex: null,
      markedForReview: false,
      revSeq: 0,
      visited: true,
    };

    const newIndex = currentRecord.optionIndex === optionIndex ? null : optionIndex;

    setAnswers((prev) => ({
      ...prev,
      [q.id]: {
        ...currentRecord,
        optionIndex: newIndex,
        visited: true,
      },
    }));
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

    const currentRecord = answers[q.id] || {
      optionIndex: null,
      markedForReview: false,
      revSeq: 0,
      visited: true,
    };

    const newAnswer: AnswerRecord = {
      ...currentRecord,
      markedForReview: markReview,
      revSeq: currentRecord.revSeq + 1,
      visited: true,
    };

    setAnswers((prev) => ({
      ...prev,
      [q.id]: newAnswer,
    }));

    // Auto-save response via response-service
    if (
      sessionIdRef.current &&
      newAnswer.optionIndex !== null &&
      !isPracticeMode &&
      !isOfflineSessionRef.current &&
      UUID_REGEX.test(sessionIdRef.current)
    ) {
      setSavingStatus('saving');
      try {
        await responseService.saveResponse(sessionIdRef.current, {
          questionId: q.id,
          responseType: 'MCQ',
          selectedOptionIndex: newAnswer.optionIndex,
          markedForReview: newAnswer.markedForReview,
          revisionSequence: newAnswer.revSeq,
          timeTakenSeconds: 5,
        });
        setSavingStatus('saved');
      } catch {
        setSavingStatus('offline');
      }
    }

    // Move to next question if available
    if (currentIndex < questions.length - 1) {
      await goToQuestion(currentIndex + 1);
    }
  };

  // ─── Final Submission via response-service ────────────────────────────────
  const handleSubmit = async (autoSubmit = false) => {
    if (!session) return;
    setSubmitting(true);
    try {
      if (
        !isPracticeMode &&
        !isOfflineSessionRef.current &&
        UUID_REGEX.test(session.sessionId)
      ) {
        await responseService.submitSession(session.sessionId);
      }
      // Clear persisted language preference after submission
      clearLanguagePreference(session.sessionId);
      toast.success(
        autoSubmit ? 'Time Expired - Exam Auto-Submitted' : 'Exam Submitted Successfully',
        'Your responses have been sealed and transmitted to evaluation-service.'
      );
      navigate('/dashboard');
    } catch {
      toast.error('Submission Failed', 'Could not seal responses. Retrying offline submission...');
    } finally {
      setSubmitting(false);
      setShowConfirm(false);
    }
  };

  const formatTimer = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
      return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // Aggregated Counts for Question Palette Summary
  const counts = {
    answered: 0,
    notAnswered: 0,
    markedForReview: 0,
    answeredAndMarked: 0,
    notVisited: 0,
  };

  questions.forEach((q) => {
    const state = getQuestionState(q.id);
    if (state === 'NOT_VISITED') counts.notVisited++;
    else if (state === 'NOT_ANSWERED') counts.notAnswered++;
    else if (state === 'ANSWERED') counts.answered++;
    else if (state === 'MARKED_FOR_REVIEW') counts.markedForReview++;
    else if (state === 'ANSWERED_AND_MARKED') counts.answeredAndMarked++;
  });

  const displayExamTitle =
    examDetails?.name ||
    examDetails?.title ||
    (examId && KNOWN_EXAM_TITLES[examId]) ||
    session?.examTitle ||
    'National Assessment Grid Examination';

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-teal-500 border-t-transparent" />
          <p className="text-sm text-slate-300 font-medium">
            Initializing Secure Delivery Node & Decrypting Exam Package...
          </p>
        </div>
      </div>
    );
  }

  const currentQ = questions[currentIndex];
  const currentAnswer = currentQ ? answers[currentQ.id] : null;
  const isAnswered = currentAnswer?.optionIndex !== null && currentAnswer?.optionIndex !== undefined;
  const regionalLangForDisplay =
    activeLanguage.code !== 'en' ? activeLanguage : preferredRegionalLang;
  const hasTrans = currentQ ? hasTranslation(currentQ, regionalLangForDisplay.code) : false;
  const isBilingualModeActive = isBilingual && hasTrans;

  const showPracticeTools = FEATURE_FLAGS.ENABLE_PRACTICE_MODE && isPracticeMode;

  return (
    <div className="flex h-screen flex-col bg-slate-100 font-sans select-none">
      {/* Concurrent Active Session Conflict Modal */}
      {concurrentConflict && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl border border-amber-200 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center gap-3 text-amber-600 mb-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 shrink-0">
                <AlertTriangle className="h-6 w-6 text-amber-600" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-gray-900">Active Exam Session Conflict</h2>
                <p className="text-xs text-amber-700 font-medium">सक्रिय परीक्षा सत्र संघर्ष (Concurrent Session)</p>
              </div>
            </div>

            <div className="space-y-3 text-sm text-gray-600">
              <p>
                You already have an active exam session in progress for another examination. Under official examination rules, only one active examination session is permitted at a time.
              </p>
              <div className="rounded-lg bg-amber-50 p-3 text-xs text-amber-900 border border-amber-200">
                <strong>Action Required:</strong> You can terminate your old ongoing session to start this exam, or return to the candidate dashboard.
              </div>
            </div>

            <div className="mt-6 flex flex-col-reverse sm:flex-row sm:justify-end gap-3">
              <button
                type="button"
                onClick={() => navigate('/candidate/exams')}
                className="w-full sm:w-auto rounded-xl border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition cursor-pointer"
              >
                Return to Dashboard
              </button>

              <button
                type="button"
                disabled={conflictLoading}
                onClick={async () => {
                  setConflictLoading(true);
                  try {
                    await initializeExamSession(true);
                  } catch (e: any) {
                    toast.error('Session Error', e?.message || 'Failed to start session');
                  } finally {
                    setConflictLoading(false);
                  }
                }}
                className="w-full sm:w-auto rounded-xl bg-red-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-red-700 transition flex items-center justify-center gap-2 cursor-pointer"
              >
                <RotateCcw className={`h-4 w-4 ${conflictLoading ? 'animate-spin' : ''}`} />
                <span>{conflictLoading ? 'Terminating & Starting...' : 'End Old Session & Start New'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Top Bar Header */}
      <header className="flex h-14 items-center justify-between border-b border-slate-700 bg-slate-900 px-4 text-white shadow-md">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-teal-600 font-bold text-white shadow">
            NAG
          </div>
          <div className="text-xs text-slate-300 hidden sm:block">
            <span className="font-semibold text-white">
              {displayExamTitle}
            </span>
          </div>

          {/* Interactive Mode Toggle Badge */}
          {FEATURE_FLAGS.ENABLE_PRACTICE_MODE && (
            <button
              onClick={() => setIsPracticeMode(!isPracticeMode)}
              className={`ml-2 flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold transition ${
                isPracticeMode
                  ? 'bg-amber-400 text-amber-950 shadow-sm ring-1 ring-amber-300'
                  : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
              }`}
              title="Toggle between Strict Official Exam Mode and Interactive Practice / Learning Mode"
            >
              <GraduationCap className="h-3.5 w-3.5" />
              <span>{isPracticeMode ? 'Practice Mode (Active)' : 'Official Exam Mode'}</span>
            </button>
          )}

          {/* Candidate Medium: English + 1 Selected Regional Language */}
          {FEATURE_FLAGS.ENABLE_MULTILINGUAL && (
            <div className="ml-2 flex items-center gap-2">
              {/* English vs Preferred Regional Medium Toggle */}
              <div className="flex items-center gap-1 rounded-full border border-slate-600 bg-slate-800 p-0.5 text-xs font-bold shadow-xs">
                <button
                  onClick={() => {
                    setActiveLanguage(ALL_EXAM_LANGUAGES[0]);
                    if (sessionIdRef.current) saveLanguagePreference(sessionIdRef.current, 'en');
                  }}
                  title="View in English (Master Reference)"
                  className={`rounded-full px-3 py-1 transition cursor-pointer ${
                    activeLanguage.code === 'en'
                      ? 'bg-teal-600 text-white shadow-xs'
                      : 'text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  English
                </button>

                <button
                  onClick={() => {
                    setActiveLanguage(preferredRegionalLang);
                    if (sessionIdRef.current) saveLanguagePreference(sessionIdRef.current, preferredRegionalLang.code);
                  }}
                  title={`View in ${preferredRegionalLang.name} (${preferredRegionalLang.nativeName})`}
                  className={`rounded-full px-3 py-1 transition cursor-pointer ${
                    activeLanguage.code === preferredRegionalLang.code
                      ? 'bg-teal-600 text-white shadow-xs'
                      : 'text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  {preferredRegionalLang.nativeName} ({preferredRegionalLang.name})
                </button>


              </div>

              {/* Bilingual Mode Toggle (English Master Reference + Candidate's Regional Medium) */}
              <button
                onClick={() => setIsBilingual((v) => !v)}
                title={`Toggle Bilingual delivery (English Reference + ${preferredRegionalLang.name})`}
                className={`flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-bold transition cursor-pointer ${
                  isBilingual
                    ? 'border-teal-500/70 bg-teal-950/70 text-teal-300 shadow-xs'
                    : 'border-slate-700 bg-slate-800 text-slate-400 hover:bg-slate-700'
                }`}
              >
                <Languages className="h-3.5 w-3.5 text-teal-400" />
                <span>
                  {isBilingual ? `Bilingual (EN + ${preferredRegionalLang.code.toUpperCase()})` : `Single (${activeLanguage.code.toUpperCase()})`}
                </span>
              </button>
            </div>
          )}
        </div>

        {/* Center / Right: Invigilation Timer & Statuses */}
        <div className="flex items-center gap-3">
          {/* Real-time countdown */}
          <div
            className={`flex items-center gap-2 rounded-lg px-3 py-1 font-mono text-sm font-bold shadow-xs ${
              timeLeft < 300
                ? 'bg-rose-600 text-white animate-pulse'
                : 'bg-slate-800 text-teal-300 border border-slate-700'
            }`}
          >
            <Clock className="h-4 w-4" />
            <span>{formatTimer(timeLeft)}</span>
          </div>

          {/* Connectivity Status */}
          <span
            className={`flex items-center gap-1 rounded px-2 py-0.5 text-[10px] font-semibold ${
              online
                ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                : 'bg-rose-950 text-rose-300 border border-rose-800'
            }`}
          >
            <span
              className={`h-1.5 w-1.5 rounded-full ${online ? 'bg-emerald-400' : 'bg-rose-400 animate-ping'}`}
            />
            <span>{online ? 'Online' : 'Offline'}</span>
          </span>

          {/* Autosave Status */}
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
              {isOfflineSession
                ? 'Practice Ready'
                : savingStatus === 'saved'
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

      {/* Regulatory Disclaimer Banner — shown when viewing regional language */}
      {FEATURE_FLAGS.ENABLE_MULTILINGUAL && (
        <ExamLanguageBanner
          activeLanguage={activeLanguage}
          onSwitchToEnglish={() => {
            setActiveLanguage(ALL_EXAM_LANGUAGES[0]);
                        if (sessionIdRef.current) {
              saveLanguagePreference(sessionIdRef.current, 'en');
            }
          }}
        />
      )}

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

                {/* Question Text: Bilingual Mode (English Reference + Regional Card) vs Single Mode */}
                <div className="mt-4">
                  {isBilingualModeActive ? (
                    <div className="space-y-4">
                      {/* English Master Reference Card */}
                      <div className="rounded-xl border border-sky-200 bg-sky-50/40 p-4 shadow-2xs">
                        <div className="inline-block rounded-full bg-sky-100 border border-sky-300 px-2.5 py-0.5 text-[10px] font-bold text-sky-800 uppercase tracking-wider mb-2">
                          English (Master Reference)
                        </div>
                        <div
                          className={`text-slate-900 leading-relaxed font-medium ${
                            fontSize === 'large' ? 'text-lg' : 'text-base'
                          }`}
                        >
                          <MathRenderer content={getEnglishContent(currentQ)} />
                        </div>
                      </div>

                      {/* Selected Regional Language Card */}
                      <div
                        className="rounded-xl border border-amber-200 bg-amber-50/40 p-4 shadow-2xs"
                        dir={regionalLangForDisplay.rtl ? 'rtl' : 'ltr'}
                      >
                        <div className="inline-block rounded-full bg-amber-100 border border-amber-300 px-2.5 py-0.5 text-[10px] font-bold text-amber-900 uppercase tracking-wider mb-2">
                          {regionalLangForDisplay.name} ({regionalLangForDisplay.nativeName})
                        </div>
                        <div
                          className={`text-slate-900 leading-relaxed font-medium ${
                            fontSize === 'large' ? 'text-lg' : 'text-base'
                          }`}
                        >
                          <MathRenderer content={getTranslatedContent(currentQ, regionalLangForDisplay.code)} />
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="rounded-xl border border-slate-100 bg-white p-2">
                      {activeLanguage.code !== 'en' && hasTrans && (
                        <div className="inline-block rounded-full bg-amber-100 border border-amber-300 px-2.5 py-0.5 text-[10px] font-bold text-amber-900 uppercase tracking-wider mb-2">
                          {activeLanguage.name} ({activeLanguage.nativeName})
                        </div>
                      )}
                      <div
                        className={`text-slate-900 leading-relaxed font-medium ${
                          fontSize === 'large' ? 'text-lg' : 'text-base'
                        }`}
                        dir={activeLanguage.rtl ? 'rtl' : 'ltr'}
                      >
                        <MathRenderer
                          content={
                            activeLanguage.code !== 'en' && hasTrans
                              ? getTranslatedContent(currentQ)
                              : getEnglishContent(currentQ)
                          }
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* Options List with Markdown & LaTeX Rendering */}
                {(() => {
                  const hasImageOptions = currentQ?.options?.some((opt: any) => opt.imageUrl);
                  return (
                    <div className={`mt-6 ${hasImageOptions ? 'grid grid-cols-2 gap-3' : 'space-y-3'}`}>
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

                        const optTransText = getTranslatedOptionText(
                          currentQ,
                          opt,
                          opt.index,
                          regionalLangForDisplay.code
                        );

                        return (
                          <div
                            key={opt.index}
                            onClick={() => handleSelectOption(opt.index)}
                            className={`flex flex-col cursor-pointer justify-between rounded-xl border-2 p-3.5 transition ${optionBorderClass}`}
                          >
                            <div className="flex items-start justify-between gap-3">
                              <div className="flex items-start gap-3 flex-1">
                                <div
                                  className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold mt-0.5 ${letterClass}`}
                                >
                                  {opt.id || String.fromCharCode(65 + opt.index)}
                                </div>

                                {isBilingualModeActive ? (
                                  <div className="flex-1 space-y-1.5">
                                    {/* English Option Row */}
                                    <div className="flex items-baseline gap-2 text-slate-800 text-sm font-medium">
                                      <span className="text-[10px] font-bold text-sky-700 uppercase shrink-0 bg-sky-50 px-1 rounded border border-sky-200">
                                        EN:
                                      </span>
                                      <MathRenderer content={getEnglishOptionText(opt)} inline />
                                    </div>
                                    {/* Regional Option Row */}
                                    {optTransText && (
                                      <div
                                        className="flex items-baseline gap-2 text-slate-900 text-sm font-semibold border-t border-slate-100 pt-1.5"
                                        dir={activeLanguage.rtl ? 'rtl' : 'ltr'}
                                      >
                                        <span className="text-[10px] font-bold text-amber-800 uppercase shrink-0 bg-amber-50 px-1 rounded border border-amber-200">
                                          {activeLanguage.code.toUpperCase()}:
                                        </span>
                                        <MathRenderer content={optTransText} inline />
                                      </div>
                                    )}
                                  </div>
                                ) : (
                                  <div className="text-sm leading-snug" dir={activeLanguage.rtl ? 'rtl' : 'ltr'}>
                                    <MathRenderer
                                      content={
                                        activeLanguage.code !== 'en' && hasTrans && optTransText
                                          ? optTransText
                                          : getEnglishOptionText(opt)
                                      }
                                      inline
                                    />
                                  </div>
                                )}
                              </div>

                              {showCorrectness && isCorrect && (
                                <span className="flex items-center gap-1 text-xs font-bold text-emerald-700 shrink-0">
                                  <Check className="h-4 w-4" /> Correct Answer
                                </span>
                              )}
                              {showCorrectness && isSelected && !isCorrect && (
                                <span className="flex items-center gap-1 text-xs font-bold text-rose-600 shrink-0">
                                  <X className="h-4 w-4" /> Your Selection
                                </span>
                              )}
                            </div>
                            {opt.imageUrl && (
                              <div className="relative mt-3">
                                <img
                                  src={opt.imageUrl}
                                  alt={opt.imageAltText || `Option ${opt.index}`}
                                  className="w-full h-auto rounded border cursor-zoom-in max-h-48 object-contain bg-gray-50"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    setZoomImage({ src: opt.imageUrl!, alt: opt.imageAltText || `Option ${opt.index}` });
                                  }}
                                />
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  );
                })()}

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
                        <div className="mt-2 text-xs leading-relaxed text-slate-800 font-medium">
                          <MathRenderer
                            content={
                              resolveExplanation(currentQ) ||
                              'The correct answer is Option ' +
                                String.fromCharCode(65 + (currentQ.correctOptionIndex ?? 0)) +
                                '.'
                            }
                          />
                        </div>
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
                    className="inline-flex items-center gap-1 rounded-lg border border-slate-300 px-3.5 py-2 text-xs font-bold text-slate-700 hover:bg-slate-100 disabled:opacity-30 transition"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    <span>Previous</span>
                  </button>
                  <button
                    onClick={() => handleSaveAndNext(false)}
                    className="inline-flex items-center gap-1 rounded-lg bg-teal-700 px-4 py-2 text-xs font-bold text-white hover:bg-teal-800 transition shadow-sm"
                  >
                    <span>Save & Next</span>
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Right: Question Palette & Candidate Profile Panel */}
        <div className="flex w-80 shrink-0 flex-col bg-slate-50">
          {/* Candidate Profile Box */}
          <div className="flex items-center gap-3 border-b border-slate-200 bg-white p-3 shadow-xs">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-teal-100 text-teal-900 font-black text-sm">
              CA
            </div>
            <div className="overflow-hidden">
              <p className="text-xs font-bold text-slate-900 truncate">Candidate Portal</p>
              <p className="text-[11px] font-mono text-slate-500">Roll: NAG-2026-0814</p>
            </div>
          </div>

          {/* Palette Legend */}
          <div className="p-3 border-b border-slate-200 bg-slate-100/70 text-[11px] space-y-1.5">
            <div className="grid grid-cols-2 gap-2">
              <div className="flex items-center gap-1.5">
                <span className="h-5 w-5 flex items-center justify-center rounded bg-emerald-600 text-white font-bold text-[10px]">
                  {counts.answered}
                </span>
                <span className="text-slate-700 font-medium">Answered</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-5 w-5 flex items-center justify-center rounded bg-rose-500 text-white font-bold text-[10px]">
                  {counts.notAnswered}
                </span>
                <span className="text-slate-700 font-medium">Not Answered</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-5 w-5 flex items-center justify-center rounded bg-purple-600 text-white font-bold text-[10px]">
                  {counts.markedForReview}
                </span>
                <span className="text-slate-700 font-medium">Marked Review</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="h-5 w-5 flex items-center justify-center rounded bg-indigo-700 text-white font-bold text-[10px]">
                  {counts.answeredAndMarked}
                </span>
                <span className="text-slate-700 font-medium">Ans & Marked</span>
              </div>
            </div>
          </div>

          {/* Section Filter Toggle in Palette */}
          <div className="flex items-center justify-between px-3 py-1.5 bg-slate-200/60 border-b border-slate-200 text-xs">
            <span className="font-bold text-slate-700 flex items-center gap-1">
              <Filter className="h-3 w-3 text-slate-500" />
              <span>Palette Scope</span>
            </span>
            <button
              onClick={() => setFilterBySection(!filterBySection)}
              className="text-[11px] font-semibold text-teal-800 hover:underline"
            >
              {filterBySection ? 'Show All 100 Questions' : 'Filter by Section'}
            </button>
          </div>

          {/* Questions Grid */}
          <div className="flex-1 overflow-y-auto p-3">
            <div className="grid grid-cols-5 gap-2">
              {currentSectionQuestionIndices.map(({ q, idx }) => {
                const state = getQuestionState(q.id);
                const isCurrent = idx === currentIndex;

                let bgClass = 'bg-slate-200 text-slate-700 hover:bg-slate-300';
                if (state === 'ANSWERED') bgClass = 'bg-emerald-600 text-white shadow-xs';
                else if (state === 'NOT_ANSWERED') bgClass = 'bg-rose-500 text-white shadow-xs';
                else if (state === 'MARKED_FOR_REVIEW') bgClass = 'bg-purple-600 text-white shadow-xs';
                else if (state === 'ANSWERED_AND_MARKED') bgClass = 'bg-indigo-700 text-white shadow-xs';

                return (
                  <button
                    key={q.id}
                    onClick={() => goToQuestion(idx)}
                    className={`flex h-9 items-center justify-center rounded-lg text-xs font-bold transition ${bgClass} ${
                      isCurrent ? 'ring-2 ring-slate-900 ring-offset-1 scale-105' : ''
                    }`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Final Submit Button */}
          <div className="border-t border-slate-200 bg-white p-3">
            <button
              onClick={() => setShowConfirm(true)}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 py-2.5 text-xs font-bold text-white hover:bg-emerald-800 transition shadow-sm"
            >
              <CheckCircle2 className="h-4 w-4" />
              <span>Submit Examination</span>
            </button>
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-xs p-4 animate-in fade-in">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
            <div className="flex items-center gap-3 text-amber-600">
              <AlertTriangle className="h-6 w-6" />
              <h3 className="text-base font-bold text-slate-900">Confirm Exam Submission</h3>
            </div>
            <p className="mt-2 text-xs text-slate-600 leading-relaxed">
              Are you sure you want to conclude and submit your examination? Once sealed, you will
              not be able to modify any responses.
            </p>

            <div className="mt-4 rounded-xl bg-slate-50 p-3 text-xs space-y-1.5 border border-slate-200">
              <div className="flex justify-between">
                <span className="text-slate-600">Total Questions:</span>
                <span className="font-bold text-slate-900">{questions.length}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Answered:</span>
                <span className="font-bold text-emerald-700">{counts.answered}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Not Answered / Unvisited:</span>
                <span className="font-bold text-rose-700">
                  {counts.notAnswered + counts.notVisited}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Marked for Review:</span>
                <span className="font-bold text-purple-700">
                  {counts.markedForReview + counts.answeredAndMarked}
                </span>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setShowConfirm(false)}
                disabled={submitting}
                className="rounded-lg border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100"
              >
                Resume Exam
              </button>
              <button
                onClick={() => handleSubmit(false)}
                disabled={submitting}
                className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-700 px-4 py-2 text-xs font-bold text-white hover:bg-emerald-800 disabled:opacity-50"
              >
                {submitting ? 'Sealing Responses...' : 'Yes, Submit Now'}
              </button>
            </div>
          </div>
        </div>
      )}

      <ImageZoomModal
        src={zoomImage?.src || ''}
        alt={zoomImage?.alt || ''}
        isOpen={zoomImage !== null}
        onClose={() => setZoomImage(null)}
      />
    </div>
  );
};

export default TakeExam;
