package com.finsurge.tmr_portal.mx_superview.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.report_publisher.RPControllerUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
@Slf4j
public class ElasticSearchUtils {

    @Value("${uam.blank.fields}")
    private String blankFields;

    public  final Environment environment ;
    private final ElasticsearchClient esClient;

    @Autowired
    private DownloadJobService downloadJobService;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    Map<String, Instant> pitIdCreationTimes = new ConcurrentHashMap<>();

    public ElasticSearchUtils(Environment environment, ElasticsearchClient esClient, DownloadJobService downloadJobService) {
        this.environment = environment;
        this.esClient = esClient;
        this.downloadJobService = downloadJobService;
    }

    // Pagination - checking is pitId  is Active or Not ?
    public boolean isPitIdExpired(String pitId) {
        Instant creationTime = pitIdCreationTimes.getOrDefault(pitId, Instant.MIN);
        Instant currentTime = Instant.now();
        Duration timeElapsed = Duration.between(creationTime, currentTime);
        String keepAliveTimeStr = environment.getProperty("keepAliveTime.pit");
        Duration keepAliveTime = Duration.parse("PT" + keepAliveTimeStr.toUpperCase());
        return timeElapsed.compareTo(keepAliveTime) < 0;
    }


    // pagination - creating pitId
    public String createPitId(String indexName) throws IOException {
        OpenPointInTimeResponse openPointInTimeResponse = esClient.openPointInTime(pit ->
                pit.index(indexName).keepAlive(Time.of(t ->
                        t.time(environment.getProperty("keepAliveTime.pit")))));
        Instant creationTime = Instant.now();
        String newPitId = openPointInTimeResponse.id();
        pitIdCreationTimes.put(newPitId, creationTime);
        return openPointInTimeResponse.id();
    }

    public boolean isDateField(String input) {
        // Regular expression to match date formats (yyyyMMdd)
        String regex = "\\b\\d{8}\\b"; // This matches 8-digit sequences
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }


    public List<String> getDataFromJsonFile(String path ,String key) throws IOException, ParseException {
        File resource = new ClassPathResource(path).getFile();
        String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(defaultConfig);
        Object fields = json.get(key);
        JSONArray newData = (JSONArray) fields;
        List<String> result = new ArrayList<>();
        for (Object obj : newData) {
            result.add(obj.toString());
        }
        return result;
    }


    public List<Query> getBoolQuery(String columnName, List<String> filterColumnVal, List<Query> queryList) {
        Query finalBoolQuery;
        var array = new ArrayList<FieldValue>();
        if (filterColumnVal.contains("Nil")) {
            for (String newData : filterColumnVal) {
                if (newData.equalsIgnoreCase("Nil")) {
                    newData = "";
                }
                array.add(FieldValue.of(newData));
            }
            Query termsQuery = TermsQuery.of(ts ->
                    ts.field(columnName)
                            .terms(TermsQueryField.of(t ->
                                    t.value(array))))._toQuery();
            finalBoolQuery = BoolQuery.of(b -> b.must(termsQuery))._toQuery();
            queryList.add(finalBoolQuery);
        } else {
            //       var array = new ArrayList<FieldValue>();
            for (String newData : filterColumnVal) {
                array.add(FieldValue.of(newData));
            }
            Query termsQuery = TermsQuery.of(ts -> ts
                    .field(columnName)
                    .terms(TermsQueryField.of(t -> t.value(array))))._toQuery();
            finalBoolQuery = BoolQuery.of(b -> b.must(termsQuery))._toQuery();
            queryList.add(finalBoolQuery);
        }

        log.info("Filter Search  Query - filterSearch :- {}", queryList);
        return queryList;
    }
    public Long getNumericCount(String filterColumn, List<Query> queryList,String indexName) throws IOException {
        String script = "def alphaValue = doc['" + filterColumn + ".keyword'].size() > 0 ? doc['" + filterColumn + ".keyword'].value : null; " +
                "return alphaValue != null && alphaValue =~ /^[^\\p{L}]+$/;";
        Query numericScriptQuery = ScriptQuery.of(s -> s.script(
                        Script.of(scr -> scr.inline(
                                InlineScript.of(i -> i.source(script))))))
                ._toQuery();
        log.info("Numeric Script query: {}", numericScriptQuery);
        List<Query> scriptQuery = new ArrayList<>();
        scriptQuery.add(numericScriptQuery);
        Query searchQuery = BoolQuery.of(q -> q.must(queryList).filter(scriptQuery))._toQuery();
        log.info("Numieric Script searchQuery : {}", searchQuery);
        CountRequest countRequest = CountRequest.of(cr -> cr.index(indexName)
                .query(searchQuery));
        log.info("Numieric CountRequest : {}", countRequest);
        long totalCount = esClient.count(countRequest).count();
        return totalCount;
    }

