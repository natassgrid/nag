import {
  Component,
  ChangeDetectionStrategy,
  input,
  signal,
  effect,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import * as QRCode from 'qrcode';

/**
 * Standard scannable QR Code generator component powered by qrcode.
 * Supports OTPAuth URIs (Authenticator 2FA apps), Verification receipts, and Admit cards.
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
  private readonly sanitizer = inject(DomSanitizer);

  readonly data = input.required<string>();
  readonly size = input<number>(160);
  readonly label = input<string>('');

  readonly qrDataUrl = signal<string>('');
  readonly qrSvg = signal<SafeHtml | null>(null);

  constructor() {
    effect(() => {
      const text = this.data();
      const currentSize = this.size() || 160;

      if (!text) {
        this.qrDataUrl.set('');
        this.qrSvg.set(null);
        return;
      }

      // Generate Data URL for image rendering (crisp, high compatibility across devices & scanners)
      QRCode.toDataURL(text, {
        width: currentSize,
        margin: 2,
        errorCorrectionLevel: 'M',
        color: {
          dark: '#0f172a',
          light: '#ffffff',
        },
      })
        .then((url: string) => {
          this.qrDataUrl.set(url);
        })
        .catch((err: unknown) => {
          console.error('Failed to generate QR Data URL:', err);
        });

      // Generate SVG for vector scaling
      QRCode.toString(text, {
        type: 'svg',
        width: currentSize,
        margin: 2,
        errorCorrectionLevel: 'M',
        color: {
          dark: '#0f172a',
          light: '#ffffff',
        },
      })
        .then((svgString: string) => {
          this.qrSvg.set(this.sanitizer.bypassSecurityTrustHtml(svgString));
        })
        .catch((err: unknown) => {
          console.error('Failed to generate QR SVG:', err);
        });
    });
  }
}
