package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.configs.elastic_data.CsvJobConfiguration;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.CounterPartyDocument;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.DormantCounterparty;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.*;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewLog;
import com.finsurge.tmr_portal.mx_superview.models.DataLoaderProcessLog;
import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.MxLogType;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxSuperViewLogRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticSearchUtils;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class Listener extends JobExecutionListenerSupport {

    @Autowired
    private ElasticUtils elasticUtils;

    @Autowired
    private GroupPortfolioService groupPortFolioService;

    @Autowired
    private OperationRightsService operationRightsService;

    @Autowired
    private ChineseWallService chineseWallService;

    @Autowired
    private FinanceRightsService financeRightsService;

    @Autowired
    private MxCounterpartyService counterpartyService;

    @Autowired
    private MxSuperViewLogRepository mxSuperViewLogRepository;

    @Autowired
    private ElasticSearchUtils elasticSearchUtils;


    @Value("${uam.paths.uploads.source-files}")
    private String filePath;

    @Value("${uam.paths.uploads.source.stp-files}")
    private String stpFilePath;

    @Autowired
    private DataImportJobRepository dataImportJobRepository;

    @Autowired
    private UserGroupAccessRightsService userGroupAccessRightsService;

    @Autowired
    private GroupNavigationRightsService groupNavigationRightsService;

    @Autowired
    private GroupListService groupListService;
    @Autowired
    private CounterpartyCreationService counterpartyCreationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

    private final JdbcTemplate jdbcTemplate;

    public LinkedBlockingQueue<DataLoaderProcessLog> logsQueue;

    @Autowired
    public Listener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        //existing code - before elastic implementation
        //logsQueue = BatchUpload.logsQueue;
        MxSuperViewLog mxSuperViewLog = new MxSuperViewLog();

        LOGGER.info("PROCESSING IN LISTENER");
        String fileName = new File(Objects.requireNonNull(jobExecution.getJobParameters().getString("filePath"))).getName();

        String dataLoading = new File(Objects.requireNonNull(jobExecution.getJobParameters().getString("dataLoading"))).getName();

        // initialize logs queue
        List<String> elasticFiles = Arrays.asList("closingentity", "counterparty", "counterpartycreation", "dormantcounterparty", "usergrouprights", "mxuserlist",
                "mxgrouplist", "userlicense", "userpolicy", "grouppfoliorights", "chinesewalltmpl", "groupnavrights", "groupcombpfolio", "operationrights", "osprightsmatrix",
                "enterpriserisk", "financeaccctrlrights", "consistencytmp", "cwtconfigmgtrights","supchgaudit","displaysi","udfstructureird","udfstructurecomm","udfstructurefxd","userloginaudit","stprightssrcmod","stprightstypology","stprightsmatrix");
        logsQueue = elasticFiles.contains(fileName.split("eod")[0]) ? CsvJobConfiguration.logsQueue : BatchUpload.logsQueue;
        // List<String> elasticFiles = Arrays.asList("closingentity","counterparty","counterpartycreation","dormantcounterparty","usergrouprights","mxuserlist","mxgrouplist","userlicense","userpolicy","grouppfoliorights","groupnavrights");
        // logsQueue= elasticFiles.contains(fileName.split("eod")[0])?CsvJobConfiguration.logsQueue:BatchUpload.logsQueue;

//        logsQueue = fileName.contains("closingEntity") || fileName.toLowerCase().contains("counterparty") || fileName.toLowerCase().contains("usergrouprightseod")
//                ? CsvJobConfiguration.logsQueue : BatchUpload.logsQueue;

        jobExecution.getStatus().toString();
        Long jobId = jobExecution.getJobParameters().getLong("importJobId");
        DataImportJob dataImportJob = dataImportJobRepository.findFirstById(jobId);

        LOGGER.info("WAITING FOR PROCESS TO COMPLETE");

        List<String> elasticTables = Arrays.asList("UAM_MX_DORMANT_COUNTERPARTY", "UAM_MX_COUNTER_PARTY", "UAM_MX_COUNTERPARTY_CREATION", "UAM_MX_CLOSING_ENTITY",
                "UAM_MX_USER_LIST", "UAM_USER_GROUP_ACCESS_RIGHT", "UAM_MX_USER_LICENSE", "UAM_MX_USER_POLICY", "UAM_MX_GROUP_PORTFOLIO_RIGHTS", "UAM_MX_CHINESE_WALL_TMPL", "UAM_MX_GROUP_NAV_RIGHTS",
                "UAM_MX_GROUP_COMBINED_PORTFOLIO", "UAM_MX_OPERATION_RIGHTS", "UAM_MX_OSP_RIGHTS_MATRIX", "UAM_MX_ENTERPRISE_RISK", "UAM_MX_FINANCE_ACCTRL_RIGHTS", "UAM_MX_CONSISTENCY_TMPL", "UAM_MX_CWT_CONFIG_MGT_RIGHT",
                "UAM_MX_SUPCHG_AUDIT","UAM_MX_COUNTER_PARTY_DISPLAY","UAM_UDF_STRUCTURE_IRD","UAM_UDF_STRUCTURE_COMM","UAM_UDF_STRUCTURE_FXD","UAM_USER_LOGIN_AUDIT", "UAM_STP_RIGHTS_SRC_MOD", "UAM_STP_RIGHTS_TYPOLOGY", "UAM_STP_RIGHTS_MATRIX");
        //"UAM_MX_USER_LIST", "UAM_USER_GROUP_ACCESS_RIGHT", "UAM_MX_USER_LICENSE", "UAM_MX_USER_POLICY", "UAM_MX_GROUP_PORTFOLIO_RIGHTS","UAM_MX_GROUP_NAV_RIGHTS");


        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {

            dataImportJob.setEndTime(LocalDateTime.now());
            dataImportJob.setJobStatus(MxJobLogType.COMPLETED);

            // For Elastic Data - Reports

//            List<String> elasticTables = Arrays.asList("UAM_MX_DORMANT_COUNTERPARTY", "UAM_MX_COUNTER_PARTY", "UAM_MX_COUNTERPARTY_CREATION","UAM_MX_CLOSING_ENTITY",
//                    "UAM_MX_USER_LIST","UAM_USER_GROUP_ACCESS_RIGHT","UAM_MX_USER_LICENSE","UAM_MX_USER_POLICY","UAM_MX_GROUP_PORTFOLIO_RIGHTS");

            String tableName = dataImportJob.getTableName();
            if (elasticTables.contains(tableName)) {
                long totalCount = elasticSearchUtils.getCount(dataImportJob.getId(), tableName);
                dataImportJob.setRecordsImported(totalCount);
            } else {
                Map<String, Object> insertedRows = jdbcTemplate.queryForMap("SELECT COUNT(*) as ROW_COUNT FROM " + dataImportJob.getTableName() + " WHERE JOB_ID = " + dataImportJob.getId());
                dataImportJob.setRecordsImported(Long.parseLong(String.valueOf(insertedRows.get("ROW_COUNT"))));
            }
            dataImportJobRepository.save(dataImportJob);
            mxSuperViewLog.setRootJobId(jobId);
            mxSuperViewLog.setCreated(LocalDateTime.now());
            mxSuperViewLog.setLogType(MxLogType.INFO);
            mxSuperViewLog.setLogMessage("Job Completed Successfully ");
            LoaderConfig.statusMap.put(jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
            mxSuperViewLogRepository.save(mxSuperViewLog);

            File file = new File(jobExecution.getJobParameters().getString("filePath"));

            File processFol = new File(file.getAbsolutePath().replace(file.getName(), "") + File.separator + "processedFiles");
            if (!processFol.exists()) {
                //processFol.mkdir();
                processFol.mkdirs();
                processFol.setWritable(true, false);
                processFol.setReadable(true, false);
                processFol.setExecutable(true, false);
            }


            try {
                // add log in logs queue
                logsQueue.put(new DataLoaderProcessLog(jobId, MxJobLogType.COMPLETED, MxLogType.INFO, String.format("%10s %1s", " : ", dataImportJob.getTableName() + " - LOADING COMPLETED")));
                LoaderConfig.logWriter(logsQueue);

                new File(processFol + File.separator + file.getName().split("\\.")[0]).createNewFile();
                //code added for remove stp file from uam-sources temp folder after data loaded
                File fileToDelete;
                if (file.getName().toLowerCase().contains("stp")) {
                    LOGGER.info("File to be cleared at UAM-SOURCES  :{}", stpFilePath + File.separator + fileName);
                    fileToDelete = new File(stpFilePath + File.separator + fileName);
                } else {
                    LOGGER.info("File to be cleared at uam-SOURCES  :{}", filePath + File.separator + fileName);
                    fileToDelete = new File(filePath + File.separator + fileName);
                }
                if (fileToDelete.exists()) {
                    boolean isDeleted = fileToDelete.delete();
                    LOGGER.info("File deleted from source path :{}", isDeleted);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                try {
                    logsQueue.put(new DataLoaderProcessLog(dataImportJob.getId(), MxJobLogType.COMPLETED, MxLogType.ERROR, String.format("%10s %1s", " : ", dataImportJob.getTableName() + " - " + e.getMessage())));
                    LoaderConfig.logWriter(logsQueue);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                LOGGER.info("Exception while moving the file to ProcessedFolder :{} for filename:{}", e.getMessage(), file.getName());
            }

            log.info("Data Loading  COMPLETED  on {} data base .", dataLoading.toUpperCase());
            LOGGER.info("!!! JOB FINISHED! Time to verify the results");

            if (dataLoading.equalsIgnoreCase("elastic")) {
                updateReportStatus(tableName, fileName);
            }


        } else {
            dataImportJob.setJobStatus(MxJobLogType.FAILED);
            dataImportJob.setEndTime(LocalDateTime.now());
            try {
                logsQueue.put(new DataLoaderProcessLog(dataImportJob.getId(), MxJobLogType.FAILED, MxLogType.ERROR, String.format("%12s %1s", " : ", dataImportJob.getTableName() + " - " + " LOADING FAILED : " + jobExecution.getAllFailureExceptions())));
                LoaderConfig.logWriter(logsQueue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String tableName = dataImportJob.getTableName();
            if (elasticTables.contains(tableName)) {
                long totalCount = elasticSearchUtils.getCount(dataImportJob.getId(), tableName);
                dataImportJob.setRecordsImported(totalCount);
            } else {
                Map<String, Object> insertedRows = jdbcTemplate.queryForMap("SELECT COUNT(*) as ROW_COUNT FROM " + dataImportJob.getTableName() + " WHERE JOB_ID = " + dataImportJob.getId());
                dataImportJob.setRecordsImported(Long.parseLong(String.valueOf(insertedRows.get("ROW_COUNT"))));
            }

            // Map<String, Object> insertedRows = jdbcTemplate.queryForMap("SELECT COUNT(*) as ROW_COUNT FROM " + dataImportJob.getTableName() + " WHERE JOB_ID = " + dataImportJob.getId());
            //dataImportJob.setRecordsImported(Long.parseLong(String.valueOf(insertedRows.get("ROW_COUNT"))));
            dataImportJobRepository.save(dataImportJob);
            LoaderConfig.statusMap.put(jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());

            mxSuperViewLog.setCreated(LocalDateTime.now());
            mxSuperViewLog.setRootJobId(jobId);
            mxSuperViewLog.setLogType(MxLogType.ERROR);
            mxSuperViewLog.setLogMessage(fileName + "\n LOADING FAILED : Caused by : \n " + jobExecution.getAllFailureExceptions());
            mxSuperViewLogRepository.save(mxSuperViewLog);
            LOGGER.info("!!! JOB FAILED! Please check application log file for further details...");
        }
    }




    @SneakyThrows
    private void updateReportStatus(String tableName, String fileName) {

        String reportDate = getReportDate(fileName);
        switch (tableName) {
            case "UAM_USER_GROUP_ACCESS_RIGHT":
                log.info("Starting to Update the  DepartmentField from GroupLabel Department(Admin) in the UserList Report :-");
                userGroupAccessRightsService.updateUserGroupDepartmentField(reportDate);
                log.info("Successfully updated - Department field:-");
                break;
            case "UAM_MX_USER_LIST":
                log.info("Combining UserList with UserGroupAccessRights");
                userGroupAccessRightsService.combineUserListWithUserRights(reportDate);
                log.info("COMPLETED :-");
                break;
            case "UAM_MX_USER_LICENSE":
                log.info("Combining UserLicense with UserGroupAccessRights");
                userGroupAccessRightsService.combineLicenseWithUserRights(reportDate);
                log.info("COMPLETED :-");
                break;
            case "UAM_MX_GROUP_LIST":
                log.info("Starting  to update the  Status Field in the {} Report File Name {} :- ", tableName, fileName);
                groupListService.updateGroupLabelStatus(reportDate);
                log.info("Successfully updated - Status field :-");
                break;
            case "UAM_MX_COUNTER_PARTY":
                log.info("Starting to Update the  All duplicate Data Status in the COUNTERPARTY Report :-");
                counterpartyService.updateAllDuplicate(reportDate);
                log.info("Duplicate field has Updated :-");
                break;
            case "UAM_MX_CONSISTENCY_TMPL":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", "UAM_MX_CONSISTENCY_TMPL");
                    String getFieldName = fields.get(0);
                    String updateFieldName = fields.get(1);
                    log.info("Starting to update the GroupLabel Field in the {} Report :- ", tableName);
                    elasticUtils.updateGroupLabels(reportDate, ConsistencyTmpl.INDEX_NAME, updateFieldName, getFieldName);
                    log.info("The GroupLabel field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "UAM_MX_GROUP_NAV_RIGHTS":
                log.info("Starting  to update the  Status Field in the {} Report :- ", tableName);
                groupNavigationRightsService.updateStatusField(getReportDate(fileName));

                break;
            case "UAM_MX_DORMANT_COUNTERPARTY":
                try {
                    log.info("Starting to Update the  dormantCounterpartyField  in the COUNTERPARTY Report :-");
                    Boolean exist = counterpartyService.doesCounterpartyExist(reportDate);
                    if (exist) {
                        counterpartyService.updateNewDormantCounterpartyField(reportDate);
                        log.info("dormantCounterpartyField  was successfully updated :-");
                    } else {
                        log.info("Counterparty NOT FOUND :-");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;

            case "UAM_MX_COUNTERPARTY_CREATION":
                log.info("Starting to Update the  creationDate in the counterparty Report :-");
                counterpartyCreationService.updateCreationDate(reportDate);
                log.info("Counterparty Creation was successfully updated :-");
                break;
            case "UAM_MX_GROUP_PORTFOLIO_RIGHTS":
                try {
                    log.info("Starting to update the Status Field in the {} Report :-", tableName);
                    groupPortFolioService.updateStatus(reportDate);
                    log.info("The status field was successfully updated:-");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "UAM_MX_CHINESE_WALL_TMPL":
                try {
                    log.info("Starting to update the  GroupLabel Field in the {} Report :- ", tableName);
                    chineseWallService.updateGroupLabels(reportDate);
                    log.info("The GroupLabel field was successfully updated:-");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "UAM_MX_OSP_RIGHTS_MATRIX":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", tableName);
                    String fetchUserGroupFieldName = fields.get(0);
                    String reportFieldName = fields.get(1);
                    log.info("Starting to update the GroupLabel Field in the {} Report :- ", tableName);
                    elasticUtils.updateGroupLabels(reportDate, OspRightsMatrix.INDEX_NAME, reportFieldName, fetchUserGroupFieldName);
                    log.info("The GroupLabel field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;

            case "UAM_MX_OPERATION_RIGHTS":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", tableName);
                    String reportFieldName = fields.get(0);
                    log.info("Starting to update the GroupLabel Field in the {} Report :- ", tableName);
                    operationRightsService.updateGroupLabels(reportDate, OperationRights.INDEX_NAME, reportFieldName);
                    log.info("The GroupLabel field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "UAM_MX_GROUP_COMBINED_PORTFOLIO":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", tableName);
                    String reportFieldName = fields.get(0); // Access the first element
                    log.info("Starting to update the Status Field in the {} Report :- ", tableName);
                    elasticUtils.updateStatusField(reportDate, GroupComboPortfolio.INDEX_NAME, reportFieldName);
                    log.info("The status field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "UAM_MX_FINANCE_ACCTRL_RIGHTS":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", tableName);
                    String reportFieldName = fields.get(0);
                    log.info("Starting to update the GroupLabel Field in the {} Report :- ", tableName);
                    financeRightsService.updateGroupLabels(reportDate, FinanceRights.INDEX_NAME, reportFieldName);
                    log.info("The GroupLabel field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;

            case "UAM_MX_ENTERPRISE_RISK":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", tableName);
                    String reportFieldName = fields.get(0); // Access the first element
                    log.info("Starting to update the Status Field in the {} Report :- ", tableName);
                    elasticUtils.updateStatusField(reportDate, EnterpriseRisk.INDEX_NAME, reportFieldName);
                    log.info("The status field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;

            case "UAM_MX_CWT_CONFIG_MGT_RIGHT":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", tableName);
                    String reportFieldName = fields.get(0); // Access the first element
                    log.info("Starting to update the Status Field in the {} Report :- ", tableName);
                    elasticUtils.updateStatusField(reportDate, ConfigMgtRight.INDEX_NAME, reportFieldName);
                    log.info("The status field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;

            case "UAM_STP_RIGHTS_MATRIX":
                try {
                    List<String> fields = elasticUtils.getDataFromJsonFile("elastic/mappings/fieldMappings.json", "UAM_STP_RIGHTS_MATRIX");
                    String fetchUserGroupFieldName = fields.get(0); // Access the first element
                    String reportFieldName = fields.get(1); // Access the second element
                    log.info("Starting to update the GroupLabel Field in the {} Report :- ", tableName);
                    elasticUtils.updateGroupLabels(reportDate, StpRightsMatrix.INDEX_NAME, reportFieldName, fetchUserGroupFieldName);
                    log.info("The GroupLabel field was successfully updated.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }
    public String getReportDate(String fileName) {
        String name = fileName.split("eod")[1];
        String year = name.substring(1, 5);
        String month = name.substring(5, 7);
        String day = name.substring(7);
        String formattedDate = year + "-" + month + "-" + day;
        String[] date = formattedDate.split("_");
        return date[0].split("\\.")[0];
    }
}
