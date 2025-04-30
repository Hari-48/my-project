package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.ChineseWallService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.GroupNavigationRightsService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.MxCounterpartyService;
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
public class ChineseWallController {
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private ChineseWallService chineseWallService;
    @Autowired
    private MxCounterpartyService mxCounterpartyService;
    @Autowired
    private GroupNavigationRightsService groupNavigationRightsService;

    @PostMapping("/api/uam/chinese-wall/filter")
    public ResponseEntity<?> getChineseWallData(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "templateLabel") String sortingOrder,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestParam(required = false, defaultValue = "false") Boolean inActiveTemplate,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {
        Document chineseWall;
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CHINESE_WALL", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            chineseWallService.getChineseWallExport(reportDate, "Chinese_Wall_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel, inActiveTemplate);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        chineseWall = chineseWallService.getChineseWallData(reportDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName(), inActiveTemplate);
        return new ResponseEntity<>(chineseWall, HttpStatus.OK);
    }


    // get the vennDiagram - count :=
    @PostMapping("api/uam/chinese-wall/shared-counterparts/chart-count")
    public List<Document> getChartCount(@RequestBody List<String> templateLabelList, @RequestParam String reportDate) throws IOException {
        return chineseWallService.getChartCount(templateLabelList, reportDate);
    }

    // get the corresponding - unique Counterpart label :=
    @PostMapping("/api/uam/chinese-wall/shared-counterparts/get-counterparts-label")
    private List<String> VennDiagram(@RequestParam(required = false) List<String> templateLabel,
                                     @RequestParam List<String> coordinates,
                                     @RequestParam List<String> intersection,
                                     @RequestParam String exclusion,
                                     @RequestParam List<String> intersectionAll,
                                     @RequestParam String reportDate) {
        return chineseWallService.getUniqueSetCounterpartyData(templateLabel, coordinates, intersection, exclusion, intersectionAll, reportDate);
    }

    @PostMapping("/api/uam/admin/manual/update")
    public ResponseEntity<?> updateChineseWallTemplate(@RequestParam String reportDate, @RequestParam String reportName) throws IOException {
        switch (reportName) {
            case "chinesewalltmpleod":
                chineseWallService.updateChineseWallTemplate(reportDate);
                break;
            case "counterpartyeod":
                mxCounterpartyService.updateAllDuplicate(reportDate);
            case "groupnavrightseod":
                //  groupNavigationRightsService.updateNavigationDepartment(reportDate);
                groupNavigationRightsService.updateUserGroupDepartmentField(reportDate);
                //update - Amend Rights
                groupNavigationRightsService.updateRightsPriorityLevel(reportDate);
        }
        return new ResponseEntity<>("Successfully updated", HttpStatus.OK);
    }

}



