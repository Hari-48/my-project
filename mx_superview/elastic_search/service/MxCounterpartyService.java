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
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
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

@Service
public class MxCounterpartyService {

    private static final Logger log = LoggerFactory.getLogger(MxCounterpartyService.class);
    @Autowired
    private DownloadJobService downloadJobService;
    @Autowired
    ElasticUtils elastic_utils;

    public final Environment environment;
    private final ElasticsearchClient esClient;

    public MxCounterpartyService(Environment environment, ElasticsearchClient esClient) {
        this.environment = environment;
        this.esClient = esClient;
    }

    @SneakyThrows
    public Document getCounterpartyData(String date, ElasticSearchModel elasticSearchModel, String sortBy, String sortingOrder,
                                        int pageSize, boolean isGlobalSearch, String bulkFilterSearchType, String tab,Boolean counterpartyCreation,String username) {
        Document searchCounterparty = new Document();
        List<Query> queryList = new ArrayList<>();
        String indexName = CounterPartyDocument.INDEX_NAME;

        // Check data is Available for  given Data , if not Available  , it will take latest Data:-
        String newReportDate = elastic_utils.reportDateCheck(date, indexName, CounterPartyDocument.class);

        // Fetching Data From  JSON File .
        List<String> fields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/mxCounterpartyFields.json", "fields");
        List<String> dateFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/mxCounterpartyFields.json", "dateFields");
        List<String> integerFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/mxCounterpartyFields.json", "integerField");
        List<String> globalSearchFields = elastic_utils.getDataFromJsonFile("elastic/GlobalSearchFields/mxCounterpartyFields.json", "globalSearchFields");
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
        log.info("Listing Data for : {}", newReportDate);
        Query reportDate = TermQuery.of(q -> q.field("reportDate").value(newReportDate))._toQuery();
        queryList.add(reportDate);

        if(counterpartyCreation){
            Query creationDateNotNull = ExistsQuery.of(e->e.field("creationDate"))._toQuery();
            queryList.add(creationDateNotNull);
        }

        // Shared Counterparts
        if (!elasticSearchModel.getDspLabel().isEmpty()) {
            var newArray = new ArrayList<FieldValue>();
            for (String counterpartLabel : elasticSearchModel.dspLabel) {
                newArray.add(FieldValue.of(counterpartLabel));
            }
            Query dspLabelQuery = TermsQuery.of(ts -> ts.field("dspLabel.keyword").terms(t -> t.value(newArray)))._toQuery();
            queryList.add(dspLabelQuery);
        }

        switch (tab) {
            case "counterpartStatic":
                // Unique Data + Top Hits in Duplicate Data Will display
                Query duplicateData = TermQuery.of(t -> t.field("counterpartStatus.keyword").value("duplicate"))._toQuery();
                Query counterpartStaticBool = BoolQuery.of(b -> b.mustNot(duplicateData))._toQuery();
                queryList.add(counterpartStaticBool);
                break;
            case "dormantCounterparty":
                Query isDormant = TermQuery.of(q -> q.field("dormantCounterparty").value(true))._toQuery();
                log.info("Dormant Query :- {}", isDormant);
                queryList.add(isDormant);
                break;
            case "dupCounterparts":
                // Duplicate Data + Top Hits in Duplicate Data Will display
                List<String> dup = List.of("duplicate", "duplicateStatic");
                var array = new ArrayList<FieldValue>();
                for (String newData : dup) {
                    array.add(FieldValue.of(newData));
                }
                Query dupCounterparts = TermsQuery.of(t ->
                        t.field("counterpartStatus.keyword").terms(TermsQueryField.of(ty -> ty.value(array))))._toQuery();
                Query duplicateTabBool = BoolQuery.of(b -> b.must(dupCounterparts))._toQuery();
                queryList.add(duplicateTabBool);
                break;
        }

        // searchBox  - Global Search  & Translate SQL Query - Elastic  Query
        if (isGlobalSearch && elasticSearchModel.getSearchTerm() != null && !elasticSearchModel.getSearchTerm().isEmpty()) {
            queryList.add(elastic_utils.getGlobalSearchTerm(elasticSearchModel.getSearchTerm(), fields, dateFields, globalSearchFields));
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
            Map<Object, Long> uniqueFilterValues;
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
            SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
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
            if (!dateFields.contains(elasticSearchModel.getFilterColumnName())) {
                filterColNumericCount = elastic_utils.getNumericCount(elasticSearchModel.getFilterColumnName(), queryList, indexName);
                filterColAlphaNumericCount = elastic_utils.getAlphaNumericCount(elasticSearchModel.getFilterColumnName(), queryList, indexName);
            }
            Integer dropDownFieldCount = 0;
          //  dropDownFieldCount = elastic_utils.getDropDownFieldCount(elasticSearchModel.getFilterColumnName(), queryList, indexName, CounterPartyDocument.class);

          dropDownFieldCount = elastic_utils.getDropDownFieldCount(date,elasticSearchModel.getFilterColumnName(),elasticSearchModel.getFilterSearch(), queryList, indexName, CounterPartyDocument.class);

            List<LinkedHashMap<Object, Object>> newDropDown = new ArrayList<>();
            for (Map.Entry<String, Long> entry : dropDownFilter.entrySet()) {
                LinkedHashMap<Object, Object> newFormat = new LinkedHashMap<>();
                newFormat.put("option", entry.getKey());
                newFormat.put("count", entry.getValue());
                newDropDown.add(newFormat);
            }

            searchCounterparty.put("filterValues", newDropDown);
            //searchClosingEntity.put("dropDownFieldCount", dropDownFieldCount);
            searchCounterparty.put("dropDownFieldCount", dropDownFieldCount);
            searchCounterparty.put("filterColumnNumericCount", filterColNumericCount);
            searchCounterparty.put("filterColumnAlphaNumericCount", filterColAlphaNumericCount);
        } else {
            List<CounterPartyDocument> allDocuments = new ArrayList<>();
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
                            field("dspLabel.keyword").order(sortingOrder.equalsIgnoreCase("asc")
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
            SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
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
            String template = "COUNTERPARTY_TEMPLATE";
            if (elasticSearchModel.getActualSearchTerm() != null && !elasticSearchModel.getActualSearchTerm().isEmpty()) {
                elastic_utils.saveWhereSearchHistory(elasticSearchModel.getActualSearchTerm(), username, template);
            }
            Query countQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();
            CountRequest totalCountRequest = CountRequest.of(cr -> cr.index(indexName)
                    .query(countQuery));
            long totalCount = esClient.count(totalCountRequest).count();
            long totalPages = (totalCount + pageSize - 1) / pageSize;
            searchCounterparty.put("totalPage", totalPages);
            searchCounterparty.put("count", totalCount);
            searchCounterparty.put("pageScroll", pageScroll);
            searchCounterparty.put("content", allDocuments);
            searchCounterparty.put("sorting", document);
            searchCounterparty.put("pitId", pitId);
            searchCounterparty.put("displayReportDate", newReportDate);
        }
        return searchCounterparty;
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getCounterpartyExport(String reportDate, String fileName, List<FieldMap> fieldMaps, String colour, List<String> fieldColumns, DownloadJob job, String outputFormat, String sortBy, String sortingOrder, String userName, int pageSize, boolean globalSearch, String bulkFilterSearchType, String tab, ElasticSearchModel elasticSearchModel,Boolean counterpartyCreation) {
        List<CounterPartyDocument> counterpartyList = new ArrayList<>();
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
            Document counterpartyData = getCounterpartyData(reportDate,
                    elasticSearchModel, sortBy, sortingOrder, pageSize, globalSearch, bulkFilterSearchType, tab, counterpartyCreation,userName);
            counterpartyList.addAll((Collection<? extends CounterPartyDocument>) counterpartyData.get("content"));
            docs = (Document) counterpartyData.get("sorting");
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, counterpartyList.size(), job.getId());
            totalPage = (long) counterpartyData.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (long) counterpartyData.get("count");
            log.info("TOTAL COUNT {}", totalCount);
        }
        log.info(" Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", counterpartyList.size(), (int) totalCount, job.getId());
        return elastic_utils.exportFileFromDetails(outputFormat, counterpartyList, fileName, fieldMaps, colour, job, fieldColumns, elasticSearchModel);
    }

