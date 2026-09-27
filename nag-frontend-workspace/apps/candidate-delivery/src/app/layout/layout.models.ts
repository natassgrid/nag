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
