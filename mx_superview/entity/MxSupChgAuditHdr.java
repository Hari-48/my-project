package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "UAM_MX_SUPCHG_AUDIT_HDR", indexes = {
        @Index(name = "IDX_UAM_AUDIT_HDR_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_AUDIT_HDR_AUDIT_ID", columnList = "AUDIT_ID"),
        @Index(name = "IDX_UAM_AUDIT_HDR_USER_NAME", columnList = "USER_NAME"),
        @Index(name = "IDX_UAM_AUDIT_HDR_USER_GROUP", columnList = "USER_GROUP"),
        @Index(name = "IDX_UAM_AUDIT_HDR_COMP_DATE", columnList = "COMP_DATE"),
        @Index(name = "IDX_UAM_AUDIT_HDR_COMP_TIME", columnList = "COMP_TIME"),
        @Index(name = "IDX_UAM_AUDIT_HDR_ACTION", columnList = "ACTION"),
        @Index(name = "IDX_UAM_AUDIT_HDR_TYPE", columnList = "TYPE"),
        @Index(name = "IDX_UAM_AUDIT_HDR_SYSTEM_DATE", columnList = "SYSTEM_DATE"),
        @Index(name = "IDX_UAM_AUDIT_HDR_REF_OBJECT", columnList = "REF_OBJECT"),
        @Index(name = "IDX_UAM_AUDIT_HDR_USER_DESK", columnList = "USER_DESK")
})

public @Data class MxSupChgAuditHdr {

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

    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "USER_GROUP")
    private String userGroup;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "COMP_DATE")
    private LocalDate compDate;

    @Column(name = "COMP_TIME",length =20)
    private String compTime;

    @Column(name = "SYSTEM_DATE")
    private LocalDate systemDate;

    @Column(name = "USER_DESK")
    private String userDesk;

    @Column(name = "REF_OBJECT")
    private String refObject;

    @Column(name = "ACTION")
    private String action;

}
