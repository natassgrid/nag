import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  ViewChild,
  model,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CandidateProfile } from '../../models';
import { WebcamCaptureModalComponent } from '../webcam-capture-modal/webcam-capture-modal.component';
import { ImageCropperModalComponent, CropMode } from '../image-cropper-modal/image-cropper-modal.component';

@Component({
  selector: 'nag-kyc-documents-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    WebcamCaptureModalComponent,
    ImageCropperModalComponent,
  ],
  templateUrl: './kyc-documents-panel.component.html',
  styleUrl: './kyc-documents-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KycDocumentsPanelComponent {
  @ViewChild('photoFileInput') photoFileInput?: ElementRef<HTMLInputElement>;
  @ViewChild('signatureFileInput') signatureFileInput?: ElementRef<HTMLInputElement>;
  @ViewChild('idProofFileInput') idProofFileInput?: ElementRef<HTMLInputElement>;

  readonly profile = model.required<CandidateProfile>();

  readonly showWebcamModal = signal<boolean>(false);
  readonly showCropperModal = signal<boolean>(false);
  readonly cropperRawImage = signal<string | null>(null);
  readonly cropperMode = signal<CropMode>('photo');
  readonly cropperTarget = signal<'photo' | 'signature'>('photo');
  readonly uploadFeedback = signal<string | null>(null);

  openWebcam(): void {
    this.showWebcamModal.set(true);
  }

  onWebcamSnapshot(dataUrl: string): void {
    this.showWebcamModal.set(false);
    this.cropperTarget.set('photo');
    this.cropperMode.set('photo');
    this.cropperRawImage.set(dataUrl);
    this.showCropperModal.set(true);
  }

  triggerPhotoUpload(): void {
    this.photoFileInput?.nativeElement.click();
  }

  triggerSignatureUpload(): void {
    this.signatureFileInput?.nativeElement.click();
  }

  triggerIdProofUpload(): void {
    this.idProofFileInput?.nativeElement.click();
  }

  onFileSelected(event: Event, target: 'photo' | 'signature' | 'idProof'): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    const reader = new FileReader();

    reader.onload = () => {
      const dataUrl = reader.result as string;
      if (target === 'photo') {
        this.cropperTarget.set('photo');
        this.cropperMode.set('photo');
        this.cropperRawImage.set(dataUrl);
        this.showCropperModal.set(true);
      } else if (target === 'signature') {
        this.cropperTarget.set('signature');
        this.cropperMode.set('signature');
        this.cropperRawImage.set(dataUrl);
        this.showCropperModal.set(true);
      } else {
        this.profile.update((p) => ({
          ...p,
          idProofUrl: dataUrl,
          idProofAssetId: `asset-id-proof-${Date.now()}`,
        }));
        this.showFeedback('ID Proof document attached successfully.');
      }
    };

    reader.readAsDataURL(file);
    input.value = '';
  }

  onCroppedImage(croppedDataUrl: string): void {
    const target = this.cropperTarget();
    if (target === 'photo') {
      this.profile.update((p) => ({
        ...p,
        photoUrl: croppedDataUrl,
        photoAssetId: `asset-photo-${Date.now()}`,
      }));
      this.showFeedback('Passport photograph updated and cropped successfully.');
    } else {
      this.profile.update((p) => ({
        ...p,
        signatureUrl: croppedDataUrl,
        signatureAssetId: `asset-sign-${Date.now()}`,
      }));
      this.showFeedback('Candidate signature updated and cropped successfully.');
    }
    this.showCropperModal.set(false);
    this.cropperRawImage.set(null);
  }

  removeDocument(target: 'photo' | 'signature' | 'idProof'): void {
    this.profile.update((p) => {
      if (target === 'photo') return { ...p, photoUrl: undefined, photoAssetId: undefined };
      if (target === 'signature') return { ...p, signatureUrl: undefined, signatureAssetId: undefined };
      return { ...p, idProofUrl: undefined, idProofAssetId: undefined };
    });
  }

  private showFeedback(msg: string): void {
    this.uploadFeedback.set(msg);
    setTimeout(() => this.uploadFeedback.set(null), 3500);
  }
}
