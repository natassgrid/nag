import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import {
  SubjectTopicService,
  SubjectHierarchy,
  TopicNode,
  SubtopicNode
} from './subject-topic.service';

@Component({
  selector: 'app-subject-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule
  ],
  template: `
    <div class="subject-management">
      <div class="header">
        <h1>Subject Management</h1>
        <p class="subtitle">Manage the Subject → Topic → Subtopic hierarchy for question categorization</p>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="40"></mat-spinner>
        <p>Loading hierarchy...</p>
      </div>

      <div *ngIf="!loading" class="content">
        <!-- Add Subject -->
        <mat-card class="add-card">
          <mat-card-content>
            <div class="inline-form">
              <mat-form-field appearance="outline" class="flex-field">
                <mat-label>New Subject Name</mat-label>
                <input matInput [(ngModel)]="newSubjectName" placeholder="e.g. General Knowledge" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Code</mat-label>
                <input matInput [(ngModel)]="newSubjectCode" placeholder="e.g. GK" />
              </mat-form-field>
              <button mat-raised-button color="primary"
                      [disabled]="!newSubjectName || creating"
                      (click)="createSubject()">
                <mat-icon>add</mat-icon> Add Subject
              </button>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Hierarchy Tree -->
        <mat-accordion multi>
          <mat-expansion-panel *ngFor="let subject of hierarchy" class="subject-panel">
            <mat-expansion-panel-header>
              <mat-panel-title>
                <mat-icon class="panel-icon">menu_book</mat-icon>
                {{ subject.name }}
                <mat-chip *ngIf="subject.code" class="code-chip">{{ subject.code }}</mat-chip>
              </mat-panel-title>
              <mat-panel-description>
                {{ subject.topics.length }} topic(s)
              </mat-panel-description>
            </mat-expansion-panel-header>

            <p *ngIf="subject.description" class="description">{{ subject.description }}</p>

            <!-- Add Topic -->
            <div class="inline-form nested">
              <mat-form-field appearance="outline" class="flex-field">
                <mat-label>New Topic</mat-label>
                <input matInput [(ngModel)]="newTopicNames[subject.id]" placeholder="Topic name" />
              </mat-form-field>
              <button mat-raised-button color="accent"
                      [disabled]="!newTopicNames[subject.id] || creating"
                      (click)="createTopic(subject)">
                <mat-icon>add</mat-icon> Add Topic
              </button>
            </div>

            <!-- Topics -->
            <mat-accordion multi class="topic-accordion">
              <mat-expansion-panel *ngFor="let topic of subject.topics" class="topic-panel">
                <mat-expansion-panel-header>
                  <mat-panel-title>
                    <mat-icon class="panel-icon">topic</mat-icon>
                    {{ topic.name }}
                  </mat-panel-title>
                  <mat-panel-description>
                    {{ topic.subtopics.length }} subtopic(s)
                  </mat-panel-description>
                </mat-expansion-panel-header>

                <p *ngIf="topic.description" class="description">{{ topic.description }}</p>

                <!-- Add Subtopic -->
                <div class="inline-form nested">
                  <mat-form-field appearance="outline" class="flex-field">
                    <mat-label>New Subtopic</mat-label>
                    <input matInput [(ngModel)]="newSubtopicNames[topic.id]" placeholder="Subtopic name" />
                  </mat-form-field>
                  <button mat-raised-button color="accent"
                          [disabled]="!newSubtopicNames[topic.id] || creating"
                          (click)="createSubtopic(subject, topic)">
                    <mat-icon>add</mat-icon> Add Subtopic
                  </button>
                </div>

                <!-- Subtopics list -->
                <div class="subtopic-list">
                  <div *ngFor="let subtopic of topic.subtopics" class="subtopic-item">
                    <mat-icon class="subtopic-icon">label</mat-icon>
                    <span>{{ subtopic.name }}</span>
                    <span *ngIf="subtopic.description" class="subtopic-desc">— {{ subtopic.description }}</span>
                  </div>
                  <p *ngIf="topic.subtopics.length === 0" class="empty-msg">No subtopics yet</p>
                </div>
              </mat-expansion-panel>
            </mat-accordion>

            <p *ngIf="subject.topics.length === 0" class="empty-msg">No topics yet. Add one above.</p>
          </mat-expansion-panel>
        </mat-accordion>

        <p *ngIf="hierarchy.length === 0" class="empty-msg center">
          No subjects found. Create one to get started.
        </p>
      </div>
    </div>
  `,
  styles: [`
    .subject-management {
      padding: 24px;
      max-width: 1000px;
      margin: 0 auto;
    }
    .header h1 {
      margin: 0;
      font-size: 24px;
    }
    .subtitle {
      color: #666;
      margin-top: 4px;
    }
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      gap: 16px;
    }
    .add-card {
      margin-bottom: 24px;
    }
    .inline-form {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .inline-form.nested {
      margin: 12px 0;
      padding-left: 8px;
    }
    .flex-field {
      flex: 1;
    }
    .subject-panel {
      margin-bottom: 8px;
    }
    .topic-accordion {
      margin-top: 8px;
    }
    .topic-panel {
      margin-bottom: 4px;
    }
    .panel-icon {
      margin-right: 8px;
      font-size: 20px;
      vertical-align: middle;
    }
    .code-chip {
      margin-left: 8px;
      font-size: 11px;
    }
    .description {
      color: #666;
      font-style: italic;
      margin: 4px 0 12px 0;
    }
    .subtopic-list {
      padding: 8px 0 8px 16px;
    }
    .subtopic-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 0;
      border-bottom: 1px solid #f0f0f0;
    }
    .subtopic-item:last-child {
      border-bottom: none;
    }
    .subtopic-icon {
      font-size: 16px;
      color: #888;
    }
    .subtopic-desc {
      color: #888;
      font-size: 13px;
    }
    .empty-msg {
      color: #999;
      font-style: italic;
      padding: 8px 0;
    }
    .empty-msg.center {
      text-align: center;
      padding: 32px;
    }
  `]
})
export class SubjectManagementComponent implements OnInit {
  hierarchy: SubjectHierarchy[] = [];
  loading = true;
  creating = false;

