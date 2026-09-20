/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import {
  Component,
  OnInit,
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  ViewEncapsulation,
  ViewChild,
  ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { Inject } from '@angular/core';

export interface SmilesInputDialogData {
  smiles?: string;
  title?: string;
  width?: number;
  height?: number;
  theme?: 'light' | 'dark';
}

export interface SmilesPreset {
  name: string;
  smiles: string;
  formula?: string;
  description?: string;
}

export interface SmilesCategory {
  name: string;
  icon: string;
  presets: SmilesPreset[];
}

/**
 * Modernized dialog for authoring 2D chemical structures from SMILES notation
 * with categorized template presets and live SmilesDrawer 2.0 preview.
 *
 * Addresses Issue #144:
 *  - Categorized chemical structure presets (Aromatics, Functional Groups, Biomolecules/Drugs, Alkanes)
 *  - Real-time 2D skeletal preview rendering with SmilesDrawer
 *  - Custom dimensions (width, height) and theme (light/dark) configuration
 *  - Molecule caption / title support
 *  - Robust syntax validation & error messaging
 *  - Keyboard accessibility (Enter / Ctrl+Enter to insert, Esc to cancel)
 *  - Pre-fill & edit mode support
 */
@Component({
  selector: 'exam-smiles-input-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatTabsModule,
    MatTooltipModule,
    MatButtonToggleModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  template: `
    <div class="smiles-dialog-container" (keydown)="onDialogKeydown($event)">
      <div mat-dialog-title class="smiles-dialog-header">
        <div class="header-title">
          <mat-icon class="header-icon">science</mat-icon>
          <span>{{ isEdit ? 'Edit Chemical Structure' : 'Insert Chemical Structure' }}</span>
        </div>
        <button mat-icon-button type="button" (click)="cancel()" class="close-btn" matTooltip="Cancel (Esc)">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="smiles-dialog-content">
        <!-- SMILES input and Title inputs -->
        <div class="inputs-section">
          <mat-form-field appearance="outline" class="full-width-field">
            <mat-label>SMILES Notation</mat-label>
            <input
              #smilesInput
              matInput
              [(ngModel)]="smiles"
              (ngModelChange)="onSmilesChange()"
              (keydown.enter)="onEnterPressed($event)"
              placeholder="e.g. c1ccccc1 (Benzene), CC(=O)O (Acetic Acid)"
              class="smiles-input-text"
              autocomplete="off"
            />
            <button
              *ngIf="smiles"
              mat-icon-button
              matSuffix
              type="button"
              (click)="clearSmiles()"
              matTooltip="Clear input"
              class="clear-btn"
            >
              <mat-icon>backspace</mat-icon>
            </button>
            <mat-hint>Enter Simplified Molecular Input Line Entry System (SMILES) string or pick a preset below.</mat-hint>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width-field">
            <mat-label>Caption / Molecule Label (optional)</mat-label>
            <input
              matInput
              [(ngModel)]="title"
              placeholder="e.g. Aspirin, Figure 1: Benzene Ring"
              autocomplete="off"
            />
          </mat-form-field>
        </div>

        <!-- Display Options & Dimensions -->
        <div class="options-row">
          <div class="dimension-fields">
            <mat-form-field appearance="outline" class="dim-field">
              <mat-label>Width (px)</mat-label>
              <input
                matInput
                type="number"
                min="140"
                max="600"
                step="10"
                [(ngModel)]="width"
                (ngModelChange)="onDimensionChange()"
              />
            </mat-form-field>

            <mat-form-field appearance="outline" class="dim-field">
              <mat-label>Height (px)</mat-label>
              <input
                matInput
                type="number"
                min="120"
                max="450"
                step="10"
                [(ngModel)]="height"
                (ngModelChange)="onDimensionChange()"
              />
            </mat-form-field>
          </div>

          <div class="theme-toggle-container">
            <label class="toggle-label">Theme:</label>
            <mat-button-toggle-group [(ngModel)]="theme" (ngModelChange)="onSmilesChange()" class="theme-toggle">
              <mat-button-toggle value="light">
                <mat-icon class="toggle-icon">light_mode</mat-icon>
                <span>Light</span>
              </mat-button-toggle>
              <mat-button-toggle value="dark">
                <mat-icon class="toggle-icon">dark_mode</mat-icon>
                <span>Dark</span>
              </mat-button-toggle>
            </mat-button-toggle-group>
          </div>
        </div>

        <!-- Live 2D Structure Preview -->
        <div class="preview-section">
          <div class="preview-header">
            <span class="preview-title">Live 2D Structure Preview:</span>
            <span *ngIf="title" class="preview-caption-badge">{{ title }}</span>
          </div>

          <div
            class="preview-box"
            [class.preview-box--dark]="theme === 'dark'"
            [class.preview-box--error]="!!renderError"
          >
            <canvas
              #canvasRef
              [width]="width"
              [height]="height"
              [style.display]="!renderError && smiles ? 'block' : 'none'"
              class="structure-canvas"
            ></canvas>

            <div *ngIf="!smiles && !renderError" class="preview-placeholder">
              <mat-icon class="placeholder-icon">biotech</mat-icon>
              <span>Select a preset template or enter SMILES notation to view 2D chemical structure</span>
            </div>

            <div *ngIf="renderError" class="preview-error">
              <mat-icon class="error-icon">error_outline</mat-icon>
              <div class="error-text">
                <strong>Invalid Chemical Notation:</strong>
                <span>{{ renderError }}</span>
              </div>
            </div>
          </div>
          <div *ngIf="title && smiles && !renderError" class="molecule-caption-label">
            {{ title }}
          </div>
        </div>

        <!-- Categorized Preset Templates -->
        <div class="presets-container">
          <div class="presets-title">Common Structure Presets</div>

          <mat-tab-group animationDuration="0ms" class="presets-tabs" [(selectedIndex)]="selectedTabIndex">
            <mat-tab *ngFor="let cat of categories">
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">{{ cat.icon }}</mat-icon>
                <span>{{ cat.name }}</span>
              </ng-template>

              <div class="presets-grid">
                <button
                  *ngFor="let p of cat.presets"
                  type="button"
                  class="preset-card-btn"
                  [matTooltip]="p.description || p.smiles"
                  matTooltipPosition="above"
                  (click)="selectPreset(p)"
                >
                  <span class="preset-name">{{ p.name }}</span>
                  <span class="preset-code">{{ p.smiles }}</span>
                </button>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="smiles-dialog-actions">
        <button mat-button (click)="cancel()" type="button">Cancel</button>
        <button
          mat-raised-button
          color="primary"
          [disabled]="!smiles.trim() || !!renderError"
          (click)="confirm()"
          type="button"
          class="confirm-btn"
        >
          <mat-icon>{{ isEdit ? 'save' : 'add' }}</mat-icon>
          <span>{{ isEdit ? 'Update Structure' : 'Insert Structure' }}</span>
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .smiles-dialog-container {
      display: flex;
      flex-direction: column;
      max-height: 90vh;
      box-sizing: border-box;
    }
    .smiles-dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 24px 8px;
      margin: 0;
      border-bottom: 1px solid #e0e0e0;
    }
    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 600;
      color: #0d47a1;
    }
    .header-icon {
      color: #1976d2;
      font-size: 24px;
      width: 24px;
      height: 24px;
    }
    .close-btn {
      color: #757575;
    }
    .smiles-dialog-content {
      padding: 16px 24px !important;
      overflow-y: auto;
      max-height: 65vh;
    }
    .inputs-section {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .full-width-field {
      width: 100%;
    }
    .smiles-input-text {
      font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace !important;
      font-size: 14px !important;
      font-weight: 500;
    }
    .clear-btn {
      color: #9e9e9e;
    }
    .options-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-top: 4px;
      margin-bottom: 12px;
      flex-wrap: wrap;
    }
    .dimension-fields {
      display: flex;
      gap: 12px;
    }
    .dim-field {
      width: 110px;
    }
    .theme-toggle-container {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .toggle-label {
      font-size: 13px;
      font-weight: 500;
      color: #555;
    }
    .theme-toggle {
      height: 38px;
    }
    .toggle-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      margin-right: 4px;
    }
    .preview-section {
      margin-bottom: 16px;
    }
    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }
    .preview-title {
      font-size: 12px;
      font-weight: 600;
      color: #424242;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .preview-caption-badge {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 12px;
      background: #e3f2fd;
      color: #1565c0;
      font-weight: 500;
    }
    .preview-box {
      min-height: 140px;
      padding: 12px;
      border: 1px solid #cfd8dc;
      border-radius: 8px;
      background: #ffffff;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: inset 0 1px 3px rgba(0,0,0,0.03);
      transition: all 0.2s ease;
      overflow: hidden;
    }
    .preview-box--dark {
      background: #1e1e24;
      border-color: #374151;
    }
    .preview-box--error {
      border-color: #ef9a9a;
      background: #fff8f8;
    }
    .structure-canvas {
      display: block;
      max-width: 100%;
      height: auto;
      border-radius: 4px;
    }
    .preview-placeholder {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #9e9e9e;
      font-size: 13px;
      font-style: italic;
      text-align: center;
    }
    .placeholder-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: #bdbdbd;
    }
    .preview-error {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      color: #c62828;
      font-size: 12.5px;
    }
    .error-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #d32f2f;
      flex-shrink: 0;
      margin-top: 1px;
    }
    .error-text {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .molecule-caption-label {
      font-size: 12px;
      color: #546e7a;
      font-style: italic;
      text-align: center;
      margin-top: 6px;
    }
    .presets-container {
      margin-top: 8px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      background: #fafafa;
    }
    .presets-title {
      font-size: 12px;
      font-weight: 600;
      color: #616161;
      padding: 8px 14px;
      background: #f5f5f5;
      border-bottom: 1px solid #e0e0e0;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .presets-tabs .mat-mdc-tab-header {
      background: #fff;
    }
    .tab-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      margin-right: 4px;
      vertical-align: middle;
    }
    .presets-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
      gap: 8px;
      padding: 10px;
      max-height: 150px;
      overflow-y: auto;
      background: #fff;
    }
    .preset-card-btn {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      justify-content: center;
      padding: 6px 10px;
      background: #f8f9fa;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      cursor: pointer;
      text-align: left;
      transition: all 0.15s ease;
      user-select: none;
    }
    .preset-card-btn:hover {
      background: #e0f2fe;
      border-color: #7dd3fc;
      transform: translateY(-1px);
      box-shadow: 0 2px 4px rgba(0,0,0,0.05);
    }
    .preset-name {
      font-size: 12px;
      font-weight: 600;
      color: #1e293b;
    }
    .preset-code {
      font-family: monospace;
      font-size: 10px;
      color: #64748b;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 100%;
    }
    .smiles-dialog-actions {
      padding: 12px 24px;
      border-top: 1px solid #e0e0e0;
      margin: 0;
      gap: 8px;
    }
    .confirm-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
  `]
})
export class SmilesInputDialogComponent implements OnInit, AfterViewInit {
  @ViewChild('canvasRef') canvasRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('smilesInput') smilesInputRef?: ElementRef<HTMLInputElement>;

