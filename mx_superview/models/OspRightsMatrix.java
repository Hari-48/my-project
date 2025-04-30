package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data
class OspRightsMatrix {


    public String validationRightTemplate;

    public String category;

    public String subCategory;

    public String queue;

    public String action;

    public String VALIDATION_RIGHT_TEMPLATE;

    public String CATEGORY;

    public String SUB_CATEGORY;

    public String QUEUE;

    public String ACTION;


    public String propertyValue;

    public List<String> validationRightTmplList;

    public List<String> categoryList;

    public List<String> subCategoryList;

    public List<String> queueList;

    public List<String> queueRightList;

    public List<String> bulkValidationEnabledList;

    public List<String> nonModifiableAutoSelectionList;

    public List<String> filterOnDataList;

    public List<String> dataFilterSharedList;

    public List<String> actionList;

    public List<String> userActionEnabledList;

    public List<String> filterOnActionList;

    public List<String> actionFilterSharedList;

    public List<String> technicalList;

    public OspRightsMatrix(String validationRightTemplate, String category, String subCategory, String queue) {
        this.validationRightTemplate = validationRightTemplate;
        this.category = category;
        this.subCategory = subCategory;
        this.queue = queue;
    }


    public OspRightsMatrix(String validationRightTemplate, String category, String subCategory, String queue,String action) {
        this.validationRightTemplate = validationRightTemplate;
        this.category = category;
        this.subCategory = subCategory;
        this.queue = queue;
        this.action=action;
    }

    public OspRightsMatrix() {
    }
}
