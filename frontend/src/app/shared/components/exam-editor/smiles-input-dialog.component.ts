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
  ViewChild,
  ElementRef,
  Inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ChemicalStructurePlugin } from './plugins/chemical-structure.plugin';

export interface SmilesInputDialogData {
  smiles?: string;
  title?: string;
}

/**
 * Dialog for authoring a 2D chemical structure via SMILES notation.
 * Uses SmilesDrawer 2.0 (smiles-drawer) for live SVG preview.
 * Opened by the ChemicalStructurePlugin toolbar button.
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
    MatChipsModule
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>
      <mat-icon style="vertical-align:middle;margin-right:6px">science</mat-icon>
      Insert Chemical Structure
    </h2>

    <mat-dialog-content>
      <mat-form-field appearance="outline" style="width:100%">
        <mat-label>SMILES notation</mat-label>
        <input
          matInput
          [(ngModel)]="smiles"
          (ngModelChange)="onSmilesChange()"
          placeholder="e.g. c1ccccc1  (benzene)"
          style="font-family: monospace"
        />
        <mat-hint>Standard SMILES — aromatic atoms in lowercase (c, n, o, s)</mat-hint>
      </mat-form-field>

      <mat-form-field appearance="outline" style="width:100%;margin-top:8px">
        <mat-label>Caption / molecule name (optional)</mat-label>
        <input matInput [(ngModel)]="title" placeholder="e.g. Benzene" />
      </mat-form-field>

      <!-- Quick-pick templates -->
      <div class="smiles-templates">
        <span class="smiles-templates-label">Quick templates:</span>
        <mat-chip-set>
          <mat-chip
            *ngFor="let t of templates"
            (click)="selectTemplate(t.smiles, t.label)"
            style="cursor:pointer"
          >{{ t.label }}</mat-chip>
        </mat-chip-set>
      </div>

      <!-- Live SmilesDrawer SVG preview -->
      <div class="smiles-preview">
        <span class="smiles-preview-label">2D Structure Preview:</span>
        <div class="smiles-canvas-wrapper">
          <canvas #smilesCanvas width="280" height="220"></canvas>
          <div *ngIf="renderError" class="smiles-error">
            <mat-icon style="font-size:14px;vertical-align:middle">warning</mat-icon>
            {{ renderError }}
          </div>
          <div *ngIf="!smiles" class="smiles-empty">
            Type a SMILES string to see the structure
          </div>
        </div>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()" type="button">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!smiles.trim() || !!renderError" (click)="confirm()" type="button">
        Insert Structure
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .smiles-templates { margin: 12px 0 8px; }
    .smiles-templates-label { font-size: 11px; color: #757575; display: block; margin-bottom: 6px; }
    .smiles-preview { margin-top: 16px; }
    .smiles-preview-label { font-size: 11px; color: #757575; display: block; margin-bottom: 8px; }
    .smiles-canvas-wrapper {
      border: 1px solid #e0e0e0; border-radius: 6px; background: #fff;
      display: flex; align-items: center; justify-content: center;
      min-height: 240px; position: relative; padding: 8px;
    }
    .smiles-error { color: #d32f2f; font-size: 12px; position: absolute; bottom: 8px; left: 8px; }
    .smiles-empty { color: #9e9e9e; font-size: 13px; }
    canvas { max-width: 100%; }
  `]
})
export class SmilesInputDialogComponent implements OnInit, AfterViewInit {

  @ViewChild('smilesCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  smiles = '';
  title = '';
  renderError = '';

  // eslint-disable-next-line @typescript-eslint/naming-convention
  readonly templates = ChemicalStructurePlugin.TEMPLATES;

  // SmilesDrawer instance — loaded dynamically to avoid build-time issues
  // if smiles-drawer is not yet installed. Gracefully degrades to show SMILES text.
  private drawer: any = null;

  constructor(
    private dialogRef: MatDialogRef<SmilesInputDialogComponent>,
    @Inject(MAT_DIALOG_DATA) data: SmilesInputDialogData,
    private cdr: ChangeDetectorRef
  ) {
    this.smiles = data?.smiles || '';
    this.title = data?.title || '';
  }

  ngOnInit(): void {}

  async ngAfterViewInit(): Promise<void> {
    await this.loadDrawer();
    if (this.smiles) this.drawSmiles();
  }

  private async loadDrawer(): Promise<void> {
    try {
      // Dynamic import so a missing package doesn't crash at startup
      const sd: any = await import('smiles-drawer');
      const SvgDrawer = sd.SvgDrawer ?? sd.default?.SvgDrawer;
      if (SvgDrawer) {
        this.drawer = new SvgDrawer({ width: 280, height: 220, compactDrawing: false });
      } else {
        // smiles-drawer v2 may export differently
        const Drawer = sd.Drawer ?? sd.default?.Drawer;
        this.drawer = Drawer ? new Drawer({ width: 280, height: 220 }) : null;
      }
    } catch {
      // Package not installed — preview unavailable, insertion still works
      this.drawer = null;
    }
  }

  onSmilesChange(): void {
    this.renderError = '';
    this.drawSmiles();
  }

  selectTemplate(smiles: string, label: string): void {
    this.smiles = smiles;
    this.title = this.title || label;
    this.renderError = '';
    this.drawSmiles();
    this.cdr.markForCheck();
  }

  private drawSmiles(): void {
    if (!this.canvasRef) return;
    const canvas = this.canvasRef.nativeElement;

    if (!this.smiles.trim()) {
      const ctx = canvas.getContext('2d');
      ctx?.clearRect(0, 0, canvas.width, canvas.height);
      this.cdr.markForCheck();
      return;
    }

    if (!this.drawer) {
      // Graceful fallback: draw the SMILES text on canvas
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.font = '13px monospace';
        ctx.fillStyle = '#424242';
        ctx.fillText(this.smiles, 12, canvas.height / 2);
      }
      this.cdr.markForCheck();
      return;
    }

    try {
      // smiles-drawer v2 API: parse → draw
      const sd: any = (window as any).__smilesDrawer ?? this.drawer;
      if (typeof this.drawer.draw === 'function') {
        // v1-style API
        this.drawer.draw(this.smiles, canvas, 'light', false);
      } else if (typeof this.drawer.drawToCanvas === 'function') {
        this.drawer.drawToCanvas(this.smiles, canvas, 'light');
      } else {
        // v2 static parse → draw
        const tree = this.drawer.parse?.(this.smiles);
        if (tree) this.drawer.draw(tree, canvas, 'light', false);
      }
      this.renderError = '';
    } catch (e: any) {
      this.renderError = e?.message || 'Invalid SMILES notation';
    }

    this.cdr.markForCheck();
  }

  cancel(): void { this.dialogRef.close(null); }

  confirm(): void {
    if (!this.smiles.trim()) return;
    this.dialogRef.close({
      smiles: this.smiles.trim(),
      title: this.title?.trim() || undefined
    });
  }
}
