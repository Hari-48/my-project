package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UdfStructureComm {
    public final static String INDEX_NAME = "uam_udf_structure_comm";
    public final static String MAPPING_PATH = "elastic/mappings/uam_udf_structure_comm.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String foComment;
    private String boComment;
    private String iaMethod;
    private Integer amount;
    private String tradabilityStatus;
    private String tradabilityMessage;
    private String afterHour;
    private String offPremises;
    private String islCode;
    private Double bimNotional;
    private String bimNotionalOvrw;
    private String bimPackageType;
    private String bimTradeDate;
    private String bimTypology;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
