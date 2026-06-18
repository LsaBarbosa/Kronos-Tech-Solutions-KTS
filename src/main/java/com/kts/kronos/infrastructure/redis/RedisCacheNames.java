package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.cache.ApplicationCacheNames;

public final class RedisCacheNames {
    private RedisCacheNames() {
    }

    public static final String DASHBOARD_SUMMARY = ApplicationCacheNames.DASHBOARD_SUMMARY;
    public static final String RECORDS_ME_TODAY = ApplicationCacheNames.RECORDS_ME_TODAY;
    public static final String RECORDS_ME_RECENT = ApplicationCacheNames.RECORDS_ME_RECENT;
    public static final String RECORDS_ME_REQUESTS = ApplicationCacheNames.RECORDS_ME_REQUESTS;
    public static final String EMPLOYEE_OWN_PROFILE = ApplicationCacheNames.EMPLOYEE_OWN_PROFILE;
    public static final String USER_OWN_PROFILE = ApplicationCacheNames.USER_OWN_PROFILE;
    public static final String EMPLOYEE_LIST = ApplicationCacheNames.EMPLOYEE_LIST;
    public static final String USER_LIST = ApplicationCacheNames.USER_LIST;
    public static final String COMPANY_GET = ApplicationCacheNames.COMPANY_GET;
    public static final String GEOLOCATION_RESOLVE = ApplicationCacheNames.GEOLOCATION_RESOLVE;
    public static final String PUBLIC_PROCESSING_CATALOG = ApplicationCacheNames.PUBLIC_PROCESSING_CATALOG;
    public static final String PUBLIC_PRIVACY_POLICY = ApplicationCacheNames.PUBLIC_PRIVACY_POLICY;
    public static final String PUBLIC_BIOMETRIC_TERM = ApplicationCacheNames.PUBLIC_BIOMETRIC_TERM;
}
