import React, { useState, useEffect, useMemo } from 'react';
import {
  DashboardController,
  DashboardState,
  EnrolledExam,
  DigitalAdmitCard,
  DashboardExamFilter,
} from './dashboard-controller';

export interface CandidateDashboardScreenProps {
  controller: DashboardController;
  onStartLiveExam?: (exam: EnrolledExam) => void;
  onOpenAdmitCard?: (card: DigitalAdmitCard) => void;
  onLaunchPractice?: (examId: string, paperId?: string) => void;
  onViewScorecards?: (examId: string) => void;
}

export const CandidateDashboardScreen: React.FC<CandidateDashboardScreenProps> = ({
  controller,
  onStartLiveExam,
  onOpenAdmitCard,
  onLaunchPractice,
  onViewScorecards,
}) => {
  const [dashboardState, setDashboardState] = useState<DashboardState>(controller.getState());

  useEffect(() => {
    return controller.subscribe((state: DashboardState) => {
      setDashboardState(state);
    });
  }, [controller]);

  const filteredExams = useMemo(() => {
    return controller.getFilteredExams();
  }, [controller, dashboardState.activeFilter, dashboardState.enrolledExams]);

  const liveExams = useMemo(() => {
    return dashboardState.enrolledExams.filter((e) => e.status === 'LIVE');
  }, [dashboardState.enrolledExams]);

  const handleLaunchLive = (exam: EnrolledExam) => {
    const launchResult = controller.startLiveExam(exam.id);
    if (launchResult && onStartLiveExam) {
      onStartLiveExam(launchResult.exam);
    }
  };

  const handleOpenAdmit = (exam: EnrolledExam) => {
    const card = controller.openAdmitCard(exam.id);
    if (card && onOpenAdmitCard) {
      onOpenAdmitCard(card);
    }
  };

  const handlePracticeClick = (exam: EnrolledExam) => {
    const papers = controller.getPracticePapers(exam.id);
    if (papers.length > 1) {
      controller.selectPracticeExam(exam.id);
    } else {
      const result = controller.startPracticePaper(exam.id, papers[0]?.paperId);
      if (result && onLaunchPractice) {
        onLaunchPractice(result.examId, result.paperId);
      }
    }
  };

  const filterTabs: { id: DashboardExamFilter; label: string; count?: number }[] = [
    { id: 'ALL', label: 'All Exams', count: dashboardState.kpis.totalRegistered },
    { id: 'LIVE', label: 'Live Now', count: dashboardState.kpis.liveCount },
    { id: 'UPCOMING', label: 'Upcoming', count: dashboardState.kpis.upcomingCount },
    { id: 'PRACTICE', label: 'Practice', count: dashboardState.kpis.practiceCount },
    { id: 'COMPLETED', label: 'Completed' },
  ];

  return (
    <div
      data-testid="candidate-dashboard-screen"
      style={{
        padding: '16px',
        paddingBottom: '80px',
        backgroundColor: '#F7FAFC',
        minHeight: '100%',
      }}
    >
      {/* 1. Candidate Welcome Header Banner */}
      <div
        data-testid="dashboard-welcome-banner"
        style={{
          backgroundColor: '#1A365D',
          color: '#FFFFFF',
          borderRadius: '12px',
          padding: '20px',
          marginBottom: '16px',
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.08)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <span
              style={{
                fontSize: '11px',
                textTransform: 'uppercase',
                letterSpacing: '1px',
                color: '#CBD5E0',
                fontWeight: '600',
              }}
            >
              National Assessment Grid
            </span>
            <h2 style={{ margin: '4px 0 0 0', fontSize: '20px', fontWeight: '800' }}>
              Welcome back, {dashboardState.candidateName}!
            </h2>
            <p style={{ margin: '6px 0 0 0', fontSize: '13px', color: '#E2E8F0' }}>
              Review your scheduled national exams, verify hall tickets, and launch live assessments.
            </p>
          </div>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '50%',
              backgroundColor: '#ECC94B',
              color: '#1A202C',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '20px',
              fontWeight: '700',
            }}
          >
            📋
          </div>
        </div>
      </div>

      {/* 2. Live Exam Critical Alert Banner */}
      {liveExams.length > 0 && (
        <div
          data-testid="live-exam-banner"
          style={{
            backgroundColor: '#C53030',
            color: '#FFFFFF',
            borderRadius: '10px',
            padding: '14px 16px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 4px 10px rgba(197, 48, 48, 0.3)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '22px' }}>🚨</span>
            <div>
              <div style={{ fontWeight: '800', fontSize: '14px', textTransform: 'uppercase' }}>
                Live Assessment in Progress
              </div>
              <div style={{ fontSize: '12px', color: '#FED7D7' }}>
                {liveExams[0].title} is active now.
              </div>
            </div>
          </div>
          <button
            data-testid={`btn-start-live-${liveExams[0].id}`}
            onClick={() => handleLaunchLive(liveExams[0])}
            style={{
              backgroundColor: '#FFFFFF',
              color: '#9B2C2C',
              border: 'none',
              borderRadius: '6px',
              padding: '8px 14px',
              fontWeight: '700',
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            Enter Exam →
          </button>
        </div>
      )}

      {/* 3. KPI Metric Cards */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '10px',
          marginBottom: '20px',
        }}
      >
        <div
          data-testid="kpi-total-registered"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '12px 14px',
            borderRadius: '10px',
            border: '1px solid #E2E8F0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          }}
        >
          <div style={{ fontSize: '11px', color: '#718096', fontWeight: '600' }}>
            Total Registered
          </div>
          <div style={{ fontSize: '22px', fontWeight: '800', color: '#2B6CB0' }}>
            {dashboardState.kpis.totalRegistered}
          </div>
        </div>

        <div
          data-testid="kpi-live-count"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '12px 14px',
            borderRadius: '10px',
            border: '1px solid #E2E8F0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          }}
        >
          <div style={{ fontSize: '11px', color: '#718096', fontWeight: '600' }}>
            Live Assessments
          </div>
          <div style={{ fontSize: '22px', fontWeight: '800', color: '#C53030' }}>
            {dashboardState.kpis.liveCount}
          </div>
        </div>

        <div
          data-testid="kpi-upcoming-count"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '12px 14px',
            borderRadius: '10px',
            border: '1px solid #E2E8F0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          }}
        >
          <div style={{ fontSize: '11px', color: '#718096', fontWeight: '600' }}>
            Upcoming Exams
          </div>
          <div style={{ fontSize: '22px', fontWeight: '800', color: '#DD6B20' }}>
            {dashboardState.kpis.upcomingCount}
          </div>
        </div>

        <div
          data-testid="kpi-scorecards-count"
          style={{
            backgroundColor: '#FFFFFF',
            padding: '12px 14px',
            borderRadius: '10px',
            border: '1px solid #E2E8F0',
            boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
          }}
        >
          <div style={{ fontSize: '11px', color: '#718096', fontWeight: '600' }}>
            Published Scorecards
          </div>
          <div style={{ fontSize: '22px', fontWeight: '800', color: '#38A169' }}>
            {dashboardState.kpis.scorecardsCount}
          </div>
        </div>
      </div>

      {/* 4. Filter Tabs Bar */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          overflowX: 'auto',
          paddingBottom: '8px',
          marginBottom: '16px',
        }}
      >
        {filterTabs.map((tab) => {
          const active = dashboardState.activeFilter === tab.id;
          return (
            <button
              key={tab.id}
              data-testid={`filter-tab-${tab.id}`}
              onClick={() => controller.setFilter(tab.id)}
              style={{
                backgroundColor: active ? '#2B6CB0' : '#EDF2F7',
                color: active ? '#FFFFFF' : '#4A5568',
                border: 'none',
                borderRadius: '20px',
                padding: '6px 14px',
                fontSize: '12px',
                fontWeight: active ? '700' : '600',
                whiteSpace: 'nowrap',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <span>{tab.label}</span>
              {tab.count !== undefined && (
                <span
                  style={{
                    backgroundColor: active ? '#4299E1' : '#CBD5E0',
                    color: active ? '#FFFFFF' : '#2D3748',
                    borderRadius: '10px',
                    padding: '1px 6px',
                    fontSize: '10px',
                    fontWeight: '700',
                  }}
                >
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* 5. Enrolled Examination Cards List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {filteredExams.length === 0 ? (
          <div
            data-testid="no-exams-placeholder"
            style={{
              padding: '36px 16px',
              textAlign: 'center',
              backgroundColor: '#FFFFFF',
              borderRadius: '12px',
              border: '1px dashed #CBD5E0',
            }}
          >
            <span style={{ fontSize: '32px' }}>📭</span>
            <div style={{ fontWeight: '700', color: '#4A5568', marginTop: '8px' }}>
              No enrolled examinations found
            </div>
            <div style={{ fontSize: '12px', color: '#718096', marginTop: '4px' }}>
              Try switching your filter or explore the Exam Catalog.
            </div>
          </div>
        ) : (
          filteredExams.map((exam) => {
            const isLive = exam.status === 'LIVE';
            const isCompleted = exam.status === 'COMPLETED';

            return (
              <div
                key={exam.id}
                data-testid={`enrolled-exam-card-${exam.id}`}
                style={{
                  backgroundColor: '#FFFFFF',
                  borderRadius: '12px',
                  border: isLive ? '2px solid #E53E3E' : '1px solid #E2E8F0',
                  padding: '16px',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.04)',
                }}
              >
                {/* Status & Code Row */}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '8px',
                  }}
                >
                  <span
                    style={{
                      fontFamily: 'monospace',
                      fontWeight: '700',
                      fontSize: '11px',
                      color: '#4A5568',
                      backgroundColor: '#EDF2F7',
                      padding: '2px 8px',
                      borderRadius: '4px',
                    }}
                  >
                    {exam.code}
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: '700',
                      padding: '3px 8px',
                      borderRadius: '12px',
                      backgroundColor: isLive
                        ? '#FED7D7'
                        : isCompleted
                        ? '#E2E8F0'
                        : '#FEFCBF',
                      color: isLive
                        ? '#9B2C2C'
                        : isCompleted
                        ? '#4A5568'
                        : '#744210',
                    }}
                  >
                    {exam.status}
                  </span>
                </div>

                {/* Exam Title */}
                <h3
                  style={{
                    margin: '0 0 6px 0',
                    fontSize: '15px',
                    fontWeight: '700',
                    color: '#2D3748',
                    lineHeight: '1.3',
                  }}
                >
                  {exam.title}
                </h3>

                <div
                  style={{
                    fontSize: '12px',
                    color: '#718096',
                    marginBottom: '10px',
                  }}
                >
                  {exam.conductingAuthority}
                </div>

                {/* Date & Shift Info */}
                <div
                  style={{
                    backgroundColor: '#F7FAFC',
                    borderRadius: '8px',
                    padding: '10px',
                    fontSize: '12px',
                    color: '#4A5568',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px',
                    marginBottom: '12px',
                  }}
                >
                  <div>
                    📅 <strong>Date:</strong> {exam.scheduledDate} ({exam.scheduledTime})
                  </div>
                  <div>
                    📍 <strong>Center:</strong> {exam.centerName}, {exam.centerCity}
                  </div>
                  <div>
                    🆔 <strong>Roll No:</strong>{' '}
                    <span style={{ fontFamily: 'monospace', fontWeight: '700' }}>
                      {exam.rollNumber}
                    </span>
                  </div>
                </div>

                {/* Action Buttons Row */}
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {isLive && (
                    <button
                      data-testid={`btn-start-live-${exam.id}`}
                      onClick={() => handleLaunchLive(exam)}
                      style={{
                        flex: 1,
                        backgroundColor: '#38A169',
                        color: '#FFFFFF',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '8px 12px',
                        fontWeight: '700',
                        fontSize: '12px',
                        cursor: 'pointer',
                      }}
                    >
                      🚀 Start Live Assessment
                    </button>
                  )}

                  {exam.admitCardReady && !isLive && (
                    <button
                      data-testid={`btn-admit-card-${exam.id}`}
                      onClick={() => handleOpenAdmit(exam)}
                      style={{
                        flex: 1,
                        backgroundColor: '#2B6CB0',
                        color: '#FFFFFF',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '8px 12px',
                        fontWeight: '700',
                        fontSize: '12px',
                        cursor: 'pointer',
                      }}
                    >
                      🎟️ View Admit Card
                    </button>
                  )}

                  {exam.practiceAvailable && !isLive && (
                    <button
                      data-testid={`btn-practice-${exam.id}`}
                      onClick={() => handlePracticeClick(exam)}
                      style={{
                        backgroundColor: '#ED8936',
                        color: '#FFFFFF',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '8px 12px',
                        fontWeight: '700',
                        fontSize: '12px',
                        cursor: 'pointer',
                      }}
                    >
                      📝 Practice Mock
                    </button>
                  )}

                  {isCompleted && (
                    <button
                      data-testid={`btn-scorecard-${exam.id}`}
                      onClick={() => onViewScorecards && onViewScorecards(exam.id)}
                      style={{
                        flex: 1,
                        backgroundColor: '#805AD5',
                        color: '#FFFFFF',
                        border: 'none',
                        borderRadius: '6px',
                        padding: '8px 12px',
                        fontWeight: '700',
                        fontSize: '12px',
                        cursor: 'pointer',
                      }}
                    >
                      📊 View Official Scorecard
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* 6. Admit Card Preview Modal / Drawer */}
      {dashboardState.selectedAdmitCard && (
        <div
          data-testid="dashboard-admit-card-dialog"
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
              borderRadius: '14px',
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
                alignItems: 'flex-start',
                marginBottom: '12px',
                borderBottom: '1px solid #E2E8F0',
                paddingBottom: '10px',
              }}
            >
              <div>
                <span
                  style={{
                    fontSize: '10px',
                    fontWeight: '700',
                    color: '#718096',
                    textTransform: 'uppercase',
                  }}
                >
                  Official Digital Hall Ticket
                </span>
                <h3 style={{ margin: '2px 0 0 0', fontSize: '16px', color: '#1A365D' }}>
                  {dashboardState.selectedAdmitCard.examTitle}
                </h3>
              </div>
              <button
                data-testid="btn-close-admit-card"
                onClick={() => controller.closeAdmitCard()}
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

            {/* Candidate & Venue Summary */}
            <div
              style={{
                backgroundColor: '#F7FAFC',
                borderRadius: '8px',
                padding: '12px',
                fontSize: '12px',
                lineHeight: '1.6',
                marginBottom: '14px',
              }}
            >
              <div>
                <strong>Candidate:</strong> {dashboardState.selectedAdmitCard.candidateName}
              </div>
              <div>
                <strong>Roll Number:</strong> {dashboardState.selectedAdmitCard.rollNumber}
              </div>
              <div>
                <strong>Date & Time:</strong> {dashboardState.selectedAdmitCard.scheduledDate} (
                {dashboardState.selectedAdmitCard.examTime})
              </div>
              <div>
                <strong>Reporting / Gate Close:</strong>{' '}
                {dashboardState.selectedAdmitCard.reportingTime} /{' '}
                {dashboardState.selectedAdmitCard.gateClosingTime}
              </div>
              <div>
                <strong>Center:</strong> {dashboardState.selectedAdmitCard.centerName} (
                {dashboardState.selectedAdmitCard.centerCode})
              </div>
              <div>
                <strong>Address:</strong> {dashboardState.selectedAdmitCard.centerAddress}
              </div>
            </div>

            {/* Instructions */}
            <div style={{ marginBottom: '16px' }}>
              <div
                style={{
                  fontSize: '12px',
                  fontWeight: '700',
                  color: '#2D3748',
                  marginBottom: '6px',
                }}
              >
                Mandatory Candidate Instructions:
              </div>
              <ul
                style={{
                  margin: 0,
                  paddingLeft: '18px',
                  fontSize: '11px',
                  color: '#4A5568',
                  lineHeight: '1.5',
                }}
              >
                {dashboardState.selectedAdmitCard.instructions.map((inst, idx) => (
                  <li key={idx}>{inst}</li>
                ))}
              </ul>
            </div>

            <button
              onClick={() => controller.closeAdmitCard()}
              style={{
                width: '100%',
                backgroundColor: '#2B6CB0',
                color: '#FFFFFF',
                border: 'none',
                borderRadius: '8px',
                padding: '10px',
                fontWeight: '700',
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              Done / Close
            </button>
          </div>
        </div>
      )}

      {/* 7. Practice Paper Selection Modal */}
      {dashboardState.selectedPracticeExam && (
        <div
          data-testid="practice-paper-picker-modal"
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
              borderRadius: '14px',
              maxWidth: '440px',
              width: '100%',
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
              }}
            >
              <h3 style={{ margin: 0, fontSize: '16px', color: '#2D3748' }}>
                Select Practice Paper
              </h3>
              <button
                data-testid="btn-close-practice-picker"
                onClick={() => controller.closePracticeDialog()}
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

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {dashboardState.selectedPracticeExam.papers.map((paper) => (
                <div
                  key={paper.paperId}
                  data-testid={`practice-paper-card-${paper.paperId}`}
                  onClick={() => {
                    const result = controller.startPracticePaper(
                      dashboardState.selectedPracticeExam!.exam.id,
                      paper.paperId
                    );
                    if (result && onLaunchPractice) {
                      onLaunchPractice(result.examId, result.paperId);
                    }
                  }}
                  style={{
                    border: '1px solid #CBD5E0',
                    borderRadius: '8px',
                    padding: '12px',
                    cursor: 'pointer',
                    backgroundColor: '#F7FAFC',
                  }}
                >
                  <div style={{ fontWeight: '700', fontSize: '13px', color: '#2B6CB0' }}>
                    {paper.name}
                  </div>
                  <div style={{ fontSize: '11px', color: '#718096', marginTop: '4px' }}>
                    Questions: {paper.totalQuestions} • Difficulty: {paper.difficultyScore}/10
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
