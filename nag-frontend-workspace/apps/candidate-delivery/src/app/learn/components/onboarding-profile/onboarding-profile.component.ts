import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import {
  LearnerProfile,
  LearnerEducationLevel,
  LearnerPriorExperience,
} from '../../models/learner.model';
import { LessonType } from '../../models/micro-lesson.model';

@Component({
  selector: 'app-onboarding-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
  ],
  templateUrl: './onboarding-profile.component.html',
  styleUrl: './onboarding-profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OnboardingProfileComponent implements OnInit {
  readonly profile = input<LearnerProfile | null>(null);
  readonly saved = output<Partial<LearnerProfile>>();

  readonly educationLevel = signal<LearnerEducationLevel>('UNDERGRADUATE');
  readonly preferredLanguage = signal<string>('en');
  readonly dailyStudyMinutes = signal<number>(60);
  readonly priorExperience = signal<LearnerPriorExperience>('INTERMEDIATE');
  readonly targetDate = signal<string>('2026-11-15');
  readonly selectedLessonTypes = signal<LessonType[]>([
    'CONCEPT',
    'WORKED_EXAMPLES',
    'TRAPS_AND_PITFALLS',
  ]);

  readonly educationLevels: Array<{ value: LearnerEducationLevel; label: string }> = [
    { value: 'HIGH_SCHOOL', label: 'Higher Secondary (10+2 / Grade 12)' },
    { value: 'UNDERGRADUATE', label: 'Undergraduate Degree (B.Sc / B.Tech / B.A)' },
    { value: 'GRADUATE', label: 'Graduate / Degree Holder' },
    { value: 'POSTGRADUATE', label: 'Postgraduate Degree (M.Sc / M.Tech / M.A)' },
    { value: 'OTHER', label: 'Other Educational Background' },
  ];

  readonly languages: Array<{ code: string; label: string }> = [
    { code: 'en', label: 'English' },
    { code: 'hi', label: 'Hindi (हिन्दी)' },
    { code: 'bn', label: 'Bengali (বাংলা)' },
    { code: 'te', label: 'Telugu (తెలుగు)' },
    { code: 'mr', label: 'Marathi (मराठी)' },
    { code: 'ta', label: 'Tamil (தமிழ்)' },
  ];

  readonly studyTimes: number[] = [30, 45, 60, 90, 120];

  readonly lessonTypeOptions: Array<{ type: LessonType; label: string; icon: string }> = [
    { type: 'CONCEPT', label: 'Core Concepts', icon: 'menu_book' },
    { type: 'WORKED_EXAMPLES', label: 'Step-by-Step Examples', icon: 'functions' },
    { type: 'TRAPS_AND_PITFALLS', label: 'Traps & Pitfalls', icon: 'warning' },
    { type: 'CHEAT_SHEET', label: 'Formula Cheat Sheet', icon: 'description' },
    { type: 'VISUAL', label: 'Visual Intuition', icon: 'visibility' },
    { type: 'QUICK_RECAP', label: '2-Min Quick Recap', icon: 'bolt' },
  ];

  ngOnInit(): void {
    const p = this.profile();
    if (p) {
      if (p.educationLevel) this.educationLevel.set(p.educationLevel);
      if (p.preferredLanguage) this.preferredLanguage.set(p.preferredLanguage);
      if (p.dailyStudyMinutes) this.dailyStudyMinutes.set(p.dailyStudyMinutes);
      if (p.priorExperience) this.priorExperience.set(p.priorExperience);
      if (p.targetDate) this.targetDate.set(p.targetDate);
      if (p.preferredLessonTypes && p.preferredLessonTypes.length > 0) {
        this.selectedLessonTypes.set(p.preferredLessonTypes);
      }
    }
  }

  toggleLessonType(type: LessonType): void {
    const current = this.selectedLessonTypes();
    if (current.includes(type)) {
      if (current.length > 1) {
        this.selectedLessonTypes.set(current.filter((t) => t !== type));
      }
    } else {
      this.selectedLessonTypes.set([...current, type]);
    }
  }

  isLessonTypeSelected(type: LessonType): boolean {
    return this.selectedLessonTypes().includes(type);
  }

  submitProfile(): void {
    this.saved.emit({
      educationLevel: this.educationLevel(),
      preferredLanguage: this.preferredLanguage(),
      dailyStudyMinutes: this.dailyStudyMinutes(),
      preferredLessonTypes: this.selectedLessonTypes(),
      priorExperience: this.priorExperience(),
      targetDate: this.targetDate(),
      completedAt: new Date().toISOString(),
    });
  }
}
