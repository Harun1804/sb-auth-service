# Database Maintenance & Operations Guide

## 📋 Table of Contents
1. Database Schema
2. Backups & Recovery
3. Monitoring
4. Maintenance Tasks
5. Troubleshooting
6. Performance Tuning

---

## 🗄️ Database Schema

### refresh_tokens Table

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45),
    device_info TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_refresh_tokens_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);
```

### Indexes

```sql
-- Single column indexes
CREATE INDEX idx_refresh_tokens_user_id 
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_token 
    ON refresh_tokens(token);

CREATE INDEX idx_refresh_tokens_expires_at 
    ON refresh_tokens(expires_at);

CREATE INDEX idx_refresh_tokens_revoked_at 
    ON refresh_tokens(revoked_at);

-- Composite index for common query patterns
CREATE INDEX idx_refresh_tokens_user_valid 
    ON refresh_tokens(user_id, revoked_at, expires_at);
```

### Relationship Diagram

```
┌─────────────────────────────────────┐
│           users                     │
├─────────────────────────────────────┤
│ id (UUID) [PK]                      │
│ email (VARCHAR)                     │
│ password (VARCHAR)                  │
│ status (ENUM)                       │
│ created_at (TIMESTAMP)              │
│ updated_at (TIMESTAMP)              │
└────────────────────┬────────────────┘
                     │ 1 to Many
                     │ (user_id FK)
                     │
┌────────────────────▼────────────────┐
│      refresh_tokens                 │
├─────────────────────────────────────┤
│ id (UUID) [PK]                      │
│ user_id (UUID) [FK]                 │
│ token (TEXT) [UNIQUE]               │
│ expires_at (TIMESTAMP)              │
│ revoked_at (TIMESTAMP) [nullable]   │
│ user_agent (VARCHAR)                │
│ ip_address (VARCHAR)                │
│ device_info (TEXT)                  │
│ created_at (TIMESTAMP)              │
│ updated_at (TIMESTAMP)              │
└─────────────────────────────────────┘
```

---

## 💾 Backups & Recovery

### Automated Backup Strategy

#### Daily Backup Script
```bash
#!/bin/bash
# backup_db.sh - Run daily via cron

BACKUP_DIR="/backups/auth_service"
DB_NAME="auth_db"
DB_USER="postgres"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/auth_db_backup_$TIMESTAMP.sql"

# Create backup
pg_dump -h localhost -U $DB_USER -d $DB_NAME > $BACKUP_FILE

# Compress
gzip $BACKUP_FILE
echo "Backup created: ${BACKUP_FILE}.gz"

# Keep only last 30 days
find $BACKUP_DIR -name "*.gz" -mtime +30 -delete
echo "Old backups cleaned up"
```

#### Cron Schedule
```bash
# Add to crontab
0 2 * * * /scripts/backup_db.sh

# Runs daily at 2:00 AM
```

### On-Demand Backup

```bash
# Full database backup
pg_dump -h localhost -U postgres -d auth_db > auth_db_full_$(date +%s).sql

# Just refresh_tokens table
pg_dump -h localhost -U postgres -d auth_db -t refresh_tokens > refresh_tokens_$(date +%s).sql

# With compression
pg_dump -h localhost -U postgres -d auth_db | gzip > auth_db_$(date +%s).sql.gz
```

### Recovery Procedures

#### Complete Database Recovery
```bash
# 1. Stop the application
systemctl stop auth-service

# 2. Drop and recreate database
psql -U postgres -d postgres -c "DROP DATABASE auth_db;"
psql -U postgres -d postgres -c "CREATE DATABASE auth_db;"

# 3. Restore from backup
psql -h localhost -U postgres -d auth_db < auth_db_backup_20240618.sql

# 4. Verify integrity
psql -h localhost -U postgres -d auth_db -c "\dt"

# 5. Start application
systemctl start auth-service
```

#### Selective Recovery (Just Tokens)
```bash
# If only refresh_tokens corrupted, but users table is fine

