package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.Lob;
import javax.persistence.Transient;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupPortfolioRights {

    public final static String INDEX_NAME = "uam_group_portfolio_rights";
    public final static String MAPPING_PATH = "elastic/mappings/uam_group_portfolio_rights.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String groupLabel;
    private String grpDesc;
    private String rightType;
    private String portfolioLabel;
    private String rights;
    private String treeLevel;
    private String portfolioType;
    private String description;
    private String pastcashProceed;
    private String level6;
    private String level5;
    private String level4;
    private String level3;
    private String level2;
    private String level1;
    private String level0;
    private String branch;
    private String department;
    private String entity;
    private String prodType;
    private String autoRoll;
    private String manualRoll;
    private String autoSweep;
    private String accSection;
    private String accCur;
    private String trdSection;
    private String closingEntity;
    private String procArea;
    private String legalEntity;
    private String comment1;
    private String comment2;
    private String comment3;
    private String comment4;
    private String comment5;
    private String comment6;
    private Integer countLive;
    private Integer countDead;
    private String status = "active";
    @Transient
    @Lob
    private String diff;
    private String sysDate;
    private String reportDate;
    private Long jobId;


}
