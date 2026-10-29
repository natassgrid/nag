import { AdminUserAccount, RoleDefinition } from './user-management.model';

export type RoleBadgeTone = 'purple' | 'indigo' | 'blue' | 'amber' | 'emerald' | 'slate';

export interface SystemRoleDetail {
  code: string;
  name: string;
  badgeTone: RoleBadgeTone;
  description: string;
  systemRole: boolean;
}

/**
 * Standard System Role Definitions for NextGen Assessment Grid (NAG).
 * Aligned with backend UserRole.java and database seed migrations.
 */
export const DEFAULT_SYSTEM_ROLES: RoleDefinition[] = [
  {
    id: 'role-super-admin',
    name: 'SUPER_ADMIN',
    displayName: 'Super Administrator',
    description: 'Full system sovereignty across all tenancies, identity management, cryptographic keys, and system configuration.',
    systemRole: true,
    permissions: ['*'],
  },
  {
    id: 'role-security-admin',
    name: 'SECURITY_ADMIN',
    displayName: 'Security Administrator',
    description: 'Manages security policies, audit trails, MFA enforcement, and access controls.',
    systemRole: true,
    permissions: [
      'IDENTITY:USER_READ',
      'IDENTITY:MFA_MANAGE',
      'AUDIT:READ',
      'AUDIT:EXPORT',
      'SECURITY:POLICY_MANAGE',
    ],
  },
  {
    id: 'role-exam-controller',
    name: 'EXAM_CONTROLLER',
    displayName: 'Examination Controller',
    description: 'Authority to schedule exam sessions, configure marking schemes, approve shift timelines, and oversee grading.',
    systemRole: true,
    permissions: [
      'EXAM:READ',
      'EXAM:CREATE',
      'EXAM:SCHEDULE',
      'EXAM:PUBLISH',
      'DELIVERY:MONITOR',
      'DELIVERY:RETEST',
    ],
  },
  {
    id: 'role-question-author',
    name: 'QUESTION_AUTHOR',
    displayName: 'Question Author',
    description: 'Creation, authoring, LaTeX formula formatting, and taxonomy tag maintenance for multi-lingual question items.',
    systemRole: true,
    permissions: [
      'QUESTION:READ',
      'QUESTION:CREATE',
      'QUESTION:EDIT',
      'QUESTION:IMPORT_EXPORT',
    ],
  },
  {
    id: 'role-reviewer',
    name: 'REVIEWER',
    displayName: 'Question Reviewer',
    description: 'Peer review, difficulty calibration, syllabus validation, and multi-lingual translation verification.',
    systemRole: true,
    permissions: ['QUESTION:READ', 'QUESTION:REVIEW'],
  },
  {
    id: 'role-sme',
    name: 'SUBJECT_MATTER_EXPERT',
    displayName: 'Subject Matter Expert',
    description: 'Subject matter expert for authoring, reviewing, rubric checks, and question validation.',
    systemRole: true,
    permissions: [
      'QUESTION:READ',
      'QUESTION:CREATE',
      'QUESTION:EDIT',
      'QUESTION:REVIEW',
      'QUESTION:APPROVE',
      'QUESTION:TRANSLATE',
    ],
  },
  {
    id: 'role-approver',
    name: 'APPROVER',
    displayName: 'Content Approver',
    description: 'Approves questions and examination blueprints for final publication.',
    systemRole: true,
    permissions: ['QUESTION:READ', 'QUESTION:APPROVE', 'EXAM:READ'],
  },
  {
    id: 'role-translator',
    name: 'TRANSLATOR',
    displayName: 'Language Translator',
    description: 'Translates questions, passages, and rubrics into regional and official languages.',
    systemRole: true,
    permissions: ['QUESTION:READ', 'QUESTION:TRANSLATE'],
  },
  {
    id: 'role-evaluator',
    name: 'EVALUATOR',
    displayName: 'Answer Evaluator',
    description: 'Evaluates subjective candidate responses and marks rubrics.',
    systemRole: true,
    permissions: [
      'EVALUATION:READ',
      'EVALUATION:SCORE_AUTO',
      'EVALUATION:SCORE_MANUAL',
      'EVALUATION:RESULT_PUBLISH',
    ],
  },
  {
    id: 'role-auditor',
    name: 'AUDITOR',
    displayName: 'Compliance Auditor',
    description: 'Inspects immutable audit logs, tamper-evident chains, and compliance reports.',
    systemRole: true,
    permissions: ['AUDIT:READ', 'AUDIT:EXPORT', 'ANALYTICS:VIEW', 'REPORTS:EXPORT'],
  },
  {
    id: 'role-candidate',
    name: 'CANDIDATE',
    displayName: 'Candidate',
    description: 'Attends examination sessions, submits responses, and views verified results.',
    systemRole: true,
    permissions: ['DELIVERY:ATTEND'],
  },
];

/**
 * Mapping of role keys (with or without 'ROLE_' prefix) to friendly display titles.
 */
export const SYSTEM_ROLE_DISPLAY_MAP: Record<string, string> = {
  SUPER_ADMIN: 'Super Administrator',
  SECURITY_ADMIN: 'Security Administrator',
  EXAM_CONTROLLER: 'Examination Controller',
  QUESTION_AUTHOR: 'Question Author',
  REVIEWER: 'Question Reviewer',
  QUESTION_REVIEWER: 'Question Reviewer',
  SUBJECT_MATTER_EXPERT: 'Subject Matter Expert',
  APPROVER: 'Content Approver',
  TRANSLATOR: 'Language Translator',
  EVALUATOR: 'Answer Evaluator',
  AUDITOR: 'Compliance Auditor',
  CANDIDATE: 'Candidate',
  ADMIN: 'Administrator',
  EXAM_ADMIN: 'Examination Administrator',
};

