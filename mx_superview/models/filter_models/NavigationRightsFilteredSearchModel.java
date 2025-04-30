package com.finsurge.tmr_portal.mx_superview.models.filter_models;

import lombok.Data;

import java.util.List;

public @Data class NavigationRightsFilteredSearchModel {

    private List<String> groupLabel;
    private List<String> rights;
    private List<String> menu;
    private List<String> path;
    private List<String> pathLabel;
    private List<String> pathRest;
    private List<String> submenu1;
    private List<String> submenu2;
    private List<String> submenu3;
    private List<String> submenu4;
    private List<String> submenu5;
    private List<String> template;
    private List<String> comments;
    private List<String> fieldColumns;
    private String searchValue;

}
