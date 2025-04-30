package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateAction;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.finsurge.tmr_portal.general.entity.DownloadJob;
import com.finsurge.tmr_portal.general.services.DownloadJobService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.Department;
import com.finsurge.tmr_portal.mx_superview.elastic_search.models.ElasticSearchModel;
import com.finsurge.tmr_portal.mx_superview.models.FieldMap;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import org.bson.Document;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class DepartmentService {

    private final static Logger log = LoggerFactory.getLogger(DepartmentService.class);
    @Autowired
    ElasticsearchClient esClient;

    @Autowired
    public Environment environment;

    @Autowired
    private DownloadJobService downloadJobService;

    @Autowired
    private ElasticUtils elastic_utils;

    @Autowired
    private GroupLabelDepartmentService groupLabelDepartmentService;

    public ResponseEntity<?> insertDepartment(List<String> departmentNames) {
        // Going to insert a Department data in ES:-
        StringBuilder responseMessage = new StringBuilder();
        BulkResponse response = null;
        for (String department : departmentNames) {
            try {
                if (!isDepartmentExist(department)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("department", department);
                    map.put("status", "active");
                    BulkRequest.Builder insert = new BulkRequest.Builder();
                    insert.operations(o -> o.index(i ->
                            i.index(Department.INDEX_NAME).
                                    document(map)));
                    response = esClient.bulk(insert.build());
                    responseMessage.append("Department '").append(department).append("' created successfully.\n");
                } else {
                    log.warn("Department '{}' already exists, skipping insertion.", department);
                    responseMessage.append("Creation Failed Department '").append(department).append("' already exists\n");
                }
            } catch (IOException e) {
                log.error("Error occurred while bulk indexing departments: " + e.getMessage(), e);
                responseMessage.append("Error occurred while processing department '").append(department).append("': ").append(e.getMessage()).append("\n");
            }
        }
        return ResponseEntity.ok(responseMessage.toString().trim());
    }

    // Checking if the Department Exists in the Department Index
    public Boolean isDepartmentExist(String department) throws IOException {
        Query departmentExist = TermQuery.of(t -> t.field("department.keyword").value(department))._toQuery();
        SearchRequest searchRequest = SearchRequest.of(s -> s.index(Department.INDEX_NAME).query(departmentExist));
        SearchResponse<Department> searchResponse = esClient.search(searchRequest, Department.class);
        log.info("Count of Department present in department index -> {}", searchResponse.hits().hits().size());
        return !searchResponse.hits().hits().isEmpty();
    }

    public <T> Document listData(String sortBy, String orderBy, int pageSize) {
        SearchResponse<Department> departmentSearchResponse = null;
        try {
            // Define the sorting options
            SortOptions sortOptions;
            if (sortBy != null) {
                sortBy = sortBy + ".keyword";
                String finalSortBy = sortBy;
                sortOptions = SortOptions.of(s -> s.field(FieldSort.of(fs ->
                        fs.field(finalSortBy).order(orderBy.equalsIgnoreCase("asc") ? SortOrder.Asc : SortOrder.Desc))));
            } else {
                sortOptions = null;
            }

            // Create the search request
            SearchRequest searchRequest = SearchRequest.of(s -> {
                s.index(Department.INDEX_NAME).size(environment.getProperty("aggregationPageSize", Integer.class));
                if (sortOptions != null) {
                    s.sort(sortOptions);
                }
                return s;
            });

            // Execute the search request
            departmentSearchResponse = esClient.search(searchRequest, Department.class);
        } catch (ElasticsearchException e) {
            log.error("Elasticsearch exception: {}", e);
            throw new RuntimeException("Elasticsearch query execution failed", e);
        } catch (IOException e) {
            log.error("IO exception: {}", e.getMessage());
            throw new RuntimeException("IO exception occurred while executing query", e);
        }
        Document resultDocument = new Document();
        List<Map<String, Object>> departmentList = new ArrayList<>();
        // Process the search response
        if (departmentSearchResponse != null && !departmentSearchResponse.hits().hits().isEmpty()) {
            List<Hit<Department>> departments = departmentSearchResponse.hits().hits();
            for (Hit<Department> department : departments) {
                assert department.source() != null;
                Document document = new Document();
                Object dep = department.source().getDepartment();
                Object status = department.source().getStatus();
                String id = department.id();
                document.put("id", id);
                document.put("department", dep);
                document.put("status", status);
                departmentList.add(document);
            }
            int totalFieldCount = departmentList.size();
            long totalPages = (totalFieldCount + pageSize - 1) / pageSize;
            resultDocument.put("departmentList", departmentList);
            resultDocument.put("totalCount", totalFieldCount); // Adding the total count of fieldsAndCounts
            resultDocument.put("totalPage", totalPages);
        } else {
            log.error("Data Not Found");
        }
        return resultDocument;
    }


    public ResponseEntity<?> updateDepartment(String id, String existingDepartmentName, String newDepartmentName, String name) throws IOException {
        log.info("The Existing Department Name is: {}", existingDepartmentName);
        groupLabelDepartmentService.updateGroupLabelDepartments(existingDepartmentName, newDepartmentName, name);
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        Map<String, String> updateMap = new HashMap<>();
        updateMap.put("department", newDepartmentName);
        UpdateAction<Object, Object> updateAction = UpdateAction.of(a -> a.doc(updateMap));
        bulkRequest.operations(BulkOperation.of(b -> b.update(UpdateOperation.of(u -> u.index(Department.INDEX_NAME).id(id).retryOnConflict(3).action(updateAction)))));
        try {
            esClient.bulk(bulkRequest.build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("Updated Successfully , This will Update the Department in Group label Department ");
    }

    public ResponseEntity<?> updateDepartmentStatus(String id, String departmentName, String name) throws IOException {
        // Fetch the current status of the department
        Department department = esClient.get(g -> g.index(Department.INDEX_NAME).id(id), Department.class).source();

        if (department == null) {
            log.error("Department not found for id: " + id);
            return ResponseEntity.status(404).body("Department not found");
        }

        String currentStatus = department.getStatus();
        String newStatus = currentStatus.equals("active") ? "inactive" : "active";
        log.info("Starting to update department status to {}", newStatus);

        String responseMessage;

        // Update related group label departments if setting to inactive
        if (newStatus.equals("inactive")) {
            String newDepartmentName = "";
            groupLabelDepartmentService.updateGroupLabelDepartments(departmentName, newDepartmentName, name);
            responseMessage = "Status Updated Successfully, it will update the department in Group Label Department Page";
        } else {
            responseMessage = "Department Updated Successfully";
        }

        // Define the script to update the status and other fields
        String scriptSource = "ctx._source.status = params.status; " +
                "ctx._source.username = params.username; " +
                "ctx._source.updatedTime = params.updatedTime";

        Map<String, JsonData> scriptParams = new HashMap<>();
        scriptParams.put("status", JsonData.of(newStatus));
        scriptParams.put("username", JsonData.of(name));
        scriptParams.put("updatedTime", JsonData.of(String.valueOf(LocalDateTime.now())));

        InlineScript inlineScript = InlineScript.of(i -> i
                .lang(ScriptLanguage.Painless)
                .source(scriptSource)
                .params(scriptParams)
        );

        Script script = Script.of(s -> s.inline(inlineScript));

        // Create the update request
        UpdateByQueryRequest updateByQueryRequest = UpdateByQueryRequest.of(ub -> ub
                .index(Department.INDEX_NAME)
                .query(TermQuery.of(t -> t.field("_id").value(id))._toQuery())
                .script(script)
        );

        log.info("Update Request - UpdateByQueryRequest: {}", updateByQueryRequest);

        try {
            UpdateByQueryResponse updateByQueryResponse = esClient.updateByQuery(updateByQueryRequest);
            if (updateByQueryResponse.updated() == 0) {
                log.error("Department not found for id: " + id);
                return ResponseEntity.status(404).body("Department not found");
            }
            log.info("Successfully updated department status to {} for id: {}", newStatus, id);
            return ResponseEntity.ok(responseMessage);
        } catch (IOException e) {
            log.error("Error occurred while updating department status to {}: {}", newStatus, e.getMessage(), e);
            return ResponseEntity.status(500).body("Error occurred while updating department status");
        }
    }

    @Async
    @Transactional
    public CompletableFuture<Void> getListDepartmentExport(String fileName, List<FieldMap> fieldMaps, String color, List<String> fieldColumns, String name,
                                                           DownloadJob job, String outputFormat, String sortBy, String orderBy, int pageSize, ElasticSearchModel elasticSearchModel) throws Exception {
        List<Object> departmentList = new ArrayList<>();
        elasticSearchModel.fieldColumns = new ArrayList<>();
        long totalCount = 0;
        long totalPage = 1;
        log.info("Trade UDF Export: Initializing job status.");
        downloadJobService.updateJobProgress("FETCH_ITEMS", 0, 0, job.getId());
        log.info("Trade UDF Export: Starting fetch.");
        for (int i = 0; i < totalPage; i++) {
            // Fetch the trade UDF data for the current page
            Document searchListDepartment = listData(sortBy, orderBy, pageSize);
            // Accumulate the trade UDF data
            departmentList.addAll((Collection<?>) searchListDepartment.get("departmentList"));
            // Update job progress
            downloadJobService.updateJobProgress("FETCH_ITEMS", (int) totalCount, departmentList.size(), job.getId());
            // Fetch total pages and count for pagination
            totalPage = (long) searchListDepartment.get("totalPage");
            log.info("TOTAL PAGE {}", totalPage);
            totalCount = (int) searchListDepartment.get("totalCount");
            log.info("TOTAL COUNT {}", totalCount);

        }
        log.info("Trade UDF Export: Fetch complete.");
        downloadJobService.updateJobProgress("FILE_WRITE", departmentList.size(), (int) totalCount, job.getId());
        // Write the fetched data to the file (CSV/XLS/XLSX/XLSB)
        return elastic_utils.exportFileFromDetails(outputFormat, departmentList, fileName, fieldMaps, color, job, fieldColumns, elasticSearchModel);
    }

}
