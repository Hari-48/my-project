package com.finsurge.tmr_portal.mx_superview.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.elasticsearch.sql.TranslateRequest;
import co.elastic.clients.elasticsearch.sql.TranslateResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
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
import lombok.Data;
import lombok.SneakyThrows;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

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

@Service
@Data
public class ElasticUtils {

    private static final Logger log = LoggerFactory.getLogger(ElasticUtils.class);

    @Autowired
    ElasticSearchUtils elasticSearchUtils;

    @Autowired
    private final ElasticsearchClient esClient;


    @Value("${uam.blank.fields}")
    private String blankFields;

    public final Environment environment;

    @Autowired
    private DownloadJobService downloadJobService;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    Map<String, Instant> pitIdCreationTimes = new ConcurrentHashMap<>();
//
////     GlobalSearch -  (searchTerm,actualSearchTerm)
//    public Query getGlobalSearchTerm(String searchTerm, List<String> fields, List<String> newDateFields, List<String> globalSearchFields) {
//        Query finalQuery = null;
//        if (!searchTerm.contains("AND ") || (!searchTerm.contains("OR"))) {
//            if (isDateField(searchTerm)) {
//                Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(newDateFields))._toQuery();
//                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
//            } else if (isNumericField(searchTerm)) {
//                Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(globalSearchFields))._toQuery();
//                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
//            } else if (globalSearchFields(searchTerm)) {
//                Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(globalSearchFields))._toQuery();
//                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
//            } else {
//                Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(fields))._toQuery();
//                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
//            }
//        } else {
//            Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(fields))._toQuery();
//            finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
//        }
//        log.info("Search term Query :- {}", finalQuery);
//        return finalQuery;
//    }

