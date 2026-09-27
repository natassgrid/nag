import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CentreResponse } from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-centre-table-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './centre-table-list.component.html',
  styleUrl: './centre-table-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreTableListComponent {
  centres = input<CentreResponse[]>([]);
  loading = input<boolean>(false);
  searchQuery = input<string>('');
  stateFilter = input<string>('ALL');

  registerFirst = output<void>();
  deactivate = output<CentreResponse>();
}
