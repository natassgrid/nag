/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * National Assessment Grid (NAG) — type stub for smiles-drawer v2.x
 *
 * The smiles-drawer package does not ship TypeScript declarations.
 * This stub silences the TS2307 error while still allowing dynamic import.
 */

declare module 'smiles-drawer' {
  /** Options accepted by SvgDrawer / Drawer constructors. */
  export interface DrawerOptions {
    width?: number;
    height?: number;
    compactDrawing?: boolean;
    atomVisualization?: 'default' | 'balls';
    explicitHydrogens?: boolean;
    terminalCarbons?: boolean;
    themes?: Record<string, unknown>;
    [key: string]: unknown;
  }

  /** Main drawing class — renders SMILES onto a <canvas> element. */
  export class Drawer {
    constructor(options?: DrawerOptions);
    /** Parse a SMILES string into an AST tree. */
    parse(smiles: string): unknown;
    /** Draw a parsed tree (or raw SMILES) onto a canvas element. */
    draw(
      treeOrSmiles: unknown,
      canvas: HTMLCanvasElement,
      theme?: string,
      invert?: boolean
    ): void;
  }

  /** Alias used in some smiles-drawer 2.x builds. */
  export class SvgDrawer extends Drawer {}

  export default Drawer;
}
