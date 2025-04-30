package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_SUP_CHG_AUDIT_BDY", indexes = {
        @Index(name = "IDX_UAM_AUDIT_BDY_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_AUDIT_BDY_AUDIT_ID", columnList = "AUDIT_ID"),
        @Index(name = "IDX_UAM_AUDIT_BDY_TYPE", columnList = "TYPE"),
        @Index(name = "IDX_UAM_AUDIT_BDY_FIELD_LABEL", columnList = "FIELD_LABEL"),
        @Index(name = "IDX_UAM_AUDIT_BDY_OLD_VALUE", columnList = "OLD_VALUE"),
        @Index(name = "IDX_UAM_AUDIT_BDY_NEW_VALUE", columnList = "NEW_VALUE")
})
public @Data class MxSupChgAuditBdy {

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

    @Column(name = "TYPE")
    private String type;

    @Column(name = "FIELD_LABEL")
    private String fieldLabel;

    @Column(name = "OLD_VALUE")
    private String oldValue;

    @Column(name = "NEW_VALUE")
    private String newValue;
}
