package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class UamReportSummary {

    private Long mismatchRows;

    private Long matchedRows;

    private Long misMatchFieldCount;

    private Long matchedFieldsCount;

    private Long availableRows;

    private Long totalFieldsCompared;

    private Long missingRows;
}
