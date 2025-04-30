package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.configs.CompareSearchConfig;
import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.repository.*;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class GroupCompareService {


    private static final Logger log = LoggerFactory.getLogger(GroupCompareService.class);

    private final MxPortfolioRightsRepository mxPortfolioRightsRepository;
    private final MxChineseWallTemplateRepository mxChineseWallTemplateRepository;
    private final MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo;
    private final MxGroupNavigationRightsRepository mxGroupNavigationRightsRepository;
    private final MxEnterPriseRiskRepo mxEnterPriseRiskRepo;
    private final MxConsistencyTemplateRepository mxConsistencyTmplRepo;
    private final MxOperationRightsRepo mxOperationRightsRepo;
    private final MxFinanceRightsRepo mxFinanceRightsRepo;
    private final MxOSPRightsTemplateRepository ospRightsTemplateRepository;
    private final MxCwtConfigMgtRightRepository mxCwtConfigMgtRightRepository;
    private final STPRightsRepository stpRightsRepository;
    private final DateTimeFormatter dateTimeFormatter;
    @Autowired
    private ViewerExportService viewerExportService;
    @Autowired
    private EntityManager entityManager;
    private final SearchHistoryRepository historyRepo;

    public GroupCompareService(STPRightsRepository stpRightsRepository, MxPortfolioRightsRepository mxPortfolioRightsRepository, MxChineseWallTemplateRepository mxChineseWallTemplateRepository, MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo, MxGroupNavigationRightsRepository mxGroupNavigationRightsRepository, MxEnterPriseRiskRepo mxEnterPriseRiskRepo, MxConsistencyTemplateRepository mxConsistencyTmplRepo, MxOperationRightsRepo mxOperationRightsRepo, MxFinanceRightsRepo mxFinanceRightsRepo, MxOSPRightsTemplateRepository mxOSPRightsTemplateRepository, MxCwtConfigMgtRightRepository mxCwtConfigMgtRightRepository, SearchHistoryRepository historyRepo) {
        this.mxPortfolioRightsRepository = mxPortfolioRightsRepository;
        this.mxChineseWallTemplateRepository = mxChineseWallTemplateRepository;
        this.mxGroupCombinedPortfolioRepo = mxGroupCombinedPortfolioRepo;
        this.mxGroupNavigationRightsRepository = mxGroupNavigationRightsRepository;
        this.mxEnterPriseRiskRepo = mxEnterPriseRiskRepo;
        this.mxConsistencyTmplRepo = mxConsistencyTmplRepo;
        this.mxOperationRightsRepo = mxOperationRightsRepo;
        this.mxFinanceRightsRepo = mxFinanceRightsRepo;
        this.ospRightsTemplateRepository = mxOSPRightsTemplateRepository;
        this.mxCwtConfigMgtRightRepository = mxCwtConfigMgtRightRepository;
        this.stpRightsRepository = stpRightsRepository;
        this.historyRepo = historyRepo;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);

    }

    public Document getMultiGroupCompareOsp(String repDate, MxCompareGroup mxCompareGroup, OspRightsMatrix filters, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {

        Document finalDocument = new Document();

        List<OspRightsMatrix> treeMapList = new ArrayList<>();
        LocalDate reportDate = LocalDate.parse(repDate, dateTimeFormatter);

        if (isMatched || (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank())) {
            Document document = new Document();
            String template= viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, mxCompareGroup.isGlobalSearch);
            String[] templateList = template.split("\\|");
            int i = 0;
            CompareSearchConfig<MxOspRightsMatrix> mxOspSpecificationBuilder = null;
            //passing constructor for match
            if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                mxOspSpecificationBuilder = new CompareSearchConfig<>(templateList[i], OspRightsMatrix.class, MxGroupCompareOsp.class, templateList[++i], repDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                        filters.getValidationRightTmplList(), filters.getCategoryList(), filters.getSubCategoryList(), filters.getQueueList(), new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
            } else if (isMatched || isAdditionalMissing) {
                mxOspSpecificationBuilder = new CompareSearchConfig<>(templateList[i], OspRightsMatrix.class, templateList[++i], repDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                        filters.getValidationRightTmplList(), filters.getCategoryList(), filters.getSubCategoryList(), filters.getQueueList(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            } else {
                mxOspSpecificationBuilder = new CompareSearchConfig<>(templateList[i], OspRightsMatrix.class, templateList[++i], repDate
                        , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), null, null, templateList[++i], null,
                        filters.getValidationRightTmplList(), filters.getCategoryList(), filters.getSubCategoryList(), filters.getQueueList(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            }

            if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
                if(mxCompareGroup.isGlobalSearch){
                    document = mxOspSpecificationBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                            true, true, isMatched, isAdditionalMissing, true,0,0, MxOspRightsMatrix.class, true);
                }
                else{
                    document = mxOspSpecificationBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, false);
                }
            } else {
                document = mxOspSpecificationBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, true);
            }

            List<MxOspRightsMatrix> ospRightsMatrices = (List<MxOspRightsMatrix>) document.get("content1");
            treeMapList = ospRightsMatrices.stream().map(osp -> new OspRightsMatrix(osp.getValidationRightTemplate(), osp.getCategory(), osp.getSubCategory(), osp.getQueue(), osp.getAction())).collect(Collectors.toList());
            document.put("treeMap", treeMapList);
            if (treeMapList.size() == 0 || !isMatched ) {
                document.put("content2", new ArrayList<>());
            } else {
                document.put("content2", findCompareOspRightsByPage(treeMapList, reportDate, mxCompareGroup));
            }
            return document;
        } else {

            CompletableFuture<Object> additionalCompletableFuture = CompletableFuture.supplyAsync(() -> {
                Page<MxOspRightsMatrix> additionalList = null;
                if (isAdditionalMissing) {
                    additionalList = ospRightsTemplateRepository.findAllGroupCompareAdditional(reportDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), filters, PageRequest.of(page, pageSize));
                }
                else{
                    additionalList = ospRightsTemplateRepository.findAllGroupCompareAdditional(reportDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), filters, PageRequest.of(page, pageSize));

                }
                finalDocument.put("content1", additionalList.getContent());
                finalDocument.put("totalPages", additionalList.getTotalPages());
                finalDocument.put("records", additionalList.getTotalElements());
                finalDocument.put("content2", new ArrayList<>());
                finalDocument.put("treeMap", additionalList.stream().map(osp -> new OspRightsMatrix(osp.getValidationRightTemplate(), osp.getCategory(), osp.getSubCategory(), osp.getQueue())).collect(Collectors.toList()));
                return finalDocument;
            });
            additionalCompletableFuture.join();
            return finalDocument;
        }
    }


    public Document identicalCondition(MxCompareGroup mxCompareGroup, Document document, boolean isCompare, boolean isMatched) {
        if ((mxCompareGroup.getCompareTemplate().equalsIgnoreCase(mxCompareGroup.getTemplate()))) {
            document.put("message", "Same templates can't be compare");
            return document;
        }
        if (isMatched && isCompare) {
            document.put("message", "Can't match with null template");
            return document;
        }
        return null;
    }


    public Document getMultiGroupCompareEnterprise(String reportDate, MxCompareGroup mxCompareGroup, int page, int pageSize, String userName, String type) {
        Document document = new Document();
        Page<MxEnterpriseRisk> enterpriseRisks = null;
        Page<MxEnterpriseRisk> compareEnterpriseRisks = null;
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);

        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {

            if (type.equalsIgnoreCase("Matched")) {
                enterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getTemplate(), repDate, PageRequest.of(page, pageSize));
                compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getCompareTemplate(), repDate, PageRequest.of(page, pageSize));
                return compareEnterprise(enterpriseRisks.getContent(), compareEnterpriseRisks.getContent(), document, pageSize, type);
            } else {
                enterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getTemplate(), repDate, PageRequest.of(page, pageSize));
                compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getCompareTemplate(), repDate, PageRequest.of(page, pageSize));
                return compareEnterprise(enterpriseRisks.getContent(), compareEnterpriseRisks.getContent(), document, pageSize, type);
            }
        }
        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, false);
            String[] templateList = template.split("\\|");
            int i = 0;
            CompareSearchConfig<MxEnterpriseRisk> mxOspSpecificationBuilder = null;
            mxOspSpecificationBuilder = new CompareSearchConfig<>(templateList[i], null, templateList[++i], reportDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);

            if(mxCompareGroup.isGlobalSearch){
                CompareSearchConfig<MxEnterpriseRisk> finalMxOspSpecificationBuilder = mxOspSpecificationBuilder;
                CompletableFuture.runAsync(() -> finalMxOspSpecificationBuilder.saveQueryMethod(true, true, mxCompareGroup.getSearchValue(), userName, mxCompareGroup.reportType));
                enterpriseRisks = mxEnterPriseRiskRepo.findByGroupLabelAndReportDateWithGlobalSearch(mxCompareGroup.getTemplate(), repDate,mxCompareGroup.getSearchValue(), PageRequest.of(page, pageSize));
                document.put("content1", enterpriseRisks.getContent());
                document.put("totalPages", enterpriseRisks.getTotalPages());
                document.put("records", enterpriseRisks.getTotalElements());
            }else {
                document = mxOspSpecificationBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), false, false, true, false, false);
            }
            List<MxEnterpriseRisk> tempList = (List<MxEnterpriseRisk>) document.get("content1");
            if (tempList.size() == 0) {
                document.put("content2", new ArrayList<>());
            } else {
                compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getCompareTemplate(), repDate, PageRequest.of(page, tempList.size()));
                document.put("content2", compareEnterpriseRisks.getContent());
            }
            return document;
        } else {
            enterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getTemplate(), repDate, PageRequest.of(page, pageSize));
            compareEnterpriseRisks = mxEnterPriseRiskRepo.findEnterpriseByGroupLabelsAndReportDate(mxCompareGroup.getCompareTemplate(), repDate, PageRequest.of(page, pageSize));

            if (compareEnterpriseRisks.getTotalElements() > enterpriseRisks.getTotalElements()) {

                document.put("totalPages", compareEnterpriseRisks.getTotalPages());
                document.put("records", compareEnterpriseRisks.getTotalElements());
                document.put("content1", enterpriseRisks.getContent());
                document.put("content2", compareEnterpriseRisks.getContent());
                return document;
            } else {
                document.put("totalPages", enterpriseRisks.getTotalPages());
                document.put("records", enterpriseRisks.getTotalElements());
                document.put("content1", enterpriseRisks.getContent());
                document.put("content2", compareEnterpriseRisks.getContent());
                return document;
            }
        }
    }


    public Document groupComparePortfolio(String repDate, MxCompareGroup mxCompareGroup, PortfolioRights filter, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {

        Document document = new Document();
        LocalDate reportDate = LocalDate.parse(repDate, dateTimeFormatter);
        List<String> treeMapList = new ArrayList();
        String monShortName = reportDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();

        String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, true);
        String[] templateList = template.split("\\|");
        int i = 0;
        CompareSearchConfig<MxGroupPortfolioRights> mxPortfolioSpecBuilder = null;
        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
            mxPortfolioSpecBuilder = new CompareSearchConfig<>(templateList[i], PortfolioRights.class, MxGroupComparePortfolio.class, templateList[++i] + " PARTITION (P_" + monShortName + ")", repDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                    filter.portfolioLabelList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
        } else if (isMatched || isAdditionalMissing) {
            mxPortfolioSpecBuilder = new CompareSearchConfig<>(templateList[i], PortfolioRights.class, templateList[++i] + " PARTITION (P_" + monShortName + ")", repDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                    filter.portfolioLabelList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
        } else {
            mxPortfolioSpecBuilder = new CompareSearchConfig<>(templateList[i], PortfolioRights.class, templateList[++i] + " PARTITION (P_" + monShortName + ")", repDate
                    , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), null, null, templateList[++i], null,
                    filter.portfolioLabelList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
        }
        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            if (mxCompareGroup.isGlobalSearch) {
                document = mxPortfolioSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true,
                        true, isMatched, isAdditionalMissing, true, reportDate.getDayOfMonth(), reportDate.getYear(), MxGroupPortfolioRights.class, true);
            } else {
                document = mxPortfolioSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true,
                        true, isMatched, isAdditionalMissing, false, reportDate.getDayOfMonth(), reportDate.getYear(), MxGroupPortfolioRights.class, false);
            }
        } else {
            document = mxPortfolioSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                    true, true, isMatched, isAdditionalMissing, true, reportDate.getDayOfMonth(), reportDate.getYear(), MxGroupPortfolioRights.class, false);
        }
        List<MxGroupPortfolioRights> groupPortfolioRights = (List<MxGroupPortfolioRights>) document.get("content1");
        treeMapList = groupPortfolioRights.stream().map(MxGroupPortfolioRights::getPortfolioLabel).collect(Collectors.toList());
        List<String> orderByFields = (List<String>) document.get("orderByFields");
        if (treeMapList.size() == 0 || !isMatched) {
            document.put("content2", new ArrayList<>());
        } else {
            List<MxGroupPortfolioRights> m = (List<MxGroupPortfolioRights>) mxPortfolioSpecBuilder.getCompareGroupSearchWithPartition(null, reportDate, mxCompareGroup, reportDate.getDayOfMonth(), reportDate.getYear(),
                    isMatched, isAdditionalMissing, MxGroupPortfolioRights.class, orderByFields, treeMapList);
            document.put("content2", m);
        }
        return document;
    }


    public Document getMultiGroupCompareNavigation(String reportDate, MxCompareGroup mxCompareGroup, NavigationRights navRightsFilters, int page, int pageSize, boolean isMatched,
                                                   boolean isAdditionalMissing, String userName, String type) {

        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        String monShortName = repDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        List<NavigationRights> treeMapList = new ArrayList<>();
        String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, true);
        String[] templateList = template.split("\\|");
        int i = 0;

        CompareSearchConfig<MxGroupNavigationRight> mxNavSpecificationBuilder = null;

        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
            mxNavSpecificationBuilder = new CompareSearchConfig<>(null, NavigationRights.class, MxGroupCompNavigation.class, templateList[++i] + " PARTITION (P_" + monShortName + ")", reportDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), mxCompareGroup.getCompareGroupLabel(), templateList[++i] + "," + "GROUP_LABEL", null,
                    navRightsFilters.commentsList, navRightsFilters.rightsList, navRightsFilters.menuList, navRightsFilters.pathList, new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
        } else if (isMatched || isAdditionalMissing) {
            mxNavSpecificationBuilder = new CompareSearchConfig<>(null, NavigationRights.class, templateList[++i] + " PARTITION (P_" + monShortName + ")", reportDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), mxCompareGroup.getCompareGroupLabel(), templateList[++i] + ",GROUP_LABEL", null,
                    navRightsFilters.commentsList, navRightsFilters.rightsList, navRightsFilters.menuList, navRightsFilters.pathList, new ArrayList<>(), page, pageSize, entityManager, historyRepo);
        } else {
            mxNavSpecificationBuilder = new CompareSearchConfig<>(null, NavigationRights.class, templateList[++i] + " PARTITION (P_" + monShortName + ")", reportDate
                    , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxCompareGroup.getCompareGroupLabel(), mxCompareGroup.getGroupLabel(), templateList[++i] + ",GROUP_LABEL", null
                    , navRightsFilters.commentsList, navRightsFilters.rightsList, navRightsFilters.menuList, navRightsFilters.pathList, new ArrayList<>(), page, pageSize, entityManager, historyRepo);
        }
        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            if (mxCompareGroup.isGlobalSearch) {
                document = mxNavSpecificationBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                        true, true, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxGroupNavigationRight.class, true);
            } else {
                document = mxNavSpecificationBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                        true, true, isMatched, isAdditionalMissing, false, repDate.getDayOfMonth(), repDate.getYear(), MxGroupNavigationRight.class, false);
            }
        } else {
            document = mxNavSpecificationBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName,
                    mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxGroupNavigationRight.class, false);
        }
        List<MxGroupNavigationRight> navigationRights = (List<MxGroupNavigationRight>) document.get("content1");
        treeMapList = navigationRights.stream().map(nav -> new NavigationRights(nav.getComments(), nav.getRights(), nav.getMenu(), nav.getPath())).collect(Collectors.toList());
        if (treeMapList.size() == 0 || !isMatched) {
            document.put("treeMap", treeMapList);
            document.put("content2", new ArrayList<>());
        } else {
            List<String> orderByFields = (List<String>) document.get("orderByFields");
            document.put("treeMap", treeMapList);
            document.put("content2", findCompareNavRightsByPage(mxNavSpecificationBuilder, treeMapList, repDate, mxCompareGroup, isMatched, isAdditionalMissing, orderByFields));
        }
        return document;
    }


    public Document getMultiGroupCompareCombPortfolio(String reportDate, MxCompareGroup mxCompareGroup, GroupCompPortfolio mxGrpCompPortfolio, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {

        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        List<GroupCompPortfolio> treeMapList = new ArrayList<>();
        Document finalDocument = new Document();
        if ((mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank())) {
            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, mxCompareGroup.isGlobalSearch);

            String[] templateList = template.split("\\|");
            int i = 0;
            CompareSearchConfig<MxGroupCombinedPortfolio> mxCombPortfolioSpecBuilder = null;
            if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                mxCombPortfolioSpecBuilder = new CompareSearchConfig<>(templateList[i], GroupCompPortfolio.class, MxGroupCompPortfolio.class, templateList[++i], reportDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                        mxGrpCompPortfolio.compfLblList, mxGrpCompPortfolio.unitList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
            } else if (isMatched || isAdditionalMissing) {
                mxCombPortfolioSpecBuilder = new CompareSearchConfig<>(templateList[i], GroupCompPortfolio.class, templateList[++i], reportDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                        mxGrpCompPortfolio.compfLblList, mxGrpCompPortfolio.unitList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            } else {
                mxCombPortfolioSpecBuilder = new CompareSearchConfig<>(templateList[i], GroupCompPortfolio.class, templateList[++i], reportDate
                        , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), null, null, templateList[++i], null,
                        mxGrpCompPortfolio.compfLblList, mxGrpCompPortfolio.unitList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            }

            if(mxCompareGroup.isGlobalSearch){
                document = mxCombPortfolioSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                        true, true, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxGroupCombinedPortfolio.class, true);
            }else {
                document = mxCombPortfolioSpecBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, false);
            }
            List<MxGroupCombinedPortfolio> grpCompPortfolio = (List<MxGroupCombinedPortfolio>) document.get("content1");
            treeMapList = grpCompPortfolio.stream().map(portfolio -> new GroupCompPortfolio(portfolio.getCompfLbl(), portfolio.getUnit())).collect(Collectors.toList());
            if (treeMapList.size() == 0 || !isMatched) {
                document.put("content2", new ArrayList<>());
            } else {
                document.put("content2", findCompareCompPortfolioByPage(treeMapList, repDate, mxCompareGroup));
            }
            return document;

        } else {
            if (isMatched) {
                CompletableFuture<Object> completableFuture = CompletableFuture.supplyAsync(() -> {
                    Page<MxGroupCombinedPortfolio> matchedList = null;
                    if (type.equalsIgnoreCase("Matched"))
                        matchedList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCompPortfolioMatchedList(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxGrpCompPortfolio, PageRequest.of(page, pageSize));
                    else if (type.equalsIgnoreCase("UnMatched"))
                        matchedList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCompPortfolioUnMatched(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxGrpCompPortfolio, PageRequest.of(page, pageSize));
                    else
                        matchedList = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioMatched(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxGrpCompPortfolio, PageRequest.of(page, pageSize));

                    finalDocument.put("content1", matchedList.getContent());
                    finalDocument.put("totalPages", matchedList.getTotalPages());
                    finalDocument.put("records", matchedList.getTotalElements());
                    return finalDocument;
                });
                finalDocument.put("content2", mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioMatched(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxGrpCompPortfolio, PageRequest.of(page, pageSize)).getContent());
                completableFuture.join();
                return finalDocument;
            }else {
                CompletableFuture<Object> additionalFuture = CompletableFuture.supplyAsync(() -> {
                    Page<MxGroupCombinedPortfolio> groupCombinedPortfolios = null;
                    if (isAdditionalMissing) {
                        groupCombinedPortfolios = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioAdditional(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxGrpCompPortfolio, PageRequest.of(page, pageSize));
                    } else {
                        groupCombinedPortfolios = mxGroupCombinedPortfolioRepo.findAllGroupCompareCombPortfolioAdditional(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxGrpCompPortfolio, PageRequest.of(page, pageSize));
                    }
                    finalDocument.put("totalPages", groupCombinedPortfolios.getTotalPages());
                    finalDocument.put("records", groupCombinedPortfolios.getTotalElements());
                    finalDocument.put("content1", groupCombinedPortfolios.getContent());
                    finalDocument.put("content2", new ArrayList<>());
                    return finalDocument;
                });
                CompletableFuture.allOf(additionalFuture).join();
                return finalDocument;
            }
        }
    }


