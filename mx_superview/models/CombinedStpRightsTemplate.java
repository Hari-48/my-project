package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class CombinedStpRightsTemplate {
    public Long id;
    public String globalTemplate;
    public String boType;
    public String srcModule;

    public String typology;
    public String actionEvent;

    public String status;

    public String view;
    public String boTemplate ;
    public String groupingTemplate;
    public String typologyGroup;
    public String rightProfile;

    public String groupLabel;
    public CombinedStpRightsTemplate(Long id, String globalTemplate, String boType, String srcModule, String typology, String actionEvent, String status,
                                     String view, String boTemplate,
                                     String groupingTemplate, String typologyGroup, String rightProfile, String groupLabel){
        this.id=id;
        this.globalTemplate=globalTemplate;
        this.boType=boType;
        this.srcModule=srcModule;
        this.typology=typology;
        this.actionEvent=actionEvent;
        this.status=status;
        this.view=view;
        this.boTemplate=boTemplate;
        this.groupingTemplate=groupingTemplate;
        this.typologyGroup=typologyGroup;
        this.rightProfile=rightProfile;
        this.groupLabel=groupLabel;
    }

}