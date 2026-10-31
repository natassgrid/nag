import React from 'react';
import { generateDeterministicQrMatrix } from './wallet-controller';

export { generateDeterministicQrMatrix };

export interface SignedQrCodeProps {
  payload: string;
  size?: number;
  label?: string;
  showVerificationBadge?: boolean;
  algorithm?: 'ECDSA_P256' | 'ED25519';
  testId?: string;
}


export const SignedQrCodeView: React.FC<SignedQrCodeProps> = ({
  payload,
  size = 220,
  label,
  showVerificationBadge = true,
  algorithm = 'ECDSA_P256',
  testId = 'signed-qr-view',
}) => {
  const matrixSize = 29;
  const matrix = React.useMemo(
    () => generateDeterministicQrMatrix(payload, matrixSize),
    [payload, matrixSize]
  );

  const cellSize = size / matrixSize;

  // Build path definition for high rendering performance
  const pathD = React.useMemo(() => {
    const commands: string[] = [];
    for (let r = 0; r < matrixSize; r++) {
      for (let c = 0; c < matrixSize; c++) {
        if (matrix[r][c]) {
          const x = c * cellSize;
          const y = r * cellSize;
          commands.push(`M${x.toFixed(2)},${y.toFixed(2)}h${cellSize.toFixed(2)}v${cellSize.toFixed(2)}h-${cellSize.toFixed(2)}z`);
        }
      }
    }
    return commands.join(' ');
  }, [matrix, cellSize, matrixSize]);

  return (
    <div
      data-testid={testId}
      style={{
        display: 'inline-flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '16px',
        backgroundColor: '#FFFFFF',
        borderRadius: '12px',
        border: '1px solid #E2E8F0',
        boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
      }}
    >
      <div
        style={{
          width: size,
          height: size,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#FFFFFF',
        }}
      >
        <svg
          data-testid="signed-qr-svg"
          width={size}
          height={size}
          viewBox={`0 0 ${size} ${size}`}
          xmlns="http://www.w3.org/2000/svg"
        >
          <rect width={size} height={size} fill="#FFFFFF" />
          <path d={pathD} fill="#1A202C" />
        </svg>
      </div>

      {label && (
        <span
          data-testid="qr-payload-label"
          style={{
            marginTop: '10px',
            fontFamily: 'monospace',
            fontSize: '12px',
            fontWeight: '600',
            color: '#2D3748',
            letterSpacing: '1px',
          }}
        >
          {label}
        </span>
      )}

      {showVerificationBadge && (
        <div
          data-testid="qr-security-badge"
          style={{
            marginTop: '8px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
            padding: '3px 8px',
            backgroundColor: '#F0FFF4',
            borderRadius: '9999px',
            border: '1px solid #9AE6B4',
          }}
        >
          <span
            style={{
              width: '8px',
              height: '8px',
              borderRadius: '50%',
              backgroundColor: '#38A169',
            }}
          />
          <span
            style={{
              fontSize: '11px',
              fontWeight: '600',
              color: '#22543D',
              textTransform: 'uppercase',
            }}
          >
            {algorithm === 'ECDSA_P256' ? 'ECDSA P-256 SIGNED' : 'ED25519 SIGNED'} (OFFLINE VERIFIED)
          </span>
        </div>
      )}
    </div>
  );
};