    public Long getAlphaNumericCount(String filterColumn, List<Query> queryList,String indexName ) throws IOException {
        String alphaNumeric = "def alphaValue = doc['" + filterColumn + ".keyword'].size() > 0 ? doc['" + filterColumn + ".keyword'].value : null; " +
                "return alphaValue != null && alphaValue =~ /[a-zA-Z]+/;";
        Query alphaNumericQuery = ScriptQuery.of(s ->
                s.script(Script.of(script ->
                        script.inline(InlineScript.of(i ->
                                i.source(alphaNumeric))))))._toQuery();
        List<Query> scriptQuery = new ArrayList<>();
        scriptQuery.add(alphaNumericQuery);
        Query filterAlphaNumericQuery = BoolQuery.of(b -> b.must(queryList).filter(scriptQuery))._toQuery();
        CountRequest countRequest = CountRequest.of(c -> c.index(indexName)
                .query(filterAlphaNumericQuery));
        log.info("Alpha Numieric CountRequest : {}", countRequest);
        Long totalCount = esClient.count(countRequest).count();
        return totalCount;
    }

    public void addToSearchAfterValues(Object element, List<FieldValue> searchAfterValues) {
        if (element instanceof String) {
            searchAfterValues.add(FieldValue.of((String) element));
        } else if (element instanceof Number) {
            searchAfterValues.add(FieldValue.of(((Number) element).longValue()));
        }
    }



// Delete Data -AutoLoad_ LoaderConfig:-
    public void deleteData(String dateValue, String reportName) throws IOException {
        //Setting report name according to .properties file
        String reportNameIndex = reportName + ".search";
        log.info("deleteData  - reportNameIndex : {}", reportNameIndex);
        //Checks count
        if (getCountByDate(dateValue, reportNameIndex) > 0) {
            log.info("Count : {}", getCountByDate(dateValue, reportNameIndex));
            Query query = TermQuery.of(t -> t.field("reportDate").value(dateValue))._toQuery();
            esClient.deleteByQuery(DeleteByQueryRequest.of(d ->
                    d.index(environment.getProperty(reportNameIndex)).
                            query(query).scrollSize(environment.getProperty("aggregationPageSize",Long.TYPE))));
        }
    }

    public Long getCountByDate(String dateValue, String reportNameIndex) throws IOException {
        Query query = TermQuery.of(t -> t.field("reportDate").value(dateValue))._toQuery();
        CountRequest countRequest = CountRequest.of(c -> c.index(environment.getProperty(reportNameIndex)).query(query));
        return esClient.count(countRequest).count();
    }


    public List<Document> checkDuplicateObjects(List<Long> countList, List<Object> keyList) {
        List<Document> resultDocuments = new ArrayList<>();
        for (int i = 0; i < keyList.size(); i++) {
            Long currentKey = countList.get(i);
            String currentValue = keyList.get(i).toString();
            boolean keyExists = false;
            for (Document doc : resultDocuments) {
                String name = doc.getString("name");
                if (name.equalsIgnoreCase(currentValue)) {
                    long oldValue = doc.getLong("value");
                    doc.put("value", oldValue + currentKey);
                    keyExists = true;
                    break;
                }
            }
            if (!keyExists) {
                Document document = new Document("name", currentValue).append("value", currentKey);
                resultDocuments.add(document);
            }
        }
        return resultDocuments;
    }

