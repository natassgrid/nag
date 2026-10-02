import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ExamModeBannerComponent } from './exam-mode-banner.component';

describe('ExamModeBannerComponent', () => {
  let component: ExamModeBannerComponent;
  let fixture: ComponentFixture<ExamModeBannerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExamModeBannerComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ExamModeBannerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('mode', 'PREVIEW');
    fixture.detectChanges();
  });

  it('should render preview banner when in PREVIEW mode', () => {
    expect(fixture.nativeElement.textContent).toContain('Preview Mode');
  });

  it('should render practice simulation banner when in PRACTICE mode', () => {
    fixture.componentRef.setInput('mode', 'PRACTICE');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Simulation Mode');
  });

  it('should emit exit event when exit button is clicked', () => {
    jest.spyOn(component.exit, 'emit');
    const exitBtn: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    exitBtn?.click();
    expect(component.exit.emit).toHaveBeenCalled();
  });
});
