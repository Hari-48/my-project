package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_OSP_RIGHTS_MATRIX", indexes = {
        @Index(name = "IDX_UAM_OSP_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_OSP_RIGHT_TEMPLATE", columnList = "OSP_RIGHT_TEMPLATE"),
        @Index(name = "IDX_UAM_OSP_RIGHT_TEMPLATE_REP_DATE",columnList = "OSP_RIGHT_TEMPLATE,REP_DATE"),
        @Index(name = "IDX_UAM_OSP_VALIDATION_RIGHT_TEMPLATE", columnList = "VALIDATION_RIGHT_TEMPLATE"),
        @Index(name = "IDX_UAM_OSP_CATEGORY", columnList = "CATEGORY"),
        @Index(name = "IDX_UAM_OSP_SUB_CATEGORY", columnList = "SUB_CATEGORY"),
        @Index(name = "IDX_UAM_OSP_QUEUE", columnList = "QUEUE"),
        @Index(name = "IDX_UAM_OSP_QUEUE_RIGHT", columnList = "QUEUE_RIGHT"),
        @Index(name = "IDX_UAM_OSP_BULK_VALIDATION_ENABLED", columnList = "BULK_VALIDATION_ENABLED"),
        @Index(name = "IDX_UAM_OSP_NON_MODIFIABLE_AUTO_SELECTION", columnList = "NON_MODIFIABLE_AUTO_SELECTION"),
        @Index(name = "IDX_UAM_OSP_FILTER_ON_DATA", columnList = "FILTER_ON_DATA"),
        @Index(name = "IDX_UAM_OSP_DATA_FILTER_SHARED", columnList = "DATA_FILTER_SHARED"),
        @Index(name = "IDX_UAM_OSP_ACTION", columnList = "ACTION"),
        @Index(name = "IDX_UAM_OSP_USER_ACTION_ENABLED", columnList = "USER_ACTION_ENABLED"),
        @Index(name = "IDX_UAM_OSP_FILTER_ON_ACTION", columnList = "FILTER_ON_ACTION"),
        @Index(name = "IDX_UAM_OSP_ACTION_FILTER_SHARED", columnList = "ACTION_FILTER_SHARED"),
        @Index(name = "IDX_UAM_OSP_TECHNICAL", columnList = "TECHNICAL")
//        @Index(name ="IDX_UAM_OSP_PRIMARY",columnList = "REP_DATE,OSP_RIGHT_TEMPLATE,VALIDATION_RIGHT_TEMPLATE, CATEGORY, SUB_CATEGORY, QUEUE,ACTION")
})
public @Data class MxOspRightsMatrix {

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

    @Column(name = "OSP_RIGHT_TEMPLATE")
    private String ospRightTemplate;

    @Column(name = "VALIDATION_RIGHT_TEMPLATE")
    public String validationRightTemplate;

    @Column(name = "CATEGORY")
    public String category;

    @Column(name = "SUB_CATEGORY")
    public String subCategory;

    @Column(name = "QUEUE")
    public String queue;

    @Column(name = "QUEUE_RIGHT")
    public String queueRight;

    @Column(name = "BULK_VALIDATION_ENABLED")
    public String bulkValidationEnabled;

    @Column(name = "NON_MODIFIABLE_AUTO_SELECTION")
    public String nonModifiableAutoSelection;

    @Column(name = "FILTER_ON_DATA")
    public String filterOnData;

    @Column(name = "DATA_FILTER_SHARED")
    public String dataFilterShared;

    @Column(name = "ACTION")
    public String action;

    @Column(name = "USER_ACTION_ENABLED")
    public String userActionEnabled;

    @Column(name = "FILTER_ON_ACTION")
    public String filterOnAction;

    @Column(name = "ACTION_FILTER_SHARED")
    public String actionFilterShared;

    @Column(name = "TECHNICAL")
    public String technical;

    // to store the diff values of compare
    @Transient
    @Lob
    private String diff;
}
