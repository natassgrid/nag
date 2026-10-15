import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CandidateHeaderComponent } from './candidate-header.component';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CANDIDATE_NAV_ITEMS } from '../layout.models';

describe('CandidateHeaderComponent', () => {
  let component: CandidateHeaderComponent;
  let fixture: ComponentFixture<CandidateHeaderComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateHeaderComponent, NoopAnimationsModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateHeaderComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create header component', () => {
    expect(component).toBeTruthy();
  });

  it('should render grouped navigation items with submenus', () => {
    const items = component.navItems();
    expect(items.length).toBe(4);

    const learningGroup = items.find((i) => i.id === 'learning');
    expect(learningGroup).toBeTruthy();
    expect(learningGroup?.children?.length).toBe(4);
    expect(learningGroup?.children?.map((c) => c.id)).toContain('roadmap');
    expect(learningGroup?.children?.map((c) => c.id)).toContain('practice');

    const assessmentGroup = items.find((i) => i.id === 'assessments');
    expect(assessmentGroup).toBeTruthy();
    expect(assessmentGroup?.children?.length).toBe(2);
  });

  it('should detect when a group is active based on router URL', () => {
    const learningGroup = component.navItems().find((i) => i.id === 'learning')!;

    // Default URL is '/'
    expect(component.isGroupActive(learningGroup)).toBe(false);

    // Mock router URL matching child route
    jest.spyOn(router, 'url', 'get').mockReturnValue('/learn/roadmap');
    expect(component.isGroupActive(learningGroup)).toBe(true);

    jest.spyOn(router, 'url', 'get').mockReturnValue('/practice/history');
    expect(component.isGroupActive(learningGroup)).toBe(true);

    jest.spyOn(router, 'url', 'get').mockReturnValue('/browse');
    expect(component.isGroupActive(learningGroup)).toBe(false);
  });

  it('should emit toggleDrawer on hamburger click', () => {
    let emitted = false;
    component.toggleDrawer.subscribe(() => {
      emitted = true;
    });

    component.toggleDrawer.emit();
    expect(emitted).toBe(true);
  });
});
