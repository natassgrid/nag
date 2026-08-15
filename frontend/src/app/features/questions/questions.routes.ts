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

import { Routes } from '@angular/router';

export const QUESTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./question-list.component').then(m => m.QuestionListComponent)
  },
  {
    path: 'review',
    loadComponent: () =>
      import('./question-review.component').then(m => m.QuestionReviewComponent)
  },
  {
    path: 'subjects',
    loadComponent: () =>
      import('./subject-management.component').then(m => m.SubjectManagementComponent)
  }
];
