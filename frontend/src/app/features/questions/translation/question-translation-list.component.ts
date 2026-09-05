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
 */

import { Component, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatCardModule } from '@angular/material/card';

import { QuestionService, QuestionResponse } from '../question.service';
import { SubjectTopicService, Subject } from '../subject-topic.service';
import {
  TranslationService,
  SUPPORTED_LANGUAGES,
  SupportedLanguage
} from './translation.service';
import { QuestionTranslationDialogComponent } from './question-translation-dialog.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher,
  FilterCategory
} from '../../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-question-translation-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatMenuModule,
    MatCardModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    QuestionTranslationDialogComponent
  ],
  templateUrl: './question-translation-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-translation-list.component.scss']
})
export class QuestionTranslationListComponent implements OnInit {
  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<QuestionResponse>;

  languages: SupportedLanguage[] = SUPPORTED_LANGUAGES;
  selectedLanguage: string = 'hi';

  drawerOpen = false;
  selectedQuestion?: QuestionResponse;
  selectedLanguageForDrawer = 'hi';

  filters: Record<string, any> = {};
  subjects: Subject[] = [];

  filterCategories: FilterCategory[] = [
    {
      key: 'subject',
      label: 'Subject',
      expanded: true,
      options: []
    },
    {
      key: 'difficulty',
      label: 'Difficulty',
      expanded: false,
      options: [
        { label: 'Easy', value: 'EASY' },
        { label: 'Medium', value: 'MEDIUM' },
        { label: 'Hard', value: 'HARD' }
      ]
    },
    {
      key: 'questionType',
      label: 'Question Type',
      expanded: false,
      options: [
        { label: 'MCQ (Single Correct)', value: 'SINGLE_MCQ' },
        { label: 'MSQ (Multiple Correct)', value: 'MULTI_MCQ' },
        { label: 'Numerical', value: 'NUMERICAL' },
        { label: 'Descriptive', value: 'DESCRIPTIVE' }
      ]
    }
  ];

  columns: ColumnDef<QuestionResponse>[] = [
    { key: 'subject', header: 'Subject', sortable: true },
    { key: 'topic', header: 'Topic', sortable: true },
    {
      key: 'content',
      header: 'Question Content',
      cell: (row) => this.truncateContent(row.content)
    },
    {
      key: 'difficulty',
      header: 'Difficulty',
      type: 'chip',
      chipClass: (val) => 'chip-' + (val || 'medium').toLowerCase(),
      sortable: true
    },
    {
      key: 'state',
      header: 'Source Status',
      type: 'chip',
      chipClass: (val) => 'chip-state-' + (val || 'draft').toLowerCase(),
      sortable: true
    },
    { key: 'actions', header: 'Localization', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<QuestionResponse> = (req) => {
    const activeSubject = Array.isArray(this.filters['subject']) ? this.filters['subject'][0] : this.filters['subject'];
    const activeDifficulty = Array.isArray(this.filters['difficulty']) ? this.filters['difficulty'][0] : this.filters['difficulty'];

    return this.questionService.getQuestions({
      subject: activeSubject || undefined,
      difficulty: activeDifficulty || undefined,
      page: req.page,
      size: req.size
    });
  };

  constructor(
    private questionService: QuestionService,
    private translationService: TranslationService,
    private subjectTopicService: SubjectTopicService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSubjects();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe(subjects => {
      this.subjects = subjects;
      const subjectCat = this.filterCategories.find(c => c.key === 'subject');
      if (subjectCat) {
        subjectCat.options = subjects.map(s => ({ label: s.name, value: s.name }));
      }
      this.cdr.markForCheck();
    });
  }

  onFilterChange(updatedFilters: Record<string, any>): void {
    this.filters = { ...updatedFilters };
  }

  reload(): void {
    this.paginatedTable?.reload();
  }

  openTranslationDrawer(question: QuestionResponse, langCode: string = 'hi'): void {
    this.selectedQuestion = question;
    this.selectedLanguageForDrawer = langCode;
    this.drawerOpen = true;
  }

  onDrawerClose(updated: boolean): void {
    this.drawerOpen = false;
    if (updated) {
      this.reload();
    }
  }

  truncateContent(text: string): string {
    if (!text) return '';
    // Strip HTML tags for table display
    const clean = text.replace(/<[^>]*>/g, '');
    return clean.length > 80 ? clean.substring(0, 80) + '...' : clean;
  }

  getLanguageName(code: string): string {
    const found = this.languages.find(l => l.code === code);
    return found ? `${found.nativeName} (${found.name})` : code;
  }
}
