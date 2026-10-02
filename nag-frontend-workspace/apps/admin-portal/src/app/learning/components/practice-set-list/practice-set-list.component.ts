import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { PracticeSet } from '../../models';

@Component({
  selector: 'app-practice-set-list',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule
  ],
  templateUrl: './practice-set-list.component.html',
  styleUrls: ['./practice-set-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetListComponent {
  sets = input.required<PracticeSet[]>();
  edit = output<PracticeSet>();
  curate = output<PracticeSet>();
  delete = output<PracticeSet>();
  togglePublish = output<PracticeSet>();
  create = output<void>();
}
