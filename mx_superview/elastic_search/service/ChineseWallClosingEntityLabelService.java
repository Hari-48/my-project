package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.ChineseWall;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.ChineseWallClosingEntityLabel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.ClosingEntity;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.ClosingEntityLabel;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
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
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChineseWallClosingEntityLabelService {

    private static final Logger log = LoggerFactory.getLogger(ChineseWallClosingEntityLabelService.class);
    @Autowired
    private final DownloadJobService downloadJobService;

    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public ChineseWallClosingEntityLabelService(DownloadJobService downloadJobService, Environment environment, ElasticsearchClient esClient) {
        this.downloadJobService = downloadJobService;
        this.environment = environment;
        this.esClient = esClient;
    }

    @SneakyThrows
    public Document getChineseWallClosingEntityLabelList(ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                                         int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String username) {

        CountRequest countRequest = CountRequest.of(c -> c.index(ChineseWallClosingEntityLabel.INDEX_NAME));
        long count = esClient.count(countRequest).count();
        String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("The report date in getChineseWallClosingEntityLabelList is : {}", reportDate);
        String newReportDate = elastic_utils.getLatestDate(reportDate, ChineseWall.INDEX_NAME, ChineseWall.class);
        log.info("The new report date in getChineseWallClosingEntityLabelList is : {}", newReportDate);
        log.info("count in getChineseWallClosingEntityLabelList :{}", count);
        if (count == 0) {
            loadChineseWallTemplateLabel(newReportDate);
        }

        Document chineseWallClosingEntityLabelList = new Document();

        List<Query> queryList = new ArrayList<>();
        String indexName = ChineseWallClosingEntityLabel.INDEX_NAME;

        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/chineseWallClosingEntityLabel.json", "fields");
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/chineseWallClosingEntityLabel.json", "integerFields");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/chineseWallClosingEntityLabel.json", "globalSearchFields");

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
            SearchResponse<ChineseWallClosingEntityLabel> searchResponse = esClient.search(searchRequest, ChineseWallClosingEntityLabel.class);

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
            dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, ChineseWallClosingEntityLabel.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }
            chineseWallClosingEntityLabelList.put("filterValues", newDropDown);
            chineseWallClosingEntityLabelList.put("dropDownFieldCount", dropDownFieldCount);
            chineseWallClosingEntityLabelList.put("filterColumnNumericCount", filterColNumericCount);
            chineseWallClosingEntityLabelList.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<ChineseWallClosingEntityLabel> allDocuments = new ArrayList<>();
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
                            field("templateLabel.keyword").order(sortingOrder.equalsIgnoreCase("asc")
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
            SearchResponse<ChineseWallClosingEntityLabel> searchResponse = esClient.search(searchRequest, ChineseWallClosingEntityLabel.class);
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
            String template = "CHINESE_WALL_CLOSING_ENTITY_LABEL_TEMPLATE";

            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }

            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();

            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));

            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            chineseWallClosingEntityLabelList.put("totalPage", totalPages);
            chineseWallClosingEntityLabelList.put("count", totalCount);
            chineseWallClosingEntityLabelList.put("pageScroll", pageScroll);
            chineseWallClosingEntityLabelList.put("content", allDocuments);
            chineseWallClosingEntityLabelList.put("sorting", document);
            chineseWallClosingEntityLabelList.put("pitId", pitId);
        }
        return chineseWallClosingEntityLabelList;
    }

    @Async
    @Transactional
    public void getChineseWallClosingEntityLabelListExport(String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, ElasticSearchModel elasticSearchModel) {
        List<ChineseWallClosingEntityLabel> chineseWallClosingEntityLabelList = new ArrayList<>();
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
            Document searchChineseWallClosingEntityLabel = getChineseWallClosingEntityLabelList(
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, userName);

            //  log.info("NEW RESPONSE :{}", searchCounterPartyValues);
            chineseWallClosingEntityLabelList.addAll((Collection<? extends ChineseWallClosingEntityLabel>) searchChineseWallClosingEntityLabel.get("content"));
            docs = (Document) searchChineseWallClosingEntityLabel.get("sorting");

            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, chineseWallClosingEntityLabelList.size(), job.getId());
            totalPage = (long) searchChineseWallClosingEntityLabel.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) searchChineseWallClosingEntityLabel.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", chineseWallClosingEntityLabelList.size(), (int) totalCount, job.getId());
        elastic_utils.exportFileFromDetails(outputFormat, chineseWallClosingEntityLabelList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }


    @SneakyThrows
    public void loadChineseWallTemplateLabel(String date) {

        log.info("Latest Date is : {}", date);

        List<String> activeTemplateLabels = getActiveTemplateLabels(date);
        for (String templateLabel : activeTemplateLabels) {
            ChineseWallClosingEntityLabel activeChineseWallTemplateLabel = new ChineseWallClosingEntityLabel();
            activeChineseWallTemplateLabel.setTemplateLabel(templateLabel);
            activeChineseWallTemplateLabel.setStatus("active");
            activeChineseWallTemplateLabel.setCreatedDate(date);
            saveChineseWallTemplateLabel(activeChineseWallTemplateLabel);
        }
        log.info("Loaded and saved unique Active template labels: {}", activeTemplateLabels);

        List<String> inactiveTemplateLabels = getInactiveTemplateLabels(date);
        for (String templateLabel : inactiveTemplateLabels) {
            ChineseWallClosingEntityLabel inactiveChineseWallTemplateLabel = new ChineseWallClosingEntityLabel();
            inactiveChineseWallTemplateLabel.setTemplateLabel(templateLabel);
            inactiveChineseWallTemplateLabel.setStatus("inactive");
            inactiveChineseWallTemplateLabel.setCreatedDate(date);
            saveChineseWallTemplateLabel(inactiveChineseWallTemplateLabel);
        }
        log.info("Loaded and saved unique Inactive template labels: {}", inactiveTemplateLabels);
    }

    @SneakyThrows
    private List<String> getActiveTemplateLabels(String date) {

        List<Query> activeTemplateLabelQueryList = new ArrayList<>();

        Query reportDate = TermQuery.of(q -> q.field("reportDate").value(date))._toQuery();
        activeTemplateLabelQueryList.add(reportDate);

        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Aggregation agg = Aggregation.of(a -> a
                .terms(t -> t
                        .field("templateLabel.displaycolumnval")
                        .size(5000)
                ));
        aggregation.put("UniqueTemplate", agg);

        log.info("Initial Update - Loading templateLabel:");

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

        activeTemplateLabelQueryList.add(shouldQuery);

        Query activeTemplateLabelSearchQuery = BoolQuery.of(q -> q.must(activeTemplateLabelQueryList))._toQuery();

        SearchRequest activeTemplateLabelSearchRequest = SearchRequest.of(s -> s
                .query(activeTemplateLabelSearchQuery)
                .index(ChineseWall.INDEX_NAME)
                .aggregations(aggregation));

        log.info("Search request for Active Template Label: {}", activeTemplateLabelSearchRequest);

        SearchResponse<ChineseWallClosingEntityLabel> activeTemplateLabelSearchResponse = esClient.search(activeTemplateLabelSearchRequest, ChineseWallClosingEntityLabel.class);

        return extractTemplateLabels(activeTemplateLabelSearchResponse);

    }

    @SneakyThrows
    private List<String> getInactiveTemplateLabels(String date) {
        List<Query> inactiveTemplateLabelQueryList = new ArrayList<>();

        Query reportDate = TermQuery.of(q -> q.field("reportDate").value(date))._toQuery();
        inactiveTemplateLabelQueryList.add(reportDate);

        HashMap<String, Aggregation> aggregation = new HashMap<>();
        Aggregation agg = Aggregation.of(a -> a
                .terms(t -> t
                        .field("templateLabel.displaycolumnval")
                        .size(5000)
                ));
        aggregation.put("UniqueTemplate", agg);

        Query inActiveTemp = BoolQuery.of(b -> b
                .should(TermQuery.of(t -> t.field("inActiveGroupLabel.keyword").value(""))._toQuery())
                .should(BoolQuery.of(r -> r
                                .mustNot(ExistsQuery.of(e -> e.field("inActiveGroupLabel"))._toQuery()))
                        ._toQuery())
                .minimumShouldMatch("1")
        )._toQuery();

        inactiveTemplateLabelQueryList.add(inActiveTemp);

        Query activeTemp = BoolQuery.of(b -> b
                .should(TermQuery.of(t -> t.field("activeGroupLabel.keyword").value(""))._toQuery())
                .should(BoolQuery.of(r -> r
                                .mustNot(ExistsQuery.of(e -> e.field("activeGroupLabel"))._toQuery()))
                        ._toQuery())
                .minimumShouldMatch("1")
        )._toQuery();

        inactiveTemplateLabelQueryList.add(activeTemp);

        SearchRequest inActiveTemplateLabelSearchRequest = SearchRequest.of(s -> s
                .index(ChineseWall.INDEX_NAME)
                .aggregations(aggregation)
                .query(BoolQuery.of(b -> b.must(inactiveTemplateLabelQueryList))._toQuery()));

        SearchResponse<ChineseWallClosingEntityLabel> inActiveTemplateLabelSearchResponse = esClient.search(inActiveTemplateLabelSearchRequest, ChineseWallClosingEntityLabel.class);

        return extractTemplateLabels(inActiveTemplateLabelSearchResponse);
    }

    private List<String> extractTemplateLabels(SearchResponse<ChineseWallClosingEntityLabel> searchResponse) {
        List<String> templateLabels = new ArrayList<>();

        // Check if the response has aggregations and contains the "UniqueTemplate" aggregation
        if (searchResponse.aggregations() != null && searchResponse.aggregations().containsKey("UniqueTemplate")) {
            Aggregate filterColumnAgg = searchResponse.aggregations().get("UniqueTemplate");

            // Check if the aggregation is a valid terms aggregation and contains buckets
            if (filterColumnAgg.sterms() != null) {
                filterColumnAgg.sterms().buckets().array().forEach(bucket -> {
                    String label = bucket.key().stringValue();
                    templateLabels.add(label);
                });
            }
        }

        return templateLabels;
    }


    private void saveChineseWallTemplateLabel(ChineseWallClosingEntityLabel chineseWallTemplateLabel) throws IOException {
        IndexRequest<ChineseWallClosingEntityLabel> indexRequest = IndexRequest.of(i -> i
                .index(ChineseWallClosingEntityLabel.INDEX_NAME)
                .document(chineseWallTemplateLabel));

        esClient.index(indexRequest);
        log.info("Saved ChineseWallClosingEntityLabel: {}", chineseWallTemplateLabel);
    }


    public ResponseEntity<?> updateEntityLabelField(String templateLabel, List<String> entityLabels, Boolean userConfirmation, String name) {
        if (userConfirmation) {
            updateEntityLabel(templateLabel, entityLabels, name);
            return new ResponseEntity<>("Successfully Updated", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Update Unsuccessful", HttpStatus.OK);
        }
    }

    @SneakyThrows
    public void updateEntityLabel(String templateLabel, List<String> entityLabels, String name) {
        // Query to find the document by templateLabel
        Query templateLabelQuery = TermQuery.of(t -> t.field("templateLabel.keyword").value(templateLabel))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(templateLabelQuery))._toQuery();

        // Search request to find documents that match the templateLabel
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).index(ChineseWallClosingEntityLabel.INDEX_NAME).size(10000));
        SearchResponse<ChineseWallClosingEntityLabel> searchResponse = esClient.search(searchRequest, ChineseWallClosingEntityLabel.class);

        // If documents are found, update the entityLabel field for each document
        if (!searchResponse.hits().hits().isEmpty()) {
            for (Hit<ChineseWallClosingEntityLabel> hit : searchResponse.hits().hits()) {
                String docId = hit.id();
                String updatedDate = String.valueOf(LocalDateTime.now());

                // Prepare the update map with new entityLabels, username, and updatedTime
                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("entityLabel", entityLabels);  // Update the entityLabel field with a List<String>
                updateFields.put("userName", name);              // Update the username
                updateFields.put("updatedTime", updatedDate);    // Update the timestamp

                // Prepare the update request
                UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u
                        .id(docId)
                        .index(ChineseWallClosingEntityLabel.INDEX_NAME)
                        .retryOnConflict(3)  // Retry in case of conflicts
                        .doc(updateFields)
                );

                // Execute the update request
                esClient.update(updateRequest, ChineseWallClosingEntityLabel.class);
            }
        }
    }

    @SneakyThrows
    public void chineseWallTemplateCompare(String reportDate) {
        log.info("Fetching current active and inactive template labels:");
        List<String> currentActiveTemplateLabels = getActiveTemplateLabels(reportDate);
        log.info("Current active template labels: {}", currentActiveTemplateLabels);
        List<String> currentInactiveTemplateLabels = getInactiveTemplateLabels(reportDate);
        log.info("Current inactive template labels: {}", currentInactiveTemplateLabels);
        Set<String> existingActiveTemplateLabelsSet = getExistingTemplateLabelsByStatus("active");
        log.info("Existing active template labels: {}", existingActiveTemplateLabelsSet);
        Set<String> existingInactiveTemplateLabelsSet = getExistingTemplateLabelsByStatus("inactive");
        log.info("Existing inactive template labels: {}", existingInactiveTemplateLabelsSet);
        List<String> newActiveTemplateLabels = new ArrayList<>();
        List<String> newInactiveTemplateLabels = new ArrayList<>();

        for (String currentActiveTemplateLabel : currentActiveTemplateLabels) {
            if (!existingActiveTemplateLabelsSet.contains(currentActiveTemplateLabel)) {
                newActiveTemplateLabels.add(currentActiveTemplateLabel);
            }
        }
        for (String currentInactiveTemplateLabel : currentInactiveTemplateLabels) {
            if (!existingInactiveTemplateLabelsSet.contains(currentInactiveTemplateLabel)) {
                newInactiveTemplateLabels.add(currentInactiveTemplateLabel);
            }
        }

        log.info("New active template labels to save: {}", newActiveTemplateLabels);
        log.info("New inactive template labels to save: {}", newInactiveTemplateLabels);

        for (String activeTemplateLabel : newActiveTemplateLabels) {
            ChineseWallClosingEntityLabel templateLabel = new ChineseWallClosingEntityLabel();
            templateLabel.setTemplateLabel(activeTemplateLabel);
            templateLabel.setStatus("active");
            templateLabel.setCreatedDate(reportDate);
            saveChineseWallTemplateLabel(templateLabel);
        }

        for (String inactiveTemplateLabel : newInactiveTemplateLabels) {
            ChineseWallClosingEntityLabel templateLabel = new ChineseWallClosingEntityLabel();
            templateLabel.setTemplateLabel(inactiveTemplateLabel);
            templateLabel.setStatus("inactive");
            templateLabel.setCreatedDate(reportDate);
            saveChineseWallTemplateLabel(templateLabel);
        }

        log.info("Successfully saved new active and inactive template labels.");
    }

    @SneakyThrows
    private Set<String> getExistingTemplateLabelsByStatus(String status) {
        SearchRequest searchRequest = SearchRequest.of(q -> q
                .index(ChineseWallClosingEntityLabel.INDEX_NAME)
                .size(Integer.valueOf(Objects.requireNonNull(environment.getProperty("aggregationPageSize")))) // Adjust size if needed
                .query(TermQuery.of(t -> t.field("status.keyword").value(status))._toQuery())
        );

        SearchResponse<ChineseWallClosingEntityLabel> searchResponse = esClient.search(searchRequest, ChineseWallClosingEntityLabel.class);
        Set<String> existingTemplateLabelsSet = new HashSet<>();

        searchResponse.hits().hits().forEach(hit -> existingTemplateLabelsSet.add(hit.source() != null ? hit.source().getTemplateLabel() : null));

        return existingTemplateLabelsSet;
    }


    @SneakyThrows
    public Document getClosingEntityLabelList() {

        CountRequest countRequest = CountRequest.of(c -> c.index(ClosingEntityLabel.INDEX_NAME));
        long count = esClient.count(countRequest).count();

        String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("The report date in getClosingEntityLabelList is : {}", reportDate);

        String newReportDate = elastic_utils.getLatestDate(reportDate, ClosingEntity.INDEX_NAME, ClosingEntity.class);
        log.info("The new report date in getClosingEntityLabelList is : {}", newReportDate);

        log.info("count in getClosingEntityLabelList :{}", count);
        if (count == 0) {
            loadClosingEntityLabel(newReportDate);
        }

        Document closingEntityLabelList = new Document();

        SearchRequest searchRequest = SearchRequest.of(q -> q.index(ClosingEntityLabel.INDEX_NAME).size(1000));
        log.info("Search Request is : {} ", searchRequest);

        List<ClosingEntityLabel> allDocuments = new ArrayList<>();

        SearchResponse<ClosingEntityLabel> searchResponse = esClient.search(searchRequest, ClosingEntityLabel.class);

        searchResponse.hits().hits().forEach(f -> allDocuments.add(f.source()));
        closingEntityLabelList.put("content", allDocuments);

        return closingEntityLabelList;
    }

    @SneakyThrows
    public void loadClosingEntityLabel(String date) {

        log.info("Latest Date in loadClosingEntityLabel is : {}", date);

        Query reportDate = TermQuery.of(q -> q.field("reportDate").value(date))._toQuery();

        log.info("Initial Update - Loading unique entityLabel:");

        SearchRequest aggSearchRequest = SearchRequest.of(s -> s
                .index(ClosingEntity.INDEX_NAME)
                .size(0)
                .aggregations("unique_entityLabel", a -> a
                        .terms(t -> t
                                .field("entityLabel.displaycolumnval")
                                .size(Integer.valueOf(Objects.requireNonNull(environment.getProperty("aggregationPageSize"))))
                        )
                )
                .aggregations("unique_entityLabel_count", a -> a
                        .cardinality(c -> c
                                .field("entityLabel.keyword")
                        )
                )
                .query(reportDate)
        );

        // Execute the aggregation search for ChineseWall
        SearchResponse<Void> aggSearchResponse = esClient.search(aggSearchRequest, Void.class);

        // Extract the unique template labels from the aggregation
        List<String> uniqueEntityLabels = new ArrayList<>();
        aggSearchResponse.aggregations().get("unique_entityLabel").sterms().buckets().array().forEach(bucket -> uniqueEntityLabels.add(bucket.key().stringValue()));

        for (String entityLabel : uniqueEntityLabels) {
            ClosingEntityLabel closingEntityLabel = new ClosingEntityLabel();
            closingEntityLabel.setEntityLabel(entityLabel);
            closingEntityLabel.setCreatedDate(date);
            saveClosingEntityLabel(closingEntityLabel);
        }

    }

    private void saveClosingEntityLabel(ClosingEntityLabel closingEntityLabel) throws IOException {
        IndexRequest<ClosingEntityLabel> indexRequest = IndexRequest.of(i -> i
                .index(ClosingEntityLabel.INDEX_NAME)
                .document(closingEntityLabel));

        esClient.index(indexRequest);
        log.info("Saved ClosingEntityLabel: {}", closingEntityLabel);
    }

    @SneakyThrows
    public void closingEntityLabelCompare(String reportDate) {
        // Fetch the current entity labels for the given report date
        log.info("Fetching current entity labels:");

        Query reportDateQuery = TermQuery.of(q -> q.field("reportDate").value(reportDate))._toQuery();
        SearchRequest aggSearchRequest = SearchRequest.of(s -> s
                .index(ClosingEntity.INDEX_NAME)
                .size(0)
                .aggregations("unique_entityLabel", a -> a
                        .terms(t -> t
                                .field("entityLabel.displaycolumnval")
                                .size(Integer.valueOf(Objects.requireNonNull(environment.getProperty("aggregationPageSize"))))
                        )
                )
                .query(reportDateQuery)
        );

        // Execute the search request
        SearchResponse<Void> aggSearchResponse = esClient.search(aggSearchRequest, Void.class);

        // Extract current entity labels from the aggregation
        List<String> currentEntityLabelsSet = new ArrayList<>();
        aggSearchResponse.aggregations().get("unique_entityLabel").sterms().buckets().array().forEach(bucket -> currentEntityLabelsSet.add(bucket.key().stringValue()));

        log.info("currentEntityLabels :{}", currentEntityLabelsSet);

        // Fetch the existing entity labels from the ClosingEntityLabel index
        Set<String> existingEntityLabelsSet = getExistingEntityLabels();
        log.info("existingEntityLabelsSet : {}", existingEntityLabelsSet);

        // Create a list to store new entity labels that are not present in the existing list
        List<String> newEntityLabels = new ArrayList<>();

        // Compare each current entity label with existing ones
        for (String currentEntityLabel : currentEntityLabelsSet) {
            if (!existingEntityLabelsSet.contains(currentEntityLabel)) {
                // If the current entity label is not present in the existing set, add it to the new list
                newEntityLabels.add(currentEntityLabel);
            }
        }

        log.info("New entity labels to save: {}", newEntityLabels);

        // Save the new entity labels
        for (String entityLabel : newEntityLabels) {
            ClosingEntityLabel closingEntityLabel = new ClosingEntityLabel();
            closingEntityLabel.setEntityLabel(entityLabel);
            closingEntityLabel.setCreatedDate(reportDate);
            saveClosingEntityLabel(closingEntityLabel);
        }

        log.info("Successfully saved new entity labels.");
    }

    @SneakyThrows
    private Set<String> getExistingEntityLabels() {
        // Fetch existing entity labels from the ClosingEntityLabel index
        SearchRequest searchRequest = SearchRequest.of(q -> q
                .index(ClosingEntityLabel.INDEX_NAME)
                .size(Integer.valueOf(Objects.requireNonNull(environment.getProperty("aggregationPageSize")))) // Adjust size if needed
        );

        SearchResponse<ClosingEntityLabel> searchResponse = esClient.search(searchRequest, ClosingEntityLabel.class);
        Set<String> existingEntityLabelsSet = new HashSet<>();

        searchResponse.hits().hits().forEach(hit -> existingEntityLabelsSet.add(hit.source() != null ? hit.source().getEntityLabel() : null));

        return existingEntityLabelsSet;
    }


}
