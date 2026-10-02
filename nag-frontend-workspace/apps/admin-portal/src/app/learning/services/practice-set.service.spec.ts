import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PracticeSetService } from './practice-set.service';
import { PracticeSet, CreatePracticeSetRequest, UpdatePracticeSetRequest } from '../models';

describe('PracticeSetService', () => {
  let service: PracticeSetService;
  let httpMock: HttpTestingController;

  const mockSets: PracticeSet[] = [
    {
      id: 'set-1',
      name: 'Physics Practice 1',
      description: 'Mock test for kinematics',
      source: 'MANUAL',
      durationMinutes: 45,
      subjectSlug: 'physics',
      published: true,
      totalQuestions: 20,
      createdBy: 'user-1',
      createdAt: '2026-10-01T10:00:00Z',
    },
    {
      id: 'set-2',
      name: 'Maths Paper Practice',
      description: 'Cloned from paper',
      source: 'EXAM_CLONE',
      durationMinutes: 60,
      subjectSlug: 'maths',
      published: false,
      totalQuestions: 30,
      createdBy: 'user-1',
      createdAt: '2026-10-02T10:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PracticeSetService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(PracticeSetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch all practice sets via GET /api/admin/practice/sets', () => {
    service.getAll().subscribe((sets) => {
      expect(sets).toEqual(mockSets);
      expect(sets.length).toBe(2);
    });

    const req = httpMock.expectOne('/api/admin/practice/sets');
    expect(req.request.method).toBe('GET');
    req.flush(mockSets);
  });

  it('should create a practice set via POST /api/admin/practice/sets', () => {
    const newReq: CreatePracticeSetRequest = {
      name: 'New Practice Set',
      description: 'Description',
      durationMinutes: 30,
      subjectSlug: 'general',
      questionIds: ['q1', 'q2'],
      source: 'MANUAL',
      totalQuestions: 2,
    };

    service.create(newReq).subscribe((created) => {
      expect(created.id).toBe('set-new');
      expect(created.name).toBe('New Practice Set');
    });

    const req = httpMock.expectOne('/api/admin/practice/sets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newReq);
    req.flush({ ...newReq, id: 'set-new', published: false, createdBy: 'user-1', createdAt: '2026-10-02T10:00:00Z' });
  });

  it('should update a practice set via PUT /api/admin/practice/sets/:id', () => {
    const updateReq: UpdatePracticeSetRequest = {
      name: 'Updated Name',
      description: 'Updated Description',
      durationMinutes: 40,
      totalQuestions: 5,
      subjectSlug: 'updated-slug',
      questionIds: ['q1', 'q2', 'q3', 'q4', 'q5'],
    };

    service.update('set-1', updateReq).subscribe((updated) => {
      expect(updated.name).toBe('Updated Name');
    });

    const req = httpMock.expectOne('/api/admin/practice/sets/set-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateReq);
    req.flush({ ...mockSets[0], ...updateReq });
  });

  it('should delete a practice set via DELETE /api/admin/practice/sets/:id', () => {
    service.delete('set-1').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne('/api/admin/practice/sets/set-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should publish a practice set via POST /api/admin/practice/sets/:id/publish', () => {
    service.publish('set-1').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne('/api/admin/practice/sets/set-1/publish');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('should unpublish a practice set via POST /api/admin/practice/sets/:id/unpublish', () => {
    service.unpublish('set-1').subscribe((res) => {
      expect(res).toBeNull();
    });

    const req = httpMock.expectOne('/api/admin/practice/sets/set-1/unpublish');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });
});
