package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.entity.UserPreference;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.repository.UserPreferenceRepository;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.general.util.Utils;
import com.finsurge.tmr_portal.mx_superview.entity.GeneralFilters;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.service.CombinedGroupsService;
import com.finsurge.tmr_portal.mx_superview.service.SnapshotDataService;
import com.finsurge.tmr_portal.mx_superview.service.ViewerExportService;
import io.swagger.annotations.ApiOperation;
import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
public class ViewerExportController {

    private final Logger log = LoggerFactory.getLogger(ViewerExportController.class);
    private final ViewerExportService viewerExportService;
    private final DateTimeFormatter dateTimeFormatter;
    private final AuditUtils auditUtils;
    private final DownloadJobService downloadJobService;
    private final UserPreferenceRepository userPreferenceRepository;
    private final CombinedGroupsService combinedGroupsService;

    public ViewerExportController(ViewerExportService viewerExportService, AuditUtils auditUtils, DownloadJobService downloadJobService, UserPreferenceRepository userPreferenceRepository, CombinedGroupsService combinedGroupsService) {
        this.viewerExportService = viewerExportService;
        this.auditUtils = auditUtils;
        this.downloadJobService = downloadJobService;
        this.userPreferenceRepository = userPreferenceRepository;
        this.combinedGroupsService = combinedGroupsService;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
    }

