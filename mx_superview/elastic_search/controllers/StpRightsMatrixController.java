package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.StpRightsMatrixService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;


@RequiredArgsConstructor
@RestController
@CrossOrigin
public class StpRightsMatrixController {
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private  StpRightsMatrixService stpRightsMatrixService;

    @PostMapping("/api/uam/stp-rights-matrix/filter")
    public ResponseEntity<?> getStpRightsMatrix(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "globalTemplate") String sortingOrder,
            @RequestParam(required = false, defaultValue = "false") Boolean inActiveTemplate,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document stpRightsMatrixList;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("STP_RIGHTS_MATRIX", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            stpRightsMatrixService.getStpRightsMatrixListExport(reportDate, "Stp_Rights_Matrix_Template",elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, inActiveTemplate, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType,elasticSearchModel);

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        stpRightsMatrixList = stpRightsMatrixService.getStpRightsMatrixList(reportDate,
                elasticSearchModel, sortBy, sortingOrder, inActiveTemplate, pageSize, isGlobalSearch, bulkFilterSearchType,authentication.getName());
        return new ResponseEntity<>(stpRightsMatrixList, HttpStatus.OK);
    }
}
