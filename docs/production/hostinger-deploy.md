# Hostinger Production Deployment Guide

## Overview

This document describes the production deployment process for Kronos on Hostinger infrastructure.

## Environment Configuration

### Java & Runtime

- **JDK Version**: 21 (eclipse-temurin:21-jre-jammy)
- **Memory**: 512MB minimum, 1GB recommended
- **GC Settings**: G1GC with `-XX:MaxGCPauseMillis=200`

### Spring Boot Profile

- **Active Profile**: `prod`
- **Configuration**: `application-prod.yml`

### Key Settings

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  profiles:
    active: prod
  jpa:
    hibernate:
      ddl-auto: validate  # Never use create/update
  
logging:
  level:
    com.kts.kronos: INFO
    org.springframework: WARN
    org.hibernate: WARN
```

## Database Setup

### PostgreSQL

- **Host**: Hostinger PostgreSQL server
- **Migrations**: Flyway v10.0.0
- **Strategy**: SQL migrations in `db/migration/`
- **Validation**: Never run with `ddl-auto: create` or `update`

### Migration Process

1. Run migrations on staging first
2. Validate schema on staging
3. Run migrations on production
4. Verify all constraints and indexes exist

```bash
./gradlew flywayInfo  # Check migration status
./gradlew flywayCleaned  # Never run in prod
```

## Secrets Management

### Environment Variables

Set on Hostinger:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/kronos_db
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<32+ char random string>
JWT_EXPIRATION=900000
AWS_REGION=sa-east-1
AWS_S3_BUCKET=kronos-documents
CORS_ALLOWED_ORIGINS=https://kronos.example.com
```

### Never Commit

- `.env` files
- Passwords or secrets
- Private keys
- Database credentials

## Container Deployment

### Docker Image

```dockerfile
FROM eclipse-temurin:21-jre-jammy

RUN useradd -m -u 1000 kronos

COPY build/libs/kronos-*.jar /app/app.jar

USER kronos
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "/app/app.jar"]
```

### Running on Hostinger

```bash
# Build image
./gradlew bootBuildImage

# Push to registry
docker push <registry>/kronos:latest

# Pull and run on Hostinger
docker pull <registry>/kronos:latest
docker run -d \
  --name kronos \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  --health-cmd="wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness" \
  --health-interval=30s \
  --health-retries=3 \
  <registry>/kronos:latest
```

## Health Checks

### Liveness Endpoint

```
GET /actuator/health/liveness
```

Returns 200 OK if application is running.

### Readiness Endpoint

```
GET /actuator/health/readiness
```

Returns 200 OK if all dependencies are ready.

### Metrics Endpoint

```
GET /actuator/prometheus
```

Prometheus metrics for monitoring.

## Logging & Monitoring

### Log Levels

- **ERROR**: Critical failures, must investigate
- **WARN**: Warnings, should review
- **INFO**: Important events (login, logout, biometric, data access)
- **DEBUG**: Never in production

### Sensitive Data

Never log:

- Passwords or secrets
- JWT tokens (except hash)
- Personal identification numbers
- Credit card data
- Medical data

### Monitoring Setup

Configure monitoring for:

- Application availability
- Database connection pool
- API response times
- Authentication failures
- Upload success rates

## Backup & Disaster Recovery

### Database Backups

- **Frequency**: Daily
- **Retention**: 30 days
- **Location**: Hostinger managed backups
- **Verification**: Weekly restore test

### Document Storage

- **Location**: AWS S3
- **Versioning**: Enabled
- **Replication**: Cross-region
- **Encryption**: AES-256

## Security Checklist

Before deploying to production:

- [ ] All secrets in environment variables (not code)
- [ ] HTTPS enabled
- [ ] Database backups configured
- [ ] Monitoring alerts configured
- [ ] Logging configured for audit trail
- [ ] CORS restricted to real domain
- [ ] Cookie security flags enabled
- [ ] Swagger/Actuator endpoints restricted
- [ ] Security headers configured
- [ ] Rate limiting enabled
- [ ] WAF rules configured
- [ ] SSL certificate valid and auto-renewal configured

## Rollback Procedure

If production deployment fails:

1. Docker container will fail health checks
2. Kubernetes/Docker orchestration will attempt restart
3. If restart fails, fall back to previous image tag
4. Check logs: `docker logs kronos`
5. Verify database migrations (should be backwards-compatible)
6. Investigate root cause before re-deploying

## Support & Troubleshooting

### Common Issues

**Port already in use**: Check if other containers are using port 8080

**Database connection failed**: Verify credentials and network access

**Health check failing**: Check application logs and database status

**High memory usage**: Increase JVM heap with `-Xmx1g`

### Getting Help

- Check application logs: `docker logs kronos`
- Check health endpoint: `curl http://localhost:8080/actuator/health`
- Check metrics: `curl http://localhost:8080/actuator/prometheus | grep jvm`
