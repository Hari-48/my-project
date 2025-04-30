package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserGroupAccessRights {

    public final static String INDEX_NAME = "uam_user_group_access_rights";
    public final static String MAPPING_PATH = "elastic/mappings/uam_user_group_access_rights.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    @JsonIgnore
    private String version;// if mismatch between Elasticsearch json and java object
    private String userName;
    private String userDesc = "";
    private String groupLabel;
    private String groupDesc;
    private String groupRoleStr;
    private String groupTypeStr;
    private String descr = "";
    private String suspended = "";
    private String suspSd;
    private String suspEd;
    private String locked = "";
    private String code = "";
    private String mngmntPolicy = "";
    private String license = "";
    private String userLabel = "";
    private String status = "active";
    private String sysDate;
    private String reportDate;
    private Long jobId;
    private Integer licenseCatCount = 0;

    private List<String> userGroupDepartment;

}