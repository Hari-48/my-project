package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class StpMatrixModel {

    private String boTemplate;

    private String boType;

    private String globalTemplate;

    private String groupingTemplate;

    private String groupLabel;

    public StpMatrixModel(String boTemplate, String boType, String globalTemplate, String groupingTemplate, String groupLabel) {
        this.boTemplate = boTemplate;
        this.boType = boType;
        this.globalTemplate = globalTemplate;
        this.groupingTemplate = groupingTemplate;
        this.groupLabel = groupLabel;
    }
}
