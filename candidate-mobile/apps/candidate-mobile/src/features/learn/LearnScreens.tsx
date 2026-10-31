import React, { useState, useEffect } from 'react';
import {
  LearnController,
  LearnState,
} from './learn-controller';
import {
  LearnerEducationLevel,
  LearnerPriorExperience,
  SUPPORTED_MICROLESSON_LANGUAGES,
  DiagnosticQuestion,
  FormulaEntry,
  StepItem,
  QuickCheckItem,
} from './learn-models';
import { FormulaRenderer } from '../formula';

// ============================================================================
// 1. Learner Onboarding Screen
// ============================================================================

export interface LearnerOnboardingScreenProps {
  controller: LearnController;
  onFinishOnboarding?: () => void;
}

export const LearnerOnboardingScreen: React.FC<LearnerOnboardingScreenProps> = ({
  controller,
  onFinishOnboarding,
}) => {
  const [state, setState] = useState<LearnState>(controller.getState());
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({});
  const [activeQuestionIndex, setActiveQuestionIndex] = useState<number>(0);

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const profile = state.profile;
  const currentStep = state.onboardingStep;

  // Step 1: Profile State
  const [eduLevel, setEduLevel] = useState<LearnerEducationLevel>(
    profile?.educationLevel || 'UNDERGRADUATE'
  );
  const [prefLang, setPrefLang] = useState<string>(profile?.preferredLanguage || 'en');
  const [studyMins, setStudyMins] = useState<number>(profile?.dailyStudyMinutes || 60);
  const [priorExp, setPriorExp] = useState<LearnerPriorExperience>(
    profile?.priorExperience || 'INTERMEDIATE'
  );

  const handleSaveProfile = () => {
    controller.updateProfile({
      educationLevel: eduLevel,
      preferredLanguage: prefLang,
      dailyStudyMinutes: studyMins,
      priorExperience: priorExp,
    });
  };

  const handleSelectExam = (examId: string) => {
    controller.selectExam(examId);
    setSelectedAnswers({});
    setActiveQuestionIndex(0);
  };

  const handleOptionSelect = (qId: string, optId: string) => {
    setSelectedAnswers((prev) => ({ ...prev, [qId]: optId }));
  };

  const handleSubmitDiagnostic = () => {
    const session = state.diagnosticSession;
    if (!session) return;
    const answers = Object.entries(selectedAnswers).map(([questionId, selectedOptionId]) => ({
      questionId,
      selectedOptionId,
    }));
    controller.submitDiagnostic(session.enrollmentId, { answers, isSkip: false });
  };

  const handleSkipDiagnostic = () => {
    const session = state.diagnosticSession;
    if (!session) return;
    controller.skipDiagnostic(session.enrollmentId);
  };

  const handleCompleteFlow = () => {
    controller.setActiveHubTab('recommendations');
    if (onFinishOnboarding) onFinishOnboarding();
  };

  return (
    <div
      data-testid="learner-onboarding-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Step Header */}
      <div style={{ marginBottom: '20px', borderBottom: '1px solid #E2E8F0', paddingBottom: '12px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A365D', margin: '0 0 4px 0' }}>
          Personalized Learning Onboarding
        </h2>
        <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
          <span
            data-testid="onboarding-step-pill-profile"
            style={{
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              backgroundColor: currentStep === 'profile' ? '#3182CE' : '#EDF2F7',
              color: currentStep === 'profile' ? '#FFFFFF' : '#4A5568',
            }}
          >
            1. Profile
          </span>
          <span
            data-testid="onboarding-step-pill-exam"
            style={{
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              backgroundColor: currentStep === 'exam-picker' ? '#3182CE' : '#EDF2F7',
              color: currentStep === 'exam-picker' ? '#FFFFFF' : '#4A5568',
            }}
          >
            2. Target Exam
          </span>
          <span
            data-testid="onboarding-step-pill-diagnostic"
            style={{
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              backgroundColor: currentStep === 'diagnostic' ? '#3182CE' : '#EDF2F7',
              color: currentStep === 'diagnostic' ? '#FFFFFF' : '#4A5568',
            }}
          >
            3. Diagnostic Assessment
          </span>
        </div>
      </div>

      {/* STEP 1: Profile Setup */}
      {currentStep === 'profile' && (
        <div data-testid="onboarding-profile-step" style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px' }}>
              Highest Education Level
            </label>
            <select
              data-testid="edu-level-select"
              value={eduLevel}
              onChange={(e) => setEduLevel(e.target.value as LearnerEducationLevel)}
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                border: '1px solid #CBD5E0',
                backgroundColor: '#FFFFFF',
              }}
            >
              <option value="HIGH_SCHOOL">High School (10+2)</option>
              <option value="UNDERGRADUATE">Undergraduate (College / B.Tech / B.Sc)</option>
              <option value="GRADUATE">Graduate (Degree Holder)</option>
              <option value="POSTGRADUATE">Postgraduate (Master&apos;s / PhD)</option>
              <option value="OTHER">Other Professional Credential</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px' }}>
              Preferred Learning Language
            </label>
            <select
              data-testid="pref-lang-select"
              value={prefLang}
              onChange={(e) => setPrefLang(e.target.value)}
              style={{
                width: '100%',
                padding: '10px',
                borderRadius: '8px',
                border: '1px solid #CBD5E0',
                backgroundColor: '#FFFFFF',
              }}
            >
              {SUPPORTED_MICROLESSON_LANGUAGES.map((lang) => (
                <option key={lang.code} value={lang.code}>
                  {lang.name} ({lang.nativeName})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px' }}>
              Daily Study Goal (Minutes)
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              {[30, 45, 60, 90, 120].map((mins) => (
                <button
                  key={mins}
                  type="button"
                  data-testid={`study-mins-btn-${mins}`}
                  onClick={() => setStudyMins(mins)}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: studyMins === mins ? '#3182CE' : '#CBD5E0',
                    backgroundColor: studyMins === mins ? '#EBF8FF' : '#FFFFFF',
                    color: studyMins === mins ? '#2B6CB0' : '#4A5568',
                    fontWeight: studyMins === mins ? '700' : '500',
                    cursor: 'pointer',
                  }}
                >
                  {mins}m
                </button>
              ))}
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '6px' }}>
              Prior Competitive Exam Preparation
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              {(['BEGINNER', 'INTERMEDIATE', 'ADVANCED'] as LearnerPriorExperience[]).map((exp) => (
                <button
                  key={exp}
                  type="button"
                  data-testid={`prior-exp-btn-${exp.toLowerCase()}`}
                  onClick={() => setPriorExp(exp)}
                  style={{
                    flex: 1,
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: priorExp === exp ? '#3182CE' : '#CBD5E0',
                    backgroundColor: priorExp === exp ? '#EBF8FF' : '#FFFFFF',
                    color: priorExp === exp ? '#2B6CB0' : '#4A5568',
                    fontWeight: priorExp === exp ? '700' : '500',
                    cursor: 'pointer',
                  }}
                >
                  {exp}
                </button>
              ))}
            </div>
          </div>

          <button
            data-testid="continue-to-exam-btn"
            onClick={handleSaveProfile}
            style={{
              marginTop: '12px',
              padding: '12px',
              backgroundColor: '#3182CE',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '8px',
              fontWeight: '700',
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            Save Profile & Choose Target Exam →
          </button>
        </div>
      )}

      {/* STEP 2: Target Exam Selection */}
      {currentStep === 'exam-picker' && (
        <div data-testid="onboarding-exam-step">
          <p style={{ fontSize: '13px', color: '#4A5568', marginBottom: '12px' }}>
            Select your primary target competitive exam. The adaptive learning path will calibrate its blueprint weights and mock difficulty accordingly.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {state.publishedExams.map((exam) => (
              <div
                key={exam.id}
                data-testid={`exam-card-${exam.id}`}
                style={{
                  border: '1px solid #E2E8F0',
                  borderRadius: '10px',
                  padding: '14px',
                  backgroundColor: '#FFFFFF',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h3 style={{ margin: '0 0 4px 0', fontSize: '15px', color: '#2D3748' }}>{exam.title}</h3>
                    <span style={{ fontSize: '12px', color: '#718096' }}>
                      {exam.conductingAuthority} • {exam.category}
                    </span>
                  </div>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      padding: '2px 8px',
                      borderRadius: '12px',
                      backgroundColor: '#EBF8FF',
                      color: '#2B6CB0',
                    }}
                  >
                    {exam.code}
                  </span>
                </div>
                <p style={{ fontSize: '12px', color: '#718096', margin: '8px 0 12px 0' }}>{exam.description}</p>
                <button
                  data-testid={`select-exam-btn-${exam.id}`}
                  onClick={() => handleSelectExam(exam.id)}
                  style={{
                    width: '100%',
                    padding: '10px',
                    backgroundColor: '#2B6CB0',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: '600',
                    fontSize: '13px',
                    cursor: 'pointer',
                  }}
                >
                  Enroll in {exam.code} & Start Diagnostic
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* STEP 3: Diagnostic Assessment & Result Summary */}
      {currentStep === 'diagnostic' && (
        <div data-testid="onboarding-diagnostic-step">
          {state.diagnosticResult ? (
            <div
              data-testid="diagnostic-result-summary"
              style={{
                backgroundColor: '#F7FAFC',
                border: '1px solid #E2E8F0',
                borderRadius: '12px',
                padding: '20px',
                textAlign: 'center',
              }}
            >
              <div
                style={{
                  width: '64px',
                  height: '64px',
                  borderRadius: '32px',
                  backgroundColor: '#319795',
                  color: '#FFFFFF',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '24px',
                  fontWeight: '800',
                  margin: '0 auto 12px auto',
                }}
              >
                {state.diagnosticResult.initialReadinessScore}%
              </div>
              <h3 style={{ margin: '0 0 6px 0', fontSize: '18px', color: '#234E52' }}>
                Baseline Readiness Calibrated!
              </h3>
              <p style={{ fontSize: '13px', color: '#4A5568', margin: '0 0 16px 0' }}>
                Accuracy: {state.diagnosticResult.accuracyPercent}% ({state.diagnosticResult.score}/
                {state.diagnosticResult.totalQuestions} questions correct)
              </p>

              {state.diagnosticResult.strengths.length > 0 && (
                <div style={{ textAlign: 'left', marginBottom: '12px' }}>
                  <h4 style={{ margin: '0 0 6px 0', fontSize: '13px', color: '#2F855A' }}>Identified Strengths:</h4>
                  {state.diagnosticResult.strengths.map((s, idx) => (
                    <div
                      key={idx}
                      style={{
                        fontSize: '12px',
                        backgroundColor: '#F0FFF4',
                        color: '#22543D',
                        padding: '4px 8px',
                        borderRadius: '4px',
                        marginBottom: '4px',
                      }}
                    >
                      ✓ {s}
                    </div>
                  ))}
                </div>
              )}

              {state.diagnosticResult.weaknesses.length > 0 && (
                <div style={{ textAlign: 'left', marginBottom: '16px' }}>
                  <h4 style={{ margin: '0 0 6px 0', fontSize: '13px', color: '#C53030' }}>
                    Weak Areas for Targeted Remediation:
                  </h4>
                  {state.diagnosticResult.weaknesses.map((w, idx) => (
                    <div
                      key={idx}
                      style={{
                        fontSize: '12px',
                        backgroundColor: '#FFF5F5',
                        color: '#742A2A',
                        padding: '4px 8px',
                        borderRadius: '4px',
                        marginBottom: '4px',
                      }}
                    >
                      ! {w}
                    </div>
                  ))}
                </div>
              )}

              <button
                data-testid="enter-learning-hub-btn"
                onClick={handleCompleteFlow}
                style={{
                  width: '100%',
                  padding: '12px',
                  backgroundColor: '#319795',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '8px',
                  fontWeight: '700',
                  fontSize: '14px',
                  cursor: 'pointer',
                }}
              >
                Go to Adaptive Learning Dashboard →
              </button>
            </div>
          ) : (
            <div>
              {state.diagnosticSession && (
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: '14px',
                    }}
                  >
                    <span style={{ fontSize: '13px', fontWeight: 700, color: '#4A5568' }}>
                      Question {activeQuestionIndex + 1} of {state.diagnosticSession.questions.length}
                    </span>
                    <button
                      data-testid="skip-diagnostic-btn"
                      onClick={handleSkipDiagnostic}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#718096',
                        fontSize: '12px',
                        cursor: 'pointer',
                        textDecoration: 'underline',
                      }}
                    >
                      Skip Diagnostic (50% Prior)
                    </button>
                  </div>

                  {(() => {
                    const q: DiagnosticQuestion = state.diagnosticSession.questions[activeQuestionIndex];
                    if (!q) return null;
                    const selected = selectedAnswers[q.id];

                    return (
                      <div
                        data-testid={`diagnostic-question-${q.id}`}
                        style={{
                          backgroundColor: '#FFFFFF',
                          border: '1px solid #CBD5E0',
                          borderRadius: '10px',
                          padding: '16px',
                          marginBottom: '16px',
                        }}
                      >
                        <div style={{ fontSize: '11px', color: '#718096', marginBottom: '8px' }}>
                          {q.subjectName} • {q.subtopicName}
                        </div>
                        <div style={{ fontSize: '14px', fontWeight: 600, marginBottom: '14px' }}>
                          <FormulaRenderer content={q.stem} testId={`diagnostic-stem-${q.id}`} />
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {q.options.map((opt) => (
                            <button
                              key={opt.id}
                              type="button"
                              data-testid={`diag-option-${q.id}-${opt.id}`}
                              onClick={() => handleOptionSelect(q.id, opt.id)}
                              style={{
                                padding: '10px 14px',
                                textAlign: 'left',
                                borderRadius: '8px',
                                border: '1px solid',
                                borderColor: selected === opt.id ? '#3182CE' : '#E2E8F0',
                                backgroundColor: selected === opt.id ? '#EBF8FF' : '#F7FAFC',
                                color: selected === opt.id ? '#2B6CB0' : '#2D3748',
                                fontWeight: selected === opt.id ? '600' : '400',
                                cursor: 'pointer',
                              }}
                            >
                              <FormulaRenderer content={opt.text} inline />
                            </button>
                          ))}
                        </div>
                      </div>
                    );
                  })()}

                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: '8px' }}>
                    <button
                      data-testid="prev-diag-q-btn"
                      disabled={activeQuestionIndex === 0}
                      onClick={() => setActiveQuestionIndex((i) => Math.max(0, i - 1))}
                      style={{
                        padding: '10px 16px',
                        borderRadius: '6px',
                        border: '1px solid #CBD5E0',
                        backgroundColor: '#FFFFFF',
                        cursor: activeQuestionIndex === 0 ? 'not-allowed' : 'pointer',
                        opacity: activeQuestionIndex === 0 ? 0.5 : 1,
                      }}
                    >
                      ← Previous
                    </button>

                    {activeQuestionIndex < state.diagnosticSession.questions.length - 1 ? (
                      <button
                        data-testid="next-diag-q-btn"
                        onClick={() =>
                          setActiveQuestionIndex((i) =>
                            Math.min((state.diagnosticSession?.questions.length || 1) - 1, i + 1)
                          )
                        }
                        style={{
                          padding: '10px 16px',
                          borderRadius: '6px',
                          border: 'none',
                          backgroundColor: '#3182CE',
                          color: '#FFFFFF',
                          fontWeight: '600',
                          cursor: 'pointer',
                        }}
                      >
                        Next →
                      </button>
                    ) : (
                      <button
                        data-testid="submit-diagnostic-btn"
                        onClick={handleSubmitDiagnostic}
                        style={{
                          padding: '10px 20px',
                          borderRadius: '6px',
                          border: 'none',
                          backgroundColor: '#38A169',
                          color: '#FFFFFF',
                          fontWeight: '700',
                          cursor: 'pointer',
                        }}
                      >
                        Submit Diagnostic Test
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 2. Adaptive Recommendations Dashboard Screen
// ============================================================================

export interface RecommendationsDashboardScreenProps {
  controller: LearnController;
  onOpenRoadmap?: () => void;
  onOpenMicroLesson?: (subtopicId: number) => void;
}

export const RecommendationsDashboardScreen: React.FC<RecommendationsDashboardScreenProps> = ({
  controller,
  onOpenRoadmap,
  onOpenMicroLesson,
}) => {
  const [state, setState] = useState<LearnState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const readinessScore = state.progress?.readinessScore ?? state.activeEnrollment?.readinessScore ?? 0;
  const nextActivity = state.nextActivity;
  const recommendation = state.recommendation;

  const handleStartActivity = () => {
    if (!nextActivity) return;
    if (nextActivity.subtopicId && onOpenMicroLesson) {
      controller.loadMicroLesson(nextActivity.subtopicId);
      onOpenMicroLesson(nextActivity.subtopicId);
    }
  };

  const handleStudySubtopic = (subtopicId: number) => {
    controller.loadMicroLesson(subtopicId);
    if (onOpenMicroLesson) onOpenMicroLesson(subtopicId);
  };

  return (
    <div
      data-testid="recommendations-dashboard-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Header & Readiness Score */}
      <div
        data-testid="readiness-score-meter"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: '#2C5282',
          borderRadius: '12px',
          padding: '16px 20px',
          color: '#FFFFFF',
          marginBottom: '16px',
        }}
      >
        <div>
          <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', opacity: 0.85 }}>
            Target: {state.activeEnrollment?.examinationCode || 'SSC-CGL-2026'}
          </span>
          <h2 style={{ margin: '2px 0 0 0', fontSize: '18px', fontWeight: '700' }}>Overall Readiness Score</h2>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: '28px', fontWeight: '800' }}>{readinessScore}%</div>
          <span style={{ fontSize: '11px', opacity: 0.85 }}>Calibrated mastery</span>
        </div>
      </div>

      {/* Next Best Action Hero */}
      {nextActivity && (
        <div
          data-testid="next-activity-hero"
          style={{
            border: '2px solid #3182CE',
            borderRadius: '12px',
            padding: '16px',
            backgroundColor: '#EBF8FF',
            marginBottom: '16px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
            <span
              style={{
                fontSize: '11px',
                fontWeight: 700,
                color: '#2B6CB0',
                backgroundColor: '#BEE3F8',
                padding: '2px 8px',
                borderRadius: '10px',
              }}
            >
              Recommended Next Action • {nextActivity.urgency} Urgency
            </span>
            <span style={{ fontSize: '12px', color: '#4A5568' }}>~{nextActivity.estimatedMinutes} mins</span>
          </div>
          <h3 style={{ margin: '0 0 4px 0', fontSize: '16px', color: '#1A365D' }}>{nextActivity.subtopicName}</h3>
          <p style={{ margin: '0 0 12px 0', fontSize: '12px', color: '#4A5568' }}>{nextActivity.reason}</p>
          <button
            data-testid="start-next-activity-btn"
            onClick={handleStartActivity}
            style={{
              padding: '10px 16px',
              backgroundColor: '#3182CE',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '6px',
              fontWeight: '700',
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            Start 5-Part Micro-Lesson →
          </button>
        </div>
      )}

      {/* Weakness Prioritization Cards */}
      <div style={{ marginBottom: '16px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: '700', color: '#2D3748', margin: '0 0 8px 0' }}>
          Prioritized Weak Areas
        </h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {recommendation?.weakTopicRecommendations.map((weak, idx) => (
            <div
              key={idx}
              data-testid={`weak-topic-card-${weak.subtopicId || idx}`}
              style={{
                border: '1px solid #FED7D7',
                borderRadius: '8px',
                padding: '12px',
                backgroundColor: '#FFF5F5',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div>
                <h4 style={{ margin: '0 0 2px 0', fontSize: '14px', color: '#742A2A' }}>
                  {weak.subtopicsToRevise?.[0] || weak.topicName}
                </h4>
                <span style={{ fontSize: '11px', color: '#9B2C2C' }}>
                  Current Mastery: {weak.currentAccuracy}%
                </span>
              </div>
              <button
                data-testid={`study-weak-btn-${weak.subtopicId || idx}`}
                onClick={() => handleStudySubtopic(weak.subtopicId || 303)}
                style={{
                  padding: '6px 12px',
                  backgroundColor: '#E53E3E',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor: 'pointer',
                }}
              >
                Study
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* 7-Day Adaptive Study Plan */}
      <div style={{ marginBottom: '16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: '700', color: '#2D3748', margin: 0 }}>
            7-Day Adaptive Study Plan
          </h3>
          <button
            data-testid="refresh-recommendations-btn"
            onClick={() => controller.generateRecommendations()}
            style={{
              background: 'none',
              border: 'none',
              color: '#3182CE',
              fontSize: '12px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            ↻ Refresh
          </button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {recommendation?.studyPlanItems.map((item) => (
            <div
              key={item.day}
              data-testid={`study-plan-day-${item.day}`}
              style={{
                border: '1px solid #E2E8F0',
                borderRadius: '8px',
                padding: '10px 12px',
                backgroundColor: item.completed ? '#F0FFF4' : '#FFFFFF',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
              }}
            >
              <input
                type="checkbox"
                data-testid={`study-plan-check-${item.day}`}
                checked={!!item.completed}
                onChange={() => controller.toggleStudyPlanDay(item.day)}
                style={{ width: '18px', height: '18px', cursor: 'pointer' }}
              />
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    fontSize: '13px',
                    fontWeight: 600,
                    color: item.completed ? '#22543D' : '#2D3748',
                    textDecoration: item.completed ? 'line-through' : 'none',
                  }}
                >
                  Day {item.day}: {item.topicName}
                </div>
                <div style={{ fontSize: '11px', color: '#718096' }}>{item.estimatedMinutes} mins</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Spaced Reviews Due */}
      {state.progress && state.progress.dueReviews.length > 0 && (
        <div data-testid="spaced-reviews-panel" style={{ marginBottom: '16px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: '700', color: '#2D3748', margin: '0 0 8px 0' }}>
            Spaced Repetition Reviews Due
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {state.progress.dueReviews.map((rev) => (
              <div
                key={rev.subtopicId}
                data-testid={`due-review-${rev.subtopicId}`}
                style={{
                  border: '1px solid #FEFCBF',
                  borderRadius: '8px',
                  padding: '10px 12px',
                  backgroundColor: '#FFFFF0',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#744210' }}>{rev.subtopicName}</div>
                  <div style={{ fontSize: '11px', color: '#975A16' }}>
                    Mastery: {rev.masteryScore}% • {rev.preferredLessonType || 'Review'}
                  </div>
                </div>
                <button
                  data-testid={`review-subtopic-btn-${rev.subtopicId}`}
                  onClick={() => handleStudySubtopic(rev.subtopicId)}
                  style={{
                    padding: '6px 10px',
                    backgroundColor: '#D69E2E',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontSize: '11px',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  Review
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {onOpenRoadmap && (
        <button
          data-testid="nav-to-roadmap-btn"
          onClick={onOpenRoadmap}
          style={{
            width: '100%',
            padding: '12px',
            backgroundColor: '#EDF2F7',
            color: '#2B6CB0',
            border: '1px solid #CBD5E0',
            borderRadius: '8px',
            fontWeight: '600',
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          View Full Dynamic Learning Path (DLP) Roadmap →
        </button>
      )}
    </div>
  );
};

// ============================================================================
// 3. Dynamic Learning Path (DLP) Roadmap Screen
// ============================================================================

export interface DlpRoadmapScreenProps {
  controller: LearnController;
  onOpenMicroLesson?: (subtopicId: number) => void;
}

export const DlpRoadmapScreen: React.FC<DlpRoadmapScreenProps> = ({
  controller,
  onOpenMicroLesson,
}) => {
  const [state, setState] = useState<LearnState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const lp = state.learningPath;
  const overallProgress = controller.getOverallProgressPercent();

  const handleMilestoneClick = (item: { subtopicId?: number; milestoneId?: string; id?: string }) => {
    if (item.subtopicId && onOpenMicroLesson) {
      controller.loadMicroLesson(item.subtopicId);
      onOpenMicroLesson(item.subtopicId);
    }
  };

  const handleStartAdaptiveQuiz = () => {
    const res = controller.startAdaptiveQuiz();
    // Simulate scoring and submission in mobile UI
    controller.submitAdaptiveQuiz(res.quizSessionId, 85);
  };

  const handleStartGatekeeper = () => {
    const res = controller.startGatekeeper();
    // Simulate gatekeeper evaluation submission
    controller.submitGatekeeper(res.gatekeeperSessionId, 88);
  };

  return (
    <div
      data-testid="dlp-roadmap-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      <div style={{ marginBottom: '16px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A365D', margin: '0 0 4px 0' }}>
          Dynamic Learning Path Roadmap
        </h2>
        <p style={{ fontSize: '12px', color: '#718096', margin: '0 0 10px 0' }}>
          Sequential prerequisite mastery progression for {lp?.targetExam || 'Competitive Exam'}
        </p>

        {/* Overall Progress Bar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              flex: 1,
              height: '10px',
              borderRadius: '5px',
              backgroundColor: '#EDF2F7',
              overflow: 'hidden',
            }}
          >
            <div
              data-testid="dlp-progress-bar"
              style={{
                width: `${overallProgress}%`,
                height: '100%',
                backgroundColor: '#38A169',
                borderRadius: '5px',
                transition: 'width 0.3s ease',
              }}
            />
          </div>
          <span style={{ fontSize: '13px', fontWeight: 700, color: '#276749' }}>
            {overallProgress}% Completed
          </span>
        </div>
      </div>

      {/* Phases List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {lp?.milestoneGraph.phases.map((phase) => {
          const isLocked = phase.status === 'LOCKED';
          const isDone = phase.status === 'COMPLETED';

          return (
            <div
              key={phase.phaseNumber}
              data-testid={`dlp-phase-${phase.phaseNumber}`}
              style={{
                border: '1px solid',
                borderColor: isDone ? '#9AE6B4' : isLocked ? '#E2E8F0' : '#BEE3F8',
                borderRadius: '12px',
                padding: '16px',
                backgroundColor: isDone ? '#F0FFF4' : isLocked ? '#F7FAFC' : '#FFFFFF',
                opacity: isLocked ? 0.75 : 1,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <span
                  style={{
                    fontSize: '11px',
                    fontWeight: 700,
                    padding: '2px 8px',
                    borderRadius: '10px',
                    backgroundColor: isDone ? '#C6F6D5' : isLocked ? '#EDF2F7' : '#BEE3F8',
                    color: isDone ? '#22543D' : isLocked ? '#718096' : '#2B6CB0',
                  }}
                >
                  {phase.status}
                </span>
                <span style={{ fontSize: '11px', color: '#718096' }}>
                  Target: {phase.targetAccuracy}% Accuracy
                </span>
              </div>
              <h3 style={{ margin: '0 0 4px 0', fontSize: '15px', color: '#2D3748' }}>{phase.name}</h3>
              <p style={{ margin: '0 0 12px 0', fontSize: '12px', color: '#718096' }}>{phase.focus}</p>

              {/* Milestones inside Phase */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {phase.items.map((item) => (
                  <div
                    key={item.milestoneId || item.id}
                    data-testid={`dlp-milestone-${item.milestoneId || item.id}`}
                    onClick={() => !isLocked && handleMilestoneClick(item)}
                    style={{
                      border: '1px solid #E2E8F0',
                      borderRadius: '8px',
                      padding: '10px 12px',
                      backgroundColor: item.completed ? '#E6FFFA' : '#FFFFFF',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      cursor: isLocked ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <div>
                      <div
                        style={{
                          fontSize: '13px',
                          fontWeight: 600,
                          color: item.completed ? '#234E52' : '#2D3748',
                          textDecoration: item.completed ? 'line-through' : 'none',
                        }}
                      >
                        {item.subtopicName}
                      </div>
                      <div style={{ fontSize: '11px', color: '#718096' }}>
                        {item.subjectName} • Target: {item.targetScore}%
                      </div>
                    </div>
                    <span
                      style={{
                        fontSize: '12px',
                        fontWeight: 700,
                        color: item.completed ? '#319795' : '#3182CE',
                      }}
                    >
                      {item.completed ? '✓ Mastered' : 'Study →'}
                    </span>
                  </div>
                ))}
              </div>

              {/* Phase Specific Trigger Buttons */}
              {phase.phaseNumber === 2 && !isLocked && !isDone && (
                <button
                  data-testid="start-phase2-quiz-btn"
                  onClick={handleStartAdaptiveQuiz}
                  style={{
                    marginTop: '12px',
                    width: '100%',
                    padding: '10px',
                    backgroundColor: '#3182CE',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: '600',
                    fontSize: '13px',
                    cursor: 'pointer',
                  }}
                >
                  Start Phase 2 Adaptive Practice Quiz
                </button>
              )}

              {phase.phaseNumber === 3 && !isLocked && !isDone && (
                <button
                  data-testid="start-gatekeeper-btn"
                  onClick={handleStartGatekeeper}
                  style={{
                    marginTop: '12px',
                    width: '100%',
                    padding: '10px',
                    backgroundColor: '#805AD5',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '6px',
                    fontWeight: '600',
                    fontSize: '13px',
                    cursor: 'pointer',
                  }}
                >
                  Attempt Full Mock Readiness Gatekeeper
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

// ============================================================================
// 4. 5-Part Micro-Lesson Viewer Screen & AI Explanation IR Engine
// ============================================================================

export interface MicroLessonViewerScreenProps {
  controller: LearnController;
  onFinishLesson?: () => void;
}

export const MicroLessonViewerScreen: React.FC<MicroLessonViewerScreenProps> = ({
  controller,
  onFinishLesson,
}) => {
  const [state, setState] = useState<LearnState>(controller.getState());
  const [showAiExplanation, setShowAiExplanation] = useState<boolean>(false);

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const lesson = state.currentLesson;
  const slideIndex = state.currentSlideIndex;
  const explanation = state.currentExplanation;

  if (!lesson) {
    return (
      <div data-testid="micro-lesson-empty" style={{ padding: '20px', textAlign: 'center' }}>
        No micro-lesson currently loaded.
      </div>
    );
  }

  const steps: StepItem[] = lesson.content.stepByStepBreakdown?.steps || [];
  const formulas: FormulaEntry[] = Array.isArray(lesson.content.formulaCheatSheet)
    ? lesson.content.formulaCheatSheet
    : lesson.content.formulaEntries || [];
  const quizItems: QuickCheckItem[] = lesson.content.quickCheckItems || [];

  const handleCompleteAndReturn = () => {
    controller.completeMicroLesson(lesson.subtopicId);
    if (onFinishLesson) onFinishLesson();
  };

  return (
    <div
      data-testid="micro-lesson-viewer-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Top Bar with Lesson Metadata */}
      <div style={{ marginBottom: '14px', borderBottom: '1px solid #E2E8F0', paddingBottom: '10px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '11px', fontWeight: 700, color: '#3182CE' }}>
            {lesson.subjectName} • {lesson.topicName}
          </span>
          <span
            style={{
              fontSize: '11px',
              backgroundColor: '#EDF2F7',
              padding: '2px 6px',
              borderRadius: '4px',
              color: '#4A5568',
            }}
          >
            ~{lesson.estimatedMinutes} mins
          </span>
        </div>
        <h2 style={{ margin: '4px 0 2px 0', fontSize: '18px', fontWeight: '700', color: '#1A365D' }}>
          {lesson.subtopicName}
        </h2>
        <span style={{ fontSize: '12px', color: '#718096' }}>{lesson.approachName}</span>
      </div>

      {/* 5-Part Slide Navigation Tabs */}
      <div
        data-testid="micro-lesson-tabs"
        style={{
          display: 'flex',
          overflowX: 'auto',
          gap: '6px',
          paddingBottom: '8px',
          marginBottom: '16px',
          borderBottom: '1px solid #E2E8F0',
        }}
      >
        {[
          { idx: 0, label: '1. Concept' },
          { idx: 1, label: '2. Formulas' },
          { idx: 2, label: '3. Steps' },
          { idx: 3, label: '4. Quick Check' },
          { idx: 4, label: '5. Summary' },
        ].map((tab) => (
          <button
            key={tab.idx}
            data-testid={`slide-tab-${tab.idx}`}
            onClick={() => controller.setSlideIndex(tab.idx)}
            style={{
              padding: '6px 10px',
              borderRadius: '16px',
              border: 'none',
              backgroundColor: slideIndex === tab.idx ? '#3182CE' : '#EDF2F7',
              color: slideIndex === tab.idx ? '#FFFFFF' : '#4A5568',
              fontSize: '12px',
              fontWeight: slideIndex === tab.idx ? 700 : 500,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* SLIDE 0: Concept Overview & Trap Alert */}
      {slideIndex === 0 && (
        <div data-testid="slide-part-concept" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div
            style={{
              backgroundColor: '#FFFFFF',
              border: '1px solid #CBD5E0',
              borderRadius: '10px',
              padding: '16px',
            }}
          >
            <h3 style={{ margin: '0 0 8px 0', fontSize: '15px', color: '#2B6CB0' }}>Core Concept Foundation</h3>
            <div style={{ fontSize: '14px', lineHeight: 1.6 }}>
              <FormulaRenderer content={lesson.content.coreConcept} testId="lesson-core-concept" />
            </div>
          </div>

          {lesson.content.trapAlert && (
            <div
              data-testid="exam-trap-alert"
              style={{
                backgroundColor: '#FFF5F5',
                border: '1px solid #FEB2B2',
                borderRadius: '10px',
                padding: '16px',
              }}
            >
              <h4 style={{ margin: '0 0 6px 0', fontSize: '14px', color: '#C53030' }}>
                ⚠️ Exam Trap & Negative Marking Pitfall
              </h4>
              <p style={{ margin: '0 0 8px 0', fontSize: '13px', color: '#742A2A' }}>
                {typeof lesson.content.trapAlert === 'string'
                  ? lesson.content.trapAlert
                  : lesson.content.trapAlert.description}
              </p>
              {typeof lesson.content.trapAlert === 'object' && lesson.content.trapAlert.correctApproach && (
                <div style={{ fontSize: '12px', color: '#22543D', backgroundColor: '#F0FFF4', padding: '6px', borderRadius: '4px' }}>
                  <strong>Correct Approach:</strong> {lesson.content.trapAlert.correctApproach}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* SLIDE 1: Core Formulas with LaTeX & Chemistry */}
      {slideIndex === 1 && (
        <div data-testid="slide-part-formulas" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <h3 style={{ margin: '0 0 4px 0', fontSize: '15px', color: '#2D3748' }}>Governing Formulas & Identities</h3>
          {formulas.map((f, idx) => (
            <div
              key={idx}
              data-testid={`formula-card-${idx}`}
              style={{
                backgroundColor: '#FFFFFF',
                border: '1px solid #CBD5E0',
                borderRadius: '10px',
                padding: '14px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
              }}
            >
              <div style={{ fontSize: '13px', fontWeight: 700, color: '#2C5282', marginBottom: '6px' }}>
                {f.name}
              </div>
              <FormulaRenderer content={f.latex} testId={`formula-latex-${idx}`} />
              <p style={{ margin: '6px 0 0 0', fontSize: '12px', color: '#718096' }}>{f.description}</p>
            </div>
          ))}
        </div>
      )}

      {/* SLIDE 2: Step-by-Step Worked Examples */}
      {slideIndex === 2 && (
        <div data-testid="slide-part-steps" style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {lesson.content.stepByStepBreakdown?.problemStatement && (
            <div
              style={{
                backgroundColor: '#F7FAFC',
                border: '1px solid #E2E8F0',
                borderRadius: '10px',
                padding: '14px',
              }}
            >
              <span style={{ fontSize: '11px', fontWeight: 700, color: '#4A5568' }}>PROBLEM STATEMENT</span>
              <div style={{ fontSize: '13px', marginTop: '4px' }}>
                <FormulaRenderer content={lesson.content.stepByStepBreakdown.problemStatement} />
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: '#718096' }}>
              Revealed {Math.min(state.revealedStepCount, steps.length)} of {steps.length} Steps
            </span>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button
                data-testid="reveal-next-step-btn"
                onClick={() => controller.revealNextStep()}
                disabled={state.revealedStepCount >= steps.length}
                style={{
                  padding: '6px 10px',
                  borderRadius: '6px',
                  border: '1px solid #CBD5E0',
                  backgroundColor: '#FFFFFF',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: state.revealedStepCount >= steps.length ? 'not-allowed' : 'pointer',
                  opacity: state.revealedStepCount >= steps.length ? 0.5 : 1,
                }}
              >
                Reveal Next Step ↓
              </button>
              <button
                data-testid="reveal-all-steps-btn"
                onClick={() => controller.revealAllSteps()}
                style={{
                  padding: '6px 10px',
                  borderRadius: '6px',
                  border: 'none',
                  backgroundColor: '#3182CE',
                  color: '#FFFFFF',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Reveal All
              </button>
            </div>
          </div>

          {steps.slice(0, state.revealedStepCount).map((step) => (
            <div
              key={step.stepNumber}
              data-testid={`step-item-${step.stepNumber}`}
              style={{
                backgroundColor: '#FFFFFF',
                border: '1px solid #CBD5E0',
                borderRadius: '10px',
                padding: '14px',
              }}
            >
              <div style={{ fontSize: '13px', fontWeight: 700, color: '#2B6CB0', marginBottom: '4px' }}>
                Step {step.stepNumber}: {step.title}
              </div>
              <p style={{ margin: '0 0 6px 0', fontSize: '13px', color: '#4A5568' }}>{step.explanation}</p>
              {step.formula && <FormulaRenderer content={step.formula} />}
              {step.tip && (
                <div style={{ fontSize: '11px', color: '#744210', backgroundColor: '#FEFCBF', padding: '4px 6px', borderRadius: '4px', marginTop: '6px' }}>
                  💡 {step.tip}
                </div>
              )}
            </div>
          ))}

          {state.revealedStepCount >= steps.length && lesson.content.stepByStepBreakdown?.finalAnswer && (
            <div
              data-testid="final-answer-banner"
              style={{
                backgroundColor: '#F0FFF4',
                border: '1px solid #9AE6B4',
                borderRadius: '8px',
                padding: '12px',
                textAlign: 'center',
                fontWeight: 700,
                color: '#22543D',
              }}
            >
              Final Answer: <FormulaRenderer content={lesson.content.stepByStepBreakdown.finalAnswer} inline />
            </div>
          )}
        </div>
      )}

      {/* SLIDE 3: Interactive Quick-Check Quiz */}
      {slideIndex === 3 && (
        <div data-testid="slide-part-quiz" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <h3 style={{ margin: '0 0 4px 0', fontSize: '15px', color: '#2D3748' }}>Quick-Check Mastery Questions</h3>
          {quizItems.map((q, qIdx) => {
            const selected = state.quizSelections[qIdx];
            return (
              <div
                key={qIdx}
                data-testid={`quick-check-${qIdx}`}
                style={{
                  backgroundColor: '#FFFFFF',
                  border: '1px solid #CBD5E0',
                  borderRadius: '10px',
                  padding: '14px',
                }}
              >
                <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '10px' }}>
                  <FormulaRenderer content={q.question} />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  {q.options?.map((opt, oIdx) => {
                    const isSelected = selected?.selectedOption === opt;
                    const isOptCorrect = opt.trim().toLowerCase() === (q.correctAnswer || q.answer).trim().toLowerCase();

                    let btnBg = '#F7FAFC';
                    let btnBorder = '#E2E8F0';
                    let btnColor = '#2D3748';

                    if (selected) {
                      if (isSelected) {
                        btnBg = selected.isCorrect ? '#C6F6D5' : '#FED7D7';
                        btnBorder = selected.isCorrect ? '#38A169' : '#E53E3E';
                        btnColor = selected.isCorrect ? '#22543D' : '#742A2A';
                      } else if (isOptCorrect) {
                        btnBg = '#C6F6D5';
                        btnBorder = '#38A169';
                        btnColor = '#22543D';
                      }
                    }

                    return (
                      <button
                        key={oIdx}
                        type="button"
                        data-testid={`quick-check-opt-${qIdx}-${oIdx}`}
                        onClick={() => controller.recordQuizAnswer(qIdx, opt)}
                        style={{
                          padding: '8px 12px',
                          textAlign: 'left',
                          borderRadius: '6px',
                          border: `1px solid ${btnBorder}`,
                          backgroundColor: btnBg,
                          color: btnColor,
                          fontSize: '12px',
                          cursor: 'pointer',
                        }}
                      >
                        <FormulaRenderer content={opt} inline />
                      </button>
                    );
                  })}
                </div>
                {selected && q.explanation && (
                  <div
                    data-testid={`quiz-explanation-${qIdx}`}
                    style={{
                      marginTop: '8px',
                      fontSize: '12px',
                      color: selected.isCorrect ? '#22543D' : '#742A2A',
                      backgroundColor: selected.isCorrect ? '#F0FFF4' : '#FFF5F5',
                      padding: '8px',
                      borderRadius: '6px',
                    }}
                  >
                    <strong>Explanation:</strong> <FormulaRenderer content={q.explanation} inline />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* SLIDE 4: Summary & Key Takeaways */}
      {slideIndex === 4 && (
        <div data-testid="slide-part-summary" style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div
            style={{
              backgroundColor: '#FFFFFF',
              border: '1px solid #CBD5E0',
              borderRadius: '10px',
              padding: '16px',
            }}
          >
            <h3 style={{ margin: '0 0 10px 0', fontSize: '15px', color: '#2C5282' }}>Key Concept Takeaways</h3>
            <ul style={{ margin: 0, paddingLeft: '20px', fontSize: '13px', lineHeight: 1.6 }}>
              {lesson.content.keyTakeaways?.map((item, idx) => (
                <li key={idx} style={{ marginBottom: '6px' }}>
                  <FormulaRenderer content={item} inline />
                </li>
              ))}
            </ul>
          </div>

          {/* Feedback & Helpful Rating */}
          <div
            style={{
              backgroundColor: '#F7FAFC',
              border: '1px solid #E2E8F0',
              borderRadius: '10px',
              padding: '14px',
              textAlign: 'center',
            }}
          >
            <span style={{ fontSize: '13px', fontWeight: 600, color: '#4A5568' }}>
              How helpful was this micro-lesson?
            </span>
            <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '8px' }}>
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  data-testid={`rating-star-${star}`}
                  onClick={() => controller.submitLessonFeedback(star)}
                  style={{
                    fontSize: '18px',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    color: (state.lessonFeedbackRating || 0) >= star ? '#D69E2E' : '#CBD5E0',
                  }}
                >
                  ★
                </button>
              ))}
            </div>
          </div>

          <button
            data-testid="complete-microlesson-btn"
            onClick={handleCompleteAndReturn}
            style={{
              padding: '12px',
              backgroundColor: '#38A169',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '8px',
              fontWeight: '700',
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            Mark Complete & Unlock Next Milestone ✓
          </button>
        </div>
      )}

      {/* AI Explanation IR Toggle Drawer */}
      <div style={{ marginTop: '20px', borderTop: '1px solid #E2E8F0', paddingTop: '12px' }}>
        <button
          data-testid="toggle-ai-explanation-btn"
          onClick={() => setShowAiExplanation((v) => !v)}
          style={{
            width: '100%',
            padding: '10px',
            backgroundColor: '#EDF2F7',
            color: '#2B6CB0',
            border: '1px solid #CBD5E0',
            borderRadius: '6px',
            fontSize: '12px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          {showAiExplanation ? '▲ Hide AI Explanation IR' : '▼ View Step-by-Step AI Explanation IR'}
        </button>

        {showAiExplanation && explanation && (
          <div
            data-testid="ai-explanation-drawer"
            style={{
              marginTop: '10px',
              backgroundColor: '#FFFFFF',
              border: '1px solid #CBD5E0',
              borderRadius: '10px',
              padding: '14px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <span style={{ fontSize: '11px', fontWeight: 700, color: '#805AD5' }}>
                AI EXPLANATION ENGINE (IR)
              </span>
              <span style={{ fontSize: '11px', color: '#718096' }}>
                Step {state.activeExplanationStepIndex + 1} of {explanation.steps.length}
              </span>
            </div>

            {(() => {
              const curStep = explanation.steps[state.activeExplanationStepIndex];
              if (!curStep) return null;
              return (
                <div data-testid={`explanation-step-${curStep.sequence}`}>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#2D3748', marginBottom: '4px' }}>
                    [{curStep.type}] {curStep.justification?.rule || 'Reasoning Transformation'}
                  </div>
                  <p style={{ margin: '0 0 6px 0', fontSize: '12px', color: '#4A5568' }}>
                    {curStep.justification?.rationale}
                  </p>
                  {curStep.math?.latex && <FormulaRenderer content={curStep.math.latex} />}
                </div>
              );
            })()}

            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '10px' }}>
              <button
                data-testid="prev-exp-step-btn"
                disabled={state.activeExplanationStepIndex === 0}
                onClick={() => controller.selectExplanationStep(state.activeExplanationStepIndex - 1)}
                style={{
                  padding: '6px 10px',
                  borderRadius: '4px',
                  border: '1px solid #CBD5E0',
                  backgroundColor: '#FFFFFF',
                  cursor: 'pointer',
                  fontSize: '11px',
                }}
              >
                ← Prev Step
              </button>
              <button
                data-testid="next-exp-step-btn"
                disabled={state.activeExplanationStepIndex >= explanation.steps.length - 1}
                onClick={() => controller.selectExplanationStep(state.activeExplanationStepIndex + 1)}
                style={{
                  padding: '6px 10px',
                  borderRadius: '4px',
                  border: 'none',
                  backgroundColor: '#805AD5',
                  color: '#FFFFFF',
                  cursor: 'pointer',
                  fontSize: '11px',
                  fontWeight: 600,
                }}
              >
                Next Step →
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Prev / Next Slide Controls */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '16px' }}>
        <button
          data-testid="prev-slide-btn"
          disabled={slideIndex === 0}
          onClick={() => controller.prevSlide()}
          style={{
            padding: '8px 14px',
            borderRadius: '6px',
            border: '1px solid #CBD5E0',
            backgroundColor: '#FFFFFF',
            fontSize: '13px',
            fontWeight: 600,
            cursor: slideIndex === 0 ? 'not-allowed' : 'pointer',
            opacity: slideIndex === 0 ? 0.5 : 1,
          }}
        >
          ← Previous
        </button>
        <button
          data-testid="next-slide-btn"
          disabled={slideIndex >= 4}
          onClick={() => controller.nextSlide()}
          style={{
            padding: '8px 14px',
            borderRadius: '6px',
            border: 'none',
            backgroundColor: '#3182CE',
            color: '#FFFFFF',
            fontSize: '13px',
            fontWeight: 600,
            cursor: slideIndex >= 4 ? 'not-allowed' : 'pointer',
            opacity: slideIndex >= 4 ? 0.5 : 1,
          }}
        >
          Next →
        </button>
      </div>
    </div>
  );
};

// ============================================================================
// 5. Learn Hub Screen Orchestrator
// ============================================================================

export interface LearnHubScreenProps {
  controller: LearnController;
  onNavigateToPractice?: () => void;
}

export const LearnHubScreen: React.FC<LearnHubScreenProps> = ({
  controller,
  onNavigateToPractice,
}) => {
  const [state, setState] = useState<LearnState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const hasOnboarded = controller.hasCompletedOnboarding();
  const activeTab = state.activeHubTab;

  return (
    <div
      data-testid="learn-hub-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        minHeight: '100vh',
        backgroundColor: '#F7FAFC',
      }}
    >
      {/* If onboarding not completed or in onboarding step */}
      {!hasOnboarded ? (
        <LearnerOnboardingScreen controller={controller} />
      ) : (
        <div>
          {/* Navigation Tab Bar */}
          <div
            style={{
              display: 'flex',
              backgroundColor: '#FFFFFF',
              borderBottom: '1px solid #E2E8F0',
              padding: '8px 16px',
              gap: '8px',
            }}
          >
            <button
              data-testid="hub-tab-recommendations"
              onClick={() => controller.setActiveHubTab('recommendations')}
              style={{
                flex: 1,
                padding: '8px',
                border: 'none',
                borderRadius: '6px',
                backgroundColor: activeTab === 'recommendations' ? '#EBF8FF' : 'transparent',
                color: activeTab === 'recommendations' ? '#2B6CB0' : '#4A5568',
                fontWeight: activeTab === 'recommendations' ? 700 : 500,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Recommendations
            </button>
            <button
              data-testid="hub-tab-roadmap"
              onClick={() => controller.setActiveHubTab('roadmap')}
              style={{
                flex: 1,
                padding: '8px',
                border: 'none',
                borderRadius: '6px',
                backgroundColor: activeTab === 'roadmap' ? '#EBF8FF' : 'transparent',
                color: activeTab === 'roadmap' ? '#2B6CB0' : '#4A5568',
                fontWeight: activeTab === 'roadmap' ? 700 : 500,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              DLP Roadmap
            </button>
            <button
              data-testid="hub-tab-lesson"
              onClick={() => controller.setActiveHubTab('lesson')}
              style={{
                flex: 1,
                padding: '8px',
                border: 'none',
                borderRadius: '6px',
                backgroundColor: activeTab === 'lesson' ? '#EBF8FF' : 'transparent',
                color: activeTab === 'lesson' ? '#2B6CB0' : '#4A5568',
                fontWeight: activeTab === 'lesson' ? 700 : 500,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Micro-Lesson
            </button>
            {onNavigateToPractice && (
              <button
                data-testid="hub-nav-practice-btn"
                onClick={onNavigateToPractice}
                style={{
                  padding: '8px 12px',
                  border: '1px solid #CBD5E0',
                  borderRadius: '6px',
                  backgroundColor: '#FFFFFF',
                  color: '#2D3748',
                  fontWeight: 600,
                  fontSize: '12px',
                  cursor: 'pointer',
                }}
              >
                Practice 🎯
              </button>
            )}
          </div>

          {/* Tab Views */}
          {activeTab === 'recommendations' && (
            <RecommendationsDashboardScreen
              controller={controller}
              onOpenRoadmap={() => controller.setActiveHubTab('roadmap')}
              onOpenMicroLesson={() => controller.setActiveHubTab('lesson')}
            />
          )}

          {activeTab === 'roadmap' && (
            <DlpRoadmapScreen
              controller={controller}
              onOpenMicroLesson={() => controller.setActiveHubTab('lesson')}
            />
          )}

          {activeTab === 'lesson' && (
            <MicroLessonViewerScreen
              controller={controller}
              onFinishLesson={() => controller.setActiveHubTab('roadmap')}
            />
          )}
        </div>
      )}
    </div>
  );
};
