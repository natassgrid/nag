import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExamRuntimeHeaderComponent } from './exam-runtime-header.component';
import { SupportedLanguage } from '@nag-frontend-workspace/shared-util-i18n';

describe('ExamRuntimeHeaderComponent', () => {
  let component: ExamRuntimeHeaderComponent;
  let fixture: ComponentFixture<ExamRuntimeHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamRuntimeHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamRuntimeHeaderComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('remainingSeconds', 180);
    fixture.componentRef.setInput('formattedTime', '03:00');
    fixture.componentRef.setInput('currentLanguage', 'en' as SupportedLanguage);
    fixture.componentRef.setInput('supportedLanguages', [{ code: 'en', label: 'English', nativeName: 'English' }]);
    fixture.detectChanges();
  });

  it('should create and indicate warning timer when below 300s', () => {
    expect(component).toBeTruthy();
    expect(component.isWarningTimer()).toBe(true);
  });

  it('should emit submit event when clicked', () => {
    let submitted = false;
    component.submit.subscribe(() => (submitted = true));
    component.onSubmit();
    expect(submitted).toBe(true);
  });
});
