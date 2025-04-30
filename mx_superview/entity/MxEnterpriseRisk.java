package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_ENTERPRISE_RISK", indexes = {
        @Index(name = "IDX_UAM_ENT_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_ENT_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_ENT_RIGHTS_TO_CREATE_EDIT_DELETE_MARKET_RISKS", columnList = "RIGHTS_TO_CREATE_EDIT_DELETE_MARKET_RISKS"),
        @Index(name = "IDX_UAM_ENT_RIGHTS_TO_CONTROL_EFFECTIVE_DATE", columnList = "RIGHTS_TO_CONTROL_EFFECTIVE_DATE"),
        @Index(name = "IDX_UAM_ENT_RIGHTS_TO_IMPORT_LIMIT_PERMISSIONS", columnList = "RIGHTS_TO_IMPORT_LIMIT_PERMISSIONS"),
        @Index(name = "IDX_UAM_ENT_GROUP_LABEL_REP_DATE",columnList = "GROUP_LABEL,REP_DATE")
})
public @Data class MxEnterpriseRisk {

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
    private String label;

    @Column(name = "RIGHTS_TO_CREATE_EDIT_DELETE_MARKET_RISKS")
    private String modRsk;

    @Column(name = "RIGHTS_TO_CONTROL_EFFECTIVE_DATE")
    private String modPst;

    @Column(name = "RIGHTS_TO_IMPORT_LIMIT_PERMISSIONS")
    private String upload;

    @Transient
    @Lob
    private String diff;

}
