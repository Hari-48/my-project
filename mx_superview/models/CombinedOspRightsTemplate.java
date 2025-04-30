package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data
class CombinedOspRightsTemplate {
    public CombinedOspRightsTemplate(Long id,String ospRightTemplate,String validationRightTemplate,String category,String subCategory,String queue,String queueRight,
                                     String bulkValidationEnabled,String nonModifiableAutoSelection,String filterOnData,String dataFilterShared,String action
    ,String userActionEnabled,String filterOnAction,String actionFilterShared,String technical,String groupLabel){

        this.id=id;
        this.ospRightTemplate=ospRightTemplate;
        this.validationRightTemplate=validationRightTemplate;
        this.category=category;
        this.subCategory=subCategory;
        this.queue=queue;
        this.queueRight=queueRight;
        this.bulkValidationEnabled=bulkValidationEnabled;
        this.nonModifiableAutoSelection=nonModifiableAutoSelection;
        this.filterOnData=filterOnData;
        this.dataFilterShared=dataFilterShared;
        this.action=action;
        this.userActionEnabled=userActionEnabled;
        this.filterOnAction=filterOnAction;
        this.actionFilterShared=actionFilterShared;
        this.technical=technical;
        this.groupLabel=groupLabel;
    }
    public Long id;
    public String ospRightTemplate;
    public String validationRightTemplate;
    public String category;
    public String subCategory;
    public String queue;
    public String queueRight;
    public String bulkValidationEnabled;
    public String nonModifiableAutoSelection;
    public String filterOnData;
    public String dataFilterShared;
    public String action;
    public String userActionEnabled;
    public String filterOnAction;
    public String actionFilterShared;
    public String technical;
    public String groupLabel;
}
