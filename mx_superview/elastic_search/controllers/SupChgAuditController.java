package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.SupChgAuditService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@CrossOrigin
public class SupChgAuditController {
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private SupChgAuditService supChgAuditService;

    @PostMapping("/api/uam/sup-chg-audit/filter")
    public ResponseEntity<?> supChgAuditList(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "auditId") String sortingOrder,
            @RequestParam String reportDate,
            @RequestParam(required = false) String fromDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document supChgAuditList;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("SUP_CHG_AUDIT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            supChgAuditService.getSupChgAuditListExport(reportDate, fromDate, "Sup_Chg_Audit_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        supChgAuditList = supChgAuditService.getSupChgAuditList(reportDate, fromDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());
        return new ResponseEntity<>(supChgAuditList, HttpStatus.OK);
    }
}
