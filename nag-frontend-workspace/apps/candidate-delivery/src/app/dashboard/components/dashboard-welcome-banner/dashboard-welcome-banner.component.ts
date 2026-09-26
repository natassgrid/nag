import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-dashboard-welcome-banner',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  templateUrl: './dashboard-welcome-banner.component.html',
  styleUrl: './dashboard-welcome-banner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardWelcomeBannerComponent {
  readonly userName = input<string>('Candidate');
}
