package com.kts.kronos.coverage;

import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnonymizationCoverageTest {

    // ── consolidate: FAILED branch (all domains fail) ─────────────────────────

    @Test
    void consolidate_allDomainsFailed_returnsFailedStatus() {
        // All results have status "FAILED" → failedDomains.size() == results.size()
        // → condition `failedDomains.size() < results.stream().filter(r->r!=null).count()` = FALSE
        // → else branch: FAILED (covers line 68 FALSE branch)
        var result1 = failedResult(AnonymizationResourceType.EMPLOYEE);
        var result2 = failedResult(AnonymizationResourceType.USER);

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), List.of(result1, result2)
        );

        assertEquals(AnonymizationConsolidatedStatus.FAILED, consolidated.consolidatedStatus());
        assertFalse(consolidated.isSuccess()); // isSuccess() returns false (covers FALSE branch)
        assertTrue(consolidated.isFailed());
    }

    @Test
    void consolidate_withNullResultEntry_addsWarning() {
        // null result in list → `if (result == null)` TRUE → adds warning, continues
        var good = successResult(AnonymizationResourceType.EMPLOYEE);

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), Arrays.asList(good, null)  // null entry
        );

        assertFalse(consolidated.warnings().isEmpty()); // CRITICAL warning added
        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, consolidated.consolidatedStatus());
    }

    @Test
    void consolidate_partialFailure_returnsPartialSuccess() {
        // 1 failed out of 2 → failedDomains.size()=1 < results.filter(!=null).count()=2 → PARTIAL_SUCCESS
        var good = successResult(AnonymizationResourceType.EMPLOYEE);
        var bad = failedResult(AnonymizationResourceType.USER);

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), List.of(good, bad)
        );

        assertEquals(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, consolidated.consolidatedStatus());
        assertTrue(consolidated.isPartialSuccess());
    }

    @Test
    void consolidate_failedResultWithNotes_addsWarningWithNote() {
        // "FAILED" result with non-null notes → adds to warnings with resourceType: notes
        var resultWithNotes = new AnonymizationExecutionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationResourceType.EMPLOYEE, "APPLY",
                Instant.now(), Instant.now(),
                "FAILED", 5L, 0L, 0L, 5L, "Critical error occurred"
        );

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), List.of(resultWithNotes)
        );

        assertTrue(consolidated.warnings().stream()
                .anyMatch(w -> w.contains("Critical error occurred")));
    }

    // ── consolidate: PARTIAL status → C=TRUE in (A||B)||C (inner=FALSE, C=TRUE) ──

    @Test
    void consolidate_partialStatus_coversPartialBranchInOrChain() {
        // "PARTIAL" → "ERROR".equals=FALSE, "FAILED".equals=FALSE → inner=FALSE
        // → outer: "PARTIAL".equals=TRUE → C=TRUE (short-circuit, covers C=TRUE branch)
        var partialResult = new AnonymizationExecutionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationResourceType.USER, "APPLY",
                Instant.now(), Instant.now(),
                "PARTIAL", 10L, 5L, 0L, 5L, null
        );

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), List.of(partialResult)
        );

        assertEquals(AnonymizationConsolidatedStatus.FAILED, consolidated.consolidatedStatus());
        assertTrue(consolidated.failedDomains().contains("USER"));
    }

    // ── consolidate: ERROR status → "ERROR".equals() = TRUE (short-circuit in ||) ──

    @Test
    void consolidate_errorStatus_coversErrorBranchInOrChain() {
        // "ERROR" == status → first || operand = TRUE (short-circuit) → covers A=TRUE branch
        var errorResult = new AnonymizationExecutionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationResourceType.EMPLOYEE, "APPLY",
                Instant.now(), Instant.now(),
                "ERROR", 5L, 0L, 0L, 5L, null
        );

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), List.of(errorResult)
        );

        assertEquals(AnonymizationConsolidatedStatus.FAILED, consolidated.consolidatedStatus());
        assertTrue(consolidated.failedDomains().contains("EMPLOYEE"));
    }

    // ── lambda$consolidate$0: r==null in count() filter (partial failure + null entry) ──

    @Test
    void consolidate_partialFailureWithNullEntry_lambdaFiltersNullInCount() {
        // [good, bad, null] → failedDomains=[USER] → else-if: filter(r!=null).count()=2
        // r=null → r!=null=FALSE branch in lambda (covers lambda$consolidate$0 B=1 in count())
        var good = successResult(AnonymizationResourceType.EMPLOYEE);
        var bad = failedResult(AnonymizationResourceType.USER);

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), Arrays.asList(good, bad, null)
        );

        // failedDomains.size()=1, non-null count=2 → 1<2 → PARTIAL_SUCCESS
        assertEquals(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, consolidated.consolidatedStatus());
        assertEquals(2, consolidated.domainResults().size()); // null filtered from final list
    }

    // ── isBlocked() ────────────────────────────────────────────────────────────

    @Test
    void isBlocked_blockedStatus_returnsTrue() {
        // Directly construct a result with BLOCKED status (consolidate never produces BLOCKED)
        var result = new AnonymizationConsolidatedResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationConsolidatedStatus.BLOCKED,
                "DRY_RUN", Instant.now(), Instant.now(),
                0L, 0L, 0L, 0L,
                List.of(), List.of(), List.of()
        );

        assertTrue(result.isBlocked()); // consolidatedStatus == BLOCKED → true (covers line 106)
    }

    @Test
    void isBlocked_nonBlockedStatus_returnsFalse() {
        var result = new AnonymizationConsolidatedResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationConsolidatedStatus.SUCCESS,
                "DRY_RUN", Instant.now(), Instant.now(),
                0L, 0L, 0L, 0L,
                List.of(), List.of(), List.of()
        );

        assertFalse(result.isBlocked());
    }

    // ── lambda$consolidate$0 ──────────────────────────────────────────────────

    @Test
    void consolidate_lambdaFilter_withNullResult_filtersOutNull() {
        // results.stream().filter(r -> r != null) lambda - null element filtered
        var good = successResult(AnonymizationResourceType.EMPLOYEE);

        var consolidated = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "APPLY", Instant.now(), Arrays.asList(good, null)
        );

        // domainResults should only contain non-null: 1 element
        assertEquals(1, consolidated.domainResults().size());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AnonymizationExecutionResult successResult(AnonymizationResourceType type) {
        return new AnonymizationExecutionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                type, "APPLY", Instant.now(), Instant.now(),
                "SUCCESS", 10L, 8L, 2L, 0L, null
        );
    }

    private AnonymizationExecutionResult failedResult(AnonymizationResourceType type) {
        return new AnonymizationExecutionResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                type, "APPLY", Instant.now(), Instant.now(),
                "FAILED", 10L, 0L, 0L, 10L, null
        );
    }
}
