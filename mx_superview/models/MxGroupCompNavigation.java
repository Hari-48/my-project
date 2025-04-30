package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import javax.persistence.Column;

@Data
public class MxGroupCompNavigation {

    private MxCompareGroup mxCompareGroup;

    private NavigationRights filters;

    private String pathLabel;

    private String pathRest;

    private String submenu1;

    private String submenu2;

    private String submenu3;


    private String submenu4;

    private String submenu5;

    private String PATH_LABEL;

    private String PATH_REST;

    private String SUBMENU_1;

    private String SUBMENU_2;

    private String SUBMENU_3;


    private String SUBMENU_4;

    private String SUBMENU_5;
}
