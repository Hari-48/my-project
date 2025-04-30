package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationRights {
    public final static String INDEX_NAME = "uam_operation_rights";
    public final static String MAPPING_PATH = "elastic/mappings/uam_operation_rights.json";


    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();


    private String operRgt;
    private String template;
    private String keyLabel;
    private String avp1;
    private String avp2;
    private String avp3;
    private String avp4;
    private String fifo1;
    private String fifo2;
    private String fifo3;
    private String fifo4;
    private String realTime;
    private String eventType;
    private String evtAccess;
    private String evtInsert;
    private String evtModify;
    private String evtDelete;


    private List<String> activeGroupLabel;
    private List<String> inActiveGroupLabel;


    private String sysDate;
    private String reportDate;
    private Long jobId;
}
