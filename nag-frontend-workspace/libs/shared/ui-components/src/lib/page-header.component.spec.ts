import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageHeaderComponent } from './page-header.component';

describe('PageHeaderComponent', () => {
  let component: PageHeaderComponent;
  let fixture: ComponentFixture<PageHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    component = fixture.componentInstance;
  });

  it('should create and render title and subtitle', () => {
    fixture.componentRef.setInput('title', 'Exam Administration');
    fixture.componentRef.setInput('subtitle', 'Manage examinations schedule');
    fixture.componentRef.setInput('icon', 'school');
    fixture.detectChanges();

    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Exam Administration');
    expect(el.textContent).toContain('Manage examinations schedule');
  });
});