//    public Document getMultiGroupCompareStp(LocalDate reportDate, MxCompareGroup mxCompareGroup, STPModel mxStpGrpCompFilter, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {
//
//        Document document = new Document();
//        Page<STPRightsMatrix> stpRightsMatrices = null;
//        Page<STPRightsMatrix> compareStpRightsMatrices = null;
//        List<STPModel> treeMapList = new ArrayList<>();
//        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
//
//            if (type.equalsIgnoreCase("Matched")) {
//                stpRightsMatrices = stpRightsRepository.findAllGroupComparStpMatchedList(reportDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), PageRequest.of(page, pageSize));
//                compareStpRightsMatrices = stpRightsRepository.findAllGroupComparStpMatchedList(reportDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), PageRequest.of(page, pageSize));
//                document.put("totalPages", stpRightsMatrices.getTotalPages());
//                document.put("records", stpRightsMatrices.getTotalElements());
//                document.put("content1", stpRightsMatrices.getContent());
//                document.put("content2", compareStpRightsMatrices.getContent());
//                document.put("treeMap", stpRightsMatrices.stream().map(stp -> new STPModel(stp.getBoType(), stp.getTypology(), stp.getAction(), stp.getStatus(), stp.getStpView())).collect(Collectors.toList()));
//                return document;
//            } else {
//                stpRightsMatrices = stpRightsRepository.findAllGroupComparStpUnMatched(reportDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), PageRequest.of(page, pageSize));
//                compareStpRightsMatrices = stpRightsRepository.findAllGroupComparStpUnMatched(reportDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), PageRequest.of(page, pageSize));
//                document.put("totalPages", stpRightsMatrices.getTotalPages());
//                document.put("records", stpRightsMatrices.getTotalElements());
//                document.put("content1", stpRightsMatrices.getContent());
//                document.put("content2", compareStpRightsMatrices.getContent());
//                document.put("treeMap", stpRightsMatrices.stream().map(stp -> new STPModel(stp.getBoType(), stp.getTypology(), stp.getAction(), stp.getStatus(), stp.getStpView())).collect(Collectors.toList()));
//                return document;
//            }
//        }
//        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
//            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null);
//            log.info("template:{}", template);
//            String[] templateList = template.split("\\|");
//            log.info("templateList:{}", Arrays.asList(templateList));
//            int i = 0;
//            CompareSearchConfig<STPRightsMatrix> mxStpSpecificationBuilder = null;
//
//            if (isAdditionalMissing) {
//                mxStpSpecificationBuilder = new CompareSearchConfig<>(templateList[i], STPModel.class, templateList[++i], reportDate.format(dateTimeFormatter)
//                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
//                        mxStpGrpCompFilter.boTypeList, mxStpGrpCompFilter.typologyList, mxStpGrpCompFilter.actionList, mxStpGrpCompFilter.statusList, mxStpGrpCompFilter.stpViewList, page, pageSize, entityManager, historyRepo);
//            } else {
//                mxStpSpecificationBuilder = new CompareSearchConfig<>(templateList[i], STPModel.class, templateList[++i], reportDate.format(dateTimeFormatter)
//                        , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), null, null, templateList[++i], null
//                        , mxStpGrpCompFilter.boTypeList, mxStpGrpCompFilter.typologyList, mxStpGrpCompFilter.actionList, mxStpGrpCompFilter.statusList, mxStpGrpCompFilter.stpViewList, page, pageSize, entityManager, historyRepo);
//            }
//
//            document = mxStpSpecificationBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, false);
//            List<STPRightsMatrix> stpRightsList = (List<STPRightsMatrix>) document.get("content1");
//            treeMapList = stpRightsList.stream().map(stp -> new STPModel(stp.getBoType(), stp.getTypology(), stp.getAction(), stp.getStatus(), stp.getStpView())).collect(Collectors.toList());
//            document.put("content2", findCompareStpRightsByPage(treeMapList, reportDate, mxCompareGroup));
//            document.put("treeMap", treeMapList);
//            return document;
//
//        } else {
//            if (isMatched) {
//
//                stpRightsMatrices = stpRightsRepository.findAllGroupCompareStpMatched(reportDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxStpGrpCompFilter, PageRequest.of(page, pageSize));
//                treeMapList = stpRightsMatrices.stream().map(stp -> new STPModel(stp.getBoType(), stp.getTypology(), stp.getAction(), stp.getStatus(), stp.getStpView())).collect(Collectors.toList());
//                if (!mxCompareGroup.hasFilters) {
//                    compareStpRightsMatrices = stpRightsRepository.findAllGroupCompareStpMatched(reportDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxStpGrpCompFilter, PageRequest.of(page, pageSize));
//                    document.put("content2", compareStpRightsMatrices.getContent());
//                } else {
//                    document.put("content2", findCompareStpRightsByPage(treeMapList, reportDate, mxCompareGroup));
//                }
//            } else {
//                if (isAdditionalMissing) {
//                    stpRightsMatrices = stpRightsRepository.findAllGroupCompareStpAdditional(reportDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxStpGrpCompFilter, PageRequest.of(page, pageSize));
//                    document.put("totalPages", stpRightsMatrices.getTotalPages());
//                    document.put("records", stpRightsMatrices.getTotalElements());
//                    document.put("treeMap", stpRightsMatrices.stream().map(stp -> new STPModel(stp.getBoType(), stp.getTypology(), stp.getAction(), stp.getStatus(), stp.getStpView())).collect(Collectors.toList()));
//                    document.put("content1", stpRightsMatrices.getContent());
//                    document.put("content2", new ArrayList<>());
//                    return document;
//                } else {
//                    compareStpRightsMatrices = stpRightsRepository.findAllGroupCompareStpAdditional(reportDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxStpGrpCompFilter, PageRequest.of(page, pageSize));
//                    document.put("totalPages", compareStpRightsMatrices.getTotalPages());
//                    document.put("records", compareStpRightsMatrices.getTotalElements());
//                    document.put("treeMap", compareStpRightsMatrices.stream().map(stp -> new STPModel(stp.getBoType(), stp.getTypology(), stp.getAction(), stp.getStatus(), stp.getStpView())).collect(Collectors.toList()));
//                    document.put("content1", compareStpRightsMatrices.getContent());
//                    document.put("content2", new ArrayList<>());
//                    return document;
//                }
//            }
//            document.put("totalPages", stpRightsMatrices.getTotalPages());
//            document.put("records", stpRightsMatrices.getTotalElements());
//            document.put("treeMap", treeMapList);
//            document.put("content1", stpRightsMatrices.getContent());
//            return document;
//        }
//    }

    public Document getMultiGroupCompareConsistency(String reportDate, MxCompareGroup mxCompareGroup, ConsistencyTemplateRights consistTmpl, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {

        Document document = new Document();
        Document finalDocument = new Document();

        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        List<ConsistencyTemplateRights> treeMapList = new ArrayList<>();

        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, mxCompareGroup.isGlobalSearch);
            String[] templateList = template.split("\\|");
            int i = 0;
            CompareSearchConfig<MxConsistencyTmpl> mxConsistSpecBuilder = null;
            if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                mxConsistSpecBuilder = new CompareSearchConfig<>(templateList[i], ConsistencyTemplateRights.class, MxConsistencyGroupCompFilter.class, templateList[++i], reportDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                        consistTmpl.categoryList, consistTmpl.itemList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
            } else if (isMatched || isAdditionalMissing) {
                mxConsistSpecBuilder = new CompareSearchConfig<>(templateList[i], ConsistencyTemplateRights.class, templateList[++i], reportDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                        consistTmpl.categoryList, consistTmpl.itemList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            } else {
                mxConsistSpecBuilder = new CompareSearchConfig<>(templateList[i], ConsistencyTemplateRights.class, templateList[++i], reportDate
                        , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), null, null, templateList[++i], null
                        , consistTmpl.categoryList, consistTmpl.itemList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            }
            if (mxCompareGroup.isGlobalSearch) {
                document = mxConsistSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                        true, true, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxConsistencyTmpl.class, true);
            } else {
                document = mxConsistSpecBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, false);
            }
            List<MxConsistencyTmpl> consistencyTmpls = (List<MxConsistencyTmpl>) document.get("content1");
            treeMapList = consistencyTmpls.stream().map(consist -> new ConsistencyTemplateRights(consist.getCategory(), consist.getItem())).collect(Collectors.toList());
            if (treeMapList.size() == 0 || !isMatched) {
                document.put("content2", new ArrayList<>());
            } else {
                document.put("content2", findCompareConsistRightsByPage(treeMapList, repDate, mxCompareGroup));
            }
            return document;

        } else {
            if (isMatched) {
                CompletableFuture<Object> completableFuture = CompletableFuture.supplyAsync(() -> {
                    Page<MxConsistencyTmpl> matchedList = null;
                    if (type.equalsIgnoreCase("Matched")) {
                        matchedList = mxConsistencyTmplRepo.findAllGrpCompareMatchedListforConsistency(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), consistTmpl, PageRequest.of(page, pageSize));
                    } else if (type.equalsIgnoreCase("Unmatched")) {
                        matchedList = mxConsistencyTmplRepo.findAllGroupCompareConsistencyUnMatched(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), consistTmpl, PageRequest.of(page, pageSize));
                    } else {
                        matchedList = mxConsistencyTmplRepo.findAllGroupCompareConsistencyMatched(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), consistTmpl, PageRequest.of(page, pageSize));
                    }
                    finalDocument.put("content1", matchedList.getContent());
                    finalDocument.put("totalPages", matchedList.getTotalPages());
                    finalDocument.put("records", matchedList.getTotalElements());
                    return finalDocument;
                });
                finalDocument.put("content2", mxConsistencyTmplRepo.findAllGrpCompareMatchedListforConsistency(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), consistTmpl, PageRequest.of(page, pageSize)).getContent()
                );
                completableFuture.join();
                return finalDocument;
            } else {
                CompletableFuture<Object> additionalCompletableFuture = CompletableFuture.supplyAsync(() -> {
                    Page<MxConsistencyTmpl> additionalList = null;
                    if (isAdditionalMissing) {
                        additionalList = mxConsistencyTmplRepo.findAllGroupCompareConsistencyAdditional(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), consistTmpl, PageRequest.of(page, pageSize));
                    } else {
                        additionalList = mxConsistencyTmplRepo.findAllGroupCompareConsistencyAdditional(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), consistTmpl, PageRequest.of(page, pageSize));
                    }
                    finalDocument.put("content1", additionalList.getContent());
                    finalDocument.put("totalPages", additionalList.getTotalPages());
                    finalDocument.put("records", additionalList.getTotalElements());
                    finalDocument.put("content2", new ArrayList<>());
                    return finalDocument;
                });
                additionalCompletableFuture.join();
                return finalDocument;
            }
        }
    }

    public Document getMultiGroupCompareConfig(String repDate, MxCompareGroup mxCompareGroup, int page, int pageSize, String userName, String type) {

        Document document = new Document();
        LocalDate reportDate = LocalDate.parse(repDate, dateTimeFormatter);
        Page<MxCwtConfigMgtRight> configMgtRights = null;
        Page<MxCwtConfigMgtRight> compareConfigRigths = null;
        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
            if (type.equalsIgnoreCase("Matched")) {
                configMgtRights = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getTemplate(), reportDate, PageRequest.of(page, pageSize));
                compareConfigRigths = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getCompareTemplate(), reportDate, PageRequest.of(page, pageSize));
                return compareConfiguration(configMgtRights.getContent(), compareConfigRigths.getContent(), document, pageSize, type);
            } else {
                configMgtRights = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getTemplate(), reportDate, PageRequest.of(page, pageSize));
                compareConfigRigths = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getCompareTemplate(), reportDate, PageRequest.of(page, pageSize));
                return compareConfiguration(configMgtRights.getContent(), compareConfigRigths.getContent(), document, pageSize, type);
            }
        }
        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, false);
            String[] templateList = template.split("\\|");
            int i = 0;
            CompareSearchConfig<MxCwtConfigMgtRight> mxCwtSpecificationBuilder = null;

            mxCwtSpecificationBuilder = new CompareSearchConfig<>(templateList[i], null, templateList[++i], repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            if(mxCompareGroup.isGlobalSearch){
                CompareSearchConfig<MxCwtConfigMgtRight> finalMxCwtSpecificationBuilder = mxCwtSpecificationBuilder;
                CompletableFuture.runAsync(() -> finalMxCwtSpecificationBuilder.saveQueryMethod(true, true, mxCompareGroup.getSearchValue(), userName, mxCompareGroup.reportType));
                configMgtRights = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDateWithGlobalSearch(mxCompareGroup.getTemplate(), reportDate,mxCompareGroup.searchValue, PageRequest.of(page, pageSize));
                document.put("totalPages", configMgtRights.getTotalPages());
                document.put("records", configMgtRights.getTotalElements());
                document.put("content1", configMgtRights.getContent());
            }else {
                document = mxCwtSpecificationBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), false, false, true, false, false);
            }
            List<MxCwtConfigMgtRight> tempList = (List<MxCwtConfigMgtRight>) document.get("content1");
            if (tempList.size() == 0) {
                document.put("content2", new ArrayList<>());
                return document;
            }
            compareConfigRigths = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getCompareTemplate(), reportDate, PageRequest.of(page, tempList.size()));
            document.put("content2", compareConfigRigths.getContent());
            return document;

        } else {
            configMgtRights = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getTemplate(), reportDate, PageRequest.of(page, pageSize));
            compareConfigRigths = mxCwtConfigMgtRightRepository.findByGroupLabelAndReportDate(mxCompareGroup.getCompareTemplate(), reportDate, PageRequest.of(page, pageSize));

            if (compareConfigRigths.getTotalElements() > configMgtRights.getTotalElements()) {

                document.put("totalPages", compareConfigRigths.getTotalPages());
                document.put("records", compareConfigRigths.getTotalElements());
                document.put("content1", configMgtRights.getContent());
                document.put("content2", compareConfigRigths.getContent());
                return document;
            } else {
                document.put("totalPages", configMgtRights.getTotalPages());
                document.put("records", configMgtRights.getTotalElements());
                document.put("content1", configMgtRights.getContent());
                document.put("content2", compareConfigRigths.getContent());
                return document;
            }
        }
    }

    public Document getMultiGroupCompareFinance(String reportDate, MxCompareGroup mxCompareGroup, int page, int pageSize, String userName, String type) {

        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);

        Page<MxFinaceAcctrlRights> finaceAcctrlRights = null;
        Page<MxFinaceAcctrlRights> compareFinaceAcctrlRights = null;
        boolean isCompare = mxCompareGroup.template.equalsIgnoreCase("nil") || mxCompareGroup.compareTemplate.equalsIgnoreCase("nil");
        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
            if (isCompare) {
                document.put("message", "Can't match with null template");
                return document;
            }
            if (type.equalsIgnoreCase("Matched")) {
                finaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, pageSize));
                compareFinaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, pageSize));
                return compareFinanceByMatch(finaceAcctrlRights.getContent(), compareFinaceAcctrlRights.getContent(), document, pageSize, type);
            } else {
                finaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, pageSize));
                compareFinaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, pageSize));
                return compareFinanceByMatch(finaceAcctrlRights.getContent(), compareFinaceAcctrlRights.getContent(), document, pageSize, type);

            }
        }

        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), mxCompareGroup.getSubReportType(), false);
            String[] templateList = template.split("\\|");
            int i = 0;
            CompareSearchConfig<MxFinaceAcctrlRights> mxOspSpecificationBuilder = null;
            mxOspSpecificationBuilder = new CompareSearchConfig<>(templateList[i], null, templateList[++i], reportDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), null, templateList[++i], templateList[5],
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            if(mxCompareGroup.isGlobalSearch){
                CompareSearchConfig<MxFinaceAcctrlRights> finalMxOspSpecificationBuilder = mxOspSpecificationBuilder;
                CompletableFuture.runAsync(() -> finalMxOspSpecificationBuilder.saveQueryMethod(true, true, mxCompareGroup.getSearchValue(), userName, mxCompareGroup.reportType));

                finaceAcctrlRights = mxFinanceRightsRepo.findAllBySearchStringAndTemplate(mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), repDate,mxCompareGroup.searchValue, PageRequest.of(page, pageSize));
                document.put("totalPages", finaceAcctrlRights.getTotalPages());
                document.put("records", finaceAcctrlRights.getTotalElements());
                document.put("content1", finaceAcctrlRights.getContent()); }
            else {
                document = mxOspSpecificationBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), false, false, true, false, false);
            }
            List<MxFinaceAcctrlRights> tempList = (List<MxFinaceAcctrlRights>) document.get("content1");
            if (tempList.size() == 0) {
                document.put("content2", new ArrayList<>());
                return document;
            }
            compareFinaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, tempList.size()));
            document.put("content2", compareFinaceAcctrlRights.getContent());
            return document;

        } else {

            finaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, pageSize));
            compareFinaceAcctrlRights = mxFinanceRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), repDate, PageRequest.of(page, pageSize));

            if (compareFinaceAcctrlRights.getTotalElements() > finaceAcctrlRights.getTotalElements()) {

                document.put("totalPages", compareFinaceAcctrlRights.getTotalPages());
                document.put("records", compareFinaceAcctrlRights.getTotalElements());
                document.put("content1", finaceAcctrlRights.getContent());
                document.put("content2", compareFinaceAcctrlRights.getContent());
                return document;
            } else {
                document.put("totalPages", finaceAcctrlRights.getTotalPages());
                document.put("records", finaceAcctrlRights.getTotalElements());
                document.put("content1", finaceAcctrlRights.getContent());
                document.put("content2", compareFinaceAcctrlRights.getContent());
                return document;
            }
        }
    }

    public Document getMultiGroupCompareChinese(String reportDate, MxCompareGroup mxCompareGroup, ChineseWall filters, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {

        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);

        String monShortName = repDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), null, true);
        String[] templateList = template.split("\\|");
        int i = 1;
        CompareSearchConfig<MxChineseWallTmpl> mxChineseSpecBuilder = null;
        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {

            mxChineseSpecBuilder = new CompareSearchConfig<>("", ChineseWall.class, MxGroupCompChinese.class, templateList[i] + " PARTITION (P_" + monShortName + ")", reportDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                    filters.counterPartLabelList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
        } else if (isMatched || isAdditionalMissing) {
            mxChineseSpecBuilder = new CompareSearchConfig<>("", ChineseWall.class, templateList[i] + " PARTITION (P_" + monShortName + ")", reportDate
                    , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), null, null, templateList[++i], null,
                    filters.counterPartLabelList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
        } else {
            mxChineseSpecBuilder = new CompareSearchConfig<>("", ChineseWall.class, templateList[i] + " PARTITION (P_" + monShortName + ")", reportDate
                    , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), null, null, templateList[++i], null
                    , filters.counterPartLabelList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
        }

        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {

            if (mxCompareGroup.isGlobalSearch) {
                document = mxChineseSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true,
                        false, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxChineseWallTmpl.class, true);
            } else {
                document = mxChineseSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true,
                        false, isMatched, isAdditionalMissing, false, repDate.getDayOfMonth(), repDate.getYear(), MxChineseWallTmpl.class, false);
            }
        } else {
            document = mxChineseSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                    true, false, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxChineseWallTmpl.class, false);
        }
        List<MxChineseWallTmpl> chineaseWallLst = (List<MxChineseWallTmpl>) document.get("content1");
        if (chineaseWallLst.size() == 0 || !isMatched ) {
            document.put("content2", new ArrayList<>());
        } else {
            List<String> orderByFields = (List<String>) document.get("orderByFields");
            List<String> treeMapList = chineaseWallLst.stream().map(MxChineseWallTmpl::getCounterpartLabel).collect(Collectors.toList());
            document.put("content2", mxChineseSpecBuilder.getCompareGroupSearchWithPartition(null, repDate, mxCompareGroup, repDate.getDayOfMonth(), repDate.getYear(),
                    isMatched, isAdditionalMissing, MxChineseWallTmpl.class, orderByFields, treeMapList));
        }
        return document;
    }

    public Document getMultiGroupCompareOperRights(String reportDate, MxCompareGroup mxCompareGroup, OperationalRights filter, int page, int pageSize, boolean isMatched, boolean isAdditionalMissing, String userName, String type) {

        Document document = new Document();
        Page<MxOperationRights> operationRights = null;
        Page<MxOperationRights> compareOperationRights = null;
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);
        List<String> treeMapList = new ArrayList<>();

        if (mxCompareGroup.getSearchValue() != null && !mxCompareGroup.getSearchValue().isBlank()) {
            String template = viewerExportService.getPropertyFields(mxCompareGroup.getReportType(), mxCompareGroup.getSubReportType(), mxCompareGroup.isGlobalSearch);
            String[] templateList = template.split("\\|");
            int i = 0;

            CompareSearchConfig<MxOperationRights> mxOperSpecBuilder = null;
            if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                mxOperSpecBuilder = new CompareSearchConfig<>(templateList[i], OperationalRights.class, MxGroupCompareOperation.class, templateList[++i], reportDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), null, templateList[++i], templateList[5],
                        filter.keyList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo, type);
            } else if (isMatched || isAdditionalMissing) {
                mxOperSpecBuilder = new CompareSearchConfig<>(templateList[i], OperationalRights.class, templateList[++i], reportDate
                        , mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), null, templateList[++i], templateList[5],
                        filter.keyList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            } else {
                mxOperSpecBuilder = new CompareSearchConfig<>(templateList[i], OperationalRights.class, templateList[++i], reportDate
                        , mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), null, templateList[++i], templateList[5]
                        , filter.keyList, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), page, pageSize, entityManager, historyRepo);
            }
            if(mxCompareGroup.isGlobalSearch){
                document = mxOperSpecBuilder.getCompareSearchWithPartition(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(),
                        true, true, isMatched, isAdditionalMissing, true, repDate.getDayOfMonth(), repDate.getYear(), MxOperationRights.class, true);
            }else {
                document = mxOperSpecBuilder.getCompareSearch(mxCompareGroup.getSearchValue(), userName, mxCompareGroup.getReportType(), true, true, isMatched, isAdditionalMissing, false);
            }
            List<MxOperationRights> operationalRights = (List<MxOperationRights>) document.get("content1");
            treeMapList = operationalRights.stream().map(MxOperationRights::getKey).collect(Collectors.toList());
            document.put("content2", findCompareOperationRightsByPage(treeMapList, repDate, mxCompareGroup));
            return document;

        } else {
            if (isMatched) {
                if (type.equalsIgnoreCase("Matched")) {
                    operationRights = mxOperationRightsRepo.findAllGroupCompareOperMatchedList(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                    compareOperationRights = mxOperationRightsRepo.findAllGroupCompareOperMatchedList(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                } else if (type.equalsIgnoreCase("Unmatched")){
                    operationRights = mxOperationRightsRepo.findAllGroupCompareOperUnMatched(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                    compareOperationRights = mxOperationRightsRepo.findAllGroupCompareOperUnMatched(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                }else {
                    operationRights = mxOperationRightsRepo.findAllGroupCompareOperRightsMatched(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                    compareOperationRights = mxOperationRightsRepo.findAllGroupCompareOperRightsMatched(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                }
                document.put("content2", compareOperationRights.getContent());
            }
            else if (isAdditionalMissing) {
                operationRights = mxOperationRightsRepo.findAllGroupCompareOperRightsAdditional(repDate, mxCompareGroup.getTemplate(), mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                document.put("content2", new ArrayList<>());
            } else {
                operationRights = mxOperationRightsRepo.findAllGroupCompareOperRightsAdditional(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getTemplate(), mxCompareGroup.getGroupLabel(), filter, PageRequest.of(page, pageSize));
                document.put("content2", new ArrayList<>());
            }
            document.put("totalPages", operationRights.getTotalPages());
            document.put("records", operationRights.getTotalElements());
            document.put("content1", operationRights.getContent());
            return document;
        }
    }

    public Document getAllFilters(String reportDate, GroupCompareFilter filter, boolean isMatched, boolean isAdditionalMissing, int page, int pageSize, String search, String type) {
        Document document = new Document();
        LocalDate repDate = LocalDate.parse(reportDate, dateTimeFormatter);

        switch (filter.getReportType()) {
            case "OSP_RIGHTS":
                if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                    List<MxOspRightsMatrix> filterList = null;
                    filterList = (List<MxOspRightsMatrix>) getSearchResults(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, search, type, OspRightsMatrix.class, MxGroupCompareOsp.class,false, MxOspRightsMatrix.class);
                    return findDistinctValuesForUniqueKey(filterList, document, filter, true);
                } else {
                    List<OspRightsMatrix> matrixList = null;

                    if (isMatched) {
                        if (type.equalsIgnoreCase("matched")) {
                            matrixList = ospRightsTemplateRepository.findGeneralPropertyListForOspMatched(filter, repDate);
                        } else if (type.equalsIgnoreCase("unmatched")) {
                            matrixList = ospRightsTemplateRepository.findGeneralPropertyListForOspUnMatched(filter, repDate);
                        } else {
                            matrixList = ospRightsTemplateRepository.findGeneralPropertyListForOspAll(filter, repDate);
                        }
                        return findDistinctValuesForUniqueKey(matrixList, document, filter, false);

                    } else {
                        if (isAdditionalMissing) {
                            matrixList = ospRightsTemplateRepository.findGeneralPropertyListForOspAdditional(filter.getTemplate(), filter.getCompareTemplate(), repDate);

                        } else {
                            matrixList = ospRightsTemplateRepository.findGeneralPropertyListForOspAdditional(filter.getCompareTemplate(), filter.getTemplate(), repDate);
                        }
                        return findDistinctValuesForUniqueKey(matrixList, document, filter, false);

                    }
                }
            case "NAVIGATION_RIGHTS":
                List<MxGroupNavigationRight> filterListed = null;
                filterListed = (List<MxGroupNavigationRight>) getSearchResults(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, search, type, NavigationRights.class, MxGroupCompNavigation.class,true,MxGroupNavigationRight.class);
                return findDistinctValuesForUniqueKey(filterListed, document, filter, true);

//            case "STP_RIGHTS":
//                List<STPModel> stpRights = null;
//                repDate = viewerExportService.getLatestDate();
//                if (isMatched) {
//                    stpRights = stpRightsRepository.findGeneralPropertyListForStpMatched(filter.getTemplate(), filter.getCompareTemplate(), repDate);
//                    return findDistinctValuesForUniqueKey(stpRights, document, filter,false);
//                } else {
//                    if (isAdditionalMissing) {
//                        stpRights = stpRightsRepository.findGeneralPropertyListForStpMatched(filter.getTemplate(), filter.getCompareTemplate(), repDate);
//                        return findDistinctValuesForUniqueKey(stpRights, document, filter,false);
//
//                    } else {
//                        stpRights = stpRightsRepository.findGeneralPropertyListForStpMatched(filter.getCompareTemplate(), filter.getTemplate(), repDate);
//                        return findDistinctValuesForUniqueKey(stpRights, document, filter,false);
//
//                    }
//                }
            case "COMBINED_PORTFOLIO":
                if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                    List<MxGroupCombinedPortfolio> filteredList = null;
                    filteredList = (List<MxGroupCombinedPortfolio>) getSearchResults(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, search, type, GroupCompPortfolio.class, MxGroupCompPortfolio.class,false,MxGroupCombinedPortfolio.class);
                    return findDistinctValuesForUniqueKey(filteredList, document, filter, true);
                } else {
                    List<GroupCompPortfolio> combPortfolioRights = null;
                    if (isMatched) {
                        if (type.equalsIgnoreCase("matched")) {
                            combPortfolioRights = mxGroupCombinedPortfolioRepo.findGeneralPropertyListForCombPortMatched(filter, repDate);
                        } else if (type.equalsIgnoreCase("unmatched")) {
                            combPortfolioRights = mxGroupCombinedPortfolioRepo.findGeneralPropertyListForCombPorUnMatched(filter, repDate);
                        } else {
                            combPortfolioRights = mxGroupCombinedPortfolioRepo.findGeneralPropertyListForCombPortAll(filter, repDate);
                        }
                        return findDistinctValuesForUniqueKey(combPortfolioRights, document, filter, false);
                    } else {
                        if (isAdditionalMissing) {
                            combPortfolioRights = mxGroupCombinedPortfolioRepo.findGeneralPropertyListForCombPortAdditional(filter.getTemplate(), filter.getCompareTemplate(), repDate);
                        } else {
                            combPortfolioRights = mxGroupCombinedPortfolioRepo.findGeneralPropertyListForCombPortAdditional(filter.getCompareTemplate(), filter.getTemplate(), repDate);
                        }
                        return findDistinctValuesForUniqueKey(combPortfolioRights, document, filter, false);
                    }
                }

            case "CONSISTENCY_TEMPLATE":
                if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                    List<MxConsistencyTmpl> filteredList = null;
                    filteredList = (List<MxConsistencyTmpl>) getSearchResults(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, search, type, ConsistencyTemplateRights.class, MxConsistencyGroupCompFilter.class,false,MxConsistencyTmpl.class);
                    return findDistinctValuesForUniqueKey(filteredList, document, filter, true);
                } else {
                    List<ConsistencyTemplateRights> consistencyTemplateRights = null;
                    if (isMatched) {
                        if (type.equalsIgnoreCase("matched")) {
                            consistencyTemplateRights = mxConsistencyTmplRepo.findGeneralPropertyListForConsistencyMatchedList(repDate, filter);
                        } else if (type.equalsIgnoreCase("unmatched")) {
                            consistencyTemplateRights = mxConsistencyTmplRepo.findGeneralPropertyListForConsistencyUnMatched(repDate, filter);
                        } else {
                            consistencyTemplateRights = mxConsistencyTmplRepo.findGeneralPropertyListForConsistencyMatched(filter, repDate);
//                        log.info("full list:{}",consistencyTemplateRights.stream().limit(pageSize).collect(Collectors.toList()));
                        }
                        return findDistinctValuesForUniqueKey(consistencyTemplateRights, document, filter, false);
                    } else {
                        if (isAdditionalMissing) {
                            consistencyTemplateRights = mxConsistencyTmplRepo.findGeneralPropertyListForConsistencyAdditional(filter.template, filter.compareTemplate, repDate);

                        } else {
                            consistencyTemplateRights = mxConsistencyTmplRepo.findGeneralPropertyListForConsistencyAdditional(filter.compareTemplate, filter.template, repDate);
                        }
                        return findDistinctValuesForUniqueKey(consistencyTemplateRights, document, filter, false);

                    }
                }
            case "CHINESE_WALL_DEFINITIONS":
                List<MxChineseWallTmpl> filterList = (List<MxChineseWallTmpl>) getSearchResults(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, search, type, ChineseWall.class, MxGroupCompChinese.class,true, MxChineseWallTmpl.class);
                document.put("content1", filterList.stream().map(MxChineseWallTmpl::getCounterpartLabel).collect(Collectors.toList()));
                document.put("content2", new ArrayList<>());
                document.put("content3", new ArrayList<>());
                document.put("content4", new ArrayList<>());
                document.put("content5", new ArrayList<>());
                return document;


            case "PORTFOLIO_RIGHTS":

                List<MxGroupPortfolioRights>     portfolioRights = (List<MxGroupPortfolioRights>) getSearchResults(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, search, type, PortfolioRights.class, MxGroupComparePortfolio.class,true,MxGroupPortfolioRights.class);
                document.put("content1", portfolioRights.stream().map(MxGroupPortfolioRights::getPortfolioLabel).collect(Collectors.toList()));
                document.put("content2", new ArrayList<>());
                document.put("content3", new ArrayList<>());
                document.put("content4", new ArrayList<>());
                document.put("content5", new ArrayList<>());
                return document;


            case "OPERATION_RIGHTS":
//                if (filter.getSearchQuery() != null && !filter.getSearchQuery().isBlank()) {
                List<MxOperationRights> matrixList = null;

                matrixList = (List<MxOperationRights>) getSearchResultsWithSubTemplate(reportDate, filter, isMatched, isAdditionalMissing, page, pageSize, type, OperationalRights.class, MxGroupCompareOperation.class);
                document.put("content1", matrixList.stream().map(MxOperationRights::getKey).collect(Collectors.toList()));
                document.put("content2", new ArrayList<>());
                document.put("content3", new ArrayList<>());
                document.put("content4", new ArrayList<>());
                document.put("content5", new ArrayList<>());
                return document;
        }

        return document;
    }

    private List<?> getSearchResults(String reportDate, GroupCompareFilter filter, boolean isMatched, boolean isAdditionalMissing, int page, int pageSize,
                                     String search, String type, Class primaryClass, Class typeSpecifierClass,boolean isPartition,Class entityClass) {

        String template ="";
        String monthNameForPartition = "";
        LocalDate repDate =LocalDate.parse(reportDate, dateTimeFormatter);;
        if(isPartition || filter.isGlobalSearch) {
            template  = viewerExportService.getPropertyFields(filter.getReportType(), null, true);
            monthNameForPartition   =  isPartition ?" PARTITION (P_" +  repDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase()+ ")":"";
        }else{
            template =  viewerExportService.getPropertyFields(filter.getReportType(), null, false);
        }
        String[] templateList = template.split("\\|");
        int i = 0;
        List<?> resultList = new ArrayList<>();
        String nav = "";
        if (filter.getReportType().equalsIgnoreCase("NAVIGATION_RIGHTS")) {
            nav = ",groupLabel";
            if(isPartition) {
                nav = ",GROUP_LABEL";
            }
        }
        CompareSearchConfig<?> filterSpecBuilder = null;

        if(isPartition || filter.isGlobalSearch ) {

           if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                filterSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, typeSpecifierClass, templateList[++i]+monthNameForPartition, reportDate
                        , filter.getTemplate(), filter.getCompareTemplate(), filter.getGroupLabel(), filter.getCompareGroupLabel(), templateList[++i] + nav, null
                        , page, pageSize, entityManager, type);
            } else if (isMatched || isAdditionalMissing) {
                filterSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, null, templateList[++i]+monthNameForPartition, reportDate
                        , filter.getTemplate(), filter.getCompareTemplate(), filter.getGroupLabel(), filter.getCompareGroupLabel(), templateList[++i] + nav, null,
                        page, pageSize, entityManager, null);
            } else {
                filterSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, null, templateList[++i]+monthNameForPartition, reportDate
                        , filter.getCompareTemplate(), filter.getTemplate(), filter.getCompareGroupLabel(), filter.getGroupLabel(), templateList[++i] + nav, null
                        , page, pageSize, entityManager, null);
            }
        }
        else{
            if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
                filterSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, typeSpecifierClass, templateList[++i], reportDate
                        , filter.getTemplate(), filter.getCompareTemplate(), filter.getGroupLabel(), filter.getCompareGroupLabel(), templateList[++i] + nav, null
                        , page, pageSize, entityManager, type);
            } else if (isMatched || isAdditionalMissing) {
                filterSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, null, templateList[++i], reportDate
                        , filter.getTemplate(), filter.getCompareTemplate(), filter.getGroupLabel(), filter.getCompareGroupLabel(), templateList[++i] + nav, null,
                        page, pageSize, entityManager, null);
            } else {
                filterSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, null, templateList[++i], reportDate
                        , filter.getCompareTemplate(), filter.getTemplate(),  filter.getCompareGroupLabel(), filter.getGroupLabel(), templateList[++i] + nav, null
                        , page, pageSize, entityManager, null);
            }
        }
        if(filter.isGlobalSearch){
            if (search != null && !search.isBlank()) {
                resultList = filterSpecBuilder.getCompareFilterSearch(filter.getSearchQuery(), filter.getReportType(), true, true, isMatched, isAdditionalMissing, true, "t.COUNTERPART_LABEL like '%" + search + "%'",repDate.getDayOfMonth(),repDate.getYear(),entityClass, filter.isGlobalSearch);
            } else {
                resultList = filterSpecBuilder.getCompareFilterSearch(filter.getSearchQuery(), filter.getReportType(), true, true, isMatched, isAdditionalMissing, true, "", repDate.getDayOfMonth(),repDate.getYear(),entityClass,filter.isGlobalSearch);
            }
        }
        else {
            if (search != null && !search.isBlank()) {
                resultList = filterSpecBuilder.getCompareFilterSearch(filter.getSearchQuery(), filter.getReportType(), true, true, isMatched, isAdditionalMissing, false, "t.COUNTERPART_LABEL like '%" + search + "%'", repDate.getDayOfMonth(),repDate.getYear(), entityClass, filter.isGlobalSearch);
            } else {
                resultList = filterSpecBuilder.getCompareFilterSearch(filter.getSearchQuery(), filter.getReportType(), true, true, isMatched, isAdditionalMissing, true, "",  repDate.getDayOfMonth(),repDate.getYear(), entityClass, filter.isGlobalSearch);
            }
        }
        return resultList;
    }

    private List<?> getSearchResultsWithSubTemplate(String reportDate, GroupCompareFilter filter, boolean isMatched, boolean isAdditionalMissing, int page, int pageSize, String type, Class primaryClass, Class typeSpecifierClass) {

        String template = viewerExportService.getPropertyFields(filter.getReportType(), filter.getSubReportType(), filter.isGlobalSearch);
        String[] templateList = template.split("\\|");
        int i = 0;
        List<?> resultList = new ArrayList<>();
        CompareSearchConfig<?> mxSpecBuilder = null;
        if (type.equalsIgnoreCase("Matched") || type.equalsIgnoreCase("Unmatched")) {
            mxSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, typeSpecifierClass, templateList[++i], reportDate
                    , filter.getTemplate(), filter.getCompareTemplate(), filter.getGroupLabel(), null, templateList[++i], templateList[5]
                    , page, pageSize, entityManager, type);
        } else if (isMatched || isAdditionalMissing) {
            mxSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, null, templateList[++i], reportDate
                    , filter.getTemplate(), filter.getCompareTemplate(), filter.getGroupLabel(), null, templateList[++i], templateList[5],
                    page, pageSize, entityManager, null);
        } else {
            mxSpecBuilder = new CompareSearchConfig<>(templateList[i], primaryClass, null, templateList[++i], reportDate
                    , filter.getCompareTemplate(), filter.getTemplate(), filter.getGroupLabel(), null, templateList[++i], templateList[5]
                    , page, pageSize, entityManager, null);
        }
        resultList = mxSpecBuilder.getCompareFilterSearch(filter.getSearchQuery(), filter.getReportType(), true, true, isMatched, isAdditionalMissing, true, "",0,0, MxOperationRights.class, filter.isGlobalSearch);
        return resultList;
    }

    public Document findDistinctValuesForUniqueKey(List<?> matchedList, Document document, GroupCompareFilter filter, boolean search) {

        switch (filter.getReportType()) {
            case "OSP_RIGHTS": {
                if (search) {
                    List<MxOspRightsMatrix> tempList = null;
                    List<MxOspRightsMatrix> matrixList = (List<MxOspRightsMatrix>) matchedList;
                    document.put("content1", matrixList.stream().map(MxOspRightsMatrix::getValidationRightTemplate).distinct().collect(Collectors.toList()));
                    if (!filter.key0.isEmpty()) {
                        tempList = matrixList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.getValidationRightTemplate()))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(MxOspRightsMatrix::getCategory).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.getCategory()))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(MxOspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getSubCategory()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", tempList.stream().map(MxOspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getSubCategory()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        }
                    } else {
                        document.put("content2", matrixList.stream().map(MxOspRightsMatrix//        List<String> label = treeMapList.stream().map(ChineseWall::getCounterpartLabel).collect(Collectors.toList());
                                ::getCategory).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = matrixList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.getCategory()))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(MxOspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getSubCategory()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", matrixList.stream().map(MxOspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = matrixList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getSubCategory()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", matrixList.stream().map(MxOspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        }

                    }
                    document.put("content5", new ArrayList<>());

                    return document;

                } else {
                    List<OspRightsMatrix> tempList = null;
                    List<OspRightsMatrix> matrixList = (List<OspRightsMatrix>) matchedList;
                    document.put("content1", matrixList.stream().map(OspRightsMatrix::getValidationRightTemplate).distinct().collect(Collectors.toList()));
                    if (!filter.key0.isEmpty()) {
                        tempList = matrixList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.validationRightTemplate))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(OspRightsMatrix::getCategory).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.category))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(OspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.subCategory))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", tempList.stream().map(OspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.subCategory))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        }
                    } else {
                        document.put("content2", matrixList.stream().map(OspRightsMatrix//        List<String> label = treeMapList.stream().map(ChineseWall::getCounterpartLabel).collect(Collectors.toList());
                                ::getCategory).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = matrixList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.category))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(OspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.subCategory))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", matrixList.stream().map(OspRightsMatrix::getSubCategory).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = matrixList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.subCategory))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", matrixList.stream().map(OspRightsMatrix::getQueue).distinct().collect(Collectors.toList()));
                            }
                        }

                    }
                    document.put("content5", new ArrayList<>());

                    return document;
                }
            }
            case "NAVIGATION_RIGHTS": {
                if (search) {
                    List<MxGroupNavigationRight> tempList = null;
                    List<MxGroupNavigationRight> navigationList = (List<MxGroupNavigationRight>) matchedList;
                    document.put("content1", navigationList.stream().map(MxGroupNavigationRight::getComments).distinct().collect(Collectors.toList()));
                    if (!filter.key0.isEmpty()) {
                        tempList = navigationList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.getComments()))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(MxGroupNavigationRight::getRights).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.getRights()))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(MxGroupNavigationRight::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getMenu()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", tempList.stream().map(MxGroupNavigationRight::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getMenu()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));
                            }
                        }
                    } else {
                        document.put("content2", navigationList.stream().map(MxGroupNavigationRight::getRights).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = navigationList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.getRights()))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(MxGroupNavigationRight::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getMenu()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", navigationList.stream().map(MxGroupNavigationRight::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = navigationList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.getMenu()))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", navigationList.stream().map(MxGroupNavigationRight::getPath).distinct().collect(Collectors.toList()));
                            }
                        }

                    }
                    document.put("content5", new ArrayList<>());
                    return document;
                } else {
                    List<NavigationRights> tempList = null;
                    List<NavigationRights> navigationList = (List<NavigationRights>) matchedList;
                    document.put("content1", navigationList.stream().map(NavigationRights::getComments).distinct().collect(Collectors.toList()));
                    if (!filter.key0.isEmpty()) {
                        tempList = navigationList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.comments))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(NavigationRights::getRights).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.rights))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(NavigationRights::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.menu))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", tempList.stream().map(NavigationRights::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.menu))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));
                            }
                        }
                    } else {
                        document.put("content2", navigationList.stream().map(NavigationRights::getRights).distinct().collect(Collectors.toList()));
                        if (!filter.key1.isEmpty()) {
                            tempList = navigationList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.rights))).collect(Collectors.toList());
                            document.put("content3", tempList.stream().map(NavigationRights::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.menu))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content3", navigationList.stream().map(NavigationRights::getMenu).distinct().collect(Collectors.toList()));
                            if (!filter.key2.isEmpty()) {
                                tempList = navigationList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.menu))).collect(Collectors.toList());
                                document.put("content4", tempList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content4", navigationList.stream().map(NavigationRights::getPath).distinct().collect(Collectors.toList()));
                            }
                        }

                    }
                    document.put("content5", new ArrayList<>());
                    return document;
                }
            }
            case "STP_RIGHTS": {
                List<STPModel> tempList = null;
                List<STPModel> stpList = (List<STPModel>) matchedList;

                document.put("content1", stpList.stream().map(STPModel::getBoType).distinct().collect(Collectors.toList()));
                if (!filter.key0.isEmpty()) {
                    tempList = stpList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.boType))).collect(Collectors.toList());
                    document.put("content2", tempList.stream().map(STPModel::getTypology).distinct().collect(Collectors.toList()));
                    if (!filter.key1.isEmpty()) {
                        tempList = tempList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.typology))).collect(Collectors.toList());
                        document.put("content3", tempList.stream().map(STPModel::getAction).distinct().collect(Collectors.toList()));
                        if (!filter.key2.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.action))).collect(Collectors.toList());
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        }
                    } else {
                        document.put("content3", tempList.stream().map(STPModel::getAction).distinct().collect(Collectors.toList()));
                        if (!filter.key2.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.action))).collect(Collectors.toList());
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        }
                    }
                } else {
                    document.put("content2", stpList.stream().map(STPModel::getTypology).distinct().collect(Collectors.toList()));
                    if (!filter.key1.isEmpty()) {
                        tempList = stpList.stream().filter(e -> filter.key1.stream().anyMatch(m -> m.equals(e.typology))).collect(Collectors.toList());
                        document.put("content3", tempList.stream().map(STPModel::getAction).distinct().collect(Collectors.toList()));
                        if (!filter.key2.isEmpty()) {
                            tempList = tempList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.action))).collect(Collectors.toList());
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        }
                    } else {
                        document.put("content3", stpList.stream().map(STPModel::getAction).distinct().collect(Collectors.toList()));
                        if (!filter.key2.isEmpty()) {
                            tempList = stpList.stream().filter(e -> filter.key2.stream().anyMatch(m -> m.equals(e.action))).collect(Collectors.toList());
                            document.put("content4", tempList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = tempList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        } else {
                            document.put("content4", stpList.stream().map(STPModel::getStatus).distinct().collect(Collectors.toList()));
                            if (!filter.key3.isEmpty()) {
                                tempList = stpList.stream().filter(e -> filter.key3.stream().anyMatch(m -> m.equals(e.status))).collect(Collectors.toList());
                                document.put("content5", tempList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));

                            } else {
                                document.put("content5", stpList.stream().map(STPModel::getStpView).distinct().collect(Collectors.toList()));
                            }
                        }
                    }

                }
                return document;
            }
            case "COMBINED_PORTFOLIO": {

                if (search) {
                    List<MxGroupCombinedPortfolio> tempList = null;
                    List<MxGroupCombinedPortfolio> compPortfolioList = (List<MxGroupCombinedPortfolio>) matchedList;

                    document.put("content1", compPortfolioList.stream().map(MxGroupCombinedPortfolio::getCompfLbl).distinct().collect(Collectors.toList()));

                    if (!filter.key0.isEmpty()) {
                        tempList = compPortfolioList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.getCompfLbl()))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(MxGroupCombinedPortfolio::getUnit).distinct().collect(Collectors.toList()));

                    } else {
                        document.put("content2", compPortfolioList.stream().map(MxGroupCombinedPortfolio::getUnit).distinct().collect(Collectors.toList()));
                    }
                } else {
                    List<GroupCompPortfolio> tempList = null;
                    List<GroupCompPortfolio> compPortfolioList = (List<GroupCompPortfolio>) matchedList;

                    document.put("content1", compPortfolioList.stream().map(GroupCompPortfolio::getCompfLbl).distinct().collect(Collectors.toList()));

                    if (!filter.key0.isEmpty()) {
                        tempList = compPortfolioList.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.compfLbl))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(GroupCompPortfolio::getUnit).distinct().collect(Collectors.toList()));

                    } else {
                        document.put("content2", compPortfolioList.stream().map(GroupCompPortfolio::getUnit).distinct().collect(Collectors.toList()));
                    }
                }
                document.put("content3", new ArrayList<>());
                document.put("content4", new ArrayList<>());
                document.put("content5", new ArrayList<>());
                return document;
            }
            case "CONSISTENCY_TEMPLATE": {
                if (search) {
                    List<MxConsistencyTmpl> tempList = null;
                    List<MxConsistencyTmpl> consistencyTmplRights = (List<MxConsistencyTmpl>) matchedList;

                    document.put("content1", consistencyTmplRights.stream().map(MxConsistencyTmpl::getCategory).distinct().collect(Collectors.toList()));

                    if (!filter.key0.isEmpty()) {
                        tempList = consistencyTmplRights.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.getCategory()))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(MxConsistencyTmpl::getItem).distinct().collect(Collectors.toList()));

                    } else {
                        document.put("content2", consistencyTmplRights.stream().map(MxConsistencyTmpl::getItem).distinct().collect(Collectors.toList()));
                    }
                } else {
                    List<ConsistencyTemplateRights> tempList = null;
                    List<ConsistencyTemplateRights> consistencyTmplRights = (List<ConsistencyTemplateRights>) matchedList;

                    document.put("content1", consistencyTmplRights.stream().map(ConsistencyTemplateRights::getCategory).distinct().collect(Collectors.toList()));

                    if (!filter.key0.isEmpty()) {
                        tempList = consistencyTmplRights.stream().filter(e -> filter.key0.stream().anyMatch(m -> m.equals(e.category))).collect(Collectors.toList());
                        document.put("content2", tempList.stream().map(ConsistencyTemplateRights::getItem).distinct().collect(Collectors.toList()));

                    } else {
                        document.put("content2", consistencyTmplRights.stream().map(ConsistencyTemplateRights::getItem).distinct().collect(Collectors.toList()));
                    }
                }
                document.put("content3", new ArrayList<>());
                document.put("content4", new ArrayList<>());
                document.put("content5", new ArrayList<>());
                return document;
            }

        }
        return document;

    }


    public List<MxGroupPortfolioRights> findComparePortfolioRightsByPage(List<String> treeMapList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
        return mxPortfolioRightsRepository.findMatchedDataByPortfolioTree(repDate, mxCompareGroup.getCompareTemplate(), treeMapList);
    }


    public List<MxOperationRights> findCompareOperationRightsByPage(List<String> treeMapList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
        return mxOperationRightsRepo.findMatchedOperationByTree(repDate, mxCompareGroup.getCompareTemplate(), mxCompareGroup.getGroupLabel(), treeMapList);

    }

    public List<MxChineseWallTmpl> findCompareChineaseWallRightsByPage(List<String> treeMapList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
        return mxChineseWallTemplateRepository.findMatchedChineaseWallByTree(repDate, mxCompareGroup.getCompareTemplate(), treeMapList);
    }

    private List<MxConsistencyTmpl> findCompareConsistRightsByPage(List<ConsistencyTemplateRights> treeMapList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
        List<String> categoryList = treeMapList.stream().map(ConsistencyTemplateRights::getCategory).collect(Collectors.toList());
        List<String> itemList = treeMapList.stream().map(ConsistencyTemplateRights::getItem).distinct().collect(Collectors.toList());
        return mxConsistencyTmplRepo.findMatchedDataByConsistencyTree(repDate, mxCompareGroup.getCompareTemplate(), categoryList, itemList);
    }

