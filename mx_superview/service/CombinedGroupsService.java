package com.finsurge.tmr_portal.mx_superview.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.configs.CombinedGroupSearchConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class CombinedGroupsService {
    //logger
    private static final Logger log = LoggerFactory.getLogger(CombinedGroupsService.class);

    private final DownloadJobService downloadJobService;
    private final DateTimeFormatter dateTimeFormatter;
    private final Environment environment;
    private final MxEnterPriseRiskRepo mxEnterPriseRiskRepo;
    private final MxChineseWallTemplateRepository chineseWallTemplateRepository;
    private final MxConsistencyTemplateRepository consistencyTemplateRepository;
    private final MxOSPRightsTemplateRepository ospRightsTemplateRepository;
    private final MxGroupNavigationRightsRepository groupNavigationRightsRepository;
    private final MxCwtConfigMgtRightRepository cwtConfigMgtRightRepository;
    private final MxOperationRightsRepo operationRightsRepo;
    private final MxFinanceRightsRepo financeRightsRepo;
    private final MxUserGroupAccessRepository mxUserGroupAccessRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final MxUserListRepository userListRepository;

    private final MxPortfolioRightsRepository mxPortfolioRightsRepository;

    private final MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo;
    private final STPRightsRepository stpRightsRepository;

    private final DataImportJobRepository dataImportJobRepository;
    private final ViewerExportService viewerExportService;
    @Autowired
    private EntityManager entityManager;

    @Value("${uam.thread.pool.value}")
    private int threadValue;

    @Value("${uam.blank.fields}")
    private String blankFields;

    public CombinedGroupsService(DataImportJobRepository dataImportJobRepository, DownloadJobService downloadJobService, Environment environment, MxEnterPriseRiskRepo mxEnterPriseRiskRepo, MxChineseWallTemplateRepository chineseWallTemplateRepository, MxConsistencyTemplateRepository consistencyTemplateRepository1, MxOSPRightsTemplateRepository ospRightsTemplateRepository, MxGroupNavigationRightsRepository groupNavigationRightsRepository, MxCwtConfigMgtRightRepository cwtConfigMgtRightRepository, MxOperationRightsRepo operationRightsRepo, MxFinanceRightsRepo financeRightsRepo, MxUserGroupAccessRepository mxUserGroupAccessRepository, SearchHistoryRepository searchHistoryRepository, MxUserListRepository userListRepository, MxPortfolioRightsRepository mxPortfolioRightsRepository, MxGroupCombinedPortfolioRepo mxGroupCombinedPortfolioRepo, STPRightsRepository stpRightsRepository, ViewerExportService viewerExportService) {
        this.downloadJobService = downloadJobService;
        this.mxPortfolioRightsRepository = mxPortfolioRightsRepository;
        this.viewerExportService = viewerExportService;
        dateTimeFormatter = DateTimeFormatter.ofPattern(SnapshotDataService.DATE_FORMAT);
        this.environment = environment;
        this.mxEnterPriseRiskRepo = mxEnterPriseRiskRepo;
        this.chineseWallTemplateRepository = chineseWallTemplateRepository;
        this.consistencyTemplateRepository = consistencyTemplateRepository1;
        this.ospRightsTemplateRepository = ospRightsTemplateRepository;
        this.groupNavigationRightsRepository = groupNavigationRightsRepository;
        this.cwtConfigMgtRightRepository = cwtConfigMgtRightRepository;
        this.operationRightsRepo = operationRightsRepo;
        this.financeRightsRepo = financeRightsRepo;
        this.mxUserGroupAccessRepository = mxUserGroupAccessRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.userListRepository = userListRepository;
        this.mxGroupCombinedPortfolioRepo = mxGroupCombinedPortfolioRepo;
        this.stpRightsRepository = stpRightsRepository;
        this.dataImportJobRepository = dataImportJobRepository;
    }

    //property
//    public String getPropertyFields(String templateName, String subTemplate) {
//        try {
//            String propertyField = "";
//            String dateField = "";
//            String subPropertyFields = "";
//            File resource = new ClassPathResource("ElasticSearch/Generic_Search_Config.json").getFile();
//            if (resource.exists()) {
//                String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
//                JSONParser parser = new JSONParser();
//                JSONObject json = (JSONObject) parser.parse(defaultConfig);
//                dateField = json.get("DATE_FIELD").toString();
//                if (templateName != null && !templateName.isBlank()) {
//                    propertyField = json.get(templateName).toString();
//                    if (subTemplate != null && !subTemplate.isBlank()) {
//                        subPropertyFields = json.get(subTemplate).toString();
//                        return dateField + "|" + propertyField + "|" + subPropertyFields;
//                    }
//                    return dateField + "|" + propertyField;
//                }
//                return dateField;
//            } else {
//                return null;
//            }
//        } catch (IOException | ParseException ex) {
//            log.error("Error reading / parsing ElasticSearch config.", ex);
//            return null;
//        }
//    }

    //export
    public CompletableFuture<Void> exportFilefromDetails(String outputFormat, List<?> exportData, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns) throws Exception {

        //if the output format is .xls,.xlsx,.xlsb
        if (outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb")) {
            File file = new File(generateExportPathUsingFileName(fileName + outputFormat));
            file.getParentFile().mkdirs();
            log.info(outputFormat.toUpperCase() + " Export: Starting write.");
            file = exportAsXlsxFromList(prepareMapListToExport(exportData), file, fieldMaps, color, job, fieldColumns);
            //update download job
            log.info(outputFormat.toUpperCase() + " Export: Write complete.");
            downloadJobService.updateJobProgress("COMPLETE", exportData.size(), exportData.size(), job.getId());
            downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
            return CompletableFuture.completedFuture(null);
        }
        //if the output format is .csv
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

    //csv export
    public File exportAsCsvFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, DownloadJob job, List<String> fieldColumns) {
        fieldColumns.add("id");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
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
                                if (fieldFormat.equalsIgnoreCase("yyyyMMdd")) {
                                    LocalDate localDate = LocalDate.parse(trade.get(fieldName).toString());
                                    fieldValue = localDate.format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else if (fieldFormat.equalsIgnoreCase("yyyyMMdd hh:mm:ss")) {
                                    LocalDateTime localDateTime = LocalDateTime.parse(trade.get(fieldName).toString());
                                    fieldValue = localDateTime.format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else {
                                    LocalTime localTime = LocalTime.parse(trade.get(fieldName).toString());
                                    fieldValue = localTime.format(DateTimeFormatter.ofPattern(fieldFormat));
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

    //xlx export
    public File exportAsXlsxFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns) throws Exception {
        fieldColumns.add("id");

        SXSSFWorkbook workbook = new SXSSFWorkbook(Integer.parseInt(Objects.requireNonNull(environment.getProperty("reports.print-rows.threads.count"))));
        SXSSFSheet sheet = workbook.createSheet();
        int rowCount = 0;
        int columnCount = 0;
        SXSSFRow row = sheet.createRow(rowCount++);
        CellStyle defaultCellStyle = sheet.getWorkbook().createCellStyle();
        int headerCount = 0;
        for (FieldMap fieldMap : fieldMaps) {
            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                SXSSFCell cell = row.createCell(columnCount++);
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
                                if (fieldFormat.equalsIgnoreCase("yyyyMMdd")) {
                                    LocalDate localDate = LocalDate.parse(trade.get(fieldName).toString());
                                    fieldValue = localDate.format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else if (fieldFormat.equalsIgnoreCase("yyyyMMdd hh:mm:ss")) {
                                    LocalDateTime localDateTime = LocalDateTime.parse(trade.get(fieldName).toString());
                                    fieldValue = localDateTime.format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else {
                                    LocalTime localTime = LocalTime.parse(trade.get(fieldName).toString());
                                    fieldValue = localTime.format(DateTimeFormatter.ofPattern(fieldFormat));
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

    //path file
    private String generateExportPathUsingFileName(String fileName) {
        String importProperty = environment.getProperty("uam.paths.export") + File.separator + fileName;
        importProperty = RPControllerUtils.fixSeparatorsForUnix(importProperty);
        return importProperty;
    }

    //portfolio export
    @Async
    @Transactional
    public CompletableFuture<Void> getCombinedPortfolioDetailsAndExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String userName) throws Exception {

        log.info(filters.toString());
        List<MxGroupPortfolioRights> portfolioLabels = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            //to retrive the Data for the given group using report Date and groupLabel
            Document portfolioLabel = getCombinedPortfolioRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, userName, countFetched, totalCount);

            portfolioLabels.addAll((List<MxGroupPortfolioRights>) portfolioLabel.get("content"));
            //update job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portfolioLabels.size(), job.getId());
            totalPage = (int) portfolioLabel.get("totalPages");
            totalCount = (long) portfolioLabel.get("records");
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize job status
        downloadJobService.updateJobProgress("FILE_WRITE", portfolioLabels.size(), (int) totalCount, job.getId());
        //export the portfolio Data
        return exportFilefromDetails(outputFormat, portfolioLabels, fileName, fieldMaps, color, job, fieldColumns);
    }

    //to get a record from the portfolio
    public List<MxGroupPortfolioRights> getPortfolioRightsList(LocalDate requestDate, List<String> groupLabel) {
        return mxPortfolioRightsRepository.findTopByReportDateAndGroupLabelIn(requestDate, groupLabel);
    }

    //display portfolio and search

    @Transactional
    public Document getCombinedPortfolioRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder portfolioRights, Boolean allGroups, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        Page<MxGroupPortfolioRights> portfolioDetails;
        LocalDate requestDate = LocalDate.parse(portfolioRights.getDateValue(), dateTimeFormatter);

        //to search the elements in the portfolio
        if (portfolioRights.getSearchWhereClause() != null && !portfolioRights.getSearchWhereClause().isBlank()) {
            if (portfolioRights.isGlobalSearch()) {
                if (allGroups) {
                    portfolioDetails = mxPortfolioRightsRepository.findByReportDateWithSearchCombinedAllGroups(requestDate, portfolioRights.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    portfolioDetails = mxPortfolioRightsRepository.findByGroupLabelAndReportDateListWithSearchCombined(portfolioRights.getTemplateValue(),
                            requestDate, portfolioRights.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", portfolioDetails.getTotalPages());
                document.put("records", portfolioDetails.getTotalElements());
                document.put("content", portfolioDetails.getContent());
                return document;
            } else {
                String monShortName = requestDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
                String groupPortfolioTableName = "UAM_MX_GROUP_PORTFOLIO_RIGHTS PARTITION (P_" + monShortName + ")~MxGroupPortfolioRights";
                String template = viewerExportService.getPropertyFields(portfolioRights.getTemplate(), portfolioRights.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                CombinedGroupSearchConfig<MxGroupPortfolioRights> mxPortfolioRightsSpecBuilder = new CombinedGroupSearchConfig<>(templateList[2], null, portfolioRights.getGroupValue(),
                        portfolioRights.getTemplateValue(), portfolioRights.getSubTemplate(), portfolioRights.getSubTemplateValue(), templateList[0], portfolioRights.getDateValue(), null, groupPortfolioTableName,
                        null, sortBy, sortingOrder, page, pageSize, null, allGroups, searchHistoryRepository, entityManager);
                return mxPortfolioRightsSpecBuilder.getSearchResultUpdate(portfolioRights.getSearchWhereClause(), username, portfolioRights.getTemplate(), null, countFetched, totalRecords);
            }
        } else {
            //if all the groups are selected
            if (allGroups) {
                //all the group label data for the given report Date in the portfolio Rights is retrived
                portfolioDetails = mxPortfolioRightsRepository.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //the data for specific group label forthe given report Date in the portfolio Rights is retrived
                portfolioDetails = mxPortfolioRightsRepository.findByGroupLabelAndReportDate(portfolioRights.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", portfolioDetails.getTotalPages());
            document.put("records", portfolioDetails.getTotalElements());
            document.put("content", portfolioDetails.getContent());
            return document;
        }
    }


    //combined chinese wall template export
    @Async
    @Transactional
    public CompletableFuture<Void> getChineseWallDetailsAndExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {
        log.info(filters.toString());
        List<CombinedChineseWallTemplate> chineseWallTmpls = new ArrayList<>();
        int pageSize = 1500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            //to retrive the Data for the given group using report Date and groupLabel and template name
            Document mxChineseWallTmpls = getChineseWall(page, pageSize, sortBy, sortingOrder, sort, allGroups, filters, username, countFetched, totalCount);
            chineseWallTmpls.addAll((List<CombinedChineseWallTemplate>) mxChineseWallTmpls.get("content"));
            //to update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallTmpls.size(), job.getId());
            totalPage = (int) mxChineseWallTmpls.get("totalPages");
            totalCount = (long) mxChineseWallTmpls.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //to finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", chineseWallTmpls.size(), (int) totalCount, job.getId());
        //to export the chinese wall template data
        return exportFilefromDetails(outputFormat, chineseWallTmpls, fileName, fieldMaps, color, job, fieldColumns);
    }

    //search and display combined chinese wall template
    @Transactional
    public Document getChineseWall(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, GeneralSpecificationBuilder filters, String username, boolean countFetched, long totalRecords) {

        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);
        Page<CombinedChineseWallTemplate> chineseWallTemplate;
        //Page<MxChineseWallTmpl> chineseWallTemplate;
        //search for combined groups chinese wall template
        if (filters.getSearchWhereClause() != null && !filters.getSearchWhereClause().isBlank()) {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), username, filters.getTemplate());
                Page<CombinedChineseWallTemplate> chineseWallTmpls;
                if (allGroups) {
                    chineseWallTmpls = chineseWallTemplateRepository.findAllByReportDateWithGlobalSearch(requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    chineseWallTmpls = chineseWallTemplateRepository.findByGroupLabelAndReportDateWithGlobalSearch(filters.getTemplateValue(), filters.getGroupValue(),
                            requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", chineseWallTmpls.getTotalPages());
                document.put("records", chineseWallTmpls.getTotalElements());
                document.put("content", chineseWallTmpls.getContent());
                return document;
            } else {
                if (filters.getGroupValue().size() > 8) {
                    document.put("content", Collections.emptyList());
                    document.put("message", "Please select only eight groups for search.");
                    return document;
                }
                String monShortName = requestDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
                String groupChinesewallTableName = "UAM_MX_CHINESE_WALL_TMPL PARTITION (P_" + monShortName + ")~MxChineseWallTmpl";
                String joinTemplate="UAM_MX_GROUP_LIST PARTITION (P_" + monShortName + ")";
                String template = viewerExportService.getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                StringBuilder fieldName = getObject(templateList[1]);
                CombinedGroupSearchConfig<CombinedChineseWallTemplate> mxChineseWallTmplSpecificationBuilder = new CombinedGroupSearchConfig<>(templateList[2], templateList[4], filters.getGroupValue(),
                        filters.getTemplateValue(), filters.getSubTemplate(), filters.getSubTemplateValue(), templateList[0], filters.getDateValue(), null, groupChinesewallTableName,
                        joinTemplate, sortBy, sortingOrder, page, pageSize, "chineseWall", allGroups, searchHistoryRepository, entityManager);
                return mxChineseWallTmplSpecificationBuilder.getSearchResultUpdate(filters.getSearchWhereClause(), username, filters.getTemplate(), fieldName, countFetched, totalRecords);
            }
        } else {
            if (allGroups) {
                //to get all records for the report date
                chineseWallTemplate = chineseWallTemplateRepository.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected template and group Label for the report
                chineseWallTemplate = chineseWallTemplateRepository.findByGroupLabelAndReportDate(filters.getTemplateValue(), filters.getGroupValue(),
                        requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", chineseWallTemplate.getTotalPages());
            document.put("records", chineseWallTemplate.getTotalElements());
            document.put("content", chineseWallTemplate.getContent());
            return document;
        }
    }

    //combined chinesewall count
    public List<MxChineseWallTmpl> getChineseWallList(LocalDate requestDate, List<String> templateValue) {
        return chineseWallTemplateRepository.findTopByReportDateAndTemplateLabelIn(requestDate, templateValue);
    }

    //Combined Consistency Export
    @Async
    @Transactional
    public CompletableFuture<Void> getConsistencyExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, List<Sort.Order> sort, Boolean allGroups, String username) throws Exception {
        log.info(filters.toString());
        List<CombinedConsistencyTemplate> consistencyTmplList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            //to retrive the Data for the given group using report Date and groupLabel and template name
            Document consistencyTmplsRights = getConsistency(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched, totalCount);

            consistencyTmplList.addAll((List<CombinedConsistencyTemplate>) consistencyTmplsRights.get("content"));
            //update job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, consistencyTmplList.size(), job.getId());
            totalPage = (int) consistencyTmplsRights.get("totalPages");
            totalCount = (long) consistencyTmplsRights.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize job status
        downloadJobService.updateJobProgress("FILE_WRITE", consistencyTmplList.size(), (int) totalCount, job.getId());
        //export the consistency Data
        return exportFilefromDetails(outputFormat, consistencyTmplList, fileName, fieldMaps, color, job, fieldColumns);
    }

    //combined consistency display and search
    @Transactional
    public Document getConsistency(int page, int pageSize, String sortBy, String sortingOrder, List<Sort.Order> sort, GeneralSpecificationBuilder consistencyFilter, Boolean allGroups, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(consistencyFilter.getDateValue(), dateTimeFormatter);
        Page<CombinedConsistencyTemplate> consistencyTemplateDetails;

        //search for combined groups consistency template
        if (consistencyFilter.getSearchWhereClause() != null && !consistencyFilter.getSearchWhereClause().isBlank()) {
            if (consistencyFilter.isGlobalSearch()) {
                saveWhereSearchHistory(consistencyFilter.getSearchWhereClause(), username, consistencyFilter.getTemplate());
                if (allGroups) {
                    consistencyTemplateDetails = consistencyTemplateRepository.findConsistencyTmplGlobalCombinedAllGroups(requestDate, consistencyFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    consistencyTemplateDetails = consistencyTemplateRepository.findConsistencyTmplGlobalCombined(consistencyFilter.getTemplateValue(), consistencyFilter.getGroupValue(),
                            requestDate, consistencyFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", consistencyTemplateDetails.getTotalPages());
                document.put("records", consistencyTemplateDetails.getTotalElements());
                document.put("content", consistencyTemplateDetails.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(consistencyFilter.getTemplate(), consistencyFilter.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                StringBuilder fieldName = getObject(templateList[1]);
                CombinedGroupSearchConfig<CombinedConsistencyTemplate> mxConsistencyTmplSpecificationBuilder = new CombinedGroupSearchConfig<>(templateList[2], templateList[4], consistencyFilter.getGroupValue(),
                        consistencyFilter.getTemplateValue(), consistencyFilter.getSubTemplate(), consistencyFilter.getSubTemplateValue(), templateList[0], consistencyFilter.getDateValue(), null, templateList[1],
                        templateList[3], sortBy, sortingOrder, page, pageSize, "consistencyTmpl", allGroups, searchHistoryRepository, entityManager);
                return mxConsistencyTmplSpecificationBuilder.getSearchResultUpdate(consistencyFilter.getSearchWhereClause(), username, consistencyFilter.getTemplate(), fieldName, countFetched, totalRecords);
            }
        } else {
            //if all the groups are selected
            if (allGroups) {
                //to get all records for the report Date
                consistencyTemplateDetails = consistencyTemplateRepository.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records with specific template and group Label
                consistencyTemplateDetails = consistencyTemplateRepository.findByGroupLabelAndReportDate(consistencyFilter.getTemplateValue(), consistencyFilter.getGroupValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", consistencyTemplateDetails.getTotalPages());
            document.put("records", consistencyTemplateDetails.getTotalElements());
            document.put("content", consistencyTemplateDetails.getContent());
            return document;
        }
    }

    // combined consistency count
    public List<MxConsistencyTmpl> getConsistencyCount(LocalDate requestDate, List<String> consistencyExportXlsxRequest) {
        return consistencyTemplateRepository.findTopByConsistencyTmplInAndReportDate(consistencyExportXlsxRequest, requestDate);
    }

    //combined Group Navigation Export
    @Async
    @Transactional
    public CompletableFuture<Void> getNavigationRightsAndExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {

        log.info(filters.toString());
        List<MxGroupNavigationRight> groupNavigationRights = new ArrayList<>();
        Document navigationTmpls = new Document();
        int pageSize = 1500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            navigationTmpls = getNavigationRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched, totalCount);
            groupNavigationRights.addAll((List<MxGroupNavigationRight>) navigationTmpls.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupNavigationRights.size(), job.getId());
            totalPage = (int) navigationTmpls.get("totalPages");
            totalCount = (long) navigationTmpls.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", groupNavigationRights.size(), (int) totalCount, job.getId());
        //to export the group navigation template data
        return exportFilefromDetails(outputFormat, groupNavigationRights, fileName, fieldMaps, color, job, fieldColumns);
    }


    //combined group Navigation search and Display
    @Transactional
    public Document getNavigationRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder navigationRightsFilter, Boolean allGroups, String username, boolean countFetched, long totalRecords) {

        LocalDate requestDate = LocalDate.parse(navigationRightsFilter.getDateValue(), dateTimeFormatter);
        Document document = new Document();
        Page<MxGroupNavigationRight> navigationRights;

        //search for combined groups navigation template
        if (navigationRightsFilter.getSearchWhereClause() != null && !navigationRightsFilter.getSearchWhereClause().isBlank()) {
            if (navigationRightsFilter.isGlobalSearch()) {
                saveWhereSearchHistory(navigationRightsFilter.getSearchWhereClause(), username, navigationRightsFilter.getTemplate());
                if (allGroups) {
                    navigationRights = groupNavigationRightsRepository.findNavigationRightsGlobalCombinedAllGroups(requestDate, navigationRightsFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    navigationRights = groupNavigationRightsRepository.findNavigationRightsGlobalCombined(navigationRightsFilter.getTemplateValue(), navigationRightsFilter.getGroupValue(),
                            requestDate, navigationRightsFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", navigationRights.getTotalPages());
                document.put("records", navigationRights.getTotalElements());
                document.put("content", navigationRights.getContent());
                return document;
            } else {
                String monShortName = requestDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
                String groupNavigationTableName = "UAM_MX_GROUP_NAV_RIGHTS PARTITION (P_" + monShortName + ")~MxGroupNavigationRight";
                String template = viewerExportService.getPropertyFields(navigationRightsFilter.getTemplate(), navigationRightsFilter.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                CombinedGroupSearchConfig<MxGroupNavigationRight> mxNavigationRightsSpecBuilder = new CombinedGroupSearchConfig<>(templateList[2], null, navigationRightsFilter.getGroupValue(),
                        navigationRightsFilter.getTemplateValue(), navigationRightsFilter.getSubTemplate(), navigationRightsFilter.getSubTemplateValue(), templateList[0], navigationRightsFilter.getDateValue(), null, groupNavigationTableName,
                        null, sortBy, sortingOrder, page, pageSize, null, allGroups, searchHistoryRepository, entityManager);
                return mxNavigationRightsSpecBuilder.getSearchResultUpdate(navigationRightsFilter.getSearchWhereClause(), username, navigationRightsFilter.getTemplate(), null, countFetched, totalRecords);
            }
        } else {
            if (allGroups) {
                //to get all records for the report date
                navigationRights = groupNavigationRightsRepository.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected template and group Label for the report
                navigationRights = groupNavigationRightsRepository.findByGroupLabelAndReportDate(navigationRightsFilter.getTemplateValue(), navigationRightsFilter.getGroupValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", navigationRights.getTotalPages());
            document.put("records", navigationRights.getTotalElements());
            document.put("content", navigationRights.getContent());
            return document;
        }
    }

    //count the navigation
    public List<MxGroupNavigationRight> getNavigationList(LocalDate requestDate, List<String> navigationRights) {
        return groupNavigationRightsRepository.findTopByTemplateInAndReportDate(navigationRights, requestDate);
    }

    //combined operation rights Export
    @Async
    @Transactional
    public CompletableFuture<Void> getOperationalExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {
        log.info(filters.toString());
        List<CombinedOperationRightsTemplate> operationArrayList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        //initialize the job status
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document operationRight = getOperationalRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched, totalCount);
            operationArrayList.addAll((List<CombinedOperationRightsTemplate>) operationRight.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, operationArrayList.size(), job.getId());
            totalPage = (int) operationRight.get("totalPages");
            totalCount = (long) operationRight.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", (int) totalCount, operationArrayList.size(), job.getId());
        //to export the operation rights data
        return exportFilefromDetails(outputFormat, operationArrayList, fileName, fieldMaps, color, job, fieldColumns);
    }

    //Combined Operation rights Data retrival
    @Transactional
    public Document getOperationalRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder filters, Boolean allGroups, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        Page<CombinedOperationRightsTemplate> operationDetails;
        LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);

        //search for combined groups operation rights
        if (filters.getSearchWhereClause() != null && !filters.getSearchWhereClause().isBlank()) {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), username, filters.getTemplate() + filters.getSubTemplateValue().toUpperCase());
                if (allGroups) {
                    if (filters.getSubTemplateValue().equalsIgnoreCase("LPOS")) {
                        operationDetails = operationRightsRepo.findOperationRightsTmplTypeLposGlobalCombinedAllGroups(requestDate, filters.getSubTemplateValue(), filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    } else {
                        operationDetails = operationRightsRepo.findOperationRightsTmplTypeNkeyGlobalCombinedAllGroups(requestDate, filters.getSubTemplateValue(), filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    }
                } else {
                    if (filters.getSubTemplateValue().equalsIgnoreCase("LPOS")) {
                        operationDetails = operationRightsRepo.findOperationRightsTmplTypeLposGlobalCombined(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(),
                                requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    } else {
                        operationDetails = operationRightsRepo.findOperationRightsTmplTypeNkeyGlobalCombined(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(),
                                requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    }
                }
                document.put("totalPages", operationDetails.getTotalPages());
                document.put("records", operationDetails.getTotalElements());
                document.put("content", operationDetails.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                StringBuilder fieldName = getObject(templateList[1]);
                CombinedGroupSearchConfig<CombinedOperationRightsTemplate> operation = new CombinedGroupSearchConfig<>(templateList[2], templateList[4], filters.getGroupValue(),
                        filters.getTemplateValue(), templateList[5], filters.getSubTemplateValue(), templateList[0], filters.getDateValue(), null, templateList[1],
                        templateList[3], sortBy, sortingOrder, page, pageSize, "operation", allGroups, searchHistoryRepository, entityManager);
                return operation.getSearchResultUpdate(filters.getSearchWhereClause(), username, filters.getTemplate(), fieldName, countFetched, totalRecords);
            }
        } else {
            //to get all records for the report date
            if (allGroups) {
                //to get all records for the report date in lpos
                if (filters.getSubTemplateValue().equalsIgnoreCase("LPOS")) {
                    operationDetails = operationRightsRepo.findAllByReportDateLpos(requestDate, filters.getSubTemplateValue(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    //to get all records for the report date in nkey
                    operationDetails = operationRightsRepo.findAllByReportDateNkey(requestDate, filters.getSubTemplateValue(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }

            }
            //to get records for selected template and group Label for the report in lpos
            else if (filters.getSubTemplateValue().equalsIgnoreCase("LPOS")) {
                operationDetails = operationRightsRepo.findByGoupLabelAndReportDateAndLpos(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected template and group Label for the report in nkey
                operationDetails = operationRightsRepo.findByGoupLabelAndReportDateAndNkey(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", operationDetails.getTotalPages());
            document.put("records", operationDetails.getTotalElements());
            document.put("content", operationDetails.getContent());
            return document;

        }
    }

    //Combined operation Rights Count
    public List<MxOperationRights> getOperationRightsList(LocalDate requestDate, List<String> operationalRights) {
        return operationRightsRepo.findTopByTemplateInAndReportDate(operationalRights, requestDate);
    }

    //Combined Configuration Management Count
    public List<MxCwtConfigMgtRight> getConfigurationManagementList(LocalDate requestDate, List<String> templateValue) {
        return cwtConfigMgtRightRepository.findTopByReportDateAndGroupLabelIn(requestDate, templateValue);
    }

    //combined Configuration Management data retrival
    @Transactional
    public Document getCombinedConfigurationManagement(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder cwtConfigMgtFilter, Boolean allGroups, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(cwtConfigMgtFilter.getDateValue(), dateTimeFormatter);
        Page<MxCwtConfigMgtRight> mxCwtConfigMgtRights;

        //search for combined groups configuration management rights
        if (cwtConfigMgtFilter.getSearchWhereClause() != null && !cwtConfigMgtFilter.getSearchWhereClause().isBlank()) {
            if (cwtConfigMgtFilter.isGlobalSearch()) {
                saveWhereSearchHistory(cwtConfigMgtFilter.getSearchWhereClause(), username, cwtConfigMgtFilter.getTemplate());
                if (allGroups) {
                    mxCwtConfigMgtRights = cwtConfigMgtRightRepository.findConfigMgtGlobalCombinedAllGroups(requestDate, cwtConfigMgtFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    mxCwtConfigMgtRights = cwtConfigMgtRightRepository.findConfigMgtGlobalCombined(cwtConfigMgtFilter.getTemplateValue(),
                            requestDate, cwtConfigMgtFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", mxCwtConfigMgtRights.getTotalPages());
                document.put("records", mxCwtConfigMgtRights.getTotalElements());
                document.put("content", mxCwtConfigMgtRights.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(cwtConfigMgtFilter.getTemplate(), cwtConfigMgtFilter.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                CombinedGroupSearchConfig<MxCwtConfigMgtRight> mxConfigurationSpecBuilder = new CombinedGroupSearchConfig<>(templateList[2], null, cwtConfigMgtFilter.getGroupValue(),
                        cwtConfigMgtFilter.getTemplateValue(), cwtConfigMgtFilter.getSubTemplate(), cwtConfigMgtFilter.getSubTemplateValue(), templateList[0], cwtConfigMgtFilter.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, allGroups, searchHistoryRepository, entityManager);
                return mxConfigurationSpecBuilder.getSearchResultUpdate(cwtConfigMgtFilter.getSearchWhereClause(), username, cwtConfigMgtFilter.getTemplate(), null, countFetched, totalRecords);
            }
        } else {
            //to get all records for the report date
            if (allGroups) {
                mxCwtConfigMgtRights = cwtConfigMgtRightRepository.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected group Label for the report date
                mxCwtConfigMgtRights = cwtConfigMgtRightRepository.findByGroupLabelAndReportDate(cwtConfigMgtFilter.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", mxCwtConfigMgtRights.getTotalPages());
            document.put("records", mxCwtConfigMgtRights.getTotalElements());
            document.put("content", mxCwtConfigMgtRights.getContent());
            return document;
        }
    }

    //combined groups configuration management Export
    @Async
    @Transactional
    public CompletableFuture<Void> getCwtConfigMgtRightsAndExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {
        log.info(filters.toString());
        List<MxCwtConfigMgtRight> cwtConfigMgtRights = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document cwtConfigMgtRight = getCombinedConfigurationManagement(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched, totalCount);
            cwtConfigMgtRights.addAll((List<MxCwtConfigMgtRight>) cwtConfigMgtRight.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, cwtConfigMgtRights.size(), job.getId());
            totalPage = (int) cwtConfigMgtRight.get("totalPages");
            totalCount = (long) cwtConfigMgtRight.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", cwtConfigMgtRights.size(), (int) totalCount, job.getId());
        //to export the operation rights data
        return exportFilefromDetails(outputFormat, cwtConfigMgtRights, fileName, fieldMaps, color, job, fieldColumns);
    }

    //combined osp rights count
    public List<MxOspRightsMatrix> getOspRightsList(LocalDate requestDate, List<String> templateValue) {
        return ospRightsTemplateRepository.findTopByReportDateAndOspRightTemplateIn(requestDate, templateValue);
    }

    //combined osp Rights export
    @Async
    @Transactional
    public CompletableFuture<Void> getOspRightsDetailsAndExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String name) throws Exception {
        log.info(filters.toString());
        List<MxOspRightsMatrix> ospRightsMatrices = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document ospRightsMatrix = getOspMatrix(page, pageSize, sortBy, sortingOrder, filters, allGroups, sort, name, countFetched, totalCount);
            ospRightsMatrices.addAll((List<MxOspRightsMatrix>) ospRightsMatrix.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, ospRightsMatrices.size(), job.getId());
            totalPage = (int) ospRightsMatrix.get("totalPages");
            totalCount = (long) ospRightsMatrix.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", ospRightsMatrices.size(), (int) totalCount, job.getId());
        //to export the combined groups osp Rights data
        return exportFilefromDetails(outputFormat, ospRightsMatrices, fileName, fieldMaps, color, job, fieldColumns);
    }

    //combined osp rights data retrival
    @Transactional
    public Document getOspMatrix(int page, int pageSize, String sortBy, String sortingOrder, GeneralSpecificationBuilder ospRightsMatrixFilter, Boolean allGroups, Sort.Order sort, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(ospRightsMatrixFilter.getDateValue(), dateTimeFormatter);
        Page<CombinedOspRightsTemplate> ospRights;

        //search for combined groups osp rights matrix
        if (ospRightsMatrixFilter.getSearchWhereClause() != null && !ospRightsMatrixFilter.getSearchWhereClause().isBlank()) {
            if (ospRightsMatrixFilter.isGlobalSearch()) {
                saveWhereSearchHistory(ospRightsMatrixFilter.getSearchWhereClause(), username, ospRightsMatrixFilter.getTemplate());
                if (allGroups) {
                    ospRights = ospRightsTemplateRepository.findOspRightsGlobalCombinedAllGroups(requestDate, ospRightsMatrixFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    ospRights = ospRightsTemplateRepository.findOspRightsGlobalCombined(ospRightsMatrixFilter.getTemplateValue(), ospRightsMatrixFilter.getGroupValue(),
                            requestDate, ospRightsMatrixFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", ospRights.getTotalPages());
                document.put("records", ospRights.getTotalElements());
                document.put("content", ospRights.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(ospRightsMatrixFilter.getTemplate(), ospRightsMatrixFilter.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                StringBuilder fieldName = getObject(templateList[1]);
                CombinedGroupSearchConfig<CombinedOspRightsTemplate> ospRightsSpecification = new CombinedGroupSearchConfig<>(templateList[2], templateList[4], ospRightsMatrixFilter.getGroupValue(),
                        ospRightsMatrixFilter.getTemplateValue(), ospRightsMatrixFilter.getSubTemplate(), ospRightsMatrixFilter.getSubTemplateValue(), templateList[0], ospRightsMatrixFilter.getDateValue(), null, templateList[1],
                        templateList[3], sortBy, sortingOrder, page, pageSize, "ospRight", allGroups, searchHistoryRepository, entityManager);
                return ospRightsSpecification.getSearchResultUpdate(ospRightsMatrixFilter.getSearchWhereClause(), username, ospRightsMatrixFilter.getTemplate(), fieldName, countFetched, totalRecords);
            }
        } else {
            //to get all records for the report date
            if (allGroups) {
                ospRights = ospRightsTemplateRepository.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected group Label for the report date
                ospRights = ospRightsTemplateRepository.findByGroupLabelAndReportDate(ospRightsMatrixFilter.getTemplateValue(), ospRightsMatrixFilter.getGroupValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", ospRights.getTotalPages());
            document.put("records", ospRights.getTotalElements());
            document.put("content", ospRights.getContent());
            return document;
        }
    }

//    public Document getOspMatrixExecuter(int page, int pageSize, String sortBy, String sortingOrder, GeneralSpecificationBuilder ospRightsMatrix, Boolean allGroups, Sort.Order sort, String name) throws ExecutionException, InterruptedException {
//        Document document = new Document();
//        LocalDate requestDate = LocalDate.parse(ospRightsMatrix.getDateValue(), dateTimeFormatter);
//        List<CompletableFuture<Page<CombinedOspRightsTemplate>>> future= new ArrayList<>();
//        CompletableFuture<Page<CombinedOspRightsTemplate>> future1=new CompletableFuture();
//        Page<CombinedOspRightsTemplate> futureCombineGroup = null;
//
//        List<CombinedOspRightsTemplate> allGroupList= new ArrayList<>();
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadValue);
//
//        //search for combined groups osp rights matrix
//        if (ospRightsMatrix.getSearchWhereClause() != null && !ospRightsMatrix.getSearchWhereClause().isBlank()) {
//            String template = getPropertyFields(ospRightsMatrix.getTemplate(), ospRightsMatrix.getSubTemplate());
//            String[] templateList = template.split("\\|");
//            StringBuilder fieldName=getObject(templateList[1]);
//
//            CombinedGroupSearchConfig<CombinedOspRightsTemplate> ospRightsSpecification = new CombinedGroupSearchConfig<>(templateList[2], templateList[4], ospRightsMatrix.getGroupValue(),
//                    ospRightsMatrix.getTemplateValue(), ospRightsMatrix.getSubTemplate(), ospRightsMatrix.getSubTemplateValue(), templateList[0], ospRightsMatrix.getDateValue(), null, templateList[1],
//                    templateList[3], sortBy, sortingOrder, page, pageSize, "ospRight", allGroups, searchHistoryRepository, entityManager);
//
//            return ospRightsSpecification.getSearchResultUpdate(ospRightsMatrix.getSearchWhereClause(), name, ospRightsMatrix.getTemplate(),fieldName);
//        }  else {
//
//            //to get all records for the report date
//            if (allGroups) {
//
//                int pager=page*5;
//                int temp=pager+5;
//                int t=10;
//                log.info("localDate future started:{}",LocalDateTime.now());
//                for(int i=pager;i<temp;i++) {
//                    int finalI = i;
//                    future1= CompletableFuture.supplyAsync(() -> {
//                        log.info("thread:{}", Thread.currentThread().getName());
//                        return ospRightsTemplateRepository.findAllByReportDate(requestDate, PageRequest.of(finalI, t, Sort.by(sort)));
//                    }, executorService);
//                    future.add(future1);
//                }
//                for(CompletableFuture<Page<CombinedOspRightsTemplate>> futures: future) {
//                    futureCombineGroup = futures.get();
//                    allGroupList.addAll(futureCombineGroup.getContent());
//                }
//                log.info("localDate future ended:{}",LocalDateTime.now());
//
//            }
//            // to get records for selected group Label for the report date
//            else {
//                int pager=page*5;
//                int temp=pager+5;
//                int t=10;
//                log.info("localDate future started:{}",LocalDateTime.now());
//                for(int i=pager;i<temp;i++) {
//                    int finalI = i;
//                    future1= CompletableFuture.supplyAsync(() -> {
//                        log.info("thread:{}", Thread.currentThread().getName());
//                        return ospRightsTemplateRepository.findByGroupLabelAndReportDate(ospRightsMatrix.getTemplateValue(), ospRightsMatrix.getGroupValue(), requestDate, PageRequest.of(finalI, t, Sort.by(sort)));
//                    }, executorService);
//                    future.add(future1);
//                }
//
//                for(CompletableFuture<Page<CombinedOspRightsTemplate>> futures: future) {
//                    futureCombineGroup = futures.get();
//                    allGroupList.addAll(futureCombineGroup.getContent());
//                }
//                log.info("localDate future ended:{}",LocalDateTime.now());
//            }
//            log.info("allGroupList:{}",allGroupList.size());
//            document.put("totalPages", (long) Math.ceil((float) futureCombineGroup.getTotalElements() / (float) pageSize));
//            document.put("records", futureCombineGroup.getTotalElements());
//            document.put("content", allGroupList);
//            return document;
//        }
//    }

    //Combined finance Rights Count
    public List<MxFinaceAcctrlRights> getFinanceRightsList(LocalDate requestDate, List<String> templateValue) {
        return financeRightsRepo.findTopByTemplateInAndReportDate(templateValue, requestDate);
    }

    //combined finance rights Export
    @Async
    @Transactional
    public CompletableFuture<Void> getFinanceExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {
        log.info(filters.toString());
        List<CombinedFinanceRightsTemplate> financeRightsArrayList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document financeRight = getFinanceRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched, totalCount);
            financeRightsArrayList.addAll((List<CombinedFinanceRightsTemplate>) financeRight.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, financeRightsArrayList.size(), job.getId());
            totalPage = (int) financeRight.get("totalPages");
            totalCount = (long) financeRight.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", (int) totalCount, financeRightsArrayList.size(), job.getId());
        //to export the combined groups finance rights data
        return exportFilefromDetails(outputFormat, financeRightsArrayList, fileName, fieldMaps, color, job, fieldColumns);
    }

    //Combined finance rights Data retrival
    @Transactional
    public Document getFinanceRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder filters, Boolean allGroups, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        Page<CombinedFinanceRightsTemplate> financeDetails;
        LocalDate requestDate = LocalDate.parse(filters.getDateValue(), dateTimeFormatter);

        //search for combined groups finance rights
        if (filters.getSearchWhereClause() != null && !filters.getSearchWhereClause().isBlank()) {
            if (filters.isGlobalSearch()) {
                saveWhereSearchHistory(filters.getSearchWhereClause(), username, filters.getTemplate() + filters.getSubTemplateValue().toUpperCase());
                if (allGroups) {
                    if (filters.getSubTemplateValue().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                        financeDetails = financeRightsRepo.findFinanceRightsTmplTypeAccCtrlGlobalCombinedAllGroups(requestDate, filters.getSubTemplateValue(), filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    } else {
                        financeDetails = financeRightsRepo.findFinanceRightsTmplTypeStatTmplGlobalCombinedAllGroups(requestDate, filters.getSubTemplateValue(), filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    }
                } else {
                    if (filters.getSubTemplateValue().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                        financeDetails = financeRightsRepo.findFinanceRightsTmplTypeAccCtrlGlobalCombined(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(),
                                requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    } else {
                        financeDetails = financeRightsRepo.findFinanceRightsTmplTypeStatTmplGlobalCombined(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(),
                                requestDate, filters.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                    }
                }
                document.put("totalPages", financeDetails.getTotalPages());
                document.put("records", financeDetails.getTotalElements());
                document.put("content", financeDetails.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(filters.getTemplate(), filters.getSubTemplate(), false);
                log.info("temp:{}", template);
                String[] templateList = template.split("\\|");
                StringBuilder fieldName = getObject(templateList[1]);
                CombinedGroupSearchConfig<CombinedFinanceRightsTemplate> financeSpecification = new CombinedGroupSearchConfig<>(templateList[2], templateList[4], filters.getGroupValue(),
                        filters.getTemplateValue(), templateList[5], filters.getSubTemplateValue(), templateList[0], filters.getDateValue(), null, templateList[1],
                        templateList[3], sortBy, sortingOrder, page, pageSize, "financeRights", allGroups, searchHistoryRepository, entityManager);
                return financeSpecification.getSearchResultUpdate(filters.getSearchWhereClause(), username, filters.getTemplate(), fieldName, countFetched, totalRecords);
            }
        } else {
            //to get all records for the report date
            if (allGroups) {
                //to get all records for the report date in ACC_CTRL_TEMP
                if (filters.getSubTemplateValue().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                    financeDetails = financeRightsRepo.findAllByReportDateAndTmplTypeAccCtrl(requestDate, filters.getSubTemplateValue(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    //to get all records for the report date in STAT_CATEG_TEMP
                    financeDetails = financeRightsRepo.findAllByReportDateAndTmplTypeStatTmpl(requestDate, filters.getSubTemplateValue(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
            }
            //to get records for selected group Label for the report date in ACC_CTRL_TEMP
            else if (filters.getSubTemplateValue().equalsIgnoreCase("ACC_CTRL_TEMP")) {
                log.info("asdfgh");
                financeDetails = financeRightsRepo.findByGoupLabelAndReportDateAndAccCtrl(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected group Label for the report date in STAT_CATEG_TEMP
                financeDetails = financeRightsRepo.findByGoupLabelAndReportDateAndStatTmpl(filters.getTemplateValue(), filters.getGroupValue(), filters.getSubTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", financeDetails.getTotalPages());
            document.put("records", financeDetails.getTotalElements());
            document.put("content", financeDetails.getContent());
            return document;

        }
    }

    //find enterprise count
    public List<MxEnterpriseRisk> getEnterpriseList(LocalDate requestDate, List<String> groupLabel) {
        return mxEnterPriseRiskRepo.findTopByReportDateAndLabelIn(requestDate, groupLabel);
    }

    @Transactional
    public Document getCombinedEnterprise(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder enterpriseRiskFilter, Boolean allGroups, String username,boolean countFetched,long totalRecords) {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(enterpriseRiskFilter.getDateValue(), dateTimeFormatter);
        Page<MxEnterpriseRisk> enterpriseDetails;

        //search for combined groups enterprise risk
        if (enterpriseRiskFilter.getSearchWhereClause() != null && !enterpriseRiskFilter.getSearchWhereClause().isBlank()) {
            if (enterpriseRiskFilter.isGlobalSearch()) {
                saveWhereSearchHistory(enterpriseRiskFilter.getSearchWhereClause(), username, enterpriseRiskFilter.getTemplate());
                if (allGroups) {
                    enterpriseDetails = mxEnterPriseRiskRepo.findEnterpriseRiskGlobalCombinedAllGroups(requestDate, enterpriseRiskFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    enterpriseDetails = mxEnterPriseRiskRepo.findEnterpriseRiskGlobalCombined(enterpriseRiskFilter.getTemplateValue(),
                            requestDate, enterpriseRiskFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", enterpriseDetails.getTotalPages());
                document.put("records", enterpriseDetails.getTotalElements());
                document.put("content", enterpriseDetails.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(enterpriseRiskFilter.getTemplate(), enterpriseRiskFilter.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                log.info("templateList:{}", template);
                CombinedGroupSearchConfig<MxEnterpriseRisk> mxEnterpriseSpecBuilder = new CombinedGroupSearchConfig<>(templateList[2], null, enterpriseRiskFilter.getGroupValue(),
                        enterpriseRiskFilter.getTemplateValue(), enterpriseRiskFilter.getSubTemplate(), enterpriseRiskFilter.getSubTemplateValue(), templateList[0], enterpriseRiskFilter.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, allGroups, searchHistoryRepository, entityManager);
                return mxEnterpriseSpecBuilder.getSearchResultUpdate(enterpriseRiskFilter.getSearchWhereClause(), username, enterpriseRiskFilter.getTemplate(), null, countFetched, totalRecords);
            }
        } else {
            //to get all records for the report date
            if (allGroups) {
                enterpriseDetails = mxEnterPriseRiskRepo.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected template and group Label for the report
                enterpriseDetails = mxEnterPriseRiskRepo.findByGroupLabelAndReportDate(enterpriseRiskFilter.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", enterpriseDetails.getTotalPages());
            document.put("records", enterpriseDetails.getTotalElements());
            document.put("content", enterpriseDetails.getContent());
            return document;
        }
    }

    //combined enterprise export
    @Async
    @Transactional
    public CompletableFuture<Void> getCombinedEnterpriseDetailsAndExport(GeneralSpecificationBuilder filters, String enterpriseRisk, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {
        log.info(filters.toString());
        List<MxEnterpriseRisk> enterpriseLabels = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document enterpriseLabel = getCombinedEnterprise(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched,totalCount);
            enterpriseLabels.addAll((List<MxEnterpriseRisk>) enterpriseLabel.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, enterpriseLabel.size(), job.getId());
            totalPage = (int) enterpriseLabel.get("totalPages");
            totalCount = (long) enterpriseLabel.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", enterpriseLabels.size(), (int) totalCount, job.getId());
        //to export the enterprise data
        return exportFilefromDetails(outputFormat, enterpriseLabels, enterpriseRisk, fieldMaps, color, job, fieldColumns);
    }

    //combined groupcombined portfolio count
    public List<MxGroupCombinedPortfolio> getGroupCombinedPortfolioList(LocalDate requestDate, List<String> groupCompPortfolio) {
        return mxGroupCombinedPortfolioRepo.findTopByUsrGroupInAndReportDate(groupCompPortfolio, requestDate);
    }

    //combined groupcombined portfolio Data Retrival
    @Transactional
    public Document getGrpCompPortfolio(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder groupCombPortfolioFilter, Boolean allGroups, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(groupCombPortfolioFilter.getDateValue(), dateTimeFormatter);
        Page<MxGroupCombinedPortfolio> combinedPortfolioDetails;

        //search for combined groups group combined portfolio
        if (groupCombPortfolioFilter.getSearchWhereClause() != null && !groupCombPortfolioFilter.getSearchWhereClause().isBlank()) {
            if (groupCombPortfolioFilter.isGlobalSearch()) {
                saveWhereSearchHistory(groupCombPortfolioFilter.getSearchWhereClause(), username, groupCombPortfolioFilter.getTemplate());
                if (allGroups) {
                    combinedPortfolioDetails = mxGroupCombinedPortfolioRepo.findCombinedPortfolioGlobalCombinedAllGroups(requestDate, groupCombPortfolioFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    combinedPortfolioDetails = mxGroupCombinedPortfolioRepo.findByGroupLabelAndReportDateListwithSearch(groupCombPortfolioFilter.getTemplateValue(),
                            requestDate, groupCombPortfolioFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", combinedPortfolioDetails.getTotalPages());
                document.put("records", combinedPortfolioDetails.getTotalElements());
                document.put("content", combinedPortfolioDetails.getContent());
                return document;
            } else {
                String template = viewerExportService.getPropertyFields(groupCombPortfolioFilter.getTemplate(), groupCombPortfolioFilter.getSubTemplate(), false);
                String[] templateList = template.split("\\|");
                CombinedGroupSearchConfig<MxGroupCombinedPortfolio> mxGroupPortfolioSpecBuilder = new CombinedGroupSearchConfig<>(templateList[2], null, groupCombPortfolioFilter.getGroupValue(),
                        groupCombPortfolioFilter.getTemplateValue(), groupCombPortfolioFilter.getSubTemplate(), groupCombPortfolioFilter.getSubTemplateValue(), templateList[0], groupCombPortfolioFilter.getDateValue(), null, templateList[1],
                        null, sortBy, sortingOrder, page, pageSize, null, allGroups, searchHistoryRepository, entityManager);
                return mxGroupPortfolioSpecBuilder.getSearchResultUpdate(groupCombPortfolioFilter.getSearchWhereClause(), username, groupCombPortfolioFilter.getTemplate(), null, countFetched, totalRecords);
            }
        }
        //to get all records for the report date
        if (allGroups) {
            combinedPortfolioDetails = mxGroupCombinedPortfolioRepo.findAllByReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
        } else {
            //to get records for selected group Label for the report date
            combinedPortfolioDetails = mxGroupCombinedPortfolioRepo.findByGroupLabelAndReportDate(groupCombPortfolioFilter.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
        }
        document.put("totalPages", combinedPortfolioDetails.getTotalPages());
        document.put("records", combinedPortfolioDetails.getTotalElements());
        document.put("content", combinedPortfolioDetails.getContent());
        return document;
    }

    //combined groupcombined portfolio Export
    @Async
    @Transactional
    public CompletableFuture<Void> getGroupCombinedPortfolio(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String username) throws Exception {
        List<MxGroupCombinedPortfolio> groupCombinedPortfolioList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document groupCombinedPortfolios = getGrpCompPortfolio(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, username, countFetched, totalCount);
            groupCombinedPortfolioList.addAll((List<MxGroupCombinedPortfolio>) groupCombinedPortfolios.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupCombinedPortfolioList.size(), job.getId());
            totalPage = (int) groupCombinedPortfolios.get("totalPages");
            totalCount = (long) groupCombinedPortfolios.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", groupCombinedPortfolioList.size(), (int) totalCount, job.getId());
        //to export the group combined portfolio data
        return exportFilefromDetails(outputFormat, groupCombinedPortfolioList, fileName, fieldMaps, colors, job, fieldColumns);
    }

    //Combined groups user details Count
    public List<MxUserListItem> getUserDetailCount(LocalDate requestDate, List<String> groupLabel) {
        return mxUserGroupAccessRepository.findUserDetails(requestDate, groupLabel);
    }

    //combined user Details Export
    @Async
    @Transactional
    public CompletableFuture<Void> getUserListExport(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String colors, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Boolean allGroups, Sort.Order sort, String username) throws Exception {
        List<CombineMxUserList> userLists = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        //initialize the job status
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document userList = getUserDetails(page, pageSize, sortBy, sortingOrder, filters, allGroups, sort, username, countFetched, totalCount);
            userLists.addAll((List<CombineMxUserList>) userList.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, userLists.size(), job.getId());
            totalPage = (int) userList.get("totalPages");
            totalCount = (long) userList.get("records");
            countFetched = true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", userLists.size(), (int) totalCount, job.getId());
        //to export the combined groups User details data
        return exportFilefromDetails(outputFormat, userLists, fileName, fieldMaps, colors, job, fieldColumns);
    }

    //Combined user Details Data retrival
    @Transactional
    public Document getUserDetails(int page, int pageSize, String sortBy, String sortingOrder, GeneralSpecificationBuilder userListFilter, Boolean allGroups, Sort.Order sort, String username, boolean countFetched, long totalRecords) {
        Document document = new Document();
        LocalDate requestDate = LocalDate.parse(userListFilter.getDateValue(), dateTimeFormatter);
        Page<CombineMxUserList> combinedUserList;
        //search for combined groups user details
        if (userListFilter.getSearchWhereClause() != null && !userListFilter.getSearchWhereClause().isBlank()) {
            if (userListFilter.isGlobalSearch()) {
                saveWhereSearchHistory(userListFilter.getSearchWhereClause(), username, userListFilter.getTemplate());
                if (allGroups) {
                    combinedUserList = userListRepository.findUserListGlobalCombinedAllGroups(requestDate, userListFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                } else {
                    combinedUserList = userListRepository.findUserListGlobalCombined(userListFilter.getTemplateValue(),
                            requestDate, userListFilter.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", combinedUserList.getTotalPages());
                document.put("records", combinedUserList.getTotalElements());
                document.put("content", combinedUserList.getContent());
                return document;
            }
            String template = viewerExportService.getPropertyFields(userListFilter.getTemplate(), userListFilter.getSubTemplate(), false);
            log.info("template:{}",template);
            String[] templateList = template.split("\\|");
            CombinedGroupSearchConfig<CombineMxUserList> ospRightsSpecification = new CombinedGroupSearchConfig<>(templateList[3], templateList[3], userListFilter.getGroupValue(),
                    userListFilter.getTemplateValue(), userListFilter.getSubTemplate(), userListFilter.getSubTemplateValue(), templateList[0], userListFilter.getDateValue(), null, templateList[1],
                    templateList[2], sortBy, sortingOrder, page, pageSize, "userlist", allGroups, searchHistoryRepository, entityManager);
            return ospRightsSpecification.getSearchResultUpdate(userListFilter.getSearchWhereClause(), username, userListFilter.getTemplate(),null,countFetched,totalRecords);
        } else {
            //to get all records for the report date
            if (allGroups) {
                //to get all records for the report date
                combinedUserList = userListRepository.findAllByReportDateGroups(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
                //to get records for selected group Label for the report date
                combinedUserList = userListRepository.findGroupLabelAndRequestDate(userListFilter.getTemplateValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", combinedUserList.getTotalPages());
            document.put("records", combinedUserList.getTotalElements());
            document.put("content", combinedUserList.getContent());
            return document;
        }
    }
//
//    //Group combinedstprights export
    @Async
    @Transactional
    public CompletableFuture<Void> getStpRightsExport(GeneralSpecificationBuilder filters, String stprights, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, LocalDate requestDate, String name) throws Exception {
        List<CombinedStpRightsTemplate> groupCombinedStpRightsList = new ArrayList<>();
        int pageSize = 500;
        int page = 0;
        long totalCount = 0;
        int totalPage = 1;

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        boolean countFetched = false;
        for (int i = 0; i < totalPage; i++) {
            Document groupCombinedStpRights = getStpRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, requestDate, name,countFetched,totalCount);
            groupCombinedStpRightsList.addAll((List<CombinedStpRightsTemplate>) groupCombinedStpRights.get("content"));
            //update the job status
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupCombinedStpRightsList.size(), job.getId());
            totalPage = (int) groupCombinedStpRights.get("totalPages");
            totalCount = (long) groupCombinedStpRights.get("records");
            countFetched=true;
            page++;
        }
        log.info(" Export: Fetch complete.");
        //finalize the job status
        downloadJobService.updateJobProgress("FILE_WRITE", groupCombinedStpRightsList.size(), (int) totalCount, job.getId());
        //to export the group combined portfolio data
        return exportFilefromDetails(outputFormat, groupCombinedStpRightsList, stprights, fieldMaps, color, job, fieldColumns);
    }

//    //combined groupcombined stprights Data Retrival
public List<MxStpRightMatrixEod> getStpRightsCount(LocalDate requestDate, List<String> templateValue) {
    return stpRightsRepository.findDistinctTopByGlobalTemplateInAndReportDate(templateValue, requestDate);
}
    //combined groupcombined stprights Data Retrival
    @Transactional
    public Document getStpRights(int page, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder stpRightsExportXlsxRequest, Boolean allGroups, LocalDate requestDate, String name,boolean countFetched,long totalRecords) {
        Document document = new Document();
        Page<CombinedStpRightsTemplate> StpRightsTemplateDetails;
        //search for combined groups Stprights template
        if (stpRightsExportXlsxRequest.getSearchWhereClause() != null && !stpRightsExportXlsxRequest.getSearchWhereClause().isBlank()) {
            if (stpRightsExportXlsxRequest.getGroupValue().size() > 5) {
                document.put("content", Collections.emptyList());
                document.put("message", "Please select only five groups for search.");
                return document;
            }
            if (stpRightsExportXlsxRequest.isGlobalSearch()) {
                saveWhereSearchHistory(stpRightsExportXlsxRequest.getSearchWhereClause(), name, stpRightsExportXlsxRequest.getTemplate());
                if(allGroups){
                    StpRightsTemplateDetails=stpRightsRepository.findAllGroupDetailsGlobalSearch(requestDate, stpRightsExportXlsxRequest.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }else{
                    StpRightsTemplateDetails=stpRightsRepository.findSelectedGroupDetailsByGlobalSearch(stpRightsExportXlsxRequest.getTemplateValue(), stpRightsExportXlsxRequest.getGroupValue(),
                            requestDate, stpRightsExportXlsxRequest.getSearchWhereClause(), PageRequest.of(page, pageSize, Sort.by(sort)));
                }
                document.put("totalPages", StpRightsTemplateDetails.getTotalPages());
                document.put("records", StpRightsTemplateDetails.getTotalElements());
                document.put("content", StpRightsTemplateDetails.getContent());
                return document;
            }

            String monShortName = requestDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
            String stprightsTableName = "UAM_MX_STP_RIGHTS_MATRIX_EOD PARTITION (P_" + monShortName + ")~MxStpRightMatrixEod";
            String joinTemplate = "UAM_MX_GROUP_LIST PARTITION (P_" + monShortName + ")";
            String template = viewerExportService.getPropertyFields(stpRightsExportXlsxRequest.getTemplate(), stpRightsExportXlsxRequest.getSubTemplate(), false);
            String[] templateList = template.split("\\|");
            StringBuilder fieldName = getObject(templateList[1]);

            CombinedGroupSearchConfig<CombinedStpRightsTemplate> stpSpecification = new CombinedGroupSearchConfig<>(templateList[2], templateList[4],
                    stpRightsExportXlsxRequest.getGroupValue(), stpRightsExportXlsxRequest.getTemplateValue(), stpRightsExportXlsxRequest.getSubTemplate(),
                    stpRightsExportXlsxRequest.getSubTemplateValue(), templateList[0], stpRightsExportXlsxRequest.getDateValue(), null, stprightsTableName,
                    joinTemplate, sortBy, sortingOrder, page, pageSize, "stpRightsMatrix", allGroups, searchHistoryRepository, entityManager);
            return stpSpecification.getSearchResultUpdate(stpRightsExportXlsxRequest.getSearchWhereClause(), name, stpRightsExportXlsxRequest.getTemplate(),
                    fieldName, countFetched, totalRecords);

        } else {
            //if all the groups are selected
            if (allGroups) {
                //to get all records for the report Date
                StpRightsTemplateDetails = stpRightsRepository.findAllGroupDetailsUsingReportDate(requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            } else {
            //to get records with specific template and group Label
            log.info("template value---"+stpRightsExportXlsxRequest.getTemplateValue()+"========"+stpRightsExportXlsxRequest.getGroupValue()+"=="+requestDate) ;
            StpRightsTemplateDetails = stpRightsRepository.findSelectedGroupDetails(stpRightsExportXlsxRequest.getTemplateValue(), stpRightsExportXlsxRequest.getGroupValue(), requestDate, PageRequest.of(page, pageSize, Sort.by(sort)));
            }
            document.put("totalPages", StpRightsTemplateDetails.getTotalPages());
            document.put("records", StpRightsTemplateDetails.getTotalElements());
            document.put("content", StpRightsTemplateDetails.getContent());
            return document;
        }
    }

    public LocalDate getLatestDates() {
        DataImportJob job = dataImportJobRepository.findDistinctTopByFileNameStartsWithAndPurgedAndJobStatusOrderByReportDateDesc("stp", 'N', MxJobLogType.COMPLETED);
        return job != null ? job.getReportDate() : null;
    }

    public StringBuilder getObject(String tableName) {
        Class className = null;
        String object = tableName;
        switch (object) {
            case "MxOspRightsMatrix": {
                className = CombinedOspRightsTemplate.class;
                break;
            }
            case "MxChineseWallTmpl": {
                className = CombinedChineseWallTemplate.class;
                break;
            }
            case "MxUserListItem": {
                className = CombinedUserList.class;
                break;
            }
            case "MxFinaceAcctrlRights": {
                className = CombinedFinanceRightsTemplate.class;
                break;
            }
            case "MxOperationRights": {
                className = CombinedOperationRightsTemplate.class;
                break;
            }
            case "MxConsistencyTmpl": {
                className = CombinedConsistencyTemplate.class;
                break;
            }
            case "MxStpRightMatrixEod": {
                className = CombinedStpRightsTemplate.class;
                break;
            }
        }
        StringBuilder fieldNames = new StringBuilder();
        Field[] fieldList = className.getDeclaredFields();
        for (Field field : fieldList) {
            fieldNames.append("t." + field.getName());
            fieldNames.append(",");
        }
        fieldNames.setCharAt(fieldNames.lastIndexOf("t"), 'g');
        fieldNames.deleteCharAt(fieldNames.lastIndexOf(","));
        log.info("fieldNames:{}", fieldNames);
        return fieldNames;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getChineseWallDetailsAndExports(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String userName) throws Exception {
        log.info(filters.toString());
        List<CombinedChineseWallTemplate> chineseWallTemplate = new ArrayList<>();
        Document chineseWallTmpls = new Document();
        int pageSize = 1500;
        int page = 0;
        File file = new File(generateExportPathUsingFileName(fileName + outputFormat));
        file.getParentFile().mkdirs();

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");

        chineseWallTmpls = getChineseWall(page, pageSize, sortBy, sortingOrder, sort, allGroups, filters, userName, false, 0);
        if (outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xls")) {
            CompletableFuture<Void> future = exportXlsxForChineseWall(file, (List<?>) chineseWallTmpls.get("content"), fieldMaps, color, job, fieldColumns, (int) chineseWallTmpls.get("totalPages"), (long) chineseWallTmpls.get("records"), pageSize, sortBy, sortingOrder, sort, filters, allGroups, userName);
        } else if (outputFormat.equalsIgnoreCase(".csv")) {

            getChineseWallDetailsAndExport(filters, fileName, fieldMaps, color, job, outputFormat, fieldColumns, sortBy, sortingOrder, sort, allGroups, userName);
        }

        log.info(" Export: Fetch complete.");
        return CompletableFuture.completedFuture(null);

    }

    @Async
    @Transactional
    public CompletableFuture<Void> exportXlsxForChineseWall(File file, List<?> content, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, int totalPages, long records, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder filters, Boolean allGroups, String userName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyyMMdd HHmmss"));
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        List<CombinedChineseWallTemplate> finalList = new ArrayList<>((List<CombinedChineseWallTemplate>) content);
        fieldColumns.add("id");
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);

        SXSSFSheet sheet = workbook.createSheet();
        int hdrRowCount = 0;
        int columnCount = 0;
        SXSSFRow headerRow = sheet.createRow(hdrRowCount);
        for (FieldMap fieldMap : fieldMaps) {
            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                SXSSFCell cell = headerRow.createCell(columnCount++);
                cell.setCellValue(fieldMap.getDisplayName());
                hdrRowCount++;
            }
        }

        try {
            AtomicInteger f = new AtomicInteger(1);
            List<CompletableFuture<SXSSFRow>> completableFutureList = new ArrayList<>();
            int processed = 0;
            AtomicInteger row_no = new AtomicInteger(1);
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) records, finalList.size(), job.getId());

            for (int page = 0; page == 0 || page < totalPages; page++) {

                if (page > 0) {
                    Document doc = getChineseWall(page, pageSize, sortBy, sortingOrder, sort, allGroups, filters, userName, true, records);
                    finalList.addAll((List<CombinedChineseWallTemplate>) doc.get("content"));
                    processed = processed + finalList.size();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) records, processed, job.getId());
                }
                write(finalList, mapper, sheet, row_no, fieldMaps, completableFutureList, fieldColumns);
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

    public void write(List<?> finalList, ObjectMapper mapper, SXSSFSheet sheet, AtomicInteger row_no, List<FieldMap> fieldMaps, List<CompletableFuture<SXSSFRow>> completableFutureList, List<String> fieldColumns) throws JsonProcessingException {
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

    public void writeXlsx(Map<String, Object> trade, List<FieldMap> fieldMaps, SXSSFRow row, List<String> fieldColumns) {


        // write values in xlsx
        int fieldColumnCount = 0;
        try {
            if (trade.get("id") != null) {

                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                        SXSSFCell cell = row.createCell(fieldColumnCount++);
                        String fieldName = fieldMap.getEntityName();
                        String fieldFormat = fieldMap.getFormat();
                        String fieldValue = "";
                        if (fieldName.equalsIgnoreCase("id")) {
                            fieldValue = String.valueOf(row.getRowNum());
                            cell.setCellValue(fieldValue);
                        } else if (fieldFormat != null) {
                            if (trade.get(fieldName) != null && !trade.get(fieldName).toString().isBlank()) {
                                if (fieldFormat.equalsIgnoreCase("yyyyMMdd")) {
                                    LocalDate localDate = LocalDate.parse(trade.get(fieldName).toString());
                                    fieldValue = localDate.format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else if (fieldFormat.equalsIgnoreCase("yyyyMMdd hh:mm:ss")) {
                                    LocalDateTime localDateTime = LocalDateTime.parse(trade.get(fieldName).toString());
                                    fieldValue = localDateTime.format(DateTimeFormatter.ofPattern(fieldFormat));
                                } else {
                                    LocalTime localTime = LocalTime.parse(trade.get(fieldName).toString());
                                    fieldValue = localTime.format(DateTimeFormatter.ofPattern(fieldFormat));
                                }
                                cell.setCellValue(fieldValue);
                            } else {
                                cell.setCellValue(blankFields);
                            }
                        } else {
                            fieldValue = trade.get(fieldName) == null ? null : trade.get(fieldName).toString();
                            cell.setCellValue((fieldValue != null ? fieldValue : blankFields));
                        }
                    }}}
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Async
    @Transactional
    public CompletableFuture<Void> getNavigationRightsAndExports(GeneralSpecificationBuilder filters, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, String outputFormat, List<String> fieldColumns, String sortBy, String sortingOrder, Sort.Order sort, Boolean allGroups, String userName) throws Exception {

        List<MxGroupNavigationRight> groupNavigationRights = new ArrayList<>();
        Document navigationTmpls = new Document();
        int pageSize = 1500;
        int page = 0;
        File file = new File(generateExportPathUsingFileName(fileName + outputFormat));
        file.getParentFile().mkdirs();

        log.info(" Export: Initializing job status.");
        //initialize the job status
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");

        navigationTmpls = getNavigationRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, userName, false, 0);
        if (outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xls")) {
            CompletableFuture<Void> future = exportXlsxForNavigation(file, (List<?>) navigationTmpls.get("content"), fieldMaps, color, job, fieldColumns, (int) navigationTmpls.get("totalPages"), (long) navigationTmpls.get("records"), pageSize, sortBy, sortingOrder, sort, filters, allGroups, userName);
        } else if (outputFormat.equalsIgnoreCase(".csv")) {
            getNavigationRightsAndExport(filters, fileName, fieldMaps, color, job, outputFormat, fieldColumns, sortBy, sortingOrder, sort, allGroups, userName);
        }

        log.info(" Export: Fetch complete.");
        return CompletableFuture.completedFuture(null);
    }

    @Async
    @Transactional
    public CompletableFuture<Void> exportXlsxForNavigation(File file, List<?> content, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, int totalPages, long records, int pageSize, String sortBy, String sortingOrder, Sort.Order sort, GeneralSpecificationBuilder filters, Boolean allGroups, String userName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
        mapper.setDateFormat(new SimpleDateFormat("yyyyMMdd HHmmss"));
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        List<MxGroupNavigationRight> finalList = new ArrayList<>((List<MxGroupNavigationRight>) content);
        fieldColumns.add("id");
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);

        SXSSFSheet sheet = workbook.createSheet();
        int hdrRowCount = 0;
        int columnCount = 0;
        SXSSFRow headerRow = sheet.createRow(hdrRowCount);
        for (FieldMap fieldMap : fieldMaps) {
            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                SXSSFCell cell = headerRow.createCell(columnCount++);
                cell.setCellValue(fieldMap.getDisplayName());
                hdrRowCount++;
            }
        }
        try {
            AtomicInteger f = new AtomicInteger(1);
            List<CompletableFuture<SXSSFRow>> completableFutureList = new ArrayList<>();
            int processed = 0;
            AtomicInteger row_no = new AtomicInteger(1);
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) records, finalList.size(), job.getId());

            for (int page = 0; page == 0 || page < totalPages; page++) {

                if (page > 0) {
                    Document doc = getNavigationRights(page, pageSize, sortBy, sortingOrder, sort, filters, allGroups, userName, true, records);
                    finalList.addAll((List<MxGroupNavigationRight>) doc.get("content"));
                    processed = processed + finalList.size();
                    downloadJobService.updateJobProgress("FETCH_ITEMS", (int) records, processed, job.getId());
                }
                write(finalList, mapper, sheet, row_no, fieldMaps, completableFutureList, fieldColumns);
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

    //to add the query to search history table
    public void saveWhereSearchHistory(String whereCondition, String username, String reportType) {
        CompletableFuture.runAsync(() -> {
            SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(username, reportType, whereCondition);
            if (searchHistory == null) {
                SearchHistory history = new SearchHistory();
                history.setUserName(username);
                history.setReportType(reportType);
                history.setSearchQuery(whereCondition);
                searchHistoryRepository.save(history);
            }
        });
    }
}






