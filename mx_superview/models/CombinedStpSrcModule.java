package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class CombinedStpSrcModule {
    public Long id;

    public String globalTem;

    public String sourceModule;

    public String sourceTemp;

    public String sourceModuleAction;

    public String groupLabel;

    public CombinedStpSrcModule(Long id,String globalTem, String sourceModule, String sourceTemp, String sourceModuleAction,String groupLabel) {
        this.id = id;
        this.globalTem = globalTem;
        this.sourceModule = sourceModule;
        this.sourceTemp = sourceTemp;
        this.sourceModuleAction = sourceModuleAction;
        this.groupLabel = groupLabel;
    }
}
