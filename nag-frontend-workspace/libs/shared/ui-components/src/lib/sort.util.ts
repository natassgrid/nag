export type SortDirection = 'asc' | 'desc' | null;

export interface SortState<T = string> {
  active: T;
  direction: SortDirection;
}

const DIFFICULTY_ORDER: Record<string, number> = {
  EASY: 1,
  MEDIUM: 2,
  HARD: 3,
};

/**
 * Compares two difficulty levels semantically: EASY (1) < MEDIUM (2) < HARD (3).
 */
export function compareDifficulty(
  a: string | null | undefined,
  b: string | null | undefined,
  direction: SortDirection = 'asc'
): number {
  const rankA = a ? (DIFFICULTY_ORDER[a.toUpperCase()] ?? 99) : 99;
  const rankB = b ? (DIFFICULTY_ORDER[b.toUpperCase()] ?? 99) : 99;
  const diff = rankA - rankB;
  return direction === 'desc' ? -diff : diff;
}

/**
 * Generic comparator for alphabetical string sorting.
 */
export function compareString(
  a: string | null | undefined,
  b: string | null | undefined,
  direction: SortDirection = 'asc'
): number {
  const strA = (a || '').toLowerCase();
  const strB = (b || '').toLowerCase();
  const diff = strA.localeCompare(strB);
  return direction === 'desc' ? -diff : diff;
}

/**
 * Returns aria-sort attribute value ('ascending' | 'descending' | 'none') for accessibility.
 */
export function getAriaSort(
  column: string,
  sortState?: SortState | null
): 'ascending' | 'descending' | 'none' {
  if (!sortState || sortState.active !== column || !sortState.direction) {
    return 'none';
  }
  return sortState.direction === 'asc' ? 'ascending' : 'descending';
}

/**
 * Cycles sort direction: null -> 'asc' -> 'desc' -> null
 */
export function nextSortDirection(current: SortDirection): SortDirection {
  if (!current) return 'asc';
  if (current === 'asc') return 'desc';
  return null;
}
