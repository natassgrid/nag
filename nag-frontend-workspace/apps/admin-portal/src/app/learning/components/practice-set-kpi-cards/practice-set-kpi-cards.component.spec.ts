import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PracticeSetKpiCardsComponent } from './practice-set-kpi-cards.component';

describe('PracticeSetKpiCardsComponent', () => {
  let component: PracticeSetKpiCardsComponent;
  let fixture: ComponentFixture<PracticeSetKpiCardsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PracticeSetKpiCardsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeSetKpiCardsComponent);
    component = fixture.componentInstance;
  });

  it('should create and calculate KPI metrics correctly', () => {
    fixture.componentRef.setInput('total', 10);
    fixture.componentRef.setInput('published', 6);
    fixture.componentRef.setInput('totalQuestions', 250);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.total()).toBe(10);
    expect(component.published()).toBe(6);
    expect(component.totalQuestions()).toBe(250);
    expect(component.draft()).toBe(4);
  });

  it('should handle zero drafts when published equals or exceeds total', () => {
    fixture.componentRef.setInput('total', 5);
    fixture.componentRef.setInput('published', 5);
    fixture.componentRef.setInput('totalQuestions', 100);
    fixture.detectChanges();

    expect(component.draft()).toBe(0);
  });
});
