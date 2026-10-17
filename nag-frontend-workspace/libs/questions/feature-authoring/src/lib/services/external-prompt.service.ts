import { Injectable } from '@angular/core';
import { DifficultyLevel, QuestionOption, QuestionType } from '@nag-frontend-workspace/questions-data-access';

export interface QuestionPromptConfig {
  subject?: string;
  topic?: string;
  subtopic?: string;
  targetExam?: string;
  questionType: QuestionType | string;
  difficulty: DifficultyLevel | string;
  cognitiveLevel: string;
  marks?: number;
  negativeMarks?: number;
  additionalInstructions?: string;
}

export interface ImportedQuestion {
  content: string;
  explanation: string;
  type: QuestionType;
  difficulty: DifficultyLevel;
  cognitiveLevel: string;
  marks: number;
  negativeMarks: number;
  options: QuestionOption[];
  answerKey: string;
  subject?: string;
  topic?: string;
  subtopic?: string;
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
}

export interface QuestionValidationResult {
  valid: boolean;
  errors: string[];
  warnings?: string[];
  data?: ImportedQuestion;
}

@Injectable({
  providedIn: 'root',
})
export class ExternalPromptService {
  /**
   * Generates a comprehensive, pedagogical, and syntax-enforcing prompt
   * for external LLMs (ChatGPT, Claude, Gemini, DeepSeek, etc.) to produce
   * a single high-quality examination question in strict JSON format.
   */
  generateQuestionPrompt(config: QuestionPromptConfig): string {
    const subject = config.subject?.trim() || 'General Science / Mathematics';
    const topic = config.topic?.trim() || 'Core Topic';
    const subtopic = config.subtopic?.trim() || 'Key Sub-topic';
    const targetExam = config.targetExam?.trim() || 'Competitive Examination (JEE / NEET / UPSC / GATE / SSC)';
    const qType = config.questionType || 'SINGLE_MCQ';
    const difficulty = config.difficulty || 'MEDIUM';
    const cognitiveLevel = config.cognitiveLevel || 'UNDERSTAND';
    const marks = config.marks ?? 4;
    const negativeMarks = config.negativeMarks ?? 1;
    const extra = config.additionalInstructions?.trim()
      ? `\nAdditional Focus & Pedagogical Constraints:\n${config.additionalInstructions.trim()}\n`
      : '';

    return `You are an expert examination question creator and psychometrician for Indian Competitive Examinations (${targetExam}).
Your task is to generate exactly ONE high-quality examination question in strict structured JSON format for use in a computer-based testing system.

Syllabus & Target Specification:
- Target Examination: ${targetExam}
- Subject: ${subject}
- Topic: ${topic}
- Sub-Topic: ${subtopic}
- Question Type: ${qType}
- Target Difficulty: ${difficulty}
- Bloom's Cognitive Level: ${cognitiveLevel}
- Positive Marks: ${marks}
- Negative Marks: ${negativeMarks}${extra}

Mathematical & Formula Formatting Rules (STRICT REQUIREMENT):
1. ALL mathematical, physical, and chemical formulas, variables, expressions, percentages, and units in EVERY field (content, options, answerKey, explanation) MUST be enclosed in KaTeX $$...$$ syntax.
   - Correct: "$$v_f = \\sqrt{2gh}$$", "$$\\Delta K = W_{\\text{ext}}$$", "$$99.9\\%$$", "$$m = 4\\text{ kg}$$"
   - Incorrect: "\\( v_f \\)", "\\[ v_f \\]", "$v_f$", "99.9%"
2. Chemical reactions MUST use valid LaTeX \\ce syntax enclosed in $$...$$ (e.g. "$$\\ce{2H2 + O2 -> 2H2O}$$").
3. In JSON strings, ALWAYS escape all LaTeX backslashes with double backslashes: write \\\\ce, \\\\frac, \\\\times, \\\\Delta, \\\\sqrt, \\\\theta, \\\\pm, \\\\sum, \\\\int, \\\\% (never emit single unescaped backslashes).
4. For multi-line formatting in content or explanation, use standard Markdown with double newlines (\\n\\n).

Question Type Specifications:
- If SINGLE_MCQ: Provide exactly 4 options with ids "A", "B", "C", "D". Exactly ONE option MUST have "isCorrect": true, and the other three MUST have "isCorrect": false. "answerKey" must be the letter of the correct option ("A", "B", "C", or "D").
- If MULTI_MCQ: Provide exactly 4 options with ids "A", "B", "C", "D". Two or more options MUST have "isCorrect": true. "answerKey" must be the comma-separated letters of all correct options (e.g. "A,C").
- If NUMERICAL: Options must be an empty array []. "answerKey" contains the exact numerical answer or range (e.g. "12.5").
- If DESCRIPTIVE: Options must be an empty array []. "answerKey" contains the model answer key points.

CRITICAL INSTRUCTION:
Return ONLY a raw, valid JSON object matching the schema below.
Do NOT wrap the output in markdown code blocks or backticks (do NOT output \`\`\`json or \`\`\`).
Do NOT include any conversational preamble, greetings, or postscript commentary.

Expected JSON Output Schema:
{
  "content": "Clear, rigorous problem statement with $$formulas$$ and context.",
  "questionType": "${qType}",
  "difficulty": "${difficulty}",
  "cognitiveLevel": "${cognitiveLevel}",
  "marks": ${marks},
  "negativeMarks": ${negativeMarks},
  "options": [
    { "id": "A", "text": "First option with $$LaTeX$$ if needed", "isCorrect": false },
    { "id": "B", "text": "Second option with $$LaTeX$$ if needed", "isCorrect": true },
    { "id": "C", "text": "Third option with $$LaTeX$$ if needed", "isCorrect": false },
    { "id": "D", "text": "Fourth option with $$LaTeX$$ if needed", "isCorrect": false }
  ],
  "answerKey": "B",
  "explanation": "Detailed step-by-step mathematical derivation showing why option B is correct and common traps are avoided."
}`;
  }

