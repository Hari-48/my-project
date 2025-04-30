package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.GroupLabelDepartmentService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.NavigationDepartmentService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class NavigationDepartmentController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private NavigationDepartmentService navigationDepartmentService;
    @Autowired
    private AuditUtils auditUtils;

    @PostMapping("/api/uam/navigation-department/filter")
    public ResponseEntity<?> getGroupList(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "role") String sortingOrder,
            @RequestParam(required = false) String createdDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document navigationDepartment;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_LIST", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            navigationDepartmentService.getNavigationDepartmentExport(createdDate, "Group_Navigation_Rights_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        navigationDepartment = navigationDepartmentService.getNavigationDepartmentData(createdDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());

        return new ResponseEntity<>(navigationDepartment, HttpStatus.OK);
    }

    @PostMapping("/api/uam/navigation-department/update")
    public ResponseEntity<?> updateDepartmentField(@RequestParam String path,
                                                   @RequestParam Boolean userConfirmation,
                                                   @RequestParam List<String> newRights,
                                                   Authentication authentication) throws IOException {
        return navigationDepartmentService.updateDepartmentField(path, authentication.getName(), userConfirmation, newRights);
    }
}
