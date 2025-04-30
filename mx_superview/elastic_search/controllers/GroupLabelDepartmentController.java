package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.GroupLabelDepartmentService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Controller
@RestController
@CrossOrigin
@RequiredArgsConstructor
public class GroupLabelDepartmentController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private GroupLabelDepartmentService groupLabelDepartmentService;
    @Autowired
    private AuditUtils auditUtils;

    @PostMapping("/api/uam/group-department/filter")
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

        Document groupDepartment;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_LIST", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            groupLabelDepartmentService.getGroupLabelDepartmentExport(createdDate, "Group_List_Template", elasticSearchModel,
                    job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        groupDepartment = groupLabelDepartmentService.getGroupLabelDepartment(createdDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());

        return new ResponseEntity<>(groupDepartment, HttpStatus.OK);
    }

    /* update department for user groups in uam Group Department Page*/
    @PostMapping("/api/uam/update-departmentField")
    public ResponseEntity<?> updateDepartmentField(@RequestParam String department, @RequestParam String groupLabel, @RequestParam String grpRoleStr, @RequestParam Boolean userConfirmation, @RequestParam String updatedDepartment, Authentication authentication) throws IOException {
        return groupLabelDepartmentService.updateDepartmentField(department, groupLabel, grpRoleStr, authentication.getName(), userConfirmation, updatedDepartment);

    }

}
