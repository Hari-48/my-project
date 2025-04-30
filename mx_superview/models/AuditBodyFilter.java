package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class AuditBodyFilter {

    public Long auditId;

    private String type;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

}
