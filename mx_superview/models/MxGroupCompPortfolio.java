package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

@Data
public class MxGroupCompPortfolio {

    private MxCompareGroup mxCompareGroup;

    private GroupCompPortfolio filters;

    private String unitType;

}
