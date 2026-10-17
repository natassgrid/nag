import { adaptToReviewItem } from './micro-lesson.model';
import {
  parseLessonForAuthoring,
  buildCreateOrUpdateRequest,
} from '../components/micro-lesson-authoring/models/micro-lesson-authoring.model';

describe('MicroLesson Model Formula Colon Parsing', () => {
  it('adaptToReviewItem should not split on colon inside LaTeX math delimiters', () => {
    const raw = {
      id: 'test-1',
      subject: 'Physics',
      topic: 'Mechanics',
      subtopic: 'Units & Dimensions',
      cheatSheet: [
        '$$\\text{Relation 2: Verify dimensional homogeneity and boundary limits}$$',
        "Ohm's Law: $$V = IR$$",
        '$$E = mc^2$$',
      ],
    };

    const adapted = adaptToReviewItem(raw);
    const formulas = adapted.content.formulaEntries;
    expect(formulas).toBeDefined();
    expect(formulas?.length).toBe(3);

    // Entry 1: Colon inside LaTeX math command
    expect(formulas![0].name).toBe('Formula 1');
    expect(formulas![0].latex).toBe(
      '$$\\text{Relation 2: Verify dimensional homogeneity and boundary limits}$$'
    );

    // Entry 2: Colon separating formula name from LaTeX
    expect(formulas![1].name).toBe("Ohm's Law");
    expect(formulas![1].latex).toBe('$$V = IR$$');

    // Entry 3: Standalone formula
    expect(formulas![2].name).toBe('Formula 3');
    expect(formulas![2].latex).toBe('$$E = mc^2$$');
  });

  it('parseLessonForAuthoring should not split on colon inside LaTeX math delimiters', () => {
    const rawLesson = adaptToReviewItem({
      id: 'test-2',
      content: {
        formulaCheatSheet:
          '$$\\text{Relation 2: Verify dimensional homogeneity and boundary limits}$$\n\nFaraday: $$\\mathcal{E} = -\\frac{d\\Phi_B}{dt}$$',
      },
    });

    const parsed = parseLessonForAuthoring(rawLesson);
    expect(parsed.formulaEntries.length).toBe(2);

    expect(parsed.formulaEntries[0].name).toBe('Formula 1');
    expect(parsed.formulaEntries[0].latex).toBe(
      '$$\\text{Relation 2: Verify dimensional homogeneity and boundary limits}$$'
    );

    expect(parsed.formulaEntries[1].name).toBe('Faraday');
    expect(parsed.formulaEntries[1].latex).toBe('$$\\mathcal{E} = -\\frac{d\\Phi_B}{dt}$$');
  });

  it('buildCreateOrUpdateRequest should omit generic Formula N: prefix in cheatSheet', () => {
    const req = buildCreateOrUpdateRequest({
      subjectId: 1,
      subjectName: 'Physics',
      topicId: 1,
      topicName: 'Mechanics',
      subtopicId: 1,
      subtopicName: 'Kinematics',
      targetExam: 'JEE_MAIN',
      status: 'APPROVED',
      coreConcept: 'Test concept',
      trapAlert: {
        title: 'Trap',
        description: 'Trap desc',
      },
      formulaEntries: [
        {
          name: 'Formula 1',
          latex: '$$\\text{Relation 2: Verify dimensional homogeneity}$$',
          description: '',
        },
        {
          name: "Newton's 2nd",
          latex: '$$F = ma$$',
          description: '',
        },
      ],
      problemStatement: 'Problem',
      steps: [],
      finalAnswer: 'Answer',
      keyTakeaways: [],
      quickCheckItems: [],
      reviewerNotes: '',
    });

    expect(req.cheatSheet).toEqual([
      '$$\\text{Relation 2: Verify dimensional homogeneity}$$',
      "Newton's 2nd: $$F = ma$$",
    ]);
  });
});
