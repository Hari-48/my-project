package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class CombineMxUserList {

    private Long id;

    private String userName;

    private String descr;

    private String suspended;

    private String locked;

    private String code;

    private String mngmntPolicy;

    private String licenseCatName;

    private String groupLabel;

    private String userLabel;

    public CombineMxUserList(Long id,String userName, String descr, String suspended, String locked, String code, String mngmntPolicy,  String licenseCatName,String groupLabel,String userLabel)
    {
        this.id=id;
        this.userName=userName;
        this.descr=descr;
        this.suspended=suspended;
        this.locked=locked;
        this.code=code;
        this.mngmntPolicy=mngmntPolicy;
        this.licenseCatName=licenseCatName;
        this.groupLabel=groupLabel;
        this.userLabel=userLabel;
    }
}
