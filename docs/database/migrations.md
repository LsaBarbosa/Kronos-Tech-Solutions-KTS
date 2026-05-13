# Database Migrations Policy

## Overview

Kronos uses Flyway for database version control. This document defines the migration strategy, best practices, and safety measures.

## Migration Framework

- **Tool**: Flyway v10.0.0
- **Location**: `src/main/resources/db/migration/`
- **Naming**: `V<number>__<description>.sql`
- **Execution**: Automatic on application startup
- **Validation**: `ddl-auto: validate` (never create/update)

## Migration Types

### Versioned Migrations

Standard migrations with version numbers:

```
V1__initial_schema.sql
V2__add_users_table.sql
V3__add_indexes.sql
```

- **Always executed in order**
- **Never modified after deployed**
- **Version increases monotonically**

### Undo Migrations

Rollback migrations (optional):

```
U1__undo_users_table.sql
```

## Writing Migrations

### SQL Standards

- Use explicit schema references: `CREATE TABLE public.users (...)`
- Include comments explaining intent
- Use explicit data types (no implicit defaults)
- Always include constraints (NOT NULL, UNIQUE, FK)

### Best Practices

#### 1. Make Migrations Idempotent When Possible

```sql
-- Bad: Will fail if already exists
CREATE TABLE users (id UUID PRIMARY KEY);

-- Good: Handles re-execution
CREATE TABLE IF NOT EXISTS users (id UUID PRIMARY KEY);

-- Good: Explicit DROP IF EXISTS for drops
DROP TABLE IF EXISTS legacy_users CASCADE;
```

#### 2. Use Explicit Sequences for IDs

```sql
-- For serial IDs
CREATE SEQUENCE users_id_seq START WITH 1 INCREMENT BY 1;
ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_id_seq');

-- For UUIDs
ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();
```

#### 3. Add Indexes for Foreign Keys

```sql
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employees(id),
    ...
);

-- Add index on foreign key
CREATE INDEX idx_documents_employee_id ON documents(employee_id);
```

#### 4. Include Data Migrations Safely

```sql
-- Step 1: Add column with default
ALTER TABLE time_records ADD COLUMN status_record VARCHAR(50) DEFAULT 'CREATED';

-- Step 2: Update data (in separate migration for safety)
UPDATE time_records SET status_record = 'CREATED' WHERE status_record IS NULL;

-- Step 3: Add constraint
ALTER TABLE time_records ALTER COLUMN status_record SET NOT NULL;
```

#### 5. Avoid Locking Transactions

```sql
-- Bad: Locks table during migration
ALTER TABLE large_table ADD COLUMN active BOOLEAN DEFAULT true;

-- Better: Use concurrent index creation
CREATE INDEX CONCURRENTLY idx_new_col ON table(column);
```

## Migration Testing

### Local Testing

```bash
# Clean and re-apply all migrations
./gradlew flywayClean
./gradlew flywayMigrate

# Verify migration info
./gradlew flywayInfo
```

### Staging Testing

1. Run migration on staging database
2. Verify all constraints and indexes
3. Run application tests
4. Check data integrity

### Production Deployment

```bash
# 1. Backup database
pg_dump -h host -U user -d kronos_db > backup_$(date +%Y%m%d).sql

# 2. Run migration
./gradlew flywayCleaned

# 3. Verify
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

# 4. Monitor logs for errors
tail -f logs/kronos.log | grep -i migration
```

## Troubleshooting Migrations

### Migration Failed

1. Check error message in logs
2. Verify database connectivity
3. Review migration SQL syntax
4. Check for permission issues
5. If blocking: Flyway has already recorded it - may need manual repair

### Rollback Procedure

Flyway doesn't automatically rollback failed migrations. Options:

**Option 1: Fix and Deploy Undo**

Create `U<version>__fix.sql` to undo the failed migration.

**Option 2: Manual Intervention**

```sql
-- Find the failed migration
SELECT * FROM flyway_schema_history WHERE success = false;

-- Manually execute the undo statements
-- Then update flyway table
DELETE FROM flyway_schema_history WHERE version = X;
```

**Option 3: Restore from Backup**

If manual fix is complex, restore database from backup and re-test migration.

## Large Migrations

For migrations that might take long:

### Step 1: Prepare

```sql
-- Add new column without constraint
ALTER TABLE large_table ADD COLUMN new_column VARCHAR(255);

-- Create index on new column
CREATE INDEX CONCURRENTLY idx_large_table_new_col ON large_table(new_column);
```

### Step 2: Backfill

In a separate migration (V<n+1>):

```sql
-- Update in batches to avoid long locks
DO $$
DECLARE
    batch_size INT := 10000;
    affected INT;
BEGIN
    LOOP
        UPDATE large_table 
        SET new_column = compute_value(old_column)
        WHERE new_column IS NULL
        LIMIT batch_size;
        
        affected := FOUND;
        EXIT WHEN affected = 0;
    END LOOP;
END $$;
```

### Step 3: Constraint

In another migration (V<n+2>):

```sql
ALTER TABLE large_table 
ALTER COLUMN new_column SET NOT NULL;

ALTER TABLE large_table
ADD CONSTRAINT check_new_col CHECK (new_column IS NOT NULL);
```

## Schema Freezing

In production, the schema is read-only:

- `ddl-auto: validate` - Only validates, doesn't modify
- All changes through migrations
- All migrations must be tested first
- No direct SQL in application

## Monitoring

Track migration performance:

```sql
-- Check last executed migrations
SELECT * FROM flyway_schema_history 
ORDER BY installed_rank DESC LIMIT 10;

-- Check migration execution time
SELECT version, description, EXTRACT(EPOCH FROM (success_timestamp - installed_on)) as duration_seconds
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

## Related Documentation

- [Spring Boot Flyway Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization)
- [Flyway Best Practices](https://flywaydb.org/documentation/concepts/migrations)
- [PostgreSQL Concurrency](https://www.postgresql.org/docs/current/sql-commands.html)
