import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subtopic, Topic } from '@nag-frontend-workspace/questions-data-access';
import { TaxonomySelectorComponent } from '@nag-frontend-workspace/questions-feature-authoring';
import {
  AUTHORING_STATUS_OPTIONS,
  DEFAULT_EXAM_OPTIONS,
  DIFFICULTY_LEVEL_OPTIONS,
  LESSON_TYPE_OPTIONS,
  TargetExamOption,
} from '../../models';

@Component({
  selector: 'app-lesson-metadata-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, TaxonomySelectorComponent],
  templateUrl: './lesson-metadata-bar.component.html',
  styleUrl: './lesson-metadata-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LessonMetadataBarComponent {
  readonly subjects = input<Subject[]>([]);
  readonly topics = input<Topic[]>([]);
  readonly subtopics = input<Subtopic[]>([]);
  readonly selectedSubjectId = input<number | null>(null);
  readonly selectedTopicId = input<number | null>(null);
  readonly selectedSubtopicId = input<number | null>(null);
  readonly customSubtopicName = input<string>('');
  readonly targetExam = input<string>('JEE_MAIN');
  readonly lessonType = input<string>('CONCEPT');
  readonly difficultyLevel = input<string>('CORE');
  readonly sequenceNo = input<number>(1);
  readonly estimatedMinutes = input<number>(5);
  readonly status = input<string>('APPROVED');
  readonly examOptions = input<TargetExamOption[]>(DEFAULT_EXAM_OPTIONS);

  readonly statusOptions = AUTHORING_STATUS_OPTIONS;
  readonly lessonTypeOptions = LESSON_TYPE_OPTIONS;
  readonly difficultyOptions = DIFFICULTY_LEVEL_OPTIONS;

  readonly subjectChange = output<number>();
  readonly topicChange = output<number>();
  readonly subtopicChange = output<number | null>();
  readonly createSubject = output<string>();
  readonly createTopic = output<string>();
  readonly customSubtopicNameChange = output<string>();
  readonly targetExamChange = output<string>();
  readonly lessonTypeChange = output<string>();
  readonly difficultyLevelChange = output<string>();
  readonly sequenceNoChange = output<number>();
  readonly estimatedMinutesChange = output<number>();
  readonly statusChange = output<string>();
}