/**
 * Normalize role string to uppercase without 'ROLE_' prefix.
 */
export function normalizeRoleKey(roleKey: string): string {
  if (!roleKey) return '';
  return roleKey.trim().toUpperCase().replace(/^ROLE_/, '');
}

/**
 * Converts a role code or key (e.g. 'EXAM_CONTROLLER', 'ROLE_SUPER_ADMIN')
 * into a human-friendly display name.
 */
export function formatRoleDisplayName(roleKey: string): string {
  if (!roleKey) return '';
  const normalized = normalizeRoleKey(roleKey);

  if (SYSTEM_ROLE_DISPLAY_MAP[normalized]) {
    return SYSTEM_ROLE_DISPLAY_MAP[normalized];
  }
  if (SYSTEM_ROLE_DISPLAY_MAP[roleKey]) {
    return SYSTEM_ROLE_DISPLAY_MAP[roleKey];
  }

  // Convert SCREAMING_SNAKE_CASE to Title Case
  return normalized
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Get the UI badge tone for a given role key.
 */
export function getSystemRoleBadgeTone(roleKey: string): RoleBadgeTone {
  const norm = normalizeRoleKey(roleKey);
  switch (norm) {
    case 'SUPER_ADMIN':
      return 'purple';
    case 'EXAM_CONTROLLER':
      return 'indigo';
    case 'QUESTION_AUTHOR':
      return 'blue';
    case 'SECURITY_ADMIN':
      return 'amber';
    case 'REVIEWER':
    case 'QUESTION_REVIEWER':
      return 'emerald';
    default:
      return 'slate';
  }
}

/**
 * Get comprehensive role metadata including tone, description, and systemRole flag.
 */
export function getSystemRoleDetail(roleCode: string): SystemRoleDetail {
  const norm = normalizeRoleKey(roleCode);
  const found = DEFAULT_SYSTEM_ROLES.find(
    (r) => normalizeRoleKey(r.name) === norm
  );

  if (found) {
    return {
      code: found.name,
      name: found.displayName,
      badgeTone: getSystemRoleBadgeTone(found.name),
      description: found.description,
      systemRole: true,
    };
  }

  return {
    code: roleCode,
    name: formatRoleDisplayName(roleCode),
    badgeTone: 'slate',
    description: 'Custom administrative role assigned with specific operational privileges.',
    systemRole: false,
  };
}

/**
 * Merge an array of backend-fetched roles with default system roles,
 * ensuring no role definitions are lost.
 */
export function mergeRolesWithSystemDefaults(
  fetchedRoles: RoleDefinition[] = []
): RoleDefinition[] {
  if (!fetchedRoles || fetchedRoles.length === 0) {
    return [...DEFAULT_SYSTEM_ROLES];
  }

  const roleMap = new Map<string, RoleDefinition>();

  // Add default system roles first
  for (const def of DEFAULT_SYSTEM_ROLES) {
    roleMap.set(normalizeRoleKey(def.name), { ...def });
  }

  // Overlay fetched roles
  for (const fetched of fetchedRoles) {
    const key = normalizeRoleKey(fetched.name);
    const existing = roleMap.get(key);
    if (existing) {
      roleMap.set(key, {
        ...existing,
        ...fetched,
        displayName: fetched.displayName || existing.displayName,
        description: fetched.description || existing.description,
      });
    } else {
      roleMap.set(key, {
        ...fetched,
        displayName: fetched.displayName || formatRoleDisplayName(fetched.name),
      });
    }
  }

  return Array.from(roleMap.values());
}

/**
 * Scan user accounts to extract any distinct roles that may not be in the known roles list.
 */
export function extractRolesFromUsers(
  users: AdminUserAccount[] = [],
  existingRoles: RoleDefinition[] = []
): RoleDefinition[] {
  const knownKeys = new Set(
    existingRoles.map((r) => normalizeRoleKey(r.name))
  );
  const extraRoles: RoleDefinition[] = [];

  for (const user of users) {
    if (!Array.isArray(user.roles)) continue;
    for (const roleKey of user.roles) {
      const norm = normalizeRoleKey(roleKey);
      if (!norm || knownKeys.has(norm)) continue;

      knownKeys.add(norm);
      extraRoles.push({
        id: `role-user-${norm.toLowerCase()}`,
        name: roleKey,
        displayName: formatRoleDisplayName(roleKey),
        description: `Operational role assigned to ${user.fullName || user.email}`,
        systemRole: false,
        permissions: [],
      });
    }
  }

  return extraRoles;
}

/**
 * Resolves the complete list of available roles for filter dropdowns:
 * merges fetched roles, system default roles, and any user-assigned roles.
 */
export function resolveAvailableRoles(
  fetchedRoles: RoleDefinition[] = [],
  users: AdminUserAccount[] = []
): RoleDefinition[] {
  const baseRoles = mergeRolesWithSystemDefaults(fetchedRoles);
  const userRoles = extractRolesFromUsers(users, baseRoles);
  return [...baseRoles, ...userRoles];
}
