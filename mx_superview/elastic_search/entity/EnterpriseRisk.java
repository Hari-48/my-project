package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnterpriseRisk {
    public final static String INDEX_NAME = "uam_enterprise_risk";
    public final static String MAPPING_PATH = "elastic/mappings/uam_enterprise_risk.json";


    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();


    @JsonIgnore
    private String version;


    private String groupLabel;
    private String marketRiskRights;
    private String controlEffectiveDateRights;
    private String importLimitRights;
    private String status = "inactive";


    private String sysDate;
    private String reportDate;
    private Long jobId;
}
