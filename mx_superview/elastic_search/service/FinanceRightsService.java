package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateByQueryRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.FinanceRights;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UserGroupList;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class FinanceRightsService {
    private static final Logger log = LoggerFactory.getLogger(FinanceRightsService.class);
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public FinanceRightsService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }

    public Document getFinanceRightsList(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder, String tmplType,
                                         int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) throws IOException, ParseException {

        Document financeRightsList = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = FinanceRights.INDEX_NAME;

        // Fetching Data From  JSON File .
        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/financeRights.json", "fields");
        //Fetching IntegerFields From  JSON File .
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/financeRights.json", "integerFields");
        //Fetching Global Search Fields From  JSON File .
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/financeRights.json", "globalSearchFields");

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

        // Filter by tmplType
        if (tmplType != null && !tmplType.isEmpty()) {
            if (tmplType.equals("STATTMPL")) {
                Query tmplTypeQuery = TermQuery.of(q -> q.field("tmplType").value("stat_categ_temp"))._toQuery();
                queryList.add(tmplTypeQuery);
            } else if (tmplType.equals("CTRL")) {
                Query tmplTypeQuery = TermQuery.of(q -> q.field("tmplType").value("acc_ctrl_temp"))._toQuery();
                queryList.add(tmplTypeQuery);
            }
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
            SearchResponse<FinanceRights> searchResponse = esClient.search(searchRequest, FinanceRights.class);
            //log.info(" FilterColumn Response :-  {}", searchResponse);
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
            uniqueFilterValues = elastic_utils.extractFilterColumnAggregation(searchResponse);

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
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, FinanceRights.class);
            //log.info("DropDown Count------------Cardinality :{}",dropDownFieldCount);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            financeRightsList.put("filterValues", newDropDown);
            log.info("DropDown Count------------Cardinality :{}", dropDownFieldCount);
            financeRightsList.put("dropDownFieldCount", dropDownFieldCount);
            financeRightsList.put("filterColumnNumericCount", filterColNumericCount);
            financeRightsList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<FinanceRights> allDocuments = new ArrayList<>();
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
                            field("tmplType.keyword").order(sortingOrder.equalsIgnoreCase("asc")
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
            SearchResponse<FinanceRights> searchResponse = esClient.search(searchRequest, FinanceRights.class);
            log.info("Total count in Response : {}", searchResponse.hits().hits().size());
            searchResponse.hits().hits().forEach(f -> allDocuments.add(f.source()));

            // SearchAfter - Sorting
            List<Object> sorting = new ArrayList<>();
            if (!searchResponse.hits().hits().isEmpty()) {
                sorting = elastic_utils.getFirstSortingOrder(searchResponse, integerFields, sortBy);
                document.put("sortingFirstIndex", sorting);
                sorting = elastic_utils.getLastSortingOrder(searchResponse, integerFields, sortBy);
                document.put("sortingLastIndex", sorting);
            }
            // saving the Search term - Query  :-
            String template = "FINANCE_RIGHTS_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));

            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            financeRightsList.put("totalPage", totalPages);
            financeRightsList.put("count", totalCount);
            financeRightsList.put("pageScroll", pageScroll);
            financeRightsList.put("content", allDocuments);
            financeRightsList.put("sorting", document);
            financeRightsList.put("pitId", pitId);
        }
        return financeRightsList;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getFinanceRightsListExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String tmplType, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) throws Exception {
        List<FinanceRights> financeRightsList = new ArrayList<>();
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
            Document searchFinanceRights = getFinanceRightsList(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, tmplType, pageSize, globalSearch, bulkFilterSearchType, userName);
            //   log.info("NEW RESPONSE :{}", searchGroupComboPortfolio);

            financeRightsList.addAll((Collection<? extends FinanceRights>) searchFinanceRights.get("content"));
            docs = (Document) searchFinanceRights.get("sorting");

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, financeRightsList.size(), job.getId());
            totalPage = (long) searchFinanceRights.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) searchFinanceRights.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", financeRightsList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, financeRightsList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }

    public void updateGroupLabels(String reportDate, String indexName, String reportFieldName) throws InterruptedException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
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

        log.info("Index request - Distinct {} :{}", reportFieldName, searchRequest);
        SearchResponse<?> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, Object.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("The Search Response is : {} ", searchResponse);
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

        Map<String, Long> sortedDistinctFieldValues = new TreeMap<>(Collections.reverseOrder());
        sortedDistinctFieldValues.putAll(distinctFieldValues);

        for (String fieldValue : sortedDistinctFieldValues.keySet()) {
            log.info("Field Value : {}", fieldValue);

            // Determine the appropriate fetchUserGroupFieldName based on the tmplType value
            String tmplTypeValue = getTmplTypeValue(indexName, reportDate, fieldValue);
            String fetchUserGroupFieldName = "STAT_CATEG_TEMP".equals(tmplTypeValue) ? "statTmpl" : "accCtrl";

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
            }
            UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                    .index(indexName)
                    .script(s -> s.inline(InlineScript.of(i ->
                            i.source("ctx._source.activeGroupLabel = params.activeGroupLabel; ctx._source.inActiveGroupLabel = params.inActiveGroupLabel").
                                    lang("painless").
                                    params(params)))).
                    query(boolQuery));
            try {
                esClient.updateByQuery(updateByQueryRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getTmplTypeValue(String indexName, String reportDate, String fieldValue) {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query fieldQuery = TermQuery.of(t -> t.field("template.keyword").value(fieldValue))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(fieldQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).size(1).index(indexName));
        SearchResponse<FinanceRights> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, FinanceRights.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!searchResponse.hits().hits().isEmpty()) {
            return Objects.requireNonNull(searchResponse.hits().hits().get(0).source()).getTmplType();
        }
        return null;
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
}
