export interface NavItem {
  id: string;
  label: string;
  route?: string;
  icon: string;
  description?: string;
  exact?: boolean;
  badge?: string;
  isPrimaryAction?: boolean;
  children?: NavItem[];
}

export const CANDIDATE_NAV_ITEMS: NavItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    route: '/dashboard',
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
        icon: 'trending_up',
        description: 'Phased STAT milestones, micro-lessons & Gatekeeper',
      },
      {
        id: 'learn',
        label: 'AI Study Plan',
        route: '/learn/recommendations',
        icon: 'auto_awesome',
        description: 'Target exam diagnostic & remedial recommendations',
      },
      {
        id: 'practice',
        label: 'Practice Hub',
        route: '/practice',
        icon: 'quiz',
        description: 'Adaptive drill quizzes & mock assessments',
      },
      {
        id: 'practice-history',
        label: 'Practice History',
        route: '/practice/history',
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
        icon: 'search',
        description: 'Upcoming exams, shifts & application registration',
      },
      {
        id: 'results',
        label: 'Scorecards & Results',
        route: '/results',
        icon: 'verified',
        description: 'Official exam scorecards, percentiles & merit lists',
      },
    ],
  },
];
