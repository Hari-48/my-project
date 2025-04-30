package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.mx_superview.models.filter_models.NavigationRightsFilteredSearchModel;
import com.finsurge.tmr_portal.mx_superview.service.FilteredSearchService;
import com.finsurge.tmr_portal.mx_superview.service.SnapshotDataService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CrossOrigin
@RestController
public class FilteredSearchController {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);

    @Autowired
    private FilteredSearchService filteredSearchService;

    @PostMapping("/api/uam/navigation/filter-search")
    public ResponseEntity<?> getNavigationFilteredData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam boolean isGlobalSearch,
            @RequestParam String repDate,
            @RequestBody NavigationRightsFilteredSearchModel filteredSearchModel) {

        LocalDate reportDate = LocalDate.parse(repDate, dateTimeFormatter);
        String searchValue = filteredSearchModel.getSearchValue();
        Document document = filteredSearchService.getNavigationFilteredData(sortBy, sortingOrder, page, pageSize, reportDate, filteredSearchModel, searchValue, isGlobalSearch);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/navigation/filter-search-column")
    public List<String> getNavigationFilteredColumnData(
            @RequestParam String repDate,
            @RequestParam String columnName,
            @RequestParam (required = false) String searchColumnValue,
            @RequestParam boolean isGlobalSearch,
            @RequestBody NavigationRightsFilteredSearchModel filteredSearchModel) throws NoSuchFieldException, IllegalAccessException {

        LocalDate reportDate = LocalDate.parse(repDate, dateTimeFormatter);
        String searchValue = filteredSearchModel.getSearchValue();
        return filteredSearchService.getNavigationFilteredColumnData(reportDate, columnName, searchColumnValue, filteredSearchModel, searchValue, isGlobalSearch);
    }
}