# 1. Backup current record
pg_dump -h localhost -U postgres -d auth_db -t refresh_tokens > refresh_tokens_corrupted.sql

# 2. Restore token table
psql -h localhost -U postgres -d auth_db < refresh_tokens_backup.sql

# 3. Verify
psql -h localhost -U postgres -d auth_db -c "SELECT COUNT(*) FROM refresh_tokens;"
```

#### Point-in-Time Recovery (Advanced)
```bash
# If PostgreSQL configured with WAL archiving
# This allows recovery to specific timestamp

# 1. Restore base backup
pg_restore -h localhost -U postgres -d auth_db auth_db_backup_full.sql

# 2. PostgreSQL recovers to timestamp using WAL files
# (Requires WAL archiving configured)
```

---

## 📊 Monitoring

### Key Metrics

#### 1. Table Size
```sql
-- Check refresh_tokens table size
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE tablename = 'refresh_tokens';

-- Expected growth: ~1KB per token
-- Daily: ~86,400 * 1KB = ~84 MB (if all tokens revoked same day)
```

#### 2. Active Sessions
```sql
-- Current active sessions per user
SELECT 
    u.email,
    COUNT(*) as active_tokens,
    MIN(rt.created_at) as oldest_session,
    MAX(rt.created_at) as newest_session
FROM refresh_tokens rt
JOIN users u ON u.id = rt.user_id
WHERE rt.revoked_at IS NULL 
  AND rt.expires_at > NOW()
GROUP BY u.email
ORDER BY COUNT(*) DESC;
```

#### 3. Revocation Rate
```sql
-- Tokens created vs revoked today
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_created,
    COUNT(DISTINCT CASE WHEN revoked_at IS NOT NULL THEN id END) as revoked,
    ROUND(100.0 * COUNT(DISTINCT CASE WHEN revoked_at IS NOT NULL 
        THEN id END) / COUNT(*), 2) as revoke_percentage
FROM refresh_tokens
WHERE DATE(created_at) >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

#### 4. Expired Tokens
```sql
-- Count tokens past expiration
SELECT 
    COUNT(*) as expired_tokens,
    COUNT(CASE WHEN revoked_at IS NOT NULL THEN 1 END) as revoked,
    COUNT(CASE WHEN revoked_at IS NULL THEN 1 END) as not_revoked,
    MIN(expires_at) as earliest_expiration,
    MAX(expires_at) as latest_expiration
FROM refresh_tokens
WHERE expires_at < NOW();
```

#### 5. Query Performance
```sql
-- Check slow queries
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time
FROM pg_stat_statements
WHERE query LIKE '%refresh_tokens%'
ORDER BY mean_time DESC;

-- If query is slow, may need additional index
```

### Dashboard Queries

#### Health Check (Run daily)
```bash
#!/bin/bash
# health_check.sh

PSQL_CMD="psql -h localhost -U postgres -d auth_db"

echo "=== Auth Service Database Health Check ==="
echo "Time: $(date)"
echo ""

echo "1. Table Size:"
$PSQL_CMD -c "SELECT pg_size_pretty(pg_total_relation_size('refresh_tokens'));"

echo ""
echo "2. Total Tokens:"
$PSQL_CMD -c "SELECT COUNT(*) as total FROM refresh_tokens;"

echo ""
echo "3. Active Tokens:"
$PSQL_CMD -c "SELECT COUNT(*) FROM refresh_tokens 
             WHERE revoked_at IS NULL AND expires_at > NOW();"

echo ""
echo "4. Expired Tokens:"
$PSQL_CMD -c "SELECT COUNT(*) FROM refresh_tokens 
             WHERE expires_at < NOW();"

echo ""
echo "5. Revoked Tokens:"
$PSQL_CMD -c "SELECT COUNT(*) FROM refresh_tokens WHERE revoked_at IS NOT NULL;"

echo ""
echo "6. Oldest Active Token:"
$PSQL_CMD -c "SELECT created_at, expires_at FROM refresh_tokens 
             WHERE revoked_at IS NULL 
             ORDER BY created_at ASC LIMIT 1;"
```

