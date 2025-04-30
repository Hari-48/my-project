package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data class STPModel {

    public String boType;

    public String typology;

    public String action;

    public String status;

    public String stpView;


    public List<String> boTypeList;
    public List<String> sourceModList;
    public List<String> typologyList;
    public List<String> actionList;
    public List<String> statusList;
    public List<String> stpViewList;

    public List<String> checkedList;

    public STPModel(String boType, String typology, String action, String status, String view) {
        this.boType = boType;
        this.typology = typology;
        this.action = action;
        this.status = status;
        this.stpView = view;
    }

    public STPModel() {
    }

}