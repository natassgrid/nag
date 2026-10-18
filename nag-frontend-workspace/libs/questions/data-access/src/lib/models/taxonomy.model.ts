export interface Subject {
  id: number;
  name: string;
  code?: string;
  description?: string;
  topicCount?: number;
  questionCount?: number;
}

export interface Topic {
  id: number;
  subjectId: number;
  name: string;
  description?: string;
  subtopicCount?: number;
  questionCount?: number;
}

export interface Subtopic {
  id: number;
  topicId: number;
  name: string;
  description?: string;
  questionCount?: number;
}

export interface SubtopicNode {
  id: number;
  name: string;
  description?: string;
  questionCount?: number;
}

export interface TopicNode {
  id: number;
  name: string;
  description?: string;
  questionCount?: number;
  subtopics: SubtopicNode[];
}

export interface SubjectHierarchy {
  id: number;
  name: string;
  code?: string;
  description?: string;
  questionCount?: number;
  topics: TopicNode[];
}

export interface TargetExaminationOption {
  id: string;
  name: string;
  code: string;
  category?: string;
  examinationType?: string;
}

export type TaxonomyNodeType = 'SUBJECT' | 'TOPIC' | 'SUBTOPIC';

export interface TaxonomySubtopicNode {
  id: number;
  topicId?: number;
  code?: string;
  name: string;
  description?: string;
  displayOrder?: number;
  active?: boolean;
  questionCount?: number;
  lessonCount?: number;
}

export interface TaxonomyTopicNode {
  id: number;
  subjectId?: number;
  code?: string;
  name: string;
  description?: string;
  displayOrder?: number;
  active?: boolean;
  questionCount?: number;
  lessonCount?: number;
  subtopics: TaxonomySubtopicNode[];
}

export interface TaxonomySubjectNode {
  id: number;
  code?: string;
  name: string;
  description?: string;
  displayOrder?: number;
  active?: boolean;
  questionCount?: number;
  lessonCount?: number;
  topics: TaxonomyTopicNode[];
}

export interface TaxonomyOverrideRequest {
  nodeType: TaxonomyNodeType;
  nodeId: number;
  hidden?: boolean;
  nameOverride?: string;
  orderOverride?: number;
}

export interface MergeTaxonomyRequest {
  nodeType: TaxonomyNodeType;
  sourceNodeId: number;
  targetNodeId: number;
}

export interface NodeOrderItem {
  id: number;
  displayOrder: number;
}

export interface ReorderTaxonomyRequest {
  nodeType: TaxonomyNodeType;
  items: NodeOrderItem[];
}

