//package com.finsurge.tmr_portal.mx_superview.controllers;
//
//import com.finsurge.tmr_portal.mx_superview.models.GlobalSearchModel;
//import com.finsurge.tmr_portal.mx_superview.service.GlobalSearchService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import javax.validation.Valid;
//
//@RestController
//@CrossOrigin("*")
//@RequiredArgsConstructor
//public class GlobalSearchController {
//
//    private final GlobalSearchService globalSearchService;
//
//    @GetMapping("/api/uam/global/portfolio-rights")
//    public ResponseEntity<?> getAllPortfolioRights(@Valid @RequestBody GlobalSearchModel mxPortfolioRightsDto,
//                                                   @RequestParam(defaultValue = "0") int pageNo,
//                                                   @RequestParam(defaultValue = "50") int pageSize,
//                                                   @RequestParam(defaultValue = "id") String sortBy,
//                                                   @RequestParam(defaultValue = "asc") String sortingOrder) throws ClassNotFoundException {
//        return new ResponseEntity<>(globalSearchService.getPortfolioRightsNew(mxPortfolioRightsDto, sortBy, sortingOrder, pageNo, pageSize), HttpStatus.OK);
//    }
//
//}
