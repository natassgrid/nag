import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PaperTranslationSubpanelComponent } from './paper-translation-subpanel.component';

describe('PaperTranslationSubpanelComponent', () => {
  let component: PaperTranslationSubpanelComponent;
  let fixture: ComponentFixture<PaperTranslationSubpanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaperTranslationSubpanelComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PaperTranslationSubpanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('supportedLanguages', [
      { code: 'hi', label: 'Hindi', nativeLabel: 'हिन्दी' },
      { code: 'bn', label: 'Bengali', nativeLabel: 'বাংলা' },
    ]);
    fixture.detectChanges();
  });

  it('should create and default to hindi language', () => {
    expect(component).toBeTruthy();
    expect(component.targetLanguage).toBe('hi');
  });

  it('should emit startTranslation with target language when onStart is invoked', () => {
    jest.spyOn(component.startTranslation, 'emit');
    component.targetLanguage = 'bn';
    component.onStart();

    expect(component.startTranslation.emit).toHaveBeenCalledWith('bn');
  });

  it('should disable queue batch translation button when translation is active', () => {
    fixture.componentRef.setInput('activeTranslationJob', {
      jobId: 'job-123',
      status: 'PENDING',
      targetLanguage: 'hi',
      totalQuestions: 100,
      processedQuestions: 0,
      progressPercentage: 0,
    });
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.disabled).toBe(true);
  });

  it('should enable queue batch translation button when idle or completed', () => {
    fixture.componentRef.setInput('activeTranslationJob', {
      jobId: 'job-123',
      status: 'COMPLETED',
      targetLanguage: 'hi',
      totalQuestions: 100,
      processedQuestions: 100,
      progressPercentage: 100,
    });
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.disabled).toBe(false);
  });
});
