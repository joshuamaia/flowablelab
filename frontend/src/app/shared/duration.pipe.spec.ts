import { formatDuration } from './duration.pipe';

describe('formatDuration', () => {
  it.each([
    [null, '—'],
    [850, '850 ms'],
    [12_000, '12 s'],
    [184_000, '3 min 4 s'],
    [7_500_000, '2 h 5 min'],
    [273_600_000, '3 d 4 h'],
  ])('%s → %s', (ms, expected) => {
    expect(formatDuration(ms as number | null)).toBe(expected);
  });
});
