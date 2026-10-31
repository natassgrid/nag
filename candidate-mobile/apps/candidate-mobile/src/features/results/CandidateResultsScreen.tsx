import React, { useState, useEffect, useMemo } from 'react';
import {
  ResultsController,
  ResultsState,
  CandidateExamResult,
} from './results-controller';
import { SignedQrCodeView } from '../wallet';

export interface CandidateResultsScreenProps {
  controller: ResultsController;
  onNavigateToWallet?: () => void;
  onExitResults?: () => void;
}

export const CandidateResultsScreen: React.FC<CandidateResultsScreenProps> = ({
  controller,
  onNavigateToWallet,
  onExitResults,
}) => {
  const [resultsState, setResultsState] = useState<ResultsState>(controller.getState());
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'SECTIONS' | 'CUTOFFS' | 'VERIFY'>('OVERVIEW');
  const [digiLockerMessage, setDigiLockerMessage] = useState<string | null>(null);

  useEffect(() => {
    return controller.subscribe((state: ResultsState) => {
      setResultsState(state);
    });
  }, [controller]);

  const selectedResult: CandidateExamResult | null = resultsState.selectedResult;

  const verificationResult = useMemo(() => {
    if (!selectedResult) return null;
    return controller.verifyScorecardIntegrity(selectedResult);
  }, [controller, selectedResult]);

  const qrPayload = useMemo(() => {
    if (!selectedResult) return '';
    return controller.generateSignedQrPayload(selectedResult);
  }, [controller, selectedResult]);

  const handleSaveWallet = () => {
    if (selectedResult) {
      controller.saveScorecardToOfflineWallet(selectedResult.id);
      if (onNavigateToWallet) {
        onNavigateToWallet();
      }
    }
  };

  const handlePushDigiLocker = async () => {
    if (selectedResult) {
      const receipt = await controller.pushToDigiLocker(selectedResult.id);
      setDigiLockerMessage(`DigiLocker Document Deposited (ID: ${receipt.docId})`);
    }
  };

  if (!selectedResult) {
    return (
      <div style={{ padding: '32px', textAlign: 'center', color: '#718096' }}>
        No published examination results available.
      </div>
    );
  }

  return (
    <div
      data-testid="candidate-results-screen"
      style={{
        padding: '16px',
        paddingBottom: '80px',
        backgroundColor: '#F7FAFC',
        minHeight: '100%',
      }}
    >
      {/* 1. Exam Selector & Top Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '12px',
        }}
      >
        <select
          data-testid="result-exam-selector"
          value={selectedResult.id}
          onChange={(e) => controller.selectResult(e.target.value)}
          style={{
            flex: 1,
            padding: '8px 12px',
            borderRadius: '8px',
            border: '1px solid #CBD5E0',
            backgroundColor: '#FFFFFF',
            fontSize: '13px',
            fontWeight: '700',
            color: '#1A365D',
            marginRight: '8px',
          }}
        >
          {resultsState.resultsList.map((r) => (
            <option key={r.id} value={r.id}>
              {r.examCode} — {r.examTitle}
            </option>
          ))}
        </select>

        {onExitResults && (
          <button
            onClick={onExitResults}
            style={{
              background: 'none',
              border: '1px solid #CBD5E0',
              borderRadius: '6px',
              padding: '6px 10px',
              fontSize: '12px',
              cursor: 'pointer',
              color: '#4A5568',
            }}
          >
            Done
          </button>
        )}
      </div>

      {/* 2. Official Rank & Percentile Banner */}
      <div
        style={{
          backgroundColor: '#1A365D',
          color: '#FFFFFF',
          borderRadius: '12px',
          padding: '16px',
          marginBottom: '16px',
          boxShadow: '0 4px 6px rgba(0,0,0,0.08)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <span
              style={{
                fontSize: '10px',
                fontFamily: 'monospace',
                backgroundColor: 'rgba(255,255,255,0.2)',
                padding: '2px 6px',
                borderRadius: '4px',
              }}
            >
              NTA OFFICIAL SCORECARD
            </span>
            <h2 style={{ margin: '4px 0 0 0', fontSize: '18px', fontWeight: '800' }}>
              {selectedResult.candidateName}
            </h2>
            <div style={{ fontSize: '11px', color: '#CBD5E0', marginTop: '2px' }}>
              Roll: {selectedResult.rollNumber} • {selectedResult.conductingAuthority}
            </div>
          </div>

          <div
            data-testid="qualification-status-badge"
            style={{
              backgroundColor:
                selectedResult.qualifyingStatus === 'QUALIFIED' ? '#38A169' : '#E53E3E',
              color: '#FFFFFF',
              fontWeight: '800',
              fontSize: '11px',
              padding: '4px 10px',
              borderRadius: '12px',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            {selectedResult.qualifyingStatus}
          </div>
        </div>

        {/* Primary Metric Grid (Percentile, AIR, Category Rank) */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '8px',
            marginTop: '16px',
          }}
        >
          <div
            data-testid="percentile-badge"
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              padding: '10px 8px',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: '10px', color: '#CBD5E0', textTransform: 'uppercase' }}>
              NTA Percentile
            </div>
            <div style={{ fontSize: '18px', fontWeight: '800', color: '#ECC94B', marginTop: '2px' }}>
              {selectedResult.percentile}%tile
            </div>
          </div>

          <div
            data-testid="rank-card-air"
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              padding: '10px 8px',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: '10px', color: '#CBD5E0', textTransform: 'uppercase' }}>
              All-India Rank
            </div>
            <div style={{ fontSize: '18px', fontWeight: '800', color: '#FFFFFF', marginTop: '2px' }}>
              #{selectedResult.allIndiaRank}
            </div>
          </div>

          <div
            data-testid="rank-card-category"
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              padding: '10px 8px',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: '10px', color: '#CBD5E0', textTransform: 'uppercase' }}>
              Category Rank
            </div>
            <div style={{ fontSize: '18px', fontWeight: '800', color: '#FFFFFF', marginTop: '2px' }}>
              #{selectedResult.categoryRank}
            </div>
          </div>
        </div>
      </div>

      {/* 3. Section Navigation Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '6px',
          overflowX: 'auto',
          paddingBottom: '8px',
          marginBottom: '14px',
        }}
      >
        {(
          [
            { id: 'OVERVIEW', label: 'Score Summary' },
            { id: 'SECTIONS', label: 'Section Breakdown' },
            { id: 'CUTOFFS', label: 'Category Cutoffs' },
            { id: 'VERIFY', label: 'Digital QR & Ledger' },
          ] as const
        ).map((tab) => {
          const active = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                backgroundColor: active ? '#2B6CB0' : '#EDF2F7',
                color: active ? '#FFFFFF' : '#4A5568',
                border: 'none',
                borderRadius: '8px',
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: active ? '700' : '600',
                cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* 4. Tab Contents */}

      {/* Tab: OVERVIEW (Multi-shift normalization) */}
      {activeTab === 'OVERVIEW' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '10px',
              padding: '14px',
              border: '1px solid #E2E8F0',
            }}
          >
            <h3 style={{ margin: '0 0 10px 0', fontSize: '14px', color: '#2D3748' }}>
              Multi-Shift Score Normalization
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div
                style={{
                  backgroundColor: '#F7FAFC',
                  borderRadius: '8px',
                  padding: '10px',
                }}
              >
                <div style={{ fontSize: '11px', color: '#718096' }}>Raw Total Marks</div>
                <div style={{ fontSize: '18px', fontWeight: '800', color: '#4A5568' }}>
                  {selectedResult.rawTotalMarks} / {selectedResult.maxScore}
                </div>
              </div>

              <div
                style={{
                  backgroundColor: '#EBF8FF',
                  borderRadius: '8px',
                  padding: '10px',
                  border: '1px solid #BEE3F8',
                }}
              >
                <div style={{ fontSize: '11px', color: '#2B6CB0' }}>Normalized Marks</div>
                <div
                  data-testid="normalized-score-text"
                  style={{ fontSize: '18px', fontWeight: '800', color: '#2B6CB0' }}
                >
                  {selectedResult.normalizedTotalMarks} / {selectedResult.maxScore}
                </div>
              </div>
            </div>

            <div
              style={{
                marginTop: '10px',
                fontSize: '11px',
                color: '#718096',
                lineHeight: '1.4',
              }}
            >
              {selectedResult.isMultiShiftNormalized ? (
                <>
                  ⚡ Multi-shift normalization applied using NTA Equi-percentile procedure.
                  Calculated Z-Score: <strong>+{selectedResult.zScore}</strong>.
                </>
              ) : (
                'Single-session direct raw examination score.'
              )}
            </div>
          </div>

          {/* Candidate Shift & Authority Info */}
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '10px',
              padding: '14px',
              border: '1px solid #E2E8F0',
              fontSize: '12px',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
            }}
          >
            <div>
              <strong>Exam Shift:</strong> {selectedResult.shiftName}
            </div>
            <div>
              <strong>Declared Date:</strong> {selectedResult.declaredDate}
            </div>
            <div>
              <strong>Total Candidates Appeared:</strong>{' '}
              {selectedResult.totalCandidatesAppeared.toLocaleString()}
            </div>
            <div>
              <strong>Candidate Category:</strong> {selectedResult.candidateCategory}
            </div>
          </div>
        </div>
      )}

      {/* Tab: SECTIONS (Section-wise Breakdown) */}
      {activeTab === 'SECTIONS' && (
        <div
          data-testid="section-performance-breakdown"
          style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}
        >
          {selectedResult.sectionScores.map((sec) => (
            <div
              key={sec.sectionId}
              style={{
                backgroundColor: '#FFFFFF',
                borderRadius: '10px',
                padding: '14px',
                border: '1px solid #E2E8F0',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '8px',
                }}
              >
                <div style={{ fontWeight: '700', fontSize: '13px', color: '#1A365D' }}>
                  {sec.sectionName}
                </div>
                <span
                  style={{
                    fontSize: '10px',
                    fontWeight: '700',
                    color: sec.status === 'CLEARED' ? '#22543D' : '#9B2C2C',
                    backgroundColor: sec.status === 'CLEARED' ? '#C6F6D5' : '#FED7D7',
                    padding: '2px 6px',
                    borderRadius: '4px',
                  }}
                >
                  {sec.status}
                </span>
              </div>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(3, 1fr)',
                  gap: '6px',
                  textAlign: 'center',
                  fontSize: '11px',
                }}
              >
                <div style={{ backgroundColor: '#F7FAFC', padding: '6px', borderRadius: '6px' }}>
                  <div style={{ color: '#718096' }}>Score</div>
                  <div style={{ fontWeight: '700', color: '#2B6CB0' }}>
                    {sec.normalizedScore}/{sec.maxMarks}
                  </div>
                </div>

                <div style={{ backgroundColor: '#F7FAFC', padding: '6px', borderRadius: '6px' }}>
                  <div style={{ color: '#718096' }}>Accuracy</div>
                  <div style={{ fontWeight: '700', color: '#38A169' }}>{sec.accuracyRate}%</div>
                </div>

                <div style={{ backgroundColor: '#F7FAFC', padding: '6px', borderRadius: '6px' }}>
                  <div style={{ color: '#718096' }}>Percentile</div>
                  <div style={{ fontWeight: '700', color: '#805AD5' }}>
                    {sec.percentileScore}%tile
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tab: CUTOFFS (Category-wise Cutoff Comparison) */}
      {activeTab === 'CUTOFFS' && (
        <div
          data-testid="category-cutoffs-table"
          style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '10px',
            padding: '14px',
            border: '1px solid #E2E8F0',
          }}
        >
          <h3 style={{ margin: '0 0 10px 0', fontSize: '14px', color: '#2D3748' }}>
            Category-Wise Qualifying Cutoff Marks
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {selectedResult.categoryCutoffs.map((cutoff) => {
              const isCandidateCat = cutoff.category === selectedResult.candidateCategory;

              return (
                <div
                  key={cutoff.category}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    backgroundColor: isCandidateCat ? '#EBF8FF' : '#F7FAFC',
                    border: isCandidateCat ? '1px solid #90CDF4' : '1px solid #EDF2F7',
                  }}
                >
                  <div>
                    <div style={{ fontSize: '12px', fontWeight: '700', color: '#2D3748' }}>
                      {cutoff.categoryLabel}
                      {isCandidateCat && (
                        <span
                          style={{
                            marginLeft: '6px',
                            fontSize: '10px',
                            backgroundColor: '#3182CE',
                            color: '#FFFFFF',
                            padding: '1px 5px',
                            borderRadius: '3px',
                          }}
                        >
                          You
                        </span>
                      )}
                    </div>
                    <div style={{ fontSize: '11px', color: '#718096' }}>
                      Cutoff Percentile: {cutoff.qualifyingPercentile}%tile
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '14px', fontWeight: '800', color: '#2B6CB0' }}>
                      {cutoff.qualifyingCutoffMarks} Marks
                    </div>
                    <span
                      style={{
                        fontSize: '10px',
                        fontWeight: '700',
                        color: cutoff.qualified ? '#22543D' : '#9B2C2C',
                      }}
                    >
                      {cutoff.qualified ? 'QUALIFIED' : 'NOT QUALIFIED'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tab: VERIFY (Digital QR & Cryptographic Ledger Proof) */}
      {activeTab === 'VERIFY' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Integrated Signed QR Code View from wallet */}
          <div
            data-testid="results-qr-code-container"
            style={{ display: 'flex', justifyContent: 'center' }}
          >
            <SignedQrCodeView
              payload={qrPayload}
              size={200}
              label={`${selectedResult.examCode} • AIR #${selectedResult.allIndiaRank}`}
              showVerificationBadge={true}
              algorithm={selectedResult.verification.signatureAlgorithm}
            />
          </div>

          {/* Cryptographic Ledger Proof */}
          <div
            style={{
              backgroundColor: '#FFFFFF',
              borderRadius: '10px',
              padding: '14px',
              border: '1px solid #E2E8F0',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
            }}
          >
            <div style={{ fontWeight: '700', fontSize: '12px', color: '#2D3748' }}>
              Immutable Academic Ledger Proof
            </div>
            <div>
              <strong>Merkle Root:</strong>{' '}
              <span style={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                {selectedResult.verification.merkleRoot}
              </span>
            </div>
            <div>
              <strong>Ledger Proof Hash:</strong>{' '}
              <span style={{ fontFamily: 'monospace', wordBreak: 'break-all' }}>
                {selectedResult.verification.ledgerProofHash}
              </span>
            </div>
            <div>
              <strong>Ledger Block Height:</strong> #{selectedResult.verification.blockHeight}
            </div>
            <div>
              <strong>Issuer Key Fingerprint:</strong>{' '}
              {selectedResult.verification.issuerPublicKeyFingerprint}
            </div>
            {verificationResult && (
              <div
                style={{
                  marginTop: '4px',
                  color: '#22543D',
                  backgroundColor: '#F0FFF4',
                  padding: '6px',
                  borderRadius: '4px',
                }}
              >
                ✓ {verificationResult.message}
              </div>
            )}
          </div>

          {/* Actions: Save to Wallet & Push to DigiLocker */}
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              data-testid="btn-save-wallet"
              onClick={handleSaveWallet}
              style={{
                flex: 1,
                backgroundColor: resultsState.isSavedToWallet ? '#38A169' : '#2B6CB0',
                color: '#FFFFFF',
                border: 'none',
                borderRadius: '8px',
                padding: '10px',
                fontSize: '12px',
                fontWeight: '700',
                cursor: 'pointer',
              }}
            >
              {resultsState.isSavedToWallet ? '✓ Saved in Offline Wallet' : '🎟️ Save to Wallet'}
            </button>

            <button
              data-testid="btn-push-digilocker"
              onClick={handlePushDigiLocker}
              disabled={resultsState.pushingDigiLocker || selectedResult.digiLockerPushed}
              style={{
                flex: 1,
                backgroundColor: selectedResult.digiLockerPushed ? '#805AD5' : '#4A5568',
                color: '#FFFFFF',
                border: 'none',
                borderRadius: '8px',
                padding: '10px',
                fontSize: '12px',
                fontWeight: '700',
                cursor: selectedResult.digiLockerPushed ? 'default' : 'pointer',
              }}
            >
              {selectedResult.digiLockerPushed
                ? '✓ Pushed to DigiLocker'
                : resultsState.pushingDigiLocker
                ? 'Pushing...'
                : '☁️ Deposit to DigiLocker'}
            </button>
          </div>

          {digiLockerMessage && (
            <div
              style={{
                fontSize: '11px',
                color: '#22543D',
                backgroundColor: '#C6F6D5',
                padding: '8px',
                borderRadius: '6px',
                textAlign: 'center',
              }}
            >
              {digiLockerMessage}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
