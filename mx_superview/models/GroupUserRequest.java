package com.finsurge.tmr_portal.mx_superview.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Lob;

@Data
public class GroupUserRequest {
    public String subTemplate;
    public String repType;
    public String repDate;

    @Lob
    public String searchWhereClause;

    @JsonProperty
    public boolean isGlobalSearch ;
}
