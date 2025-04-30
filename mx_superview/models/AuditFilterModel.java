package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

public @Data class AuditFilterModel {

    public Long auditId;

    public String userName;

    public String userGroup;

    public String compDate;

    private String compTime;


}