  smiles = '';
  title = '';
  width = 260;
  height = 200;
  theme: 'light' | 'dark' = 'light';
  isEdit = false;
  renderError = '';
  selectedTabIndex = 0;

  private drawerInstance: any = null;
  private drawerLoading = false;

  categories: SmilesCategory[] = [];

  constructor(
    private dialogRef: MatDialogRef<SmilesInputDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: SmilesInputDialogData,
    private cdr: ChangeDetectorRef
  ) {
    this.smiles = data?.smiles || '';
    this.title = data?.title || '';
    this.width = data?.width || 260;
    this.height = data?.height || 200;
    this.theme = data?.theme || 'light';
    this.isEdit = !!data?.smiles;
  }

  ngOnInit(): void {
    this.initializePresetCategories();
  }

  ngAfterViewInit(): void {
    if (this.smiles) {
      this.drawStructure();
    }
  }

  private initializePresetCategories(): void {
    this.categories = [
      {
        name: 'Aromatics & Rings',
        icon: 'hexagon',
        presets: [
          { name: 'Benzene', smiles: 'c1ccccc1', description: 'C6H6 - Standard benzene ring' },
          { name: 'Toluene', smiles: 'Cc1ccccc1', description: 'Methylbenzene' },
          { name: 'Phenol', smiles: 'Oc1ccccc1', description: 'Hydroxybenzene' },
          { name: 'Aniline', smiles: 'Nc1ccccc1', description: 'Aminobenzene' },
          { name: 'Benzoic Acid', smiles: 'c1ccccc1C(=O)O', description: 'Carboxybenzene' },
          { name: 'Pyridine', smiles: 'c1ccncc1', description: 'Heterocyclic aromatic' },
          { name: 'Cyclohexane', smiles: 'C1CCCCC1', description: '6-membered aliphatic ring' },
          { name: 'Cyclopentane', smiles: 'C1CCCC1', description: '5-membered ring' },
          { name: 'Naphthalene', smiles: 'c1ccc2ccccc2c1', description: 'Bicyclic aromatic' },
          { name: 'Anthracene', smiles: 'c1ccc2cc3ccccc3cc2c1', description: 'Tricyclic aromatic' },
          { name: 'Furan', smiles: 'c1ccoc1', description: '5-membered oxygen ring' },
          { name: 'Thiophene', smiles: 'c1ccsc1', description: '5-membered sulfur ring' },
          { name: 'Pyrrole', smiles: 'c1cc[nH]c1', description: '5-membered nitrogen ring' }
        ]
      },
      {
        name: 'Functional Groups',
        icon: 'bubble_chart',
        presets: [
          { name: 'Ethanol', smiles: 'CCO', description: 'Alcohol: CH3-CH2-OH' },
          { name: 'Methanol', smiles: 'CO', description: 'Alcohol: CH3-OH' },
          { name: 'Acetic Acid', smiles: 'CC(=O)O', description: 'Carboxylic acid' },
          { name: 'Acetone', smiles: 'CC(=O)C', description: 'Ketone: (CH3)2CO' },
          { name: 'Acetaldehyde', smiles: 'CC=O', description: 'Aldehyde: CH3CHO' },
          { name: 'Diethyl Ether', smiles: 'CCOCC', description: 'Ether: (C2H5)2O' },
          { name: 'Ethyl Acetate', smiles: 'CCOC(=O)C', description: 'Ester: CH3COOCH2CH3' },
          { name: 'Formaldehyde', smiles: 'C=O', description: 'HCHO' },
          { name: 'Acetonitrile', smiles: 'CC#N', description: 'Nitrile: CH3CN' },
          { name: 'Chloroform', smiles: 'ClC(Cl)Cl', description: 'Trichloromethane' },
          { name: 'Urea', smiles: 'NC(=O)N', description: '(NH2)2CO' }
        ]
      },
      {
        name: 'Biomolecules & Drugs',
        icon: 'medication',
        presets: [
          { name: 'Aspirin', smiles: 'CC(=O)Oc1ccccc1C(=O)O', description: 'Acetylsalicylic acid' },
          { name: 'Paracetamol', smiles: 'CC(=O)Nc1ccc(O)cc1', description: 'Acetaminophen' },
          { name: 'Caffeine', smiles: 'Cn1c(=O)c2c(ncn2C)n(c1=O)C', description: '1,3,7-Trimethylxanthine' },
          { name: 'Ibuprofen', smiles: 'CC(C)Cc1ccc(cc1)C(C)C(=O)O', description: 'NSAID pain reliever' },
          { name: 'D-Glucose', smiles: 'OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O', description: 'Aldohexose carbohydrate' },
          { name: 'L-Alanine', smiles: 'N[C@@H](C)C(=O)O', description: 'Amino acid' },
          { name: 'Glycine', smiles: 'NCC(=O)O', description: 'Simplest amino acid' },
          { name: 'L-Cysteine', smiles: 'N[C@@H](CS)C(=O)O', description: 'Thiol amino acid' },
          { name: 'L-Serine', smiles: 'N[C@@H](CO)C(=O)O', description: 'Hydroxyl amino acid' }
        ]
      },
      {
        name: 'Alkanes & Chains',
        icon: 'linear_scale',
        presets: [
          { name: 'Methane', smiles: 'C', description: 'CH4' },
          { name: 'Ethane', smiles: 'CC', description: 'CH3-CH3' },
          { name: 'Propane', smiles: 'CCC', description: 'CH3-CH2-CH3' },
          { name: 'n-Butane', smiles: 'CCCC', description: 'CH3-(CH2)2-CH3' },
          { name: 'Isobutane', smiles: 'CC(C)C', description: '2-Methylpropane' },
          { name: 'Ethene', smiles: 'C=C', description: 'Ethylene: H2C=CH2' },
          { name: 'Ethyne', smiles: 'C#C', description: 'Acetylene: HC≡CH' },
          { name: '1,3-Butadiene', smiles: 'C=CC=C', description: 'Conjugated diene' },
          { name: 'Isoprene', smiles: 'CC(=C)C=C', description: '2-Methyl-1,3-butadiene' }
        ]
      }
    ];
  }

