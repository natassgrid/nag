import {
  compareDifficulty,
  compareString,
  getAriaSort,
  nextSortDirection,
  SortState,
} from './sort.util';

describe('sort.util', () => {
  describe('compareDifficulty', () => {
    it('should order EASY < MEDIUM < HARD in ascending order', () => {
      expect(compareDifficulty('EASY', 'MEDIUM', 'asc')).toBeLessThan(0);
      expect(compareDifficulty('MEDIUM', 'HARD', 'asc')).toBeLessThan(0);
      expect(compareDifficulty('HARD', 'EASY', 'asc')).toBeGreaterThan(0);
      expect(compareDifficulty('MEDIUM', 'MEDIUM', 'asc')).toBe(0);
    });

    it('should reverse order in descending order', () => {
      expect(compareDifficulty('EASY', 'HARD', 'desc')).toBeGreaterThan(0);
      expect(compareDifficulty('HARD', 'EASY', 'desc')).toBeLessThan(0);
    });
  });

  describe('compareString', () => {
    it('should sort strings alphabetically', () => {
      expect(compareString('Algebra', 'Geometry', 'asc')).toBeLessThan(0);
      expect(compareString('Geometry', 'Algebra', 'desc')).toBeLessThan(0);
      expect(compareString('Math', 'math', 'asc')).toBe(0);
    });
  });

  describe('getAriaSort', () => {
    it('should return correct aria-sort values', () => {
      const stateAsc: SortState = { active: 'title', direction: 'asc' };
      const stateDesc: SortState = { active: 'title', direction: 'desc' };

      expect(getAriaSort('title', stateAsc)).toBe('ascending');
      expect(getAriaSort('title', stateDesc)).toBe('descending');
      expect(getAriaSort('other', stateAsc)).toBe('none');
      expect(getAriaSort('title', null)).toBe('none');
    });
  });

  describe('nextSortDirection', () => {
    it('should cycle through null -> asc -> desc -> null', () => {
      expect(nextSortDirection(null)).toBe('asc');
      expect(nextSortDirection('asc')).toBe('desc');
      expect(nextSortDirection('desc')).toBe(null);
    });
  });
});
