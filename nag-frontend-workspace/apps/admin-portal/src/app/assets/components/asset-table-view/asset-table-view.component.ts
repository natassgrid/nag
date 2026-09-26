import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  inject,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AssetService } from '../../asset.service';
import { AssetResponse } from '../../asset.model';

@Component({
  selector: 'nag-asset-table-view',
  standalone: true,
  imports: [CommonModule, DatePipe, MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './asset-table-view.component.html',
  styleUrl: './asset-table-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetTableViewComponent {
  readonly assetService = inject(AssetService);

  assets = input<AssetResponse[]>([]);
  failedImages = input<Set<string>>(new Set<string>());

  preview = output<AssetResponse>();
  replaceBinary = output<AssetResponse>();
  editMetadata = output<AssetResponse>();
  copyCdnUrl = output<AssetResponse>();
  archive = output<AssetResponse>();
  restore = output<AssetResponse>();
  delete = output<AssetResponse>();
  imageError = output<string>();

  isImageFailed(id: string): boolean {
    return this.failedImages().has(id);
  }
}
