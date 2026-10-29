export type FormulaApplicabilityMode =
  | 'MANDATORY'
  | 'OPTIONAL_NUMERICAL'
  | 'OPTIONAL_NOT_APPLICABLE';

export interface SubjectTopicFormulaPolicy {
  mode: FormulaApplicabilityMode;
  required: boolean;
  badgeLabel: string;
  reason: string;
}

const MANDATORY_SUBJECT_PATTERNS = [
  'mathematics',
  'math',
  'quantitative aptitude',
  'numerical ability',
  'arithmetic',
  'general science',
  'physics',
  'chemistry',
  'statistics',
  'data interpretation',
];

const MANDATORY_NUMERICAL_TOPIC_PATTERNS = [
  'numerical operation',
  'number series',
  'symbolic operation',
  'mathematical step operation',
  'inequalit',
  'everyday science',
  'physics',
  'chemistry',
  'probability',
  'permutation',
  'combination',
  'mensuration',
  'trigonometry',
  'calculus',
  'algebra',
  'geometry',
  'arithmetic',
  'time and work',
  'time, speed',
  'profit and loss',
  'simple and compound interest',
  'ratio and proportion',
  'percentage',
  'data interpretation',
  'tabular analysis',
  'bar & line',
  'pie chart',
  'caselet',
];

const OPTIONAL_NUMERICAL_TOPIC_PATTERNS = [
  // Banking & Economics quantitative topics
  'monetary policy',
  'repo',
  'crr',
  'slr',
  'capital adequacy',
  'basel',
  'crar',
  'bond',
  'yield',
  'priority sector',
  'national income',
  'gdp',
  'inflation',
  'fiscal',
  'tax',
  'economic',
  // Computer Science quantitative/formal topics
  'algorithm',
  'complexity',
  'big-o',
  'asymptotic',
  'computer organization',
  'architecture',
  'cpu',
  'pipelin',
  'cache',
  'memory',
  'operating system',
  'scheduling',
  'paging',
  'network',
  'subnet',
  'cidr',
  'boolean',
  'logic gate',
  'relational algebra',
  // Ecology & Biology quantitative topics
  'ecosystem dynamics',
  'energy flow',
  'lindeman',
  'trophic',
  'genetics',
  'hardy-weinberg',
  'mendel',
  'population',
  'cardiac',
  'respiration',
  'photosynthesis',
  // Reasoning symbolic/formal topics
  'coding and decoding',
  'coding-decoding',
  'venn diagram',
  'syllogism',
  'statement and conclusion',
  'direction',
  'distance',
  'clock',
  'calendar',
  'dice',
  'cube',
  'ranking',
  'blood relation',
  'series',
  // Hindi Sandhi / Geography / CDP metrics
  'संधि',
  'sandhi',
  'समास',
  'latitude',
  'longitude',
  'time zone',
  'iq',
  'intelligence',
];

const NON_APPLICABLE_SUBJECT_PATTERNS = [
  'english',
  'general hindi',
  'hindi',
  'public administration',
  'governance',
  'child development',
  'pedagogy',
  'history',
  'polity',
  'geography',
  'reasoning',
  'general studies',
  'general awareness',
  'banking',
  'environmental',
  'ecology',
  'computer science',
  'biology',
  'life science',
];

/**
 * Classifies whether Part 3 (KaTeX Formula Cheat Sheet Builder) is mandatory,
 * optional-numerical, or optional-not-applicable based on the selected subject,
 * topic, subtopic, and lesson type.
 */
export function resolveFormulaApplicability(
  subjectName?: string | null,
  topicName?: string | null,
  subtopicName?: string | null,
  lessonType?: string | null
): SubjectTopicFormulaPolicy {
  const subj = (subjectName || '').trim().toLowerCase();
  const top = (topicName || '').trim().toLowerCase();
  const sub = (subtopicName || '').trim().toLowerCase();
  const type = (lessonType || '').trim().toUpperCase();
  const combinedTopic = `${top} ${sub}`.trim();

  if (type === 'CHEAT_SHEET') {
    return {
      mode: 'MANDATORY',
      required: true,
      badgeLabel: 'Required (Formula Cheat Sheet Lesson)',
      reason:
        'Part 3 is required because the lesson type is explicitly set to Formula Cheat Sheet (CHEAT_SHEET).',
    };
  }

  // Biology under General Science or standalone Biology is optional unless numerical topic
  const isBiologyTopic =
    subj.includes('biology') ||
    subj.includes('life science') ||
    top.includes('biology') ||
    top.includes('life science') ||
    top.includes('botany') ||
    top.includes('zoology');

  if (!isBiologyTopic) {
    const isMandatorySubject = MANDATORY_SUBJECT_PATTERNS.some((p) => subj.includes(p));
    if (isMandatorySubject) {
      return {
        mode: 'MANDATORY',
        required: true,
        badgeLabel: 'Required (Math / Science / Numerical)',
        reason: `Part 3 KaTeX Formula Cheat Sheet is mandatory for ${subjectName || 'Mathematics, Science & Numerical'} lessons.`,
      };
    }
  }

  const isMandatoryNumericalTopic = MANDATORY_NUMERICAL_TOPIC_PATTERNS.some((p) =>
    combinedTopic.includes(p)
  );
  if (isMandatoryNumericalTopic && !isBiologyTopic) {
    return {
      mode: 'MANDATORY',
      required: true,
      badgeLabel: 'Required (Numerical / Science Topic)',
      reason: `Part 3 KaTeX Formula Cheat Sheet is mandatory for numerical/scientific topic "${topicName || subtopicName}".`,
    };
  }

  const isOptionalNumericalTopic = OPTIONAL_NUMERICAL_TOPIC_PATTERNS.some((p) =>
    combinedTopic.includes(p)
  );
  if (isOptionalNumericalTopic || isBiologyTopic) {
    return {
      mode: 'OPTIONAL_NUMERICAL',
      required: false,
      badgeLabel: 'Optional (Symbolic / Quantitative Topic)',
      reason: `Part 3 is optional for ${subjectName || 'this subject'} (${topicName || 'this topic'}). Add KaTeX equations or symbolic rules only if helpful, or skip Part 3.`,
    };
  }

  if (subj && NON_APPLICABLE_SUBJECT_PATTERNS.some((p) => subj.includes(p))) {
    return {
      mode: 'OPTIONAL_NOT_APPLICABLE',
      required: false,
      badgeLabel: 'Optional — Not Applicable for Subject',
      reason: `KaTeX formulas are not applicable for ${subjectName}${topicName ? ' — ' + topicName : ''}. Part 3 can be safely left empty and will be hidden from candidates.`,
    };
  }

  if (!subj) {
    return {
      mode: 'MANDATORY',
      required: true,
      badgeLabel: 'Required (Math / Science / Numerical)',
      reason: 'Part 3 is required for Mathematics, Science, and Numerical topics, and optional for non-formulaic subjects.',
    };
  }

  return {
    mode: 'OPTIONAL_NOT_APPLICABLE',
    required: false,
    badgeLabel: 'Optional for Non-Math/Science Subject',
    reason: `Part 3 is optional for ${subjectName}. Include KaTeX formulas only if the topic involves mathematical or scientific expressions.`,
  };
}
