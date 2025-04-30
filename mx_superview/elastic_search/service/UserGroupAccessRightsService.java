package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.entity.domain.Group;
import com.finsurge.tmr_portal.general.entity.domain.User;
import com.finsurge.tmr_portal.general.repository.UserRepository;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import lombok.SneakyThrows;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
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
public class UserGroupAccessRightsService {

    private static final Logger log = LoggerFactory.getLogger(UserGroupAccessRightsService.class);
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    private final UserRepository userRepository;

    public UserGroupAccessRightsService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient, UserRepository userRepository) {
        this.environment = environment;
        this.esClient = esClient;
        this.userRepository = userRepository;
    }

    @SneakyThrows
    public Document getUserRights(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                  int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) {

        Document userList = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = UserGroupAccessRights.INDEX_NAME;

        String groupName = getGroupAccessName(username);
        switch(groupName){
            case "THAIUSERUAM":
                Query thaiUser = TermQuery.of(t -> t.field("mngmntPolicy.keyword").value("CIMB TH IT POLICY"))._toQuery();
                queryList.add(thaiUser);
                break;
        }

        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/userGroupAccessRights.json", "fields");
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/userGroupAccessRights.json", "integerFields");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/userGroupAccessRights.json", "globalSearchFields");

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
            SearchResponse<UserGroupAccessRights> searchResponse = esClient.search(searchRequest, UserGroupAccessRights.class);
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
            Integer dropDownFieldCount = 0;
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, UserGroupAccessRights.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            userList.put("filterValues", newDropDown);
            //portfolioList.put("dropDownFieldCount", dropDownFieldCount);
            userList.put("dropDownFieldCount", dropDownFieldCount);
            userList.put("filterColumnNumericCount", filterColNumericCount);
            userList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<UserGroupAccessRights> allDocuments = new ArrayList<>();
            Document document = new Document();
            List<FieldValue> searchAfterValues = new ArrayList<>();
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
            SearchResponse<UserGroupAccessRights> searchResponse = esClient.search(searchRequest, UserGroupAccessRights.class);
            log.info("Total count in Response : {}", searchResponse.hits().hits().size());
            searchResponse.hits().hits().forEach(f -> allDocuments.add(f.source()));

            // SearchAfter - Sorting
            List<Object> sorting;
            if (!searchResponse.hits().hits().isEmpty()) {
                // Processing last hit
                sorting = elastic_utils.getFirstSortingOrder(searchResponse, integerFields, sortBy);
                document.put("sortingFirstIndex", sorting);
                // Processing last hit
                sorting = elastic_utils.getLastSortingOrder(searchResponse, integerFields, sortBy);
                document.put("sortingLastIndex", sorting);
            }
            // saving the Search term - Query  :-
            String template = "USER_LIST_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));
            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            userList.put("totalPage", totalPages);
            userList.put("count", totalCount);
            userList.put("pageScroll", pageScroll);
            userList.put("content", allDocuments);
            userList.put("sorting", document);
            userList.put("pitId", pitId);
        }
        return userList;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getUserRightsExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) {
        List<UserGroupAccessRights> userList = new ArrayList<>();
        Document docs = new Document();
        fieldColumns = new ArrayList<>();
        long totalCount = 0;
        long totalPage = 1;
        log.info(" Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info(" Export: Starting fetch.");
        List<Object> searchAfterValue;
        for (int i = 0; i < totalPage; i++) {
            if (i > 0) {
                searchAfterValue = (List<Object>) docs.get("sortingLastIndex");
                elasticSearchModel.setSearchAfterValue(searchAfterValue);
            }
            Document userListData = getUserRights(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);
            userList.addAll((Collection<? extends UserGroupAccessRights>) userListData.get("content"));
            docs = (Document) userListData.get("sorting");
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, userList.size(), job.getId());
            totalPage = (long) userListData.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) userListData.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", userList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, userList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }


    @SneakyThrows
    public SearchResponse<UserList> getUserList(String reportDate) {
        Query repDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        return esClient.search(SearchRequest.of(s -> s.
                size(environment.getProperty("aggregationPageSize", Integer.class)).
                query(repDateQuery).
                index(UserList.INDEX_NAME)), UserList.class);
    }

    @SneakyThrows
    public void combineUserListWithUserRights(String reportDate) {
        SearchResponse<UserList> userListSearchResponse = getUserList(reportDate);
        for (Hit<UserList> userList : userListSearchResponse.hits().hits()) {
            assert userList.source() != null;
            String userName = userList.source().getUserName();
            //  fetching Username from user access rights
            Query reportDateQuery = TermQuery.of(q -> q.field("reportDate").value(reportDate))._toQuery();
            Query userNameQuery = TermQuery.of(q -> q.field("userName.keyword").value(userName))._toQuery();
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(userNameQuery))._toQuery();
            SearchRequest groupRightsRequest = SearchRequest.of(s -> s.index(UserGroupAccessRights.INDEX_NAME).query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));
            SearchResponse<UserGroupAccessRights> groupAccessRightsSearchResponse = esClient.search(groupRightsRequest, UserGroupAccessRights.class);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("descr", userList.source().getDescr());
            userMap.put("suspended", userList.source().getSuspended());
            userMap.put("suspEd", userList.source().getSuspEd());
            userMap.put("suspSd", userList.source().getSuspSd());
            userMap.put("locked", userList.source().getLocked());
            userMap.put("code", userList.source().getCode());
            userMap.put("mngmntPolicy", userList.source().getMngmntPolicy());
            userMap.put("userLabel", userList.source().getUserLabel());

            if (!groupAccessRightsSearchResponse.hits().hits().isEmpty()) {
                groupAccessRightsSearchResponse.hits().hits().forEach(hit -> {
                            Map<String, Object> newUserMap = new HashMap<>(userMap);
                            String documentId = hit.id();
                            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u ->
                                    u.index(UserGroupAccessRights.INDEX_NAME).
                                            id(documentId).
                                            doc(newUserMap).
                                            retryOnConflict(3));
                            try {
                                esClient.update(updateRequest, UserGroupAccessRights.class);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            } else {
                userMap.put("userName", userName);
                userMap.put("reportDate", reportDate);
                userMap.put("systemDate", String.valueOf(java.time.Clock.systemUTC().instant()));
                userMap.put("userDesc", userList.source().getDescr());
                userMap.put("groupLabel", "");
                userMap.put("groupDesc", "");
                userMap.put("groupRoleStr", "");
                userMap.put("groupTypeStr", "");
                // for user license fields
                userMap.put("license", "");
                userMap.put("licenseCatCount", 0);
                userMap.put("status", "inactive");

                BulkRequest.Builder br = new BulkRequest.Builder();
                br.operations(op -> op
                        .index(idx -> idx
                                .index(UserGroupAccessRights.INDEX_NAME)
                                .document(userMap)
                        )
                );
                esClient.bulk(br.build());
            }
        }
    }

    @SneakyThrows
    public SearchResponse<UserLicense> getUserLicense(String reportDate) {
        Query repDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        return esClient.search(SearchRequest.of(s -> s.
                size(environment.getProperty("aggregationPageSize", Integer.class)).
                query(repDateQuery).
                index(UserLicense.INDEX_NAME)), UserLicense.class);
    }

    @SneakyThrows
    public void combineLicenseWithUserRights(String repDate) {
        SearchResponse<UserLicense> userListSearchResponse = getUserLicense(repDate);
        for (Hit<UserLicense> userLicense : userListSearchResponse.hits().hits()) {
            assert userLicense.source() != null;
            String userName = userLicense.source().getUserName();
            Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(repDate))._toQuery();
            Query userNameQuery = TermQuery.of(t -> t.field("userName.keyword").value(userName))._toQuery();
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(userNameQuery))._toQuery();
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(UserGroupAccessRights.INDEX_NAME).query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));
            SearchResponse<UserGroupAccessRights> groupAccessRightsSearchResponse = esClient.search(searchRequest, UserGroupAccessRights.class);
            Map<String, Object> userLicenserMap = new HashMap<>();
            if (!groupAccessRightsSearchResponse.hits().hits().isEmpty()) {
                groupAccessRightsSearchResponse.hits().hits().forEach(hit -> {
                            userLicenserMap.put("license", userLicense.source().getLicenseCat());
                            userLicenserMap.put("licenseCatCount", userLicense.source().getLicenseCatCount());
                            String documentId = hit.id();
                            UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u ->
                                    u.index(UserGroupAccessRights.INDEX_NAME).
                                            id(documentId).
                                            doc(userLicenserMap));
                            try {
                                esClient.update(updateRequest, UserGroupAccessRights.class);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            } else {
                userLicenserMap.put("license", userLicense.source().getLicenseCat());
                userLicenserMap.put("licenseCatCount", userLicense.source().getLicenseCatCount());
                log.info("license cat count:{}", userLicense.source().getLicenseCatCount());
                userLicenserMap.put("userName", userName);
                userLicenserMap.put("reportDate", repDate);
                userLicenserMap.put("systemDate", String.valueOf(java.time.Clock.systemUTC().instant()));
                userLicenserMap.put("userDesc", "");
                userLicenserMap.put("groupLabel", "");
                userLicenserMap.put("groupDesc", "");
                userLicenserMap.put("groupRoleStr", "");
                userLicenserMap.put("groupTypeStr", "");
                userLicenserMap.put("status", "inactive");
                // for user list fields
                userLicenserMap.put("descr", "");
                userLicenserMap.put("suspended", "");
                userLicenserMap.put("suspEd", "");
                userLicenserMap.put("suspSd", "");
                userLicenserMap.put("locked", "");
                userLicenserMap.put("code", "");
                userLicenserMap.put("mngmntPolicy", "");
                userLicenserMap.put("userLabel", "");
                BulkRequest.Builder br = new BulkRequest.Builder();
                br.operations(op -> op
                        .index(idx -> idx
                                .index(UserGroupAccessRights.INDEX_NAME)
                                .document(userLicenserMap)
                        )
                );
                esClient.bulk(br.build());
            }
        }
    }

    @SneakyThrows
    public void updateUserGroupDepartmentField(String reportDate) {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s ->
                s.index(UserGroupAccessRights.INDEX_NAME).query(reportDateQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));
        log.info("user group label searchRequest :  {}", searchRequest);
        SearchResponse<UserGroupAccessRights> searchResponse = esClient.search(searchRequest, UserGroupAccessRights.class);
        for (Hit<UserGroupAccessRights> hit : searchResponse.hits().hits()) {
            assert hit.source() != null;
            String grpLabel = hit.source().getGroupLabel();
            if (grpLabel.isEmpty() || !grpLabel.isBlank()) {
                Map<Object, Object> departmentMap = getDepartment(grpLabel);
                UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.id(hit.id()).index(UserGroupAccessRights.INDEX_NAME).doc(departmentMap));
                try {
                    esClient.update(updateRequest, UserGroupAccessRights.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Map<Object, Object> getDepartment(String userGroupLabel) throws IOException {
        Query userGroupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(userGroupLabel))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(userGroupLabelQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(GroupLabelDepartment.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class)));
        SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);
        return getObjectMap(searchResponse);
    }

    @NotNull
    private static Map<Object, Object> getObjectMap(SearchResponse<GroupLabelDepartment> searchResponse) {
        Map<Object, Object> dep = new HashMap<>();
        if (!searchResponse.hits().hits().isEmpty()) {
            List<String> departments = new ArrayList<>();
            for (Hit<GroupLabelDepartment> hit : searchResponse.hits().hits()) {
                assert hit.source() != null;
                String department = hit.source().getDepartment();
                if (!department.isEmpty()) {
                    departments.add(department);
                }
            }
            dep.put("userGroupDepartment", departments);
        }
        return dep;
    }

    private String getGroupAccessName(String username) {
        List<User> groupAccessName = userRepository.getGroupAccessName(username);
        List<Group> groups = new ArrayList<>(groupAccessName.get(0).getGroups());
        //List<AccessTemplate> accessTemplates = new ArrayList<>(groups.get(0).getAccessTemplates());
        String groupName = groups.get(0).getName();
        log.info("USER_NAME:{}, GROUP_NAME:{}", username,groupName);
        return groupName;
    }

}


