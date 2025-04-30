package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class GroupDepartment {
    private Long id;
    private String groupLabel;
    private String groupRole;

    public GroupDepartment(Long id,String groupLabel, String groupRole) {
        this.id=id;
        this.groupLabel = groupLabel;
        this.groupRole = groupRole;
    }
}