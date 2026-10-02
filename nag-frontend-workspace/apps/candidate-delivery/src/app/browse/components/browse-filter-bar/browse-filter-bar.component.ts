import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { SearchInputComponent } from '@nag-frontend-workspace/shared-ui-components';
import { CatalogFilterState } from '../../models';

@Component({
  selector: 'nag-browse-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, SearchInputComponent],
  templateUrl: './browse-filter-bar.component.html',
  styleUrl: './browse-filter-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrowseFilterBarComponent {
  readonly filters = input.required<CatalogFilterState>();
  readonly totalMatches = input<number>(0);

  readonly searchChange = output<string>();
  readonly categoryChange = output<string>();
  readonly statusChange = output<string>();
  readonly sortChange = output<'DATE_ASC' | 'FEE_ASC' | 'FEE_DESC' | 'TITLE_ASC'>();
  readonly resetFilters = output<void>();

  readonly categories = [
    { id: 'ALL', label: 'All Categories', icon: 'apps' },
    { id: 'ENGINEERING', label: 'Engineering & Tech', icon: 'code' },
    { id: 'CIVIL_SERVICES', label: 'Civil & Public Admin', icon: 'account_balance' },
    { id: 'BANKING', label: 'Banking & Finance', icon: 'savings' },
    { id: 'DEFENSE', label: 'Defense & Police', icon: 'military_tech' },
    { id: 'MEDICAL', label: 'Medical & Healthcare', icon: 'medical_services' },
  ];

  readonly statuses = [
    { id: 'ALL', label: 'All Statuses' },
    { id: 'OPEN', label: 'Open Registration' },
    { id: 'CLOSING_SOON', label: 'Closing Soon' },
    { id: 'APPLIED', label: 'My Enrolled' },
    { id: 'PRACTICE', label: 'Practice / Mock Tests' },
  ];
}
