package com.kts.kronos.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Configuration
@EnableConfigurationProperties
@RequiredArgsConstructor
public class ProductionConfigValidator {

	private final Environment env;

	@PostConstruct
	void validate() {
		String[] activeProfiles = env.getActiveProfiles();
		boolean isProdProfile = false;
		for (String profile : activeProfiles) {
			if ("prod".equals(profile)) {
				isProdProfile = true;
				break;
			}
		}

		if (!isProdProfile) {
			return;
		}

		validateRequiredVariable("DB_HOST");
		validateRequiredVariable("DB_PORT");
		validateRequiredVariable("DB_NAME");
		validateRequiredVariable("DB_USERNAME");
		validateRequiredVariable("DB_PASSWORD");

		validateRequiredVariable("JWT_SECRET");
		validateJwtSecretLength();

		validateRequiredVariable("FRONTEND_BASE_URL_PLATAFORM");
		validateRequiredVariable("FRONTEND_BASE_URL_RECORD");
		validateRequiredVariable("FRONTEND_ALLOWED_ORIGINS");

		validateRequiredVariable("AWS_REGION");
		validateRequiredVariable("AWS_ACCESS_KEY_ID");
		validateRequiredVariable("AWS_SECRET_ACCESS_KEY");
		validateRequiredVariable("AWS_S3_BUCKET_NAME");
		validateRequiredVariable("AWS_REKOGNITION_COLLECTION_ID");

		validateMailConfiguration();
		validateJpaConfiguration();
		validateCookieConfiguration();
	}

	private void validateRequiredVariable(String key) {
		String value = env.getProperty(key);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
				String.format("PRODUCTION: Required environment variable '%s' is not set or is empty.", key)
			);
		}
	}

	private void validateJwtSecretLength() {
		String jwtSecret = env.getProperty("JWT_SECRET");
		if (jwtSecret == null || jwtSecret.length() < 64) {
			throw new IllegalStateException(
				"PRODUCTION: JWT_SECRET must be at least 64 characters (use 'openssl rand -base64 64' to generate)."
			);
		}
	}

	private void validateMailConfiguration() {
		String mailHost = env.getProperty("MAIL_HOST");
		String mailPort = env.getProperty("MAIL_PORT");
		String mailUsername = env.getProperty("MAIL_USERNAME");
		String mailPassword = env.getProperty("MAIL_PASSWORD");

		if (mailHost == null || mailHost.isBlank()) {
			throw new IllegalStateException("PRODUCTION: MAIL_HOST is required.");
		}
		if (mailPort == null || mailPort.isBlank()) {
			throw new IllegalStateException("PRODUCTION: MAIL_PORT is required.");
		}
		if (mailUsername == null || mailUsername.isBlank()) {
			throw new IllegalStateException("PRODUCTION: MAIL_USERNAME is required.");
		}
		if (mailPassword == null || mailPassword.isBlank()) {
			throw new IllegalStateException("PRODUCTION: MAIL_PASSWORD is required.");
		}
	}

	private void validateJpaConfiguration() {
		String ddlAuto = env.getProperty("JPA_DDL_AUTO", "validate");

		if ("create".equalsIgnoreCase(ddlAuto)
			|| "create-drop".equalsIgnoreCase(ddlAuto)
			|| "update".equalsIgnoreCase(ddlAuto)) {
			throw new IllegalStateException(
				String.format(
					"PRODUCTION SECURITY: JPA_DDL_AUTO='%s' is not allowed in production. "
					+ "Use 'validate' or 'none'. Database schema must be managed with Flyway.",
					ddlAuto
				)
			);
		}
	}

	private void validateCookieConfiguration() {
		String cookieSecure = env.getProperty("AUTH_COOKIE_SECURE", "true");

		if (!"true".equalsIgnoreCase(cookieSecure)) {
			throw new IllegalStateException(
				"PRODUCTION SECURITY: AUTH_COOKIE_SECURE must be 'true' in production. "
				+ "Cookies must be transmitted over HTTPS only."
			);
		}
	}
}
