package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.mx_superview.entity.CounterPartyFilters;
import com.finsurge.tmr_portal.mx_superview.service.CounterPartyFiltersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin("*")
public class CounterPartyFiltersController {

    @Autowired
    private CounterPartyFiltersService counterPartyFiltersService;
    @GetMapping("/api/uam/get-counterparty-filter-data")
    public ResponseEntity<?> getAllDataByFilters(@RequestParam int page,@RequestParam(defaultValue = "50") int pageSize,@RequestParam String status ,@RequestParam(defaultValue = "id") String sortBy,
                                                 @RequestParam String sortingOrder){
        sortBy = sortBy == null ? "label" : sortBy.trim();
        sortingOrder = sortingOrder == null ? "asc" : sortingOrder.trim();
        Sort.Order sort;
        if ("desc".equalsIgnoreCase(sortingOrder)) {
            sort = Sort.Order.desc(sortBy).ignoreCase();
        } else {
            sort = Sort.Order.asc(sortBy).ignoreCase();
        }
        return new ResponseEntity<>(counterPartyFiltersService.getAllDataByFilter(status, PageRequest.of(page,pageSize,Sort.by(sort))),HttpStatus.OK);
    }
    @PostMapping("/api/uam/add-counterparty-filter-data")
    public ResponseEntity addCounterPartyFilterData(@RequestBody CounterPartyFilters counterPartyFilters, Authentication authentication){
        log.info("Authentication:{}",authentication.getName());
        return counterPartyFiltersService.addCounterPartyFilterData(counterPartyFilters,authentication.getName());
    }

    @PutMapping("/api/uam/update-counterparty-filter-data/{id}")
    public ResponseEntity addDataInFilters(@PathVariable Long id,@RequestBody CounterPartyFilters counterPartyFilters,Authentication authentication){
        return counterPartyFiltersService.editCounterPartyFilterData(id,counterPartyFilters,authentication.getName());
    }

    @DeleteMapping("/api/uam/delete-counterparty-filter-data/{id}")
    public ResponseEntity deleteData(@PathVariable Long id){
        return counterPartyFiltersService.deleteDataInFilters(id);
    }
}