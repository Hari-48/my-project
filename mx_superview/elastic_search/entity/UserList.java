package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserList {

    public final static String INDEX_NAME = "uam_user_list";
    public final static String MAPPING_PATH = "elastic/mappings/uam_mx_user_list.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    @JsonIgnore
    private String version;

    private String userLabel;
    private String userName;
    private String descr;
    private String suspended;
    private String suspSd;
    private String suspEd;
    private String locked;
    private String code;
    private String mngmntPolicy;

    private String sysDate;
    private String reportDate;
    private Long jobId;

}


