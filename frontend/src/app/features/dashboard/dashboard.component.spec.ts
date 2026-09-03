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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../core/services/auth.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getUserName', 'getUserId', 'getUserRoles']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();
  });

  function createComponent(userName: string, roles: string[]) {
    authServiceSpy.getUserName.and.returnValue(userName);
    authServiceSpy.getUserId.and.returnValue('018f4e2a-0000-7000-8000-000000000001');
    authServiceSpy.getUserRoles.and.returnValue(roles);
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should create', () => {
    createComponent('admin', ['Super_Admin']);
    expect(component).toBeTruthy();
  });

  it('should display welcome message with username', () => {
    createComponent('john_doe', ['Candidate']);
    expect(fixture.componentInstance.username).toBe('john_doe');
  });

  it('should show admin cards for Super_Admin role', () => {
    createComponent('admin', ['Super_Admin']);
    const titles = component.cards.map(c => c.title);
    expect(titles).toContain('Users');
    expect(titles).toContain('Audit Log');
    expect(titles).toContain('Notifications');
  });

  it('should show candidate cards for Candidate role', () => {
    createComponent('student1', ['Candidate']);
    const titles = component.cards.map(c => c.title);
    expect(titles).toContain('My Exams');
    expect(titles).toContain('Results');
    expect(titles).toContain('Notifications');
    expect(titles).not.toContain('Users');
  });

  it('should show question bank for Question_Author role', () => {
    createComponent('author1', ['Question_Author']);
    const titles = component.cards.map(c => c.title);
    expect(titles).toContain('Question Bank');
    expect(titles).toContain('Notifications');
    expect(titles).not.toContain('Examinations');
  });

  it('should show evaluations for Evaluator role', () => {
    createComponent('evaluator1', ['Evaluator']);
    const titles = component.cards.map(c => c.title);
    expect(titles).toContain('Evaluations');
    expect(titles).toContain('Notifications');
  });

  it('should show exam controller cards', () => {
    createComponent('controller1', ['Exam_Controller']);
    const titles = component.cards.map(c => c.title);
    expect(titles).toContain('Examinations');
    expect(titles).toContain('Analytics');
    expect(titles).toContain('Notifications');
  });

  it('should show notifications card for all roles', () => {
    createComponent('anyone', []);
    const titles = component.cards.map(c => c.title);
    expect(titles).toContain('Notifications');
    expect(titles.length).toBe(1);
  });

  it('should navigate when Go button is clicked', () => {
    createComponent('admin', ['Super_Admin']);
    component.navigateTo('/admin/users');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/users']);
  });
});
