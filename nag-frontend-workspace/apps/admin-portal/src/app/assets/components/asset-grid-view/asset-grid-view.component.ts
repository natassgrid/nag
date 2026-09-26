import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AssetService } from '../../asset.service';
import { AssetResponse } from '../../asset.model';

@Component({
  selector: 'nag-asset-grid-view',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatMenuModule],
  templateUrl: './asset-grid-view.component.html',
  styleUrl: './asset-grid-view.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetGridViewComponent {
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
