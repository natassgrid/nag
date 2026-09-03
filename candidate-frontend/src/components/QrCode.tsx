// src/components/QrCode.tsx
// Generates a robust, deterministic SVG QR pattern based on payload hash

import React from 'react';

interface QrCodeProps {
  value: string;
  size?: number;
  className?: string;
}

export const QrCode: React.FC<QrCodeProps> = ({ value, size = 128, className = '' }) => {
  // Generate a deterministic 21x21 matrix based on the string hash
  const matrixSize = 25;
  const matrix: boolean[][] = Array.from({ length: matrixSize }, () =>
    Array(matrixSize).fill(false)
  );

  // Simple string hash algorithm to seed pseudo-random visual matrix
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash << 5) - hash + value.charCodeAt(i);
    hash |= 0;
  }

  const seededRandom = (seed: number) => {
    const x = Math.sin(seed++) * 10000;
    return x - Math.floor(x);
  };

  let seed = Math.abs(hash);

  // Fill finder patterns in 3 corners (top-left, top-right, bottom-left)
  const setFinderPattern = (startRow: number, startCol: number) => {
    for (let r = 0; r < 7; r++) {
      for (let c = 0; c < 7; c++) {
        if (
          r === 0 ||
          r === 6 ||
          c === 0 ||
          c === 6 ||
          (r >= 2 && r <= 4 && c >= 2 && c <= 4)
        ) {
          matrix[startRow + r][startCol + c] = true;
        } else {
          matrix[startRow + r][startCol + c] = false;
        }
      }
    }
  };

  setFinderPattern(0, 0);
  setFinderPattern(0, matrixSize - 7);
  setFinderPattern(matrixSize - 7, 0);

  // Fill alignment timing patterns
  for (let i = 7; i < matrixSize - 7; i++) {
    matrix[6][i] = i % 2 === 0;
    matrix[i][6] = i % 2 === 0;
  }

  // Populate data cells
  for (let r = 0; r < matrixSize; r++) {
    for (let c = 0; c < matrixSize; c++) {
      const inTopLeft = r < 8 && c < 8;
      const inTopRight = r < 8 && c >= matrixSize - 8;
      const inBottomLeft = r >= matrixSize - 8 && c < 8;
      const inTiming = r === 6 || c === 6;

      if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
        seed++;
        matrix[r][c] = seededRandom(seed) > 0.45;
      }
    }
  }

  const cellSize = size / matrixSize;

  return (
    <svg
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
      className={`bg-white p-1 rounded shadow-sm border border-slate-200 ${className}`}
      xmlns="http://www.w3.org/2000/svg"
      aria-label={`QR Code for: ${value}`}
    >
      <rect width="100%" height="100%" fill="#ffffff" />
      {matrix.map((row, rIdx) =>
        row.map((isDark, cIdx) =>
          isDark ? (
            <rect
              key={`${rIdx}-${cIdx}`}
              x={cIdx * cellSize}
              y={rIdx * cellSize}
              width={cellSize}
              height={cellSize}
              fill="#0f172a"
            />
          ) : null
        )
      )}
    </svg>
  );
};
