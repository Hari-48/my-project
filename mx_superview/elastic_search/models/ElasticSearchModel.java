package com.finsurge.tmr_portal.mx_superview.elastic_search.models;

import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import lombok.Data;

import java.util.List;
import java.util.Map;
@Data
public class ElasticSearchModel {
    //for search

    //bulkFilters :-
    private List<String> bulkFilterColumnValue;
    private String bulkFilterColumnName;

    private String filterColumnName;
    private String filterColumnSearchValue ;

    private String pageSorting ;  // for pagination sorting - default asc

    private List<Object> searchAfterValue ;  // for pagination - sysdate

  //  private String searchValue;

    private Map<String, List<String>> filterSearch;

    private List<String> numericFilterColumn; // for filter numeric values
    private List<String> alphaNumericFilterColumn; // for filter alpha numeric values
    private String filterColumnBasedOn ;  // alphanumeric or alpha


    private String searchTerm ;// for global search and t.search
    private String actualSearchTerm ;// for global search and t.search

    public String pitId ;

    //for export
    public List<String> fieldColumns;
    public List<FieldMap> fieldMaps;
    public String color;

    public List<String> dspLabel;

}
