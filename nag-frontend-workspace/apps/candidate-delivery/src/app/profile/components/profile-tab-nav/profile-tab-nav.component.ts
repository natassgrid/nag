import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ProfileTab, ProfileTabOption } from '../../models';

@Component({
  selector: 'nag-profile-tab-nav',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './profile-tab-nav.component.html',
  styleUrl: './profile-tab-nav.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileTabNavComponent {
  readonly activeTab = input.required<ProfileTab>();
  readonly tabs = input.required<ProfileTabOption[]>();
  readonly tabChange = output<ProfileTab>();

  selectTab(tabId: ProfileTab): void {
    this.tabChange.emit(tabId);
  }
}
