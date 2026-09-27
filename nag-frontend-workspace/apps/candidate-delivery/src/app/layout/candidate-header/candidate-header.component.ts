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
import { NavItem, CANDIDATE_NAV_ITEMS } from '../layout.models';

@Component({
  selector: 'nag-candidate-header',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  templateUrl: './candidate-header.component.html',
  styleUrl: './candidate-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateHeaderComponent {
  readonly isDrawerOpen = input<boolean>(false);
  readonly userName = input<string>('Guest');
  readonly isAuthenticated = input<boolean>(false);
  readonly navItems = input<NavItem[]>(CANDIDATE_NAV_ITEMS);

  readonly toggleDrawer = output<void>();
  readonly logout = output<void>();
}
