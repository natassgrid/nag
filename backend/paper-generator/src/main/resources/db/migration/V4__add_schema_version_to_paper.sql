-- Add schema version column to paper table for paper serialization versioning
ALTER TABLE paper_generator.paper ADD COLUMN schema_version VARCHAR(10) DEFAULT '1.0';
