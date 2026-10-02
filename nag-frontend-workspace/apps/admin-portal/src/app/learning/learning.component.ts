import { Component, ChangeDetectionStrategy, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import {
  PracticeSetKpiCardsComponent,
  PracticeSetListComponent,
  PracticeSetFormModalComponent,
  PracticeSetDeleteDialogComponent,
} from './components';
import { PracticeSetService } from './services';
import { PracticeSet } from './models';

@Component({
  selector: 'app-learning',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PracticeSetKpiCardsComponent,
    PracticeSetListComponent,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './learning.component.html',
  styleUrls: ['./learning.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LearningComponent implements OnInit {
  private readonly practiceSetService = inject(PracticeSetService);
  private readonly dialog = inject(MatDialog);

  readonly practiceSets = signal<PracticeSet[]>([]);
  readonly isLoading = signal(true);
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL');

  readonly filteredSets = computed(() => {
    let list = this.practiceSets();
    const query = this.searchQuery().trim().toLowerCase();
    if (query) {
      list = list.filter(
        (s) =>
          s.name.toLowerCase().includes(query) ||
          (s.description && s.description.toLowerCase().includes(query)) ||
          (s.subjectSlug && s.subjectSlug.toLowerCase().includes(query))
      );
    }
    const filter = this.statusFilter();
    if (filter === 'PUBLISHED') {
      list = list.filter((s) => s.published);
    } else if (filter === 'DRAFT') {
      list = list.filter((s) => !s.published);
    }
    return list;
  });

  readonly totalSets = computed(() => this.practiceSets().length);
  readonly publishedCount = computed(() => this.practiceSets().filter((s) => s.published).length);
  readonly totalQuestionsCount = computed(() =>
    this.practiceSets().reduce((sum, s) => sum + (s.totalQuestions || 0), 0)
  );

  ngOnInit() {
    this.loadSets();
  }

  loadSets() {
    this.isLoading.set(true);
    this.practiceSetService.getAll().subscribe({
      next: (sets) => {
        this.practiceSets.set(sets);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load sets', err);
        this.isLoading.set(false);
      },
    });
  }

  openCreateModal() {
    const dialogRef = this.dialog.open(PracticeSetFormModalComponent, {
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set: null, mode: 'create' },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) this.loadSets();
    });
  }

  openEditModal(set: PracticeSet) {
    const dialogRef = this.dialog.open(PracticeSetFormModalComponent, {
      width: '640px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set, mode: 'edit' },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) this.loadSets();
    });
  }

  openDeleteDialog(set: PracticeSet) {
    const dialogRef = this.dialog.open(PracticeSetDeleteDialogComponent, {
      width: '440px',
      maxWidth: '95vw',
      panelClass: 'custom-dialog-container',
      data: { set },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) this.loadSets();
    });
  }

  togglePublish(set: PracticeSet) {
    const action = set.published
      ? this.practiceSetService.unpublish(set.id)
      : this.practiceSetService.publish(set.id);
    action.subscribe({
      next: () => this.loadSets(),
      error: (err) => console.error('Failed to toggle publish status', err),
    });
  }
}
