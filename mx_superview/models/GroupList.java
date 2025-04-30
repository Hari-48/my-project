package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class GroupList {
    private Long id;
    private String groupLabel;
    private String groupRole;
    private Long count;

    public GroupList() {
    }

    public GroupList(Long id,String groupLabel, String groupRole) {
        this.id=id;
        this.groupLabel = groupLabel;
        this.groupRole = groupRole;
    }

    public GroupList(Long id, String groupLabel, String groupRole, Long count) {
        this.id = id;
        this.groupLabel = groupLabel;
        this.groupRole = groupRole;
        this.count = count;
    }
}
