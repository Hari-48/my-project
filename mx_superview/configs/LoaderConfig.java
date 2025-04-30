package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.util.AuditUtils;

import com.finsurge.tmr_portal.mx_superview.configs.elastic_data.CsvJobConfiguration;
import com.finsurge.tmr_portal.mx_superview.configs.elastic_data.ElasticSearchItemWriter;
import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.entity.MxPreference;
import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewLog;
import com.finsurge.tmr_portal.mx_superview.models.DataLoaderProcessLog;
import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.MxLogType;
import com.finsurge.tmr_portal.mx_superview.models.ReportType;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxPreferenceRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxSuperViewJobRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxSuperViewLogRepository;
import com.finsurge.tmr_portal.mx_superview.util.ReportsDataLoadingComparator;
import com.finsurge.tmr_portal.mx_superview.util.ElasticSearchUtils;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


@Configuration
@EnableScheduling
public class LoaderConfig {

    private static final Logger log = LoggerFactory.getLogger(LoaderConfig.class);
    private JobLauncher jobLauncher;

    private BatchUpload batchUpload;

    private AuditUtils auditUtils;
    @Autowired
    private ElasticSearchUtils elasticSearchUtils;


    private static Environment environment;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UtilsConfigs utilsConfigs;
    public static List<String> listOfNames = new ArrayList();
    private final DataImportJobRepository dataImportJobRepository;
    public static HashMap<String, BatchStatus> statusMap = new HashMap<String, BatchStatus>();
    public static LinkedBlockingQueue<DataLoaderProcessLog> logsQueue;
    public int fileCount;
    public boolean isStpLoaded;
    private final MxSuperViewLogRepository mxSuperViewLogRepository;
    private final MxSuperViewJobRepository mxSuperViewJobRepository;
    private final MxPreferenceRepository mxPreferenceRepository;
    private ElasticSearchItemWriter elasticSearchItemWriter;
    private final CsvJobConfiguration csvJobConfiguration;

    public LoaderConfig(DataImportJobRepository dataImportJobRepository, JobLauncher jobLauncher, AuditUtils auditUtils, BatchUpload batchUpload, Environment environment, UtilsConfigs utilsConfigs, MxSuperViewLogRepository mxSuperViewLogRepository, MxSuperViewJobRepository mxSuperViewJobRepository, MxPreferenceRepository mxPreferenceRepository, ElasticSearchItemWriter elasticSearchItemWriter, CsvJobConfiguration csvJobConfiguration) {
        this.jobLauncher = jobLauncher;
        this.batchUpload = batchUpload;
        this.auditUtils = auditUtils;
        this.environment = environment;
        this.dataImportJobRepository = dataImportJobRepository;
        this.utilsConfigs = utilsConfigs;
        this.mxSuperViewLogRepository = mxSuperViewLogRepository;
        this.mxSuperViewJobRepository = mxSuperViewJobRepository;
        this.mxPreferenceRepository = mxPreferenceRepository;
        this.elasticSearchItemWriter = elasticSearchItemWriter;
        this.csvJobConfiguration = csvJobConfiguration;
    }

