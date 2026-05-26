# Kronos - Time & Document Management System

A comprehensive time tracking and document management system for Brazilian companies, built with Spring Boot 3.5.3 and Java 21.

## Quick Start

### Prerequisites

- Java 21 (JDK via eclipse-temurin)
- PostgreSQL 14+
- Docker (for containerized deployment)

### Local Development

```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew unitTest

# Start the application
./gradlew bootRun

# Application runs at http://localhost:8080
```

### Docker Deployment

```bash
# Build Docker image
./gradlew bootBuildImage

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kronos \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  kronos:latest
```

## Documentation

### Security & Compliance

- **[Session Policy](docs/security/session-policy.md)** - JWT tokens, cookie security, session management
- **[CSRF Protection](docs/security/csrf-policy.md)** - CSRF strategy, SameSite cookies, token validation
- **[Data Retention](docs/legal/data-retention.md)** - LGPD compliance, soft delete, legal requirements

### Operations

- **[Production Deployment](docs/production/hostinger-deploy.md)** - Hostinger setup, Docker, monitoring, troubleshooting
- **[Database Migrations](docs/database/migrations.md)** - Flyway strategy, migration patterns, best practices
- **[Pre-Production Checklist](PRE_PRODUCTION_CHECKLIST.md)** - Complete checklist before deploying to production

### Validate documentation links

```bash
./scripts/check-doc-links.sh
```

## Project Structure

```
src/
├── main/
│   ├── java/com/kts/kronos/
│   │   ├── adapter/          # API controllers, DTOs, REST layer
│   │   ├── application/      # Business logic, use cases, services
│   │   ├── domain/           # Domain models, enums, entities
│   │   ├── constants/        # Messages, error codes, constants
│   │   └── config/           # Spring configuration
│   └── resources/
│       ├── application.yml   # Default configuration
│       ├── db/migration/     # Flyway database migrations
│       └── i18n/             # Internationalization messages
└── test/
    └── java/com/kts/kronos/  # Unit and integration tests
```

## Key Features

### Time Management

- Clock in/out with biometric verification
- Time record adjustments with approval workflow
- Overtime calculation and tracking
- Leave & vacation management
- Automatic break detection

### Document Management

- Secure document upload (PDF, JPEG, PNG)
- Document versioning
- Time-limited downloads
- Audit trail for all access

### Security

- JWT-based authentication
- Biometric acceptance flow (facial recognition)
- Role-based access control (RBAC)
- LGPD-compliant data handling
- Encrypted sensitive data storage

### Compliance

- LGPD (Lei Geral de Proteção de Dados) compliant
- Brazilian labor law (CLT) requirements
- Audit logging for all operations
- Soft delete with retention policies

## Configuration

### Environment Variables (Production)

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/kronos_db
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<32+ random characters>
JWT_EXPIRATION=900000  # 15 minutes
AWS_REGION=sa-east-1
AWS_S3_BUCKET=kronos-documents
CORS_ALLOWED_ORIGINS=https://kronos.example.com
```

### Spring Profiles

- **development**: Local development with minimal security
- **test**: Test environment with H2 database
- **prod**: Production hardened configuration

## Testing

```bash
# Unit tests only
./gradlew unitTest

# All tests including integration
./gradlew test

# Run specific test class
./gradlew test --tests "TimeRecordServiceTest"

# Generate coverage report
./gradlew jacocoTestReport
# Report available at: build/reports/jacoco/test/html/index.html
```

## Database

### Migrations

Flyway handles all database schema changes:

```bash
# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Migrate (runs automatically on startup)
./gradlew flywayCleaned
```

### Supported Databases

- PostgreSQL 14+ (production)
- H2 (testing)

## API Documentation

Once running, access Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

**Note**: Swagger is disabled in production profile.

## Monitoring

### Health Endpoints

- **Liveness**: `GET /actuator/health/liveness`
- **Readiness**: `GET /actuator/health/readiness`
- **Metrics**: `GET /actuator/prometheus`

### Key Metrics

- JVM memory usage
- Database connection pool stats
- HTTP request latency
- Authentication success/failure rates

## Deployment

### Staging

```bash
./gradlew clean build
docker build -t kronos:staging .
docker push registry.example.com/kronos:staging
```

### Production

See [Production Deployment Guide](docs/production/hostinger-deploy.md)

**Before deploying, review the [Pre-Production Checklist](PRE_PRODUCTION_CHECKLIST.md)**

## Security Best Practices

1. **Never commit secrets** - Use environment variables
2. **Always use HTTPS** - Cookies marked Secure in production
3. **Validate input** - All endpoints validate user input
4. **Audit sensitive operations** - All data access is logged
5. **Encrypt sensitive data** - Passwords, tokens, biometric hashes
6. **Restrict file uploads** - Only PDF, JPEG, PNG allowed
7. **Monitor logs** - Watch for authentication failures and suspicious activity

## Troubleshooting

### Build Issues

```bash
# Clean and rebuild
./gradlew clean build --no-build-cache

# Show dependency tree
./gradlew dependencies

# Check for conflicts
./gradlew dependencyInsight --dependency junit
```

### Runtime Issues

```bash
# Check logs
tail -f logs/kronos.log

# View application properties
curl http://localhost:8080/actuator/configprops

# Check database connection
curl http://localhost:8080/actuator/health/db
```

## Contributing

### Code Standards

- Follow existing code style
- Write unit tests for new functionality
- Update documentation for API changes
- Reference issue numbers in commit messages

### Pull Request Process

1. Create feature branch from `main`
2. Write tests for new features
3. Pass `./gradlew clean build`
4. Submit PR with detailed description
5. Address code review comments
6. Merge after approval

## License

Internal use only. Kronos Tech Solutions.

## Support

- **Documentation**: See `/docs` directory
- **Issues**: Report to development team
- **On-Call**: Follow runbooks in operations documentation

## Version History

- **v0.0.1** - Initial development release (May 2026)

---

**Last Updated**: May 13, 2026
**Maintained By**: Kronos Development Team