  /**
   * Sanitizes unescaped backslashes in JSON (e.g., from unescaped LaTeX like \omega or \Delta)
   * so that JSON.parse does not fail with "Bad escaped character".
   */
  sanitizeJsonBackslashes(jsonStr: string): string {
    return jsonStr.replace(/(?<!\\)\\(?!["\\/bfnrt]|u[0-9a-fA-F]{4})/g, '\\\\');
  }

  /**
   * Validates and parses external JSON representing a single question.
   * Strips markdown fences, parses JSON, verifies required fields,
   * checks option logic, and normalizes into an ImportedQuestion object.
   */
  validateAndParseQuestionJson(rawInput: string): QuestionValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!rawInput || !rawInput.trim()) {
      return { valid: false, errors: ['JSON input is empty. Please paste valid question JSON.'] };
    }

    // Strip markdown code fences if user copied them with ``` or ```json
    let cleaned = rawInput.trim();
    if (cleaned.startsWith('```')) {
      cleaned = cleaned.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim();
    }

    let parsed: any;
    try {
      parsed = JSON.parse(cleaned);
    } catch {
      // Attempt with backslash sanitizer if direct parse fails due to LaTeX escapes
      try {
        const sanitized = this.sanitizeJsonBackslashes(cleaned);
        parsed = JSON.parse(sanitized);
      } catch (err: any) {
        return {
          valid: false,
          errors: [`Invalid JSON syntax: ${err?.message || 'Unable to parse JSON string'}`],
        };
      }
    }

    // If an array was provided with 1 item, extract it
    if (Array.isArray(parsed)) {
      if (parsed.length === 0) {
        return { valid: false, errors: ['The JSON array is empty. Exactly 1 question is required.'] };
      }
      if (parsed.length > 1) {
        warnings.push(`The JSON contains ${parsed.length} questions. Importing the first question into the editor.`);
      }
      parsed = parsed[0];
    }

    if (!parsed || typeof parsed !== 'object') {
      return { valid: false, errors: ['Parsed JSON must be an object representing a question.'] };
    }

    // 1. Validate content
    const content = typeof parsed.content === 'string' ? parsed.content.trim() : '';
    if (!content) {
      errors.push('Question "content" (problem statement) is missing or empty.');
    }

    // 2. Normalize question type
    const rawType = (parsed.questionType || parsed.type || 'SINGLE_MCQ').toString().toUpperCase().trim();
    let normalizedType: QuestionType = 'MULTIPLE_CHOICE';
    if (rawType === 'MULTI_MCQ' || rawType === 'MULTIPLE_SELECT' || rawType === 'MULTIPLE_MCQ') {
      normalizedType = 'MULTIPLE_SELECT';
    } else if (rawType === 'NUMERICAL') {
      normalizedType = 'NUMERICAL';
    } else if (rawType === 'DESCRIPTIVE' || rawType === 'TEXT') {
      normalizedType = 'DESCRIPTIVE';
    } else {
      normalizedType = 'MULTIPLE_CHOICE';
    }

    // 3. Normalize difficulty
    const rawDiff = (parsed.difficulty || 'MEDIUM').toString().toUpperCase().trim();
    const validDifficulties: DifficultyLevel[] = ['EASY', 'MEDIUM', 'HARD', 'EXPERT'];
    const difficulty: DifficultyLevel = validDifficulties.includes(rawDiff as DifficultyLevel)
      ? (rawDiff as DifficultyLevel)
      : 'MEDIUM';

    // 4. Normalize cognitive level
    const rawCog = (parsed.cognitiveLevel || 'UNDERSTAND').toString().toUpperCase().trim();
    const validCogLevels = ['REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'];
    const cognitiveLevel = validCogLevels.includes(rawCog) ? rawCog : 'UNDERSTAND';

    // 5. Marks and negative marks
    let marks = typeof parsed.marks === 'number' ? parsed.marks : 4;
    let negativeMarks = typeof parsed.negativeMarks === 'number' ? parsed.negativeMarks : 1;
    if (marks <= 0) {
      warnings.push(`Positive marks was set to ${marks}; resetting to standard 4.`);
      marks = 4;
    }
    if (negativeMarks < 0 || negativeMarks > marks) {
      warnings.push(`Negative marks was invalid (${negativeMarks}); resetting to standard 1.`);
      negativeMarks = 1;
    }

    // 6. Options validation for MCQ / MSQ
    const isChoiceQuestion = normalizedType === 'MULTIPLE_CHOICE' || normalizedType === 'MULTIPLE_SELECT';
    const rawOptions = Array.isArray(parsed.options) ? parsed.options : [];
    const normalizedOptions: QuestionOption[] = [];

    if (isChoiceQuestion) {
      if (rawOptions.length < 2) {
        errors.push(`Multiple choice questions require at least 2 options (received ${rawOptions.length}).`);
      }

      const answerKeyRaw = (parsed.answerKey || '').toString().trim().toUpperCase();
      const answerKeyTokens = answerKeyRaw
        ? answerKeyRaw.split(',').map((s: string) => s.trim())
        : [];

      for (let i = 0; i < rawOptions.length; i++) {
        const opt = rawOptions[i];
        const letter = (opt.id || String.fromCharCode(65 + i)).toString().toUpperCase().trim();
        const text = typeof opt.text === 'string' ? opt.text.trim() : (typeof opt.content === 'string' ? opt.content.trim() : '');

        if (!text) {
          errors.push(`Option ${letter} has empty text.`);
        }

        // Determine if correct from opt.isCorrect or from answerKey
        let isCorrect = Boolean(opt.isCorrect);
        if (opt.isCorrect === undefined && answerKeyTokens.length > 0) {
          isCorrect = answerKeyTokens.includes(letter);
        }

        normalizedOptions.push({
          id: letter,
          text,
          isCorrect,
        });
      }

      // Check correct answer counts
      const correctCount = normalizedOptions.filter((o) => o.isCorrect).length;
      if (normalizedType === 'MULTIPLE_CHOICE') {
        if (correctCount === 0) {
          // If no option marked correct, check if answerKey matches any option
          if (answerKeyTokens.length === 1) {
            const matched = normalizedOptions.find((o) => o.id === answerKeyTokens[0]);
            if (matched) {
              matched.isCorrect = true;
            } else {
              errors.push(`Single choice question has no correct option selected (answerKey "${answerKeyRaw}" not found in options).`);
            }
          } else {
            errors.push('Single choice question requires exactly 1 correct option (none marked correct).');
          }
        } else if (correctCount > 1) {
          errors.push(`Single choice question requires exactly 1 correct option (found ${correctCount} correct options).`);
        }
      } else if (normalizedType === 'MULTIPLE_SELECT') {
        if (correctCount === 0) {
          errors.push('Multiple select question requires at least 1 correct option.');
        }
      }
    } else {
      // Numerical or descriptive: options not required
      if (rawOptions.length > 0) {
        normalizedOptions.push(
          ...rawOptions.map((o: any, idx: number) => ({
            id: o.id || String.fromCharCode(65 + idx),
            text: o.text || o.content || '',
            isCorrect: Boolean(o.isCorrect),
          }))
        );
      }
    }

    // 7. KaTeX formula balance check
    const mathDelimiters = (content.match(/\$\$/g) || []).length;
    if (mathDelimiters % 2 !== 0) {
      warnings.push('Unbalanced $$ KaTeX delimiters detected in question problem statement.');
    }

    const explanation = typeof parsed.explanation === 'string' ? parsed.explanation.trim() : '';

    // Calculate answerKey
    const computedAnswerKey = normalizedOptions
      .filter((o) => o.isCorrect)
      .map((o) => o.id)
      .join(',') || (parsed.answerKey || '').toString().trim();

    if (errors.length > 0) {
      return { valid: false, errors, warnings };
    }

    const data: ImportedQuestion = {
      content,
      explanation,
      type: normalizedType,
      difficulty,
      cognitiveLevel,
      marks,
      negativeMarks,
      options: normalizedOptions,
      answerKey: computedAnswerKey,
      subject: parsed.subject,
      topic: parsed.topic,
      subtopic: parsed.subtopic,
      subjectId: parsed.subjectId ? Number(parsed.subjectId) : undefined,
      topicId: parsed.topicId ? Number(parsed.topicId) : undefined,
      subtopicId: parsed.subtopicId ? Number(parsed.subtopicId) : undefined,
    };

    return {
      valid: true,
      errors: [],
      warnings,
      data,
    };
  }

  /**
   * Helper to trigger download of text content as a file.
   */
  downloadFile(filename: string, content: string, mimeType = 'text/plain;charset=utf-8'): void {
    const blob = new Blob([content], { type: mimeType });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  /**
   * Helper to copy text to clipboard.
   */
  async copyToClipboard(text: string): Promise<boolean> {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
        return true;
      }
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.select();
      const success = document.execCommand('copy');
      document.body.removeChild(textArea);
      return success;
    } catch {
      return false;
    }
  }
}
