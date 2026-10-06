package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.BiometricConsentStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsCoverageTest {

    private static String validSecret() {
        return Base64.getEncoder()
            .encodeToString("this-is-a-jwt-secret-with-32-bytes!!!".getBytes(StandardCharsets.UTF_8));
    }

    private static Key rebuildKey() {
        byte[] secretBytes = Base64.getDecoder().decode(validSecret());
        return Keys.hmacShaKeyFor(secretBytes);
    }

    // ── L60: double-quoted secret → first=='"' && last=='"' = TRUE ─────────────

    @Test
    void constructor_withDoubleQuotedBase64Secret_stripsQuotes() {
        // L60: first=='"' TRUE, last=='"' TRUE → entire expression TRUE
        String doubleQuoted = "\"" + validSecret() + "\"";
        JwtUtils jwtUtils = new JwtUtils(doubleQuoted, 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "PARTNER", UUID.randomUUID(), true);
        assertNotNull(token);
    }

    // ── L60: single-quoted secret → first=='\'' && last=='\'' = TRUE ───────────

    @Test
    void constructor_withSingleQuotedBase64Secret_stripsQuotes() {
        // L60: first=='\'' TRUE, last=='\'' TRUE → second operand TRUE
        String singleQuoted = "'" + validSecret() + "'";
        JwtUtils jwtUtils = new JwtUtils(singleQuoted, 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "PARTNER", UUID.randomUUID(), true);
        assertNotNull(token);
    }

    // ── L60: mismatched quotes → last=='"' = FALSE (first=='"', last!='"') ──────

    @Test
    void constructor_withMismatchedDoubleQuoteStart_throwsIllegalArg() {
        // L60: first=='"' TRUE, last=='"' FALSE → first && FALSE; last!='\'' → overall FALSE
        // No quotes stripped → leading '"' is invalid Base64 → throws
        assertThrows(IllegalArgumentException.class,
            () -> new JwtUtils("\"" + validSecret(), 60_000L));
    }

    // ── L60: mismatched single-quote → last=='\'' = FALSE (first='\'', last!='\''')──

    @Test
    void constructor_withMismatchedSingleQuoteStart_throwsIllegalArg() {
        // L60: first=='\'' TRUE, last=='\'' FALSE → second && FALSE → overall FALSE
        assertThrows(IllegalArgumentException.class,
            () -> new JwtUtils("'" + validSecret(), 60_000L));
    }

    // ── L101/L103: generateToken 6-arg with null userId/employeeId ──────────────

    @Test
    void generateToken6arg_withNullEmployeeIdAndUserId_setsNullClaims() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "hash", "1.0", "hash", false);
        String token = jwtUtils.generateToken(null, "user", "PARTNER", null, consent, 0L);
        assertNotNull(token);
        assertNull(jwtUtils.getEmployeeIdFromToken(token));
        assertNull(jwtUtils.getUserIdFromToken(token));
    }

    // ── L171: getEmployeeIdFromToken with blank claim → isBlank=TRUE → null ─────

    @Test
    void getEmployeeIdFromToken_withBlankClaim_returnsNull() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = rebuildKey();
        String token = Jwts.builder()
            .claim("employeeId", " ")
            .claim("userId", UUID.randomUUID().toString())
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
        assertNull(jwtUtils.getEmployeeIdFromToken(token));
    }

    // ── L184: getUserIdFromToken with blank claim → isBlank=TRUE → null ─────────

    @Test
    void getUserIdFromToken_withBlankClaim_returnsNull() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = rebuildKey();
        String token = Jwts.builder()
            .claim("userId", " ")
            .claim("employeeId", UUID.randomUUID().toString())
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
        assertNull(jwtUtils.getUserIdFromToken(token));
    }

    // ── L262: getActiveCompanyIdFromToken with blank claim → null ────────────────

    @Test
    void getActiveCompanyIdFromToken_withBlankClaim_returnsNull() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = rebuildKey();
        String token = Jwts.builder()
            .claim("activeCompanyId", " ")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
        assertNull(jwtUtils.getActiveCompanyIdFromToken(token));
    }
}
