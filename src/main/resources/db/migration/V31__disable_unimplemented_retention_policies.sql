-- Disable retention policies for resources without implemented processors
-- TIME_RECORD and EMPLOYEE_CONTRACT should be preserved (PRESERVE_LEGAL_EVIDENCE)
-- while their processors for APPLY mode are being developed.

UPDATE tb_retention_policy
   SET enabled = false
 WHERE resource_type IN ('TIME_RECORD', 'EMPLOYEE_CONTRACT')
   AND enabled = true;
