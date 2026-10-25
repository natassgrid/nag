import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PaperUploadRequest } from '../../models';

@Component({
  selector: 'nag-paper-upload-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './paper-upload-dialog.component.html',
  styleUrl: './paper-upload-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperUploadDialogComponent {
  isOpen = input<boolean>(false);
  uploading = input<boolean>(false);
  availableSubjects = input<string[]>([]);

  closeModal = output<void>();
  upload = output<{ file: File; metadata: Partial<PaperUploadRequest> }>();

  selectedFile = signal<File | null>(null);
  dragOver = signal<boolean>(false);

  // Form State
  examinationName = signal<string>('');
  examYear = signal<number>(new Date().getFullYear());
  department = signal<string>('');
  paperCode = signal<string>('');
  bookletSerialNo = signal<string>('');
  subjectName = signal<string>('');
  totalQuestionsExpected = signal<number | null>(null);
  durationMinutes = signal<number | null>(null);
  forceReExtract = signal<boolean>(false);
  pageSelection = signal<string>('');

  readonly isPageSelectionValid = computed(() => {
    const raw = (this.pageSelection() || '').trim();
    if (!raw) return true;
    const parts = raw.split(',').map((p) => p.trim());
    if (parts.length === 0) return true;
    for (const part of parts) {
      if (!part) return false;
      const rangeMatch = part.match(/^(\d+)(?:-(\d+))?$/);
      if (!rangeMatch) return false;
      const start = parseInt(rangeMatch[1], 10);
      if (start <= 0) return false;
      if (rangeMatch[2]) {
        const end = parseInt(rangeMatch[2], 10);
        if (end <= 0 || end < start) return false;
      }
    }
    return true;
  });

  readonly isFormValid = computed(() => {
    const file = this.selectedFile();
    const examName = (this.examinationName() || '').trim();
    const year = this.examYear();
    return (
      !!file &&
      examName.length > 0 &&
      year > 1900 &&
      year < 2100 &&
      this.isPageSelectionValid()
    );
  });

  readonly formattedFileSize = computed(() => {
    const file = this.selectedFile();
    if (!file) return '';
    const bytes = file.size;
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  });

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')) {
        this.selectedFile.set(file);
      }
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')) {
        this.selectedFile.set(file);
      }
    }
  }

  removeFile(): void {
    this.selectedFile.set(null);
  }

  resetForm(): void {
    this.selectedFile.set(null);
    this.examinationName.set('');
    this.examYear.set(new Date().getFullYear());
    this.department.set('');
    this.paperCode.set('');
    this.bookletSerialNo.set('');
    this.subjectName.set('');
    this.totalQuestionsExpected.set(null);
    this.durationMinutes.set(null);
    this.forceReExtract.set(false);
    this.pageSelection.set('');
  }

  handleClose(): void {
    if (!this.uploading()) {
      this.resetForm();
      this.closeModal.emit();
    }
  }

  onSubmit(): void {
    const file = this.selectedFile();
    if (!file || !this.isFormValid()) return;

    const metadata: Partial<PaperUploadRequest> = {
      examinationName: (this.examinationName() || '').trim(),
      examYear: this.examYear(),
      department: (this.department() || '').trim() || undefined,
      paperCode: (this.paperCode() || '').trim() || undefined,
      bookletSerialNo: (this.bookletSerialNo() || '').trim() || undefined,
      subjectName: (this.subjectName() || '').trim() || undefined,
      totalQuestionsExpected: this.totalQuestionsExpected() ?? undefined,
      durationMinutes: this.durationMinutes() ?? undefined,
      forceReExtract: this.forceReExtract(),
      pageSelection: (this.pageSelection() || '').trim() || undefined,
    };

    this.upload.emit({ file, metadata });
  }
}
