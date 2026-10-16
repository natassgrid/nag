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
} from '../models/micro-lesson.model';

export interface MicroLessonQueryParams {
  targetExam?: string;
  subtopicName?: string;
  topicName?: string;
  subjectName?: string;
}

@Injectable({ providedIn: 'root' })
export class MicroLessonService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/micro-lessons';

  getPublishedMicroLesson(
    subtopicId: number,
    paramsOrTargetExam?: string | MicroLessonQueryParams
  ): Observable<SubtopicMicroLesson> {
    let params = new HttpParams();
    if (typeof paramsOrTargetExam === 'string') {
      if (paramsOrTargetExam) {
        params = params.set('targetExam', paramsOrTargetExam);
      }
    } else if (paramsOrTargetExam) {
      if (paramsOrTargetExam.targetExam) {
        params = params.set('targetExam', paramsOrTargetExam.targetExam);
      }
      if (paramsOrTargetExam.subtopicName) {
        params = params.set('subtopicName', paramsOrTargetExam.subtopicName);
      }
      if (paramsOrTargetExam.topicName) {
        params = params.set('topicName', paramsOrTargetExam.topicName);
      }
      if (paramsOrTargetExam.subjectName) {
        params = params.set('subjectName', paramsOrTargetExam.subjectName);
      }
    }

    return this.http
      .get<any>(`${this.apiUrl}/subtopic/${subtopicId}`, { params })
      .pipe(map((raw) => this.normalizeMicroLesson(raw)));
  }

  normalizeMicroLesson(raw: any): SubtopicMicroLesson {
    if (!raw) {
      throw new Error('Empty micro-lesson payload');
    }

    const rawContent = raw.content || {};

    // 1. Core concept
    const coreConcept: string = rawContent.coreConcept || raw.coreConcept || '';

    // 2. Trap alert
    let trapAlert: string | TrapAlert =
      rawContent.trapAlert || raw.trapAlert || raw.trap || '';
    if (raw.trap && typeof raw.trap === 'string' && !rawContent.trapAlert) {
      trapAlert = {
        title: 'Exam Trap & Common Pitfall',
        description: raw.trap,
        commonMistake: raw.trap,
        correctApproach:
          'Apply the governing formula and verify boundary conditions carefully.',
      };
    } else if (typeof trapAlert === 'string' && trapAlert) {
      trapAlert = {
        title: 'Exam Trap & Common Pitfall',
        description: trapAlert,
        commonMistake: trapAlert,
        correctApproach:
          'Apply the governing formula and verify boundary conditions carefully.',
      };
    }

    // 3. Formula Cheat Sheet
    let formulaCheatSheet: string | FormulaEntry[] =
      rawContent.formulaCheatSheet || '';
    if (Array.isArray(raw.cheatSheet)) {
      formulaCheatSheet = raw.cheatSheet.map((formula: string, idx: number) => ({
        name: `Key Formula ${idx + 1}`,
        latex: formula.replace(/^\$\$|\$\$$/g, '').trim(),
        description: formula,
        variables: [],
      }));
    } else if (Array.isArray(rawContent.formulaCheatSheet)) {
      formulaCheatSheet = rawContent.formulaCheatSheet;
    } else if (
      typeof rawContent.formulaCheatSheet === 'string' &&
      rawContent.formulaCheatSheet
    ) {
      formulaCheatSheet = rawContent.formulaCheatSheet;
    }

    // 4. Step-by-Step Breakdown
    const rawBreakdown =
      rawContent.stepByStepBreakdown || raw.stepByStepBreakdown;
    let breakdown: StepByStepBreakdown = {
      problemStatement: '',
      steps: [],
    };

    if (rawBreakdown) {
      const questionStem =
        rawBreakdown.questionStem || rawBreakdown.problemStatement || '';
      const finalAnswer = rawBreakdown.finalAnswer || '';
      let steps: StepItem[] = [];

      if (Array.isArray(rawBreakdown.steps)) {
        steps = rawBreakdown.steps.map((st: any, idx: number) => {
          if (typeof st === 'string') {
            return {
              stepNumber: idx + 1,
              title: `Step ${idx + 1}`,
              explanation: st,
              formula: undefined,
            };
          }
          return {
            stepNumber: st.stepNumber ?? idx + 1,
            title: st.title || `Step ${idx + 1}`,
            explanation: st.explanation || (typeof st === 'string' ? st : ''),
            formula: st.formula,
            tip: st.tip,
          };
        });
      }

      breakdown = {
        problemStatement: questionStem,
        questionStem,
        steps,
        finalAnswer,
      };
    }

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
          typeof trapAlert === 'object' && trapAlert
            ? trapAlert.description
            : typeof trapAlert === 'string'
            ? trapAlert
            : '';
        if (trapDesc) {
          keyTakeaways.push(`Watch out: ${trapDesc}`);
        }
      }
    }

    // 6. Quick check items
    let quickCheckItems: QuickCheckItem[] = rawContent.quickCheckItems || [];
    if (
      (!quickCheckItems || quickCheckItems.length === 0) &&
      rawBreakdown?.questionStem
    ) {
      quickCheckItems = [
        {
          question: rawBreakdown.questionStem,
          answer: rawBreakdown.finalAnswer || 'See step-by-step breakdown above.',
          explanation: Array.isArray(rawBreakdown.steps)
            ? rawBreakdown.steps
                .map((s: any) => (typeof s === 'string' ? s : s.explanation))
                .join(' ')
            : undefined,
        },
      ];
    }

    return {
      id: raw.id || '',
      subtopicId: raw.subtopicId != null ? Number(raw.subtopicId) : 0,
      subtopicName: raw.subtopicName || raw.subtopic || '',
      topicName: raw.topicName || raw.topic || '',
      subjectName: raw.subjectName || raw.subject || '',
      targetExam: raw.targetExam || '',
      status: raw.status || 'PUBLISHED',
      contentVersion:
        raw.contentVersion != null ? Number(raw.contentVersion) : 1,
      content: {
        coreConcept,
        trapAlert,
        formulaCheatSheet,
        stepByStepBreakdown: breakdown,
        keyTakeaways,
        quickCheckItems,
      },
      createdAt: raw.createdAt,
      updatedAt: raw.updatedAt,
    };
  }
}
