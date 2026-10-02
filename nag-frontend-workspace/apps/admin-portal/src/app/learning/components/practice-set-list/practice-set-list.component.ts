import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { PracticeSet } from '../../models';

@Component({
  selector: 'app-practice-set-list',
  standalone: true,
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSlideToggleModule
  ],
  templateUrl: './practice-set-list.component.html',
  styleUrls: ['./practice-set-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeSetListComponent {
  sets = input.required<PracticeSet[]>();
  edit = output<PracticeSet>();
  delete = output<PracticeSet>();
  togglePublish = output<PracticeSet>();

  displayedColumns = ['name', 'questions', 'duration', 'subject', 'published', 'actions'];
}
