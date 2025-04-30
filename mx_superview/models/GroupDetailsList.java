package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;


public @Data
class GroupDetailsList {

    private Long id;

    private String groupLabel;

    private String grpRoleStr;

    private String grpDesc;

    private String template;


    public GroupDetailsList(Long id,String groupLabel, String grpRoleStr, String grpDesc, String template) {
        this.id=id;
        this.groupLabel = groupLabel;
        this.grpRoleStr = grpRoleStr;
        this.grpDesc = grpDesc;
        this.template = template;
    }
}
