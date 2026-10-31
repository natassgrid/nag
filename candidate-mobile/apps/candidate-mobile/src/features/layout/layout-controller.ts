import {
  CANDIDATE_MOBILE_NAV_ITEMS,
  MobileNavItem,
  MobileRouteKey,
} from './layout-models';

export class CandidateLayoutController {
  private isDrawerOpen = false;
  private expandedGroups: Set<string> = new Set(['learning', 'assessments']);
  private activeRouteKey: MobileRouteKey = 'DASHBOARD';
  private userName = 'Aarav Sharma';
  private activeExamEnrollmentTitle: string | null = 'UPSC CSE Prelims 2026';

  constructor(initialRoute: MobileRouteKey = 'DASHBOARD', userName = 'Aarav Sharma') {
    this.activeRouteKey = initialRoute;
    this.userName = userName;
  }

  getNavItems(): MobileNavItem[] {
    return CANDIDATE_MOBILE_NAV_ITEMS;
  }

  getIsDrawerOpen(): boolean {
    return this.isDrawerOpen;
  }

  openDrawer(): void {
    this.isDrawerOpen = true;
  }

  closeDrawer(): void {
    this.isDrawerOpen = false;
  }

  toggleDrawer(): void {
    this.isDrawerOpen = !this.isDrawerOpen;
  }

  toggleGroup(groupId: string): void {
    if (this.expandedGroups.has(groupId)) {
      this.expandedGroups.delete(groupId);
    } else {
      this.expandedGroups.add(groupId);
    }
  }

  isGroupExpanded(groupId: string): boolean {
    return this.expandedGroups.has(groupId);
  }

  getActiveRouteKey(): MobileRouteKey {
    return this.activeRouteKey;
  }

  navigate(routeKey: MobileRouteKey): void {
    this.activeRouteKey = routeKey;
    this.isDrawerOpen = false;
  }

  isGroupActive(item: MobileNavItem): boolean {
    if (!item.children || item.children.length === 0) {
      return item.routeKey === this.activeRouteKey;
    }
    return item.children.some((child) => child.routeKey === this.activeRouteKey);
  }

  getUserName(): string {
    return this.userName;
  }

  setUserName(name: string): void {
    this.userName = name || 'Guest';
  }

  getUserInitials(): string {
    const name = (this.userName || 'G').trim();
    if (!name) return 'G';
    const parts = name.split(/[\s_.-]+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }

  getActiveEnrollmentTitle(): string | null {
    return this.activeExamEnrollmentTitle;
  }

  setActiveEnrollmentTitle(title: string | null): void {
    this.activeExamEnrollmentTitle = title;
  }
}