---

## 🧹 Maintenance Tasks

### Daily Tasks

#### 1. Automated Cleanup (Runs at 2:00 AM)
```sql
-- Deletes tokens revoked > 30 days ago
DELETE FROM refresh_tokens 
WHERE (revoked_at IS NOT NULL OR expires_at <= CURRENT_TIMESTAMP)
  AND updated_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
```

#### 2. Check Database Health
```bash
# Run health check script
./health_check.sh

# Monitor results for issues
```

#### 3. Review Logs
```bash
# Check application logs
tail -f /var/log/auth-service/app.log | grep -i refresh_token

# Look for errors or unusual patterns
```

### Weekly Tasks

#### 1. Analyze Index Usage
```sql
-- Check index efficiency
SELECT 
    idx.indexname,
    idx.indexdef,
    stat.idx_scan as scans,
    stat.idx_tup_read as tuples_read,
    stat.idx_tup_fetch as tuples_fetched
FROM pg_indexes idx
LEFT JOIN pg_stat_all_indexes stat 
    ON idx.indexname = stat.indexrelname
WHERE idx.tablename = 'refresh_tokens'
ORDER BY stat.idx_scan DESC;
```

#### 2. Vacuum & Analyze
```bash
# Optimize database
psql -h localhost -U postgres -d auth_db -c "VACUUM ANALYZE refresh_tokens;"

# Reclaims unused space and updates statistics
```

#### 3. Backup Verification
```bash
# Test backup restoration
pg_restore --list auth_db_backup.sql | grep refresh_tokens

# Verify backup contains tokens table
```

### Monthly Tasks

#### 1. Archive Old Data
```bash
-- Archive tokens from 90 days ago for auditing
-- (Optional, if you need long-term audit trail)

CREATE TABLE refresh_tokens_archive AS
SELECT * FROM refresh_tokens 
WHERE updated_at < CURRENT_TIMESTAMP - INTERVAL '90 days';

DELETE FROM refresh_tokens 
WHERE updated_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
```

#### 2. Performance Review
```sql
-- Analyze token statistics for the month
SELECT 
    DATE_TRUNC('day', created_at) as day,
    COUNT(*) as tokens_created,
    COUNT(CASE WHEN revoked_at IS NOT NULL THEN 1 END) as tokens_revoked,
    AVG(EXTRACT(EPOCH FROM (revoked_at - created_at))) as avg_lifetime_seconds
FROM refresh_tokens
WHERE DATE(created_at) >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY day DESC;
```

#### 3. Security Audit
```sql
-- Find suspicious patterns
-- Multiple tokens from same IP
SELECT 
    ip_address,
    COUNT(*) as token_count,
    COUNT(DISTINCT user_id) as unique_users,
    MAX(created_at) as last_used
FROM refresh_tokens
GROUP BY ip_address
HAVING COUNT(*) > 100
ORDER BY token_count DESC;

-- Tokens from multiple IPs (account compromise indicator)
SELECT 
    user_id,
    COUNT(DISTINCT ip_address) as unique_ips,
    COUNT(*) as total_tokens,
    MAX(created_at) as last_login
FROM refresh_tokens
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours'
GROUP BY user_id
HAVING COUNT(DISTINCT ip_address) > 3;
```

---

## 🔧 Troubleshooting

### Issue: "Connection refused"

**Symptoms:**
```
ERROR: could not connect to server: Connection refused
```

**Solution:**
```bash
# 1. Check PostgreSQL is running
systemctl status postgresql

# 2. Start if not running
systemctl start postgresql

# 3. Check connection string
echo $DATABASE_URL

# 4. Test connection
psql -h localhost -U postgres -d auth_db
```

