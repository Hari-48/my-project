package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.nio.file.Paths;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ClosingEntityLabel {
    public final static String INDEX_NAME = "uam_closing_entity_label";
    public final static String MAPPING_PATH = "elastic/mappings/uam_closing_entity_label.json";

    @JsonIgnore
    private String _class;
    private String id = UUID.randomUUID().toString();
    @JsonIgnore
    private String version;

    private String entityLabel;
    private String createdDate;
    private String updatedTime;

}
