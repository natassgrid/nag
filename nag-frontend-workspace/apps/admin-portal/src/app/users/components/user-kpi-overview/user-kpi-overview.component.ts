import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { UserManagementKpiStats } from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-user-kpi-overview',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './user-kpi-overview.component.html',
  styleUrl: './user-kpi-overview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserKpiOverviewComponent {
  readonly stats = input.required<UserManagementKpiStats>();
}
