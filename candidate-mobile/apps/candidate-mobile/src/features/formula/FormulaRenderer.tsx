import React from 'react';
import {
  parseFormulaText,
  formatReadableLatex,
  FormulaToken,
  SmilesSvgDescriptor,
} from './formula-parser';

export interface FormulaRendererProps {
  content?: string | null;
  inline?: boolean;
  className?: string;
  testId?: string;
}

/**
 * Safely renders LaTeX expression to HTML using offline KaTeX if available.
 */
function renderKatexHtml(latex: string, displayMode: boolean): string | null {
  if (
    typeof window !== 'undefined' &&
    (window as any).katex &&
    typeof (window as any).katex.renderToString === 'function'
  ) {
    try {
      return (window as any).katex.renderToString(latex, {
        displayMode,
        throwOnError: false,
      });
    } catch {
      return null;
    }
  }
  return null;
}

/**
 * Formats chemical formula strings (subscripts and reaction arrows)
 * when rendered in offline/mobile environments.
 */
function formatChemistryContent(raw: string): string {
  if (!raw) return '';
  return raw
    .replace(/->/g, ' → ')
    .replace(/<->/g, ' ⇄ ')
    .replace(/\^\{([^}]+)\}/g, '⁽$1⁾')
    .replace(/_\{([^}]+)\}/g, '₍$1₎')
    .replace(/_([0-9]+)/g, (_, n) =>
      n
        .split('')
        .map((digit: string) => '₀₁₂₃₄₅₆₇₈₉'[parseInt(digit, 10)] || digit)
        .join('')
    )
    .replace(/([A-Za-z)\]])([0-9]+)/g, (_, el, num) => {
      const sub = num
        .split('')
        .map((digit: string) => '₀₁₂₃₄₅₆₇₈₉'[parseInt(digit, 10)] || digit)
        .join('');
      return `${el}${sub}`;
    });
}

/**
 * Mobile-optimised Formula & Molecular Structure Renderer.
 * Supports LaTeX ($$...$$), mhchem chemistry (\ce{}, \pu{}),
 * and 2D SMILES vector representations with zero external network overhead.
 */
export const FormulaRenderer: React.FC<FormulaRendererProps> = ({
  content,
  inline = false,
  className = '',
  testId = 'formula-renderer',
}) => {
  if (!content || !content.trim()) {
    return null;
  }

  const tokens = parseFormulaText(content);

  const containerStyle: React.CSSProperties = inline
    ? { display: 'inline', verticalAlign: 'baseline' }
    : { display: 'block', margin: '4px 0', lineHeight: 1.6 };

  return (
    <div
      data-testid={testId}
      className={`nag-formula-renderer ${className}`.trim()}
      style={containerStyle}
    >
      {tokens.map((token: FormulaToken, index: number) => {
        const key = `token-${index}-${token.type}`;

        switch (token.type) {
          case 'text':
            return (
              <span key={key} className="formula-text-segment" style={{ whiteSpace: 'pre-wrap' }}>
                {token.content}
              </span>
            );

          case 'latex': {
            const displayMode = Boolean(token.displayMode && !inline);
            const katexHtml = renderKatexHtml(token.content, displayMode);
            const readableFallback = formatReadableLatex(token.content);

            if (displayMode) {
              return (
                <div
                  key={key}
                  className="formula-latex-block"
                  data-testid="latex-block"
                  style={{
                    display: 'block',
                    textAlign: 'center',
                    padding: '8px 12px',
                    margin: '6px 0',
                    backgroundColor: '#F7FAFC',
                    borderRadius: '6px',
                    fontFamily: 'KaTeX_Main, Times New Roman, serif',
                    fontSize: '1.05em',
                    color: '#1A202C',
                    border: '1px solid #E2E8F0',
                    overflowX: 'auto',
                  }}
                >
                  {katexHtml ? (
                    <span
                      className="latex-expression"
                      dangerouslySetInnerHTML={{ __html: katexHtml }}
                    />
                  ) : (
                    <span className="latex-expression" style={{ fontStyle: 'italic' }}>
                      {readableFallback}
                    </span>
                  )}
                </div>
              );
            }

            return (
              <span
                key={key}
                className="formula-latex-inline"
                data-testid="latex-inline"
                style={{
                  fontFamily: 'KaTeX_Main, Times New Roman, serif',
                  padding: '0 2px',
                  color: '#2B6CB0',
                  fontWeight: 600,
                  display: 'inline',
                  verticalAlign: 'baseline',
                }}
              >
                {katexHtml ? (
                  <span dangerouslySetInnerHTML={{ __html: katexHtml }} />
                ) : (
                  <span>{readableFallback}</span>
                )}
              </span>
            );
          }

          case 'chemistry':
            return (
              <span
                key={key}
                className={`formula-chem-${token.notation}`}
                data-testid={`chem-${token.notation}`}
                style={{
                  fontFamily: 'system-ui, -apple-system, sans-serif',
                  fontWeight: 600,
                  color: token.notation === 'ce' ? '#2C5282' : '#744210',
                  backgroundColor: token.notation === 'ce' ? '#EBF8FF' : '#FEFCBF',
                  padding: '1px 4px',
                  borderRadius: '3px',
                }}
              >
                {formatChemistryContent(token.content)}
              </span>
            );

          case 'smiles':
            return (
              <div
                key={key}
                className="formula-smiles-container"
                data-testid="smiles-structure"
                style={{
                  display: 'inline-block',
                  margin: '6px 4px',
                  padding: '6px',
                  backgroundColor: '#FFFFFF',
                  borderRadius: '8px',
                  border: '1px solid #E2E8F0',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                  textAlign: 'center',
                }}
              >
                <div
                  className="smiles-svg-wrapper"
                  dangerouslySetInnerHTML={{ __html: token.descriptor.svgString }}
                />
                <div
                  className="smiles-label"
                  style={{
                    fontSize: '10px',
                    color: '#718096',
                    fontFamily: 'monospace',
                    marginTop: '2px',
                  }}
                >
                  {token.smiles}
                </div>
              </div>
            );

          default:
            return null;
        }
      })}
    </div>
  );
};
