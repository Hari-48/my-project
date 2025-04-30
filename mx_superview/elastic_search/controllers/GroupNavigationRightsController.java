package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.GroupNavigationRightsService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@CrossOrigin
public class GroupNavigationRightsController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private GroupNavigationRightsService groupNavigationRightsService;

    @PostMapping("/api/uam/group-navigation-rights/filter")
    public ResponseEntity<?> getGroupList(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "groupLabel") String sortingOrder,
            @RequestParam String reportDate,
            // @RequestParam (defaultValue = "active") String status ,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document groupNavigationRightsList;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_NAVIGATION_RIGHTS", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            groupNavigationRightsService.getNavigationRightsExport(reportDate, "Group_Navigation_Rights_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        groupNavigationRightsList = groupNavigationRightsService.getNavigationRightsData(reportDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());

        return new ResponseEntity<>(groupNavigationRightsList, HttpStatus.OK);
    }

    @PostMapping("api/uam/group-navigation/update/user_group_department")
    private void updateDepartmentField(@RequestParam String reportDate) throws IOException, InterruptedException {
        groupNavigationRightsService.updateUserGroupDepartmentField(reportDate);
    }

    @PostMapping("api/uam/group-navigation/update/amend_rights")
    private void updateNavigationDepartment(@RequestParam String reportDate) throws IOException, InterruptedException {
        groupNavigationRightsService.updateNavigationDepartment(reportDate);
    }

    @PostMapping("/api/uam/group-navigation/duplicate")
    public List<String> getDuplicateData(@RequestParam String reportDate) {
        return groupNavigationRightsService.getPathField(reportDate);
    }

    @PostMapping("/api/uam/group-navigation/update/rights_level")
    public void updateRightsLevel(@RequestParam String reportDate) throws IOException {
        groupNavigationRightsService.updateRightsPriorityLevel(reportDate);
    }

}


