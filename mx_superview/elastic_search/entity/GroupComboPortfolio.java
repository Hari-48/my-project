package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupComboPortfolio {
    public final static String INDEX_NAME = "uam_group_combined_portfolio";
    public final static String MAPPING_PATH = "elastic/mappings/uam_group_combined_portfolio.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String userGroup;
    private String compfLbl;
    private String unit;
    private String unitType;
    private String status = "inactive";

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
