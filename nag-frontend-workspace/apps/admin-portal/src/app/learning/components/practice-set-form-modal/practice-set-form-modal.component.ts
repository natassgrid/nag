import { Component, ChangeDetectionStrategy, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { PracticeSetService } from '../../services';
import { PracticeSet, UpdatePracticeSetRequest } from '../../models';

export interface PracticeSetFormModalData {
  set: PracticeSet | null;
  mode: 'create' | 'edit';
}

@Component({
  selector: 'app-practice-set-form-modal',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './practice-set-form-modal.component.html',
  styleUrls: ['./practice-set-form-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetFormModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PracticeSetFormModalComponent>);
  public readonly data = inject<PracticeSetFormModalData>(MAT_DIALOG_DATA);
  private readonly practiceSetService = inject(PracticeSetService);

  form!: FormGroup;

  ngOnInit() {
    this.form = this.fb.group({
      name: [this.data.set?.name || '', [Validators.required, Validators.maxLength(100)]],
      description: [this.data.set?.description || '', [Validators.maxLength(500)]],
      durationMinutes: [this.data.set?.durationMinutes || 30, [Validators.required, Validators.min(1)]],
      subjectSlug: [this.data.set?.subjectSlug || ''],
    });
  }

  onSubmit() {
    if (this.form.invalid) return;

    const formValue = this.form.value;
    
    if (this.data.mode === 'create') {
      const createReq: any = { // Note: Create req needs questionIds, we simplify here
        ...formValue,
        questionIds: []
      };
      this.practiceSetService.create(createReq).subscribe({
        next: (res) => this.dialogRef.close(res),
        error: (err) => console.error('Failed to create', err)
      });
    } else {
      const updateReq: UpdatePracticeSetRequest = {
        name: formValue.name,
        description: formValue.description,
        durationMinutes: formValue.durationMinutes,
        totalQuestions: this.data.set?.totalQuestions || 0
      };
      
      this.practiceSetService.update(this.data.set!.id, updateReq).subscribe({
        next: (res) => this.dialogRef.close(res),
        error: (err) => console.error('Failed to update', err)
      });
    }
  }

  onCancel() {
    this.dialogRef.close();
  }
}
