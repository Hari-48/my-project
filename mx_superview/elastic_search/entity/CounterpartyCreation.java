package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CounterpartyCreation {

    public final static String INDEX_NAME = "uam_counterparty_creation";
    public final static String MAPPING_PATH = "/elastic/mappings/uam_counterparty_creation.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private Integer cpId;
    private String dspLabel;
    private String oadId;
    private String mgrId;
    private String creationDate;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
