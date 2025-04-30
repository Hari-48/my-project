package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_DISTRIBUTION", indexes = {
        @Index(name = "IDX_UAM_DIST_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_DIST_DST_PROF", columnList = "DST_PROF")
})
public @Data class MxDistribution {

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

    @Column(name = "DST_PROF")
    private String dstPrf;

    @Column(name = "ARC_TIME")
    private String arcTime;

    @Column(name = "NB_LEGS")
    private String nbLegs;

    @Column(name = "MP_TIME")
    private String mpTime;

    @Column(name = "LOADER_REF")
    private String loaderref;

    @Column(name = "DATEFMT")
    private String dateFmt;
}
