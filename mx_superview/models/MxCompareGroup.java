package com.finsurge.tmr_portal.mx_superview.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Lob;
import java.util.List;

@Data
public class MxCompareGroup {

    public String reportType;

    public String subReportType;

    public String template;

    public String compareTemplate;

    public String groupLabel;

    public String compareGroupLabel;

    @Lob
    public String searchValue;

    @JsonProperty
    public  boolean hasFilters;

    @JsonProperty
    public  boolean isGlobalSearch;
}
