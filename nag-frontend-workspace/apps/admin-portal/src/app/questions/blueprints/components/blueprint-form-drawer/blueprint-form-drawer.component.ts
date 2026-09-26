import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  BlueprintTemplateResponse,
  Subject,
  SubjectHierarchy,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-blueprint-form-drawer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatIconModule, MatButtonModule],
  templateUrl: './blueprint-form-drawer.component.html',
  styleUrl: './blueprint-form-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BlueprintFormDrawerComponent {
  isOpen = input<boolean>(false);
  editingTemplate = input<BlueprintTemplateResponse | null>(null);
  saving = input<boolean>(false);
  form = input.required<FormGroup>();
  taxonomySubjects = input<Subject[]>([]);
  taxonomyHierarchy = input<SubjectHierarchy[]>([]);

  closeDrawer = output<void>();
  save = output<void>();
  addRule = output<void>();
  removeRule = output<number>();
  ruleSubjectChanged = output<number>();

  get rulesArray(): FormArray {
    return this.form().get('rules') as FormArray;
  }

  getTopicsForSubject(subjectName: string): string[] {
    const node = this.taxonomyHierarchy().find(
      (h) => h.name.toLowerCase() === (subjectName || '').toLowerCase()
    );
    if (node && node.topics && node.topics.length > 0) {
      return node.topics.map((t) => t.name);
    }
    return [];
  }

  onSubjectChange(index: number): void {
    this.ruleSubjectChanged.emit(index);
  }
}
