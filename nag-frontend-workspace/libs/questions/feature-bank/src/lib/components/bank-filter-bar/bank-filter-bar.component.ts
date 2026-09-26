import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SearchInputComponent } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'nag-bank-filter-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    SearchInputComponent,
  ],
  templateUrl: './bank-filter-bar.component.html',
  styleUrl: './bank-filter-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BankFilterBarComponent {
  readonly searchQuery = input<string>('');
  readonly selectedSubject = input<string>('ALL');
  readonly selectedDifficulty = input<string>('ALL');
  readonly selectedStatus = input<string>('ALL');
  readonly pageSize = input<number>(20);
  readonly subjects = input<string[]>([]);
  readonly difficulties = input<string[]>(['ALL', 'EASY', 'MEDIUM', 'HARD']);
  readonly statuses = input<string[]>([
    'ALL',
    'APPROVED',
    'REVIEW',
    'DRAFT',
    'REJECTED',
  ]);
  readonly pageSizes = input<number[]>([10, 20, 50, 100]);
  readonly hasActiveFilters = input<boolean>(false);

  readonly searchChange = output<string>();
  readonly subjectChange = output<string>();
  readonly difficultyChange = output<string>();
  readonly statusChange = output<string>();
  readonly pageSizeChange = output<number>();
  readonly reset = output<void>();
  readonly toggleSemanticSearch = output<void>();

  onPageSizeSelected(sizeStr: string): void {
    const size = parseInt(sizeStr, 10) || 20;
    this.pageSizeChange.emit(size);
  }
}
