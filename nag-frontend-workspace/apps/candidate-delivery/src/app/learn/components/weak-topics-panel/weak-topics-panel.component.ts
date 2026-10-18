import { Component, ChangeDetectionStrategy, input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { WeakTopicRecommendation } from '../../models';

@Component({
  selector: 'app-weak-topics-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './weak-topics-panel.component.html',
  styleUrl: './weak-topics-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeakTopicsPanelComponent {
  private readonly router = inject(Router);

  topics = input.required<WeakTopicRecommendation[]>();

  getTopicTitle(wt: WeakTopicRecommendation): string {
    return wt.topicName || wt.topic || 'Unknown Topic';
  }

  getAccuracy(wt: WeakTopicRecommendation): number {
    const acc = wt.currentAccuracy ?? 0;
    return acc > 1 ? acc / 100 : acc;
  }

  getSubtopics(wt: WeakTopicRecommendation): string[] {
    return wt.subtopicsToRevise || wt.subtopics || [];
  }

  hashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = (hash << 5) - hash + str.charCodeAt(i);
      hash |= 0;
    }
    return hash;
  }

  onStudyLesson(wt: WeakTopicRecommendation): void {
    const topic = this.getTopicTitle(wt);
    const subtopics = this.getSubtopics(wt);
    const subtopic = subtopics[0] || topic;
    const subject = wt.subjectName || wt.subject || '';
    const subtopicId = wt.subtopicId || Math.abs(this.hashCode(subtopic));
    this.router.navigate(['/learn/lesson', subtopicId], {
      queryParams: {
        subtopicName: subtopic,
        topicName: topic,
        subjectName: subject,
      },
    });
  }

  onPractice(wt: WeakTopicRecommendation): void {
    const topic = this.getTopicTitle(wt);
    this.router.navigate(['/practice'], {
      queryParams: {
        search: topic,
      },
    });
  }
}
