package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="UAM_MX_STP_SOURCE_MODULE_EOD", indexes = {
        @Index(name = "IDX_STP_TYPO_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_STP_TYPO_SOURCE_MODULE", columnList = "SOURCE_MODULE"),
        @Index(name = "IDX_STP_TYPO_SOURCE_TEMPLATE", columnList = "SOURCE_TEMPLATE"),
        @Index(name = "IDX_STP_TYPO_SOURCE_MODULE_ACTION", columnList = "SOURCE_MODULE_ACTION"),
        @Index(name = "IDX_STP_TYPO_REP_DATE_SOURCE_MODULE", columnList = "REP_DATE,SOURCE_MODULE"),
        @Index(name = "IDX_STP_TYPO_SOURCE_PROCESSING_TEMPLATE", columnList = "PROCESSING_TEMPLATE"),
        @Index(name = "IDX_STP_TYPO_SOURCE_PROCESSING_CENTER", columnList = "PROCESSING_CENTER")

})
public @Data class MxStpSourceModuleEod {
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

    @Column(name = "MX_REP_DATE")
    private LocalDate mxReportDate;

    @Column(name="PROCESSING_CENTER")
    private String processingCenter;

    @Column(name="PROCESSING_TEMPLATE")
    private String processingTemplate;

    @Column(name="GLOBAL_TEMPLATE")
    private String globalTemplate;

    @Column(name="SOURCE_MODULE")
    private String sourceModule;

    @Column(name="SOURCE_TEMPLATE")
    private String sourceTemp;

    @Column(name="SOURCE_MODULE_ACTION")
    private String sourceModuleAction;
}
