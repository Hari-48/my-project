package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

public @Data class AuditFields {

    public Long id;

    public Long auditId;

    public String userName;

    public String userGroup;

    public String type;

    public LocalDate compDate;

    public LocalTime compTime;

    public LocalDate systemDate;

    public String userDesk;

    public String refObject;

    public String action;

    public String fieldLabel;

    public String oldValue;

    public String newValue;

    public AuditFields(Long id,Long auditId, String userName, String userGroup, String type, LocalDate compDate,LocalTime compTime, LocalDate systemDate, String userDesk, String refObject, String action, String fieldLabel, String oldValue, String newValue) {
        this.id=id;
        this.auditId = auditId;
        this.userName = userName;
        this.userGroup = userGroup;
        this.type = type;
        this.compDate = compDate;
        this.compTime = compTime;
        this.systemDate = systemDate;
        this.userDesk = userDesk;
        this.refObject = refObject;
        this.action = action;
        this.fieldLabel = fieldLabel;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
