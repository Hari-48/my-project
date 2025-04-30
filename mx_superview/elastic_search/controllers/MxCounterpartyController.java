package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
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

@CrossOrigin
@RestController
@RequiredArgsConstructor
public class MxCounterpartyController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private MxCounterpartyService mxCounterpartyService;

    @PostMapping("/api/uam/counter-party/filter")
    public ResponseEntity<?> getCounterPartySearchData(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "entityLabel") String sortingOrder,
            @RequestParam String reportDate,
            @RequestParam boolean isGlobalSearch,
            @RequestParam(required = false, defaultValue = "exact") String bulkFilterSearchType,
            @RequestParam String tab,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestParam(required = false) Boolean counterpartyCreation,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {
        Document counterparty;
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("COUNTERPARTY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            mxCounterpartyService.getCounterpartyExport(reportDate, "Closing_Entity_Template", elasticSearchModel.fieldMaps,
                    elasticSearchModel.color, elasticSearchModel.getFieldColumns(), job, outputFormat, sortBy, sortingOrder, authentication.getName(), pageSize, isGlobalSearch,bulkFilterSearchType, tab,elasticSearchModel,counterpartyCreation);

            // save audit for this actionclosingEntityService
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        //if (sortingOrder.equalsIgnoreCase("asc"))
        counterparty = mxCounterpartyService.getCounterpartyData(reportDate,
                elasticSearchModel, sortBy, sortingOrder, pageSize, isGlobalSearch, bulkFilterSearchType,tab,counterpartyCreation,authentication.getName());
        return new ResponseEntity<>(counterparty, HttpStatus.OK);
    }

    @PostMapping("/api/uam/counter-party/udfFields")
    public ResponseEntity<?> getUDFMultiValueFields(@RequestParam String fieldName,
                                                    @RequestParam String reportDate,
                                                    @RequestParam(required = false, defaultValue = "desc") String sortBy) throws IOException {
        return new ResponseEntity<>(mxCounterpartyService.getCounterPartyUDFMultiFieldValues(fieldName, reportDate, sortBy), HttpStatus.OK);
    }


    @PostMapping("/api/uam/counter-party/duplicate/dsp-label")
    public List<String> getDuplicateData(@RequestParam String reportDate) {
        return mxCounterpartyService.getDuplicateDspLabel(reportDate);
    }

    // Listener
    @PostMapping("/api/uam/counter-party/update/duplicate/dupTab")
    private void updateDupTabs(String reportDate){
        mxCounterpartyService.updateAllDuplicate(reportDate);
    }

    // Listener
    @PostMapping("/api/uam/counter-party/update/duplicate/counterpartStatic")
    private void updateCounterpartStatic(String reportDate){
        mxCounterpartyService.updateDuplicateTopHits(reportDate);
    }

//    @PostMapping("/api/uam/counter-party/merge")
//    private void merge(String reportDate){
//        mxCounterpartyService.mergeMgrId(reportDate);
//    }

//    @PostMapping("/api/uam/counter-party/duplicate/update")
//    public void update() {
//        mxCounterpartyService.updateDuplicateStatusNew();
//        ;
//
//    }
}
