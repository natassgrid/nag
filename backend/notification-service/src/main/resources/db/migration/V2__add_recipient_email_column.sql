SET search_path TO notification_service;

ALTER TABLE notification ADD COLUMN recipient_email VARCHAR(320);

CREATE INDEX idx_notification_recipient_email ON notification(recipient_email);
