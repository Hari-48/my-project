package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class GroupNavigationCombine {

    public Long id;

    public String groupLabel;

    public String rights;

    public String menu;

    public String path;

    public String pathLabel;

    public String pathRest;

    public String submenu1;

    public String submenu2;

    public String submenu3;

    public String submenu4;

    public String submenu5;

    public String template;

    public String comments;

    public GroupNavigationCombine(Long id, String groupLabel, String rights, String menu, String path, String pathLabel, String pathRest, String submenu1, String submenu2, String submenu3, String submenu4, String submenu5, String template, String comments) {
        this.id = id;
        this.groupLabel = groupLabel;
        this.rights = rights;
        this.menu = menu;
        this.path = path;
        this.pathLabel = pathLabel;
        this.pathRest = pathRest;
        this.submenu1 = submenu1;
        this.submenu2 = submenu2;
        this.submenu3 = submenu3;
        this.submenu4 = submenu4;
        this.submenu5 = submenu5;
        this.template = template;
        this.comments = comments;
    }
}
