import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  PaperDetail,
  PaperTranslateResponse,
} from '@nag-frontend-workspace/examinations-data-access';
import { LanguageOption } from '@nag-frontend-workspace/shared-util-i18n';
import { PaperTranslationSubpanelComponent } from '../paper-translation-subpanel/paper-translation-subpanel.component';

@Component({
  selector: 'nag-paper-inspection-drawer',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    PaperTranslationSubpanelComponent,
  ],
  templateUrl: './paper-inspection-drawer.component.html',
  styleUrl: './paper-inspection-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperInspectionDrawerComponent {
  open = input<boolean>(false);
  loading = input<boolean>(false);
  paperDetail = input<PaperDetail | null>(null);
  paperId = input<string | null>(null);
  isTranslating = input<boolean>(false);
  activeTranslationJob = input<PaperTranslateResponse | null>(null);
  supportedLanguages = input<LanguageOption[]>([]);

  close = output<void>();
  startTranslation = output<{ targetLanguage: string; overwriteExisting: boolean }>();

  onClose(): void {
    this.close.emit();
  }

  onStartTranslation(targetLanguage: string): void {
    this.startTranslation.emit({
      targetLanguage,
      overwriteExisting: false,
    });
  }

  getMerkleRoot(paper: PaperDetail): string {
    return (
      paper.merkleRootHash ||
      paper.encryptedPackageRef ||
      "0x4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"
    );
  }

  getLedgerTxHash(paper: PaperDetail): string {
    return (
      paper.ledgerTxHash ||
      (paper.id
        ? `0x${paper.id.replace(/-/g, "")}3a92ff19b882ac`
        : "0x3a92ff19b882ac0018f2894b9812cc93198ba11124ad90019283ba8712399182")
    );
  }

  getConsensusTimestamp(paper: PaperDetail): string {
    return paper.consensusTimestamp || paper.createdAt || "2026-10-03 11:45:00 UTC";
  }

  getPublicVerifierUrl(paper: PaperDetail): string {
    const hash = this.getMerkleRoot(paper);
    return `/verify?hash=${encodeURIComponent(hash)}`;
  }

  getLedgerExplorerUrl(paper: PaperDetail): string {
    const tx = this.getLedgerTxHash(paper);
    return `/mock/ledger/tx/${encodeURIComponent(tx)}`;
  }

  copyHash(text: string): void {
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      navigator.clipboard.writeText(text);
    }
  }

}
