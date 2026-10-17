import { Injectable } from '@angular/core';
import {
  FormulaEntry,
  MicroLessonStep,
  QuickCheckItem,
  TrapAlert,
} from '../models/micro-lesson.model';
import { ParsedAuthoringLessonState } from '../components/micro-lesson-authoring/models';

export interface MicroLessonPromptConfig {
  subject?: string;
  topic?: string;
  subtopic?: string;
  targetExam?: string;
  referenceText?: string;
}

export interface MicroLessonValidationResult {
  valid: boolean;
  errors: string[];
  warnings?: string[];
  data?: ParsedAuthoringLessonState;
}

@Injectable({
  providedIn: 'root',
})
export class ExternalMicroLessonPromptService {
  /**
   * Generates a pedagogical, KaTeX-enforcing prompt for external LLMs
   * to produce a high-impact remedial micro-lesson in strict JSON format.
   */
  generateMicroLessonPrompt(config: MicroLessonPromptConfig): string {
    const subject = config.subject?.trim() || 'Physics';
    const topic = config.topic?.trim() || 'Classical Mechanics';
    const subtopic = config.subtopic?.trim() || 'Work-Energy Theorem';
    const targetExam = config.targetExam?.trim() || 'JEE Main & Advanced (JEE_MAIN)';
    const ref = config.referenceText?.trim()
      ? `\nReference Focus & Diagnosis Context:\n${config.referenceText.trim()}\n`
      : '';

    return `You are an expert master faculty and pedagogical content designer for Indian Competitive Examinations (${targetExam}).
Your task is to produce exactly ONE high-impact, modular remedial micro-lesson designed to cure conceptual gaps diagnosed during mock test evaluation.

Syllabus & Target Specification:
- Target Examination: ${targetExam}
- Subject: ${subject}
- Topic: ${topic}
- Sub-Topic: ${subtopic}${ref}

Pedagogical Architecture Guidelines:
1. Core Concept: Must be explained in exactly 3 simple, lucid sentences using native Indian contexts or everyday relatable analogies (e.g., flywheel in an atta chakki, steering a loaded bicycle, tea kettle steam, railway track expansion, monsoon rain runoffs).
2. The Trap (trapAlert): Directly highlight the exact psychological, sign convention, or boundary condition trap test-setters deliberately insert to mislead candidates.
3. The Cheat Sheet (formulaEntries): Provide 2 to 3 indispensable mathematical formulas or core frameworks with KaTeX notation.
4. Step-by-Step Breakdown: Present one standard medium-difficulty exam problem deconstructed into explicit numbered deduction steps (Step 1, Step 2, Step 3), followed by the unambiguous final answer with proper physical/mathematical units.
5. Key Takeaways: 2 to 3 concise, memorable summary rules.
6. Quick Check Items: 1 to 2 diagnostic concept checks with options, correct answer, and explanation.

Mathematical & Formula Formatting Rules (STRICT REQUIREMENT):
1. ALL mathematical, physical, and chemical formulas, variables, expressions, percentages, and unit notations in EVERY field (coreConcept, trapAlert, formulaEntries, problemStatement, steps, finalAnswer) MUST be enclosed in KaTeX $$...$$ syntax.
   - Correct: "$$W_{\\text{all}} = \\Delta K$$", "$$v_f = 5\\sqrt{2}\\text{ m/s}$$", "$$4.025\\%$$"
   - Incorrect: "\\( W \\)", "$W$", "4.025%"
2. Chemical reactions MUST use valid LaTeX \\ce syntax enclosed in $$...$$ (e.g. "$$\\ce{2H2 + O2 -> 2H2O}$$").
3. In JSON strings, ALWAYS escape all LaTeX backslashes with double backslashes: write \\\\ce, \\\\frac, \\\\times, \\\\Delta, \\\\sqrt, \\\\theta, \\\\pm, \\\\sum, \\\\int, \\\\% (never emit single unescaped backslashes).

CRITICAL INSTRUCTION:
Return ONLY a raw, valid JSON object matching the schema below.
Do NOT wrap the output in markdown code blocks or backticks (do NOT output \`\`\`json or \`\`\`).
Do NOT include any conversational preamble, greetings, or postscript commentary.

Expected JSON Output Schema:
{
  "targetExam": "${targetExam}",
  "subtopicCustomName": "${subtopic}",
  "coreConcept": "3 simple sentences with native Indian analogy and $$formula$$ notations specific to the subtopic.",
  "trapAlert": {
    "title": "Sign Convention & Reference Frame Pitfall",
    "description": "Candidates frequently apply formulas without verifying reference frame conditions.",
    "commonMistake": "Neglecting pseudo-forces in accelerating frames.",
    "correctApproach": "Draw the complete free-body diagram and include $$-m\\vec{a}_{\\text{frame}}$$."
  },
  "formulaEntries": [
    {
      "name": "Work-Energy Theorem",
      "latex": "$$W_{\\text{all}} = \\Delta K = \\frac{1}{2}mv_f^2 - \\frac{1}{2}mv_i^2$$",
      "description": "Net work of all forces equals change in kinetic energy.",
      "variables": ["W_all: Total work", "m: Mass", "v_f: Final velocity"]
    }
  ],
  "stepByStepBreakdown": {
    "problemStatement": "A block of mass $$m = 4\\text{ kg}$$ is pushed across a rough horizontal surface ($$\\mu_k = 0.25$$) by a force $$F = 30\\text{ N}$$ over $$d = 5\\text{ m}$$. Find its final speed starting from rest.",
    "steps": [
      {
        "stepNumber": 1,
        "title": "Calculate Net Work Done",
        "explanation": "Compute the work done by applied force and frictional opposing force: $$W_{\\text{net}} = (F - f_k)d$$.",
        "formula": "$$f_k = \\mu_k mg = 0.25 \\times 4 \\times 10 = 10\\text{ N}$$",
        "tip": "Always check whether normal force is altered by any vertical component."
      },
      {
        "stepNumber": 2,
        "title": "Apply Work-Energy Theorem to Solve Velocity",
        "explanation": "Equate net work to change in kinetic energy: $$W_{\\text{net}} = \\frac{1}{2}mv_f^2$$.",
        "formula": "$$v_f = \\sqrt{\\frac{2W_{\\text{net}}}{m}} = \\sqrt{\\frac{2(100)}{4}} = 5\\sqrt{2}\\text{ m/s}$$",
        "tip": "Confirm units match standard SI meters per second."
      }
    ],
    "finalAnswer": "$$v_f = 5\\sqrt{2}\\text{ m/s} \\approx 7.07\\text{ m/s}$$"
  },
  "keyTakeaways": [
    "Include both conservative and non-conservative work terms in $$W_{\\text{all}} = \\Delta K$$.",
    "Verify whether the frame of reference is inertial before applying Newton\'s laws."
  ],
  "quickCheckItems": [
    {
      "question": "Does the Work-Energy Theorem hold when non-conservative friction is present?",
      "options": [
        "Yes, provided friction work is included in $$W_{\\text{all}}$$",
        "No, only for conservative fields",
        "Only when net acceleration is zero",
        "Only in vacuum"
      ],
      "answer": "Yes, provided friction work is included in $$W_{\\text{all}}$$",
      "explanation": "The theorem $$W_{\\text{all}} = \\Delta K$$ is universally valid when work of all forces is summed."
    }
  ]
}`;
  }

