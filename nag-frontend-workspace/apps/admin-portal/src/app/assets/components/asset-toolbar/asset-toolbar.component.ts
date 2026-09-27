import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-asset-toolbar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './asset-toolbar.component.html',
  styleUrl: './asset-toolbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetToolbarComponent {
  searchQuery = input<string>('');
  selectedType = input<string>('ALL');
  selectedStatus = input<string>('ACTIVE');
  viewMode = input<'grid' | 'table'>('grid');

  searchQueryChange = output<string>();
  selectedTypeChange = output<string>();
  selectedStatusChange = output<string>();
  viewModeChange = output<'grid' | 'table'>();
}
