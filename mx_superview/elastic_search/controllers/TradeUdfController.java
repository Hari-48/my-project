package com.finsurge.tmr_portal.mx_superview.elastic_search.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.TradeUdfEntityConfig;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.TradeUdfService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping
public class TradeUdfController {

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private AuditUtils auditUtils;
    @Autowired
    private TradeUdfService tradeUdfService;

    @PostMapping("/api/uam/trade-udf/headers")
    public ResponseEntity<?> getHeaders(
            @RequestParam String reportDate,
            @RequestParam String entityType,
            @RequestParam(required = false, defaultValue = "l1") String fxdLayoutIdentifier,
            @RequestParam(defaultValue = "field") String sortBy,
            @RequestParam(defaultValue = "desc") String sortingOrder,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) {
        try {
            TradeUdfEntityConfig config = TradeUdfEntityConfig.from(entityType, fxdLayoutIdentifier);
            if (isExport) {
                DownloadJob job = downloadJobService.createNewJob("TRADE_UDF_HEADER", authentication.getName());
                if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                        || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                    throw new Exception("Output format not supported!");
                }
                tradeUdfService.getTradeUdfHeadersExport(reportDate, "Trade_Udf_Header_Template",
                        elasticSearchModel.fieldMaps, elasticSearchModel.color, elasticSearchModel.fieldColumns,
                        config.getIndexName(), config.getEntityClass(), authentication.getName(), config.getExcludeFieldsKey(), config.getIntegerFieldsKey(), config.getDoubleFieldskey(),
                        job, outputFormat, sortBy, sortingOrder, pageSize, elasticSearchModel);
                auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
                return new ResponseEntity<>(job, HttpStatus.OK);
            }
            Document document = tradeUdfService.getTradeUdfHeaders(reportDate, sortBy, sortingOrder, pageSize, config.getIndexName(), config.getEntityClass(), authentication.getName(), config.getExcludeFieldsKey(),
                    config.getIntegerFieldsKey(), config.getDoubleFieldskey());
            return ResponseEntity.ok(document);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Document("error", "Unable to fetch headers"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/api/uam/trade-udf/field-value-count")
    public ResponseEntity<?> getTradeUdfFieldValueCount(
            @RequestParam String reportDate,
            @RequestParam String tradeUdfFieldName,
            @RequestParam String entityType,
            @RequestParam(defaultValue = "fieldValue") String sortBy,
            @RequestParam(defaultValue = "desc") String sortingOrder,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody ElasticSearchModel elasticSearchModel,
            Authentication authentication) throws Exception {
        try {
            TradeUdfEntityConfig config = TradeUdfEntityConfig.from(entityType);
            if (isExport) {
                DownloadJob job = downloadJobService.createNewJob("TRADE_UDF_FIELD_VALUE", authentication.getName());
                if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                        || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                    throw new Exception("Output format not supported!");
                }
                tradeUdfService.getTradeUdfFieldValueExport(reportDate, tradeUdfFieldName, "Trade_Udf_Field_Value_Template",
                        elasticSearchModel.fieldMaps, elasticSearchModel.color, elasticSearchModel.fieldColumns,
                        config.getIndexName(), config.getEntityClass(), authentication.getName(), config.getIntegerFieldsKey(), config.getDoubleFieldskey(),
                        job, outputFormat, sortBy, sortingOrder, pageSize, elasticSearchModel);
                auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                        null, null, null, authentication, null, null, null, null, null);
                return new ResponseEntity<>(job, HttpStatus.OK);
            }
            Document tradeUdfFieldValueCount = tradeUdfService.getTradeUdfFieldValueCount(reportDate, tradeUdfFieldName, sortBy, sortingOrder, pageSize,
                    config.getIndexName(), config.getEntityClass(), authentication.getName(), config.getIntegerFieldsKey(), config.getDoubleFieldskey());
            return new ResponseEntity<>(tradeUdfFieldValueCount, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Document("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Document("error", "Unable to fetch field value count"));
        }
    }

}