    public List<Document> getCounterPartyUDFMultiFieldValues(String fieldName, String reportDate, String sortBy) throws IOException {
        String indexName = CounterPartyDocument.INDEX_NAME;

        // Check data is Available for  given Data , if not Available  , it will take latest Data:-
        String newReportDate = elastic_utils.reportDateCheck(reportDate, indexName, ClosingEntity.class);

        // Report Date Added - Query :-
        log.info("Listing Data for : {}", newReportDate);
        Query query = TermQuery.of(t -> t.field("reportDate").value(newReportDate))._toQuery();
        List<Query> queryList = new ArrayList<>();
        queryList.add(query);
        // added unique and single duplicate data
        Query duplicateData = TermQuery.of(t -> t.field("counterpartStatus.keyword").value("duplicate"))._toQuery();
        Query counterpartStaticBool = BoolQuery.of(b -> b.mustNot(duplicateData))._toQuery();
        queryList.add(counterpartStaticBool);
        Query searchQuery = BoolQuery.of(q -> q.must(queryList))._toQuery();

        NamedValue<SortOrder> sort = NamedValue.of("_key", SortOrder.Desc);
        Aggregation aggregation = Aggregation.of(a ->
                a.terms(TermsAggregation.of(terms ->
                        terms.field(fieldName + ".displaycolumnval").size(Integer.MAX_VALUE).order(List.of(sort)))));
        SearchRequest searchRequest = SearchRequest.of(s ->
                s.index(environment.getProperty("counterparty.search")).
                        query(searchQuery).
                        aggregations("Counterparty", aggregation));
        log.info("Search Request :{}", searchRequest);
        SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
        Map<String, Long> udfFieldValues = new HashMap<>();
        searchResponse.aggregations().get("Counterparty").sterms().buckets().array().forEach(b -> udfFieldValues.put(
                b.key().stringValue(), b.docCount()));
        List<String> keyList = new ArrayList<>(udfFieldValues.keySet());
        List<Long> countList = new ArrayList<>(udfFieldValues.values());
        List<Document> resultDocuments = checkDuplicates(countList, keyList);
        resultDocuments.sort(Comparator.comparingLong(doc -> (Long) doc.get("value")));
        if (sortBy.equalsIgnoreCase("desc")) {
            Collections.reverse(resultDocuments);
        }
        return resultDocuments;
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


    public void updateNewDormantCounterpartyField(String reportDate) throws IOException {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Map<String, FieldValue> afterKey = null;
        HashMap<String, Aggregation> aggregation = new HashMap<>();
        int iterationCount = getCount(reportDate, DormantCounterparty.INDEX_NAME);
        log.info("Iteration Count :{}", iterationCount);

        for (int i = 0; i < iterationCount; i++) {
            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("dspLabel.keyword")));
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
            Aggregation counterpartLabel = Aggregation.of(aa -> aa.composite(newAggregation.composite()));
            aggregation.put("dormantCounterparty", counterpartLabel);

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(DormantCounterparty.INDEX_NAME).aggregations(aggregation).query(reportDateQuery).size(environment.getProperty("aggregationPageSize", Integer.class)));
            log.info("Search Request - Dormant Counterparty :{}", searchRequest);

