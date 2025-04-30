package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.mx_superview.entity.UamSummaryReportJob;
import com.finsurge.tmr_portal.mx_superview.models.UamReportSummary;
import com.finsurge.tmr_portal.mx_superview.service.UamSummaryReportJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RestController
public class UamSummaryReportJobController {

    private static Logger log = LoggerFactory.getLogger(UamSummaryReportJobController.class);

    @Autowired
    private UamSummaryReportJobService jobService;


    @GetMapping("/api/uam-summary-report/jobs/{id}")
    public ResponseEntity<?> getJob(
            @PathVariable Long id
    ) {
        UamSummaryReportJob job = jobService.getJob(id);
        if(job == null) {
            return new ResponseEntity<>("No such job exists.", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @GetMapping("/api/uam-summary-report/jobs/{id}/get-summary")
    public ResponseEntity<?> getFileFromJobID(
            @PathVariable Long id
    ) {
        UamReportSummary summary = jobService.getSummary(id);
        return new ResponseEntity<>(summary,HttpStatus.OK);
    }
}
