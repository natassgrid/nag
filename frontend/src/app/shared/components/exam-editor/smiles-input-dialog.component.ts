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
  templateUrl: './smiles-input-dialog.component.html',
  styleUrls: ['./smiles-input-dialog.component.scss']
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
