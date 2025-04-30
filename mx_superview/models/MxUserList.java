package com.finsurge.tmr_portal.mx_superview.models;


import lombok.*;


@Getter
@Setter
public class MxUserList {

    private Long id;

    private String userName;

    private String descr;

    private String suspended;

    private String locked;

    private String code;

    private String mngmntPolicy;

    private String userLabel;

    private String licenseCatName;

    public MxUserList() {
    }

    public MxUserList(Long id,String userName, String descr, String suspended, String locked, String code, String mngmntPolicy,String userLabel,String licenseCatName)
    {
        this.id=id;
        this.userName=userName;
        this.descr=descr;
        this.suspended=suspended;
        this.locked=locked;
        this.code=code;
        this.mngmntPolicy=mngmntPolicy;
        this.userLabel=userLabel;
        this.licenseCatName=licenseCatName;
    }

}
