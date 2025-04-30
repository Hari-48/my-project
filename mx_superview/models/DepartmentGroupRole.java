package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class DepartmentGroupRole {

    private String groupLabel;
    private String groupRole;

    public DepartmentGroupRole(String groupLabel, String groupRole) {
        this.groupLabel = groupLabel;
        this.groupRole = groupRole;
    }

    public DepartmentGroupRole() {

    }
}