            SearchResponse<DormantCounterparty> searchResponse = esClient.search(searchRequest, DormantCounterparty.class);
            log.info("Search Response - Dormant Counterparty :{}", searchResponse.hits().hits().size());


            if (!searchResponse.hits().hits().isEmpty()) {
                for (Hit<DormantCounterparty> hit : searchResponse.hits().hits()) {
                    Integer cpId = Objects.requireNonNull(hit.source()).getCpId();
                    String mgrId = hit.source().getMgrId();
                    String oadId = hit.source().getOadId();
                    updateDormant(reportDate, cpId, mgrId, oadId);
                }
            } else {
                return;
            }
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("dormantCounterparty").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
    }

    private int getCount(String reportDate, String indexName) throws IOException {
        long iterationCount = 0;
        CountResponse countResponse = esClient.count(CountRequest.of(c -> c.query(TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery()).index(indexName)));
        long newCount = countResponse.count();
        if (newCount > iterationCount) {
            iterationCount = newCount;
        }
        long finalCount = Math.floorDiv(iterationCount, environment.getProperty("aggregationPageSize", Integer.class));
        return (int) finalCount + 1;
    }

    public void updateDormant(String reportDate, Integer cpId, String mgrId, String oadId) throws IOException {
        Query reportDateQuery = TermQuery.of(q -> q.field("reportDate").value(reportDate))._toQuery();
        Query cpIdQuery = TermQuery.of(q -> q.field("cpId").value(cpId))._toQuery();
        Query mgrIdQuery = TermQuery.of(q -> q.field("mgrId.keyword").value(mgrId))._toQuery();
        Query oadIdQuery = TermQuery.of(q -> q.field("oadId.keyword").value(oadId))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(cpIdQuery).must(mgrIdQuery).must(oadIdQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(environment.getProperty("counterparty.search")).query(boolQuery));
        SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
        if (!searchResponse.hits().hits().isEmpty()) {
            searchResponse.hits().hits().forEach(hit -> {
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("dormantCounterparty", "true");
                String documentId = hit.id();
                GetResponse<CounterPartyDocument> getResponse;
                try {
                    getResponse = esClient.get(g -> g
                            .index(environment.getProperty("counterparty.search"))
                            .id(documentId), CounterPartyDocument.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                long seqNo = getResponse.seqNo();
                long primaryTerm = getResponse.primaryTerm();
                UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u
                        .index(environment.getProperty("counterparty.search"))
                        .id(documentId)
                        .ifSeqNo(seqNo)
                        .ifPrimaryTerm(primaryTerm)
                        .doc(jsonMap));
                try {
                    esClient.update(updateRequest, CounterPartyDocument.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public void updateChineseWallTemplate(String reportDate) throws IOException {
        Map<String, FieldValue> afterKey = null;
        int iterationCount = getCount(reportDate, CounterPartyDocument.INDEX_NAME);
        // Initial search to determine the total number of hits
        for (int i = 0; i < iterationCount; i++) {
            List<Map<String, CompositeAggregationSource>> compoAgg = new ArrayList<>();
            CompositeAggregationSource aggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("dspLabel.displaycolumnval")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("composite", aggregationSource);
            compoAgg.add(map);

            Aggregation aggregation;
            Map<String, FieldValue> finalAfterKey = afterKey;
            if (afterKey != null) {
                aggregation = CompositeAggregation.of(c -> c.size(environment.getProperty("aggregationPageSize", Integer.class)).sources(compoAgg).after(finalAfterKey))._toAggregation();
            } else {
                aggregation = CompositeAggregation.of(c -> c.size(environment.getProperty("aggregationPageSize", Integer.class)).sources(compoAgg))._toAggregation();
            }
            Query query = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index(CounterPartyDocument.INDEX_NAME)
                    .query(query)
                    .aggregations("Aggregation", aggregation)
            );
            SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
            update(searchResponse, reportDate);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
    }

    public void update(SearchResponse<CounterPartyDocument> searchResponse, String reportDate) {
        Map<String, Long> distinctLabel = new TreeMap<>();
        CompositeAggregate compositeAgg = searchResponse.aggregations().get("Aggregation").composite();
        for (CompositeBucket bucket : compositeAgg.buckets().array()) {
            String compositeValue = bucket.key().get("composite").stringValue();
            long docCount = bucket.docCount();
            distinctLabel.put(compositeValue, docCount);
        }
        log.info("Distinct Label :{}", distinctLabel);
        for (String label : distinctLabel.keySet()) {

            Map<Object, Object> updateData = getChineseWallTemplate(label, reportDate);
            Query reportDateQuery = TermQuery.of(t ->
                    t.field("reportDate").value(reportDate))._toQuery();
            Query dspLabelQuery = TermQuery.of(t ->
                    t.field("dspLabel.keyword").value(label))._toQuery();
            Query boolQuery = BoolQuery.of(b ->
                    b.must(reportDateQuery).must(dspLabelQuery))._toQuery();

            Map<String, JsonData> params = new HashMap<>();
            for (Map.Entry<Object, Object> entry : updateData.entrySet()) {
                params.put(entry.getKey().toString(), JsonData.of(entry.getValue()));
            }
            // Updating Based on GroupLabels
            UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                    .index(CounterPartyDocument.INDEX_NAME)
                    .script(s -> s.inline(InlineScript.of(il ->
                            il.source("ctx._source.chineseWallTemplate = params.chineseWallTemplate;").
                                    lang("painless").
                                    params(params)))).
                    query(boolQuery));

          //  log.info("Update Request - UpdateByQueryRequest : {}", updateByQueryRequest);
            try {
                esClient.updateByQuery(updateByQueryRequest);
                log.info("Successfully ChineseWallTemplate Updated");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Map<Object, Object> getChineseWallTemplate(String template, String reportDate) {
        Map<Object, Object> templateLabelMap = new HashMap<>();
        List<String> templateLabel = new ArrayList<>();
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query templateQuery = TermQuery.of(t -> t.field("counterpartLabel.keyword").value(template))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(templateQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.query(boolQuery).size(environment.getProperty("aggregationPageSize", Integer.class)).index(ChineseWall.INDEX_NAME));
        SearchResponse<ChineseWall> searchResponse;
        try {
            searchResponse = esClient.search(searchRequest, ChineseWall.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Hit<ChineseWall> hit : searchResponse.hits().hits()) {
            String chineseWallTemplate = Objects.requireNonNull(hit.source()).getTemplateLabel();
            templateLabel.add(chineseWallTemplate);
        }
        templateLabelMap.put("chineseWallTemplate", templateLabel);
        return templateLabelMap;
    }

    public Boolean doesCounterpartyExist(String reportDate) throws IOException {
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(CounterPartyDocument.INDEX_NAME).query(reportDateQuery));
        SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
        if (!searchResponse.hits().hits().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    @SneakyThrows
    public List<String> getDuplicateDspLabel(String repDate) {

        CountRequest countRequest = CountRequest.of(c -> c.index(CounterPartyDocument.INDEX_NAME).query(TermQuery.of(t -> t.field("reportDate").value(repDate))._toQuery()));
        CountResponse countResponse = esClient.count(countRequest);
        Long totalCount = countResponse.count();

        log.info("TotalCount :{}", totalCount);
        long iterationCount = (totalCount / 10000) + 1;
        log.info("total count : {} iterationCount : {}", totalCount, iterationCount);


        Query reportDate = TermQuery.of(t -> t.field("reportDate").value(repDate))._toQuery();
        List<String> cpLabel = new ArrayList<>();
        Map<String, FieldValue> afterKey = null;
        for (int i = 0; i < iterationCount; i++) {

            Aggregation aggregation;
            HashMap<String, Aggregation> aggregations = new HashMap<>();

            List<Map<String, CompositeAggregationSource>> compAgg = new ArrayList<>();
            CompositeAggregationSource compositeAggregationSource = CompositeAggregationSource.of(cs -> cs.terms(t -> t.field("dspLabel.keyword")));
            Map<String, CompositeAggregationSource> map = new HashMap<>();
            map.put("Composite", compositeAggregationSource);
            compAgg.add(map);

            Map<String, FieldValue> finalAfterKey = afterKey;

            if (afterKey != null) {
                aggregation = CompositeAggregation.of(c -> c.sources(compAgg).size(10000).after(finalAfterKey))._toAggregation();
            } else {
                aggregation = CompositeAggregation.of(c -> c.sources(compAgg).size(10000))._toAggregation();
            }

            Script script = Script.of(s -> s.inline(InlineScript.of(ii -> ii.source("params.duplicate_data_count >1"))));

            Map<String, String> bucketsPathMap = new HashMap<>();
            bucketsPathMap.put("duplicate_data_count", "_count");


            Aggregation agg = Aggregation.of(a -> a.bucketSelector(BucketSelectorAggregation.of(bs ->
                    bs.bucketsPath(BucketsPath.of(bp -> bp.dict(bucketsPathMap))).script(script))._toAggregation().bucketSelector()));


            Aggregation newAggregation = Aggregation.of(a -> a.composite(aggregation.composite()).aggregations("fetching_duplicate_dsplabel", agg));

            aggregations.put("unique_data", newAggregation);

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(CounterPartyDocument.INDEX_NAME).query(reportDate).aggregations(aggregations));

            //log.info("Search request-------------:{}", searchRequest);
            SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
            List<CompositeBucket> buckets = searchResponse.aggregations().get("unique_data").composite().buckets().array();
            List<String> dspLabels = new ArrayList<>();
            for (CompositeBucket data : buckets) {
                Map<String, FieldValue> keyMap = data.key();
                FieldValue counterpartLabel = keyMap.get("Composite");
                if (counterpartLabel != null) {
                    dspLabels.add(counterpartLabel.stringValue().toUpperCase());
                }
            }
            cpLabel.addAll(dspLabels);
            CompositeAggregate compositeAgg = searchResponse.aggregations().get("unique_data").composite();
            afterKey = compositeAgg.afterKey();
            if (afterKey == null) {
                break;
            }
        }
        return cpLabel;
    }


    // Here we update all Duplicate data to counterpartStatus to counterpartStatus:-
    @SneakyThrows
    public void updateAllDuplicate(String reportDate) {
        List<String> duplicateData = getDuplicateDspLabel(reportDate);
        var array = new ArrayList<FieldValue>();
        for (String newData : duplicateData) {
            array.add(FieldValue.of(newData));
        }
        Query dspLabelQuery = TermsQuery.of(t ->
                t.field("dspLabel.keyword").terms(TermsQueryField.of(ty -> ty.value(array))))._toQuery();
        Query repQuery = TermQuery.of(t ->
                t.field("reportDate").value(reportDate))._toQuery();
        Query boolQuery = BoolQuery.of(b ->
                b.must(repQuery).must(dspLabelQuery))._toQuery();

        Map<String, JsonData> params = new HashMap<>();
        params.put("counterpartStatus", JsonData.of("duplicate"));
        UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                .index(CounterPartyDocument.INDEX_NAME)
                .script(s -> s.inline(InlineScript.of(i ->
                        i.source("ctx._source.counterpartStatus = params.counterpartStatus;")
                                .lang("painless")
                                .params(params))))
                .query(boolQuery));
        try {
            esClient.updateByQuery(updateByQueryRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("updated Successfully");
        updateDuplicateTopHits(reportDate);

    }


    // update - from duplicate data ...top hit only will update to duplicateStatic
// update - from duplicate data ...top hit only will update
    @SneakyThrows
    public void updateDuplicateTopHits(String reportDate) {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));

        List<String> duplicateData = getDuplicateDspLabel(reportDate);

        for (String dspLabel : duplicateData) {

            Query repQuery = TermQuery.of(t ->
                    t.field("reportDate").value(reportDate))._toQuery();
            Query dspLabelQuery = TermQuery.of(t ->
                    t.field("dspLabel.keyword").value(dspLabel))._toQuery();
            Query boolQuery = BoolQuery.of(b ->
                    b.must(repQuery).must(dspLabelQuery))._toQuery();


            SortOptions sortOptions1 = SortOptions.of(s ->
                    s.field(FieldSort.of(fs -> fs.field("mgrId.keyword").order(SortOrder.Desc))));
            SortOptions sortOptions2 = SortOptions.of(s ->
                    s.field(FieldSort.of(fs -> fs.field("oadId.keyword").order(SortOrder.Desc))));

            SearchRequest searchRequest = SearchRequest.of(s ->
                    s.query(boolQuery).
                            size(environment.getProperty("aggregationPageSize", Integer.class)).
                            index(CounterPartyDocument.INDEX_NAME));

            SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);

            List<String> mgrIds = new ArrayList<>();
            List<String> oadIds = new ArrayList<>();

            Map<String, String> params = new HashMap<>();
            for (Hit<CounterPartyDocument> hit : searchResponse.hits().hits()) {
                assert hit.source() != null;
                mgrIds.add(hit.source().getMgrId());
                oadIds.add(hit.source().getOadId());
            }
            // check whether the list contains same values?
            boolean allMgrIdsSame = mgrIds.stream().distinct().count() == 1;
            if (allMgrIdsSame) {
                params.put("duplicateCp", "oadId");
            } else {
                params.put("duplicateCp", "mgrId");
            }
            SearchRequest searchRequestNew = SearchRequest.of(s ->
                    s.query(boolQuery).
                            size(1).
                            index(CounterPartyDocument.INDEX_NAME)
                            .sort(sortOptions1, sortOptions2));

            SearchResponse<CounterPartyDocument> searchResponseNew = esClient.search(searchRequestNew, CounterPartyDocument.class);

            if (!searchResponseNew.hits().hits().isEmpty()) {
                searchResponseNew.hits().hits().forEach(hit -> {
                    //  Map<String, String> params = new HashMap<>();
                    params.put("counterpartStatus", "duplicateStatic");
                    UpdateRequest<Object, Object> updateRequest = UpdateRequest.of(u -> u.
                            doc(params).
                            index(CounterPartyDocument.INDEX_NAME).
                            id(hit.id()));
                    try {
                        esClient.update(updateRequest, CounterPartyDocument.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
