import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthoringSubQuestion } from '../../models/authoring.model';
import { PassageTextBoxComponent } from '../passage-text-box/passage-text-box.component';
import { PassageSubquestionEditorComponent } from '../passage-subquestion-editor/passage-subquestion-editor.component';
import { PassagePreviewViewportComponent } from '../passage-preview-viewport/passage-preview-viewport.component';

@Component({
  selector: 'nag-passage-authoring-panel',
  standalone: true,
  imports: [
    CommonModule,
    PassageTextBoxComponent,
    PassageSubquestionEditorComponent,
    PassagePreviewViewportComponent,
  ],
  templateUrl: './passage-authoring-panel.component.html',
  styleUrl: './passage-authoring-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PassageAuthoringPanelComponent {
  readonly passageTitle = input<string>('');
  readonly passageContent = input<string>('');
  readonly subQuestions = input<AuthoringSubQuestion[]>([]);
  readonly activeSubQuestionIndex = input<number>(0);

  readonly passageTitleChange = output<string>();
  readonly passageContentChange = output<string>();
  readonly subQuestionsChange = output<AuthoringSubQuestion[]>();
  readonly activeSubQuestionIndexChange = output<number>();
  readonly validationError = output<string>();
}
