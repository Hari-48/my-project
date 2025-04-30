package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
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
public class GroupListService {

    private static final Logger log = LoggerFactory.getLogger(GroupListService.class);
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    private GroupLabelDepartmentService groupLabelDepartmentService;
    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public GroupListService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }

    public Document getGroupList(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                 int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) throws IOException, ParseException {

        Document searchGroupList = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = UserGroupList.INDEX_NAME;

        // Fetching Data From  JSON File .
        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/userGroupList.json", "fields");
        //Fetching DateFields From  JSON File .
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/userGroupList.json", "integerFields");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/userGroupList.json", "globalSearchFields");

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
            SearchResponse<UserGroupList> searchResponse = esClient.search(searchRequest, UserGroupList.class);
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
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, UserGroupList.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            searchGroupList.put("filterValues", newDropDown);
            searchGroupList.put("dropDownFieldCount", dropDownFieldCount);
            searchGroupList.put("filterColumnNumericCount", filterColNumericCount);
            searchGroupList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<UserGroupList> allDocuments = new ArrayList<>();
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
            SearchResponse<UserGroupList> searchResponse = esClient.search(searchRequest, UserGroupList.class);
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
            String template = "GROUP_LIST_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));

            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            searchGroupList.put("totalPage", totalPages);
            searchGroupList.put("count", totalCount);
            searchGroupList.put("pageScroll", pageScroll);
            searchGroupList.put("content", allDocuments);
            searchGroupList.put("sorting", document);
            searchGroupList.put("pitId", pitId);
        }
        return searchGroupList;
    }


    @Async
    @Transactional

    public CompletableFuture<Void> getUserGroupExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) throws Exception {
        List<UserGroupList> userGroupLists = new ArrayList<>();
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
            Document searchGroupList = getGroupList(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);
            //   log.info("NEW RESPONSE :{}", searchGroupList);

            userGroupLists.addAll((Collection<? extends UserGroupList>) searchGroupList.get("content"));
            docs = (Document) searchGroupList.get("sorting");

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, userGroupLists.size(), job.getId());
            totalPage = (long) searchGroupList.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) searchGroupList.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", userGroupLists.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, userGroupLists, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }


    public void updateGroupLabelStatus(String reportDate) throws IOException, InterruptedException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        String userGroupAccessIndexName = UserGroupAccessRights.INDEX_NAME;

        String groupListIndex = UserGroupList.INDEX_NAME;

        // check UserGroupAccessRights Data is available for this date -( If not Available  default status Inactive will remain)
        Boolean isUserGroupAccessRightExists = isUserAccessRightExist(reportDate, userGroupAccessIndexName);

        if (isUserGroupAccessRightExists) {
            Query groupListDate = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();

//            Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime",Integer.class));

            SearchRequest searchRequest1 = SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).query(groupListDate)
                    .size(environment.getProperty("aggregationPageSize", Integer.class)));

            SearchResponse<UserGroupList> searchResponse = esClient.search(searchRequest1, UserGroupList.class);

            for (Hit<UserGroupList> userGroupList : searchResponse.hits().hits()) {
                String groupLabel = Objects.requireNonNull(userGroupList.source()).getGroupLabel();
                Query groupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(groupLabel))._toQuery();
                Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
                Query boolQuery = BoolQuery.of(b -> b.must(groupLabelQuery).must(reportDateQuery))._toQuery();
                CountRequest countRequest = CountRequest.of(s -> s.index(UserGroupAccessRights.INDEX_NAME).query(boolQuery));

                long count = esClient.count(countRequest).count();
                if (count > 0) {
                    // update userGroupList index
                    Map<String, Object> groupListMap = new HashMap<>();
                    groupListMap.put("status", "active");
                    SearchRequest searchRequest = SearchRequest.of(s -> s.index(groupListIndex).query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));

                    SearchResponse<UserGroupList> groupListResponse = esClient.search(searchRequest, UserGroupList.class);
                    //   log.info("update Group Label Status searchRequest : {}", searchRequest);
                    if (!groupListResponse.hits().hits().isEmpty()) {
                        groupListResponse.hits().hits().forEach(hit -> {
                                    String documentId = hit.id();
                                    GetResponse<UserGroupList> getResponse = null;
                                    try {
                                        getResponse = esClient.get(g -> g
                                                .index(groupListIndex)
                                                .id(documentId), UserGroupList.class);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    long seqNo = getResponse.seqNo();
                                    long primaryTerm = getResponse.primaryTerm();
                                    UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u ->
                                            u.index(groupListIndex).
                                                    id(documentId).
                                                    ifSeqNo(seqNo).
                                                    ifPrimaryTerm(primaryTerm).
                                                    doc(groupListMap));
                                    try {
                                        esClient.update(updateRequest, UserGroupList.class);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
                    }
                }
            }
        }
        log.info("The status field was successfully updated.");
        log.info("Starting  to update the  Department Field in the UAM_MX_GROUP_LIST Report :- ");
        updateDepartmentField(reportDate);
        log.info("The department field was successfully updated.");
    }

    public Boolean isUserAccessRightExist(String reportDate, String indexName) throws IOException {
        Query query = TermQuery.of(q -> q.field("reportDate").value(reportDate))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName).query(query));
        SearchResponse<UserGroupAccessRights> searchResponse = esClient.search(searchRequest, UserGroupAccessRights.class);
        // log.info(searchResponse.toString());
        return !searchResponse.hits().hits().isEmpty();
    }

    public void updateDepartmentField(String reportDate) throws IOException, InterruptedException {

        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));

        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();


        SearchRequest searchRequest = SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).query(reportDateQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));


        SearchResponse<UserGroupList> searchResponse = esClient.search(searchRequest, UserGroupList.class);
        for (Hit<UserGroupList> hit : searchResponse.hits().hits()) {

            String id = hit.id();
            String groupLabel = Objects.requireNonNull(hit.source()).getGroupLabel();
            String grpRoleStr = Objects.requireNonNull(hit.source()).getGrpRoleStr();
            Map<Object, Object> departmentMap = getDepartment(groupLabel, grpRoleStr, reportDate);
            //  log.info("updateDepartmentField department map :{}", departmentMap) ;
            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.id(id).index(UserGroupList.INDEX_NAME).doc(departmentMap));
            try {
                esClient.update(updateRequest, UserGroupList.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<Object, Object> getDepartment(String groupLabel, String grpRoleStr, String reportDate) throws IOException {

        Query groupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(groupLabel))._toQuery();

        Query roleQuery = TermQuery.of(t -> t.field("grpRoleStr.keyword").value(grpRoleStr))._toQuery();

        Query boolQuery = BoolQuery.of(b -> b.must(groupLabelQuery).must(roleQuery))._toQuery();

        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(GroupLabelDepartment.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class)));
        //log.info("getDepartment search request : {}", searchRequest) ;
        SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);

        Map<Object, Object> dep = new HashMap<>();

        if (!searchResponse.hits().hits().isEmpty()) {
            List<String> departments = new ArrayList<>();
            for (Hit<GroupLabelDepartment> hit : searchResponse.hits().hits()) {
                assert hit.source() != null;
                String department = hit.source().getDepartment();
                departments.add(department);
            }
            dep.put("department", departments);
        } else {
            log.info("update group label in group label department");
            // implement new group label department from group list
            Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
            Query groupWithReportDateQuery = BoolQuery.of(b -> b.must(groupLabelQuery).must(reportDateQuery).must(roleQuery))._toQuery();
            SearchResponse<UserGroupList> groupListSearchResponse;
            try {
                groupListSearchResponse = esClient.search(SearchRequest.of(s -> s.index(UserGroupList.INDEX_NAME).query(groupWithReportDateQuery)
                        .size(environment.getProperty("aggregationPageSize", Integer.class))), UserGroupList.class);
                groupLabelDepartmentService.groupLabelDepartment(groupListSearchResponse);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return dep;
    }

}





