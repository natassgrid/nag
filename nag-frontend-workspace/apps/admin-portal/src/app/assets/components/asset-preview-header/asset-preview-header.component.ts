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
import { MatDialogModule } from '@angular/material/dialog';
import { AssetResponse } from '../../asset.model';

@Component({
  selector: 'app-asset-preview-header',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './asset-preview-header.component.html',
  styleUrl: './asset-preview-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPreviewHeaderComponent {
  readonly asset = input.required<AssetResponse>();
  readonly replacing = input<boolean>(false);

  readonly fileSelected = output<Event>();
  readonly copyUrl = output<void>();
  readonly download = output<void>();
}
