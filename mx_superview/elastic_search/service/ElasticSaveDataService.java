package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ElasticSaveDataService {

    private final ElasticsearchClient esClient;

    public ElasticSaveDataService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void saveReport(List<?> report, String index) throws IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Object data : report) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(index)
                            .document(data)
                    )
            );
        }
        BulkResponse result = esClient.bulk(br.build());
    }
}
