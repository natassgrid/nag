import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { CandidateProfile } from '../../models';

@Component({
  selector: 'nag-kyc-documents-panel',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './kyc-documents-panel.component.html',
  styleUrl: './kyc-documents-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KycDocumentsPanelComponent {
  readonly profile = input.required<CandidateProfile>();
}
