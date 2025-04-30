package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.UserGroupAccessRightsService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class UserGroupAccessRightsController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private UserGroupAccessRightsService userGroupAccessRightsService;

    @PostMapping("/api/uam/user-access-rights/filter")
    public ResponseEntity<?> getClosingEntityData(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "userName") String sortingOrder,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
//            @RequestParam(defaultValue = "active") String status,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document userRights;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("USER_RIGHTS", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            userGroupAccessRightsService.getUserRightsExport(reportDate, "User_List_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        userRights = userGroupAccessRightsService.getUserRights(reportDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());
        return new ResponseEntity<>(userRights, HttpStatus.OK);
    }

    @PostMapping("api/uam/user-access-rights/department/update")
    private void updateDepartmentField(@RequestParam String reportDate) {
        userGroupAccessRightsService.updateUserGroupDepartmentField(reportDate);
    }
}




