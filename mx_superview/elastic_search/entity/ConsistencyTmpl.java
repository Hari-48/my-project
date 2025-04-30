package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsistencyTmpl {
    public final static String INDEX_NAME = "uam_consistency_template";
    public final static String MAPPING_PATH = "elastic/mappings/uam_consistency_template.json";


    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();


    private String consistencyTmpl;
    private String category;
    private String item;
    private String accessRight;
    private String insertRight;
    private String modifyRight;
    private String deleteRight;
    private String mandatoryRight;
    private String accountingRight;
    private String paymentRight;


    private List<String> activeGroupLabel;
    private List<String> inActiveGroupLabel;


    private String sysDate;
    private String reportDate;
    private Long jobId;
}
