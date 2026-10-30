import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  SubtopicMicroLesson,
  FormulaEntry,
  StepItem,
  StepByStepBreakdown,
  TrapAlert,
  QuickCheckItem,
  LessonSlide,
  MicroTutorialApproach,
  LessonType,
  LessonDifficultyLevel,
  extractStepsFromLesson,
  extractProblemStatementFromLesson,
  parseFormulaEntries,
  parseTrapAlert,
  parseStepBreakdown,
} from '../models/micro-lesson.model';

export interface MicroLessonQueryParams {
  targetExam?: string;
  subtopicName?: string;
  topicName?: string;
  subjectName?: string;
  lang?: string;
  lessonType?: string;
  difficulty?: string;
}

@Injectable({ providedIn: 'root' })
export class MicroLessonService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/micro-lessons';

  getPublishedMicroLesson(
    subtopicId: number,
    paramsOrTargetExam?: string | MicroLessonQueryParams
  ): Observable<SubtopicMicroLesson> {
    const params = this.buildParams(paramsOrTargetExam);
    return this.http
      .get<any>(`${this.apiUrl}/subtopic/${subtopicId}`, { params })
      .pipe(
        map((raw) => {
          if (Array.isArray(raw)) {
            return this.normalizeMicroLesson(raw[0]);
          }
          return this.normalizeMicroLesson(raw);
        })
      );
  }

  getMicroLessonsBySubtopic(
    subtopicId: number,
    paramsOrTargetExam?: string | MicroLessonQueryParams
  ): Observable<SubtopicMicroLesson[]> {
    const params = this.buildParams(paramsOrTargetExam);

    return this.http
      .get<any>(`${this.apiUrl}/subtopic/${subtopicId}`, { params })
      .pipe(
        map((raw) => {
          if (Array.isArray(raw)) {
            return raw.map((item) => this.normalizeMicroLesson(item));
          }
          const single = this.normalizeMicroLesson(raw);
          return [single];
        })
      );
  }

  getMicroLessonById(id: string): Observable<SubtopicMicroLesson> {
    return this.http
      .get<any>(`${this.apiUrl}/${id}`)
      .pipe(map((raw) => this.normalizeMicroLesson(raw)));
  }

  getMicroLessonBySubtopicAndApproach(
    subtopicId: number,
    lessonType?: string,
    difficulty?: string,
    targetExam?: string
  ): Observable<SubtopicMicroLesson> {
    const query: MicroLessonQueryParams = {};
    if (lessonType) query.lessonType = lessonType;
    if (difficulty) query.difficulty = difficulty;
    if (targetExam) query.targetExam = targetExam;

    const params = this.buildParams(query);
    return this.http
      .get<any>(`${this.apiUrl}/subtopic/${subtopicId}/approach`, { params })
      .pipe(map((raw) => this.normalizeMicroLesson(raw)));
  }

  getMicroLessonVariants(
    lessonId: string
  ): Observable<SubtopicMicroLesson[]> {
    return this.http
      .get<any[]>(`${this.apiUrl}/${lessonId}/variants`)
      .pipe(
        map((rawList) =>
          Array.isArray(rawList)
            ? rawList.map((item) => this.normalizeMicroLesson(item))
            : []
        )
      );
  }

  private buildParams(
    paramsOrTargetExam?: string | MicroLessonQueryParams
  ): HttpParams {
    let params = new HttpParams();
    if (!paramsOrTargetExam) return params;

    if (typeof paramsOrTargetExam === 'string') {
      params = params.set('targetExam', paramsOrTargetExam);
    } else {
      const q = paramsOrTargetExam;
      if (q.targetExam) params = params.set('targetExam', q.targetExam);
      if (q.subtopicName) params = params.set('subtopicName', q.subtopicName);
      if (q.topicName) params = params.set('topicName', q.topicName);
      if (q.subjectName) params = params.set('subjectName', q.subjectName);
      if (q.lang) params = params.set('lang', q.lang);
      if (q.lessonType) params = params.set('lessonType', q.lessonType);
      if (q.difficulty) params = params.set('difficulty', q.difficulty);
    }
    return params;
  }

  getMicroLessonByLanguage(
    lessonId: string,
    lang: string
  ): Observable<SubtopicMicroLesson> {
    return this.http
      .get<any>(`/api/v1/microlessons/${lessonId}/language/${lang}`)
      .pipe(map((raw) => this.normalizeMicroLesson(raw)));
  }

  normalizeMicroLesson(raw: any): SubtopicMicroLesson {
    if (!raw) {
      throw new Error('Empty micro-lesson payload');
    }

    const rawContent = raw.content || {};

    // 1. Core concept
    const coreConcept: string = rawContent.coreConcept || raw.coreConcept || '';

    // 2. Trap alert normalization via shared utility
    const { trapAlert, structuredTrapAlert } = parseTrapAlert(
      rawContent.trapAlert || raw.trapAlert || raw.trap,
      rawContent.stepByStepBreakdown?.structuredTrapAlert || raw.stepByStepBreakdown?.structuredTrapAlert
    );

    // 3. Formula Cheat Sheet normalization via shared utility
    const { formulaCheatSheet, formulaEntries } = parseFormulaEntries(
      rawContent.formulaCheatSheet || raw.formulaCheatSheet,
      rawContent.cheatSheet || raw.cheatSheet
    );

    // 4. Step-by-Step Breakdown normalization via shared utility
    const rawBreakdown = rawContent.stepByStepBreakdown || raw.stepByStepBreakdown;
    const breakdown = parseStepBreakdown(
      rawBreakdown,
      rawContent.problemStatement || raw.problemStatement,
      rawContent.questionStem || raw.questionStem,
      rawContent.finalAnswer || raw.finalAnswer
    );

    // 5. Key takeaways
    let keyTakeaways: string[] = rawContent.keyTakeaways || [];
    if (!keyTakeaways || keyTakeaways.length === 0) {
      if (coreConcept) {
        const parts = coreConcept
          .split(/\n+/)
          .map((p: string) => p.trim())
          .filter((p: string) => p.length > 0);
        keyTakeaways = parts.slice(0, 3);
        const trapDesc =
          structuredTrapAlert?.description ||
          (typeof trapAlert === 'string' ? trapAlert : '') ||
          '';
        if (trapDesc) {
          keyTakeaways.push(`Watch out: ${trapDesc}`);
        }
      }
    }

    // 6. Quick check items
    const rawQuickCheck: any[] = Array.isArray(rawContent.quickCheckItems)
      ? rawContent.quickCheckItems
      : Array.isArray(raw.quickCheckItems)
      ? raw.quickCheckItems
      : [];

    let quickCheckItems: QuickCheckItem[] = rawQuickCheck
      .filter((item) => item && item.question)
      .map((item) => {
        const ans = (item.correctAnswer || item.answer || 'Verified solution').trim();
        const opts: string[] =
          Array.isArray(item.options) && item.options.length >= 2
            ? item.options
            : [
                ans,
                'Applies only when boundary conditions are zero',
                'Inversely proportional to the governing parameter',
                'Cannot be determined from given constraints',
              ];
        return {
          question: item.question,
          options: opts,
          correctAnswer: ans,
          answer: ans,
          explanation: item.explanation || `Correct Answer: ${ans}`,
        };
      });

    if (quickCheckItems.length === 0 && rawBreakdown?.questionStem) {
      const ans = rawBreakdown.finalAnswer || 'See step-by-step breakdown above.';
      quickCheckItems = [
        {
          question: rawBreakdown.questionStem,
          options: [
            ans,
            'Zero under standard equilibrium conditions',
            'Twice the initial reference value',
            'Insufficient information to evaluate',
          ],
          correctAnswer: ans,
          answer: ans,
          explanation: Array.isArray(rawBreakdown.steps)
            ? rawBreakdown.steps
                .map((s: any) => (typeof s === 'string' ? s : s.explanation))
                .join(' ')
            : undefined,
        },
      ];
    }

    const lessonType = raw.lessonType || 'CONCEPT';
    const sequenceNo = raw.sequenceNo != null ? Number(raw.sequenceNo) : 1;
    const subtopicName = raw.subtopicName || raw.subtopic || '';
    const approachName = raw.approachName || raw.title || `Approach ${sequenceNo}`;
    const title = raw.title || raw.approachName || `Approach ${sequenceNo}`;

    return {
      id: raw.id || '',
      subtopicId: raw.subtopicId != null ? Number(raw.subtopicId) : 0,
      subtopicName,
      topicName: raw.topicName || raw.topic || '',
      subjectName: raw.subjectName || raw.subject || '',
      targetExam: raw.targetExam || '',
      title,
      approachName,
      sequenceNo,
      estimatedMinutes: raw.estimatedMinutes != null ? Number(raw.estimatedMinutes) : 5,
      status: raw.status || 'PUBLISHED',
      contentVersion:
        raw.contentVersion != null ? Number(raw.contentVersion) : 1,
      languageCode: raw.languageCode || 'en',
      fallbackToEnglish: !!raw.fallbackToEnglish,
      lessonType,
      difficultyLevel: this.normalizeDifficultyLevel(raw.difficultyLevel),
      mediaHook: raw.mediaHook,
      content: {
        coreConcept,
        trapAlert,
        structuredTrapAlert,
        formulaCheatSheet,
        formulaEntries,
        stepByStepBreakdown: breakdown,
        keyTakeaways,
        quickCheckItems,
      },
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }

  buildSlidesForLesson(lesson: SubtopicMicroLesson): LessonSlide[] {
    const subtopic = lesson.subtopicName || 'Micro-Tutorial';
    const subject = lesson.subjectName || 'Subject';
    const topic = lesson.topicName || 'Topic';
    const approach = lesson.approachName || this.deriveApproachLabel(lesson.lessonType || 'CONCEPT');

    const trapText =
      lesson.content.structuredTrapAlert?.description ||
      (typeof lesson.content.trapAlert === 'object' && lesson.content.trapAlert
        ? (lesson.content.trapAlert as any).description
        : typeof lesson.content.trapAlert === 'string'
        ? lesson.content.trapAlert
        : '');

    const formulaCount = Array.isArray(lesson.content.formulaCheatSheet)
      ? lesson.content.formulaCheatSheet.length
      : lesson.content.formulaCheatSheet
      ? 1
      : 0;

    const stepCount = extractStepsFromLesson(lesson).length;
    const problemStmt = extractProblemStatementFromLesson(lesson);
    const quizCount = lesson.content.quickCheckItems?.length || 1;

    return [
      {
        id: `${lesson.id || 'lesson'}-slide-intro`,
        index: 0,
        type: 'INTRO',
        title: subtopic,
        subtitle: `${subject} • ${topic} (${approach})`,
        icon: 'school',
        durationSeconds: 6,
        narrationText: `Welcome to the interactive tutorial on ${subtopic} under ${subject}, ${topic}. Current approach: ${approach}.`,
        mediaHook: { avatarMood: 'SPEAKING', animationPreset: 'zoom' },
      },
      {
        id: `${lesson.id || 'lesson'}-slide-concept`,
        index: 1,
        type: 'CONCEPT',
        title: 'Core Concept & Exam Trap',
        subtitle: 'Foundational principle and negative-marking pitfalls',
        icon: 'lightbulb',
        durationSeconds: 10,
        narrationText: this.stripLatexForSpeech(
          `Core Concept: ${lesson.content.coreConcept || ''}. Watch out for the exam trap: ${trapText}`
        ),
        mediaHook: { avatarMood: 'SPEAKING', animationPreset: 'slide' },
      },
      {
        id: `${lesson.id || 'lesson'}-slide-formula`,
        index: 2,
        type: 'FORMULA',
        title: 'Formula & Identity Cheat Sheet',
        subtitle: `${formulaCount} governing identity card${formulaCount === 1 ? '' : 's'}`,
        icon: 'functions',
        durationSeconds: 8,
        narrationText: this.stripLatexForSpeech(
          `Review the key formulas and governing relations for ${subtopic}.`
        ),
        mediaHook: { avatarMood: 'THINKING', animationPreset: 'spotlight' },
      },
      {
        id: `${lesson.id || 'lesson'}-slide-steps`,
        index: 3,
        type: 'STEPS',
        title: 'Interactive Step-by-Step Solver',
        subtitle: stepCount > 0 ? `Reveal ${stepCount} logical step${stepCount === 1 ? '' : 's'} in sequence` : 'Guided walkthrough',
        icon: 'format_list_numbered',
        durationSeconds: Math.max(8, stepCount * 4),
        narrationText: this.stripLatexForSpeech(
          `Let's solve this step by step: ${problemStmt || subtopic}`
        ),
        mediaHook: { avatarMood: 'SPEAKING', animationPreset: 'slide' },
      },
      {
        id: `${lesson.id || 'lesson'}-slide-quiz`,
        index: 4,
        type: 'QUIZ',
        title: 'Quick Check & Mastery Stat',
        subtitle: `${quizCount} interactive check${quizCount === 1 ? '' : 's'} to verify mastery`,
        icon: 'quiz',
        durationSeconds: 12,
        narrationText: `Time for a quick check! Select the best option below to verify your understanding of ${subtopic}.`,
        mediaHook: { avatarMood: 'CELEBRATING', animationPreset: 'zoom' },
      },
    ];
  }

  buildApproachesForSubtopic(
    primaryLesson: SubtopicMicroLesson,
    variants: SubtopicMicroLesson[] = []
  ): MicroTutorialApproach[] {
    const baseList = variants && variants.length > 0 ? variants : (primaryLesson ? [primaryLesson] : []);
    return baseList.map((item, idx) => {
      const seq = item.sequenceNo != null ? item.sequenceNo : idx + 1;
      const approachName = item.approachName || item.title || `Approach ${seq}`;
      const title = item.title || `Approach ${seq}: ${approachName}`;
      return {
        id: item.id ? String(item.id) : `approach-${seq}`,
        sequenceNo: seq,
        title,
        approachName,
        description: item.subtopicName || '',
        icon: 'alt_route',
        badge: `Approach ${seq}`,
        lessonType: (item.lessonType as any) || 'CONCEPT',
        difficultyLevel: (item.difficultyLevel as any) || 'CORE',
        estimatedMinutes: item.estimatedMinutes || 5,
        lesson: item,
      };
    });
  }

  deriveApproachLabel(lessonType: string): string {
    switch ((lessonType || '').toUpperCase()) {
      case 'WORKED_EXAMPLES':
      case 'WORKED_EXAMPLE':
        return 'Step-by-Step Worked Method';
      case 'TRAPS_AND_PITFALLS':
        return 'Exam Traps & Elimination';
      case 'CHEAT_SHEET':
        return 'Formula & Shortcut Sheet';
      case 'VISUAL':
        return 'Visual & Intuitive Model';
      case 'QUICK_RECAP':
        return 'Rapid Revision Recap';
      case 'CONCEPT':
      default:
        return 'Conceptual Foundation';
    }
  }

  deriveApproachTitle(lessonType: string, sequenceNo: number, subtopicName?: string): string {
    const label = this.deriveApproachLabel(lessonType);
    return subtopicName ? `Approach ${sequenceNo}: ${label} (${subtopicName})` : `Approach ${sequenceNo}: ${label}`;
  }

  private deriveApproachDescription(lessonType: string): string {
    switch ((lessonType || '').toUpperCase()) {
      case 'WORKED_EXAMPLES':
        return 'Learn through guided numerical & analytical problem decomposition.';
      case 'TRAPS_AND_PITFALLS':
        return 'Master boundary conditions, sign conventions & option elimination.';
      case 'CHEAT_SHEET':
        return 'Focus on high-yield identities, dimensional shortcuts & rapid recall.';
      case 'VISUAL':
        return 'Build spatial and graphical intuition for faster comprehension.';
      case 'QUICK_RECAP':
        return 'Condense key exam takeaways into a high-impact summary.';
      case 'CONCEPT':
      default:
        return 'Understand the underlying theory and foundational laws from scratch.';
    }
  }

  private deriveApproachIcon(lessonType: string): string {
    switch ((lessonType || '').toUpperCase()) {
      case 'WORKED_EXAMPLES':
        return 'calculate';
      case 'TRAPS_AND_PITFALLS':
        return 'warning_amber';
      case 'CHEAT_SHEET':
        return 'bolt';
      case 'VISUAL':
        return 'visibility';
      case 'QUICK_RECAP':
        return 'auto_awesome';
      case 'CONCEPT':
      default:
        return 'menu_book';
    }
  }

  private stripLatexForSpeech(text: string): string {
    if (!text) return '';
    return text
      .replace(/\$\$/g, ' ')
      .replace(/\$/g, ' ')
      .replace(/\\frac\{([^}]*)\}\{([^}]*)\}/g, '$1 divided by $2')
      .replace(/\\[a-zA-Z]+/g, ' ')
      .replace(/[*_#`~{}]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }

  private normalizeDifficultyLevel(diff?: string): 'FOUNDATION' | 'CORE' | 'ADVANCED' {
    if (!diff) return 'CORE';
    const upper = diff.trim().toUpperCase();
    if (upper === 'FOUNDATION' || upper === 'EASY' || upper === 'BEGINNER') return 'FOUNDATION';
    if (upper === 'ADVANCED' || upper === 'HARD' || upper === 'EXPERT') return 'ADVANCED';
    return 'CORE';
  }
}
