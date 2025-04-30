package com.finsurge.tmr_portal.mx_superview.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Lob;
import java.util.List;

public @Data class GeneralSpecificationBuilder {

    private String template;

    private List<String> groupValue;

    private List<String> templateValue;

    private String subTemplate;

    private String subTemplateValue;

    @Lob
    private String searchWhereClause;

    private String operand;

    private String fromDateValue;

    private String dateValue;

    @JsonProperty
    private boolean isGlobalSearch ;
}
