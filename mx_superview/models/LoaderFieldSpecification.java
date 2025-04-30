package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;
import tech.tablesaw.api.ColumnType;

public @Data class LoaderFieldSpecification {

    private String name;
    private String originalName;
    private ColumnType type;
    private Integer columnIndex;

//    private Boolean indexed;
//    private Boolean collapse;

    public LoaderFieldSpecification() {
    }

    public LoaderFieldSpecification(String name, ColumnType type, Integer columnIndex) {
        this.name = name;
        this.type = type;
        this.columnIndex = columnIndex;
    }

    public LoaderFieldSpecification(String name, String originalName, ColumnType type, Integer columnIndex) {
        this.name = name;
        this.originalName = originalName;
        this.type = type;
        this.columnIndex = columnIndex;
    }

    public static String getUniversalName(String sourceName) {
        String universalName = sourceName.trim().replaceAll("[^a-zA-Z0-9]", " ");
        while(universalName.contains("  ")) {
            universalName = universalName.replace("  ", " ");
        }
        universalName = universalName.replace(" ", "_");
        universalName = universalName.toLowerCase();
        return universalName;
    }

}