### Issue: "Table doesn't exist"

**Symptoms:**
```
ERROR: relation "refresh_tokens" does not exist
```

**Solution:**
```bash
# 1. Check migration ran
psql -h localhost -U postgres -d auth_db -c "SELECT * FROM flyway_schema_history;"

# 2. If migration didn't run:
mvn flyway:migrate

# 3. Or manually create table
psql -h localhost -U postgres -d auth_db < V2__Create_refresh_tokens_table.sql
```

### Issue: "Unique constraint violation"

**Symptoms:**
```
ERROR: duplicate key value violates unique constraint "refresh_tokens_token_key"
```

**Solution:**
```sql
-- Find duplicate tokens
SELECT token, COUNT(*) FROM refresh_tokens 
GROUP BY token HAVING COUNT(*) > 1;

-- Should not occur - indicates bug if you see this
-- Check application logs for issues

-- Temporary fix (if needed):
DELETE FROM refresh_tokens 
WHERE token IN (SELECT token FROM refresh_tokens 
                GROUP BY token HAVING COUNT(*) > 1)
  AND created_at < CURRENT_TIMESTAMP - INTERVAL '1 hour';
```

### Issue: "Slow refresh queries"

**Symptoms:**
- Refresh endpoint takes > 100ms
- Application logs show slow queries

**Diagnosis:**
```sql
-- Check query plan
EXPLAIN ANALYZE
SELECT * FROM refresh_tokens 
WHERE token = 'your_token'
  AND revoked_at IS NULL 
  AND expires_at > CURRENT_TIMESTAMP;

-- Should show index usage
```

**Solution:**
```bash
# 1. Rebuild indexes
psql -h localhost -U postgres -d auth_db << EOF
REINDEX TABLE refresh_tokens;
EOF

# 2. Check table statistics are up to date
psql -h localhost -U postgres -d auth_db -c "ANALYZE refresh_tokens;"

# 3. If still slow, may need hardware upgrade or database tuning
```

### Issue: "Disk space full"

**Symptoms:**
```
ERROR: could not extend file: No space left on device
```

**Solution:**
```bash
# 1. Check disk usage
df -h /var/lib/postgresql

# 2. Clean up old backups
rm -f /backups/auth_service/*.gz

# 3. Run cleanup manually
psql -h localhost -U postgres -d auth_db << EOF
DELETE FROM refresh_tokens 
WHERE (revoked_at IS NOT NULL OR expires_at <= NOW())
  AND updated_at < NOW() - INTERVAL '10 days';
VACUUM FULL refresh_tokens;
EOF

# 4. Add more disk space if needed
```

---

## ⚙️ Performance Tuning

### Index Optimization

#### Check Index Usage
```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
WHERE tablename = 'refresh_tokens'
ORDER BY idx_scan DESC;
```

#### Add Missing Indexes
```sql
-- If token lookups are slow
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_active 
    ON refresh_tokens(token) 
    WHERE revoked_at IS NULL;

-- If user lookups are slow
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active 
    ON refresh_tokens(user_id) 
    WHERE revoked_at IS NULL;
```

### Connection Pool Tuning

#### application.yaml
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # Concurrent connections
      minimum-idle: 5             # Min idle connections
      connection-timeout: 20000   # 20 seconds
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1200000       # 20 minutes
```

### Query Optimization

#### Current Most Common Queries
```sql
-- Query 1: Validate token (most frequent)
-- Performance: <5ms with index
SELECT * FROM refresh_tokens 
WHERE token = ? AND revoked_at IS NULL AND expires_at > ?
LIMIT 1;

-- Query 2: Find user tokens (logout all)
-- Performance: <10ms with composite index
SELECT * FROM refresh_tokens 
WHERE user_id = ? AND revoked_at IS NULL;

-- Query 3: Revoke token
-- Performance: <5ms
UPDATE refresh_tokens SET revoked_at = ? WHERE token = ?;

