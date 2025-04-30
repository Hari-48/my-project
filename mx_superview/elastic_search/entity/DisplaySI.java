package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DisplaySI {


    public final static String INDEX_NAME = "uam_display_si";

    public final static String MAPPING_PATH = "/elastic/mappings/uam_display_si.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String counterpartLabel;
    private String ref;
    private String settlType;
    private String previous;
    private String next;
    private String multiple;
    private String amend;
    private String nostroVostro;
    private String creditDebt;
    private String nature;
    private String insDate;
    private String modDate;
    private String startDT;
    private String endDT;
    private String insTime;
    private String modTime;
    private String counterpart;
    private String entity;
    private String tradeSection;
    private String family;
    private String trnGroup;
    private String type;
    private String instrument;
    private String currency;
    private String odCurrency;
    private String code;
    private String displayUser;
    private String status;
    private String market;
    private String clearCenter;
    private String settleMethod;
    private String comments;
    private String typology;
    private String siUsage;
    private String legalEntity;
    private String processEntity;
    private String flowTypology0;
    private String flowTypology1;
    private String flowTypology2;
    private String flowTypology3;
    private String flowTypology4;
    private String customInfo;
    private String physcialProduct;
    private String location;
    private String clear;
    private String portfolio;
    private String startegy;
    private String processingArea;
    private String vostroService;
    private String agreement;
    private String agreementType;
    private String GoverningLaw;
    private String cancelled;

    private String sysDate;
    private String reportDate;
    private Long jobId;

}