    public List<Document> checkDuplicates(List<Long> countList, List<String> keyList) {
        List<Document> resultDocuments = new ArrayList<>();
        for (int i = 0; i < keyList.size(); i++) {
            Long currentKey = countList.get(i);
            String currentValue = keyList.get(i);
            boolean keyExists = false;
            for (Document doc : resultDocuments) {
                String name = doc.getString("name");
                if (name.equalsIgnoreCase(currentValue)) {
                    long oldValue = doc.getLong("value");
                    doc.put("value", oldValue + currentKey);
                    keyExists = true;
                    break;
                }
            }
            if (!keyExists) {
                Document document = new Document("name", currentValue).append("value", currentKey);
                resultDocuments.add(document);
            }
        }
        return resultDocuments;
    }

    public void saveWhereSearchHistory(String whereCondition, String userName, String reportType) {
        CompletableFuture.runAsync(() -> {
            SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(userName, reportType, whereCondition);
            if (searchHistory == null) {
                SearchHistory history = new SearchHistory();
                history.setUserName(userName);
                history.setReportType(reportType);
                history.setSearchQuery(whereCondition);
                searchHistoryRepository.save(history);
            }
        });
    }



    public CompletableFuture<Void> exportFileFromDetails(String outputFormat, List<?> exportData, String fileName,List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, ElasticSearchModel elasticSearchModel) throws Exception {
        if (outputFormat.equalsIgnoreCase(".xls") || outputFormat.equalsIgnoreCase(".xlsx") || outputFormat.equalsIgnoreCase(".xlsb")) {

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File file = new File(generateExportPathUsingFileName(fileName + "_" + timestamp + outputFormat));
            file.getParentFile().mkdirs();
            file = exportAsXlsxFromList(prepareMapListToExport(exportData), file, fieldMaps, color, job, fieldColumns, elasticSearchModel);
            //update download job

            downloadJobService.updateJobProgress("COMPLETE", exportData.size(), exportData.size(), job.getId());
            downloadJobService.updateJob(job.getId(), file.getAbsolutePath(), true);
            return CompletableFuture.completedFuture(null);
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(generateExportPathUsingFileName(fileName + "_" + timestamp + ".csv"));
        //  File file = new File(generateExportPathUsingFileName(fileName + ".csv"));
        file.getParentFile().mkdirs();
        log.info("CSV Export: Starting write.");
        file = exportAsCsvFromList(prepareMapListToExport(exportData), file, fieldMaps, job, fieldColumns, elasticSearchModel);
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
        //     log.info("JSON STRING {}",jsonString);
        return mapper.readValue(jsonString, new TypeReference<>() {
        });
    }

    public File exportAsCsvFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, DownloadJob job, List<String> fieldColumns, ElasticSearchModel elasticSearchModel) {

        List<String> columnsToWrite = new ArrayList<>();
        columnsToWrite.add("id");
        columnsToWrite.addAll(elasticSearchModel.getFieldColumns());

        fieldColumns.add("id");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, Charset.forName("Cp1252")))) {

            int i = 1;
            if (columnsToWrite.size() == 1) {
                // Write all fields if columnsToWrite is null or empty
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
            } else {
                // Write only specified columns
                for (String columnToWrite : columnsToWrite) {
                    for (FieldMap fieldMap : fieldMaps) {
                        if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                            if (columnToWrite.equals(fieldMap.getEntityName())) {
                                bw.write(fieldMap.getDisplayName());
                                if (fieldMaps.size() != i) {
                                    bw.append(",");
                                }
                                break; // Move to the next column after finding a match
                            }
                        }
                    }
                }
                bw.newLine();
            }
            int lineCount = 1;
            for (Map<String, Object> trade : list) {
                trade.remove("id");
                trade.remove("score");
                trade.remove("sortValues");
                int j = 1;
                if (columnsToWrite.size() == 1) {
                    // Write all fields if columnsToWrite is null or empty
                    for (FieldMap fieldMap : fieldMaps) {
                        if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                            String fieldValue = getCsvValues(fieldMap, trade, lineCount);
                            if (fieldValue != null) {

                                if (fieldValue.equals("[]") || (fieldValue.equals("[, ]"))) {
                                    fieldValue = "Nil";
                                } else if (fieldValue.equalsIgnoreCase("active") || (fieldValue.equalsIgnoreCase("inactive"))) {
                                    fieldValue = toCamelCase(fieldValue);
                                }else {
                                    fieldValue = fieldValue.replace("[", "").replace("]", "");
                                }
                            }
                            bw.write(escapeAndQuote(fieldValue != null ? fieldValue : blankFields));
                            if (fieldMaps.size() != j) {
                                bw.append(",");
                            }
                            j++;
                        }
                    }
                } else {
                    // Write only specified columns
                    for (String columnToWrite : columnsToWrite) {
                        for (FieldMap fieldMap : fieldMaps) {
                            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                                if (columnToWrite.equals(fieldMap.getEntityName())) {
                                    String fieldValue = getCsvValues(fieldMap, trade, lineCount);
                                    // Remove square brackets if present
                                    if (fieldValue != null) {
                                        if (fieldValue.equals("[]") || (fieldValue.equals("[, ]"))) {
                                            fieldValue = "Nil";
                                        } else if (fieldValue.equalsIgnoreCase("active") || (fieldValue.equalsIgnoreCase("inactive"))) {
                                            fieldValue = toCamelCase(fieldValue);
                                        }else {
                                            fieldValue = fieldValue.replace("[", "").replace("]", "");
                                        }
                                    }
                                    bw.write(escapeAndQuote(fieldValue != null ? fieldValue : blankFields));
                                    if (fieldMaps.size() != j) {
                                        bw.append(",");
                                    }
                                    j++;
                                }
                            }
                        }
                    }
                }
                bw.newLine();
                lineCount++;
            }
            log.info("CSV Export: Updating final status.");
            if (job != null) {
                downloadJobService.updateJobProgress("FILE_WRITE", list.size(), lineCount, job.getId());
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private String escapeAndQuote(String value) {
        // Escape double quotes
        value = value.replaceAll("\"", "\"\"");
        // Enclose within double quotes
        return "\"" + value + "\"";
    }
    private String generateExportPathUsingFileName(String fileName) {
        String importProperty = environment.getProperty("uam.paths.export") + File.separator + fileName;
        importProperty = RPControllerUtils.fixSeparatorsForUnix(importProperty);
        return importProperty;
    }

    public File exportAsXlsxFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, ElasticSearchModel elasticSearchModel) throws Exception {
        List<String> columnsToWrite = new ArrayList<>(elasticSearchModel.getFieldColumns());

        columnsToWrite.add(0, "id");
        fieldColumns.add("id");
        try {
            Thread.currentThread().setName("xlsxExport");
//            log.info("inside export method {}", Thread.currentThread().getName());
            SXSSFWorkbook workbook = new SXSSFWorkbook(100);
            SXSSFSheet sheet = workbook.createSheet();

            // Initialize counters and row for the header
            int rowCount = 0;
            int columnCount = 0;
            SXSSFRow row = sheet.createRow(rowCount++);

            // Create default cell style
            CellStyle defaultCellStyle = sheet.getWorkbook().createCellStyle();

            // Write header row
            // writing all data
            if (columnsToWrite.size() == 1) {
                for (FieldMap fieldMap : fieldMaps) {
                    if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                        SXSSFCell cell = row.createCell(columnCount++);
                        cell.setCellValue(fieldMap.getDisplayName());
                    }
                }
            } else {
                for (String columnToWrite : columnsToWrite) {
                    for (FieldMap fieldMap : fieldMaps) {
                        if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                            if (columnToWrite.equals(fieldMap.getEntityName())) {
                                SXSSFCell cell = row.createCell(columnCount++);
                                cell.setCellValue(fieldMap.getDisplayName());
                            }
                        }
                    }
                }
            }
            int itemCount = 0;
            for (Map<String, Object> trade : list) {
                columnCount = 0;
                row = sheet.createRow(rowCount++);
                if ((columnsToWrite.size() == 1)) {
                    for (FieldMap fieldMap : fieldMaps) {
                        if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                            SXSSFCell cell = row.createCell(columnCount++);
                            String fieldValue = getXlsxValues(fieldMap, trade, rowCount - 1);
                            if (fieldValue != null) {
                                if (fieldValue.equals("[]") || (fieldValue.equals("[, ]"))) {
                                    fieldValue = "Nil";
                                } else if (fieldValue.equalsIgnoreCase("active") || (fieldValue.equalsIgnoreCase("inactive"))) {
                                    fieldValue = toCamelCase(fieldValue);
                                }else {
                                    fieldValue = fieldValue.replace("[", "").replace("]", "");
                                }
                            }
                            cell.setCellValue(fieldValue);
                        }
                    }
                    itemCount++;
                } else {
                    for (String columnToWrite : columnsToWrite) {
                        for (FieldMap fieldMap : fieldMaps) {
                            if (fieldColumns.size() == 1 || fieldColumns.contains(fieldMap.getEntityName())) {
                                if (columnToWrite.equals(fieldMap.getEntityName())) {
                                    SXSSFCell cell = row.createCell(columnCount++);
                                    String fieldValue = getXlsxValues(fieldMap, trade, rowCount - 1);
                                    if (fieldValue != null) {
                                        if (fieldValue.equals("[]") || (fieldValue.equals("[, ]"))) {
                                            fieldValue = "Nil";
                                        } else if (fieldValue.equalsIgnoreCase("active") || (fieldValue.equalsIgnoreCase("inactive"))) {
                                            fieldValue = toCamelCase(fieldValue);
                                        }else {
                                            fieldValue = fieldValue.replace("[", "").replace("]", "");
                                        }
                                    }
                                    cell.setCellValue(fieldValue);
                                }
                            }
                        }
                    }
                    itemCount++;
                }
            }

            FileOutputStream out = new FileOutputStream(file);
            try {

                workbook.write(out);
                log.info("Workbook successfully written to file");
            } catch (IOException e) {
                log.error("Error writing workbook to file: {}", e.getMessage());
                e.printStackTrace();
            }
            workbook.close();
            out.close();
            downloadJobService.updateJobProgress("FILE_SAVING", list.size(), itemCount, job.getId());
        } catch (Exception e) {
            log.error("Error", e);
            throw e;
        }
        return file;
    }

    private String getXlsxValues(FieldMap fieldMap, Map<String, Object> trade, int lineCount) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
        String dateFormatDynamic = "";
        String dateTimeFormatDynamic = dateFormatDynamic.concat(" ").concat("HH:mm:ss");
        String fieldName = fieldMap.getEntityName();
        String fieldFormat = fieldMap.getFormat();
        Object fieldValue = trade.get(fieldName);
        if ("id".equalsIgnoreCase(fieldName)) {
            return String.valueOf(lineCount);
        }
        String formattedValue = "";
        if (fieldFormat != null) {
            if (fieldValue != null && !fieldValue.toString().isBlank()) {
                if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy") && !fieldValue.toString().contains("-")) {
                    formattedValue = LocalDate.parse(fieldValue.toString(), dateFormatter).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy HH:mm:ss")) {
                    formattedValue = LocalDateTime.parse(fieldValue.toString(), dateTimeFormatter).format(DateTimeFormatter.ofPattern(dateTimeFormatDynamic));
                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy")) {
                    formattedValue = LocalDate.parse(fieldValue.toString()).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                }
            } else if (fieldFormat.equalsIgnoreCase("yyyyMMMdd")) {
                formattedValue = LocalDate.parse(fieldValue.toString()).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
            } else {
                formattedValue = LocalTime.parse(fieldValue.toString()).format(DateTimeFormatter.ofPattern(fieldFormat));
            }
        } else {
            formattedValue = blankFields;
        }
        {
            formattedValue = (fieldValue != null && !fieldValue.toString().isEmpty()) ? fieldValue.toString() : blankFields;
        }
        return formattedValue;
    }

    private String getCsvValues(FieldMap fieldMap, Map<String, Object> trade, int lineCount) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
        String dateFormatDynamic = "";
        String dateTimeFormatDynamic = dateFormatDynamic.concat(" ").concat("HH:mm:ss");
        String fieldName = fieldMap.getEntityName();
        String fieldFormat = fieldMap.getFormat();
        Object fieldValue = trade.get(fieldName);
        if ("id".equalsIgnoreCase(fieldName)) {
            return String.valueOf(lineCount);
        }
        String formattedValue = "";
        if (fieldFormat != null) {
            if (fieldValue != null && !fieldValue.toString().isBlank()) {
                if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy") && (!fieldValue.toString().contains("-"))) {
                    formattedValue = LocalDate.parse(fieldValue.toString(), dateFormatter).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy HH:mm:ss")) {
                    formattedValue = LocalDateTime.parse(fieldValue.toString(), dateTimeFormatter).format(DateTimeFormatter.ofPattern(dateTimeFormatDynamic));
                } else if (fieldFormat.equalsIgnoreCase("dd-MMM-yyyy")) {
                    formattedValue = LocalDate.parse(fieldValue.toString()).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
                }
            } else if (fieldFormat.equalsIgnoreCase("yyyyMMMdd")) {
                formattedValue = LocalDate.parse(fieldValue.toString()).format(DateTimeFormatter.ofPattern(dateFormatDynamic));
            } else {
                formattedValue = LocalTime.parse(fieldValue.toString()).format(DateTimeFormatter.ofPattern(fieldFormat));
            }
        } else {
            formattedValue = blankFields;
        }
        {
            formattedValue = (fieldValue != null && !fieldValue.toString().isEmpty()) ? fieldValue.toString() : blankFields;
        }
        return formattedValue;
    }


    private static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public Long getCount(Long jobId, String tableName) {
        long totalCount = 0;
        String indexName = null;
        switch (tableName) {
            case "UAM_MX_COUNTER_PARTY": {
                indexName = CounterPartyDocument.INDEX_NAME;
                break;
            }
            case "UAM_MX_DORMANT_COUNTERPARTY": {
                indexName = DormantCounterparty.INDEX_NAME;
                break;
            }
            case "UAM_MX_CLOSING_ENTITY": {
                indexName = ClosingEntity.INDEX_NAME;
                break;
            }
            case "UAM_MX_COUNTERPARTY_CREATION": {
                indexName = CounterpartyCreation.INDEX_NAME;
                break;
            }
            case "UAM_USER_GROUP_ACCESS_RIGHT": {
                indexName = UserGroupAccessRights.INDEX_NAME;
                break;
            }
            case "UAM_MX_USER_LIST": {
                indexName = UserList.INDEX_NAME;
                break;
            }
            case "UAM_MX_GROUP_LIST": {
                indexName = UserGroupList.INDEX_NAME;
                break;
            }
            case "UAM_MX_USER_LICENSE": {
                indexName = UserLicense.INDEX_NAME;
                break;
            }case "UAM_MX_USER_POLICY": {
                indexName = UserPolicy.INDEX_NAME;
                break;
            }case "UAM_MX_GROUP_PORTFOLIO_RIGHTS": {
                indexName =GroupPortfolioRights.INDEX_NAME;
                break;
            }case "UAM_MX_CHINESE_WALL_TMPL": {
                indexName = ChineseWall.INDEX_NAME;
                break;
            }
            case "UAM_MX_GROUP_NAV_RIGHTS": {
                indexName = GroupNavigationRights.INDEX_NAME;
                break;
            }
            case "UAM_MX_GROUP_COMBINED_PORTFOLIO": {
                indexName = GroupComboPortfolio.INDEX_NAME;
                break;
            }
            case "UAM_MX_OPERATION_RIGHTS": {
                indexName = OperationRights.INDEX_NAME;
                break;
            }
            case "UAM_MX_OSP_RIGHTS_MATRIX": {
                indexName = OspRightsMatrix.INDEX_NAME;
                break;
            }
            case "UAM_MX_ENTERPRISE_RISK": {
                indexName = EnterpriseRisk.INDEX_NAME;
                break;
            }
            case "UAM_MX_FINANCE_ACCTRL_RIGHTS": {
                indexName = FinanceRights.INDEX_NAME;
                break;
            }
            case "UAM_MX_CONSISTENCY_TMPL": {
                indexName = ConsistencyTmpl.INDEX_NAME;
                break;
            }
            case "UAM_MX_CWT_CONFIG_MGT_RIGHT": {
                indexName = ConfigMgtRight.INDEX_NAME;
                break;
            }case "UAM_MX_COUNTER_PARTY_DISPLAY": {
                indexName = DisplaySI.INDEX_NAME;
                break;
            }
            case "UAM_MX_SUPCHG_AUDIT": {
                indexName = SupChgAudit.INDEX_NAME;
                break;
            }
            case "UAM_UDF_STRUCTURE_IRD": {
                indexName = UdfStructureIrd.INDEX_NAME;
                break;
            }
            case "UAM_UDF_STRUCTURE_COMM": {
                indexName = UdfStructureComm.INDEX_NAME;
                break;
            }
            case "UAM_UDF_STRUCTURE_FXD": {
                indexName = UdfStructureFxd.INDEX_NAME;
                break;
            }
            case "UAM_USER_LOGIN_AUDIT": {
                indexName = UserLoginAudit.INDEX_NAME;
                break;
            }
            case "UAM_STP_RIGHTS_SRC_MOD": {
                indexName = StpRightsSrcMod.INDEX_NAME;
                break;
            }
            case "UAM_STP_RIGHTS_TYPOLOGY": {
                indexName = StpRightsTypology.INDEX_NAME;
                break;
            }
            case "UAM_STP_RIGHTS_MATRIX": {
                indexName = StpRightsMatrix.INDEX_NAME;
                break;
            }
        }
        Query countQuery = TermQuery.of(t -> t.field("jobId").value(String.valueOf(jobId)))._toQuery();
        String finalIndexName = indexName;

        CountRequest countRequest = CountRequest.of(cr -> cr.index(finalIndexName)
                .query(countQuery));
        try {
            totalCount = esClient.count(countRequest).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return totalCount;
    }
    public String deleteData(String tableName, LocalDate reportDate,Long jobId) throws IOException {
        String indexName = switch (Objects.requireNonNull(tableName)) {
            case "UAM_MX_COUNTER_PARTY" -> CounterPartyDocument.INDEX_NAME;
            case "UAM_MX_DORMANT_COUNTERPARTY" ->DormantCounterparty.INDEX_NAME;
            case "UAM_MX_CLOSING_ENTITY"->ClosingEntity.INDEX_NAME;
            case "UAM_MX_COUNTERPARTY_CREATION"->CounterpartyCreation.INDEX_NAME;
            case "UAM_MX_USER_LIST"->UserList.INDEX_NAME;
            case "UAM_MX_GROUP_LIST"->UserGroupList.INDEX_NAME;
            case "UAM_MX_USER_LICENSE"->UserLicense.INDEX_NAME;
            case "UAM_USER_GROUP_ACCESS_RIGHT"->UserGroupAccessRights.INDEX_NAME;
            case "UAM_MX_USER_POLICY"->UserPolicy.INDEX_NAME;
            case "UAM_MX_GROUP_PORTFOLIO_RIGHTS"->GroupPortfolioRights.INDEX_NAME ;
            case "UAM_MX_CHINESE_WALL_TMPL"->ChineseWall.INDEX_NAME;
            case "UAM_MX_GROUP_NAV_RIGHTS"->GroupNavigationRights.INDEX_NAME;
            case "UAM_MX_GROUP_COMBINED_PORTFOLIO"->GroupComboPortfolio.INDEX_NAME;
            case "UAM_MX_OPERATION_RIGHTS"->OperationRights.INDEX_NAME;
            case "UAM_MX_OSP_RIGHTS_MATRIX"->OspRightsMatrix.INDEX_NAME;
            case "UAM_MX_ENTERPRISE_RISK"->EnterpriseRisk.INDEX_NAME;
            case "UAM_MX_FINANCE_ACCTRL_RIGHTS"-> FinanceRights.INDEX_NAME;
            case "UAM_MX_CONSISTENCY_TMPL"->ConsistencyTmpl.INDEX_NAME;
            case "UAM_MX_CWT_CONFIG_MGT_RIGHT"-> ConfigMgtRight.INDEX_NAME;
            case "UAM_MX_COUNTER_PARTY_DISPLAY"-> DisplaySI.INDEX_NAME;
            case "UAM_MX_SUPCHG_AUDIT"-> SupChgAudit.INDEX_NAME;
            case "UAM_UDF_STRUCTURE_IRD" -> UdfStructureIrd.INDEX_NAME;
            case "UAM_UDF_STRUCTURE_COMM" -> UdfStructureComm.INDEX_NAME;
            case "UAM_UDF_STRUCTURE_FXD" -> UdfStructureFxd.INDEX_NAME;
            case "UAM_USER_LOGIN_AUDIT" -> UserLoginAudit.INDEX_NAME;
            case "UAM_STP_RIGHTS_SRC_MOD" -> StpRightsSrcMod.INDEX_NAME;
            case "UAM_STP_RIGHTS_TYPOLOGY" -> StpRightsTypology.INDEX_NAME;
            case "UAM_STP_RIGHTS_MATRIX" -> StpRightsMatrix.INDEX_NAME;

            default -> null;
        };
        Query deleteQuery = TermQuery.of(t -> t.field("reportDate").value(String.valueOf(reportDate)))._toQuery();
        Query jobIdQuery = TermQuery.of(t -> t.field("jobId").value(String.valueOf(jobId)))._toQuery();
        DeleteByQueryRequest request =DeleteByQueryRequest.of(d -> d.
                index(indexName).
                query(BoolQuery.of(b->b.must(deleteQuery).must(jobIdQuery))._toQuery()).
                scrollSize(environment.getProperty("aggregationPageSize",Long.TYPE)));

        DeleteByQueryResponse deleteByQueryResponse = esClient.deleteByQuery(request);

        log.info("Delete Query with BatchSize :{}",request);
        log.info(String.valueOf(deleteByQueryResponse.deleted()));
        return "DELETED SUCCESS FULLY";
    }


    public boolean isNumericField(String input) {
        // Regular expression to match date formats (yyyyMMdd)
        String regex = "\\d+"; // This matches 8-digit sequences
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public boolean globalSearchFields(String input) {
        String regex = "\\d+(\\s+(AND|OR)\\s+\\d+)*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    @SneakyThrows
    public <T> Integer getDropDownFieldCount(String filterColumnName, List<Query> queryList, String indexName, Class<T> documentClass) {
        Integer count = 0;
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Aggregation agg = Aggregation.of(a -> a.cardinality(c -> c.field(filterColumnName + ".keyword")));
        aggregation.put("unique_categories", agg);

        Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .query(searchQuery)
                .index(indexName).aggregations(aggregation));

        log.info("getDropDownFieldCount - Search request :{}", searchRequest);

        SearchResponse<T> searchResponse = esClient.search(searchRequest, documentClass);
        if (searchResponse.aggregations() != null && searchResponse.aggregations().containsKey("unique_categories")) {
            Aggregate uniqueCategoriesAgg = searchResponse.aggregations().get("unique_categories");
            // Check if it is a CardinalityAggregate and extract the value
            if (uniqueCategoriesAgg.isCardinality()) {
                CardinalityAggregate cardinalityAgg = uniqueCategoriesAgg.cardinality();
                count = Math.toIntExact(cardinalityAgg.value());  // Extract the value
            }
        }

        return count;
    }
}
