import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { ExamDeliveryComponent } from './exam-delivery.component';
import { ExamDeliveryService } from './services';

describe('ExamDeliveryComponent', () => {
  let component: ExamDeliveryComponent;
  let fixture: ComponentFixture<ExamDeliveryComponent>;
  let deliveryService: ExamDeliveryService;
  let notificationService: jest.Mocked<NotificationService>;
  let router: Router;

  beforeEach(async () => {
    const notificationServiceMock = {
      confirm: jest.fn().mockResolvedValue(true),
      warning: jest.fn(),
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ExamDeliveryComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        ExamDeliveryService,
        { provide: NotificationService, useValue: notificationServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamDeliveryComponent);
    component = fixture.componentInstance;
    deliveryService = TestBed.inject(ExamDeliveryService);
    notificationService = TestBed.inject(NotificationService) as any;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
  });

  it('should initialize questions and format countdown timer', () => {
    expect(component.questions().length).toBeGreaterThan(0);
    expect(component.formattedTime()).toBeDefined();
  });

  it('should allow selecting and clearing options', () => {
    const q1 = component.questions()[0];
    component.selectOption(q1, 'opt-b');

    expect(component.countAnswered()).toBe(1);
    expect(component.questions()[0].selectedOptionId).toBe('opt-b');

    component.clearResponse(q1);
    expect(component.countAnswered()).toBe(0);
    expect(component.questions()[0].selectedOptionId).toBeUndefined();
  });

  it('should toggle flag for review', () => {
    const q1 = component.questions()[0];
    component.toggleFlag(q1);
    expect(component.questions()[0].isFlagged).toBe(true);
    expect(component.countFlagged()).toBe(1);

    component.toggleFlag(q1);
    expect(component.questions()[0].isFlagged).toBe(false);
  });

  it('should navigate between questions', () => {
    component.goToQuestion(1);
    expect(component.currentIndex()).toBe(1);
    expect(component.currentItem()?.questionCode).toBe('NES-MATH-202');

    component.prevQuestion();
    expect(component.currentIndex()).toBe(0);

    component.nextQuestion();
    expect(component.currentIndex()).toBe(1);
  });

  it('should navigate back to dashboard when exit banner is confirmed', async () => {
    const navigateSpy = jest.spyOn(router, 'navigate').mockImplementation(() => Promise.resolve(true));
    await component.handleExitBanner();
    expect(notificationService.confirm).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should submit exam when confirmed', async () => {
    const sealSpy = jest.spyOn(deliveryService, 'sealAndSubmit');
    await component.confirmSubmission();
    expect(notificationService.confirm).toHaveBeenCalled();
    expect(sealSpy).toHaveBeenCalled();
  });
});
