import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, FormArray, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  BlueprintTemplateResponse,
  Subject,
  SubjectHierarchy,
  TaxonomyStore,
  TaxonomyTopicNode,
  TaxonomySubtopicNode,
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
  private readonly taxonomyStore = inject(TaxonomyStore);

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

  readonly allSubjects = this.taxonomyStore.subjects;

  getTopicsForSubjectId(rawSubjectId: any): TaxonomyTopicNode[] {
    if (rawSubjectId == null || rawSubjectId === '') return [];
    return this.taxonomyStore.getTopicsForSubject(Number(rawSubjectId));
  }

  getSubtopicsForTopicId(rawTopicId: any): TaxonomySubtopicNode[] {
    if (rawTopicId == null || rawTopicId === '') return [];
    return this.taxonomyStore.getSubtopicsForTopic(Number(rawTopicId));
  }

  onSubjectChange(index: number, rawId: any): void {
    const numId = Number(rawId);
    const ruleGroup = this.rulesArray.at(index) as FormGroup;
    if (!ruleGroup) return;

    const subName = this.taxonomyStore.subjectName(numId);
    ruleGroup.patchValue({
      subjectId: numId,
      subject: subName,
      topicId: null,
      topic: '',
      subtopicId: null,
      subtopic: '',
    });
    this.ruleSubjectChanged.emit(index);
  }

  onTopicChange(index: number, rawId: any): void {
    const ruleGroup = this.rulesArray.at(index) as FormGroup;
    if (!ruleGroup) return;

    if (!rawId || rawId === 'null') {
      ruleGroup.patchValue({
        topicId: null,
        topic: '',
        subtopicId: null,
        subtopic: '',
      });
      return;
    }

    const numId = Number(rawId);
    const topName = this.taxonomyStore.topicName(numId);
    ruleGroup.patchValue({
      topicId: numId,
      topic: topName,
      subtopicId: null,
      subtopic: '',
    });
  }

  onSubtopicChange(index: number, rawId: any): void {
    const ruleGroup = this.rulesArray.at(index) as FormGroup;
    if (!ruleGroup) return;

    if (!rawId || rawId === 'null') {
      ruleGroup.patchValue({
        subtopicId: null,
        subtopic: '',
      });
      return;
    }

    const numId = Number(rawId);
    const subtopName = this.taxonomyStore.subtopicName(numId);
    ruleGroup.patchValue({
      subtopicId: numId,
      subtopic: subtopName,
    });
  }
}
