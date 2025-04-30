package com.finsurge.tmr_portal.mx_superview.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Lob;

public @Data class GeneralSpecification {

 private String template;

 private String templateValue;

 private String subTemplate;

 private String subTemplateValue;

 @Lob
 private String searchWhereClause;

 private String operand;

 private String fromDateValue;

 private String dateValue;

 // for comapre
 private String compareTemplateValue;
 private String groupName ;
 private String compareGroupName ;

 //global serach identification
 @JsonProperty

 private boolean isGlobalSearch ;

}

