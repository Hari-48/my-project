package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

public @Data class GroupDetails {

    private String groupLabel;

    private String grpRoleStr;

    private String grpTypeStr;

    private String grpDesc;

    private String stpRgtTmpl;

    private String consitencyTmpl;

    private String navigationTmpl;

    private String chineseWall;

    private String ospRightTemplate;

    private String fod;

    private String helpMonit;

    private String riskLimCheck;

    private String repTmpl;

    private String dstProf;

    private String rfqRole;

    private String viewEdit;

    private String layoutEdit;

    private String sfvAdmin;

    private String rqWhereE;

    private String sqlRgtTpl;

    private String edit;

    private String trdSqlQry;

    private String queryFilter;

    private String mReportOpd;

    private String dateMode;

    private String irsLabel;

    private String ldLabel;

    private String cdLabel;

    private String rtgaLabel;

    private String accMode;

    private String lposRight;

    private String nkeyTmpl;

    private String statTmpl;

    private String accCtrl;

    private String createDate;

    private String modDateTime;

    private BigInteger usersCount;

    public GroupDetails(String groupLabel, String grpRoleStr, String grpTypeStr, String grpDesc, String stpRgtTmpl, String consitencyTmpl, String navigationTmpl, String chineseWall, String ospRightTemplate, String fod, String helpMonit, String riskLimCheck, String repTmpl, String dstProf, String rfqRole, String viewEdit, String layoutEdit, String sfvAdmin, String rqWhereE, String sqlRgtTpl, String edit, String trdSqlQry,
                         String queryFilter, String mReportOpd, String dateMode, String irsLabel, String ldLabel, String cdLabel, String rtgaLabel, String accMode, String lposRight, String nkeyTmpl, String statTmpl, String accCtrl, String createDate, String modDateTime) {
        this.groupLabel = groupLabel;
        this.grpRoleStr = grpRoleStr;
        this.grpTypeStr = grpTypeStr;
        this.grpDesc = grpDesc;
        this.stpRgtTmpl = stpRgtTmpl;
        this.consitencyTmpl = consitencyTmpl;
        this.navigationTmpl = navigationTmpl;
        this.chineseWall = chineseWall;
        this.ospRightTemplate = ospRightTemplate;
        this.fod = fod;
        this.helpMonit = helpMonit;
        this.riskLimCheck = riskLimCheck;
        this.repTmpl = repTmpl;
        this.dstProf = dstProf;
        this.rfqRole = rfqRole;
        this.viewEdit = viewEdit;
        this.layoutEdit = layoutEdit;
        this.sfvAdmin = sfvAdmin;
        this.rqWhereE = rqWhereE;
        this.sqlRgtTpl = sqlRgtTpl;
        this.edit = edit;
        this.trdSqlQry = trdSqlQry;
        this.queryFilter = queryFilter;
        this.mReportOpd = mReportOpd;
        this.dateMode = dateMode;
        this.irsLabel = irsLabel;
        this.ldLabel = ldLabel;
        this.cdLabel = cdLabel;
        this.rtgaLabel = rtgaLabel;
        this.accMode = accMode;
        this.lposRight = lposRight;
        this.nkeyTmpl = nkeyTmpl;
        this.statTmpl = statTmpl;
        this.accCtrl = accCtrl;
        this.createDate = createDate;
        this.modDateTime= modDateTime;
    }

    public GroupDetails() {

    }
}
