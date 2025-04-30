package com.finsurge.tmr_portal.mx_superview.entity;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_FINANCE_ACCTRL_RIGHTS", indexes = {
        @Index(name = "IDX_UAM_FIN_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_FIN_DESCRIPTION", columnList = "DESCRIPTION"),
        @Index(name = "IDX_UAM_FIN_FILTER", columnList = "FILTER"),
        @Index(name = "IDX_UAM_FIN_TMPL_TYPE", columnList = "TMPL_TYPE"),
        @Index(name = "IDX_UAM_FIN_TEMPLATE", columnList = "TEMPLATE"),
        @Index(name = "IDX_UAM_FIN_FIL_DESC", columnList = "FIL_DESC"),
        @Index(name = "IDX_UAM_FIN_TEMPLATE_DATE_TMPL_TYPE",columnList = "TEMPLATE,REP_DATE,TMPL_TYPE"),
})
public @Data class MxFinaceAcctrlRights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SYS_DATE")
    private LocalDate sysDate;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

    @Column(name = "TMPL_TYPE")
    private String tmplType;

    @Column(name = "TEMPLATE")
    private String template;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "FILTER")
    private String filter;

    @Column(name = "FIL_DESC")
    private String filDesc;

    @Transient
    @Lob
    private String diff;

}
