package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.*;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ElasticIndexCreationService {

    private final ElasticsearchClient esClient;
    @Autowired
    private Environment environment;

    @PostConstruct
    public void createDynamicTemplate() throws IOException, ParseException {
        File resource = new ClassPathResource("elastic/mappings/dynamicTemplate.json").getFile();
        String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(defaultConfig);
        String templateName = environment.getProperty("uam.dynamicTemplate");
        boolean templateExists = false;
        try {
            GetIndexTemplateRequest getIndexTemplateRequest = GetIndexTemplateRequest.of(b -> b.name(templateName));
            GetIndexTemplateResponse getIndexTemplateResponse = esClient.indices().getIndexTemplate(getIndexTemplateRequest);
            templateExists = getIndexTemplateResponse.indexTemplates().stream().anyMatch(t -> templateName.equals(t.name()));
        } catch (ElasticsearchException e) {
            if (Objects.equals(e.error().type(), "resource_not_found_exception")) {
                log.info("Template '{}' does not exist. Proceeding to create it.", templateName);
            } else {
                throw e;
            }
        }
        if (templateExists) {
            log.info("Template '{}' already exists. Skipping creation.", templateName);
            return;
        }
        // Create PutIndexTemplateRequest using ElasticsearchClient
        PutIndexTemplateRequest putIndexTemplateRequest = PutIndexTemplateRequest.of(b -> b
                .name(templateName)
                .indexPatterns("uam*")
                .priority(1)
                .template(t -> t
                        .settings(s -> s
                                .withJson(new StringReader(jsonObject.get("settings").toString()))
                        )
                        .mappings(m -> m
                                .withJson(new StringReader(jsonObject.get("mappings").toString()))
                        )
                )
        );
        PutIndexTemplateResponse putIndexTemplateResponse = esClient.indices().putIndexTemplate(putIndexTemplateRequest);
        log.info("Dynamic Template creation acknowledged: {}", putIndexTemplateResponse.acknowledged());
    }

    @PostConstruct
    public void createIndexesIfNotExist() {
        createIndex(CounterPartyDocument.INDEX_NAME, CounterPartyDocument.MAPPING_PATH);
        createIndex(DormantCounterparty.INDEX_NAME, DormantCounterparty.MAPPING_PATH);
        createIndex(CounterpartyCreation.INDEX_NAME, CounterpartyCreation.MAPPING_PATH);
        createIndex(ClosingEntity.INDEX_NAME, ClosingEntity.MAPPING_PATH);
        createIndex(UserGroupAccessRights.INDEX_NAME, UserGroupAccessRights.MAPPING_PATH);
        createIndex(UserList.INDEX_NAME, UserList.MAPPING_PATH);
        createIndex(UserGroupList.INDEX_NAME, UserGroupList.MAPPING_PATH);
        createIndex(UserLicense.INDEX_NAME, UserLicense.MAPPING_PATH);
        createIndex(UserPolicy.INDEX_NAME, UserPolicy.MAPPING_PATH);
        createIndex(GroupPortfolioRights.INDEX_NAME, GroupPortfolioRights.MAPPING_PATH);
        createIndex(ChineseWall.INDEX_NAME, ChineseWall.MAPPING_PATH);
        createIndex(GroupNavigationRights.INDEX_NAME, GroupNavigationRights.MAPPING_PATH);
        createIndex(Department.INDEX_NAME, Department.MAPPING_PATH);
        createIndex(GroupLabelDepartment.INDEX_NAME, GroupLabelDepartment.MAPPING_PATH);
        createIndex(GroupComboPortfolio.INDEX_NAME, GroupComboPortfolio.MAPPING_PATH);
        createIndex(OperationRights.INDEX_NAME, OperationRights.MAPPING_PATH);
        createIndex(OspRightsMatrix.INDEX_NAME, OspRightsMatrix.MAPPING_PATH);
        createIndex(EnterpriseRisk.INDEX_NAME, EnterpriseRisk.MAPPING_PATH);
        createIndex(FinanceRights.INDEX_NAME, FinanceRights.MAPPING_PATH);
        createIndex(ConsistencyTmpl.INDEX_NAME, ConsistencyTmpl.MAPPING_PATH);
        createIndex(ConfigMgtRight.INDEX_NAME, ConfigMgtRight.MAPPING_PATH);
        createIndex(DisplaySI.INDEX_NAME, DisplaySI.MAPPING_PATH);
        createIndex(SupChgAudit.INDEX_NAME, SupChgAudit.MAPPING_PATH);
        createIndex(UdfStructureIrd.INDEX_NAME, UdfStructureIrd.MAPPING_PATH);
        createIndex(UdfStructureComm.INDEX_NAME, UdfStructureComm.MAPPING_PATH);
        createIndex(UdfStructureFxd.INDEX_NAME, UdfStructureFxd.MAPPING_PATH);
        createIndex(UserLoginAudit.INDEX_NAME, UserLoginAudit.MAPPING_PATH);
        createIndex(NavigationRightsDepartment.INDEX_NAME, NavigationRightsDepartment.MAPPING_PATH);
        createIndex(StpRightsSrcMod.INDEX_NAME, StpRightsSrcMod.MAPPING_PATH);
        createIndex(StpRightsTypology.INDEX_NAME, StpRightsTypology.MAPPING_PATH);
        createIndex(StpRightsMatrix.INDEX_NAME, StpRightsMatrix.MAPPING_PATH);
        createIndex(ChineseWallClosingEntityLabel.INDEX_NAME, ChineseWallClosingEntityLabel.MAPPING_PATH);
        createIndex(ClosingEntityLabel.INDEX_NAME, ClosingEntityLabel.MAPPING_PATH);

    }

    public void createIndex(String indexName, String mappingJsonPath) {
        ExistsRequest getIndexRequest = ExistsRequest.of(g -> g.index(indexName));
        boolean indexExists = false;
        try {
            indexExists = esClient.indices().exists(getIndexRequest).value();
        } catch (IOException e) {
            log.error("IOException occurred: {}", e.getMessage());
        }
        if (!indexExists) {
            try {
                File resource = new ClassPathResource(mappingJsonPath).getFile();
                String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(defaultConfig);
                Reader jsonReader = new StringReader(jsonObject.toString());
                CreateIndexRequest request = CreateIndexRequest.of(i -> i
                        .index(indexName)
                        .withJson(jsonReader)
                );
                boolean response = esClient.indices().create(request).acknowledged();
                if (response) {
                    log.warn("Index {} created.", indexName);
                } else {
                    log.error("Failed to create index {}.", indexName);
                }
            } catch (IOException e) {
                log.error("IOException occurred: {}", e.getMessage());
            } catch (ParseException e) {
                log.error("ParseException occurred: {}", e.getMessage());
            }
        } else {
            log.warn("Index {} already exists. Skipping index creation.", indexName);
        }
    }
}
