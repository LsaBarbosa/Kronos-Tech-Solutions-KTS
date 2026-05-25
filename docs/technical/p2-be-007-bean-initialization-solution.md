# P2-BE-007: Spring Bean Initialization Solution for Integration Tests

**Date:** 2026-05-25  
**Status:** 🔄 PARTIAL FIX IMPLEMENTED  
**Branch:** feature/lgpd-compliance

---

## Problem Statement

`LgpdRetentionIntegrationTest` and `LgpdRetentionAuditValidationTest` fail to initialize Spring context with the following error chain:

```
UnsatisfiedDependencyException: No qualifying bean of type 
'com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository' available
```

This cascades through the entire dependency chain:
- AuthController → AuthService → PasswordResetTokenProviderImpl → PasswordResetTokenRepository

### Root Cause

Spring Data JPA repositories (extending `JpaRepository`) are **not automatically registered** as beans in the test context. Unlike typical `@Repository` or `@Component` beans, JPA repository interfaces require:

1. Spring Data JPA proxy generation (via `@EnableJpaRepositories`)
2. Explicit classpath scanning or configuration

When `@SpringBootTest` loads the full application context, it attempts to autowire all services and controllers. Those services depend on repositories, but repositories haven't been auto-configured yet.

---

## Solution Implemented: TestConfiguration with Mock Beans

### Current Fix (Partial)

Added `@TestConfiguration` inner class to both test files:

```java
@TestConfiguration
static class RetentionTestConfiguration {
    @Bean
    public PasswordResetTokenRepository passwordResetTokenRepository() {
        return Mockito.mock(PasswordResetTokenRepository.class);
    }

    @Bean
    public LegalConsentRepository legalConsentRepository() {
        return Mockito.mock(LegalConsentRepository.class);
    }

    @Bean
    public AuditLogRepository auditLogRepository() {
        return Mockito.mock(AuditLogRepository.class);
    }

    @Bean
    public CompanyRepository companyRepository() {
        return Mockito.mock(CompanyRepository.class);
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }
}
```

Imported via:
```java
@Import(LgpdRetentionIntegrationTest.RetentionTestConfiguration.class)
```

### What Works

✅ Tests compile successfully  
✅ @TestConfiguration properly instantiates mock beans  
✅ First-level dependencies resolved (PasswordResetTokenRepository)  
✅ Mail service mocked  

### What Remains

⏳ Cascading repository dependencies:
- `companyService` → `userService` → `DocumentProvider` → **DocumentRepository**
- `authService` → Many more repositories

The test context tries to instantiate ALL controllers/services from the application, each with their own dependencies.

---

## Recommended Solutions (in order of preference)

### Solution 1: Complete Repository Mock Factory (FASTEST - 20 min)

Create a utility class that mocks all 24 repositories systematically:

```java
@TestConfiguration
static class CompleteRepositoryMockConfiguration {
    @Bean public AfdEntryRepository afdEntryRepository() { return mock(...); }
    @Bean public AnonymizationConsolidatedResultRepository anonymizationRepository() { ... }
    @Bean public AuditLogRepository auditLogRepository() { ... }
    // ... 21 more
}
```

**Pros:**
- Simple, straightforward
- No test code changes needed
- Works immediately

**Cons:**
- Verbose
- Brittle (new repositories break it)
- Creates large mock context

---

### Solution 2: @DataJpaTest with Selective Imports (RECOMMENDED - 30 min)

Replace `@SpringBootTest` with `@DataJpaTest` + explicit component imports:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import({
    RetentionPolicyExecutor.class,
    RetentionPolicyCatalog.class,
    RetentionDomainProcessor.class
})
@EnableTransactionManagement
class LgpdRetentionIntegrationTest {
    // ... tests unchanged
}
```

**Pros:**
- Only loads what you need
- Repositories auto-configured by @DataJpaTest
- Cleaner separation of concerns
- Future-proof (new controllers don't break tests)

**Cons:**
- Requires identifying all necessary components
- May miss transitive dependencies initially
- Slightly more setup

---

### Solution 3: TestRestTemplate with Web Layer Testing (ALTERNATIVE - 40 min)

Move from direct Spring component testing to HTTP endpoint testing:

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(MockRepositoryConfiguration.class) // Just core repos
class LgpdRetentionIntegrationTest {
    @Autowired private TestRestTemplate rest;

    void testDryRunViaApi() {
        ResponseEntity<RetentionResult> response = rest.postForEntity(
            "/retention/execute/DOCUMENT/DRY_RUN",
            null,
            RetentionResult.class
        );
        assertEquals(OK, response.getStatusCode());
    }
}
```

**Pros:**
- Tests via HTTP (realistic)
- Validates full request/response cycle
- Clear separation between Web and Service testing

**Cons:**
- Different test pattern
- Requires endpoint mocking for error cases
- More setup

---

### Solution 4: Use Existing TestApplication Config (15 min)

File: `src/test/java/testsupport/LgpdComplianceTestApplication.java`

Already exists and excludes `MailSenderAutoConfiguration`. Extend it:

```java
@SpringBootTest(classes = {
    LgpdComplianceTestApplication.class,
    LgpdRetentionIntegrationTest.RepositoryMockConfiguration.class
})
@ActiveProfiles("test")
class LgpdRetentionIntegrationTest {
    // ...
}
```

**Pros:**
- Reuses existing infrastructure
- Minimal changes

**Cons:**
- Still requires repository mocks
- Less explicit

---

## Implementation Checklist

- [ ] Choose recommended solution (Solution 2 suggested)
- [ ] Implement solution for `LgpdRetentionIntegrationTest`
- [ ] Implement solution for `LgpdRetentionAuditValidationTest`
- [ ] Run tests: `./gradlew test --tests "LgpdRetention*"`
- [ ] Verify both test classes execute (7 + 6 = 13 tests total)
- [ ] Update P2-BE-007 status to "COMPLETED"
- [ ] Document in BACKLOG-REVIEW-PHASE-0-2.md

---

## Why This Happened

The project uses Spring Data JPA repositories extensively:

```
24 total repositories in com.kts.kronos.adapter.out.persistence
```

These are **implicit components** (not marked `@Component`). Spring Data JPA creates proxy beans automatically, but only when:

1. JpaRepository scanning is enabled (`@EnableJpaRepositories`)
2. The context is fully initialized

In integration tests with `@SpringBootTest`, the first bean created triggers full autowiring, and repositories may not be ready. This is a known Spring Data limitation—not specific to this project.

---

## Performance Impact

Current failures:
- 137 tests failed / 1495 total = 9% failure rate
- Most failures are in `@SpringBootTest` classes
- Issue affects multiple test classes beyond P2-BE-007

---

## Files Involved

- `src/test/java/com/kts/kronos/integration/LgpdRetentionIntegrationTest.java` (90% complete, bean init blocker)
- `src/test/java/com/kts/kronos/integration/LgpdRetentionAuditValidationTest.java` (90% complete, bean init blocker)
- `src/test/java/testsupport/LgpdComplianceTestApplication.java` (existing helper)

---

## Next Steps

1. **Immediate** (Today): Implement Solution 2 or Solution 1
2. **This Sprint**: Execute all retention tests
3. **Before Production**: Audit other @SpringBootTest failures

---

## References

- Spring Data JPA Repository Bean Registration: https://spring.io/projects/spring-data-jpa
- TestConfiguration Pattern: https://spring.io/blog/2016/04/15/testing-improvements-in-spring-boot-1-4
- Current Status: P2-BE-007 Retention Integration Tests (90% complete, 15-30 min to resolve)
