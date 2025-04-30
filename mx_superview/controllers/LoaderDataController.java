package com.finsurge.tmr_portal.mx_superview.controllers;

import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.services.AccessControlService;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.configs.BatchUpload;

import com.finsurge.tmr_portal.mx_superview.configs.elastic_data.ElasticSearchItemWriter;
import com.finsurge.tmr_portal.mx_superview.configs.LoaderConfig;

import com.finsurge.tmr_portal.mx_superview.configs.elastic_data.CsvJobConfiguration;
import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewJob;
import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewLog;
import com.finsurge.tmr_portal.mx_superview.models.*;
import com.finsurge.tmr_portal.mx_superview.repository.*;
import com.finsurge.tmr_portal.mx_superview.service.LoaderConfigurationDataService;
import com.finsurge.tmr_portal.mx_superview.service.LoaderDataService;
import com.finsurge.tmr_portal.mx_superview.service.UamSummaryReportJobService;
import com.finsurge.tmr_portal.mx_superview.util.ElasticSearchUtils;
import com.finsurge.tmr_portal.util.CommonUtils;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/loader/config")
@CrossOrigin("*")
public class LoaderDataController {

    private static final Logger log = LoggerFactory.getLogger(LoaderDataController.class);

    @Autowired
    private MxSuperViewJobRepository mxSuperViewJobRepository;

    @Autowired
    private ElasticSearchUtils elasticSearchUtils;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MxSuperViewLogRepository mxSuperViewLogRepository;

    private AccessControlService accessControlService;

    private LoaderConfigurationDataService configurationDataService;
    private JobLauncher jobLauncher;
    private BatchUpload batchUpload;
    private final MxUserListRepository mxUserListRepo;
    private final MxGroupListRepository mxGroupRepo;
    private final MxUserGroupAccessRepository mxUserGroupAccessRepo;
    private final MxPortfolioLabelRepository mxPortfolioLabelRepository;
    private final MxPortfolioRightsRepository mxPortfolioRightsRepository;
    private final ElasticSearchItemWriter elasticSearchItemWriter;

    private final Environment environment;
    private final LoaderDataService loaderDataService;
    private final DataImportJobRepository dataImportJobRepository;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AuditUtils auditUtils;
    private final CommonUtils utils;
    public static LinkedBlockingQueue<DataLoaderProcessLog> logsQueue;

    private final String sourcesPath;
    private final String configsPath;
    private final UamSummaryReportJobService reportJobService;
    @Autowired
    CsvJobConfiguration csvJobConfiguration;


    public LoaderDataController(Environment environment, MxUserListRepository mxUserListRepo, MxGroupListRepository mxGroupRepo, MxUserGroupAccessRepository mxUserGroupAccessRepo,
                                LoaderConfigurationDataService configurationDataService, JobLauncher jobLauncher, BatchUpload batchUpload, MxPortfolioLabelRepository mxPortfolioLabelRepository,
                                MxPortfolioRightsRepository mxPortfolioRightsRepository, LoaderDataService loaderDataService, DataImportJobRepository dataImportJobRepository,
                                AuditUtils auditUtils, CommonUtils utils, UamSummaryReportJobService reportJobService, ElasticSearchItemWriter elasticSearchItemWriter, AccessControlService accessControlService) {
        this.environment = environment;
        this.mxUserListRepo = mxUserListRepo;
        this.mxGroupRepo = mxGroupRepo;
        this.mxUserGroupAccessRepo = mxUserGroupAccessRepo;
        this.configurationDataService = configurationDataService;
        this.jobLauncher = jobLauncher;
        this.batchUpload = batchUpload;
        this.mxPortfolioLabelRepository = mxPortfolioLabelRepository;
        this.mxPortfolioRightsRepository = mxPortfolioRightsRepository;
        this.reportJobService = reportJobService;
        this.elasticSearchItemWriter = elasticSearchItemWriter;
        this.loaderDataService = loaderDataService;
        this.dataImportJobRepository = dataImportJobRepository;
        this.auditUtils = auditUtils;
        this.utils = utils;
        this.accessControlService = accessControlService;
        sourcesPath = this.environment.getProperty("uam.paths.uploads.source-files");
        configsPath = this.environment.getProperty("uam.paths.uploads.config-files");
    }


