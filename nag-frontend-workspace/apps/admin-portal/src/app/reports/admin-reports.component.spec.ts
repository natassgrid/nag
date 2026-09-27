import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminReportsComponent } from './admin-reports.component';

describe('AdminReportsComponent', () => {
  let component: AdminReportsComponent;
  let fixture: ComponentFixture<AdminReportsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminReportsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render admin reports dashboard', () => {
    expect(component).toBeTruthy();
    expect(component.selectedExam).toBe('NES-2026-S1');
    expect(component.reportType).toBe('RESULTS');
  });
});
