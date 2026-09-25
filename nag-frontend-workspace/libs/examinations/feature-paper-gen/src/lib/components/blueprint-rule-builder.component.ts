import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BlueprintRule } from '@nag-frontend-workspace/examinations-data-access';
import { Subject, SubjectHierarchy } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-blueprint-rule-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './blueprint-rule-builder.component.html',
  styleUrl: './blueprint-rule-builder.component.scss',
})
export class BlueprintRuleBuilderComponent {
  rules = input.required<BlueprintRule[]>();
  taxonomySubjects = input<Subject[]>([]);
  taxonomyHierarchy = input<SubjectHierarchy[]>([]);

  addRule = output<void>();
  removeRule = output<number>();
  ruleSubjectChange = output<BlueprintRule>();

  getTopicsForSubject(subjectName: string): string[] {
    const node = this.taxonomyHierarchy().find(
      (h) => h.name.toLowerCase() === (subjectName || '').toLowerCase()
    );
    if (node && node.topics && node.topics.length > 0) {
      return node.topics.map((t) => t.name);
    }
    return [];
  }

  onSubjectChanged(rule: BlueprintRule): void {
    const topics = this.getTopicsForSubject(rule.subject);
    if (topics.length > 0) {
      rule.topic = topics[0];
    } else {
      rule.topic = '';
    }
    this.ruleSubjectChange.emit(rule);
  }

  onAddRule(): void {
    this.addRule.emit();
  }

  onRemoveRule(index: number): void {
    this.removeRule.emit(index);
  }
}
