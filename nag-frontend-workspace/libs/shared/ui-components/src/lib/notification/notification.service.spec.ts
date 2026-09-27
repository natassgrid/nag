import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NotificationService],
    });
    service = TestBed.inject(NotificationService);
  });

  it('should add success toast and auto remove after duration', fakeAsync(() => {
    const id = service.success('Saved successfully', 'Exam schedule is saved', 2000);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].title).toBe('Saved successfully');
    expect(service.toasts()[0].type).toBe('success');

    tick(2100);
    expect(service.toasts().length).toBe(0);
  }));

  it('should support error, warning, and info toasts', () => {
    service.error('Failed', 'Network timeout');
    service.warning('Warning', 'Check date');
    service.info('Notice', 'System maintenance');

    expect(service.toasts().length).toBe(3);
  });

  it('should open confirm dialog and resolve true/false', async () => {
    const confirmPromise = service.confirm({
      title: 'Delete Question',
      message: 'Are you sure?',
    });

    expect(service.confirmState()).toBeTruthy();
    expect(service.confirmState()?.options.title).toBe('Delete Question');

    service.resolveConfirm(true);

    const result = await confirmPromise;
    expect(result).toBe(true);
    expect(service.confirmState()).toBeNull();
  });
});
