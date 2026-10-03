import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { RouterModule } from '@angular/router';
import { PracticeService } from '../../services/practice.service';
import { PracticeSet } from '../../models';
import { PracticeSetCardComponent } from '../practice-set-card/practice-set-card.component';
import { PracticeLaunchDialogComponent } from '../practice-launch-dialog/practice-launch-dialog.component';

export type SourceFilter = 'ALL' | 'EXAM_CLONE' | 'MANUAL';

@Component({
  selector: 'app-practice-hub',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    PracticeSetCardComponent
  ],
  templateUrl: './practice-hub.component.html',
  styleUrl: './practice-hub.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeHubComponent implements OnInit {
  private readonly practiceService = inject(PracticeService);
  private readonly dialog = inject(MatDialog);

  readonly practiceSets = signal<PracticeSet[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  readonly searchQuery = signal<string>('');
  readonly selectedSource = signal<SourceFilter>('ALL');
  readonly selectedSubject = signal<string>('ALL');

  readonly subjects = computed(() => {
    const rawSubjects = this.practiceSets()
      .map(s => s.subjectSlug)
      .filter((s): s is string => !!s && s.trim().length > 0);
    return Array.from(new Set(rawSubjects)).sort();
  });

  readonly kpiStats = computed(() => {
    const sets = this.practiceSets();
    const totalSets = sets.length;
    const officialCount = sets.filter(s => s.source === 'EXAM_CLONE').length;
    const curatedCount = sets.filter(s => s.source === 'MANUAL').length;
    const totalQuestions = sets.reduce((sum, s) => sum + (s.totalQuestions || 0), 0);
    const avgDuration = totalSets > 0 ? Math.round(sets.reduce((sum, s) => sum + (s.durationMinutes || 0), 0) / totalSets) : 0;

    return {
      totalSets,
      officialCount,
      curatedCount,
      totalQuestions,
      avgDuration
    };
  });

  readonly filteredSets = computed(() => {
    let list = this.practiceSets();
    const query = this.searchQuery().trim().toLowerCase();
    const source = this.selectedSource();
    const subject = this.selectedSubject();

    if (source !== 'ALL') {
      list = list.filter(s => s.source === source);
    }

    if (subject !== 'ALL') {
      list = list.filter(s => s.subjectSlug?.toLowerCase() === subject.toLowerCase());
    }

    if (query) {
      list = list.filter(s =>
        (s.name && s.name.toLowerCase().includes(query)) ||
        (s.description && s.description.toLowerCase().includes(query)) ||
        (s.subjectSlug && s.subjectSlug.toLowerCase().includes(query))
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadPracticeSets();
  }

  loadPracticeSets(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.practiceService.getSets().subscribe({
      next: (sets) => {
        this.practiceSets.set(sets || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load practice sets.');
        this.isLoading.set(false);
      }
    });
  }

  setSource(source: SourceFilter): void {
    this.selectedSource.set(source);
  }

  setSubject(subject: string): void {
    this.selectedSubject.set(subject);
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedSource.set('ALL');
    this.selectedSubject.set('ALL');
  }

  openLaunch(set: PracticeSet): void {
    this.dialog.open(PracticeLaunchDialogComponent, {
      data: { set },
      width: '440px',
      panelClass: 'nag-dialog-clean'
    });
  }
}
