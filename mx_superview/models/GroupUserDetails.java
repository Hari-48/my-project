package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class GroupUserDetails {

    private Long id;
    private String groupLabel;
    private String grpRoleStr;
    private String grpDesc;
    private String template;
    private String userName;

    public GroupUserDetails(Long id, String groupLabel, String grpRoleStr, String grpDesc, String template, String userName) {
        this.id = id;
        this.groupLabel = groupLabel;
        this.grpRoleStr = grpRoleStr;
        this.grpDesc = grpDesc;
        this.template = template;
        this.userName = userName;
    }

    public GroupUserDetails(Long id, String groupLabel, String grpRoleStr, String grpDesc, String userName) {
        this.id = id;
        this.groupLabel = groupLabel;
        this.grpRoleStr = grpRoleStr;
        this.grpDesc = grpDesc;
        this.userName = userName;
    }

}
