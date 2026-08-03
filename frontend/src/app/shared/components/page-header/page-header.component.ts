import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="page-header">
      <div class="page-header__left">
        <h1 class="page-header__title">
          <mat-icon *ngIf="icon" class="page-header__icon">{{ icon }}</mat-icon>
          {{ title }}
        </h1>
        <p *ngIf="subtitle" class="page-header__subtitle">{{ subtitle }}</p>
      </div>
      <div class="page-header__actions">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      flex-wrap: wrap;
    }

    .page-header__left {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .page-header__title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 10px;
      color: #4A4A4A;
    }

    .page-header__icon {
      font-size: 28px;
      height: 28px;
      width: 28px;
      color: #536DFE;
    }

    .page-header__subtitle {
      margin: 0;
      font-size: 14px;
      color: #757575;
    }

    .page-header__actions {
      display: flex;
      gap: 8px;
      align-items: center;
      flex-shrink: 0;
    }
  `]
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() icon = '';
}
