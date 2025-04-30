package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.ChineseWall;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.CounterPartyDocument;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UserGroupList;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import lombok.SneakyThrows;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChineseWallService {

    private static final Logger log = LoggerFactory.getLogger(ChineseWallService.class);
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;

    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public ChineseWallService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }

    @SneakyThrows
    public Document getChineseWallData(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                       int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username, Boolean inActiveTemplate) {


        Document searchChineseWall = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = ChineseWall.INDEX_NAME;

        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/chineseWallTemplate.json", "fields");
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/chineseWallTemplate.json", "integerFields");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/chineseWallTemplate.json", "globalSearchFields");

        //  pit Id - pagination
        String pitId;
        pitId = elasticSearchModel.getPitId();
        if (elasticSearchModel.getPitId().isBlank()) {
            pitId = elastic_utils.createPitId(indexName);
        } else {
            if (elastic_utils.isPitIdExpired(pitId)) {
                pitId = elasticSearchModel.getPitId();
            } else {
                pitId = elastic_utils.createPitId(indexName);
            }
        }
        String finalPitId = pitId;
        // Report Date Added - Query :-
        Query reportDate = TermQuery.of(q -> q.field("reportDate").value(date))._toQuery();
        queryList.add(reportDate);

        // To get InActiveTemplate ( Both  Active and InActive groups are Empty or Nil )
