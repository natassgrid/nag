import {
  Component,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  NotificationService,
  PageHeaderComponent,
} from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
  ],
  templateUrl: './admin-reports.component.html',
  styleUrl: './admin-reports.component.scss',
})
export class AdminReportsComponent {
  private readonly notificationService = inject(NotificationService);

  selectedExam = 'NES-2026-S1';
  reportType = 'RESULTS';
  exporting = signal<boolean>(false);

  exportData(format: 'csv' | 'pdf'): void {
    this.exporting.set(true);
    setTimeout(() => {
      this.exporting.set(false);
      this.notificationService.success(
        'Report Exported',
        `Exported ${this.reportType} for ${this.selectedExam} as signed .${format.toUpperCase()} package.`
      );
    }, 800);
  }
}
