import {
  Component,
  ChangeDetectionStrategy,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-asset-metrics-ribbon',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './asset-metrics-ribbon.component.html',
  styleUrl: './asset-metrics-ribbon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetMetricsRibbonComponent {
  totalElements = input<number>(0);
  totalStorageFormatted = input<string>('0 B');
  imageCount = input<number>(0);
  audioCount = input<number>(0);
}
