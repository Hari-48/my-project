package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class StpSourceModule {

    private String processingCenter;
    private String processingTemplate;

    public StpSourceModule(String processingCenter, String processingTemplate) {
        this.processingCenter = processingCenter;
        this.processingTemplate = processingTemplate;
    }


}
