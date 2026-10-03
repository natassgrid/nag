export interface NavItem {
  id: string;
  label: string;
  route: string;
  icon: string;
  exact?: boolean;
  badge?: string;
  isPrimaryAction?: boolean;
}

export const CANDIDATE_NAV_ITEMS: NavItem[] = [
  {
    id: 'dashboard',
    label: 'My Dashboard',
    route: '/dashboard',
    icon: 'dashboard',
    exact: true,
  },
  {
    id: 'practice',
    label: 'Practice Hub',
    route: '/practice',
    icon: 'quiz',
  },
  {
    id: 'learn',
    label: 'AI Study Plan',
    route: '/learn/recommendations',
    icon: 'auto_awesome',
  },
  {
    id: 'browse',
    label: 'Browse Exams',
    route: '/browse',
    icon: 'search',
  },
  {
    id: 'profile',
    label: 'Profile & KYC',
    route: '/profile',
    icon: 'person',
  },
  {
    id: 'results',
    label: 'Scorecards & Results',
    route: '/results',
    icon: 'verified',
  },
];