    @PostMapping("/api/get/groupList/{date}")
    public ResponseEntity<?> getGroupList(@PathVariable String date,
                                          @RequestParam boolean isExport,
                                          @RequestBody(required = false) GroupListExportXlsxRequest groupListExportXlsxRequest,
                                          @RequestParam(required = false) String outputFormat,
                                          @RequestParam(defaultValue = "") String groupLabel,
                                          @RequestParam boolean isAllGroups,
                                          HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(date, dateTimeFormatter);
        groupLabel = groupLabel == null || groupLabel.isBlank() ? null : groupLabel;

        if (isExport) {

            DownloadJob job = downloadJobService.createNewJob("GROUPLIST_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getGroupDetailsAndExport("GroupList", groupListExportXlsxRequest.fieldMaps, groupListExportXlsxRequest.color, job, outputFormat, requestDate, groupListExportXlsxRequest.fieldColumns, groupLabel, isAllGroups, groupListExportXlsxRequest.getGroupList());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        List<Object[]> groupNamesObj = viewerExportService.getGroupList(date);
        List<GroupList> groupNames;

        groupNames = groupNamesObj.stream().map(ch -> new GroupList(new BigInteger(ch[0].toString()).longValue(),
                ch[1] != null ? ch[1].toString() : null, ch[2] != null ? ch[2].toString() : null,
                ch[3] != null ? Long.valueOf(ch[3].toString()) : null)).collect(Collectors.toList());

        if (groupNames.isEmpty()) {
            groupNames = Collections.emptyList();
        } else {
            //user has user preference pick the user preference group
            UserPreference preference = userPreferenceRepository.findFirstByUsername(authentication.getName());
            if (preference != null) {
                if (preference.getPreferences() != null || !preference.getPreferences().isBlank()) {
                    JSONObject preObj = new JSONObject(preference.getPreferences());
                    if (preference.getPreferences().contains("uam_user_preference_group")) {
                        String userGroup = preObj.getString("uam_user_preference_group");
                        //remove user preference group's current index and add it to the top of the list
                        if (userGroup != null && !userGroup.isBlank()) {
                            GroupList groupRole = viewerExportService.getUserPreference(userGroup, requestDate);
                            if (groupRole != null) {
                                if (groupNames.contains(groupRole)) {
                                    groupNames.remove(groupRole);
                                    groupNames.add(0, groupRole);
                                }
                            } else {
                                return new ResponseEntity<>(groupNames, HttpStatus.OK);
                            }
                        }
                    }
                } else {
                    return new ResponseEntity<>(groupNames, HttpStatus.OK);
                }
            }
        }

        //todo - FO,BO,MO - MO,FO,BO
        return new ResponseEntity<>(groupNames, HttpStatus.OK);
    }


    @PostMapping("/api/uam/portfolio")
    public ResponseEntity<?> getGroupPortfolioRights(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(defaultValue = "id") String sortBy,
                                                     @RequestParam(defaultValue = "asc") String sortingOrder,
                                                     @RequestParam boolean isExport,
                                                     @RequestBody(required = false) PortfolioExportXlsxRequest portfolioRights,
                                                     @RequestParam(required = false) String outputFormat,
                                                     HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(portfolioRights.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();
        log.info("portfolio loading started");
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("PORTFOLIO_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getPortfolioDetailsAndExport(portfolioRights.filters, "PortfolioRights", portfolioRights.fieldMaps, portfolioRights.color, job, outputFormat, portfolioRights.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        Document document = new Document();
        if ((portfolioRights.filters.getSearchWhereClause() == null || portfolioRights.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (portfolioRights.getFilters().getTemplateValue() == null || portfolioRights.getFilters().getTemplateValue().isBlank() || viewerExportService.getPortfolioRightsList(requestDate, portfolioRights.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getPortfolioRights(page, pageSize, sortBy, sortingOrder, sort, portfolioRights.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, portfolioRights.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);

    }

    @PostMapping("/api/uam/consistency-template")
    public ResponseEntity<?> getGroupConsistencyTemplateRights(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                               @RequestParam(defaultValue = "id") String sortBy,
                                                               @RequestParam(defaultValue = "asc") String sortingOrder,
                                                               @RequestParam boolean isExport,
                                                               @RequestBody(required = false) ConsistencyExportXlsxRequest consistencyExportXlsxRequest,
                                                               @RequestParam(required = false) String outputFormat,
                                                               HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(consistencyExportXlsxRequest.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = null;
        Sort.Order sort1 = null;
        Sort.Order sort2 = null;
        List<Sort.Order> orders = new ArrayList<>();
        if ((!sortBy.equalsIgnoreCase("category")) || (!sortBy.equalsIgnoreCase("item"))) {
            sort = Sort.Order.asc(sortBy);
            sort1 = Sort.Order.asc("category");
            sort2 = Sort.Order.asc("item");
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
                sort1 = Sort.Order.desc("category");
                sort2 = Sort.Order.desc("item");
            }
            orders.add(sort.ignoreCase());
            orders.add(sort1.ignoreCase());
            orders.add(sort2.ignoreCase());

        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
            sort = sort.ignoreCase();
            orders.add(sort);
        }


        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CONSISTENCY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getConsistencyExport(consistencyExportXlsxRequest.getFilters(), "Consistency",
                    consistencyExportXlsxRequest.fieldMaps, consistencyExportXlsxRequest.colors, job, outputFormat, consistencyExportXlsxRequest.fieldColumns, sortBy, sortingOrder, orders, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        Document document = new Document();
        if ((consistencyExportXlsxRequest.filters.getSearchWhereClause() == null || consistencyExportXlsxRequest.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (consistencyExportXlsxRequest.getFilters().getTemplateValue() == null || consistencyExportXlsxRequest.getFilters().getTemplateValue().isBlank() || viewerExportService.getConsistencyCount(requestDate, consistencyExportXlsxRequest.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getConsitency(page, pageSize, sortBy, sortingOrder, orders, consistencyExportXlsxRequest.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, consistencyExportXlsxRequest.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/cwt-config-mgt")
    public ResponseEntity<?> getGroupCwtConfigMgtRight(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(defaultValue = "id") String sortBy,
                                                       @RequestParam(defaultValue = "asc") String sortingOrder,
                                                       @RequestParam boolean isExport,
                                                       @RequestBody(required = false) CwtConfigExportXlsxRequest cwtConfigMgtRight,
                                                       @RequestParam(required = false) String outputFormat,
                                                       HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(cwtConfigMgtRight.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();


        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CWT_CONFIG_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getCwtConfigMgtRightsAndExport(cwtConfigMgtRight.getFilters(), "CwtConfigMgtRights",
                    cwtConfigMgtRight.fieldMaps, cwtConfigMgtRight.color, job, outputFormat, cwtConfigMgtRight.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        //getCwtConfigRightsCount
        Document document = new Document();
        if ((cwtConfigMgtRight.filters.getSearchWhereClause() == null || cwtConfigMgtRight.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (cwtConfigMgtRight.getFilters().getTemplateValue() == null || cwtConfigMgtRight.getFilters().getTemplateValue().isBlank() || viewerExportService.getCwtConfigRightsCount(requestDate, cwtConfigMgtRight.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getConfiguration(page, pageSize, sortBy, sortingOrder, sort, cwtConfigMgtRight.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, cwtConfigMgtRight.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/osp-rights-matrix")
    public ResponseEntity<?> getGroupOSPRightsMatrix(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(defaultValue = "id") String sortBy,
                                                     @RequestParam(defaultValue = "asc") String sortingOrder,
                                                     @RequestParam boolean isExport,
                                                     @RequestBody(required = false) OspRightsExportXlsxRequest ospRightsMatrix,
                                                     @RequestParam(required = false) String outputFormat,
                                                     HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(ospRightsMatrix.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);

        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("OSP_RIGHTS_MATRIX_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getOspRightsMatrixAndExport(ospRightsMatrix.getFilters(), "OspRightsMatrix",
                    ospRightsMatrix.fieldMaps, ospRightsMatrix.color, job, outputFormat, ospRightsMatrix.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        Document document = new Document();
        if ((ospRightsMatrix.filters.getSearchWhereClause() == null || ospRightsMatrix.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (ospRightsMatrix.getFilters().getTemplateValue() == null || ospRightsMatrix.getFilters().getTemplateValue().isBlank() || viewerExportService.getOspRightsMatrixCount(requestDate, ospRightsMatrix.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getOspMatrix(page, pageSize, sortBy, sortingOrder, sort, ospRightsMatrix.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, ospRightsMatrix.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/navigation")
    public ResponseEntity<?> getGroupNavRights(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(defaultValue = "id") String sortBy,
                                               @RequestParam(defaultValue = "asc") String sortingOrder,
                                               @RequestParam boolean isExport,
                                               @RequestBody(required = false) NavigationExportXlsxRequest navigationRights,
                                               @RequestParam(required = false) String outputFormat,
                                               HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(navigationRights.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("NAVIGATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getNavigationRightsAndExport(navigationRights.getFilters(), "NavigationRights", navigationRights.fieldMaps, navigationRights.color, job, outputFormat, navigationRights.fieldColumns, navigationRights.getGroupLabel(), sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        Document document = new Document();
        if ((navigationRights.filters.getSearchWhereClause() == null || navigationRights.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (navigationRights.getFilters().getTemplateValue() == null || navigationRights.getFilters().getTemplateValue().isBlank() || viewerExportService.getNavigationList(requestDate, navigationRights.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getNavigationRights(page, pageSize, sortBy, sortingOrder, sort, navigationRights.filters, navigationRights.getGroupLabel(), authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, navigationRights.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/operationRight")
    public ResponseEntity<?> getOperationRights(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(defaultValue = "id") String sortBy,
                                                @RequestParam(defaultValue = "asc") String sortingOrder,
                                                @RequestParam boolean isExport,
                                                @RequestBody(required = false) OperationExportXlsx operationExportXlsx,
                                                @RequestParam(required = false) String outputFormat,
                                                HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(operationExportXlsx.filters.getDateValue(), dateTimeFormatter);

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("OPERATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getOperationalExport(operationExportXlsx.filters, "OPERATION RIGHT", operationExportXlsx.fieldMaps,
                    operationExportXlsx.colors, job, outputFormat, operationExportXlsx.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        Document document = new Document();
        if ((operationExportXlsx.filters.getSearchWhereClause() == null || operationExportXlsx.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (operationExportXlsx.getFilters().getTemplateValue() == null || operationExportXlsx.getFilters().getTemplateValue().isBlank() || viewerExportService.getOperationRightsList(requestDate, operationExportXlsx.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getOperationalRights(page, pageSize, sortBy, sortingOrder, sort, operationExportXlsx.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, operationExportXlsx.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/finance")
    public ResponseEntity<?> getFinanceDetails(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(defaultValue = "id") String sortBy,
                                               @RequestParam(defaultValue = "asc") String sortingOrder,
                                               @RequestParam boolean isExport,
                                               @RequestBody(required = false) FinanceExportXlsxRequest financeRight,
                                               @RequestParam(required = false) String outputFormat,
                                               HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(financeRight.filters.getDateValue(), dateTimeFormatter);

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("FINANCE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getFinanceDetailsAndExport(financeRight.filters, "FinanceRights", financeRight.fieldMaps,
                    financeRight.color, job, outputFormat, financeRight.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        Document document = new Document();
        if ((financeRight.filters.getSearchWhereClause() == null || financeRight.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (financeRight.getFilters().getTemplateValue() == null || financeRight.getFilters().getTemplateValue().isBlank() || viewerExportService.getFinanceRightsList(requestDate, financeRight.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getFinanceRights(page, pageSize, sortBy, sortingOrder, sort, financeRight.filters, authentication.getName(), false, 0);

        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, financeRight.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);

    }

//    @PostMapping("/api/uam/stp-rights")
//    public ResponseEntity<?> getCombinedGroupStpRights(@RequestParam(defaultValue = "0") int page,
//                                                       @RequestParam(defaultValue = "20") int pageSize,
//                                                       @RequestParam(defaultValue = "id") String sortBy,
//                                                       @RequestParam(defaultValue = "asc") String sortingOrder,
//                                                       @RequestParam(defaultValue = "false") Boolean allGroups,
//                                                       @RequestParam boolean isExport,
//                                                       @RequestBody(required = false) CombinedStpRightsExportXlsxRequest stpRightsExportXlsxRequest,
//                                                       @RequestParam(required = false) String outputFormat,
//                                                       HttpServletRequest request, Authentication authentication) throws Exception {
//
//        LocalDate requestDate = combinedGroupsService.getLatestDates();
//        Document document = new Document();
//
//        if (stpRightsExportXlsxRequest.filters.getSearchWhereClause() != null && !stpRightsExportXlsxRequest.filters.getSearchWhereClause().isBlank() && allGroups) {
//            document.put("content", Collections.emptyList());
//            document.put("message", "Please select only eight groups for search.");
//            return new ResponseEntity<>(document, HttpStatus.OK);
//        }
//        //for sorting elements
//        sortBy = Utils.processRequestParam(sortBy);
//        sortingOrder = Utils.processRequestParam(sortingOrder);
//        Sort.Order sort;
//        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
//            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
//        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
//            sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
//        } else {
//            sort = Sort.Order.asc(sortBy);
//            if (sortingOrder.equalsIgnoreCase("desc")) {
//                sort = Sort.Order.desc(sortBy);
//            }
//        }
//        sort = sort.ignoreCase();
//
//        //to export the Data in csv,Xlxs,xls,xlsb format
//        if (isExport) {
//            DownloadJob job = downloadJobService.createNewJob("STP_EXPORT", authentication.getName());
//            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
//                throw new Exception("Output format not supported!");
//            }
//            //downloader job for consistency export
//            CompletableFuture<Void> future = combinedGroupsService.getStpRightsExport(stpRightsExportXlsxRequest.getFilters(), "Stprights", stpRightsExportXlsxRequest.fieldMaps, stpRightsExportXlsxRequest.color, job, outputFormat, stpRightsExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, allGroups, requestDate, authentication.getName());
//            // save audit for this action
//            auditUtils.saveAudit(AuditModule.ElasticSearchUtils, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
//            return new ResponseEntity<>(job, HttpStatus.OK);
//        }
    //to check whether the selected groups contains Data for the given Date
//        if (!allGroups) {
//            if (stpRightsExportXlsxRequest.getFilters().getTemplateValue() == null || stpRightsExportXlsxRequest.getFilters().getTemplateValue().isEmpty() ||
//                    stpRightsExportXlsxRequest.getFilters().getTemplateValue().size() == 0) {
//                document.put("content", new ArrayList<>());
//                return new ResponseEntity<>(document, HttpStatus.OK);
//            }
//            log.info("count started:{}", LocalDateTime.now());
//            List<STPRightsMatrix> stprights = combinedGroupsService.getStpRightsCount(requestDate, stpRightsExportXlsxRequest.filters.getTemplateValue());
//            log.info("count ended:{}", LocalDateTime.now());
//            if (stprights == null || stprights.size() == 0) {
//                document.put("content", new ArrayList<>());
//                return new ResponseEntity<>(document, HttpStatus.OK);
//            }
//        }

//        try {
//            //to retrive the Data for the given group using report Date and groupLabel and Template Name
//            log.info("Query started:{}", LocalDateTime.now());
//            document = combinedGroupsService.getStpRights(page, pageSize, sortBy, sortingOrder, sort, stpRightsExportXlsxRequest.filters, false, requestDate, authentication.getName(), false, 0);
//            log.info("count started:{}", LocalDateTime.now());
//        } catch (Exception ex) {
//            document = generalException(ex, stpRightsExportXlsxRequest.getFilters().getSearchWhereClause(), document);
//        }
//        return new ResponseEntity<>(document, HttpStatus.OK);
//    }

    @PostMapping("/api/uam/enterprise")
    public ResponseEntity<?> getEnterpriseDetails(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(defaultValue = "id") String sortBy,
                                                  @RequestParam(defaultValue = "asc") String sortingOrder,
                                                  @RequestParam boolean isExport,
                                                  @RequestBody EnterpriseExportXlsxRequest enterpriseRisk,
                                                  @RequestParam(required = false) String outputFormat,
                                                  HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(enterpriseRisk.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);

        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("ENTERPRISE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            CompletableFuture<Void> future = viewerExportService.getEnterpriseDetailsAndExport(enterpriseRisk.getFilters(), "EnterpriseRisks",
                    enterpriseRisk.fieldMaps, enterpriseRisk.colors, job, outputFormat, enterpriseRisk.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        Document document = new Document();

        if ((enterpriseRisk.filters.getSearchWhereClause() == null || enterpriseRisk.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (enterpriseRisk.getFilters().getTemplateValue() == null || enterpriseRisk.getFilters().getTemplateValue().isBlank() || viewerExportService.getEnterPriseRiskCount(requestDate, enterpriseRisk.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getEnterPriseRisk(page, pageSize, sortBy, sortingOrder, sort, enterpriseRisk.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, enterpriseRisk.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/uam/groupcombportfolio")
    public ResponseEntity<?> getGrpCompPortfolio(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(defaultValue = "id") String sortBy,
                                                 @RequestParam(defaultValue = "asc") String sortingOrder,
                                                 @RequestParam boolean isExport,
                                                 @RequestBody(required = false) GroupCompPortfolioExport groupCompPortfolioExport,
                                                 @RequestParam(required = false) String outputFormat,
                                                 HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(groupCompPortfolioExport.filters.getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUP_COMBINED_PORTFOLIO_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getGroupCombinedPortfolio(groupCompPortfolioExport.getFilters(), "GroupCombinedPortfolio", groupCompPortfolioExport.fieldMaps, groupCompPortfolioExport.colors, job, outputFormat, groupCompPortfolioExport.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        Document document = new Document();
        if ((groupCompPortfolioExport.filters.getSearchWhereClause() == null || groupCompPortfolioExport.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (groupCompPortfolioExport.getFilters().getTemplateValue() == null || groupCompPortfolioExport.getFilters().getTemplateValue().isBlank() || viewerExportService.getGroupCombinedPortfolioList(requestDate, groupCompPortfolioExport.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getGrpCompPortfolio(page, pageSize, sortBy, sortingOrder, sort, groupCompPortfolioExport.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, groupCompPortfolioExport.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //audit body details display and export
    @PostMapping("/api/uam/audit-bdy")
    public ResponseEntity<?> getAuditBodyDetails(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam boolean isExport,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestBody(required = false) AuditBodyExport auditBodyExport,
            @RequestParam(required = false) String outputFormat
    ) throws Exception {

        LocalDate requestDate = LocalDate.parse(auditBodyExport.filters.getDateValue(), dateTimeFormatter);

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("AUDIT_BDY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getAuditBdyForExport(auditBodyExport.getFilters(),
                    "AUDIT_BDY_EXPORT", auditBodyExport.fieldMaps, auditBodyExport.colors, job, outputFormat, auditBodyExport.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        Document document = new Document();


        if ((auditBodyExport.filters.getSearchWhereClause() == null || auditBodyExport.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (viewerExportService.getAuditBodyList(requestDate) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getAuditBdyTmpl(page, pageSize, sortBy, sortingOrder, sort, auditBodyExport.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, auditBodyExport.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }


    @PostMapping("/api/uam/audit-hdr")
    public ResponseEntity<?> getAudit(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int pageSize,
                                      @RequestParam(defaultValue = "id") String sortBy,
                                      @RequestParam(defaultValue = "asc") String sortingOrder,
                                      @RequestParam boolean isExport,
                                      @RequestBody(required = false) AuditExportModel auditExportModel,
                                      @RequestParam(required = false) String outputFormat,
                                      HttpServletRequest request, Authentication authentication) throws Exception {

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        if(!sortBy.equalsIgnoreCase("auditId")) {
            sort = sort.ignoreCase();
        }
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("AUDIT_HDR_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getAuditForExport(auditExportModel.getFilters(),
                    "AUDIT_HDR_EXPORT", auditExportModel.fieldMaps, auditExportModel.colors, job, outputFormat, auditExportModel.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        Document document = new Document();

        try {
            document = viewerExportService.getAuditList(page, pageSize, sortBy, sortingOrder, sort, auditExportModel.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, auditExportModel.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);

    }


    @PostMapping("/api/uam/chineseWall")
    public ResponseEntity<?> getChineseWallSearch(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(defaultValue = "id") String sortBy,
                                                  @RequestParam(defaultValue = "asc") String sortingOrder,
                                                  @RequestParam boolean isExport,
                                                  @RequestParam(required = false) String outputFormat,
                                                  @RequestBody ChineseWallExportXlsxRequest chineseWallExportXlsxRequest,
                                                  HttpServletRequest request, Authentication authentication) throws Exception {

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();
        LocalDate requestDate = LocalDate.parse(chineseWallExportXlsxRequest.filters.getDateValue(), dateTimeFormatter);
        if (chineseWallExportXlsxRequest.filters.getDateValue() == null)
            return new ResponseEntity("No Date found.", HttpStatus.NOT_FOUND);

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CHINESEWALL_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getChineseWallDetailsAndExport(chineseWallExportXlsxRequest.getFilters(), "ChineaseWall",
                    chineseWallExportXlsxRequest.fieldMaps, chineseWallExportXlsxRequest.colors, job, outputFormat, chineseWallExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }
        Document document = new Document();
        if ((chineseWallExportXlsxRequest.filters.getSearchWhereClause() == null || chineseWallExportXlsxRequest.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (chineseWallExportXlsxRequest.getFilters().getTemplateValue() == null || chineseWallExportXlsxRequest.getFilters().getTemplateValue().isBlank() || viewerExportService.getChineseWallList(requestDate, chineseWallExportXlsxRequest.filters.getTemplateValue()) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getChineseWall(page, pageSize, sortBy, sortingOrder, sort, chineseWallExportXlsxRequest.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, chineseWallExportXlsxRequest.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/get/user-policy/export/{reportDate}")
    public ResponseEntity<?> exportUserPolicy(@PathVariable String reportDate,
                                              @RequestParam(required = false) String outputFormat,
                                              @RequestBody UserPolicyExport userpolicy,
                                              HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(reportDate, dateTimeFormatter);

        DownloadJob job = downloadJobService.createNewJob("USER_POLICY_EXPORT", authentication.getName());
        if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
            throw new Exception("Output format not supported!");
        }
        CompletableFuture<Void> future = viewerExportService.getUserPolicy(userpolicy.getPolicyName(), "USER_POLICY_EXPORT", userpolicy.getColor(), userpolicy.getFieldMaps(), job, outputFormat, userpolicy.getFieldColumns(), requestDate);
        // save audit for this action
        auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                null, null, null, authentication, null, null, null, null, null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @PostMapping("/api/uam/counterParty/Display")
    public ResponseEntity<?> getCounterPartyDisplay(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody CounterPartyDisplayModel counterParty,
            HttpServletRequest request,
            Authentication authentication

    ) throws Exception {

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        LocalDate requestDate = LocalDate.parse(counterParty.filters.getDateValue(), dateTimeFormatter);
        if (counterParty.filters.getDateValue() == null)
            return new ResponseEntity("No Date found.", HttpStatus.NOT_FOUND);

        Document document = new Document();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("COUNTERPARTY_DISPLAY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }

            CompletableFuture<Void> future = viewerExportService.getCounterpartyDisplayExportFile(counterParty.getFilters(), "Counterparty_Display",
                    counterParty.fieldMaps, counterParty.colors, job, outputFormat, counterParty.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);

        }
        if ((counterParty.filters.getSearchWhereClause() == null || counterParty.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (viewerExportService.getCounterpartyCount(requestDate) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getCounterPartyDisplay(page, pageSize, sortBy, sortingOrder, sort, counterParty.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, counterParty.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);

    }

    @PostMapping("/api/uam/counterParty/template")
    public ResponseEntity<?> getCounterPartyTemplate(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam Boolean isExport,
            @RequestParam(required = false) String outputFormat,
            @RequestBody CounterPartySplitModel counterParty,
            HttpServletRequest request,
            Authentication authentication

    ) throws Exception {
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        LocalDate requestDate = LocalDate.parse(counterParty.filters.getDateValue(), dateTimeFormatter);
        if (counterParty.filters.getDateValue() == null)
            return new ResponseEntity("No Date found.", HttpStatus.NOT_FOUND);

        Document document = new Document();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("COUNTERPARTY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getCounterpartyExport(counterParty.getFilters(), "Counterparty_Template",
                    counterParty.fieldMaps, counterParty.colors, job, outputFormat, counterParty.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);

        }
        if ((counterParty.filters.getSearchWhereClause() == null || counterParty.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (viewerExportService.getCounterpartyCountSplit(requestDate) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getCounterPartySplit(page, pageSize, sortBy, sortingOrder, sort, counterParty.filters, authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, counterParty.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);

    }

    @PostMapping("/api/uam/user-details")
    public ResponseEntity<?> getUserDetails(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int pageSize,
                                            @RequestParam(defaultValue = "id") String sortBy,
                                            @RequestParam(defaultValue = "asc") String sortingOrder,
                                            @RequestParam boolean isExport,
                                            @RequestBody UserFilter mxUserList,
                                            @RequestParam(required = false) String outputFormat,
                                            HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(mxUserList.getFilters().getDateValue(), dateTimeFormatter);
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("licenseCatName") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "jt.licenseCatName");
        } else if (sortBy.equalsIgnoreCase("licenseCatName") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "jt.licenseCatName");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("USERLIST_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getUserListExport(mxUserList.getFilters(), "User List",
                    mxUserList.fieldMaps, mxUserList.colors, job, outputFormat, mxUserList.fieldColumns, sortBy, sortingOrder, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);

        }

        Document document = new Document();
        if ((mxUserList.filters.getSearchWhereClause() == null || mxUserList.filters.getSearchWhereClause().isBlank()) && page == 0) {
            if (mxUserList.getFilters().getTemplateValue() == null || mxUserList.getFilters().getTemplateValue().isBlank() || viewerExportService.getUserDetailCount(requestDate) == null) {
                document.put("content", new ArrayList<>());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        try {
            document = viewerExportService.getUserDetails(page, pageSize, sortBy, sortingOrder, sort, mxUserList.getFilters(), authentication.getName(), false, 0);
        } catch (Exception ex) {
            document = viewerExportService.generalException(ex, mxUserList.getFilters().getSearchWhereClause(), document);
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get the filters for each report")
    @PostMapping("/api/uam/general/filters")
    public ResponseEntity<?> getFilterList(@RequestParam(defaultValue = "0", required = false) int page,
                                           @RequestParam(defaultValue = "20", required = false) int pageSize,
                                           @RequestParam String reportDate, @RequestBody GeneralFilters filters) {
        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        document = viewerExportService.getAllFilters(repDate, filters);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get the templates for each report")
    @PostMapping("/api/uam/general/template-list")
    public ResponseEntity<?> getTemplateListByReport(@RequestParam String reportDate, @RequestBody GeneralFilters reportType) {
        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        document = viewerExportService.getAllTemplateByReportType(repDate, reportType, document);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get the group for each report")
    @PostMapping("/api/uam/general/template/group-List")
    public ResponseEntity<?> getGroupListByTemplate(@RequestParam String reportDate, @RequestBody GeneralFilters reportType) {
        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        document = viewerExportService.getAllGroupsByTemplate(repDate, reportType, document);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //to get the details of group using template value
    @PostMapping("/api/uam/group-details/template")
    public ResponseEntity<?> getGroupDetailsUsingTemplate(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam(required = false) String outputFormat,
            @RequestParam Boolean isExport,
            @RequestBody GeneralFilters filters,
            Authentication authentication
    ) throws Exception {

        LocalDate repDate = LocalDate.parse(filters.getReportDate(), dateTimeFormatter);

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        Document document = new Document();


        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("DETAILS_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getGroupDetailsFromTemplateExport("Details", filters.fieldMaps, filters.color, job, outputFormat, repDate, filters.fieldColumns, sort,filters);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);

        } else {
            if (filters.getTemplate() == null || filters.getTemplate().isBlank()) {
                document.put("Message", "No Template Found.");
                return new ResponseEntity<>(document, HttpStatus.BAD_REQUEST);
            }
            List<?> listDetails = viewerExportService.getGroupValues( repDate ,filters);
            if (listDetails.size() == 0) {
                document.put("Message", "No Data Found.");
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document.put("data", listDetails);
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
    }

    //user details by group
    @PostMapping("/api/uam/user-details/group")
    public ResponseEntity<?> getUserDetailsUsingGroup(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam(required = false) String outputFormat,
            @RequestParam Boolean isExport,
            @RequestBody GeneralFilters filters,
            Authentication authentication
    ) throws Exception {

        LocalDate repDate = LocalDate.parse(filters.getReportDate(), dateTimeFormatter);

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        Document document = new Document();


        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("DETAILS_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getUserDetailsFromGroup("Details", filters.fieldMaps, filters.color, job, outputFormat, repDate, filters.fieldColumns,  sort, filters.getGroupLabel());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);

        } else {
            if (filters.getGroupLabel() == null || filters.getGroupLabel().isBlank()) {
                document.put("Message","No Group Found.");
                return new ResponseEntity<>(document, HttpStatus.BAD_REQUEST);
            }
            List<UserGroupDetails> listDetails = viewerExportService.getUserDetailsData(filters.getGroupLabel(), repDate, sort);
            if (listDetails.size() == 0) {
                document.put("Message", "No Data Found.");
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document.put("data", listDetails);
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
    }

    @PostMapping("/api/uam/stp-matrix")
    public ResponseEntity<?> getStpMatrixDetailsUsingRepDate(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam(required = false) String outputFormat,
            @RequestParam Boolean isExport,
            @RequestBody StpMatrixExportXlsxRequest filters,
            Authentication authentication
    ) throws Exception {

        Document document = new Document();

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();


//        LocalDate requestDate = viewerExportService.getStpMatrixLatestDate();
        LocalDate requestDate = LocalDate.parse(filters.getFilters().getDateValue(), dateTimeFormatter);


        DayOfWeek day = requestDate.getDayOfWeek();
        if(day != DayOfWeek.FRIDAY|| day != DayOfWeek.FRIDAY){
            document.put("content", new ArrayList<>());
            requestDate = viewerExportService.getStpMatrixLatestDate();
            document.put("latestDate",requestDate!=null?requestDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")):null);
            return new ResponseEntity<>(document, HttpStatus.OK);
        }

        log.info("requestDate:{}",requestDate);

        if(isExport){
            DownloadJob job = downloadJobService.createNewJob("STP_RIGHTS_MATRIX_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getStpMatrixExport("StpMatrixDetails", filters.fieldMaps, filters.colors, job, outputFormat, requestDate, filters.fieldColumns,sort,authentication.getName(),filters,sortingOrder,sortBy);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }else{
            if ((filters.filters.getSearchWhereClause() == null || filters.filters.getSearchWhereClause().isBlank()) && page == 0) {
                if (filters.getFilters().getTemplateValue() == null || filters.getFilters().getTemplateValue().isBlank() || viewerExportService.getStpRightsMatrix(requestDate, filters.filters.getTemplateValue()) == null) {
                    document.put("content", new ArrayList<>());
                    document.put("message","There is no data for this group.");
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }
            try {
                document = viewerExportService.getStpMatrixDetails(page, pageSize, sortBy, sortingOrder,requestDate,filters.filters.getTemplateValue(), sort, filters.filters, authentication.getName(), false, 0);
            } catch (Exception ex) {
                document = viewerExportService.generalException(ex, filters.getFilters().getSearchWhereClause(), document);
            }
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
    }

    @PostMapping("/api/uam/stp-src-module")
    public ResponseEntity<?> getStpSrcModuleDetailsUsingRepDate(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam(required = false) String outputFormat,
            @RequestParam Boolean isExport,
            @RequestBody StpMatrixExportXlsxRequest filters,
            Authentication authentication
    ) throws Exception {

        Document document = new Document();

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

//        LocalDate requestDate = viewerExportService.getStpMatrixLatestDate();
        LocalDate requestDate = LocalDate.parse(filters.getFilters().getDateValue(), dateTimeFormatter);


        if(requestDate==null) {
            return new ResponseEntity("No Date found.", HttpStatus.NOT_FOUND);
        }

        log.info("request Date:{}",requestDate);

        if(isExport){
            DownloadJob job = downloadJobService.createNewJob("STP_SRC_MODULE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getStpSrcModuleExport("StpSrcModuleDetails", filters.fieldMaps, filters.colors, job, outputFormat, requestDate, filters.fieldColumns,sort,authentication.getName(),filters,sortingOrder,sortBy);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }else{
            if ((filters.filters.getSearchWhereClause() == null || filters.filters.getSearchWhereClause().isBlank()) && page == 0) {
                if (filters.getFilters().getTemplateValue() == null || filters.getFilters().getTemplateValue().isBlank() || viewerExportService.getStpSrcModule(requestDate, filters.filters.getTemplateValue()) == null) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }
            try {
                document = viewerExportService.getStpSrcModuleDetails(page, pageSize, sortBy, sortingOrder,requestDate,filters.filters.getTemplateValue(), sort, filters.filters, authentication.getName(), false, 0);
            } catch (Exception ex) {
                document = viewerExportService.generalException(ex, filters.getFilters().getSearchWhereClause(), document);
            }
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
    }

    @PostMapping("/api/uam/stp-filter")
    public ResponseEntity<?> getStpRightsFilter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortingOrder,
            @RequestParam String boType,
            @RequestParam String boTemplate,
            @RequestBody StpMatrixExportXlsxRequest filters,
            Authentication authentication) {
        Document document = new Document();

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

//        LocalDate requestDate = viewerExportService.getStpMatrixLatestDate();
        LocalDate requestDate = LocalDate.parse(filters.getFilters().getDateValue(), dateTimeFormatter);
        log.info("Request Date : {}", requestDate);
        if (requestDate == null) {
            return new ResponseEntity<>("No Date found.", HttpStatus.NOT_FOUND);
        }
        document = viewerExportService.getStpRightsMatrixFilterDetails(page, pageSize, sortBy, sortingOrder, requestDate, boType, boTemplate, sort, filters.filters, authentication.getName(), false, 0);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //stp rights typology api
    @PostMapping("/api/uam/stp-rights-typology")
    public ResponseEntity<?> getStpRightsTypology(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(defaultValue = "id") String sortBy,
                                                  @RequestParam(defaultValue = "asc") String sortingOrder,
                                                  @RequestParam(required = false) String outputFormat,
                                                  @RequestParam Boolean isExport,
                                                  @RequestBody StpMatrixExportXlsxRequest filters,
                                                  Authentication authentication
    ) throws Exception {
        Document document = new Document();

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

//        LocalDate requestDate = viewerExportService.getStpMatrixLatestDate();
        LocalDate requestDate = LocalDate.parse(filters.getFilters().getDateValue(), dateTimeFormatter);

        if(requestDate==null) {
            return new ResponseEntity("No Date found.", HttpStatus.NOT_FOUND);
        }
        log.info("report date:{}",requestDate);
        if(isExport){
            DownloadJob job = downloadJobService.createNewJob("STP_RIGHTS_TYPOLOGY", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getStpRightsTypologyExport("StpRightsTypologyDetails", filters.fieldMaps, filters.colors, job, outputFormat, requestDate, filters.fieldColumns,sort,authentication.getName(),filters,sortingOrder,sortBy);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }else{
            if ((filters.filters.getSearchWhereClause() == null || filters.filters.getSearchWhereClause().isBlank()) && page == 0) {
                if (filters.getFilters().getTemplateValue() == null || filters.getFilters().getTemplateValue().isBlank() || viewerExportService.getStpRightsTypology(requestDate, filters.filters.getTemplateValue()) == null) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }
            try {
                document = viewerExportService.getStpRightsTypologyDetails(page, pageSize, sortBy, sortingOrder,requestDate,filters.filters.getTemplateValue(), sort, filters.filters, authentication.getName(), false, 0);
            } catch (Exception ex) {
                document = viewerExportService.generalException(ex, filters.getFilters().getSearchWhereClause(), document);
            }
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
    }

    @GetMapping("/api/uam/stp/get-latest-date")
    public String getStpLatestDate() {
        return viewerExportService.getStpLatestDate();
    }

    @PostMapping("/api/uam/stp/get-groupLabels-rights-profile")
    public ResponseEntity<?> getStpRightsProfile(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                 @RequestParam(defaultValue = "stpRgtTmpl") String sortBy,
                                                 @RequestParam(defaultValue = "asc") String sortingOrder,
                                                 @RequestParam(required = false) String outputFormat,
                                                 @RequestParam Boolean isExport,
                                                 @RequestBody StpMatrixExportXlsxRequest filters,
                                                 Authentication authentication) throws Exception {
        Document document = new Document();

        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

//        LocalDate requestDate = viewerExportService.getStpMatrixLatestDate();
        LocalDate requestDate = LocalDate.parse(filters.getFilters().getDateValue(), dateTimeFormatter);

        if(requestDate==null) {
            return new ResponseEntity("No Date found.", HttpStatus.NOT_FOUND);
        }

        if(isExport){
            DownloadJob job = downloadJobService.createNewJob("GROUP_DETAILS_RIGHTS_PROFILE", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx"))) {
                throw new Exception("Output format not supported!");
            }
            CompletableFuture<Void> future = viewerExportService.getStpRightsProfileExport("StpRightsProfileGroupDetails", filters.fieldMaps, filters.colors, job, outputFormat, requestDate, filters.fieldColumns,sort,authentication.getName(),filters,sortingOrder,sortBy);
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }else{
            if ((filters.filters.getSearchWhereClause() == null || filters.filters.getSearchWhereClause().isBlank()) && page == 0) {
                if (filters.getFilters().getTemplateValue() == null || filters.getFilters().getTemplateValue().isBlank() || viewerExportService.getStpRightsProfile(requestDate, filters.filters.getTemplateValue()) == null) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }
            try {
                document = viewerExportService.getGroupDetailsFromStpRightsProfile(page, pageSize, sortBy, sortingOrder,requestDate,filters.filters.getTemplateValue(), sort, filters.filters, authentication.getName(), false, 0);
            } catch (Exception ex) {
                document = viewerExportService.generalException(ex, filters.getFilters().getSearchWhereClause(), document);
            }
            return new ResponseEntity<>(document, HttpStatus.OK);
        }

    }
    @PostMapping("api/uam/stp-processing-details")
    public ResponseEntity<?> getStpProcessingDetails(
            @RequestBody StpMatrixExportXlsxRequest filters
    ) {
//        LocalDate requestDate = viewerExportService.getStpMatrixLatestDate();
        LocalDate requestDate = LocalDate.parse(filters.getFilters().getDateValue(), dateTimeFormatter);
        log.info("request Date:{}",requestDate);
        List<StpSourceModule> processingTemplateDetails = viewerExportService.getProcessingDetails(filters.getFilters().getTemplateValue(), requestDate);
        return new ResponseEntity<>(processingTemplateDetails, HttpStatus.OK);
    }
}

