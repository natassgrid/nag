import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { InlineComment } from '../../../../models';

export type CommentTag = 'ACCURACY' | 'METHODOLOGY' | 'SYNTAX_LOGIC' | 'FORMATTING';

@Component({
  selector: 'nag-evaluation-response-viewer',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './evaluation-response-viewer.component.html',
  styleUrl: './evaluation-response-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationResponseViewerComponent {
  readonly responseLines = input<string[]>([]);
  readonly inlineComments = input<InlineComment[]>([]);
  readonly selectedCommentLine = input<number | null>(null);
  readonly selectedCommentTag = input<CommentTag>('ACCURACY');
  readonly newCommentText = input<string>('');

  readonly selectLine = output<number>();
  readonly tagChange = output<CommentTag>();
  readonly commentTextChange = output<string>();
  readonly addComment = output<void>();
  readonly removeComment = output<string>();

  readonly availableTags: readonly CommentTag[] = [
    'ACCURACY',
    'METHODOLOGY',
    'SYNTAX_LOGIC',
    'FORMATTING',
  ];

  getCommentsForLine(lineNum: number): InlineComment[] {
    const list = this.inlineComments() || [];
    return list.filter((c) => c && c.lineNumber === lineNum);
  }

  onLineClick(lineIndex: number): void {
    this.selectLine.emit(lineIndex);
  }

  onTagSelect(tag: CommentTag): void {
    this.tagChange.emit(tag);
  }

  onTextChange(text: string): void {
    this.commentTextChange.emit(text);
  }

  onAddComment(): void {
    this.addComment.emit();
  }

  onRemoveComment(commentId: string): void {
    this.removeComment.emit(commentId);
  }
}
