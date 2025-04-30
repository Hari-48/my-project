package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_STP_RIGHTS_TYPOLOGY_EOD",indexes = {
        @Index(name = "IDX_UAM_STP_TYPOLOGY_TYPOLOGY_GROUP", columnList = "TYPOLOGY_GROUP"),
        @Index(name = "IDX_UAM_STP_TYPOLOGY_TYPOLOGY", columnList = "TYPOLOGY"),
        @Index(name = "IDX_UAM_STP_TYPOLOGY_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_STP_TYPOLOGY_MX_REP_DATE", columnList = "MX_REP_DATE"),
        @Index(name = "IDX_UAM_STP_TYPOLOGY_REP_DATE_TYPOLOGY_GROUP", columnList = "REP_DATE,TYPOLOGY_GROUP")
})
public @Data
class MxStpRightsTypologyEod {

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

    @Column(name = "TYPOLOGY_GROUP")
    private String typologyGroup;

    @Column(name = "TYPOLOGY")
    private String typology;

}
