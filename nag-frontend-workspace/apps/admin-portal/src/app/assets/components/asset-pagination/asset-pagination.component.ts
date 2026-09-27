import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-asset-pagination',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './asset-pagination.component.html',
  styleUrl: './asset-pagination.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPaginationComponent {
  currentPage = input<number>(0);
  pageSize = input<number>(12);
  totalPages = input<number>(0);
  totalElements = input<number>(0);

  pageChange = output<number>();

  fromItem = computed(() => {
    return this.totalElements() === 0 ? 0 : this.currentPage() * this.pageSize() + 1;
  });

  toItem = computed(() => {
    const end = (this.currentPage() + 1) * this.pageSize();
    return end > this.totalElements() ? this.totalElements() : end;
  });
}
