package com.finsurge.tmr_portal.mx_superview.elastic_search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.json.JsonData;
import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;

import com.finsurge.tmr_portal.mx_superview.elastic_search.models.HouseKeepingLogsRepository;
import com.finsurge.tmr_portal.mx_superview.entity.MxPreference;
import com.finsurge.tmr_portal.mx_superview.repository.MxPreferenceRepository;
import com.finsurge.tmr_portal.mx_superview.util.ElasticUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Primary
@Service("HouseKeepingService")
@Slf4j
public class HouseKeepingService {
    @Autowired
    private final ElasticUtils elastic_utils;
    @Autowired
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private MxPreferenceRepository mxPreferenceRepository;
    @Autowired
    private HouseKeepingLogsRepository houseKeepingLogsRepository;
    @Autowired
    private final ElasticsearchClient client;
    private static Environment environment;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LinkedBlockingQueue<HouseKeepingLogs> logsQueue = new LinkedBlockingQueue<>();

    public HouseKeepingService(ElasticUtils elastic_utils, Environment environment, JdbcTemplate jdbcTemplate, ElasticsearchClient client) {
        this.elastic_utils = elastic_utils;
        this.jdbcTemplate = jdbcTemplate;
        this.client = client;
        HouseKeepingService.environment = environment;
    }

    public void executeHouseKeeping(String currentDate) throws InterruptedException, IOException {

        Map<Object, Object> dateMap = getPurgeDates(currentDate);

        boolean elastic = false;
        boolean uamJobs = false;
        Long totalDataDeletedInDb = 0L;
        Long deletedReportsInUamJobs = 0L;

// Deleting in Elastic
        try {
            totalDataDeletedInDb = purgeInElastic(
                    dateMap.get("startDate").toString(),
                    dateMap.get("endDate").toString(),
                    dateMap.get("lastFriday").toString()
            );
            elastic = true; // Mark Elasticsearch deletion as successful
        } catch (Exception e) {
            logsQueue.put(new HouseKeepingLogs("FAILED", e.getMessage(), LocalDateTime.now()));
            logWriter(logsQueue);
        }

// Deleting in MySQL
        try {
            deletedReportsInUamJobs = purgeInMysql(
                    dateMap.get("startDate"),
                    dateMap.get("endDate"),
                    dateMap.get("lastFriday")
            );
            uamJobs = true;
            log.info("Total Reports Deleted in UAM_HOUSE_KEEPING_LOGS between:{} and {} is {}", deletedReportsInUamJobs, dateMap.get("startDate"), dateMap.get("endDate"));
        } catch (Exception e) {
            logsQueue.put(new HouseKeepingLogs("FAILED", e.getMessage(), LocalDateTime.now()));
            logWriter(logsQueue);
        }

// Check the status of both deletions
        if (elastic && uamJobs) {
            insertPurgeLog(deletedReportsInUamJobs, totalDataDeletedInDb, dateMap.get("startDate").toString(), dateMap.get("endDate").toString(), "Deleted in Both UAM_JOBS and ELASTIC", "SUCCESS", "true", "true");
        } else if (elastic) {
            insertPurgeLog(deletedReportsInUamJobs, totalDataDeletedInDb, dateMap.get("startDate").toString(), dateMap.get("endDate").toString(), "Deleted in ELASTIC only", "FAILED", "true", "false");
        } else if (uamJobs) {
            insertPurgeLog(deletedReportsInUamJobs, totalDataDeletedInDb, dateMap.get("startDate").toString(), dateMap.get("endDate").toString(), "Deleted in UAM_JOBS only", "FAILED", "false", "true");
        } else {
            insertPurgeLog(deletedReportsInUamJobs, totalDataDeletedInDb, dateMap.get("startDate").toString(), dateMap.get("endDate").toString(), "No Data Deleted", "FAILED", "false", "false");
        }
    }

