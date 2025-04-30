package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

public @Data class CombinedConsistencyTemplate {
    public CombinedConsistencyTemplate(Long id,String consistencyTmpl, String category, String item,String accessRight,String insertRight,String modifyRight,String deleteRight,String mandatoryRight,String accountingRight,String paymentRight,String groupLabel) {
        this.id =id;
        this.consistencyTmpl = consistencyTmpl;
        this.category = category;
        this.item = item;
        this.accessRight = accessRight;
        this.insertRight = insertRight;
        this.modifyRight = modifyRight;
        this.deleteRight = deleteRight;
        this.mandatoryRight = mandatoryRight;
        this.accountingRight = accountingRight;
        this.paymentRight = paymentRight;
        this.groupLabel = groupLabel;
    }

    public Long id;

    public String consistencyTmpl;

    public String category;

    public String item;

    public String accessRight;

    public String insertRight;

    public String modifyRight;

    public String deleteRight;

    public String mandatoryRight;

    public String accountingRight;

    public String paymentRight;

    public String groupLabel;
}
