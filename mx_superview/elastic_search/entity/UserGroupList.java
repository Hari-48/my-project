package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroupList {


    public final static String INDEX_NAME = "uam_user_group_list";
    public final static String MAPPING_PATH = "elastic/mappings/uam_user_group_list.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    @JsonIgnore
    private String version;// if mismatch between Elasticsearch json and java object

    private String groupLabel;
    private String grpRoleStr;
    private String grpTypeStr;
    private String stpRgtTmpl;
    private String grpDesc;
    private String createDate;
    private String modDatetime;
    private String consistencyTmpl;
    private String navigationTmpl;
    private String chineseWall;
    private String ospRightTemplate;
    private String fod;
    private String helpMonit;
    private String riskLimcheck;
    private String repTmpl;
    private String dstProf;
    private String rfqRole;
    private String viewEdit;
    private String layoutEdit;
    private String sfvAdmin;
    private String rqwhereE;
    private String sqlrgtTpl;
    private String edit;
    private String trdsqlQry;
    private String queryFilter;
    private String mreportOpd;
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
    private List<String> department;
    private String status = "inactive";

    private String sysDate;
    private String reportDate;
    private Long jobId;


}
