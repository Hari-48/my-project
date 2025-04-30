package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.DataImportJob;
import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface DataImportJobRepository extends CrudRepository<DataImportJob, Long> {

    @Query(value = "select distinct t.reportDate from DataImportJob t where t.fileName not like 'counterparty%' and t.fileName not like 'displaysi%' and (t.purged=:purged  or t.purged is null) order by t.reportDate desc ")
    List<LocalDate> getDistinctWeekDaysReportDateByPurged(Character purged);

    @Query(value = "select distinct t.reportDate from DataImportJob t where (t.fileName like 'counterparty%' or t.fileName like 'displaysi%') and (t.purged=:purged or t.purged is null) order by t.reportDate desc")
    List<LocalDate> getDistinctWeekEndDaysReportDateByPurged(Character purged);

    DataImportJob findFirstById(Long jobId);


    @Query(value = "select t from DataImportJob  t where (:fileName is null or upper(t.fileName) like upper(concat('%', :fileName, '%'))) and (:jobStatus is null or t.jobStatus = :jobStatus) and (:reportType is null or t.reportType = :reportType) and (:loadingType is null or t.autoLoad = :loadingType)")
    Page<DataImportJob> findAllByFileName(String fileName, MxJobLogType jobStatus, ReportType reportType, Character loadingType, Pageable pageable);

    DataImportJob findDistinctTopByFileNameStartsWithAndPurgedAndJobStatusOrderByReportDateDesc(String fileName, Character purged, MxJobLogType status);

    @Query(value = "select t.reportDate from DataImportJob t where t.fileName like 'stprights%' and t.purged=:purged and t.jobStatus=:status group by t.reportDate" +
            " having count(t)=:stpFileCount order by t.reportDate desc")
    Page<LocalDate> findLatestDate(long stpFileCount, Character purged, MxJobLogType status, Pageable pageable);


    @Query(value = "select count(*) from DataImportJob t where t.reportDate = :reportDate and t.jobStatus != 'COMPLETED'")
    Long completedCount(@Param("reportDate") LocalDate reportDate);


}