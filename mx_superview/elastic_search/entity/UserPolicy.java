package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPolicy {

    public final static String INDEX_NAME = "uam_user_policy";
    public final static String MAPPING_PATH = "elastic/mappings/uam_user_policy.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    @JsonIgnore
    private String version;

    private String mreference;
    private String mname;
    private String pwdUserBy;
    private String frcPwdChg;
    private String prtPwdUse;
    private String reuseOpwd;
    private String prtPwdChg;
    private String dispLastLog;
    private String autoSusp;
    private String autoLock;
    private String maxPwd;
    private String lockUacc;
    private String failLock;
    private String definePwd;
    private String diffName;
    private String revrseName;
    private String minCharacter;
    private String maxChar;
    private String diffChar;
    private String alphbtChar;
    private String nalphbtChar;
    private String pciChar;
    private String minUchar;
    private String minLchar;
    private String minScharac;
    private String minNchar;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}

