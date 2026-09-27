import { ChangeDetectionStrategy, Component, model } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { CandidateProfile } from '../../models';

@Component({
  selector: 'nag-personal-details-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './personal-details-panel.component.html',
  styleUrl: './personal-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonalDetailsPanelComponent {
  readonly profile = model.required<CandidateProfile>();
}
