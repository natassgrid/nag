import React, { useState, useEffect } from 'react';
import {
  PracticeController,
  PracticeState,
  QuestionReviewFilter,
} from './practice-controller';
import {
  PracticeSet,
  PracticeDifficulty,
  PracticeSessionMode,
  QuestionResult,
} from './practice-models';
import { FormulaRenderer } from '../formula';

// ============================================================================
// 1. Practice Hub Screen
// ============================================================================

export interface PracticeHubScreenProps {
  controller: PracticeController;
  onStartSet?: (setId: string) => void;
  onOpenHistory?: () => void;
  onStudySubtopic?: (subtopicId: number) => void;
}

export const PracticeHubScreen: React.FC<PracticeHubScreenProps> = ({
  controller,
  onStartSet,
  onOpenHistory,
}) => {
  const [state, setState] = useState<PracticeState>(controller.getState());
  const [showCustomModal, setShowCustomModal] = useState<boolean>(false);

  // Custom drill form state
  const [customSubj, setCustomSubj] = useState<string>('Quantitative Aptitude');
  const [customDiff, setCustomDiff] = useState<PracticeDifficulty>('CORE');
  const [customCount, setCustomCount] = useState<number>(10);
  const [customMode, setCustomMode] = useState<PracticeSessionMode>('TIMED');

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const kpis = controller.getKpiStats();
  const filteredSets = controller.getFilteredSets();
  const subjects = controller.getSubjects();

  const handleStartPractice = (setId: string) => {
    controller.startSession(setId);
    if (onStartSet) onStartSet(setId);
  };

  const handleGenerateCustom = () => {
    const newSet = controller.generateCustomPractice({
      subject: customSubj,
      difficulty: customDiff,
      questionCount: customCount,
      mode: customMode,
    });
    setShowCustomModal(false);
    handleStartPractice(newSet.id);
  };

  return (
    <div
      data-testid="practice-hub-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Header & History Button */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '16px',
        }}
      >
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#1A365D', margin: 0 }}>
            Practice Drills & Mock CBT
          </h2>
          <p style={{ fontSize: '12px', color: '#718096', margin: '2px 0 0 0' }}>
            Official exam simulations, curated sectional problem sets & custom drills
          </p>
        </div>
        {onOpenHistory && (
          <button
            data-testid="hub-to-history-btn"
            onClick={onOpenHistory}
            style={{
              padding: '6px 12px',
              backgroundColor: '#EDF2F7',
              border: '1px solid #CBD5E0',
              borderRadius: '6px',
              fontSize: '12px',
              fontWeight: 600,
              color: '#2B6CB0',
              cursor: 'pointer',
            }}
          >
            History 📊
          </button>
        )}
      </div>

      {/* KPI Stats Strip */}
      <div
        data-testid="practice-kpi-strip"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: '8px',
          marginBottom: '16px',
        }}
      >
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#2B6CB0' }}>{kpis.totalSets}</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Total Drills</div>
        </div>
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#319795' }}>{kpis.officialCount}</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Official CBT</div>
        </div>
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#805AD5' }}>{kpis.curatedCount}</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Curated Sets</div>
        </div>
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#D69E2E' }}>{kpis.totalQuestions}</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Questions</div>
        </div>
      </div>

      {/* Search & Custom Generator Trigger */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
        <input
          data-testid="practice-search-input"
          type="text"
          placeholder="Search by topic, keyword, or exam..."
          value={state.searchQuery}
          onChange={(e) => controller.setSearchQuery(e.target.value)}
          style={{
            flex: 1,
            padding: '10px 12px',
            borderRadius: '8px',
            border: '1px solid #CBD5E0',
            fontSize: '13px',
          }}
        />
        <button
          data-testid="open-custom-generator-btn"
          onClick={() => setShowCustomModal((v) => !v)}
          style={{
            padding: '10px 14px',
            backgroundColor: '#3182CE',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '8px',
            fontWeight: 600,
            fontSize: '12px',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          + Custom Drill
        </button>
      </div>

      {/* Custom Practice Generator Modal / Accordion */}
      {showCustomModal && (
        <div
          data-testid="custom-generator-panel"
          style={{
            backgroundColor: '#F7FAFC',
            border: '1px solid #CBD5E0',
            borderRadius: '10px',
            padding: '16px',
            marginBottom: '16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}
        >
          <div style={{ fontSize: '14px', fontWeight: 700, color: '#1A365D' }}>
            ⚡ Configure Custom Adaptive Practice Session
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>Subject</label>
            <select
              data-testid="custom-subject-select"
              value={customSubj}
              onChange={(e) => setCustomSubj(e.target.value)}
              style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid #CBD5E0' }}
            >
              <option value="Quantitative Aptitude">Quantitative Aptitude</option>
              <option value="General Intelligence & Reasoning">General Intelligence & Reasoning</option>
              <option value="Physics">Physics</option>
              <option value="Mathematics">Mathematics</option>
              <option value="English Comprehension">English Comprehension</option>
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>Difficulty</label>
              <select
                data-testid="custom-difficulty-select"
                value={customDiff}
                onChange={(e) => setCustomDiff(e.target.value as PracticeDifficulty)}
                style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid #CBD5E0' }}
              >
                <option value="FOUNDATION">Foundation</option>
                <option value="CORE">Core</option>
                <option value="ADVANCED">Advanced</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>Mode</label>
              <select
                data-testid="custom-mode-select"
                value={customMode}
                onChange={(e) => setCustomMode(e.target.value as PracticeSessionMode)}
                style={{ width: '100%', padding: '8px', borderRadius: '6px', border: '1px solid #CBD5E0' }}
              >
                <option value="TIMED">Timed Drill</option>
                <option value="UNTIMED">Untimed Study</option>
              </select>
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '4px' }}>Questions Count</label>
            <div style={{ display: 'flex', gap: '8px' }}>
              {[5, 10, 15, 20].map((cnt) => (
                <button
                  key={cnt}
                  type="button"
                  data-testid={`custom-count-${cnt}`}
                  onClick={() => setCustomCount(cnt)}
                  style={{
                    flex: 1,
                    padding: '6px',
                    borderRadius: '6px',
                    border: '1px solid',
                    borderColor: customCount === cnt ? '#3182CE' : '#CBD5E0',
                    backgroundColor: customCount === cnt ? '#EBF8FF' : '#FFFFFF',
                    color: customCount === cnt ? '#2B6CB0' : '#4A5568',
                    fontWeight: customCount === cnt ? 700 : 500,
                    cursor: 'pointer',
                  }}
                >
                  {cnt} Qs
                </button>
              ))}
            </div>
          </div>

          <button
            data-testid="generate-custom-btn"
            onClick={handleGenerateCustom}
            style={{
              padding: '10px',
              backgroundColor: '#38A169',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '6px',
              fontWeight: 700,
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            Launch Custom Session Now →
          </button>
        </div>
      )}

      {/* Source Filter Chips */}
      <div style={{ display: 'flex', gap: '6px', marginBottom: '14px', overflowX: 'auto' }}>
        {(['ALL', 'EXAM_CLONE', 'MANUAL'] as const).map((src) => (
          <button
            key={src}
            data-testid={`filter-source-${src}`}
            onClick={() => controller.setSourceFilter(src)}
            style={{
              padding: '6px 12px',
              borderRadius: '16px',
              border: 'none',
              backgroundColor: state.selectedSource === src ? '#2B6CB0' : '#EDF2F7',
              color: state.selectedSource === src ? '#FFFFFF' : '#4A5568',
              fontSize: '12px',
              fontWeight: state.selectedSource === src ? 700 : 500,
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            {src === 'ALL' ? 'All Drills' : src === 'EXAM_CLONE' ? 'Official CBT Mocks' : 'Curated Topic Drills'}
          </button>
        ))}
      </div>

      {/* Practice Sets List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {filteredSets.map((pSet: PracticeSet) => (
          <div
            key={pSet.id}
            data-testid={`practice-set-card-${pSet.id}`}
            style={{
              backgroundColor: '#FFFFFF',
              border: '1px solid #E2E8F0',
              borderRadius: '10px',
              padding: '14px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <span
                  style={{
                    fontSize: '10px',
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: '4px',
                    backgroundColor: pSet.source === 'EXAM_CLONE' ? '#EBF8FF' : '#FEFCBF',
                    color: pSet.source === 'EXAM_CLONE' ? '#2B6CB0' : '#744210',
                    marginRight: '6px',
                  }}
                >
                  {pSet.source === 'EXAM_CLONE' ? 'Official Simulation' : 'Topic Drill'}
                </span>
                <span style={{ fontSize: '11px', color: '#718096' }}>{pSet.subjectSlug}</span>
              </div>
              <span style={{ fontSize: '11px', fontWeight: 600, color: '#4A5568' }}>
                ⏱ {pSet.durationMinutes}m
              </span>
            </div>

            <h3 style={{ margin: '8px 0 4px 0', fontSize: '15px', color: '#1A365D' }}>{pSet.name}</h3>
            <p style={{ margin: '0 0 10px 0', fontSize: '12px', color: '#718096' }}>{pSet.description}</p>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '11px', color: '#718096' }}>
                {pSet.totalQuestions} Questions • {pSet.totalMarks || pSet.totalQuestions * 2} Marks
              </span>
              <button
                data-testid={`start-set-${pSet.id}`}
                onClick={() => handleStartPractice(pSet.id)}
                style={{
                  padding: '8px 14px',
                  backgroundColor: '#3182CE',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '12px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Start Practice →
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// ============================================================================
// 2. Practice Result Panel Screen
// ============================================================================

export interface PracticeResultPanelScreenProps {
  controller: PracticeController;
  onStudyWeakSubtopic?: (subtopicId: number) => void;
  onReattempt?: () => void;
  onReturnToHub?: () => void;
}

export const PracticeResultPanelScreen: React.FC<PracticeResultPanelScreenProps> = ({
  controller,
  onStudyWeakSubtopic,
  onReattempt,
  onReturnToHub,
}) => {
  const [state, setState] = useState<PracticeState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const result = state.currentResult;
  if (!result) {
    return (
      <div data-testid="practice-result-empty" style={{ padding: '20px', textAlign: 'center' }}>
        No practice result available.
      </div>
    );
  }

  const filteredQuestions = controller.getFilteredResults();

  const handleStudySubtopic = (q: QuestionResult) => {
    const subtopicId = q.subtopicId || 303;
    if (onStudyWeakSubtopic) {
      onStudyWeakSubtopic(subtopicId);
    }
  };

  return (
    <div
      data-testid="practice-result-panel-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Top Banner & Marks */}
      <div
        data-testid="result-score-banner"
        style={{
          backgroundColor: '#2A4365',
          borderRadius: '12px',
          padding: '16px 20px',
          color: '#FFFFFF',
          marginBottom: '16px',
        }}
      >
        <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', opacity: 0.85 }}>
          Session Result • {result.practiceSetName}
        </span>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '4px' }}>
          <div>
            <div style={{ fontSize: '28px', fontWeight: 800 }}>
              {result.obtainedMarks} / {result.totalMarks} Marks
            </div>
            <span style={{ fontSize: '12px', opacity: 0.85 }}>
              Accuracy: {result.accuracyPercent}% • Time: {result.timeSpentSeconds || 104}s
            </span>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#68D391' }}>
              {result.correctCount} Correct
            </div>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#FC8181' }}>
              {result.incorrectCount} Incorrect
            </div>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#CBD5E0' }}>
              {result.skippedCount} Skipped
            </div>
          </div>
        </div>
      </div>

      {/* Filter Tabs for Question Review */}
      <div style={{ marginBottom: '12px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 700, color: '#2D3748', margin: '0 0 8px 0' }}>
          Question-by-Question Solution Review
        </h3>
        <div style={{ display: 'flex', gap: '6px', overflowX: 'auto' }}>
          {(['ALL', 'CORRECT', 'INCORRECT', 'SKIPPED', 'FLAGGED'] as QuestionReviewFilter[]).map((f) => (
            <button
              key={f}
              data-testid={`review-filter-${f.toLowerCase()}`}
              onClick={() => controller.setReviewFilter(f)}
              style={{
                padding: '6px 10px',
                borderRadius: '14px',
                border: 'none',
                backgroundColor: state.reviewFilter === f ? '#3182CE' : '#EDF2F7',
                color: state.reviewFilter === f ? '#FFFFFF' : '#4A5568',
                fontSize: '11px',
                fontWeight: state.reviewFilter === f ? 700 : 500,
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {f} ({
                f === 'ALL'
                  ? result.questionResults.length
                  : f === 'CORRECT'
                  ? result.correctCount
                  : f === 'INCORRECT'
                  ? result.incorrectCount
                  : f === 'SKIPPED'
                  ? result.skippedCount
                  : result.flaggedCount || 0
              })
            </button>
          ))}
        </div>
      </div>

      {/* Questions Review List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '20px' }}>
        {filteredQuestions.map((q, idx) => {
          let opts: Array<{ id: string; text: string }> = [];
          if (q.optionsJson) {
            try {
              opts = JSON.parse(q.optionsJson);
            } catch (e) {
              opts = [];
            }
          }

          const isBookmark = state.bookmarkedQuestions.has(q.questionId);

          return (
            <div
              key={q.questionId}
              data-testid={`review-question-${q.questionId}`}
              style={{
                backgroundColor: '#FFFFFF',
                border: '1px solid',
                borderColor: q.correct ? '#9AE6B4' : q.candidateAnswer ? '#FEB2B2' : '#CBD5E0',
                borderRadius: '10px',
                padding: '14px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <span style={{ fontSize: '11px', fontWeight: 700, color: '#4A5568' }}>
                  Q{idx + 1} • {q.subject}
                </span>
                <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      padding: '2px 6px',
                      borderRadius: '4px',
                      backgroundColor: q.correct ? '#C6F6D5' : q.candidateAnswer ? '#FED7D7' : '#EDF2F7',
                      color: q.correct ? '#22543D' : q.candidateAnswer ? '#742A2A' : '#4A5568',
                    }}
                  >
                    {q.correct ? 'Correct (+2)' : q.candidateAnswer ? 'Incorrect (-0.5)' : 'Skipped (0)'}
                  </span>
                  <button
                    data-testid={`bookmark-btn-${q.questionId}`}
                    onClick={() => controller.toggleBookmark(q.questionId)}
                    style={{
                      background: 'none',
                      border: 'none',
                      fontSize: '14px',
                      cursor: 'pointer',
                    }}
                  >
                    {isBookmark ? '★' : '☆'}
                  </button>
                </div>
              </div>

              {/* Question Stem */}
              <div style={{ fontSize: '13px', fontWeight: 600, margin: '6px 0 10px 0' }}>
                <FormulaRenderer content={q.content} />
              </div>

              {/* Options */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '10px' }}>
                {opts.map((opt) => {
                  const isSelected = q.candidateAnswer === opt.id;
                  const isAnswer = q.correctAnswer === opt.id;

                  let border = '#E2E8F0';
                  let bg = '#F7FAFC';
                  let label = '';

                  if (isAnswer) {
                    border = '#38A169';
                    bg = '#F0FFF4';
                    label = ' ✓ Correct Answer';
                  } else if (isSelected && !q.correct) {
                    border = '#E53E3E';
                    bg = '#FFF5F5';
                    label = ' ✗ Your Answer';
                  }

                  return (
                    <div
                      key={opt.id}
                      style={{
                        padding: '6px 10px',
                        borderRadius: '6px',
                        border: `1px solid ${border}`,
                        backgroundColor: bg,
                        fontSize: '12px',
                      }}
                    >
                      <FormulaRenderer content={opt.text} inline />
                      {label && <strong style={{ fontSize: '11px', marginLeft: '6px' }}>{label}</strong>}
                    </div>
                  );
                })}
              </div>

              {/* Explanation & Deep link trigger */}
              {q.explanation && (
                <div
                  style={{
                    backgroundColor: '#F7FAFC',
                    border: '1px solid #E2E8F0',
                    borderRadius: '6px',
                    padding: '8px 10px',
                    fontSize: '12px',
                    color: '#2D3748',
                    marginBottom: '8px',
                  }}
                >
                  <strong>Solution Derivation:</strong>
                  <div style={{ marginTop: '4px' }}>
                    <FormulaRenderer content={q.explanation} />
                  </div>
                </div>
              )}

              {/* Deep link button to micro-lesson if question was missed */}
              {!q.correct && (
                <button
                  data-testid={`study-weak-subtopic-btn-${q.questionId}`}
                  onClick={() => handleStudySubtopic(q)}
                  style={{
                    width: '100%',
                    padding: '8px',
                    backgroundColor: '#EBF8FF',
                    color: '#2B6CB0',
                    border: '1px solid #BEE3F8',
                    borderRadius: '6px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer',
                  }}
                >
                  📖 Study Micro-Lesson on this Weak Subtopic →
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* Action Buttons */}
      <div style={{ display: 'flex', gap: '8px' }}>
        {onReturnToHub && (
          <button
            data-testid="return-to-hub-btn"
            onClick={onReturnToHub}
            style={{
              flex: 1,
              padding: '12px',
              backgroundColor: '#EDF2F7',
              color: '#4A5568',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            ← Practice Hub
          </button>
        )}
        {onReattempt && (
          <button
            data-testid="reattempt-drill-btn"
            onClick={onReattempt}
            style={{
              flex: 1,
              padding: '12px',
              backgroundColor: '#3182CE',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            Re-attempt Drill ⟲
          </button>
        )}
      </div>
    </div>
  );
};

// ============================================================================
// 3. Practice History & Analytics Screen
// ============================================================================

export interface PracticeHistoryScreenProps {
  controller: PracticeController;
  onViewResult?: (sessionId: string) => void;
  onReattempt?: (setId: string) => void;
}

export const PracticeHistoryScreen: React.FC<PracticeHistoryScreenProps> = ({
  controller,
  onViewResult,
  onReattempt,
}) => {
  const [state, setState] = useState<PracticeState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const avgAcc = controller.getAverageAccuracy();
  const highestScore = controller.getHighestScore();
  const totalSolved = controller.getTotalQuestionsSolved();
  const subjectMetrics = controller.getSubjectWiseMetrics();
  const historyItems = controller.getFilteredHistory();

  return (
    <div
      data-testid="practice-history-screen"
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
          Practice History & Analytics
        </h2>
        <p style={{ fontSize: '12px', color: '#718096', margin: 0 }}>
          Chronological attempt performance and subject accuracy trends
        </p>
      </div>

      {/* Analytics KPI strip */}
      <div
        data-testid="history-stats-strip"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '8px',
          marginBottom: '16px',
        }}
      >
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '20px', fontWeight: 800, color: '#3182CE' }}>{avgAcc}%</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Avg Accuracy</div>
        </div>
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '20px', fontWeight: 800, color: '#38A169' }}>{highestScore}</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Best Score</div>
        </div>
        <div style={{ backgroundColor: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '8px', padding: '10px', textAlign: 'center' }}>
          <div style={{ fontSize: '20px', fontWeight: 800, color: '#805AD5' }}>{totalSolved}</div>
          <div style={{ fontSize: '11px', color: '#718096' }}>Solved Qs</div>
        </div>
      </div>

      {/* Subject-Wise Accuracy Breakdown */}
      <div style={{ marginBottom: '16px' }}>
        <h3 style={{ fontSize: '14px', fontWeight: 700, color: '#2D3748', margin: '0 0 8px 0' }}>
          Subject Mastery Trends
        </h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {subjectMetrics.map((sm) => (
            <div
              key={sm.subject}
              data-testid={`subject-metric-${sm.subject}`}
              style={{
                backgroundColor: '#FFFFFF',
                border: '1px solid #E2E8F0',
                borderRadius: '8px',
                padding: '8px 12px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div>
                <span style={{ fontSize: '13px', fontWeight: 600, color: '#2D3748' }}>{sm.subject}</span>
                <div style={{ fontSize: '11px', color: '#718096' }}>{sm.attemptsCount} attempts</div>
              </div>
              <div style={{ fontSize: '13px', fontWeight: 700, color: '#2B6CB0' }}>
                {sm.averageAccuracy}%
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Search Input */}
      <input
        data-testid="history-search-input"
        type="text"
        placeholder="Filter history by set name or subject..."
        value={state.historySearchQuery}
        onChange={(e) => controller.setHistorySearchQuery(e.target.value)}
        style={{
          width: '100%',
          padding: '8px 12px',
          borderRadius: '6px',
          border: '1px solid #CBD5E0',
          fontSize: '12px',
          marginBottom: '12px',
          boxSizing: 'border-box',
        }}
      />

      {/* History Items List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {historyItems.map((item) => (
          <div
            key={item.sessionId}
            data-testid={`history-item-${item.sessionId}`}
            style={{
              backgroundColor: '#FFFFFF',
              border: '1px solid #E2E8F0',
              borderRadius: '8px',
              padding: '12px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h4 style={{ margin: '0 0 2px 0', fontSize: '13px', color: '#1A365D' }}>{item.practiceSetName}</h4>
                <span style={{ fontSize: '11px', color: '#718096' }}>{item.subject} • {item.mode}</span>
              </div>
              <span
                style={{
                  fontSize: '12px',
                  fontWeight: 800,
                  color: item.accuracyPercent >= 60 ? '#276749' : '#C53030',
                }}
              >
                {item.accuracyPercent}%
              </span>
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginTop: '8px',
                paddingTop: '8px',
                borderTop: '1px solid #F7FAFC',
              }}
            >
              <span style={{ fontSize: '11px', color: '#718096' }}>
                Score: {item.obtainedMarks}/{item.totalMarks} ({item.correctCount}C / {item.incorrectCount}I)
              </span>
              <div style={{ display: 'flex', gap: '6px' }}>
                {onViewResult && (
                  <button
                    data-testid={`view-result-btn-${item.sessionId}`}
                    onClick={() => onViewResult(item.sessionId)}
                    style={{
                      padding: '4px 8px',
                      backgroundColor: '#EDF2F7',
                      color: '#2B6CB0',
                      border: 'none',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    View
                  </button>
                )}
                {onReattempt && (
                  <button
                    data-testid={`reattempt-btn-${item.sessionId}`}
                    onClick={() => onReattempt(item.practiceSetId)}
                    style={{
                      padding: '4px 8px',
                      backgroundColor: '#3182CE',
                      color: '#FFFFFF',
                      border: 'none',
                      borderRadius: '4px',
                      fontSize: '11px',
                      fontWeight: 600,
                      cursor: 'pointer',
                    }}
                  >
                    Re-attempt
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// ============================================================================
// 4. Practice Container Screen Orchestrator
// ============================================================================

export interface PracticeContainerScreenProps {
  controller: PracticeController;
  onStudyWeakSubtopic?: (subtopicId: number) => void;
  onNavigateToLearn?: () => void;
}

export const PracticeContainerScreen: React.FC<PracticeContainerScreenProps> = ({
  controller,
  onStudyWeakSubtopic,
  onNavigateToLearn,
}) => {
  const [state, setState] = useState<PracticeState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((s) => setState(s));
  }, [controller]);

  const activeTab = state.activeTab;

  return (
    <div
      data-testid="practice-container-screen"
      style={{
        maxWidth: '600px',
        margin: '0 auto',
        minHeight: '100vh',
        backgroundColor: '#F7FAFC',
      }}
    >
      {/* Top Header Tabs */}
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
          data-testid="practice-tab-hub"
          onClick={() => controller.setActiveTab('hub')}
          style={{
            flex: 1,
            padding: '8px',
            border: 'none',
            borderRadius: '6px',
            backgroundColor: activeTab === 'hub' ? '#EBF8FF' : 'transparent',
            color: activeTab === 'hub' ? '#2B6CB0' : '#4A5568',
            fontWeight: activeTab === 'hub' ? 700 : 500,
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          Catalog Hub
        </button>
        <button
          data-testid="practice-tab-result"
          onClick={() => controller.setActiveTab('result')}
          style={{
            flex: 1,
            padding: '8px',
            border: 'none',
            borderRadius: '6px',
            backgroundColor: activeTab === 'result' ? '#EBF8FF' : 'transparent',
            color: activeTab === 'result' ? '#2B6CB0' : '#4A5568',
            fontWeight: activeTab === 'result' ? 700 : 500,
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          Result Review
        </button>
        <button
          data-testid="practice-tab-history"
          onClick={() => controller.setActiveTab('history')}
          style={{
            flex: 1,
            padding: '8px',
            border: 'none',
            borderRadius: '6px',
            backgroundColor: activeTab === 'history' ? '#EBF8FF' : 'transparent',
            color: activeTab === 'history' ? '#2B6CB0' : '#4A5568',
            fontWeight: activeTab === 'history' ? 700 : 500,
            fontSize: '13px',
            cursor: 'pointer',
          }}
        >
          History
        </button>
        {onNavigateToLearn && (
          <button
            data-testid="practice-nav-learn-btn"
            onClick={onNavigateToLearn}
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
            Learn 📚
          </button>
        )}
      </div>

      {/* Screen Views */}
      {activeTab === 'hub' && (
        <PracticeHubScreen
          controller={controller}
          onStartSet={() => controller.setActiveTab('result')}
          onOpenHistory={() => controller.setActiveTab('history')}
        />
      )}

      {activeTab === 'result' && (
        <PracticeResultPanelScreen
          controller={controller}
          onStudyWeakSubtopic={onStudyWeakSubtopic}
          onReattempt={() => controller.setActiveTab('hub')}
          onReturnToHub={() => controller.setActiveTab('hub')}
        />
      )}

      {activeTab === 'history' && (
        <PracticeHistoryScreen
          controller={controller}
          onViewResult={() => controller.setActiveTab('result')}
          onReattempt={() => controller.setActiveTab('hub')}
        />
      )}
    </div>
  );
};
