import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { StatusBadgeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { EnrolledExam } from '../../models';

@Component({
  selector: 'app-enrolled-assessment-card',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    StatusBadgeComponent,
  ],
  templateUrl: './enrolled-assessment-card.component.html',
  styleUrl: './enrolled-assessment-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnrolledAssessmentCardComponent {
  readonly exam = input.required<EnrolledExam>();

  readonly openAdmitCard = output<EnrolledExam>();
}
