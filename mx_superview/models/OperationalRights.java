package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data
class OperationalRights {

    public String key;

    public String KEY_LABEL;


    public OperationalRights(String key) {
        this.key = key;
    }

    public List<String> keyList;

    public List<String> avp1List;

    public List<String> avp2List;

    public List<String> avp3List;

    public List<String> avp4List;

    public List<String> fifo1List;

    public List<String> fifo2List;

    public List<String> fifo3List;

    public List<String> fifo4List;

    public List<String> realTimeList;

    public List<String> eventTypeLikeList;

    public List<String> evtAccessList;

    public OperationalRights() {
    }

    public List<String> evtInsertList;

    public List<String> evtModifyList;

    public List<String> evtDeleteList;


}