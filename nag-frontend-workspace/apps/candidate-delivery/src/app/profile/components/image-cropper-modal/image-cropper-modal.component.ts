import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSliderModule } from '@angular/material/slider';

export type CropMode = 'photo' | 'signature' | 'free';

@Component({
  selector: 'nag-image-cropper-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatSliderModule],
  templateUrl: './image-cropper-modal.component.html',
  styleUrl: './image-cropper-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageCropperModalComponent implements OnInit {
  @ViewChild('previewCanvas', { static: false }) previewCanvas?: ElementRef<HTMLCanvasElement>;

  readonly imageSrc = input.required<string>();
  readonly mode = input<CropMode>('photo');
  readonly title = input<string>('Crop & Adjust Image');

  readonly cropped = output<string>();
  readonly cancelled = output<void>();

  readonly zoom = signal<number>(1);
  readonly rotation = signal<number>(0);
  readonly panX = signal<number>(0);
  readonly panY = signal<number>(0);
  readonly isDragging = signal<boolean>(false);

  private dragStart = { x: 0, y: 0 };
  private imageElement: HTMLImageElement = new Image();
  readonly imageLoaded = signal<boolean>(false);

  ngOnInit(): void {
    this.loadImage();
  }

  private loadImage(): void {
    this.imageElement = new Image();
    this.imageElement.crossOrigin = 'anonymous';
    this.imageElement.onload = () => {
      this.imageLoaded.set(true);
      this.render();
    };
    this.imageElement.src = this.imageSrc();
  }

  onZoomChange(val: number): void {
    this.zoom.set(val);
    this.render();
  }

  rotate(deltaDeg: number): void {
    const next = (this.rotation() + deltaDeg) % 360;
    this.rotation.set(next);
    this.render();
  }

  resetAdjustments(): void {
    this.zoom.set(1);
    this.rotation.set(0);
    this.panX.set(0);
    this.panY.set(0);
    this.render();
  }

  startDrag(e: MouseEvent | TouchEvent): void {
    this.isDragging.set(true);
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    this.dragStart = { x: clientX - this.panX(), y: clientY - this.panY() };
  }

  onDrag(e: MouseEvent | TouchEvent): void {
    if (!this.isDragging()) return;
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    this.panX.set(clientX - this.dragStart.x);
    this.panY.set(clientY - this.dragStart.y);
    this.render();
  }

  endDrag(): void {
    this.isDragging.set(false);
  }

  render(): void {
    const canvas = this.previewCanvas?.nativeElement;
    if (!canvas || !this.imageLoaded()) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const w = canvas.width;
    const h = canvas.height;

    ctx.clearRect(0, 0, w, h);
    ctx.save();

    // Center origin
    ctx.translate(w / 2 + this.panX(), h / 2 + this.panY());
    ctx.rotate((this.rotation() * Math.PI) / 180);
    ctx.scale(this.zoom(), this.zoom());

    const img = this.imageElement;
    // Scale image to cover canvas while maintaining aspect ratio
    const imgRatio = img.width / img.height;
    const canvasRatio = w / h;
    let drawW = w;
    let drawH = h;

    if (imgRatio > canvasRatio) {
      drawH = h;
      drawW = h * imgRatio;
    } else {
      drawW = w;
      drawH = w / imgRatio;
    }

    ctx.drawImage(img, -drawW / 2, -drawH / 2, drawW, drawH);
    ctx.restore();
  }

  applyCrop(): void {
    const targetW = this.mode() === 'signature' ? 600 : 400;
    const targetH = this.mode() === 'signature' ? 200 : 400;

    const exportCanvas = document.createElement('canvas');
    exportCanvas.width = targetW;
    exportCanvas.height = targetH;
    const ctx = exportCanvas.getContext('2d');
    if (!ctx) return;

    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, targetW, targetH);

    ctx.save();
    ctx.translate(targetW / 2 + (this.panX() * targetW) / 360, targetH / 2 + (this.panY() * targetH) / 360);
    ctx.rotate((this.rotation() * Math.PI) / 180);
    ctx.scale(this.zoom(), this.zoom());

    const img = this.imageElement;
    const imgRatio = img.width / img.height;
    const targetRatio = targetW / targetH;
    let drawW = targetW;
    let drawH = targetH;

    if (imgRatio > targetRatio) {
      drawH = targetH;
      drawW = targetH * imgRatio;
    } else {
      drawW = targetW;
      drawH = targetW / imgRatio;
    }

    ctx.drawImage(img, -drawW / 2, -drawH / 2, drawW, drawH);
    ctx.restore();

    const croppedDataUrl = exportCanvas.toDataURL('image/jpeg', 0.92);
    this.cropped.emit(croppedDataUrl);
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
