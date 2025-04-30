package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class CombinedOperationRightsTemplate {
    public CombinedOperationRightsTemplate(Long id, String operRgts, String template, String key, String avp1, String avp2, String avp3, String avp4, String fifo1,
                                           String fifo2, String fifo3, String fifo4, String realTime, String eventType, String evtAccess,
                                           String evtInsert, String evtModify, String evtDelete, String groupLabel) {

        this.id = id;

        this.operRgts = operRgts;

        this.template = template;

        this.key = key;

        this.avp1 = avp1;

        this.avp2 = avp2;

        this.avp3 = avp3;

        this.avp4 = avp4;

        this.fifo1 = fifo1;

        this.fifo2 = fifo2;

        this.fifo3 = fifo3;

        this.fifo4 = fifo4;

        this.realTime = realTime;

        this.eventType = eventType;

        this.evtAccess = evtAccess;

        this.evtInsert = evtInsert;

        this.evtModify = evtModify;

        this.evtDelete = evtDelete;

        this.groupLabel = groupLabel;
    }

    public Long id;

    public String operRgts;

    public String template;

    public String key;

    public String avp1;

    public String avp2;

    public String avp3;

    public String avp4;

    public String fifo1;

    public String fifo2;

    public String fifo3;

    public String fifo4;

    public String realTime;

    public String eventType;

    public String evtAccess;

    public String evtInsert;

    public String evtModify;

    public String evtDelete;

    public String groupLabel;

}
