package com.finsurge.tmr_portal.mx_superview.entity;

import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import lombok.Data;

import java.util.List;

public @Data class GeneralFilters {

    private String template;

    private String subTemplate;

    private String groupLabel;

    private String reportType;

    private String subReportType;

    private String propertyName;

    private String reportDate;

    private String templateName;

    //for exporting user and group details

    public List<FieldMap> fieldMaps;

    public List<String> fieldColumns;

    public String color;
}
