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
import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.service.CombinedGroupsService;
import com.finsurge.tmr_portal.mx_superview.service.SnapshotDataService;
import com.finsurge.tmr_portal.mx_superview.service.ViewerExportService;
import org.bson.Document;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import java.time.DayOfWeek;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
public class CombinedGroupsController {

    //logger
    private final Logger log = LoggerFactory.getLogger(CombinedGroupsController.class);

    //accessing classes
    private final DownloadJobService downloadJobService;
    private final UserPreferenceRepository userPreferenceRepository;
    private final CombinedGroupsService combinedGroupsService;
    private final ViewerExportService viewerExportService;
    private final DateTimeFormatter dateTimeFormatter;
    private final AuditUtils auditUtils;


    //Constructor parameter
    public CombinedGroupsController( DownloadJobService downloadJobService, UserPreferenceRepository userPreferenceRepository, CombinedGroupsService combinedGroupsService, ViewerExportService viewerExportService, AuditUtils auditUtils) {
        this.downloadJobService = downloadJobService;
        this.userPreferenceRepository = userPreferenceRepository;
        this.combinedGroupsService = combinedGroupsService;
        this.viewerExportService = viewerExportService;
        this.auditUtils = auditUtils;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
    }


    //Combined Group List
    @PostMapping("/api/get/combined/groupList/{date}")
    public ResponseEntity<?> getGroupListCombined(@PathVariable String date,
                                                  @RequestParam boolean isExport,
                                                  @RequestBody(required = false) GroupListExportXlsxRequest groupListExportXlsxRequest,
                                                  @RequestParam boolean isAllGroups,
                                                  @RequestParam(required = false) String outputFormat,
                                                  @RequestParam(defaultValue = "") String groupLabel,
                                                  HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(date, dateTimeFormatter);
        groupLabel = groupLabel == null || groupLabel.isBlank() ? null : groupLabel;

        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("GROUPLIST_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
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
                        log.info("group name:{}",groupNames);
                        if (userGroup != null && !userGroup.isBlank()) {
                            GroupList groupRole=viewerExportService.getUserPreference(userGroup,requestDate);
                            if (groupNames.contains(groupRole)) {
                                groupNames.remove(groupRole);
                                log.info("userPreference:{}",groupRole);
                                groupNames.add(0,groupRole);
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

    //Combined group for portfolio
    @PostMapping("/api/uam/combined/portfolio")
    public ResponseEntity<?> getCombinedGroupPortfolioRights(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize,
                                                             @RequestParam(defaultValue = "id") String sortBy,
                                                             @RequestParam(defaultValue = "asc") String sortingOrder,
                                                             @RequestParam(defaultValue = "false") Boolean allGroups,
                                                             @RequestParam boolean isExport,
                                                             @RequestBody(required = false) CombinedGroupPortfolioXlsxRequest portfolioRights,
                                                             @RequestParam(required = false) String outputFormat,
                                                             HttpServletRequest request, Authentication authentication) throws Exception {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(portfolioRights.filters.getDateValue(), dateTimeFormatter);

        //for sorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("PORTFOLIO_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for portfolio export
            CompletableFuture<Void> future = combinedGroupsService.getCombinedPortfolioDetailsAndExport(portfolioRights.getFilters(), "PortfolioRights", portfolioRights.fieldMaps, portfolioRights.color, job, outputFormat, portfolioRights.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (portfolioRights.getFilters().getTemplateValue() == null || portfolioRights.getFilters().getTemplateValue().isEmpty()) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxGroupPortfolioRights> portfolioList = combinedGroupsService.getPortfolioRightsList(requestDate, portfolioRights.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (portfolioList == null || portfolioList.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getCombinedPortfolioRights(page, pageSize, sortBy, sortingOrder, sort, portfolioRights.filters,
                    allGroups, authentication.getName(),false,0);
            log.info("query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, portfolioRights.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //combined chinese wall
    @PostMapping("/api/uam/combined/chineseWall")
    public ResponseEntity<?> getChineseWallSearch(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(defaultValue = "id") String sortBy,
                                                  @RequestParam(defaultValue = "asc") String sortingOrder,
                                                  @RequestParam(defaultValue = "false") Boolean allGroups,
                                                  @RequestParam boolean isExport,
                                                  @RequestParam(required = false) String outputFormat,
                                                  @RequestBody(required = false) CombinedChineseWallExportXlsxRequest chineseWallExportXlsxRequest,
                                                  HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(chineseWallExportXlsxRequest.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        if (allGroups || chineseWallExportXlsxRequest.filters.getGroupValue().size() != 0 && chineseWallExportXlsxRequest.filters.getGroupValue().size() > 5) {
            document.put("message", "Please select only five groups.");
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }

        //for shorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CHINESEWALL_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for chinese wall export
            // CompletableFuture<Void> future = combinedGroupsService.getChineseWallDetailsAndExport(chineseWallExportXlsxRequest.getFilters(), "ChineaseWall", chineseWallExportXlsxRequest.fieldMaps, chineseWallExportXlsxRequest.color, job, outputFormat, chineseWallExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            CompletableFuture<Void> futures = combinedGroupsService.getChineseWallDetailsAndExports(chineseWallExportXlsxRequest.getFilters(), "ChineaseWall", chineseWallExportXlsxRequest.fieldMaps, chineseWallExportXlsxRequest.color, job, outputFormat, chineseWallExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());

            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (chineseWallExportXlsxRequest.filters.getTemplateValue() == null || chineseWallExportXlsxRequest.filters.getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxChineseWallTmpl> chineseWall = combinedGroupsService.getChineseWallList(requestDate, chineseWallExportXlsxRequest.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (chineseWall == null || chineseWall.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getChineseWall(page, pageSize, sortBy, sortingOrder, sort, allGroups, chineseWallExportXlsxRequest.filters, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, chineseWallExportXlsxRequest.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }
    //consistency template
    @PostMapping("/api/uam/combined/consistency-template")
    public ResponseEntity<?> getCombinedGroupConsistencyTemplateRights(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                                       @RequestParam(defaultValue = "id") String sortBy,
                                                                       @RequestParam(defaultValue = "asc") String sortingOrder,
                                                                       @RequestParam(defaultValue = "false") Boolean allGroups,
                                                                       @RequestParam boolean isExport,
                                                                       @RequestBody(required = false) CombinedGroupConsistencyExportXlsxRequest consistencyExportXlsxRequest,
                                                                       @RequestParam(required = false) String outputFormat,
                                                                       HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(consistencyExportXlsxRequest.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for sorting elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort=null;
        Sort.Order sort1=null;
        Sort.Order sort2=null;
        Sort.Order sort3=null;
        Sort.Order sort4=null;
        List<Sort.Order> orders=new ArrayList<>();
        if((sortBy.toLowerCase().endsWith("right")) || (sortBy.equalsIgnoreCase("consistencyTmpl"))) {
            sort = Sort.Order.asc(sortBy);
            sort1 = Sort.Order.asc("category");
            sort2 = Sort.Order.asc("item");
            sort4 = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
                sort1 = Sort.Order.desc("category");
                sort2 = Sort.Order.desc("item");
                sort4 = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
            }
            orders.add(sort.ignoreCase());
            orders.add(sort1.ignoreCase());
            orders.add(sort2.ignoreCase());
            orders.add(sort4.ignoreCase());
            if (!sortBy.equalsIgnoreCase("consistencyTmpl") && sortingOrder.equalsIgnoreCase("asc")) {
                sort3 = Sort.Order.asc("consistencyTmpl");
                orders.add(sort3.ignoreCase());
            }
            else if (!sortBy.equalsIgnoreCase("consistencyTmpl") && sortingOrder.equalsIgnoreCase("desc")) {
                sort3 = Sort.Order.desc("consistencyTmpl");
                orders.add(sort3.ignoreCase());
            }
        }
        else if ( sortBy.equalsIgnoreCase("groupLabel")) {
            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
            sort1 = Sort.Order.asc("category");
            sort2 = Sort.Order.asc("item");
            sort3 = Sort.Order.asc("consistencyTmpl");
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
                sort1 = Sort.Order.desc("category");
                sort2 = Sort.Order.desc("item");
                sort3 = Sort.Order.desc("consistencyTmpl");
            }
            orders.add(sort.ignoreCase());
            orders.add(sort1.ignoreCase());
            orders.add(sort2.ignoreCase());
            orders.add(sort3.ignoreCase());
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
           // sort = sort.ignoreCase();
            orders.add(sort.ignoreCase());
        }

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CONSISTENCY_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for consistency export
            CompletableFuture<Void> future = combinedGroupsService.getConsistencyExport(consistencyExportXlsxRequest.getFilters(), "Consistency", consistencyExportXlsxRequest.fieldMaps, consistencyExportXlsxRequest.color, job, outputFormat, consistencyExportXlsxRequest.fieldColumns, sortBy, sortingOrder, orders, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (consistencyExportXlsxRequest.getFilters().getTemplateValue() == null || consistencyExportXlsxRequest.getFilters().getTemplateValue().isEmpty() || consistencyExportXlsxRequest.getFilters().getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxConsistencyTmpl> consistency = combinedGroupsService.getConsistencyCount(requestDate, consistencyExportXlsxRequest.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (consistency == null || consistency.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and Template Name
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getConsistency(page, pageSize, sortBy, sortingOrder, orders, consistencyExportXlsxRequest.filters, allGroups, authentication.getName(),false,0);
            log.info("count started:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, consistencyExportXlsxRequest.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //combined Navigation template
    @PostMapping("/api/uam/combine/navigation")
    public ResponseEntity<?> getGroupNavRights(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(defaultValue = "id") String sortBy,
                                               @RequestParam(defaultValue = "asc") String sortingOrder,
                                               @RequestParam(defaultValue = "false") Boolean allGroups,
                                               @RequestParam boolean isExport,
                                               @RequestBody(required = false) CombinedNavigationExportXlsxRequest navigationRights,
                                               @RequestParam(required = false) String outputFormat,
                                               HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(navigationRights.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for sorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("NAVIGATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for navigation export
            // CompletableFuture<Void> future = combinedGroupsService.getNavigationRightsAndExport(navigationRights.getFilters(), "NavigationRights", navigationRights.fieldMaps, navigationRights.color, job, outputFormat, navigationRights.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            CompletableFuture<Void> futures = combinedGroupsService.getNavigationRightsAndExports(navigationRights.getFilters(), "NavigationRights", navigationRights.fieldMaps, navigationRights.color, job, outputFormat, navigationRights.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (navigationRights.getFilters().getTemplateValue() == null || navigationRights.getFilters().getTemplateValue().isEmpty() || navigationRights.getFilters().getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxGroupNavigationRight> navigationRight = combinedGroupsService.getNavigationList(requestDate, navigationRights.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (navigationRight == null || navigationRight.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getNavigationRights(page, pageSize, sortBy, sortingOrder, sort, navigationRights.filters, allGroups, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, navigationRights.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }
    //combined operation Rights
    @PostMapping("/api/uam/combined/operationRight")
    public ResponseEntity<?> getCombinedOperationRights(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(defaultValue = "id") String sortBy,
                                                        @RequestParam(defaultValue = "asc") String sortingOrder,
                                                        @RequestParam(defaultValue = "false") Boolean allGroups,
                                                        @RequestParam boolean isExport,
                                                        @RequestBody(required = false) CombinedOperationExportXlsx operationExportXlsx,
                                                        @RequestParam(required = false) String outputFormat,
                                                        HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(operationExportXlsx.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for sorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("OPERATION_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for operation rights export
            CompletableFuture<Void> future = combinedGroupsService.getOperationalExport(operationExportXlsx.filters, "OPERATION RIGHT", operationExportXlsx.fieldMaps, operationExportXlsx.color, job, outputFormat, operationExportXlsx.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (operationExportXlsx.filters.getTemplateValue() == null || operationExportXlsx.filters.getTemplateValue().isEmpty()) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxOperationRights> operationRights = combinedGroupsService.getOperationRightsList(requestDate, operationExportXlsx.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (operationRights == null || operationRights.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template and sub template value
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getOperationalRights(page, pageSize, sortBy, sortingOrder, sort, operationExportXlsx.filters, allGroups, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, operationExportXlsx.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //Combined groups configuration management
    @PostMapping("/api/uam/combined/configurationManagementRights")
    public ResponseEntity<?> getCombinedConfigurationManagementRights(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                                      @RequestParam(defaultValue = "id") String sortBy,
                                                                      @RequestParam(defaultValue = "asc") String sortingOrder,
                                                                      @RequestParam(defaultValue = "false") Boolean allGroups,
                                                                      @RequestParam boolean isExport,
                                                                      @RequestBody(required = false) CombinedConfigurationManagementExportXlsxRequest combinedConfigurationManagementExportXlsxRequest,
                                                                      @RequestParam(required = false) String outputFormat,
                                                                      HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(combinedConfigurationManagementExportXlsxRequest.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for sorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("CONFIGURATION_MANAGEMENT_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for configuration management rights export
            CompletableFuture<Void> future = combinedGroupsService.getCwtConfigMgtRightsAndExport(combinedConfigurationManagementExportXlsxRequest.getFilters(), "GroupCombinedPortfolio", combinedConfigurationManagementExportXlsxRequest.fieldMaps, combinedConfigurationManagementExportXlsxRequest.color, job, outputFormat, combinedConfigurationManagementExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (combinedConfigurationManagementExportXlsxRequest.getFilters().getTemplateValue() == null || combinedConfigurationManagementExportXlsxRequest.getFilters().getTemplateValue().isEmpty() || combinedConfigurationManagementExportXlsxRequest.getFilters().getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxCwtConfigMgtRight> configurationManagementList = combinedGroupsService.getConfigurationManagementList(requestDate, combinedConfigurationManagementExportXlsxRequest.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (configurationManagementList == null || configurationManagementList.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getCombinedConfigurationManagement(page, pageSize, sortBy, sortingOrder, sort, combinedConfigurationManagementExportXlsxRequest.filters, allGroups, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, combinedConfigurationManagementExportXlsxRequest.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //combined Groups Osp Rights
    @PostMapping("/api/uam/combined/osprights")
    public ResponseEntity<?> getCombinedOSPRights(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(defaultValue = "id") String sortBy,
                                                  @RequestParam(defaultValue = "asc") String sortingOrder,
                                                  @RequestParam(defaultValue = "false") Boolean allGroups,
                                                  @RequestParam boolean isExport,
                                                  @RequestParam(required = false) String outputFormat,
                                                  @RequestBody(required = false) CombinedOspRightsExportXlxsRequest ospRightsExportXlsxRequest,
                                                  HttpServletRequest request, Authentication authentication) throws Exception {

        LocalDate requestDate = LocalDate.parse(ospRightsExportXlsxRequest.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for sorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("OSP_RIGHTS_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for OSP rights matrix export
            CompletableFuture<Void> future = combinedGroupsService.getOspRightsDetailsAndExport(ospRightsExportXlsxRequest.getFilters(), "ospRightsTemplate", ospRightsExportXlsxRequest.fieldMaps, ospRightsExportXlsxRequest.color, job, outputFormat, ospRightsExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (ospRightsExportXlsxRequest.filters.getTemplateValue() == null || ospRightsExportXlsxRequest.filters.getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxOspRightsMatrix> ospRightsList = combinedGroupsService.getOspRightsList(requestDate, ospRightsExportXlsxRequest.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (ospRightsList == null || ospRightsList.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template
            //for testing purpose

                log.info("Query started for existing code:{}", LocalDateTime.now());
                document = combinedGroupsService.getOspMatrix(page, pageSize, sortBy, sortingOrder, ospRightsExportXlsxRequest.filters, allGroups, sort, authentication.getName(),false,0);
                log.info("Query ended for existing code:{}", LocalDateTime.now());

        } catch (Exception ex) {
            document = exceptionResponse(ex, ospRightsExportXlsxRequest.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //Combined group Finance
    @PostMapping("/api/uam/combined/financeRight")
    public ResponseEntity<?> getCombinedFinanceRights(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(defaultValue = "id") String sortBy,
                                                      @RequestParam(defaultValue = "asc") String sortingOrder,
                                                      @RequestParam(defaultValue = "false") Boolean allGroups,
                                                      @RequestParam boolean isExport,
                                                      @RequestBody(required = false) CombinedFinanceRightsExportXlsxRequest financeExportXlsx,
                                                      @RequestParam(required = false) String outputFormat,
                                                      HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(financeExportXlsx.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for shorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("FINANCE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for finance rights export
            CompletableFuture<Void> future = combinedGroupsService.getFinanceExport(financeExportXlsx.filters, "OPERATION RIGHT", financeExportXlsx.fieldMaps, financeExportXlsx.color, job, outputFormat, financeExportXlsx.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (financeExportXlsx.filters.getTemplateValue() == null || financeExportXlsx.filters.getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxFinaceAcctrlRights> financeRights = combinedGroupsService.getFinanceRightsList(requestDate, financeExportXlsx.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (financeRights == null || financeRights.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getFinanceRights(page, pageSize, sortBy, sortingOrder, sort, financeExportXlsx.filters, allGroups, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, financeExportXlsx.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //combined enterprise risk
    @PostMapping("/api/uam/combined/enterprise")
    public ResponseEntity<?> getCombinedGroupEnterprise(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(defaultValue = "id") String sortBy,
                                                        @RequestParam(defaultValue = "asc") String sortingOrder,
                                                        @RequestParam(defaultValue = "false") Boolean allGroups,
                                                        @RequestParam boolean isExport,
                                                        @RequestBody(required = false) CombinedEnterpriseXlsxRequest enterpriseRisk,
                                                        @RequestParam(required = false) String outputFormat,
                                                        HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(enterpriseRisk.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for shorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("ENTERPRISE_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for enterprise export
            CompletableFuture<Void> future = combinedGroupsService.getCombinedEnterpriseDetailsAndExport(enterpriseRisk.getFilters(), "enterpriseRisk", enterpriseRisk.fieldMaps, enterpriseRisk.color, job, outputFormat, enterpriseRisk.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //if only specific groups are selected.
            if (!allGroups) {
                if (enterpriseRisk.getFilters().getTemplateValue() == null || enterpriseRisk.getFilters().getTemplateValue().isEmpty()) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxEnterpriseRisk> hasRecord = combinedGroupsService.getEnterpriseList(requestDate, enterpriseRisk.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (hasRecord == null || hasRecord.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getCombinedEnterprise(page, pageSize, sortBy, sortingOrder, sort,
                    enterpriseRisk.filters, allGroups, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, enterpriseRisk.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //Combined groups groupcombined portfolio
    @PostMapping("/api/uam/combined/groupcombportfolio")
    public ResponseEntity<?> getCombinedGrpCompPortfolio(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(defaultValue = "id") String sortBy,
                                                         @RequestParam(defaultValue = "asc") String sortingOrder,
                                                         @RequestParam(defaultValue = "false") Boolean allGroups,
                                                         @RequestParam boolean isExport,
                                                         @RequestBody(required = false) CombinedGroupCombinedPortfolioExportXlsxRequest groupCompPortfolioExport,
                                                         @RequestParam(required = false) String outputFormat,
                                                         HttpServletRequest request, Authentication authentication) throws Exception {
        LocalDate requestDate = LocalDate.parse(groupCompPortfolioExport.filters.getDateValue(), dateTimeFormatter);
        Document document = new Document();

        //for shorting the elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort = Sort.Order.asc(sortBy);
        if (sortingOrder.equalsIgnoreCase("desc")) {
            sort = Sort.Order.desc(sortBy);
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("COMBINED_PORTFOLIO_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for operation rights export
            CompletableFuture<Void> future = combinedGroupsService.getGroupCombinedPortfolio(groupCompPortfolioExport.getFilters(), "GroupCombinedPortfolio", groupCompPortfolioExport.fieldMaps, groupCompPortfolioExport.color, job, outputFormat, groupCompPortfolioExport.fieldColumns, sortBy, sortingOrder, sort, allGroups, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (groupCompPortfolioExport.getFilters().getTemplateValue() == null || groupCompPortfolioExport.getFilters().getTemplateValue().isEmpty() || groupCompPortfolioExport.getFilters().getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxGroupCombinedPortfolio> combinedPortfolio = combinedGroupsService.getGroupCombinedPortfolioList(requestDate, groupCompPortfolioExport.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (combinedPortfolio == null || combinedPortfolio.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel and template
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getGrpCompPortfolio(page, pageSize, sortBy, sortingOrder, sort, groupCompPortfolioExport.filters, allGroups, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, groupCompPortfolioExport.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    //combine groups user details
    @PostMapping("/api/uam/combine/user-details")
    public ResponseEntity<?> getCombinedUserDetails(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam(defaultValue = "id") String sortBy,
                                                    @RequestParam(defaultValue = "asc") String sortingOrder,
                                                    @RequestParam(defaultValue = "false") Boolean allGroups,
                                                    @RequestParam boolean isExport,
                                                    @RequestBody CombinedUserList mxUserList,
                                                    @RequestParam(required = false) String outputFormat,
                                                    HttpServletRequest request, Authentication authentication) throws Exception {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(mxUserList.getFilters().getDateValue(), dateTimeFormatter);

        //for sorting elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "l.groupLabel");
        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "l.groupLabel");
        } else if (sortBy.equalsIgnoreCase("licenseCatName") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "m.licenseCatName");
        } else if (sortBy.equalsIgnoreCase("licenseCatName") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "m.licenseCatName");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("USERLIST_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls")
                    || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for user details export
            CompletableFuture<Void> future = combinedGroupsService.getUserListExport(mxUserList.getFilters(), "User List",
                    mxUserList.fieldMaps, mxUserList.colors, job, outputFormat, mxUserList.fieldColumns, sortBy, sortingOrder, allGroups, sort, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);

        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (mxUserList.getFilters().getTemplateValue() == null || mxUserList.getFilters().getTemplateValue().isEmpty() || mxUserList.getFilters().getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxUserListItem> userList = combinedGroupsService.getUserDetailCount(requestDate, mxUserList.getFilters().getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (userList == null || userList.size() == 0) {
                    document.put("content", new ArrayList<>());
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }

            //to retrive the Data for the given group using report Date and groupLabel
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getUserDetails(page, pageSize, sortBy, sortingOrder, mxUserList.getFilters(), allGroups, sort, authentication.getName(),false,0);
            log.info("Query ended:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxUserList.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }
    //combined Stp rights

    @PostMapping("/api/uam/combined/stprights")
    public ResponseEntity<?> getCombinedGroupStpRights(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(defaultValue = "id") String sortBy,
                                                       @RequestParam(defaultValue = "asc") String sortingOrder,
                                                       @RequestParam(defaultValue = "false") Boolean allGroups,
                                                       @RequestParam boolean isExport,
                                                       @RequestBody(required = false) CombinedStpRightsExportXlsxRequest stpRightsExportXlsxRequest,
                                                       @RequestParam(required = false) String outputFormat,
                                                       HttpServletRequest request, Authentication authentication) throws Exception {

        //LocalDate requestDate = combinedGroupsService.getLatestDates();
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(stpRightsExportXlsxRequest.getFilters().getDateValue(), dateTimeFormatter);


        DayOfWeek day = requestDate.getDayOfWeek();
        if(day != DayOfWeek.FRIDAY|| day != DayOfWeek.FRIDAY){
            document.put("content", new ArrayList<>());
            requestDate = viewerExportService.getStpMatrixLatestDate();
            document.put("latestDate",requestDate!=null?requestDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")):null);
            return new ResponseEntity<>(document, HttpStatus.OK);
        }

        log.info("requestDate:{}",requestDate);

        if (allGroups || stpRightsExportXlsxRequest.filters.getGroupValue().size() != 0 && stpRightsExportXlsxRequest.filters.getGroupValue().size() > 5) {
            document.put("message", "Please select only five groups.");
            document.put("content", new ArrayList<>());
            return new ResponseEntity<>(document, HttpStatus.OK);
        }
        //for sorting elements
        sortBy = Utils.processRequestParam(sortBy);
        sortingOrder = Utils.processRequestParam(sortingOrder);
        Sort.Order sort;
        if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("asc")) {
            sort = new Sort.Order(Sort.Direction.ASC, "g.groupLabel");
        } else if (sortBy.equalsIgnoreCase("groupLabel") && sortingOrder.equalsIgnoreCase("desc")) {
            sort = new Sort.Order(Sort.Direction.DESC, "g.groupLabel");
        } else {
            sort = Sort.Order.asc(sortBy);
            if (sortingOrder.equalsIgnoreCase("desc")) {
                sort = Sort.Order.desc(sortBy);
            }
        }
        sort = sort.ignoreCase();

        //to export the Data in csv,Xlxs,xls,xlsb format
        if (isExport) {
            DownloadJob job = downloadJobService.createNewJob("STP_EXPORT", authentication.getName());
            if (!(outputFormat.equalsIgnoreCase(".csv") || outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb"))) {
                throw new Exception("Output format not supported!");
            }
            //downloader job for consistency export
            CompletableFuture<Void> future = combinedGroupsService.getStpRightsExport(stpRightsExportXlsxRequest.getFilters(), "Stprights", stpRightsExportXlsxRequest.fieldMaps, stpRightsExportXlsxRequest.color, job, outputFormat, stpRightsExportXlsxRequest.fieldColumns, sortBy, sortingOrder, sort, allGroups, requestDate, authentication.getName());
            // save audit for this action
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_VIEWER, AuditAction.DOWNLOAD, null, null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>(job, HttpStatus.OK);
        }

        try {
            //to check whether the selected groups contains Data for the given Date
            if (!allGroups) {
                if (stpRightsExportXlsxRequest.getFilters().getTemplateValue() == null || stpRightsExportXlsxRequest.getFilters().getTemplateValue().isEmpty() || stpRightsExportXlsxRequest.getFilters().getTemplateValue().size() == 0) {
                    document.put("content", new ArrayList<>());
                    document.put("message","There is no data for this group.");
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                log.info("count started:{}", LocalDateTime.now());
                List<MxStpRightMatrixEod> stprights = combinedGroupsService.getStpRightsCount(requestDate, stpRightsExportXlsxRequest.filters.getTemplateValue());
                log.info("count ended:{}", LocalDateTime.now());
                if (stprights == null || stprights.size() == 0) {
                    document.put("content", new ArrayList<>());
                    document.put("message","There is no data for this group.");
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
            }
            //to retrive the Data for the given group using report Date and groupLabel and Template Name
            log.info("Query started:{}", LocalDateTime.now());
            document = combinedGroupsService.getStpRights(page, pageSize, sortBy, sortingOrder, sort, stpRightsExportXlsxRequest.filters, allGroups, requestDate, authentication.getName(),false,0);
            log.info("count started:{}", LocalDateTime.now());
        } catch (Exception ex) {
            document = exceptionResponse(ex, stpRightsExportXlsxRequest.getFilters().getSearchWhereClause());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    public Document exceptionResponse(Exception ex, String searchValue) {
        Document document = new Document();
        ex.printStackTrace();
        log.info("EXCEPTION while processing the request: {}", ex.getMessage());
        document.put("content", Collections.emptyList());
        if (searchValue != null && !searchValue.isBlank()) {
            document.put("message", "Invalid Query.");
        } else {
            document.put("message", "There is a problem in processing your request.");
        }
        return document;
    }


}