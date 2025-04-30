package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class GroupPortfolioRights {
    public Long id;
    public String groupLabel;
    public String grpDesc;
    public String rightType;
    public String portfolioLabel;
    public String rights;
    public String treeLevel;
    public String portfolioType;
    public String description;
    public String pastCashProceed;
    public String level6;
    public String level5;
    public String level4;
    public String level3;
    public String level2;
    public String level1;
    public String level0;
    public String branch;
    public String department;
    public String ENTITY;
    public String prodtype;
    public String autoRoll;
    public String manualRoll;
    public String autoSweep;
    public String accSection;
    public String accCur;
    public String trdSection;
    public String closingEntity;
    public String procArea;
    public String legalEntity;
    public String comment1;
    public String comment2;
    public String comment3;
    public String comment4;
    public String comment5;
    public String comment6;

    public GroupPortfolioRights(Long id, String groupLabel, String grpDesc, String rightType, String portfolioLabel, String rights, String treeLevel, String portfolioType, String description, String pastCashProceed, String level6, String level5, String level4, String level3, String level2, String level1, String level0, String branch, String department, String ENTITY, String prodtype, String autoRoll, String manualRoll, String autoSweep, String accSection, String accCur, String trdSection, String closingEntity, String procArea, String legalEntity, String comment1, String comment2, String comment3, String comment4, String comment5, String comment6) {
        this.id = id;
        this.groupLabel = groupLabel;
        this.grpDesc = grpDesc;
        this.rightType = rightType;
        this.portfolioLabel = portfolioLabel;
        this.rights = rights;
        this.treeLevel = treeLevel;
        this.portfolioType = portfolioType;
        this.description = description;
        this.pastCashProceed = pastCashProceed;
        this.level6 = level6;
        this.level5 = level5;
        this.level4 = level4;
        this.level3 = level3;
        this.level2 = level2;
        this.level1 = level1;
        this.level0 = level0;
        this.branch = branch;
        this.department = department;
        this.ENTITY = ENTITY;
        this.prodtype = prodtype;
        this.autoRoll = autoRoll;
        this.manualRoll = manualRoll;
        this.autoSweep = autoSweep;
        this.accSection = accSection;
        this.accCur = accCur;
        this.trdSection = trdSection;
        this.closingEntity = closingEntity;
        this.procArea = procArea;
        this.legalEntity = legalEntity;
        this.comment1 = comment1;
        this.comment2 = comment2;
        this.comment3 = comment3;
        this.comment4 = comment4;
        this.comment5 = comment5;
        this.comment6 = comment6;
    }
}
