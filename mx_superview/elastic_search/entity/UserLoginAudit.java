package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLoginAudit {
    public final static String INDEX_NAME = "uam_user_login_audit";
    public final static String MAPPING_PATH = "elastic/mappings/uam_user_login_audit.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    private String auditId;
    private String userName;
    private String groupLabel;
    private String status;
    private String osUser;
    private String stationName;
    private String stationIp;
    private String loginDate;
    private String loginTime;
    private String logoutDate;
    private String logoutTime;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}
