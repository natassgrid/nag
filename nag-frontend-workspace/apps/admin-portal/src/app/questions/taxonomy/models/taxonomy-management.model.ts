export interface CreateSubjectDto {
  name: string;
  code?: string;
  description?: string;
}

export interface CreateTopicDto {
  name: string;
  description?: string;
}

export interface CreateSubtopicDto {
  name: string;
  description?: string;
}

export interface TaxonomyStats {
  totalSubjects: number;
  totalTopics: number;
  totalSubtopics: number;
}
