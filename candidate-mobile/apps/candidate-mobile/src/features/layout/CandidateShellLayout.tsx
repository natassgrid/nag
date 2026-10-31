import React from 'react';
import {
  CANDIDATE_MOBILE_NAV_ITEMS,
  MobileNavItem,
  MobileRouteKey,
} from './layout-models';

export interface CandidateShellLayoutProps {
  activeRoute: MobileRouteKey;
  userName: string;
  isAuthenticated: boolean;
  isDrawerOpen: boolean;
  expandedGroups: string[];
  activeEnrollmentTitle?: string | null;
  onToggleDrawer: () => void;
  onCloseDrawer: () => void;
  onToggleGroup: (groupId: string) => void;
  onNavigate: (routeKey: MobileRouteKey) => void;
  onLogout: () => void;
  children?: React.ReactNode;
}

export function getUserInitialsFromName(userName: string): string {
  const clean = (userName || 'G').trim();
  if (!clean) return 'G';
  const parts = clean.split(/[\s_.-]+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return clean.slice(0, 2).toUpperCase();
}

export const CandidateShellLayout: React.FC<CandidateShellLayoutProps> = ({
  activeRoute,
  userName,
  isAuthenticated,
  isDrawerOpen,
  expandedGroups,
  activeEnrollmentTitle,
  onToggleDrawer,
  onCloseDrawer,
  onToggleGroup,
  onNavigate,
  onLogout,
  children,
}) => {
  const initials = getUserInitialsFromName(userName);

  const renderDrawerItem = (item: MobileNavItem) => {
    const hasChildren = Boolean(item.children && item.children.length > 0);
    const isExpanded = expandedGroups.includes(item.id);
    const isActive =
      item.routeKey === activeRoute ||
      Boolean(item.children?.some((c) => c.routeKey === activeRoute));

    if (hasChildren && item.children) {
      return (
        <div key={item.id} data-testid={`nav-group-${item.id}`}>
          <button
            type="button"
            onClick={() => onToggleGroup(item.id)}
            data-active={isActive}
          >
            <span>{item.label}</span>
            {item.badge ? <span>[{item.badge}]</span> : null}
            <span>{isExpanded ? '▾' : '▸'}</span>
          </button>
          {isExpanded ? (
            <div>
              {item.children.map((child) => (
                <button
                  key={child.id}
                  type="button"
                  data-testid={`nav-item-${child.id}`}
                  data-active={child.routeKey === activeRoute}
                  onClick={() => {
                    if (child.routeKey) {
                      onNavigate(child.routeKey);
                    }
                    onCloseDrawer();
                  }}
                >
                  <strong>{child.label}</strong>
                  {child.description ? <small> — {child.description}</small> : null}
                </button>
              ))}
            </div>
          ) : null}
        </div>
      );
    }

    return (
      <button
        key={item.id}
        type="button"
        data-testid={`nav-item-${item.id}`}
        data-active={isActive}
        onClick={() => {
          if (item.routeKey) {
            onNavigate(item.routeKey);
          }
          onCloseDrawer();
        }}
      >
        <span>{item.label}</span>
      </button>
    );
  };

  return (
    <div data-testid="candidate-mobile-shell">
      <header data-testid="candidate-mobile-header">
        <button
          type="button"
          data-testid="toggle-drawer-btn"
          onClick={onToggleDrawer}
        >
          ☰ Menu
        </button>
        <div>
          <strong>NAG Candidate Portal</strong>
          {activeEnrollmentTitle ? (
            <span data-testid="active-enrollment-pill">
              Target: {activeEnrollmentTitle}
            </span>
          ) : null}
        </div>
        {isAuthenticated ? (
          <div data-testid="header-user-badge">
            <button
              type="button"
              onClick={() => onNavigate('PROFILE')}
              title={userName}
            >
              <span>{initials}</span>
              <span>{userName}</span>
            </button>
          </div>
        ) : null}
      </header>

      {isDrawerOpen ? (
        <aside data-testid="candidate-mobile-drawer">
          <div>
            <strong>{userName}</strong>
            <span> ({initials})</span>
            <button type="button" onClick={onCloseDrawer}>
              ✕
            </button>
          </div>
          <nav>{CANDIDATE_MOBILE_NAV_ITEMS.map(renderDrawerItem)}</nav>
          {isAuthenticated ? (
            <button
              type="button"
              data-testid="drawer-logout-btn"
              onClick={() => {
                onCloseDrawer();
                onLogout();
              }}
            >
              Sign Out
            </button>
          ) : null}
        </aside>
      ) : null}

      <main data-testid="candidate-mobile-content">{children}</main>
    </div>
  );
};
