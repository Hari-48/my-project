package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.util.NamedValue;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import lombok.SneakyThrows;
import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class TradeUdfService {

    private static final Logger log = LoggerFactory.getLogger(TradeUdfService.class);

    private final ElasticsearchClient esClient;

    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private ElasticUtils elastic_utils;

    @Autowired
    public TradeUdfService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public <T> Document getTradeUdfHeaders(String date, String sortBy, String sortingOrder, int pageSize, String indexName, Class<T> clazz, String userName, String excludeFieldsKey, String integerFieldsKey, String doubleFieldskey) throws IOException {
        List<Query> queryList = new ArrayList<>();
        String newReportDate = elastic_utils.reportDateCheck(date, indexName, clazz);
        // Create the TermQuery to filter by reportDate
        Query reportDateQuery = TermQuery.of(q -> q.field("reportDate").value(newReportDate))._toQuery();
        queryList.add(reportDateQuery);
        // Build the SearchRequest
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(reportDateQuery)
                .size(1) // Adjust size if more documents are needed
        );
        // Execute the search request with the generic type
        SearchResponse<T> searchResponse = esClient.search(searchRequest, clazz);
        // Collect all documents from the search response
        List<T> allDocuments = new ArrayList<>();
        searchResponse.hits().hits().forEach(hit -> allDocuments.add(hit.source()));
        log.info("The Exclude Field Key is : {}", excludeFieldsKey);
        // Extract field names from the first document if available
        List<String> fieldNames = Collections.emptyList();
        if (!allDocuments.isEmpty()) {
            T document = allDocuments.get(0);
            fieldNames = extractFieldNames(document, excludeFieldsKey);
        }
        // List to hold the fields and their counts
        List<Map<String, Object>> fieldsAndCounts = new ArrayList<>();
        // Iterate over field names to get unique field counts
        for (String fieldName : fieldNames) {
            Integer uniqueFieldCount = getUniqueFieldCount(fieldName, queryList, indexName, clazz, integerFieldsKey, doubleFieldskey);
            Map<String, Object> fieldCountMap = new LinkedHashMap<>();
            fieldCountMap.put("field", fieldName);
            fieldCountMap.put("count", uniqueFieldCount);
            fieldsAndCounts.add(fieldCountMap);
        }
        // Sorting logic based on sortBy and sortingOrder
        if ("field".equalsIgnoreCase(sortBy)) {
            // Case-insensitive sorting for field names (field values)
            fieldsAndCounts.sort(Comparator.comparing(map -> ((String) map.get("field")).toLowerCase()));
        } else if ("count".equalsIgnoreCase(sortBy)) {
            // Sorting by count
            fieldsAndCounts.sort(Comparator.comparingInt(map -> (Integer) map.get("count")));
        }
        // Handle the sorting order (ascending by default, reverse for descending)
        if ("desc".equalsIgnoreCase(sortingOrder)) {
            Collections.reverse(fieldsAndCounts);
        }
        // Calculate the total count of the fields in fieldsAndCounts
        int totalFieldCount = fieldsAndCounts.size();
        long totalPages = (totalFieldCount + pageSize - 1) / pageSize;
        // Prepare and return the result document
        Document resultDocument = new Document();
        resultDocument.put("fieldsAndUniqueCounts", fieldsAndCounts);
        resultDocument.put("displayReportDate", newReportDate);
        resultDocument.put("totalCount", totalFieldCount); // Adding the total count of fieldsAndCounts
        resultDocument.put("totalPage", totalPages);
        return resultDocument;
    }


    private List<String> extractFieldNames(Object document, String excludeFieldsKey) {
        List<String> fieldNames = new ArrayList<>();
        Field[] fields = document.getClass().getDeclaredFields();
        // Set of field names to be excluded
        List<String> excludedFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/tradeUdf.json", excludeFieldsKey);
        // Collect field names, excluding the ones in the set
        for (Field field : fields) {
            String fieldName = field.getName();
            if (!excludedFields.contains(fieldName)) {
                fieldNames.add(fieldName);
            }
        }
        return fieldNames;
    }


    @SneakyThrows
    public <T> Document getTradeUdfFieldValueCount(String date, String tradeUdfFieldName, String sortBy, String sortingOrder, int pageSize, String indexName, Class<T> clazz, String userName, String integerFieldsKey, String doubleFieldkey) throws IOException, ParseException {
        Document tradeUdfFieldValueCount = new Document();
        // Get integer and double field lists from JSON
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/tradeUdf.json", integerFieldsKey);
        List<String> doubleFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/tradeUdf.json", doubleFieldkey);
        // Create TermQuery to filter by reportDate
        Query reportDate = TermQuery.of(q -> q.field("reportDate").value(date))._toQuery();
        // Handle UDF field logic
        if (tradeUdfFieldName != null && !tradeUdfFieldName.isEmpty()) {
            Map<Object, Long> uniqueFieldValues = new TreeMap<>();
            NamedValue<SortOrder> sort = NamedValue.of("_key", SortOrder.Desc);
            String fieldName;
            // Determine the appropriate field name based on the tradeUdfFieldName
            if (doubleFields.contains(tradeUdfFieldName)) {
                fieldName = tradeUdfFieldName;
            } else if (integerFields.contains(tradeUdfFieldName)) {
                fieldName = tradeUdfFieldName;
            } else {
                fieldName = tradeUdfFieldName + ".displaycolumnval";
            }
            // Create an aggregation to get the unique values of the field
            Aggregation aggregation = Aggregation.of(a ->
                    a.terms(TermsAggregation.of(terms ->
                            terms.field(fieldName).size(Integer.MAX_VALUE).order(List.of(sort)))));
            // Build the search request
            SearchRequest searchRequest = SearchRequest.of(s ->
                    s.index(indexName)
                            .query(reportDate)
                            .aggregations("TradeUdfField", aggregation));
            log.info("TradeUdfField Request: {}", searchRequest);
            // Execute the search request with the generic class
            SearchResponse<T> searchResponse = esClient.search(searchRequest, clazz);
            LinkedHashMap<String, Long> tradeUdfFieldValue = new LinkedHashMap<>();
            // Process the aggregation results
            if (searchResponse.aggregations() != null && searchResponse.aggregations().get("TradeUdfField") != null) {
                if (searchResponse.aggregations().get("TradeUdfField").isSterms()) {
                    searchResponse.aggregations().get("TradeUdfField").sterms().buckets().array().forEach(b ->
                            uniqueFieldValues.put(b.key().stringValue(), b.docCount()));
                } else if (searchResponse.aggregations().get("TradeUdfField").isLterms()) {
                    searchResponse.aggregations().get("TradeUdfField").lterms().buckets().array().forEach(b ->
                            uniqueFieldValues.put(b.key(), b.docCount()));
                } else if (searchResponse.aggregations().get("TradeUdfField").isDterms()) {
                    searchResponse.aggregations().get("TradeUdfField").dterms().buckets().array().forEach(b -> {
                        // Use the roundToOriginalPrecision method for dynamic rounding
                        String roundedValue = roundToOriginalPrecision(b.key());
                        uniqueFieldValues.put(roundedValue, b.docCount());
                    });
                }
            }
            // Process the unique field values into a result document
            List<Object> keyList = new ArrayList<>(uniqueFieldValues.keySet());
            List<Long> countList = new ArrayList<>(uniqueFieldValues.values());
            List<Document> resultDocuments = elastic_utils.checkDuplicateObjects(countList, keyList);
            // Sorting logic based on sortBy and sortingOrder
            if ("fieldValue".equalsIgnoreCase(sortBy)) {
                // Check if all field values can be parsed as BigDecimal (for numbers)
                boolean isNumeric = resultDocuments.stream()
                        .map(doc -> doc.get("name").toString())
                        .allMatch(value -> {
                            try {
                                new BigDecimal(value); // Try parsing the value as BigDecimal
                                return true; // It's numeric
                            } catch (NumberFormatException e) {
                                return false; // It's not numeric
                            }
                        });

                if (isNumeric) {
                    // Sort numerically using BigDecimal comparison
                    resultDocuments.sort((doc1, doc2) -> {
                        BigDecimal num1 = new BigDecimal(doc1.get("name").toString());
                        BigDecimal num2 = new BigDecimal(doc2.get("name").toString());
                        return num1.compareTo(num2);
                    });
                } else {
                    // Sort lexicographically (case-insensitive)
                    resultDocuments.sort(Comparator.comparing(doc -> ((String) doc.get("name")).toLowerCase()));
                }
            } else if ("count".equalsIgnoreCase(sortBy)) {
                // Sort by count (value)
                resultDocuments.sort(Comparator.comparingLong(doc -> (Long) doc.get("value")));
            }
            // Handle the sorting order
            if ("desc".equalsIgnoreCase(sortingOrder)) {
                Collections.reverse(resultDocuments);
            }
            for (Document doc : resultDocuments) {
                tradeUdfFieldValue.put((String) doc.get("name"), (Long) doc.get("value"));
            }
            // Format the tradeUdfFieldValue for the response
            List<LinkedHashMap<Object, Object>> newTradeUdfFields = new ArrayList<>();
            for (Map.Entry<String, Long> entry : tradeUdfFieldValue.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                String fieldValue = entry.getKey();
//                if (fieldValue.endsWith(".0")) {
//                    fieldValue = fieldValue.substring(0, fieldValue.length() - 2);
//                }
                if (fieldValue.isEmpty()) {
                    fieldValue = "Nil - (Blank)";
                }
                newFormat.put("fieldValue", fieldValue);
                newFormat.put("count", entry.getValue());
                newTradeUdfFields.add(newFormat);
            }
            int totalFieldCount = newTradeUdfFields.size();
            long totalPages = (totalFieldCount + pageSize - 1) / pageSize;
            tradeUdfFieldValueCount.put("tradeUdfFieldValueCount", newTradeUdfFields);
            tradeUdfFieldValueCount.put("totalCount", totalFieldCount);
            tradeUdfFieldValueCount.put("totalPage", totalPages);
        }
        return tradeUdfFieldValueCount;
    }


    public static String roundToOriginalPrecision(double value) {
        // Convert the double to a BigDecimal
        BigDecimal bigDecimal = new BigDecimal(Double.toString(value));
        // Get the string representation of the BigDecimal
        String valueAsString = bigDecimal.stripTrailingZeros().toPlainString();
        // Determine the number of decimal places
        int decimalPlaces = 0;
        if (valueAsString.contains(".")) {
            decimalPlaces = valueAsString.length() - valueAsString.indexOf('.') - 1;
        }
        // Round to the exact number of decimal places
        return bigDecimal.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString();
    }


    @SneakyThrows
    public <T> Integer getUniqueFieldCount(String filterColumnName, List<Query> queryList, String indexName, Class<T> documentClass, String integerFieldsKey, String doubleFieldskey) {
        Integer count = 0;
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/tradeUdf.json", integerFieldsKey);
        List<String> doubleFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/tradeUdf.json", doubleFieldskey);
        // Determine the appropriate field type and keyword suffix
        String fieldToAggregate = filterColumnName;
        if (doubleFields.contains(filterColumnName)) {
            Aggregation agg = Aggregation.of(a -> a.cardinality(c -> c.field(fieldToAggregate)));
            aggregation.put("unique_categories", agg);
        } else if (integerFields.contains(filterColumnName)) {
            Aggregation agg = Aggregation.of(a -> a.cardinality(c -> c.field(fieldToAggregate)));
            aggregation.put("unique_categories", agg);
        } else {
            // Use ".keyword" for other fields
            Aggregation agg = Aggregation.of(a -> a.cardinality(c -> c.field(fieldToAggregate + ".keyword")));
            aggregation.put("unique_categories", agg);
        }
        Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .query(searchQuery)
                .index(indexName)
                .aggregations(aggregation));
        log.info("getUniqueFieldCount - Search request :{}", searchRequest);
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


    @Async
    @Transactional
    public CompletableFuture<Void> getTradeUdfHeadersExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String color, List<String> fieldColumns, String indexName, Class<?> entityClass, String name,
                                                            String excludeFieldsKey, String integerFieldsKey, String doubleFieldskey, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, int pageSize, ElasticSearchModel elasticSearchModel) throws Exception {
        List<Object> tradeUdfList = new ArrayList<>();
        elasticSearchModel.fieldColumns = new ArrayList<>();
        long totalCount = 0;
        long totalPage = 1;
        log.info("Trade UDF Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info("Trade UDF Export: Starting fetch.");
        for (int i = 0; i < totalPage; i++) {
            // Fetch the trade UDF data for the current page
            Document searchTradeUdf = getTradeUdfHeaders(reportDate, sortBy, sortingOrder, pageSize, indexName, entityClass, name,
                    excludeFieldsKey, integerFieldsKey, doubleFieldskey);
            // Accumulate the trade UDF data
            tradeUdfList.addAll((Collection<?>) searchTradeUdf.get("fieldsAndUniqueCounts"));
            // Update job progress
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, tradeUdfList.size(), job.getId());
            // Fetch total pages and count for pagination
            totalPage = (long) searchTradeUdf.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (int) searchTradeUdf.get("totalCount");
            log.info("TOTAL COUNT {}", totalCount);

        }
        log.info("Trade UDF Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", tradeUdfList.size(), (int) totalCount, job.getId());
        // Write the fetched data to the file (CSV/XLS/XLSX/XLSB)
        return elastic_utils.exportFileFromDetails(outputFormat, tradeUdfList, fileName, fieldMaps, color, job, fieldColumns, elasticSearchModel);
    }


    @Async
    @Transactional
    public CompletableFuture<Void> getTradeUdfFieldValueExport(String reportDate, String tradeUdfFieldName, String fileName, List<FieldMap> fieldMaps, String color, List<String> fieldColumns, String indexName, Class<?> entityClass, String name,
                                                               String integerFieldsKey, String doubleFieldskey, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, int pageSize, ElasticSearchModel elasticSearchModel) throws Exception {
        List<Object> tradeUdfList = new ArrayList<>();
        elasticSearchModel.fieldColumns = new ArrayList<>();
        long totalCount = 0;
        long totalPage = 1;
        log.info("Trade UDF Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info("Trade UDF Export: Starting fetch.");
        for (int i = 0; i < totalPage; i++) {
            // Fetch the trade UDF data for the current page
            Document searchTradeUdf = getTradeUdfFieldValueCount(reportDate, tradeUdfFieldName, sortBy, sortingOrder, pageSize, indexName, entityClass, name,
                    integerFieldsKey, doubleFieldskey);
            // Accumulate the trade UDF data
            tradeUdfList.addAll((Collection<?>) searchTradeUdf.get("tradeUdfFieldValueCount"));
            // Update job progress
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, tradeUdfList.size(), job.getId());
            // Fetch total pages and count for pagination
            totalPage = (long) searchTradeUdf.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (int) searchTradeUdf.get("totalCount");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info("Trade UDF Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", tradeUdfList.size(), (int) totalCount, job.getId());
        // Write the fetched data to the file (CSV/XLS/XLSX/XLSB)
        return elastic_utils.exportFileFromDetails(outputFormat, tradeUdfList, fileName, fieldMaps, color, job, fieldColumns, elasticSearchModel
        );
    }
}
