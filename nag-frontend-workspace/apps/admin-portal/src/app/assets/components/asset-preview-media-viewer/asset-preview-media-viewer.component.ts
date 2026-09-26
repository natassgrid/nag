import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AssetResponse } from '../../asset.model';

@Component({
  selector: 'app-asset-preview-media-viewer',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './asset-preview-media-viewer.component.html',
  styleUrl: './asset-preview-media-viewer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPreviewMediaViewerComponent {
  readonly asset = input.required<AssetResponse>();
  readonly blobUrl = input<string | null>(null);
  readonly loading = input<boolean>(false);
  readonly loadError = input<boolean>(false);
  readonly replacing = input<boolean>(false);
  readonly formattedSize = input<string>('');

  readonly retryStream = output<void>();
  readonly fileSelected = output<Event>();
  readonly download = output<void>();
}
