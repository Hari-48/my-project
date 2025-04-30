package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data class EnterpriseRisk {

    private String label;

    private String modRsk;

    private String modPst;

    private String upload;

    private List<String> modRskList;

    private List<String> modPstList;

    private List<String> uploadList;
}
