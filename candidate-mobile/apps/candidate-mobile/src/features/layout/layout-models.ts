export type MobileRouteKey =
  | 'AUTH'
  | 'DASHBOARD'
  | 'CATALOG'
  | 'APPLICATION_WIZARD'
  | 'WALLET'
  | 'PRACTICE_HUB'
  | 'PRACTICE_RESULT'
  | 'PRACTICE_HISTORY'
  | 'LEARN_ONBOARDING'
  | 'LEARN_RECOMMENDATIONS'
  | 'LEARN_ROADMAP'
  | 'LEARN_MICRO_LESSON'
  | 'CBT_ENGINE'
  | 'PERFORMANCE_RESULTS'
  | 'REVIEW'
  | 'RESULTS'
  | 'PROFILE';

export interface MobileNavItem {
  id: string;
  label: string;
  route?: string;
  routeKey?: MobileRouteKey;
  icon: string;
  description?: string;
  exact?: boolean;
  badge?: string;
  isPrimaryAction?: boolean;
  children?: MobileNavItem[];
}

export const CANDIDATE_MOBILE_NAV_ITEMS: MobileNavItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    route: '/dashboard',
    routeKey: 'DASHBOARD',
    icon: 'dashboard',
    exact: true,
  },
  {
    id: 'learning',
    label: 'Learning & Practice',
    icon: 'psychology',
    badge: 'AI',
    children: [
      {
        id: 'roadmap',
        label: 'Learning Roadmap',
        route: '/learn/roadmap',
        routeKey: 'LEARN_ROADMAP',
        icon: 'trending_up',
        description: 'Phased STAT milestones, micro-lessons & Gatekeeper',
      },
      {
        id: 'learn',
        label: 'AI Study Plan',
        route: '/learn/recommendations',
        routeKey: 'LEARN_RECOMMENDATIONS',
        icon: 'auto_awesome',
        description: 'Target exam diagnostic & remedial recommendations',
      },
      {
        id: 'practice',
        label: 'Practice Hub',
        route: '/practice',
        routeKey: 'PRACTICE_HUB',
        icon: 'quiz',
        description: 'Adaptive drill quizzes & mock assessments',
      },
      {
        id: 'practice-history',
        label: 'Practice History',
        route: '/practice/history',
        routeKey: 'PRACTICE_HISTORY',
        icon: 'history',
        description: 'Review past attempts & question scorecards',
      },
    ],
  },
  {
    id: 'assessments',
    label: 'Exams & Results',
    icon: 'school',
    children: [
      {
        id: 'browse',
        label: 'Browse Exams',
        route: '/browse',
        routeKey: 'CATALOG',
        icon: 'search',
        description: 'Upcoming exams, shifts & application registration',
      },
      {
        id: 'wallet',
        label: 'Hall Ticket Wallet',
        route: '/wallet',
        routeKey: 'WALLET',
        icon: 'qr_code_2',
        description: 'Offline digital admit cards & signed QR verification',
      },
      {
        id: 'review',
        label: 'Answer Key Review',
        route: '/review',
        routeKey: 'REVIEW',
        icon: 'fact_check',
        description: 'Recorded response sheet & provisional key challenges',
      },
      {
        id: 'results',
        label: 'Scorecards & Results',
        route: '/results',
        routeKey: 'RESULTS',
        icon: 'verified',
        description: 'Official exam scorecards, percentiles & merit lists',
      },
    ],
  },
  {
    id: 'profile',
    label: 'Candidate Profile',
    route: '/profile',
    routeKey: 'PROFILE',
    icon: 'person',
    description: 'Personal details, education, DigiLocker & PwD settings',
  },
];
