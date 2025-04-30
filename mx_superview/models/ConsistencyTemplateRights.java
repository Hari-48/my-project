package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data
class ConsistencyTemplateRights {

    public String category;

    public String item;

    public String CATEGORY;

    public String ITEM;

    public ConsistencyTemplateRights(String category, String item) {
        this.category = category;
        this.item = item;
    }

    public ConsistencyTemplateRights() {
    }

    public List<String> categoryList;

    public List<String> itemList;

    public List<String> accessRightList;

    public List<String> insertRightList;

    public List<String> modifyRightList;

    public List<String> deleteRightList;

    public List<String> mandatoryRightList;

    public List<String> accountingRightList;

    public List<String> paymentRightList;
}