package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChineseWallClosingEntityLabel {

    public final static String INDEX_NAME = "uam_chinese_wall_closing_entity_label";
    public final static String MAPPING_PATH = "elastic/mappings/uam_chinese_wall_closing_entity_label.json";

    @JsonIgnore
    private String _class;
    private String id = UUID.randomUUID().toString();

    private List<String> entityLabel = new ArrayList<>();
    private String templateLabel;
    private String status;

    private String createdDate;
    private String updatedTime;
    private String userName;
}
