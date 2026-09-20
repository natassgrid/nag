/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ExamEditorComponent } from './exam-editor.component';
import { MathInputDialogComponent } from './math-input-dialog.component';
import { SmilesInputDialogComponent } from './smiles-input-dialog.component';
import {
  serialiseDocument,
  deserialiseContent,
  parseMarkdownToDocument,
  encodeMathSentinel,
  encodeSmilesSentinel,
  decodeSentinel,
  sanitizeLatex
} from './utils/serializer';
import { ExamDocument } from './models';

describe('ExamEditor & Dialog Components (Issues #143 & #144)', () => {

  describe('Serializer round-trip and unicode safety tests', () => {
    it('should serialize and deserialize math-inline nodes', () => {
      const doc: ExamDocument = [
        {
          type: 'math-inline',
          latex: '\\frac{a}{b}',
          display: false,
          children: [{ text: '' }]
        }
      ];
      const markdown = serialiseDocument(doc);
      expect(markdown).toBe('$$\\frac{a}{b}$$');

      const deserialized = deserialiseContent(markdown);
      expect(deserialized.length).toBeGreaterThan(0);
    });

    it('should sanitize AI-generated latex anomalies like \\ ext and unescaped tab characters', () => {
      expect(sanitizeLatex('\\ ext{kg}')).toBe('\\text{kg}');
      expect(sanitizeLatex('\\  ext{meters}')).toBe('\\text{meters}');
      expect(sanitizeLatex('\\ text{speed}')).toBe('\\text{speed}');
      expect(sanitizeLatex('\\t ext{time}')).toBe('\\text{time}');
      expect(sanitizeLatex('\text{mass}')).toBe('\\text{mass}');
      expect(sanitizeLatex('\\ frac{1}{2}')).toBe('\\frac{1}{2}');
      expect(sanitizeLatex('\\\\frac{a}{b}')).toBe('\\frac{a}{b}');
      expect(sanitizeLatex('50%')).toBe('50\\%');
    });

    it('should serialize inline math and SMILES sentinels inside paragraphs without null characters', () => {
      const doc: ExamDocument = [
        {
          type: 'paragraph',
          children: [
            { text: 'The average weight of ' },
            { text: encodeMathSentinel('\\frac{a}{b}') },
            { text: ' is 50 kg and structure ' },
            { text: encodeSmilesSentinel('c1ccccc1') },
            { text: '.' }
          ]
        }
      ];
      const markdown = serialiseDocument(doc);
      expect(markdown).toBe('The average weight of $$\\frac{a}{b}$$ is 50 kg and structure <smiles>c1ccccc1</smiles>.');
      expect(markdown.indexOf('\x00')).toBe(-1);
      expect(markdown.indexOf('\u0000')).toBe(-1);

      const parsed = parseMarkdownToDocument(markdown);
      expect(parsed.length).toBe(1);
      const p = parsed[0] as any;
      expect(p.children.length).toBe(5);
      const mathSentinel = decodeSentinel(p.children[1].text);
      expect(mathSentinel?.kind).toBe('math');
      expect(mathSentinel?.payload).toBe('\\frac{a}{b}');
    });

    it('should correctly decode legacy sentinels and strip null characters from serialization', () => {
      const legacyDoc: ExamDocument = [
        {
          type: 'paragraph',
          children: [
            { text: 'Question: ' },
            { text: '\x00math\x00\\ce{CH4 + 2O2 -> CO2 + 2H2O}\x00' },
            { text: '\x00smiles\x00CCO\x00' }
          ]
        }
      ];
      const markdown = serialiseDocument(legacyDoc);
      expect(markdown).toBe('Question: $$\\ce{CH4 + 2O2 -> CO2 + 2H2O}$$<smiles>CCO</smiles>');
      expect(markdown).not.toContain('\x00');
    });

    it('should serialize and deserialize chemical-structure nodes with attributes', () => {
      const doc: ExamDocument = [
        {
          type: 'chemical-structure',
          smiles: 'c1ccccc1',
          title: 'Benzene',
          width: 300,
          height: 220,
          theme: 'dark',
          children: [{ text: '' }]
        }
      ];
      const markdown = serialiseDocument(doc);
      expect(markdown).toContain('<smiles title="Benzene" width="300" height="220" theme="dark">c1ccccc1</smiles>');

      const parsed = parseMarkdownToDocument(markdown);
      expect(parsed.length).toBe(1);
      expect(parsed[0].type).toBe('chemical-structure');
      const chem = parsed[0] as any;
      expect(chem.smiles).toBe('c1ccccc1');
      expect(chem.title).toBe('Benzene');
      expect(chem.width).toBe(300);
      expect(chem.height).toBe(220);
      expect(chem.theme).toBe('dark');
    });
  });

  describe('MathInputDialogComponent', () => {
    let component: MathInputDialogComponent;
    let fixture: ComponentFixture<MathInputDialogComponent>;
    const mockDialogRef = {
      close: jasmine.createSpy('close')
    };

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [MathInputDialogComponent, NoopAnimationsModule],
        providers: [
          { provide: MatDialogRef, useValue: mockDialogRef },
          { provide: MAT_DIALOG_DATA, useValue: { latex: '\\sqrt{x}', display: true } }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(MathInputDialogComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should initialize with provided data and categories', () => {
      expect(component.latex).toBe('\\sqrt{x}');
      expect(component.isDisplay).toBeTrue();
      expect(component.isEdit).toBeTrue();
      expect(component.categories.length).toBeGreaterThan(0);
    });

    it('should close with result on confirm', () => {
      component.confirm();
      expect(mockDialogRef.close).toHaveBeenCalledWith({
        latex: '\\sqrt{x}',
        display: true
      });
    });

    it('should insert snippet into latex input', () => {
      component.latex = '';
      component.insertSnippet('\\alpha');
      expect(component.latex).toContain('\\alpha');
    });
  });

  describe('SmilesInputDialogComponent', () => {
    let component: SmilesInputDialogComponent;
    let fixture: ComponentFixture<SmilesInputDialogComponent>;
    const mockDialogRef = {
      close: jasmine.createSpy('close')
    };

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [SmilesInputDialogComponent, NoopAnimationsModule],
        providers: [
          { provide: MatDialogRef, useValue: mockDialogRef },
          {
            provide: MAT_DIALOG_DATA,
            useValue: { smiles: 'c1ccccc1', title: 'Benzene', width: 280, height: 210, theme: 'dark' }
          }
        ]
      }).compileComponents();

      fixture = TestBed.createComponent(SmilesInputDialogComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should initialize with provided data and categories', () => {
      expect(component.smiles).toBe('c1ccccc1');
      expect(component.title).toBe('Benzene');
      expect(component.width).toBe(280);
      expect(component.height).toBe(210);
      expect(component.theme).toBe('dark');
      expect(component.isEdit).toBeTrue();
      expect(component.categories.length).toBeGreaterThan(0);
    });

    it('should close with result on confirm', () => {
      component.confirm();
      expect(mockDialogRef.close).toHaveBeenCalledWith({
        smiles: 'c1ccccc1',
        title: 'Benzene',
        width: 280,
        height: 210,
        theme: 'dark'
      });
    });

    it('should select preset', () => {
      component.selectPreset({ name: 'Aspirin', smiles: 'CC(=O)Oc1ccccc1C(=O)O' });
      expect(component.smiles).toBe('CC(=O)Oc1ccccc1C(=O)O');
    });
  });

  describe('ExamEditorComponent', () => {
    let component: ExamEditorComponent;
    let fixture: ComponentFixture<ExamEditorComponent>;

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [ExamEditorComponent, NoopAnimationsModule]
      }).compileComponents();

      fixture = TestBed.createComponent(ExamEditorComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should create and load initial document', () => {
      expect(component).toBeTruthy();
      expect(component.document).toBeDefined();
    });

    it('should handle value binding changes', () => {
      component.value = '# Sample Title\n$$\\int x dx$$\n<smiles>CCO</smiles>';
      expect(component.document.length).toBeGreaterThan(0);
    });
  });
});
