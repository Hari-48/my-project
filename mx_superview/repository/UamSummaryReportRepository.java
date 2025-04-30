package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.UamSummaryReportJob;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UamSummaryReportRepository extends CrudRepository<UamSummaryReportJob, Long> {

    @Modifying
    @Query(value = "update UamSummaryReportJob t set t.isReady = :isReady, t.reportSummary = :reportSummary where t.id = :id")
    int updateUamSummaryReportJobStatus(Character isReady, String reportSummary, Long id);

    @Modifying
    @Query(value = "update UamSummaryReportJob t set t.itemsTotal = :itemsTotal, t.itemsProcessed = :itemsProcessed, t.progress = :progress, t.progressStage = :progressStage where t.id = :id")
    int updateUamSummaryReportJobProgress(Integer itemsTotal, Integer itemsProcessed, Integer progress, String progressStage, Long id);

}
