ALTER TABLE tb_message
    ADD COLUMN scope VARCHAR(20);

UPDATE tb_message
   SET scope = 'DIRECT'
 WHERE scope IS NULL;

ALTER TABLE tb_message
    ALTER COLUMN scope SET NOT NULL;

ALTER TABLE tb_message
    ADD CONSTRAINT chk_tb_message_scope CHECK (scope IN ('DIRECT', 'GLOBAL'));

CREATE TABLE tb_message_delivery (
    message_delivery_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id             UUID NOT NULL,
    recipient_employee_id  UUID NOT NULL,
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen_at                TIMESTAMP WITHOUT TIME ZONE NULL,

    CONSTRAINT fk_message_delivery_message
        FOREIGN KEY (message_id) REFERENCES tb_message (message_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_message_delivery_recipient
        FOREIGN KEY (recipient_employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uk_message_delivery_message_recipient
        UNIQUE (message_id, recipient_employee_id)
);

INSERT INTO tb_message_delivery (
    message_delivery_id,
    message_id,
    recipient_employee_id,
    created_at,
    seen_at
)
SELECT
    gen_random_uuid(),
    m.message_id,
    COALESCE(m.recipient_employee_id, m.employee_id),
    m.created_at,
    CASE
        WHEN e.last_seen_message_timestamp IS NOT NULL
         AND e.last_seen_message_timestamp >= m.created_at
            THEN e.last_seen_message_timestamp
        ELSE NULL
    END
FROM tb_message m
JOIN tb_employee e
  ON e.employee_id = COALESCE(m.recipient_employee_id, m.employee_id);

CREATE INDEX idx_message_delivery_message_id
    ON tb_message_delivery (message_id);

CREATE INDEX idx_message_delivery_recipient_employee_id
    ON tb_message_delivery (recipient_employee_id);

CREATE INDEX idx_message_delivery_seen_at
    ON tb_message_delivery (seen_at);
