package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_GROUP_PORTFOLIO_RIGHTS", indexes = {
        @Index(name = "IDX_UAM_GRP_PORT_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_GRP_PORT_PORTFOLIO_LABEL", columnList = "PORTFOLIO_LABEL"),
        @Index(name = "IDX_UAM_GRP_PORT_PORTFOLIO_TYPE", columnList = "PORTFOLIO_TYPE"),
        @Index(name = "IDX_UAM_GRP_PORT_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_GRP_PORT_GRP_DESC", columnList = "GRP_DESC"),
        @Index(name = "IDX_UAM_GRP_PORT_RIGHT_TYPE", columnList = "RIGHT_TYPE"),
        @Index(name = "IDX_UAM_GRP_PORT_RIGHTS", columnList = "RIGHTS"),
        @Index(name = "IDX_UAM_GRP_PORT_TREE_LEVEL", columnList = "TREE_LEVEL"),
        @Index(name = "IDX_UAM_GRP_PORT_DESCRIPTION", columnList = "DESCRIPTION"),
        @Index(name = "IDX_UAM_GRP_PORT_PAST_CASH_PROCEED", columnList = "PAST_CASH_PROCEED"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL6", columnList = "LEVEL6"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL5", columnList = "LEVEL5"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL4", columnList = "LEVEL4"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL3", columnList = "LEVEL3"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL2", columnList = "LEVEL2"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL1", columnList = "LEVEL1"),
        @Index(name = "IDX_UAM_GRP_PORT_LEVEL0", columnList = "LEVEL0"),
        @Index(name = "IDX_UAM_GRP_PORT_BRANCH", columnList = "BRANCH"),
        @Index(name = "IDX_UAM_GRP_PORT_DEPARTMENT", columnList = "DEPARTMENT"),
        @Index(name = "IDX_UAM_GRP_PORT_ENTITY", columnList = "ENTITY"),
        @Index(name = "IDX_UAM_GRP_PORT_PROD_TYPE", columnList = "PROD_TYPE"),
        @Index(name = "IDX_UAM_GRP_PORT_AUTO_ROLL", columnList = "AUTO_ROLL"),
        @Index(name = "IDX_UAM_GRP_PORT_MANUAL_ROLL", columnList = "MANUAL_ROLL"),
        @Index(name = "IDX_UAM_GRP_PORT_AUTO_SWEEP", columnList = "AUTO_SWEEP"),
        @Index(name = "IDX_UAM_GRP_PORT_ACC_SECTION", columnList = "ACC_SECTION"),
        @Index(name = "IDX_UAM_GRP_PORT_ACC_CUR", columnList = "ACC_CUR"),
        @Index(name = "IDX_UAM_GRP_PORT_TRD_SECTION", columnList = "TRD_SECTION"),
        @Index(name = "IDX_UAM_GRP_PORT_CLOSING_ENTITY", columnList = "CLOSING_ENTITY"),
        @Index(name = "IDX_UAM_GRP_PORT_PROC_AREA", columnList = "PROC_AREA"),
        @Index(name = "IDX_UAM_GRP_PORT_LEGAL_ENTITY", columnList = "LEGAL_ENTITY"),
        @Index(name = "IDX_UAM_GRP_PORT_COMMENT1", columnList = "COMMENT1"),
        @Index(name = "IDX_UAM_GRP_PORT_COMMENT2", columnList = "COMMENT2"),
        @Index(name = "IDX_UAM_GRP_PORT_COMMENT3", columnList = "COMMENT3"),
        @Index(name = "IDX_UAM_GRP_PORT_COMMENT4", columnList = "COMMENT4"),
        @Index(name = "IDX_UAM_GRP_PORT_COMMENT5", columnList = "COMMENT5"),
        @Index(name = "IDX_UAM_GRP_PORT_COMMENT6", columnList = "COMMENT6"),
        @Index(name = "IDX_UAM_GRP_PORT_GROUP_LABEL_REP_DATE",columnList = "GROUP_LABEL,REP_DATE"),
        @Index(name = "IDX_UAM_GRP_PORT_PRIMARY_KEY",columnList = "GROUP_LABEL,REP_DATE,PORTFOLIO_LABEL")

})
public @Data class MxGroupPortfolioRights {

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

    @Column(name = "GROUP_LABEL",length = 300)
    private String groupLabel;

    @Column(name = "GRP_DESC")
    public String grpDesc;

    @Column(name = "RIGHT_TYPE")
    public String rightType;

    @Column(name = "PORTFOLIO_LABEL")
    public String portfolioLabel;

    @Column(name = "RIGHTS")
    private String rights;

    @Column(name = "TREE_LEVEL")
    public String treeLevel;

    @Column(name = "PORTFOLIO_TYPE")
    public String portfolioType;

    @Column(name = "DESCRIPTION")
    public String description;

    @Column(name = "PAST_CASH_PROCEED")
    public String pastCashProceed;

    @Column(name = "LEVEL6")
    public String level6;

    @Column(name = "LEVEL5")
    public String level5;

    @Column(name = "LEVEL4")
    public String level4;

    @Column(name = "LEVEL3")
    public String level3;

    @Column(name = "LEVEL2")
    public String level2;

    @Column(name = "LEVEL1")
    public String level1;

    @Column(name = "LEVEL0")
    public String level0;

    @Column(name = "BRANCH")
    public String branch;

    @Column(name = "DEPARTMENT")
    public String department;

    @Column(name = "ENTITY")
    public String ENTITY;

    @Column(name = "PROD_TYPE")
    public String prodtype;

    @Column(name = "AUTO_ROLL")
    public String autoRoll;

    @Column(name = "MANUAL_ROLL")
    public String manualRoll;

    @Column(name = "AUTO_SWEEP")
    public String autoSweep;

    @Column(name = "ACC_SECTION")
    public String accSection;

    @Column(name = "ACC_CUR")
    public String accCur;

    @Column(name = "TRD_SECTION")
    public String trdSection;

    @Column(name = "CLOSING_ENTITY")
    public String closingEntity;

    @Column(name = "PROC_AREA")
    public String procArea;

    @Column(name = "LEGAL_ENTITY")
    public String legalEntity;

    @Column(name = "COMMENT1")
    public String comment1;

    @Column(name = "COMMENT2")
    public String comment2;

    @Column(name = "COMMENT3")
    public String comment3;

    @Column(name = "COMMENT4")
    public String comment4;

    @Column(name = "COMMENT5")
    public String comment5;

    @Column(name = "COMMENT6")
    public String comment6;

    @Transient
    @Lob
    private String diff;

}
