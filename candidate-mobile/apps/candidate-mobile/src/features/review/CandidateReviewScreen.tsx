import React, { useState, useEffect, useMemo } from 'react';
import {
  ReviewController,
  ReviewState,
  ReviewQuestionItem,
  ReviewStatusFilter,
  ChallengeReasonType,
} from './review-controller';
import { FormulaRenderer } from '../formula';

export interface CandidateReviewScreenProps {
  controller: ReviewController;
  onExitReview?: () => void;
}

export const CandidateReviewScreen: React.FC<CandidateReviewScreenProps> = ({
  controller,
  onExitReview,
}) => {
  const [reviewState, setReviewState] = useState<ReviewState>(controller.getState());

  // Challenge Modal Form State
  const [reasonType, setReasonType] = useState<ChallengeReasonType>(
    'INCORRECT_PROVISIONAL_KEY'
  );
  const [claimedOption, setClaimedOption] = useState<string>('1');
  const [justification, setJustification] = useState<string>('');
  const [bookTitle, setBookTitle] = useState<string>('');
  const [bookAuthor, setBookAuthor] = useState<string>('');
  const [bookPage, setBookPage] = useState<string>('');
  const [challengeError, setChallengeError] = useState<string | null>(null);

  useEffect(() => {
    return controller.subscribe((state: ReviewState) => {
      setReviewState(state);
    });
  }, [controller]);

  const filteredQuestions = useMemo(() => {
    return controller.getFilteredQuestions();
  }, [controller, reviewState.statusFilter, reviewState.attempt.questions]);

  const currentQuestion: ReviewQuestionItem | null = useMemo(() => {
    return controller.getCurrentQuestion();
  }, [controller, reviewState.currentIndex, filteredQuestions]);

  const handleOpenChallenge = (q: ReviewQuestionItem) => {
    setReasonType('INCORRECT_PROVISIONAL_KEY');
    setClaimedOption(q.userChoice || q.options[0]?.id || '1');
    setJustification('');
    setBookTitle('');
    setBookAuthor('');
    setBookPage('');
    setChallengeError(null);
    controller.openChallengeModal(q.id);
  };

  const handleSubmitChallenge = () => {
    if (!currentQuestion) return;
    setChallengeError(null);

    const res = controller.submitChallenge({
      questionId: currentQuestion.id,
      candidateId: reviewState.attempt.rollNumber,
      claimedCorrectOptionId: claimedOption,
      reasonType,
      justificationText: justification,
      bookCitation: {
        bookTitle,
        author: bookAuthor,
        pageNumber: bookPage,
      },
      feeAmount: 200,
    });

    if (!res.success) {
      setChallengeError(res.error || 'Failed to submit answer key challenge.');
    }
  };

  const filterTabs: { id: ReviewStatusFilter; label: string; count: number }[] = [
    { id: 'ALL', label: 'All', count: reviewState.stats.totalQuestions },
    { id: 'CORRECT', label: 'Correct', count: reviewState.stats.correct },
    { id: 'INCORRECT', label: 'Incorrect', count: reviewState.stats.incorrect },
    { id: 'UNATTEMPTED', label: 'Unattempted', count: reviewState.stats.unattempted },
    { id: 'CHALLENGED', label: 'Challenged', count: reviewState.stats.challengedCount },
  ];

  return (
    <div
      data-testid="candidate-review-screen"
      style={{
        padding: '16px',
        paddingBottom: '80px',
        backgroundColor: '#F7FAFC',
        minHeight: '100%',
      }}
    >
      {/* 1. Exam Header & Recorded Response Banner */}
      <div
        style={{
          backgroundColor: '#1A365D',
          color: '#FFFFFF',
          borderRadius: '12px',
          padding: '16px',
          marginBottom: '16px',
          boxShadow: '0 2px 4px rgba(0,0,0,0.08)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <span
              style={{
                fontSize: '11px',
                fontFamily: 'monospace',
                backgroundColor: 'rgba(255,255,255,0.15)',
                padding: '2px 6px',
                borderRadius: '4px',
              }}
            >
              {reviewState.attempt.examCode}
            </span>
            <h2 style={{ margin: '4px 0 0 0', fontSize: '17px', fontWeight: '800' }}>
              Recorded Responses & Answer Key Review
            </h2>
            <div style={{ fontSize: '12px', color: '#CBD5E0', marginTop: '2px' }}>
              Candidate: {reviewState.attempt.candidateName} • Roll: {reviewState.attempt.rollNumber}
            </div>
          </div>
          {onExitReview && (
            <button
              onClick={onExitReview}
              style={{
                backgroundColor: 'transparent',
                border: '1px solid #CBD5E0',
                color: '#FFFFFF',
                borderRadius: '6px',
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
              }}
            >
              Exit Review
            </button>
          )}
        </div>
      </div>

      {/* 2. Review Summary Stats Grid */}
      <div
        data-testid="review-stats-summary"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: '8px',
          marginBottom: '16px',
        }}
      >
        <div
          data-testid="stat-score"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '10px 8px',
            borderRadius: '8px',
            border: '1px solid #E2E8F0',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '10px', color: '#718096', fontWeight: '700' }}>Raw Marks</div>
          <div style={{ fontSize: '18px', fontWeight: '800', color: '#2B6CB0' }}>
            {reviewState.stats.rawScore}/{reviewState.stats.maxMarks}
          </div>
        </div>

        <div
          data-testid="stat-correct"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '10px 8px',
            borderRadius: '8px',
            border: '1px solid #E2E8F0',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '10px', color: '#718096', fontWeight: '700' }}>Correct</div>
          <div style={{ fontSize: '18px', fontWeight: '800', color: '#38A169' }}>
            {reviewState.stats.correct}
          </div>
        </div>

        <div
          data-testid="stat-incorrect"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '10px 8px',
            borderRadius: '8px',
            border: '1px solid #E2E8F0',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '10px', color: '#718096', fontWeight: '700' }}>Incorrect</div>
          <div style={{ fontSize: '18px', fontWeight: '800', color: '#E53E3E' }}>
            {reviewState.stats.incorrect}
          </div>
        </div>

        <div
          data-testid="stat-unattempted"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '10px 8px',
            borderRadius: '8px',
            border: '1px solid #E2E8F0',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '10px', color: '#718096', fontWeight: '700' }}>Unattempted</div>
          <div style={{ fontSize: '18px', fontWeight: '800', color: '#718096' }}>
            {reviewState.stats.unattempted}
          </div>
        </div>
      </div>

      {/* 3. Status Filter Buttons */}
      <div
        style={{
          display: 'flex',
          gap: '6px',
          overflowX: 'auto',
          paddingBottom: '8px',
          marginBottom: '14px',
        }}
      >
        {filterTabs.map((tab) => {
          const active = reviewState.statusFilter === tab.id;
          return (
            <button
              key={tab.id}
              data-testid={`filter-btn-${tab.id}`}
              onClick={() => controller.setStatusFilter(tab.id)}
              style={{
                backgroundColor: active ? '#2B6CB0' : '#EDF2F7',
                color: active ? '#FFFFFF' : '#4A5568',
                border: 'none',
                borderRadius: '16px',
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: active ? '700' : '600',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {tab.label} ({tab.count})
            </button>
          );
        })}
      </div>

      {/* 4. Question Navigation Palette Grid */}
      <div
        style={{
          display: 'flex',
          gap: '6px',
          overflowX: 'auto',
          paddingBottom: '8px',
          marginBottom: '16px',
        }}
      >
        {filteredQuestions.map((q, idx) => {
          const isSelected = q.id === currentQuestion?.id;
          let bgColor = '#EDF2F7';
          let textColor = '#4A5568';

          if (q.isChallenged) {
            bgColor = '#FAF5FF';
            textColor = '#6B46C1';
          } else if (q.isCorrect) {
            bgColor = '#F0FFF4';
            textColor = '#22543D';
          } else if (q.userChoice !== undefined) {
            bgColor = '#FFF5F5';
            textColor = '#9B2C2C';
          }

          return (
            <button
              key={q.id}
              onClick={() => controller.selectQuestionIndex(idx)}
              style={{
                width: '36px',
                height: '36px',
                minWidth: '36px',
                borderRadius: '8px',
                border: isSelected ? '2px solid #2B6CB0' : `1px solid ${bgColor}`,
                backgroundColor: bgColor,
                color: textColor,
                fontWeight: '800',
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              {q.questionNumber}
            </button>
          );
        })}
      </div>

      {/* 5. Question Detail Card */}
      {currentQuestion ? (
        <div
          data-testid="review-question-card"
          style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '12px',
            padding: '16px',
            border: '1px solid #E2E8F0',
            boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
          }}
        >
          {/* Header row: Q Number, Subject, Marks Awarded */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '12px',
              borderBottom: '1px solid #EDF2F7',
              paddingBottom: '8px',
            }}
          >
            <div>
              <span style={{ fontWeight: '800', fontSize: '15px', color: '#1A365D' }}>
                Question {currentQuestion.questionNumber}
              </span>
              <span style={{ fontSize: '12px', color: '#718096', marginLeft: '8px' }}>
                {currentQuestion.subject}
              </span>
            </div>

            <div>
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: '700',
                  padding: '3px 8px',
                  borderRadius: '12px',
                  backgroundColor: currentQuestion.isCorrect
                    ? '#C6F6D5'
                    : currentQuestion.userChoice === undefined
                    ? '#EDF2F7'
                    : '#FED7D7',
                  color: currentQuestion.isCorrect
                    ? '#22543D'
                    : currentQuestion.userChoice === undefined
                    ? '#4A5568'
                    : '#9B2C2C',
                }}
              >
                {currentQuestion.marksAwarded > 0
                  ? `+${currentQuestion.marksAwarded} Marks`
                  : `${currentQuestion.marksAwarded} Marks`}
              </span>
            </div>
          </div>

          {/* Question Stem rendered via FormulaRenderer */}
          <div style={{ marginBottom: '16px', fontSize: '14px', lineHeight: '1.6' }}>
            <FormulaRenderer content={currentQuestion.content} />
          </div>

          {/* Options List */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
            {currentQuestion.options.map((option) => {
              const isMarkedByCandidate = currentQuestion.userChoice === option.id;
              const isProvisionalKey = currentQuestion.provisionalAnswerKey === option.id;

              let borderColor = '#E2E8F0';
              let bgColor = '#FFFFFF';

              if (isProvisionalKey) {
                borderColor = '#48BB78';
                bgColor = '#F0FFF4';
              } else if (isMarkedByCandidate && !option.isCorrect) {
                borderColor = '#F56565';
                bgColor = '#FFF5F5';
              }

              return (
                <div
                  key={option.id}
                  style={{
                    border: `1px solid ${borderColor}`,
                    backgroundColor: bgColor,
                    borderRadius: '8px',
                    padding: '10px 12px',
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '10px',
                  }}
                >
                  <span
                    style={{
                      fontFamily: 'monospace',
                      fontWeight: '700',
                      fontSize: '12px',
                      color: '#4A5568',
                      marginTop: '2px',
                    }}
                  >
                    ({option.id})
                  </span>

                  <div style={{ flex: 1, fontSize: '13px' }}>
                    <FormulaRenderer content={option.text} inline />
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-end' }}>
                    {isProvisionalKey && (
                      <span
                        style={{
                          fontSize: '10px',
                          fontWeight: '700',
                          color: '#22543D',
                          backgroundColor: '#9AE6B4',
                          padding: '2px 6px',
                          borderRadius: '4px',
                        }}
                      >
                        ✓ Official Key
                      </span>
                    )}
                    {isMarkedByCandidate && (
                      <span
                        style={{
                          fontSize: '10px',
                          fontWeight: '700',
                          color: '#2B6CB0',
                          backgroundColor: '#BEE3F8',
                          padding: '2px 6px',
                          borderRadius: '4px',
                        }}
                      >
                        Your Response
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Detailed Solution / Explanation */}
          <div
            style={{
              backgroundColor: '#F7FAFC',
              borderRadius: '8px',
              padding: '12px',
              border: '1px solid #EDF2F7',
              marginBottom: '16px',
            }}
          >
            <div
              style={{
                fontSize: '11px',
                fontWeight: '700',
                textTransform: 'uppercase',
                color: '#4A5568',
                marginBottom: '4px',
              }}
            >
              Academic Solution & Derivation:
            </div>
            <div style={{ fontSize: '13px', color: '#2D3748', lineHeight: '1.5' }}>
              <FormulaRenderer content={currentQuestion.explanation} />
            </div>
          </div>

          {/* Answer Key Challenge / Objection Section */}
          <div
            style={{
              borderTop: '1px solid #E2E8F0',
              paddingTop: '12px',
              display: 'flex',
              flexDirection: 'column',
              gap: '8px',
            }}
          >
            {currentQuestion.isChallenged ? (
              <div
                data-testid="challenge-receipt-badge"
                style={{
                  backgroundColor: '#FAF5FF',
                  border: '1px solid #D6BCFA',
                  borderRadius: '8px',
                  padding: '10px',
                  fontSize: '12px',
                }}
              >
                <div style={{ fontWeight: '700', color: '#6B46C1' }}>
                  ✓ Answer Key Objection Submitted (Under Expert Review)
                </div>
                <div style={{ color: '#4A5568', marginTop: '2px' }}>
                  Ref: {currentQuestion.challengeDetails?.transactionReference} • Claimed Option: (
                  {currentQuestion.challengeDetails?.claimedCorrectOptionId})
                </div>
                <div style={{ color: '#718096', fontSize: '11px', marginTop: '2px' }}>
                  Citation: {currentQuestion.challengeDetails?.bookCitation.bookTitle} (Page{' '}
                  {currentQuestion.challengeDetails?.bookCitation.pageNumber})
                </div>
              </div>
            ) : (
              <button
                data-testid="btn-raise-challenge"
                onClick={() => handleOpenChallenge(currentQuestion)}
                style={{
                  backgroundColor: '#6B46C1',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '8px',
                  padding: '10px',
                  fontSize: '13px',
                  fontWeight: '700',
                  cursor: 'pointer',
                }}
              >
                ⚖️ Raise Answer Key Challenge / Objection (₹200)
              </button>
            )}

            {/* Navigation Buttons */}
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '8px', marginTop: '4px' }}>
              <button
                data-testid="btn-prev-question"
                onClick={() => controller.prevQuestion()}
                disabled={reviewState.currentIndex === 0}
                style={{
                  flex: 1,
                  backgroundColor: '#EDF2F7',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '8px',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor: reviewState.currentIndex === 0 ? 'not-allowed' : 'pointer',
                  color: reviewState.currentIndex === 0 ? '#A0AEC0' : '#2D3748',
                }}
              >
                ← Previous
              </button>

              <button
                data-testid="btn-next-question"
                onClick={() => controller.nextQuestion()}
                disabled={reviewState.currentIndex >= filteredQuestions.length - 1}
                style={{
                  flex: 1,
                  backgroundColor: '#EDF2F7',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '8px',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor:
                    reviewState.currentIndex >= filteredQuestions.length - 1
                      ? 'not-allowed'
                      : 'pointer',
                  color:
                    reviewState.currentIndex >= filteredQuestions.length - 1
                      ? '#A0AEC0'
                      : '#2D3748',
                }}
              >
                Next →
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: '32px', color: '#718096' }}>
          No questions match the selected filter.
        </div>
      )}

      {/* 6. Challenge Objection Modal */}
      {reviewState.isChallengeModalOpen && (
        <div
          data-testid="challenge-modal"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '16px',
          }}
        >
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '12px',
              maxWidth: '460px',
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              padding: '20px',
              boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '12px',
                borderBottom: '1px solid #E2E8F0',
                paddingBottom: '8px',
              }}
            >
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1A365D' }}>
                Raise Answer Key Challenge
              </h3>
              <button
                data-testid="btn-cancel-challenge"
                onClick={() => controller.closeChallengeModal()}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '20px',
                  color: '#A0AEC0',
                  cursor: 'pointer',
                }}
              >
                ✕
              </button>
            </div>

            {challengeError && (
              <div
                style={{
                  backgroundColor: '#FED7D7',
                  color: '#9B2C2C',
                  fontSize: '12px',
                  padding: '8px',
                  borderRadius: '6px',
                  marginBottom: '10px',
                }}
              >
                {challengeError}
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div>
                <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                  Nature of Objection *
                </label>
                <select
                  data-testid="input-challenge-reason"
                  value={reasonType}
                  onChange={(e) => setReasonType(e.target.value as ChallengeReasonType)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                    marginTop: '4px',
                  }}
                >
                  <option value="INCORRECT_PROVISIONAL_KEY">
                    Provisional Answer Key is Incorrect
                  </option>
                  <option value="QUESTION_AMBIGUOUS_OR_INCORRECT">
                    Question Stem is Incorrect / Ambiguous
                  </option>
                  <option value="MULTIPLE_CORRECT_OPTIONS">
                    Multiple Options are Correct
                  </option>
                  <option value="NO_CORRECT_OPTION">No Correct Option Available</option>
                  <option value="OUT_OF_SYLLABUS">Question is Out of Prescribed Syllabus</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                  Claimed Correct Option
                </label>
                <select
                  value={claimedOption}
                  onChange={(e) => setClaimedOption(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                    marginTop: '4px',
                  }}
                >
                  {currentQuestion?.options.map((opt) => (
                    <option key={opt.id} value={opt.id}>
                      Option ({opt.id})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568' }}>
                  Academic Justification (min 10 characters) *
                </label>
                <textarea
                  data-testid="input-justification"
                  rows={3}
                  placeholder="Provide step-by-step academic justification demonstrating why the provisional key is flawed..."
                  value={justification}
                  onChange={(e) => setJustification(e.target.value)}
                  style={{
                    width: '100%',
                    boxSizing: 'border-box',
                    padding: '8px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    fontSize: '12px',
                    marginTop: '4px',
                  }}
                />
              </div>

              {/* Standard Reference Citation */}
              <div
                style={{
                  backgroundColor: '#F7FAFC',
                  padding: '10px',
                  borderRadius: '8px',
                  border: '1px solid #E2E8F0',
                }}
              >
                <div
                  style={{
                    fontSize: '11px',
                    fontWeight: '700',
                    color: '#2B6CB0',
                    marginBottom: '6px',
                  }}
                >
                  Standard Textbook Reference Citation *
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <input
                    data-testid="input-citation-book"
                    type="text"
                    placeholder="Book Title (e.g. Introduction to Algorithms / CLRS)"
                    value={bookTitle}
                    onChange={(e) => setBookTitle(e.target.value)}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      padding: '6px 8px',
                      borderRadius: '4px',
                      border: '1px solid #CBD5E0',
                      fontSize: '12px',
                    }}
                  />

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                    <input
                      data-testid="input-citation-author"
                      type="text"
                      placeholder="Author(s)"
                      value={bookAuthor}
                      onChange={(e) => setBookAuthor(e.target.value)}
                      style={{
                        padding: '6px 8px',
                        borderRadius: '4px',
                        border: '1px solid #CBD5E0',
                        fontSize: '12px',
                      }}
                    />

                    <input
                      data-testid="input-citation-page"
                      type="text"
                      placeholder="Page Number (e.g. 652)"
                      value={bookPage}
                      onChange={(e) => setBookPage(e.target.value)}
                      style={{
                        padding: '6px 8px',
                        borderRadius: '4px',
                        border: '1px solid #CBD5E0',
                        fontSize: '12px',
                      }}
                    />
                  </div>
                </div>
              </div>

              {/* Fee Notice */}
              <div
                style={{
                  backgroundColor: '#FEFCBF',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  fontSize: '11px',
                  color: '#744210',
                }}
              >
                Challenge Processing Fee: <strong>₹200 per question</strong>. If the objection is
                upheld by the Subject Matter Committee, the fee will be refunded in full.
              </div>

              <button
                data-testid="btn-submit-challenge"
                onClick={handleSubmitChallenge}
                style={{
                  width: '100%',
                  marginTop: '4px',
                  backgroundColor: '#6B46C1',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '10px',
                  fontSize: '13px',
                  fontWeight: '700',
                  cursor: 'pointer',
                }}
              >
                Submit Objection & Pay ₹200
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
