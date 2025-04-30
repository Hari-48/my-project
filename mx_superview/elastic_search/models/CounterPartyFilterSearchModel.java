package com.finsurge.tmr_portal.mx_superview.elastic_search.models;

import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import lombok.Data;

import java.util.List;
import java.util.Map;

public @Data class CounterPartyFilterSearchModel {

    //for search
    private List<String> searchColumnValue;
    private String searchValue;
    private String columnName;
    private Map<String, List<String>> filterSearch;

    private String filterColumn; // for excel filter
    private String filterValue ;  // for excel filter search
    private String filterColumnBasedOn ;  // alphanumeric or alpha

    private List<String> filterColumnNumeric; // for filter numeric values
    private List<String> filterColumnAlphaNumeric; // for filter alpha numeric values

    private String pageSorting ;  // for pagination sorting - default asc
    private List<Object> searchAfterValue ;  // for pagination - sysdate
    private String searchTerm ;// for global search and t.search
    private String actualSearchTerm ;// for global search and t.search
    public String indexName  = "uam_counterparty";

    public String pitId ;


    //for export
    public List<String> fieldColumns;
    public List<FieldMap> fieldMaps;
    public String color;
}