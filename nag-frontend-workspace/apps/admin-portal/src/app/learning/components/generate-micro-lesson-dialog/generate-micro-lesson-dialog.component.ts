import { Component, ChangeDetectionStrategy, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SubjectTopicService, SubjectHierarchy } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { GenerateMicroLessonRequest, MicroLessonReviewItem } from '../../models/micro-lesson.model';
import { BatchGenerateMicroLessonDialogComponent } from '../batch-generate-dialog/batch-generate-dialog.component';

@Component({
  selector: 'app-generate-micro-lesson-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './generate-micro-lesson-dialog.component.html',
  styleUrls: ['./generate-micro-lesson-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerateMicroLessonDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<GenerateMicroLessonDialogComponent>);
  private readonly dialog = inject(MatDialog);
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly subjectTopicService = inject(SubjectTopicService);

  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly isLoadingTaxonomy = signal<boolean>(false);

  readonly selectedSubjectId = signal<number | null>(null);
  readonly selectedSubjectName = signal<string>('');
  readonly isCustomSubject = signal<boolean>(false);

  readonly selectedTopicId = signal<number | null>(null);
  readonly selectedTopicName = signal<string>('');
  readonly isCustomTopic = signal<boolean>(false);

  readonly selectedSubtopicId = signal<number | null>(null);
  readonly selectedSubtopicName = signal<string>('');
  readonly isCustomSubtopic = signal<boolean>(false);

  readonly targetExam = signal<string>('JEE_MAIN');
  readonly referenceText = signal<string>('');
  readonly mode = signal<'SINGLE' | 'BATCH'>('SINGLE');

  readonly isGenerating = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly examOptions = [
    { label: 'JEE Main & Advanced', value: 'JEE_MAIN' },
    { label: 'NEET-UG (Medical)', value: 'NEET' },
    { label: 'GATE Engineering', value: 'GATE' },
    { label: 'SSC CGL', value: 'SSC_CGL' },
    { label: 'UPSC Civil Services', value: 'UPSC_CSAT' },
  ];

  readonly availableTopics = computed(() => {
    const subjectId = this.selectedSubjectId();
    if (!subjectId) return [];
    const subj = this.hierarchy().find((s) => s.id === subjectId);
    return subj ? subj.topics : [];
  });

  readonly availableSubtopics = computed(() => {
    const topicId = this.selectedTopicId();
    if (!topicId) return [];
    const topic = this.availableTopics().find((t) => t.id === topicId);
    return topic ? topic.subtopics : [];
  });

  ngOnInit(): void {
    this.loadTaxonomy();
  }

  loadTaxonomy(): void {
    this.isLoadingTaxonomy.set(true);
    this.subjectTopicService.getHierarchy().subscribe({
      next: (tree) => {
        const list = tree || [];
        this.hierarchy.set(list);
        this.isLoadingTaxonomy.set(false);

        if (list.length > 0) {
          const firstSubject = list[0];
          this.selectedSubjectId.set(firstSubject.id);
          this.selectedSubjectName.set(firstSubject.name);
          this.isCustomSubject.set(false);

          if (firstSubject.topics && firstSubject.topics.length > 0) {
            const firstTopic = firstSubject.topics[0];
            this.selectedTopicId.set(firstTopic.id);
            this.selectedTopicName.set(firstTopic.name);
            this.isCustomTopic.set(false);

            if (firstTopic.subtopics && firstTopic.subtopics.length > 0) {
              const firstSubtopic = firstTopic.subtopics[0];
              this.selectedSubtopicId.set(firstSubtopic.id);
              this.selectedSubtopicName.set(firstSubtopic.name);
              this.isCustomSubtopic.set(false);
            } else {
              this.isCustomSubtopic.set(true);
            }
          } else {
            this.isCustomTopic.set(true);
            this.isCustomSubtopic.set(true);
          }
        } else {
          this.isCustomSubject.set(true);
          this.isCustomTopic.set(true);
          this.isCustomSubtopic.set(true);
        }
      },
      error: (err) => {
        console.error('Failed to load subject taxonomy for micro-lesson dialog', err);
        this.isLoadingTaxonomy.set(false);
        this.isCustomSubject.set(true);
        this.isCustomTopic.set(true);
        this.isCustomSubtopic.set(true);
      },
    });
  }

  onSubjectChange(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomSubject.set(true);
      this.selectedSubjectId.set(null);
      this.selectedSubjectName.set('');
      this.isCustomTopic.set(true);
      this.selectedTopicId.set(null);
      this.selectedTopicName.set('');
      this.isCustomSubtopic.set(true);
      this.selectedSubtopicId.set(null);
      this.selectedSubtopicName.set('');
      return;
    }

    const id = Number(val);
    const subj = this.hierarchy().find((s) => s.id === id);
    if (subj) {
      this.isCustomSubject.set(false);
      this.selectedSubjectId.set(subj.id);
      this.selectedSubjectName.set(subj.name);

      if (subj.topics && subj.topics.length > 0) {
        const firstTopic = subj.topics[0];
        this.isCustomTopic.set(false);
        this.selectedTopicId.set(firstTopic.id);
        this.selectedTopicName.set(firstTopic.name);

        if (firstTopic.subtopics && firstTopic.subtopics.length > 0) {
          const firstSub = firstTopic.subtopics[0];
          this.isCustomSubtopic.set(false);
          this.selectedSubtopicId.set(firstSub.id);
          this.selectedSubtopicName.set(firstSub.name);
        } else {
          this.isCustomSubtopic.set(true);
          this.selectedSubtopicId.set(null);
          this.selectedSubtopicName.set('');
        }
      } else {
        this.isCustomTopic.set(true);
        this.selectedTopicId.set(null);
        this.selectedTopicName.set('');
        this.isCustomSubtopic.set(true);
        this.selectedSubtopicId.set(null);
        this.selectedSubtopicName.set('');
      }
    }
  }

  onTopicChange(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomTopic.set(true);
      this.selectedTopicId.set(null);
      this.selectedTopicName.set('');
      this.isCustomSubtopic.set(true);
      this.selectedSubtopicId.set(null);
      this.selectedSubtopicName.set('');
      return;
    }

    const id = Number(val);
    const topic = this.availableTopics().find((t) => t.id === id);
    if (topic) {
      this.isCustomTopic.set(false);
      this.selectedTopicId.set(topic.id);
      this.selectedTopicName.set(topic.name);

      if (topic.subtopics && topic.subtopics.length > 0) {
        const firstSub = topic.subtopics[0];
        this.isCustomSubtopic.set(false);
        this.selectedSubtopicId.set(firstSub.id);
        this.selectedSubtopicName.set(firstSub.name);
      } else {
        this.isCustomSubtopic.set(true);
        this.selectedSubtopicId.set(null);
        this.selectedSubtopicName.set('');
      }
    }
  }

  onSubtopicChange(val: string): void {
    if (val === '__CUSTOM__') {
      this.isCustomSubtopic.set(true);
      this.selectedSubtopicId.set(null);
      this.selectedSubtopicName.set('');
      return;
    }

    const id = Number(val);
    const sub = this.availableSubtopics().find((s) => s.id === id);
    if (sub) {
      this.isCustomSubtopic.set(false);
      this.selectedSubtopicId.set(sub.id);
      this.selectedSubtopicName.set(sub.name);
    }
  }

  switchToBatch(): void {
    this.dialogRef.close(null);
    this.dialog.open(BatchGenerateMicroLessonDialogComponent, {
      width: '560px',
      panelClass: 'nag-dialog-clean',
    });
  }

  onGenerate(): void {
    const subtopic = this.selectedSubtopicName().trim();
    if (!subtopic) {
      this.errorMessage.set('Subtopic name is required.');
      return;
    }

    this.isGenerating.set(true);
    this.errorMessage.set(null);

    const req: GenerateMicroLessonRequest = {
      subjectId: this.selectedSubjectId() ?? undefined,
      subject: this.selectedSubjectName().trim() || 'General',
      topicId: this.selectedTopicId() ?? undefined,
      topic: this.selectedTopicName().trim() || 'General',
      subtopicId: this.selectedSubtopicId() ?? undefined,
      subtopic,
      targetExam: this.targetExam(),
      referenceText: this.referenceText().trim() || undefined,
    };

    this.microLessonService.generateSingle(req).subscribe({
      next: (item: MicroLessonReviewItem) => {
        this.isGenerating.set(false);
        this.dialogRef.close(item);
      },
      error: (err) => {
        this.isGenerating.set(false);
        this.errorMessage.set(
          err?.error?.message || 'AI micro-lesson generation failed. Please try again.'
        );
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(null);
  }
}
