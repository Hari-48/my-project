package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.general.models.AuditAction;
import com.finsurge.tmr_portal.general.models.AuditModule;
import com.finsurge.tmr_portal.general.models.AuditObject;
import com.finsurge.tmr_portal.general.util.AuditUtils;
import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewLog;
import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.MxLogType;
import com.finsurge.tmr_portal.mx_superview.models.ReportType;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxSuperViewJobRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxSuperViewLogRepository;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableScheduling
public class IntradayConfig {
    private final Logger log = LoggerFactory.getLogger(IntradayConfig.class);

    private JobLauncher jobLauncher;

    private BatchUpload batchUpload;

    private AuditUtils auditUtils;

    private Environment environment;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DataImportJobRepository dataImportJobRepository;

    private final MxSuperViewLogRepository mxSuperViewLogRepository;

    private final UtilsConfigs utilsConfigs;

    private final MxSuperViewJobRepository mxSuperViewJobRepository;

    public IntradayConfig(DataImportJobRepository dataImportJobRepository, JobLauncher jobLauncher, AuditUtils auditUtils, BatchUpload batchUpload, Environment environment, MxSuperViewLogRepository mxSuperViewLogRepository, UtilsConfigs utilsConfigs, MxSuperViewJobRepository mxSuperViewJobRepository) {
        this.jobLauncher = jobLauncher;
        this.batchUpload = batchUpload;
        this.auditUtils = auditUtils;
        this.environment = environment;
        this.dataImportJobRepository = dataImportJobRepository;
        this.mxSuperViewLogRepository = mxSuperViewLogRepository;
        this.utilsConfigs = utilsConfigs;
        this.mxSuperViewJobRepository = mxSuperViewJobRepository;
    }


