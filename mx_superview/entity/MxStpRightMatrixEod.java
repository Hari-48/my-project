package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_STP_RIGHTS_MATRIX_EOD", indexes = {
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_GLOBAL_TEMPLATE", columnList = "GLOBAL_TEMPLATE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_BO_TYPE", columnList = "BO_TYPE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_SRC_MODULE", columnList = "SRC_MODULE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_BO_TEMPLATE", columnList = "BO_TEMPLATE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_GROUPING_TEMPLATE", columnList = "GROUPING_TEMPLATE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_TYPOLOGY_GROUP", columnList = "TYPOLOGY_GROUP"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_TYPOLOGY", columnList = "TYPOLOGY"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_RIGHTS_PROFILE", columnList = "RIGHTS_PROFILE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_ACTION_EVENT", columnList = "ACTION_EVENT"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_VIEWS", columnList = "VIEWS"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_REP_DATE_GLOBAL_TEMPLATE", columnList = "REP_DATE,GLOBAL_TEMPLATE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_REP_DATE_RIGHTS_PROFILE", columnList = "REP_DATE,RIGHTS_PROFILE"),
        @Index(name = "IDX_UAM_STP_RIGHTS_MATRIX_EOD_BO_TEMPLATE_BO_TYPE", columnList = "BO_TEMPLATE,BO_TYPE"),
        @Index(name = "IDX_UAM_MATRIX_EOD_BO_TEMPLATE_BO_TYPE_GLOBAL_TEMPLATE", columnList = "BO_TEMPLATE,BO_TYPE,GLOBAL_TEMPLATE"),
        @Index(name = "IDX_UAM_MATRIX_EOD_REP_DATE_RIGHTS_PROFILE_GLOBAL_TEMPLATE", columnList = "REP_DATE,RIGHTS_PROFILE,GLOBAL_TEMPLATE")

})
public @Data class MxStpRightMatrixEod {
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

    @Column(name = "GLOBAL_TEMPLATE")
    private String globalTemplate;

    @Column(name = "SRC_MODULE")
    private String srcModule;

    @Column(name = "BO_TYPE")
    private String boType;

    @Column(name = "BO_TEMPLATE")
    private String boTemplate;

    @Column(name = "GROUPING_TEMPLATE")
    private String groupingTemplate;

    @Column(name = "TYPOLOGY_GROUP")
    private String typologyGroup;

    @Column(name = "TYPOLOGY")
    private String typology;

    @Column(name = "RIGHTS_PROFILE")
    private String rightProfile;

    @Column(name = "ACTION_EVENT")
    private String actionEvent;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "VIEWS")
    private String view;

    @Transient
    @Lob
    private String diff;
}
