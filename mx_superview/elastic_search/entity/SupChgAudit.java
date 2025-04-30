package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupChgAudit {
    public final static String INDEX_NAME = "uam_supchg_audit";
    public final static String MAPPING_PATH = "elastic/mappings/uam_supchg_audit.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String auditId;
    private String userName;
    private String userGroup;
    private String type;
    private String refObject;
    private String action;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private String compDate;
    private String compTime;
    private String systemDate;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
