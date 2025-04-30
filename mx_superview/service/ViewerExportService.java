package com.finsurge.tmr_portal.mx_superview.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.configs.SearchConfig;
import com.finsurge.tmr_portal.mx_superview.entity.*;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.repository.*;
import com.finsurge.tmr_portal.report_publisher.RPControllerUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ViewerExportService {
    private static final Logger log = LoggerFactory.getLogger(ViewerExportService.class);

    private final DateTimeFormatter dateTimeFormatter;
    private final DownloadJobService downloadJobService;
    private final Environment environment;
    private final MxEnterPriseRiskRepo mxEnterPriseRiskRepo;
    private final MxChineseWallTemplateRepository chineseWallTemplateRepository;
    private final MxConsistencyTemplateRepository consistencyTemplateRepository;
    private final MxDistributionRepo distributionRepo;
    private final MxPortfolioRightsRepository portfolioLabelRepository;
    private final MxOSPRightsTemplateRepository ospRightsTemplateRepository;
    private final MxGroupNavigationRightsRepository groupNavigationRightsRepository;
    private final MxCwtConfigMgtRightRepository cwtConfigMgtRightRepository;
    private final MxOperationRightsRepo operationRightsRepo;
    private final MxFinanceRightsRepo financeRightsRepo;
    private final MxGroupListRepository groupListRepo;
    private final MXCounterpartyRepository mxCounterpartyRepository;
    private final MxCounterPartyDisplayRepository mxCounterPartyDisplayRepository;
    private final MxUserGroupAccessRepository mxUserGroupAccessRepository;
    private final MxSupChgAuditBdyRepository mxAuditBdyRepo;
    private final SearchHistoryRepository searchHistoryRepository;
    private final MxUserListRepository userListRepository;
    private final DataImportJobRepository dataImportJobRepository;
    private final STPSrcModuleRepository stpSrcModuleRepository;
    private final STPRightsTypologyRepository stpRightsTypologyRepository;
    private final MxPreferenceRepository mxPreferenceRepository;
    //  private final DataImportJobRepository dataImportJobRepository;

    private final MxAuditRepo mxAuditRepo;

    private final MxUserPolicyRepo mxUserPolicyRepo;

    private final MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo;
    private final MxPortfolioRightsRepository mxGroupPortfolioRights;
    private final STPRightsRepository stpRightsRepository;

    @Autowired
    private EntityManager entityManager;

    public ViewerExportService(DownloadJobService downloadJobService, Environment environment, MxEnterPriseRiskRepo mxEnterPriseRiskRepo, MxChineseWallTemplateRepository chineseWallTemplateRepository, MxConsistencyTemplateRepository consistencyTemplateRepository1, MxDistributionRepo distributionRepo, MxPortfolioRightsRepository portfolioLabelRepository, MxOSPRightsTemplateRepository ospRightsTemplateRepository, MxGroupNavigationRightsRepository groupNavigationRightsRepository, MxCwtConfigMgtRightRepository cwtConfigMgtRightRepository, MxOperationRightsRepo operationRightsRepo, MxFinanceRightsRepo financeRightsRepo, MxGroupListRepository groupListRepo, MXCounterpartyRepository mxCounterpartyRepository, MxCounterPartyDisplayRepository mxCounterPartyDisplayRepository, MxUserGroupAccessRepository mxUserGroupAccessRepository, MxSupChgAuditBdyRepository mxAuditBdyRepo, SearchHistoryRepository searchHistoryRepository, MxUserListRepository userListRepository, DataImportJobRepository dataImportJobRepository, STPSrcModuleRepository stpSrcModuleRepository, STPRightsTypologyRepository stpRightsTypologyRepository, MxPreferenceRepository mxPreferenceRepository, MxAuditRepo mxAuditRepo, MxUserPolicyRepo mxUserPolicyRepo, MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo, MxPortfolioRightsRepository mxGroupPortfolioRights, STPRightsRepository stpRightsRepository) {
        this.stpSrcModuleRepository = stpSrcModuleRepository;
        this.stpRightsTypologyRepository = stpRightsTypologyRepository;
        this.mxPreferenceRepository = mxPreferenceRepository;
        this.mxGroupPortfolioRights = mxGroupPortfolioRights;
        this.stpRightsRepository = stpRightsRepository;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
        this.downloadJobService = downloadJobService;
        this.environment = environment;
        this.mxEnterPriseRiskRepo = mxEnterPriseRiskRepo;
        this.chineseWallTemplateRepository = chineseWallTemplateRepository;
        this.consistencyTemplateRepository = consistencyTemplateRepository1;
        this.distributionRepo = distributionRepo;
        this.portfolioLabelRepository = portfolioLabelRepository;
        this.ospRightsTemplateRepository = ospRightsTemplateRepository;
        this.groupNavigationRightsRepository = groupNavigationRightsRepository;
        this.cwtConfigMgtRightRepository = cwtConfigMgtRightRepository;
        this.operationRightsRepo = operationRightsRepo;
        this.financeRightsRepo = financeRightsRepo;
        this.groupListRepo = groupListRepo;
        this.mxCounterpartyRepository = mxCounterpartyRepository;
        this.mxCounterPartyDisplayRepository = mxCounterPartyDisplayRepository;
        this.mxUserGroupAccessRepository = mxUserGroupAccessRepository;
        this.mxAuditBdyRepo = mxAuditBdyRepo;
        this.searchHistoryRepository = searchHistoryRepository;
        this.userListRepository = userListRepository;
        this.dataImportJobRepository = dataImportJobRepository;
        this.mxAuditRepo = mxAuditRepo;
        this.mxUserPolicyRepo = mxUserPolicyRepo;
        this.mxGroupCombinedPortfolioRepo = mxGroupCombinedPortfolioRepo;

    }

    @Value("${uam.stp.file.count}")
    private long stpFileCount;

    @Value("${uam.blank.fields}")
    private String blankFields;

    @Async
    @Transactional
    public CompletableFuture<Void> getEnterpriseDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                                 String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxEnterpriseRisk> enterpriseRisks = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        Document enterpriseRisk = new Document();
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            enterpriseRisk = getEnterPriseRisk(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            enterpriseRisks.addAll((List<MxEnterpriseRisk>) enterpriseRisk.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, enterpriseRisks.size(), job.getId());
            totalPage = (int) enterpriseRisk.get("totalPages");
            totalCount = (long) enterpriseRisk.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", enterpriseRisks.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, enterpriseRisks, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document getEnterPriseRisk(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification enterpriseRisk, String userName, boolean countFetched, long totalRecords) {
        if (enterpriseRisk.getSearchWhereClause() == null || enterpriseRisk.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(enterpriseRisk.getDateValue(), dateTimeFormatter);
            Page<MxEnterpriseRisk> enterpriseList = mxEnterPriseRiskRepo.findByGroupLabelAndReportDate(enterpriseRisk.getTemplateValue().toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", enterpriseList.getTotalPages());
            document.put("records", enterpriseList.getTotalElements());
            document.put("content", enterpriseList.getContent());
            return document;
        } else {
            if (enterpriseRisk.isGlobalSearch()) {
                saveWhereSearchHistory(enterpriseRisk.getSearchWhereClause(), userName, enterpriseRisk.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(enterpriseRisk.getDateValue(), dateTimeFormatter);
                Page<MxEnterpriseRisk> enterpriseList = mxEnterPriseRiskRepo.findByEnterpriseRiskGlobalSearch(enterpriseRisk.getTemplateValue(), requestDate, enterpriseRisk.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", enterpriseList.getTotalPages());
                document.put("records", enterpriseList.getTotalElements());
                document.put("content", enterpriseList.getContent());
                return document;
            }
            String template = getPropertyFields(enterpriseRisk.getTemplate(), enterpriseRisk.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxEnterpriseRisk> mxEnterpriseRiskBuilderSpecification = new SearchConfig<>(templateList[2],
                    enterpriseRisk.getTemplateValue(), enterpriseRisk.getSubTemplate(), enterpriseRisk.getSubTemplateValue(), templateList[0], enterpriseRisk.getDateValue(), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            return mxEnterpriseRiskBuilderSpecification.getSearchResultUpdate(enterpriseRisk.getSearchWhereClause(), userName, enterpriseRisk.getTemplate(), countFetched, totalRecords);
        }

    }

    public MxEnterpriseRisk getEnterPriseRiskCount(LocalDate requestDate, String enterpriseRisk) {
        return mxEnterPriseRiskRepo.findTopByLabelAndReportDate(enterpriseRisk.toUpperCase(), requestDate);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getChineseWallDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                                  String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxChineseWallTmpl> chineseWallTmpls = new ArrayList<>();
        //Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document mxChineseWallTmpls = getChineseWall(page, pageSize, sortBy, sortingOrder, sort, filters, userName, false, totalCount);

            log.debug("mxChineseWallTmpls:{}", mxChineseWallTmpls);

            chineseWallTmpls.addAll((List<MxChineseWallTmpl>) mxChineseWallTmpls.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallTmpls.size(), job.getId());
            totalPage = (int) mxChineseWallTmpls.get("totalPages");
            totalCount = (long) mxChineseWallTmpls.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", chineseWallTmpls.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, chineseWallTmpls, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document getChineseWall(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalRecords) throws ClassNotFoundException {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
            Page<MxChineseWallTmpl> chineseWallTmpls = chineseWallTemplateRepository.findByTemplateAndReportDate(filters.getTemplateValue().toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", chineseWallTmpls.getTotalPages());
            document.put("records", chineseWallTmpls.getTotalElements());
            document.put("content", chineseWallTmpls.getContent());
            return document;
        } else {
            if (filters.isGlobalSearch()) {
//                String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), true);
//                String[] templateList = template.split("\\|");
//                GlobalSearchService globalSearch = new GlobalSearchService(templateList[2],
//                        Collections.singletonList(filters.getTemplateValue()), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], filters.getDateValue(), null, templateList[1],
//                        null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
//                return globalSearch.getGlobalSearchResults(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalRecords, MxChineseWallTmpl.class,false);

                saveWhereSearchHistory(filters.getSearchWhereClause(), userName, filters.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
                Page<MxChineseWallTmpl> chinesewallList = chineseWallTemplateRepository.findByTemplateAndReportDateWithGlobalSearch(filters.getTemplateValue(), requestDate, filters.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", chinesewallList.getTotalPages());
                document.put("records", chinesewallList.getTotalElements());
                document.put("content", chinesewallList.getContent());
                return document;

            } else {
                String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
                String[] templateList = template.split("\\|");

                SearchConfig<MxChineseWallTmpl> mxChineseWallTmplSpecificationBuilder = new SearchConfig<>(templateList[2],
                        filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], filters.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
                return mxChineseWallTmplSpecificationBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalRecords);
            }
        }
    }


    @Async
    @Transactional
    public CompletableFuture<Void> getPortfolioDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxGroupPortfolioRights> portfolioLabels = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document portfolioLabel = getPortfolioRights(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);
            log.debug("portfolioLabel:{}", portfolioLabel);
            portfolioLabels.addAll((List<MxGroupPortfolioRights>) portfolioLabel.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
            totalPage = (int) portfolioLabel.get("totalPages");
            totalCount = (long) portfolioLabel.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", portfolioLabels.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, portfolioLabels, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getConsistencyExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                        String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, List<Sort.Order> sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxConsistencyTmpl> consistencyTmplList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {

            Document consistencyTmplsRights = getConsitency(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);
            log.debug("consistencyTmplsRights:{}", consistencyTmplsRights);

            consistencyTmplList.addAll((List<MxConsistencyTmpl>) consistencyTmplsRights.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, consistencyTmplList.size(), job.getId());
            totalPage = (int) consistencyTmplsRights.get("totalPages");
            totalCount = (long) consistencyTmplsRights.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", consistencyTmplList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, consistencyTmplList, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getCwtConfigMgtRightsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                                  String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxCwtConfigMgtRight> cwtConfigMgtRights = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document cwtConfigMgtRight = getConfiguration(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            log.debug("cwtConfigMgtRight:{}", cwtConfigMgtRight);

            cwtConfigMgtRights.addAll((List<MxCwtConfigMgtRight>) cwtConfigMgtRight.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, cwtConfigMgtRights.size(), job.getId());
            totalPage = (int) cwtConfigMgtRight.get("totalPages");
            totalCount = (long) cwtConfigMgtRight.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", cwtConfigMgtRights.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, cwtConfigMgtRights, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getOspRightsMatrixAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                               String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxOspRightsMatrix> ospRightsMatrices = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document ospRightsMatrix = getOspMatrix(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            log.debug("ospRightsMatrix:{}", ospRightsMatrix);

            ospRightsMatrices.addAll((List<MxOspRightsMatrix>) ospRightsMatrix.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, ospRightsMatrices.size(), job.getId());
            totalPage = (int) ospRightsMatrix.get("totalPages");
            totalCount = (long) ospRightsMatrix.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", ospRightsMatrices.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, ospRightsMatrices, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document getConsitency(int page, int pageSize, String sortBy, String sortingOrder, List<Sort.Order> sort, GeneralSpecification consistencyExportXlsxRequest, String userName, boolean countFetched, long totalRecords) {
        if (consistencyExportXlsxRequest.getSearchWhereClause() == null || consistencyExportXlsxRequest.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(consistencyExportXlsxRequest.getDateValue(), dateTimeFormatter);
            Page<MxConsistencyTmpl> consistencyList = consistencyTemplateRepository.findByTemplateAndReportDateList(consistencyExportXlsxRequest.getTemplateValue().toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", consistencyList.getTotalPages());
            document.put("records", consistencyList.getTotalElements());
            document.put("content", consistencyList.getContent());
            return document;
        } else {
            if (consistencyExportXlsxRequest.isGlobalSearch()) {
                saveWhereSearchHistory(consistencyExportXlsxRequest.getSearchWhereClause(), userName, consistencyExportXlsxRequest.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(consistencyExportXlsxRequest.getDateValue(), dateTimeFormatter);
                Page<MxConsistencyTmpl> consistencyList = consistencyTemplateRepository.findByGroupLabelAndReportDateListWithSearch(consistencyExportXlsxRequest.getTemplateValue(), requestDate, consistencyExportXlsxRequest.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", consistencyList.getTotalPages());
                document.put("records", consistencyList.getTotalElements());
                document.put("content", consistencyList.getContent());
                return document;
            }
            String template = getPropertyFields(consistencyExportXlsxRequest.getTemplate(), consistencyExportXlsxRequest.getSubTemplate(), false);
            String[] templateList = template.split("\\|");

            SearchConfig<MxConsistencyTmpl> mxConsistencyBuilderSpecification = new SearchConfig<>(templateList[2],
                    consistencyExportXlsxRequest.getTemplateValue(), consistencyExportXlsxRequest.getSubTemplate(), consistencyExportXlsxRequest.getSubTemplateValue(), templateList[0], consistencyExportXlsxRequest.getDateValue(), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            return mxConsistencyBuilderSpecification.getSearchResultUpdate(consistencyExportXlsxRequest.getSearchWhereClause(), userName, consistencyExportXlsxRequest.getTemplate(), countFetched, totalRecords);
        }
    }

    @Transactional
    public Document getConfiguration(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification cwtConfigMgtRight, String userName, boolean countFetched, long totalRecords) {
        if (cwtConfigMgtRight.getSearchWhereClause() == null || cwtConfigMgtRight.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(cwtConfigMgtRight.getDateValue(), dateTimeFormatter);
            Page<MxCwtConfigMgtRight> cwtManagementList = cwtConfigMgtRightRepository.findByGroupLabelAndReportDate(cwtConfigMgtRight.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", cwtManagementList.getTotalPages());
            document.put("records", cwtManagementList.getTotalElements());
            document.put("content", cwtManagementList.getContent());
            return document;
        } else {
            if (cwtConfigMgtRight.isGlobalSearch()) {
                saveWhereSearchHistory(cwtConfigMgtRight.getSearchWhereClause(), userName, cwtConfigMgtRight.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(cwtConfigMgtRight.getDateValue(), dateTimeFormatter);
                Page<MxCwtConfigMgtRight> configurationList = cwtConfigMgtRightRepository.findByGroupLabelAndReportDateWithGlobalSearch(cwtConfigMgtRight.getTemplateValue(), requestDate, cwtConfigMgtRight.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", configurationList.getTotalPages());
                document.put("records", configurationList.getTotalElements());
                document.put("content", configurationList.getContent());
                return document;
            }
            String template = getPropertyFields(cwtConfigMgtRight.getTemplate(), cwtConfigMgtRight.getSubTemplate(), false);
            String[] templateList = template.split("\\|");

            SearchConfig<MxCwtConfigMgtRight> mxConfigurationBuilderSpecification = new SearchConfig<>(templateList[2],
                    cwtConfigMgtRight.getTemplateValue(), cwtConfigMgtRight.getSubTemplate(), cwtConfigMgtRight.getSubTemplateValue(), templateList[0], cwtConfigMgtRight.getDateValue(), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            Document document = mxConfigurationBuilderSpecification.getSearchResultUpdate(cwtConfigMgtRight.getSearchWhereClause(), userName, cwtConfigMgtRight.getTemplate(), countFetched, totalRecords);
            return document;
        }
    }

    @Transactional
    public Document getOspMatrix(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification ospRightsMatrix, String userName, boolean countFetched, long totalRecords) {
        if (ospRightsMatrix.getSearchWhereClause() == null || ospRightsMatrix.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(ospRightsMatrix.getDateValue(), dateTimeFormatter);
            Page<MxOspRightsMatrix> ospRightsList = ospRightsTemplateRepository.findByTemplateAndReportDateList(ospRightsMatrix.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", ospRightsList.getTotalPages());
            document.put("records", ospRightsList.getTotalElements());
            document.put("content", ospRightsList.getContent());
            return document;
        } else {
            if (ospRightsMatrix.isGlobalSearch()) {
                saveWhereSearchHistory(ospRightsMatrix.getSearchWhereClause(), userName, ospRightsMatrix.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(ospRightsMatrix.getDateValue(), dateTimeFormatter);
                Page<MxOspRightsMatrix> configurationList = ospRightsTemplateRepository.findByTemplateAndReportDateListWithGlobalSearch(ospRightsMatrix.getTemplateValue(), requestDate, ospRightsMatrix.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", configurationList.getTotalPages());
                document.put("records", configurationList.getTotalElements());
                document.put("content", configurationList.getContent());
                return document;
            }
            String template = getPropertyFields(ospRightsMatrix.getTemplate(), ospRightsMatrix.getSubTemplate(), false);
            String[] templateList = template.split("\\|");

            SearchConfig<MxOspRightsMatrix> mxOspRightsBuilderSpecification = new SearchConfig<>(templateList[2],
                    ospRightsMatrix.getTemplateValue(), ospRightsMatrix.getSubTemplate(), ospRightsMatrix.getSubTemplateValue(), templateList[0], ospRightsMatrix.getDateValue(), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            Document document = mxOspRightsBuilderSpecification.getSearchResultUpdate(ospRightsMatrix.getSearchWhereClause(), userName, ospRightsMatrix.getTemplate(), countFetched, totalRecords);
            return document;
        }
    }


    @Async
    @Transactional
    public CompletableFuture<Void> getNavigationRightsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String groupLabel, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxGroupNavigationRight> groupNavigationRights = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }

        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document navigationTmpls = getNavigationRights(page, pageSize, sortBy, sortingOrder, sort, filters, groupLabel, userName, countFetched, totalCount);

            log.debug("navigationTmpls:{}", navigationTmpls);

            groupNavigationRights.addAll((List<MxGroupNavigationRight>) navigationTmpls.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupNavigationRights.size(), job.getId());
            totalPage = (int) navigationTmpls.get("totalPages");
            totalCount = (long) navigationTmpls.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", groupNavigationRights.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, groupNavigationRights, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getOperationalExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                        String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxOperationRights> operationArrayList = new ArrayList<>();
        //Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;

        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document operationRight = getOperationalRights(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            log.debug("operationRight:{}", operationRight);

            operationArrayList.addAll((List<MxOperationRights>) operationRight.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, operationArrayList.size(), job.getId());
            totalPage = (int) operationRight.get("totalPages");
            totalCount = (long) operationRight.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", (int) totalCount, operationArrayList.size(), job.getId());
        return exportFilefromDetails(outputFormat, operationArrayList, fileName, fieldMaps, color, job, fieldColumns);
    }

    @Transactional
    public Document getOperationalRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalRecords) {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
            Page<MxOperationRights> operationList = operationRightsRepo.findByTemplateAndReportDateList(filters.getTemplateValue().toUpperCase(), filters.getSubTemplateValue().toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", operationList.getTotalPages());
            document.put("records", operationList.getTotalElements());
            document.put("content", operationList.getContent());
            return document;
        } else {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), userName, filters.getTemplate() + filters.getSubTemplateValue().toUpperCase());
                Document document = new Document();
                Page<MxOperationRights> operationList = null;
                LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
                if (filters.getSubTemplateValue().equalsIgnoreCase("NKEY")) {
                    operationList = operationRightsRepo.findByTemplateAndReportDateAndNkeyListWithGlobalSearch(filters.getTemplateValue(), filters.getSubTemplateValue(), requestDate, filters.getSearchWhereClause()
                            , PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    operationList = operationRightsRepo.findByTemplateAndReportDateAndLposListWithGlobalSearch(filters.getTemplateValue(), filters.getSubTemplateValue(), requestDate, filters.getSearchWhereClause()
                            , PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", operationList.getTotalPages());
                document.put("records", operationList.getTotalElements());
                document.put("content", operationList.getContent());
                return document;
            }
            String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxOperationRights> mxOperationRightsSpec = new SearchConfig<>(templateList[2],
                    filters.getTemplateValue(), templateList[5], filters.getSubTemplateValue(), templateList[0], filters.getDateValue(), null, templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            Document document = mxOperationRightsSpec.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalRecords);
            return document;
        }
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getFinanceDetailsAndExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                              String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        log.info(filters.toString());

        List<MxFinaceAcctrlRights> financeLables = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {

            Document financeRights = getFinanceRights(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);
            log.debug("financeRights:{}", financeRights);
            financeLables.addAll((List<MxFinaceAcctrlRights>) financeRights.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, financeLables.size(), job.getId());
            totalPage = (int) financeRights.get("totalPages");
            totalCount = (long) financeRights.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", financeLables.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, financeLables, fileName, fieldMaps, color, job, fieldColumns);
    }

    private String generateExportPathUsingFileName(String fileName) {
        String importProperty = environment.getProperty("uam.paths.export") + File.separator + fileName;
        importProperty = RPControllerUtils.fixSeparatorsForUnix(importProperty);
        return importProperty;
    }


    public CompletableFuture<Void> exportFilefromDetails(String outputFormat, List<?> exportData, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns) throws Exception {

        if (outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb")) {
            File file = new File(generateExportPathUsingFileName(fileName + outputFormat));
            file.getParentFile().mkdirs();
            file = exportAsXlsxFromList(prepareMapListToExport(exportData), file, fieldMaps, color, job, fieldColumns);
            //update download job
            downloadJobService.updateJobProgress("COMPLETE", exportData.size(), exportData.size(), job.getId());
            downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
            return CompletableFuture.completedFuture(null);
        }

        File file = new File(generateExportPathUsingFileName(fileName + ".csv"));
        file.getParentFile().mkdirs();
        log.info("CSV Export: Starting write.");
        file = exportAsCsvFromList(prepareMapListToExport(exportData), file, fieldMaps, job, fieldColumns);
        //update download job
        log.info("CSV Export: Write complete.");
        downloadJobService.updateJobProgress("COMPLETE", exportData.size(), exportData.size(), job.getId());
        downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
        return CompletableFuture.completedFuture(null);
    }

    public List<Map<String, Object>> prepareMapListToExport(List<?> list) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyyMMdd HHmmss"));
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        String jsonString = mapper.writeValueAsString(list);
        return mapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    public File exportAsCsvFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, DownloadJob job, List<String> fieldColumns) {
        fieldColumns.add("id");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
        String dateFormatDynamic =null;
        MxPreference mxPreference = mxPreferenceRepository.findByPropertyName("DATE_FORMAT");
        dateFormatDynamic=  (mxPreference==null||mxPreference.getPropertyValue()==null)?"yyyyMMdd":mxPreference.getPropertyValue()  ;
        String dateTimeFormatDynamic=dateFormatDynamic.concat(" ").concat("HH:mm:ss");
        //try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("Cp1252")))) {
            //try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.defaultCharset()))) {
            //try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("windows-1252")))) {
            // write header in csv
            int i = 1;
            for (FieldMap fieldMap : fieldMaps) {
                if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                    bw.write(fieldMap.getDisplayName());
                    if (fieldMaps.size() != i) {
                        bw.append(",");
                    }
                    i++;
                }
            }
            bw.newLine();
            // write values in csv
            int lineCount = 0;
            for (Map<String, Object> trade : list) {
                int j = 1;
                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                        String fieldName = fieldMap.getEntityName();
                        String fieldFormat = fieldMap.getFormat();
                        String fieldDisplayName = fieldMap.getDisplayName();
                        String fieldValue = "";
                        if (fieldName.equalsIgnoreCase("id")) {
                            fieldValue = String.valueOf(lineCount + 1);
                        } else if (fieldFormat != null) {
                            if (trade.get(fieldName) != null && !trade.get(fieldName).toString().isBlank()) {
                                if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy") && (!trade.get(fieldName).toString().contains("-"))) {
                                    fieldValue = LocalDate.parse(trade.get(fieldName).toString(), dateFormatter).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy HH:mm:ss")) {
                                    fieldValue = LocalDateTime.parse(trade.get(fieldName).toString(), dateTimeformatter).format(DateTimeFormatter.ofPattern(dateTimeFormatDynamic));
                                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy")) {

                                    fieldValue = LocalDate.parse(trade.get(fieldName).toString()).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                                } else {
                                    fieldValue = LocalTime.parse(trade.get(fieldName).toString()).format(DateTimeFormatter.ofPattern(fieldFormat));
                                }

                            } else {
                                fieldValue = blankFields;
                            }
                        } else {
                            fieldValue = trade.get(fieldName) == null ? null : trade.get(fieldName).toString();
                        }
                        bw.write((fieldValue != null ? fieldValue : blankFields));
                        if (fieldMaps.size() != j) {
                            bw.append(",");
                        }
                        j++;
                    }
                }
                bw.newLine();
                lineCount++;
            }
            log.info("CSV Export: Updating final status.");
            if (job != null) {
                downloadJobService.updateJobProgress("FILE_WRITE", list.size(), lineCount, job.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public File exportAsXlsxFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns) throws Exception {
        fieldColumns.add("id");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
        //to do....
        // log.info("test", mxPreferenceRepository.findByPropertyName("DATE_FORMAT").getPropertyValue());
        String dateFormatDynamic =null;
        MxPreference mxPreference = mxPreferenceRepository.findByPropertyName("DATE_FORMAT");
        dateFormatDynamic=  (mxPreference==null||mxPreference.getPropertyValue()==null)?"yyyyMMdd":mxPreference.getPropertyValue()  ;
        String dateTimeFormatDynamic=dateFormatDynamic.concat(" ").concat("HH:mm:ss");
        SXSSFWorkbook workbook = new SXSSFWorkbook(Integer.parseInt(Objects.requireNonNull(environment.getProperty("reports.print-rows.threads.count"))));
        SXSSFSheet sheet = workbook.createSheet();
        int rowCount = 0;
        int columnCount = 0;
        SXSSFRow row = sheet.createRow(rowCount++);
//        Font headerFont = workbook.createFont();
//        headerFont.setBold(true);
//        headerFont.setColor(HSSFColor.HSSFColorPredefined.valueOf(color).getIndex());
//        headerFont.setFontHeightInPoints((short) 12);
//        CellStyle headerCellStyle = sheet.getWorkbook().createCellStyle();
//        headerCellStyle.setFont(headerFont);
        CellStyle defaultCellStyle = sheet.getWorkbook().createCellStyle();
        int headerCount = 0;
        for (FieldMap fieldMap : fieldMaps) {
            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                SXSSFCell cell = row.createCell(columnCount++);
                // cell.setCellStyle(headerCellStyle);
                cell.setCellValue(fieldMap.getDisplayName());
                headerCount++;
            }
        }
        // write values in xlsx
        int itemCount = 0;
        for (Map<String, Object> trade : list) {
            if (trade.get("id") != null) {
                columnCount = 0;
                row = sheet.createRow(rowCount++);
                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                        SXSSFCell cell = row.createCell(columnCount++);
                        cell.setCellStyle(defaultCellStyle);

                        String fieldName = fieldMap.getEntityName();
                        String fieldFormat = fieldMap.getFormat();
                        String fieldValue = "";
                        if (fieldName.equalsIgnoreCase("id")) {
                            fieldValue = String.valueOf(rowCount - 1);
                            cell.setCellValue(fieldValue);
                        } else if (fieldFormat != null) {
                            if (trade.get(fieldName) != null && !trade.get(fieldName).toString().isBlank()) {
                                if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy") && (!trade.get(fieldName).toString().contains("-"))) {
                                    fieldValue = LocalDate.parse(trade.get(fieldName).toString(), dateFormatter).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy HH:mm:ss")) {
                                    fieldValue = LocalDateTime.parse(trade.get(fieldName).toString(), dateTimeformatter).format(DateTimeFormatter.ofPattern(dateTimeFormatDynamic));
                                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy")) {

                                    fieldValue = LocalDate.parse(trade.get(fieldName).toString()).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                                } else {
                                    fieldValue = LocalTime.parse(trade.get(fieldName).toString()).format(DateTimeFormatter.ofPattern(fieldFormat));
                                }
                                cell.setCellValue(fieldValue);
                            } else {
                                cell.setCellValue(blankFields);
                            }
                        } else {
                            fieldValue = trade.get(fieldName) == null ? null : trade.get(fieldName).toString();
                            cell.setCellValue((fieldValue != null ? fieldValue : blankFields));
                        }
                    }
                }
            }
            itemCount++;

        }
        log.info("XLSX Export: Flushing to file.");
        downloadJobService.updateJobProgress("FILE_SAVING", list.size(), itemCount, job.getId());
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (IOException ex) {
            log.error("Excel Export: Failed to flush data to file.", ex);
        }
        // dispose of temporary files backing this workbook on disk
        workbook.dispose();
        return file;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupCombinedPortfolio(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {

        List<MxGroupCombinedPortfolio> groupCombinedPortfolioList = new ArrayList<>();
        //Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document groupCombinedPortfolios = getGrpCompPortfolio(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            log.debug("groupCombinedPortfolios:{}", groupCombinedPortfolios);

            groupCombinedPortfolioList.addAll((List<MxGroupCombinedPortfolio>) groupCombinedPortfolios.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupCombinedPortfolioList.size(), job.getId());
            totalPage = (int) groupCombinedPortfolios.get("totalPages");
            totalCount = (long) groupCombinedPortfolios.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", groupCombinedPortfolioList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, groupCombinedPortfolioList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getAuditForExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat,
                                                     List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        List<MxSupChgAuditHdr> auditList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;

        int totalPage = 1;
        long totalCount = 0;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {

            Document auditFields = getAuditList(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            auditList.addAll((List<MxSupChgAuditHdr>) auditFields.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, auditList.size(), job.getId());
            totalPage = (int) auditFields.get("totalPages");
            totalCount = (long) auditFields.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", auditList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, auditList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document getAuditList(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification auditHdrList, String userName, boolean countFetched, long totalRecords) {
        if (auditHdrList.getSearchWhereClause() == null || auditHdrList.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(auditHdrList.getDateValue(), dateTimeFormatter);
            LocalDate fromDate = LocalDate.parse(auditHdrList.getFromDateValue(), dateTimeFormatter);
            Page<MxSupChgAuditHdr> auditHeaderList = mxAuditRepo.findByReportDateList(requestDate, fromDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", auditHeaderList.getTotalPages());
            document.put("records", auditHeaderList.getTotalElements());
            document.put("content", auditHeaderList.getContent());
            return document;
        } else {
            if (auditHdrList.isGlobalSearch()) {
                saveWhereSearchHistory(auditHdrList.getSearchWhereClause(), userName, auditHdrList.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(auditHdrList.getDateValue(), dateTimeFormatter);
                LocalDate fromDate = LocalDate.parse(auditHdrList.getFromDateValue(), dateTimeFormatter);
                Page<MxSupChgAuditHdr> auditHeaderList = mxAuditRepo.findByReportDateListWithGlobalSearch(requestDate, fromDate, auditHdrList.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", auditHeaderList.getTotalPages());
                document.put("records", auditHeaderList.getTotalElements());
                document.put("content", auditHeaderList.getContent());
                return document;
            }
            String template = getPropertyFields(auditHdrList.getTemplate(), auditHdrList.getSubTemplate(), false);
            String[] templateList = template.split("\\|");

            SearchConfig<MxSupChgAuditHdr> mxEnterpriseRiskBuilderSpecification = new SearchConfig<>(auditHdrList.getTemplate(),
                    auditHdrList.getTemplateValue(), auditHdrList.getSubTemplate(), auditHdrList.getSubTemplateValue(), templateList[0], auditHdrList.getDateValue(), auditHdrList.getFromDateValue(), templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            return mxEnterpriseRiskBuilderSpecification.getSearchResultUpdate(auditHdrList.getSearchWhereClause(), userName, auditHdrList.getTemplate(), countFetched, totalRecords);

        }
    }

    @Async
    public CompletableFuture<Void> getGroupDetailsAndExport(String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, LocalDate requestDate, List<String> fieldColumns, String groupLabel, boolean isAllGroups, List<String> groupList) throws Exception {

        List<MxGroupsListItem> groupsListItems = new ArrayList<>();
        List<Long> groupIdAsLong = new ArrayList<>();
        Page<MxGroupsListItem> mxGroupDetails = null;
        Sort.Order sort = Sort.Order.asc("groupLabel");
        int pageSize = 500;
        int page = 0;

        int totalCount = 0;
        if (!isAllGroups)
            if (groupList != null && groupList.size() != 0) {
                groupList.removeAll(Collections.singletonList(null));
                groupIdAsLong = groupList.stream().map(Long::parseLong).collect(Collectors.toList());
            }
        int totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        for (int i = 0; i < totalPage; i++) {

            if (isAllGroups) {
                mxGroupDetails = groupListRepo.findByReportDate(requestDate, groupLabel, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                mxGroupDetails = groupListRepo.findByReportDateGroupList(requestDate, groupIdAsLong, PageRequest.of(page, pageSize, Sort.by(sort)));
            }

            groupsListItems.addAll(mxGroupDetails.getContent());
            downloadJobService.updateJobProgress("FETCH_ITEMS", totalCount, groupsListItems.size(), job.getId());
            totalCount = (int) mxGroupDetails.getTotalElements();
            totalPage = mxGroupDetails.getTotalPages();
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", groupsListItems.size(), totalCount, job.getId());
        return exportFilefromDetails(outputFormat, groupsListItems, fileName, fieldMaps, color, job, fieldColumns);
    }

//    @Async
//    @Transactional
//    public CompletableFuture<Void> getUserPolicy(String policyName, String fileName, String color, List<FieldMap> fieldMaps, DownloadJob job, String outputFormat, List<String> fieldColumns, LocalDate requestDate) throws Exception {
//
//        List<MxUserPolicy> mxUserPolicies = new ArrayList<>();
//        Sort.Order sort = Sort.Order.desc("id");
//        int pageSize = 500;
//        int page = 0;
//
//        int totalCount = 0;
//        int totalPage = 1;
//        log.info(" Export: Initializing job status.");
//        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
//        log.info(" Export: Starting fetch.");
//        for (int i = 0; i < totalPage; i++) {
//            Page<MxUserPolicy> mxUserPolicy = mxUserPolicyRepo.findAllUserPolicy(policyName, requestDate, PageRequest.of(page, pageSize));
//
//            log.debug(" Export: Found {} items to export in page {} of {} pages and total {} items.",
//                    mxUserPolicy.getContent().size(),
//                    (i + 1),
//                    mxUserPolicy.getTotalPages(),
//                    mxUserPolicy.getTotalElements());
//
//            mxUserPolicies.addAll(mxUserPolicy.getContent());
//            downloadJobService.updateJobProgress("FETCH_ITEMS", totalCount, mxUserPolicies.size(), job.getId());
//            totalPage = mxUserPolicy.getTotalPages();
//            totalCount = (int) mxUserPolicy.getTotalElements();
//            page++;
//        }
//        log.info(" Export: Fetch complete.");
//        downloadJobService.updateJobProgress("FILE_WRITE", totalCount, mxUserPolicies.size(), job.getId());
//        return exportFilefromDetails(outputFormat, mxUserPolicies, fileName, fieldMaps, color, job, fieldColumns);
//    }


    public MxChineseWallTmpl getChineseWallList(LocalDate requestDate, String template) {
        return chineseWallTemplateRepository.findTopByReportDateAndTemplateLabel(requestDate, template.toUpperCase());
    }


    public Long getAuditHeaderList(LocalDate requestDate) {
        return mxAuditRepo.findAllByRepDate(requestDate);
    }

    public MxGroupPortfolioRights getPortfolioRightsList(LocalDate requestDate, String portfolioRights) {
        return mxGroupPortfolioRights.findTopByGroupLabelAndReportDate(portfolioRights, requestDate);
    }


    public MxGroupNavigationRight getNavigationList(LocalDate requestDate, String navigationRights) {
        return groupNavigationRightsRepository.findTopByTemplateAndReportDate(navigationRights, requestDate);
    }

    public MxConsistencyTmpl getConsistencyCount(LocalDate requestDate, String consistencyExportXlsxRequest) {
        return consistencyTemplateRepository.findTopByConsistencyTmplAndReportDate(consistencyExportXlsxRequest, requestDate);
    }

    public MxCwtConfigMgtRight getCwtConfigRightsCount(LocalDate requestDate, String cwtConfigMgtRight) {
        return cwtConfigMgtRightRepository.findTopByGroupLabelAndReportDate(cwtConfigMgtRight, requestDate);
    }

    public MxOspRightsMatrix getOspRightsMatrixCount(LocalDate requestDate, String ospRightsMatrix) {
        return ospRightsTemplateRepository.findTopByOspRightTemplateAndReportDate(ospRightsMatrix, requestDate);
    }

    public MxOperationRights getOperationRightsList(LocalDate requestDate, String operationalRights) {
        return operationRightsRepo.findTopByTemplateAndReportDate(operationalRights, requestDate);
    }

    public MxFinaceAcctrlRights getFinanceRightsList(LocalDate requestDate, String template) {
        return financeRightsRepo.findTopByTemplateAndReportDate(template, requestDate);
    }

//    public STPRightsMatrix getStpRightsList(LocalDate requestDate, String globalTem) {
//        return stpRightsMatrixRepo.findTopByGlobalTemAndReportDate(globalTem.toUpperCase(), requestDate);
//    }

    public MxGroupCombinedPortfolio getGroupCombinedPortfolioList(LocalDate requestDate, String groupCompPortfolio) {
        return mxGroupCombinedPortfolioRepo.findTopByUsrGroupAndReportDate(groupCompPortfolio.toUpperCase(), requestDate);
    }

    public List<Object[]> getGroupList(String requestDate) {
        //to get the users count based on the group to identify dormant users
        return groupListRepo.findAllUniqueGroupList(requestDate);
    }

    public MxCounterPartyDisplay getCounterpartyCount(LocalDate requestDate) {
        return mxCounterPartyDisplayRepository.findTopByReportDate(requestDate);
    }

    @Transactional
    public Document getCounterPartyDisplay(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification counterPtyDpl, String userName, boolean countFetched, long totalRecords) {
        if (counterPtyDpl.getSearchWhereClause() == null || counterPtyDpl.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(counterPtyDpl.getDateValue(), dateTimeFormatter);
            Page<MxCounterPartyDisplay> counterpartyDisplay = mxCounterPartyDisplayRepository.findByTemplateAndReportDateList(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", counterpartyDisplay.getTotalPages());
            document.put("records", counterpartyDisplay.getTotalElements());
            document.put("content", counterpartyDisplay.getContent());
            return document;
        } else {
            if (counterPtyDpl.isGlobalSearch()) {
                saveWhereSearchHistory(counterPtyDpl.getSearchWhereClause(), userName, counterPtyDpl.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(counterPtyDpl.getDateValue(), dateTimeFormatter);
                Page<MxCounterPartyDisplay> auditBodyList = mxCounterPartyDisplayRepository.findByTemplateAndReportDateListWithGlobalSearch(requestDate, counterPtyDpl.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", auditBodyList.getTotalPages());
                document.put("records", auditBodyList.getTotalElements());
                document.put("content", auditBodyList.getContent());
                return document;
            }
            String template = getPropertyFields(counterPtyDpl.getTemplate(), counterPtyDpl.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxCounterPartyDisplay> displaySpecBuilder = new SearchConfig<>(counterPtyDpl.getTemplate(),
                    counterPtyDpl.getTemplateValue(), counterPtyDpl.getSubTemplate(), counterPtyDpl.getSubTemplateValue(), templateList[0], counterPtyDpl.getDateValue(), null, templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

            return displaySpecBuilder.getSearchResultUpdate(counterPtyDpl.getSearchWhereClause(), userName, counterPtyDpl.getTemplate(), countFetched, totalRecords);
        }
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getCounterpartyExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                         String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        List<MxCounterParty> counterPartyList = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;

        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document counterParty = getCounterPartySplit(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);

            log.debug("counterParty:{}", counterParty);

            counterPartyList.addAll((List<MxCounterParty>) counterParty.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, counterPartyList.size(), job.getId());
            totalPage = (int) counterParty.get("totalPages");
            totalCount = (long) counterParty.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", counterPartyList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, counterPartyList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document getCounterPartySplit(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification counterPartyTemplate, String userName, boolean countFetched, long totalRecords) {
        if (counterPartyTemplate.getSearchWhereClause() == null || counterPartyTemplate.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(counterPartyTemplate.getDateValue(), dateTimeFormatter);
            Page<MxCounterParty> counterpartyDisplay = mxCounterpartyRepository.findByTemplateAndReportDateList(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", counterpartyDisplay.getTotalPages());
            document.put("records", counterpartyDisplay.getTotalElements());
            document.put("content", counterpartyDisplay.getContent());
            return document;
        }
        if (counterPartyTemplate.isGlobalSearch()) {
            String template = getPropertyFields(counterPartyTemplate.getTemplate(), counterPartyTemplate.getSubTemplate(), true);
            String[] templateList = template.split("\\|");
            GlobalSearchService search = new GlobalSearchService(counterPartyTemplate.getTemplate(),
                    new ArrayList<>(Collections.singleton(counterPartyTemplate.getTemplateValue())), counterPartyTemplate.getSubTemplate(), counterPartyTemplate.getSubTemplateValue(), templateList[0], counterPartyTemplate.getDateValue(), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            return search.getGlobalSearchResults(counterPartyTemplate.getSearchWhereClause(), userName, counterPartyTemplate.getTemplate(), countFetched, totalRecords, MxCounterParty.class, false);
        }
        String template = getPropertyFields(counterPartyTemplate.getTemplate(), counterPartyTemplate.getSubTemplate(), false);
        String[] templateList = template.split("\\|");
        SearchConfig<MxCounterParty> counterPartySpecBuilder = new SearchConfig<>(counterPartyTemplate.getTemplate(),
                counterPartyTemplate.getTemplateValue(), counterPartyTemplate.getSubTemplate(), counterPartyTemplate.getSubTemplateValue(), templateList[0], counterPartyTemplate.getDateValue(), null, templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

        Document document = counterPartySpecBuilder.getSearchResultUpdate(counterPartyTemplate.getSearchWhereClause(), userName, counterPartyTemplate.getTemplate(), countFetched, totalRecords);
        return document;
    }


    public MxCounterParty getCounterpartyCountSplit(LocalDate requestDate) {
        return mxCounterpartyRepository.findTopByReportDate(requestDate);
    }

    @Transactional
    @Async
    public CompletableFuture<Void> getCounterpartyDisplayExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                                String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {

        List<MxCounterPartyDisplay> counterPartyList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;

        long totalCount = 0;

        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        for (int i = 0; i < totalPage; i++) {
            Document counterParty = getCounterPartyDisplay(page, pageSize, sortBy, sortingOrder, sort, filters, userName, false, 0);

            log.debug("counterParty:{}", counterParty);

            counterPartyList.addAll((List<MxCounterPartyDisplay>) counterParty.get("content"));

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, counterPartyList.size(), job.getId());
            totalPage = (int) counterParty.get("totalPages");
            totalCount = (long) counterParty.get("records");
            page++;
        }
        log.info(" Export: Fetch complete.");


        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", counterPartyList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, counterPartyList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    public List<String> getUserName(String searchGroupName, LocalDate requestDate) {
        return mxUserGroupAccessRepository.findUsername(searchGroupName, requestDate);
    }

    public MxSupChgAuditBdy getAuditBodyList(LocalDate reportDate) {
        return mxAuditBdyRepo.findTopByReportDate(reportDate);
    }

    @Transactional
    public Document getPortfolioRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification portfolioRights, String userName, boolean countFetched, long totalRecords) throws ClassNotFoundException {
        if (portfolioRights.getSearchWhereClause() == null || portfolioRights.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(portfolioRights.getDateValue(), dateTimeFormatter);
            Page<MxGroupPortfolioRights> portfolioList = mxGroupPortfolioRights.findByGroupLabelAndReportDateList(portfolioRights.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", portfolioList.getTotalPages());
            document.put("records", portfolioList.getTotalElements());
            document.put("content", portfolioList.getContent());
            return document;
        } else {
            if (portfolioRights.isGlobalSearch()) {
//                String template = getPropertyFields(portfolioRights.getTemplate(), portfolioRights.getSubTemplate(), true);
//                String[] templateList = template.split("\\|");
//                GlobalSearchService globalSearch = new GlobalSearchService(templateList[2],
//                        Collections.singletonList(portfolioRights.getTemplateValue()), portfolioRights.getSubTemplate(), portfolioRights.getSubTemplateValue(), templateList[0], portfolioRights.getDateValue(), null, templateList[1],
//                        null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
//                return globalSearch.getGlobalSearchResults(portfolioRights.getSearchWhereClause(), userName, portfolioRights.getTemplate(), countFetched, totalRecords, MxGroupPortfolioRights.class,false);

                saveWhereSearchHistory(portfolioRights.getSearchWhereClause(), userName, portfolioRights.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(portfolioRights.getDateValue(), dateTimeFormatter);
                Page<MxGroupPortfolioRights> portfolioList = mxGroupPortfolioRights.findByGroupLabelAndReportDateListWithSearch(portfolioRights.getTemplateValue(), requestDate, portfolioRights.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", portfolioList.getTotalPages());
                document.put("records", portfolioList.getTotalElements());
                document.put("content", portfolioList.getContent());
                return document;
            } else {
                String template = getPropertyFields(portfolioRights.getTemplate(), portfolioRights.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                SearchConfig<MxGroupPortfolioRights> mxPortfolioRightsSpecBuilder = new SearchConfig<>(templateList[2],
                        portfolioRights.getTemplateValue(), portfolioRights.getSubTemplate(), portfolioRights.getSubTemplateValue(), templateList[0], portfolioRights.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
                return mxPortfolioRightsSpecBuilder.getSearchResultUpdate(portfolioRights.getSearchWhereClause(), userName, portfolioRights.getTemplate(), countFetched, totalRecords);
            }
        }
    }


    @Transactional
    public Document getNavigationRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification navigationRights, String groupLabel, String userName, boolean countFetched, long totalRecords) {
        if (navigationRights.getSearchWhereClause() == null || navigationRights.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(navigationRights.getDateValue(), dateTimeFormatter);
            Page<MxGroupNavigationRight> groupNavigationList = groupNavigationRightsRepository.findByTemplateAndReportDateList(navigationRights.getTemplateValue().toUpperCase(), groupLabel.toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", groupNavigationList.getTotalPages());
            document.put("records", groupNavigationList.getTotalElements());
            document.put("content", groupNavigationList.getContent());
            return document;
        } else {
            if (navigationRights.isGlobalSearch()) {
                saveWhereSearchHistory(navigationRights.getSearchWhereClause(), userName, navigationRights.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(navigationRights.getDateValue(), dateTimeFormatter);
                Page<MxGroupNavigationRight> resultList = groupNavigationRightsRepository.findByGroupLabelAndReportDateListWithSearch(navigationRights.getTemplateValue(), groupLabel, requestDate, navigationRights.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", resultList.getTotalPages());
                document.put("records", resultList.getTotalElements());
                document.put("content", resultList.getContent());
                return document;
            }
            String template = getPropertyFields(navigationRights.getTemplate(), navigationRights.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxGroupNavigationRight> mxNavigationRightsSpecBuilder = new SearchConfig<>(templateList[2] + "~" + "groupLabel" + "~" + groupLabel,
                    navigationRights.getTemplateValue(), navigationRights.getSubTemplate(), navigationRights.getSubTemplateValue(), templateList[0], navigationRights.getDateValue(), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            Document document = mxNavigationRightsSpecBuilder.getSearchResultUpdate(navigationRights.getSearchWhereClause(), userName, navigationRights.getTemplate(), countFetched, totalRecords);
            return document;
        }
    }


    @Transactional
    public Document getGrpCompPortfolio(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification groupCombPortfolio, String userName, boolean countFetched, long totalRecords) {
        if (groupCombPortfolio.getSearchWhereClause() == null || groupCombPortfolio.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(groupCombPortfolio.getDateValue(), dateTimeFormatter);
            Page<MxGroupCombinedPortfolio> combinedPortfolioList = mxGroupCombinedPortfolioRepo.findByGroupLabelAndReportDateList(groupCombPortfolio.getTemplateValue().toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", combinedPortfolioList.getTotalPages());
            document.put("records", combinedPortfolioList.getTotalElements());
            document.put("content", combinedPortfolioList.getContent());
            return document;
        } else {
            if (groupCombPortfolio.isGlobalSearch()) {

                String template = getPropertyFields(groupCombPortfolio.getTemplate(), groupCombPortfolio.getSubTemplate(), true);
                String[] templateList = template.split("\\|");
                GlobalSearchService search = new GlobalSearchService(templateList[2],
                        new ArrayList<>(Collections.singleton(groupCombPortfolio.getTemplateValue())), groupCombPortfolio.getSubTemplate(), groupCombPortfolio.getSubTemplateValue(), templateList[0], groupCombPortfolio.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
                return search.getGlobalSearchResults(groupCombPortfolio.getSearchWhereClause(), userName, groupCombPortfolio.getTemplate(), countFetched, totalRecords, MxGroupCombinedPortfolio.class, false);

            } else {
                String template = getPropertyFields(groupCombPortfolio.getTemplate(), groupCombPortfolio.getSubTemplate(), false);
                String[] templateList = template.split("\\|");

                SearchConfig<MxGroupCombinedPortfolio> mxGroupCombPortfolioSpecBuilder = new SearchConfig<>(templateList[2],
                        groupCombPortfolio.getTemplateValue(), groupCombPortfolio.getSubTemplate(), groupCombPortfolio.getSubTemplateValue(), templateList[0], groupCombPortfolio.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
                return mxGroupCombPortfolioSpecBuilder.getSearchResultUpdate(groupCombPortfolio.getSearchWhereClause(), userName, groupCombPortfolio.getTemplate(), countFetched, totalRecords);
            }
        }
    }


    public LocalDate getLatestDate() {
        DataImportJob job = dataImportJobRepository.findDistinctTopByFileNameStartsWithAndPurgedAndJobStatusOrderByReportDateDesc("stp", 'N', MxJobLogType.COMPLETED);
        return job != null ? job.getReportDate() : null;
    }

    public Long getAuditHdrList(LocalDate requestDate) {
        return mxAuditRepo.findAllByRepDate(requestDate);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getAuditBdyForExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat,
                                                        List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
        List<MxSupChgAuditBdy> auditBodyTmplList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;

        long totalCount = 0;
        int totalPage = 1;

        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document auditBodyFields = getAuditBdyTmpl(page, pageSize, sortBy, sortingOrder, sort, filters, userName, false, totalCount);
//            log.debug(" Export: Found {} items to export in page {} of {} pages and total {} items.",
//                    auditBodyFields.getContent().size(),
//                    (i + 1),
//                    auditBodyFields.getTotalPages(),
//                    auditBodyFields.getTotalElements());

            auditBodyTmplList.addAll((List<MxSupChgAuditBdy>) auditBodyFields.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, auditBodyTmplList.size(), job.getId());
            totalPage = (int) auditBodyFields.get("totalPages");
            totalCount = (long) auditBodyFields.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", auditBodyTmplList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, auditBodyTmplList, fileName, fieldMaps, colors, job, fieldColumns);

    }

    @Transactional
    public Document getAuditBdyTmpl(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification auditBdy, String userName, boolean countFetched, long totalRecords) {
        Long auditId;
        if (auditBdy.getSearchWhereClause() == null || auditBdy.getSearchWhereClause().isBlank()) {
            Document document = new Document();

            LocalDate requestDate = LocalDate.parse(auditBdy.getDateValue(), dateTimeFormatter);
            auditId = Long.parseLong(auditBdy.getTemplateValue());
            Page<MxSupChgAuditBdy> auditBodyList = mxAuditBdyRepo.findByReportDateAndAuditIdList(auditId, requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", auditBodyList.getTotalPages());
            document.put("records", auditBodyList.getTotalElements());
            document.put("content", auditBodyList.getContent());
            return document;
        } else {
            if (auditBdy.isGlobalSearch()) {
                saveWhereSearchHistory(auditBdy.getSearchWhereClause(), userName, auditBdy.getTemplate());
                Document document = new Document();
                auditId = Long.parseLong(auditBdy.getTemplateValue());
                LocalDate requestDate = LocalDate.parse(auditBdy.getDateValue(), dateTimeFormatter);
                Page<MxSupChgAuditBdy> auditBodyList = mxAuditBdyRepo.findByReportDateAndAuditIdWithGlobalSearch(auditId, requestDate, auditBdy.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", auditBodyList.getTotalPages());
                document.put("records", auditBodyList.getTotalElements());
                document.put("content", auditBodyList.getContent());
                return document;
            }
            String template = getPropertyFields(auditBdy.getTemplate(), auditBdy.getSubTemplate(), false);
            String[] templateList = template.split("\\|");

            SearchConfig<MxSupChgAuditBdy> mxEnterpriseRiskBuilderSpecification = new SearchConfig<>(templateList[2],
                    auditBdy.getTemplateValue(), auditBdy.getSubTemplate(), auditBdy.getSubTemplateValue(), templateList[0], auditBdy.getDateValue(), null, templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
            Document document = mxEnterpriseRiskBuilderSpecification.getSearchResultUpdate(auditBdy.getSearchWhereClause(), userName, auditBdy.getTemplate(), countFetched, totalRecords);
            return document;
        }
    }

    public MxUserListItem getUserDetailCount(LocalDate requestDate) {
        return userListRepository.findTopByReportDate(requestDate);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getUserListExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                     String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {

        List<MxUserList> userLists = new ArrayList<>();
        int pageSize = 500;
        int page = 0;

        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document userList = getUserDetails(page, pageSize, sortBy, sortingOrder, sort, filters, userName, countFetched, totalCount);
            log.debug("userlist:{}", userList);
            userLists.addAll((List<MxUserList>) userList.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, userLists.size(), job.getId());
            totalPage = (int) userList.get("totalPages");
            totalCount = (long) userList.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", userLists.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, userLists, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document getUserDetails(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification userList, String userName, boolean countFetched, long totalRecords) {
        if (userList.getSearchWhereClause() == null || userList.getSearchWhereClause().isBlank() || userList.getSearchWhereClause().length() == 0) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(userList.getDateValue(), dateTimeFormatter);
            searchHistoryRepository.sqlMode();
            Page<MxUserList> counterpartyDisplay = userListRepository.findByTemplateAndReportDateList(userList.getTemplateValue().toUpperCase(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", counterpartyDisplay.getTotalPages());
            document.put("records", counterpartyDisplay.getTotalElements());
            document.put("content", counterpartyDisplay.getContent());
            return document;
        } else {
            if (userList.isGlobalSearch()) {
                saveWhereSearchHistory(userList.getSearchWhereClause(), userName, userList.getTemplate());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(userList.getDateValue(), dateTimeFormatter);
                Page<MxUserList> userDetails = userListRepository.findByTemplateAndReportDateListWithGlobalSearch(userList.getTemplateValue(), requestDate, userList.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", userDetails.getTotalPages());
                document.put("records", userDetails.getTotalElements());
                document.put("content", userDetails.getContent());
                return document;
            }
            String template = getPropertyFields(userList.getTemplate(), userList.getSubTemplate(), false);
            String[] templateList = template.split("\\|");

            SearchConfig<MxUserList> mxUserListSpec = new SearchConfig<>(templateList[3], userList.getTemplateValue(), userList.getSubTemplate(), userList.getSubTemplateValue(),
                    templateList[0], userList.getDateValue(), null, templateList[1], templateList[2], sortBy, sortingOrder, page, pageSize, "userList", searchHistoryRepository, entityManager);
            return mxUserListSpec.getSearchResultUpdate(userList.getSearchWhereClause(), userName, userList.getTemplate(), countFetched, totalRecords);
        }
    }

    @Transactional
    public Document getFinanceRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification financeRights, String userName, boolean countFetched, long totalRecords) {
        if (financeRights.getSearchWhereClause() == null || financeRights.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            LocalDate requestDate = LocalDate.parse(financeRights.getDateValue(), dateTimeFormatter);
            Page<MxFinaceAcctrlRights> financeList = financeRightsRepo.findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(financeRights.getTemplateValue(), financeRights.getSubTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", financeList.getTotalPages());
            document.put("records", financeList.getTotalElements());
            document.put("content", financeList.getContent());
            return document;
        } else {
            if (financeRights.isGlobalSearch()) {
                saveWhereSearchHistory(financeRights.getSearchWhereClause(), userName, financeRights.getTemplate() + financeRights.getSubTemplateValue().toUpperCase());
                Document document = new Document();
                LocalDate requestDate = LocalDate.parse(financeRights.getDateValue(), dateTimeFormatter);
                Page<MxFinaceAcctrlRights> financeList = financeRightsRepo.findByTemplateAndReportDateListWithGlobalSearch(financeRights.getTemplateValue(), financeRights.getSubTemplateValue(), requestDate, financeRights.getSearchWhereClause()
                        , PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", financeList.getTotalPages());
                document.put("records", financeList.getTotalElements());
                document.put("content", financeList.getContent());
                return document;
            }
            String template = getPropertyFields(financeRights.getTemplate(), financeRights.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxFinaceAcctrlRights> mxFinaceAcctrlRightsSpec = new SearchConfig<>(templateList[2],
                    financeRights.getTemplateValue(), templateList[5], financeRights.getSubTemplateValue(), templateList[0], financeRights.getDateValue(), null, templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

            return mxFinaceAcctrlRightsSpec.getSearchResultUpdate(financeRights.getSearchWhereClause(), userName, financeRights.getTemplate(), countFetched, totalRecords);

        }
    }

    public String getPropertyFields(String templateName, String subTemplate, boolean globalSearch) {
        try {
            String propertyField = "";
            String dateField = "";
            String subPropertyFields = "";
            File resource;
            if (globalSearch) {
                resource = new ClassPathResource("UAM/Generic_Search_Config_TableName.json").getFile();
            } else {
                resource = new ClassPathResource("UAM/Generic_Search_Config.json").getFile();
            }
            if (resource.exists()) {
                String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(defaultConfig);
                dateField = json.get("DATE_FIELD").toString();
                if (templateName != null && !templateName.isBlank()) {
                    propertyField = json.get(templateName).toString();
                    if (subTemplate != null && !subTemplate.isBlank()) {
                        subPropertyFields = json.get(subTemplate).toString();
                        return dateField + "|" + propertyField + "|" + subPropertyFields;
                    }
                    return dateField + "|" + propertyField;
                }
                return dateField;
            } else {
                return null;
            }
        } catch (IOException | ParseException ex) {
            log.error("Error reading / parsing ElasticSearchUtils config.", ex);
            return null;
        }
    }


//    public STPRightsMatrix getStpRightsList(LocalDate requestDate, String globalTem) {
//        return stpRightsRepo.findTopByGlobalTemAndReportDate(globalTem, requestDate);
//    }

//    @Async
//    @Transactional
//    public CompletableFuture<Void> getStpExport(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps, String color,
//                                                DownloadJob job, String outputFormat, List<String> fieldColumns, LocalDate repDate, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {
//        log.info(filters.toString());
//
//        List<STPRightsMatrix> stpTmplList = new ArrayList<>();
//        int pageSize = 500;
//        int page = 0;
//        long totalCount = 0;
//        int totalPage = 1;
//
//        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
//            filters.setTemplateValue("");
//        }
//        log.info(" Export: Initializing job status.");
//        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
//        log.info(" Export: Starting fetch.");
//        boolean countFetched = false;
//        for (int i = 0; i < totalPage; i++) {
//            Document stpTmplsRights = getStpList(page, pageSize, sortBy, sortingOrder, sort, repDate, filters, userName, countFetched, totalCount);
//
////            log.info(" Export: Found {} items to export in page {} of {} pages and total {} items.",
////                    stpTmplsRights.getContent().size(),
////                    (i + 1),
////                    stpTmplsRights.getTotalPages(),
////                    stpTmplsRights.getTotalElements());
//
//            stpTmplList.addAll((List<STPRightsMatrix>) stpTmplsRights.get("content"));
//            totalPage = (int) stpTmplsRights.get("totalPages");
//            totalCount = (long) stpTmplsRights.get("records");
//            countFetched = true;
//            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpTmplList.size(), job.getId());
//
//            page++;
//        }
//
//        log.info(" Export: Fetch complete.");
//        downloadJobService.updateJobProgress("FILE_WRITE", stpTmplList.size(), (int) totalCount, job.getId());
//        return exportFilefromDetails(outputFormat, stpTmplList, fileName, fieldMaps, color, job, fieldColumns);
//    }

//    @Transactional
//    public Document getStpList(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, LocalDate requestDate, GeneralSpecification stpExport, String username, boolean countFetched, long totalRecords) {
//        if (stpExport.getSearchWhereClause() == null || stpExport.getSearchWhereClause().isBlank()) {
//            Document document = new Document();
//
////         document=   prepareNativeQuery(requestDate.format(dateTimeFormatter),stpExport.getTemplateValue(),page,pageSize,sortBy,sortingOrder);
//            String formattedDate = requestDate != null ? requestDate.format(dateTimeFormatter) : null;
//            Page<STPRightsMatrix> stpRightsRights = stpRightsRepo.findDistinctByReportDateAndGlobalTem(requestDate, stpExport.getTemplateValue(), PageRequest.of(page, pageSize, Sort.by(sort)));
//            document.put("totalPages", stpRightsRights.getTotalPages());
//            document.put("records", stpRightsRights.getTotalElements());
//            document.put("content", stpRightsRights.getContent());
//            return document;
//        } else {
//            String template = getPropertyFields(stpExport.getTemplate(), stpExport.getSubTemplate());
//            String[] templateList = template.split("\\|");
//
//            SearchConfig<STPRightsMatrix> mxStpModelSpec = new SearchConfig<>(templateList[2],
//                    stpExport.getTemplateValue(), stpExport.getSubTemplate(), stpExport.getSubTemplateValue(), templateList[0], requestDate.format(dateTimeFormatter), null, templateList[1], null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);
//            log.info(" search query fetch started:{}", LocalDateTime.now());
//
//            return mxStpModelSpec.getSearchResultUpdate(stpExport.getSearchWhereClause(), username, stpExport.getTemplate(), countFetched, totalRecords);
//        }
//    }

//    private Document prepareNativeQuery(String repDate, String templateValue, int page, int pageSize, String sortBy, String sortingOrder) {
//        List<STPRightsMatrix> stpRightsMatrices = new ArrayList<STPRightsMatrix>();
//        Document doc = new Document();
//        try (Connection connection = ConnectionService.getConnection()) {
//            StringBuilder query = new StringBuilder();
////            query="SELECT * FROM UAM_MX_STP_RIGHTS_MATRIX WHERE GLOBALTEM =? and to_char(REP_DATE,'YYYYMMDD')=?  ORDER BY %s %s" + " OFFSET nvl(" + page+","+1+")*"+ pageSize +" ROWS FETCH NEXT "+ pageSize+" ROWS ONLY ";
//            query.append("SELECT * FROM UAM_MX_STP_RIGHTS_MATRIX WHERE GLOBALTEM =? and date_format(REP_DATE,'%Y%m%d')=?  ORDER BY ? ? LIMIT " + page + ", " + pageSize);
//
////            String matrixList = String.format(query.toString(), sortBy, sortingOrder);
//            PreparedStatement preparedStatement = connection.prepareStatement(String.valueOf(query));
//            preparedStatement.setString(1, templateValue);
//            preparedStatement.setString(2, repDate);
//            preparedStatement.setString(3, sortBy);
//            preparedStatement.setString(4, sortingOrder);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            while (resultSet.next()) {
//                STPRightsMatrix stpRightsMatrix = new STPRightsMatrix();
//                stpRightsMatrix.setId(resultSet.getLong("ID"));
//                stpRightsMatrix.setGlobalTem(resultSet.getString("GLOBALTEM"));
//                stpRightsMatrix.setBoType(resultSet.getString("BOTYPE"));
//                stpRightsMatrix.setStatus(resultSet.getString("SOURCEMOD"));
//                stpRightsMatrix.setTypology(resultSet.getString("TYPOLOGY"));
//                stpRightsMatrix.setStatus(resultSet.getString("STATUS"));
//                stpRightsMatrix.setStpView(resultSet.getString("STPVIEW"));
//                stpRightsMatrix.setChecked(resultSet.getString("CHECKED"));
//                stpRightsMatrices.add(stpRightsMatrix);
//            }
//            query.setLength(0);
//            query.append("SELECT count(*) FROM UAM_MX_STP_RIGHTS_MATRIX WHERE GLOBALTEM =? and date_format(REP_DATE,'%Y%m%d')=? ");
//            PreparedStatement countStatement = connection.prepareStatement(String.valueOf(query));
//            countStatement.setString(1, templateValue);
//            countStatement.setString(2, repDate);
//            ResultSet countResult = countStatement.executeQuery();
//            int count = 0;
//            if (countResult.next()) {
//                count = countResult.getInt(1);
//            }
//            doc.put("totalPages", (int) Math.ceil((float) count / (float) pageSize));
//            doc.put("records", count);
//            doc.put("content", stpRightsMatrices);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return doc;
//
//    }

    public GroupList getUserPreference(String userGroup, LocalDate repDate) {
        List<GroupList> groupDetails = groupListRepo.findTopByGroupLabelAndReportDate(userGroup, repDate);
        return !groupDetails.isEmpty() ? groupDetails.get(0) : new GroupList();
    }

    public Document getAllFilters(LocalDate reportDate, GeneralFilters filters) {

        Document document = new Document();
        List<?> filterList = new ArrayList<>();
        switch (filters.getReportType()) {
            case "PORTFOLIO_RIGHTS": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxGroupPortfolioRights.class);
                break;
            }
            case "COMBINED_PORTFOLIO": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxGroupCombinedPortfolio.class);

                break;
            }
            case "CHINESE_WALL_DEFINITIONS": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxChineseWallTmpl.class);

                break;
            }
            case "NAVIGATION_RIGHTS": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxGroupNavigationRight.class);

                break;
            }
            case "OSP_RIGHTS": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxOspRightsMatrix.class);

                break;
            }
//            case "STP_RIGHTS": {
//                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), STPRightsMatrix.class);
//
//                break;
//            }
            case "CONSISTENCY_TEMPLATE": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxConsistencyTmpl.class);
                break;
            }
            case "OPERATION_RIGHTS": {
                if (filters.getSubTemplate().equalsIgnoreCase("LPOS")) {
                    filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxOperationRights.class);
                } else {
                    filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxOperationRights.class);
                }
                break;
            }
            case "FINANCE_RIGHTS": {
                if (filters.getSubTemplate().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                    filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxFinaceAcctrlRights.class);
                } else {
                    filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxFinaceAcctrlRights.class);
                }
                break;
            }
            case "CONFIGURATION_MANAGEMENT_RIGHTS": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxCwtConfigMgtRight.class);
                break;
            }
            case "ENTERPRISE_RISK_MANAGEMENT": {
                filterList = getSpecificFields(reportDate, filters.getTemplate(), filters.getSubTemplate(), filters.getPropertyName(), filters.getGroupLabel(), filters.getReportType(), filters.getSubReportType(), MxEnterpriseRisk.class);
                break;
            }
            case "USER_LIST": {
                break;
            }

        }
        document.put("content", filterList);
        return document;
    }

    public List<?> getSpecificFields(LocalDate reportDate, String template, String subTemplate, String propertyName, String groupLabel, String reportType, String subReportType, Class filterClass) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> cr = cb.createQuery(filterClass);
        Root<?> root = cr.from(filterClass);
        List<Predicate> predicates = new ArrayList<>();
        cr.select(root.get(propertyName)).distinct(true);
        predicates.add(cb.equal(root.get(subTemplate), template));
        predicates.add(cb.equal(root.get("reportDate"), reportDate));
        Query query = entityManager.createQuery(cr.where(cb.and(predicates.toArray(new Predicate[0]))));
        return query.getResultList();
    }

    public Document getAllTemplateByReportType(LocalDate reportDate, GeneralFilters filters, Document document) {

        List<?> filterList = new ArrayList<>();
        switch (filters.getReportType()) {
            case "CHINESE_WALL_DEFINITIONS":
                filterList = groupListRepo.findUniqueChineseWallTemplates(reportDate);
                break;
            case "NAVIGATION_RIGHTS":
                filterList = groupListRepo.findUniqueNavigationTemplates(reportDate);
                break;
            case "OSP_RIGHTS":
                filterList = groupListRepo.findUniqueOspRightsMatrixTemplates(reportDate);
                break;
            case "STP_RIGHTS":
                filterList = groupListRepo.findUniqueStpRightsTemplates(reportDate);
                break;
            case "CONSISTENCY_TEMPLATE":
                filterList = groupListRepo.findUniqueConsistencyTemplates(reportDate);
                break;
            case "OPERATION_RIGHTS":
                if (filters.getSubTemplate().equalsIgnoreCase("NKEY")) {
                    filterList = groupListRepo.findUniqueOperationRightsTemplateByNkey(reportDate);
                } else {
                    filterList = groupListRepo.findUniqueOperationRightsTemplateByLpos(reportDate);
                }
                break;
            case "FINANCE_RIGHTS":
                if (filters.getSubTemplate().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                    filterList = groupListRepo.findUniqueFinanceTemplateByAccCtrl(reportDate);
                } else {
                    filterList = groupListRepo.findUniqueFinanceTemplateByStatTmpl(reportDate);
                }
                break;
            case "PORTFOLIO_RIGHTS":
            case "COMBINED_PORTFOLIO":
            case "CONFIGURATION_MANAGEMENT_RIGHTS":
            case "ENTERPRISE_RISK_MANAGEMENT":
                filterList = groupListRepo.findUniqueGroupLabels(reportDate);
                break;
            case "USER_LIST": {
                break;
            }
        }
        document.put("content", filterList);
        return document;
    }

    public Document getAllGroupsByTemplate(LocalDate reportDate, GeneralFilters reportType, Document document) {
        List<?> filterList = new ArrayList<>();
        switch (reportType.getReportType()) {
            case "PORTFOLIO_RIGHTS":
                break;
            case "COMBINED_PORTFOLIO":
                break;
            case "CHINESE_WALL_DEFINITIONS":
                filterList = groupListRepo.findGroupsByChineseWallTemplates(reportDate, reportType.getTemplate());
                break;
            case "NAVIGATION_RIGHTS":
                break;
            case "OSP_RIGHTS":
                break;
            case "STP_RIGHTS":
                break;
            case "CONSISTENCY_TEMPLATE":
                filterList = groupListRepo.findAllConsistencyGroupNameByTemplate(reportDate, reportType.getTemplate());
                break;
            case "OPERATION_RIGHTS":
                break;
            case "FINANCE_RIGHTS":
                break;
            case "CONFIGURATION_MANAGEMENT_RIGHTS":
                break;
            case "ENTERPRISE_RISK_MANAGEMENT":
                break;
            case "USER_LIST": {
                break;
            }
        }
        document.put("content", filterList);
        return document;
    }

    @Transactional
    @Async
    public CompletableFuture<Void> getCounterpartyDisplayExportFile(GeneralSpecification filters, String fileName, List<FieldMap> fieldMaps,
                                                                    String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, String userName) throws Exception {

        int pageSize = 20000;
        int page = 0;

        File file = new File(generateExportPathUsingFileName(fileName + outputFormat));
        file.getParentFile().mkdirs();
        if (filters.getTemplateValue() == null && filters.getSearchWhereClause().isBlank()) {
            filters.setTemplateValue("");
        }
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        Document counterParty = getCounterPartyDisplay(page, pageSize, sortBy, sortingOrder, sort, filters, userName, false, 0);

        if (outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xls")) {
            CompletableFuture<Void> future = exportExcelFilefromDetails(file, (List<?>) counterParty.get("content"), fieldMaps, colors, job, fieldColumns,
                    (int) counterParty.get("totalPages"), (long) counterParty.get("records"), pageSize, sortBy, sortingOrder, sort, filters, userName);
        } else {
            CompletableFuture<Void> future = exportCsvFilefromDetails(file, (List<?>) counterParty.get("content"), fieldMaps, colors, job, fieldColumns,
                    (int) counterParty.get("totalPages"), (long) counterParty.get("records"), pageSize, sortBy, sortingOrder, sort, filters, userName);
        }
        return CompletableFuture.completedFuture(null);
    }


    @Async
//    @Transactional
    public CompletableFuture<Void> exportExcelFilefromDetails(File file, List<?> exportData, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, Integer totalPage, long totalSize,
                                                              int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification filters, String userName) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyyMMdd HHmmss"));
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        List<MxCounterPartyDisplay> finalList = new ArrayList<>((List<MxCounterPartyDisplay>) exportData);

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);

        SXSSFSheet sheet = workbook.createSheet();
        int hdrRowCount = 0;
        int columnCount = 0;
        SXSSFRow headerRow = sheet.createRow(hdrRowCount);
        fieldColumns.add("NO");
        for (FieldMap fieldMap : fieldMaps) {
            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getDisplayName())) {
                SXSSFCell cell = headerRow.createCell(columnCount++);
                cell.setCellValue(fieldMap.getDisplayName());
                hdrRowCount++;
            }
        }
        try {
            AtomicInteger f = new AtomicInteger(1);
            List<CompletableFuture<SXSSFRow>> completableFutureList = new ArrayList<>();
            int processed = finalList.size();
            AtomicInteger row_no = new AtomicInteger(1);
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalSize, finalList.size(), job.getId());

            for (int page = 0; page == 0 || page < totalPage; page++) {

                if (page > 0) {
                    Document doc = getCounterPartyDisplay(page, pageSize, sortBy, sortingOrder, sort, filters, userName, true, totalSize);
                    finalList.addAll((List<MxCounterPartyDisplay>) doc.get("content"));
                    processed = processed + finalList.size();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalSize, processed, job.getId());
                }

                int recordsToBeWrite = 0;
                String jsonString = mapper.writeValueAsString(finalList);
                List<Map<String, Object>> list = mapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {
                });
                finalList.clear();
                if (!list.isEmpty()) {
                    for (Map<String, Object> maps : list) {
                        SXSSFRow row = sheet.createRow(row_no.getAndIncrement());
                        CompletableFuture<SXSSFRow> future = CompletableFuture.supplyAsync(() -> {
                            writeXlsx(maps, fieldMaps, row, fieldColumns);
                            return null;
                        });
                        completableFutureList.add(future);
                        recordsToBeWrite++;
                        if (recordsToBeWrite == 100) {
                            recordsToBeWrite = 0;
                            CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[100])).join();
                            completableFutureList.clear();
                        }
                    }
                }
                list.clear();
            }
            log.info("XLSX Export: Flushing to file.:{}", row_no.get());
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception occurs :{}", e.getMessage());
        }
        // dispose of temporary files backing this workbook on disk
        workbook.close();
        downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
        log.info("XLSX Export: Complete.");
        return CompletableFuture.completedFuture(null);
    }


    //    @Async
    public void writeXlsx(Map<String, Object> trade, List<FieldMap> fieldMaps, SXSSFRow row, List<String> fieldColumns) {

        // write values in xlsx
        int fieldColumnCount = 0;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
        try {
            if (trade.get("id") != null) {

                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getDisplayName())) {

                        SXSSFCell cell = row.createCell(fieldColumnCount++);
                        String fieldName = fieldMap.getEntityName();
                        String fieldFormat = fieldMap.getFormat();
                        String fieldValue = "";
                        if (fieldName.equalsIgnoreCase("id")) {
                            fieldValue = String.valueOf(row.getRowNum());
                            cell.setCellValue(fieldValue);
                        } else if (fieldFormat != null) {
                            if (trade.get(fieldName) != null && !trade.get(fieldName).toString().isBlank()) {
                                if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy") && (!trade.get(fieldName).toString().contains("-"))) {
                                    fieldValue = LocalDate.parse(trade.get(fieldName).toString(), dateFormatter).format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy HH:mm:ss")) {
                                    fieldValue = LocalDateTime.parse(trade.get(fieldName).toString(), dateTimeformatter).format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy")) {
                                    fieldValue = LocalDate.parse(trade.get(fieldName).toString()).format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else {
                                    fieldValue = LocalTime.parse(trade.get(fieldName).toString()).format(DateTimeFormatter.ofPattern(fieldFormat));
                                }
                                cell.setCellValue(fieldValue);
                            } else {
                                cell.setCellValue(blankFields);
                            }
                        } else {
                            fieldValue = trade.get(fieldName) == null ? null : trade.get(fieldName).toString();
                            cell.setCellValue((fieldValue != null ? fieldValue : blankFields));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void exportAsCsv(BufferedWriter bw, Map<String, Object> list, File file, List<FieldMap> fieldMaps, DownloadJob job, List<String> fieldColumns) {
//        fieldColumns.add("id");
        try {
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("Cp1252")))) {
            //try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.defaultCharset()))) {
            //try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("windows-1252")))) {
            // write header in csv

            bw.newLine();
            // write values in csv
            int lineCount = 0;
//            for (Map<String, Object> trade : list) {
            int j = 1;
            for (FieldMap fieldMap : fieldMaps) {
                if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                    String fieldName = fieldMap.getEntityName();
                    String fieldFormat = fieldMap.getFormat();
                    String fieldDisplayName = fieldMap.getDisplayName();
                    String fieldValue = "";
                    if (fieldName.equalsIgnoreCase("id")) {
                        fieldValue = String.valueOf(lineCount + 1);
                    } else if (fieldFormat != null) {
                        if (list.get(fieldName) != null && !list.get(fieldName).toString().isBlank()) {
                            if (fieldFormat.equalsIgnoreCase("yyyyMMdd")) {
                                LocalDate localDate = LocalDate.parse(list.get(fieldName).toString());
                                fieldValue = localDate.format(DateTimeFormatter.ofPattern(fieldFormat));
                            } else if (fieldFormat.equalsIgnoreCase("yyyyMMdd hh:mm:ss")) {
                                LocalDateTime localDateTime = LocalDateTime.parse(list.get(fieldName).toString());
                                fieldValue = localDateTime.format(DateTimeFormatter.ofPattern(fieldFormat));
                            } else {
                                LocalTime localTime = LocalTime.parse(list.get(fieldName).toString());
                                fieldValue = localTime.format(DateTimeFormatter.ofPattern(fieldFormat));
                            }

                        } else {
                            fieldValue = "";
                        }
                    } else {
                        fieldValue = list.get(fieldName) == null ? null : list.get(fieldName).toString();
                    }
                    bw.write((fieldValue != null ? fieldValue : ""));
                    if (fieldMaps.size() != j) {
                        bw.append(",");
                    }
                    j++;
                }
//                }
//                bw.newLine();
                lineCount++;
            }
//            if (job != null) {
//                downloadJobService.updateJobProgress("FILE_WRITE", list.size(), lineCount, job.getId());
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        return file;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> exportCsvFilefromDetails(File file, List<?> exportData, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, int totalPage, long totalSize,
                                                            int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecification filters, String userName) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyyMMdd HHmmss"));
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        List<MxCounterPartyDisplay> finalList = new ArrayList<>((List<MxCounterPartyDisplay>) exportData);
        fieldColumns.add("NO");
        int processed = finalList.size();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        List<MxCounterPartyCsvExportRequest> rows = new ArrayList<>();
        try {

            long count = 1;

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalSize, finalList.size(), job.getId());

            log.info("totalSize:{}", totalSize);

            for (int page = 0; page == 0 || page < totalPage; page++) {

                if (page > 0) {
                    Document doc = getCounterPartyDisplay(page, pageSize, sortBy, sortingOrder, sort, filters, userName, true, totalSize);
                    finalList.addAll((List<MxCounterPartyDisplay>) doc.get("content"));
                    processed = processed + finalList.size();
                    if (page + 1 < totalPage) {
                        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalSize, processed, job.getId());
                    }
                }

                String jsonString = mapper.writeValueAsString(finalList);


                List<MxCounterPartyDisplay> list = mapper.readValue(jsonString, new TypeReference<List<MxCounterPartyDisplay>>() {
                });
                finalList.clear();

                for (MxCounterPartyDisplay mxCounterPartyDisplay : list) {
                    mxCounterPartyDisplay.setId(count++);
                    MxCounterPartyCsvExportRequest row = mapper.convertValue(mxCounterPartyDisplay, MxCounterPartyCsvExportRequest.class);
                    row.setInsDate(mxCounterPartyDisplay.getInsDate() != null ? mxCounterPartyDisplay.getInsDate().format(formatter) : blankFields);
                    row.setModDate(mxCounterPartyDisplay.getModDate() != null ? mxCounterPartyDisplay.getModDate().format(formatter) : blankFields);
                    row.setStartDt(mxCounterPartyDisplay.getStartDt() != null ? mxCounterPartyDisplay.getStartDt().format(formatter) : blankFields);
                    row.setEndDt(mxCounterPartyDisplay.getEndDt() != null ? mxCounterPartyDisplay.getEndDt().format(formatter) : blankFields);
                    rows.add(row);
                }
            }

            JsonNode jsonTree = mapper.readTree(mapper.writeValueAsString(rows));
            CsvMapper csvMapper = new CsvMapper();
            csvMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
            CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
            //fetch header from the obejct and create header
            if (totalSize > 0) {
                jsonTree.iterator().next().fieldNames().forEachRemaining(fieldName -> {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldName)) {
                        csvSchemaBuilder.addColumn(fieldName);
                    }
                });
                CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
                //export value with header and values.
                csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(file, jsonTree);
            } else {
                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getDisplayName())) {
                        csvSchemaBuilder.addColumn(fieldMap.getDisplayName());
                    }
                }
                CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
                csvMapper.writerFor(JsonNode.class).with(csvSchema).writeValue(file, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception Occur while processsing the request:{}", e.getMessage());
        }
        downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalSize, processed, job.getId());

        log.info("CSV Export: Flushing to file.");
        downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);

        // dispose of temporary files backing this workbook on disk
        log.info("CSV Export: Complete.");
        return CompletableFuture.completedFuture(null);
    }

    public List<?> getGroupValues(LocalDate repDate, GeneralFilters filters) {
        List<CountUserDetails> userDetails = new ArrayList<>();
        List<Object[]> object = new ArrayList<>();
        List<?> groupLabels = new ArrayList<>();
        groupLabels = getSpecificFields(repDate, filters.getTemplate(), filters.getTemplateName(),
                "groupLabel", "", filters.getGroupLabel(), filters.getReportType(), MxGroupsListItem.class);
        switch (filters.getReportType()) {
            case "CHINESE_WALL_DEFINITIONS": {
                object = groupListRepo.getChineseWallTemplateDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
                break;
            }
//            case "STP_RIGHTS": {
//                if (isExport) {
//                    groupDetails = groupListRepo.getStpRightsTemplate(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
//                } else {
//
//                    object = groupListRepo.getStpRightsTemplateDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
//                }
//                break;
//            }
            case "CONSISTENCY_TEMPLATE": {
                object = groupListRepo.getConsistencyTemplateDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
                break;
            }
            case "FINANCE_RIGHTS": {
                if (filters.getSubReportType().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                    object = groupListRepo.getFinanaceRightDetailsNativeAcctrl(groupLabels, repDate.format(dateTimeFormatter));
                } else {
                    object = groupListRepo.getFinanceRightDetailsNativeStatCateg(groupLabels, repDate.format(dateTimeFormatter));
                }
                break;
            }
            case "OPERATION_RIGHTS": {
                if (filters.getSubReportType().equalsIgnoreCase("NKEY")) {
                    object = groupListRepo.getOperationNkeyDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
                } else {
                    object = groupListRepo.getOperationLposDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
                }
                break;
            }
            case "NAVIGATION_RIGHTS": {
                object = groupListRepo.getNavigationRightsDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
                break;
            }
            case "OSP_RIGHTS": {
                object = groupListRepo.getOspRightsTemplateDetailsNative(groupLabels, repDate.format(dateTimeFormatter));
                break;
            }
        }
        userDetails = getUserDetailsCount(object);
        return userDetails;
    }

    private List<CountUserDetails> getUserDetailsCount(List<Object[]> object) {
        int i = 0;
        return object.stream().map(ch -> new CountUserDetails(new BigInteger(ch[i].toString()).longValue(),
                ch[i + 1] != null ? ch[i + 1].toString() : null, ch[i + 2] != null ? ch[i + 2].toString() : null,
                ch[i + 3] != null ? ch[i + 3].toString() : null, ch[i + 4] != null ? ch[i + 4].toString() : null, new BigInteger(ch[i + 5].toString()).longValue())).collect(Collectors.toList());
    }

    public List<UserGroupDetails> getUserDetailsData(String groupValue, LocalDate repDate, Sort.Order sort) {
        return userListRepository.findUserDetailsByGroupLabel(groupValue, repDate, Sort.by(sort));
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupDetailsFromTemplateExport(String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, LocalDate repDate, List<String> fieldColumns, Sort.Order sort, GeneralFilters filters) throws Exception {
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        List<?> details = getGroupValuesExport(repDate, sort, filters);
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", details.size(), details.size(), job.getId());
        return exportFilefromDetails(outputFormat, details, fileName, fieldMaps, color, job, fieldColumns);
    }

    public List<?> getGroupValuesExport(LocalDate repDate, Sort.Order sort, GeneralFilters filters) {
        List<?> groupExport = new ArrayList<>();
        switch (filters.getReportType()) {
            case "CHINESE_WALL_DEFINITIONS":
                groupExport = groupListRepo.getChineseWallTemplateDetails(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                break;
            case "NAVIGATION_RIGHTS":
                groupExport = groupListRepo.getNavigationTemplate(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                break;
            case "OSP_RIGHTS":
                groupExport = groupListRepo.getOspRightsTemplate(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                break;
            case "STP_RIGHTS":
                break;
            case "CONSISTENCY_TEMPLATE":
                groupExport = groupListRepo.getConsistencyTemplateDetails(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                break;
            case "OPERATION_RIGHTS":
                if (filters.getSubReportType().equalsIgnoreCase("NKEY")) {
                    groupExport = groupListRepo.getOperationNkeyTemplateDetails(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                } else {
                    groupExport = groupListRepo.getOperationLposTemplateDetails(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                }
                break;
            case "FINANCE_RIGHTS":
                if (filters.getSubReportType().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                    groupExport = groupListRepo.getFinanceAcctrlTemplateDetails(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                } else {
                    groupExport = groupListRepo.getFinanceStatCategTemplateDetails(filters.getTemplate(), repDate, Sort.by(sort), filters.getGroupLabel());
                }
                break;
        }
        return groupExport;
    }

    public LocalDate getStpMatrixLatestDate() {
        //if stp three files are not loaded properly for the same date the latest date will be null
        Page<LocalDate> latestDate = dataImportJobRepository.findLatestDate(stpFileCount, 'N', MxJobLogType.COMPLETED, PageRequest.of(0, 1));
        return latestDate.getContent().size() == 0 ? null : latestDate.getContent().get(0);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getStpMatrixExport(String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat, LocalDate requestDate, List<String> fieldColumns, Sort.Order sort, String userName, StpMatrixExportXlsxRequest filters, String sortingOrder, String sortBy) throws Exception {

        List<MxStpRightMatrixEod> stpRightsMatrices = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document stpMatrixDetails = getStpMatrixDetails(page, pageSize, sortBy, sortingOrder, requestDate, filters.filters.getTemplateValue(), sort, filters.filters, userName, countFetched, totalCount);

            log.debug("stp Matrix details:{}", stpMatrixDetails);

            stpRightsMatrices.addAll((List<MxStpRightMatrixEod>) stpMatrixDetails.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpRightsMatrices.size(), job.getId());
            totalPage = (int) stpMatrixDetails.get("totalPages");
            totalCount = (long) stpMatrixDetails.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", stpRightsMatrices.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, stpRightsMatrices, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document getStpMatrixDetails(int page, int pageSize, String sortBy, String sortingOrder, LocalDate requestDate, String templateValue, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalRecords) {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            Page<MxStpRightMatrixEod> stpRightsMatrix = stpRightsRepository.findDistinctByReportDateAndGlobalTemplate(requestDate, templateValue, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", stpRightsMatrix.getTotalPages());
            document.put("records", stpRightsMatrix.getTotalElements());
            document.put("content", stpRightsMatrix.getContent());
            return document;
        } else {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), userName, filters.getTemplate());
                //log- first
                Document document = new Document();
                Page<MxStpRightMatrixEod> stpRightsMatrix = stpRightsRepository.findByTemplateAndReportDateListWithGlobalSearch(filters.getTemplateValue(), requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", stpRightsMatrix.getTotalPages());
                document.put("records", stpRightsMatrix.getTotalElements());
                document.put("content", stpRightsMatrix.getContent());
                return document;
            }
            String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxStpRightMatrixEod> stpMatrixSpecBuilder = new SearchConfig<>(templateList[2],
                    filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], requestDate.format(dateTimeFormatter), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

            return stpMatrixSpecBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalRecords);
        }
    }

    public MxStpRightMatrixEod getStpRightsMatrix(LocalDate requestDate, String templateValue) {
        return stpRightsRepository.findTopByReportDateAndGlobalTemplate(requestDate, templateValue);
    }

    public Document generalException(Exception ex, String filters, Document document) {
        ex.printStackTrace();
        log.info("Exception occured while data retrieval :{}", ex.getMessage());
        document.put("content", Collections.emptyList());
        if (filters != null && !filters.isBlank()) {
            document.put("message", "Invalid Query.");
        } else {
            document.put("message", "There is a problem in processing your request.");
        }
        return document;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getUserDetailsFromGroup(String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, LocalDate repDate, List<String> fieldColumns, Sort.Order sort, String groupLabel) throws Exception {
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        List<UserGroupDetails> details = getUserDetailsData(groupLabel, repDate, sort);
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", details.size(), details.size(), job.getId());
        return exportFilefromDetails(outputFormat, details, fileName, fieldMaps, color, job, fieldColumns);
    }

    public LocalDate getStpSrcModuleLatestDate() {
        DataImportJob job = dataImportJobRepository.findDistinctTopByFileNameStartsWithAndPurgedAndJobStatusOrderByReportDateDesc("stprightssrcmodeod", 'N', MxJobLogType.COMPLETED);
        return job != null ? job.getReportDate() : null;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getStpSrcModuleExport(String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat, LocalDate requestDate, List<String> fieldColumns, Sort.Order sort, String userName, StpMatrixExportXlsxRequest filters, String sortingOrder, String sortBy) throws Exception {
        List<MxStpSourceModuleEod> stpSrcModuleList = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document stpSrcModuleDetails = getStpSrcModuleDetails(page, pageSize, sortBy, sortingOrder, requestDate, filters.filters.getTemplateValue(), sort, filters.filters, userName, countFetched, totalCount);

            log.debug("stp Matrix details:{}", stpSrcModuleDetails);

            stpSrcModuleList.addAll((List<MxStpSourceModuleEod>) stpSrcModuleDetails.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpSrcModuleList.size(), job.getId());
            totalPage = (int) stpSrcModuleDetails.get("totalPages");
            totalCount = (long) stpSrcModuleDetails.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", stpSrcModuleList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, stpSrcModuleList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document getStpSrcModuleDetails(int page, int pageSize, String sortBy, String sortingOrder, LocalDate requestDate, String templateValue, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalCount) {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            Page<MxStpSourceModuleEod> stpSrcModule = stpSrcModuleRepository.findDistinctByReportDateAndGlobalTemplate(requestDate, templateValue, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", stpSrcModule.getTotalPages());
            document.put("records", stpSrcModule.getTotalElements());
            document.put("content", stpSrcModule.getContent());
            return document;
        } else {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), userName, filters.getTemplate());
                Document document = new Document();
                Page<MxStpSourceModuleEod> stpSrcModule = stpSrcModuleRepository.findDistinctByReportDateAndGlobalTemplateWithGlobalSearch(filters.getTemplateValue(), requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", stpSrcModule.getTotalPages());
                document.put("records", stpSrcModule.getTotalElements());
                document.put("content", stpSrcModule.getContent());
                return document;
            }
            String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<MxStpSourceModuleEod> stpSrcModuleSpecBuilder = new SearchConfig<>(templateList[2],
                    filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], requestDate.format(dateTimeFormatter), null, templateList[1],
                    null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

            return stpSrcModuleSpecBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalCount);
        }
    }

    public MxStpSourceModuleEod getStpSrcModule(LocalDate requestDate, String templateValue) {
        return stpSrcModuleRepository.findTopByReportDateAndGlobalTemplate(requestDate, templateValue);
    }


    public Document getStpRightsMatrixFilterDetails(int page, int pageSize, String sortBy, String sortingOrder, LocalDate requestDate, String boType, String boTemplate, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalCount) {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            List<StpMatrixModel> res = new ArrayList<>();

//            Page<StpMatrixModel> stpSrcModule = stpRightsRepository.findStpMatrixFilterByRepDateAndTemplate(requestDate, filters.getTemplateValue(), boTemplate, boType, PageRequest.of(page, pageSize, Sort.by(sort)));
            List<Object[]> stpDetails = stpRightsRepository.findStpMatrixFilterByRepDateAndTemplateNative(requestDate.format(dateTimeFormatter), filters.getTemplateValue(), boTemplate, boType);
            res = stpDetails.stream().map(ch -> new StpMatrixModel(ch[0] != null ? ch[0].toString() : null,
                    ch[1] != null ? ch[1].toString() : null, ch[2] != null ? ch[2].toString() : null,
                    ch[3] != null ? ch[3].toString() : null, ch[4] != null ? ch[4].toString() : null)).collect(Collectors.toList());
            long start = PageRequest.of(page, pageSize, Sort.by(sort)).getOffset();
            long end = (start + PageRequest.of(page, pageSize, Sort.by(sort)).getPageSize()) > res.size() ? res.size() : (start + PageRequest.of(page, pageSize, Sort.by(sort)).getPageSize());
            log.info("Pagination ented:{}", LocalDate.now());

            Page<StpMatrixModel> stpSrcModule = new PageImpl<>(res.subList((int) start, (int) end), PageRequest.of(page, pageSize, Sort.by(sort)), res.size());
            document.put("totalPages", stpSrcModule.getTotalPages());
            document.put("records", stpSrcModule.getTotalElements());
            document.put("content", stpSrcModule.getContent());
            return document;
        }
        String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
        String[] templateList = template.split("\\|");
        SearchConfig<MxStpSourceModuleEod> stpSrcModuleSpecBuilder = new SearchConfig<>(templateList[2],
                filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], requestDate.format(dateTimeFormatter), null, templateList[1],
                null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

        return stpSrcModuleSpecBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalCount);
    }

    public LocalDate getStpRightsTypologyLatestDate() {
        DataImportJob job = dataImportJobRepository.findDistinctTopByFileNameStartsWithAndPurgedAndJobStatusOrderByReportDateDesc("stprightstypology", 'N', MxJobLogType.COMPLETED);
        return job != null ? job.getReportDate() : null;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getStpRightsTypologyExport(String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat,
                                                              LocalDate requestDate, List<String> fieldColumns, Sort.Order sort, String userName,
                                                              StpMatrixExportXlsxRequest filters, String sortingOrder, String sortBy) throws Exception {
        List<MxStpRightsTypologyEod> stpRightsTypologyList = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document stpRightsTypologyDetail = getStpRightsTypologyDetails(page, pageSize, sortBy, sortingOrder, requestDate, filters.filters.getTemplateValue(), sort, filters.filters, userName, countFetched, totalCount);

            log.debug("stp Matrix details:{}", stpRightsTypologyDetail);

            stpRightsTypologyList.addAll((List<MxStpRightsTypologyEod>) stpRightsTypologyDetail.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpRightsTypologyList.size(), job.getId());
            totalPage = (int) stpRightsTypologyDetail.get("totalPages");
            totalCount = (long) stpRightsTypologyDetail.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", stpRightsTypologyList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, stpRightsTypologyList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    @Transactional
    public Document getStpRightsTypologyDetails(int page, int pageSize, String sortBy, String sortingOrder, LocalDate requestDate, String templateValue, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalCount) {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            Page<MxStpRightsTypologyEod> stpRightsTypology = stpRightsTypologyRepository.findDistinctByTypologyGroupAndReportDate(templateValue, requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", stpRightsTypology.getTotalPages());
            document.put("records", stpRightsTypology.getTotalElements());
            document.put("content", stpRightsTypology.getContent());
            return document;
        }
        String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
        String[] templateList = template.split("\\|");
        SearchConfig<MxStpRightsTypologyEod> stpMatrixSpecBuilder = new SearchConfig<>(templateList[2],
                filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], requestDate.format(dateTimeFormatter), null, templateList[1],
                null, sortBy, sortingOrder, page, pageSize, null, searchHistoryRepository, entityManager);

        return stpMatrixSpecBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalCount);
    }

    public MxStpRightsTypologyEod getStpRightsTypology(LocalDate requestDate, String templateValue) {
        return stpRightsTypologyRepository.findTopByTypologyGroupAndReportDate(templateValue, requestDate);
    }

    public void saveWhereSearchHistory(String whereCondition, String userName, String reportType) {
        //log
        CompletableFuture.runAsync(() -> {
            SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(userName, reportType, whereCondition);
            //llog last
            if (searchHistory == null) {
                SearchHistory history = new SearchHistory();
                history.setUserName(userName);
                history.setReportType(reportType);
                history.setSearchQuery(whereCondition);
                searchHistoryRepository.save(history);
            }
        });
//log
    }

    public String getStpLatestDate() {
        DataImportJob job = dataImportJobRepository.findDistinctTopByFileNameStartsWithAndPurgedAndJobStatusOrderByReportDateDesc("stprights", 'N', MxJobLogType.COMPLETED);
        return job != null ? job.getReportDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : null;
    }

    public File exportAsXlsxFrom(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, DownloadJob job, List<String> fieldColumns) throws Exception {
        fieldColumns.add("id");
        SXSSFWorkbook workbook = new SXSSFWorkbook(Integer.parseInt(Objects.requireNonNull(environment.getProperty("reports.print-rows.threads.count"))));
        SXSSFSheet sheet = workbook.createSheet();
        int rowCount = 0;
        int columnCount = 0;
        SXSSFRow row = sheet.createRow(rowCount++);
        CellStyle defaultCellStyle = sheet.getWorkbook().createCellStyle();
        int headerCount = 0;
        for (FieldMap fieldMap : fieldMaps) {
            if (fieldMap.getDisplayName() != null && fieldMap.getDisplayName().contains(",")) {
                String[] data = fieldMap.getDisplayName().split(",");

                for (int i = 0; i < data.length; i++) {
                    SXSSFCell cell = row.createCell(columnCount++);
                    cell.setCellValue(data[i]);
                }

                headerCount++;
            }
        }
        // write values in xlsx
        int itemCount = 0;
        columnCount = 0;
        for (Map<String, Object> userPolicyInfo : list) {
            for (FieldMap fieldMap : fieldMaps) {
                if (fieldMap.getEntityName() != null) {
                    row = sheet.createRow(rowCount++);
                    SXSSFCell cell = row.createCell(columnCount++);
                    cell.setCellStyle(defaultCellStyle);
                    cell.setCellValue((fieldMap.getDisplayName() != null ? fieldMap.getDisplayName() : blankFields));
                    cell = row.createCell(columnCount++);
                    String fieldName = fieldMap.getEntityName();
                    String fieldValue = "";
                    if (fieldName.equalsIgnoreCase("id")) {
                        fieldValue = String.valueOf(rowCount - 1);
                    } else {
                        fieldValue = userPolicyInfo.get(fieldName) == null ? null : userPolicyInfo.get(fieldName).toString();
                    }
                    cell.setCellValue((fieldValue != null ? fieldValue : blankFields));

                }
                columnCount = 0;
            }
            itemCount++;
        }

        log.info("XLSX Export: Flushing to file.");
        downloadJobService.updateJobProgress("FILE_SAVING", list.size(), itemCount, job.getId());
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (IOException ex) {
            log.error("Excel Export: Failed to flush data to file.", ex);
        }
        // dispose of temporary files backing this workbook on disk
        workbook.dispose();
        return file;
    }

    public File exportAsCsvFrom(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, DownloadJob job, List<String> fieldColumns) throws IOException {
        fieldColumns.add("id");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("Cp1252")))) {
            // write header in csv
            for (FieldMap fieldMap : fieldMaps) {
                if (fieldMap.getDisplayName() != null && fieldMap.getDisplayName().contains(",")) {
                    bw.write(fieldMap.getDisplayName());
                }
            }
            bw.newLine();
            // write values in csv
            int lineCount = 0;
            for (Map<String, Object> userPolicyInfo : list) {
                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldMap.getEntityName() != null) {
                        String fieldValue = "";
                        bw.write((fieldMap.getDisplayName() != null ? fieldMap.getDisplayName() : blankFields));
                        bw.append(",");
                        fieldValue = userPolicyInfo.get(fieldMap.getEntityName()) == null ? null : userPolicyInfo.get(fieldMap.getEntityName()).toString();
                        bw.write((fieldValue != null ? fieldValue : blankFields));
                        bw.newLine();
                    }
                }
            }
            log.info("CSV Export: Updating final status.");
            if (job != null) {
                downloadJobService.updateJobProgress("FILE_WRITE", list.size(), lineCount, job.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getUserPolicy(String policyName, String fileName, String color, List<FieldMap> fieldMaps, DownloadJob job, String outputFormat, List<String> fieldColumns, LocalDate requestDate) throws Exception {
        File file = new File(generateExportPathUsingFileName(fileName + outputFormat));
        file.getParentFile().mkdirs();
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        List<MxUserPolicy> mxUserPolicy = mxUserPolicyRepo.findUserPolicy(policyName, requestDate);
        log.info(" Export:  fetch completed.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", mxUserPolicy.size(), mxUserPolicy.size(), job.getId());

        List<Map<String, Object>> list = prepareMapListToExport(mxUserPolicy);
        if (outputFormat.equalsIgnoreCase(".csv")) {
            log.info(" Export:  csv write started..");
            file = exportAsCsvFrom(list, file, fieldMaps, job, fieldColumns);
            //update download job
            log.info(" Export:  csv write completed..");
            downloadJobService.updateJobProgress("COMPLETE", list.size(), list.size(), job.getId());
            downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
            return CompletableFuture.completedFuture(null);
        } else {
            log.info(" Export:  xlsx write started..");
            file = exportAsXlsxFrom(list, file, fieldMaps, job, fieldColumns);
            //update download job
            log.info(" Export:  xlsx write completed..");
            downloadJobService.updateJobProgress("COMPLETE", list.size(), list.size(), job.getId());
            downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
            return CompletableFuture.completedFuture(null);
        }
    }

    public MxStpRightMatrixEod getStpRightsProfile(LocalDate requestDate, String templateValue) {
        return stpRightsRepository.findTopByRightProfileAndReportDate(templateValue, requestDate);
    }

    @Transactional
    public Document getGroupDetailsFromStpRightsProfile(int page, int pageSize, String sortBy, String sortingOrder, LocalDate requestDate, String templateValue, Sort.Order sort, GeneralSpecification filters, String userName, boolean countFetched, long totalCount) {
        if (filters.getSearchWhereClause() == null || filters.getSearchWhereClause().isBlank()) {
            Document document = new Document();
            Page<GroupDetailsList> stpSrcModule = groupListRepo.getGroupDetailsFromRightsProfile(templateValue, requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            document.put("totalPages", stpSrcModule.getTotalPages());
            document.put("records", stpSrcModule.getTotalElements());
            document.put("content", stpSrcModule.getContent());
            return document;
        } else {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), userName, filters.getTemplate());
                Document document = new Document();
                Page<GroupDetailsList> stpSrcModule = groupListRepo.getGroupDetailsFromRightsProfileWithGlobalSearch(templateValue, requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                document.put("totalPages", stpSrcModule.getTotalPages());
                document.put("records", stpSrcModule.getTotalElements());
                document.put("content", stpSrcModule.getContent());
                return document;
            }
            String template = getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            SearchConfig<GroupDetailsList> stpSrcModuleSpecBuilder = new SearchConfig<>(templateList[2],
                    filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], requestDate.format(dateTimeFormatter), null, templateList[1],
                    templateList[3], sortBy, sortingOrder, page, pageSize, "rightsProfile", searchHistoryRepository, entityManager);

            return stpSrcModuleSpecBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), userName, filters.getTemplate(), countFetched, totalCount);
        }

    }

    @Async
    @Transactional
    public CompletableFuture<Void> getStpRightsProfileExport(String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat,
                                                             LocalDate requestDate, List<String> fieldColumns, Sort.Order sort, String userName,
                                                             StpMatrixExportXlsxRequest filters, String sortingOrder, String sortBy) throws Exception {
        List<GroupDetailsList> stpRightsProfileList = new ArrayList<>();
        // Sort.Order sort = Sort.Order.desc("id");
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document stpRightsProfileDetail = getGroupDetailsFromStpRightsProfile(page, pageSize, sortBy, sortingOrder, requestDate, filters.filters.getTemplateValue(), sort, filters.filters, userName, countFetched, totalCount);

            log.debug("stp Matrix details:{}", stpRightsProfileDetail);

            stpRightsProfileList.addAll((List<GroupDetailsList>) stpRightsProfileDetail.get("content"));
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, stpRightsProfileList.size(), job.getId());
            totalPage = (int) stpRightsProfileDetail.get("totalPages");
            totalCount = (long) stpRightsProfileDetail.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", stpRightsProfileList.size(), (int) totalCount, job.getId());
        return exportFilefromDetails(outputFormat, stpRightsProfileList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    public List<StpSourceModule> getProcessingDetails(String templateValue, LocalDate requestDate) {
        List<Object[]> stpSrcModule = stpSrcModuleRepository.findByReportDateAndGlobalTemplate(templateValue, requestDate.format(dateTimeFormatter));
        List<StpSourceModule> res = stpSrcModule.stream()
                .map(ch -> new StpSourceModule(
                        ch[0] != null ? ch[0].toString() : null,
                        ch[1] != null ? ch[1].toString() : null))
                .collect(Collectors.toList());
        return res;
    }

//    private File exportAsCsvFrom(List<FieldMap> fieldMaps, File file, String policyName, LocalDate requestDate, List<String> fieldColumns, DownloadJob job) {
//        fieldColumns.add("id");
//        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("Cp1252")))) {
//            // write header in csv
//            for (FieldMap fieldMap : fieldMaps) {
//                if (fieldMap.getDisplayName() != null && fieldMap.getDisplayName().contains(",")) {
//                    bw.write(fieldMap.getDisplayName());
//                }
//            }
//            bw.newLine();
//            List<MxUserPolicy> mxUserPolicy = mxUserPolicyRepo.findAllUserPolicy(policyName, requestDate);
//
//            downloadJobService.updateJobProgress("FETCH_ITEMS", mxUserPolicy.size(), mxUserPolicy.size(), job.getId());
//
//            List<Map<String, Object>> list=prepareMapListToExport(mxUserPolicy);
//            // write values in csv
//            int lineCount = 0;
//            for (Map<String, Object> trade : list) {
//                for (FieldMap fieldMap : fieldMaps) {
//                    if (fieldMap.getEntityName() != null) {
//                        String fieldValue = "";
//                        bw.write((fieldMap.getDisplayName() != null ? fieldMap.getDisplayName() : ""));
//                        bw.append(",");
//                        fieldValue = trade.get(fieldMap.getEntityName()) == null ? null : trade.get(fieldMap.getEntityName()).toString();
//                        bw.write((fieldValue != null ? fieldValue : ""));
//                        bw.newLine();
//                    }
//                }
//            }
//            log.info("CSV Export: Updating final status.");
//            if (job != null) {
//                downloadJobService.updateJobProgress("FILE_WRITE", list.size(), lineCount, job.getId());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return file;
//    }
}
