package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_CWT_CONFIG_MGT_RIGHT", indexes = {
        @Index(name = "IDX_UAM_CWT_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_CWT_HIERARCHY_TMPL", columnList = "HIERARCHY_TMPL"),
        @Index(name = "IDX_UAM_CWT_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_CWT_IRS_LABEL", columnList = "IRS_LABEL"),
        @Index(name = "IDX_UAM_CWT_LD_LABEL", columnList = "LD_LABEL"),
        @Index(name = "IDX_UAM_CWT_CD_LABEL", columnList = "CD_LABEL"),
        @Index(name = "IDX_UAM_CWT_RTGA_LABEL", columnList = "RTGA_LABEL"),
        @Index(name = "IDX_UAM_CWT_JOIN_RIGHT_TMPL", columnList = "JOIN_RIGHT_TMPL"),
        @Index(name = "IDX_UAM_CWT_EXPORT", columnList = "EXPORT"),
        @Index(name = "IDX_UAM_CWT_IMPORT", columnList = "IMPORT"),
        @Index(name = "IDX_UAM_CWT_EDIT", columnList = "EDIT"),
        @Index(name = "IDX_UAM_CWT_PURGED", columnList = "PURGED"),
        @Index(name = "IDX_UAM_CWT_GROUP_LABEL_REP_DATE",columnList = "GROUP_LABEL,REP_DATE")
})

public @Data
class MxCwtConfigMgtRight {

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

    @Column(name = "GROUP_LABEL")
    private String groupLabel;

    @Column(name = "IRS_LABEL")
    private String irsLabel;

    @Column(name = "LD_LABEL")
    private String ldLabel;

    @Column(name = "CD_LABEL")
    private String cdLabel;

    @Column(name = "RTGA_LABEL")
    private String rtgaLabel;

    @Column(name = "HIERARCHY_TMPL")
    private String hierarchyTmpl;

    @Column(name = "JOIN_RIGHT_TMPL")
    private String joinRightTmpl;

    @Column(name = "EXPORT")
    private String export;

    @Column(name = "IMPORT")
    private String imports;

    @Column(name = "EDIT")
    private String edit;

    @Column(name = "PURGED")
    private String purge;

    @Transient
    @Lob
    private String diff;

}