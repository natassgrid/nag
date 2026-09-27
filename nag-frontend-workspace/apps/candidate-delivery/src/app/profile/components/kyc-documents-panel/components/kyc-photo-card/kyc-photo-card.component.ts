import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-kyc-photo-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './kyc-photo-card.component.html',
  styleUrl: './kyc-photo-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KycPhotoCardComponent {
  readonly photoUrl = input<string | undefined>(undefined);
  readonly photoAssetId = input<string | undefined>(undefined);

  readonly openWebcam = output<void>();
  readonly uploadFile = output<void>();
  readonly removePhoto = output<void>();

  onOpenWebcam(): void {
    this.openWebcam.emit();
  }

  onUploadFile(): void {
    this.uploadFile.emit();
  }

  onRemove(): void {
    this.removePhoto.emit();
  }
}