  newSubjectName = '';
  newSubjectCode = '';
  newTopicNames: Record<string, string> = {};
  newSubtopicNames: Record<string, string> = {};

  constructor(
    private subjectTopicService: SubjectTopicService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadHierarchy();
  }

  loadHierarchy(): void {
    this.loading = true;
    this.subjectTopicService.getHierarchy().subscribe({
      next: (data) => {
        this.hierarchy = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load hierarchy', 'Close', { duration: 3000 });
      }
    });
  }

  createSubject(): void {
    if (!this.newSubjectName) return;
    this.creating = true;
    this.subjectTopicService.createSubject({
      name: this.newSubjectName,
      code: this.newSubjectCode || undefined
    }).subscribe({
      next: () => {
        this.snackBar.open('Subject created', 'Close', { duration: 2000 });
        this.newSubjectName = '';
        this.newSubjectCode = '';
        this.creating = false;
        this.loadHierarchy();
      },
      error: (err) => {
        this.creating = false;
        const msg = err?.error?.message || 'Failed to create subject';
        this.snackBar.open(msg, 'Close', { duration: 3000 });
      }
    });
  }

  createTopic(subject: SubjectHierarchy): void {
    const name = this.newTopicNames[subject.id];
    if (!name) return;
    this.creating = true;
    this.subjectTopicService.createTopic(subject.id, { name }).subscribe({
      next: () => {
        this.snackBar.open('Topic created', 'Close', { duration: 2000 });
        this.newTopicNames[subject.id] = '';
        this.creating = false;
        this.loadHierarchy();
      },
      error: (err) => {
        this.creating = false;
        const msg = err?.error?.message || 'Failed to create topic';
        this.snackBar.open(msg, 'Close', { duration: 3000 });
      }
    });
  }

  createSubtopic(subject: SubjectHierarchy, topic: TopicNode): void {
    const name = this.newSubtopicNames[topic.id];
    if (!name) return;
    this.creating = true;
    this.subjectTopicService.createSubtopic(subject.id, topic.id, { name }).subscribe({
      next: () => {
        this.snackBar.open('Subtopic created', 'Close', { duration: 2000 });
        this.newSubtopicNames[topic.id] = '';
        this.creating = false;
        this.loadHierarchy();
      },
      error: (err) => {
        this.creating = false;
        const msg = err?.error?.message || 'Failed to create subtopic';
        this.snackBar.open(msg, 'Close', { duration: 3000 });
      }
    });
  }
}
