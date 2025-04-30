package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.HashMap;


public @Data class LoaderConfiguration {

    private String source;
    private String templateName;
    private HashMap<String, LoaderFieldSpecification> fieldMap;

}
