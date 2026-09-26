import { ChangeDetectionStrategy, Component, model } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { CandidateProfile } from '../../models';

@Component({
  selector: 'nag-contact-details-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './contact-details-panel.component.html',
  styleUrl: './contact-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContactDetailsPanelComponent {
  readonly profile = model.required<CandidateProfile>();
}
