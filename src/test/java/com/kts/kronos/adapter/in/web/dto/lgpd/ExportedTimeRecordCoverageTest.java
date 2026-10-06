package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LgpdEmployeeExportResponse.ExportedTimeRecord.from() branch coverage:
 *  - geolocationPresent: lat!=null (T/F) and lng!=null when lat==null (T/F)
 *  - endGeolocationPresent: endLat!=null (T/F) and endLng!=null when endLat==null (T/F)
 *  - includePreciseGeolocation ternaries: TRUE/FALSE (4 ternaries)
 */
class ExportedTimeRecordCoverageTest {

    @Test
    void from_withAllNullCoords_includeFalse_geoAbsent() {
        var record = buildRecord(null, null, null, null);
        var exported = LgpdEmployeeExportResponse.ExportedTimeRecord.from(record, false);
        assertFalse(exported.geolocationPresent());
        assertFalse(exported.endGeolocationPresent());
        assertNull(exported.latitude());
        assertNull(exported.longitude());
        assertNull(exported.endLatitude());
        assertNull(exported.endLongitude());
    }

    @Test
    void from_withLatitudeSet_includeFalse_geoPresent() {
        // lat!=null → geolocationPresent TRUE branch
        var record = buildRecord(-23.5505, null, null, null);
        var exported = LgpdEmployeeExportResponse.ExportedTimeRecord.from(record, false);
        assertTrue(exported.geolocationPresent());
        assertNull(exported.latitude()); // include=FALSE → null (FALSE ternary branch)
    }

    @Test
    void from_withOnlyLongitudeSet_includeFalse_geoPresent() {
        // lat==null FALSE → lng!=null TRUE branch for geolocationPresent
        var record = buildRecord(null, -46.6333, null, null);
        var exported = LgpdEmployeeExportResponse.ExportedTimeRecord.from(record, false);
        assertTrue(exported.geolocationPresent());
        assertNull(exported.longitude()); // include=FALSE → null
    }

    @Test
    void from_withEndLatitudeSet_includeFalse_endGeoPresent() {
        // endLat!=null → endGeolocationPresent TRUE branch
        var record = buildRecord(null, null, -23.5505, null);
        var exported = LgpdEmployeeExportResponse.ExportedTimeRecord.from(record, false);
        assertFalse(exported.geolocationPresent());
        assertTrue(exported.endGeolocationPresent());
        assertNull(exported.endLatitude()); // include=FALSE → null
    }

    @Test
    void from_withOnlyEndLongitudeSet_includeFalse_endGeoPresent() {
        // endLat==null FALSE → endLng!=null TRUE branch for endGeolocationPresent
        var record = buildRecord(null, null, null, -46.6333);
        var exported = LgpdEmployeeExportResponse.ExportedTimeRecord.from(record, false);
        assertFalse(exported.geolocationPresent());
        assertTrue(exported.endGeolocationPresent());
        assertNull(exported.endLongitude()); // include=FALSE → null
    }

    @Test
    void from_withAllCoordsSet_includeTrue_returnsCoords() {
        // includePreciseGeolocation=true → all 4 ternary TRUE branches
        var record = buildRecord(-23.5505, -46.6333, -23.5506, -46.6334);
        var exported = LgpdEmployeeExportResponse.ExportedTimeRecord.from(record, true);
        assertTrue(exported.geolocationPresent());
        assertTrue(exported.endGeolocationPresent());
        assertEquals(-23.5505, exported.latitude());
        assertEquals(-46.6333, exported.longitude());
        assertEquals(-23.5506, exported.endLatitude());
        assertEquals(-46.6334, exported.endLongitude());
    }

    private TimeRecord buildRecord(Double lat, Double lng, Double endLat, Double endLng) {
        return new TimeRecord(
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(8),
                StatusRecord.CREATED,
                false,
                true,
                UUID.randomUUID(),
                lat, lng, endLat, endLng,
                null, null,
                null, null
        );
    }
}
