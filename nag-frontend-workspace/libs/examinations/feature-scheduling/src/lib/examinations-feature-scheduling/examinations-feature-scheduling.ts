import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  StatCardComponent,
  StatusBadgeComponent,
  SearchInputComponent,
  EmptyStateComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  ExaminationService,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-examinations-feature-scheduling',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    StatCardComponent,
    StatusBadgeComponent,
    SearchInputComponent,
    EmptyStateComponent,
  ],
  templateUrl: './examinations-feature-scheduling.component.html',
  styleUrl: './examinations-feature-scheduling.component.scss',
})
export class ExaminationsFeatureScheduling implements OnInit {
  readonly examService = inject(ExaminationService);

  searchFilter = signal<string>('');
  showCreateModal = signal<boolean>(false);

  newTitle = '';
  newCode = '';
  newSubject = '';
  newDate = '2026-10-15';
  newSlots = 50000;

  totalCapacity = computed(() =>
    this.examService
      .schedules()
      .reduce((sum, item) => sum + item.totalSlots, 0)
  );

  totalRegistered = computed(() =>
    this.examService
      .schedules()
      .reduce((sum, item) => sum + item.registeredCandidates, 0)
  );

  utilizationRate = computed(() => {
    const cap = this.totalCapacity();
    if (!cap) return 0;
    return Math.round((this.totalRegistered() / cap) * 100);
  });

  filteredSchedules = computed(() => {
    const query = this.searchFilter().toLowerCase().trim();
    if (!query) return this.examService.schedules();
    return this.examService
      .schedules()
      .filter(
        (s) =>
          s.title.toLowerCase().includes(query) ||
          s.examCode.toLowerCase().includes(query) ||
          s.subject.toLowerCase().includes(query)
      );
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.examService.loadSchedules().subscribe();
  }

  statusVariant(status: string): StatusVariant {
    switch (status) {
      case 'IN_PROGRESS':
        return 'warn';
      case 'COMPLETED':
        return 'neutral';
      case 'SCHEDULED':
        return 'primary';
      case 'CANCELLED':
        return 'error';
      default:
        return 'neutral';
    }
  }

  viewCenters(): void {
    alert('Center Capacity Allocation Matrix: 42 test centers allocated across 8 geographical zones.');
  }

  submitSchedule(): void {
    if (!this.newTitle || !this.newCode) return;

    this.examService
      .createSchedule({
        examCode: this.newCode,
        title: this.newTitle,
        subject: this.newSubject || 'General',
        sessionDate: this.newDate,
        startTime: '09:30 AM',
        endTime: '12:30 PM',
        totalSlots: this.newSlots,
        status: 'SCHEDULED',
      })
      .subscribe(() => {
        this.showCreateModal.set(false);
        this.newTitle = '';
        this.newCode = '';
      });
  }
}