    @Scheduled(cron = "${uam.cron.eod.expressions}")
    public void autoLoad() throws Exception {
        logsQueue = new LinkedBlockingQueue<>();
        // create log file path
        createLogPath();
        // delete old log file
        deleteLogFile();

        String srcPath = environment.getProperty("uam.source.eod.path");
        String destPath = environment.getProperty("uam.paths.uploads.source-files");
        String jsonPath = environment.getProperty("uam.paths.uploads.config-files");
        String tempPath = environment.getProperty("uam.paths.uploads.source-files.stp");
        long threadIdleTime = Long.parseLong(Objects.requireNonNull(environment.getProperty("uam.autoLoad.thread.IdleTime")));
        //boolean stpLoadedAllowed = Boolean.parseBoolean(environment.getProperty("uam.autoLoad.stp.enable"));
        boolean stpLoadedAllowed = true; // Here Stp allowed is true
        // Retrieve stp_loading preference value from database
        MxPreference stpLoadingValue = mxPreferenceRepository.findByPropertyName("STP_DATA_LOADING");// getting stp allowed from the DB
        if (stpLoadingValue == null) {
            stpLoadedAllowed = true;
        } else {
            if (stpLoadingValue.getPropertyValue() == null || stpLoadingValue.getPropertyValue().isEmpty()) {
                stpLoadedAllowed = true;
            } else {
                // Otherwise, use the actual value.
                stpLoadedAllowed = Boolean.parseBoolean(stpLoadingValue.getPropertyValue());
            }
        }
        File resource = new ClassPathResource("UAM/uam_config.json").getFile();
        log.info("autoload startaed:{}", LocalDateTime.now());
        long stpLoadCount = 0;
        long stpLoaded = 0;
        JSONParser parser = new JSONParser();
        isStpLoaded = false;
        if (resource.exists()) {
            String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
            JSONObject json = (JSONObject) parser.parse(defaultConfig);
            stpLoadCount = (long) json.get("data_load_stp_max_file_count");
        }
        srcPath = srcPath.replace("*", dateTimeFormatter.format(LocalDate.now().minusDays(1)));

        File srcFolder = new File(srcPath);
        File[] files = srcFolder.listFiles();

        if (files != null) {

            File destFolder = new File(destPath);
            log.info("EOD Files started  to move");

            try {
                FileUtils.copyDirectory(srcFolder, destFolder);
                log.info("EOD Files copied to destination");

            } catch (IOException e) {
                e.printStackTrace();
                log.info("EOD Files cant copied to destination:{}", e.getMessage());
            }
            File[] listOfFiles = destFolder.listFiles();
            File tempFile = null;
            if (listOfFiles != null) {
                tempFile = new File(destPath + File.separator + tempPath);
                if (!tempFile.exists()) {
                    log.info("Temp Folder creating and setting permission");

//                    tempFile.mkdir();
                    tempFile.mkdirs();
                    tempFile.setWritable(true, false);
                    tempFile.setReadable(true, false);
                    tempFile.setExecutable(true, false);
                }
                log.info("Temp Folder exist");
                for (File child : listOfFiles) {
                    if (child.getName().contains("stp")) {
                        Path destFilePath = Paths.get(destFolder + File.separator + child.getName());
                        Path tempFilePath = Paths.get(tempFile + File.separator + child.getName());
                        // Move stp files to temp folder
                        log.info("STP  Files started to be moved to tempStp Folder ");
                        try {
                            Files.move(destFilePath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            log.info("STP  Files can't be moved to tempStp Folder ");
                        }
                        log.info("STP FILES moved to the path:{}", tempFilePath.toAbsolutePath());
                    }
                }
            }
            log.info("destFolder:{}", destFolder.getAbsolutePath());
            // STP Data loading after other reports
            log.info("file temp folder path {} , destPath {}", tempFile, destPath);
            uploadFiles(destFolder, destPath, jsonPath, stpLoaded, stpLoadCount, stpLoadedAllowed, logsQueue);
            log.info("Other Files Count:{}", fileCount);

            // STP Data loading after other reports
            log.info("stpLoadedAllowed:{}", stpLoadedAllowed);
            if (stpLoadedAllowed) {
                if (tempFile != null) {
                    if (tempFile.listFiles() != null) {
                        boolean stopLoad = false;
                        //testing whether code with status checking itself enough to load stp at last
                        do {
                            log.info("STP Loading Check :{}", LocalDateTime.now());
                            int tempCount = 0;
                            log.info("Other Files Count:{} match Count:{}", fileCount, tempCount);
                            for (BatchStatus value : LoaderConfig.statusMap.values()) {
                                if (value == BatchStatus.COMPLETED || value == BatchStatus.FAILED) {

                                    tempCount++;
                                }
                            }
                            log.info("Other Files Count after for loop:{} match Count:{}", fileCount, tempCount);
                            if (fileCount == tempCount) {
                                log.info("Count matched:");
                                log.info("stp file temp folder path {} , destPath {}", tempFile, destPath);
                                uploadFiles(tempFile, destPath, jsonPath, stpLoaded, stpLoadCount, stpLoadedAllowed, logsQueue);
                                stopLoad = true;
                            }
                            log.info("Thread started to sleep:{} ", threadIdleTime);
                            Thread.sleep(threadIdleTime * 60000L);
                            log.info("Thread resume to check whether the files Loaded Completely");
                        } while (!stopLoad);
                    }
                }
            }
        }
    }


    private long getReadableFileSizeKb(double length) {
        double kilobytes = (length / (double) 1024);
        return (long) Math.ceil(kilobytes);
    }

    @Scheduled(cron = "${uam.cron.clear.sources.folder.expressions}")
    public void clearList() throws IOException {
        log.info("List cleared at {}", LocalDateTime.now());
        listOfNames.clear();
        statusMap.clear();
        fileCount = 0;
        isStpLoaded = false;
        String intraDaySrcFiles = environment.getProperty("uam.paths.uploads.intraday.source-files");
        String eodSrcFiles = environment.getProperty("uam.paths.uploads.source-files");

        if (!eodSrcFiles.isEmpty()) {
            File sources = new File(eodSrcFiles);
            log.info("eodSrcFiles:{}", sources.getName());
            FileUtils.cleanDirectory(sources);
        }

        if (!intraDaySrcFiles.isEmpty()) {
            File intraFayFile = new File(intraDaySrcFiles);
            log.info("intraDayFile:{}", intraFayFile.getName());
            FileUtils.cleanDirectory(intraFayFile);
        }
        log.info("Sources cleared at {}", LocalDateTime.now());

    }

    public void uploadFiles(File destFolder, String destPath, String jsonPath, long stpLoaded, long stpLoadCount, boolean stpLoadedAllowed, LinkedBlockingQueue<DataLoaderProcessLog> logsQueue) throws InterruptedException {
        long dataLoadingJobSleepTime = Long.parseLong(Objects.requireNonNull(environment.getProperty("uam.data.loading.job.sleep.minutes")));

        if (destFolder.listFiles() != null) {

            File[] arr = destFolder.listFiles();
            //to change the files in custome order based on json
            Arrays.sort(arr, new ReportsDataLoadingComparator());
            //to change the files in custome order based on json
            for (File file : arr) {
                File processFile = new File(destPath + File.separator + "processedFiles");
                if (!processFile.exists()) {
                    processFile.mkdirs();
                    processFile.setWritable(true, false);
                    processFile.setReadable(true, false);
                    processFile.setExecutable(true, false);
                }
                if (file.isFile()) {
                    boolean res = true;
                    // for dormnat couterparty update in counterparty table
                    String dormantCounterpartyDate = null;
                    String fileNames = file.getName().split("_")[0];

                    if (fileNames.toLowerCase().contains("stp")) {
                        File stpProcessFile = new File(destFolder + File.separator + "processedFiles");
                        res = new File(stpProcessFile, file.getName().split("\\.")[0]).exists();
                    } else {
                        res = new File(processFile, file.getName().split("\\.")[0]).exists();
                    }

                    log.info("Filename and processed files :{} {}", file.getName(), processFile);
                    log.info("res:{}", res);
                    if (!res) {
                        File[] fileArr = new File(jsonPath).listFiles();
                        for (File jsonFile : fileArr) {
                            DataImportJob importJob = new DataImportJob();
                            String jsonFileName = jsonFile.getName().split("\\.")[0];
                            if (fileNames.equals(jsonFileName)) {
                                if (!fileNames.toLowerCase().startsWith("stp") || stpLoadedAllowed) {
                                    try {
                                        String fileName = file.getName();
                                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy:HH:mm:ss");
                                        String jobName = fileName + "_" + dateFormatter.format(LocalDateTime.now());
                                        BufferedReader reader = new BufferedReader(new FileReader(file));
                                        long lines = 0L;
                                        while (reader.readLine() != null) lines++;
                                        reader.close();

                                        String reportDate;
                                        LocalDate requestDate;
                                        String tableName = utilsConfigs.getTableName(jsonFile.getAbsolutePath());
                                        if (!listOfNames.contains(tableName)) {
                                            log.info("file to be processed:{}", fileNames);
                                            listOfNames.add(tableName);
                                            log.info("Table cleanup started..");
                                            reportDate = fileName.split("_")[1].split("\\.")[0];
                                            requestDate = LocalDate.parse(reportDate, dateTimeFormatter);
                                            // for counterparty
                                            String reportName = fileName.split("eod")[0];
                                            log.info("fileName : {} ReportName : {}", fileName, reportName);
                                            String deleteQuery;
                                            List<String> elasticFiles = Arrays.asList("closingentity", "counterparty", "counterpartycreation", "dormantcounterparty", "userpolicy", "grouppfoliorights", "chinesewalltmpl", "groupnavrights",
                                                    "groupcombpfolio", "operationrights", "osprightsmatrix", "enterpriserisk", "financeaccctrlrights", "consistencytmp", "cwtconfigmgtrights", "supchgaudit", "displaysi", "udfstructureird",
                                                    "udfstructurecomm", "udfstructurefxd", "userloginaudit", "stprightssrcmod", "stprightstypology", "stprightsmatrix");
                                            List<String> mysqlAndElastic = List.of("usergrouprights", "mxuserlist", "mxgrouplist", "userlicense");
                                            if (elasticFiles.contains(reportName)) {
                                                elasticSearchUtils.deleteData(String.valueOf(requestDate), reportName);
                                            } else if (mysqlAndElastic.contains(reportName)) {
                                                deleteQuery = "DELETE from " + tableName + "  where DATE_FORMAT(REP_DATE,'%Y%m%d')='" + reportDate + "'";
                                                utilsConfigs.deleteQuery(deleteQuery);
                                                elasticSearchUtils.deleteData(String.valueOf(requestDate), reportName);
                                            } else if (fileName.toLowerCase().contains("stp")) {
                                                deleteQuery = "DELETE from " + tableName + "  where DATE_FORMAT(MX_REP_DATE,'%Y%m%d')='" + reportDate + "'";
                                                utilsConfigs.deleteQuery(deleteQuery);
                                            } else {
                                                deleteQuery = "DELETE from " + tableName + "  where DATE_FORMAT(REP_DATE,'%Y%m%d')='" + reportDate + "'";
                                                utilsConfigs.deleteQuery(deleteQuery);
                                            }
                                            log.info("Data import job started..");
                                            importJob.setDataType("");
                                            importJob.setImportTimestamp(LocalDateTime.now());
                                            importJob.setRecordsInSource(lines);
                                            importJob.setRecordsImported(0L);
                                            importJob.setFileName(fileName);
                                            importJob.setFileSize(getReadableFileSizeKb(file.length()));
                                            importJob.setFileLastModifed(file.lastModified());
                                            importJob.setTableName(tableName);
                                            importJob.setReportType(ReportType.EOD);
                                            importJob.setPurged(false);
                                            importJob.setJobStatus(MxJobLogType.STARTED);
                                            importJob.setAutoLoad(true);
                                            if (fileName.toLowerCase().contains("stp")) {
                                                importJob.setReportDate(requestDate.minusDays(1));
                                            } else {
                                                importJob.setReportDate(requestDate);
                                            }
                                            importJob = dataImportJobRepository.save(importJob);
                                            logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO, String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
                                            logWriter(logsQueue);
                                            MxSuperViewLog mxSuperViewLog = new MxSuperViewLog();
                                            mxSuperViewLog.setRootJobId(importJob.getId());
                                            mxSuperViewLog.setCreated(LocalDateTime.now());
                                            mxSuperViewLog.setLogType(MxLogType.INFO);
                                            mxSuperViewLog.setLogMessage("Job is Started");
                                            mxSuperViewLogRepository.save(mxSuperViewLog);
                                            batchUpload.setIdAndReportDate(importJob, reportDate, fileName);
                                            //Job job = batchUpload.createJob(jobName);
                                            JobExecution execution;
                                            if (elasticFiles.contains(reportName)) {
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
                                                        .addString("csvJsonPath", jsonFile.getAbsolutePath())
                                                        .addString("entityName", tableName)
                                                        .addLong("importJobId", importJob.getId())
                                                        .addString("dataLoading", "elastic")
                                                        .toJobParameters());
                                                // for verification
                                                if (execution.getStatus() == BatchStatus.COMPLETED) {
                                                    log.info("Elastic ElasticSearchUtils: Elastic AUTO LOAD : jobName {} Job completed successfully at " + jobName, LocalDateTime.now());
                                                }
                                            } else if (mysqlAndElastic.contains(reportName)) {
                                                log.info("--LOADING IN MYSQL AND ELASTIC--");
                                                //csv - mysql
                                                logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO, String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
                                                LoaderConfig.logWriter(logsQueue);
                                                Job job = batchUpload.createJob(jobName);
                                                execution = jobLauncher.run(job, new JobParametersBuilder()
                                                        .addString("filePath", file.getAbsolutePath())
                                                        .addString("jsonPath", jsonFile.getAbsolutePath())
                                                        .addLong("importJobId", importJob.getId())
                                                        .addString("dataLoading", "mysql")
                                                        .toJobParameters());
                                                // for verification
                                                if (execution.getStatus() == BatchStatus.COMPLETED) {
                                                    log.info("ElasticSearchUtils: Mysql AUTO LOAD : Job jobName {} completed successfully at " + jobName, LocalDateTime.now());
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
                                                        .addString("csvJsonPath", jsonFile.getAbsolutePath())
                                                        .addString("entityName", tableName)
                                                        .addLong("importJobId", importJob.getId())
                                                        .addString("dataLoading", "elastic")
                                                        .toJobParameters());
                                                // for verification
                                                if (execution.getStatus() == BatchStatus.COMPLETED) {
                                                    log.info("Elastic ElasticSearchUtils: Elastic AUTO LOAD : jobName {} Job completed successfully at " + jobName, LocalDateTime.now());
                                                }
                                            } else {
                                                log.info("--LOADING IN MYSQL--");
                                                // csv to mysql
                                                logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.STARTED, MxLogType.INFO, String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING STARTED")));
                                                LoaderConfig.logWriter(logsQueue);
                                                Job job = batchUpload.createJob(jobName);
                                                execution = jobLauncher.run(job, new JobParametersBuilder()
                                                        .addString("filePath", file.getAbsolutePath())
                                                        .addString("jsonPath", jsonFile.getAbsolutePath())
                                                        .addLong("importJobId", importJob.getId())
                                                        .addString("dataLoading", "mysql")
                                                        .toJobParameters());
                                                // for verification
                                                if (execution.getStatus() == BatchStatus.COMPLETED) {
                                                    log.info("ElasticSearchUtils: Mysql AUTO LOAD : Job jobName {} completed successfully at " + jobName, LocalDateTime.now());
                                                }
                                            }
                                            fileCount++;
                                            log.info("job id :{}", execution.getJobParameters().getLong("importJobId"));
                                            Thread.sleep(dataLoadingJobSleepTime * 60000L);
                                            // save audit for this action
                                            auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_LOAD_JOBS, AuditAction.UPLOAD, importJob.getId(), file.getName(), null, null, null, null, null, null, null, null);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        logsQueue.put(new DataLoaderProcessLog(importJob.getId(), MxJobLogType.FAILED, MxLogType.ERROR, String.format("%12s %1s", " : ", importJob.getTableName() + " - LOADING FAILED : " + ex.getMessage())));
                                        logWriter(logsQueue);
                                        log.info("Exception Occur can't upload file with filename:{}", file.getName());
                                    }
                                }
                            }
                        }
                    } else {
                        log.info("{} already in processing", file.getName());
                    }
                }
            }
        }
        log.info("Total files loaded:{}", listOfNames);
    }

