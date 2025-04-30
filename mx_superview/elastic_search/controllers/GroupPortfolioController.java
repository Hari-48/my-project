package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.GroupPortfolioService;
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
public class GroupPortfolioController {

    @Autowired
    private GroupPortfolioService groupPortFolioService;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;

    @PostMapping("/api/uam/portfolio/filter")
    public ResponseEntity<?> getPortFolio(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "userName") String sortingOrder,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document portFolioRights;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_PORT_FOLIO_RIGHTS", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            groupPortFolioService.getPortFolioRightsExport(reportDate, "Group_PortFolio_RightsTemplate", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);


            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        portFolioRights = groupPortFolioService.getPortfolioData(reportDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());
        return new ResponseEntity<>(portFolioRights, HttpStatus.OK);
    }

    @PostMapping("/api/uam/portfolio/update/portfolio-status")
    public void updatePortfolioStatus(@RequestParam String reportDate) throws IOException {
        groupPortFolioService.updateStatus(reportDate);

    }

    @PostMapping("/api/uam/portfolio/getCount")
    public Document getCount(@RequestParam String reportDate, @RequestParam String sortOrder, @RequestParam String sortBy) throws IOException {
        return groupPortFolioService.getCount(reportDate, sortOrder, sortBy);
    }

    @PostMapping("/api/uam/portfolio/group-compare")
    public Document compareGroupComparePortfolioRights(@RequestParam String fromGroupLabel,
                                                       @RequestParam String toGroupLabel,
                                                       @RequestParam String reportDate,
                                                       @RequestParam String compareType) throws IOException {
        return groupPortFolioService.comparePortfolioRightsByGroupLabel(fromGroupLabel, toGroupLabel, reportDate, compareType);
    }

}
