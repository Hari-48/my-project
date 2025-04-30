package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_PORTFOLIO_LABEL",indexes = {
        @Index(name = "IDX_UAM_PORTFOLIO_REP_DATE",columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_PORTFOLIO_PORTFOLIO_LABEL",columnList = "PORTFOLIO_LABEL"),
        @Index(name = "IDX_UAM_PORTFOLIO_TREE_LEVEL",columnList = "TREE_LEVEL"),
        @Index(name = "IDX_UAM_PORTFOLIO_PORTFOLIO_TYPE",columnList = "PORTFOLIO_TYPE"),
        @Index(name = "IDX_UAM_PORTFOLIO_PARENT_PFOLIO",columnList = "PARENT_PFOLIO")
})
public @Data class MxPortfolioLabel {

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

    @Column(name = "PORTFOLIO_LABEL")
    private String portfolioLabel;

    @Column(name = "TREE_LEVEL")
    private String treeLevel;

    @Column(name = "PORTFOLIO_TYPE")
    private String portfolioType;

    @Column(name = "PARENT_PFOLIO")
    private String parentPortFolio;
}
