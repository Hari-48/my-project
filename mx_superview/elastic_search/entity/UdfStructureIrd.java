package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UdfStructureIrd {
    public final static String INDEX_NAME = "uam_udf_structure_ird";
    public final static String MAPPING_PATH = "elastic/mappings/uam_udf_structure_ird.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String tradabilityStatus;
    private String bimTradeDate;
    private String bimTypology;
    private String islamicCode;
    private String tradabilityMessage;
    private String foComment;
    private String boComment;
    private Integer fop;
    private String specialLendingCode;
    private String iaMethod;
    private Integer amount;
    private Double rrHaircut;
    private String rmName;
    private String dtccUtiPrefix;
    private String dtccPriorUtiVal;
    private String dtccPriorUtiPfx;
    private String dtccUtiValue;
    private String dtccUpdateDate;
    private String dtccUsiUpdated;
    private String amentComment;
    private String nextResetDate;
    private String externamImpact;
    private String loanAc;
    private String source;
    private String t24Branch;
    private Integer dtccElectConf;
    private String categoryCode;
    private Integer assetType;
    private Integer dtccNexusFlag;
    private String rtgsStatus;
    private Double day1pl;
    private String eqdNote;
    private String afterHour;
    private String offPremises;
    private String pds;
    private String aooClientTrade;
    private String endBeneficiary;
    private String purpose;
    private String subPurpose;
    private Double bimNotional;
    private String bimNotionalOverview;
    private String bimPackageType;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
