import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-passage-text-box',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './passage-text-box.component.html',
  styleUrl: './passage-text-box.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PassageTextBoxComponent {
  readonly passageTitle = input<string>('');
  readonly passageContent = input<string>('');

  readonly passageTitleChange = output<string>();
  readonly passageContentChange = output<string>();
}
