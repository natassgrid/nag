import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PracticeSetListComponent } from './practice-set-list.component';
import { PracticeSet } from '../../models';

describe('PracticeSetListComponent', () => {
  let component: PracticeSetListComponent;
  let fixture: ComponentFixture<PracticeSetListComponent>;

  const mockSets: PracticeSet[] = [
    {
      id: 'set-1',
      name: 'Sample Practice Set 1',
      description: 'Description 1',
      source: 'EXAM_CLONE',
      durationMinutes: 45,
      subjectSlug: 'maths',
      published: true,
      totalQuestions: 15,
      createdBy: 'admin',
      createdAt: '2026-10-01T10:00:00Z',
    },
    {
      id: 'set-2',
      name: 'Sample Practice Set 2',
      description: 'Description 2',
      source: 'MANUAL',
      durationMinutes: 60,
      subjectSlug: 'physics',
      published: false,
      totalQuestions: 25,
      createdBy: 'admin',
      createdAt: '2026-10-02T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PracticeSetListComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PracticeSetListComponent);
    component = fixture.componentInstance;
  });

  it('should create and render empty state when no sets provided', () => {
    fixture.componentRef.setInput('sets', []);
    fixture.detectChanges();

    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No Practice Sets Found');
  });

  it('should render table rows when practice sets are provided', () => {
    fixture.componentRef.setInput('sets', mockSets);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sample Practice Set 1');
    expect(compiled.textContent).toContain('Sample Practice Set 2');
    expect(compiled.textContent).toContain('Attached Paper');
    expect(compiled.textContent).toContain('Manual Curation');
  });

  it('should emit edit event when edit button clicked', () => {
    fixture.componentRef.setInput('sets', mockSets);
    fixture.detectChanges();

    const editSpy = jest.spyOn(component.edit, 'emit');
    component.edit.emit(mockSets[0]);
    expect(editSpy).toHaveBeenCalledWith(mockSets[0]);
  });

  it('should emit curate event when curate button clicked', () => {
    fixture.componentRef.setInput('sets', mockSets);
    fixture.detectChanges();

    const curateSpy = jest.spyOn(component.curate, 'emit');
    component.curate.emit(mockSets[1]);
    expect(curateSpy).toHaveBeenCalledWith(mockSets[1]);
  });

  it('should emit delete event when delete button clicked', () => {
    fixture.componentRef.setInput('sets', mockSets);
    fixture.detectChanges();

    const deleteSpy = jest.spyOn(component.delete, 'emit');
    component.delete.emit(mockSets[0]);
    expect(deleteSpy).toHaveBeenCalledWith(mockSets[0]);
  });

  it('should emit togglePublish event when publish toggle changed', () => {
    fixture.componentRef.setInput('sets', mockSets);
    fixture.detectChanges();

    const toggleSpy = jest.spyOn(component.togglePublish, 'emit');
    component.togglePublish.emit(mockSets[0]);
    expect(toggleSpy).toHaveBeenCalledWith(mockSets[0]);
  });

  it('should emit create event when create button is clicked', () => {
    const createSpy = jest.spyOn(component.create, 'emit');
    component.create.emit();
    expect(createSpy).toHaveBeenCalled();
  });
});
