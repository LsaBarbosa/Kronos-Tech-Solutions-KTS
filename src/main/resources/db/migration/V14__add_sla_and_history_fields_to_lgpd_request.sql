-- Add SLA and additional fields to tb_lgpd_request
ALTER TABLE tb_lgpd_request
    ADD COLUMN due_at TIMESTAMP NULL,
    ADD COLUMN priority VARCHAR(30) DEFAULT 'NORMAL',
    ADD COLUMN closed_reason VARCHAR(100) NULL,
    ADD COLUMN public_resolution_notes TEXT NULL,
    ADD COLUMN internal_notes TEXT NULL;

-- Add event tracking fields to tb_lgpd_request_history
ALTER TABLE tb_lgpd_request_history
    ADD COLUMN event_type VARCHAR(50) NULL,
    ADD COLUMN previous_status VARCHAR(50) NULL,
    ADD COLUMN new_status VARCHAR(50) NULL,
    ADD COLUMN public_note TEXT NULL,
    ADD COLUMN internal_note TEXT NULL,
    ADD COLUMN actor_user_id UUID NULL,
    ADD COLUMN visible_to_data_subject BOOLEAN DEFAULT TRUE;

-- Create index on due_at for overdue queries
CREATE INDEX idx_lgpd_request_due_at ON tb_lgpd_request(due_at);

-- Create index on event_type for history queries
CREATE INDEX idx_lgpd_request_history_event_type ON tb_lgpd_request_history(event_type);

-- Create index on visible_to_data_subject for filtering
CREATE INDEX idx_lgpd_request_history_visible_to_subject ON tb_lgpd_request_history(visible_to_data_subject);
