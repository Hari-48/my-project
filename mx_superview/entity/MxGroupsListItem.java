package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_GROUP_LIST", indexes = {
        @Index(name = "IDX_UAM_GRP_LIST_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_GRP_LIST_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_GRP_LIST_CONSITENCY_TMPL", columnList = "CONSITENCY_TMPL"),
        @Index(name = "IDX_UAM_GRP_LIST_OSP_RIGHT_TEMPLATE", columnList = "OSP_RIGHT_TEMPLATE"),
        @Index(name = "IDX_UAM_GRP_LIST_STP_RGT_TMPL", columnList = "STP_RGT_TMPL"),
        @Index(name = "IDX_UAM_GRP_LIST_NAVIGATION_TMPL", columnList = "NAVIGATION_TMPL"),
        @Index(name = "IDX_UAM_GRP_LIST_CHINESE_WALL", columnList = "CHINESE_WALL"),
        @Index(name = "IDX_UAM_GRP_LIST_LPOS_RIGHT", columnList = "LPOS_RIGHT"),
        @Index(name = "IDX_UAM_GRP_LIST_NKEY_TMPL", columnList = "NKEY_TMPL"),
        @Index(name = "IDX_UAM_GRP_LIST_STAT_TMPL", columnList = "STAT_TMPL"),
        @Index(name = "IDX_UAM_GRP_LIST_ACC_CTRL", columnList = "ACC_CTRL")
})
public @Data class MxGroupsListItem {

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

    @Column(name = "GRP_ROLE_STR")
    private String grpRoleStr;

    @Column(name = "GRP_TYPE_STR")
    private String grpTypeStr;

    @Column(name = "GRP_DESC")
    private String grpDesc;

    @Column(name = "STP_RGT_TMPL")
    private String stpRgtTmpl;

    @Column(name = "CONSITENCY_TMPL")
    private String consitencyTmpl;

    @Column(name = "NAVIGATION_TMPL")
    private String navigationTmpl;

    @Column(name = "CHINESE_WALL")
    private String chineseWall;

    @Column(name = "OSP_RIGHT_TEMPLATE")
    private String ospRightTemplate;

    @Column(name = "FOD")
    private String fod;

    @Column(name = "HELP_MONIT")
    private String helpMonit;

    @Column(name = "RISK_LIMCHECK")
    private String riskLimCheck;

    @Column(name = "REP_TMPL")
    private String repTmpl;

    @Column(name = "DST_PROF")
    private String dstProf;

    @Column(name = "RFQ_ROLE")
    private String rfqRole;

    @Column(name = "VIEW_EDIT")
    private String viewEdit;

    @Column(name = "LAYOUT_EDIT")
    private String layoutEdit;

    @Column(name = "SFV_ADMIN")
    private String sfvAdmin;

    @Column(name = "RQWHERE_E")
    private String rqWhereE;

    @Column(name = "SQLRGT_TPL")
    private String sqlRgtTpl;

    @Column(name = "EDIT")
    private String edit;

    @Column(name = "TRDSQL_QRY")
    private String trdSqlQry;

    @Column(name = "QUERY_FILTER")
    private String queryFilter;

    @Column(name = "MREPORT_OPD")
    private String mReportOpd;

    @Column(name = "DATE_MODE")
    private String dateMode;

    @Column(name = "IRS_LABEL")
    private String irsLabel;

    @Column(name = "LD_LABEL")
    private String ldLabel;

    @Column(name = "CD_LABEL")
    private String cdLabel;

    @Column(name = "RTGA_LABEL")
    private String rtgaLabel;

    @Column(name = "ACC_MODE")
    private String accMode;

    @Column(name = "LPOS_RIGHT")
    private String lposRight;

    @Column(name = "NKEY_TMPL")
    private String nkeyTmpl;

    @Column(name = "STAT_TMPL")
    private String statTmpl;

    @Column(name = "ACC_CTRL")
    private String accCtrl;

    @Column(name = "CREATE_DATE")
    private String createDate;

    @Column(name = "MOD_DATETIME")
    private String modDateTime;

    @Column(name = "iDay")
    private Integer iDay;

    @Column(name = "iMonth")
    private Integer iMonth;

    @Column(name = "iYear")
    private Integer iYear;
}
