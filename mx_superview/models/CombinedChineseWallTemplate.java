package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class CombinedChineseWallTemplate {
    public CombinedChineseWallTemplate(Long id,String templateLabel, String counterpartLabel, String counterpartDescription,String groupLabel) {
        this.id =id;
        this.templateLabel = templateLabel;
        this.counterpartLabel = counterpartLabel;
        this.counterpartDescription = counterpartDescription;
        this.groupLabel = groupLabel;
    }

    public Long id;

    public String templateLabel;

    public String counterpartLabel;

    public String counterpartDescription;

    public String groupLabel;

    public CombinedChineseWallTemplate() {
    }
}
