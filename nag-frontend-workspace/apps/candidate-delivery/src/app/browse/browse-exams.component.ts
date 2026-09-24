import {
  Component,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  SearchInputComponent,
  StatusBadgeComponent,
} from '@nag-frontend-workspace/shared-ui-components';

export interface PublicExamListing {
  id: string;
  code: string;
  title: string;
  department: string;
  applicationDeadline: string;
  examDate: string;
  feeAmount: number;
  eligibility: string;
  totalSeats: number;
  applied: boolean;
}

@Component({
  selector: 'app-browse-exams',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    SearchInputComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './browse-exams.component.html',
  styleUrl: './browse-exams.component.scss',
})
export class BrowseExamsComponent {
  searchQuery = signal<string>('');
  selectedCategory = signal<string>('ALL');

  selectedApplyExam = signal<PublicExamListing | null>(null);
  selectedCenterCity = 'DELHI_NCR';

  exams = signal<PublicExamListing[]>([
    {
      id: 'exam-1',
      code: 'NES-2026-S1',
      title: 'National Eligibility Screening (Computer Science & AI)',
      department: 'Ministry of Education / National Testing Agency',
      applicationDeadline: '2026-09-26',
      examDate: '2026-09-28',
      feeAmount: 650,
      eligibility: 'B.Tech / MCA / M.Sc with min 60% aggregate',
      totalSeats: 250000,
      applied: true,
    },
    {
      id: 'exam-2',
      code: 'GATE-DPI-2026',
      title: 'Graduate Assessment for Open DPI Engineering',
      department: 'Digital India Corporation',
      applicationDeadline: '2026-10-05',
      examDate: '2026-10-15',
      feeAmount: 850,
      eligibility: 'Graduate Degree in Engineering or Sciences',
      totalSeats: 150000,
      applied: false,
    },
    {
      id: 'exam-3',
      code: 'CSE-PRE-2026',
      title: 'Civil Services Preliminary Screening (General Studies & CSAT)',
      department: 'Union Public Service Commission',
      applicationDeadline: '2026-11-01',
      examDate: '2026-11-25',
      feeAmount: 100,
      eligibility: 'Any Recognized Bachelor Degree',
      totalSeats: 1000000,
      applied: false,
    },
    {
      id: 'exam-4',
      code: 'RBI-GRADE-B-2026',
      title: 'Reserve Bank Officer Recruitment Phase I',
      department: 'Reserve Bank of India',
      applicationDeadline: '2026-10-20',
      examDate: '2026-11-10',
      feeAmount: 850,
      eligibility: 'Graduation with min 60% marks',
      totalSeats: 80000,
      applied: false,
    },
  ]);

  filteredExams = () => {
    const q = this.searchQuery().toLowerCase().trim();
    return this.exams().filter((e) => {
      const matchQuery =
        !q ||
        e.title.toLowerCase().includes(q) ||
        e.code.toLowerCase().includes(q) ||
        e.department.toLowerCase().includes(q);
      return matchQuery;
    });
  };

  onSearchChange(text: string): void {
    this.searchQuery.set(text);
  }

  openApplyModal(exam: PublicExamListing): void {
    this.selectedApplyExam.set(exam);
  }

  confirmApplication(exam: PublicExamListing): void {
    this.exams.update((list) =>
      list.map((item) => (item.id === exam.id ? { ...item, applied: true } : item))
    );
    this.selectedApplyExam.set(null);
    alert(`Successfully registered for ${exam.title}! Your admit card will be generated in your dashboard.`);
  }
}
