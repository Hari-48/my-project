package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;
;

import java.util.List;

public @Data class PortfolioRights {

    private List<String> rightTypeList;

    public String portfolioLabel;

    public String PORTFOLIO_LABEL;

    public List<String> portfolioLabelList;

    public List<String> rightsList;

    public List<String> treeLevelList;

    public List<String> portfolioTypeList;

    public List<String> branchList;

    public List<String> departmentList;

    public List<String> ENTITYList;

    public List<String> prodtypeList;

    public List<String> autoRollList;

    public List<String> manualRollList;

    public List<String> autoSweepList;

    public List<String> accSectionList;

    public List<String> accCurList;

    public List<String> trdSectionList;

    public List<String> closingEntityList;

    public List<String> procAreaList;

    public List<String> legalEntityList;

    public List<String> grpDescList;

    public List<String> level0List;

    public List<String> level1List;

    public List<String> level2List;

    public List<String> level3List;

    public List<String> level4List;

    public List<String> level5List;

    public List<String> level6List;

    public List<String> descriptionList;

    public List<String> comment1List;

    public List<String> comment2List;

    public List<String> comment3List;

    public List<String> comment4List;

    public List<String> comment5List;

    public List<String> pastCashProceedList;

    public PortfolioRights(String portfolioLabel) {
        this.portfolioLabel = portfolioLabel;
    }
    public PortfolioRights() {
    }
}