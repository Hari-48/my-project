package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data
class CombinedOspRightsExportXlxsRequest {
    public GeneralSpecificationBuilder filters;

    public List<FieldMap> fieldMaps;

    public String color;

    public List<String> fieldColumns;
}
