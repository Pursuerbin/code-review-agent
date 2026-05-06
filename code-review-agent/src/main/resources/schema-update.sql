ALTER TABLE review_task ADD COLUMN task_type VARCHAR(50) DEFAULT 'REVIEW';
ALTER TABLE review_task ADD COLUMN extra_context TEXT;