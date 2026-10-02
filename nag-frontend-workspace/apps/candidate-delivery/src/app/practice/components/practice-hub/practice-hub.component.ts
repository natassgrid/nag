import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Router, RouterModule } from '@angular/router';
import { PracticeService } from '../../services/practice.service';
import { PracticeSet } from '../../models';
import { PracticeSetCardComponent } from '../practice-set-card/practice-set-card.component';
import { PracticeLaunchDialogComponent } from '../practice-launch-dialog/practice-launch-dialog.component';

@Component({
  selector: 'app-practice-hub',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    PracticeSetCardComponent
  ],
  templateUrl: './practice-hub.component.html',
  styleUrl: './practice-hub.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeHubComponent implements OnInit {
  private readonly practiceService = inject(PracticeService);
  private readonly dialog = inject(MatDialog);

  readonly practiceSets = signal<PracticeSet[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.practiceService.getSets().subscribe({
      next: (sets) => {
        this.practiceSets.set(sets);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load practice sets.');
        this.isLoading.set(false);
      }
    });
  }

  openLaunch(set: PracticeSet): void {
    this.dialog.open(PracticeLaunchDialogComponent, {
      data: { set },
      width: '400px'
    });
  }
}
