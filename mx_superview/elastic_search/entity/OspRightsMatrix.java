package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OspRightsMatrix {
    public final static String INDEX_NAME = "uam_osp_rights_matrix";
    public final static String MAPPING_PATH = "elastic/mappings/uam_osp_rights_matrix.json";


    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();


    private String ospRightTemplate;
    private String validationRightTemplate;
    private String category;
    private String subCategory;
    private String queue;
    private String queueRight;
    private String bulkValidationEnabled;
    private String nonModifiableAutoSelection;
    private String filterOnData;
    private String dataFilterShared;
    private String action;
    private String userActionEnabled;
    private String filterOnAction;
    private String actionFilterShared;
    private String technical;


    private List<String> activeGroupLabel;
    private List<String> inActiveGroupLabel;


    private String sysDate;
    private String reportDate;
    private Long jobId;
}
