import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SearchInputComponent } from './search-input.component';

describe('SearchInputComponent', () => {
  let component: SearchInputComponent;
  let fixture: ComponentFixture<SearchInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SearchInputComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SearchInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should emit search query when input changes after debounce', fakeAsync(() => {
    const searchSpy = jest.fn();
    component.searchChange.subscribe(searchSpy);

    component.onInputChange({ target: { value: 'Mathematics' } } as any);
    tick(350);

    expect(searchSpy).toHaveBeenCalledWith('Mathematics');
    expect(component.internalValue()).toBe('Mathematics');
  }));

  it('should clear search on clear()', () => {
    const searchSpy = jest.fn();
    component.searchChange.subscribe(searchSpy);

    component.internalValue.set('previous');
    component.clear();

    expect(component.internalValue()).toBe('');
    expect(searchSpy).toHaveBeenCalledWith('');
  });
});
