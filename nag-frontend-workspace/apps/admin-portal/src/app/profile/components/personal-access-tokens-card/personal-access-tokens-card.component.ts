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

  readonly fieldErrors = signal<Record<string, string>>({});
  readonly hasSubmitted = signal<boolean>(false);

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
    this.fieldErrors.set({});
    this.hasSubmitted.set(false);
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  onTokenNameChange(val: string): void {
    this.tokenName.set(val);
    if (this.hasSubmitted() || this.fieldErrors()['tokenName']) {
      this.validateField('tokenName');
    }
  }

  onIpWhitelistChange(val: string): void {
    this.ipWhitelist.set(val);
    if (this.hasSubmitted() || this.fieldErrors()['ipWhitelist']) {
      this.validateField('ipWhitelist');
    }
  }

  validateField(field: 'tokenName' | 'ipWhitelist'): void {
    const errors = { ...this.fieldErrors() };
    if (field === 'tokenName') {
      if (!this.tokenName().trim()) {
        errors['tokenName'] = 'Token description / name is required.';
      } else {
        delete errors['tokenName'];
      }
    } else if (field === 'ipWhitelist') {
      const val = this.ipWhitelist().trim();
      if (val) {
        const entries = val.split(',').map((s) => s.trim()).filter(Boolean);
        const valid = entries.length > 0 && entries.every((entry) => this.isValidIpOrCidr(entry));
        if (!valid) {
          errors['ipWhitelist'] = 'Enter valid IP addresses or CIDR notations (e.g. 192.168.1.1, 10.0.0.0/24).';
        } else {
          delete errors['ipWhitelist'];
        }
      } else {
        delete errors['ipWhitelist'];
      }
    }
    this.fieldErrors.set(errors);
  }

  validateAll(): boolean {
    const errors: Record<string, string> = {};
    if (!this.tokenName().trim()) {
      errors['tokenName'] = 'Token description / name is required.';
    }
    const val = this.ipWhitelist().trim();
    if (val) {
      const entries = val.split(',').map((s) => s.trim()).filter(Boolean);
      const valid = entries.length > 0 && entries.every((entry) => this.isValidIpOrCidr(entry));
      if (!valid) {
        errors['ipWhitelist'] = 'Enter valid IP addresses or CIDR notations (e.g. 192.168.1.1, 10.0.0.0/24).';
      }
    }
    this.fieldErrors.set(errors);
    return Object.keys(errors).length === 0;
  }

  private isValidIpOrCidr(ip: string): boolean {
    const ipv4CidrRegex = /^(\d{1,3}\.){3}\d{1,3}(\/([0-9]|[1-2][0-9]|3[0-2]))?$/;
    const ipv6Regex = /^([0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}(\/\d{1,3})?$|^::1$/;
    if (!ipv4CidrRegex.test(ip) && !ipv6Regex.test(ip)) {
      return false;
    }
    if (ipv4CidrRegex.test(ip)) {
      const baseIp = ip.split('/')[0];
      const parts = baseIp.split('.').map(Number);
      if (parts.some((p) => p < 0 || p > 255)) return false;
    }
    return true;
  }

  submitCreate(): void {
    this.hasSubmitted.set(true);
    if (!this.validateAll()) return;
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