    public void executeSummaryDeadTradeCount(String s) throws IOException {
        String sql = readFile("STP_LOAD/STP_LOAD_QUERIES.sql");
        log.info("Starting to execution : Data Loading : STP file:{}", s);
//        for (String singleQuery : sql.split(";")) {
        try {
            sql = sql.replace("[PATH]", s);
            log.info("executing query : {}", sql);
            utilsConfigs.insertQuery(sql);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Query {} execution failed : {}", sql, e.getMessage());
        }
//        }
        log.info("Completed of execution : Data Loading : DEAD_TRADE_COUNT");
//        removeFile(environment.getProperty("summary.table.path.DEAD_TRADE_COUNT"));
        log.info("Deleted : Data Loading : DEAD_TRADE_COUNT");
    }

    private String readFile(final String relFilePath) throws IOException {
        final URL url = Resources.getResource(relFilePath);
        return Resources.toString(url, StandardCharsets.UTF_8);
    }


    public static void createLogPath() {
        File logFilePath = new File(Objects.requireNonNull(environment.getProperty("uam.autoload.log.filepath")));
        if (!logFilePath.exists()) {
            logFilePath.mkdirs();
            logFilePath.setWritable(true, false);
            logFilePath.setReadable(true, false);
            logFilePath.setExecutable(true, false);
        }
    }

    public static void deleteLogFile() {
        File fileToDelete = new File(Objects.requireNonNull(environment.getProperty("uam.autoload.log.filepath")) + "/uam_autoload_" + LocalDate.now().minusDays(Long.parseLong(Objects.requireNonNull(environment.getProperty("uam.autoload.log.filescount")))).format(dateTimeFormatter) + ".log");
        if (fileToDelete.exists()) {
            fileToDelete.delete();
        }
    }

    public static void logWriter(LinkedBlockingQueue<DataLoaderProcessLog> messageQueue) {
        File logFile = new File(environment.getProperty("uam.autoload.log.filepath") + "/uam_autoload_" + LocalDate.now().format(dateTimeFormatter) + ".log");
        if (!logFile.exists()) {
            log.info("File not Exists");
        }
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(logFile, true))) {
            // take log message from the logs queue
            DataLoaderProcessLog logMessage = messageQueue.take();
            // write logs in the log file
            pw.println("JOB ID : " + logMessage.getJobId() + "\t" + logMessage.getCreated().format(formatter) + " " + logMessage.getProcessStage() + " " + logMessage.getLogType() + logMessage.getMessage());
            pw.flush();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

