package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.mx_superview.elastic_search.service.CounterpartyCreationService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class CounterpartyCreationController {
    @Autowired
    CounterpartyCreationService counterpartyCreationService;

    @GetMapping("api/uam/counterparty-creation/weekDate")
    public Document getWeeklyDatesCount(@RequestParam String reportDate) throws IOException {
        return counterpartyCreationService.getWeeklyDatesCount(reportDate);
    }

    @PostMapping("api/uam/counterparty-creation/weekNumber")
    public Integer getWeeklyDate(@RequestParam String date) {
        return counterpartyCreationService.getCountOfWeek(date);
    }

    // Getting a cpId :-
    @PostMapping("api/uam/counterparty-creation/cpid")
    public Document getCpId(@RequestParam String reportDate, @RequestParam String creationDate) throws IOException {
        return counterpartyCreationService.getCpId(reportDate, creationDate);

    }

}