//        if (inActiveTemplate) {
//            Query inActiveTemp = BoolQuery.of(b -> b.should(TermQuery.of(t -> t.field("inActiveGroupLabel.keyword").value(""))._toQuery()).should(BoolQuery.of(r -> r.mustNot(ExistsQuery.of(e -> e.field("inActiveGroupLabel"))._toQuery()))._toQuery()).minimumShouldMatch("1"))._toQuery();
//            Query activeTemp = BoolQuery.of(b -> b.should(TermQuery.of(t -> t.field("activeGroupLabel.keyword").value(""))._toQuery()).should(BoolQuery.of(r -> r.mustNot(ExistsQuery.of(e -> e.field("activeGroupLabel"))._toQuery()))._toQuery()).minimumShouldMatch("1"))._toQuery();
//            queryList.add(inActiveTemp);
//            queryList.add(activeTemp);
//        } else {
//
//            Query query1 = BoolQuery.of(b -> b.
//                    must(ExistsQuery.of(e -> e.field("inActiveGroupLabel"))._toQuery()).
//                    must(ExistsQuery.of(w -> w.field("activeGroupLabel"))._toQuery()).
//                    must(BoolQuery.of(o -> o.mustNot(TermQuery.of(tm -> tm.field("inActiveGroupLabel.keyword").value(""))._toQuery()).
//                            must(BoolQuery.of(h -> h.mustNot(TermQuery.of(rr -> rr.field("activeGroupLabel.keyword").value(""))._toQuery()))._toQuery()))._toQuery()))._toQuery();
//
//            Query query2 = BoolQuery.of(ll -> ll.must(ExistsQuery.of(ee -> ee.field("inActiveGroupLabel"))._toQuery()).
//                    must(BoolQuery.of(o -> o.mustNot(TermQuery.of(tm -> tm.field("inActiveGroupLabel.keyword").value(""))._toQuery()))._toQuery()).
//                    must(BoolQuery.of(o -> o.mustNot(ExistsQuery.of(ee -> ee.field("activeGroupLabel"))._toQuery()))._toQuery()))._toQuery();
//
//            Query query3 = BoolQuery.of(ll -> ll.must(ExistsQuery.of(ee -> ee.field("activeGroupLabel"))._toQuery()).
//                    must(BoolQuery.of(o -> o.mustNot(TermQuery.of(tm -> tm.field("activeGroupLabel.keyword").value(""))._toQuery()))._toQuery()).
//                    must(BoolQuery.of(o -> o.mustNot(ExistsQuery.of(ee -> ee.field("InActiveGroupLabel"))._toQuery()))._toQuery()))._toQuery();
//
//
//            Query query4 = BoolQuery.of(ll -> ll.must(ExistsQuery.of(e -> e.field("activeGroupLabel"))._toQuery()).
//                    must(ExistsQuery.of(e1 -> e1.field("inActiveGroupLabel"))._toQuery()).
//                    must(TermQuery.of(t -> t.field("activeGroupLabel.keyword").value(""))._toQuery()).
//                    must(BoolQuery.of(b -> b.mustNot(TermQuery.of(t1 -> t1.field("inActiveGroupLabel.keyword").value(""))._toQuery()))._toQuery()))._toQuery();
//
//
//            Query shouldQuery = BoolQuery.of(bool -> bool.should(query1, query2, query3, query4).minimumShouldMatch("1"))._toQuery();
//            queryList.add(shouldQuery);
//        }

        if (inActiveTemplate) {
            log.info("inActiveTemplate is true. Adding inactive template queries.");
            elastic_utils.addInActiveTemplate(queryList);
        } else {
            log.info("inActiveTemplate is false. Adding active template queries.");
            elastic_utils.addActiveTemplate(queryList);
        }

        // searchBox  - Global Search  & Translate SQL Query - Elastic  Query
        if (isGlobalSearch && elasticSearchModel.getSearchTerm() != null && !elasticSearchModel.getSearchTerm().isEmpty()) {
            queryList.add(elastic_utils.getGlobalSearchTerm(elasticSearchModel.getSearchTerm(), fields, integerFields, globalSearchFields));
        } else {
            if (elasticSearchModel.getSearchTerm() != null && !elasticSearchModel.getSearchTerm().isEmpty()) {
                queryList.add(elastic_utils.getTranslateQuery(elasticSearchModel.getSearchTerm(), indexName));
            }
        }
        // Bulk Filter ( Partial filter ) :-
        if (elasticSearchModel.getBulkFilterColumnName() != null && !elasticSearchModel.getBulkFilterColumnName().isEmpty()) {
            queryList.add(elastic_utils.getBulkFilterQuery(elasticSearchModel.getBulkFilterColumnName(), integerFields, elasticSearchModel.getBulkFilterColumnValue(), bulkFilterSearchType));
        }
        // add query for other fields
        if (elasticSearchModel.getFilterSearch() != null && !elasticSearchModel.getFilterSearch().isEmpty()) {
            elastic_utils.getBoolQuery(elasticSearchModel.getFilterSearch(), elasticSearchModel, integerFields, queryList);
        }
        // inside - dropDown - alphaNumeric
        if (!elasticSearchModel.getAlphaNumericFilterColumn().isEmpty()) {
            for (String alphaNumericColumn : elasticSearchModel.getAlphaNumericFilterColumn()) {
                queryList.add(elastic_utils.getAlphaNumericQuery(alphaNumericColumn));
            }
        }
        // inside - dropDown - Numeric
        if (!elasticSearchModel.getNumericFilterColumn().isEmpty()) {
            for (String numericColumn : elasticSearchModel.getNumericFilterColumn()) {
                queryList.add(elastic_utils.getNumericQuery(numericColumn));
            }
        }
        // when clicks filter column - dropdown
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        if (elasticSearchModel.getFilterColumnName() != null && !elasticSearchModel.getFilterColumnName().isEmpty()) {
            Map<Object, Long> uniqueFilterValues = new TreeMap<>();
            Aggregation filterColumns = elastic_utils.getFilterColumn(elasticSearchModel.getFilterColumnName(), integerFields, pageSize);
            aggregation.put("FilterColumn", filterColumns);
            if (!elasticSearchModel.getFilterColumnBasedOn().isEmpty()) {
                if (elasticSearchModel.getFilterColumnBasedOn().equalsIgnoreCase("numeric")) {
                    Query numericScriptQuery = elastic_utils.getNumericScriptQuery(elasticSearchModel.getFilterColumnName(), integerFields);
                    aggregation.put("NumericQuery", numericScriptQuery._toAggregation());
                }
                if (elasticSearchModel.getFilterColumnBasedOn().equalsIgnoreCase("alphanumeric")) {
                    Query alphaNumericScriptQuery = elastic_utils.getAlphaNumericScriptQuery(elasticSearchModel.getFilterColumnName(), integerFields);
                    aggregation.put("AlphaNumericQuery ", alphaNumericScriptQuery._toAggregation());
                }
            }
            // dropDown - searchBox - searchingValue -[ FilterColumnSearchValue]
            if (!elasticSearchModel.getFilterColumnSearchValue().isEmpty()) {
                if (elasticSearchModel.getFilterColumnSearchValue().contains("..")) {
                    queryList.add(elastic_utils.getRangeQuery(elasticSearchModel.getFilterColumnSearchValue(), elasticSearchModel.getFilterColumnName()));
                } else if (elasticSearchModel.getFilterColumnSearchValue().equalsIgnoreCase("nil")) {
                    queryList.add(elastic_utils.getNilQuery(elasticSearchModel.getFilterColumnName()));
                } else {
                    queryList.add(elastic_utils.getWildcardQuery(integerFields, elasticSearchModel.getFilterColumnName(), elasticSearchModel.getFilterColumnSearchValue()));
                }
            }
            //Adding All Queries into Bool Query :-
            Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            log.info(" Bool Query :- {}", searchQuery);
            HashMap<String, Aggregation> filterColumnAggregation = new HashMap<>();
            if (elasticSearchModel.getFilterSearch().get(elasticSearchModel.getFilterColumnName()) != null) {
                Aggregation filterSearchAggregation = elastic_utils.getFilterColumnAgg(integerFields, elasticSearchModel.getFilterColumnName(), elasticSearchModel, filterColumns);
                filterColumnAggregation.put("FilterColumnAgg", filterSearchAggregation);
            }
            // Request
            SearchRequest searchRequest = SearchRequest.of(q -> q
                    .query(searchQuery)
                    .aggregations(aggregation).
                    aggregations(filterColumnAggregation).
                    index(indexName));
            log.info(" FilterColumn Request :-  {}", searchRequest);
            SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);
            Map<Object, Long> filterColumnValues = elastic_utils.extractFilterColumnAgg(searchResponse);
            // //Fetching  - Filter Column Aggregation
            List<Object> filterColumnKeyList = new ArrayList<>(filterColumnValues.keySet());
            List<Long> filterColumnCountList = new ArrayList<>(filterColumnValues.values());
            List<Document> filterColumnDocuments = elastic_utils.checkDuplicateObjects(filterColumnCountList, filterColumnKeyList);
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
            List<Object> keyList = new ArrayList<>(uniqueFilterValues.keySet());
            List<Long> countList = new ArrayList<>(uniqueFilterValues.values());
            List<Document> resultDocuments = elastic_utils.checkDuplicateObjects(countList, keyList);
            //   Map<String, Long> dropDownFilter = new TreeMap<>();
            for (Document doc : resultDocuments) {
                dropDownFilter.put((String) doc.get("name"), (Long) doc.get("value"));
            }
            long filterColNumericCount = 0;
            long filterColAlphaNumericCount = 0;
            filterColNumericCount = elastic_utils.getNumericCount(elasticSearchModel.getFilterColumnName(), queryList, indexName);
            filterColAlphaNumericCount = elastic_utils.getAlphaNumericCount(elasticSearchModel.getFilterColumnName(), queryList, indexName);

            Integer dropDownFieldCount = 0;
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, ChineseWall.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            searchChineseWall.put("filterValues", newDropDown);
            searchChineseWall.put("dropDownFieldCount", dropDownFieldCount);
            searchChineseWall.put("filterColumnNumericCount", filterColNumericCount);
            searchChineseWall.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<ChineseWall> allDocuments = new ArrayList<>();
            Document document = new Document();
            List<FieldValue> searchAfterValues = new ArrayList<FieldValue>();
            // for export
            SortOptions sortOptions1;
            if (sortBy != null) {
                sortOptions1 = elastic_utils.getSortOptions(integerFields, sortBy, sortingOrder);
            } else {
                sortOptions1 = null;
            }
            SortOptions sortOptions2 = SortOptions.of(s ->
                    s.field(FieldSort.of(fs -> fs.
                            field("counterpartLabel.keyword").order(sortingOrder.equalsIgnoreCase("asc")
                                    ? SortOrder.Asc : SortOrder.Desc))));
            Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            // Execute the page scroll - initial search
            String pageScroll = "initial";
            SearchRequest searchRequest;
            if (elasticSearchModel.getSearchAfterValue().isEmpty()) {
                searchRequest = SearchRequest.of(q -> q.pit(pit -> pit.id(finalPitId)).query(searchQuery).size(pageSize)
                        .sort(sortOptions1, sortOptions2));
                log.info("Initial Request  :- {}", searchRequest);
            } else {
                pageScroll = elasticSearchModel.getSearchAfterValue().toString();
                List<Object> sort = elasticSearchModel.getSearchAfterValue();
                for (int i = 0; i < 3; i++) {
                    Object newData = sort.get(i);
                    elastic_utils.addToSearchAfterValues(newData, searchAfterValues);
                }
                searchRequest = SearchRequest.of(q -> q.pit(pit -> pit.id(finalPitId)).query(searchQuery).size(pageSize)
                        .sort(sortOptions1, sortOptions2).searchAfter(searchAfterValues));
                log.info(" Subsequent  Pagination Request :-{}", searchRequest);
            }
            SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);
            log.info("Total count in Response : {}", searchResponse.hits().hits().size());
            searchResponse.hits().hits().forEach(f -> allDocuments.add(f.source()));
            // SearchAfter - Sorting
            List<Object> sorting = new ArrayList<>();
            if (!searchResponse.hits().hits().isEmpty()) {
                // Processing last hit
                sorting = elastic_utils.getFirstSortingOrder(searchResponse, integerFields, sortBy);
                document.put("sortingFirstIndex", sorting);
                // Processing last hit
                sorting = elastic_utils.getLastSortingOrder(searchResponse, integerFields, sortBy);
                document.put("sortingLastIndex", sorting);
            }
            // saving the Search term - Query  :-
            String template = "CHINESE_WALL_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));
            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            searchChineseWall.put("totalPage", totalPages);
            searchChineseWall.put("count", totalCount);
            searchChineseWall.put("pageScroll", pageScroll);
            searchChineseWall.put("content", allDocuments);
            searchChineseWall.put("sorting", document);
            searchChineseWall.put("pitId", pitId);
        }
        return searchChineseWall;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getChineseWallExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel, Boolean inActiveTemplate) throws Exception {
        List<ChineseWall> chineseWallList = new ArrayList<>();
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
            Document chineseWallValues = getChineseWallData(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName, inActiveTemplate);

            //  log.info("NEW RESPONSE :{}", searchCounterPartyValues);

            chineseWallList.addAll((Collection<? extends ChineseWall>) chineseWallValues.get("content"));
            docs = (Document) chineseWallValues.get("sorting");

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallList.size(), job.getId());
            totalPage = (long) chineseWallValues.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) chineseWallValues.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", chineseWallList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, chineseWallList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }

    public Map<Object, Object> getGroupLabels(String template, String reportDate) {
        Map<Object, Object> groupLabelMap = new HashMap<>();

        List<String> activeGroups = new ArrayList<>();
        List<String> inActiveGroups = new ArrayList<>();

        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query templateQuery = TermQuery.of(t -> t.field("chineseWall.keyword").value(template))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateQuery))._toQuery();
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
        // Sort the list
        Collections.sort(list);

        // Use a LinkedHashSet to remove duplicates and maintain the insertion order
        LinkedHashSet<String> set = new LinkedHashSet<>(list);

        // Convert the set back to a list
        list = new ArrayList<>(set);

        return list;
    }


    public void updateGroupLabels(String reportDate) {

        Query query = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();

        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Aggregation templateLabel = Aggregation.of(a ->
                a.terms(TermsAggregation.of(ta ->
                        ta.field("templateLabel.keyword").size(environment.getProperty("aggregationPageSize", Integer.class)))));

        aggregation.put("templateLabel", templateLabel);

        log.info("TemplateLabel - Aggregation :{}", aggregation);
        SearchRequest searchRequest = SearchRequest.of(s ->
                s.index(ChineseWall.INDEX_NAME).aggregations(aggregation).
                        size(environment.getProperty("aggregationPageSize", Integer.class)).
                        query(query));

        log.info("Chinese Wall request - Distinct template Label :{}", searchRequest);
        SearchResponse<ChineseWall> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, ChineseWall.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Long> distinctTemplate = new TreeMap<>();


        if (searchResponse.aggregations() != null && searchResponse.aggregations().get("templateLabel") != null) {
            if (searchResponse.aggregations().get("templateLabel").isSterms()) {
                searchResponse.aggregations().get("templateLabel").sterms().buckets().array().forEach(b -> distinctTemplate.put(
                        b.key().stringValue(), b.docCount()));
            } else if (searchResponse.aggregations().get("templateLabel").isLterms()) {
                searchResponse.aggregations().get("templateLabel").lterms().buckets().array().forEach(b -> distinctTemplate.put(
                        String.valueOf(b.key()), b.docCount()));
            }
        }

        Map<String, Long> sortedDistinctTemplate = new TreeMap<>(Collections.reverseOrder());
        sortedDistinctTemplate.putAll(distinctTemplate);


        for (String label : sortedDistinctTemplate.keySet()) {
            log.info("template label : {}", label);
            // Getting the GroupLabel (Active groups and Inactive Groups) for corresponding Template
            Map<Object, Object> updateData = getGroupLabels(label, reportDate);
            Query reportDateQuery = TermQuery.of(t ->
                    t.field("reportDate").value(reportDate))._toQuery();
            Query templateQuery = TermQuery.of(t ->
                    t.field("templateLabel.keyword").value(label))._toQuery();
            Query boolQuery = BoolQuery.of(b ->
                    b.must(reportDateQuery).must(templateQuery))._toQuery();
            Map<String, JsonData> params = new HashMap<>();
            for (Map.Entry<Object, Object> entry : updateData.entrySet()) {
                params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
                params.put(entry.getValue().toString(), JsonData.of(entry.getValue()));
            }
            // Updating Based on GroupLabels
            UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                    .index(ChineseWall.INDEX_NAME)
                    .script(s -> s.inline(InlineScript.of(i ->
                            i.source("ctx._source.activeGroupLabel = params.activeGroupLabel;" +
                                            " ctx._source.inActiveGroupLabel = params.inActiveGroupLabel").
                                    lang("painless").
                                    params(params)))).
                    query(boolQuery));
            //   log.info("Update Request - UpdateByQueryRequest : {}", updateByQueryRequest);
            try {
                esClient.updateByQuery(updateByQueryRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @SneakyThrows(Exception.class)
    public void updateCounterpartyStatus(String reportDate) {

        Map<String, FieldValue> afterKey = null;
        int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);
        // Initial search to determine the total number of hits
        Query initialQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query countQuery = BoolQuery.of(q -> q.must(initialQuery))._toQuery();
        CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(ChineseWall.INDEX_NAME).query(initialQuery));
        long totalCount = esClient.count(totalCountRequest).count();
        long iterationCount = (totalCount / pageSize) + 1;
        log.info("total count : {} iterationCount : {}", totalCount, iterationCount);
        for (int i = 0; i < iterationCount; i++) {

            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.displaycolumnval")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("composite", aggregationSource);
            compoAgg.add(map);

            Aggregation aggregation;
            Map<String, FieldValue> finalAfterKey = afterKey;
            if (afterKey != null) {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey))._toAggregation();
            } else {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg))._toAggregation();
            }

            log.info("Aggregation :{}", aggregation);
            Query query = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();

            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index(ChineseWall.INDEX_NAME)
                    .query(query)
                    .aggregations("Aggregation", aggregation)
                    .size(pageSize));

            log.info("Getting counterpartLabel with Pagination ( After Key ) - Request :[{}]", searchRequest);
            SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);

            updateCounterpartyStatusInChineseWall(searchResponse, reportDate);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
        log.info("Successfully counterpartyStatus Update in ChineseWall Report Report");
    }

    public void updateCounterpartyStatusInChineseWall(SearchResponse<ChineseWall> searchResponse, String reportDate) {
        Map<String, Long> distinctLabel = new TreeMap<>();

        CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();
        for (CompositeBucket bucket : compositeAgg.buckets().array()) {
            String compositeValue = bucket.key().get("composite").stringValue();
            long docCount = bucket.docCount();
            distinctLabel.put(compositeValue, docCount);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (String label : distinctLabel.keySet()) {
            executorService.execute(() -> {
                Map<Object, Object> updateData = getCounterpartyStatus(label, reportDate);
                Query reportDateQuery = TermQuery.of(t ->
                        t.field("reportDate").value(reportDate))._toQuery();
                Query dspLabelQuery = TermQuery.of(t ->
                        t.field("counterpartLabel.keyword").value(label))._toQuery();
                Query boolQuery = BoolQuery.of(b ->
                        b.must(reportDateQuery).must(dspLabelQuery))._toQuery();
                Map<String, JsonData> params = new HashMap<>();
                for (Map.Entry<Object, Object> entry : updateData.entrySet()) {
                    params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
                }
                // Updating Based on GroupLabels
                UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                        .index(ChineseWall.INDEX_NAME)
                        .script(s -> s.inline(InlineScript.of(il ->
                                il.source("ctx._source.counterpartyStatus = params.counterpartyStatus;").
                                        lang("painless").
                                        params(params)))).
                        query(boolQuery));
                try {
                    esClient.updateByQuery(updateByQueryRequest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executorService.shutdown();
        log.info("Successfully counterpartyStatus field Updated");
    }

    public Map<Object, Object> getCounterpartyStatus(String template, String reportDate) {
        Map<Object, Object> statusMap = new HashMap<>();
        String counterpartyStatus = "";
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query templateQuery = TermQuery.of(t -> t.field("dspLabel.keyword").value(template))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)).index(CounterPartyDocument.INDEX_NAME));
        SearchResponse<CounterPartyDocument> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Hit<CounterPartyDocument> hit : searchResponse.hits().hits()) {
            counterpartyStatus = Objects.requireNonNull(hit.source()).getStatus();
        }
        statusMap.put("counterpartyStatus", counterpartyStatus);
        return statusMap;
    }

    @SneakyThrows
    public ResponseEntity<?> updateChineseWallTemplate(String reportDate) {
        Boolean chineseWallTemplateIsEmpty = checkChineseWallTemplateInCounterParty(reportDate);
        if (chineseWallTemplateIsEmpty) {
            //updateChineseWallTemplate(reportDate);
            log.info("Starting update to ChineseWall Template in counterparty report .....");
            Map<String, FieldValue> afterKey = null;
            int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);
            // search to determine the total number of hits
            Query initialQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(ChineseWall.INDEX_NAME).query(initialQuery));
            long totalCount = esClient.count(totalCountRequest).count();
            long iterationCount = (totalCount / pageSize) + 1;
            log.info("total count : {} iterationCount : {}", totalCount, iterationCount);
            for (int i = 0; i < iterationCount; i++) {
                List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
                CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.displaycolumnval")));
                Map<String, CompositeAggregationSource> map = new HashMap<>();
                map.put("composite", aggregationSource);
                compoAgg.add(map);
                Aggregation aggregation;
                Map<String, FieldValue> finalAfterKey = afterKey;
                if (afterKey != null) {
                    aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey))._toAggregation();
                } else {
                    aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg))._toAggregation();
                }
                SearchRequest searchRequest = SearchRequest.of(sr -> sr
                        .index(ChineseWall.INDEX_NAME)
                        .query(initialQuery)
                        .aggregations("Aggregation", aggregation)
                        .size(pageSize));
                SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);
                List<String> inActiveTemplate;
                try {
                    inActiveTemplate = getInActiveTemplate(reportDate);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                updateChineseWallTemplateInCounterparty(searchResponse, reportDate, inActiveTemplate);
                CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();
                afterKey = compositeAgg.afterKey();
                if (afterKey == null) {
                    break;
                }
            }
            return new ResponseEntity<>("Successfully ChineseWallTemplate Update in Counterparty Report", HttpStatus.OK);

        } else {
            return new ResponseEntity<>("Already ChineWall Template field  had  updated in  counterparty Report", HttpStatus.OK);
        }
    }

    private Boolean checkChineseWallTemplateInCounterParty(String reportDate) throws IOException {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query activeTemp = BoolQuery.of(b ->
                b.should(ExistsQuery.of(e -> e.field("activeChinesewallTemplate"))._toQuery()).
                        should(TermQuery.of(t -> t.field("activeChinesewallTemplate.keyword").value(""))._toQuery()))._toQuery();
        Query inActiveTemp = BoolQuery.of(b ->
                b.should(ExistsQuery.of(e -> e.field("inActiveChinesewallTemplate"))._toQuery()).
                        should(TermQuery.of(t -> t.field("inActiveChinesewallTemplate.keyword").value(""))._toQuery()))._toQuery();
        Query bool = BoolQuery.of(b -> b.must(activeTemp).must(inActiveTemp).must(reportDateQuery))._toQuery();
        CountRequest searchRequest = CountRequest.of(s -> s.query(bool).index(CounterPartyDocument.INDEX_NAME));
        CountResponse countResponse = esClient.count(searchRequest);
        if (countResponse.count() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public void updateChineseWallTemplateInCounterparty(SearchResponse<ChineseWall> searchResponse, String reportDate, List<String> inActiveTemplate) {
        Map<String, Long> distinctLabel = new TreeMap<>();
        CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();
        for (CompositeBucket bucket : compositeAgg.buckets().array()) {
            String compositeValue = bucket.key().get("composite").stringValue();
            long docCount = bucket.docCount();
            distinctLabel.put(compositeValue, docCount);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (String label : distinctLabel.keySet()) {
            executorService.execute(() -> {
                // get ChineseWall template
                Map<Object, Object> updateData = getChineseWallTemplate(label, reportDate, inActiveTemplate);
                Query reportDateQuery = TermQuery.of(t ->
                        t.field("reportDate").value(reportDate))._toQuery();
                Query dspLabelQuery = TermQuery.of(t ->
                        t.field("dspLabel.keyword").value(label))._toQuery();
                Query boolQuery = BoolQuery.of(b ->
                        b.must(reportDateQuery).must(dspLabelQuery))._toQuery();

                Map<String, JsonData> params = new HashMap<>();
                for (Map.Entry<Object, Object> entry : updateData.entrySet()) {
                    params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
                    params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
                }

                // Updating Based on GroupLabels
                UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                        .index(CounterPartyDocument.INDEX_NAME)
                        .script(s -> s.inline(InlineScript.of(il ->
                                il.source("ctx._source.inActiveChinesewallTemplate = params.inActiveChinesewallTemplate;" +
                                                "ctx._source.activeChinesewallTemplate = params.activeChinesewallTemplate").
                                        lang("painless").
                                        params(params)))).
                        query(boolQuery));
                try {
                    esClient.updateByQuery(updateByQueryRequest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executorService.shutdown();
    }


    public Map<Object, Object> getChineseWallTemplate(String counterpartyLabel, String reportDate, List<String> inActiveTemplate) {

        Map<Object, Object> templateMap = new HashMap<>();
        List<String> chineseWallTemplate = new ArrayList<>();
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query counterpartLabelQuery = TermQuery.of(t -> t.field("counterpartLabel.keyword").value(counterpartyLabel))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(counterpartLabelQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)).index(ChineseWall.INDEX_NAME));
        SearchResponse<ChineseWall> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, ChineseWall.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Hit<ChineseWall> hit : searchResponse.hits().hits()) {
            String status = Objects.requireNonNull(hit.source()).getTemplateLabel();
            chineseWallTemplate.add(status);
        }

        if (chineseWallTemplate.isEmpty()) {
            chineseWallTemplate.add("");
        }
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        for (String temp : chineseWallTemplate) {
            if (inActiveTemplate.contains(temp.toLowerCase())) {
                list1.add(temp);
            } else {
                list2.add(temp);
            }
        }
        if (list1.isEmpty()) {
            list1.add("");
        } else if (list2.isEmpty()) {
            list2.add("");
        }
        templateMap.put("inActiveChinesewallTemplate", list1);
        templateMap.put("activeChinesewallTemplate", list2);
        // return chineseWallTemplate;
        templateMap.put("chineseWallTemplate", chineseWallTemplate);
        return templateMap;
    }

    public List<String> getInActiveTemplate(String reportDate) throws IOException {
        List<String> inActiveTemplate = new ArrayList<>();
        Query repDate = TermQuery.of(q -> q.field("reportDate").value(reportDate))._toQuery();
        Query inActiveTemp = BoolQuery.of(b -> b.should(TermQuery.of(t -> t.field("inActiveGroupLabel.keyword").value(""))._toQuery()).should(BoolQuery.of(r -> r.mustNot(ExistsQuery.of(e -> e.field("inActiveGroupLabel"))._toQuery()))._toQuery()).minimumShouldMatch("1"))._toQuery();
        Query activeTemp = BoolQuery.of(b -> b.should(TermQuery.of(t -> t.field("activeGroupLabel.keyword").value(""))._toQuery()).should(BoolQuery.of(r -> r.mustNot(ExistsQuery.of(e -> e.field("activeGroupLabel"))._toQuery()))._toQuery()).minimumShouldMatch("1"))._toQuery();
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Aggregation agg = Aggregation.of(a -> a.terms(TermsAggregation.of(t -> t.field("templateLabel.keyword").size(10000))));
        aggregation.put("unique_categories", agg);
        Query mustQuery = BoolQuery.of(m -> m.must(repDate).must(inActiveTemp).must(activeTemp))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(ChineseWall.INDEX_NAME).query(mustQuery).size(1).aggregations(aggregation));
        SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);
        if (searchResponse.aggregations() != null && searchResponse.aggregations().get("unique_categories") != null) {
            if (searchResponse.aggregations().get("unique_categories").isSterms()) {
                searchResponse.aggregations().get("unique_categories").sterms().buckets().array().forEach(b -> inActiveTemplate.add(
                        b.key().stringValue()));
            } else if (searchResponse.aggregations().get("unique_categories").isLterms()) {
                searchResponse.aggregations().get("unique_categories").lterms().buckets().array().forEach(b -> inActiveTemplate.add(
                        String.valueOf(b.key())));
            }
        }
        Set<String> uniqueTemplates = new LinkedHashSet<>(inActiveTemplate);
        inActiveTemplate.clear();
        inActiveTemplate.addAll(uniqueTemplates);
        return inActiveTemplate;
    }


    public List<Document> getChartCount(List<String> templateLabelList, String reportDate) throws IOException {
        List<Document> documents = new ArrayList<>();
        // getting all possibility List
        List<List<String>> subList = generateSubsets(templateLabelList);
        log.info("sublist :{}", subList);
        // Loop All Possibility list and get count
        Document document = new Document();
        for (List<String> newList : subList) {
            log.info("newList :{} , length:{}", newList, newList.toString().split(",").length);
            document = getCountOfCommonTemplateLabel(newList, reportDate);
            documents.add(document);
        }
        if (templateLabelList.size() == 2 || templateLabelList.size() == 3 || templateLabelList.size() == 4) {
            documents = adjustCounts(documents, templateLabelList.size());
        }
        //adjustCounts(documents, singleKeyDoc, tripleKeyDoc, templateLabelList.size());
        return documents;
    }

    public static List<List<String>> generateSubsets(List<String> inputList) {
        List<List<String>> subsets = new ArrayList<>();
        int n = inputList.size();
        // Generate all possible combinations using bitmasking
        for (int i = 1; i < (1 << n); i++) {
            List<String> subset = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    subset.add(inputList.get(j));
                }
            }
            subsets.add(subset);
        }
        log.info("All Possible Co-ordinates:{}", subsets);
        return subsets;
    }


    public Document getCountOfCommonTemplateLabel(List<String> templateLabelList, String reportDate) throws IOException {
        Map<String, Long> uniqueFilterValues = new TreeMap<>();
        Document document = new Document();
        SearchResponse<ChineseWall> searchResponse = null;
        Map<String, FieldValue> afterKey = null;

        log.info("Template Label List :{}", templateLabelList);
        int iterationCount = getIterationCount(templateLabelList, reportDate);


        for (int i = 0; i < iterationCount; i++) {
            var array = new ArrayList<FieldValue>();
            for (String templateLabel : templateLabelList) {
                array.add(FieldValue.of(templateLabel));
            }
            Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
            Query templateLabelQuery = TermsQuery.of(t -> t.field("templateLabel.keyword").terms(TermsQueryField.of(ts -> ts.value(array))))._toQuery();
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateLabelQuery))._toQuery();


            int targetCount = templateLabelList.size();
            Map<String, JsonData> params = new HashMap<>();
            params.put("targetCount", JsonData.of(targetCount));


            Script script = Script.of(s -> s.inline(InlineScript.of(ii -> ii.source("params.count_of_counterpartLabel_Label == params.targetCount").params(params))));

            HashMap<String, Aggregation> aggregation = new HashMap<>();

            Map<String, String> bucketsPathMap = new HashMap<>();
            bucketsPathMap.put("count_of_counterpartLabel_Label", "unique_template_Label");

            Aggregation unique_template_Label = CardinalityAggregation.of(c -> c.field("templateLabel.keyword"))._toAggregation();

            Aggregation common_Template = Aggregation.of(a -> a.bucketSelector(BucketSelectorAggregation.of(b -> b.bucketsPath(
                    BucketsPath.of(p -> p.dict(bucketsPathMap))).script(script))._toAggregation().bucketSelector()));

            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();

            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.keyword")));

            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("composite", aggregationSource);
            compoAgg.add(map);

            Aggregation newAggregation;
            if (afterKey != null) {
                Map<String, FieldValue> finalAfterKey = afterKey;
                newAggregation = CompositeAggregation.of(c -> c.size(environment.getProperty("aggregationPageSize", Integer.class)).sources(compoAgg).after(finalAfterKey))._toAggregation();
            } else {
                newAggregation = CompositeAggregation.of(c -> c.size(environment.getProperty("aggregationPageSize", Integer.class)).sources(compoAgg))._toAggregation();
            }
            Aggregation counterpartLabel = Aggregation.of(aa -> aa.composite(newAggregation.composite()).aggregations("unique_template_Label", unique_template_Label).aggregations("common_Template", common_Template));
            aggregation.put("counterpartLabel", counterpartLabel);
            SearchRequest searchRequest = SearchRequest.of(s -> s.aggregations(aggregation).index(ChineseWall.INDEX_NAME).size(0).query(boolQuery));

            log.info("venn search request:{}", searchRequest);


            searchResponse = esClient.search(searchRequest, ChineseWall.class);
            CompositeAggregate compositeAggs = searchResponse.aggregations().get("counterpartLabel").composite();
            for (CompositeBucket bucket : compositeAggs.buckets().array()) {
                String compositeValue = bucket.key().get("composite").stringValue();
                long docCount = bucket.docCount();
                uniqueFilterValues.put(compositeValue, docCount);
            }
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("counterpartLabel").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (String label : templateLabelList) {
            joiner.add(label);
        }
        String coordinatesString = joiner.toString();
        document.put("coordinates", coordinatesString);
        document.put("count", uniqueFilterValues.values().size());

        int numberOfTemplateLabel = coordinatesString.split(",").length;
        if (numberOfTemplateLabel == 1) {
            document.put("counterpartLabel", "");
        } else {
            document.put("counterpartLabel", uniqueFilterValues.keySet());
        }
        return document;
    }

    public int getIterationCount(List<String> templateLabelList, String reportDate) throws IOException {
        long iterationCount = 0;
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();

        for (String templateLabel : templateLabelList) {

            Query templateLabelQuery = TermQuery.of(t -> t.field("templateLabel.keyword").value(templateLabel))._toQuery();
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateLabelQuery))._toQuery();
            CountRequest countRequest = CountRequest.of(c -> c.index(ChineseWall.INDEX_NAME).query(boolQuery));
            log.info("venn diagram Iteration countRequest :{}", countRequest);

            CountResponse countResponse = esClient.count(countRequest);
            long newCount = countResponse.count();

            log.info("new count:{} Iteration count:{}", newCount, iterationCount);

            if (newCount > iterationCount) {
                iterationCount = newCount + iterationCount;
            }
        }

        long finalCount = Math.floorDiv(iterationCount, environment.getProperty("aggregationPageSize", Integer.class));
        log.info("final count :{}", finalCount);
        return (int) finalCount + 1;
    }

    private List<Document> adjustCounts(List<Document> documents, int templateSize) {
        Map<String, Integer> countsMap = new HashMap<>();
        log.info("Venn Diagram - templateSize:{}", templateSize);
        switch (templateSize) {
            case 2: {
                Integer maxIntersectCount = getMaxIntersectCount(documents, templateSize, countsMap);
                countsMap = getMaxIntersectCountMap(documents, templateSize, countsMap);
                log.info("case 2 before - counts Map:{}", countsMap);
                for (Document doc1 : documents) {
                    String coordinates = doc1.getString("coordinates");
                    Integer doc1Count = doc1.getInteger("count");
                    if (!coordinates.contains(",")) {
                        doc1Count = doc1Count - maxIntersectCount;
                        // subtract A & B & C A ∩ B ∩ C
                        countsMap.put(coordinates, doc1Count);
                    }
                }
                log.info("case 2 after - countsMap :{}", countsMap);
                break;
            }
            case 3: {
                Integer doc3Count = 0;
                for (Document doc3 : documents) {
                    if (doc3.getString("coordinates").contains(",") && doc3.getString("coordinates").split(",").length == 3) {
                        doc3Count = doc3.getInteger("count");
                        countsMap.put(doc3.getString("coordinates"), doc3Count);
                    }
                }
                log.info("case 3 - doc3Count:{}", doc3Count);
                for (Document doc2 : documents) {
                    String coordinates = doc2.getString("coordinates");
                    int count = doc2.getInteger("count");
                    Integer doc2Count = 0;
                    if (coordinates.contains(",") && coordinates.split(",").length == 2) {
                        doc2Count = doc2.getInteger("count") - doc3Count;
                        countsMap.put(coordinates, doc2Count);
                    }
                }
                for (Document doc1 : documents) {
                    String coordinates = doc1.getString("coordinates");
                    Integer doc1Count = doc1.getInteger("count");
                    if (!coordinates.contains(",")) {
                        for (String key : countsMap.keySet()) {
                            if (key.contains(coordinates) && key.split(",").length == 2) {
                                log.info("key value :{}", countsMap.get(key));
                                doc1Count = doc1Count - countsMap.get(key);
                            }
                        }
                        doc1Count = doc1Count - doc3Count;
                        countsMap.put(coordinates, doc1Count);
                    }
                }
                log.info("case 3 - after countsMap :{}", countsMap);
                break;
            }
            // hold not used now for 4
            case 4: {
                Integer maxIntersectCount = getMaxIntersectCount(documents, templateSize, countsMap);
                countsMap = getMaxIntersectCountMap(documents, templateSize, countsMap);
                log.info("case 4 before - counts Map:{}", countsMap);
                // for 3
                for (Document doc2 : documents) {
                    String coordinates = doc2.getString("coordinates");
                    int count = doc2.getInteger("count");
                    Integer doc2Count = 0;
                    if (coordinates.contains(",") && coordinates.split(",").length == 3) {
                        doc2Count = doc2.getInteger("count") - maxIntersectCount;
                        countsMap.put(coordinates, doc2Count);
                    }
                }
                // for 2
                for (Document doc1 : documents) {
                    String coordinates = doc1.getString("coordinates");
                    Integer doc1Count = doc1.getInteger("count");
                    if (coordinates.contains(",") && coordinates.split(",").length == 2) {
                        //log.info("case 4  2 coodrinates coordinates :{}  count:{}", coordinates, doc1Count);
                        getTwoIntersectCount(documents, coordinates, doc1Count, countsMap, maxIntersectCount);
                    }
                }
                // for 1
                for (Document doc : documents) {
                    String coordinates = doc.getString("coordinates");
                    Integer docCount = doc.getInteger("count");
                    if (!coordinates.contains(",")) {
                        int totalCnt = 0;
                        for (String key : countsMap.keySet()) {
                            if (key.contains(coordinates)) {
                                //log.info("case 4 -2 val count - counts Map:", countsMap.get(key));
                                totalCnt = totalCnt + countsMap.get(key);
                            }
                            log.info("case 4  2 tptalcount  count:{}", totalCnt);
                        }
                        docCount = docCount - totalCnt;
                        countsMap.put(coordinates, docCount);
                    }
                }
                log.info("case 4 -after - counts Map:{}", countsMap);
                break;
            }
        }
        // Create the updated list of documents
        List<Document> updatedDocuments = new ArrayList<>();
        for (Document doc : documents) {
            String coordinates = doc.getString("coordinates");
            log.info("update coordinates:{}", coordinates);
            //if (!coordinates.contains(",")) {
            log.info("update inside if coordinates:{} count :{}", coordinates, countsMap.get(coordinates));
            doc.put("count", countsMap.get(coordinates));
            //}
            updatedDocuments.add(doc);
        }
        return updatedDocuments;
    }


    private Map<String, Integer> getMaxIntersectCountMap(List<Document> documents, int templateSize, Map<String, Integer> countsMap) {
        Integer docCount = 0;
        for (Document doc3 : documents) {
            if (doc3.getString("coordinates").contains(",") && doc3.getString("coordinates").split(",").length == templateSize) {
                docCount = doc3.getInteger("count");
                countsMap.put(doc3.getString("coordinates"), docCount);
                return countsMap;
            }
        }
        return countsMap;
    }

    private Integer getMaxIntersectCount(List<Document> documents, int templateSize, Map<String, Integer> countsMap) {
        Integer docCount = 0;
        for (Document doc3 : documents) {
            if (doc3.getString("coordinates").contains(",") && doc3.getString("coordinates").split(",").length == templateSize) {
                docCount = doc3.getInteger("count");
                countsMap.put(doc3.getString("coordinates"), docCount);
                return docCount;
            }
        }
        return docCount;
    }


    private Map<String, Integer> getTwoIntersectCount(List<Document> documents, String keyToCheck, Integer keytoCheckCount,
                                                      Map<String, Integer> countsMap, Integer maxIntersectCount) {
        Integer docCount = 0;
        log.info("twoCoordinatesKey :{} ", keyToCheck);
        for (Document doc1 : documents) {
            String coordinates = doc1.getString("coordinates");
            Integer doc1Count = doc1.getInteger("count");

            if (coordinates.contains(",") && coordinates.split(",").length == 3) {
                log.info("case 4  coordinates :{}  count:{} ", coordinates, doc1Count);
                //for (String mapKey : countsMap.keySet()) {
                log.info("checkKeyInMap mapkey : {} ---- keytocheck:{} ", coordinates, keyToCheck);
                //if (sortKey(coordinates).contains(sortKey(keyToCheck))) {
                if (containsAllElements(keyToCheck, coordinates)) {
                    log.info("exists in key map : {}", coordinates);
                    docCount = docCount + doc1.getInteger("count");
                }
                //}
            }
        }
        docCount = docCount - maxIntersectCount;
        docCount = keytoCheckCount - docCount;
        countsMap.put(keyToCheck, docCount);
        return countsMap;
    }

    public static boolean containsAllElements(String strA, String strB) {
        // convert both strings into sets
        Set<String> setA = new HashSet<>(Arrays.asList(strA.split(",\\s*")));
        Set<String> setB = new HashSet<>(Arrays.asList(strB.split(",\\s*")));
        return setB.containsAll(setA);
    }


    public List<String> getTotalCpLabel(List<String> uniqueSetTemplateLabel, String reportDate, String type) throws IOException {

        Map<String, FieldValue> afterKey = null;
        List<String> cpLabel = new ArrayList<>();
        int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);

        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();

        var array = new ArrayList<FieldValue>();
        for (String intersectionList : uniqueSetTemplateLabel) {
            array.add(FieldValue.of(intersectionList));
        }
        Query templateLabelQuery = TermsQuery.of(ts -> ts.field("templateLabel.keyword").terms(t -> t.value(array)))._toQuery();
        //  Query templateLabelQuery = TermQuery.of(t -> t.field("templateLabel.keyword").value(uniqueSetTemplateLabel))._toQuery();
        Query bool = BoolQuery.of(b -> b.must(reportDateQuery).must(templateLabelQuery))._toQuery();
        CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(ChineseWall.INDEX_NAME).query(bool));
        long totalCount = esClient.count(totalCountRequest).count();
        long iterationCount = (totalCount / pageSize) + 1;
        log.info("total count : {} iterationCount : {}", totalCount, iterationCount);

        for (int i = 0; i < iterationCount; i++) {
            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("composite", aggregationSource);
            compoAgg.add(map);

            Aggregation aggregation;
            Map<String, FieldValue> finalAfterKey = afterKey;
            if (afterKey != null) {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey))._toAggregation();
            } else {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg))._toAggregation();
            }
            SearchRequest searchRequest;
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateLabelQuery))._toQuery();


            if (type.equalsIgnoreCase("uniqueSet")) {
                searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(ChineseWall.INDEX_NAME).aggregations("Aggregation", aggregation).size(2));
            } else {
                //Aggregation common_counterpartLabel = Aggregation.of(a -> a.terms(TermsAggregation.of(ts -> ts.field("counterpartLabel.keyword").minDocCount(2).size(10000))));
                searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(ChineseWall.INDEX_NAME).aggregations("Aggregation", aggregation).size(2));
            }
            SearchResponse<?> searchResponse = esClient.search(searchRequest, ChineseWall.class);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();

            if (type.equalsIgnoreCase("uniqueSet")) {
                List<String> extractedKeys = extractCompositeKeys(compositeAgg.buckets().array());
                cpLabel.addAll(extractedKeys);
            } else {
                compositeAgg.buckets().array().forEach(bucket -> {
                    List<String> newList = new ArrayList<>();
                    if (bucket.docCount() == 2) {
                        Map<String, FieldValue> key = bucket.key();
                        FieldValue counterpartLabelValue = key.get("composite");
                        if (counterpartLabelValue != null) {
                            cpLabel.add(counterpartLabelValue.stringValue().toUpperCase());
                        }
                    }

                    cpLabel.addAll(newList);
                });
            }

            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }

        return cpLabel;
    }

    public List<String> extractCompositeKeys(List<CompositeBucket> buckets) {
        List<String> cpLabel = new ArrayList<>();
        for (CompositeBucket bucket : buckets) {
            Map<String, FieldValue> keyMap = bucket.key();
            for (FieldValue fieldValue : keyMap.values()) {
                String valueString = fieldValue.isString() ? fieldValue.stringValue() : fieldValue.toString();
                cpLabel.add(valueString);
            }
        }
        return cpLabel;
    }

    @SneakyThrows
    public List<String> getUniqueSetCounterpartyData(List<String> uniqueTemplateLabel, List<String> coordinates, List<String> intersection, String exclusion, List<String> intersectionAll, String reportDate) {
        List<String> counterpartLabelList;
        if (intersection.isEmpty() && (exclusion.isEmpty()) && (intersectionAll.isEmpty())) {
            counterpartLabelList = getUniqueSetCounterpartLabel(uniqueTemplateLabel, coordinates, reportDate);
        } else if (uniqueTemplateLabel.isEmpty() && (coordinates.isEmpty()) && (intersectionAll.isEmpty())) {
            counterpartLabelList = getIntersectionOfTwoTemplate(intersection, exclusion, reportDate);
        } else {
            counterpartLabelList = getTotalCpLabelOfThree(intersectionAll, reportDate);
        }
        return counterpartLabelList;
    }


    public List<String> getUniqueSetCounterpartLabel(List<String> uniqueTemplateLabel, List<String> coordinates, String reportDate) throws IOException {
        log.info("Starting getUniqueSetCounterpartLabel...");
        Query boolQuery = null;
        Map<String, FieldValue> afterKey = null;
        List<String> cpLabel = new ArrayList<>();
        int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);
        List<String> templateLabelList;
        if (coordinates.size() == 1) {
            log.info("Getting  unique Counterpart Label Between TWO Sets ");
            templateLabelList = getTotalCpLabel(coordinates, reportDate, "uniqueSet");
        } else {
            log.info("Getting  unique Counterpart Label Between THREE Sets ");
            templateLabelList = getTotalCpLabel(uniqueTemplateLabel, reportDate, "uniqueSet");
        }

        var array = new ArrayList<FieldValue>();
        for (String newTemplateLabel : templateLabelList) {
            array.add(FieldValue.of(newTemplateLabel));
        }
        long iterationCount = getIterationCount(uniqueTemplateLabel, reportDate);
        log.info("total count : {} iterationCount : {}", array.size(), iterationCount);

        for (int i = 0; i < iterationCount; i++) {

            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("composite", aggregationSource);
            compoAgg.add(map);

            CompositeAggregation aggregation;
            Map<String, FieldValue> finalAfterKey = afterKey;
            if (afterKey != null) {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey));
            } else {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg));
            }
            Aggregation agg = null;


            if (coordinates.size() == 2) {
                Map<String, String> bucketsPathMap = new HashMap<>();
                Script script = Script.of(s -> s.inline(InlineScript.of(ii -> ii.source("params.template1_count == 0 && params.template2_count == 0"))));
                bucketsPathMap.put("template1_count", "in_template1._count");
                bucketsPathMap.put("template2_count", "in_template2._count");

                Aggregation exclude_if_in_either_t2_or_t3 = Aggregation.of(a -> a.bucketSelector(BucketSelectorAggregation.of(b -> b.bucketsPath(
                        BucketsPath.of(p -> p.dict(bucketsPathMap))).script(script))._toAggregation().bucketSelector()));

                Query template1 = TermQuery.of(t -> t.field("templateLabel.keyword").value(coordinates.get(0)))._toQuery();
                Query template2 = TermQuery.of(t -> t.field("templateLabel.keyword").value(coordinates.get(1)))._toQuery();

                Map<String, Aggregation> aggs = new HashMap<>();
                aggs.put("in_template1", Aggregation.of(f -> f.filter(template1)));
                aggs.put("in_template2", Aggregation.of(f -> f.filter(template2)));
                aggs.put("exclude_if_in_either_t2_or_t3", exclude_if_in_either_t2_or_t3);
                Aggregation aggregations = Aggregation.of(a -> a.
                        terms(TermsAggregation.of(t ->
                                t.field("counterpartLabel.keyword").
                                        size(environment.getProperty("aggregationPageSize", Integer.class)))).aggregations(aggs));

                agg = Aggregation.of(a -> a.composite(aggregation).aggregations(aggs));
                Query cpQuery = TermsQuery.of(ts -> ts.field("counterpartLabel.keyword").terms(TermsQueryField.of(t -> t.value(array))))._toQuery();
                Query dateQuery = TermQuery.of(ts -> ts.field("reportDate").value(reportDate))._toQuery();
                //boolQuery = BoolQuery.of(b -> b.must(cpQuery))._toQuery();
                boolQuery = BoolQuery.of(b -> b.must(cpQuery).must(dateQuery))._toQuery();
            } else {
                // opposite :-
                Query cpQuery = TermsQuery.of(ts -> ts.field("counterpartLabel.keyword").terms(TermsQueryField.of(t -> t.value(array))))._toQuery();
                Query tempQuery = TermQuery.of(ts -> ts.field("templateLabel.keyword").value(uniqueTemplateLabel.get(0)))._toQuery();
                Query dateQuery = TermQuery.of(ts -> ts.field("reportDate").value(reportDate))._toQuery();
                boolQuery = BoolQuery.of(b -> b.must(tempQuery).must(dateQuery).mustNot(cpQuery))._toQuery();
                agg = Aggregation.of(a -> a.composite(aggregation));
            }

            Query finalBoolQuery = boolQuery;
            Aggregation finalAgg = agg;
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(ChineseWall.INDEX_NAME).query(finalBoolQuery).aggregations("filtered_labels", finalAgg));
            log.info("UniqueSetCounterpartLabel - Search Request - :{}", searchRequest);
            SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);


            List<CompositeBucket> buckets = searchResponse.aggregations().get("filtered_labels").composite().buckets().array();
            List<String> counterpartLabels = new ArrayList<>();
            for (CompositeBucket data : buckets) {
                Map<String, FieldValue> keyMap = data.key();
                FieldValue counterpartLabel = keyMap.get("composite");
                if (counterpartLabel != null) {
                    counterpartLabels.add(counterpartLabel.stringValue().toUpperCase());
                }
            }

            cpLabel.addAll(counterpartLabels);

            CompositeAggregate compositeAgg = searchResponse.aggregations().get("filtered_labels").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }

        }
        log.info("Total count of unique counterpart labels: {}", cpLabel.size());
        return cpLabel;
    }


    @SneakyThrows
    private List<String> getIntersectionOfTwoTemplate(List<String> intersection, String exclusion, String reportDate) {
        Map<String, FieldValue> afterKey = null;
        List<String> cpLabel = new ArrayList<>();
        Set<String> uniqueCpLabels = new HashSet<>();
        int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);

        // - Get Total cpLabel of both intersection (A&B)
        List<String> templateLabelList = getTotalCpLabel(intersection, reportDate, "intersection");

        // After Getting cpLabel wants to subratct [(A&B) -C]
        var array = new ArrayList<FieldValue>();
        for (String templateLabel : templateLabelList) {
            array.add(FieldValue.of(templateLabel));
        }

        long iterationCount = (array.size() / pageSize) + 1;
        for (int i = 0; i < iterationCount; i++) {
            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("composite", aggregationSource);
            compoAgg.add(map);
            CompositeAggregation aggregation;
            if (afterKey != null) {
                Map<String, FieldValue> finalAfterKey = afterKey;
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey));
            } else {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg));
            }

            Map<String, String> bucketsPathMap = new HashMap<>();
            Script script = Script.of(s -> s.inline(InlineScript.of(il ->
                    il.source("params.template1_count == 0"))));
            bucketsPathMap.put("template1_count", "in_template1._count");

            Aggregation excludeIfInEitherT2OrT3 = Aggregation.of(a -> a.bucketSelector(
                    BucketSelectorAggregation.of(b -> b.bucketsPath(
                            BucketsPath.of(p -> p.dict(bucketsPathMap))).script(script))._toAggregation().bucketSelector()));

            Query template1 = TermQuery.of(t -> t.field("templateLabel.keyword").value(exclusion))._toQuery();

            Map<String, Aggregation> aggs = new HashMap<>();
            aggs.put("in_template1", Aggregation.of(f -> f.filter(template1)));
            aggs.put("exclude_if_in_either_t2_or_t3", excludeIfInEitherT2OrT3);

            Aggregation agg = Aggregation.of(a ->
                    a.composite(aggregation).aggregations(aggs));

            Query cpQuery = TermsQuery.of(ts ->
                    ts.field("counterpartLabel.keyword").terms(TermsQueryField.of(t ->
                            t.value(array))))._toQuery();

            Query boolQuery = BoolQuery.of(b -> b.must(cpQuery))._toQuery();

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(ChineseWall.INDEX_NAME).query(boolQuery).aggregations("filtered_labels", agg));
            SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);

            List<CompositeBucket> buckets = searchResponse.aggregations().get("filtered_labels").composite().buckets().array();
            List<String> counterpartLabels = new ArrayList<>();
            for (CompositeBucket data : buckets) {
                Map<String, FieldValue> keyMap = data.key();
                FieldValue counterpartLabel = keyMap.get("composite");
                if (counterpartLabel != null) {
                    counterpartLabels.add(counterpartLabel.stringValue().toUpperCase());
                }
            }
            uniqueCpLabels.addAll(counterpartLabels);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("filtered_labels").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
        cpLabel.addAll(uniqueCpLabels);
        log.info("Total count of Intersertion of :{} counterpart labels: {}", intersection, cpLabel.size());
        return cpLabel;
    }


    @SneakyThrows
    private List<String> getTotalCpLabelOfThree(List<String> intersectionAll, String reportDate) {
        Map<String, FieldValue> afterKey = null;
        List<String> cpLabel = new ArrayList<>();
        int paramCount = intersectionAll.size();

        int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);

        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        var array = new ArrayList<FieldValue>();
        for (String intersectionList : intersectionAll) {
            array.add(FieldValue.of(intersectionList));
        }
        Query templateLabelQuery = TermsQuery.of(ts -> ts.field("templateLabel.keyword").terms(t -> t.value(array)))._toQuery();

        while (true) {
            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("counterpartLabel.keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("counterpartLabel", aggregationSource);
            compoAgg.add(map);

            Aggregation commonCounterpartLabel;
            if (afterKey != null) {
                Map<String, FieldValue> finalAfterKey = afterKey;
                commonCounterpartLabel = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey))._toAggregation();
            } else {
                commonCounterpartLabel = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg))._toAggregation();
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(ChineseWall.INDEX_NAME)
                    .query(BoolQuery.of(b -> b.must(reportDateQuery).must(templateLabelQuery))._toQuery())
                    .aggregations("common_counterpartLabel", commonCounterpartLabel)
                    .size(0)
            );

            SearchResponse<ChineseWall> searchResponse = esClient.search(searchRequest, ChineseWall.class);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("common_counterpartLabel").composite();
            compositeAgg.buckets().array().forEach(bucket -> {
                if (bucket.docCount() == paramCount) {
                    Map<String, FieldValue> key = bucket.key();
                    FieldValue counterpartLabelValue = key.get("counterpartLabel");
                    if (counterpartLabelValue != null) {
                        cpLabel.add(counterpartLabelValue.stringValue().toUpperCase());
                    }
                }
            });
            afterKey = compositeAgg.afterKey();
            if (afterKey == null || afterKey.isEmpty()) {
                break;
            }
        }
        log.info("Total count of Intersertion of :{} counterpart labels: {}", intersectionAll, cpLabel.size());
        return cpLabel;
    }


    @SneakyThrows(Exception.class)
    @Scheduled(cron = "${uam.cron.autoload.chinese-wall.update.expressions}")
    public void updateChineseWallTemplate() {
        log.info("Scheduler---->>>>");
        LocalDate today = LocalDate.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String reportDate = today.format(formatter);
//        String reportDate = "2024-07-29";
        // check whether chinseWallTemplate field is Update ?
        Boolean chineseWallTemplateIsEmpty = checkChineseWallTemplateInCounterParty(reportDate);
        log.info("reportDate:{}", reportDate);
        updateChineseWallTemplate(reportDate);
    }

}




