import React, { useState, useMemo, useCallback } from 'react';
import {
  AuthController,
  AuthState,
  MobileOtpLoginScreen,
  CandidateRegistrationScreen,
  TotpVerificationScreen,
} from './features/auth';
import {
  WalletController,
  HallTicketWalletScreen,
} from './features/wallet';
import {
  CatalogController,
  ExamCatalogScreen,
} from './features/catalog';
import {
  ApplicationWizardController,
  ExamApplicationWizardScreen,
} from './features/application';
import {
  CbtSessionController,
  MockCbtEngineScreen,
  MockPerformanceScreen,
  LocalGradingBreakdown,
} from './features/cbt';
import {
  DashboardController,
  CandidateDashboardScreen,
  EnrolledExam,
} from './features/dashboard';
import {
  ProfileController,
  CandidateProfileScreen,
} from './features/profile';
import {
  ReviewController,
  CandidateReviewScreen,
} from './features/review';
import {
  ResultsController,
  CandidateResultsScreen,
} from './features/results';
import {
  LearnController,
  LearnHubScreen,
} from './features/learn';
import {
  PracticeController,
  PracticeContainerScreen,
} from './features/practice';
import {
  CandidateLayoutController,
  CANDIDATE_MOBILE_NAV_ITEMS,
  MobileRouteKey,
} from './features/layout';
import { DockerApiService } from './services/docker-api.service';
import { DockerTargetMode } from '@nag/candidate-core';

export type AppNavScreen =
  | 'AUTH_LOGIN'
  | 'AUTH_REGISTER'
  | 'AUTH_TOTP'
  | 'DASHBOARD'
  | 'CATALOG'
  | 'APPLICATION_WIZARD'
  | 'WALLET'
  | 'PRACTICE'
  | 'LEARN'
  | 'REVIEW'
  | 'RESULTS'
  | 'PROFILE'
  | 'CBT_ENGINE'
  | 'PERFORMANCE_RESULTS';

