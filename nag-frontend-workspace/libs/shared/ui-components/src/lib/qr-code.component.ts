import {
  Component,
  ChangeDetectionStrategy,
  input,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Simple SVG-based QR Code generator component for Admit Cards & Verification Receipts.
 */
@Component({
  selector: 'nag-qr-code',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './qr-code.component.html',
  styleUrl: './qr-code.component.scss',
})
export class QrCodeComponent {
  data = input.required<string>();
  size = input<number>(120);
  label = input<string>('');

  dataMatrix = computed(() => {
    const text = this.data() || 'NAG';
    const dots: Array<{ id: number; x: number; y: number; w: number; h: number }> = [];

    // Simple pseudo-random hash generator based on string
    let hash = 0;
    for (let i = 0; i < text.length; i++) {
      hash = (hash << 5) - hash + text.charCodeAt(i);
      hash |= 0;
    }

    const gridSize = 14;
    const step = 4;
    const offset = 22;

    for (let row = 0; row < gridSize; row++) {
      for (let col = 0; col < gridSize; col++) {
        // Skip corner target boxes
        if (row < 4 && col < 4) continue;
        if (row < 4 && col > 9) continue;
        if (row > 9 && col < 4) continue;

        const val = Math.abs(Math.sin((hash * (row + 1) * 31 + (col + 1) * 17))) * 100;
        if (val > 48) {
          dots.push({
            id: row * gridSize + col,
            x: offset + col * step,
            y: offset + row * step,
            w: 3.2,
            h: 3.2,
          });
        }
      }
    }

    return dots;
  });
}
