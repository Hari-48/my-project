package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;


public @Data class NavigationRights {

    public String  comments;

    public String rights;

    public String  menu;

    public String path;

    public String  COMMENTS;

    public String  RIGHTS;

    public String MENU;

    public String PATH;
    public NavigationRights() {
    }

    public NavigationRights(String comments, String rights, String menu, String path) {
        this.comments = comments;
        this.rights = rights;
        this.menu = menu;
        this.path = path;
    }

    public List<String> rightsList;
    public List<String> menuList;
    public List<String> pathList;
    public List<String> pathLabelList;
    public List<String> pathRestList;

    public List<String> submenuList1;
    public List<String> submenuList2;
    public List<String> submenuList3;
    public List<String> submenuList4;
    public List<String> submenuList5;
    public List<String> commentsList;


}