package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_CONSISTENCY_TMPL", indexes = {
        @Index(name = "IDX_UAM_CONS_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_CONS_CONSISTENCY_TMPL", columnList = "CONSISTENCY_TMPL"),
       @Index(name = "IDX_UAM_CONS_CONSISTENCY_TMPL_REP_DATE",columnList = "CONSISTENCY_TMPL,REP_DATE"),
        @Index(name = "IDX_UAM_CONS_CATEGORY", columnList = "CATEGORY"),
        @Index(name = "IDX_UAM_CONS_ITEM", columnList = "ITEM"),
        @Index(name = "IDX_UAM_CONS_ACCESS_RIGHT", columnList = "ACCESS_RIGHT"),
        @Index(name = "IDX_UAM_CONS_INSERT_RIGHT", columnList = "INSERT_RIGHT"),
        @Index(name = "IDX_UAM_CONS_MODIFY_RIGHT", columnList = "MODIFY_RIGHT"),
        @Index(name = "IDX_UAM_CONS_DELETE_RIGHT", columnList = "DELETE_RIGHT"),
        @Index(name = "IDX_UAM_CONS_MANDATORY_RIGHT", columnList = "MANDATORY_RIGHT"),
        @Index(name = "IDX_UAM_CONS_ACCOUNTING_RIGHT", columnList = "ACCOUNTING_RIGHT"),
        @Index(name = "IDX_UAM_CONS_PAYMENT_RIGHT", columnList = "PAYMENT_RIGHT"),
        @Index(name = "IDX_UAM_CONS_CONSISTENCY_PRIMARY_KEY",columnList = "REP_DATE,CONSISTENCY_TMPL,CATEGORY,ITEM")
})

public @Data class MxConsistencyTmpl {

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

    @Column(name = "CONSISTENCY_TMPL")
    private String consistencyTmpl;

    @Column(name = "CATEGORY")
    public String category;

    @Column(name = "ITEM")
    public String item;

    @Column(name = "ACCESS_RIGHT")
    public String accessRight;

    @Column(name = "INSERT_RIGHT")
    public String insertRight;

    @Column(name = "MODIFY_RIGHT")
    public String modifyRight;

    @Column(name = "DELETE_RIGHT")
    public String deleteRight;

    @Column(name = "MANDATORY_RIGHT")
    public String mandatoryRight;

    @Column(name = "ACCOUNTING_RIGHT")
    public String accountingRight;

    @Column(name = "PAYMENT_RIGHT")
    public String paymentRight;

    @Transient
    @Lob
    private String diff;


}
