package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinanceRights {
    public final static String INDEX_NAME = "uam_finance_rights";
    public final static String MAPPING_PATH = "elastic/mappings/uam_finance_rights.json";


    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();


    private String tmplType;
    private String template;
    private String description;
    private String filter;
    private String filDesc;


    private List<String> activeGroupLabel;
    private List<String> inActiveGroupLabel;


    private String sysDate;
    private String reportDate;
    private Long jobId;
}
