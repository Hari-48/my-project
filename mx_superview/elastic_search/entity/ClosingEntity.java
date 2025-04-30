package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.nio.file.Paths;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ClosingEntity {


    public final static String INDEX_NAME = "uam_closing_entity";
    public final static String MAPPING_PATH = "elastic/mappings/uam_closing_entity.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();
    @JsonIgnore
    private String version;  // if mismatch between Elasticsearch json and java object

    private String entityLabel;
    private String entityDescription;
    private String entityDate;
    private String eodShifter;
    private String calender;
    private String closingSet;
    private String interFixingSet;
    private String plCurrency;
    private String legalEntity;
    private String timeZone;
    private String tradeAcceptMode;
    private String reportingTemplate;
    private String accountingTemplate;
    private String accountingCurrency;
    private String plSetting;
    private String accountingDate;
    private String lastAccPurgeDate;
    private String limitationDate;
    private String openPeriodStartDate;
    private String commentS0;
    private String commentS1;
    private String commentS2;
    private String commentS3;
    private String commentS4;
    private String commentS5;
    private String strategyTree;
    private String counterpartCode;
    private String chatsEligible;
    private String entityGroup;


    private String sysDate;
    private String reportDate;
    private Long jobId;

}
