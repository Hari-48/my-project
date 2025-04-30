package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
//import org.springframework.data.elasticsearch.annotations.Document;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
//@Document(indexName = "uam_dormant_counterparty")
public @Data class DormantCounterparty {

    public final static String INDEX_NAME = "uam_dormant_counterparty";
    public final static String MAPPING_PATH = "/elastic/mappings/uam_dormant_counterparty.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private Integer cpId;
    private String dspLabel;
    private String counterPartyName;
    private String country;
    private String sector;
    private String comment0;
    private String comment1;
    private String comment2;
    private String amdDate;
    private String amdTime;
    private String oadId;
    private String mgrId;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
