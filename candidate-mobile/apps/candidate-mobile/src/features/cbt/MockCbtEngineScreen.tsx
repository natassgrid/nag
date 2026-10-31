import React, { useState, useEffect } from 'react';
import {
  CbtSessionController,
  CbtSessionState,
  ExamItem,
  NtaQuestionStatus,
} from './cbt-session-controller';
import { FormulaRenderer } from '../formula/FormulaRenderer';

export interface MockCbtEngineScreenProps {
  controller: CbtSessionController;
  onSubmitCompleted?: () => void;
  onExitSession?: () => void;
}

export const MockCbtEngineScreen: React.FC<MockCbtEngineScreenProps> = ({
  controller,
  onSubmitCompleted,
  onExitSession,
}) => {
  const [state, setState] = useState<CbtSessionState>(controller.getState());
  const [showPalette, setShowPalette] = useState<boolean>(false);
  const [showSubmitConfirm, setShowSubmitConfirm] = useState<boolean>(false);

  useEffect(() => {
    const unsub = controller.subscribe((s: CbtSessionState) => {
      setState(s);
      if (s.isSubmitted && onSubmitCompleted) {
        onSubmitCompleted();
      }
    });

    const timerInterval = setInterval(() => {
      controller.tickTimer(1);
    }, 1000);

    return () => {
      unsub();
      clearInterval(timerInterval);
    };
  }, [controller, onSubmitCompleted]);

  const activeItem: ExamItem | null = controller.getActiveItem();
  const activeEntry = controller.getActiveQuestionState();
  const paletteSummary = controller.getPaletteSummary();

  const formatTimer = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const getStatusColor = (status: NtaQuestionStatus): { bg: string; text: string; label: string } => {
    switch (status) {
      case 'ANSWERED':
        return { bg: '#38A169', text: '#FFFFFF', label: 'Answered' };
      case 'NOT_ANSWERED':
        return { bg: '#E53E3E', text: '#FFFFFF', label: 'Not Answered' };
      case 'MARKED_FOR_REVIEW':
        return { bg: '#805AD5', text: '#FFFFFF', label: 'Marked for Review' };
      case 'ANSWERED_AND_MARKED_FOR_REVIEW':
        return { bg: '#553C9A', text: '#FAF089', label: 'Ans & Marked for Review' };
      case 'NOT_VISITED':
      default:
        return { bg: '#CBD5E0', text: '#2D3748', label: 'Not Visited' };
    }
  };

  const handleConfirmSubmit = () => {
    setShowSubmitConfirm(false);
    controller.submitExam();
    if (onSubmitCompleted) {
      onSubmitCompleted();
    }
  };

  return (
    <div
      data-testid="mock-cbt-engine-screen"
      style={{
        maxWidth: '640px',
        margin: '0 auto',
        backgroundColor: '#F7FAFC',
        minHeight: '100vh',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* 1. Header Bar: Exam Title & Countdown Timer */}
      <div
        style={{
          padding: '12px 16px',
          backgroundColor: '#1A365D',
          color: '#FFFFFF',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        }}
      >
        <div>
          <h2 style={{ fontSize: '14px', fontWeight: '700', margin: 0 }}>{state.examTitle}</h2>
          <span style={{ fontSize: '11px', color: '#CBD5E0' }}>
            NAG High-Security Mobile CBT Engine
          </span>
        </div>

        <div style={{ textAlign: 'right' }}>
          <span style={{ fontSize: '10px', color: '#A0AEC0', display: 'block', textTransform: 'uppercase' }}>
            Time Remaining
          </span>
          <span
            data-testid="cbt-timer"
            style={{
              fontFamily: 'monospace',
              fontSize: '16px',
              fontWeight: '700',
              color: state.remainingSeconds < 300 ? '#FEB2B2' : '#68D391',
            }}
          >
            {formatTimer(state.remainingSeconds)}
          </span>
        </div>
      </div>

      {/* 2. Security & Proctoring Status Ribbon */}
      <div
        data-testid="security-status-ribbon"
        style={{
          padding: '6px 16px',
          backgroundColor: '#2D3748',
          color: '#E2E8F0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '11px',
        }}
      >
        <div style={{ display: 'flex', gap: '12px' }}>
          <span data-testid="flag-secure-badge" style={{ color: state.security.isFlagSecureActive ? '#68D391' : '#FEB2B2' }}>
            🔒 FLAG_SECURE Active
          </span>
          <span data-testid="lock-task-badge" style={{ color: state.security.isLockTaskPinned ? '#68D391' : '#FEB2B2' }}>
            🛡️ LockTaskMode Pinned
          </span>
        </div>

        <button
          data-testid="toggle-palette-btn"
          onClick={() => setShowPalette(!showPalette)}
          style={{
            backgroundColor: '#4A5568',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '4px',
            padding: '3px 8px',
            fontSize: '11px',
            fontWeight: '600',
            cursor: 'pointer',
          }}
        >
          {showPalette ? 'Hide Palette ▲' : 'Question Palette ▼'}
        </button>
      </div>

      {/* 3. Section Navigation Tabs */}
      <div
        data-testid="section-tabs-bar"
        style={{
          display: 'flex',
          backgroundColor: '#FFFFFF',
          borderBottom: '1px solid #E2E8F0',
          overflowX: 'auto',
        }}
      >
        {state.sections.map((sec) => {
          const isActive = sec.id === state.activeSectionId;
          return (
            <button
              key={sec.id}
              data-testid={`sec-tab-${sec.id}`}
              onClick={() => controller.switchSection(sec.id)}
              style={{
                flex: 1,
                padding: '10px 14px',
                border: 'none',
                borderBottom: isActive ? '3px solid #2B6CB0' : '3px solid transparent',
                backgroundColor: isActive ? '#EBF8FF' : '#FFFFFF',
                color: isActive ? '#2B6CB0' : '#4A5568',
                fontWeight: isActive ? '700' : '500',
                fontSize: '12px',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {sec.title.split(':')[0]}
            </button>
          );
        })}
      </div>

      {/* 4. Question Navigation Palette Bottom Sheet / Drawer */}
      {showPalette && (
        <div
          data-testid="question-palette-drawer"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '14px 16px',
            borderBottom: '2px solid #CBD5E0',
            boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)',
          }}
        >
          {/* Legend Summary */}
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              gap: '8px',
              fontSize: '11px',
              marginBottom: '12px',
              paddingBottom: '10px',
              borderBottom: '1px solid #EDF2F7',
            }}
          >
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '12px', height: '12px', borderRadius: '3px', backgroundColor: '#38A169' }} />
              Answered ({paletteSummary.answeredCount})
            </span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '12px', height: '12px', borderRadius: '3px', backgroundColor: '#E53E3E' }} />
              Not Answered ({paletteSummary.notAnsweredCount})
            </span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '12px', height: '12px', borderRadius: '3px', backgroundColor: '#805AD5' }} />
              Marked for Review ({paletteSummary.markedForReviewCount})
            </span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '12px', height: '12px', borderRadius: '3px', backgroundColor: '#553C9A' }} />
              Ans & Marked ({paletteSummary.answeredAndMarkedForReviewCount})
            </span>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '12px', height: '12px', borderRadius: '3px', backgroundColor: '#CBD5E0' }} />
              Not Visited ({paletteSummary.notVisitedCount})
            </span>
          </div>

          {/* Question Grid Buttons */}
          <div
            data-testid="question-buttons-grid"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(6, 1fr)',
              gap: '8px',
            }}
          >
            {state.items.map((item, idx) => {
              const entry = state.questionStates[item.id];
              const status = entry ? entry.status : 'NOT_VISITED';
              const col = getStatusColor(status);
              const isCurrent = idx === state.activeQuestionIndex;

              return (
                <button
                  key={item.id}
                  data-testid={`q-grid-btn-${item.questionNumber}`}
                  onClick={() => {
                    controller.jumpToQuestion(idx);
                    setShowPalette(false);
                  }}
                  style={{
                    padding: '8px 0',
                    borderRadius: '6px',
                    border: isCurrent ? '2px solid #2B6CB0' : '1px solid transparent',
                    backgroundColor: col.bg,
                    color: col.text,
                    fontWeight: '700',
                    fontSize: '13px',
                    cursor: 'pointer',
                    outline: isCurrent ? '2px solid #3182CE' : 'none',
                  }}
                >
                  {item.questionNumber}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* 5. Main Question & Options Workspace */}
      <div style={{ flex: 1, padding: '16px', overflowY: 'auto' }}>
        {activeItem ? (
          <div>
            {/* Question Header: Number & Marks */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '12px',
                paddingBottom: '8px',
                borderBottom: '1px solid #E2E8F0',
              }}
            >
              <div>
                <span
                  data-testid="active-question-number"
                  style={{ fontSize: '15px', fontWeight: '700', color: '#1A202C' }}
                >
                  Question {activeItem.questionNumber} of {state.items.length}
                </span>
                <span style={{ fontSize: '11px', color: '#718096', marginLeft: '8px' }}>
                  [{activeItem.topic}]
                </span>
              </div>

              <div style={{ fontSize: '12px', fontWeight: '600' }}>
                <span style={{ color: '#276749' }}>+{activeItem.marks.toFixed(1)}</span>
                <span style={{ color: '#C53030', marginLeft: '6px' }}>
                  -{activeItem.negativeMarks.toFixed(1)}
                </span>
              </div>
            </div>

            {/* Question Stem with FormulaRenderer */}
            <div
              data-testid="question-content-box"
              style={{
                fontSize: '14px',
                color: '#2D3748',
                lineHeight: 1.6,
                backgroundColor: '#FFFFFF',
                padding: '16px',
                borderRadius: '8px',
                border: '1px solid #E2E8F0',
                marginBottom: '16px',
              }}
            >
              <FormulaRenderer content={activeItem.content} />
            </div>

            {/* 4 Multiple Choice Options */}
            <div data-testid="options-list" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {activeItem.options.map((option) => {
                const isSelected = activeEntry?.selectedOptionId === option.id;

                return (
                  <label
                    key={option.id}
                    data-testid={`option-card-${option.id}`}
                    style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      padding: '12px 14px',
                      borderRadius: '8px',
                      border: isSelected ? '2px solid #2B6CB0' : '1px solid #CBD5E0',
                      backgroundColor: isSelected ? '#EBF8FF' : '#FFFFFF',
                      cursor: 'pointer',
                    }}
                  >
                    <input
                      type="radio"
                      name={`option-group-${activeItem.id}`}
                      value={option.id}
                      checked={isSelected}
                      onChange={() => controller.selectOption(option.id)}
                      style={{ marginTop: '4px', marginRight: '10px' }}
                    />
                    <div style={{ flex: 1, fontSize: '13px', color: '#1A202C' }}>
                      <strong style={{ marginRight: '6px', color: '#2B6CB0' }}>({option.id})</strong>
                      <FormulaRenderer content={option.text} inline />
                    </div>
                  </label>
                );
              })}
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', padding: '40px' }}>No active question loaded.</div>
        )}
      </div>

      {/* 6. Sticky Action Footer */}
      <div
        data-testid="cbt-action-footer"
        style={{
          padding: '12px 16px',
          backgroundColor: '#FFFFFF',
          borderTop: '1px solid #CBD5E0',
          boxShadow: '0 -2px 6px rgba(0,0,0,0.05)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '8px', marginBottom: '8px' }}>
          <button
            data-testid="cbt-clear-response-btn"
            onClick={() => controller.clearResponse()}
            style={{
              padding: '10px 12px',
              fontSize: '12px',
              color: '#4A5568',
              backgroundColor: '#EDF2F7',
              border: '1px solid #CBD5E0',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            Clear Response
          </button>

          <button
            data-testid="cbt-mark-review-btn"
            onClick={() => controller.markForReviewAndNext()}
            style={{
              padding: '10px 12px',
              fontSize: '12px',
              color: '#553C9A',
              backgroundColor: '#FAF5FF',
              border: '1px solid #D6BCFA',
              borderRadius: '6px',
              fontWeight: '600',
              cursor: 'pointer',
            }}
          >
            Mark for Review & Next
          </button>

          <button
            data-testid="cbt-save-next-btn"
            onClick={() => controller.saveAndNext()}
            style={{
              padding: '10px 16px',
              fontSize: '12px',
              color: '#FFFFFF',
              backgroundColor: '#2B6CB0',
              border: 'none',
              borderRadius: '6px',
              fontWeight: '700',
              cursor: 'pointer',
            }}
          >
            Save & Next →
          </button>
        </div>

        <button
          data-testid="cbt-submit-test-btn"
          onClick={() => setShowSubmitConfirm(true)}
          style={{
            width: '100%',
            padding: '10px',
            backgroundColor: '#22543D',
            color: '#FFFFFF',
            fontWeight: '700',
            fontSize: '13px',
            borderRadius: '6px',
            border: 'none',
            cursor: 'pointer',
          }}
        >
          Submit Mock Examination
        </button>
      </div>

      {/* Confirmation Modal */}
      {showSubmitConfirm && (
        <div
          data-testid="submit-confirm-modal"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 1000,
          }}
        >
          <div
            style={{
              maxWidth: '380px',
              width: '100%',
              backgroundColor: '#FFFFFF',
              borderRadius: '12px',
              padding: '20px',
              textAlign: 'center',
            }}
          >
            <h3 style={{ margin: '0 0 8px 0', fontSize: '17px', color: '#1A202C' }}>
              Submit Mock Examination?
            </h3>
            <p style={{ margin: '0 0 16px 0', fontSize: '13px', color: '#718096', lineHeight: 1.4 }}>
              You have answered <strong>{paletteSummary.answeredCount + paletteSummary.answeredAndMarkedForReviewCount}</strong> out of {state.items.length} questions. Local grading and performance breakdown will be generated instantly.
            </p>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                data-testid="cancel-submit-modal-btn"
                onClick={() => setShowSubmitConfirm(false)}
                style={{
                  flex: 1,
                  padding: '10px',
                  backgroundColor: '#EDF2F7',
                  border: '1px solid #CBD5E0',
                  borderRadius: '6px',
                  fontSize: '13px',
                  cursor: 'pointer',
                }}
              >
                Continue Test
              </button>
              <button
                data-testid="confirm-submit-modal-btn"
                onClick={handleConfirmSubmit}
                style={{
                  flex: 1,
                  padding: '10px',
                  backgroundColor: '#22543D',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  fontWeight: '700',
                  fontSize: '13px',
                  cursor: 'pointer',
                }}
              >
                Yes, Submit Now
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
