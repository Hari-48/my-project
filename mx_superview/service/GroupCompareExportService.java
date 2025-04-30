package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditObjectType;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.models.GeneralSpecification;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompareType;
import com.finsurge.tmr_portal.mx_superview.models.UamReportSummary;
import com.finsurge.tmr_portal.mx_superview.repository.*;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GroupCompareExportService {
    private static final Logger log = LoggerFactory.getLogger(ViewerExportService.class);
    private final DateTimeFormatter dateTimeFormatter;
    private final DownloadJobService downloadJobService;
    private final ViewerExportService viewerExportService;
    private final MxPortfolioRightsRepository mxPortfolioRightsRepository;
    private final MxOSPRightsTemplateRepository mxOSPRightsTemplateRepository;
    private final MxChineseWallTemplateRepository mxChineseWallTemplateRepository;
    private final MxEnterPriseRiskRepo mxEnterPriseRiskRepo;
    private final MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo;
    private final MxGroupNavigationRightsRepository mxGroupNavigationRightsRepository;
    private final MxConsistencyTemplateRepository mxConsistencyTemplateRepository;
    private final STPRightsRepository stpRightsRepository;
    private final MxFinanceRightsRepo mxFinanceRightsRepo;
    private final MxOperationRightsRepo mxOperationRightsRepo;

    private final MxOverviewAuditService mxOverviewAuditService;

    private final UamSummaryReportJobService reportJobService;
    private final MxCwtConfigMgtRightRepository mxCwtConfigMgtRightRepository;
    @Value("${uam.compare.exclude.fields}")
    private int excludeCount;
    @Value("${uam.compare.stp_navigation.exclude.fields}")
    private int stpNavexcludeCount;

    @Value("${uam.thread.pool.value}")
    private int threadValue;

    public GroupCompareExportService(DownloadJobService downloadJobService, ViewerExportService viewerExportService, MxPortfolioRightsRepository mxPortfolioRightsRepository, MxOSPRightsTemplateRepository mxOSPRightsTemplateRepository, MxChineseWallTemplateRepository mxChineseWallTemplateRepository, MxEnterPriseRiskRepo mxEnterPriseRiskRepo, MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo, MxGroupNavigationRightsRepository mxGroupNavigationRightsRepository, MxConsistencyTemplateRepository mxConsistencyTemplateRepository, STPRightsRepository stpRightsRepository, MxFinanceRightsRepo mxFinanceRightsRepo, MxOperationRightsRepo mxOperationRightsRepo, MxOverviewAuditService mxOverviewAuditService, UamSummaryReportJobService reportJobService, MxCwtConfigMgtRightRepository mxCwtConfigMgtRightRepository) {
        this.viewerExportService = viewerExportService;
        this.mxPortfolioRightsRepository = mxPortfolioRightsRepository;
        this.mxOSPRightsTemplateRepository = mxOSPRightsTemplateRepository;
        this.mxChineseWallTemplateRepository = mxChineseWallTemplateRepository;
        this.mxEnterPriseRiskRepo = mxEnterPriseRiskRepo;
        this.mxGroupCombinedPortfolioRepo = mxGroupCombinedPortfolioRepo;
        this.mxGroupNavigationRightsRepository = mxGroupNavigationRightsRepository;
        this.mxConsistencyTemplateRepository = mxConsistencyTemplateRepository;
        this.stpRightsRepository = stpRightsRepository;
        this.mxFinanceRightsRepo = mxFinanceRightsRepo;
        this.mxOperationRightsRepo = mxOperationRightsRepo;
        this.mxOverviewAuditService = mxOverviewAuditService;
        this.reportJobService = reportJobService;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
        this.downloadJobService = downloadJobService;
        this.mxCwtConfigMgtRightRepository=mxCwtConfigMgtRightRepository;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getPortfolioDetailsSummaryReport(GeneralSpecification filters, String type,UamSummaryReportJob job) throws ExecutionException, InterruptedException {
        UamReportSummary summary = new UamReportSummary();
        log.info(" Report Summary: Starting fetch.");
        log.info(" Report Summary: Initializing job status.");
        log.info("Portfolio Rights download job service.Started.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            long totalCount = 0;
            long compareCount = 0;
            int totalPage = 1;
            int pageSize = 500;
            int page = 0;
            int batchSize = 5;
            Page<MxGroupPortfolioRights> additionalPortfolioRightsList;
            Page<MxGroupPortfolioRights> missingPortfolioRightsList;
            Map<String, Long> comparePortfolioSummaryValue = new HashMap<>();
            long reportFieldsCnt = MxGroupPortfolioRights.class.getDeclaredFields().length - excludeCount;
            Map<String, Long> comparePortfolioSummaryList = new HashMap<>();

            if (filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                comparePortfolioSummaryList.put("mismatchRows", 0L);
                comparePortfolioSummaryList.put("matchedRows", 0L);
                comparePortfolioSummaryList.put("misMatchFieldCount", 0L);
                comparePortfolioSummaryList.put("matchedFieldsCount", 0L);
                comparePortfolioSummaryList.put("totalFieldsCompared", 0L);
                comparePortfolioSummaryList.put("availableRows", 0L);
                comparePortfolioSummaryList.put("missingRows", 0L);
                downloadJobService.updateJobProgress("FETCH_COMPLETE", comparePortfolioSummaryList.size(), comparePortfolioSummaryList.size(), job.getId());
                reportJobService.updateJob(job.getId(), new JSONObject(comparePortfolioSummaryList).toString(), true);
                return CompletableFuture.completedFuture(null);
            } else {
                try {
                    for (int i = 0; i < totalPage; i++) {
                        Document comparePortfolioSummary = comparePortfolioRights(page, pageSize, filters, type);
                        comparePortfolioSummaryValue.putAll((Map<? extends String, ? extends Long>) comparePortfolioSummary.get("content"));
                        totalCount = (long) comparePortfolioSummary.get("records");
                        compareCount = (long) comparePortfolioSummary.get("compareRecords");
                        totalPage = (int) comparePortfolioSummary.get("totalPages");
                        if (i > 0) {
                            comparePortfolioSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + comparePortfolioSummaryValue.get("mismatchRows"));
                            comparePortfolioSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + comparePortfolioSummaryValue.get("matchedRows"));
                            comparePortfolioSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + comparePortfolioSummaryValue.get("misMatchFieldCount"));
                        } else {
                            comparePortfolioSummaryList.putAll(comparePortfolioSummaryValue);
                        }
                        page++;

                    }
                } catch (Exception e) {
                    log.info("Exception occured in getPortfolio SummaryReport");
                    e.printStackTrace();
                }
            }
                    reportJobService.updateJobProgress("FETCH IN PROGRESS", summary.getClass().getDeclaredFields().length, comparePortfolioSummaryList.size(), job.getId());
                    LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
                    additionalPortfolioRightsList = mxPortfolioRightsRepository.findAllPortfolioGroupCompareAdditionalExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                            PageRequest.of(0, pageSize));
                    long additionalTotalRows = additionalPortfolioRightsList.getTotalElements();
                    missingPortfolioRightsList = mxPortfolioRightsRepository.findAllPortfolioGroupCompareAdditionalExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                            PageRequest.of(0, pageSize));
                    long missingTotalRows = missingPortfolioRightsList.getTotalElements();
                    if (totalCount != 0 && compareCount != 0) {
                        comparePortfolioSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
                    } else {
                        comparePortfolioSummaryList.put("totalFieldsCompared", 0L);
                    }
                    long matchedFieldsCount = comparePortfolioSummaryList.get("totalFieldsCompared") - comparePortfolioSummaryList.get("misMatchFieldCount");
                    comparePortfolioSummaryList.put("availableRows", additionalTotalRows);
                    comparePortfolioSummaryList.put("missingRows", missingTotalRows);
                    comparePortfolioSummaryList.put("matchedFieldsCount", matchedFieldsCount);
                    log.info("Portfolio Fetch completed");
                    reportJobService.updateJobProgress("FETCH COMPLETED", summary.getClass().getDeclaredFields().length, comparePortfolioSummaryList.size(), job.getId());

                    //updating job with generated summary
                    reportJobService.updateJob(job.getId(), new JSONObject(comparePortfolioSummaryList).toString(), true);

            } catch(Exception e){
                throw new RuntimeException(e);
            }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getPortfolioDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                                DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info(filters.toString());
        List<MxGroupPortfolioRights> portfolioLabels = new ArrayList<>();
        Page<MxGroupPortfolioRights> additionalPortfolioRightsList;
        Page<MxGroupPortfolioRights> missingPortfolioRightsList;
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info(" Export: Starting fetch.");
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
       if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
           try {
               if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                   for (int i = 0; i < totalPage; i++) {
                       Document portfolioLabel = comparePortfolioRights(page, pageSize, filters, type);
                       portfolioLabels.addAll((List<MxGroupPortfolioRights>) portfolioLabel.get("content"));
                       totalPage = (int) portfolioLabel.get("totalPages");
                       totalCount = Long.valueOf(String.valueOf(portfolioLabel.get("records")));
                       downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
                       page++;
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }

           // additional
           if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
               int additionalTotalPage = 1;
               for (int i = 0; i < additionalTotalPage; i++) {
                   additionalPortfolioRightsList = mxPortfolioRightsRepository.findAllPortfolioGroupCompareAdditionalExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                           PageRequest.of(additionalPage, pageSize));
                   additionalPortfolioRightsList.forEach(c -> c.setDiff("Additonal row " + filters.getGroupName()));
                   portfolioLabels.addAll(additionalPortfolioRightsList.getContent());
                   additionalTotalPage = additionalPortfolioRightsList.getTotalPages();
                   totalCount = additionalPortfolioRightsList.getTotalElements();
                   downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
                   additionalPage++;
               }
           }
           // missing
           if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
               int missingTotalPage = 1;
               for (int i = 0; i < missingTotalPage; i++) {
                   missingPortfolioRightsList = mxPortfolioRightsRepository.findAllPortfolioGroupCompareAdditionalExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                           PageRequest.of(missingPage, pageSize));
                   missingPortfolioRightsList.forEach(c -> c.setDiff("Missing row " + filters.getGroupName()));
                   portfolioLabels.addAll(missingPortfolioRightsList.getContent());
                   missingTotalPage = missingPortfolioRightsList.getTotalPages();
                   totalCount = missingPortfolioRightsList.getTotalElements();
                   downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
                   missingPage++;
               }
           }
       }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", portfolioLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, portfolioLabels, fileName, fieldMaps, color, job, fieldColumns);
    }


    @Transactional
    public Document  comparePortfolioRights(int page, int pageSize, GeneralSpecification generalPortfolioRights, String type) {
        Document document = new Document();
        Page<MxGroupPortfolioRights> portfolioRightsList;
        Page<MxGroupPortfolioRights> comparePortfolioRightsList;
        List<MxGroupPortfolioRights> matchedPortfolioList = new ArrayList<>();
        List<MxGroupPortfolioRights> misMatchedPortfolioList = new ArrayList<>();
        Map<String, Long> summaryList = new HashMap<String, Long>();
        LocalDate repDate = LocalDate.parse(generalPortfolioRights.getDateValue(), dateTimeFormatter);
          portfolioRightsList = mxPortfolioRightsRepository.findAllPortfolioGroupCompareExport(repDate, generalPortfolioRights.getTemplateValue(), generalPortfolioRights.getCompareTemplateValue(),
                PageRequest.of(page, pageSize));
        comparePortfolioRightsList = mxPortfolioRightsRepository.findAllPortfolioGroupCompareExport(repDate, generalPortfolioRights.getCompareTemplateValue(), generalPortfolioRights.getTemplateValue(),
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxGroupPortfolioRights portfolioRights : portfolioRightsList) {
            StringBuilder message = new StringBuilder("");
            if (comparePortfolioRightsList.getContent().size() > i) {
                MxGroupPortfolioRights comparePortfolioRight = comparePortfolioRightsList.getContent().get(i);
                if (comparePortfolioRight != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(portfolioRights, comparePortfolioRight);
                    if (diff1.hasChanges()) {
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        message.append("Mis matched row");
                        portfolioRights.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedPortfolioList.add(portfolioRights);
                    } else {
                        message.append("Matched row");
                        portfolioRights.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedPortfolioList.add(portfolioRights);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", portfolioRightsList.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            List<MxGroupPortfolioRights> allPortfolioList = new ArrayList<>();
            allPortfolioList.addAll(matchedPortfolioList);
            allPortfolioList.addAll(misMatchedPortfolioList);
            document.put("records", allPortfolioList.size());
            document.put("content", portfolioRightsList.getContent());
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedPortfolioList.size());
            document.put("content", matchedPortfolioList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedPortfolioList.size());
            document.put("content", misMatchedPortfolioList);
        } else if (type.equalsIgnoreCase("summary")) {
            document.put("records", portfolioRightsList.getTotalElements());
            document.put("content", summaryList);
            document.put("compareRecords", comparePortfolioRightsList.getTotalElements());
        } else {
            document.put("records", portfolioRightsList.getTotalElements());
            document.put("content", portfolioRightsList.getContent());
        }
        return document;
    }

    //OSP Rights Group Comparison
    @Async
    @Transactional
    public CompletableFuture<Void> getOspRightsDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                                DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info(filters.toString());
        List<MxOspRightsMatrix> osprightsLabels = new ArrayList<>();
        Page<MxOspRightsMatrix> additionalOspRightsList;
        Page<MxOspRightsMatrix> missingOspRightsList;
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info(" Export: Starting OSP Rights Comparison fetch.");
        log.info(" Export: Initializing OSP Rights Comparison job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                try {
                    if((!(filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")))
                            && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
                        for (int i = 0; i < totalPage; i++) {
                            Document ospRightsLabel = compareOspRights(page, pageSize, filters, type);
                            osprightsLabels.addAll((List<MxOspRightsMatrix>) ospRightsLabel.get("content"));
                            totalPage = (int) ospRightsLabel.get("totalPages");
                            totalCount = Long.valueOf(String.valueOf(ospRightsLabel.get("records")));
                            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, osprightsLabels.size(), job.getId());
                            page++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();

            }
        }
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            // additional
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
                int additionalTotalPage = 1;
                for (int i = 0; i < additionalTotalPage; i++) {
                    additionalOspRightsList = mxOSPRightsTemplateRepository.findAllGroupCompareAdditionalExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                            PageRequest.of(additionalPage, pageSize));
                    additionalOspRightsList.forEach(c -> c.setDiff("Data Available in " + filters.getGroupName()));
                    osprightsLabels.addAll(additionalOspRightsList.getContent());
                    additionalTotalPage = additionalOspRightsList.getTotalPages();
                    totalCount = additionalOspRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) additionalOspRightsList.getTotalElements(), osprightsLabels.size(), job.getId());
                    additionalPage++;
                }
            }
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
                int missingTotalPage = 1;
                for (int i = 0; i < missingTotalPage; i++) {
                    missingOspRightsList = mxOSPRightsTemplateRepository.findAllGroupCompareAdditionalExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                            PageRequest.of(missingPage, pageSize));
                    missingOspRightsList.forEach(c -> c.setDiff("Data not available on " + filters.getGroupName() + " - but available on " + filters.getCompareGroupName()));
                    osprightsLabels.addAll(missingOspRightsList.getContent());
                    missingTotalPage = missingOspRightsList.getTotalPages();
                    totalCount = missingOspRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) missingOspRightsList.getTotalElements(), osprightsLabels.size(), job.getId());
                    missingPage++;
                }
            }
        }
        log.info(" Export: Fetch OSP Rights Comparison complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", osprightsLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, osprightsLabels, fileName, fieldMaps, color, job, fieldColumns);
    }

    public Document compareOspRights(int page, int pageSize, GeneralSpecification generalPortfolioRights, String type) {
        Document document = new Document();
        Page<MxOspRightsMatrix> ospRightsList;
        Page<MxOspRightsMatrix> compareOspRightsList;
        List<MxOspRightsMatrix> matchedOspRightsList = new ArrayList<>();
        List<MxOspRightsMatrix> misMatchedOspRightsList = new ArrayList<>();
        Map<String, Long> summaryList = new HashMap<String, Long>();
        LocalDate repDate = LocalDate.parse(generalPortfolioRights.getDateValue(), dateTimeFormatter);
        ospRightsList = mxOSPRightsTemplateRepository.findAllGroupCompareMatchedExport(repDate, generalPortfolioRights.getTemplateValue(), generalPortfolioRights.getCompareTemplateValue(),
                PageRequest.of(page, pageSize));
        compareOspRightsList = mxOSPRightsTemplateRepository.findAllGroupCompareMatchedExport(repDate, generalPortfolioRights.getCompareTemplateValue(), generalPortfolioRights.getTemplateValue(),
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchRowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxOspRightsMatrix ospRightsMatrix : ospRightsList) {
            StringBuilder message = new StringBuilder("");
            if (compareOspRightsList.getContent().size() > i) {
                MxOspRightsMatrix compareOspRight = compareOspRightsList.getContent().get(i);
                if (compareOspRight != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(ospRightsMatrix, compareOspRight);
                    if (diff1.hasChanges()) {
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        ospRightsMatrix.setDiff(String.valueOf(message));
                        misMatchRowCount++;
                        misMatchedOspRightsList.add(ospRightsMatrix);
                    } else {
                        ospRightsMatrix.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedOspRightsList.add(ospRightsMatrix);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchRowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", ospRightsList.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            document.put("records", ospRightsList.getTotalElements());
            document.put("content", ospRightsList.getContent());
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedOspRightsList.size());
            document.put("content", matchedOspRightsList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedOspRightsList.size());
            document.put("content", misMatchedOspRightsList);
        } else if (type.equalsIgnoreCase("summary")) {
            document.put("records", ospRightsList.getTotalElements());
            document.put("compareRecords", compareOspRightsList.getTotalElements());
            document.put("content", summaryList);
        } else {
            document.put("records", ospRightsList.getTotalElements());
            document.put("content", ospRightsList.getContent());
        }
        return document;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getOspReportsSummaryReport(GeneralSpecification filters, String type,UamSummaryReportJob job) throws ExecutionException, InterruptedException {

        UamReportSummary summary = new UamReportSummary();
        log.info(" Report Summary: Starting fetch.");
        log.info(" Report Summary: Initializing job status.");
        log.info("OSP Rights download job service.Started.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            long totalCount = 0;
            long compareCount = 0;
            int totalPage = 1;
            int pageSize = 500;
            int page = 0;
            int batchSize = 5;
            Page<MxOspRightsMatrix> additionalOspList;
            Page<MxOspRightsMatrix> missingOspList;
            Map<String, Long> compareOspSummaryValue = new HashMap<>();
            long reportFieldsCnt = MxOspRightsMatrix.class.getDeclaredFields().length - excludeCount;
            Map<String, Long> compareOspSummaryList = new HashMap<>();
            if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))||filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                compareOspSummaryList.put("mismatchRows", 0L);
                compareOspSummaryList.put("matchedRows", 0L);
                compareOspSummaryList.put("misMatchFieldCount", 0L);
                compareOspSummaryList.put("availableRows", 0L);
                compareOspSummaryList.put("missingRows", 0L);
                compareOspSummaryList.put("matchedFieldsCount", 0L);
                compareOspSummaryList.put("totalFieldsCompared", 0L);
                log.info("Osp Rights download job service.fetch complete.");
                downloadJobService.updateJobProgress("FETCH_COMPLETE", compareOspSummaryList.size(), compareOspSummaryList.size(), job.getId());
                reportJobService.updateJob(job.getId(), new JSONObject(compareOspSummaryList).toString(), true);
                return CompletableFuture.completedFuture(null);
            } else if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
                compareOspSummaryList.put("mismatchRows", 0L);
                compareOspSummaryList.put("matchedRows", 0L);
                compareOspSummaryList.put("misMatchFieldCount", 0L);
                compareOspSummaryList.put("matchedFieldsCount", 0L);
            }else {
                try {
                    for (int i = 0; i < totalPage; i++) {
                        Document compareOspSummary = compareOspRights(page, pageSize, filters, type);
                        compareOspSummaryValue.putAll((Map<? extends String, ? extends Long>) compareOspSummary.get("content"));
                        totalCount = (long) compareOspSummary.get("records");
                        totalPage = (int) compareOspSummary.get("totalPages");
                        compareCount=(long) compareOspSummary.get("compareRecords");
                        if (i > 0) {
                            compareOspSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareOspSummaryValue.get("mismatchRows"));
                            compareOspSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareOspSummaryValue.get("matchedRows"));
                            compareOspSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + compareOspSummaryValue.get("misMatchFieldCount"));
                        } else {
                            compareOspSummaryList.putAll(compareOspSummaryValue);
                        }
                        page++;
                    }
                    reportJobService.updateJobProgress("FETCH IN PROGRESS", summary.getClass().getDeclaredFields().length, compareOspSummaryList.size(), job.getId());
                }catch (Exception e) {
                    log.info("Exception occured in getNavigationSummaryReport");
                    e.printStackTrace();
                }
            }
            if (totalCount != 0 && compareCount != 0){
                compareOspSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
            } else {
                compareOspSummaryList.put("totalFieldsCompared", 0L);
            }
            long matchedFieldsCount = compareOspSummaryList.get("totalFieldsCompared") - compareOspSummaryList.get("misMatchFieldCount");
            compareOspSummaryList.put("matchedFieldsCount", matchedFieldsCount);
            LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
            additionalOspList = mxOSPRightsTemplateRepository.findAllGroupCompareAdditionalExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                    PageRequest.of(0, pageSize));
            long additionalTotalRows = additionalOspList.getTotalElements();
            missingOspList = mxOSPRightsTemplateRepository.findAllGroupCompareAdditionalExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                    PageRequest.of(0, pageSize));
            long missingTotalRows = missingOspList.getTotalElements();

            compareOspSummaryList.put("availableRows", additionalTotalRows);
            compareOspSummaryList.put("missingRows", missingTotalRows);
            log.info("Osp Fetch completed");
            reportJobService.updateJobProgress("FETCH COMPLETED", summary.getClass().getDeclaredFields().length, compareOspSummaryList.size(), job.getId());

            //updating job with generated summary
            reportJobService.updateJob(job.getId(), new JSONObject(compareOspSummaryList).toString(), true);


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    //chinesewall
    @Async
    @Transactional
    public CompletableFuture<Void> getChineseWallDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                                  DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info("Chinesewall Export filters : {} ", filters.toString());
        List<MxChineseWallTmpl> chineseWallLabels = new ArrayList<>();
        Page<MxChineseWallTmpl> additionalChineseWallList; //= null;
        Page<MxChineseWallTmpl> missingChineseWallList; //= null;
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info("Compare Chinesewall Export: Starting fetch.");
        log.info("Compare Chinesewall Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                if((!(filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")))
                        && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document chineseWallTemp = compareChienesewallTemplate(page, pageSize, filters, type);
                        chineseWallLabels.addAll((Collection<? extends MxChineseWallTmpl>) chineseWallTemp.get("content"));
                        totalPage = (int) chineseWallTemp.get("totalPages");
                        totalCount = totalCount + (int) chineseWallTemp.get("records");
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallLabels.size(), job.getId());
                        page++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            // additional
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
                int additionalTotalPage = 1;
                for (int i = 0; i < additionalTotalPage; i++) {
                    additionalChineseWallList = mxChineseWallTemplateRepository.findAllGroupCompareChineseAdditionalExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                            PageRequest.of(additionalPage, pageSize));
                    additionalChineseWallList.forEach(c -> c.setDiff("Additonal row " + filters.getGroupName()));
                    chineseWallLabels.addAll(additionalChineseWallList.getContent());
                    additionalTotalPage=additionalChineseWallList.getTotalPages();
                    totalCount = additionalChineseWallList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallLabels.size(), job.getId());
                    additionalPage++;
                }
            }
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {    // missing
                int missingTotalPage = 1;
                for (int i = 0; i < missingTotalPage; i++) {
                    missingChineseWallList = mxChineseWallTemplateRepository.findAllGroupCompareChineseAdditionalExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                            PageRequest.of(missingPage, pageSize));
                    missingChineseWallList.forEach(c -> c.setDiff("Missing row " + filters.getGroupName()));
                    chineseWallLabels.addAll(missingChineseWallList.getContent());
                    missingTotalPage=missingChineseWallList.getTotalPages();
                    totalCount = missingChineseWallList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallLabels.size(), job.getId());
                    missingPage++;
                }
            }
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", chineseWallLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, chineseWallLabels, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document compareChienesewallTemplate(int page, int pageSize, GeneralSpecification generalChienesewall, String type) {
        Document document = new Document();
        Page<MxChineseWallTmpl> chineseWallList; //= null;
        Page<MxChineseWallTmpl> compareChineseWallList; //= null ;
        List<MxChineseWallTmpl> matchedChineseWallList = new ArrayList<>();
        List<MxChineseWallTmpl> misMatchedChineseWallList = new ArrayList<>();
        Map<String, Long> summaryList = new HashMap<>();
        LocalDate repDate = LocalDate.parse(generalChienesewall.getDateValue(), dateTimeFormatter);
        chineseWallList = mxChineseWallTemplateRepository.findAllGroupCompareChineseMatchedExport(repDate, generalChienesewall.getTemplateValue(), generalChienesewall.getCompareTemplateValue(),
                PageRequest.of(page, pageSize));
        compareChineseWallList = mxChineseWallTemplateRepository.findAllGroupCompareChineseMatchedExport(repDate, generalChienesewall.getCompareTemplateValue(), generalChienesewall.getTemplateValue(),
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxChineseWallTmpl chineseWallValue : chineseWallList) {
            StringBuilder message = new StringBuilder("");
            if (compareChineseWallList.getContent().size() > i) {
                MxChineseWallTmpl compareChineseWall = compareChineseWallList.getContent().get(i);
                if (compareChineseWall != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(chineseWallValue, compareChineseWall);
                    if (diff1.hasChanges()) {
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        message.append("Mis Matched row");
                        chineseWallValue.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedChineseWallList.add(chineseWallValue);
                    } else {
                        message.append("Matched row");
                        chineseWallValue.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedChineseWallList.add(chineseWallValue);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", chineseWallList.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            document.put("records", chineseWallList.getTotalElements());
            document.put("content", chineseWallList.getContent());
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedChineseWallList.size());
            document.put("content", matchedChineseWallList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedChineseWallList.size());
            document.put("content", misMatchedChineseWallList);
        } else if (type.equalsIgnoreCase("summary")) {
            document.put("records", chineseWallList.getTotalElements());
            document.put("content", summaryList);
        } else {
            document.put("records", chineseWallList.getTotalElements());
            document.put("content", chineseWallList.getContent());
        }
        return document;
    }

    @Async
    public CompletableFuture<Void> getChineseWallSummaryReport(GeneralSpecification filters, String type,UamSummaryReportJob job) throws ExecutionException, InterruptedException {

        UamReportSummary summary = new UamReportSummary();
        log.info(" Report Summary: Starting fetch.");
        log.info(" Report Summary: Initializing job status.");
        ConcurrentHashMap<String, Long> resultedMap= new ConcurrentHashMap<>();
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        int pageSize = 500;
        if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))||
                filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
            resultedMap.put("mismatchRows", 0L);
            resultedMap.put("matchedRows", 0L);
            resultedMap.put("misMatchFieldCount", 0L);
            resultedMap.put("availableRows", 0L);
            resultedMap.put("missingRows", 0L);
            resultedMap.put("matchedFieldsCount", 0L);
            resultedMap.put("totalFieldsCompared", 0L);
            log.info("Chinesewall Rights download job service.fetch complete.");
            downloadJobService.updateJobProgress("FETCH_COMPLETE", resultedMap.size(), resultedMap.size(), job.getId());
            reportJobService.updateJob(job.getId(), new JSONObject(resultedMap).toString(), true);
            return CompletableFuture.completedFuture(null);
        } else if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
            resultedMap.put("mismatchRows", 0L);
            resultedMap.put("matchedRows", 0L);
            resultedMap.put("misMatchFieldCount", 0L);
            resultedMap.put("matchedFieldsCount", 0L);
            resultedMap.put("totalFieldsCompared", 0L);
        }else {
            try {
                LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
                Page<MxChineseWallTmpl> tempList = mxChineseWallTemplateRepository.findAllGroupCompareChineseMatchedExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                        PageRequest.of(0, pageSize));
                Page<MxChineseWallTmpl> compareTempList = mxChineseWallTemplateRepository.findAllGroupCompareChineseMatchedExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                        PageRequest.of(0, pageSize));
                int totalPage = tempList.getTotalPages();
                long totalCount = tempList.getTotalElements();

                ExecutorService executorService = Executors.newFixedThreadPool(threadValue);// TBD for strategy
                CompletableFuture<ConcurrentHashMap<String, Long>>[] futures = new CompletableFuture[totalPage];
                for (int page = 0; page < totalPage; page++) {
                    final int finalPage = page;
                    futures[page] = CompletableFuture
                            .supplyAsync(() -> compareChienesewallTemplate(finalPage, pageSize, filters, type), executorService)
                            .thenApply(document -> {
                                return constructMapFromDocument(document);
                            });
                }

                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
                CompletableFuture<ConcurrentHashMap<String, Long>> allPageContentsFuture = allFutures.thenApply(result -> {
                    ConcurrentHashMap<String, Long> tempMap = new ConcurrentHashMap<>();
                    tempMap.put("mismatchRows", 0L);
                    tempMap.put("matchedRows", 0L);
                    tempMap.put("misMatchFieldCount", 0L);

                    return Arrays.asList(futures).stream()
                            .map(pageContentFuture -> pageContentFuture.join())
                            .reduce((oldMap, newMap) -> {
                                oldMap.computeIfPresent("mismatchRows", (key, newValue) -> newValue + newMap.get("mismatchRows"));
                                oldMap.computeIfPresent("matchedRows", (key, newValue) -> newValue + newMap.get("matchedRows"));
                                oldMap.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + newMap.get("misMatchFieldCount"));
                                return oldMap;
                            }).orElse(tempMap);
                });

                resultedMap = allPageContentsFuture.get();

                reportJobService.updateJobProgress("FETCH IN PROGRESS", summary.getClass().getDeclaredFields().length, resultedMap.size(), job.getId());
                long reportFieldsCnt = MxChineseWallTmpl.class.getDeclaredFields().length - excludeCount;

                if (compareTempList.getTotalElements() > 0 && totalCount > 0) {
                    resultedMap.put("totalFieldsCompared", totalCount * reportFieldsCnt);
                } else {
                    resultedMap.put("totalFieldsCompared", 0L);
                }
                long matchedFieldsCount = resultedMap.get("totalFieldsCompared") - resultedMap.get("misMatchFieldCount");
                resultedMap.put("matchedFieldsCount", matchedFieldsCount);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
            calculateMissingAndAvailableRows(filters, pageSize, resultedMap);
            reportJobService.updateJobProgress("FETCH COMPLETES", summary.getClass().getDeclaredFields().length, resultedMap.size(), job.getId());
            reportJobService.updateJob(job.getId(), new JSONObject(resultedMap).toString(), true);
        return CompletableFuture.completedFuture(null);
    }

    private ConcurrentHashMap<String, Long> constructMapFromDocument(Document compareChineseWallSummary) {
        ConcurrentHashMap<String, Long> compareSummaryMap = new ConcurrentHashMap<>();
        compareSummaryMap.putAll((Map<? extends String, ? extends Long>) compareChineseWallSummary.get("content"));
        return compareSummaryMap;
    }

    private void calculateMissingAndAvailableRows(GeneralSpecification filters, int pageSize, ConcurrentHashMap<String,Long> resultedMap){
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        Page<MxChineseWallTmpl>  additionalChineseWallList = mxChineseWallTemplateRepository.findAllGroupCompareChineseAdditionalExport(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                PageRequest.of(0, pageSize));
        long additionalTotalRows = additionalChineseWallList.getTotalElements();
        Page<MxChineseWallTmpl> missingChineseWallList = mxChineseWallTemplateRepository.findAllGroupCompareChineseAdditionalExport(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                PageRequest.of(0, pageSize));
        long missingTotalRows = missingChineseWallList.getTotalElements();
        resultedMap.put("availableRows", additionalTotalRows);
        resultedMap.put("missingRows", missingTotalRows);
    }

    //Enterprise export
    @Async
    @Transactional
    public CompletableFuture<Void> getEnterpriseDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                                 DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info("Enterprise Export filters : {} ", filters.toString());
        List<MxEnterpriseRisk> enterpriseLabels = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info("Compare Enterprise Export: Starting fetch.");
        log.info("Compare Enterprise Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            try {
                if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document enterpriseTemp = compareEnterpriseTemplate(page, pageSize, filters, type);
                        enterpriseLabels.addAll((Collection<? extends MxEnterpriseRisk>) enterpriseTemp.get("content"));
                        totalPage = (int) enterpriseTemp.get("totalPages");
                        totalCount = (long) enterpriseTemp.get("records");
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, enterpriseLabels.size(), job.getId());
                        page++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("Enterprise Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", enterpriseLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, enterpriseLabels, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document compareEnterpriseTemplate(int page, int pageSize, GeneralSpecification generalEnterprise, String type) {
        Document document = new Document();
        Page<MxEnterpriseRisk> enterpriseList;
        Page<MxEnterpriseRisk> compareEnterpriseList;
        Map<String, Long> summaryList = new HashMap<>();
        List<MxEnterpriseRisk> misMatchedEnterpriseList = new ArrayList<>();
        List<MxEnterpriseRisk> matchedEnterpriseList = new ArrayList<>();
        LocalDate repDate = LocalDate.parse(generalEnterprise.getDateValue(), dateTimeFormatter);
        enterpriseList = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(generalEnterprise.getTemplateValue(), repDate,
                PageRequest.of(page, pageSize));
        compareEnterpriseList = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(generalEnterprise.getCompareTemplateValue(), repDate,
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxEnterpriseRisk enterpriseValue : enterpriseList) {
            StringBuilder message = new StringBuilder("");
            if (compareEnterpriseList.getContent().size() > i) {
                MxEnterpriseRisk compareEnterprise = compareEnterpriseList.getContent().get(i);
                if (compareEnterprise != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(enterpriseValue, compareEnterprise);
                    if (diff1.hasChanges()) {
                        message.append("Mis Matched row");
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        enterpriseValue.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedEnterpriseList.add(enterpriseValue);
                    } else {
                        message.append("Matched row");
                        enterpriseValue.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedEnterpriseList.add(enterpriseValue);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            if (compareEnterpriseList.getTotalElements() > enterpriseList.getTotalElements()) {
                document.put("totalPages", compareEnterpriseList.getTotalPages());
                document.put("records", compareEnterpriseList.getTotalElements());
                List<MxEnterpriseRisk> tempList = new ArrayList<>(enterpriseList.getContent());
                List<MxEnterpriseRisk> compareTempList=new ArrayList<>(compareEnterpriseList.getContent().subList(tempList.size(), (int) compareEnterpriseList.getTotalElements()));
                compareTempList.forEach(f -> f.setDiff("Missing Row"));
                tempList.addAll(compareTempList);
                document.put("content", tempList);
            } else {
                document.put("records", enterpriseList.getTotalElements());
                List<MxEnterpriseRisk> tempList = new ArrayList<>(compareEnterpriseList.getContent());
                List<MxEnterpriseRisk> compareTempList=new ArrayList<>(enterpriseList.getContent().subList(tempList.size(), (int) enterpriseList.getTotalElements()));
                compareTempList.forEach(f -> f.setDiff("Additional Row"));
                tempList.addAll(compareTempList);
                document.put("content", tempList);
                document.put("totalPages", enterpriseList.getTotalPages());}
        }  if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedEnterpriseList.size());
            document.put("content", matchedEnterpriseList);
            document.put("totalPages", enterpriseList.getTotalPages());}
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedEnterpriseList.size());
            document.put("content", misMatchedEnterpriseList);
            document.put("totalPages", enterpriseList.getTotalPages());
        }  if (type.equalsIgnoreCase("summary")) {
            if (compareEnterpriseList.getTotalElements() > enterpriseList.getTotalElements()) {
                document.put("records", compareEnterpriseList.getTotalElements());
                document.put("compareRecord",enterpriseList.getTotalElements());
                document.put("totalPages", compareEnterpriseList.getTotalPages());
                summaryList.put("missingRows", compareEnterpriseList.getTotalElements() - enterpriseList.getTotalElements());
                summaryList.put("availableRows",0L);
            } else {
                document.put("records", enterpriseList.getTotalElements());
                document.put("compareRecord",compareEnterpriseList.getTotalElements());
                document.put("totalPages", enterpriseList.getTotalPages());
                summaryList.put("availableRows", enterpriseList.getTotalElements() - compareEnterpriseList.getTotalElements());
                summaryList.put("missingRows",0L);
            }
            document.put("content", summaryList);
        }
        return document;
    }

    public Map<String, Long> getEnterpriseSummaryReport(GeneralSpecification filters, String type) throws ExecutionException, InterruptedException {
        CompletableFuture<Map<String, Long>> futureOutput =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        long totalCount = 0;
                        long compareCount=0;
                        int totalPage = 1;
                        int pageSize = 500;
                        int page = 0;
                        Map<String, Long> compareEnterpriseSummaryValue = new HashMap<>();
                        long reportFieldsCnt = MxEnterpriseRisk.class.getDeclaredFields().length - excludeCount;
                        Map<String, Long> compareEnterpriseSummaryList = new HashMap<>();
                        if (filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                            compareEnterpriseSummaryList.put("mismatchRows", 0L);
                            compareEnterpriseSummaryList.put("matchedRows", 0L);
                            compareEnterpriseSummaryList.put("misMatchFieldCount", 0L);
                            compareEnterpriseSummaryList.put("matchedFieldsCount", 0L);
                            compareEnterpriseSummaryList.put("availableRows", 0L);
                            compareEnterpriseSummaryList.put("missingRows", 0L);
                            compareEnterpriseSummaryList.put("totalFieldsCompared", 0L);
                            return compareEnterpriseSummaryList;
                        } else {
                            try {
                                for (int i = 0; i < totalPage; i++) {
                                    Document compareEnterpriseSummary = compareEnterpriseTemplate(page, pageSize, filters, type);
                                    compareEnterpriseSummaryValue.putAll((Map<? extends String, ? extends Long>) compareEnterpriseSummary.get("content"));
                                    totalCount = (long) compareEnterpriseSummary.get("records");
                                    totalPage = (int) compareEnterpriseSummary.get("totalPages");
                                    compareCount=(long) compareEnterpriseSummary.get("compareRecord");
                                    if (i > 0) {
                                        compareEnterpriseSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareEnterpriseSummaryValue.get("mismatchRows"));
                                        compareEnterpriseSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareEnterpriseSummaryValue.get("matchedRows"));
                                        compareEnterpriseSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + compareEnterpriseSummaryValue.get("misMatchFieldCount"));
                                    } else {
                                        compareEnterpriseSummaryList.putAll(compareEnterpriseSummaryValue);
                                    }
                                    page++;
                                }
                            } catch (Exception e) {
                                log.info("Exception occured in getEnterpriseSummaryReport");
                                e.printStackTrace();
                            }
                        }
                        if(totalCount !=0 && compareCount !=0) {
                            compareEnterpriseSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
                        }else{
                            compareEnterpriseSummaryList.put("totalFieldsCompared", 0L);
                        }
                                long matchedFieldsCount = compareEnterpriseSummaryList.get("totalFieldsCompared") - compareEnterpriseSummaryList.get("misMatchFieldCount");
                                compareEnterpriseSummaryList.put("matchedFieldsCount", matchedFieldsCount);
                                return compareEnterpriseSummaryList;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        return futureOutput.get();
    }

    public void excludeProperties(ObjectDifferBuilder objectDifferBuilder) {
        objectDifferBuilder.inclusion().exclude().propertyName("id");
        objectDifferBuilder.inclusion().exclude().propertyName("sysDate");
        objectDifferBuilder.inclusion().exclude().propertyName("jobId");
        objectDifferBuilder.inclusion().exclude().propertyName("reportDate");
        objectDifferBuilder.inclusion().exclude().propertyName("mxReportDate");  // for STP only
        objectDifferBuilder.inclusion().exclude().propertyName("groupLabel");  // for ospright teplate
        objectDifferBuilder.inclusion().exclude().propertyName("ospRightTemplate");  // for portfolio
        objectDifferBuilder.inclusion().exclude().propertyName("templateLabel");  // for chinesewall
        objectDifferBuilder.inclusion().exclude().propertyName("label"); //for enterprise
        objectDifferBuilder.inclusion().exclude().propertyName("usrGroup");//for group combine portfolio
        objectDifferBuilder.inclusion().exclude().propertyName("consistencyTmpl");//for consistency
        objectDifferBuilder.inclusion().exclude().propertyName("globalTem");//for stp
        objectDifferBuilder.inclusion().exclude().propertyName("template");//for finance
        objectDifferBuilder.inclusion().exclude().propertyName("tmplType");//for finance
        objectDifferBuilder.inclusion().exclude().propertyName("operRgts");//operation

    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupCombinePortfolioDetailsSummaryReportDetails(GeneralSpecification filters, String type, UamSummaryReportJob job) throws Exception {
        UamReportSummary summary = new UamReportSummary();
        log.info(" Report Summary: Starting fetch.");
        log.info(" Report Summary: Initializing job status.");
        log.info("Portfolio Rights download job service.Started.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            long totalCount = 0;
            long compareCount = 0;
            int totalPage = 1;
            int pageSize = 500;
            int page = 0;
            int batchSize = 5;
            Page<MxGroupCombinedPortfolio> additionalPortfolioRightsList;
            Page<MxGroupCombinedPortfolio> missingPortfolioRightsList;
            Map<String, Long> comparePortfolioSummaryValue = new HashMap<>();
            long reportFieldsCnt = MxGroupCombinedPortfolio.class.getDeclaredFields().length - excludeCount;
            Map<String, Long> comparePortfolioSummaryList = new HashMap<>();

            if (filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                comparePortfolioSummaryList.put("mismatchRows", 0L);
                comparePortfolioSummaryList.put("matchedRows", 0L);
                comparePortfolioSummaryList.put("misMatchFieldCount", 0L);
                comparePortfolioSummaryList.put("matchedFieldsCount", 0L);
                comparePortfolioSummaryList.put("totalFieldsCompared", 0L);
                comparePortfolioSummaryList.put("availableRows", 0L);
                comparePortfolioSummaryList.put("missingRows", 0L);
                downloadJobService.updateJobProgress("FETCH_COMPLETE", comparePortfolioSummaryList.size(), comparePortfolioSummaryList.size(), job.getId());
                reportJobService.updateJob(job.getId(), new JSONObject(comparePortfolioSummaryList).toString(), true);
                return CompletableFuture.completedFuture(null);
            } else {
                try {
                    for (int i = 0; i < totalPage; i++) {
                        Document comparePortfolioSummary = compareGroupCombinePortfolioRights(page, pageSize, filters, type);
                        comparePortfolioSummaryValue.putAll((Map<? extends String, ? extends Long>) comparePortfolioSummary.get("content"));
                        totalCount = (long) comparePortfolioSummary.get("records");
                        compareCount = (long) comparePortfolioSummary.get("compareRecords");
                        totalPage = (int) comparePortfolioSummary.get("totalPages");
                        if (i > 0) {
                            comparePortfolioSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + comparePortfolioSummaryValue.get("mismatchRows"));
                            comparePortfolioSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + comparePortfolioSummaryValue.get("matchedRows"));
                            comparePortfolioSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + comparePortfolioSummaryValue.get("misMatchFieldCount"));
                        } else {
                            comparePortfolioSummaryList.putAll(comparePortfolioSummaryValue);
                        }
                        page++;

                    }
                } catch (Exception e) {
                    log.info("Exception occured in getPortfolio SummaryReport");
                    e.printStackTrace();
                }
            }
            reportJobService.updateJobProgress("FETCH IN PROGRESS", summary.getClass().getDeclaredFields().length, comparePortfolioSummaryList.size(), job.getId());
            LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
            additionalPortfolioRightsList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioAdditionalValues(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                    PageRequest.of(0, pageSize));
            long additionalTotalRows = additionalPortfolioRightsList.getTotalElements();
            missingPortfolioRightsList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioAdditionalValues(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                    PageRequest.of(0, pageSize));
            long missingTotalRows = missingPortfolioRightsList.getTotalElements();
            if (totalCount != 0 && compareCount != 0) {
                comparePortfolioSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
            } else {
                comparePortfolioSummaryList.put("totalFieldsCompared", 0L);
            }
            long matchedFieldsCount = comparePortfolioSummaryList.get("totalFieldsCompared") - comparePortfolioSummaryList.get("misMatchFieldCount");
            comparePortfolioSummaryList.put("availableRows", additionalTotalRows);
            comparePortfolioSummaryList.put("missingRows", missingTotalRows);
            comparePortfolioSummaryList.put("matchedFieldsCount", matchedFieldsCount);
            log.info("Portfolio Fetch completed");
            reportJobService.updateJobProgress("FETCH COMPLETED", summary.getClass().getDeclaredFields().length, comparePortfolioSummaryList.size(), job.getId());

            //updating job with generated summary
            reportJobService.updateJob(job.getId(), new JSONObject(comparePortfolioSummaryList).toString(), true);

        } catch(Exception e){
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupCombinePortfolioDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                                            DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info(filters.toString());
        List<MxGroupCombinedPortfolio> portfolioLabels = new ArrayList<>();
        Page<MxGroupCombinedPortfolio> additionalPortfolioRightsList = null;
        Page<MxGroupCombinedPortfolio> missingPortfolioRightsList = null;
        int pageSize = 50;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info(" Export: Starting fetch.");
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            try {
                if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document portfolioLabel = compareGroupCombinePortfolioRights(page, pageSize, filters, type);
                        portfolioLabels.addAll((List<MxGroupCombinedPortfolio>) portfolioLabel.get("content"));
                        totalPage = (int) portfolioLabel.get("totalPages");
                        totalCount = Long.valueOf(String.valueOf(portfolioLabel.get("records")));
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
                        page++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // additional
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
                int additionalTotalPage = 1;
                for (int i = 0; i < additionalTotalPage; i++) {
                    additionalPortfolioRightsList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioAdditionalValues(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                            PageRequest.of(additionalPage, pageSize));
                    additionalPortfolioRightsList.forEach(c -> c.setDiff("Additonal row " + filters.getTemplateValue()));
                    portfolioLabels.addAll(additionalPortfolioRightsList.getContent());
                    additionalTotalPage = additionalPortfolioRightsList.getTotalPages();
                    totalCount = additionalPortfolioRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
                    additionalPage++;
                }
            }
            // missing
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
                int missingTotalPage = 1;
                for (int i = 0; i < missingTotalPage; i++) {
                    missingPortfolioRightsList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioAdditionalValues(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                            PageRequest.of(missingPage, pageSize));
                    missingPortfolioRightsList.forEach(c -> c.setDiff("Missing row " + filters.getTemplateValue()));
                    portfolioLabels.addAll(missingPortfolioRightsList.getContent());
                    missingTotalPage = missingPortfolioRightsList.getTotalPages();
                    totalCount = missingPortfolioRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
                    missingPage++;
                }
            }
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", portfolioLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, portfolioLabels, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document compareGroupCombinePortfolioRights(int page, int pageSize, GeneralSpecification generalPortfolioRights, String type) {
        Document document = new Document();
        Page<MxGroupCombinedPortfolio> portfolioRightsList = null;
        Page<MxGroupCombinedPortfolio> comparePortfolioRightsList = null;
        List<MxGroupCombinedPortfolio> matchedPortfolioList = new ArrayList<>();
        List<MxGroupCombinedPortfolio> misMatchedPortfolioList = new ArrayList<>();
        Map<String, Long> summaryList = new HashMap<String, Long>();
        LocalDate repDate = LocalDate.parse(generalPortfolioRights.getDateValue(), dateTimeFormatter);
        portfolioRightsList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioMatchedValues(repDate, generalPortfolioRights.getTemplateValue(), generalPortfolioRights.getCompareTemplateValue(),
                PageRequest.of(page, pageSize));
        comparePortfolioRightsList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioMatchedValues(repDate, generalPortfolioRights.getCompareTemplateValue(), generalPortfolioRights.getTemplateValue(),
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        long fieldCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxGroupCombinedPortfolio portfolioRights : portfolioRightsList) {
            StringBuilder message = new StringBuilder("");
            if (comparePortfolioRightsList.getContent().size() > i) {
                MxGroupCombinedPortfolio comparePortfolioRight = comparePortfolioRightsList.getContent().get(i);
                if (comparePortfolioRight != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(portfolioRights, comparePortfolioRight);
                    if (diff1.hasChanges()) {
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                final Object oldValue = node.canonicalGet(portfolioRights);
                                final Object newValue = node.canonicalGet(comparePortfolioRight);
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append("- ").append(node.getPropertyName());

                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        message.append("Mis Matched row");
                        portfolioRights.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedPortfolioList.add(portfolioRights);
                    } else {
                        message.append("Matched row");
                        portfolioRights.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedPortfolioList.add(portfolioRights);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", portfolioRightsList.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            List<MxGroupCombinedPortfolio> allPortfolioList = new ArrayList<>();
            allPortfolioList.addAll(matchedPortfolioList);
            allPortfolioList.addAll(misMatchedPortfolioList);
            document.put("records", allPortfolioList.size());
            document.put("content", allPortfolioList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedPortfolioList.size());
            document.put("content", matchedPortfolioList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedPortfolioList.size());
            document.put("content", misMatchedPortfolioList);
        } else if (type.equalsIgnoreCase("summary")) {
            document.put("records", portfolioRightsList.getTotalElements());
            document.put("compareRecords", comparePortfolioRightsList.getTotalElements());
            document.put("content", summaryList);
        } else {
            document.put("records", portfolioRightsList.getTotalElements());
            document.put("content", portfolioRightsList.getContent());
        }
        return document;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getNavigationDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info(filters.toString());
        List<MxGroupNavigationRight> navigationLabels = new ArrayList<>();
        Page<MxGroupNavigationRight> additionalNavigationList = null;
        Page<MxGroupNavigationRight> missingNavigationList = null;
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;

        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info(" Export: Starting fetch.");
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                if((!(filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")))
                        && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document navigationlabel = compareNavigation(page, pageSize, filters, type);
                        navigationLabels.addAll((List<MxGroupNavigationRight>) navigationlabel.get("content"));
                        totalPage = (int) navigationlabel.get("totalPages");
                        totalCount = Long.valueOf(String.valueOf(navigationlabel.get("records")));
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, navigationLabels.size(), job.getId());
                        page++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            // additional
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
                int additionalTotalPage = 1;
                for (int i = 0; i < additionalTotalPage; i++) {
                    additionalNavigationList = mxGroupNavigationRightsRepository.findAllNavigationGroupCompareAdditionalExport(repDate, filters.getGroupName(), filters.getCompareGroupName(), filters.getTemplateValue(), filters.getCompareTemplateValue(),
                            PageRequest.of(additionalPage, pageSize));
                    additionalNavigationList.forEach(c -> c.setDiff("Additonal row " + filters.getGroupName()));
                    navigationLabels.addAll(additionalNavigationList.getContent());
                    additionalTotalPage = additionalNavigationList.getTotalPages();
                    totalCount = additionalNavigationList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, navigationLabels.size(), job.getId());
                    additionalPage++;
                }
            }
            // missing
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
                int missingTotalPage = 1;
                for (int i = 0; i < missingTotalPage; i++) {
                    missingNavigationList = mxGroupNavigationRightsRepository.findAllNavigationGroupCompareAdditionalExport(repDate, filters.getCompareGroupName(), filters.getGroupName(), filters.getCompareTemplateValue(), filters.getTemplateValue(),
                            PageRequest.of(missingPage, pageSize));
                    missingNavigationList.forEach(c -> c.setDiff("Missing row " + filters.getGroupName()));
                    navigationLabels.addAll(missingNavigationList.getContent());
                    missingTotalPage = missingNavigationList.getTotalPages();
                    totalCount = missingNavigationList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, navigationLabels.size(), job.getId());
                    missingPage++;
                }
            }
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", navigationLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, navigationLabels, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document compareNavigation(int page, int pageSize, GeneralSpecification filters, String type) {
        Document document = new Document();
        Page<MxGroupNavigationRight> navigationList ;
        Page<MxGroupNavigationRight> compareNavigationList ;
        List<MxGroupNavigationRight> matchedNavigationList = new ArrayList<>() ;
        List<MxGroupNavigationRight> misMatchedNavigationList = new ArrayList<>() ;
        Map<String, Long> summaryList=new HashMap<>();
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        navigationList = mxGroupNavigationRightsRepository.findNavigationByGroupLabelsAndReportDate(filters.getGroupName(), filters.getCompareGroupName(), filters.getTemplateValue(), repDate,
                PageRequest.of(page, pageSize), filters.getCompareTemplateValue());
        compareNavigationList = mxGroupNavigationRightsRepository.findNavigationByGroupLabelsAndReportDate(filters.getCompareGroupName(), filters.getGroupName(), filters.getCompareTemplateValue(), repDate,
                PageRequest.of(page, pageSize), filters.getTemplateValue());
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxGroupNavigationRight navigationValue : navigationList) {
            StringBuilder message = new StringBuilder("");
            if (compareNavigationList.getContent().size() > i) {
                MxGroupNavigationRight compareNavigation = compareNavigationList.getContent().get(i);
                if (compareNavigation != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(navigationValue, compareNavigation);
                    if (diff1.hasChanges()) {
                        message.append("Mis Matched row");
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        navigationValue.setDiff(String.valueOf(message));
                        misMatchrowCount++ ;
                        misMatchedNavigationList.add(navigationValue);
                    } else {
                        message.append("Matched row");
                        navigationValue.setDiff(String.valueOf(message));
                        matchRowCount++ ;
                        matchedNavigationList.add(navigationValue);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", navigationList.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            document.put("records", navigationList.getTotalElements());
            document.put("content", navigationList.getContent());
        } else if(type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedNavigationList.size());
            document.put("content", matchedNavigationList);
        } else if(type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedNavigationList.size());
            document.put("content", misMatchedNavigationList);
        } else if(type.equalsIgnoreCase("summary")) {
            document.put("records", navigationList.getTotalElements());
            document.put("compareRecords", compareNavigationList.getTotalElements());
            document.put("content", summaryList);
        } else {
            document.put("records", navigationList.getTotalElements());
            document.put("content", navigationList.getContent());
        }
        return document;
    }

    @Async
    public CompletableFuture<Void> getNavigationDetailsSummaryReport(GeneralSpecification filters, String type,UamSummaryReportJob job) throws ExecutionException, InterruptedException {
        UamReportSummary summary = new UamReportSummary();
        log.info(" Report Summary: Starting fetch.");
        log.info(" Report Summary: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            long totalCount = 0;
            long compareCount=0;
            int totalPage = 1;
            int pageSize = 500;
            int page = 0;
            int batchSize = 5;
            Page<MxGroupNavigationRight> additionalNavigationList;
            Page<MxGroupNavigationRight> missingNavigationList;
            Map<String, Long> compareNavigationSummaryValue = new HashMap<>();
            long reportFieldsCnt = MxGroupNavigationRight.class.getDeclaredFields().length - excludeCount;
            Map<String, Long> compareNavigationSummaryList = new HashMap<>();
            if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))
                    ||filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                compareNavigationSummaryList.put("mismatchRows", 0L);
                compareNavigationSummaryList.put("matchedRows", 0L);
                compareNavigationSummaryList.put("misMatchFieldCount", 0L);
                compareNavigationSummaryList.put("availableRows", 0L);
                compareNavigationSummaryList.put("missingRows", 0L);
                compareNavigationSummaryList.put("matchedFieldsCount", 0L);
                compareNavigationSummaryList.put("totalFieldsCompared", 0L);
                log.info("Navigation Rights download job service.fetch complete.");
                downloadJobService.updateJobProgress("FETCH_COMPLETE", compareNavigationSummaryList.size(), compareNavigationSummaryList.size(), job.getId());
                reportJobService.updateJob(job.getId(), new JSONObject(compareNavigationSummaryList).toString(), true);
                return CompletableFuture.completedFuture(null);
            } else if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
                compareNavigationSummaryList.put("mismatchRows", 0L);
                compareNavigationSummaryList.put("matchedRows", 0L);
                compareNavigationSummaryList.put("misMatchFieldCount", 0L);
                compareNavigationSummaryList.put("matchedFieldsCount", 0L);
            }else {
                try {
                    for (int i = 0; i < totalPage; i++) {
                        Document compareNavigationSummary = compareNavigation(page, pageSize, filters, type);
                        compareNavigationSummaryValue.putAll((Map<? extends String, ? extends Long>) compareNavigationSummary.get("content"));
                        totalCount = (long) compareNavigationSummary.get("records");
                        totalPage = (int) compareNavigationSummary.get("totalPages");
                        compareCount = (long) compareNavigationSummary.get("compareRecords");
                        if (i > 0) {
                            compareNavigationSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareNavigationSummaryValue.get("mismatchRows"));
                            compareNavigationSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareNavigationSummaryValue.get("matchedRows"));
                            compareNavigationSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + compareNavigationSummaryValue.get("misMatchFieldCount"));
                        } else {
                            compareNavigationSummaryList.putAll(compareNavigationSummaryValue);
                        }
                        page++;

                    }
                } catch (Exception e) {
                    log.info("Exception occured in getNavigationSummaryReport");
                    e.printStackTrace();
                }
            }
                reportJobService.updateJobProgress("FETCH IN PROGRESS", summary.getClass().getDeclaredFields().length, compareNavigationSummaryList.size(), job.getId());
                LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
                additionalNavigationList = mxGroupNavigationRightsRepository.findAllNavigationGroupCompareAdditionalExport(repDate, filters.getGroupName(), filters.getCompareGroupName(), filters.getTemplateValue(), filters.getCompareTemplateValue(),
                        PageRequest.of(0, pageSize));
                long additionalTotalRows = additionalNavigationList.getTotalElements();
                missingNavigationList = mxGroupNavigationRightsRepository.findAllNavigationGroupCompareAdditionalExport(repDate, filters.getCompareGroupName(), filters.getGroupName(), filters.getCompareTemplateValue(), filters.getTemplateValue(),
                        PageRequest.of(0, pageSize));
                long missingTotalRows = missingNavigationList.getTotalElements();
                if(totalCount >0 && compareCount>0) {
                    compareNavigationSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
                }else{
                    compareNavigationSummaryList.put("totalFieldsCompared", 0L);
                }
                long matchedFieldsCount = compareNavigationSummaryList.get("totalFieldsCompared") - compareNavigationSummaryList.get("misMatchFieldCount");
                compareNavigationSummaryList.put("availableRows", additionalTotalRows);
                compareNavigationSummaryList.put("missingRows", missingTotalRows);
                compareNavigationSummaryList.put("matchedFieldsCount", matchedFieldsCount);
                log.info("Navigation Fetch completed");
                reportJobService.updateJobProgress("FETCH COMPLETED", summary.getClass().getDeclaredFields().length, compareNavigationSummaryList.size(), job.getId());

                //updating job with generated summary
                reportJobService.updateJob(job.getId(), new JSONObject(compareNavigationSummaryList).toString(), true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public Map<String, Long> getConsistencyDetailsSummaryReport(GeneralSpecification filters, String type) throws ExecutionException, InterruptedException {
        CompletableFuture<Map<String, Long>> futureOutput =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return getGroupConsistencyDetailsSummaryReportDetails(filters, type);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        return futureOutput.get();
    }

    public Map<String, Long> getGroupConsistencyDetailsSummaryReportDetails(GeneralSpecification filters, String type) throws Exception {
        long totalCount = 0;
        long compareCount=0;
        int totalPage = 1;
        int pageSize = 50;
        int page = 0;
        Page<MxConsistencyTmpl> additionalConsistencyList;
        Page<MxConsistencyTmpl> missingConsistencyList;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        Map<String, Long> compareConsistencySummaryValue = new HashMap<String, Long>();
        long reportFieldsCnt = MxConsistencyTmpl.class.getDeclaredFields().length - excludeCount;
        Map<String, Long> compareConsistencySummaryList = new HashMap<String, Long>();
        if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))||
                filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
            compareConsistencySummaryList.put("mismatchRows", 0L);
            compareConsistencySummaryList.put("matchedRows", 0L);
            compareConsistencySummaryList.put("misMatchFieldCount", 0L);
            compareConsistencySummaryList.put("availableRows", 0L);
            compareConsistencySummaryList.put("missingRows", 0L);
            compareConsistencySummaryList.put("matchedFieldsCount", 0L);
            compareConsistencySummaryList.put("totalFieldsCompared", 0L);
            log.info("Consistency Rights download job service.fetch complete.");
        } else if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
            compareConsistencySummaryList.put("mismatchRows", 0L);
            compareConsistencySummaryList.put("matchedRows", 0L);
            compareConsistencySummaryList.put("misMatchFieldCount", 0L);
            compareConsistencySummaryList.put("matchedFieldsCount", 0L);
        }else {
            try {
                for (int i = 0; i < totalPage; i++) {
                    Document compareConsistencySummary = compareGroupConsistency(page, pageSize, filters, type);
                    compareConsistencySummaryValue.putAll((Map<String, Long>) compareConsistencySummary.get("content"));
                    totalCount = (long) compareConsistencySummary.get("records");
                    totalPage = (int) compareConsistencySummary.get("totalPages");
                    compareCount = (long) compareConsistencySummary.get("compareRecords");
                    if (i > 0) {
                        compareConsistencySummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareConsistencySummaryValue.get("mismatchRows"));
                        compareConsistencySummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareConsistencySummaryValue.get("matchedRows"));
                    } else {
                        compareConsistencySummaryList.putAll(compareConsistencySummaryValue);
                    }
                    page++;
                }
            } catch (Exception e) {
                log.info("Exception occured in getConsistencyDetailsSummaryReport");
                e.printStackTrace();
            }
        }
            additionalConsistencyList = mxConsistencyTemplateRepository.findAllGroupCompareConsistencyAdditionalList(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                    PageRequest.of(0, pageSize));
            long additionalTotalRows = additionalConsistencyList.getTotalElements();
            missingConsistencyList = mxConsistencyTemplateRepository.findAllGroupCompareConsistencyAdditionalList(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                    PageRequest.of(0, pageSize));
            long missingTotalRows = missingConsistencyList.getTotalElements();
            if(totalCount >0 && compareCount >0) {
                compareConsistencySummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
            }else{
                compareConsistencySummaryList.put("totalFieldsCompared", 0L);
            }
            long matchedFieldsCount = compareConsistencySummaryList.get("totalFieldsCompared") - compareConsistencySummaryList.get("misMatchFieldCount");
            compareConsistencySummaryList.put("availableRows", additionalTotalRows);
            compareConsistencySummaryList.put("missingRows", missingTotalRows);
            compareConsistencySummaryList.put("matchedFieldsCount", matchedFieldsCount);

        return compareConsistencySummaryList;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupConsistencyAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                                DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info(filters.toString());
        List<MxConsistencyTmpl> consistencyTmplList = new ArrayList<>();
        Page<MxConsistencyTmpl> additionalConsistencyRightsList = null;
        Page<MxConsistencyTmpl> missingConsistencyRightsList = null;
        int pageSize = 50;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info(" Export: Starting fetch.");
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                if ((!(filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")))
                        && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document consistency = compareGroupConsistency(page, pageSize, filters, type);
                        consistencyTmplList.addAll((List<MxConsistencyTmpl>) consistency.get("content"));
                        totalPage = (int) consistency.get("totalPages");
                        totalCount = Long.valueOf(String.valueOf(consistency.get("records")));
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, consistencyTmplList.size(), job.getId());
                        page++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            // additional
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
                int additionalTotalPage = 1;
                for (int i = 0; i < additionalTotalPage; i++) {
                    additionalConsistencyRightsList = mxConsistencyTemplateRepository.findAllGroupCompareConsistencyAdditionalList(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(),
                            PageRequest.of(additionalPage, pageSize));
                    additionalConsistencyRightsList.forEach(c -> c.setDiff("Additonal row " + filters.getTemplateValue()));
                    consistencyTmplList.addAll(additionalConsistencyRightsList.getContent());
                    additionalTotalPage = additionalConsistencyRightsList.getTotalPages();
                    totalCount = additionalConsistencyRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, consistencyTmplList.size(), job.getId());
                    additionalPage++;
                }
            }
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
                // missing
                int missingTotalPage = 1;
                for (int i = 0; i < missingTotalPage; i++) {
                    missingConsistencyRightsList = mxConsistencyTemplateRepository.findAllGroupCompareConsistencyAdditionalList(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(),
                            PageRequest.of(missingPage, pageSize));
                    missingConsistencyRightsList.forEach(c -> c.setDiff("Missing row " + filters.getTemplateValue()));
                    consistencyTmplList.addAll(missingConsistencyRightsList.getContent());
                    missingTotalPage = missingConsistencyRightsList.getTotalPages();
                    totalCount = missingConsistencyRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, consistencyTmplList.size(), job.getId());
                    missingPage++;
                }
            }
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", consistencyTmplList.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, consistencyTmplList, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document compareGroupConsistency(int page, int pageSize, GeneralSpecification generalConsistencyRights,String type) {
        Document document = new Document();
        Page<MxConsistencyTmpl> mxConsistencyTmpls = null;
        Page<MxConsistencyTmpl> compareConsistencyRightsList = null ;
        List<MxConsistencyTmpl> matchedConsistencyList = new ArrayList<>() ;
        List<MxConsistencyTmpl> misMatchedConsistencyList = new ArrayList<>() ;
        Map<String, Long> summaryList=new HashMap<String, Long>();
        LocalDate repDate = LocalDate.parse(generalConsistencyRights.getDateValue(), dateTimeFormatter);
        mxConsistencyTmpls = mxConsistencyTemplateRepository.findAllGroupCompareConsistencyMatchedList(repDate,  generalConsistencyRights.getTemplateValue(),generalConsistencyRights.getCompareTemplateValue(),
                PageRequest.of(page, pageSize));
        compareConsistencyRightsList = mxConsistencyTemplateRepository.findAllGroupCompareConsistencyMatchedList(repDate, generalConsistencyRights.getCompareTemplateValue(), generalConsistencyRights.getTemplateValue(),
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        long fieldCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxConsistencyTmpl consistencyRights : mxConsistencyTmpls) {
            StringBuilder message = new StringBuilder("");
            if (compareConsistencyRightsList.getContent().size() > i) {
                MxConsistencyTmpl compareConsistency = compareConsistencyRightsList.getContent().get(i);
                if (compareConsistency != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(consistencyRights, compareConsistency);
                    if (diff1.hasChanges()) {
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                final Object oldValue = node.canonicalGet(consistencyRights);
                                final Object newValue = node.canonicalGet(compareConsistency);
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append("- ").append(node.getPropertyName());

                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        consistencyRights.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedConsistencyList.add(consistencyRights);
                    } else {
                        consistencyRights.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedConsistencyList.add(consistencyRights);
                    }
                }
            }
            //set the difference
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", mxConsistencyTmpls.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            document.put("records", mxConsistencyTmpls.getTotalElements());
            document.put("content", mxConsistencyTmpls.getContent());
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedConsistencyList.size());
            document.put("content", matchedConsistencyList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedConsistencyList.size());
            document.put("content", misMatchedConsistencyList);
        } else if (type.equalsIgnoreCase("summary")) {
            document.put("records", mxConsistencyTmpls.getTotalElements());
            document.put("compareRecords", compareConsistencyRightsList.getTotalElements());
            document.put("content", summaryList);
        } else {
            document.put("records", mxConsistencyTmpls.getTotalElements());
            document.put("content", mxConsistencyTmpls.getContent());
        }
        return document;
    }

