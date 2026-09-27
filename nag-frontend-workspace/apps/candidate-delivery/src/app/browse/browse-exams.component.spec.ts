import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BrowseExamsComponent } from './browse-exams.component';
import { CandidateBrowseService, DEFAULT_MOCK_EXAMS } from './services';

describe('BrowseExamsComponent', () => {
  let component: BrowseExamsComponent;
  let fixture: ComponentFixture<BrowseExamsComponent>;
  let browseService: CandidateBrowseService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrowseExamsComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [provideRouter([]), CandidateBrowseService],
    }).compileComponents();

    fixture = TestBed.createComponent(BrowseExamsComponent);
    component = fixture.componentInstance;
    browseService = TestBed.inject(CandidateBrowseService);
    browseService.catalog.set(DEFAULT_MOCK_EXAMS);
    fixture.detectChanges();
  });

  it('should create the browse exams component', () => {
    expect(component).toBeTruthy();
  });

  it('should filter exams by search query', () => {
    component.onSearchChange('GATE-DPI');
    expect(component.filteredExams().length).toBe(1);
    expect(component.filteredExams()[0].code).toBe('GATE-DPI-2026');
  });

  it('should filter exams by category', () => {
    component.onCategoryChange('BANKING');
    expect(component.filteredExams().length).toBe(1);
    expect(component.filteredExams()[0].code).toBe('RBI-GRADE-B-2026');
  });

  it('should filter exams by status APPLIED', () => {
    component.onStatusChange('APPLIED');
    const appliedExams = component.filteredExams();
    expect(appliedExams.every((e) => e.applied)).toBe(true);
  });

  it('should sort exams by fee low to high', () => {
    component.onSortChange('FEE_ASC');
    const sorted = component.filteredExams();
    for (let i = 0; i < sorted.length - 1; i++) {
      expect(sorted[i].feeAmount).toBeLessThanOrEqual(sorted[i + 1].feeAmount);
    }
  });

  it('should reset all filters to default', () => {
    component.onSearchChange('Civil');
    component.onCategoryChange('CIVIL_SERVICES');
    component.onStatusChange('CLOSING_SOON');
    component.resetFilters();

    expect(component.filters().searchQuery).toBe('');
    expect(component.filters().category).toBe('ALL');
    expect(component.filters().statusFilter).toBe('ALL');
    expect(component.filteredExams().length).toBe(DEFAULT_MOCK_EXAMS.length);
  });

  it('should open details drawer when openDetails is invoked', () => {
    const target = DEFAULT_MOCK_EXAMS[0];
    component.openDetails(target);
    expect(component.selectedDetailExam()).toBe(target);
  });

  it('should open apply modal when openApplyModal is invoked', () => {
    const target = DEFAULT_MOCK_EXAMS[1];
    component.openApplyModal(target);
    expect(component.selectedApplyExam()).toBe(target);
  });
});
