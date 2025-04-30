package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupLabelDepartment {

    public final static String INDEX_NAME = "uam_group_label_department";
    public final static String MAPPING_PATH = "elastic/mappings/uam_groupLabel_department.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    @JsonIgnore
    private String version;

    private String createdDate = "";
    private String grpRoleStr = "";
    private String groupLabel = "";
    private String groupLabelStatus = "";
    private String department = "";
    private String username = "";
    private String departmentStatus = "Active";
    private String updatedTime = "";

}
