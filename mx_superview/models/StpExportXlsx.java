package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data class StpExportXlsx {

    public GeneralSpecification filters;

    public List<FieldMap> fieldMaps;

    public String colors;

    public List<String> fieldColumns;
    public STPModel stpModel;
}