-- Query 4: Cleanup expired
-- Performance: ~100ms for 1M+ tokens
DELETE FROM refresh_tokens 
WHERE expires_at < ? AND updated_at < ?;
```

### Database Configuration

#### PostgreSQL postgresql.conf Tuning
```ini
# Connection pooling
max_connections = 200
reserved_connections = 5

# Query optimization
shared_buffers = 256MB          # 25% of RAM
effective_cache_size = 1GB      # 75% of RAM
work_mem = 16MB                 # RAM / max_connections
maintenance_work_mem = 64MB

# WAL (Write-Ahead Logging)
wal_buffers = 16MB
checkpoint_timeout = 15min

# Query planner
random_page_cost = 1.1          # For SSD
```

---

## 📋 Checklists

### Pre-Deployment Checklist
```
[ ] Database backup created
[ ] Migration script tested
[ ] Connection strings updated
[ ] JWT secret rotated
[ ] Monitoring configured
[ ] Runbooks prepared
[ ] Team trained on new features
[ ] Deployment plan reviewed
```

### Post-Deployment Checklist
```
[ ] Migration completed successfully
[ ] Verify refresh_tokens table created
[ ] Test login endpoint
[ ] Test logout endpoint
[ ] Check cleanup job scheduled
[ ] Monitor logs for errors
[ ] Verify performance metrics
[ ] Alert team to new features
```

### Monthly Maintenance Checklist
```
[ ] Backup verified
[ ] Table size within limits
[ ] Cleanup job ran successfully
[ ] Indexes optimized
[ ] No slow queries
[ ] No security alerts
[ ] Documentation updated
[ ] Team prepared for incidents
```

---

## 🚨 Emergency Procedures

### Database is Down

```bash
#!/bin/bash
# emergency_restore.sh

echo "Starting emergency database recovery..."

# 1. Stop application
systemctl stop auth-service

# 2. Check PostgreSQL
systemctl status postgresql
if [ $? -ne 0 ]; then
    systemctl start postgresql
    sleep 5
fi

# 3. Test connection
psql -h localhost -U postgres -d auth_db -c "SELECT 1;" 
if [ $? -eq 0 ]; then
    echo "Database is responding"
else
    echo "Database recovery failed!"
    exit 1
fi

# 4. Run repair
psql -h localhost -U postgres -d auth_db << EOF
REINDEX TABLE refresh_tokens;
VACUUM FULL refresh_tokens;
ANALYZE refresh_tokens;
EOF

# 5. Restart application
systemctl start auth-service

echo "Recovery completed"
```

### Corrupted Data

```bash
#!/bin/bash
# rollback_data.sh

# 1. Identify corruption
CHECKPOINT=$(date -d '1 hour ago' +"%Y-%m-%d %H:%M:%S")

echo "Rolling back data since: $CHECKPOINT"

# 2. Backup corrupted data
pg_dump -h localhost -U postgres -d auth_db -t refresh_tokens \
    > refresh_tokens_corrupted_$(date +%s).sql

# 3. Restore from good backup
psql -h localhost -U postgres -d auth_db < refresh_tokens_good_backup.sql

# 4. Verify
psql -h localhost -U postgres -d auth_db << EOF
SELECT COUNT(*) FROM refresh_tokens;
SELECT COUNT(*) FROM refresh_tokens WHERE revoked_at IS NULL;
EOF
```

---

## 📞 Support & Escalation

### Contact Information
- **DBA Team:** dba@company.com
- **On-Call:** See pagerduty
- **Emergency:** Slack #incidents

### Escalation Process
1. Check this runbook
2. Try basic troubleshooting
3. Contact DBA team
4. If still down: page on-call DBA

---

## Summary

✅ Regular backups protect against data loss
✅ Monitoring detects issues early
✅ Maintenance keeps system healthy
✅ Procedures enable quick recovery
✅ Documented runbooks prevent delays

