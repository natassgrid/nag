import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MicroLessonStatsComponent } from './micro-lesson-stats.component';

describe('MicroLessonStatsComponent', () => {
  let component: MicroLessonStatsComponent;
  let fixture: ComponentFixture<MicroLessonStatsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MicroLessonStatsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MicroLessonStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render initial 0 stats', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Total Micro-Lessons');
    expect(compiled.textContent).toContain('AI Generated (Pending Review)');
    expect(compiled.textContent).toContain('Approved / Published');
  });

  it('should reflect input values', () => {
    fixture.componentRef.setInput('total', 42);
    fixture.componentRef.setInput('aiGenerated', 12);
    fixture.componentRef.setInput('approved', 30);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('42');
    expect(compiled.textContent).toContain('12');
    expect(compiled.textContent).toContain('30');
  });
});
