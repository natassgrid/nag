import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetService } from './asset.service';
import { AssetResponse } from './asset.model';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-asset-metadata-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressSpinnerModule, RightDrawerComponent],
  template: `
    <app-right-drawer
      [isOpen]="isOpen"
      title="Edit Asset Metadata"
      subtitle="Update title, description, alt text, and tags for this asset."
      width="480px"
      (close)="cancel()"
    >
      <div drawer-body>
        <p class="filename" *ngIf="asset">{{ asset.originalFilename }} ({{ asset.assetType }})</p>
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
      </div>

      <div drawer-footer>
        <button mat-button [disabled]="saving" (click)="cancel()">Cancel</button>
        <button mat-raised-button color="primary" [disabled]="saving" (click)="save()">
          <mat-spinner *ngIf="saving" diameter="18" style="display:inline-block;margin-right:6px;vertical-align:middle;"></mat-spinner>
          {{ saving ? 'Saving...' : 'Save' }}
        </button>
      </div>
    </app-right-drawer>
  `,
  styles: [`
    .metadata-form { display: flex; flex-direction: column; gap: 4px; }
    .filename { font-size: 13px; color: #666; margin-bottom: 12px; }
    .error-msg { color: #c62828; font-size: 13px; }
  `]
})
export class AssetMetadataDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() asset?: AssetResponse;
  @Output() close = new EventEmitter<AssetResponse | null>();

  form!: FormGroup;
  saving = false;
  error = '';

  constructor(
    private fb: FormBuilder,
    private assetService: AssetService
  ) {
    this.initForm();
  }

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
    }
  }

  initForm(): void {
    const a = this.asset;
    this.form = this.fb.group({
      title: [a?.title || ''],
      description: [a?.description || ''],
      altText: [a?.altText || ''],
      tags: [a?.tags || ''],
      language: [a?.language || '']
    });
  }

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    if (!this.asset) return;
    this.saving = true;
    this.error = '';
    this.assetService.updateMetadata(this.asset.id, this.form.value).subscribe({
      next: (updated) => { this.saving = false; this.close.emit(updated); },
      error: (err) => { this.saving = false; this.error = err?.error?.message || 'Failed to update'; }
    });
  }
}