//    public List<STPRightsMatrix> findCompareStpRightsByPage(List<STPModel> treeList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
//        List<String> boTypeList = treeList.stream().map(STPModel::getBoType).collect(Collectors.toList());
//        List<String> typologyList = treeList.stream().map(STPModel::getTypology).distinct().collect(Collectors.toList());
//        List<String> actionList = treeList.stream().map(STPModel::getAction).collect(Collectors.toList());
//        List<String> statusList = treeList.stream().map(STPModel::getStatus).collect(Collectors.toList());
//        List<String> viewList = treeList.stream().map(STPModel::getStpView).collect(Collectors.toList());
//        return stpRightsRepository.findMatchedDataByStpTree(repDate, mxCompareGroup.getCompareTemplate(), boTypeList, typologyList, actionList, statusList, viewList);
//    }


    public List<MxGroupCombinedPortfolio> findCompareCompPortfolioByPage(List<GroupCompPortfolio> treeMapList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
        List<String> compList = treeMapList.stream().map(GroupCompPortfolio::getCompfLbl).collect(Collectors.toList());
        List<String> unitList = treeMapList.stream().map(GroupCompPortfolio::getUnit).distinct().collect(Collectors.toList());
        return mxGroupCombinedPortfolioRepo.findMatchedDataByCompTree(repDate, mxCompareGroup.getCompareTemplate(), compList, unitList);

    }

    public List<?> findCompareNavRightsByPage(CompareSearchConfig<MxGroupNavigationRight> mxNavSpecificationBuilder, List<NavigationRights> treeMapList,
                                              LocalDate repDate, MxCompareGroup mxCompareGroup, boolean isMatched, boolean isAdditionalMissing, List<String> orderByFields) {
        HashMap<String, List<String>> keyMap = new HashMap<>();

        keyMap.put("COMMENTS", treeMapList.stream().map(NavigationRights::getComments).collect(Collectors.toList()));
        keyMap.put("RIGHTS", treeMapList.stream().map(NavigationRights::getRights).collect(Collectors.toList()));

        keyMap.put("MENU", treeMapList.stream().map(NavigationRights::getMenu).collect(Collectors.toList()));
        keyMap.put("PATH", treeMapList.stream().map(NavigationRights::getPath).collect(Collectors.toList()));

        return mxNavSpecificationBuilder.getCompareGroupSearchWithPartition(keyMap, repDate, mxCompareGroup, repDate.getDayOfMonth(), repDate.getYear(),
                isMatched, isAdditionalMissing, MxGroupNavigationRight.class, orderByFields, null);
    }

    public List<MxOspRightsMatrix> findCompareOspRightsByPage(List<OspRightsMatrix> treeMapList, LocalDate repDate, MxCompareGroup mxCompareGroup) {
        List<String> validation = new ArrayList<>();
        List<String> category = new ArrayList<>();
        List<String> subCategory = new ArrayList<>();
        List<String> queue = new ArrayList<>();
        List<String> action = new ArrayList<>();
        CompletableFuture<Void> keyFuture = CompletableFuture.runAsync(() -> {
            validation.addAll(treeMapList.stream().map(OspRightsMatrix::getValidationRightTemplate).collect(Collectors.toList()));
        });
        CompletableFuture<Void> keyFuture1 = CompletableFuture.runAsync(() -> {
            category.addAll(treeMapList.stream().map(OspRightsMatrix::getCategory).distinct().collect(Collectors.toList()));
        });
        CompletableFuture<Void> keyFuture2 = CompletableFuture.runAsync(() -> {
            subCategory.addAll(treeMapList.stream().map(OspRightsMatrix::getSubCategory).collect(Collectors.toList()));
        });
        CompletableFuture<Void> keyFuture3 = CompletableFuture.runAsync(() -> {
            queue.addAll(treeMapList.stream().map(OspRightsMatrix::getQueue).collect(Collectors.toList()));
        });
        CompletableFuture<Void> keyFuture4 = CompletableFuture.runAsync(() -> {
            action.addAll(treeMapList.stream().map(OspRightsMatrix::getAction).collect(Collectors.toList()));
        });
        CompletableFuture.allOf(keyFuture, keyFuture1, keyFuture2, keyFuture3, keyFuture4).join();
        return ospRightsTemplateRepository.findMatchedDataByTree(repDate, mxCompareGroup.getCompareTemplate(), validation, category, subCategory, queue, action);
    }

    public Document compareEnterprise(List<MxEnterpriseRisk> enterpriseRisks, List<MxEnterpriseRisk> compareEnterpriseRisks, Document document, int pageSize, String type) {
        int index = 0;
        List<MxEnterpriseRisk> enterpriseRisksList = new ArrayList<>();
        List<MxEnterpriseRisk> compareEnterpriseRisksList = new ArrayList<>();
        List<MxEnterpriseRisk> matchedList = new ArrayList<>();
        List<MxEnterpriseRisk> matchedCompareList = new ArrayList<>();
        for (MxEnterpriseRisk enterpriseRisk : enterpriseRisks) {
            StringBuilder message = new StringBuilder("");
            if (compareEnterpriseRisks.size() > index) {
                MxEnterpriseRisk compareEnterpriseRisk = compareEnterpriseRisks.get(index);
                // suppose if there is no row in compared group
                if (compareEnterpriseRisk != null) {
                    message = objectDiff(enterpriseRisk, compareEnterpriseRisk, message);
                    if (message != null && !message.toString().isBlank()) {
                        enterpriseRisksList.add(enterpriseRisk);
                        compareEnterpriseRisksList.add(compareEnterpriseRisk);
                    } else {
                        matchedList.add(enterpriseRisk);
                        matchedCompareList.add(compareEnterpriseRisk);
                    }
                    log.info("two ids :{}\t second id:{}", enterpriseRisk.getId(), compareEnterpriseRisk.getId());
                }
            }
            index++;
        }
        document.put("totalPages", (long) Math.ceil((float) enterpriseRisksList.size() / (float) pageSize));
        document.put("records", enterpriseRisksList.size());
        if (type.equalsIgnoreCase("Matched")) {
            document.put("content1", matchedList);
            document.put("content2", matchedCompareList);
        } else {
            document.put("content1", enterpriseRisksList);
            document.put("content2", compareEnterpriseRisksList);
        }
        return document;
    }

    private Document compareFinanceByMatch(List<MxFinaceAcctrlRights> financeRights, List<MxFinaceAcctrlRights> compareFinance, Document document, int pageSize, String type) {

        int index = 0;
        List<MxFinaceAcctrlRights> financList = new ArrayList<>();
        List<MxFinaceAcctrlRights> compareFinancList = new ArrayList<>();
        List<MxFinaceAcctrlRights> matchedList = new ArrayList<>();
        List<MxFinaceAcctrlRights> compareMatchedList = new ArrayList<>();
        for (MxFinaceAcctrlRights financeRight : financeRights) {
            StringBuilder message = new StringBuilder("");
            if (compareFinance.size() > index) {
                MxFinaceAcctrlRights compareConfig = compareFinance.get(index);
                // suppose if there is no row in compared group
                if (compareConfig != null) {
                    message = objectDiff(financeRight, compareConfig, message);
                    if (message != null && !message.toString().isBlank()) {
                        financList.add(financeRight);
                        compareFinancList.add(compareConfig);
                    } else {
                        matchedList.add(financeRight);
                        compareMatchedList.add(compareConfig);
                    }
                }
            }
            index++;
        }
        document.put("totalPages", (long) Math.ceil((float) financList.size() / (float) pageSize));
        document.put("records", financList.size());
        if (type.equalsIgnoreCase("Matched")) {
            document.put("content1", matchedList);
            document.put("content2", compareMatchedList);
        } else {
            document.put("content1", financList);
            document.put("content2", compareFinancList);
        }
        return document;
    }


    public Document compareConfiguration(List<MxCwtConfigMgtRight> configMgtRights, List<MxCwtConfigMgtRight> compareConfigMgtRights, Document document, int pageSize, String type) {
        int index = 0;
        List<MxCwtConfigMgtRight> configMgtRightsList = new ArrayList<>();
        List<MxCwtConfigMgtRight> compareConfigMgtRightsList = new ArrayList<>();
        List<MxCwtConfigMgtRight> matchedList = new ArrayList<>();
        List<MxCwtConfigMgtRight> compareMatchedList = new ArrayList<>();
        for (MxCwtConfigMgtRight configMgtRight : configMgtRights) {
            StringBuilder message = new StringBuilder("");
            if (compareConfigMgtRights.size() > index) {
                MxCwtConfigMgtRight compareConfig = compareConfigMgtRights.get(index);
                // suppose if there is no row in compared group
                if (compareConfig != null) {
                    message = objectDiff(configMgtRight, compareConfig, message);
                    if (message != null && !message.toString().isBlank()) {
                        configMgtRightsList.add(configMgtRight);
                        compareConfigMgtRightsList.add(compareConfig);
                    } else {
                        matchedList.add(configMgtRight);
                        compareMatchedList.add(compareConfig);
                    }
                    log.info("two ids :{}\t second id:{}", configMgtRight.getId(), compareConfig.getId());
                }
            }

            index++;
        }
        document.put("totalPages", (long) Math.ceil((float) configMgtRightsList.size() / (float) pageSize));
        document.put("records", configMgtRightsList.size());
        if (type.equalsIgnoreCase("Matched")) {
            document.put("content1", matchedList);
            document.put("content2", compareMatchedList);
        } else {
            document.put("content1", configMgtRightsList);
            document.put("content2", compareConfigMgtRightsList);
        }
        return document;
    }

    public void excludeProperties(ObjectDifferBuilder objectDifferBuilder) {
        objectDifferBuilder.inclusion().exclude().propertyName("id");
        objectDifferBuilder.inclusion().exclude().propertyName("sysDate");
        objectDifferBuilder.inclusion().exclude().propertyName("jobId");
        objectDifferBuilder.inclusion().exclude().propertyName("reportDate");
        objectDifferBuilder.inclusion().exclude().propertyName("label");
        objectDifferBuilder.inclusion().exclude().propertyName("groupLabel");
        objectDifferBuilder.inclusion().exclude().propertyName("tmplType");
        objectDifferBuilder.inclusion().exclude().propertyName("template");

    }

    //comparing two objects
    public StringBuilder objectDiff(Object baseObject, Object compareObject, StringBuilder message) {
        ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
        excludeProperties(objectDifferBuilder);
        DiffNode diff1 = objectDifferBuilder.build().compare(baseObject, compareObject);
        if (diff1.hasChanges()) {
            diff1.visit((node, visit) -> {
                if (!node.hasChildren()) {
                    final Object oldValue = node.canonicalGet(baseObject);
                    final Object newValue = node.canonicalGet(compareObject);
                    message.append(node.getPropertyName()).append("#").append(oldValue).append(" - ").append(newValue).append("###");
                }
            });
        }
        return message;
    }
}

