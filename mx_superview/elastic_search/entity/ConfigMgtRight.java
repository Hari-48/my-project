package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigMgtRight {
    public final static String INDEX_NAME = "uam_config_mgt_right";
    public final static String MAPPING_PATH = "elastic/mappings/uam_config_mgt_right.json";


    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();


    private String groupLabel;
    private String irsLabel;
    private String ldLabel;
    private String cdLabel;
    private String rtgaLabel;
    private String hierarchyTmpl;
    private String joinRightTmpl;
    private String exportPermission;
    private String importPermission;
    private String edit;
    private String purged;
    private String status = "inactive";


    private String sysDate;
    private String reportDate;
    private Long jobId;
}
