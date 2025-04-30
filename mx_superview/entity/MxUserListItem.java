package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_USER_LIST", indexes = {
        @Index(name = "IDX_UAM_USR_LIST_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_USR_LIST_USER_NAME", columnList = "USER_NAME"),
        @Index(name="IDX_UAM_USR_LIST_USER_LABEL", columnList = "USER_LABEL"),
        @Index(name = "IDX_UAM_USR_LIST_SUSPENDED", columnList = "SUSPENDED"),
        @Index(name = "IDX_UAM_USR_LIST_LOCKED", columnList = "LOCKED"),
        @Index(name = "IDX_UAM_USR_LIST_CODE", columnList = "CODE"),
        @Index(name = "IDX_UAM_USR_LIST_MNGMNT_POLICY", columnList = "MNGMNT_POLICY"),
        @Index(name = "IDX_UAM_USR_LIST_DESCR", columnList = "DESCR"),
        @Index(name = "IDX_UAM_USR_USER_NAME_REP_DATE_MNGMNT_POLICY",columnList = "USER_NAME,REP_DATE,MNGMNT_POLICY")


})
public @Data class MxUserListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SYS_DATE")
    private LocalDate sysDate;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "DESCR")
    private String descr;

    @Column(name = "SUSPENDED")
    private String suspended;

    @Column(name = "SUSP_SD")
    private LocalDate suspSd;

    @Column(name = "SUSP_ED")
    private LocalDate suspEd;

    @Column(name = "LOCKED")
    private String locked;

    @Column(name = "CODE")
    private String code;

    @Column(name = "MNGMNT_POLICY")
    private String mngmntPolicy;

    @Column(name="USER_LABEL")
    private String userLabel;

}
