package com.finsurge.tmr_portal.mx_superview.configs;

import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewLog;
import com.finsurge.tmr_portal.mx_superview.models.DataLoaderProcessLog;
import com.finsurge.tmr_portal.mx_superview.models.FileDatas;
import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.MxLogType;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxSuperViewLogRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
@EnableBatchProcessing
public class BatchUpload {

    private static final Logger log = LoggerFactory.getLogger(BatchUpload.class);

    public JobBuilderFactory jobBuilderFactory;
    public StepBuilderFactory stepBuilderFactory;
    public JobRepository jobRepository;
    public DataSource dataSource;

    private Listener completionListener;

    public final MxSuperViewLogRepository mxSuperViewLogRepository;
    private final DataImportJobRepository dataImportJobRepository;
    public static LinkedBlockingQueue<DataLoaderProcessLog> logsQueue;

    private final DateTimeFormatter yyyyMMdd =  DateTimeFormatter.ofPattern("yyyyMMdd");
    public String localDate;
    public Long jobId;
    public String reportDate;
    public String mxReportDate;
    public String stpReportDate;
    public int day;
    public int month;
    public int year;


    public BatchUpload(JobBuilderFactory jobBuilderFactory,
                       StepBuilderFactory stepBuilderFactory,
                       JobRepository jobRepository,
                       Listener completionListener,
                       Environment env,
                       MxSuperViewLogRepository mxSuperViewLogRepository, DataImportJobRepository dataImportJobRepository) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.jobRepository = jobRepository;
        this.completionListener = completionListener;
        this.mxSuperViewLogRepository = mxSuperViewLogRepository;
        this.dataImportJobRepository = dataImportJobRepository;

        HikariConfig dataSourceConfig = new HikariConfig();
        dataSourceConfig.setDriverClassName(env.getRequiredProperty("uam.database.driver"));
        dataSourceConfig.setJdbcUrl(env.getRequiredProperty("uam.database.url"));
        dataSourceConfig.setUsername(env.getRequiredProperty("uam.database.username"));
        dataSourceConfig.setPassword(env.getRequiredProperty("uam.database.password"));
        dataSourceConfig.setConnectionTimeout(600000);
        dataSource = new HikariDataSource(dataSourceConfig);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<FileDatas> reader(@Value("#{jobParameters['filePath']}") String filePath, @Value("#{jobParameters['jsonPath']}") String jsonPath) throws IOException, ParseException {

        return new FlatFileItemReaderBuilder<FileDatas>().name("data")
                .resource(new FileSystemResource(filePath))
                //.encoding("Cp1252")
                .encoding("UTF-8")
                .lineMapper(new CustomLineMapper(jsonColumnNames(jsonPath), "~"))
                .linesToSkip(1)
                .build();
    }

