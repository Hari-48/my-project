package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.ClosingEntityService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class ClosingEntityController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private ClosingEntityService closingEntityService;

    @PostMapping("/api/uam/closing-entity/filter")
    public ResponseEntity<?> getClosingEntityData(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "entityLabel") String sortingOrder,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            @RequestParam(required = false) Boolean isCaseSensitive,
            Authentication authentication) throws Exception {
        Document closingEntity;
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CLOSING_ENTITY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            closingEntityService.getClosingEntityExport(reportDate, "Closing_Entity_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel, isCaseSensitive);

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        closingEntity = closingEntityService.getClosingEntityData(reportDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName(), isCaseSensitive);
        return new ResponseEntity<>(closingEntity, HttpStatus.OK);
    }

    @PostMapping("api/uam/closing-entity/reportDate")
    public String reportDate(@RequestParam String reportDate) throws IOException {
        return closingEntityService.setReportDate(reportDate);
    }
}
