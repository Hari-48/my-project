package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin
@RequiredArgsConstructor
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;

    @PostMapping("api/uam/admin/insert-department")
    public ResponseEntity<?> insertDep(@RequestBody List<String> departmentNames) {
        return departmentService.insertDepartment(departmentNames);
    }

    // list the department
    @PostMapping("api/uam/admin/list-department")
    public ResponseEntity<?> listDepartment(@RequestParam(required = false, defaultValue = "department") String sortBy,
                                            @RequestParam(required = false, defaultValue = "asc") String orderBy,
                                            @RequestParam(required = false, defaultValue = "20") int pageSize,
                                            @RequestParam(required = false) Boolean isExport,
                                            @RequestParam(required = false) String outputFormat,
                                            @RequestBody ElasticSearchModel elasticSearchModel,
                                            Authentication authentication) throws Exception {
        if (isExport) {
            // Create a new download job
            DownloadJob job = downloadJobService.createNewJob("LIST_DEPARTMENT", authentication.getName());
            // Validate the output format
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            // Trigger the export process asynchronously
            departmentService.getListDepartmentExport("List_Department_Template",
                    elasticSearchModel.fieldMaps, elasticSearchModel.color, elasticSearchModel.fieldColumns,
                    authentication.getName(), job, outputFormat, sortBy, orderBy, pageSize, elasticSearchModel);
            // Save audit for export action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            // Return the job details so the front-end can track the progress
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        Document document = departmentService.listData(sortBy, orderBy, pageSize);
        return ResponseEntity.ok(document);
    }

    @PostMapping("api/uam/admin/update-department")
    public ResponseEntity<?> updateDepartment(@RequestParam String id, @RequestParam String departmentName, @RequestParam String newDepartmentName, Authentication authentication) throws IOException {
        return departmentService.updateDepartment(id, departmentName, newDepartmentName, authentication.getName());
    }

    @PutMapping("api/uam/admin/update-department-status")
    public ResponseEntity<?> updateDepartmentStatus(@RequestParam String id, @RequestParam String departmentName, Authentication authentication) throws IOException {
        return departmentService.updateDepartmentStatus(id, departmentName, authentication.getName());
    }

}
