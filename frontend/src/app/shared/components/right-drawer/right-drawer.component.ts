import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-right-drawer',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  template: `
    <div class="right-drawer-overlay" [class.open]="isOpen" (click)="onBackdropClick($event)">
      <div class="right-drawer-panel" [style.width]="width" (click)="$event.stopPropagation()">
        <div class="right-drawer-header">
          <div class="header-text-container">
            <h3>{{ title }}</h3>
            <p class="drawer-subtitle" *ngIf="subtitle">{{ subtitle }}</p>
          </div>
          <button mat-icon-button (click)="close.emit()" aria-label="Close drawer">
            <mat-icon>close</mat-icon>
          </button>
        </div>
        <div class="right-drawer-body">
          <ng-content select="[drawer-body], form, div"></ng-content>
        </div>
        <div class="right-drawer-footer" *ngIf="showFooter">
          <ng-content select="[drawer-footer]"></ng-content>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .right-drawer-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.32);
      z-index: 1000;
      display: flex;
      justify-content: flex-end;
      opacity: 0;
      visibility: hidden;
      transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1), visibility 0.25s;
    }
    .right-drawer-overlay.open {
      opacity: 1;
      visibility: visible;
    }
    .right-drawer-panel {
      width: 480px;
      max-width: 90vw;
      height: 100%;
      background: #ffffff;
      box-shadow: -4px 0 24px rgba(0, 0, 0, 0.15);
      display: flex;
      flex-direction: column;
      transform: translateX(100%);
      transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .right-drawer-overlay.open .right-drawer-panel {
      transform: translateX(0);
    }
    .right-drawer-header {
      min-height: 56px;
      padding: 12px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #e0e0e0;
      background: #ffffff;
    }
    .header-text-container {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .right-drawer-header h3 {
      margin: 0;
      font-size: 17px;
      font-weight: 600;
      color: #212121;
      line-height: 1.2;
    }
    .right-drawer-header .drawer-subtitle {
      margin: 0;
      font-size: 12px;
      color: #666666;
      line-height: 1.3;
    }
    .right-drawer-body {
      flex: 1;
      overflow-y: auto;
      padding: 20px;
    }
    .right-drawer-footer {
      padding: 16px 20px;
      border-top: 1px solid #e0e0e0;
      background: #ffffff;
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  `]
})
export class RightDrawerComponent {
  @Input() isOpen = false;
  @Input() title = '';
  @Input() subtitle = '';
  @Input() width = '480px';
  @Input() showFooter = true;
  @Output() close = new EventEmitter<void>();

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('right-drawer-overlay')) {
      this.close.emit();
    }
  }
}
