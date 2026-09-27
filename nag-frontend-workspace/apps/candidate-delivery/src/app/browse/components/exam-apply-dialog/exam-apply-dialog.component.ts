import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import {
  CatalogExam,
  PublicCentre,
  ApplyExamPayload,
  ApplicationReceipt,
} from '../../models';
import { CandidateBrowseService } from '../../services';

@Component({
  selector: 'nag-exam-apply-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
  ],
  templateUrl: './exam-apply-dialog.component.html',
  styleUrl: './exam-apply-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamApplyDialogComponent implements OnInit {
  private readonly browseService = inject(CandidateBrowseService);
  private readonly authService = inject(AuthService);
  private readonly notificationService = inject(NotificationService);

  readonly exam = input.required<CatalogExam>();
  readonly centres = input.required<PublicCentre[]>();

  readonly closeDialog = output<void>();
  readonly applicationCompleted = output<ApplicationReceipt>();

  currentStep = signal<number>(1);
  submitting = signal<boolean>(false);
  submissionReceipt = signal<ApplicationReceipt | null>(null);

  // Form Model
  firstChoiceCentreId = signal<string>('');
  secondChoiceCentreId = signal<string>('');
  thirdChoiceCentreId = signal<string>('');
  isPwdRequired = signal<boolean>(false);
  isScribeRequired = signal<boolean>(false);
  selectedCategory = signal<string>('GENERAL');
  paymentMethod = signal<string>('UPI');

  readonly candidateUser = computed(() => {
    const user = this.authService.currentUser();
    return {
      name: user?.username || 'Aryan Sharma',
      email: user?.username && user.username.includes('@') ? user.username : 'aryan.sharma@gov.in',
      mobile: '+91 98765 43210',
      digiLockerStatus: 'VERIFIED',
      aadhaarLast4: '8839',
    };
  });

  readonly calculatedFee = computed(() => {
    const baseFee = this.exam().feeAmount;
    const cat = this.selectedCategory();
    const isPwd = this.isPwdRequired();

    if (isPwd || cat === 'SC' || cat === 'ST' || cat === 'PWD') {
      return 0; // 100% Fee Waiver for SC/ST/PwD per GOI DPI guidelines
    }
    if (cat === 'OBC' || cat === 'EWS') {
      return Math.round(baseFee * 0.75); // 25% Concession
    }
    return baseFee;
  });

  ngOnInit(): void {
    const available = this.centres();
    if (available.length > 0) {
      this.firstChoiceCentreId.set(available[0]?.id || '');
      this.secondChoiceCentreId.set(available[1]?.id || available[0]?.id || '');
      this.thirdChoiceCentreId.set(available[2]?.id || available[0]?.id || '');
    }
  }

  @HostListener('window:keydown.escape')
  handleEscapeKey(): void {
    if (!this.submitting() && !this.submissionReceipt()) {
      this.closeDialog.emit();
    }
  }

  goToNextStep(): void {
    if (this.currentStep() === 2) {
      if (!this.firstChoiceCentreId()) {
        this.notificationService.error(
          'Selection Required',
          'Please select your 1st choice examination center city.'
        );
        return;
      }
    }
    this.currentStep.update((s) => s + 1);
  }

  goToPreviousStep(): void {
    this.currentStep.update((s) => Math.max(1, s - 1));
  }

  submitApplication(): void {
    this.submitting.set(true);

    const payload: ApplyExamPayload = {
      firstChoiceCentreId: this.firstChoiceCentreId(),
      secondChoiceCentreId: this.secondChoiceCentreId() || undefined,
      thirdChoiceCentreId: this.thirdChoiceCentreId() || undefined,
      pwdRequired: this.isPwdRequired(),
      scribeRequired: this.isScribeRequired(),
    };

    this.browseService.applyForExam(this.exam().id, payload).subscribe({
      next: (receipt) => {
        // Adjust calculated fee on receipt
        receipt.feePaid = this.calculatedFee();
        receipt.category = this.selectedCategory();
        this.submissionReceipt.set(receipt);
        this.currentStep.set(4);
        this.submitting.set(false);
        this.notificationService.success(
          'Application Submitted Successfully',
          `Enrollment confirmed for ${this.exam().title}. Application No: ${receipt.applicationNumber}`
        );
        this.applicationCompleted.emit(receipt);
      },
      error: (err) => {
        this.submitting.set(false);
        this.notificationService.error(
          'Submission Error',
          err?.message || 'Unable to submit exam application. Please retry.'
        );
      },
    });
  }

  printReceipt(): void {
    window.print();
  }
}
