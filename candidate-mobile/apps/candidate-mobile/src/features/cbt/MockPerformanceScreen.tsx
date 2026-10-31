import React from 'react';
import {
  LocalGradingBreakdown,
  SectionPerformanceSummary,
  TopicMasterySummary,
} from './cbt-session-controller';

export interface MockPerformanceScreenProps {
  grading: LocalGradingBreakdown;
  onRetakeTest?: () => void;
  onReturnToCatalog?: () => void;
  onViewWallet?: () => void;
}

export const MockPerformanceScreen: React.FC<MockPerformanceScreenProps> = ({
  grading,
  onRetakeTest,
  onReturnToCatalog,
  onViewWallet,
}) => {
  const isPass = grading.accuracyPercentage >= 50;

  return (
    <div
      data-testid="mock-performance-screen"
      style={{
        maxWidth: '560px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Header */}
      <div style={{ textAlign: 'center', marginBottom: '20px' }}>
        <span style={{ fontSize: '11px', fontWeight: '700', color: '#2B6CB0', textTransform: 'uppercase' }}>
          NAG Instant Local Grading Engine
        </span>
        <h2 style={{ fontSize: '20px', fontWeight: '700', margin: '4px 0 2px 0', color: '#1A365D' }}>
          Mock CBT Performance Breakdown
        </h2>
        <span style={{ fontSize: '12px', color: '#718096' }}>
          Evaluated locally with zero cloud latency
        </span>
      </div>

      {/* Hero Score Card */}
      <div
        data-testid="hero-score-card"
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          padding: '20px',
          border: '1px solid #E2E8F0',
          boxShadow: '0 4px 10px rgba(0,0,0,0.06)',
          textAlign: 'center',
          marginBottom: '16px',
        }}
      >
        <span style={{ fontSize: '12px', color: '#718096', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          Total Score Obtained
        </span>
        <div style={{ margin: '8px 0', display: 'flex', alignItems: 'baseline', justifyContent: 'center', gap: '6px' }}>
          <span
            data-testid="raw-score-value"
            style={{ fontSize: '36px', fontWeight: '800', color: isPass ? '#22543D' : '#C53030' }}
          >
            {grading.rawScore.toFixed(1)}
          </span>
          <span style={{ fontSize: '18px', color: '#718096', fontWeight: '500' }}>
            / {grading.maxMarks.toFixed(1)}
          </span>
        </div>

        {/* Accuracy & Percentage Chips */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '10px' }}>
          <span
            data-testid="accuracy-percentage-badge"
            style={{
              padding: '4px 10px',
              borderRadius: '20px',
              backgroundColor: '#EBF8FF',
              color: '#2B6CB0',
              fontSize: '12px',
              fontWeight: '700',
            }}
          >
            Accuracy: {grading.accuracyPercentage.toFixed(1)}%
          </span>

          <span
            data-testid="overall-percentage-badge"
            style={{
              padding: '4px 10px',
              borderRadius: '20px',
              backgroundColor: '#F0FFF4',
              color: '#22543D',
              fontSize: '12px',
              fontWeight: '700',
            }}
          >
            Marks: {grading.percentage.toFixed(1)}%
          </span>
        </div>

        {/* 4-Stat Metric Strip */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(4, 1fr)',
            gap: '8px',
            marginTop: '20px',
            paddingTop: '16px',
            borderTop: '1px solid #EDF2F7',
          }}
        >
          <div>
            <span style={{ fontSize: '10px', color: '#718096', textTransform: 'uppercase', display: 'block' }}>
              Attempted
            </span>
            <strong data-testid="stat-attempted" style={{ fontSize: '16px', color: '#2D3748' }}>
              {grading.totalAttempted}
            </strong>
          </div>
          <div>
            <span style={{ fontSize: '10px', color: '#718096', textTransform: 'uppercase', display: 'block' }}>
              Correct
            </span>
            <strong data-testid="stat-correct" style={{ fontSize: '16px', color: '#38A169' }}>
              {grading.totalCorrect}
            </strong>
          </div>
          <div>
            <span style={{ fontSize: '10px', color: '#718096', textTransform: 'uppercase', display: 'block' }}>
              Incorrect
            </span>
            <strong data-testid="stat-incorrect" style={{ fontSize: '16px', color: '#E53E3E' }}>
              {grading.totalIncorrect}
            </strong>
          </div>
          <div>
            <span style={{ fontSize: '10px', color: '#718096', textTransform: 'uppercase', display: 'block' }}>
              Unattempted
            </span>
            <strong data-testid="stat-unattempted" style={{ fontSize: '16px', color: '#718096' }}>
              {grading.totalUnattempted}
            </strong>
          </div>
        </div>

        {/* Positive vs Negative Marks */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-around',
            backgroundColor: '#F7FAFC',
            borderRadius: '6px',
            padding: '8px',
            marginTop: '12px',
            fontSize: '12px',
          }}
        >
          <span style={{ color: '#276749' }}>
            Earned: <strong>+{grading.positiveMarksObtained.toFixed(1)}</strong>
          </span>
          <span style={{ color: '#C53030' }}>
            Penalty Deductions: <strong>-{grading.negativeMarksDeducted.toFixed(1)}</strong>
          </span>
        </div>
      </div>

      {/* Section-Wise Performance Table */}
      <div
        data-testid="section-performance-card"
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          padding: '16px',
          border: '1px solid #E2E8F0',
          marginBottom: '16px',
        }}
      >
        <h3 style={{ fontSize: '14px', fontWeight: '700', margin: '0 0 12px 0', color: '#2D3748' }}>
          Section-Wise Accuracy & Marks
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {grading.sectionBreakdowns.map((sec: SectionPerformanceSummary) => (
            <div
              key={sec.sectionId}
              data-testid={`sec-row-${sec.sectionId}`}
              style={{
                padding: '10px 12px',
                backgroundColor: '#F7FAFC',
                borderRadius: '8px',
                border: '1px solid #EDF2F7',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                <strong style={{ fontSize: '13px', color: '#1A202C' }}>{sec.sectionTitle}</strong>
                <span style={{ fontSize: '13px', fontWeight: '700', color: '#2B6CB0' }}>
                  {sec.scoreObtained.toFixed(1)} / {sec.maxScore.toFixed(1)}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#718096' }}>
                <span>
                  Attempted: {sec.attemptedQuestions}/{sec.totalQuestions} ({sec.correctQuestions} correct)
                </span>
                <span style={{ fontWeight: '600', color: '#276749' }}>
                  Accuracy: {sec.accuracyPercentage.toFixed(0)}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Topic Mastery Progress Bars */}
      <div
        data-testid="topic-mastery-card"
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '12px',
          padding: '16px',
          border: '1px solid #E2E8F0',
          marginBottom: '16px',
        }}
      >
        <h3 style={{ fontSize: '14px', fontWeight: '700', margin: '0 0 12px 0', color: '#2D3748' }}>
          Topic Mastery Breakdown
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {grading.topicBreakdowns.map((topic: TopicMasterySummary) => (
            <div key={topic.topic} data-testid={`topic-bar-${topic.topic.replace(/\s+/g, '-')}`}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', marginBottom: '4px' }}>
                <span style={{ fontWeight: '600', color: '#2D3748' }}>{topic.topic}</span>
                <span style={{ color: '#718096' }}>
                  {topic.correctCount}/{topic.totalQuestions} ({topic.masteryPercentage.toFixed(0)}%)
                </span>
              </div>
              <div
                style={{
                  height: '8px',
                  backgroundColor: '#EDF2F7',
                  borderRadius: '4px',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    height: '100%',
                    width: `${topic.masteryPercentage}%`,
                    backgroundColor:
                      topic.masteryPercentage >= 75
                        ? '#38A169'
                        : topic.masteryPercentage >= 50
                        ? '#ECC94B'
                        : '#E53E3E',
                    borderRadius: '4px',
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Hardware-Signed Submission Receipt Card */}
      <div
        data-testid="hardware-submission-receipt"
        style={{
          backgroundColor: '#F7FAFC',
          borderRadius: '12px',
          padding: '14px',
          border: '1px dashed #CBD5E0',
          marginBottom: '20px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
          <span style={{ fontSize: '12px' }}>🛡️</span>
          <strong style={{ fontSize: '12px', color: '#2D3748', textTransform: 'uppercase' }}>
            Hardware Keystore Tamper-Evident Receipt
          </strong>
        </div>

        <div style={{ fontSize: '11px', color: '#4A5568', display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <div>
            <span style={{ color: '#718096' }}>Receipt ID: </span>
            <code data-testid="receipt-id-value" style={{ fontWeight: '600' }}>{grading.receipt.receiptId}</code>
          </div>
          <div>
            <span style={{ color: '#718096' }}>SHA-256 Digest: </span>
            <code style={{ fontSize: '10px' }}>{grading.receipt.checksum.slice(0, 32)}...</code>
          </div>
          <div>
            <span style={{ color: '#718096' }}>Signer Key: </span>
            <span>{grading.receipt.signerKeyAlias} (ECDSA P-256)</span>
          </div>
        </div>
      </div>

      {/* Bottom Actions */}
      <div style={{ display: 'flex', gap: '10px' }}>
        {onRetakeTest && (
          <button
            data-testid="retake-test-btn"
            onClick={onRetakeTest}
            style={{
              flex: 1,
              padding: '12px',
              backgroundColor: '#2B6CB0',
              color: '#FFFFFF',
              fontWeight: '600',
              fontSize: '13px',
              borderRadius: '6px',
              border: 'none',
              cursor: 'pointer',
            }}
          >
            Retake Mock Test
          </button>
        )}

        {onViewWallet && (
          <button
            data-testid="view-wallet-from-results-btn"
            onClick={onViewWallet}
            style={{
              flex: 1,
              padding: '12px',
              backgroundColor: '#EDF2F7',
              color: '#2B6CB0',
              fontWeight: '600',
              fontSize: '13px',
              borderRadius: '6px',
              border: '1px solid #CBD5E0',
              cursor: 'pointer',
            }}
          >
            Admit Card Wallet 🎟️
          </button>
        )}

        {onReturnToCatalog && (
          <button
            data-testid="return-catalog-btn"
            onClick={onReturnToCatalog}
            style={{
              flex: 1,
              padding: '12px',
              backgroundColor: '#EDF2F7',
              color: '#4A5568',
              fontWeight: '600',
              fontSize: '13px',
              borderRadius: '6px',
              border: '1px solid #CBD5E0',
              cursor: 'pointer',
            }}
          >
            Exam Catalog
          </button>
        )}
      </div>
    </div>
  );
};