    private void insertPurgeLog(Long deletedRow, Long deletedCount, String startDate, String endDate, String message, String status, String elastic, String uamJobs) {
        LocalDate startDates = LocalDate.parse(startDate);
        LocalDate endDates = LocalDate.parse(endDate);
        String insertQuery = "INSERT INTO UAM_HOUSE_KEEPING_LOGS (deletedCount, deletedRows, fromDate, toDate, purgedDate, message ,status, elastic, uamJobs) VALUES (?,?,?,?,?,?,?,?,?)";
        log.info("insert query:{}", insertQuery);
        try {
            jdbcTemplate.update(insertQuery, deletedCount, deletedRow, startDates, endDates, LocalDateTime.now(), message, status, elastic, uamJobs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long purgeInMysql(Object startDate, Object endDate, Object specialDate) {
        // DELETE FROM UAM_DATA_IMPORT_JOBS WHERE REP_DATE BETWEEN '2024-07-01' AND '2024-07-08' AND REP_DATE <> '2024-07-06';
        String deleteQuery = "DELETE FROM UAM_DATA_IMPORT_JOBS" +
                " WHERE DATE_FORMAT(REP_DATE, '%Y-%m-%d') BETWEEN ? AND ?" +
                " AND DATE_FORMAT(REP_DATE, '%Y-%m-%d') <> ?";
        Integer result = jdbcTemplate.update(deleteQuery, startDate, endDate, specialDate);
        log.info("Deleted Rows - Reports :{}", result);
        return result.longValue();
    }

    @SneakyThrows
    private Long purgeInElastic(String startDate, String endDate, String specialDate) {
        List<String> uamReports = elastic_utils.getDataFromJsonFile("elastic/HouseKeeping/uamReports.json", "reports");
        long[] deletedCount = {0};
        ExecutorService service = Executors.newFixedThreadPool(10);
        for (String reports : uamReports) {
            String indexName = getIndexName(reports);

            service.execute(() -> {
                Query rangeQuery;
                if (!(startDate.isEmpty())) {
                    rangeQuery = RangeQuery.of(r -> r.field("reportDate")
                            .lte(JsonData.of(endDate))
                            .gte(JsonData.of(startDate)))._toQuery();
                } else {
                    rangeQuery = RangeQuery.of(r -> r.field("reportDate")
                            .lt(JsonData.of(specialDate)))._toQuery();
                }
                Query exceptLastFriday = TermQuery.of(t -> t.field("reportDate").value(specialDate))._toQuery();
                Query boolQuery = BoolQuery.of(b -> b.must(rangeQuery).mustNot(exceptLastFriday))._toQuery();
                DeleteByQueryRequest deleteByQueryRequest = DeleteByQueryRequest.of(d -> d
                        .query(boolQuery)
                        .index(indexName)
                        .conflicts(Conflicts.Proceed));

                log.info("Delete Query Request: {}", deleteByQueryRequest);
                try {
                    DeleteByQueryResponse deleteByQueryResponse = client.deleteByQuery(deleteByQueryRequest);
                    long deleted = (deleteByQueryResponse.deleted() != null) ? deleteByQueryResponse.deleted() : 0;
                    synchronized (deletedCount) {
                        deletedCount[0] += deleted;
                    }
                    log.info("Deleted {} documents from index {}", deleted, indexName);
                } catch (Exception e) {
                    log.error("Error executing delete by query for index {}: {}", indexName, e.getMessage());
                    try {
                        logsQueue.put(new HouseKeepingLogs("FAILED", e.getMessage(), LocalDateTime.now()));
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    try {
                        logWriter(logsQueue);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Executor service interrupted: {}", e.getMessage());
        }
        return deletedCount[0];
    }

    ///  @Scheduled(cron="")
    public Map<Object, Object> getPurgeDates(String currentDate) {
        Map<Object, Object> dates = new HashMap<>();

        MxPreference mxPreference = mxPreferenceRepository.findByPropertyName("UAM_HOUSE_KEEPING");
        int monthCount = Integer.parseInt(mxPreference.getPropertyValue());
        log.info("Getting Month Count -{}",monthCount);

//       LocalDate beforeSixMonth = LocalDate.parse(currentDate).minusMonths(environment.getProperty("uam.housekeeping.cleanup.prior.month", Integer.class));
        LocalDate beforeSixMonth = LocalDate.parse(currentDate).minusMonths(monthCount);
        LocalDate startDate = beforeSixMonth.minusWeeks(1);
        log.info("before six month :{} and 1 week :{}", beforeSixMonth, startDate);
        YearMonth yearMonth = YearMonth.of(startDate.getYear(), startDate.getMonth());
        LocalDate lastFriday = yearMonth.atEndOfMonth();
        log.info("before yearMonth :{} and 1 week :{}", yearMonth, lastFriday);
        while (lastFriday.getDayOfWeek() != DayOfWeek.FRIDAY) {
            lastFriday = lastFriday.minusDays(1);
        }
        LocalDate checkDate = startDate;
        while (checkDate.isBefore(beforeSixMonth) || checkDate.equals(beforeSixMonth)) {
            if (checkDate.getDayOfWeek() == DayOfWeek.FRIDAY) {
                if (checkDate.equals(lastFriday)) {
                    System.out.println("This is the last Friday: " + checkDate);
                } else {
                    System.out.println("This is not the last Friday: " + checkDate);
                }
            }
            checkDate = checkDate.plusDays(1);
        }
        dates.put("startDate", startDate);
        dates.put("endDate", beforeSixMonth);
        dates.put("lastFriday", lastFriday);
        return dates;
    }

    public static void logWriter(LinkedBlockingQueue<HouseKeepingLogs> messageQueue) throws IOException {

        File houseKeepingDirPath = new File(environment.getProperty("uam.houseKeeping.log.filepath"));
        if (!houseKeepingDirPath.exists()) {
            log.info("{}:Path is not Exist", houseKeepingDirPath);
            if (houseKeepingDirPath.mkdirs()) {
                houseKeepingDirPath.setWritable(true, false);
                houseKeepingDirPath.setReadable(true, false);
                houseKeepingDirPath.setExecutable(true, false);
            }
        }

        File logFile = new File(environment.getProperty("uam.houseKeeping.log.filepath") + "/uam_houseKeeping_" + LocalDateTime.now().format(dateTimeFormatter) + ".log");
        log.info("FileName :{}", logFile);
        if (!logFile.exists()) {
            log.info("{}:File is not Exist", logFile);
            if (logFile.createNewFile()) {
                logFile.setWritable(true, false);
                logFile.setReadable(true, false);
                logFile.setExecutable(true, false);
                System.out.println("Created log file: " + logFile.getAbsolutePath());
            }
        }
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(logFile, true))) {
            // take log message from the logs queue
            HouseKeepingLogs logMessage = messageQueue.take();
            pw.println("STATUS : " + logMessage.getStatus() + " " + " PURGED DATE TIME : " + logMessage.getPurgedDate().format(formatter) + " " + "EXCEPTION : " + logMessage.getMessage());
            // write logs in the log file
            // pw.println("JOB ID : " + logMessage.getJobId() + "\t" + logMemxuserlisteod_20230501.csvssage.getCreated().format(formatter) + " " + logMessage.getProcessStage() + " " + logMessage.getLogType() + logMessage.getMessage());
            pw.flush();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getIndexName(String report) {
        return switch (report) {
            case "DormantCounterparty" -> DormantCounterparty.INDEX_NAME;
            case "CounterPartyDocument" -> CounterPartyDocument.INDEX_NAME;
            case "ClosingEntity" -> ClosingEntity.INDEX_NAME;
            case "CounterpartyCreation" -> CounterpartyCreation.INDEX_NAME;
            case "UserGroupAccessRights" -> UserGroupAccessRights.INDEX_NAME;
            case "UserList" -> UserList.INDEX_NAME;
            case "UserGroupList" -> UserGroupList.INDEX_NAME;
            case "UserLicense" -> UserLicense.INDEX_NAME;
            case "UserPolicy" -> UserPolicy.INDEX_NAME;
            case "GroupPortfolioRights" -> GroupPortfolioRights.INDEX_NAME;
            case "GroupNavigationRight" -> GroupNavigationRights.INDEX_NAME;
            case "ChineseWall" -> ChineseWall.INDEX_NAME;
            case "GroupComboPortfolio" -> GroupComboPortfolio.INDEX_NAME;
            case "OperationRights" -> OperationRights.INDEX_NAME;
            case "OspRightsMatrix" -> OspRightsMatrix.INDEX_NAME;
            case "EnterpriseRisk" -> EnterpriseRisk.INDEX_NAME;
            case "FinanceRights" -> FinanceRights.INDEX_NAME;
            case "ConsistencyTmpl" -> ConsistencyTmpl.INDEX_NAME;
            case "ConfigMgtRight" -> ConfigMgtRight.INDEX_NAME;
            case "SupChgAudit" -> SupChgAudit.INDEX_NAME;
            case "DisplaySI" -> DisplaySI.INDEX_NAME;
            case "UdfStructureIrd" -> UdfStructureIrd.INDEX_NAME;
            case "UdfStructureComm" -> UdfStructureComm.INDEX_NAME;
            case "UdfStructureFxd" -> UdfStructureFxd.INDEX_NAME;
            case "UserLoginAudit" -> UserLoginAudit.INDEX_NAME;
            default -> throw new IllegalStateException("Unexpected value: " + report);
        };
    }

    public Page<HouseKeepingLogs> getLogs(int page, int pageSize, HouseKeepingLogsFilter filter, String sortingOrder, String sortBy) {
        // Determine sorting direction based on the input
        Sort.Direction direction = sortingOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Create a pageable object with sorting
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        // Call the repository method with filtering and sorting
        return houseKeepingLogsRepository.findAllByFilters(
                filter.getMessage(),
                filter.getStatus(),
                pageable
        );
    }


}