//    @Async
//   @Transactional
//    public CompletableFuture<Void> getStpDetailsSummaryReport(GeneralSpecification filters, String type,UamSummaryReportJob job,LocalDate repDate) throws ExecutionException, InterruptedException {
//        UamReportSummary summary = new UamReportSummary();
//        log.info(" Report Summary: Starting fetch.");
//        log.info(" Report Summary: Initializing job status.");
//        ConcurrentHashMap<String, Long> resultedMap = new ConcurrentHashMap<>();
//        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
//        int pageSize = 500;
//        if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))
//        || filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
//            resultedMap.put("mismatchRows", 0L);
//            resultedMap.put("matchedRows", 0L);
//            resultedMap.put("misMatchFieldCount", 0L);
//            resultedMap.put("availableRows", 0L);
//            resultedMap.put("missingRows", 0L);
//            resultedMap.put("matchedFieldsCount", 0L);
//            resultedMap.put("totalFieldsCompared", 0L);
//            log.info("Stp Rights download job service.fetch complete.");
//            downloadJobService.updateJobProgress("FETCH_COMPLETE", resultedMap.size(), resultedMap.size(), job.getId());
//            reportJobService.updateJob(job.getId(), new JSONObject(resultedMap).toString(), true);
//            return CompletableFuture.completedFuture(null);
//        } else if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
//            resultedMap.put("mismatchRows", 0L);
//            resultedMap.put("matchedRows", 0L);
//            resultedMap.put("misMatchFieldCount", 0L);
//            resultedMap.put("matchedFieldsCount", 0L);
//            resultedMap.put("totalFieldsCompared", 0L);
//        }else {
//            try {
//                Page<STPRightsMatrix> tempList = stpRightsRepository.findAllMatchedStp(filters.getTemplateValue(), filters.getCompareTemplateValue(), repDate,
//                        PageRequest.of(0, 1));
//                Page<STPRightsMatrix> compareTempList = stpRightsRepository.findAllMatchedStp(filters.getCompareTemplateValue(), filters.getTemplateValue(), repDate,
//                        PageRequest.of(0, 1));
//
//                int totalPage = tempList.getTotalPages();
//                long totalCount = tempList.getTotalElements();
//
//                ExecutorService executorService = Executors.newFixedThreadPool(threadValue);// TBD for strategy
//                CompletableFuture<ConcurrentHashMap<String, Long>>[] futures = new CompletableFuture[totalPage];
//                for (int page = 0; page < totalPage; page++) {
//                    final int finalPage = page;
//                    futures[page] = CompletableFuture
//                            .supplyAsync(() -> compareGroupStp(finalPage, pageSize, filters, type, repDate), executorService)
//                            .thenApply(document -> {
//                                return constructMapFromDocumentForStp(document);
//                            });
//                }
//
//                CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
//                CompletableFuture<ConcurrentHashMap<String, Long>> allPageContentsFuture = allFutures.thenApply(result -> {
//                    ConcurrentHashMap<String, Long> tempMap = new ConcurrentHashMap<>();
//                    tempMap.put("mismatchRows", 0L);
//                    tempMap.put("matchedRows", 0L);
//                    tempMap.put("misMatchFieldCount", 0L);
//
//                    return Arrays.asList(futures).stream()
//                            .map(pageContentFuture -> pageContentFuture.join())
//                            .reduce((oldMap, newMap) -> {
//                                oldMap.computeIfPresent("mismatchRows", (key, newValue) -> newValue + newMap.get("mismatchRows"));
//                                oldMap.computeIfPresent("matchedRows", (key, newValue) -> newValue + newMap.get("matchedRows"));
//                                oldMap.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + newMap.get("misMatchFieldCount"));
//                                return oldMap;
//                            }).orElse(tempMap);
//                });
//                resultedMap = allPageContentsFuture.get();
//
//                reportJobService.updateJobProgress("FETCH IN PROGRESS", summary.getClass().getDeclaredFields().length, resultedMap.size(), job.getId());
//
//                long reportFieldsCnt = STPRightsMatrix.class.getDeclaredFields().length - excludeCount;
//                if (compareTempList.getTotalElements() > 0 && totalCount > 0) {
//                    resultedMap.put("totalFieldsCompared", totalCount * reportFieldsCnt);
//                } else {
//                    resultedMap.put("totalFieldsCompared", 0L);
//                }
//                long matchedFieldsCount = resultedMap.get("totalFieldsCompared") - resultedMap.get("misMatchFieldCount");
//                resultedMap.put("matchedFieldsCount", matchedFieldsCount);
//            } catch (Exception e) {
//                log.info("Exception occured in getStpDetailsSummaryReport");
//                e.printStackTrace();
//            }
//        }
//            calculateMissingAndAvailableRowsInStp(filters, pageSize, resultedMap, repDate);
//            reportJobService.updateJobProgress("FETCH COMPLETES", summary.getClass().getDeclaredFields().length, resultedMap.size(), job.getId());
//            reportJobService.updateJob(job.getId(), new JSONObject(resultedMap).toString(), true);
//        return CompletableFuture.completedFuture(null);
//    }
//
//    private ConcurrentHashMap<String, Long> constructMapFromDocumentForStp(Document compareStpSummary) {
//        ConcurrentHashMap<String, Long> compareSummaryMap = new ConcurrentHashMap<>();
//        compareSummaryMap.putAll((Map<? extends String, ? extends Long>) compareStpSummary.get("content"));
//        return compareSummaryMap;
//    }
//
//    private void calculateMissingAndAvailableRowsInStp(GeneralSpecification filters, int pageSize, ConcurrentHashMap<String,Long> resultedMap,LocalDate repDate){
//        Page<STPRightsMatrix> additionalStpRightsList = stpRightsRepository.findAllAdditionalStp(filters.getTemplateValue(), filters.getCompareTemplateValue(), repDate,
//                PageRequest.of(0, 1));
//        long additionalTotalRows = additionalStpRightsList.getTotalElements();
//        Page<STPRightsMatrix> missingStpRightsList = stpRightsRepository.findAllAdditionalStp(filters.getCompareTemplateValue(), filters.getTemplateValue(), repDate,
//                PageRequest.of(0, 1));
//        long missingTotalRows = missingStpRightsList.getTotalElements();
//        resultedMap.put("availableRows", additionalTotalRows);
//        resultedMap.put("missingRows", missingTotalRows);
//    }
//
//    @Async
//    @Transactional
//    public CompletableFuture<Void> getGroupStpAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,LocalDate repDate,
//                                                        DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
//        log.info(filters.toString());
//        List<STPRightsMatrix> stpRightTmplList = new ArrayList<>();
//        Page<STPRightsMatrix> additionalStpRightsList = null;
//        Page<STPRightsMatrix> missingStpRightsList = null;
//        int pageSize = 500;
//        int page = 0;
//        long totalCount = 0;
//        int totalPage = 1;
//        int additionalPage = 0;
//        int missingPage = 0;
//        log.info(" Export: Starting fetch.");
//        log.info(" Export: Initializing job status.");
//        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
//        try {
//            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
//                if((!(filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")))
//                        && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
//                    for (int i = 0; i < totalPage; i++) {
//                        Document stpRights = compareGroupStp(page, pageSize, filters, type, repDate);
//                        stpRightTmplList.addAll((List<STPRightsMatrix>) stpRights.get("content"));
//                        totalPage = (int) stpRights.get("totalPages");
//                        totalCount = Long.valueOf(String.valueOf(stpRights.get("records")));
//                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpRightTmplList.size(), job.getId());
//                        page++;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
//            // additional
//            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
//                int additionalTotalPage = 1;
//                for (int i = 0; i < additionalTotalPage; i++) {
//                    additionalStpRightsList = stpRightsRepository.findAllAdditionalStp(filters.getTemplateValue(), filters.getCompareTemplateValue(), repDate,
//                            PageRequest.of(additionalPage, pageSize));
//                    additionalStpRightsList.forEach(c -> c.setDiff("Additonal row " + filters.getTemplateValue()));
//                    stpRightTmplList.addAll(additionalStpRightsList.getContent());
//                    additionalTotalPage = additionalStpRightsList.getTotalPages();
//                    totalCount = additionalStpRightsList.getTotalElements();
//                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpRightTmplList.size(), job.getId());
//                    additionalPage++;
//                }
//            }
//            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
//                // missing
//                int missingTotalPage = 1;
//                for (int i = 0; i < missingTotalPage; i++) {
//                    missingStpRightsList = stpRightsRepository.findAllAdditionalStp(filters.getCompareTemplateValue(), filters.getTemplateValue(), repDate,
//                            PageRequest.of(missingPage, pageSize));
//                    missingStpRightsList.forEach(c -> c.setDiff("Missing row " + filters.getTemplateValue()));
//                    stpRightTmplList.addAll(missingStpRightsList.getContent());
//                    missingTotalPage = missingStpRightsList.getTotalPages();
//                    totalCount = missingStpRightsList.getTotalElements();
//                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpRightTmplList.size(), job.getId());
//                    missingPage++;
//                }
//            }
//        }
//        log.info(" Export: Fetch complete.");
//        downloadJobService.updateJobProgress("FILE_WRITE", stpRightTmplList.size(), (int) totalCount, job.getId());
//        return viewerExportService.exportFilefromDetails(outputFormat, stpRightTmplList, fileName, fieldMaps, color, job, fieldColumns);
//    }
//
//    @Transactional
//    public Document compareGroupStp(int page, int pageSize, GeneralSpecification generalSpecification, String type,LocalDate repDate) {
//        Document document = new Document();
//        Page<STPRightsMatrix> mxStpRightsTmpls = null;
//        Page<STPRightsMatrix> compareStpRightsList = null;
//        List<STPRightsMatrix> matchedStpList = new ArrayList<>();
//        List<STPRightsMatrix> misMatchedStpList = new ArrayList<>();
//        Map<String, Long> summaryList = new HashMap<String, Long>();
//        mxStpRightsTmpls = stpRightsRepository.findAllMatchedStp(generalSpecification.getTemplateValue(), generalSpecification.getCompareTemplateValue(), repDate,
//                PageRequest.of(page, pageSize));
//        compareStpRightsList = stpRightsRepository.findAllMatchedStp(generalSpecification.getCompareTemplateValue(), generalSpecification.getTemplateValue(), repDate,
//                PageRequest.of(page, pageSize));
//        //compare date from both dates
//        int i = 0;
//        long misMatchrowCount = 0;
//        long matchRowCount = 0;
//        long fieldCount = 0;
//        AtomicLong misMatchFieldCount = new AtomicLong();
//        for (STPRightsMatrix StpRights : mxStpRightsTmpls) {
//            StringBuilder message = new StringBuilder("");
//            if (compareStpRightsList.getContent().size() > i) {
//                STPRightsMatrix compareStpRights = compareStpRightsList.getContent().get(i);
//                if (compareStpRights != null) {
//                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
//                    excludeProperties(objectDifferBuilder);
//                    DiffNode diff1 = objectDifferBuilder.build().compare(StpRights, compareStpRights);
//                    if (diff1.hasChanges()) {
//                        diff1.visit((node, visit) -> {
//                            if (!node.hasChildren()) {
//                                final Object oldValue = node.canonicalGet(StpRights);
//                                final Object newValue = node.canonicalGet(compareStpRights);
//                                if (message.toString().isEmpty() || message.toString().equals(""))
//                                    message.append(node.getPropertyName());
//                                else
//                                    message.append("- ").append(node.getPropertyName());
//
//                                misMatchFieldCount.getAndIncrement();
//                            }
//                        });
//                        StpRights.setDiff(String.valueOf(message));
//                        misMatchrowCount++;
//                        misMatchedStpList.add(StpRights);
//                    } else {
//                        StpRights.setDiff(String.valueOf(message));
//                        matchRowCount++;
//                        matchedStpList.add(StpRights);
//                    }
//                }
//            }
//            //set the difference
//            i++;
//        }
//        summaryList.put("mismatchRows", misMatchrowCount);
//        summaryList.put("matchedRows", matchRowCount);
//        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
//        document.put("totalPages", mxStpRightsTmpls.getTotalPages());
//        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
//            document.put("records", mxStpRightsTmpls.getTotalElements());
//            document.put("content", mxStpRightsTmpls.getContent());
//        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
//            document.put("records", matchedStpList.size());
//            document.put("content", matchedStpList);
//        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
//            document.put("records", misMatchedStpList.size());
//            document.put("content", misMatchedStpList);
//        } else if (type.equalsIgnoreCase("summary")) {
//            document.put("records", mxStpRightsTmpls.getTotalElements());
//            document.put("content", summaryList);
//            document.put("compareRecords", compareStpRightsList.getTotalElements());
//        } else {
//            document.put("records", mxStpRightsTmpls.getTotalElements());
//            document.put("content", mxStpRightsTmpls.getContent());
//        }
//        return document;
//    }
    //Finance export
    @Async
    @Transactional
    public CompletableFuture<Void> getFinanceDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                              DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info("Finance Export filters : {} ", filters.toString());
        List<MxFinaceAcctrlRights> financeRights = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info("Compare Finance Export: Starting fetch.");
        log.info("Compare Finance Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                if((!(filters.getTemplateValue().equalsIgnoreCase("null") || (filters.getCompareTemplateValue().equalsIgnoreCase("null"))))
                        && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document resultList = compareFinanceTemplate(page, pageSize, filters, type);
                        financeRights.addAll((Collection<? extends MxFinaceAcctrlRights>) resultList.get("content"));
                        totalPage = (int) resultList.get("totalPages");
                        totalCount = (long) resultList.get("records");
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, financeRights.size(), job.getId());
                        page++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Finance Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", financeRights.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, financeRights, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document compareFinanceTemplate(int page, int pageSize, GeneralSpecification filters, String type) {
        Document document = new Document();
        Page<MxFinaceAcctrlRights> finaceAcctrlRightsList;
        Page<MxFinaceAcctrlRights> compareFinaceAcctrlRightsList;
        Map<String, Long> summaryList = new HashMap<>();
        List<MxFinaceAcctrlRights> misMatchedFinanceist = new ArrayList<>();
        List<MxFinaceAcctrlRights> matchedFnanceList = new ArrayList<>();
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        finaceAcctrlRightsList = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(filters.getTemplateValue(), filters.getSubTemplateValue(), repDate, PageRequest.of(page, pageSize));
        compareFinaceAcctrlRightsList = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(filters.getCompareTemplateValue(), filters.getSubTemplateValue(), repDate, PageRequest.of(page, pageSize));
        //compare both groups data
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxFinaceAcctrlRights finaceAcctrlRights : finaceAcctrlRightsList) {
            StringBuilder message = new StringBuilder("");
            if (compareFinaceAcctrlRightsList.getContent().size() > i) {
                MxFinaceAcctrlRights compareFinanceRights = compareFinaceAcctrlRightsList.getContent().get(i);
                if (compareFinanceRights != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(finaceAcctrlRights, compareFinanceRights);
                    if (diff1.hasChanges()) {
                        message.append("Mis Matched row");
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        finaceAcctrlRights.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedFinanceist.add(finaceAcctrlRights);
                    } else {
                        message.append("Matched row");
                        finaceAcctrlRights.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedFnanceList.add(finaceAcctrlRights);
                    }
                }
            }
            i++;
        }

        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            if (compareFinaceAcctrlRightsList.getTotalElements() > finaceAcctrlRightsList.getTotalElements()) {
                document.put("totalPages", compareFinaceAcctrlRightsList.getTotalPages());
                document.put("records", compareFinaceAcctrlRightsList.getTotalElements());
                List<MxFinaceAcctrlRights> tempList = new ArrayList<>(finaceAcctrlRightsList.getContent());
                List<MxFinaceAcctrlRights> compareTempList =compareFinaceAcctrlRightsList.getContent().subList(tempList.size(), (int) compareFinaceAcctrlRightsList.getTotalElements());
                compareTempList.forEach(f -> f.setDiff("Missing Row"));
                tempList.addAll(compareTempList);
                document.put("content", tempList);
            } else {
                document.put("records", finaceAcctrlRightsList.getTotalElements());
                document.put("totalPages", finaceAcctrlRightsList.getTotalPages());
                List<MxFinaceAcctrlRights> tempList = new ArrayList<>(compareFinaceAcctrlRightsList.getContent());
                List<MxFinaceAcctrlRights> compareTempList =finaceAcctrlRightsList.getContent().subList(tempList.size(), (int) finaceAcctrlRightsList.getTotalElements());
                compareTempList.forEach(f -> f.setDiff("Additional Row"));
                tempList.addAll(compareTempList);
                document.put("content", tempList);
            }
        }  if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedFnanceList.size());
            document.put("content", matchedFnanceList);
            document.put("totalPages", finaceAcctrlRightsList.getTotalPages());}
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedFinanceist.size());
            document.put("content", misMatchedFinanceist);
            document.put("totalPages", finaceAcctrlRightsList.getTotalPages());
        }  if (type.equalsIgnoreCase("summary")) {
            if (compareFinaceAcctrlRightsList.getTotalElements() > finaceAcctrlRightsList.getTotalElements()) {
                document.put("records", compareFinaceAcctrlRightsList.getTotalElements());
                document.put("compareRecords", compareFinaceAcctrlRightsList.getTotalElements());
                document.put("totalPages", compareFinaceAcctrlRightsList.getTotalPages());
                summaryList.put("availableRows",0L);
                summaryList.put("missingRows", compareFinaceAcctrlRightsList.getTotalElements()-finaceAcctrlRightsList.getTotalElements());
            } else {
                document.put("records", finaceAcctrlRightsList.getTotalElements());
                document.put("compareRecords", compareFinaceAcctrlRightsList.getTotalElements());
                document.put("totalPages", finaceAcctrlRightsList.getTotalPages());
                summaryList.put("availableRows", finaceAcctrlRightsList.getTotalElements()-compareFinaceAcctrlRightsList.getTotalElements());
                summaryList.put("missingRows",0L);
            }

            document.put("content", summaryList);
        }

        return document;
    }

    public Map<String, Long> getFinanceSummaryReport(GeneralSpecification filters, String type) throws ExecutionException, InterruptedException {
        CompletableFuture<Map<String, Long>> futureOutput =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        long totalCount = 0;
                        long compareCount=0;
                        int totalPage = 1;
                        int pageSize = 500;
                        int page = 0;
                        Map<String, Long> compareFinanceSummaryValue = new HashMap<>();
                        long reportFieldsCnt = MxFinaceAcctrlRights.class.getDeclaredFields().length - stpNavexcludeCount;
                        Map<String, Long> compareFinanceSummaryList = new HashMap<>();
                        if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))
                                ||filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                            compareFinanceSummaryList.put("mismatchRows", 0L);
                            compareFinanceSummaryList.put("matchedRows", 0L);
                            compareFinanceSummaryList.put("misMatchFieldCount", 0L);
                            compareFinanceSummaryList.put("availableRows", 0L);
                            compareFinanceSummaryList.put("missingRows", 0L);
                            compareFinanceSummaryList.put("matchedFieldsCount", 0L);
                            compareFinanceSummaryList.put("totalFieldsCompared", 0L);
                            log.info("finance Rights download job service.fetch complete.");
                            return compareFinanceSummaryList;
                        }
                        if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
                            compareFinanceSummaryList.put("mismatchRows", 0L);
                            compareFinanceSummaryList.put("matchedRows", 0L);
                            compareFinanceSummaryList.put("misMatchFieldCount", 0L);
                            compareFinanceSummaryList.put("matchedFieldsCount", 0L);
                            compareFinanceSummaryList.put("availableRows", 0L);
                            compareFinanceSummaryList.put("totalFieldsCompared", 0L);
                        }
                        try {
                            for (int i = 0; i < totalPage; i++) {
                                Document compareFinanceSummary = compareFinanceTemplate(page, pageSize, filters, type);
                                compareFinanceSummaryValue.putAll((Map<? extends String, ? extends Long>) compareFinanceSummary.get("content"));
                                totalCount = (long) compareFinanceSummary.get("records");
                                totalPage = (int) compareFinanceSummary.get("totalPages");
                                compareCount = (long) compareFinanceSummary.get("compareRecords");
                                log.info("Finance Template page : {} content : {} ", i, compareFinanceSummary.get("content"));
                                if (i > 0) {
                                    compareFinanceSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareFinanceSummaryValue.get("mismatchRows"));
                                    compareFinanceSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareFinanceSummaryValue.get("matchedRows"));
                                    compareFinanceSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + compareFinanceSummaryValue.get("misMatchFieldCount"));
                                } else {
                                    compareFinanceSummaryList.putAll(compareFinanceSummaryValue);
                                }
                                page++;
                            }
                        }catch (Exception e) {
                            log.info("Exception occured in getFinnaceSummary");
                            e.printStackTrace();
                        }
                            if(totalCount >0 && compareCount >0) {
                                compareFinanceSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
                            }else{
                                compareFinanceSummaryList.put("totalFieldsCompared", 0L);
                            }
                            long matchedFieldsCount = compareFinanceSummaryList.get("totalFieldsCompared") - compareFinanceSummaryList.get("misMatchFieldCount");
                            compareFinanceSummaryList.put("matchedFieldsCount", matchedFieldsCount);
                        return compareFinanceSummaryList;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        return futureOutput.get();
    }
    @Async
    @Transactional
    public CompletableFuture<Void> getConfigurationDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.debug("Configuration Export filters : {} ", filters.toString());
        List<MxCwtConfigMgtRight> configLabels = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info("Compare Configuration Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            try {
                if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document configTemp = compareConfigurationTemplate(page, pageSize, filters, type);
                        configLabels.addAll((Collection<? extends MxCwtConfigMgtRight>) configTemp.get("content"));
                        totalPage = (int) configTemp.get("totalPages");
                        totalCount = Long.parseLong(String.valueOf(configTemp.get("records")));
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, configLabels.size(), job.getId());
                        page++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("Configuration Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", configLabels.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, configLabels, fileName, fieldMaps, color, job, fieldColumns);
    }
    @Transactional
    public Document compareConfigurationTemplate(int page, int pageSize, GeneralSpecification filters, String type) {
        Document document = new Document();
        Page<MxCwtConfigMgtRight> configurationList;
        Page<MxCwtConfigMgtRight> compareConfigurationList;
        Map<String, Long> summaryList = new HashMap<>();
        List<MxCwtConfigMgtRight> misMatchedConfigurationList = new ArrayList<>();
        List<MxCwtConfigMgtRight> matchedConfigurationList = new ArrayList<>();
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        configurationList = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(filters.getTemplateValue(), repDate,
                PageRequest.of(page, pageSize));
        compareConfigurationList = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(filters.getCompareTemplateValue(), repDate,
                PageRequest.of(page, pageSize));

        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxCwtConfigMgtRight configurationValue : configurationList) {
            StringBuilder message = new StringBuilder("");
            if (compareConfigurationList.getContent().size() > i) {
                MxCwtConfigMgtRight compareConfiguration = compareConfigurationList.getContent().get(i);
                if (compareConfiguration != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(configurationValue, compareConfiguration);
                    if (diff1.hasChanges()) {
                        message.append("Mis Matched row");
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append(" - ").append(node.getPropertyName());
                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        configurationValue.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedConfigurationList.add(configurationValue);
                    } else {
                        message.append("Matched row");
                        configurationValue.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedConfigurationList.add(configurationValue);
                    }
                }
            }
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            if (compareConfigurationList.getTotalElements() > compareConfigurationList.getTotalElements()) {
                document.put("totalPages", compareConfigurationList.getTotalPages());
                document.put("records", compareConfigurationList.getTotalElements());
                List<MxCwtConfigMgtRight> tempList = new ArrayList<>(compareConfigurationList.getContent());
                List<MxCwtConfigMgtRight> compareTempList = new ArrayList<>(compareConfigurationList.getContent().subList(tempList.size(), (int) compareConfigurationList.getTotalElements()));
                compareTempList.forEach(f -> f.setDiff("Missing Row"));
                tempList.addAll(compareTempList);
                document.put("content", tempList);
            } else {
                document.put("records", configurationList.getTotalElements());
                List<MxCwtConfigMgtRight> tempList = new ArrayList<>(compareConfigurationList.getContent());
                List<MxCwtConfigMgtRight> compareTempList = new ArrayList<>(configurationList.getContent().subList(tempList.size(), (int) configurationList.getTotalElements()));
                compareTempList.forEach(f -> f.setDiff("Additional Row"));
                tempList.addAll(compareTempList);
                document.put("content", tempList);
                document.put("totalPages", configurationList.getTotalPages());
            }
        }
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedConfigurationList.size());
            document.put("content", matchedConfigurationList);
            document.put("totalPages", configurationList.getTotalPages());
        }
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedConfigurationList.size());
            document.put("content", misMatchedConfigurationList);
            document.put("totalPages", configurationList.getTotalPages());
        }
        if (type.equalsIgnoreCase("summary")) {
            if (compareConfigurationList.getTotalElements() > configurationList.getTotalElements()) {
                document.put("records", compareConfigurationList.getTotalElements());
                document.put("compareRecords", configurationList.getTotalElements());
                document.put("totalPages", compareConfigurationList.getTotalPages());
                summaryList.put("missingRows", compareConfigurationList.getTotalElements() - configurationList.getTotalElements());
                summaryList.put("availableRows", 0L);
            } else {
                document.put("records", configurationList.getTotalElements());
                document.put("compareRecords", compareConfigurationList.getTotalElements());
                document.put("totalPages", configurationList.getTotalPages());
                summaryList.put("availableRows", configurationList.getTotalElements() - compareConfigurationList.getTotalElements());
                summaryList.put("missingRows", 0L);
            }
            document.put("content", summaryList);
        }
            return document;
        }
    public Map<String, Long> getConfigurationSummaryReport(GeneralSpecification filters, String type) throws ExecutionException, InterruptedException {

        CompletableFuture<Map<String, Long>> futureOutput =
                CompletableFuture.supplyAsync(() -> {
                            try {
                                long totalCount = 0;
                                long totalCountContend = 0;
                                int totalPage = 1;
                                int pageSize = 500;
                                int page = 0;
                                Map<String, Long> compareConfigurationSummaryValue = new HashMap<>();
                                long reportFieldsCnt = MxCwtConfigMgtRight.class.getDeclaredFields().length - excludeCount;
                                Map<String, Long> compareConfigurationSummaryList = new HashMap<>();
                                if (filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
                                    compareConfigurationSummaryList.put("mismatchRows", 0L);
                                    compareConfigurationSummaryList.put("matchedRows", 0L);
                                    compareConfigurationSummaryList.put("misMatchFieldCount", 0L);
                                    compareConfigurationSummaryList.put("matchedFieldsCount", 0L);
                                    compareConfigurationSummaryList.put("totalFieldsCompared", 0L);
                                    compareConfigurationSummaryList.put("availableRows", 0L);
                                    compareConfigurationSummaryList.put("missingRows", 0L);
                                    return compareConfigurationSummaryList;
                                } else {
                                    try {
                                        for (int i = 0; i < totalPage; i++) {
                                            Document compareConfigurationSummary = compareConfigurationTemplate(page, pageSize, filters, type);
                                            compareConfigurationSummaryValue.putAll((Map<? extends String, ? extends Long>) compareConfigurationSummary.get("content"));
                                            totalCount = (long) compareConfigurationSummary.get("records");
                                            totalCountContend = (long) compareConfigurationSummary.get("compareRecords");
                                            totalPage = (int) compareConfigurationSummary.get("totalPages");
                                            if (i > 0) {
                                                compareConfigurationSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareConfigurationSummaryValue.get("mismatchRows"));
                                                compareConfigurationSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareConfigurationSummaryValue.get("matchedRows"));
                                                compareConfigurationSummaryList.computeIfPresent("misMatchFieldCount", (key, newValue) -> newValue + compareConfigurationSummaryValue.get("misMatchFieldCount"));
                                            } else {
                                                compareConfigurationSummaryList.putAll(compareConfigurationSummaryValue);
                                            }
                                            page++;
                                        }
                                    } catch (Exception e) {
                                        log.info("Exception occured in getConfigurationSummaryReport");
                                        e.printStackTrace();
                                    }
                                }
                                if (totalCount != 0 && totalCountContend != 0) {
                                    compareConfigurationSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
                                } else {
                                    compareConfigurationSummaryList.put("totalFieldsCompared", 0L);
                                }
                                long matchedFieldsCount = compareConfigurationSummaryList.get("totalFieldsCompared") - compareConfigurationSummaryList.get("misMatchFieldCount");
                                compareConfigurationSummaryList.put("matchedFieldsCount", matchedFieldsCount);
                                return compareConfigurationSummaryList;

                            }  catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
        return futureOutput.get();
    }

    public void doAudit(AuditAction action, Long objectId, String objectName, AuditObjectType objectType, String message,
                        Authentication authentication, Object oldTObject, Object newObject, String info1, String info2, String info3) {

        mxOverviewAuditService.doAudit(action, objectId, objectName, objectType, message, authentication, oldTObject, newObject, info1, info2, info3);

    }

    public Map<String, Long> getOperationDetailsSummaryReport(GeneralSpecification filters, String type) throws ExecutionException, InterruptedException {
        CompletableFuture<Map<String, Long>> futureOutput =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return getGroupOperationDetailsSummaryReportDetails(filters, type);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        return futureOutput.get();
    }

    public Map<String, Long> getGroupOperationDetailsSummaryReportDetails(GeneralSpecification filters, String type) throws Exception {
        long totalCount = 0;
        long compareCount=0;
        int totalPage = 1;
        int pageSize = 500;
        int page = 0;
        Page<MxOperationRights> additionalOperationList;
        Page<MxOperationRights> missingOperationList;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        Map<String, Long> compareOperationSummaryValue = new HashMap<String, Long>();
        long reportFieldsCnt = MxOperationRights.class.getDeclaredFields().length - excludeCount;
        Map<String, Long> compareOperationSummaryList = new HashMap<String, Long>();
        if ((filters.getTemplateValue().equalsIgnoreCase("null") && filters.getCompareTemplateValue().equalsIgnoreCase("null"))
                ||filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue())) {
            compareOperationSummaryList.put("mismatchRows", 0L);
            compareOperationSummaryList.put("matchedRows", 0L);
            compareOperationSummaryList.put("misMatchFieldCount", 0L);
            compareOperationSummaryList.put("availableRows", 0L);
            compareOperationSummaryList.put("missingRows", 0L);
            compareOperationSummaryList.put("matchedFieldsCount", 0L);
            compareOperationSummaryList.put("totalFieldsCompared", 0L);
            log.info("Operation Rights download job service.fetch complete.");
            return compareOperationSummaryList;
        } else if (filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")) {
            compareOperationSummaryList.put("mismatchRows", 0L);
            compareOperationSummaryList.put("matchedRows", 0L);
            compareOperationSummaryList.put("misMatchFieldCount", 0L);
            compareOperationSummaryList.put("matchedFieldsCount", 0L);
        }else {
        try {
            for (int i = 0; i < totalPage; i++) {
                Document compareOperationSummary = compareGroupOperationRights(page, pageSize, filters, type);
                compareOperationSummaryValue.putAll((Map<String, Long>) compareOperationSummary.get("content"));
                totalCount = (long) compareOperationSummary.get("records");
                totalPage = (int) compareOperationSummary.get("totalPages");
                compareCount = (long) compareOperationSummary.get("compareRecords");
                if (i > 0) {
                    compareOperationSummaryList.computeIfPresent("mismatchRows", (key, newValue) -> newValue + compareOperationSummaryValue.get("mismatchRows"));
                    compareOperationSummaryList.computeIfPresent("matchedRows", (key, newValue) -> newValue + compareOperationSummaryValue.get("matchedRows"));
                } else {
                    compareOperationSummaryList.putAll(compareOperationSummaryValue);
                }
                page++;
            }
        } catch (Exception e) {
            log.info("Exception occured in getOperationDetailsSummaryReport");
            e.printStackTrace();
        }
        }
            additionalOperationList = mxOperationRightsRepo.findGroupCompareOperRightsAdditionalValue(repDate,filters.getTemplateValue(), filters.getCompareTemplateValue(), filters.getSubTemplateValue(), 
                    PageRequest.of(0, 1));
            long additionalTotalRows = additionalOperationList.getTotalElements();
            missingOperationList = mxOperationRightsRepo.findGroupCompareOperRightsAdditionalValue(repDate,filters.getCompareTemplateValue(), filters.getTemplateValue(), filters.getSubTemplateValue(), 
                    PageRequest.of(0, 1));
            long missingTotalRows = missingOperationList.getTotalElements();
            if(totalCount !=0 && compareCount !=0) {
                compareOperationSummaryList.put("totalFieldsCompared", totalCount * reportFieldsCnt);
            }else{
                compareOperationSummaryList.put("totalFieldsCompared", 0L);
            }
            long matchedFieldsCount = compareOperationSummaryList.get("totalFieldsCompared") - compareOperationSummaryList.get("misMatchFieldCount");
            compareOperationSummaryList.put("availableRows", additionalTotalRows);
            compareOperationSummaryList.put("missingRows", missingTotalRows);
            compareOperationSummaryList.put("matchedFieldsCount", matchedFieldsCount);

        return compareOperationSummaryList;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupOperationAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
                                                        DownloadJob job, String outputFormat, List<String> fieldColumns, String type) throws Exception {
        log.info(filters.toString());
        List<MxOperationRights> operationRightTmplList = new ArrayList<>();
        Page<MxOperationRights> additionalOperationRightsList = null;
        Page<MxOperationRights> missingOperationRightsList = null;
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        int additionalPage = 0;
        int missingPage = 0;
        LocalDate repDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        log.info(" Export: Starting fetch.");
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        try {
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED)) || type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
                if((!(filters.getTemplateValue().equalsIgnoreCase("null") || filters.getCompareTemplateValue().equalsIgnoreCase("null")))
                        && !(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
                    for (int i = 0; i < totalPage; i++) {
                        Document operationRights = compareGroupOperationRights(page, pageSize, filters, type);
                        operationRightTmplList.addAll((List<MxOperationRights>) operationRights.get("content"));
                        totalPage = (int) operationRights.get("totalPages");
                        totalCount = Long.valueOf(String.valueOf(operationRights.get("records")));
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, operationRightTmplList.size(), job.getId());
                        page++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!(filters.getTemplateValue().equalsIgnoreCase(filters.getCompareTemplateValue()))) {
            // additional
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ADDITIONAL))) {
                int additionalTotalPage = 1;
                for (int i = 0; i < additionalTotalPage; i++) {
                    additionalOperationRightsList = mxOperationRightsRepo.findGroupCompareOperRightsAdditionalValue(repDate, filters.getTemplateValue(), filters.getCompareTemplateValue(), filters.getSubTemplateValue(),
                            PageRequest.of(additionalPage, pageSize));
                    additionalOperationRightsList.forEach(c -> c.setDiff("Additonal row " + filters.getTemplateValue()));
                    operationRightTmplList.addAll(additionalOperationRightsList.getContent());
                    additionalTotalPage = additionalOperationRightsList.getTotalPages();
                    totalCount = additionalOperationRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, operationRightTmplList.size(), job.getId());
                    additionalPage++;
                }
            }
            if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MISSING))) {
                // missing
                int missingTotalPage = 1;
                for (int i = 0; i < missingTotalPage; i++) {
                    missingOperationRightsList = mxOperationRightsRepo.findGroupCompareOperRightsAdditionalValue(repDate, filters.getCompareTemplateValue(), filters.getTemplateValue(), filters.getSubTemplateValue(),
                            PageRequest.of(missingPage, pageSize));
                    missingOperationRightsList.forEach(c -> c.setDiff("Missing row " + filters.getTemplateValue()));
                    operationRightTmplList.addAll(missingOperationRightsList.getContent());
                    missingTotalPage = missingOperationRightsList.getTotalPages();
                    totalCount = missingOperationRightsList.getTotalElements();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, operationRightTmplList.size(), job.getId());
                    missingPage++;
                }
            }
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", operationRightTmplList.size(), (int) totalCount, job.getId());
        return viewerExportService.exportFilefromDetails(outputFormat, operationRightTmplList, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document compareGroupOperationRights(int page, int pageSize, GeneralSpecification generalSpecification, String type) {
        Document document = new Document();
        Page<MxOperationRights> mxoperationRightsTmpls = null;
        Page<MxOperationRights> compareOperationRightsList = null;
        List<MxOperationRights> matchedOperationList = new ArrayList<>();
        List<MxOperationRights> misMatchedOperationList = new ArrayList<>();
        Map<String, Long> summaryList = new HashMap<String, Long>();
        LocalDate repDate = LocalDate.parse(generalSpecification.getDateValue(), dateTimeFormatter);
        mxoperationRightsTmpls = mxOperationRightsRepo.findGroupCompareOperRightsMatchedValue(repDate,generalSpecification.getTemplateValue(), generalSpecification.getCompareTemplateValue(), generalSpecification.getSubTemplateValue(),
                PageRequest.of(page, pageSize));
        compareOperationRightsList = mxOperationRightsRepo.findGroupCompareOperRightsMatchedValue(repDate,generalSpecification.getCompareTemplateValue(), generalSpecification.getTemplateValue(), generalSpecification.getSubTemplateValue(),
                PageRequest.of(page, pageSize));
        //compare date from both dates
        int i = 0;
        long misMatchrowCount = 0;
        long matchRowCount = 0;
        long fieldCount = 0;
        AtomicLong misMatchFieldCount = new AtomicLong();
        for (MxOperationRights operationRights : mxoperationRightsTmpls) {
            StringBuilder message = new StringBuilder("");
            if (compareOperationRightsList.getContent().size() > i) {
                MxOperationRights compareOperationRights = compareOperationRightsList.getContent().get(i);
                if (compareOperationRights != null) {
                    ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
                    excludeProperties(objectDifferBuilder);
                    DiffNode diff1 = objectDifferBuilder.build().compare(operationRights, compareOperationRights);
                    if (diff1.hasChanges()) {
                        diff1.visit((node, visit) -> {
                            if (!node.hasChildren()) {
                                final Object oldValue = node.canonicalGet(operationRights);
                                final Object newValue = node.canonicalGet(compareOperationRights);
                                if (message.toString().isEmpty() || message.toString().equals(""))
                                    message.append(node.getPropertyName());
                                else
                                    message.append("- ").append(node.getPropertyName());

                                misMatchFieldCount.getAndIncrement();
                            }
                        });
                        operationRights.setDiff(String.valueOf(message));
                        misMatchrowCount++;
                        misMatchedOperationList.add(operationRights);
                    } else {
                        operationRights.setDiff(String.valueOf(message));
                        matchRowCount++;
                        matchedOperationList.add(operationRights);
                    }
                }
            }
            //set the difference
            i++;
        }
        summaryList.put("mismatchRows", misMatchrowCount);
        summaryList.put("matchedRows", matchRowCount);
        summaryList.put("misMatchFieldCount", misMatchFieldCount.longValue());
        document.put("totalPages", mxoperationRightsTmpls.getTotalPages());
        if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.ALL))) {
            document.put("records", mxoperationRightsTmpls.getTotalElements());
            document.put("content", mxoperationRightsTmpls.getContent());
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.MATCHED))) {
            document.put("records", matchedOperationList.size());
            document.put("content", matchedOperationList);
        } else if (type.equalsIgnoreCase(String.valueOf(GroupCompareType.UNMATCHED))) {
            document.put("records", misMatchedOperationList.size());
            document.put("content", misMatchedOperationList);
        } else if (type.equalsIgnoreCase("summary")) {
            document.put("compareRecords", compareOperationRightsList.getTotalElements());
            document.put("records", mxoperationRightsTmpls.getTotalElements());
            document.put("content", summaryList);
        } else {
            document.put("records", mxoperationRightsTmpls.getTotalElements());
            document.put("content", mxoperationRightsTmpls.getContent());
        }
        return document;
    }
}



