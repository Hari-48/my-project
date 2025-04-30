package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.entity.UamSummaryReportJob;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.service.GroupCompareExportService;
import com.finsurge.tmr_portal.mx_superview.service.UamSummaryReportJobService;
import com.finsurge.tmr_portal.mx_superview.service.ViewerExportService;
import io.swagger.annotations.ApiOperation;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin("*")
public class GroupCompareExportController {
    private final DownloadJobService downloadJobService;

    private final UamSummaryReportJobService reportJobService;
    private final GroupCompareExportService groupCompareExportService;
    private final ViewerExportService viewerExportService;

    public GroupCompareExportController(DownloadJobService downloadJobService, UamSummaryReportJobService reportJobService, GroupCompareExportService groupCompareExportService, ViewerExportService viewerExportService) {
        this.downloadJobService = downloadJobService;
        this.reportJobService = reportJobService;
        this.groupCompareExportService = groupCompareExportService;
        this.viewerExportService = viewerExportService;
    }

    @ApiOperation("get group wise data Comparison Export for portfolio")
    @PostMapping("/api/uam/groups-compare-detail/portfolio")
    public ResponseEntity<?> detailedGroupComparePortfolioRights(@RequestParam boolean isExport,
                                                                 @RequestParam(required = false) String outputFormat,
                                                                 @RequestBody(required = false) PortfolioExportXlsxRequest portfolioRights,
                                                                 @RequestParam(defaultValue = "all") String type,
                                                                 Authentication authentication) throws Exception {
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_PORTFOLIO_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getPortfolioDetailsAndExport(portfolioRights.getFilters(), "Group_Compare_PortfolioRights", portfolioRights.fieldMaps, portfolioRights.color,
                    job, outputFormat, portfolioRights.fieldColumns, type);

            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary

        // ElasticSearch Summary Report job creation
        UamSummaryReportJob job = reportJobService.createNewJob("GROUP_COMPARE_PORTFOLIO_SUMMARY",authentication.getName());
        // summary
        CompletableFuture<Void> future  = groupCompareExportService.getPortfolioDetailsSummaryReport(portfolioRights.getFilters(), type,job);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    //OSP Rights
    @ApiOperation("get group wise data Comparison for OSP rights")
    @PostMapping("/api/uam/groups-compare-detail/osp-rights")
    public ResponseEntity<?> detailedGroupCompareOSPRights(@RequestParam boolean isExport,
                                                           @RequestParam(required = false) String outputFormat,
                                                           @RequestBody(required = false) OspRightsExportXlsxRequest ospRightsMatrix,
                                                           @RequestParam(defaultValue = "all") String type,
                                                           Authentication authentication) throws Exception {
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_OSP_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getOspRightsDetailsAndExport(ospRightsMatrix.getFilters(), "Group_Compare_OspRights", ospRightsMatrix.fieldMaps, ospRightsMatrix.color,
                    job, outputFormat, ospRightsMatrix.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // ElasticSearch Summary Report job creation
        UamSummaryReportJob job = reportJobService.createNewJob("GROUP_COMPARE_OSP_EXPORRT",authentication.getName());
        // summary
        CompletableFuture<Void> future  = groupCompareExportService.getOspReportsSummaryReport(ospRightsMatrix.getFilters(), type,job);
        return new ResponseEntity<>(job, HttpStatus.OK);
//        Map<String, Long> getOspReportsSummary  = groupCompareExportService.getOspReportsSummaryReport(ospRightsMatrix.getFilters(), type);
//        return new ResponseEntity<>(getOspReportsSummary, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison Export for Chinesewall")
    @PostMapping("/api/uam/groups-compare-detail/chinesewall")
    public ResponseEntity<?> detailedGroupCompareChinesewall(@RequestParam boolean isExport,
                                                                 @RequestParam(required = false) String outputFormat,
                                                                 @RequestBody(required = false) ChineseWallExportXlsxRequest chineseWallExportXlsxRequest,
                                                                 @RequestParam(defaultValue = "all") String type,
                                                                 Authentication authentication) throws Exception {
        Document document = new Document();
        if (chineseWallExportXlsxRequest.getFilters().getCompareTemplateValue() == null && chineseWallExportXlsxRequest.getFilters().getTemplateValue() == null) {
            document.put("message", "No records found");
            document.put("content", Collections.emptyList());
        }
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_CHINESEWALL_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getChineseWallDetailsAndExport(chineseWallExportXlsxRequest.getFilters(),
                    "Group_Compare_Chinesewall", chineseWallExportXlsxRequest.fieldMaps, chineseWallExportXlsxRequest.colors,
                    job, outputFormat, chineseWallExportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        // ElasticSearch Summary Report job creation
        UamSummaryReportJob job = reportJobService.createNewJob("GROUP_COMPARE_CHINESE_SUMMARY",authentication.getName());
        // summary
        CompletableFuture<Void> future  = groupCompareExportService.getChineseWallSummaryReport(chineseWallExportXlsxRequest.getFilters(), type,job);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison Export for Enterprise")
    @PostMapping("/api/uam/groups-compare-detail/enterprise")
    public ResponseEntity<?> detailedGroupCompareEnterprise(@RequestParam boolean isExport,
                                                             @RequestParam(required = false) String outputFormat,
                                                             @RequestBody(required = false) EnterpriseExportXlsxRequest enterpriseExportXlsxRequest,
                                                             @RequestParam(defaultValue = "all") String type,
                                                             Authentication authentication) throws Exception {

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_ENTERPRISE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getEnterpriseDetailsAndExport(enterpriseExportXlsxRequest.getFilters(),
                    "Group_Compare_Enterprise", enterpriseExportXlsxRequest.fieldMaps, enterpriseExportXlsxRequest.colors,
                    job, outputFormat, enterpriseExportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        Map<String, Long> getEnterpriseSummary  = groupCompareExportService.getEnterpriseSummaryReport(enterpriseExportXlsxRequest.getFilters(), type);
        return new ResponseEntity<>(getEnterpriseSummary, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison Export for portfolio")
    @PostMapping("/api/uam/groups-compare-detail/combineportfolio")
    public ResponseEntity<?> detailedGroupCompareGroupCombinePortfolioRights(@RequestParam boolean isExport,
                                                                             @RequestParam(required = false) String outputFormat,
                                                                             @RequestBody(required = false) GroupCompPortfolioExport portfolioRights,
                                                                             @RequestParam(defaultValue = "all") String type,
                                                                             Authentication authentication) throws Exception {

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_COMBINED_PORTFOLIO_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getGroupCombinePortfolioDetailsAndExport(portfolioRights.getFilters(), "Group_Compare_PortfolioRights", portfolioRights.fieldMaps, portfolioRights.colors,
                    job, outputFormat, portfolioRights.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        UamSummaryReportJob job = reportJobService.createNewJob("GROUP_COMPARE_COMBINED_PORTFOLIO",authentication.getName());
        // summary
        CompletableFuture<Void> future  = groupCompareExportService.getGroupCombinePortfolioDetailsSummaryReportDetails(portfolioRights.getFilters(), type,job);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }
    @ApiOperation("get group wise data Comparison Export for navigation")
    @PostMapping("/api/uam/groups-compare-detail/navigation")
    public ResponseEntity<?> detailedGroupCompareNavigation(@RequestParam boolean isExport,
                                                            @RequestParam(required = false) String outputFormat,
                                                            @RequestBody(required = false) NavigationExportXlsxRequest navigationExportXlsxRequest,
                                                            @RequestParam(defaultValue = "all") String type,
                                                            Authentication authentication) throws Exception {

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_NAVIGATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getNavigationDetailsAndExport(navigationExportXlsxRequest.getFilters(),"Group_Compare_Navigation", navigationExportXlsxRequest.fieldMaps, navigationExportXlsxRequest.color,
                    job, outputFormat, navigationExportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        UamSummaryReportJob job = reportJobService.createNewJob("GROUP_COMPARE_NAVIGATION_EXPORRT",authentication.getName());
        // summary
        CompletableFuture<Void> future  =  groupCompareExportService.getNavigationDetailsSummaryReport(navigationExportXlsxRequest.getFilters(), type,job);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison Export for consistency")
    @PostMapping("/api/uam/groups-compare-detail/consistency-template")
    public ResponseEntity<?> detailedGroupCompareConsistencyTemplate(@RequestParam boolean isExport,
                                                                     @RequestParam(required = false) String outputFormat,
                                                                     @RequestBody(required = false) ConsistencyExportXlsxRequest exportXlsxRequest,
                                                                     @RequestParam(defaultValue = "all") String type,
                                                                     Authentication authentication) throws Exception {

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_CONSISTENCY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getGroupConsistencyAndExport(exportXlsxRequest.getFilters(), "Group_Compare_Consistency", exportXlsxRequest.fieldMaps, exportXlsxRequest.colors,
                    job, outputFormat, exportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        Map<String, Long> geConsistencyDetailsSummary  = groupCompareExportService.getConsistencyDetailsSummaryReport(exportXlsxRequest.getFilters(), type);
        return new ResponseEntity<>(geConsistencyDetailsSummary, HttpStatus.OK);
    }

//    @ApiOperation("get group wise data Comparison Export for stp")
//    @PostMapping("/api/uam/groups-compare-detail/stp-template")
//    public ResponseEntity<?> detailedGroupCompareStpTemplate(@RequestParam boolean isExport,
//                                                             @RequestParam(required = false) String outputFormat,
//                                                             @RequestBody(required = false) StpExportXlsx exportXlsxRequest,
//                                                             @RequestParam(defaultValue = "all") String type,
//                                                             Authentication authentication) throws Exception {
//        LocalDate requestDate = viewerExportService.getLatestDate();
//
//        if (isExport) {
//            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_STP_EXPORT", authentication.getName());
//            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
//                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
//                throw new Exception("Output format not supported!");
//            }
//            CompletableFuture<Void> future = groupCompareExportService.getGroupStpAndExport(exportXlsxRequest.getFilters(), "Group_Compare_Stp", exportXlsxRequest.fieldMaps, exportXlsxRequest.colors,requestDate,
//                    job, outputFormat, exportXlsxRequest.fieldColumns, type);
//            // save audit for this action
//            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
//                    null, null, null, authentication, null, null, null, null, null);
//            return new ResponseEntity<>(job, HttpStatus.OK);
//        }
//        // summary
//      //  Map<String, Long> getStpDetailsSummary  = groupCompareExportService.getStpDetailsSummaryReport(exportXlsxRequest.getFilters(), type);
//        // ElasticSearch Summary Report job creation
//        UamSummaryReportJob job = reportJobService.createNewJob("GROUP_COMPARE_STP_EXPORRT",authentication.getName());
//        // summary
//        CompletableFuture<Void> future  =  groupCompareExportService.getStpDetailsSummaryReport(exportXlsxRequest.getFilters(), type,job,requestDate);
//        return new ResponseEntity<>(job, HttpStatus.OK);
//    }

    @ApiOperation("get group wise data Comparison Export for finance")
    @PostMapping("/api/uam/groups-compare-detail/finance")
    public ResponseEntity<?> detailedGroupCompareFinanceRights(@RequestParam boolean isExport,
                                                                     @RequestParam(required = false) String outputFormat,
                                                                     @RequestBody(required = false) FinanceExportXlsxRequest exportXlsxRequest,
                                                                     @RequestParam(defaultValue = "all") String type,
                                                                     Authentication authentication) throws Exception {
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_FINANCE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getFinanceDetailsAndExport(exportXlsxRequest.getFilters(), "Group_Compare_Finance", exportXlsxRequest.fieldMaps, exportXlsxRequest.color,
                    job, outputFormat, exportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        Map<String, Long> getFinanceRightsSummary  = groupCompareExportService.getFinanceSummaryReport(exportXlsxRequest.getFilters(), type);
        return new ResponseEntity<>(getFinanceRightsSummary, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison Export for Configuration management")
    @PostMapping("/api/uam/groups-compare-detail/configuration")
    public ResponseEntity<?> detailedGroupCompareConfiguration(@RequestParam boolean isExport,
                                                               @RequestParam(required = false) String outputFormat,
                                                               @RequestBody(required = false) CwtConfigExportXlsxRequest cwtConfigExportXlsxRequest,
                                                               @RequestParam(defaultValue = "all") String type,
                                                               Authentication authentication) throws Exception {

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_CONFIGURATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getConfigurationDetailsAndExport(cwtConfigExportXlsxRequest.getFilters(),
                    "Group_Compare_Configuration", cwtConfigExportXlsxRequest.fieldMaps, cwtConfigExportXlsxRequest.color,
                    job, outputFormat, cwtConfigExportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        Map<String, Long> getEnterpriseSummary  = groupCompareExportService.getConfigurationSummaryReport(cwtConfigExportXlsxRequest.getFilters(), type);
        return new ResponseEntity<>(getEnterpriseSummary, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison Export for operation rights")
    @PostMapping("/api/uam/groups-compare-detail/operation-rights")
    public ResponseEntity<?> detailedGroupCompareOperationRights(@RequestParam boolean isExport,
                                                             @RequestParam(required = false) String outputFormat,
                                                             @RequestBody(required = false) OperationExportXlsx exportXlsxRequest,
                                                             @RequestParam(defaultValue = "all") String type,
                                                             Authentication authentication) throws Exception {
        Document document = new Document();
        if (exportXlsxRequest.getFilters().getCompareTemplateValue() == null && exportXlsxRequest.getFilters().getTemplateValue() == null) {
            document.put("message", "No records found");
            document.put("content", Collections.emptyList());
        }
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMPARE_OPERATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = groupCompareExportService.getGroupOperationAndExport(exportXlsxRequest.getFilters(), "Group_Compare_Operation", exportXlsxRequest.fieldMaps, exportXlsxRequest.colors,
                    job, outputFormat, exportXlsxRequest.fieldColumns, type);
            // save audit for this action
            groupCompareExportService.doAudit(AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        // summary
        Map<String, Long> getStpDetailsSummary  = groupCompareExportService.getOperationDetailsSummaryReport(exportXlsxRequest.getFilters(), type);
        return new ResponseEntity<>(getStpDetailsSummary, HttpStatus.OK);
    }
}


