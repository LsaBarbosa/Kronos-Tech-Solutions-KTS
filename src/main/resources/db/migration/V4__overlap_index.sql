-- KRN-P2-011: Índice combinado para consultas de overlap de registros de trabalho.
-- Otimiza a query em validateNonBreakOverlap que filtra por (employee_id, start_work, status_record).
CREATE INDEX IF NOT EXISTS idx_tb_time_records_employee_start_status
    ON tb_time_records (employee_id, start_work, status_record)
    WHERE start_work IS NOT NULL;
