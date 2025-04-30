package com.finsurge.tmr_portal.mx_superview.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsurge.tmr_portal.mx_superview.models.UamReportSummary;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "UAM_SUMMARY_REPORT_JOB",
        indexes = {
                @Index(name = "IDX_UAM_SURJOB_USRNM", columnList = "USERNAME"),
                @Index(name = "IDX_UAM_SURJOB_TYPE", columnList = "REPORT_NAME"),
                @Index(name = "IDX_UAM_SURJOB_STATUS", columnList = "IS_READY")
        }
)
public @Data class UamSummaryReportJob {

    private static ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "REPORT_NAME")
    private String reportName;

    @Column(name = "PROGRESS_STAGE")
    private String progressStage;

    @Column(name = "PROGRESS")
    private Integer progress;

    @Column(name = "ITEMS_PROCESSED")
    private Integer itemsProcessed;

    @Column(name = "ITEMS_TOTAL")
    private Integer itemsTotal;

    @Column(name = "IS_READY")
    private Character isReady = 'N';

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "REQUEST_TIME")
    private LocalDateTime requestTime;

    @Column(name = "READY_TIME")
    private LocalDateTime readyTime;

    @Lob
    @Column(name = "REPORT_SUMMARY")
    private String reportSummary;

    public boolean getIsReady() {
        return isReady != null && isReady.equals('Y');
    }
    public void setIsReady(boolean isReady) {
        this.isReady = isReady ? 'Y' : 'N';
    }

    public UamReportSummary getReportSummary() {
        if (reportSummary == null) {
            return null;
        }
        try {
            return objectMapper.readValue(reportSummary, UamReportSummary.class);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    public void setReportSummary(UamReportSummary reportSummary) {
        if (reportSummary == null) {
            this.reportSummary = null;
        } else {
            try {
                this.reportSummary = objectMapper.writeValueAsString(reportSummary);
            } catch (JsonProcessingException ignored) {
                this.reportSummary = null;
            }
        }
    }

}