    @PostMapping("/run")
    public ResponseEntity<?> startUpload(@RequestBody FileUploadConfig fileUploadConfig, Authentication authentication) throws Exception {
        logsQueue = new LinkedBlockingQueue<>();
        // create log file path
        LoaderConfig.createLogPath();
        // delete old log file
        LoaderConfig.deleteLogFile();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy:HH:mm:ss");
        String jobName = fileUploadConfig.getFileName() + "_" + dateFormatter.format(LocalDateTime.now());

        File file = new File(sourcesPath + File.separator + fileUploadConfig.getFileName());
        if (!file.exists()) {
            return new ResponseEntity<>("Import file not found at " + file.getAbsolutePath(), HttpStatus.NOT_FOUND);
        }
        JSONParser parser = new JSONParser();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        long lines = 0L;
        while (reader.readLine() != null) lines++;
        reader.close();
        String reportDate = null;

//        if (fileUploadConfig.getFileName().toLowerCase().contains("stp")) {
//                reportDate = fileUploadConfig.getFileName().split("_")[2].split("\\.")[0];
//        } else {
        reportDate = fileUploadConfig.getFileName().split("_")[1].split("\\.")[0];
//        }

        LocalDate requestDate = LocalDate.parse(reportDate, dateTimeFormatter);
        File[] fileArr = new File(configsPath).listFiles();
        String tableName = null;
        if (fileArr != null) {
            for (File jsonFile : fileArr) {
                if (jsonFile.getName().equalsIgnoreCase(fileUploadConfig.getJsonName())) {
                    JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(jsonFile.getAbsolutePath()));
                    tableName = (String) jsonObject.get("tableName");
                }
            }
        }
        DataImportJob importJob = new DataImportJob();
        importJob.setDataType("");
        importJob.setImportTimestamp(LocalDateTime.now());
        importJob.setRecordsInSource(lines);
        importJob.setRecordsImported(0L);
        importJob.setFileName(fileUploadConfig.getFileName());
        importJob.setTableName(tableName);
        importJob.setPurged(false);
        if (fileUploadConfig.getFileName().split("_")[0].endsWith("adc")) {
            importJob.setReportType(ReportType.INTRADAY);
        } else {
            importJob.setReportType(ReportType.EOD);
        }
        if (jobName.toLowerCase().contains("stp")) {
            importJob.setReportDate(requestDate.minusDays(1));
        } else {
            importJob.setReportDate(requestDate);
        }
        importJob.setFileSize(getReadableFileSizeKb(file.length()));
        importJob.setFileLastModifed(file.lastModified());
        importJob.setAutoLoad(false);
        importJob = dataImportJobRepository.save(importJob);

        //start batch process
        JobExecution execution;
        log.info("FILE NAME :{}", fileUploadConfig.getFileName());

        List<String> elasticFiles = Arrays.asList("closingentity", "counterparty", "counterpartycreation", "dormantcounterparty",  "userpolicy","grouppfoliorights","chinesewalltmpl","groupnavrights",
                "groupcombpfolio","operationrights","osprightsmatrix","enterpriserisk","financeaccctrlrights","consistencytmp","cwtconfigmgtrights","supchgaudit","displaysi","udfstructureird","udfstructurecomm","udfstructurefxd","userloginaudit","stprightssrcmod","stprightstypology","stprightsmatrix");
        //List<String> elasticFiles = Arrays.asList("closingentity", "counterparty", "counterpartycreation", "dormantcounterparty",  "userpolicy","grouppfoliorights","groupnavrights");
        List<String> mysqlAndElastic = List.of("usergrouprights","mxuserlist", "mxgrouplist", "userlicense");

