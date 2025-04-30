package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.models.MxGroupCompChinese;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.service.GroupCompareService;
import com.finsurge.tmr_portal.mx_superview.service.ViewerExportService;
import io.swagger.annotations.ApiOperation;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;

@RestController
@CrossOrigin("*")
public class GroupCompareController {
    private final GroupCompareService groupCompareService;
    private final AuditUtils auditUtils;
    private final ViewerExportService viewerExportService;
    private final Logger log=LoggerFactory.getLogger(GroupCompareController.class);

    public GroupCompareController(GroupCompareService groupCompareService, AuditUtils auditUtils, ViewerExportService viewerExportService) {
        this.groupCompareService = groupCompareService;
        this.auditUtils = auditUtils;
        this.viewerExportService = viewerExportService;
    }


    @ApiOperation("get group wise data Comparison for Osp")
    @PostMapping("/api/uam/groups-compare/osp")
    public ResponseEntity<?> getGroupCompareOsp(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(defaultValue = "false") boolean isMatched,
                                                @RequestParam(defaultValue = "all",required = false) String type,
                                                @RequestParam(defaultValue = "false") boolean isAdditionalMissing,
                                                @RequestParam String reportDate, @RequestBody MxGroupCompareOsp mxCompareGroupOsp, Authentication authentication) {
        Document document = new Document();
        try {
            boolean isCompare = mxCompareGroupOsp.getMxCompareGroup().template.equalsIgnoreCase("null") || mxCompareGroupOsp.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null");
            document = groupCompareService.identicalCondition(mxCompareGroupOsp.getMxCompareGroup(), document, isCompare, isMatched);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareOsp(reportDate, mxCompareGroupOsp.getMxCompareGroup(), mxCompareGroupOsp.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxCompareGroupOsp.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }


    @ApiOperation("get group wise data Comparison for Configuration")
    @PostMapping("/api/uam/groups-compare/configuration")
    public ResponseEntity<?> getGroupCompareConfigurationManagement(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                                    @RequestParam String reportDate, @RequestParam(defaultValue = "all",required = false) String type,
                                                                    @RequestBody MxCompareGroup mxCompareGroup, Authentication authentication) {
        Document document = new Document();
        try {

            document = groupCompareService.identicalCondition(mxCompareGroup, document, false, false);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareConfig(reportDate, mxCompareGroup, page, pageSize, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxCompareGroup.getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison for Enterprise")
    @PostMapping("/api/uam/groups-compare/enterprise")
    public ResponseEntity<?> getGroupCompareEnterprise(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam String reportDate,@RequestParam(defaultValue = "all",required = false) String type,
                                                       @RequestBody MxCompareGroup mxCompareGroup, Authentication authentication) {
        Document document = new Document();

        try {
            document=groupCompareService.identicalCondition(mxCompareGroup,document,false,false);
            if(document!=null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareEnterprise(reportDate, mxCompareGroup, page, pageSize, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxCompareGroup.getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison for portfolio")
    @PostMapping("/api/uam/groups-compare/portfolio")
    public ResponseEntity<?> groupComparePortfolioRights(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int pageSize,
                                                         @RequestParam(defaultValue = "false") boolean isMatched,
                                                         @RequestParam(defaultValue = "false") boolean isAdditionalMissing, Authentication authentication,
                                                         @RequestParam(defaultValue = "all",required = false) String type,
                                                         @RequestParam String reportDate, @RequestBody MxGroupComparePortfolio mxCompareGroupPortfolio) {

        Document document = new Document();
        try {
            document = groupCompareService.identicalCondition(mxCompareGroupPortfolio.getMxCompareGroup(), document, mxCompareGroupPortfolio.getMxCompareGroup().template.equalsIgnoreCase("null") ||
                    mxCompareGroupPortfolio.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null"), isMatched);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.groupComparePortfolio(reportDate, mxCompareGroupPortfolio.getMxCompareGroup(), mxCompareGroupPortfolio.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxCompareGroupPortfolio.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get group wise data for Navigation")
    @PostMapping("/api/uam/groups-compare/navigation")
    public ResponseEntity<?> getGroupCompareNavigation(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(defaultValue = "false") boolean isMatched,
                                                       @RequestParam(defaultValue = "false") boolean isAdditionalMissing,
                                                       @RequestParam(defaultValue = "all",required = false) String type,
                                                       @RequestParam String reportDate, @RequestBody MxGroupCompNavigation mxGroupCompNavigation, Authentication authentication) {
        Document document = new Document();
        try {
            document = groupCompareService.identicalCondition(mxGroupCompNavigation.getMxCompareGroup(), document,
                    mxGroupCompNavigation.getMxCompareGroup().template.equalsIgnoreCase("null") || mxGroupCompNavigation.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null"), isMatched);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareNavigation(reportDate, mxGroupCompNavigation.getMxCompareGroup(), mxGroupCompNavigation.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxGroupCompNavigation.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }


    @ApiOperation("get group wise data for Combined Portfolio")
    @PostMapping("/api/uam/groups-compare/combined-portfolio")
    public ResponseEntity<?> getGroupCompareCombinedPortfolio(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int pageSize,
                                                              @RequestParam(defaultValue = "false") boolean isMatched,
                                                              @RequestParam(defaultValue = "false") boolean isAdditionalMissing,
                                                              @RequestParam(defaultValue = "all",required = false) String type,
                                                              @RequestParam String reportDate, @RequestBody MxGroupCompPortfolio mxGroupCompPortfolio, Authentication authentication) {
        Document document = new Document();
        try {
          
                document = groupCompareService.identicalCondition(mxGroupCompPortfolio.getMxCompareGroup(), document,mxGroupCompPortfolio.getMxCompareGroup().template.equalsIgnoreCase("null") ||
                                mxGroupCompPortfolio.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null"), isMatched);
                if (document != null) {
                    return new ResponseEntity<>(document, HttpStatus.OK);
                }
                document = groupCompareService.getMultiGroupCompareCombPortfolio(reportDate, mxGroupCompPortfolio.getMxCompareGroup(), mxGroupCompPortfolio.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxGroupCompPortfolio.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }
//
//    @ApiOperation("get group wise data for stp")
//    @PostMapping("/api/uam/groups-compare/stp")
//    public ResponseEntity<?> getGroupCompareStp(@RequestParam(defaultValue = "0") int page,
//                                                @RequestParam(defaultValue = "20") int pageSize,
//                                                @RequestParam(defaultValue = "false") boolean isMatched,
//                                                @RequestParam(defaultValue = "false") boolean isAdditionalMissing,
//                                                @RequestParam(defaultValue = "all",required = false) String type,
//                                                @RequestParam String reportDate, @RequestBody MxStpGroupCompFilter mxStpGroupCompFilter, Authentication authentication) {
//        Document document = new Document();
//        LocalDate requestDate = viewerExportService.getLatestDate();
//        try {
//            boolean isCompare = mxStpGroupCompFilter.getMxCompareGroup().template.equalsIgnoreCase("null") || mxStpGroupCompFilter.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null");
//            document = groupCompareService.identicalCondition(mxStpGroupCompFilter.getMxCompareGroup(), document, isCompare, isMatched);
//            if (document != null) {
//                return new ResponseEntity<>(document, HttpStatus.OK);
//            }
//            document = groupCompareService.getMultiGroupCompareStp(requestDate, mxStpGroupCompFilter.getMxCompareGroup(), mxStpGroupCompFilter.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
//            document.put("key", requestDate);
//        } catch (Exception ex) {
//            document = exceptionResponse(ex, mxStpGroupCompFilter.getMxCompareGroup().getSearchValue());
//        }
//        return new ResponseEntity<>(document, HttpStatus.OK);
//    }

    @ApiOperation("get group wise data for Consistency")
    @PostMapping("/api/uam/groups-compare/consistency")
    public ResponseEntity<?> getGroupCompareConsistency(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(defaultValue = "false") boolean isMatched,
                                                        @RequestParam(defaultValue = "false") boolean isAdditionalMissing,
                                                        @RequestParam(defaultValue = "all",required = false) String type,
                                                        @RequestParam String reportDate, @RequestBody MxConsistencyGroupCompFilter mxConsistencyGroupCompFilter, Authentication authentication) {
        Document document = new Document();
        try {
            boolean isCompare = mxConsistencyGroupCompFilter.getMxCompareGroup().template.equalsIgnoreCase("null") || mxConsistencyGroupCompFilter.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null");
            document = groupCompareService.identicalCondition(mxConsistencyGroupCompFilter.getMxCompareGroup(), document, isCompare, isMatched);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareConsistency(reportDate, mxConsistencyGroupCompFilter.getMxCompareGroup(), mxConsistencyGroupCompFilter.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxConsistencyGroupCompFilter.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get group wise data Comparison for Finance")
    @PostMapping("/api/uam/group-compare/finance")
    public ResponseEntity<?> getGroupCompareByFinanceManagement(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int pageSize,
                                                                @RequestParam String reportDate, Authentication authentication,
                                                                @RequestParam(defaultValue = "all",required = false) String type,
                                                                @RequestBody MxCompareGroup mxCompareGroup) {
        Document document = new Document();
        try {

            document = groupCompareService.identicalCondition(mxCompareGroup, document, false, false);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareFinance(reportDate, mxCompareGroup, page, pageSize, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxCompareGroup.getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get group wise data for Chinese Wall")
    @PostMapping("/api/uam/groups-compare/chinese-wall")
    public ResponseEntity<?> getGroupCompareChineseWall(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(defaultValue = "false") boolean isMatched,
                                                        @RequestParam(defaultValue = "false") boolean isAdditionalMissing, Authentication authentication,
                                                        @RequestParam(defaultValue = "all",required = false) String type,
                                                        @RequestParam String reportDate, @RequestBody MxGroupCompChinese mxGroupCompChinese) {
        Document document = new Document();
        try {
            boolean isCompare = mxGroupCompChinese.getMxCompareGroup().template.equalsIgnoreCase("null") || mxGroupCompChinese.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null");
            document = groupCompareService.identicalCondition(mxGroupCompChinese.getMxCompareGroup(), document, isCompare, isMatched);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareChinese(reportDate, mxGroupCompChinese.getMxCompareGroup(), mxGroupCompChinese.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxGroupCompChinese.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get group wise data for Operation Rights")
    @PostMapping("/api/uam/groups-compare/operation-rights")
    public ResponseEntity<?> getGroupCompareOperationRights(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                            @RequestParam(defaultValue = "false") boolean isMatched,
                                                            @RequestParam(defaultValue = "false") boolean isAdditionalMissing, Authentication authentication,
                                                            @RequestParam(defaultValue = "all",required = false) String type,
                                                            @RequestParam String reportDate, @RequestBody MxGroupCompareOperation mxGroupCompareOperation) {
        Document document = new Document();
        try {

            boolean isCompare = mxGroupCompareOperation.getMxCompareGroup().template.equalsIgnoreCase("null") || mxGroupCompareOperation.getMxCompareGroup().compareTemplate.equalsIgnoreCase("null");
            document = groupCompareService.identicalCondition(mxGroupCompareOperation.getMxCompareGroup(), document, isCompare, isMatched);
            if (document != null) {
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            document = groupCompareService.getMultiGroupCompareOperRights(reportDate, mxGroupCompareOperation.getMxCompareGroup(), mxGroupCompareOperation.getFilters(), page, pageSize, isMatched, isAdditionalMissing, authentication.getName(), type);
        } catch (Exception ex) {
            document = exceptionResponse(ex, mxGroupCompareOperation.getMxCompareGroup().getSearchValue());
        }
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @ApiOperation("get the filters for each report")
    @PostMapping("/api/uam/groups-compare/filters")
    public ResponseEntity<?> getGroupCompareFilterList(@RequestParam(defaultValue = "false") boolean isMatched,
                                                       @RequestParam(defaultValue = "false") boolean isAdditionalMissing,
                                                       @RequestParam(defaultValue = "0", required = false) int page,
                                                       @RequestParam(defaultValue = "100", required = false) int pageSize,
                                                       @RequestParam String reportDate, @RequestBody GroupCompareFilter filter) {
        Document document = new Document();

        document = groupCompareService.getAllFilters(reportDate, filter, isMatched, isAdditionalMissing,page,pageSize,filter.search,filter.type);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    public Document exceptionResponse(Exception ex, String searchValue) {
        Document document = new Document();
        ex.printStackTrace();
        if (searchValue != null && !searchValue.isBlank()) {
            document.put("message", "Invalid Query.");
        } else {
            document.put("message", "There is a problem in processing your request.");
        }
        document.put("content1", Collections.emptyList());
        document.put("content2", Collections.emptyList());
        log.info("Exception Occur while comparing the groups:{}",ex.getMessage());
        return document;
    }
}
