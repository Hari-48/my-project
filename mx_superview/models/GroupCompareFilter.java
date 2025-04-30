package com.finsurge.tmr_portal.mx_superview.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public @Data
class GroupCompareFilter {

    public List<String> key0;
    public List<String> key1;
    public List<String> key2;
    public List<String> key3;
    public List<String> key4;
    public  String propertyName;
    public String reportType;
    public String template;
    public String compareTemplate;
    public String groupLabel;
    public String compareGroupLabel;
    public String filter0;
    public String filter1;
    public String filter2;
    public String filter3;
    public String filter4;

    public String search;
    public String search1;
    public String search2;
    public String search3;
    public String search4;

    public String type;
    public String searchQuery;
    public String subReportType;
    @JsonProperty
    public boolean isGlobalSearch;
}
