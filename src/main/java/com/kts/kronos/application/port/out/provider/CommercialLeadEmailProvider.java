package com.kts.kronos.application.port.out.provider;

public interface CommercialLeadEmailProvider {
    void sendLeadNotification(String name, String company, String corporateEmail);
}