  /**
   * Sanitizes unescaped backslashes in JSON so that JSON.parse does not fail
   * when external models emit single-escaped LaTeX strings like \omega or \Delta.
   */
  sanitizeJsonBackslashes(jsonStr: string): string {
    return jsonStr.replace(/(?<!\\)\\(?!["\\/bfnrt]|u[0-9a-fA-F]{4})/g, '\\\\');
  }

  /**
   * Validates and parses external JSON representing a single remedial micro-lesson.
   * Tolerates markdown code fences, unescaped LaTeX backslashes, flat vs nested formats,
   * validates required sections, and normalizes into a ParsedAuthoringLessonState object.
   */
  validateAndParseMicroLessonJson(rawInput: string): MicroLessonValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!rawInput || !rawInput.trim()) {
      return { valid: false, errors: ['JSON input is empty. Please paste valid micro-lesson JSON.'] };
    }

    let cleaned = rawInput.trim();
    if (cleaned.startsWith('```')) {
      cleaned = cleaned.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim();
    }

    let parsed: any;
    try {
      parsed = JSON.parse(cleaned);
    } catch {
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

    // Unwrap array of 1 item if applicable
    if (Array.isArray(parsed)) {
      if (parsed.length === 0) {
        return { valid: false, errors: ['The JSON array is empty. Exactly 1 micro-lesson is required.'] };
      }
      if (parsed.length > 1) {
        warnings.push(`The JSON contains ${parsed.length} items. Importing the first micro-lesson into the studio.`);
      }
      parsed = parsed[0];
    }

    if (!parsed || typeof parsed !== 'object') {
      return { valid: false, errors: ['Parsed JSON must be an object representing a micro-lesson.'] };
    }

    // If wrapped in { "content": { ... } }, unwrap
    const contentObj = parsed.content && typeof parsed.content === 'object' ? parsed.content : parsed;

    // 1. Core Concept validation
    const coreConcept = (contentObj.coreConcept || parsed.coreConcept || '').toString().trim();
    if (!coreConcept) {
      errors.push('Micro-Lesson "coreConcept" is required and cannot be empty.');
    }

    // 2. Trap Alert validation
    let trapAlert: TrapAlert = {
      title: 'Exam Trap & Pitfall',
      description: '',
      commonMistake: '',
      correctApproach: '',
    };

    const rawTrap = contentObj.structuredTrapAlert || contentObj.trapAlert || parsed.trapAlert || contentObj.trap || parsed.trap;
    if (typeof rawTrap === 'string') {
      trapAlert.description = rawTrap.trim();
    } else if (rawTrap && typeof rawTrap === 'object') {
      trapAlert = {
        title: (rawTrap.title || 'Exam Trap & Pitfall').toString().trim(),
        description: (rawTrap.description || rawTrap.trap || '').toString().trim(),
        commonMistake: (rawTrap.commonMistake || '').toString().trim(),
        correctApproach: (rawTrap.correctApproach || '').toString().trim(),
      };
    }

    if (!trapAlert.description) {
      errors.push('Micro-Lesson "trapAlert" (or "trap") description is required.');
    }

    // 3. Formula Entries validation (handles formulaEntries array, cheatSheet string array, or formulaCheatSheet string)
    let formulaEntries: FormulaEntry[] = [];
    const rawFormulas = contentObj.formulaEntries || parsed.formulaEntries;
    const rawCheatSheet = contentObj.cheatSheet || parsed.cheatSheet || contentObj.formulaCheatSheet || parsed.formulaCheatSheet;

    if (Array.isArray(rawFormulas) && rawFormulas.length > 0) {
      formulaEntries = rawFormulas.map((f: any, idx: number) => ({
        name: (f.name || `Formula ${idx + 1}`).toString().trim(),
        latex: (f.latex || f.formula || '').toString().trim(),
        description: (f.description || '').toString().trim(),
        variables: Array.isArray(f.variables) ? f.variables.map(String) : [],
      }));
    } else if (Array.isArray(rawCheatSheet) && rawCheatSheet.length > 0) {
      formulaEntries = rawCheatSheet.map((item: any, idx: number) => {
        if (typeof item === 'string') {
          return {
            name: `Formula ${idx + 1}`,
            latex: item.trim(),
            description: '',
            variables: [],
          };
        }
        return {
          name: (item.name || `Formula ${idx + 1}`).toString().trim(),
          latex: (item.latex || item.formula || '').toString().trim(),
          description: (item.description || '').toString().trim(),
          variables: Array.isArray(item.variables) ? item.variables.map(String) : [],
        };
      });
    } else if (typeof rawCheatSheet === 'string' && rawCheatSheet.trim()) {
      const blocks = rawCheatSheet.split('\n\n').map((s) => s.trim()).filter(Boolean);
      formulaEntries = blocks.map((b, idx) => ({
        name: `Formula ${idx + 1}`,
        latex: b,
        description: '',
        variables: [],
      }));
    }

    if (formulaEntries.length === 0) {
      warnings.push('No formula cheat sheet entries were found in the JSON. Initialized with an empty formula slot.');
      formulaEntries = [{ name: 'Formula 1', latex: '', description: '', variables: [] }];
    }

    // 4. Step-by-Step Breakdown validation
    const bd = contentObj.stepByStepBreakdown || parsed.stepByStepBreakdown || {};
    const problemStatement = (bd.problemStatement || bd.questionStem || '').toString().trim();
    const finalAnswer = (bd.finalAnswer || '').toString().trim();
    const rawSteps = Array.isArray(bd.steps) ? bd.steps : [];

    if (!problemStatement && rawSteps.length === 0) {
      errors.push('Micro-Lesson "stepByStepBreakdown" is missing problem statement or steps.');
    }

    const steps: MicroLessonStep[] = rawSteps.map((s: any, idx: number) => {
      if (typeof s === 'string') {
        return {
          stepNumber: idx + 1,
          title: `Step ${idx + 1}`,
          explanation: s.trim(),
          formula: '',
          tip: '',
        };
      }
      return {
        stepNumber: s.stepNumber || idx + 1,
        title: (s.title || `Step ${idx + 1}`).toString().trim(),
        explanation: (s.explanation || '').toString().trim(),
        formula: (s.formula || '').toString().trim(),
        tip: (s.tip || '').toString().trim(),
      };
    });

    if (steps.length === 0) {
      steps.push({ stepNumber: 1, title: 'Step 1', explanation: '', formula: '', tip: '' });
    }

    // 5. Key takeaways
    const rawTakeaways = contentObj.keyTakeaways || parsed.keyTakeaways;
    const keyTakeaways: string[] = Array.isArray(rawTakeaways) && rawTakeaways.length > 0
      ? rawTakeaways.map(String)
      : [''];

    // 6. Quick check items
    const rawQuickChecks = contentObj.quickCheckItems || parsed.quickCheckItems;
    const quickCheckItems: QuickCheckItem[] = Array.isArray(rawQuickChecks) && rawQuickChecks.length > 0
      ? rawQuickChecks.map((qc: any) => ({
          question: (qc.question || '').toString().trim(),
          options: Array.isArray(qc.options) ? qc.options.map(String) : [],
          answer: (qc.answer || qc.correctAnswer || '').toString().trim(),
          correctAnswer: (qc.correctAnswer || qc.answer || '').toString().trim(),
          explanation: (qc.explanation || '').toString().trim(),
        }))
      : [{ question: '', options: [], answer: '', correctAnswer: '', explanation: '' }];

    if (errors.length > 0) {
      return { valid: false, errors, warnings };
    }

    const targetExam = (parsed.targetExam || contentObj.targetExam || 'JEE_MAIN').toString().trim();
    const subtopicCustomName = (parsed.subtopicCustomName || parsed.subtopicName || parsed.subtopic || '').toString().trim();
    const status = (parsed.status || 'APPROVED').toString().trim();
    const reviewerNotes = (parsed.reviewerNotes || '').toString().trim();

    const data: ParsedAuthoringLessonState = {
      targetExam,
      status,
      subtopicCustomName,
      coreConcept,
      trapAlert,
      formulaEntries,
      problemStatement,
      steps,
      finalAnswer,
      keyTakeaways,
      quickCheckItems,
      reviewerNotes,
    };

    return {
      valid: true,
      errors: [],
      warnings,
      data,
    };
  }

  /**
   * Helper to download file.
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
