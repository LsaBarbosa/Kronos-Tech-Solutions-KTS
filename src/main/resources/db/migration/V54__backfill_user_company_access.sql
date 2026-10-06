-- Ensure users created before automatic access provisioning have an explicit
-- company scope. The insert is idempotent because (user_id, company_id) is
-- unique in tb_user_company_access.
INSERT INTO tb_user_company_access
    (access_id, user_id, company_id, employee_id, role, is_active, is_default, created_at)
SELECT
    gen_random_uuid(),
    u.user_id,
    e.company_id,
    e.employee_id,
    u.role,
    u.is_active,
    TRUE,
    CURRENT_TIMESTAMP
FROM tb_user u
JOIN tb_employee e ON e.employee_id = u.employee_id
WHERE u.role IN ('MANAGER', 'PARTNER')
  AND u.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tb_user_company_access existing
      WHERE existing.user_id = u.user_id
        AND existing.company_id = e.company_id
  );