    /*  @Bean
      public ItemCountItemStream stream() {
          return new ItemCountItemStream();
      }
  */
    @Bean
    @StepScope
    public JdbcBatchItemWriter<FileDatas> writer(@Value("#{jobParameters['jsonPath']}") String jsonPath, @Value("#{jobParameters['importJobId']}") Long importJobId) throws IOException, ParseException, InterruptedException {
        StringBuilder queryColumns = new StringBuilder();
        StringBuilder valueColumns = new StringBuilder();
        ArrayList<String> fields = new ArrayList<>(jsonColumnNames(jsonPath));
        String tableName = getTableName(jsonPath);
        DataImportJob importJob = dataImportJobRepository.findFirstById(importJobId);
        for(String field : fields) {
            queryColumns.append(field);
            valueColumns.append(":").append(field);
            if(jsonColumnNames(jsonPath).indexOf(field) != jsonColumnNames(jsonPath).size()-1) {
                queryColumns.append(", ");
                valueColumns.append(", ");
            }
        }
        String query="";
        localDate = yyyyMMdd.format(LocalDate.now());
        if(tableName.startsWith("UAM_MX_STP")) {
            mxReportDate=yyyyMMdd.format(LocalDate.parse(reportDate,yyyyMMdd));
            stpReportDate = yyyyMMdd.format(LocalDate.parse(mxReportDate,yyyyMMdd).minusDays(1));
            year = LocalDate.parse(stpReportDate,yyyyMMdd).getYear();
            month = LocalDate.parse(stpReportDate,yyyyMMdd).getMonthValue();
            day = LocalDate.parse(stpReportDate,yyyyMMdd).getDayOfMonth();

            query = "INSERT INTO "+tableName+" (" + queryColumns +", SYS_DATE, JOB_ID, REP_DATE, MX_REP_DATE,iDay,iMonth,iYear"+") VALUES (" + valueColumns +", date_format('"+localDate+"','%Y%m%d'),"+importJobId+",date_format('"+stpReportDate+"','%Y%m%d')"+",date_format('"+mxReportDate+"','%Y%m%d'),"+day+","+month+","+year+")";
        }else if(tableName.startsWith("UAM_MX_CHINESE")||tableName.startsWith("UAM_MX_GROUP_LIST")||tableName.startsWith("UAM_MX_GROUP_PORTFOLIO")||tableName.startsWith("UAM_MX_GROUP_NAV")){
            LocalDate repDate=LocalDate.parse(reportDate,yyyyMMdd);
            year = repDate.getYear();
            month = repDate.getMonthValue();
            day = repDate.getDayOfMonth();
            reportDate = yyyyMMdd.format(LocalDate.parse(reportDate,yyyyMMdd));
            query = "INSERT INTO "+tableName+" (" + queryColumns +", SYS_DATE, JOB_ID, REP_DATE,iDay,iMonth,iYear"+") VALUES (" + valueColumns +", date_format('"+localDate+"','%Y%m%d'),"+importJobId+",date_format('"+reportDate+"','%Y%m%d'),"+day+","+month+","+year+")";
        }else{
            reportDate = yyyyMMdd.format(LocalDate.parse(reportDate,yyyyMMdd));
            query = "INSERT INTO "+tableName+" (" + queryColumns +", SYS_DATE, JOB_ID, REP_DATE"+") VALUES (" + valueColumns +", date_format('"+localDate+"','%Y%m%d'),"+importJobId+",date_format('"+reportDate+"','%Y%m%d'))";

        }
        // add log in logs queue
        logsQueue.put(new DataLoaderProcessLog(importJobId, MxJobLogType.RUNNING, MxLogType.INFO, String.format("%12s %1s", " : ", tableName + " - WRITING DATA")));
        LoaderConfig.logWriter(logsQueue);

        log.info("QUERY : {}",query);
        return new JdbcBatchItemWriterBuilder<FileDatas>()
                .itemSqlParameterSourceProvider(item -> {
                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
                    mapSqlParameterSource.addValues(item.getDataMap());
                    return mapSqlParameterSource;
                })
                .sql(query)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JobLauncher jobLauncher() {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(taskExecutor());
        return jobLauncher;
    }


    /*@Bean
    public SimpleAsyncTaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor=new SimpleAsyncTaskExecutor();
        simpleAsyncTaskExecutor.setConcurrencyLimit(10);
        simpleAsyncTaskExecutor.setThreadPriority(1);
        simpleAsyncTaskExecutor.setThreadNamePrefix("Import");
        return simpleAsyncTaskExecutor;
    }*/

    //improve performance
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setQueueCapacity(16);
        //Thread name set to start with  uam-executor-
        executor.setThreadNamePrefix("uam-import-");
        executor.initialize();
        return executor;
    }

    public Job createJob(String jobName) throws Exception {
        // initialize logs queue
        logsQueue = new LinkedBlockingQueue<>();
        return jobBuilderFactory.get(jobName)
                .incrementer(new RunIdIncrementer())
                .listener(completionListener)
                .flow(importStep())
                .end()
                .build();
    }

    @Bean
    public CustomProcessor<FileDatas> process() {
        return new CustomProcessor<FileDatas>();
    }

    @Bean
    public Step importStep() throws IOException, ParseException, InterruptedException {
        return stepBuilderFactory.get("importStep")
                .<FileDatas, FileDatas> chunk(10000)
                .reader(reader(null,null))
                .processor(process())
                .writer(writer(null, null))
                .taskExecutor(taskExecutor())
                .build();
    }


    public static ArrayList<String> jsonColumnNames(@Value("#{jobParameters['jsonPath']}") String jsonPath) throws IOException,org.json.simple.parser.ParseException {

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(jsonPath));
        JSONArray jsonArray = (JSONArray) jsonObject.get("fields");
        ArrayList<String> arr = new ArrayList<>();
        for (Object o : jsonArray) {
            arr.add((String) o);
        }
        return arr;
    }

    public static String getTableName(@Value("#{jobParameters['jsonPath']}") String jsonPath) throws IOException,ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(jsonPath));
        return (String) jsonObject.get("tableName");
    }


    public void setIdAndReportDate(DataImportJob importJob, String reportDate, String fileName) {
        this.jobId=importJob.getId();
        this.reportDate=reportDate;
        importJob.setJobStatus(MxJobLogType.RUNNING);
        MxSuperViewLog mxSuperViewLog = new MxSuperViewLog();
        mxSuperViewLog.setRootJobId(jobId);

        mxSuperViewLog.setCreated(LocalDateTime.now());
        mxSuperViewLog.setLogType(MxLogType.INFO);
        mxSuperViewLog.setLogMessage(fileName+" is Loading to DB");

        mxSuperViewLogRepository.save(mxSuperViewLog);
    }
}
