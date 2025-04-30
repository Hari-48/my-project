package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.FinanceRightsService;
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
public class FinanceRightsController {
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private FinanceRightsService financeRightsService;

    @PostMapping("/api/uam/finance-rights/filter")
    public ResponseEntity<?> getFinanceRightsList(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "tmplType") String sortingOrder,
            @RequestParam(required = false) String tmplType,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document financeRightsList;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("FINANCE_RIGHTS", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            financeRightsService.getFinanceRightsListExport(reportDate, "Finance_Rights_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, tmplType, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        financeRightsList = financeRightsService.getFinanceRightsList(reportDate,
                elasticSearchModel, sortBy, sortingOrder, tmplType, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());
        return new ResponseEntity<>(financeRightsList, HttpStatus.OK);
    }
}
