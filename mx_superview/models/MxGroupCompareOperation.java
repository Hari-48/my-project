package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

@Data
public class MxGroupCompareOperation {

    private MxCompareGroup mxCompareGroup;

    private OperationalRights filters;

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


}
