package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StpRightsSrcMod {
    public final static String INDEX_NAME = "uam_stp_rights_src_mod";
    public final static String MAPPING_PATH = "elastic/mappings/uam_stp_rights_src_mod.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String processingCenter;
    private String processingTemplate;
    private String globalTemplate;
    private String sourceModule;
    private String sourceTemplate;
    private String sourceModuleAction;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
