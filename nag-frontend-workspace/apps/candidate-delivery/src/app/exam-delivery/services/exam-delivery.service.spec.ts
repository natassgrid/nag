import { TestBed } from '@angular/core/testing';
import { ExamDeliveryService } from './exam-delivery.service';

describe('ExamDeliveryService', () => {
  let service: ExamDeliveryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ExamDeliveryService],
    });
    service = TestBed.inject(ExamDeliveryService);
  });

  afterEach(() => {
    service.stopTimer();
  });

  it('should initialize with default LIVE questions', () => {
    service.initialize('LIVE', 'NES-2026-TEST');
    expect(service.deliveryMode()).toBe('LIVE');
    expect(service.examId()).toBe('NES-2026-TEST');
    expect(service.questions().length).toBeGreaterThan(0);
    expect(service.sessionMeta().sessionId).toBe('NES-2026-A48');
    expect(service.currentIndex()).toBe(0);
  });

  it('should initialize with PRACTICE mock questions', () => {
    service.initialize('PRACTICE', 'MOCK-1');
    expect(service.deliveryMode()).toBe('PRACTICE');
    expect(service.sessionMeta().title).toContain('Practice');
    expect(service.questions().length).toBe(5);
  });

  it('should initialize with PREVIEW orientation questions', () => {
    service.initialize('PREVIEW');
    expect(service.deliveryMode()).toBe('PREVIEW');
    expect(service.sessionMeta().title).toContain('Preview');
    expect(service.questions().length).toBe(3);
  });

  it('should handle question selection and clear response', () => {
    service.initialize('LIVE');
    const q1 = service.questions()[0];
    service.selectOption(q1, 'opt-b');

    expect(service.countAnswered()).toBe(1);
    expect(service.questions()[0].selectedOptionId).toBe('opt-b');

    service.clearResponse(q1);
    expect(service.countAnswered()).toBe(0);
    expect(service.questions()[0].selectedOptionId).toBeUndefined();
  });

  it('should toggle question flag', () => {
    service.initialize('LIVE');
    const q1 = service.questions()[0];
    service.toggleFlag(q1);
    expect(service.questions()[0].isFlagged).toBe(true);
    expect(service.countFlagged()).toBe(1);

    service.toggleFlag(q1);
    expect(service.questions()[0].isFlagged).toBe(false);
  });

  it('should navigate next, previous, and to specific question', () => {
    service.initialize('LIVE');
    service.nextQuestion();
    expect(service.currentIndex()).toBe(1);

    service.prevQuestion();
    expect(service.currentIndex()).toBe(0);

    service.goToQuestion(2);
    expect(service.currentIndex()).toBe(2);
    expect(service.questions()[2].isVisited).toBe(true);
  });

  it('should calculate score and seal submission cryptographically', async () => {
    service.initialize('PRACTICE');
    const q1 = service.questions()[0];
    // prac-1 correct option is 'opt-b' (marks 4)
    service.selectOption(q1, 'opt-b');

    const receipt = await service.sealAndSubmit();
    expect(receipt).toBeDefined();
    expect(receipt.signature).toBeDefined();
    expect(receipt.hash).toBeDefined();
    expect(receipt.mode).toBe('PRACTICE');
    expect(receipt.correctCount).toBe(1);
    expect(receipt.score).toBe(4);
    expect(service.submissionReceipt()).toEqual(receipt);
  });
});
