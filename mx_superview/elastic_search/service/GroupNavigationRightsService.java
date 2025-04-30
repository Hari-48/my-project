package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
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
import java.util.stream.Collectors;

@Service
public class GroupNavigationRightsService {
    private static final Logger log = LoggerFactory.getLogger(GroupNavigationRightsService.class);
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;


    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public GroupNavigationRightsService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }

    @SneakyThrows
    public Document getNavigationRightsData(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                            int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) {

        Document navigationList = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = GroupNavigationRights.INDEX_NAME;

        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupNavigationsRights.json", "fields");
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupNavigationsRights.json", "integerFields");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupNavigationsRights.json", "globalSearchFields");

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
//            Map<Object, Long> uniqueFilterValues = new TreeMap<>();
//            Aggregation filterColumns = elastic_utils.getFilterColumn(elasticSearchModel.getFilterColumnName(), integerFields, pageSize);

            String filterColumnVal;
            Map<Object, Long> uniqueFilterValues = new TreeMap<>();

            String filterColumn = elasticSearchModel.getFilterColumnName();
            if (!integerFields.contains(filterColumn)) {
                filterColumnVal = filterColumn + ".displaycolumnval";
            } else {
                filterColumnVal = filterColumn;
            }
            NamedValue<SortOrder> sort = NamedValue.of("_key", SortOrder.Asc);
            Aggregation filterColumns;

            if (filterColumn.equalsIgnoreCase("userGroupDepartment")) {
                filterColumns = Aggregation.of(a ->
                        a.terms(TermsAggregation.of(ta ->
                                ta.script(s -> s.inline(InlineScript.of(i -> i.
                                        source("if (doc['userGroupDepartment.displaycolumnval'].size() == 0) { return ''; } else { return doc['userGroupDepartment.displaycolumnval'].value;}").
                                        lang("painless")))))));
            } else if (filterColumn.equalsIgnoreCase("departmentAmendRights")) {
                filterColumns = Aggregation.of(a ->
                        a.terms(TermsAggregation.of(ta ->
                                ta.script(s -> s.inline(InlineScript.of(i -> i.
                                        source("if (doc['departmentAmendRights.displaycolumnval'].size() == 0) { return ''; } else { return doc['departmentAmendRights.displaycolumnval'].value;}").
                                        lang("painless")))))));
            } else {
                filterColumns = Aggregation.of(a ->
                        a.terms(TermsAggregation.of(ta ->
                                ta.field(filterColumnVal).order(List.of(sort)).size(pageSize))));
            }

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
            SearchResponse<GroupNavigationRights> searchResponse = esClient.search(searchRequest, GroupNavigationRights.class);
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
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, GroupNavigationRights.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);

                //dropDownFieldCount = newDropDown.stream().count();

            }
            navigationList.put("filterValues", newDropDown);
            log.info("DropDown Count------------Cardinality :{}", dropDownFieldCount);
            navigationList.put("dropDownFieldCount", dropDownFieldCount);
            navigationList.put("filterColumnNumericCount", filterColNumericCount);
            navigationList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<GroupNavigationRights> allDocuments = new ArrayList<>();
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
                            field("template.keyword").order(sortingOrder.equalsIgnoreCase("asc")
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
            SearchResponse<GroupNavigationRights> searchResponse = esClient.search(searchRequest, GroupNavigationRights.class);
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
            String template = "GROUP_NAVIGATION-RIGHTS_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));
            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            navigationList.put("totalPage", totalPages);
            navigationList.put("count", totalCount);
            navigationList.put("pageScroll", pageScroll);
            navigationList.put("content", allDocuments);
            navigationList.put("sorting", document);
            navigationList.put("pitId", pitId);
        }
        return navigationList;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getNavigationRightsExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) throws Exception {
        List<GroupNavigationRights> navigationRightsList = new ArrayList<>();
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
            Document navigationRights = getNavigationRightsData(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);

            navigationRightsList.addAll((Collection<? extends GroupNavigationRights>) navigationRights.get("content"));
            docs = (Document) navigationRights.get("sorting");
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, navigationRightsList.size(), job.getId());
            totalPage = (long) navigationRights.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) navigationRights.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", navigationRightsList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, navigationRightsList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }

    public Map<Object, Object> getGroupLabels(String template, String reportDate) {
        Map<Object, Object> groupLabelMap = new HashMap<>();
        List<String> activeGroups = new ArrayList<>();
        List<String> inActiveGroups = new ArrayList<>();
        Query reportDateQuery = TermQuery.of(t ->
                t.field("reportDate").value(reportDate))._toQuery();
        Query templateQuery = TermQuery.of(t ->
                t.field("navigationTmpl.keyword").value(template))._toQuery();
        Query boolQuery = BoolQuery.of(b ->
                b.must(reportDateQuery).must(templateQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s ->
                s.query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)).index(UserGroupList.INDEX_NAME));
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
        groupLabelMap.put("activeGroupLabel", activeGroups);
        groupLabelMap.put("inActiveGroupLabel", inActiveGroups);
        return groupLabelMap;
    }

    public void updateGroupLabels(String reportDate) {
        log.info("Start to Update Group Label Field on {} :-", reportDate);
        Query query = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        HashMap<String, Aggregation> aggregation = new HashMap<>();

        Aggregation templateLabel = Aggregation.of(a ->
                a.terms(TermsAggregation.of(ta ->
                        ta.field("template.keyword").size(environment.getProperty("aggregationPageSize", Integer.class)))));
        aggregation.put("template", templateLabel);

        SearchRequest searchRequest = SearchRequest.of(s ->
                s.index(GroupNavigationRights.INDEX_NAME).aggregations(aggregation).
                        size(environment.getProperty("aggregationPageSize", Integer.class)).
                        query(query));

        log.info("group Navigation Rights request - Distinct template Label :{}", searchRequest);
        SearchResponse<GroupNavigationRights> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, GroupNavigationRights.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, Long> distinctTemplate = new TreeMap<>();
        if (searchResponse.aggregations() != null && searchResponse.aggregations().get("template") != null) {
            if (searchResponse.aggregations().get("template").isSterms()) {
                searchResponse.aggregations().get("template").sterms().buckets().array().forEach(b -> distinctTemplate.put(
                        b.key().stringValue(), b.docCount()));

            } else if (searchResponse.aggregations().get("template").isLterms()) {
                searchResponse.aggregations().get("template").lterms().buckets().array().forEach(b -> distinctTemplate.put(
                        String.valueOf(b.key()), b.docCount()));
            }
        }
        log.info("Distinct Label Count :{}", distinctTemplate.size());
        for (String label : distinctTemplate.keySet()) {
            Map<Object, Object> updateData = getGroupLabels(label, reportDate);
            Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
            Query templateQuery = TermQuery.of(t -> t.field("template.keyword").value(label))._toQuery();
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateQuery))._toQuery();

            Map<String, JsonData> params = new HashMap<>();
            for (Map.Entry<Object, Object> entry : updateData.entrySet()) {
                params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
                params.put(entry.getValue().toString(), JsonData.of(entry.getValue()));
            }
            UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                    .index(GroupNavigationRights.INDEX_NAME)
                    .script(s -> s.inline(InlineScript.of(i ->
                            i.source("ctx._source.activeGroupLabel = params.activeGroupLabel; ctx._source.inActiveGroupLabel = params.inActiveGroupLabel").
                                    lang("painless").
                                    params(params)))).
                    query(boolQuery));
            //log.info("Update Request - UpdateByQueryRequest : {}", updateByQueryRequest);
            try {
                esClient.updateByQuery(updateByQueryRequest);
                log.info("Successfully,  Active and Inactive GroupLabel was Updated for {} Template Label ", label);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<String> getActiveGroupLabels(String reportDate) {
        List<String> grouplabelList = new ArrayList<>();
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query activeGroupLabel = TermQuery.of(t -> t.field("status").value("active"))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(activeGroupLabel))._toQuery();

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
        return grouplabelList;
    }

    public void updateStatusField(String reportDate) throws IOException, InterruptedException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        List<String> groupLabels = getActiveGroupLabels(reportDate);
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        var array = new ArrayList<FieldValue>();
        for (String newData : groupLabels) {
            array.add(FieldValue.of(newData));
        }

        Query groupLabelsQuery = TermsQuery.of(t -> t.field("groupLabel.keyword").terms(TermsQueryField.of(ty -> ty.value(array))))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(groupLabelsQuery))._toQuery();
        Map<String, JsonData> params = new HashMap<>();
        params.put("status", JsonData.of("active"));

        UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                .index(GroupNavigationRights.INDEX_NAME)
                .script(s -> s.inline(InlineScript.of(i ->
                        i.source("ctx._source.status = params.status;")
                                .lang("painless")
                                .params(params))))
                .query(boolQuery).conflicts(Conflicts.Proceed)
        );
        // log.info("Update Request - UpdateByQueryRequest : {}", updateByQueryRequest);
        try {

            UpdateByQueryResponse response = esClient.updateByQuery(updateByQueryRequest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update status field", e);
        }
        log.info("The status field was successfully updated.");
        updateNavigationDepartment(reportDate);
        //  updateUserGroupDepartmentField(reportDate);
    }


    @SneakyThrows
    public void updateUserGroupDepartmentField(String reportDate) {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        log.info("Starting  to update the userGroupDepartment Field");
        List<String> groupLabels = getActiveGroupLabels(reportDate);
        log.info("Size:{}", groupLabels.size());
        for (String grpLabel : groupLabels) {
            Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();

            Map<Object, Object> departmentMap = getDepartment(grpLabel, reportDate);
            Query groupLabelsQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(grpLabel))._toQuery();
            Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(groupLabelsQuery))._toQuery();

            Map<String, JsonData> params = new HashMap<>();
            params.put("userGroupDepartment", JsonData.of(departmentMap.get("userGroupDepartment")));



            UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                    .index(GroupNavigationRights.INDEX_NAME)
                    .script(s -> s.inline(InlineScript.of(i ->
                            i.source("ctx._source.userGroupDepartment = params.userGroupDepartment;")
                                    .lang("painless")
                                    .params(params)))).
                    query(boolQuery).conflicts(Conflicts.Proceed));
            try {
                UpdateByQueryResponse response = esClient.updateByQuery(updateByQueryRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("NavigationDepartment- updated successfully");
    }


    private Map<Object, Object> getDepartment(String userGroupLabel, String reportDate) throws IOException {
        Query userGroupLabelQuery = TermQuery.of(t -> t.field("groupLabel.keyword").value(userGroupLabel))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(userGroupLabelQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(GroupLabelDepartment.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class)));
        // log.info("getDepartment search request : {}", searchRequest) ;
        SearchResponse<GroupLabelDepartment> searchResponse = esClient.search(searchRequest, GroupLabelDepartment.class);
        Map<Object, Object> dep = new HashMap<>();

        if (!searchResponse.hits().hits().isEmpty()) {
            List<String> departments = new ArrayList<>();
            for (Hit<GroupLabelDepartment> hit : searchResponse.hits().hits()) {
                assert hit.source() != null;
                String department = hit.source().getDepartment();
                departments.add(department);
            }
            dep.put("userGroupDepartment", departments);
        }
        return dep;
    }

    @SneakyThrows
    public List<String> getPathField(String repDate) {
        Query rep = TermQuery.of(t -> t.field("reportDate").value(repDate))._toQuery();
        Query status = TermQuery.of(t -> t.field("status").value("active"))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(rep).must(status))._toQuery();
        CountRequest countRequest = CountRequest.of(c -> c.index(GroupNavigationRights.INDEX_NAME).query(boolQuery));
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
            CompositeAggregationSource compositeAggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("path.keyword")));
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
            SearchRequest searchRequest = SearchRequest.of(s -> s.index(GroupNavigationRights.INDEX_NAME).query(boolQuery).aggregations(aggregations));
            SearchResponse<GroupNavigationRights> searchResponse = esClient.search(searchRequest, GroupNavigationRights.class);
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

        return new ArrayList<>(uniqueLabels);
    }

    @SneakyThrows
    public void updateNavigationDepartment(String reportDate) throws IOException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        log.info("Starting  to update the NavigationDepartment Field");
        List<String> paths = getPathField(reportDate);

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (String path : paths) {
            executorService.submit(() -> {
                updateAmendRights(path, reportDate);
            });
        }
        executorService.shutdown();
        log.info("Successfully Updated AmendRights field in GroupNavigation Report");
    }

    @SneakyThrows
    public void updateAmendRights(String path, String reportDate) {
        List<GroupNavigationRights> allResults = new ArrayList<>();

        SearchResponse<NavigationRightsDepartment> responses = esClient.
                search(SearchRequest.of(s ->
                        s.index(NavigationRightsDepartment.INDEX_NAME).
                                query(TermQuery.of(t -> t.field("path.keyword").value(path))._toQuery())), NavigationRightsDepartment.class);

        Query pathQuery = TermQuery.of(t -> t.field("path.keyword").value(path))._toQuery();
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query bool = BoolQuery.of(b -> b.must(reportDateQuery).must(pathQuery))._toQuery();

        if (!responses.hits().hits().isEmpty()) {
            for (Hit<NavigationRightsDepartment> hit : responses.hits().hits()) {
                List<String> amendRights = hit.source().getDepartmentAmendRights();


                Map<String, JsonData> params = new HashMap<>();
                if(!amendRights.isEmpty()) {
                    params.put("departmentAmendRights", JsonData.of(amendRights));
                }
                else {
                    params.put("departmentAmendRights", JsonData.of(Collections.singleton("")));
                }

                Query boolQuery = BoolQuery.of(b -> b.must(pathQuery).must(reportDateQuery))._toQuery();
                UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                        .index(GroupNavigationRights.INDEX_NAME)
                        .script(s -> s.inline(InlineScript.of(i ->
                                i.source("ctx._source.departmentAmendRights = params.departmentAmendRights;")
                                        .lang("painless")
                                        .params(params)))).
                        query(boolQuery).conflicts(Conflicts.Proceed));
                try {
                    UpdateByQueryResponse response = esClient.updateByQuery(updateByQueryRequest);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            log.info("{} This menu path is not available in the Navigation Department, so it will be added new.", path);
            SearchResponse<GroupNavigationRights> groupNavigationRightsSearchResponse;
            try {
                groupNavigationRightsSearchResponse = esClient.search(SearchRequest.of(s -> s.index(GroupNavigationRights.INDEX_NAME).query(bool)
                        .size(environment.getProperty("aggregationPageSize", Integer.class))), GroupNavigationRights.class);

                List<GroupNavigationRights> results = groupNavigationRightsSearchResponse.hits().hits().stream().map(hit -> hit.source()).collect(Collectors.toList());
                allResults.addAll(results);
                insertMenuPath(allResults);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SneakyThrows
    public void updateRightsPriorityLevel(String reportDate) throws IOException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));

        SearchResponse<GroupNavigationRights> searchResponse = null;
        int pageSize = environment.getProperty("aggregationPageSize", Integer.class, 10000);
        Map<String, FieldValue> afterKey = null;
        Query repDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query statusQuery = TermQuery.of(t -> t.field("status").value("active"))._toQuery();

        Query groupDepartmentExist = ExistsQuery.of(e -> e.field("userGroupDepartment.keyword"))._toQuery();
        Script groupDepartmentScript = Script.of(s -> s.inline(InlineScript.of(ii -> ii.source("doc['userGroupDepartment.keyword'].value != ''").lang("painless"))));

        Query amendRightsExist = ExistsQuery.of(e -> e.field("departmentAmendRights.keyword"))._toQuery();
        Script amendRightsScript = Script.of(s -> s.inline(InlineScript.of(ii -> ii.source("doc['departmentAmendRights.keyword'].value != ''").lang("painless"))));


        Query departmentScriptQuery = ScriptQuery.of(s -> s.script(groupDepartmentScript))._toQuery();
        Query rightsScriptQuery = ScriptQuery.of(s -> s.script(amendRightsScript))._toQuery();

        Query boolQuery = BoolQuery.of(b -> b.must(repDateQuery).must(statusQuery).must(amendRightsExist).must(departmentScriptQuery).must(groupDepartmentExist).must(rightsScriptQuery))._toQuery();

        while (true) {

            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(c -> c.terms(t -> t.field("groupLabel.keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("map2", aggregationSource);
            compoAgg.add(map);

            Aggregation aggregation;
            if (afterKey != null) {
                Map<String, FieldValue> finalAfterKey = afterKey;
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg).after(finalAfterKey))._toAggregation();
            } else {
                aggregation = CompositeAggregation.of(c -> c.size(pageSize).sources(compoAgg))._toAggregation();
            }

            SearchRequest searchRequest = SearchRequest.of(s -> s.
                    index(GroupNavigationRights.INDEX_NAME).
                    query(boolQuery).
                    aggregations("Aggregations", aggregation).
                    size(pageSize));
            searchResponse = esClient.search(searchRequest, GroupNavigationRights.class);
            updateRightsPriority(searchResponse);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregations").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null || afterKey.isEmpty()) {
                break;
            }
        }
    }


    private void updateRightsPriority(SearchResponse<GroupNavigationRights> searchResponse) {
        log.info("response Count.....>.{}", searchResponse.hits().hits().size());
        Map<String, String> params = new HashMap<>();
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(() -> {
            for (Hit<GroupNavigationRights> hit : searchResponse.hits().hits()) {
                String id = hit.id();
                //String groupLabel = hit.source().getGroupLabel();
                String department = Objects.requireNonNull(hit.source()).getUserGroupDepartment().get(0);
                List<String> menuPathDep = hit.source().getDepartmentAmendRights();

                String rights = hit.source().getRights();

                if (menuPathDep.contains(department)) {
                    params.put("rightsLevel", "low");
                } else {
                    if ((rights.contains("I") || rights.contains("S") || rights.contains("U"))) {
                        params.put("rightsLevel", "high");
                    } else {
                        params.put("rightsLevel", "medium");
                    }
                }
                UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.
                        id(id).
                        index(GroupNavigationRights.INDEX_NAME).
                        doc(params));
                try {
                    esClient.update(updateRequest, GroupNavigationRights.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        service.shutdown();
    }

    public void insertMenuPath(List<GroupNavigationRights> navigationRightsSearchResponse) throws IOException {
        if (navigationRightsSearchResponse.isEmpty()) {
            return;
        }
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (GroupNavigationRights hit : navigationRightsSearchResponse) {
            Map<String, Object> newMap = new HashMap<>();
            String path = hit.getPath();
            String pathRest = hit.getPathRest();
            String pathLabel = hit.getPathLabel();
            String subMenu = hit.getSubMenu1();
            String date = String.valueOf(LocalDate.now());
            newMap.put("path", path);
            newMap.put("pathRest", pathRest);
            newMap.put("pathLabel", pathLabel);
            newMap.put("subMenu1", subMenu);
            br.operations(op -> op
                    .index(idx -> idx
                            .index(NavigationRightsDepartment.INDEX_NAME)
                            .document(newMap)
                    )
            );
        }
        BulkResponse result = esClient.bulk(br.build());
        if (result.errors()) {
            log.error("Bulk request encountered errors:");
            result.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("Error: {}", item.error().reason());
                }
            });
        } else {
            log.info("Bulk request completed successfully with {} items", result.items().size());
        }
    }

    @SneakyThrows(Exception.class)
    @Scheduled(cron = "${uam.cron.autoload.group-navigation.update.expressions}")
    public void updateUserGroupDepartmentField() {
        LocalDate today = LocalDate.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String reportDate = today.format(formatter);
        log.info("reportDate:{}", reportDate);
        updateUserGroupDepartmentField(reportDate);
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        //update - Amend Rights
        updateRightsPriorityLevel(reportDate);
    }
}
