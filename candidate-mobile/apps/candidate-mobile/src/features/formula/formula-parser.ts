/**
 * NAG Formula & Chemical Structure Rendering Engine
 * Parses LaTeX/KaTeX ($$...$$), mhchem chemistry (\ce{}, \pu{}),
 * and SMILES molecular strings into structured tokens and offline SVG descriptors.
 */

export interface SvgAtom {
  id: number;
  symbol: string;
  x: number;
  y: number;
  color: string;
  charge?: number;
}

export interface SvgBond {
  from: number;
  to: number;
  order: 1 | 2 | 3;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

export interface SmilesSvgDescriptor {
  smiles: string;
  width: number;
  height: number;
  viewBox: string;
  atoms: SvgAtom[];
  bonds: SvgBond[];
  svgString: string;
}

export type FormulaToken =
  | { type: 'text'; content: string }
  | { type: 'latex'; content: string; displayMode: boolean }
  | { type: 'chemistry'; notation: 'ce' | 'pu'; content: string }
  | { type: 'smiles'; smiles: string; descriptor: SmilesSvgDescriptor };

/**
 * Repairs control character artifacts from JSON unescaping
 * (e.g. form-feed for \frac, tab for \times, \theta, \tau, etc.)
 */
export function repairEscapedArtifacts(text: string): string {
  if (!text) return '';
  return text
    .replace(/\x0crac/g, '\\frac')
    .replace(/\x0corall/g, '\\forall')
    .replace(/\x0clat/g, '\\flat')
    .replace(/\t+(?:times|imes)/g, '\\times')
    .replace(/\t+(?:theta|heta)/g, '\\theta')
    .replace(/\t+(?:tau|au)/g, '\\tau')
    .replace(/\t+ext(?=[^a-zA-Z]|$)/g, '\\text')
    .replace(/\t+an(?=[^a-zA-Z]|$)/g, '\\tan')
    .replace(/\t+o(?=[^a-zA-Z]|$)/g, '\\to')
    .replace(/\r+ho/g, '\\rho')
    .replace(/\r+ight/g, '\\right')
    .replace(/\x08eta/g, '\\beta')
    .replace(/\x08egin/g, '\\begin')
    .replace(/\x08ar(?=[^a-zA-Z]|$)/g, '\\bar')
    .replace(/\x08ullet/g, '\\bullet')
    .replace(/\x08oxed/g, '\\boxed');
}

/**
 * Normalizes backslashes in LaTeX formulas:
 * 1. Collapses over-escaped backslashes (\\\\frac -> \frac)
 * 2. Normalizes escaped braces in \ce\{...\} to \ce{...}
 * 3. Never splits \n when followed by a lowercase letter (\neq, \neg, \rightarrow, \theta, \tau)
 */
export function normalizeLatexFormula(formula: string): string {
  if (!formula) return '';
  let cleaned = repairEscapedArtifacts(formula.trim());

  // Normalize \ce\{...\} or \\ce\{...\} to standard \ce{...}
  cleaned = cleaned.replace(/\\\\?ce\s*\\\{([\s\S]*?)\\\}/g, (_, inner) => {
    return `\\ce{${inner.replace(/\\\{/g, '{').replace(/\\\}/g, '}')}}`;
  });

  // Preserve \begin environments without altering spacing
  if (cleaned.includes('\\begin')) {
    return cleaned;
  }

  // Collapse double-escaped backslashes: \\<letter>, \\%, etc.
  return cleaned.replace(/\\\\(?=[a-zA-Z% ,;!_{}])/g, '\\');
}

/**
 * Normalizes text newlines according to NAG standard:
 * Converts \n to real newline ONLY when it is NOT followed by a lowercase letter
 * (preventing commands like \neq, \neg, \rightarrow, \text from being split).
 */
export function normalizeNewlinesPreservingLatex(text: string): string {
  if (!text) return '';
  // Match literal backslash followed by 'n' NOT followed by [a-z]
  return text.replace(/\\n(?![a-z])/g, '\n');
}

const ELEMENT_COLORS: Record<string, string> = {
  C: '#2D3748',
  c: '#4A5568',
  N: '#3182CE',
  n: '#4299E1',
  O: '#E53E3E',
  o: '#F56565',
  S: '#D69E2E',
  s: '#ECC94B',
  P: '#DD6B20',
  F: '#38A169',
  Cl: '#2F855A',
  CL: '#2F855A',
  Br: '#9B2C2C',
  BR: '#9B2C2C',
  I: '#6B46C1',
  H: '#718096',
};

/**
 * Parses a SMILES string and computes 2D coordinates and bond vectors
 * for offline SVG rendering without external network or heavy dependencies.
 */
export function renderSmilesToSvgDescriptor(smilesInput: string): SmilesSvgDescriptor {
  const smiles = (smilesInput || '').trim();
  if (!smiles) {
    return {
      smiles: '',
      width: 100,
      height: 100,
      viewBox: '0 0 100 100',
      atoms: [],
      bonds: [],
      svgString: '<svg width="100" height="100" viewBox="0 0 100 100"></svg>',
    };
  }

  // Tokenize SMILES
  interface RawAtom {
    id: number;
    symbol: string;
    isAromatic: boolean;
    ringNumbers: number[];
  }

  interface RawBond {
    from: number;
    to: number;
    order: 1 | 2 | 3;
  }

  const rawAtoms: RawAtom[] = [];
  const rawBonds: RawBond[] = [];
  const ringOpenings: Map<number, { atomId: number; bondOrder: 1 | 2 | 3 }> = new Map();
  const branchStack: number[] = [];

  let currentAtomIndex = -1;
  let nextBondOrder: 1 | 2 | 3 = 1;
  let i = 0;

  while (i < smiles.length) {
    const ch = smiles[i];

    if (ch === '(') {
      if (currentAtomIndex !== -1) {
        branchStack.push(currentAtomIndex);
      }
      i++;
      continue;
    }

    if (ch === ')') {
      if (branchStack.length > 0) {
        currentAtomIndex = branchStack.pop()!;
      }
      i++;
      continue;
    }

    if (ch === '=') {
      nextBondOrder = 2;
      i++;
      continue;
    }

    if (ch === '#') {
      nextBondOrder = 3;
      i++;
      continue;
    }

    if (ch === '-') {
      nextBondOrder = 1;
      i++;
      continue;
    }

    if (ch === ':') {
      nextBondOrder = 1;
      i++;
      continue;
    }

    // Check for 2-letter element (Cl, Br)
    let symbol = '';
    if (i + 1 < smiles.length) {
      const two = smiles.slice(i, i + 2);
      if (two === 'Cl' || two === 'Br') {
        symbol = two;
        i += 2;
      }
    }

    if (!symbol) {
      // 1-letter element or aromatic
      if (/[BCNOPSFIbcnops]/.test(ch)) {
        symbol = ch;
        i++;
      } else {
        // Ring closure digit
        if (/[0-9]/.test(ch)) {
          const ringNum = parseInt(ch, 10);
          if (currentAtomIndex !== -1) {
            if (ringOpenings.has(ringNum)) {
              const open = ringOpenings.get(ringNum)!;
              ringOpenings.delete(ringNum);
              rawBonds.push({
                from: open.atomId,
                to: currentAtomIndex,
                order: open.bondOrder > nextBondOrder ? open.bondOrder : nextBondOrder,
              });
              nextBondOrder = 1;
            } else {
              ringOpenings.set(ringNum, { atomId: currentAtomIndex, bondOrder: nextBondOrder });
              nextBondOrder = 1;
            }
          }
          i++;
          continue;
        }
        // Skip unhandled characters
        i++;
        continue;
      }
    }

    // Add Atom
    const atomId = rawAtoms.length;
    const isAromatic = symbol === symbol.toLowerCase() && /[a-z]/.test(symbol);
    rawAtoms.push({
      id: atomId,
      symbol: symbol.toUpperCase(),
      isAromatic,
      ringNumbers: [],
    });

    if (currentAtomIndex !== -1) {
      rawBonds.push({
        from: currentAtomIndex,
        to: atomId,
        order: nextBondOrder,
      });
      nextBondOrder = 1;
    }

    currentAtomIndex = atomId;
  }

  // Compute 2D coordinates
  const bondLength = 36;
  const atomCoords: { x: number; y: number }[] = [];

  // Check if structure is cyclic
  const ringSize = rawAtoms.length;
  const hasRing = rawBonds.length >= rawAtoms.length && rawAtoms.length >= 3;

  if (hasRing && ringSize <= 8) {
    // Layout in a regular polygon
    const radius = bondLength / (2 * Math.sin(Math.PI / ringSize));
    const centerX = 120;
    const centerY = 100;
    for (let idx = 0; idx < rawAtoms.length; idx++) {
      const angle = (2 * Math.PI * idx) / ringSize - Math.PI / 2;
      atomCoords.push({
        x: Math.round(centerX + radius * Math.cos(angle)),
        y: Math.round(centerY + radius * Math.sin(angle)),
      });
    }
  } else {
    // Acyclic / branched zigzag layout
    let curX = 40;
    let curY = 80;
    let angleDirection = 1; // 1 = down-right, -1 = up-right

    for (let idx = 0; idx < rawAtoms.length; idx++) {
      if (idx === 0) {
        atomCoords.push({ x: curX, y: curY });
      } else {
        const parentBond = rawBonds.find(b => b.to === idx);
        if (parentBond) {
          const parentCoord = atomCoords[parentBond.from];
          const isBranch = rawBonds.filter(b => b.from === parentBond.from).length > 1;

          if (isBranch && parentBond.from !== idx - 1) {
            // Branch perpendicular
            atomCoords.push({
              x: parentCoord.x,
              y: parentCoord.y - bondLength,
            });
          } else {
            angleDirection *= -1;
            const deltaY = angleDirection * (bondLength * 0.5);
            const deltaX = bondLength * 0.866;
            curX = parentCoord.x + deltaX;
            curY = parentCoord.y + deltaY;
            atomCoords.push({ x: Math.round(curX), y: Math.round(curY) });
          }
        } else {
          curX += bondLength;
          atomCoords.push({ x: Math.round(curX), y: Math.round(curY) });
        }
      }
    }
  }

  // Calculate bounding box and normalise with padding
  const padding = 24;
  let minX = Number.POSITIVE_INFINITY;
  let minY = Number.POSITIVE_INFINITY;
  let maxX = Number.NEGATIVE_INFINITY;
  let maxY = Number.NEGATIVE_INFINITY;

  for (const c of atomCoords) {
    if (c.x < minX) minX = c.x;
    if (c.y < minY) minY = c.y;
    if (c.x > maxX) maxX = c.x;
    if (c.y > maxY) maxY = c.y;
  }

  if (!Number.isFinite(minX)) {
    minX = 0;
    minY = 0;
    maxX = 100;
    maxY = 100;
  }

  const shiftX = padding - minX;
  const shiftY = padding - minY;
  const totalWidth = Math.max(80, Math.round(maxX - minX + padding * 2));
  const totalHeight = Math.max(60, Math.round(maxY - minY + padding * 2));

  const finalAtoms: SvgAtom[] = rawAtoms.map(a => {
    const rawPos = atomCoords[a.id] || { x: 50, y: 50 };
    return {
      id: a.id,
      symbol: a.symbol,
      x: rawPos.x + shiftX,
      y: rawPos.y + shiftY,
      color: ELEMENT_COLORS[a.symbol] || ELEMENT_COLORS[a.symbol.toUpperCase()] || '#2D3748',
    };
  });

  const finalBonds: SvgBond[] = rawBonds.map(b => {
    const fromAtom = finalAtoms[b.from];
    const toAtom = finalAtoms[b.to];
    return {
      from: b.from,
      to: b.to,
      order: b.order,
      x1: fromAtom ? fromAtom.x : 0,
      y1: fromAtom ? fromAtom.y : 0,
      x2: toAtom ? toAtom.x : 0,
      y2: toAtom ? toAtom.y : 0,
    };
  });

  // Construct SVG Elements string
  let bondSvgLines = '';
  for (const bond of finalBonds) {
    if (bond.order === 1) {
      bondSvgLines += `<line x1="${bond.x1}" y1="${bond.y1}" x2="${bond.x2}" y2="${bond.y2}" stroke="#4A5568" stroke-width="2.5" stroke-linecap="round" />`;
    } else if (bond.order === 2) {
      const dx = bond.x2 - bond.x1;
      const dy = bond.y2 - bond.y1;
      const len = Math.hypot(dx, dy) || 1;
      const ox = (-dy / len) * 2.5;
      const oy = (dx / len) * 2.5;
      bondSvgLines += `<line x1="${bond.x1 + ox}" y1="${bond.y1 + oy}" x2="${bond.x2 + ox}" y2="${bond.y2 + oy}" stroke="#4A5568" stroke-width="2" stroke-linecap="round" />`;
      bondSvgLines += `<line x1="${bond.x1 - ox}" y1="${bond.y1 - oy}" x2="${bond.x2 - ox}" y2="${bond.y2 - oy}" stroke="#4A5568" stroke-width="2" stroke-linecap="round" />`;
    } else if (bond.order === 3) {
      const dx = bond.x2 - bond.x1;
      const dy = bond.y2 - bond.y1;
      const len = Math.hypot(dx, dy) || 1;
      const ox = (-dy / len) * 3.5;
      const oy = (dx / len) * 3.5;
      bondSvgLines += `<line x1="${bond.x1}" y1="${bond.y1}" x2="${bond.x2}" y2="${bond.y2}" stroke="#4A5568" stroke-width="2" stroke-linecap="round" />`;
      bondSvgLines += `<line x1="${bond.x1 + ox}" y1="${bond.y1 + oy}" x2="${bond.x2 + ox}" y2="${bond.y2 + oy}" stroke="#4A5568" stroke-width="1.8" stroke-linecap="round" />`;
      bondSvgLines += `<line x1="${bond.x1 - ox}" y1="${bond.y1 - oy}" x2="${bond.x2 - ox}" y2="${bond.y2 - oy}" stroke="#4A5568" stroke-width="1.8" stroke-linecap="round" />`;
    }
  }

  let atomSvgNodes = '';
  for (const atom of finalAtoms) {
    const isCarbon = atom.symbol === 'C';
    // Draw background circle for heteroatoms
    if (!isCarbon) {
      atomSvgNodes += `<circle cx="${atom.x}" cy="${atom.y}" r="11" fill="#FFFFFF" />`;
      atomSvgNodes += `<text x="${atom.x}" y="${atom.y + 4.5}" font-family="system-ui, sans-serif" font-size="13" font-weight="700" fill="${atom.color}" text-anchor="middle">${atom.symbol}</text>`;
    } else {
      // Carbon junction dot
      atomSvgNodes += `<circle cx="${atom.x}" cy="${atom.y}" r="2.5" fill="#718096" />`;
    }
  }

  const svgString = `<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="${totalHeight}" viewBox="0 0 ${totalWidth} ${totalHeight}" class="smiles-molecule">${bondSvgLines}${atomSvgNodes}</svg>`;

  return {
    smiles,
    width: totalWidth,
    height: totalHeight,
    viewBox: `0 0 ${totalWidth} ${totalHeight}`,
    atoms: finalAtoms,
    bonds: finalBonds,
    svgString,
  };
}

/**
 * Parses raw text containing $$...$$ LaTeX, \ce{...} / \pu{...} mhchem,
 * and [SMILES: ...] / \smiles{...} molecular representations.
 */
export function parseFormulaText(rawInput: string | null | undefined): FormulaToken[] {
  if (!rawInput) return [];

  // Step 1: Normalize newlines without breaking lowercase LaTeX commands
  const textWithSafeNewlines = normalizeNewlinesPreservingLatex(rawInput);
  const text = repairEscapedArtifacts(textWithSafeNewlines);

  const tokens: FormulaToken[] = [];
  let currentIndex = 0;

  // Regex pattern matching:
  // 1. $$ ... $$ (display or inline LaTeX)
  // 2. \ce{ ... } or \\ce{ ... } (chemistry)
  // 3. \pu{ ... } or \\pu{ ... } (physical units)
  // 4. [SMILES: ...] or \smiles{...}
  const tokenRegex = /(\$\$[\s\S]*?\$\$|\\\\?ce\{[\s\S]*?\}|\\\\?pu\{[\s\S]*?\}|\[SMILES:\s*([^\]]+)\]|\\\\?smiles\{([^}]+)\})/g;

  let match: RegExpExecArray | null = null;

  while ((match = tokenRegex.exec(text)) !== null) {
    const matchIndex = match.index;
    const matchString = match[0];

    // Push preceding text token if non-empty
    if (matchIndex > currentIndex) {
      tokens.push({
        type: 'text',
        content: text.slice(currentIndex, matchIndex),
      });
    }

    if (matchString.startsWith('$$')) {
      // LaTeX $$ ... $$
      const rawFormula = matchString.slice(2, -2);
      const isDisplay = rawFormula.includes('\n') || /^\s*\\[a-zA-Z]/.test(rawFormula);
      tokens.push({
        type: 'latex',
        content: normalizeLatexFormula(rawFormula),
        displayMode: isDisplay,
      });
    } else if (/^\\\\?ce\{/.test(matchString)) {
      // mhchem \ce{...}
      const inner = matchString.replace(/^\\\\?ce\{/, '').slice(0, -1);
      tokens.push({
        type: 'chemistry',
        notation: 'ce',
        content: inner.trim(),
      });
    } else if (/^\\\\?pu\{/.test(matchString)) {
      // mhchem \pu{...}
      const inner = matchString.replace(/^\\\\?pu\{/, '').slice(0, -1);
      tokens.push({
        type: 'chemistry',
        notation: 'pu',
        content: inner.trim(),
      });
    } else if (matchString.startsWith('[SMILES:')) {
      // [SMILES: ...]
      const smilesValue = (match[2] || '').trim();
      tokens.push({
        type: 'smiles',
        smiles: smilesValue,
        descriptor: renderSmilesToSvgDescriptor(smilesValue),
      });
    } else if (/^\\\\?smiles\{/.test(matchString)) {
      // \smiles{...}
      const smilesValue = (match[3] || '').trim();
      tokens.push({
        type: 'smiles',
        smiles: smilesValue,
        descriptor: renderSmilesToSvgDescriptor(smilesValue),
      });
    }

    currentIndex = matchIndex + matchString.length;
  }

  // Push trailing text if any
  if (currentIndex < text.length) {
    tokens.push({
      type: 'text',
      content: text.slice(currentIndex),
    });
  }

  return tokens;
}
