import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ExamSyllabusNode } from '@nag-frontend-workspace/examinations-data-access';
import {
  TaxonomySubjectNode,
  TaxonomyTopicNode,
  TaxonomySubtopicNode,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-exam-form-syllabus',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './exam-form-syllabus.component.html',
  styleUrl: './exam-form-syllabus.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamFormSyllabusComponent {
  readonly tree = input<TaxonomySubjectNode[]>([]);
  readonly selectedNodes = input<ExamSyllabusNode[]>([]);

  readonly nodesChange = output<ExamSyllabusNode[]>();

  // Track expanded subject and topic IDs
  readonly expandedSubjectIds = signal<Set<number>>(new Set());
  readonly expandedTopicIds = signal<Set<number>>(new Set());

  readonly selectedMap = computed(() => {
    const map = new Map<string, ExamSyllabusNode>();
    for (const node of this.selectedNodes() || []) {
      if (node && node.nodeType && node.nodeId != null) {
        map.set(`${node.nodeType}_${node.nodeId}`, node);
      }
    }
    return map;
  });

  readonly syllabusCounts = computed(() => {
    const nodes = this.selectedNodes() || [];
    let subjects = 0;
    let topics = 0;
    let subtopics = 0;
    for (const n of nodes) {
      if (n.nodeType === 'SUBJECT') subjects++;
      else if (n.nodeType === 'TOPIC') topics++;
      else if (n.nodeType === 'SUBTOPIC') subtopics++;
    }
    return { subjects, topics, subtopics, total: nodes.length };
  });

  isNodeSelected(type: 'SUBJECT' | 'TOPIC' | 'SUBTOPIC', id: number): boolean {
    return this.selectedMap().has(`${type}_${id}`);
  }

  isSubjectExpanded(id: number): boolean {
    return this.expandedSubjectIds().has(id);
  }

  isTopicExpanded(id: number): boolean {
    return this.expandedTopicIds().has(id);
  }

  toggleSubjectExpand(id: number): void {
    const next = new Set(this.expandedSubjectIds());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.expandedSubjectIds.set(next);
  }

  toggleTopicExpand(id: number): void {
    const next = new Set(this.expandedTopicIds());
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    this.expandedTopicIds.set(next);
  }

  toggleNode(
    type: 'SUBJECT' | 'TOPIC' | 'SUBTOPIC',
    id: number,
    name: string,
    code?: string
  ): void {
    const key = `${type}_${id}`;
    const current = [...(this.selectedNodes() || [])];
    const exists = current.some((n) => n.nodeType === type && Number(n.nodeId) === Number(id));

    let updated: ExamSyllabusNode[];
    if (exists) {
      updated = current.filter((n) => !(n.nodeType === type && Number(n.nodeId) === Number(id)));
    } else {
      updated = [
        ...current,
        {
          nodeType: type,
          nodeId: Number(id),
          name: name || undefined,
          code: code || undefined,
        },
      ];
    }
    this.nodesChange.emit(updated);
  }

  selectAllSubjects(): void {
    const nodes: ExamSyllabusNode[] = [];
    for (const s of this.tree()) {
      nodes.push({
        nodeType: 'SUBJECT',
        nodeId: Number(s.id),
        name: s.name,
        code: s.code,
      });
      for (const t of s.topics || []) {
        nodes.push({
          nodeType: 'TOPIC',
          nodeId: Number(t.id),
          name: t.name,
          code: t.code,
        });
        for (const st of t.subtopics || []) {
          nodes.push({
            nodeType: 'SUBTOPIC',
            nodeId: Number(st.id),
            name: st.name,
            code: st.code,
          });
        }
      }
    }
    this.nodesChange.emit(nodes);
  }

  clearSyllabus(): void {
    this.nodesChange.emit([]);
  }
}
