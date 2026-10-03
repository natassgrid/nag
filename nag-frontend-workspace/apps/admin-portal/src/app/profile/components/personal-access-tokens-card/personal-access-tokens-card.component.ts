import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import {
  PersonalAccessToken,
  CreateTokenPayload,
  CreatedTokenResult,
} from '../../profile.model';

@Component({
  selector: 'nag-personal-access-tokens-card',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './personal-access-tokens-card.component.html',
  styleUrl: './personal-access-tokens-card.component.scss',
})
export class PersonalAccessTokensCardComponent {
  readonly tokens = input<PersonalAccessToken[]>([]);
  readonly isCreating = input<boolean>(false);
  readonly createdTokenSecret = input<CreatedTokenResult | null>(null);

  readonly createToken = output<CreateTokenPayload>();
  readonly revokeToken = output<string>();
  readonly clearCreatedSecret = output<void>();

  // Modal / Form state
  readonly showCreateModal = signal<boolean>(false);
  readonly tokenName = signal<string>('');
  readonly expiresInDays = signal<number>(30);
  readonly ipWhitelist = signal<string>('');
  readonly selectedScopes = signal<string[]>(['READ']);
  readonly copied = signal<boolean>(false);

  readonly availableScopes = [
    { code: 'READ', label: 'Read Only (Query data & status)' },
    { code: 'WRITE', label: 'Write Access (Authoring & updates)' },
    { code: 'ADMIN', label: 'Admin Access (Management & configurations)' },
    { code: 'AUDIT', label: 'Audit Access (Inspect compliance logs)' },
  ];

  toggleScope(scope: string): void {
    const current = this.selectedScopes();
    if (current.includes(scope)) {
      if (current.length > 1) {
        this.selectedScopes.set(current.filter((s) => s !== scope));
      }
    } else {
      this.selectedScopes.set([...current, scope]);
    }
  }

  openCreateModal(): void {
    this.tokenName.set('');
    this.expiresInDays.set(30);
    this.ipWhitelist.set('');
    this.selectedScopes.set(['READ']);
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  submitCreate(): void {
    if (!this.tokenName().trim()) return;
    this.createToken.emit({
      name: this.tokenName().trim(),
      scopes: this.selectedScopes(),
      expiresInDays: Number(this.expiresInDays()),
      ipWhitelist: this.ipWhitelist().trim() || undefined,
    });
    this.showCreateModal.set(false);
  }

  copySecret(secret: string): void {
    navigator.clipboard.writeText(secret);
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 2500);
  }
}
