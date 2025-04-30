package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class CountUserDetails {

    private Long id;

    private String groupLabel;

    private String grpRoleStr;

    private String grpDesc;

    private String template;

    private Long count;

    public CountUserDetails(Long id, String groupLabel, String grpRoleStr, String grpDesc, String template, Long count) {
        this.id = id;
        this.groupLabel = groupLabel;
        this.grpRoleStr = grpRoleStr;
        this.grpDesc = grpDesc;
        this.template = template;
        this.count = count;
    }
}
