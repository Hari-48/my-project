package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;

public @Data
class FinanceRights {

    public String description;

    public String filter;

    public String tmplType;

    public String template;

    public List<String> descriptionLikeList;

    public List<String> filterLikeList;

    public List<String> filDescLikeList;

}