        String newFileName = fileUploadConfig.getFileName().split("eod")[0];
        batchUpload.setIdAndReportDate(importJob, reportDate, file.getName());
        if (elasticFiles.contains(newFileName)) {
            log.info("--LOADING IN  ELASTIC--");
            // csv to elsatic
            logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO,
                    String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
            LoaderConfig.logWriter(logsQueue);
            elasticSearchItemWriter.setReportDateAndJobId(reportDate, importJob.getId());
            Job elasticJob = csvJobConfiguration.csvToElasticSearchJob(jobName);
            execution = jobLauncher.run(elasticJob, new JobParametersBuilder()
                    .addString("filePath", file.getAbsolutePath())
                    .addString("csvFilePath", file.getAbsolutePath())
                    .addString("entityName", tableName)
                    .addString("csvJsonPath", configsPath + File.separator + fileUploadConfig.getJsonName())
                    .addLong("importJobId", importJob.getId())
                    .addString("dataLoading", "elastic")

                    .toJobParameters());
            // for verification
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info("Elastic ElasticSearchUtils: MANUAL : Job completed successfully at " + LocalDateTime.now());
            }
        } else if (mysqlAndElastic.contains(newFileName)) {
            log.info("--LOADING IN MYSQL AND ELASTIC--");
            //csv - mysql
            logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO, String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
            LoaderConfig.logWriter(logsQueue);
            Job job = batchUpload.createJob(jobName);
            execution = jobLauncher.run(job, new JobParametersBuilder()
                    .addString("filePath", file.getAbsolutePath())
                    .addString("dataLoading", "mysql")
                    .addString("jsonPath", configsPath + File.separator + fileUploadConfig.getJsonName())
                    .addLong("importJobId", importJob.getId())
                    .addString("dataLoading", "elastic")
                    .toJobParameters());

           /* while (!(execution.getStatus() == BatchStatus.COMPLETED)){
                Thread.sleep(environment.getProperty("uam.manualLoading.sleepTime",Integer.class));
            }*/

            // for verification
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info("ElasticSearchUtils: MANUAL : Job completed successfully at " + LocalDateTime.now());
            }
            // csv to elsatic
            logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO,
                    String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
            LoaderConfig.logWriter(logsQueue);
            elasticSearchItemWriter.setReportDateAndJobId(reportDate, importJob.getId());
            Job elasticJob = csvJobConfiguration.csvToElasticSearchJob(jobName);
            execution = jobLauncher.run(elasticJob, new JobParametersBuilder()
                    .addString("filePath", file.getAbsolutePath())
                    .addString("csvFilePath", file.getAbsolutePath())
                    .addString("dataLoading", "elastic")
                    .addString("entityName", tableName)
                    .addString("csvJsonPath", configsPath + File.separator + fileUploadConfig.getJsonName())
                    .addLong("importJobId", importJob.getId())
                    .toJobParameters());
            // for verification

            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info("Elastic ElasticSearchUtils: MANUAL : Job completed successfully at " + LocalDateTime.now());
            }
        } else {
             log.info("--LOADING IN MYSQL--");
            // csv to mysql
            logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO, String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
            LoaderConfig.logWriter(logsQueue);
            Job job = batchUpload.createJob(jobName);
            execution = jobLauncher.run(job, new JobParametersBuilder()
                    .addString("filePath", file.getAbsolutePath())
                    .addString("jsonPath", configsPath + File.separator + fileUploadConfig.getJsonName())
                    .addLong("importJobId", importJob.getId())
                    .addString("dataLoading", "mysql")
                    .toJobParameters());
            // for verification
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info("ElasticSearchUtils: MANUAL : Job completed successfully at " + LocalDateTime.now());
            }
        }

        MxSuperViewLog mxSuperViewLog = new MxSuperViewLog();
        mxSuperViewLog.setRootJobId(execution.getJobParameters().getLong("importJobId"));
        mxSuperViewLog.setCreated(LocalDateTime.now());
        mxSuperViewLog.setLogType(MxLogType.INFO);
        mxSuperViewLog.setLogMessage("Job is Started ");
        mxSuperViewLogRepository.save(mxSuperViewLog);

        //batchUpload.setIdAndReportDate(importJob, reportDate, file.getName());

        MxSuperViewJob mxSuperViewJob = new MxSuperViewJob();
        mxSuperViewJob.setJobId(execution.getJobParameters().getLong("importJobId"));
        mxSuperViewJob.setCreated(LocalDateTime.now());
        mxSuperViewJob.setLogType(MxJobLogType.STARTED);
        mxSuperViewJob.setLogMessage(fileUploadConfig.getFileName() + " Job is Started");

        mxSuperViewJobRepository.save(mxSuperViewJob);
        // save audit for this action
        auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_LOAD_JOBS, AuditAction.UPLOAD, importJob.getId(),
                fileUploadConfig.getFileName(), null, null, authentication, null, null, null, null, null);

        return new ResponseEntity<>(importJob, HttpStatus.OK);
    }

    @PostMapping("/api/create/json")
    public ResponseEntity<?> createUserJson() throws IOException {
        LocalDate todayDate = LocalDate.parse(dateTimeFormatter.format(LocalDate.now()), dateTimeFormatter);
        loaderDataService.createJson(todayDate, dateTimeFormatter);
        return new ResponseEntity<>("JSON CREATED SUCCESSFULLY", HttpStatus.OK);
    }

    @DeleteMapping("/api/uam/delete-by-jobId/{jobId}")
    public ResponseEntity<?> deleteByJobId(@PathVariable Long jobId, Authentication authentication) throws
            IOException {
        DataImportJob dataImportJob = dataImportJobRepository.findFirstById(jobId);
        if (dataImportJob != null) {
            String tableName = dataImportJob.getTableName();
            LocalDate reportDate = dataImportJob.getReportDate();

            List<String> elasticTables = Arrays.asList("UAM_MX_CLOSING_ENTITY", "UAM_MX_COUNTER_PARTY", "UAM_MX_COUNTERPARTY_CREATION", "UAM_MX_DORMANT_COUNTERPARTY",  "UAM_MX_USER_POLICY","UAM_MX_GROUP_PORTFOLIO_RIGHTS","UAM_MX_CHINESE_WALL_TMPL","UAM_MX_GROUP_NAV_RIGHTS",
                    "UAM_MX_GROUP_COMBINED_PORTFOLIO", "UAM_MX_OPERATION_RIGHTS","UAM_MX_OSP_RIGHTS_MATRIX","UAM_MX_ENTERPRISE_RISK","UAM_MX_FINANCE_ACCTRL_RIGHTS","UAM_MX_CONSISTENCY_TMPL","UAM_MX_CWT_CONFIG_MGT_RIGHT","UAM_MX_SUPCHG_AUDIT",
                    "UAM_MX_COUNTER_PARTY_DISPLAY","UAM_UDF_STRUCTURE_IRD","UAM_UDF_STRUCTURE_COMM","UAM_UDF_STRUCTURE_FXD","UAM_USER_LOGIN_AUDIT", "UAM_STP_RIGHTS_SRC_MOD", "UAM_STP_RIGHTS_TYPOLOGY", "UAM_STP_RIGHTS_MATRIX");
            //List<String> elasticTables = Arrays.asList("UAM_MX_CLOSING_ENTITY", "UAM_MX_COUNTER_PARTY", "UAM_MX_COUNTERPARTY_CREATION", "UAM_MX_DORMANT_COUNTERPARTY",  "UAM_MX_USER_POLICY","UAM_MX_GROUP_PORTFOLIO_RIGHTS","UAM_MX_GROUP_NAV_RIGHTS");

            List<String> mysqlAndElastic = Arrays.asList("UAM_USER_GROUP_ACCESS_RIGHT","UAM_MX_USER_LIST",
                    "UAM_MX_GROUP_LIST", "UAM_MX_USER_LICENSE");

            if (elasticTables.contains(tableName)) {
                log.info("-- Deleting Data in ES--");
                elasticSearchUtils.deleteData(tableName, reportDate,jobId);
            } else if (mysqlAndElastic.contains(tableName)) {
                log.info("-- Deleting Data in ES AND  MYSQL--");
                elasticSearchUtils.deleteData(tableName, reportDate,jobId);
                String deleteQuery = "DELETE from " + tableName + "  where JOB_ID =" + dataImportJob.getId();
                jdbcTemplate.update(deleteQuery);
            } else {
                try {
                    log.info("-- Deleting Data in MYSQL--");
                    String deleteQuery = "DELETE from " + tableName + "  where JOB_ID =" + dataImportJob.getId();
                    jdbcTemplate.update(deleteQuery);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log.info("Delete unsuccessful for jobId:{}", jobId);
                    return new ResponseEntity<>("Delete unsuccessful for jobId " + jobId, HttpStatus.BAD_REQUEST);
                }
            }

            dataImportJob.setPurged(true);
            dataImportJobRepository.save(dataImportJob);
            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_LOAD_JOBS, AuditAction.DELETE, jobId,
                    null, null, null, authentication, null, null, null, null, null);
            return new ResponseEntity<>("Deleted  Successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Job with ID " + jobId + " not found.", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/api/get/data-loader/jobs")
    public ResponseEntity<?> getDataLoaderJobs(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestParam(defaultValue = "id") String sortBy,
                                               @RequestParam(defaultValue = "asc") String sortingOrder,
                                               @RequestBody(required = false) MxJobFilter mxJobFilter,
                                               Authentication authentication) {
        if (!accessControlService.userIsAdmin(authentication)) {
            return new ResponseEntity("User does not have access to this module.", HttpStatus.UNAUTHORIZED);
        }

        Page<DataImportJob> mxSuperViewJobPage = loaderDataService.getSuperViewJob(page, pageSize, mxJobFilter, sortingOrder, sortBy);
        Document document = new Document();
        document.put("totalPages", mxSuperViewJobPage.getTotalPages());
        document.put("records", mxSuperViewJobPage.getTotalElements());
        document.put("content", mxSuperViewJobPage.getContent());
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    @PostMapping("/api/get/data-loader/logs")
    public ResponseEntity<?> getDataLoaderLogs(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int pageSize,
                                               @RequestBody(required = false) MxLogFilter mxLogFilter) {
        Page<MxSuperViewLog> mxSuperViewLogPage = loaderDataService.getSuperViewLog(page, pageSize, mxLogFilter.getRootJobId(), mxLogFilter.getLogType());
        Document document = new Document();
        document.put("totalPages", mxSuperViewLogPage.getTotalPages());
        document.put("records", mxSuperViewLogPage.getTotalElements());
        document.put("content", mxSuperViewLogPage.getContent());
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

    private long getReadableFileSizeKb(double length) {
        double kilobytes = (length / (double) 1024);
        return (long) Math.ceil(kilobytes);
    }

    @PostMapping("/api/get/data-loader/log-file")
    public ResponseEntity<?> getDataLoaderLogFile() throws IOException {
        File logFile = new File(Objects.requireNonNull(environment.getProperty("uam.autoload.log.filepath")) + "/uam_autoload_" + LocalDate.now().format(dateTimeFormatter) + ".log");
        if (logFile.exists()) {
            List<String> lines = Files.readAllLines(Paths.get(String.valueOf(logFile)));
//            StringBuilder fileContent = new StringBuilder();
//            for (String line : lines) {
//                fileContent.append(line).append("\n");
//            }
            return new ResponseEntity<>(lines, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("File Not Exists.", HttpStatus.OK);
        }
    }
}
