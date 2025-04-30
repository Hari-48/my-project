package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)

public class GroupNavigationRights {


    public final static String INDEX_NAME = "uam_group_navigation_rights";
    public final static String MAPPING_PATH = "elastic/mappings/uam_group_navigation_rights.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String groupLabel;
    private String template;
    private String comments;
    private String rights;
    private String menu;
    private String path;
    private String pathLabel;
    private String pathRest;
    private String subMenu1;
    private String subMenu2;
    private String subMenu3;
    private String subMenu4;
    private String subMenu5;

    private List<String> activeGroupLabel;
    private List<String> inActiveGroupLabel;

    private List<String> userGroupDepartment;
    private List<String> departmentAmendRights;

    private String rightsLevel = "";

    private String status = "inactive";

    private String sysDate;
    private String reportDate;
    private Long jobId;

}

