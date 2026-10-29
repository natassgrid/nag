import {
  DEFAULT_SYSTEM_ROLES,
  normalizeRoleKey,
  formatRoleDisplayName,
  getSystemRoleBadgeTone,
  getSystemRoleDetail,
  mergeRolesWithSystemDefaults,
  extractRolesFromUsers,
  resolveAvailableRoles,
} from './system-roles.defaults';
import { AdminUserAccount, RoleDefinition } from './user-management.model';

describe('SystemRolesDefaults & Utilities', () => {
  it('should have standard default system roles defined', () => {
    expect(DEFAULT_SYSTEM_ROLES.length).toBeGreaterThanOrEqual(10);
    const codes = DEFAULT_SYSTEM_ROLES.map((r) => r.name);
    expect(codes).toContain('SUPER_ADMIN');
    expect(codes).toContain('SECURITY_ADMIN');
    expect(codes).toContain('EXAM_CONTROLLER');
    expect(codes).toContain('QUESTION_AUTHOR');
    expect(codes).toContain('REVIEWER');
    expect(codes).toContain('CANDIDATE');
  });

  describe('normalizeRoleKey', () => {
    it('should strip ROLE_ prefix and trim whitespace', () => {
      expect(normalizeRoleKey('ROLE_SUPER_ADMIN')).toBe('SUPER_ADMIN');
      expect(normalizeRoleKey('  ROLE_EXAM_CONTROLLER  ')).toBe('EXAM_CONTROLLER');
      expect(normalizeRoleKey('QUESTION_AUTHOR')).toBe('QUESTION_AUTHOR');
      expect(normalizeRoleKey('')).toBe('');
    });
  });

  describe('formatRoleDisplayName', () => {
    it('should format known system roles to proper human-readable titles', () => {
      expect(formatRoleDisplayName('SUPER_ADMIN')).toBe('Super Administrator');
      expect(formatRoleDisplayName('ROLE_SUPER_ADMIN')).toBe('Super Administrator');
      expect(formatRoleDisplayName('EXAM_CONTROLLER')).toBe('Examination Controller');
      expect(formatRoleDisplayName('QUESTION_AUTHOR')).toBe('Question Author');
      expect(formatRoleDisplayName('REVIEWER')).toBe('Question Reviewer');
      expect(formatRoleDisplayName('CANDIDATE')).toBe('Candidate');
    });

    it('should convert custom SCREAMING_SNAKE_CASE to Title Case', () => {
      expect(formatRoleDisplayName('DISTRICT_SUPERVISOR')).toBe('District Supervisor');
      expect(formatRoleDisplayName('ROLE_INVIGILATOR_LEAD')).toBe('Invigilator Lead');
    });

    it('should return empty string for falsy input', () => {
      expect(formatRoleDisplayName('')).toBe('');
    });
  });

  describe('getSystemRoleBadgeTone', () => {
    it('should return mapped tones for known roles and slate for unknown', () => {
      expect(getSystemRoleBadgeTone('SUPER_ADMIN')).toBe('purple');
      expect(getSystemRoleBadgeTone('ROLE_EXAM_CONTROLLER')).toBe('indigo');
      expect(getSystemRoleBadgeTone('QUESTION_AUTHOR')).toBe('blue');
      expect(getSystemRoleBadgeTone('SECURITY_ADMIN')).toBe('amber');
      expect(getSystemRoleBadgeTone('REVIEWER')).toBe('emerald');
      expect(getSystemRoleBadgeTone('UNKNOWN_ROLE')).toBe('slate');
    });
  });

  describe('getSystemRoleDetail', () => {
    it('should return full role detail for system roles', () => {
      const detail = getSystemRoleDetail('SUPER_ADMIN');
      expect(detail.code).toBe('SUPER_ADMIN');
      expect(detail.name).toBe('Super Administrator');
      expect(detail.badgeTone).toBe('purple');
      expect(detail.systemRole).toBe(true);
      expect(detail.description).toBeTruthy();
    });

    it('should return fallback detail for custom roles', () => {
      const detail = getSystemRoleDetail('CUSTOM_EXAM_AUDITOR');
      expect(detail.code).toBe('CUSTOM_EXAM_AUDITOR');
      expect(detail.name).toBe('Custom Exam Auditor');
      expect(detail.badgeTone).toBe('slate');
      expect(detail.systemRole).toBe(false);
    });
  });

  describe('mergeRolesWithSystemDefaults', () => {
    it('should return default system roles when fetched list is empty', () => {
      const merged = mergeRolesWithSystemDefaults([]);
      expect(merged.length).toBe(DEFAULT_SYSTEM_ROLES.length);
    });

    it('should overlay backend details onto system defaults and retain custom roles', () => {
      const fetched: RoleDefinition[] = [
        {
          id: 'custom-id-1',
          name: 'SUPER_ADMIN',
          displayName: 'Custom Super Admin Title',
          description: 'Updated backend description',
          systemRole: true,
          permissions: ['ALL'],
        },
        {
          id: 'custom-id-2',
          name: 'REGIONAL_COORDINATOR',
          displayName: 'Regional Coordinator',
          description: 'Custom regional authority',
          systemRole: false,
          permissions: ['EXAM:READ'],
        },
      ];

      const merged = mergeRolesWithSystemDefaults(fetched);
      const superAdmin = merged.find((r) => r.name === 'SUPER_ADMIN');
      expect(superAdmin?.displayName).toBe('Custom Super Admin Title');
      expect(superAdmin?.id).toBe('custom-id-1');

      const regional = merged.find((r) => r.name === 'REGIONAL_COORDINATOR');
      expect(regional).toBeDefined();
      expect(regional?.displayName).toBe('Regional Coordinator');
    });
  });

  describe('extractRolesFromUsers', () => {
    it('should extract distinct roles not in existing roles list', () => {
      const users: AdminUserAccount[] = [
        {
          id: 'u-1',
          username: 'user1',
          email: 'u1@test.com',
          fullName: 'User One',
          roles: ['SUPER_ADMIN', 'CHIEF_EXAMINER'],
          status: 'ACTIVE',
          twoFactorEnabled: true,
          createdAt: '',
        },
      ];

      const existing: RoleDefinition[] = [
        {
          id: 'r-1',
          name: 'SUPER_ADMIN',
          displayName: 'Super Administrator',
          description: '',
          systemRole: true,
          permissions: [],
        },
      ];

      const extracted = extractRolesFromUsers(users, existing);
      expect(extracted.length).toBe(1);
      expect(extracted[0].name).toBe('CHIEF_EXAMINER');
      expect(extracted[0].displayName).toBe('Chief Examiner');
    });
  });

  describe('resolveAvailableRoles', () => {
    it('should return non-empty array with system roles and user roles', () => {
      const users: AdminUserAccount[] = [
        {
          id: 'u-1',
          username: 'officer',
          email: 'officer@test.com',
          fullName: 'Officer Two',
          roles: ['VENUE_MARSHAL'],
          status: 'ACTIVE',
          twoFactorEnabled: false,
          createdAt: '',
        },
      ];

      const resolved = resolveAvailableRoles([], users);
      expect(resolved.length).toBeGreaterThan(DEFAULT_SYSTEM_ROLES.length);
      expect(resolved.some((r) => r.name === 'VENUE_MARSHAL')).toBe(true);
      expect(resolved.some((r) => r.name === 'SUPER_ADMIN')).toBe(true);
    });
  });
});
