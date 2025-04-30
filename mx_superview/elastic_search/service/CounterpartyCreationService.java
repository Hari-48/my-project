package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateAction;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.CounterPartyDocument;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.CounterpartyCreation;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import lombok.SneakyThrows;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class CounterpartyCreationService {

    private static final Logger log = LoggerFactory.getLogger(CounterpartyCreation.class);

    @Autowired
    ElasticUtils elastic_utils;
    public final Environment environment;
    private final ElasticsearchClient esClient;

    public CounterpartyCreationService(SearchHistoryRepository searchHistoryRepository, Environment environment, ElasticsearchClient esClient) {
        this.environment = environment;
        this.esClient = esClient;
    }

    public Document getWeeklyDatesCount(String reportDate) throws IOException {

        String indexName = CounterpartyCreation.INDEX_NAME;
        String newReportDate = elastic_utils.reportDateCheck(reportDate, indexName, CounterpartyCreation.class);

        HashMap<String, Aggregation> aggregationMap = new HashMap<>();
        Aggregation aggregation = Aggregation.of(a ->
                a.dateHistogram(DateHistogramAggregation.of(d ->
                        d.field("creationDate").calendarInterval(CalendarInterval.Week).format("yyyy-MM-dd").timeZone("+00:00"))));
        aggregationMap.put("weeklyReportDate", aggregation);

        log.info("Listing Data for : {}", newReportDate);
        Query dateQuery = TermQuery.of(t -> t.field("reportDate").value(newReportDate))._toQuery();

        SearchRequest searchRequest = SearchRequest.of(s -> s.index(CounterpartyCreation.INDEX_NAME).query(dateQuery).aggregations(aggregationMap));
        log.info("Request---:{}", searchRequest);
        SearchResponse<CounterpartyCreation> searchResponse = esClient.search(searchRequest, CounterpartyCreation.class);

        Map<String, Long> weeklyReport = new TreeMap<>();
        if (searchResponse.aggregations() != null && searchResponse.aggregations().get("weeklyReportDate") != null) {
            searchResponse.aggregations().get("weeklyReportDate").dateHistogram().buckets().array().forEach(b -> weeklyReport.put(String.valueOf(b.keyAsString()), b.docCount()));
        }
        Document document = new Document();
        document.put("WeeklyReport", weeklyReport);
        document.put("displayReportDate", newReportDate);
        return document;
    }


    public Integer getCountOfWeek(String reportDate) {
        LocalDate date = LocalDate.parse(reportDate);
        WeekFields weekFields = WeekFields.ISO;
        return date.get(weekFields.weekOfWeekBasedYear());
    }


    public Document getCpId(String reportDate, String creationDate) throws IOException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
        LocalDate fromDate = LocalDate.parse(creationDate);
        String formattedFromDate = fromDate.format(formatter);
        LocalDate oneWeek = fromDate.plusWeeks(1);
        String formattedToDate = oneWeek.format(formatter);
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query dateQuery = RangeQuery.of(r ->
                r.field("creationDate").gte(JsonData.of(formattedFromDate)).lt(JsonData.of(formattedToDate)))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(dateQuery))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(CounterpartyCreation.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class)).query(boolQuery));
        SearchResponse<CounterpartyCreation> searchResponse = esClient.search(searchRequest, CounterpartyCreation.class);
        Document data = new Document();
        List<String> cpIdList = new ArrayList<>();
        //   List<String> mgrIdList = new ArrayList<>();
        for (Hit<CounterpartyCreation> hit : searchResponse.hits().hits()) {
            String cpId = String.valueOf(Objects.requireNonNull(hit.source()).getCpId());
            //   String mgrId = String.valueOf(hit.source().getMgrId());
            //   mgrIdList.add(mgrId);
            cpIdList.add(cpId);
        }
        data.put("cpId", cpIdList);
        //      data.put("mgrId",mgrIdList);
        return data;
    }

    @SneakyThrows
    public void updateCreationDate(String reportDate) throws IOException {
        Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime", Integer.class));
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(CounterpartyCreation.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class)).query(reportDateQuery));
        SearchResponse<CounterpartyCreation> searchResponse = esClient.search(searchRequest, CounterpartyCreation.class);
        for (Hit<CounterpartyCreation> hit : searchResponse.hits().hits()) {
            String mgrId = Objects.requireNonNull(hit.source()).getMgrId();
            String oadId = Objects.requireNonNull(hit.source()).getOadId();
            Integer cpId = Objects.requireNonNull(hit.source()).getCpId();
            String creationDate = Objects.requireNonNull(hit.source()).getCreationDate();
            update(mgrId, oadId, cpId, creationDate, reportDate);
        }
    }

    private void update(String mgrId, String oadId, Integer cpId, String creationDate, String reportDate) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
        LocalDate fromDate;
        try {
            fromDate = LocalDate.parse(creationDate, formatter);
        } catch (DateTimeParseException e) {
            throw new IOException("Invalid date format for creationDate: " + creationDate, e);
        }
        String formattedFromDate = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Map<String, String> updateMap = new HashMap<>();
        updateMap.put("creationDate", formattedFromDate);

        // Construct queries
        Query reportDateQuery = TermQuery.of(t -> t.field("reportDate").value(reportDate))._toQuery();
        Query mgrIdQuery = TermQuery.of(t -> t.field("mgrId.keyword").value(mgrId))._toQuery();
        Query oadIdQuery = TermQuery.of(t -> t.field("oadId.keyword").value(oadId))._toQuery();
        Query cpIdQuery = TermQuery.of(t -> t.field("cpId").value(cpId))._toQuery();
        Query boolQuery = BoolQuery.of(b -> b.must(reportDateQuery).must(mgrIdQuery).must(oadIdQuery).must(cpIdQuery))._toQuery();

        // Execute search
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(CounterPartyDocument.INDEX_NAME)
                .size(environment.getProperty("aggregationPageSize", Integer.class))
                .query(boolQuery));
        SearchResponse<CounterPartyDocument> searchResponse = esClient.search(searchRequest, CounterPartyDocument.class);
        int numHits = searchResponse.hits().hits().size();
        if (numHits == 0) {
//            System.out.println("No documents found for the provided criteria:");
            log.info("mgrId: {}, oadId: {}, cpId: {}, creationDate: {}, reportDate: {}", mgrId, oadId, cpId, creationDate, reportDate);
            return;
        }

        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        for (Hit<CounterPartyDocument> hit : searchResponse.hits().hits()) {
            String docId = hit.id();
            UpdateAction<Object, Object> updateAction = UpdateAction.of(a -> a.doc(updateMap));

            bulkRequest.operations(BulkOperation.of(b -> b.update(UpdateOperation.of(u ->
                    u.index(CounterPartyDocument.INDEX_NAME).id(docId).retryOnConflict(3).action(updateAction)))));
        }

        try {
            esClient.bulk(bulkRequest.build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}





