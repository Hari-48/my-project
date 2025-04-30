package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NavigationRightsDepartment {

    public final static String INDEX_NAME = "uam_navigation_department";
    public final static String MAPPING_PATH = "elastic/mappings/uam_navigation_rights_department.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String path;
    private String pathLabel;
    private String pathRest;
    private String subMenu1;

    private List<String> departmentAmendRights = new ArrayList<>(Collections.singleton(""));


    private String createdDate;
    private String updatedTime;
    private String userName;

}
