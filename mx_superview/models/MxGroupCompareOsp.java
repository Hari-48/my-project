package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import javax.persistence.Column;

@Data
public class MxGroupCompareOsp {

    private MxCompareGroup mxCompareGroup;

    private OspRightsMatrix filters;

    private String queueRight;

    private String bulkValidationEnabled;

    private String nonModifiableAutoSelection;

    private String filterOnData;

    private String dataFilterShared;

    private String userActionEnabled;

    private String filterOnAction;

    private String actionFilterShared;

    private String technical;


}
