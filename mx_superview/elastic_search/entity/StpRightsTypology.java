package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StpRightsTypology {
    public final static String INDEX_NAME = "uam_stp_rights_typology";
    public final static String MAPPING_PATH = "elastic/mappings/uam_stp_rights_typology.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String typologyGroup;  // corresponds to TYPOLOGY_GROUP
    private String typology;       // corresponds to TYPOLOGY

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
