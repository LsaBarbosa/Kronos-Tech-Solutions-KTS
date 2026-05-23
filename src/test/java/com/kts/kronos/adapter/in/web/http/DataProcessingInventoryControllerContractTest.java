package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.constants.ApiPaths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract Tests for Inventory API Routes
 *
 * Verifies that backend route constants match the expected API contract.
 * This ensures frontend routes will match backend endpoints.
 */
class DataProcessingInventoryControllerContractTest {

    @Test
    void shouldDefineInventoryConstant() {
        assertNotNull(ApiPaths.LGPD_INVENTORY);
        assertEquals("/inventory", ApiPaths.LGPD_INVENTORY);
    }

    @Test
    void shouldDefineActiveInventoryConstant() {
        assertNotNull(ApiPaths.LGPD_INVENTORY_ACTIVE);
        assertEquals("/inventory/active", ApiPaths.LGPD_INVENTORY_ACTIVE);
    }

    @Test
    void shouldDefineInventoryByCodeConstant() {
        assertNotNull(ApiPaths.LGPD_INVENTORY_BY_CODE);
        assertEquals("/inventory/{processCode}", ApiPaths.LGPD_INVENTORY_BY_CODE);
    }

    @Test
    void shouldDefineInventoryByIdConstant() {
        assertNotNull(ApiPaths.LGPD_INVENTORY_ID);
        assertEquals("/inventory/{inventoryId}", ApiPaths.LGPD_INVENTORY_ID);
    }

    @Test
    void shouldHaveCorrectBasePath() {
        String basePath = "/api" + ApiPaths.LGPD + ApiPaths.LGPD_INVENTORY;
        assertEquals("/api/lgpd/inventory", basePath);
    }

    @Test
    void shouldNotDuplicateApiPrefix() {
        String basePath = "/api" + ApiPaths.LGPD + ApiPaths.LGPD_INVENTORY;
        assertFalse(basePath.contains("/api/api"), "Base path should not contain duplicate /api");
    }

    @Test
    void shouldConstructActivePathCorrectly() {
        String activePath = "/api" + ApiPaths.LGPD + ApiPaths.LGPD_INVENTORY_ACTIVE;
        assertEquals("/api/lgpd/inventory/active", activePath);
    }

    @Test
    void shouldUseProcessCodeForRead() {
        String pathTemplate = ApiPaths.LGPD_INVENTORY_BY_CODE;
        assertTrue(pathTemplate.contains("{processCode}"), "GET by processCode should use {processCode}");
        assertFalse(pathTemplate.contains("{inventoryId}"), "GET should not use {inventoryId}");
    }

    @Test
    void shouldUseInventoryIdForUpdate() {
        String pathTemplate = ApiPaths.LGPD_INVENTORY_ID;
        assertTrue(pathTemplate.contains("{inventoryId}"), "PATCH should use {inventoryId}");
        assertFalse(pathTemplate.contains("{processCode}"), "PATCH should not use {processCode}");
    }

    @Test
    void shouldUseLgpdConstantInControllerPath() {
        // Verify LGPD constant is used for base path
        assertEquals("/lgpd", ApiPaths.LGPD);
        assertFalse(ApiPaths.LGPD.contains("/api"));
    }
}
