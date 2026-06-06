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
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getUserId', 'getUserRoles']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();
  });

  function createComponent(userId: string, roles: string[]) {
    authServiceSpy.getUserId.and.returnValue(userId);
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
    const heading = fixture.nativeElement.querySelector('.welcome-message');
    expect(heading.textContent).toContain('john_doe');
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
