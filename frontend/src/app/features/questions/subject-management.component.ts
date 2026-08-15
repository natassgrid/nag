import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

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
    MatChipsModule,
    PageHeaderComponent
  ],
  templateUrl: './subject-management.component.html',
  styleUrls: ['./subject-management.component.scss']
})
export class SubjectManagementComponent implements OnInit {
  hierarchy: SubjectHierarchy[] = [];
  loading = true;
  creating = false;
  showAddSubject = true;

  newSubjectName = '';
  newSubjectCode = '';
  newTopicNames: Record<string, string> = {};
  newSubtopicNames: Record<string, string> = {};

  constructor(
    private subjectTopicService: SubjectTopicService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  toggleAddForm(): void {
    this.showAddSubject = !this.showAddSubject;
    this.cdr.detectChanges();
  }

  ngOnInit(): void {
    this.loadHierarchy();
  }

  loadHierarchy(): void {
    this.loading = true;
    this.subjectTopicService.getHierarchy().subscribe({
      next: (data) => {
        this.hierarchy = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
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
