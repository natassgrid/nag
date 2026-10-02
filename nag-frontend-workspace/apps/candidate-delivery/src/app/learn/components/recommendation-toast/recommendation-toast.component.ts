import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-recommendation-toast',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, RouterModule],
  templateUrl: './recommendation-toast.component.html',
  styleUrl: './recommendation-toast.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendationToastComponent {
  message = input<string>('New AI Study Recommendations available!');
}
