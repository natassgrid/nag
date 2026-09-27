import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-centre-filter-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './centre-filter-bar.component.html',
  styleUrl: './centre-filter-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreFilterBarComponent {
  searchQuery = input<string>('');
  stateFilter = input<string>('ALL');
  statusFilter = input<string>('ALL');
  uniqueStates = input<string[]>([]);

  searchQueryChange = output<string>();
  stateFilterChange = output<string>();
  statusFilterChange = output<string>();
}
