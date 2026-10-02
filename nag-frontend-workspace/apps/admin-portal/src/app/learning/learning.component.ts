import { Component, ChangeDetectionStrategy, inject, OnInit, signal, computed } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { PracticeSetKpiCardsComponent, PracticeSetListComponent, PracticeSetFormModalComponent, PracticeSetDeleteDialogComponent } from './components';
import { PracticeSetService } from './services';
import { PracticeSet } from './models';

@Component({
  selector: 'app-learning',
  standalone: true,
  imports: [
    PracticeSetKpiCardsComponent,
    PracticeSetListComponent,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './learning.component.html',
  styleUrls: ['./learning.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LearningComponent implements OnInit {
  private readonly practiceSetService = inject(PracticeSetService);
  private readonly dialog = inject(MatDialog);

  practiceSets = signal<PracticeSet[]>([]);
  isLoading = signal(true);

  totalSets = computed(() => this.practiceSets().length);
  publishedCount = computed(() => this.practiceSets().filter(s => s.published).length);

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
      }
    });
  }

  openCreateModal() {
    const dialogRef = this.dialog.open(PracticeSetFormModalComponent, {
      width: '600px',
      data: { set: null, mode: 'create' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.loadSets();
    });
  }

  openEditModal(set: PracticeSet) {
    const dialogRef = this.dialog.open(PracticeSetFormModalComponent, {
      width: '600px',
      data: { set, mode: 'edit' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.loadSets();
    });
  }

  openDeleteDialog(set: PracticeSet) {
    const dialogRef = this.dialog.open(PracticeSetDeleteDialogComponent, {
      width: '400px',
      data: { set }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) this.loadSets();
    });
  }

  togglePublish(set: PracticeSet) {
    const action = set.published ? this.practiceSetService.unpublish(set.id) : this.practiceSetService.publish(set.id);
    action.subscribe({
      next: () => this.loadSets(),
      error: (err) => console.error('Failed to toggle publish status', err)
    });
  }
}
