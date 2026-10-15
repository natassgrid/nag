import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CandidateDrawerComponent } from './candidate-drawer.component';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('CandidateDrawerComponent', () => {
  let component: CandidateDrawerComponent;
  let fixture: ComponentFixture<CandidateDrawerComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CandidateDrawerComponent, NoopAnimationsModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateDrawerComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create drawer component', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle expandable sub-menu groups', () => {
    expect(component.isGroupExpanded('learning')).toBe(true);

    component.toggleGroup('learning');
    expect(component.isGroupExpanded('learning')).toBe(false);

    component.toggleGroup('learning');
    expect(component.isGroupExpanded('learning')).toBe(true);
  });

  it('should compute user initials accurately', () => {
    fixture.componentRef.setInput('userName', 'Jane Doe');
    expect(component.getUserInitials()).toBe('JD');

    fixture.componentRef.setInput('userName', 'Admin');
    expect(component.getUserInitials()).toBe('AD');
  });

  it('should detect when grouped items match current route', () => {
    const learningGroup = component.navItems().find((i) => i.id === 'learning')!;

    jest.spyOn(router, 'url', 'get').mockReturnValue('/learn/recommendations');
    expect(component.isGroupActive(learningGroup)).toBe(true);
  });

  it('should emit closeDrawer on escape or backdrop click', () => {
    let closed = false;
    component.closeDrawer.subscribe(() => {
      closed = true;
    });

    fixture.componentRef.setInput('isOpen', true);
    component.onEscape();
    expect(closed).toBe(true);

    closed = false;
    component.onBackdropClick();
    expect(closed).toBe(true);
  });
});