  selectPreset(preset: SmilesPreset): void {
    this.smiles = preset.smiles;
    if (!this.title) {
      this.title = preset.name;
    }
    this.onSmilesChange();
  }

  onSmilesChange(): void {
    this.drawStructure();
  }

  onDimensionChange(): void {
    if (this.width < 140) this.width = 140;
    if (this.width > 600) this.width = 600;
    if (this.height < 120) this.height = 120;
    if (this.height > 450) this.height = 450;
    this.drawStructure();
  }

  clearSmiles(): void {
    this.smiles = '';
    this.renderError = '';
    const canvas = this.canvasRef?.nativeElement;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
    }
    this.cdr.markForCheck();
    this.smilesInputRef?.nativeElement.focus();
  }

  onEnterPressed(event: Event): void {
    event.preventDefault();
    this.confirm();
  }

  onDialogKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      this.confirm();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.cancel();
    }
  }

  private async drawStructure(): Promise<void> {
    const trimmed = this.smiles.trim();
    const canvas = this.canvasRef?.nativeElement;

    if (!trimmed || !canvas) {
      this.renderError = '';
      this.cdr.markForCheck();
      return;
    }

    try {
      const drawer = await this.getDrawer();
      if (!drawer) {
        this.renderError = 'SmilesDrawer renderer is loading...';
        this.cdr.markForCheck();
        return;
      }

      // Clear previous canvas
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }

      if (typeof drawer.draw === 'function') {
        drawer.draw(trimmed, canvas, this.theme, false);
      } else if (typeof drawer.drawToCanvas === 'function') {
        drawer.drawToCanvas(trimmed, canvas, this.theme);
      } else if (typeof drawer.parse === 'function') {
        const tree = drawer.parse(trimmed);
        if (tree) {
          drawer.draw(tree, canvas, this.theme, false);
        }
      }

      this.renderError = '';
    } catch (e: any) {
      this.renderError = e?.message || 'Invalid SMILES chemical notation';
    }

    this.cdr.markForCheck();
  }

  private async getDrawer(): Promise<any> {
    if (this.drawerInstance) {
      // Update drawer dimensions
      this.drawerInstance.opts = {
        ...(this.drawerInstance.opts || {}),
        width: this.width,
        height: this.height
      };
      return this.drawerInstance;
    }

    if (this.drawerLoading) return null;
    this.drawerLoading = true;

    try {
      const sd: any = await import('smiles-drawer');
      const SvgDrawer = sd.SvgDrawer ?? sd.default?.SvgDrawer;
      const Drawer = sd.Drawer ?? sd.default?.Drawer ?? SvgDrawer ?? sd.default;

      if (Drawer) {
        this.drawerInstance = new Drawer({
          width: this.width,
          height: this.height,
          compactDrawing: false
        });
      }
    } catch (err) {
      console.warn('Could not load SmilesDrawer:', err);
    } finally {
      this.drawerLoading = false;
    }

    return this.drawerInstance;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    const trimmed = this.smiles.trim();
    if (!trimmed || this.renderError) return;
    this.dialogRef.close({
      smiles: trimmed,
      title: this.title.trim() || undefined,
      width: this.width,
      height: this.height,
      theme: this.theme
    });
  }
}
