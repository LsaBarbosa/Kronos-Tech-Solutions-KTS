-- Create notification tracking table for LGPD request workflow
-- Tracks email/internal notifications with retry logic for status change events

CREATE TABLE tb_lgpd_request_notification (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES tb_lgpd_request(request_id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    recipient_user_id UUID NOT NULL REFERENCES tb_user(user_id),
    notification_channel VARCHAR(20) NOT NULL,
    sent_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for efficient querying
CREATE INDEX idx_lgpd_notification_request
    ON tb_lgpd_request_notification(request_id);

CREATE INDEX idx_lgpd_notification_status
    ON tb_lgpd_request_notification(status)
    WHERE status = 'PENDING';

CREATE INDEX idx_lgpd_notification_recipient
    ON tb_lgpd_request_notification(recipient_user_id);

CREATE INDEX idx_lgpd_notification_retry
    ON tb_lgpd_request_notification(next_retry_at)
    WHERE status = 'FAILED' AND next_retry_at IS NOT NULL;