   @Scheduled(cron = "${uam.cron.intraday.expressions}")
    public void autoLoad() throws Exception {
        String srcPath = environment.getProperty("uam.source.intraday.path");
        String destPath = environment.getProperty("uam.paths.uploads.intraday.source-files");
        String jsonPath = environment.getProperty("uam.paths.uploads.intraday.config-files");
        File resource = new ClassPathResource("UAM/uam_config.json").getFile();
        long stpLoadCount = 0;
        long stpLoaded = 0;
        JSONParser parser = new JSONParser();
        if (resource.exists()) {
            String defaultConfig = new String(Files.readAllBytes(resource.toPath()));
            JSONObject json = (JSONObject) parser.parse(defaultConfig);
            stpLoadCount =(long)json.get("data_load_stp_max_file_count");
        }

        srcPath = srcPath.replace("*", dateTimeFormatter.format(LocalDate.now().minusDays(1)));
        File srcFolder = new File(srcPath);
        File[] files = srcFolder.listFiles();
        if (files != null) {
                File destFolder = new File(destPath);
                FileUtils.copyDirectory(srcFolder, destFolder);

            if (destFolder.listFiles() != null) {

                File[] arr = destFolder.listFiles();

                for (File file : arr) {
                    File processFile = new File(destPath + File.separator + "processedFiles");

                    if (!processFile.exists()) {
                        processFile.mkdir();
                    }

                    boolean res = new File(processFile, file.getName().split("\\.")[0]).exists();

                    if (!res) {

                        String fileNames = file.getName().split("_")[0];

                        File[] fileArr = new File(jsonPath).listFiles();


                        for (File jsonFile : fileArr) {

                            String jsonFileName = jsonFile.getName().split("\\.")[0];

                            if (fileNames.equals(jsonFileName)) {
                                try {
                                String fileName = file.getName();
                                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy:HH:mm:ss");
                                String jobName = fileName + "_" + dateFormatter.format(LocalDateTime.now());
                                BufferedReader reader = new BufferedReader(new FileReader(file));
                                long lines = 0L;
                                while (reader.readLine() != null) lines++;
                                reader.close();


                                String reportDate = fileName.split("_")[1].split("\\.")[0];
                                LocalDate requestDate = LocalDate.parse(reportDate, dateTimeFormatter);

                                String tableName = utilsConfigs.getTableName(jsonFile.getAbsolutePath());

                                log.info("Processing ");

                                //stp have reportdate in the differnt index and stp max file count is 4
                                    if (fileName.toLowerCase().contains("stp")) {
                                        reportDate = fileName.split("_")[2].split("\\.")[0];
                                        requestDate = LocalDate.parse(reportDate, dateTimeFormatter);
                                        //delete the already loaded contents for that date
                                        if (stpLoaded == 0 || stpLoaded == stpLoadCount) {
                                            String deleteQuery = "DELETE from " + tableName + " t where date_format(t.REP_DATE,'%Y%m%d')='" + reportDate + "'";
                                            utilsConfigs.deleteQuery(deleteQuery);
                                        }

                                        stpLoaded++;
                                    } else {
                                        reportDate = fileName.split("_")[1].split("\\.")[0];
                                        requestDate = LocalDate.parse(reportDate, dateTimeFormatter);
                                        String deleteQuery = "DELETE from " + tableName + " t where date_format(t.REP_DATE,'%Y%m%d')='" + reportDate + "'";
                                        utilsConfigs.deleteQuery(deleteQuery);
                                    }

                                    DataImportJob importJob = new DataImportJob();
                                    importJob.setDataType("");
                                    importJob.setImportTimestamp(LocalDateTime.now());
                                    importJob.setRecordsInSource(lines);
                                    importJob.setRecordsImported(0L);
                                    importJob.setFileName(fileName);
                                    importJob.setFileSize(getReadableFileSizeKb(file.length()));
                                    importJob.setTableName(tableName);
                                    importJob.setFileLastModifed(file.lastModified());
                                    importJob.setReportType(ReportType.INTRADAY);
                                    importJob.setAutoLoad(true);
                                    importJob.setPurged(false);
                                    if(fileName.toLowerCase().contains("stp")){
                                        importJob.setReportDate(requestDate.minusDays(1));
                                    }
                                    else{
                                        importJob.setReportDate(requestDate);

                                    }                                    importJob.setJobStatus(MxJobLogType.STARTED);
                                    importJob = dataImportJobRepository.save(importJob);


                                    MxSuperViewLog mxSuperViewLog = new MxSuperViewLog();
                                    mxSuperViewLog.setRootJobId(importJob.getId());
                                    mxSuperViewLog.setCreated(LocalDateTime.now());
                                    mxSuperViewLog.setLogType(MxLogType.INFO);
                                    mxSuperViewLog.setLogMessage("Job is Started ");

                                mxSuperViewLogRepository.save(mxSuperViewLog);

                                log.info("Intraday Loading Started");
                                batchUpload.setIdAndReportDate(importJob, reportDate, fileName);
                                Job job = batchUpload.createJob(jobName);
                                JobExecution execution = jobLauncher.run(job, new JobParametersBuilder()
                                        .addString("filePath", file.getAbsolutePath())
                                        .addString("jsonPath", jsonFile.getAbsolutePath())
                                        .addLong("importJobId", importJob.getId())
                                        .toJobParameters());

                                log.info("job id {}", execution.getJobParameters().getLong("importJobId"));

                                // save audit for this action
                                auditUtils.saveAudit(AuditModule.UAM, AuditObject.UAM_DATA_LOAD_JOBS, AuditAction.UPLOAD, importJob.getId(),
                                        file.getName(), null, null, null, null, null, null, null, null);
                            }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                    log.info("Exception Occur can't upload file with filename:{}"+file.getName());
                                }
                            }
                        }
                    } else {
                        log.info("{} Already Processed", file.getName());
                    }
                }
//            }
            }
        }
    }

    private long getReadableFileSizeKb(double length) {
        double kilobytes = (length / (double) 1024);
        return (long) Math.ceil(kilobytes);
    }
}

