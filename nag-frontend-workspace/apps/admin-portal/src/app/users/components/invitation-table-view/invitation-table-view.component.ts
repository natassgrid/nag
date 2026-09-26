import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AdminInvitationItem,
  RoleDefinition,
} from '@nag-frontend-workspace/shared-data-access-auth';

@Component({
  selector: 'nag-invitation-table-view',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './invitation-table-view.component.html',
  styleUrl: './invitation-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationTableViewComponent {
  readonly invitations = input<AdminInvitationItem[]>([]);
  readonly roles = input<RoleDefinition[]>([]);
  readonly loading = input<boolean>(false);

  readonly inviteNew = output<void>();
  readonly resendInvite = output<AdminInvitationItem>();
  readonly revokeInvite = output<AdminInvitationItem>();

  getRoleName(roleKey: string): string {
    const r = this.roles().find((item) => item.name === roleKey);
    return r ? r.displayName : roleKey;
  }

  copyToken(token?: string): void {
    if (!token) return;
    navigator.clipboard?.writeText(token);
  }
}
