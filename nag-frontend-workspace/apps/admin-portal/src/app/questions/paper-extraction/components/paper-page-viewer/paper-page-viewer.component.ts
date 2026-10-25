import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  effect,
  inject,
  DestroyRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PaperExtractionService } from '../../services';

@Component({
  selector: 'nag-paper-page-viewer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './paper-page-viewer.component.html',
  styleUrl: './paper-page-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperPageViewerComponent {
  private readonly paperService = inject(PaperExtractionService);
  private readonly destroyRef = inject(DestroyRef);

  paperId = input.required<string>();
  currentPage = input<number>(1);
  totalPages = input<number>(1);

  pageChange = output<number>();
  closeViewer = output<void>();

  imageUrl = signal<string | null>(null);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  zoomPercent = signal<number>(100);

  constructor() {
    effect(() => {
      const id = this.paperId();
      const page = this.currentPage();
      if (id && page > 0) {
        this.fetchPageImage(id, page);
      }
    });

    this.destroyRef.onDestroy(() => {
      this.cleanupCurrentUrl();
    });
  }

  private cleanupCurrentUrl(): void {
    const current = this.imageUrl();
    if (current && current.startsWith('blob:')) {
      URL.revokeObjectURL(current);
    }
  }

  fetchPageImage(id: string, page: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.paperService.getPageImage(id, page).subscribe({
      next: (blob) => {
        this.cleanupCurrentUrl();
        const objUrl = URL.createObjectURL(blob);
        this.imageUrl.set(objUrl);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.message || err?.message || 'Failed to render page image preview'
        );
      },
    });
  }

  prevPage(): void {
    const p = this.currentPage();
    if (p > 1) {
      this.pageChange.emit(p - 1);
    }
  }

  nextPage(): void {
    const p = this.currentPage();
    const total = Math.max(1, this.totalPages());
    if (p < total) {
      this.pageChange.emit(p + 1);
    }
  }

  onPageJump(targetPage: number): void {
    const total = Math.max(1, this.totalPages());
    const validPage = Math.min(Math.max(1, targetPage), total);
    this.pageChange.emit(validPage);
  }

  zoomIn(): void {
    this.zoomPercent.update((z) => Math.min(250, z + 25));
  }

  zoomOut(): void {
    this.zoomPercent.update((z) => Math.max(50, z - 25));
  }

  resetZoom(): void {
    this.zoomPercent.set(100);
  }
}
