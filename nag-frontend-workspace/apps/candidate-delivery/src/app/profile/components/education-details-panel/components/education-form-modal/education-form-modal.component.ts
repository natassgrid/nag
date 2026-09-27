import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EducationEntry } from '../../../../models';
import { DEFAULT_QUALIFICATIONS, EducationFormState } from '../../models';

@Component({
  selector: 'nag-education-form-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './education-form-modal.component.html',
  styleUrl: './education-form-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EducationFormModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly isEditing = input<boolean>(false);
  readonly initialData = input<EducationEntry | null>(null);
  readonly saving = input<boolean>(false);

  readonly save = output<EducationEntry>();
  readonly close = output<void>();

  readonly qualificationOptions = DEFAULT_QUALIFICATIONS;
  readonly currentYear = new Date().getFullYear();
  readonly formError = signal<string | null>(null);

  readonly form = signal<EducationFormState>(this.createEmptyForm());

  constructor() {
    effect(() => {
      const open = this.isOpen();
      const data = this.initialData();
      const editing = this.isEditing();

      if (open) {
        this.formError.set(null);
        if (editing && data) {
          const scoreStr = String(data.percentageOrCgpa || '');
          const isCgpa =
            scoreStr.toLowerCase().includes('cgpa') ||
            (Number(scoreStr) > 0 && Number(scoreStr) <= 10);
          const rawScore = scoreStr.replace(/[^0-9.]/g, '');

          this.form.set({
            id: data.id,
            qualification: data.qualification || '10th Standard / Secondary (SSC)',
            courseName: data.courseName || '',
            boardOrUniversity: data.boardOrUniversity || '',
            institutionName: data.institutionName || '',
            passingYear: data.passingYear || this.currentYear,
            scoreType: isCgpa ? 'CGPA' : 'PERCENTAGE',
            scoreValue: rawScore,
            specialization: data.specialization || '',
            rollNumber: data.rollNumber || '',
            certificateAssetId: data.certificateAssetId,
            certificateFileName: data.certificateFileName,
          });
        } else {
          this.form.set(this.createEmptyForm());
        }
      }
    });
  }

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

    const qualification = String(current.qualification ?? '').trim();
    const boardOrUniversity = String(current.boardOrUniversity ?? '').trim();
    const courseName = String(current.courseName ?? '').trim();
    const institutionName = String(current.institutionName ?? '').trim();
    const specialization = String(current.specialization ?? '').trim();
    const rollNumber = String(current.rollNumber ?? '').trim();
    const scoreValueStr = String(current.scoreValue ?? '').trim();

    // Validation
    if (!qualification) {
      this.formError.set('Please select or specify a qualification level.');
      return;
    }
    if (!boardOrUniversity) {
      this.formError.set('Board or University name is required.');
      return;
    }
    if (
      !current.passingYear ||
      Number(current.passingYear) < 1950 ||
      Number(current.passingYear) > this.currentYear + 5
    ) {
      this.formError.set(
        `Passing year must be between 1950 and ${this.currentYear + 5}.`
      );
      return;
    }
    if (!scoreValueStr) {
      this.formError.set('Percentage or CGPA score is required.');
      return;
    }

    const numScore = parseFloat(scoreValueStr);
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

    const formattedScore =
      current.scoreType === 'CGPA'
        ? `${numScore.toFixed(2)} CGPA`
        : `${numScore.toFixed(2)}%`;

    const entry: EducationEntry = {
      id: current.id || `edu-${Date.now()}`,
      qualification,
      courseName: courseName || undefined,
      boardOrUniversity,
      institutionName: institutionName || undefined,
      passingYear: Number(current.passingYear),
      percentageOrCgpa: formattedScore,
      specialization: specialization || undefined,
      rollNumber: rollNumber || undefined,
      certificateAssetId: current.certificateAssetId || undefined,
      certificateFileName: current.certificateFileName || undefined,
    };

    this.save.emit(entry);
  }

  onClose(): void {
    this.close.emit();
  }
}
