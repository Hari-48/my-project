package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.GroupPortfolioRights;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.UserGroupList;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import lombok.SneakyThrows;
import org.bson.Document;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GroupPortfolioService {
    private static final Logger log = LoggerFactory.getLogger(GroupPortfolioService.class);
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    ElasticUtils elastic_utils;
    public final Environment environment;
    private final ElasticsearchClient esClient;

    public GroupPortfolioService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }

    @SneakyThrows
    public Document getPortfolioData(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                     int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) {
        Document portfolioList = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = GroupPortfolioRights.INDEX_NAME;
        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupPortFolioRights.json", "fields");
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupPortFolioRights.json", "integerFields");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupPortFolioRights.json", "globalSearchFields");

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
                    queryList.add(elastic_utils.getRangeQuery(elasticSearchModel.getFilterColumnSearchValue(), elasticSearchModel.getFilterColumnName(), integerFields));
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
            SearchResponse<GroupPortfolioRights> searchResponse = esClient.search(searchRequest, GroupPortfolioRights.class);
            Map<Object, Long> filterColumnValues = elastic_utils.extractFilterColumnAgg(searchResponse);
            // //Fetching Filter Column Aggregation
            List<Object> filterColumnKeyList = new ArrayList<>(filterColumnValues.keySet());
            List<Long> filterColumnCountList = new ArrayList<>(filterColumnValues.values());
            List<Document> filterColumnDocuments = elastic_utils.checkDuplicateObjects(filterColumnCountList, filterColumnKeyList);
            // Map<String, Long> dropDownFilter = new TreeMap<>();
            //Fetching  - Filter Column Aggregation
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
//             total  field count in DropDown :-
//            Object dropDownFieldCount = 0;
//            dropDownFieldCount = getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName);

            Integer dropDownFieldCount = 0;
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, GroupPortfolioRights.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            portfolioList.put("filterValues", newDropDown);
            //portfolioList.put("dropDownFieldCount", dropDownFieldCount);
            portfolioList.put("dropDownFieldCount", dropDownFieldCount);
            portfolioList.put("filterColumnNumericCount", filterColNumericCount);
            portfolioList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<GroupPortfolioRights> allDocuments = new ArrayList<>();
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
                            field("groupLabel.keyword").order(sortingOrder.equalsIgnoreCase("asc")
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
            SearchResponse<GroupPortfolioRights> searchResponse = esClient.search(searchRequest, GroupPortfolioRights.class);
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
            String template = "GROUP_PORTFOLIO_RIGHTS_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));
            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            portfolioList.put("totalPage", totalPages);
            portfolioList.put("count", totalCount);
            portfolioList.put("pageScroll", pageScroll);
            portfolioList.put("content", allDocuments);
            portfolioList.put("sorting", document);
            portfolioList.put("pitId", pitId);
        }
        return portfolioList;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getPortFolioRightsExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) throws Exception {
        List<GroupPortfolioRights> portFolioList = new ArrayList<>();
        Document docs = new Document();
        fieldColumns = new ArrayList<>();
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
            Document portfolioData = getPortfolioData(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);
            portFolioList.addAll((Collection<? extends GroupPortfolioRights>) portfolioData.get("content"));
            docs = (Document) portfolioData.get("sorting");
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, portFolioList.size(), job.getId());
            totalPage = (long) portfolioData.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) portfolioData.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", portFolioList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, portFolioList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }

    public void updateStatus(String repDate) throws IOException {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(repDate))._toQuery();
        Query groupStatusQuery = TermQuery.of(t -> t.field("status.keyword").value("inactive"))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(groupStatusQuery))._toQuery();
        SearchRequest userGroupListRequest = SearchRequest.of(s ->
                s.index(UserGroupList.INDEX_NAME).
                        query(boolQuery).
                        size(environment.getProperty("aggregationPageSize", Integer.class)));
        //  log.info("Inactive GroupList Request :{}", userGroupListRequest);
        SearchResponse<UserGroupList> userGroupListResponse = null;
        try {
            userGroupListResponse = esClient.search(userGroupListRequest, UserGroupList.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("GroupList Response count :{}", userGroupListResponse.hits().hits().size());
        if (!userGroupListResponse.hits().hits().isEmpty()) {
            userGroupListResponse.hits().hits().forEach(hit -> {
                UserGroupList groupList = hit.source();
                if (groupList != null) {
                    Query groupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(groupList.getGroupLabel()))._toQuery();
                    Query portfolioBoolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(groupLabelQuery))._toQuery();
                    SearchRequest groupPortFolioRequest = SearchRequest.of(s -> s.index(GroupPortfolioRights.INDEX_NAME).query(portfolioBoolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));
                    SearchResponse<GroupPortfolioRights> groupPortFolioRightsResponse = null;
                    try {
                        groupPortFolioRightsResponse = esClient.search(groupPortFolioRequest, GroupPortfolioRights.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String, Object> portfolioMap = new HashMap<>();
                    portfolioMap.put("status", "inactive");
                    if (!groupPortFolioRightsResponse.hits().hits().isEmpty()) {
                        groupPortFolioRightsResponse.hits().hits().forEach(portfolioHit -> {
                                    String documentId = portfolioHit.id();
                                    UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u ->
                                            u.index(GroupPortfolioRights.INDEX_NAME).
                                                    id(documentId).
                                                    doc(portfolioMap).
                                                    retryOnConflict(3));
                                    try {
                                        esClient.update(updateRequest, GroupPortfolioRights.class);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
                    }
                }
            });
        }
    }

    public Document getCount(String reportDate, String sortOrder, String sortBy) throws IOException {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        String value = "*" + "suspend" + "*";
        Query wildCardQuery = WildcardQuery.of(w -> w.field("description.keyword").value(value))._toQuery();
        //  Suspended word present in Closing Entity Field
        Query mustQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(wildCardQuery))._toQuery();
        //  Suspended word NOT  present in Closing Entity Field
        Query mustNotQuery = BoolQuery.of(b -> b.must(reportDateQuery).mustNot(wildCardQuery))._toQuery();
        // Total Count of Each closingEntity Field
        Query totalCountQuery = BoolQuery.of(b -> b.must(reportDateQuery))._toQuery();
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Map<String, Long> suspended = new TreeMap<>();
        Map<String, Long> nonSuspended = new TreeMap<>();
        Map<String, Long> totalCount = new TreeMap<>();
        Document document = new Document();
        NamedValue<SortOrder> sort = NamedValue.of("_key", SortOrder.Asc);
        // NamedValue<SortOrder> sort1 = NamedValue.of("_key", SortOrder.Desc);
        Aggregation aggregations = Aggregation.of(a ->
                a.terms(TermsAggregation.of(t ->
                        t.field("closingEntity.displaycolumnval").order(List.of(sort)).size(environment.getProperty("aggregationPageSize", Integer.class)))));
        aggregation.put("closingEntity", aggregations);
        SearchRequest mustRequest = SearchRequest.of(s -> s.index(GroupPortfolioRights.INDEX_NAME).
                query(mustQuery).
                aggregations(aggregation).
                size(environment.getProperty("aggregationPageSize", Integer.class)));
        SearchRequest mustNotRequest = SearchRequest.of(s -> s.index(GroupPortfolioRights.INDEX_NAME).
                query(mustNotQuery).
                aggregations(aggregation).
                size(environment.getProperty("aggregationPageSize", Integer.class)));
        SearchRequest totalCountRequest = SearchRequest.of(s -> s.index(GroupPortfolioRights.INDEX_NAME).
                query(totalCountQuery).
                aggregations(aggregation).
                size(environment.getProperty("aggregationPageSize", Integer.class)));
        SearchResponse<GroupPortfolioRights> mustResponse = esClient.search(mustRequest, GroupPortfolioRights.class);
        SearchResponse<GroupPortfolioRights> mustNotResponse = esClient.search(mustNotRequest, GroupPortfolioRights.class);
        SearchResponse<GroupPortfolioRights> totalCountResponse = esClient.search(totalCountRequest, GroupPortfolioRights.class);
        if (mustResponse.aggregations() != null && mustResponse.aggregations().get("closingEntity") != null) {
            if (mustResponse.aggregations().get("closingEntity").isSterms()) {
                mustResponse.aggregations().get("closingEntity").sterms().buckets().array().forEach(b -> suspended.put(
                        b.key().stringValue(), b.docCount()));
            } else if (mustResponse.aggregations().get("closingEntity").isLterms()) {
                mustResponse.aggregations().get("closingEntity").lterms().buckets().array().forEach(b -> suspended.put(
                        String.valueOf(b.key()), b.docCount()));
            }
        }
        if (mustNotResponse.aggregations() != null && mustNotResponse.aggregations().get("closingEntity") != null) {
            if (mustNotResponse.aggregations().get("closingEntity").isSterms()) {
                mustNotResponse.aggregations().get("closingEntity").sterms().buckets().array().forEach(b -> nonSuspended.put(
                        b.key().stringValue(), b.docCount()));
            } else if (mustNotResponse.aggregations().get("closingEntity").isLterms()) {
                mustNotResponse.aggregations().get("closingEntity").lterms().buckets().array().forEach(b -> nonSuspended.put(
                        String.valueOf(b.key()), b.docCount()));
            }
        }
        if (totalCountResponse.aggregations() != null && totalCountResponse.aggregations().get("closingEntity") != null) {
            if (totalCountResponse.aggregations().get("closingEntity").isSterms()) {
                totalCountResponse.aggregations().get("closingEntity").sterms().buckets().array().forEach(b -> totalCount.put(
                        b.key().stringValue(), b.docCount()));
            } else if (totalCountResponse.aggregations().get("closingEntity").isLterms()) {
                totalCountResponse.aggregations().get("closingEntity").lterms().buckets().array().forEach(b -> totalCount.put(
                        String.valueOf(b.key()), b.docCount()));
            }
        }
        document.put("totalCount", totalCount);
        document.put("suspend", suspended);
        document.put("nonSuspend", nonSuspended);
        Comparator<Map.Entry<String, Long>> dynamicComparator = (entry1, entry2) -> {
            Long value1 = entry1.getValue();
            Long value2 = entry2.getValue();
            if (sortOrder.equalsIgnoreCase("asc")) {
                return value1.compareTo(value2);
            } else {
                return value2.compareTo(value1);
            }
        };
        // Sort the specified field based on dynamicComparator
        Map<String, Long> sortedField;
        if (sortBy.equalsIgnoreCase("suspended")) {
            sortedField = sortMap(suspended, dynamicComparator);
        } else if (sortBy.equalsIgnoreCase("nonSuspended")) {
            sortedField = sortMap(nonSuspended, dynamicComparator);
        } else {
            sortedField = sortMap(totalCount, dynamicComparator);
        }
        // Reflect the same sorting order changes in other two fields
        Map<String, Long> sortedSuspend = new LinkedHashMap<>();
        Map<String, Long> sortedNonSuspend = new LinkedHashMap<>();
        Map<String, Long> sortedTotalCount = new LinkedHashMap<>();

        sortedField.forEach((key, values) -> {
            sortedSuspend.put(key, suspended.getOrDefault(key, 0L));
            sortedNonSuspend.put(key, nonSuspended.getOrDefault(key, 0L));
            sortedTotalCount.put(key, totalCount.getOrDefault(key, 0L));
        });
//  for Suspended :- Adding remaining Data
        nonSuspended.forEach((key, val1) -> {
            if (!sortedField.containsKey(key)) {
                sortedSuspend.put(key, 0L);
                sortedNonSuspend.put(key, val1);
                sortedTotalCount.put(key, totalCount.getOrDefault(key, 0L));
            }
        });
        // Create a new Document and put the sorted maps into it
        Document sortedDocument = new Document();
        sortedDocument.put("totalCount", sortedTotalCount);
        sortedDocument.put("suspend", sortedSuspend);
        sortedDocument.put("nonSuspend", sortedNonSuspend);
        return sortedDocument;
    }

    private Map<String, Long> sortMap(Map<String, Long> map, Comparator<Map.Entry<String, Long>> comparator) {
        // Convert the map entries to a list
        List<Map.Entry<String, Long>> entryList = new ArrayList<>(map.entrySet());
        // Sort the list using the provided comparator
        entryList.sort(comparator);
        // Create a LinkedHashMap to maintain the order of insertion
        LinkedHashMap<String, Long> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    public Document comparePortfolioRightsByGroupLabel(String baseGroupLabel, String compareGroupLabel, String reportDate, String compareType) throws IOException {

        Document document = new Document();
        String sortParam = "portfolioLabel.keyword";

        List<GroupPortfolioRights> baseGroupList = getListByGroupLabel(baseGroupLabel, sortParam, reportDate);
        List<GroupPortfolioRights> compareGroupList = getListByGroupLabel(compareGroupLabel, sortParam, reportDate);

        List<GroupPortfolioRights> additionalBaseGroupData = new ArrayList<>();
        List<GroupPortfolioRights> additionalCompareGroupData = new ArrayList<>();

        List<GroupPortfolioRights> matchedData = new ArrayList<>();
        List<GroupPortfolioRights> unMatchedData = new ArrayList<>();

        // check whether the Base response & compare response is empty :-
        if (baseGroupList.isEmpty() && compareGroupList.isEmpty()) {
            document.put("content", Collections.emptyList());
            document.put("message", "There is no data for this user groups " + baseGroupLabel + " - " + compareGroupLabel);
            return document;
        }

        if (baseGroupList.isEmpty()) {
            document.put("content", Collections.emptyList());
            document.put("message", "There is no data for this user group " + baseGroupLabel);
            return document;
        }

        if (compareGroupList.isEmpty()) {
            document.put("content", Collections.emptyList());
            document.put("message", "There is no data for this user group " + compareGroupLabel);
            return document;
        }
        document.put("BaseGroup :", baseGroupLabel);
        document.put("CompareGroup", compareGroupLabel);
        document.put("TotalRowsInBaseGroup", baseGroupList.size());
        document.put("TotalRowsInCompareGroup", compareGroupList.size());


        // if (compareType.equalsIgnoreCase("summary") || compareType.equalsIgnoreCase("additionalBaseGroup") || compareType.equalsIgnoreCase("matched") || compareType.equalsIgnoreCase("unMatched")) {
            additionalBaseGroupData = getAdditionalDataInCompare(baseGroupList, compareGroupList);
            if (!additionalBaseGroupData.isEmpty()) {
                for (GroupPortfolioRights extraElement : additionalBaseGroupData) {
                    baseGroupList.remove(extraElement);
                }
            }
       // if (compareType.equalsIgnoreCase("summary") || compareType.equalsIgnoreCase("additionalCompareGroup") || compareType.equalsIgnoreCase("unMatched") || compareType.equalsIgnoreCase("matched")) {
            additionalCompareGroupData = getAdditionalDataInCompare(compareGroupList, baseGroupList);
            if (!additionalCompareGroupData.isEmpty()) {
                for (GroupPortfolioRights extraElement : additionalCompareGroupData) {
                    compareGroupList.remove(extraElement);
                }
            }
      //  }

        switch (compareType) {
            case "summary":
                document.put("AdditionalDataPresentInBaseGroup", additionalBaseGroupData.size());
                document.put("AdditionalDataPresentInCompareGroup", additionalCompareGroupData.size());

                matchedData = getCompareList(baseGroupList, compareGroupList, "matched");
                document.put("MatchedCount", matchedData.size());

                unMatchedData = getCompareList(baseGroupList, compareGroupList, "unMatched");
                document.put("UnMatchedCount", unMatchedData.size());
                break;

            case "additionalBaseGroup":
                document.put("AdditionalCountInBaseGroup", additionalBaseGroupData.size());
                document.put("List", additionalBaseGroupData);
                break;

            case "additionalCompareGroup":
                document.put("AdditionalCountInCompareGroup", additionalCompareGroupData.size());
                document.put("List", additionalCompareGroupData);
                break;
            case "matched":
                matchedData = getCompareList(baseGroupList, compareGroupList, compareType);
                document.put("MatchedCount", matchedData.size());
                document.put("List",  matchedData);
                break;
            case "unMatched":
                unMatchedData = getCompareList(baseGroupList, compareGroupList, compareType);
                document.put("UnMatchedCount", unMatchedData.size());
                document.put("List",  unMatchedData);
                break;
        }

        @SuppressWarnings("unchecked")
        List<GroupPortfolioRights> newData = (List<GroupPortfolioRights>) document.get("List");
        if (newData != null) {
            List<String> portfolioLabel = newData.stream()
                    .map(GroupPortfolioRights::getPortfolioLabel)
                    .collect(Collectors.toList());
            document.put("portfolioLabel", portfolioLabel);
        } else {
            document.put("portfolioLabel", Collections.emptyList());
        }

        return document;
    }
    public StringBuilder objectDiff(Object baseObject, Object compareObject, StringBuilder message) {
        ObjectDifferBuilder objectDifferBuilder = ObjectDifferBuilder.startBuilding();
        excludeProperties(objectDifferBuilder);
        DiffNode diff1 = objectDifferBuilder.build().compare(baseObject, compareObject);
        log.info("diff1:{}, diff1 changes:{}", diff1, diff1.hasChanges());

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

    public void excludeProperties(ObjectDifferBuilder objectDifferBuilder) {
        objectDifferBuilder.inclusion().exclude().propertyName("id");
        objectDifferBuilder.inclusion().exclude().propertyName("sysDate");
        objectDifferBuilder.inclusion().exclude().propertyName("jobId");
        objectDifferBuilder.inclusion().exclude().propertyName("reportDate");
        objectDifferBuilder.inclusion().exclude().propertyName("groupLabel");
        objectDifferBuilder.inclusion().exclude().propertyName("grpDesc");
        // objectDifferBuilder.inclusion().exclude().propertyName("mxReportDate");
    }

    public List<GroupPortfolioRights> getListByGroupLabel(String groupLabel, String sortParam, String reportDate) throws IOException {
        List<GroupPortfolioRights> portfolioList = new ArrayList<>();
        Query baseGroupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(groupLabel))._toQuery();
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        SortOptions sortOptions = SortOptions.of(s ->
                s.field(FieldSort.of(fs -> fs.field(sortParam).order(SortOrder.Desc))));

        Query baseQuery = BoolQuery.of(b -> b.must(baseGroupLabelQuery).must(reportDateQuery))._toQuery();
        SearchRequest baseSearch = SearchRequest.of(s -> s.index(GroupPortfolioRights.INDEX_NAME).query(baseQuery)
                .sort(sortOptions).size(environment.getProperty("aggregationPageSize", Integer.class)));
        log.info("Base Search :{}", baseSearch);
        SearchResponse<GroupPortfolioRights> baseResponses = esClient.search(baseSearch, GroupPortfolioRights.class);
        if (!baseResponses.hits().hits().isEmpty()) {
            for (Hit<GroupPortfolioRights> hit : baseResponses.hits().hits()) {
                GroupPortfolioRights content = hit.source();
                portfolioList.add(content);
            }
        }
        return portfolioList ;
    }
    public List<GroupPortfolioRights> getAdditionalDataInCompare(List<GroupPortfolioRights> list1, List<GroupPortfolioRights> list2 ) {
        List<GroupPortfolioRights> additionalData = new ArrayList<>(list1);
        List<String> portfolioLabels = list2.stream()
                .map(GroupPortfolioRights::getPortfolioLabel)
                .collect(Collectors.toList());
        additionalData.removeIf(item -> portfolioLabels.contains(item.getPortfolioLabel()));
        return additionalData ;
    }
    public List<GroupPortfolioRights> getCompareList(List<GroupPortfolioRights> baseGroupList, List<GroupPortfolioRights> compareGroupList, String compareType) {
        log.info("bb compareType message:{} {}",baseGroupList.size(), compareGroupList.size()) ;
        List<GroupPortfolioRights> compareData = new ArrayList<>();


        baseGroupList.sort(Comparator.comparing(GroupPortfolioRights::getPortfolioLabel)
                .thenComparing(GroupPortfolioRights::getPortfolioLabel));
        compareGroupList.sort(Comparator.comparing(GroupPortfolioRights::getPortfolioLabel)
                .thenComparing(GroupPortfolioRights::getPortfolioLabel));

        int i = 0;
        for (GroupPortfolioRights data1 : baseGroupList) {
            StringBuilder message = new StringBuilder();
            if (!(compareGroupList.isEmpty())) {
                GroupPortfolioRights data2 = compareGroupList.get(i);
                if (data2 != null) {
                    log.info("List=>1:{}\t List=>2:{}", data1.getPortfolioLabel(), data2.getPortfolioLabel());
                    message = objectDiff(data1, data2, message);
                    if(message.length() > 0 && compareType.equalsIgnoreCase("unmatched") ) {
                        log.info("aa compareType message:{} {}",compareType, message);
                        data1.setDiff(String.valueOf(message));
                        compareData.add(data1) ;
                    }
                    if(message.length() == 0 && compareType.equalsIgnoreCase("matched") ) {
                        log.info("bb compareType message:{} {}",compareType, message);
                        compareData.add(data1) ;
                    }
                }
            }
            if(message.length() != 0) {
                data1.setDiff(String.valueOf(message));
            }
            i++;
        }
        return compareData ;
    }

}



































