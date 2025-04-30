package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;

import java.util.List;


public @Data
class CwtConfigMgtRight {

    public String hierarchyTmpl;

    public String groupLabel;

    public String irsLabel;

    public String ldLabel;

    public String cdLabel;

    public String rtgaLabel;

    public List<String> irsLabelLikeList;

    public List<String> ldLabelLikeList;

    public List<String> cdLabelLikeList;

    public List<String> rtgaLabelLikeList;

    public List<String> hierarchyTmplLikeList;

    public List<String> joinRightTmplList;

    public List<String> exportList;

    public List<String> importsList;

    public List<String> editList;

    public List<String> purgeList;
}