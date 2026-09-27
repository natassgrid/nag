import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EducationEntry } from '../../models';

export interface EducationFormState {
  id: string;
  qualification: string;
  courseName: string;
  boardOrUniversity: string;
  institutionName: string;
  passingYear: number;
  scoreType: 'PERCENTAGE' | 'CGPA';
  scoreValue: string;
  specialization: string;
  rollNumber: string;
  certificateAssetId?: string;
  certificateFileName?: string;
}

const DEFAULT_QUALIFICATIONS = [
  '10th Standard / Secondary (SSC)',
  '12th Standard / Higher Secondary (HSC)',
  'Diploma / Polytechnic',
  'Bachelor Degree (B.Tech / B.E / B.Sc / B.Com / B.A / BBA)',
  'Master Degree (M.Tech / M.E / M.Sc / M.Com / MBA / MCA)',
  'Doctorate (Ph.D)',
  'Professional Certification / Other',
];

@Component({
  selector: 'nag-education-details-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './education-details-panel.component.html',
  styleUrl: './education-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EducationDetailsPanelComponent {
  readonly education = input.required<EducationEntry[]>();
  readonly saving = input<boolean>(false);

  readonly saveEducation = output<{ isNew: boolean; data: EducationEntry }>();
  readonly deleteEducation = output<string>();

  readonly isModalOpen = signal<boolean>(false);
  readonly isEditing = signal<boolean>(false);
  readonly isDeleteConfirmOpen = signal<boolean>(false);
  readonly entryToDelete = signal<EducationEntry | null>(null);
  readonly formError = signal<string | null>(null);

  readonly qualificationOptions = DEFAULT_QUALIFICATIONS;
  readonly currentYear = new Date().getFullYear();

  readonly form = signal<EducationFormState>(this.createEmptyForm());

  createEmptyForm(): EducationFormState {
    return {
      id: '',
      qualification: '10th Standard / Secondary (SSC)',
      courseName: '',
      boardOrUniversity: '',
      institutionName: '',
      passingYear: this.currentYear - 2,
      scoreType: 'PERCENTAGE',
      scoreValue: '',
      specialization: '',
      rollNumber: '',
      certificateAssetId: '',
      certificateFileName: '',
    };
  }

  openAddModal(): void {
    this.isEditing.set(false);
    this.form.set(this.createEmptyForm());
    this.formError.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(entry: EducationEntry): void {
    this.isEditing.set(true);
    const isCgpa = entry.percentageOrCgpa.toLowerCase().includes('cgpa') || Number(entry.percentageOrCgpa) <= 10;
    const rawScore = entry.percentageOrCgpa.replace(/[^0-9.]/g, '');

    this.form.set({
      id: entry.id,
      qualification: entry.qualification || '10th Standard / Secondary (SSC)',
      courseName: entry.courseName || '',
      boardOrUniversity: entry.boardOrUniversity || '',
      institutionName: entry.institutionName || '',
      passingYear: entry.passingYear || this.currentYear,
      scoreType: isCgpa ? 'CGPA' : 'PERCENTAGE',
      scoreValue: rawScore,
      specialization: entry.specialization || '',
      rollNumber: entry.rollNumber || '',
      certificateAssetId: entry.certificateAssetId,
      certificateFileName: entry.certificateFileName,
    });
    this.formError.set(null);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.formError.set(null);
  }

  onScoreTypeChange(type: 'PERCENTAGE' | 'CGPA'): void {
    this.form.update((f) => ({ ...f, scoreType: type }));
  }

  onCertificateSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.form.update((f) => ({
        ...f,
        certificateFileName: file.name,
        certificateAssetId: `asset-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
      }));
    }
  }

  submitForm(): void {
    const current = this.form();
    this.formError.set(null);

    // Validation
    if (!current.qualification || !current.qualification.trim()) {
      this.formError.set('Please select or specify a qualification level.');
      return;
    }
    if (!current.boardOrUniversity || !current.boardOrUniversity.trim()) {
      this.formError.set('Board or University name is required.');
      return;
    }
    if (!current.passingYear || current.passingYear < 1950 || current.passingYear > this.currentYear + 5) {
      this.formError.set(`Passing year must be between 1950 and ${this.currentYear + 5}.`);
      return;
    }
    if (!current.scoreValue || !current.scoreValue.trim()) {
      this.formError.set('Percentage or CGPA score is required.');
      return;
    }

    const numScore = parseFloat(current.scoreValue);
    if (isNaN(numScore)) {
      this.formError.set('Please enter a valid numeric score.');
      return;
    }

    if (current.scoreType === 'PERCENTAGE' && (numScore < 0 || numScore > 100)) {
      this.formError.set('Percentage must be between 0% and 100%.');
      return;
    }

    if (current.scoreType === 'CGPA' && (numScore < 0 || numScore > 10)) {
      this.formError.set('CGPA must be between 0.0 and 10.0.');
      return;
    }

    const formattedScore = current.scoreType === 'CGPA' ? `${numScore.toFixed(2)} CGPA` : `${numScore.toFixed(2)}%`;

    const entry: EducationEntry = {
      id: current.id || `edu-${Date.now()}`,
      qualification: current.qualification.trim(),
      courseName: current.courseName?.trim() || undefined,
      boardOrUniversity: current.boardOrUniversity.trim(),
      institutionName: current.institutionName?.trim() || undefined,
      passingYear: Number(current.passingYear),
      percentageOrCgpa: formattedScore,
      specialization: current.specialization?.trim() || undefined,
      rollNumber: current.rollNumber?.trim() || undefined,
      certificateAssetId: current.certificateAssetId || undefined,
      certificateFileName: current.certificateFileName || undefined,
    };

    this.saveEducation.emit({
      isNew: !this.isEditing(),
      data: entry,
    });

    this.closeModal();
  }

  promptDelete(entry: EducationEntry): void {
    this.entryToDelete.set(entry);
    this.isDeleteConfirmOpen.set(true);
  }

  confirmDelete(): void {
    const entry = this.entryToDelete();
    if (entry) {
      this.deleteEducation.emit(entry.id);
    }
    this.isDeleteConfirmOpen.set(false);
    this.entryToDelete.set(null);
  }

  cancelDelete(): void {
    this.isDeleteConfirmOpen.set(false);
    this.entryToDelete.set(null);
  }
}
