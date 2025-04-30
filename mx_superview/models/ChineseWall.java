package com.finsurge.tmr_portal.mx_superview.models;


import lombok.Data;

import java.util.List;

public @Data class ChineseWall {


    public String counterpartLabel;
    public String COUNTERPART_LABEL;
    public List<String> counterPartLabelList;
    public List<String> counterpartDescription;
    public ChineseWall(String counterpartLabel) {
        this.counterpartLabel = counterpartLabel;
    }

    public ChineseWall() {
    }
}

