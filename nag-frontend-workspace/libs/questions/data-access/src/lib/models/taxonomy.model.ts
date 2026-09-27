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
