import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';
import { AssetResponse } from '../../asset.model';

@Component({
  selector: 'app-asset-preview-metadata-ribbon',
  standalone: true,
  imports: [CommonModule, MatChipsModule],
  templateUrl: './asset-preview-metadata-ribbon.component.html',
  styleUrl: './asset-preview-metadata-ribbon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPreviewMetadataRibbonComponent {
  readonly asset = input.required<AssetResponse>();
  readonly formattedSize = input<string>('');
  readonly formattedDuration = input<string>('');
}
