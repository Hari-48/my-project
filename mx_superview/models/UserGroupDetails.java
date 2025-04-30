package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class UserGroupDetails {

    private  Long id;

    private String userName;

    private String userDesc;

    private String grpRoleStr;

    private String grpDesc;

    public UserGroupDetails(Long id,String userName, String userDesc, String grpRoleStr, String grpDesc) {
        this.id=id;
        this.userName = userName;
        this.userDesc = userDesc;
        this.grpRoleStr = grpRoleStr;
        this.grpDesc = grpDesc;
    }
}