    // GlobalSearch -  (searchTerm,actualSearchTerm)
    public Query getGlobalSearchTerm(String searchTerm, List<String> fields, List<String> newDateFields, List<String> globalSearchFields) {
        Query finalQuery = null;
        String wildcardSearchTerm;
        log.info("searchTerm -----:{}",searchTerm);

        if (!searchTerm.contains("AND ") && !searchTerm.contains("OR ")) {
            // Check if the search term is enclosed in double quotes
            if (searchTerm.startsWith("\"") && searchTerm.endsWith("\"")) {
                // Exact match: keep the surrounding quotes and do not apply wildcard
                wildcardSearchTerm = searchTerm;
                log.info("The Quoted Search term is : \\\"{}\\\"", wildcardSearchTerm);
            } else {
                // Determine if the search term is a single word or a phrase
                if (searchTerm.contains(" ") && !searchTerm.matches(".*[~`!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/].*")) {
                    // It's a phrase: apply wildcard and use AND to combine the terms
                    String[] terms = searchTerm.split("\\s+");
                    StringBuilder searchBuilder = new StringBuilder();

                    for (String term : terms) {
                        searchBuilder.append(term).append("* OR ");
                    }
                    // Remove the trailing 'AND' and trim any excess space
                    wildcardSearchTerm = searchBuilder.toString().replaceAll(" OR $", "").trim();
                } else if (searchTerm.matches(".*[~`!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?/].*")) {
                    // Special characters present: wrap with quotes and add wildcard
                    wildcardSearchTerm = "\"" + searchTerm + "\"";

                    log.info("The Special Character Search term is : \\\"{}\\\"", wildcardSearchTerm);
                } else if (searchTerm.matches("^[1-9]\\d*$")) {
                    wildcardSearchTerm=searchTerm;
                } else {
                    // It's a single term: apply a wildcard to the single term
                    wildcardSearchTerm = searchTerm + "*";
                }
            }
            log.info("The Wild card Search Term is : {} ", wildcardSearchTerm);

            if (isDateField(wildcardSearchTerm)) {
                Query newSearchTerm = QueryStringQuery.of(q -> q.query(wildcardSearchTerm).fields(newDateFields))._toQuery();
                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
            } else if (isNumericField(wildcardSearchTerm)) {
                Query newSearchTerm = QueryStringQuery.of(q -> q.query(wildcardSearchTerm).fields(globalSearchFields))._toQuery();
                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
            } else if (globalSearchFields(wildcardSearchTerm)) {
                Query newSearchTerm = QueryStringQuery.of(q -> q.query(wildcardSearchTerm).fields(globalSearchFields))._toQuery();
                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
            } else {
                Query newSearchTerm = QueryStringQuery.of(q -> q.query(wildcardSearchTerm).fields(fields))._toQuery();
                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
            }
        } else {
            Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(fields))._toQuery();
            finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
        }
        log.info("Search term Query :- {}", finalQuery);
        return finalQuery;
    }

    // MySQL Search  -(searchTerm,actualSearchTerm)
    @SneakyThrows(IOException.class)
    public Query getTranslateQuery(String searchTerm, String indexName) {
        String sqlQuery = "SELECT ";
        sqlQuery += "* ";
        sqlQuery += "FROM " + indexName;
        sqlQuery += " t WHERE ";
        sqlQuery += searchTerm;
        String finalSqlQuery = sqlQuery;
        TranslateResponse translateResponse = esClient.sql().translate(TranslateRequest.of(t -> t.query(finalSqlQuery)));
        Query query = translateResponse.query();
        log.info(" MY SQL to Elastic Query :-{}", query);
        return query;
    }

    //Bulk Filter Query  - (searchTerm,actualSearchTerm)
    public Query getBulkFilterQuery(String columnName, List<String> integerFields, List<String> columnValue) {
        var array = new ArrayList<FieldValue>();
        Query termsQuery;
        Query bulkWildcardQuery;
        List<Query> bulkWildCardQueries = new ArrayList<>();
        if (!integerFields.contains(columnName)) {
            for (String newData : columnValue) {
                String filterValue = "*" + FieldValue.of(newData).stringValue() + "*";
                bulkWildcardQuery = WildcardQuery.of(w ->
                        w.field(columnName + ".keyword").value(filterValue))._toQuery();
                bulkWildCardQueries.add(bulkWildcardQuery);
            }
            termsQuery = BoolQuery.of(q -> q.should(bulkWildCardQueries))._toQuery();
        } else {
            for (String newData : columnValue) {
                array.add(FieldValue.of(newData));
            }
            termsQuery = TermsQuery.of(ts -> ts
                    .field(columnName)
                    .terms(TermsQueryField.of(t -> t.value(array))))._toQuery();
        }
        log.info("BulkFilter Query - columnName  :-{}", termsQuery);
        return termsQuery;
    }

    //Bulk Filter Query  - (searchTerm,actualSearchTerm)
    public Query getBulkFilterQuery(String columnName, List<String> integerFields, List<String> columnValue, String bulkFilterSearchType) {
        var array = new ArrayList<FieldValue>();
        List<Query> bulkWildCardQueries = new ArrayList<>();
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (!integerFields.contains(columnName)) {
            // Check if the bulkFilterSearchType is 'partial'
            if ("partial".equalsIgnoreCase(bulkFilterSearchType)) {
                // Perform wildcard query with *
                for (String newData : columnValue) {
                    String filterValue = "*" + FieldValue.of(newData).stringValue() + "*";
                    Query bulkWildcardQuery = WildcardQuery.of(w ->
                            w.field(columnName + ".keyword").value(filterValue))._toQuery();
                    bulkWildCardQueries.add(bulkWildcardQuery);
                }
            } else {
                log.info("Entering into the exact part ..........................................");
                // Perform Query String query for exact match with AND operator for comma-separated values
                for (String newData : columnValue) {
                    // Remove commas and replace them with ' AND ' (keeping the spaces around AND)
                    String formattedValue = newData.replace(",", " AND");
                    // No need for surrounding quotes here as query string handles it automatically
                    String queryString = formattedValue;

//                  Build the QueryString query
                    Query queryStringQuery = QueryStringQuery.of(qs -> qs
                            .fields(columnName + ".keyword")
                            .query(queryString)
                    )._toQuery();

                    bulkWildCardQueries.add(queryStringQuery);
                }
            }

        } else {
            for (String newData : columnValue) {
                array.add(FieldValue.of(newData));
            }
            Query termsQuery = TermsQuery.of(ts -> ts
                    .field(columnName)
                    .terms(TermsQueryField.of(t -> t.value(array))))._toQuery();
            bulkWildCardQueries.add(termsQuery);
        }
        // Add all wildcard/terms queries to the BoolQuery
        boolQueryBuilder.should(bulkWildCardQueries);

        Query finalQuery = boolQueryBuilder.build()._toQuery();
        log.info("BulkFilter Query - columnName  :-{}", finalQuery);
        return finalQuery;
    }


    // DropDown  List - AlphaNumeric(alphaNumericFilterColumn,filterColumnName)
    public Query getAlphaNumericQuery(String alphaNumericColumn) {
        String script = "def alphaValue = doc['" + alphaNumericColumn + ".keyword'].value; " +
                "return alphaValue != null && alphaValue =~ /[a-zA-Z]+/;";
        Query scriptQuery = ScriptQuery.of(s -> s.script(
                Script.of(scr -> scr.inline(
                        InlineScript.of(i -> i.source(script))))))._toQuery();
        log.info("ScriptQuery - FilterColumnAlphaNumeric :- {}", scriptQuery);
        return scriptQuery;
    }

    // DropDown List  - Numeric(numericFilterColumn,filterColumnName)
    public Query getNumericQuery(String numericColumn) {
        String script = "def alphaValue = doc['" + numericColumn + ".keyword'].value; " +
                "return alphaValue != null && alphaValue =~ /^[^\\p{L}]+$/;";
        Query scriptQuery = ScriptQuery.of(s ->
                s.script(Script.of(scr ->
                        scr.inline(InlineScript.of(i ->
                                i.source(script))))))._toQuery();
        log.info("ScriptQuery - FilterColumnNumeric :- {}", scriptQuery);
        return scriptQuery;
    }

    // Listing DropDown - Shows the field Count - (filterColumnName)
    public Aggregation getFilterColumn(String filterColumnName, List<String> integerFields, int pageSize) {
        String filterColumnVal;
        if (!integerFields.contains(filterColumnName)) {
            filterColumnVal = filterColumnName + ".displaycolumnval";
        } else {
            filterColumnVal = filterColumnName;
        }
        NamedValue<SortOrder> sort = NamedValue.of("_key", SortOrder.Asc);
        Aggregation filterColumns = Aggregation.of(a ->
                a.terms(TermsAggregation.of(ta ->
                        ta.field(filterColumnVal).order(List.of(sort)).size(pageSize).missing(""))));
        log.info("FilterColumn Aggregation :{}", filterColumns);
        return filterColumns;
    }

    public Query getNumericScriptQuery(String filterColumnName, List<String> integerFields) {
        String filterColumnVal;
        if (!integerFields.contains(filterColumnName)) {
            filterColumnVal = filterColumnName + ".displaycolumnval";
        } else {
            filterColumnVal = filterColumnName;
        }

        String script = "if (doc['" + filterColumnVal + "'].size() > 0) { " +
                "  try { " +
                "    Integer.parseInt(doc['" + filterColumnVal + "'].value); " +
                "  } catch (NumberFormatException e) { " +
                "    return -1 ; " +  // Return -1 for non-numeric values
                "  } " +
                "}";
        Query numericQuery = ScriptQuery.of(s ->
                s.script(Script.of(scr ->
                        scr.inline(InlineScript.of(i ->
                                i.source(script))))))._toQuery();

        log.info("Numeric Script Query - DropDown :{}", numericQuery);
        return numericQuery;
    }

    public Query getAlphaNumericScriptQuery(String filterColumnName, List<String> integerFields) {
        String filterColumnVal;
        if (!integerFields.contains(filterColumnName)) {
            filterColumnVal = filterColumnName + ".displaycolumnval";
        } else {
            filterColumnVal = filterColumnName;
        }
        String script = "if (doc['" + filterColumnVal + "'].size() > 0) { " +
                "  def value = doc['" + filterColumnVal + "'].value; " +
                "  if (value =~ /^[0-9]+$/) { " +  // Regex to match numeric values
                "    return Integer.parseInt(value); " +
                "  } else { " +
                "    return value; " +  // Return non-numeric alpha-numeric values
                "  } " +
                "} else { " +
                "  return null; " +  // Return null for missing values
                "}";
        Query alphaNumericQuery = ScriptQuery.of(s ->
                s.script(Script.of(scr ->
                        scr.inline(InlineScript.of(i ->
                                i.source(script))))))._toQuery();

        log.info(" Alpha Numeric Script Query - DropDown :{}", alphaNumericQuery);
        return alphaNumericQuery;
    }


    // Inside DropDown Searching
    public Query getRangeQuery(String filterValue, String filterColumnName) {
        String[] newValue = filterValue.split("\\.\\.");

        Query rangeQuery = RangeQuery.of(r ->
                r.field(filterColumnName + ".keyword").from(newValue[0]).to(newValue[1]))._toQuery();
        log.info("Range Query - filterValue (..):-{}", rangeQuery);
        return rangeQuery;
    }

    public Query getRangeQuery(String filterValue, String filterColumnName, List<String> integerFields) {
        String[] newValue = filterValue.split("\\.\\.");
        Query rangeQuery;
        if (integerFields.contains(filterColumnName)) {
            rangeQuery = RangeQuery.of(r ->
                    r.field(filterColumnName ).from(newValue[0]).to(newValue[1]))._toQuery();
        } else {
            rangeQuery = RangeQuery.of(r ->
                    r.field(filterColumnName + ".keyword").from(newValue[0]).to(newValue[1]))._toQuery();
        }
        log.info("Range Query - filterValue (..):-{}", rangeQuery);
        return rangeQuery;
    }


    // Inside DropDown Searching
    public Query getNilQuery(String filterColumnName) {
        String filterValue = "";
        String finalFilterValue1 = filterValue;
        Query newTerm = TermQuery.of(t -> t.field(filterColumnName + ".keyword").value(finalFilterValue1))._toQuery();
        return newTerm;
    }

    // Inside DropDown Searching
    public Query getWildcardQuery(List<String> integerFields, String filterColumnName, String filterValue) {
        String finalFilterValue;
        Query wildcardQuery;
        if (!integerFields.contains(filterColumnName)) {
            filterValue = "*" + filterValue + "*";
            finalFilterValue = filterValue;
            wildcardQuery = WildcardQuery.of(w ->
                    w.field(filterColumnName + ".keyword").value(finalFilterValue))._toQuery();
        } else {
            finalFilterValue = filterValue;
            wildcardQuery = TermQuery.of(w -> w.field(filterColumnName).value(finalFilterValue))._toQuery();
        }
        log.info("Wild" +
                "card Query - filterValue (..):-{}", wildcardQuery);
        return wildcardQuery;
    }


    public Aggregation getFilterColumnAgg(List<String> integerFields, String filterColumnName, ElasticSearchModel elasticSearchModel, Aggregation filterColumns) {
        String filterColumnVal;
        if (!integerFields.contains(filterColumnName)) {
            filterColumnVal = filterColumnName + ".displaycolumnval";
        } else {
            filterColumnVal = filterColumnName;
        }
        log.info("preserved value in filter search  column : {} ", elasticSearchModel.getFilterSearch().get(filterColumnName));
        var array = new ArrayList<FieldValue>();
        for (String newData : elasticSearchModel.getFilterSearch().get(filterColumnName)) {
            array.add(FieldValue.of(newData));
        }
        TermsQuery termsQueryBuilder = TermsQuery.of(t -> t.field(filterColumnVal).terms(TermsQueryField.of(tf -> tf.value(array))));
        Aggregation filterSearchAggregation = Aggregation.of(a -> a.filter(termsQueryBuilder._toQuery()).aggregations("FilterColumnAggValue", filterColumns));
        return filterSearchAggregation;
    }

    public SortOptions getSortOptions(List<String> integerFields, String sortBy, String sortingOrder) {
        if (!integerFields.contains(sortBy)) {
            sortBy = sortBy + ".keyword";
        }
        String finalSortBy = sortBy;
        log.info("getSortOptions method final Sort By :{}", finalSortBy);
        SortOptions sortOptions1 = SortOptions.of(s ->
                s.field(FieldSort.of(fs -> fs.field(finalSortBy).order(sortingOrder.equalsIgnoreCase("asc")
                        ? SortOrder.Asc : SortOrder.Desc))));
        log.info("getSortOptions : ! {}", sortOptions1);
        return sortOptions1;
    }

    public List<Object> getFirstSortingOrder(SearchResponse<?> searchResponse, List<String> integerFields, String sortBy) {
        List<Object> sorting = new ArrayList<>();
        Hit<?> firstHit = searchResponse.hits().hits().get(0);
        List<FieldValue> firstHitSortValues = firstHit.sort();
        if (!firstHitSortValues.isEmpty()) {
            if (!integerFields.contains(sortBy)) {
                sorting.add(firstHitSortValues.get(0).stringValue());
            } else {
                sorting.add(firstHitSortValues.get(0).longValue());
            }
            if (firstHitSortValues.size() > 1) {
                sorting.add(firstHitSortValues.get(1).stringValue());
            }
            if (firstHitSortValues.size() > 2) {
                sorting.add(firstHitSortValues.get(2).longValue());
            }
        }
        return sorting;
    }

    public List<Object> getLastSortingOrder(SearchResponse<?> searchResponse, List<String> integerFields, String sortBy) {
        List<Object> sorting = new ArrayList<>();
        Hit<?> lastHit = searchResponse.hits().hits().get(searchResponse.hits().hits().size() - 1);
        List<FieldValue> lastHitSortValues = lastHit.sort();
        if (!lastHitSortValues.isEmpty()) {
            if (!integerFields.contains(sortBy)) {
                sorting.add(lastHitSortValues.get(0).stringValue());
            } else {
                sorting.add(lastHitSortValues.get(0).longValue());
            }
            if (lastHitSortValues.size() > 1) {
                sorting.add(lastHitSortValues.get(1).stringValue());
            }
            if (lastHitSortValues.size() > 2) {
                sorting.add(lastHitSortValues.get(2).longValue());
            }
        }
        return sorting;
    }

    public void getBoolQuery(Map<String, List<String>> filterSearchMap, ElasticSearchModel elasticSearchModel, List<String> integerFields, List<Query> queryList) {
        for (Map.Entry<String, List<String>> entry : filterSearchMap.entrySet()) {
            String fieldName = entry.getKey();
            List<String> fieldValues = entry.getValue();
            if (fieldValues != null && !fieldValues.isEmpty()) {
                if (!fieldName.equalsIgnoreCase(elasticSearchModel.getFilterColumnName())) {
                    if (!integerFields.contains(fieldName)) {
                        getBoolQuery(fieldName + ".keyword", fieldValues, queryList);
                    } else {
                        getBoolQuery(fieldName, fieldValues, queryList);
                    }
                }
            }
        }
    }

    public Map<Object, Long> extractFilterColumnAgg(SearchResponse<?> searchResponse) {
        Map<Object, Long> filterColumnValues = new TreeMap<>();
        if (searchResponse.aggregations() != null && searchResponse.aggregations().get("FilterColumnAgg") != null) {
            if (searchResponse.aggregations().get("FilterColumnAgg").isFilter()) {
                if (searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue") != null) {
                    if (searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue").isSterms()) {
                        searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue").sterms()
                                .buckets().array().forEach(b -> filterColumnValues.put(b.key().stringValue(), b.docCount()));
                    } else if (searchResponse.aggregations().get("FilterColumn").isLterms()) {
                        searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue").lterms()
                                .buckets().array().forEach(b -> filterColumnValues.put(b.key(), b.docCount()));
                    }
                }
            }
        }
        return filterColumnValues;
    }

    public Map<Object, Long> extractFilterColumnAggregation(SearchResponse<?> searchResponse) {
        Map<Object, Long> uniqueFilterValues = new TreeMap<>();
        if (searchResponse.aggregations() != null && searchResponse.aggregations().get("FilterColumn") != null) {
            if (searchResponse.aggregations().get("FilterColumn").isSterms()) {
                searchResponse.aggregations().get("FilterColumn").sterms().buckets().array().forEach(b -> uniqueFilterValues.put(
                        b.key().stringValue(), b.docCount()));
            } else if (searchResponse.aggregations().get("FilterColumn").isLterms()) {
                searchResponse.aggregations().get("FilterColumn").lterms().buckets().array().forEach(b -> uniqueFilterValues.put(
                        b.key(), b.docCount()));
            }
        }
        return uniqueFilterValues;
    }

    public boolean isPitIdExpired(String pitId) {
        Instant creationTime = pitIdCreationTimes.getOrDefault(pitId, Instant.MIN);
        Instant currentTime = Instant.now();
        Duration timeElapsed = Duration.between(creationTime, currentTime);
        String keepAliveTimeStr = environment.getProperty("keepAliveTime.pit");
        Duration keepAliveTime = Duration.parse("PT" + keepAliveTimeStr.toUpperCase());
        return timeElapsed.compareTo(keepAliveTime) < 0;
    }

    // pagination - creating pitId
    @SneakyThrows(IOException.class)
    public String createPitId(String indexName) {
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

    @SneakyThrows({IOException.class, ParseException.class})
    public List<String> getDataFromJsonFile(String path, String key) {
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

    @SneakyThrows(IOException.class)
    public Long getNumericCount(String filterColumn, List<Query> queryList, String indexName) {
        String script = "def alphaValue = doc['" + filterColumn + ".keyword'].size() > 0 ? doc['" + filterColumn + ".keyword'].value : null; " +
                "return alphaValue != null && alphaValue =~ /^[^\\p{L}]+$/;";
        Query numericScriptQuery = ScriptQuery.of(s -> s.script(
                        Script.of(scr -> scr.inline(
                                InlineScript.of(i -> i.source(script))))))
                ._toQuery();
        List<Query> scriptQuery = new ArrayList<>();
        scriptQuery.add(numericScriptQuery);
        Query searchQuery = BoolQuery.of(q -> q.must(queryList).filter(scriptQuery))._toQuery();
        CountRequest countRequest = CountRequest.of(cr -> cr.index(indexName)
                .query(searchQuery));
        log.info("Numeric CountRequest : {}", countRequest);
        long totalCount = esClient.count(countRequest).count();
        return totalCount;
    }

    @SneakyThrows(Exception.class)
    public Long getAlphaNumericCount(String filterColumn, List<Query> queryList, String indexName) {
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
        log.info("Alpha Numeric CountRequest : {}", countRequest);
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
    @SneakyThrows(IOException.class)
    public void deleteData(String dateValue, String reportName) {
        //Setting report name according to .properties file
        String reportNameIndex = reportName + ".search";
        //Checks count
        if (getCountByDate(dateValue, reportNameIndex) > 0) {
            log.info("Count : {}", getCountByDate(dateValue, reportNameIndex));
            Query query = TermQuery.of(t -> t.field("reportDate").value(dateValue))._toQuery();
            esClient.deleteByQuery(DeleteByQueryRequest.of(d -> d.index(environment.getProperty(reportNameIndex)).query(query)));
        }
    }

    @SneakyThrows(IOException.class)
    public Long getCountByDate(String dateValue, String reportNameIndex) {
        Query query = TermQuery.of(t -> t.field("reportDate").value(dateValue))._toQuery();
        CountRequest countRequest = CountRequest.of(c -> c.index(environment.getProperty(reportNameIndex)).query(query));
        return esClient.count(countRequest).count();
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
            // Fetch the existing SearchHistory if it exists
            SearchHistory searchHistory = searchHistoryRepository.getTopByUserNameAndReportTypeAndSearchQuery(userName, reportType, whereCondition);

            if (searchHistory != null) {
                // If the search term exists, update the timestamp
                searchHistory.setSystemTimestamp(LocalDateTime.now());
                searchHistoryRepository.save(searchHistory);
            } else {
                // If the search term does not exist, create a new entry
                SearchHistory history = new SearchHistory();
                history.setUserName(userName);
                history.setReportType(reportType);
                history.setSearchQuery(whereCondition);
                searchHistoryRepository.save(history);
            }
        });
    }

    @SneakyThrows(Exception.class)
    public CompletableFuture<Void> exportFileFromDetails(String outputFormat, List<?> exportData, String fileName, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, ElasticSearchModel elasticSearchModel) {
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

    @SneakyThrows(JsonProcessingException.class)

    public List<Map<String, Object>> prepareMapListToExport(List<?> list) {
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
                                } else {
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
                                        } else {
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

    private static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
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

    @SneakyThrows(Exception.class)
    public File exportAsXlsxFromList(List<Map<String, Object>> list, File file, List<FieldMap> fieldMaps, String color, DownloadJob job, List<String> fieldColumns, ElasticSearchModel elasticSearchModel) {
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
                                } else {
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
                                        } else {
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
            }
            case "UAM_MX_USER_POLICY": {
                indexName = UserPolicy.INDEX_NAME;
                break;
            }
            case "UAM_MX_GROUP_PORTFOLIO_RIGHTS": {
                indexName = GroupPortfolioRights.INDEX_NAME;
                break;
            }
            case "UAM_MX_CHINESE_WALL_TMPL": {
                indexName = ChineseWall.INDEX_NAME;
                break;
            }
            case "UAM_MX_GROUP_NAV_RIGHTS": {
                indexName = GroupNavigationRights.INDEX_NAME;
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

    @SneakyThrows(Exception.class)
    public String deleteData(String tableName, LocalDate reportDate) {
        String indexName = switch (Objects.requireNonNull(tableName)) {
            case "UAM_MX_COUNTER_PARTY" -> CounterPartyDocument.INDEX_NAME;
            case "UAM_MX_DORMANT_COUNTERPARTY" -> DormantCounterparty.INDEX_NAME;
            case "UAM_MX_CLOSING_ENTITY" -> ClosingEntity.INDEX_NAME;
            case "UAM_MX_COUNTERPARTY_CREATION" -> CounterpartyCreation.INDEX_NAME;
            case "UAM_MX_USER_LIST" -> UserList.INDEX_NAME;
            case "UAM_MX_GROUP_LIST" -> UserGroupList.INDEX_NAME;
            case "UAM_MX_USER_LICENSE" -> UserLicense.INDEX_NAME;
            case "UAM_USER_GROUP_ACCESS_RIGHT" -> UserGroupAccessRights.INDEX_NAME;
            case "UAM_MX_USER_POLICY" -> UserPolicy.INDEX_NAME;
            case "UAM_MX_GROUP_PORTFOLIO_RIGHTS" -> GroupPortfolioRights.INDEX_NAME;
            case "UAM_MX_CHINESE_WALL_TMPL" -> ChineseWall.INDEX_NAME;
            case "UAM_MX_GROUP_NAV_RIGHTS" -> GroupNavigationRights.INDEX_NAME;
            default -> null;
        };
        Query deleteQuery = TermQuery.of(t -> t.field("reportDate").value(String.valueOf(reportDate)))._toQuery();
        String finalIndexName = indexName;
        DeleteByQueryResponse deleteByQueryResponse = esClient.deleteByQuery(DeleteByQueryRequest.of(d -> d.index(finalIndexName).query(deleteQuery)));
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

    public Map<Object, Object> getGroupLabels(String fetchUserGroupFieldName, String fieldValue, String reportDate) {
        Map<Object, Object> groupLabelMap = new HashMap<>();

        List<String> activeGroups = new ArrayList<>();
        List<String> inActiveGroups = new ArrayList<>();

        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query fieldQuery = TermQuery.of(t -> t.field(fetchUserGroupFieldName + ".keyword").value(fieldValue))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(fieldQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)).index(UserGroupList.INDEX_NAME));

        SearchResponse<UserGroupList> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, UserGroupList.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Hit<UserGroupList> hit : searchResponse.hits().hits()) {
            String groupLabel = Objects.requireNonNull(hit.source()).getGroupLabel();
            String status = Objects.requireNonNull(hit.source()).getStatus();

            if ("active".equalsIgnoreCase(status)) {
                activeGroups.add(groupLabel);
            } else {
                inActiveGroups.add(groupLabel);
            }
        }

        if (activeGroups.isEmpty() && inActiveGroups.isEmpty()) {
            activeGroups.add("");
            inActiveGroups.add("");
        } else if (activeGroups.isEmpty()) {
            activeGroups.add("");
        } else if (inActiveGroups.isEmpty()) {
            inActiveGroups.add("");
        }

        activeGroups = sortAndRemoveDuplicates(activeGroups);
        inActiveGroups = sortAndRemoveDuplicates(inActiveGroups);

        groupLabelMap.put("activeGroupLabel", activeGroups);
        groupLabelMap.put("inActiveGroupLabel", inActiveGroups);

        return groupLabelMap;
    }


    private static List<String> sortAndRemoveDuplicates(List<String> list) {
        Collections.sort(list);
        LinkedHashSet<String> set = new LinkedHashSet<>(list);
        list = new ArrayList<>(set);
        return list;
    }

    // Update Status field for the Enterprise Risk , Config management Rights , Group Combined Portfolio - User Group Details
    public void updateStatusField(String reportDate, String indexName, String reportFieldName) throws InterruptedException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        List<String> grouplabelList = new ArrayList<>();
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query inActiveGroupLabel = TermQuery.of(t -> t.field("status").value("active"))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(inActiveGroupLabel))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class)).query(boolQuery));
        SearchResponse<UserGroupList> userGroupListSearchResponse = null;
        try {
            userGroupListSearchResponse = esClient.search(searchRequest, UserGroupList.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Hit<UserGroupList> userGroupListHit : userGroupListSearchResponse.hits().hits()) {
            String groupLabel = Objects.requireNonNull(userGroupListHit.source()).getGroupLabel();
            grouplabelList.add(groupLabel);
        }
        updateStatus(grouplabelList, reportDate , indexName , reportFieldName);
    }

    // Update Status field for the Enterprise Risk , Config management Rights , Group Combined Portfolio - Updating the Status
    private void updateStatus(List<String> groupLabel, String reportDate, String indexName, String reportFieldName) {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        var array = new ArrayList<FieldValue>();
        for (String newData : groupLabel) {
            array.add(FieldValue.of(newData));
        }
        Query groupLabelsQuery = TermsQuery.of(t -> t.field(reportFieldName + ".keyword").terms(TermsQueryField.of(ty -> ty.value(array))))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(groupLabelsQuery))._toQuery();
        Map<String, JsonData> params = new HashMap<>();
        params.put("status", JsonData.of("active"));
        UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                .index(indexName)
                .script(s -> s.inline(InlineScript.of(i ->
                        i.source("ctx._source.status = params.status;")
                                .lang("painless")
                                .params(params))))
                .query(boolQuery)
        );
        //log.info("Update Request - UpdateByQueryRequest : {}", updateByQueryRequest);
        try {
            UpdateByQueryResponse response = esClient.updateByQuery(updateByQueryRequest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update status field", e);
        }
    }


    public void updateGroupLabels(String reportDate, String indexName, String reportFieldName, String fetchUserGroupFieldName) {
        Query query = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();


        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Aggregation fieldAggregation = Aggregation.of(a ->
                a.terms(TermsAggregation.of(ta ->
                        ta.field(reportFieldName + ".keyword").size(environment.getProperty("aggregationPageSize", Integer.class)))));

        aggregation.put(reportFieldName, fieldAggregation);

        log.info("{} - Aggregation :{}", reportFieldName, aggregation);
        SearchRequest searchRequest = SearchRequest.of(s ->
                s.index(indexName).aggregations(aggregation).
                        size(environment.getProperty("aggregationPageSize", Integer.class)).
                        query(query));

        log.info("Index request - Distinct {} : {}", reportFieldName, searchRequest);
        SearchResponse<?> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Long> distinctFieldValues = new TreeMap<>();

        if (searchResponse.aggregations() != null && searchResponse.aggregations().get(reportFieldName) != null) {
            if (searchResponse.aggregations().get(reportFieldName).isSterms()) {
                searchResponse.aggregations().get(reportFieldName).sterms().buckets().array().forEach(b -> distinctFieldValues.put(
                        b.key().stringValue(), b.docCount()));


            } else if (searchResponse.aggregations().get(reportFieldName).isLterms()) {
                searchResponse.aggregations().get(reportFieldName).lterms().buckets().array().forEach(b -> distinctFieldValues.put(
                        String.valueOf(b.key()), b.docCount()));
            }
        }

        Map<String, Long> sortedDistinctFieldValues = new TreeMap<>();
        sortedDistinctFieldValues.putAll(distinctFieldValues);

        for (String fieldValue : sortedDistinctFieldValues.keySet()) {
            log.info("Field Value : {}", fieldValue);
            Map<Object, Object> updateData = getGroupLabels(fetchUserGroupFieldName, fieldValue, reportDate);
            Query reportDateQuery = TermQuery.of(t ->
                    t.field("reportDate").value(reportDate))._toQuery();
            Query fieldQuery = TermQuery.of(t ->
                    t.field(reportFieldName + ".keyword").value(fieldValue))._toQuery();
            Query boolQuery = BoolQuery.of(b ->
                    b.must(reportDateQuery).must(fieldQuery))._toQuery();
            Map<String, JsonData> params = new HashMap<>();
            for (Map.Entry<Object, Object> entry : updateData.entrySet()) {
                params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
                params.put(entry.getValue().toString(), JsonData.of(entry.getValue()));
            }
            UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                    .index(indexName)
                    .script(s -> s.inline(InlineScript.of(i ->
                            i.source("ctx._source.activeGroupLabel = params.activeGroupLabel; ctx._source.inActiveGroupLabel = params.inActiveGroupLabel")
                                    .lang("painless")
                                    .params(params))))
                    .query(boolQuery)
                    .conflicts(Conflicts.Proceed)
                    .slices(s -> s.value(5)) // Split the request into 5 smaller tasks
                    .timeout(Time.of(t -> t.time("10m")))
                    .waitForCompletion(false)
            );
            try {
                esClient.updateByQuery(updateByQueryRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T> String reportDateCheck(String date, String indexName, Class<T> documentClass) throws IOException {
        Query reportDate = TermQuery.of(t -> t.field("reportDate").value(date))._toQuery();
        Query mustQuery = BoolQuery.of(b -> b.must(reportDate))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName).query(mustQuery));
        SearchResponse<T> searchResponse = esClient.search(searchRequest, documentClass);

        if (!searchResponse.hits().hits().isEmpty()) {
            return date;
        } else {
            return getLatestDate(date, indexName, documentClass);
        }
    }

    @SneakyThrows
    public  <T> String getLatestDate(String date, String indexName, Class<T> documentClass) {
        String latestDate = date;
        Query dateQuery = RangeQuery.of(r -> r.field("reportDate").lte(JsonData.of(date)).format("yyyy-MM-dd"))._toQuery();
        SortOptions sortOptions = SortOptions.of(s -> s.field(FieldSort.of(fs -> fs.field("reportDate").order(SortOrder.Desc))));

        SearchRequest searchRequest = SearchRequest.of(s -> s.query(dateQuery).index(indexName)
                .size(1).sort(sortOptions)
                .source(SourceConfig.of(sc -> sc.filter(SourceFilter.of(sf -> sf.includes("reportDate"))))));

        SearchResponse<T> searchResponse = esClient.search(searchRequest, documentClass);

        if (!searchResponse.hits().hits().isEmpty()) {
            for (Hit<T> hit : searchResponse.hits().hits()) {
                T document = hit.source();
                latestDate = Objects.requireNonNull(document)
                        .getClass()
                        .getMethod("getReportDate")
                        .invoke(document)
                        .toString();
            }
        }
        return latestDate;
    }


    // elastic search Query:-
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



    // for large data counts - ( counterparty report ) - manual Load
    @SneakyThrows
    public <T> Integer getDropDownFieldCount(String reportDate ,String filterColumnName, Map<String,List<String>> filterSearch,List<Query> queryList, String indexName, Class<T> documentClass) {
        Query boolQuery = BoolQuery.of(b -> b.must(queryList))._toQuery();
        CountRequest countRequest = CountRequest.of(c -> c.index(indexName).query(boolQuery));
        CountResponse countResponse = esClient.count(countRequest);
        Long totalCount = countResponse.count();
        log.info("TotalCount :{}", totalCount);
        long iterationCount = (totalCount / 10000) + 1;
        log.info("total count : {} iterationCount : {}", totalCount, iterationCount);
        Set<String> uniqueLabels = new HashSet<>();
        Map<String, FieldValue> afterKey = null;

        for (int i = 0; i < iterationCount; i++) {
            Aggregation aggregation;
            HashMap<String, Aggregation> aggregations = new HashMap<>();
            List<Map<String, CompositeAggregationSource>> compAgg = new ArrayList<>();
            CompositeAggregationSource compositeAggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field(filterColumnName+".keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("Composite", compositeAggregationSource);
            compAgg.add(map);
            Map<String, FieldValue> finalAfterKey = afterKey;
            if (afterKey != null) {
                aggregation = CompositeAggregation.of(c -> c.sources(compAgg).size(environment.getProperty("aggregationPageSize", Integer.class)).after(finalAfterKey))._toAggregation();
            } else {
                aggregation = CompositeAggregation.of(c -> c.sources(compAgg).size(environment.getProperty("aggregationPageSize", Integer.class)))._toAggregation();
            }
            Script script = Script.of(s -> s.inline(InlineScript.of(ii -> ii.source("params.duplicate_data >= 1"))));
            Map<String, String> bucketsPathMap = new HashMap<>();
            bucketsPathMap.put("duplicate_data", "_count");
            Aggregation agg = Aggregation.of(a -> a.bucketSelector(BucketSelectorAggregation.of(bs ->
                    bs.bucketsPath(BucketsPath.of(bp -> bp.
                            dict(bucketsPathMap))).script(script))._toAggregation().bucketSelector()));
            Aggregation newAggregation = Aggregation.of(a -> a.composite(aggregation.composite()).aggregations("fetching_duplicate_path", agg));
            aggregations.put("unique_data", newAggregation);
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName).query(boolQuery).aggregations(aggregations));
            SearchResponse<T> searchResponse = esClient.search(searchRequest, documentClass);
            List<CompositeBucket> buckets = searchResponse.aggregations().get("unique_data").composite().buckets().array();
            for (CompositeBucket data : buckets) {
                Map<String, FieldValue> keyMap = data.key();
                FieldValue pathField = keyMap.get("Composite");
                if (pathField != null) {
                    uniqueLabels.add(pathField.stringValue().toUpperCase());
                }
            }
            // newList.addAll(dspLabels);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("unique_data").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
        log.info("Total count of Duplicate Data :{}", uniqueLabels.size());

        return uniqueLabels.size();
       // return new ArrayList<>(uniqueLabels);
    }

    // Adding InActiveTemplate( inActiveGroupLabel and activeGroupLabel is false)
    public void addInActiveTemplate(List<Query> queryList) {
        Query inActiveTemp = BoolQuery.of(b -> b.should(TermQuery.of(t -> t.field("inActiveGroupLabel.keyword").value(""))._toQuery()).should(BoolQuery.of(r -> r.mustNot(ExistsQuery.of(e -> e.field("inActiveGroupLabel"))._toQuery()))._toQuery()).minimumShouldMatch("1"))._toQuery();
        Query activeTemp = BoolQuery.of(b -> b.should(TermQuery.of(t -> t.field("activeGroupLabel.keyword").value(""))._toQuery()).should(BoolQuery.of(r -> r.mustNot(ExistsQuery.of(e -> e.field("activeGroupLabel"))._toQuery()))._toQuery()).minimumShouldMatch("1"))._toQuery();
        queryList.add(inActiveTemp);
        queryList.add(activeTemp);
    }

    // Adding ActiveTemplate( inActiveGroupLabel or activeGroupLabel is true)
    public void addActiveTemplate(List<Query> queryList) {
        Query query1 = BoolQuery.of(b -> b.
                must(ExistsQuery.of(e -> e.field("inActiveGroupLabel"))._toQuery()).
                must(ExistsQuery.of(w -> w.field("activeGroupLabel"))._toQuery()).
                must(BoolQuery.of(o -> o.mustNot(TermQuery.of(tm -> tm.field("inActiveGroupLabel.keyword").value(""))._toQuery()).
                        must(BoolQuery.of(h -> h.mustNot(TermQuery.of(rr -> rr.field("activeGroupLabel.keyword").value(""))._toQuery()))._toQuery()))._toQuery()))._toQuery();

        Query query2 = BoolQuery.of(ll -> ll.must(ExistsQuery.of(ee -> ee.field("inActiveGroupLabel"))._toQuery()).
                must(BoolQuery.of(o -> o.mustNot(TermQuery.of(tm -> tm.field("inActiveGroupLabel.keyword").value(""))._toQuery()))._toQuery()).
                must(BoolQuery.of(o -> o.mustNot(ExistsQuery.of(ee -> ee.field("activeGroupLabel"))._toQuery()))._toQuery()))._toQuery();

        Query query3 = BoolQuery.of(ll -> ll.must(ExistsQuery.of(ee -> ee.field("activeGroupLabel"))._toQuery()).
                must(BoolQuery.of(o -> o.mustNot(TermQuery.of(tm -> tm.field("activeGroupLabel.keyword").value(""))._toQuery()))._toQuery()).
                must(BoolQuery.of(o -> o.mustNot(ExistsQuery.of(ee -> ee.field("InActiveGroupLabel"))._toQuery()))._toQuery()))._toQuery();


        Query query4 = BoolQuery.of(ll -> ll.must(ExistsQuery.of(e -> e.field("activeGroupLabel"))._toQuery()).
                must(ExistsQuery.of(e1 -> e1.field("inActiveGroupLabel"))._toQuery()).
                must(TermQuery.of(t -> t.field("activeGroupLabel.keyword").value(""))._toQuery()).
                must(BoolQuery.of(b -> b.mustNot(TermQuery.of(t1 -> t1.field("inActiveGroupLabel.keyword").value(""))._toQuery()))._toQuery()))._toQuery();


        Query shouldQuery = BoolQuery.of(bool -> bool.should(query1, query2, query3, query4).minimumShouldMatch("1"))._toQuery();
        queryList.add(shouldQuery);
    }
}
