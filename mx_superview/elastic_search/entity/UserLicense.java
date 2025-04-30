package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLicense {

    public final static String INDEX_NAME = "uam_user_license";
    public final static String MAPPING_PATH = "elastic/mappings/uam_user_license.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    @JsonIgnore
    private String version;

    private String userName;
    private String licenseCat;
    private Integer licenseCatCount;

    private String sysDate;
    private String reportDate;
    private Long jobId;

}
