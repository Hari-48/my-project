package com.finsurge.tmr_portal.mx_superview.configs.elastic_data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.finsurge.tmr_portal.mx_superview.configs.Listener;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.models.DataLoaderProcessLog;

import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.MxLogType;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import static com.finsurge.tmr_portal.mx_superview.configs.LoaderConfig.logWriter;


@Configuration
@EnableBatchProcessing
@Data
public class CsvJobConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CsvJobConfiguration.class);

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    @Autowired
    private Listener listener;

    @Autowired
    private DataImportJobRepository dataImportJobRepository;

    public String reportDate;

    public static LinkedBlockingQueue<DataLoaderProcessLog> logsQueue;

    @Autowired
    private ElasticSearchItemWriter elasticSearchItemWriter;

    @Bean
    public Step csvToElasticSearchStep() throws IOException, ClassNotFoundException, InterruptedException {
        return stepBuilderFactory.get("csvToElasticSearchStep")
                .chunk(10000)
                .reader(csvReader(null, null,null,null))
                //  .processor(process())
                .writer(elasticSearchItemWriter)
                .build();
    }
     public Job csvToElasticSearchJob(String jobName) throws Exception {
        // initialize logs queue
        logsQueue = new LinkedBlockingQueue<>();
        return jobBuilderFactory.get(jobName)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                //.start(csvToElasticSearchStep())
                .flow(csvToElasticSearchStep())
                .end()
                .build();
    }


    @Bean
    @StepScope
    public  FlatFileItemReader<?> csvReader(@Value("#{jobParameters['csvFilePath']}") String csvFilePath,
                                            @Value("#{jobParameters['csvJsonPath']}") String csvJsonPath,
                                            @Value("#{jobParameters['entityName']}" )String tableName,
                                            @Value("#{jobParameters['importJobId']}") Long importJobId) throws IOException, InterruptedException {

        log.info("TABLE NAME : {}",tableName);
        Class<?> clazz = null;
        switch(tableName){
            case"UAM_MX_DORMANT_COUNTERPARTY":{
                clazz= DormantCounterparty.class;
                break;
            }
            case"UAM_MX_COUNTER_PARTY":{
                clazz=CounterPartyDocument.class;
                break;
            }
            case"UAM_MX_CLOSING_ENTITY":{
                clazz= ClosingEntity.class;
                break;
            }
            case "UAM_MX_COUNTERPARTY_CREATION":{
                clazz= CounterpartyCreation.class;
                break;
            }
            case"UAM_USER_GROUP_ACCESS_RIGHT":{
                clazz= UserGroupAccessRights.class;
                break;
            }
            case"UAM_MX_USER_LIST":{
                clazz = UserList.class;
                break;
            }
            case"UAM_MX_GROUP_LIST":{
                clazz = UserGroupList.class;
                break;
            }
            case"UAM_MX_USER_LICENSE":{
                clazz = UserLicense.class;
                break;
            }
            case"UAM_MX_USER_POLICY":{
                clazz = UserPolicy.class;
                break;
            }
            case"UAM_MX_GROUP_PORTFOLIO_RIGHTS":{
                clazz = GroupPortfolioRights.class;
                break;
            } case"UAM_MX_GROUP_NAV_RIGHTS": {
                clazz = GroupNavigationRights.class;
                break;
            }
            case"UAM_MX_CHINESE_WALL_TMPL":{
                clazz = ChineseWall.class;
                break;
            }
            case "UAM_MX_GROUP_COMBINED_PORTFOLIO": {
                clazz = GroupComboPortfolio.class;
                break;
            }
            case "UAM_MX_OPERATION_RIGHTS": {
                clazz = OperationRights.class;
                break;
            }
            case "UAM_MX_OSP_RIGHTS_MATRIX": {
                clazz = OspRightsMatrix.class;
                break;
            }
            case "UAM_MX_ENTERPRISE_RISK": {
                clazz = EnterpriseRisk.class;
                break;
            }
            case "UAM_MX_FINANCE_ACCTRL_RIGHTS": {
                clazz = FinanceRights.class;
                break;
            }
            case "UAM_MX_CONSISTENCY_TMPL": {
                clazz = ConsistencyTmpl.class;
                break;
            }
            case "UAM_MX_CWT_CONFIG_MGT_RIGHT": {
                clazz = ConfigMgtRight.class;
                break;
            }
//            case "UAM_MX_COUNTER_PARTY_DISPLAY": {
//                clazz = DisplaySI.class;
//                break;
//            }
            case "UAM_MX_SUPCHG_AUDIT": {
                clazz = SupChgAudit.class;
                break;
            }
            case "UAM_MX_COUNTER_PARTY_DISPLAY": {
                clazz = DisplaySI.class;
                break;
            }
            case "UAM_UDF_STRUCTURE_IRD":{
                clazz = UdfStructureIrd.class;
                break;
            }
            case "UAM_UDF_STRUCTURE_COMM":{
                clazz = UdfStructureComm.class;
                break;
            }
            case "UAM_UDF_STRUCTURE_FXD":{
                clazz = UdfStructureFxd.class;
                break;
            }
            case "UAM_USER_LOGIN_AUDIT":{
                clazz = UserLoginAudit.class;
                break;
            }
            case "UAM_STP_RIGHTS_SRC_MOD":{
                clazz = StpRightsSrcMod.class;
                break;
            }
            case "UAM_STP_RIGHTS_TYPOLOGY":{
                clazz = StpRightsTypology.class;
                break;
            }
            case "UAM_STP_RIGHTS_MATRIX":{
                clazz = StpRightsMatrix.class;
                break;
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(csvJsonPath));
        log.info("CSV File Path : {} --- Json File Path : {}", csvFilePath, csvJsonPath);
        JsonNode valuesNode = rootNode.get("fields");

        // Convert valuesNode to String[] array
        String[] jsonArray = objectMapper.convertValue(valuesNode, String[].class);

        FlatFileItemReader reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(csvFilePath));
        reader.setLinesToSkip(1);
        reader.setEncoding("UTF-8");

        // autoloader log //logsQueue.put
        DataImportJob existingImportJob = dataImportJobRepository.findFirstById(importJobId);
        existingImportJob.setJobStatus(MxJobLogType.RUNNING);
        logsQueue.put(new DataLoaderProcessLog(existingImportJob.getId(), MxJobLogType.RUNNING,
                MxLogType.INFO, String.format("%12s %1s", " : ", existingImportJob.getTableName() + " - READING & WRITING IN PROGRESS.")));
        logWriter(logsQueue);
        dataImportJobRepository.save(existingImportJob);

        CustomDelimitedLineTokenizer lineTokenizer = new CustomDelimitedLineTokenizer();
        lineTokenizer.setDelimiter("~");
        lineTokenizer.setQuoteCharacter('"');
        lineTokenizer.setNames(jsonArray);

        DefaultLineMapper<T> lineMapper = new DefaultLineMapper<>();

        lineMapper.setLineTokenizer(lineTokenizer);
        BeanWrapperFieldSetMapper<T> wrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        wrapperFieldSetMapper.setTargetType((Class<? extends T>) clazz);
        lineMapper.setFieldSetMapper(wrapperFieldSetMapper);
        reader.setLineMapper(lineMapper);
        log.info("READER :{}",reader);
        return reader;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }
}