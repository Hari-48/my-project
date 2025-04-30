package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data class GroupCompPortfolio {

    public String compfLbl;

    public String unit;

    public String COMPF_LBL;

    public String UNIT;

    public GroupCompPortfolio() {
    }

    public GroupCompPortfolio(String compfLbl, String unit) {
        this.compfLbl = compfLbl;
        this.unit = unit;
    }
    public List<String> compfLblList;
    public List<String> unitList;
    public List<String> unitTypeList;


}