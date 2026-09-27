import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BrowseFilterBarComponent } from './browse-filter-bar.component';

describe('BrowseFilterBarComponent', () => {
  let component: BrowseFilterBarComponent;
  let fixture: ComponentFixture<BrowseFilterBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrowseFilterBarComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(BrowseFilterBarComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('filters', {
      searchQuery: '',
      category: 'ALL',
      statusFilter: 'ALL',
      sortBy: 'DATE_ASC',
    });
    fixture.componentRef.setInput('totalMatches', 5);
    fixture.detectChanges();
  });

  it('should create filter bar', () => {
    expect(component).toBeTruthy();
  });

  it('should emit category change when clicked', () => {
    let emittedCategory = '';
    component.categoryChange.subscribe((cat) => (emittedCategory = cat));
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const techBtn = Array.from(buttons).find((b: any) =>
      b.textContent.includes('Engineering & Tech')
    ) as HTMLButtonElement;
    techBtn?.click();
    expect(emittedCategory).toBe('ENGINEERING');
  });
});
