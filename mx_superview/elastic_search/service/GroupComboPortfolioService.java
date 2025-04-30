package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.GroupComboPortfolio;
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
public class GroupComboPortfolioService {
    private static final Logger log = LoggerFactory.getLogger(GroupComboPortfolioService.class);
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public GroupComboPortfolioService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.searchHistoryRepository = searchHistoryRepository;
        this.environment = environment;
        this.esClient = esClient;
    }

    public Document getGroupComboPortfolioList(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                               int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) throws IOException, ParseException {

        Document groupComboPortfolioList = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = GroupComboPortfolio.INDEX_NAME;

        // Fetching Data From  JSON File .
        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupComboPortfolio.json", "fields");
        //Fetching IntegerFields From  JSON File .
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupComboPortfolio.json", "integerFields");
        //Fetching Global Search Fields From  JSON File .
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/groupComboPortfolio.json", "globalSearchFields");

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
            SearchResponse<GroupComboPortfolio> searchResponse = esClient.search(searchRequest, GroupComboPortfolio.class);
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

            log.info("Total counts in dropDownFilter : {} ", dropDownFilter.size());

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
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, GroupComboPortfolio.class);
            //log.info("DropDown Count------------Cardinality :{}",dropDownFieldCount);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            groupComboPortfolioList.put("filterValues", newDropDown);
            groupComboPortfolioList.put("dropDownFieldCount", dropDownFieldCount);
            groupComboPortfolioList.put("filterColumnNumericCount", filterColNumericCount);
            groupComboPortfolioList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<GroupComboPortfolio> allDocuments = new ArrayList<>();
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
                            field("userGroup.keyword").order(sortingOrder.equalsIgnoreCase("asc")
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
            SearchResponse<GroupComboPortfolio> searchResponse = esClient.search(searchRequest, GroupComboPortfolio.class);
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
            String template = "GROUP_COMBINED_PORTFOLIO_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));

            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            groupComboPortfolioList.put("totalPage", totalPages);
            groupComboPortfolioList.put("count", totalCount);
            groupComboPortfolioList.put("pageScroll", pageScroll);
            groupComboPortfolioList.put("content", allDocuments);
            groupComboPortfolioList.put("sorting", document);
            groupComboPortfolioList.put("pitId", pitId);
        }
        return groupComboPortfolioList;
    }


    @Async
    @Transactional

    public CompletableFuture<Void> getGroupComboPortfolioListExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) throws Exception {
        List<GroupComboPortfolio> groupComboPortfolioList = new ArrayList<>();
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
            Document searchGroupComboPortfolio = getGroupComboPortfolioList(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);
            //   log.info("NEW RESPONSE :{}", searchGroupComboPortfolio);

            groupComboPortfolioList.addAll((Collection<? extends GroupComboPortfolio>) searchGroupComboPortfolio.get("content"));
            docs = (Document) searchGroupComboPortfolio.get("sorting");

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, groupComboPortfolioList.size(), job.getId());
            totalPage = (long) searchGroupComboPortfolio.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) searchGroupComboPortfolio.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", groupComboPortfolioList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, groupComboPortfolioList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }
}
