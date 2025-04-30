package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.service.SearchHistoryService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
public class SearchHistoryController {


    private static final Logger log = LoggerFactory.getLogger(SearchHistoryController.class);

    @Autowired
    private SearchHistoryService searchHistoryService;

    @PostMapping(value = "/api/uam/viewer/search-history")
    public ResponseEntity<?> getAllSearchHistory(Authentication authentication,
                                                 @RequestParam String reportType, @RequestParam(required = false) String search,
                                                 @RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "20") int pageSize) {

        Page<SearchHistory> searchHistory =  searchHistoryService.getSearchPredicates(authentication.getName(),reportType,page,pageSize,search);
        Document document = new Document();
        document.put("totalPages",searchHistory.getTotalPages());
        document.put("records",searchHistory.getTotalElements());
        document.put("content",searchHistory.getContent());
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @DeleteMapping(value = "/api/uam/viewer/search-history/delete")
    public ResponseEntity<?> deleteSearchHistory( Authentication authentication,@RequestParam String reportType, @RequestParam(required = false) Long id) {
        searchHistoryService.deleteSearchHistory(authentication.getName(),reportType,id);
        return new ResponseEntity<>(new Document().append("status", "Search Query Deleted  Successfully"), HttpStatus.OK);
    }
}
