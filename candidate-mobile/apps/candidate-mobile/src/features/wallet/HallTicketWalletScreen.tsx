import React, { useState, useEffect } from 'react';
import {
  WalletController,
  WalletState,
  DigitalAdmitCard,
  QrVerificationResult,
} from './wallet-controller';
import { SignedQrCodeView } from './SignedQrCodeView';

export interface HallTicketWalletScreenProps {
  controller: WalletController;
  onNavigateToCatalog?: () => void;
  onLaunchPractice?: (examId: string) => void;
}

export const HallTicketWalletScreen: React.FC<HallTicketWalletScreenProps> = ({
  controller,
  onNavigateToCatalog,
  onLaunchPractice,
}) => {
  const [walletState, setWalletState] = useState<WalletState>(controller.getState());
  const [now, setNow] = useState<number>(Date.now());

  useEffect(() => {
    const unsub = controller.subscribe((s: WalletState) => {
      setWalletState(s);
    });
    const interval = setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => {
      unsub();
      clearInterval(interval);
    };
  }, [controller]);

  const activeCard: DigitalAdmitCard | null = controller.getSelectedAdmitCard();

  const verification: QrVerificationResult | null = activeCard?.signedQrPayload
    ? controller.verifyOfflinePayload(activeCard.signedQrPayload)
    : null;

  const countdown = activeCard
    ? controller.calculateGateCountdown(activeCard.examDate, activeCard.gateClosingTime, now)
    : null;

  return (
    <div
      data-testid="hall-ticket-wallet-screen"
      style={{
        maxWidth: '520px',
        margin: '0 auto',
        padding: '16px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#1A202C',
      }}
    >
      {/* Wallet Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '16px',
        }}
      >
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: '700', margin: 0, color: '#1A365D' }}>
            Offline Admit Card Wallet
          </h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '4px' }}>
            <span
              style={{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                backgroundColor: '#38A169',
              }}
            />
            <span style={{ fontSize: '12px', color: '#4A5568', fontWeight: '500' }}>
              Stored in Android Keystore (EncryptedSharedPreferences)
            </span>
          </div>
        </div>

        {onNavigateToCatalog && (
          <button
            data-testid="wallet-browse-exams-btn"
            onClick={onNavigateToCatalog}
            style={{
              padding: '8px 12px',
              fontSize: '12px',
              fontWeight: '600',
              color: '#2B6CB0',
              backgroundColor: '#EBF8FF',
              border: '1px solid #BEE3F8',
              borderRadius: '6px',
              cursor: 'pointer',
            }}
          >
            + Apply Exams
          </button>
        )}
      </div>

      {/* Card Selector Tabs if multiple */}
      {walletState.admitCards.length > 1 && (
        <div
          data-testid="admit-card-selector-tabs"
          style={{
            display: 'flex',
            gap: '8px',
            overflowX: 'auto',
            paddingBottom: '8px',
            marginBottom: '16px',
          }}
        >
          {walletState.admitCards.map((card) => {
            const isSelected = card.hallTicketNumber === activeCard?.hallTicketNumber;
            return (
              <button
                key={card.hallTicketNumber}
                data-testid={`tab-${card.hallTicketNumber}`}
                onClick={() => controller.selectAdmitCard(card.hallTicketNumber)}
                style={{
                  padding: '8px 14px',
                  borderRadius: '20px',
                  border: isSelected ? '2px solid #2B6CB0' : '1px solid #CBD5E0',
                  backgroundColor: isSelected ? '#EBF8FF' : '#FFFFFF',
                  color: isSelected ? '#2B6CB0' : '#4A5568',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor: 'pointer',
                  whiteSpace: 'nowrap',
                }}
              >
                {card.examCode}
              </button>
            );
          })}
        </div>
      )}

      {/* If No Admit Cards */}
      {walletState.admitCards.length === 0 && (
        <div
          data-testid="wallet-empty-state"
          style={{
            padding: '40px 20px',
            textAlign: 'center',
            backgroundColor: '#F7FAFC',
            borderRadius: '12px',
            border: '2px dashed #E2E8F0',
          }}
        >
          <p style={{ fontSize: '15px', color: '#4A5568', margin: '0 0 12px 0' }}>
            No Hall Tickets downloaded yet.
          </p>
          {onNavigateToCatalog && (
            <button
              onClick={onNavigateToCatalog}
              style={{
                padding: '10px 18px',
                backgroundColor: '#2B6CB0',
                color: '#FFFFFF',
                borderRadius: '6px',
                border: 'none',
                fontWeight: '600',
                cursor: 'pointer',
              }}
            >
              Browse Examination Catalog
            </button>
          )}
        </div>
      )}

      {/* Active Admit Card Pass View */}
      {activeCard && (
        <div
          data-testid="admit-card-detail-view"
          style={{
            backgroundColor: '#FFFFFF',
            borderRadius: '14px',
            border: '1px solid #CBD5E0',
            boxShadow: '0 6px 16px rgba(0, 0, 0, 0.08)',
            overflow: 'hidden',
          }}
        >
          {/* Header Banner */}
          <div
            style={{
              padding: '16px',
              backgroundColor: '#1A365D',
              color: '#FFFFFF',
              borderBottom: '3px solid #ECC94B',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <span style={{ fontSize: '11px', letterSpacing: '1px', textTransform: 'uppercase', color: '#CBD5E0' }}>
                  NextGen Assessment Grid (NAG)
                </span>
                <h3 style={{ fontSize: '16px', fontWeight: '700', margin: '4px 0 2px 0' }}>
                  {activeCard.examTitle}
                </h3>
                <span style={{ fontSize: '12px', color: '#E2E8F0' }}>{activeCard.shiftName}</span>
              </div>
              <span
                data-testid="hall-ticket-badge"
                style={{
                  padding: '4px 8px',
                  backgroundColor: 'rgba(255,255,255,0.15)',
                  borderRadius: '4px',
                  fontFamily: 'monospace',
                  fontSize: '12px',
                  fontWeight: '700',
                  color: '#FAF089',
                }}
              >
                {activeCard.hallTicketNumber}
              </span>
            </div>
          </div>

          {/* Gate Countdown Alert */}
          {countdown && (
            <div
              data-testid="gate-countdown-banner"
              style={{
                padding: '10px 16px',
                backgroundColor: countdown.isClosed ? '#FED7D7' : '#FEFCBF',
                color: countdown.isClosed ? '#9B2C2C' : '#744210',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                fontSize: '13px',
                fontWeight: '600',
              }}
            >
              <span>Gate Closing Time: {activeCard.gateClosingTime} hrs</span>
              <span style={{ fontFamily: 'monospace' }}>
                {countdown.isClosed ? 'GATE CLOSED' : countdown.formatted}
              </span>
            </div>
          )}

          {/* QR Code Section */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              padding: '20px 16px',
              backgroundColor: '#F7FAFC',
              borderBottom: '1px dashed #CBD5E0',
            }}
          >
            <SignedQrCodeView
              payload={
                activeCard.signedQrPayload
                  ? JSON.stringify(activeCard.signedQrPayload)
                  : activeCard.hallTicketNumber
              }
              size={190}
              label={activeCard.hallTicketNumber}
              showVerificationBadge={true}
              algorithm={activeCard.signedQrPayload?.algorithm || 'ECDSA_P256'}
            />
            {verification && (
              <span
                data-testid="qr-offline-verified-text"
                style={{
                  fontSize: '11px',
                  color: verification.valid ? '#276749' : '#C53030',
                  fontWeight: '600',
                  marginTop: '6px',
                }}
              >
                {verification.message}
              </span>
            )}
          </div>

          {/* Candidate & Venue Particulars */}
          <div style={{ padding: '16px' }}>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: '12px',
                marginBottom: '16px',
              }}
            >
              <div>
                <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Candidate Name</span>
                <p style={{ margin: '2px 0 0 0', fontWeight: '700', fontSize: '14px', color: '#2D3748' }}>
                  {activeCard.candidateName}
                </p>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Date of Exam</span>
                <p style={{ margin: '2px 0 0 0', fontWeight: '700', fontSize: '14px', color: '#2D3748' }}>
                  {activeCard.examDate}
                </p>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Reporting Time</span>
                <p style={{ margin: '2px 0 0 0', fontWeight: '700', fontSize: '14px', color: '#2B6CB0' }}>
                  {activeCard.reportingTime} AM
                </p>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: '#718096', textTransform: 'uppercase' }}>Test Duration</span>
                <p style={{ margin: '2px 0 0 0', fontWeight: '700', fontSize: '14px', color: '#2D3748' }}>
                  {activeCard.examStartTime} - {activeCard.examEndTime}
                </p>
              </div>
            </div>

            {/* Examination Centre Details */}
            <div
              data-testid="admit-card-centre-box"
              style={{
                padding: '12px',
                backgroundColor: '#EDF2F7',
                borderRadius: '8px',
                marginBottom: '16px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '11px', fontWeight: '700', color: '#4A5568', textTransform: 'uppercase' }}>
                  Examination Centre
                </span>
                <span style={{ fontSize: '11px', fontFamily: 'monospace', color: '#718096' }}>
                  {activeCard.centreId}
                </span>
              </div>
              <h4 style={{ margin: '4px 0 2px 0', fontSize: '13px', fontWeight: '700', color: '#1A202C' }}>
                {activeCard.centreName}
              </h4>
              <p style={{ margin: '0', fontSize: '12px', color: '#4A5568', lineHeight: 1.4 }}>
                {activeCard.centreAddress}, {activeCard.centreCity}, {activeCard.centreState} - {activeCard.centrePincode}
              </p>
              {activeCard.centreGpsCoordinates && (
                <div style={{ marginTop: '6px', fontSize: '11px', color: '#2B6CB0', fontWeight: '600' }}>
                  GPS: {activeCard.centreGpsCoordinates.latitude}° N, {activeCard.centreGpsCoordinates.longitude}° E
                </div>
              )}
            </div>

            {/* Quick Action: Practice CBT */}
            {onLaunchPractice && (
              <button
                data-testid="start-mock-from-admit-card-btn"
                onClick={() => onLaunchPractice(activeCard.examId)}
                style={{
                  width: '100%',
                  padding: '12px',
                  backgroundColor: '#2B6CB0',
                  color: '#FFFFFF',
                  fontWeight: '600',
                  fontSize: '13px',
                  borderRadius: '6px',
                  border: 'none',
                  cursor: 'pointer',
                  marginBottom: '16px',
                }}
              >
                Launch Mock Practice Test for this Exam
              </button>
            )}

            {/* Important Instructions Checklist */}
            <div>
              <span style={{ fontSize: '12px', fontWeight: '700', color: '#2D3748' }}>
                Important Instructions:
              </span>
              <ul style={{ margin: '6px 0 0 0', paddingLeft: '18px', fontSize: '12px', color: '#4A5568', lineHeight: 1.5 }}>
                {activeCard.instructions.map((inst, i) => (
                  <li key={i} style={{ marginBottom: '4px' }}>
                    {inst}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
