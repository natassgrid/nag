import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from './asset.service';
import { AssetResponse } from './asset.model';

export interface AssetMetadataDialogData { asset: AssetResponse; }

@Component({
  selector: 'app-asset-metadata-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>Edit Metadata</h2>
    <mat-dialog-content>
      <p class="filename">{{ data.asset.originalFilename }} ({{ data.asset.assetType }})</p>
      <form [formGroup]="form" class="metadata-form">
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Alt Text (accessibility)</mat-label>
          <input matInput formControlName="altText" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Tags (comma-separated)</mat-label>
          <input matInput formControlName="tags" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Language</mat-label>
          <input matInput formControlName="language" placeholder="e.g. en, hi" />
        </mat-form-field>
      </form>
      <p *ngIf="error" class="error-msg">{{ error }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [disabled]="saving" (click)="dialogRef.close()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="saving" (click)="save()">
        <mat-spinner *ngIf="saving" diameter="18" style="display:inline-block;margin-right:6px;vertical-align:middle;"></mat-spinner>
        {{ saving ? 'Saving...' : 'Save' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .metadata-form { display: flex; flex-direction: column; gap: 4px; min-width: 400px; }
    .filename { font-size: 13px; color: #666; margin-bottom: 12px; }
    .error-msg { color: #c62828; font-size: 13px; }
  `]
})
export class AssetMetadataDialogComponent {
  form: FormGroup;
  saving = false;
  error = '';

  constructor(
    public dialogRef: MatDialogRef<AssetMetadataDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssetMetadataDialogData,
    private fb: FormBuilder,
    private assetService: AssetService
  ) {
    const a = data.asset;
    this.form = this.fb.group({
      title: [a.title || ''],
      description: [a.description || ''],
      altText: [a.altText || ''],
      tags: [a.tags || ''],
      language: [a.language || '']
    });
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.assetService.updateMetadata(this.data.asset.id, this.form.value).subscribe({
      next: (updated) => { this.saving = false; this.dialogRef.close(updated); },
      error: (err) => { this.saving = false; this.error = err?.error?.message || 'Failed to update'; }
    });
  }
}
