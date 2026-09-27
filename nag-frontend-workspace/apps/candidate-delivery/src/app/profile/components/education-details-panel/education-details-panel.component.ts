import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EducationEntry } from '../../models';
import {
  EducationDeleteDialogComponent,
  EducationEmptyStateComponent,
  EducationFormModalComponent,
  EducationItemCardComponent,
} from './components';

@Component({
  selector: 'nag-education-details-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    EducationItemCardComponent,
    EducationEmptyStateComponent,
    EducationFormModalComponent,
    EducationDeleteDialogComponent,
  ],
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
  readonly activeEntry = signal<EducationEntry | null>(null);

  readonly isDeleteConfirmOpen = signal<boolean>(false);
  readonly entryToDelete = signal<EducationEntry | null>(null);

  openAddModal(): void {
    this.isEditing.set(false);
    this.activeEntry.set(null);
    this.isModalOpen.set(true);
  }

  openEditModal(entry: EducationEntry): void {
    this.isEditing.set(true);
    this.activeEntry.set(entry);
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.activeEntry.set(null);
  }

  onSave(entry: EducationEntry): void {
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
