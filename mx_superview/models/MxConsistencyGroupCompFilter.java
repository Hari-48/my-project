package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

@Data
public class MxConsistencyGroupCompFilter {

    private MxCompareGroup mxCompareGroup;

    private ConsistencyTemplateRights filters;
    private String accessRight;

    private String insertRight;

    private String modifyRight;

    private String deleteRight;

    private String mandatoryRight;

    private String accountingRight;

    private String paymentRight;

}
