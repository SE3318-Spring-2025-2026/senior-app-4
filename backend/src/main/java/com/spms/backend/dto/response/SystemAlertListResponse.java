package com.spms.backend.dto.response;

import java.util.List;

public class SystemAlertListResponse {

    private String message;
    private int total;
    private List<SystemAlertResponse> alerts;

    public SystemAlertListResponse() {}

    public SystemAlertListResponse(String message, int total, List<SystemAlertResponse> alerts) {
        this.message = message;
        this.total = total;
        this.alerts = alerts;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<SystemAlertResponse> getAlerts() { return alerts; }
    public void setAlerts(List<SystemAlertResponse> alerts) { this.alerts = alerts; }
}
