import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import {
  SubjectTopicService,
  SubjectHierarchy,
  TopicNode,
  SubtopicNode,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'app-admin-subject-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatExpansionModule,
  ],
  templateUrl: './admin-subject-management.component.html',
  styleUrl: './admin-subject-management.component.scss',
})
export class AdminSubjectManagementComponent implements OnInit {
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly snackBar = inject(MatSnackBar);

  readonly hierarchy = signal<SubjectHierarchy[]>([]);
  readonly loading = signal<boolean>(true);
  readonly searchQuery = signal<string>('');
  readonly creating = signal<boolean>(false);

  // Expanded panel tracking
  readonly expandedSubjects = signal<Set<number>>(new Set<number>());
  readonly expandedTopics = signal<Set<number>>(new Set<number>());

  // Quick-Add Subject Modal / Panel
  showAddSubject = signal<boolean>(false);
  newSubjectName = '';
  newSubjectCode = '';
  newSubjectDescription = '';

  // Quick-Add Topic tracking per subjectId
  readonly addingTopicSubjectId = signal<number | null>(null);
  newTopicName = '';
  newTopicDescription = '';

  // Quick-Add Subtopic tracking per topicId
  readonly addingSubtopicTopicId = signal<number | null>(null);
  newSubtopicName = '';
  newSubtopicDescription = '';

  // Computed Metrics
  readonly totalSubjects = computed(() => this.hierarchy().length);
  readonly totalTopics = computed(() =>
    this.hierarchy().reduce((acc, sub) => acc + (sub.topics?.length || 0), 0)
  );
  readonly totalSubtopics = computed(() =>
    this.hierarchy().reduce(
      (acc, sub) =>
        acc +
        (sub.topics?.reduce(
          (tAcc, top) => tAcc + (top.subtopics?.length || 0),
          0
        ) || 0),
      0
    )
  );

  // Filtered Hierarchy based on search query
  readonly filteredHierarchy = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    if (!query) return this.hierarchy();

