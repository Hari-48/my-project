package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.sql.TranslateRequest;
import co.elastic.clients.elasticsearch.sql.TranslateResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.GroupLabelDepartment;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UserGroupAccessRights;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UserGroupList;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticSearchUtils;
import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class GroupLabelDepartmentService {

    private final static Logger log = LoggerFactory.getLogger(GroupLabelDepartmentService.class);

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    ElasticSearchUtils elasticSearchUtils;

    public final Environment environment;


    public GroupLabelDepartmentService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }


    public Document getGroupLabelDepartment(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                            int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) throws IOException, ParseException {

        CountRequest countRequest = CountRequest.of(c -> c.index(GroupLabelDepartment.INDEX_NAME));
        long count = esClient.count(countRequest).count();
        log.info("count;{}", count);

        if (count == 0) {
            loadGroupDepartment();
        }

        Document groupLabelDepartment = new Document();
        List<Query> queryList = new ArrayList<>();

        String indexName = GroupLabelDepartment.INDEX_NAME;

        // Fetching Data From  JSON File .
        List<String> fields = elasticSearchUtils.getDataFromJsonFile("elastic/GlobalSearchFields/groupDepartment.json", "fields");

        //Fetching DateFields From  JSON File .
        List<String> newDateFields = elasticSearchUtils.getDataFromJsonFile("elastic/GlobalSearchFields/groupDepartment.json", "dateFields");

        //  pit Id - pagination
        String pitId;
        pitId = elasticSearchModel.getPitId();
        if (elasticSearchModel.getPitId().isBlank()) {
            pitId = elasticSearchUtils.createPitId(indexName);
        } else {
            if (elasticSearchUtils.isPitIdExpired(pitId)) {
                pitId = elasticSearchModel.getPitId();
            } else {
                pitId = elasticSearchUtils.createPitId(indexName);
            }
        }
        String finalPitId = pitId;

        // Inside SearchBox
        if (isGlobalSearch && elasticSearchModel.getSearchTerm() != null && !elasticSearchModel.getSearchTerm().isEmpty()) {
            String searchTerm = elasticSearchModel.getSearchTerm();
            Query finalQuery = null;
            if (!searchTerm.contains("AND ") || (!searchTerm.contains("OR"))) {
                if (!elasticSearchUtils.isDateField(searchTerm)) {
                    Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(fields))._toQuery();
                    finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
                } else {
                    Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(newDateFields))._toQuery();
                    finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
                }
            } else {
                Query newSearchTerm = QueryStringQuery.of(q -> q.query(searchTerm).fields(fields))._toQuery();
                finalQuery = BoolQuery.of(b -> b.should(newSearchTerm))._toQuery();
            }
            queryList.add(finalQuery);
        }

        // SQL Query - Elastic  Query - Translate
        else {
            if (elasticSearchModel.getSearchTerm() != null && !elasticSearchModel.getSearchTerm().isEmpty()) {
                String sqlQuery = "SELECT ";
                sqlQuery += "* ";
                sqlQuery += "FROM " + indexName;
                sqlQuery += " t WHERE ";
                sqlQuery += elasticSearchModel.getSearchTerm();
                String finalSqlQuery = sqlQuery;
                TranslateResponse translateResponse = esClient.sql().translate(TranslateRequest.of(t -> t.query(finalSqlQuery)));
                Query query = translateResponse.query();
                log.info("SQL to  Elastic Query -searchTerm :-{}", query);
                queryList.add(query);
            }
        }

        // Bulk Filter
        if (elasticSearchModel.getBulkFilterColumnName() != null && !elasticSearchModel.getBulkFilterColumnName().isEmpty()) {
            String columnName = elasticSearchModel.getBulkFilterColumnName();
            var array = new ArrayList<FieldValue>();
            List<Query> bulkWildCardQueries = new ArrayList<>();
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            if (!date.contains(columnName)) {
                // Check if the bulkFilterSearchType is 'partial'
                if ("partial".equalsIgnoreCase(bulkFilterSearchType)) {
                    // Perform wildcard query with *
                    for (String newData : elasticSearchModel.getBulkFilterColumnValue()) {
                        String filterValue = "*" + FieldValue.of(newData).stringValue() + "*";
                        Query bulkWildcardQuery = WildcardQuery.of(w ->
                                w.field(columnName + ".keyword").value(filterValue))._toQuery();
                        bulkWildCardQueries.add(bulkWildcardQuery);
                    }
                } else {
                    // Perform normal terms query without wildcard
                    for (String newData : elasticSearchModel.getBulkFilterColumnValue()) {
                        array.add(FieldValue.of(newData));
                    }
                    Query termsQuery = TermsQuery.of(ts -> ts
                            .field(columnName + ".keyword")
                            .terms(TermsQueryField.of(t -> t.value(array))))._toQuery();
                    bulkWildCardQueries.add(termsQuery);
                }

            } else {
                for (String newData : elasticSearchModel.getBulkFilterColumnValue()) {
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
            queryList.add(finalQuery);
        }


        // add query for other fields
        Map<String, List<String>> filterSearchMap = elasticSearchModel.getFilterSearch();
        if (filterSearchMap != null && !filterSearchMap.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : filterSearchMap.entrySet()) {
                String fieldName = entry.getKey();
                List<String> fieldValues = entry.getValue();
                if (fieldValues != null && !fieldValues.isEmpty()) {
                    if (!fieldName.equalsIgnoreCase(elasticSearchModel.getFilterColumnName())) {
//                        if (!fieldName.equalsIgnoreCase("amddate")) {
                        if (!newDateFields.contains(fieldName)) {
                            elasticSearchUtils.getBoolQuery(fieldName + ".keyword", fieldValues, queryList);
                        } else {
                            elasticSearchUtils.getBoolQuery(fieldName, fieldValues, queryList);
                        }
                    }
                }
            }
        }

        // inside - dropDown - alphaNumeric
        if (!elasticSearchModel.getAlphaNumericFilterColumn().isEmpty()) {
            for (String alphaNumericColumn : elasticSearchModel.getAlphaNumericFilterColumn()) {
                String script = "def alphaValue = doc['" + alphaNumericColumn + ".keyword'].value; " +
                        "return alphaValue != null && alphaValue =~ /[a-zA-Z]+/;";
                Query scriptQuery = ScriptQuery.of(s -> s.script(
                        Script.of(scr -> scr.inline(
                                InlineScript.of(i -> i.source(script))))))._toQuery();
                log.info("ScriptQuery - FilterColumnAlphaNumeric :- {}", scriptQuery);
                queryList.add(scriptQuery);
            }
        }
        // inside - dropDown - Numeric
        if (!elasticSearchModel.getNumericFilterColumn().isEmpty()) {
            for (String columnNumeric : elasticSearchModel.getNumericFilterColumn()) {
                String script = "def alphaValue = doc['" + columnNumeric + ".keyword'].value; " +
                        "return alphaValue != null && alphaValue =~ /^[0-9\\s]+$/;";
                Query scriptQuery = ScriptQuery.of(s ->
                        s.script(Script.of(scr ->
                                scr.inline(InlineScript.of(i ->
                                        i.source(script))))))._toQuery();
                log.info("ScriptQuery - FilterColumnNumeric :- {}", scriptQuery);
                queryList.add(scriptQuery);
            }
        }
        // when clicks filter column - dropdown
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        if (elasticSearchModel.getFilterColumnName() != null && !elasticSearchModel.getFilterColumnName().isEmpty()) {

            String filterColumnVal;
            Map<String, Long> uniqueFilterValues = new TreeMap<>();
            String filterColumn = elasticSearchModel.getFilterColumnName();
            if (!newDateFields.contains(filterColumn)) {
                filterColumnVal = filterColumn + ".displaycolumnval";
            } else {
                filterColumnVal = filterColumn;
            }
            NamedValue<SortOrder> sort = NamedValue.of("_key", SortOrder.Asc);
            Aggregation filterColumns;
            if (filterColumn.equalsIgnoreCase("department")) {
                filterColumns = Aggregation.of(a ->
                        a.terms(TermsAggregation.of(ta ->
                                ta.script(s -> s.inline(InlineScript.of(i -> i.
                                        source("if (doc['department.displaycolumnval'].size() == 0) { return ''; } else { return doc['department.displaycolumnval'].value;}").
                                        lang("painless")))))));
            } else {
                filterColumns = Aggregation.of(a ->
                        a.terms(TermsAggregation.of(ta ->
                                ta.field(filterColumnVal).order(List.of(sort)).size(pageSize))));
            }

            aggregation.put("FilterColumn", filterColumns);

            if (!elasticSearchModel.getFilterColumnBasedOn().isEmpty() && elasticSearchModel.getFilterColumnBasedOn() != null) {
                if (elasticSearchModel.getFilterColumnBasedOn().equalsIgnoreCase("numeric")) {
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
                    aggregation.put("NumericQuery", numericQuery._toAggregation());
                }
                if (elasticSearchModel.getFilterColumnBasedOn().equalsIgnoreCase("alphanumeric")) {
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
                    aggregation.put("AlphaNumericQuery ", alphaNumericQuery._toAggregation());
                }
            }
            // dropDown - searchBox - searchingValue
            if (!elasticSearchModel.getFilterColumnSearchValue().isEmpty()) {
                String filterValue = elasticSearchModel.getFilterColumnSearchValue();
                if (filterValue.contains("..")) {
                    String[] newValue = filterValue.split("\\.\\.");
                    Query rangeQuery = RangeQuery.of(r ->
                            r.field(filterColumn + ".keyword").from(newValue[0]).to(newValue[1]))._toQuery();
                    log.info("Range Query - filterValue (..):-{}", rangeQuery);
                    queryList.add(rangeQuery);
                } else {
                    String finalFilterValue;
                    Query wildcardQuery;
                    if (!newDateFields.contains(filterColumn)) {
                        filterValue = "*" + filterValue + "*";
                        finalFilterValue = filterValue;
                        wildcardQuery = WildcardQuery.of(w ->
                                w.field(filterColumn + ".keyword").value(finalFilterValue))._toQuery();
                    } else {
                        finalFilterValue = filterValue;
                        wildcardQuery = TermQuery.of(w -> w.field(filterColumn).value(finalFilterValue))._toQuery();
                    }
                    log.info("Wild" +
                            "card Query - filterValue (..):-{}", wildcardQuery);
                    queryList.add(wildcardQuery);
                }
            }
            //Adding All Queries into Bool Query :-
            Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            log.info(" Bool Query :- {}", searchQuery);

            HashMap<String, Aggregation> filterColumnAggregation = new HashMap<>();
            if (elasticSearchModel.getFilterSearch().get(filterColumn) != null) {
                log.info("preserved value in filter search  column : {} ", elasticSearchModel.getFilterSearch().get(filterColumn));
                var array = new ArrayList<FieldValue>();
                for (String newData : elasticSearchModel.getFilterSearch().get(filterColumn)) {
                    array.add(FieldValue.of(newData));
                }
                TermsQuery termsQueryBuilder = TermsQuery.of(t -> t.field(filterColumnVal).terms(TermsQueryField.of(tf -> tf.value(array))));
                Aggregation filterSearchAggregation = Aggregation.of(a -> a.filter(termsQueryBuilder._toQuery()).aggregations("FilterColumnAggValue", filterColumns));
                filterColumnAggregation.put("FilterColumnAgg", filterSearchAggregation);
            }

            // Request
            SearchRequest searchRequest = SearchRequest.of(q -> q
                    .query(searchQuery)
                    .aggregations(aggregation).index(indexName));
            log.info(" FilterColumn Request :-  {}", searchRequest);
            SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);

            Map<String, Long> filterColumnValues = new TreeMap();
            if (searchResponse.aggregations() != null && searchResponse.aggregations().get("FilterColumnAgg") != null) {
                if (searchResponse.aggregations().get("FilterColumnAgg").isFilter()) {
                    if (searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue") != null) {
                        if (searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue").isSterms()) {
                            searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue").sterms()
                                    .buckets().array().forEach(b -> filterColumnValues.put(b.key().stringValue(), b.docCount()));
                        } else if (searchResponse.aggregations().get("FilterColumn").isLterms()) {
                            searchResponse.aggregations().get("FilterColumnAgg").filter().aggregations().get("FilterColumnAggValue").lterms()
                                    .buckets().array().forEach(b -> filterColumnValues.put(String.valueOf(b.key()), b.docCount()));
                        }
                    }
                }
            }
            // //Fetching Filter Column Aggregation
            List<String> filterColumnKeyList = new ArrayList<>(filterColumnValues.keySet());
            List<Long> filterColumnCountList = new ArrayList<>(filterColumnValues.values());
            List<Document> filterColumnDocuments = elasticSearchUtils.checkDuplicates(filterColumnCountList, filterColumnKeyList);
            // Map<String, Long> dropDownFilter = new TreeMap<>();
            LinkedHashMap<String, Long> dropDownFilter = new LinkedHashMap<>();
            for (Document doc : filterColumnDocuments) {
                dropDownFilter.put((String) doc.get("name"), (Long) doc.get("value"));
            }
            log.info("preserved values in dropDownFilter : {} ", dropDownFilter);

            // Fetching Aggregations
            if (searchResponse.aggregations() != null && searchResponse.aggregations().get("FilterColumn") != null) {
                if (searchResponse.aggregations().get("FilterColumn").isSterms()) {
                    searchResponse.aggregations().get("FilterColumn").sterms().buckets().array().forEach(b -> uniqueFilterValues.put(
                            b.key().stringValue(), b.docCount()));
                } else if (searchResponse.aggregations().get("FilterColumn").isLterms()) {
                    searchResponse.aggregations().get("FilterColumn").lterms().buckets().array().forEach(b -> uniqueFilterValues.put(
                            String.valueOf(b.key()), b.docCount()));
                }
            }
            // column -  dropDown
            List<String> keyList = new ArrayList<>(uniqueFilterValues.keySet());
            List<Long> countList = new ArrayList<>(uniqueFilterValues.values());
            List<Document> resultDocuments = elasticSearchUtils.checkDuplicates(countList, keyList);
//            Map<String, Long> dropDownFilter = new TreeMap<>();
            for (Document doc : resultDocuments) {
                dropDownFilter.put((String) doc.get("name"), (Long) doc.get("value"));
            }

            long filterColNumericCount = 0;
            long filterColAlphaNumericCount = 0;

            if (!newDateFields.contains(filterColumn)) {
                filterColNumericCount = elasticSearchUtils.getNumericCount(filterColumn, queryList, indexName);
                filterColAlphaNumericCount = elasticSearchUtils.getAlphaNumericCount(filterColumn, queryList, indexName);
            }
            Integer dropDownFieldCount = 0;
            dropDownFieldCount = elasticSearchUtils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, GroupLabelDepartment.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            groupLabelDepartment.put("filterValues", newDropDown);
            groupLabelDepartment.put("dropDownFieldCount", dropDownFieldCount);
            groupLabelDepartment.put("filterColumnNumericCount", filterColNumericCount);
            groupLabelDepartment.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {

            // for export
            SortOptions sortOptions1;
            if (sortBy != null) {
                if (!newDateFields.contains(sortBy)) {
                    sortBy = sortBy + ".keyword";
                }
                String finalSortBy = sortBy;
                sortOptions1 = SortOptions.of(s ->
                        s.field(FieldSort.of(fs -> fs.field(finalSortBy).order(sortingOrder.equalsIgnoreCase("asc")
                                ? SortOrder.Asc : SortOrder.Desc))));
            } else {
                sortOptions1 = null;
            }
            SortOptions sortOptions2 = SortOptions.of(s ->
                    s.field(FieldSort.of(fs -> fs.field("groupLabel.keyword").order(sortingOrder.equalsIgnoreCase("asc")
                            ? SortOrder.Asc : SortOrder.Desc))));
            Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            List<GroupLabelDepartment> allDocuments = new ArrayList<>();
            Document document = new Document();
            List<FieldValue> searchAfterValues = new ArrayList<FieldValue>();
            // Execute the page scroll - initial search
            String pageScroll = "initial";
            SearchRequest searchRequest;

            if (elasticSearchModel.getSearchAfterValue().isEmpty()) {
                searchRequest = SearchRequest.of(q -> q.pit(pit -> pit.id(finalPitId)).query(searchQuery).size(pageSize)
                        .sort(sortOptions1, sortOptions2));
                //log.info("Initial Request :- {}", searchRequest);
            } else {
                pageScroll = elasticSearchModel.getSearchAfterValue().toString();
                List<Object> sort = elasticSearchModel.getSearchAfterValue();
                if (sortBy == null) {
                    // sortArray.add(FieldValue.of(filteredSearchModel.getSearchAfterValue()));
                } else {
                    Object newData = new Object();
                    for (int i = 0; i < 3; i++) {
                        newData = sort.get(i);
                        elasticSearchUtils.addToSearchAfterValues(newData, searchAfterValues);
                    }
                }

                searchRequest = SearchRequest.of(q -> q.pit(pit -> pit.id(finalPitId)).query(searchQuery).size(pageSize)
                        .sort(sortOptions1, sortOptions2).searchAfter(searchAfterValues));
                log.info(" Subsequent  Pagination Request :-{}", searchRequest);
            }
            SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);
            //log.info("Response :- {}", searchResponse);
            log.info("Total count in Response : {}", searchResponse.hits().hits().size());
            searchResponse.hits().hits().forEach(f -> allDocuments.add(f.source()));

            // SearchAfter - Sorting
            List<Object> sorting = new ArrayList<>();
            if (!searchResponse.hits().hits().isEmpty()) {
                Hit<GroupLabelDepartment> firstHit = searchResponse.hits().hits().get(0);
                List<FieldValue> firstHitSortValues = firstHit.sort();
                if (!firstHitSortValues.isEmpty()) {
                    if (!newDateFields.contains(sortBy)) {
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
                document.put("sortingFirstIndex", sorting);
                // Processing last hit
                sorting = new ArrayList<>(); // Clearing the list for the next hit
                Hit<GroupLabelDepartment> lastHit = searchResponse.hits().hits().get(searchResponse.hits().hits().size() - 1);
                List<FieldValue> lastHitSortValues = lastHit.sort();
                if (!lastHitSortValues.isEmpty()) {
                    if (!newDateFields.contains(sortBy)) {
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
                document.put("sortingLastIndex", sorting);
            }

            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));

            String template = "USER_DEPARTMENT";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elasticSearchUtils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            groupLabelDepartment.put("totalPage", totalPages);
            groupLabelDepartment.put("count", totalCount);
            groupLabelDepartment.put("pageScroll", pageScroll);
            groupLabelDepartment.put("content", allDocuments);
            groupLabelDepartment.put("sorting", document);
            groupLabelDepartment.put("pitId", pitId);
        }
        return groupLabelDepartment;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getGroupLabelDepartmentExport(String reportDate, String fileName, ElasticSearchModel elasticSearchModel, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String name, int pageSize, boolean globalSearch, String bulkFilterSearchType, String userName) throws Exception {
        List<GroupLabelDepartment> groupLabelDepartmentList = new ArrayList<>();
        Document docs = new Document();
        elasticSearchModel.fieldColumns = new ArrayList<>();
        long totalCount = 0;
        long totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        List<Object> searchAfterValue = elasticSearchModel.getSearchAfterValue();

        Map<Object, Object> search = new HashMap<>();
        for (int i = 0; i < totalPage; i++) {
            if (i > 0) {
                List<Object> sort = (List<Object>) docs.get("sortingLastIndex");
                searchAfterValue = sort;
                elasticSearchModel.setSearchAfterValue(searchAfterValue);
            }
            Document groupDepartment = getGroupLabelDepartment(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);
            groupLabelDepartmentList.addAll((Collection<? extends GroupLabelDepartment>) groupDepartment.get("content"));
            docs = (Document) groupDepartment.get("sorting");

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupLabelDepartmentList.size(), job.getId());
            totalPage = (long) groupDepartment.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) groupDepartment.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", groupLabelDepartmentList.size(), (int) totalCount, job.getId());
        return elasticSearchUtils.exportFileFromDetails(outputFormat, groupLabelDepartmentList, fileName, elasticSearchModel.fieldMaps, elasticSearchModel.color, job, elasticSearchModel.fieldColumns, elasticSearchModel);
    }

    public void loadGroupDepartment() throws IOException {
        // when Index is empty ,  I need to Update this  Query.( for only initial Stage )
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(GroupLabelDepartment.INDEX_NAME).size(10000));
        SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);
        log.info("Group Label Count : {}", searchResponse.hits().hits().size());
        if (searchResponse.hits().hits().isEmpty()) {
            log.info("Initial Update:-");
            // Fetching Data from UserGroupList
            String latestDate = null;
            SearchRequest groupList = SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).size(1).
                    sort(SortOptions.of(y -> y.field(FieldSort.of(f -> f.field("reportDate").order(SortOrder.Desc))))));
            SearchResponse<UserGroupList> searchFirstDataResponse = esClient.search(groupList, UserGroupList.class);
            if (!searchFirstDataResponse.hits().hits().isEmpty()) {
                latestDate = searchFirstDataResponse.hits().hits().get(0).source().getReportDate();
            }
            final String reportDate = latestDate;
            log.info("Date :{}", reportDate);

            Query groupListQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
            SearchResponse<UserGroupList> groupListSearchResponse;
            try {
                groupListSearchResponse = esClient.search(SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).query(groupListQuery)
                        .size(10000)), UserGroupList.class);
                //insert records in group labels
                groupLabelDepartment(groupListSearchResponse);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void groupLabelDepartment(SearchResponse<UserGroupList> groupListSearchResponse) throws IOException {
        if (!groupListSearchResponse.hits().hits().isEmpty()) {
            for (Hit<UserGroupList> hit : groupListSearchResponse.hits().hits()) {
                if (!(hit.source() == null)) {
                    Map<String, Object> newMap = new HashMap<>();
                    String groupLabel = hit.source().getGroupLabel();
                    String role = hit.source().getGrpRoleStr();
                    String status = hit.source().getStatus();
                    String date = String.valueOf(LocalDate.now());
//                    log.info("Update Group Label Department : {}", groupLabel) ;
                    newMap.put("groupLabel", groupLabel);
                    newMap.put("grpRoleStr", role);
                    newMap.put("groupLabelStatus", status);
                    newMap.put("createdDate", date);
                    BulkRequest.Builder br = new BulkRequest.Builder();
                    br.operations(op -> op
                            .index(idx -> idx
                                    .index(GroupLabelDepartment.INDEX_NAME).
                                    id(hit.id())
                                    .document(newMap)
                            )
                    );
                    BulkResponse result = esClient.bulk(br.build());
                }
            }
        }
    }

    public Boolean isDepartmentExist(String department) throws IOException {

        Query departmentExist = TermQuery.of(t -> t.field("department.keyword").value(department))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).query(departmentExist));
        SearchResponse<UserGroupAccessRights> searchResponse = esClient.search(searchRequest, UserGroupAccessRights.class);
        log.info("Count of Department present in groupList->{}", searchResponse.hits().hits().size());
        return !searchResponse.hits().hits().isEmpty();
    }

    public ResponseEntity<?> updateDepartmentField(String department, String groupLabel, String grpRoleStr, String name, Boolean userConfirmation, String updatedDepartment) throws IOException {
        Boolean departmentPresent = isDepartmentExist(department);
        if (departmentPresent) {
            if (userConfirmation) {
                updateDepartments(groupLabel, grpRoleStr, updatedDepartment, name);
                return new ResponseEntity<>("Successfully Updated ", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Inundation Unsuccessful", HttpStatus.OK);
            }
        } else {
            updateDepartments(groupLabel, grpRoleStr, updatedDepartment, name);
            return new ResponseEntity<>("Successfully Updated ", HttpStatus.OK);
        }
    }

    public void updateDepartments(String groupLabel, String grpRoleStr, String department, String name) throws
            IOException {
        Query groupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(groupLabel))._toQuery();
        Query roleQuery = TermQuery.of(t -> t.field("grpRoleStr.keyword").value(grpRoleStr))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(groupLabelQuery).must(roleQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(GroupLabelDepartment.INDEX_NAME).size(10000));
//        log.info("update department search request {}", searchRequest) ;
        SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);
        if (!searchResponse.hits().hits().isEmpty()) {
            for (Hit<GroupLabelDepartment> hit : searchResponse.hits().hits()) {
                String docId = hit.id();
                String updatedDate = String.valueOf(LocalDateTime.now());
                Map<String, Object> updateDepartment = new HashMap<>();
                updateDepartment.put("department", department);
                updateDepartment.put("username", name);
                updateDepartment.put("updatedTime", updatedDate);
                UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.id(docId).index(GroupLabelDepartment.INDEX_NAME).
                        retryOnConflict(3).
                        doc(updateDepartment));
                esClient.update(updateRequest, GroupLabelDepartment.class);
            }
        }
    }

    public void updateGroupLabelDepartments(String existingDepartmentName, String newDepartmentName, String name) throws IOException {
        Query oldDepartmentQuery = TermQuery.of(t -> t.field("department.keyword").value(existingDepartmentName))._toQuery();
        String scriptSource = "ctx._source.department = params.newDepartmentName; " +
                "ctx._source.username = params.username; " +
                "ctx._source.updatedTime = params.updatedTime";

        Map<String, JsonData> scriptParams = new HashMap<>();
        scriptParams.put("newDepartmentName", JsonData.of(newDepartmentName));
        scriptParams.put("username", JsonData.of(name));
        scriptParams.put("updatedTime", JsonData.of(String.valueOf(LocalDateTime.now())));

        InlineScript inlineScript = InlineScript.of(i -> i
                .lang(ScriptLanguage.Painless)
                .source(scriptSource)
                .params(scriptParams)
        );

        Script script = Script.of(s -> s.inline(inlineScript));

        UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                .index(GroupLabelDepartment.INDEX_NAME)
                .query(oldDepartmentQuery)
                .script(script)
        );

        log.info("Update Request - UpdateByQueryRequest: {}", updateByQueryRequest);
        try {
            UpdateByQueryResponse updateByQueryResponse = esClient.updateByQuery(updateByQueryRequest);
//            log.info("Successfully updated {} documents", updateByQueryResponse.updated());
        } catch (IOException e) {
            log.error("Error occurred while updating documents: " + e.getMessage(), e);
            throw e;
        }
    }
}





