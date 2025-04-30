package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.UamSummaryReportJob;
import com.finsurge.tmr_portal.mx_superview.models.UamReportSummary;
import com.finsurge.tmr_portal.mx_superview.repository.UamSummaryReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class UamSummaryReportJobService {

    private static Logger log = LoggerFactory.getLogger(UamSummaryReportJobService.class);
    @Autowired
    private UamSummaryReportRepository jobRepository;
    //track status
    public UamSummaryReportJob getJob(Long id) {
        return jobRepository.findById(id).orElse(null);
    }

    public UamReportSummary getSummary(Long id) {
        UamSummaryReportJob job = jobRepository.findById(id).orElse(null);
        return job.getReportSummary();
    }

    //create new summary report job
    @Transactional
    public UamSummaryReportJob createNewJob(String reportName, String username) {
        UamSummaryReportJob job = new UamSummaryReportJob();
        job.setReportName(reportName);
        job.setUsername(username);
        job.setRequestTime(LocalDateTime.now());
        job.setProgress(0);
        job.setItemsTotal(0);
        job.setItemsProcessed(0);
        job.setProgressStage("INITIALIZED");
        return jobRepository.save(job);
    }

    //update status & summary
    @Transactional
    public void updateJob(Long id, String reportSummary, boolean isReady) {
        jobRepository.updateUamSummaryReportJobStatus(isReady ? 'Y' : 'N', reportSummary, id);
    }

    //calculate the job progress
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateJobProgress(String stage, Integer itemsTotal, Integer itemsProcessed, Long id) {
        Integer progress = 0;
        if (itemsTotal != null && itemsProcessed != null && itemsTotal > 0) {
            BigDecimal progressBigInt = new BigDecimal(itemsProcessed).divide(new BigDecimal(itemsTotal), 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
            progress = progressBigInt.intValue();
        }
        log.info("({} / {}) * 100 = Progress: {}", itemsProcessed, itemsTotal, progress);
        jobRepository.updateUamSummaryReportJobProgress(itemsTotal, itemsProcessed, progress, stage, id);
    }
}