    return this.hierarchy()
      .map((subject) => {
        const subjectMatch =
          subject.name.toLowerCase().includes(query) ||
          (subject.code && subject.code.toLowerCase().includes(query)) ||
          (subject.description && subject.description.toLowerCase().includes(query));

        const matchedTopics = (subject.topics || []).filter((topic) => {
          const topicMatch =
            topic.name.toLowerCase().includes(query) ||
            (topic.description && topic.description.toLowerCase().includes(query));

          const matchedSubtopics = (topic.subtopics || []).filter(
            (st) =>
              st.name.toLowerCase().includes(query) ||
              (st.description && st.description.toLowerCase().includes(query))
          );

          return topicMatch || matchedSubtopics.length > 0;
        });

        if (subjectMatch || matchedTopics.length > 0) {
          return {
            ...subject,
            topics: matchedTopics.length > 0 ? matchedTopics : subject.topics,
          };
        }
        return null;
      })
      .filter((s): s is SubjectHierarchy => s !== null);
  });

  ngOnInit(): void {
    this.loadHierarchy();
  }

  loadHierarchy(): void {
    this.loading.set(true);
    this.subjectTopicService.getHierarchy().subscribe({
      next: (data) => {
        this.hierarchy.set(data || []);
        // Auto-expand first 2 subjects if available
        if (data && data.length > 0) {
          const initExp = new Set<number>();
          data.slice(0, 3).forEach((s) => initExp.add(s.id));
          this.expandedSubjects.set(initExp);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(
          err?.error?.message || 'Failed to load subject taxonomy hierarchy',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  toggleSubject(id: number): void {
    const current = new Set(this.expandedSubjects());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.expandedSubjects.set(current);
  }

  isSubjectExpanded(id: number): boolean {
    return this.expandedSubjects().has(id);
  }

  toggleTopic(id: number): void {
    const current = new Set(this.expandedTopics());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.expandedTopics.set(current);
  }

  isTopicExpanded(id: number): boolean {
    return this.expandedTopics().has(id);
  }

  expandAll(): void {
    const allSubjectIds = new Set<number>();
    const allTopicIds = new Set<number>();
    this.hierarchy().forEach((s) => {
      allSubjectIds.add(s.id);
      s.topics?.forEach((t) => allTopicIds.add(t.id));
    });
    this.expandedSubjects.set(allSubjectIds);
    this.expandedTopics.set(allTopicIds);
  }

  collapseAll(): void {
    this.expandedSubjects.set(new Set<number>());
    this.expandedTopics.set(new Set<number>());
  }

  // --- Create Subject ---
  createSubject(): void {
    if (!this.newSubjectName.trim()) {
      this.snackBar.open('Please enter a valid subject name', 'Dismiss', {
        duration: 3000,
      });
      return;
    }

    this.creating.set(true);
    this.subjectTopicService
      .createSubject({
        name: this.newSubjectName.trim(),
        code: this.newSubjectCode.trim() || undefined,
        description: this.newSubjectDescription.trim() || undefined,
      })
      .subscribe({
        next: (created) => {
          this.creating.set(false);
          this.snackBar.open(
            `Subject "${created.name}" created successfully!`,
            'Dismiss',
            { duration: 3000 }
          );
          this.newSubjectName = '';
          this.newSubjectCode = '';
          this.newSubjectDescription = '';
          this.showAddSubject.set(false);
          this.loadHierarchy();
        },
        error: (err) => {
          this.creating.set(false);
          this.snackBar.open(
            err?.error?.message || 'Failed to create subject',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
  }

  // --- Create Topic ---
  openAddTopic(subjectId: number): void {
    this.addingTopicSubjectId.set(subjectId);
    this.newTopicName = '';
    this.newTopicDescription = '';
  }

  closeAddTopic(): void {
    this.addingTopicSubjectId.set(null);
    this.newTopicName = '';
    this.newTopicDescription = '';
  }

  createTopic(subjectId: number): void {
    if (!this.newTopicName.trim()) {
      this.snackBar.open('Please enter a valid topic name', 'Dismiss', {
        duration: 3000,
      });
      return;
    }

    this.creating.set(true);
    this.subjectTopicService
      .createTopic(subjectId, {
        name: this.newTopicName.trim(),
        description: this.newTopicDescription.trim() || undefined,
      })
      .subscribe({
        next: (created) => {
          this.creating.set(false);
          this.snackBar.open(
            `Topic "${created.name}" created successfully!`,
            'Dismiss',
            { duration: 3000 }
          );
          this.closeAddTopic();
          // Ensure subject is expanded
          const currentExp = new Set(this.expandedSubjects());
          currentExp.add(subjectId);
          this.expandedSubjects.set(currentExp);
          this.loadHierarchy();
        },
        error: (err) => {
          this.creating.set(false);
          this.snackBar.open(
            err?.error?.message || 'Failed to create topic',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
  }

  // --- Create Subtopic ---
  openAddSubtopic(topicId: number): void {
    this.addingSubtopicTopicId.set(topicId);
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
  }

  closeAddSubtopic(): void {
    this.addingSubtopicTopicId.set(null);
    this.newSubtopicName = '';
    this.newSubtopicDescription = '';
  }

  createSubtopic(subjectId: number, topicId: number): void {
    if (!this.newSubtopicName.trim()) {
      this.snackBar.open('Please enter a valid subtopic name', 'Dismiss', {
        duration: 3000,
      });
      return;
    }

    this.creating.set(true);
    this.subjectTopicService
      .createSubtopic(subjectId, topicId, {
        name: this.newSubtopicName.trim(),
        description: this.newSubtopicDescription.trim() || undefined,
      })
      .subscribe({
        next: (created) => {
          this.creating.set(false);
          this.snackBar.open(
            `Subtopic "${created.name}" created successfully!`,
            'Dismiss',
            { duration: 3000 }
          );
          this.closeAddSubtopic();
          // Ensure topic is expanded
          const currentExp = new Set(this.expandedTopics());
          currentExp.add(topicId);
          this.expandedTopics.set(currentExp);
          this.loadHierarchy();
        },
        error: (err) => {
          this.creating.set(false);
          this.snackBar.open(
            err?.error?.message || 'Failed to create subtopic',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
  }
}
