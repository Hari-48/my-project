package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.ChineseWallClosingEntityLabelService;
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

public class ChineseWallClosingEntityLabelController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private ChineseWallClosingEntityLabelService chineseWallClosingEntityLabelService;
    @Autowired
    private AuditUtils auditUtils;


    @PostMapping("/api/uam/chinese-wall-closing-entity-label/filter")
    public ResponseEntity<?> getChineseWallClosingEntityLabel(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "templateLabel") String sortingOrder,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {

        Document chineseWallClosingEntityLabelList;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CHINESE_WALL_CLOSING_ENTITY_LABEL", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            chineseWallClosingEntityLabelService.getChineseWallClosingEntityLabelListExport("Chinese_Wall_Closing_Entity_Label_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.fieldColumns, job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch, bulkFilterSearchType, elasticSearchModel);

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //if (sortingOrder.equalsIgnoreCase("asc"))
        chineseWallClosingEntityLabelList = chineseWallClosingEntityLabelService.getChineseWallClosingEntityLabelList(
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType, authentication.getName());

        return new ResponseEntity<>(chineseWallClosingEntityLabelList, HttpStatus.OK);
    }

    @PostMapping("/api/uam/chinese-wall-closing-entity-label/update")
    public ResponseEntity<?> updateEntityLabelField(@RequestParam String templateLabel,
                                                   @RequestParam List<String> entityLabel,
                                                   @RequestParam Boolean userConfirmation,
                                                   Authentication authentication) throws IOException {
        return chineseWallClosingEntityLabelService.updateEntityLabelField(templateLabel, entityLabel,userConfirmation, authentication.getName());
    }

    @PostMapping("/api/uam/closing-entity-label/filter")
    public ResponseEntity<?> getClosingEntityLabel(
            Authentication authentication) throws Exception {

        Document closingEntityLabelList;


        //if (sortingOrder.equalsIgnoreCase("asc"))
        closingEntityLabelList = chineseWallClosingEntityLabelService.getClosingEntityLabelList();

        return new ResponseEntity<>(closingEntityLabelList, HttpStatus.OK);
    }

    @PostMapping("/api/uam/closing-entity-label/compare")
    public ResponseEntity<?> compareClosingEntityLabel(
            @RequestParam(required = false) String reportDate,
            Authentication authentication) throws Exception {

        chineseWallClosingEntityLabelService.closingEntityLabelCompare(reportDate);

        return new ResponseEntity<>("Closing entity label comparison completed successfully", HttpStatus.OK);
    }

    @PostMapping("/api/uam/chinese-wall-closing-entity-label/compare")
    public ResponseEntity<?> compareChineseWallClosingEntityLabel(
            @RequestParam(required = false) String reportDate,
            Authentication authentication) throws Exception {

        chineseWallClosingEntityLabelService.chineseWallTemplateCompare(reportDate);

        return new ResponseEntity<>("Chinese Wall Closing Entity Label comparison completed successfully", HttpStatus.OK);
    }


}
