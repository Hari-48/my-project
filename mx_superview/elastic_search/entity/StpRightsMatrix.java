package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StpRightsMatrix {
    public final static String INDEX_NAME = "uam_stp_rights_matrix";
    public final static String MAPPING_PATH = "elastic/mappings/uam_stp_rights_matrix.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String globalTemplate;
    private String srcModule;
    private String boType;
    private String boTemplate;
    private String groupingTemplate;
    private String typologyGroup;
    private String typology;
    private String rightsProfile;
    private String actionEvent;
    private String status;
    private String views;

    private List<String> activeGroupLabel;
    private List<String> inActiveGroupLabel;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
