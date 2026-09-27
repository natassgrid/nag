import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  input,
  output,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NavItem, CANDIDATE_NAV_ITEMS } from '../layout.models';

@Component({
  selector: 'nag-candidate-drawer',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  templateUrl: './candidate-drawer.component.html',
  styleUrl: './candidate-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateDrawerComponent {
  readonly isOpen = input<boolean>(false);
  readonly userName = input<string>('Guest');
  readonly isAuthenticated = input<boolean>(false);
  readonly navItems = input<NavItem[]>(CANDIDATE_NAV_ITEMS);

  readonly closeDrawer = output<void>();
  readonly logout = output<void>();

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.isOpen()) {
      this.closeDrawer.emit();
    }
  }

  onBackdropClick(): void {
    this.closeDrawer.emit();
  }

  onItemClick(): void {
    this.closeDrawer.emit();
  }

  onLogoutClick(): void {
    this.closeDrawer.emit();
    this.logout.emit();
  }

  getUserInitials(): string {
    const name = this.userName() || 'G';
    const parts = name.trim().split(/[\s_.-]+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }
}