export const App: React.FC = () => {
  const authController = useMemo(() => new AuthController(), []);
  const walletController = useMemo(() => new WalletController(), []);
  const catalogController = useMemo(() => new CatalogController(), []);
  const dashboardController = useMemo(() => new DashboardController(), []);
  const profileController = useMemo(() => new ProfileController(), []);
  const reviewController = useMemo(() => new ReviewController(), []);
  const resultsController = useMemo(() => new ResultsController(), []);
  const learnController = useMemo(() => new LearnController(), []);
  const practiceController = useMemo(() => new PracticeController(), []);
  const layoutController = useMemo(() => new CandidateLayoutController('AUTH'), []);
  const defaultDockerMode: DockerTargetMode =
    typeof navigator !== 'undefined' && /Android/i.test(navigator.userAgent || '')
      ? 'android-emulator'
      : 'localhost-gateway';
  const dockerApiService = useMemo(() => new DockerApiService(defaultDockerMode), [defaultDockerMode]);

  const [currentScreen, setCurrentScreen] = useState<AppNavScreen>('AUTH_LOGIN');
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);
  const [dockerMode, setDockerMode] = useState<DockerTargetMode>(
    dockerApiService.getActiveMode()
  );
  const [dockerBaseUrl, setDockerBaseUrl] = useState<string>(
    dockerApiService.getActiveBaseUrl()
  );
  const [authState, setAuthState] = useState<AuthState>(authController.getState());
  const [selectedExamId, setSelectedExamId] = useState<string>('EXAM-UPSC-2026');
  const [wizardController, setWizardController] = useState<ApplicationWizardController>(
    () => new ApplicationWizardController('EXAM-UPSC-2026')
  );
  const [cbtController, setCbtController] = useState<CbtSessionController>(
    () => new CbtSessionController()
  );
  const [gradingResult, setGradingResult] = useState<LocalGradingBreakdown | null>(null);

  const triggerDockerSync = useCallback(() => {
    (catalogController as any).syncFromDocker?.();
    (dashboardController as any).syncFromDocker?.();
    (practiceController as any).syncFromDocker?.();
    (learnController as any).syncFromDocker?.();
    (resultsController as any).syncFromDocker?.();
    (profileController as any).syncFromDocker?.();
  }, [
    catalogController,
    dashboardController,
    practiceController,
    learnController,
    resultsController,
    profileController,
  ]);

  const handleAuthenticated = useCallback(() => {
    setCurrentScreen('DASHBOARD');
    triggerDockerSync();
  }, [triggerDockerSync]);

  const handleNavigateToRegister = useCallback(() => {
    setCurrentScreen('AUTH_REGISTER');
  }, []);

  const handleNavigateToLogin = useCallback(() => {
    setCurrentScreen('AUTH_LOGIN');
  }, []);

  React.useEffect(() => {
    return authController.subscribe((s: AuthState) => {
      setAuthState(s);
    });
  }, [authController]);

  const handleRouteKeyNavigation = (routeKey: MobileRouteKey) => {
    layoutController.navigate(routeKey);
    setIsDrawerOpen(false);
    switch (routeKey) {
      case 'DASHBOARD':
        setCurrentScreen('DASHBOARD');
        break;
      case 'CATALOG':
        setCurrentScreen('CATALOG');
        break;
      case 'APPLICATION_WIZARD':
        setCurrentScreen('APPLICATION_WIZARD');
        break;
      case 'WALLET':
        setCurrentScreen('WALLET');
        break;
      case 'PRACTICE_HUB':
        practiceController.setActiveTab('hub');
        setCurrentScreen('PRACTICE');
        break;
      case 'PRACTICE_RESULT':
        practiceController.setActiveTab('result');
        setCurrentScreen('PRACTICE');
        break;
      case 'PRACTICE_HISTORY':
        practiceController.setActiveTab('history');
        setCurrentScreen('PRACTICE');
        break;
      case 'LEARN_ONBOARDING':
        learnController.setActiveHubTab('recommendations');
        setCurrentScreen('LEARN');
        break;
      case 'LEARN_RECOMMENDATIONS':
        learnController.setActiveHubTab('recommendations');
        setCurrentScreen('LEARN');
        break;
      case 'LEARN_ROADMAP':
        learnController.setActiveHubTab('roadmap');
        setCurrentScreen('LEARN');
        break;
      case 'LEARN_MICRO_LESSON':
        learnController.setActiveHubTab('lesson');
        setCurrentScreen('LEARN');
        break;
      case 'REVIEW':
        setCurrentScreen('REVIEW');
        break;
      case 'RESULTS':
        setCurrentScreen('RESULTS');
        break;
      case 'PROFILE':
        setCurrentScreen('PROFILE');
        break;
      case 'CBT_ENGINE':
        setCurrentScreen('CBT_ENGINE');
        break;
      case 'PERFORMANCE_RESULTS':
        setCurrentScreen('PERFORMANCE_RESULTS');
        break;
      case 'AUTH':
      default:
        setCurrentScreen('AUTH_LOGIN');
        break;
    }
  };

  // Handle Application Start
  const handleStartApplication = (examId: string) => {
    const exam = catalogController.getState().exams.find((e) => e.id === examId);
    setSelectedExamId(examId);
    setWizardController(
      new ApplicationWizardController(
        examId,
        exam ? exam.title : 'National Examination',
        authState.candidateProfile ? authState.candidateProfile.id : 'CND-884210'
      )
    );
    setCurrentScreen('APPLICATION_WIZARD');
  };

  // Handle Application Completed
  const handleApplicationCompleted = (hallTicketNumber: string) => {
    const state = wizardController.getState();
    if (state.issuedHallTicket) {
      walletController.addAdmitCard(state.issuedHallTicket);
    }
    walletController.selectAdmitCard(hallTicketNumber);
    setCurrentScreen('WALLET');
  };

  // Handle CBT Mock Launch
  const handleLaunchPractice = (examId: string) => {
    setSelectedExamId(examId);
    const newSession = new CbtSessionController(examId);
    setCbtController(newSession);
    setCurrentScreen('CBT_ENGINE');
  };

  // Handle CBT Completed
  const handleCbtCompleted = () => {
    const breakdown = cbtController.getState().gradingResult;
    setGradingResult(breakdown);
    setCurrentScreen('PERFORMANCE_RESULTS');
  };

  return (
    <div
      data-testid="nag-candidate-mobile-app"
      style={{
        minHeight: '100vh',
        backgroundColor: '#EDF2F7',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Global Mobile App Top Bar */}
      <header
        style={{
          padding: '12px 16px',
          backgroundColor: '#1A365D',
          color: '#FFFFFF',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          boxShadow: '0 2px 4px rgba(0,0,0,0.12)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            type="button"
            data-testid="header-drawer-toggle-btn"
            onClick={() => setIsDrawerOpen((prev) => !prev)}
            style={{
              background: 'rgba(255,255,255,0.15)',
              border: 'none',
              color: '#FFFFFF',
              borderRadius: '6px',
              padding: '4px 8px',
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            ☰
          </button>
          <div
            style={{
              width: '28px',
              height: '28px',
              borderRadius: '6px',
              backgroundColor: '#ECC94B',
              color: '#1A365D',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: '900',
              fontSize: '14px',
            }}
          >
            N
          </div>
          <div>
            <h1 style={{ fontSize: '15px', fontWeight: '800', margin: 0, letterSpacing: '0.5px' }}>
              NAG Mobile
            </h1>
            <span
              data-testid="header-docker-api-url"
              style={{ fontSize: '10px', color: '#CBD5E0', display: 'block' }}
            >
              Docker API: {dockerBaseUrl}
            </span>
          </div>
        </div>

        {/* Right Info: Candidate Profile / Auth State */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {authState.candidateProfile ? (
            <>
              <button
                type="button"
                data-testid="header-profile-btn"
                onClick={() => setCurrentScreen('PROFILE')}
                style={{
                  background: 'none',
                  border: 'none',
                  textAlign: 'right',
                  cursor: 'pointer',
                }}
              >
                <span style={{ fontSize: '12px', fontWeight: '700', color: '#FFFFFF', display: 'block' }}>
                  {authState.candidateProfile.fullName}
                </span>
                <span style={{ fontSize: '10px', color: '#68D391' }}>
                  {authState.digiLocker.linked ? '✓ Aadhaar Verified' : 'Standard Profile'}
                </span>
              </button>
              <button
                type="button"
                data-testid="header-logout-btn"
                onClick={() => {
                  authController.logout();
                  setCurrentScreen('AUTH_LOGIN');
                }}
                style={{
                  padding: '4px 8px',
                  backgroundColor: 'rgba(229, 62, 62, 0.2)',
                  color: '#FED7D7',
                  border: '1px solid rgba(229, 62, 62, 0.4)',
                  borderRadius: '4px',
                  fontSize: '11px',
                  fontWeight: '600',
                  cursor: 'pointer',
                }}
              >
                Logout
              </button>
            </>
          ) : (
            <button
              data-testid="header-sign-in-btn"
              onClick={() => setCurrentScreen('AUTH_LOGIN')}
              style={{
                padding: '6px 12px',
                backgroundColor: 'rgba(255,255,255,0.15)',
                color: '#FFFFFF',
                border: '1px solid rgba(255,255,255,0.3)',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
              }}
            >
              Sign In
            </button>
          )}
        </div>
      </header>

      {/* Slide-out Mobile Navigation Drawer (Parity with candidate-drawer) */}
      {isDrawerOpen && (
        <aside
          data-testid="mobile-nav-drawer"
          style={{
            backgroundColor: '#FFFFFF',
            borderBottom: '2px solid #CBD5E0',
            padding: '12px 16px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          }}
        >
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {CANDIDATE_MOBILE_NAV_ITEMS.map((item) => {
              if (item.children && item.children.length > 0) {
                return item.children.map((child) => (
                  <button
                    key={child.id}
                    type="button"
                    data-testid={`drawer-nav-${child.id}`}
                    onClick={() => child.routeKey && handleRouteKeyNavigation(child.routeKey)}
                    style={{
                      padding: '6px 10px',
                      borderRadius: '6px',
                      border: '1px solid #CBD5E0',
                      backgroundColor: '#F7FAFC',
                      fontSize: '12px',
                      fontWeight: 600,
                      color: '#2D3748',
                      cursor: 'pointer',
                    }}
                  >
                    {child.label}
                  </button>
                ));
              }
              return (
                <button
                  key={item.id}
                  type="button"
                  data-testid={`drawer-nav-${item.id}`}
                  onClick={() => item.routeKey && handleRouteKeyNavigation(item.routeKey)}
                  style={{
                    padding: '6px 10px',
                    borderRadius: '6px',
                    border: '1px solid #CBD5E0',
                    backgroundColor: '#EBF8FF',
                    fontSize: '12px',
                    fontWeight: 700,
                    color: '#2B6CB0',
                    cursor: 'pointer',
                  }}
                >
                  {item.label}
                </button>
              );
            })}
          </div>
          <div
            data-testid="docker-api-mode-selector"
            style={{
              marginTop: '10px',
              paddingTop: '10px',
              borderTop: '1px solid #E2E8F0',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              flexWrap: 'wrap',
            }}
          >
            <span style={{ fontSize: '11px', fontWeight: 700, color: '#4A5568' }}>
              Docker Target ({dockerMode}):
            </span>
            {(
              [
                'localhost-gateway',
                'android-emulator',
                'localhost-nginx',
                'docker-internal',
              ] as DockerTargetMode[]
            ).map((mode) => (
              <button
                key={mode}
                type="button"
                data-testid={`docker-mode-btn-${mode}`}
                onClick={() => {
                  const cfg = dockerApiService.setTargetMode(mode);
                  setDockerMode(cfg.mode);
                  setDockerBaseUrl(cfg.baseUrl);
                  triggerDockerSync();
                }}
                style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  border: '1px solid #CBD5E0',
                  backgroundColor: dockerMode === mode ? '#2B6CB0' : '#EDF2F7',
                  color: dockerMode === mode ? '#FFFFFF' : '#2D3748',
                  fontSize: '10px',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                {mode}
              </button>
            ))}
          </div>
        </aside>
      )}

      {/* Screen Router Viewport */}
      <main
        style={{
          flex: 1,
          paddingBottom:
            currentScreen !== 'CBT_ENGINE' &&
            currentScreen !== 'AUTH_LOGIN' &&
            currentScreen !== 'AUTH_REGISTER' &&
            currentScreen !== 'AUTH_TOTP'
              ? '64px'
              : '0',
        }}
      >
        {currentScreen === 'AUTH_LOGIN' && (
          <MobileOtpLoginScreen
            controller={authController}
            onAuthenticated={handleAuthenticated}
            onNavigateToRegister={handleNavigateToRegister}
          />
        )}

        {currentScreen === 'AUTH_REGISTER' && (
          <CandidateRegistrationScreen
            controller={authController}
            onNavigateToLogin={handleNavigateToLogin}
          />
        )}

        {currentScreen === 'AUTH_TOTP' && (
          <TotpVerificationScreen
            controller={authController}
            onAuthenticated={handleAuthenticated}
          />
        )}

        {currentScreen === 'DASHBOARD' && (
          <CandidateDashboardScreen
            controller={dashboardController}
            onStartLiveExam={(exam: EnrolledExam) => handleLaunchPractice(exam.id)}
            onOpenAdmitCard={() => setCurrentScreen('WALLET')}
            onLaunchPractice={(examId: string) => handleLaunchPractice(examId)}
            onViewScorecards={() => setCurrentScreen('RESULTS')}
          />
        )}

        {currentScreen === 'CATALOG' && (
          <ExamCatalogScreen
            controller={catalogController}
            onApplyExam={handleStartApplication}
            onPracticeExam={handleLaunchPractice}
            onNavigateToWallet={() => setCurrentScreen('WALLET')}
          />
        )}

        {currentScreen === 'APPLICATION_WIZARD' && (
          <ExamApplicationWizardScreen
            controller={wizardController}
            onApplicationCompleted={handleApplicationCompleted}
            onCancel={() => setCurrentScreen('CATALOG')}
          />
        )}

        {currentScreen === 'WALLET' && (
          <HallTicketWalletScreen
            controller={walletController}
            onNavigateToCatalog={() => setCurrentScreen('CATALOG')}
            onLaunchPractice={handleLaunchPractice}
          />
        )}

        {currentScreen === 'PRACTICE' && (
          <PracticeContainerScreen
            controller={practiceController}
            onNavigateToLearn={() => setCurrentScreen('LEARN')}
            onStudyWeakSubtopic={(subtopicId: number) => {
              learnController.loadMicroLesson(subtopicId);
              learnController.setActiveHubTab('lesson');
              setCurrentScreen('LEARN');
            }}
          />
        )}

        {currentScreen === 'LEARN' && (
          <LearnHubScreen
            controller={learnController}
            onNavigateToPractice={() => setCurrentScreen('PRACTICE')}
          />
        )}

        {currentScreen === 'REVIEW' && (
          <CandidateReviewScreen
            controller={reviewController}
            onExitReview={() => setCurrentScreen('DASHBOARD')}
          />
        )}

        {currentScreen === 'RESULTS' && (
          <CandidateResultsScreen
            controller={resultsController}
            onNavigateToWallet={() => setCurrentScreen('WALLET')}
            onExitResults={() => setCurrentScreen('DASHBOARD')}
          />
        )}

        {currentScreen === 'PROFILE' && (
          <CandidateProfileScreen
            controller={profileController}
            onProfileSaved={() => setCurrentScreen('DASHBOARD')}
          />
        )}

        {currentScreen === 'CBT_ENGINE' && (
          <MockCbtEngineScreen
            controller={cbtController}
            onSubmitCompleted={handleCbtCompleted}
            onExitSession={() => setCurrentScreen('DASHBOARD')}
          />
        )}

        {currentScreen === 'PERFORMANCE_RESULTS' && gradingResult && (
          <MockPerformanceScreen
            grading={gradingResult}
            onRetakeTest={() => handleLaunchPractice(selectedExamId)}
            onReturnToCatalog={() => setCurrentScreen('CATALOG')}
            onViewWallet={() => setCurrentScreen('WALLET')}
          />
        )}
      </main>

      {/* Bottom Navigation Bar (Hidden during fullscreen CBT and auth screens) */}
      {currentScreen !== 'CBT_ENGINE' &&
        currentScreen !== 'AUTH_LOGIN' &&
        currentScreen !== 'AUTH_REGISTER' &&
        currentScreen !== 'AUTH_TOTP' && (
        <nav
          data-testid="bottom-nav-bar"
          style={{
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            height: '56px',
            backgroundColor: '#FFFFFF',
            borderTop: '1px solid #CBD5E0',
            display: 'flex',
            justifyContent: 'space-around',
            alignItems: 'center',
            zIndex: 100,
            boxShadow: '0 -2px 6px rgba(0,0,0,0.06)',
          }}
        >
          <button
            data-testid="nav-tab-dashboard"
            onClick={() => setCurrentScreen('DASHBOARD')}
            style={{
              background: 'none',
              border: 'none',
              color: currentScreen === 'DASHBOARD' ? '#2B6CB0' : '#718096',
              fontWeight: currentScreen === 'DASHBOARD' ? '700' : '500',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: '16px' }}>🏠</span>
            Dashboard
          </button>

          <button
            data-testid="nav-tab-catalog"
            onClick={() => setCurrentScreen('CATALOG')}
            style={{
              background: 'none',
              border: 'none',
              color: currentScreen === 'CATALOG' ? '#2B6CB0' : '#718096',
              fontWeight: currentScreen === 'CATALOG' ? '700' : '500',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: '16px' }}>📚</span>
            Catalog
          </button>

          <button
            data-testid="nav-tab-learn"
            onClick={() => setCurrentScreen('LEARN')}
            style={{
              background: 'none',
              border: 'none',
              color: currentScreen === 'LEARN' ? '#2B6CB0' : '#718096',
              fontWeight: currentScreen === 'LEARN' ? '700' : '500',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: '16px' }}>🧠</span>
            AI Learn
          </button>

          <button
            data-testid="nav-tab-practice"
            onClick={() => setCurrentScreen('PRACTICE')}
            style={{
              background: 'none',
              border: 'none',
              color: currentScreen === 'PRACTICE' ? '#2B6CB0' : '#718096',
              fontWeight: currentScreen === 'PRACTICE' ? '700' : '500',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: '16px' }}>🎯</span>
            Practice
          </button>

          <button
            data-testid="nav-tab-wallet"
            onClick={() => setCurrentScreen('WALLET')}
            style={{
              background: 'none',
              border: 'none',
              color: currentScreen === 'WALLET' ? '#2B6CB0' : '#718096',
              fontWeight: currentScreen === 'WALLET' ? '700' : '500',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: '16px' }}>🎟️</span>
            Wallet
          </button>

          <button
            data-testid="nav-tab-results"
            onClick={() => setCurrentScreen('RESULTS')}
            style={{
              background: 'none',
              border: 'none',
              color: currentScreen === 'RESULTS' ? '#2B6CB0' : '#718096',
              fontWeight: currentScreen === 'RESULTS' ? '700' : '500',
              fontSize: '11px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: '16px' }}>🏆</span>
            Results
          </button>
        </nav>
      )}
    </div>
  );
};

export default App;
