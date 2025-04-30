package com.finsurge.tmr_portal.mx_superview.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor
public @Data
class DataLoaderProcessLog {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long jobId;

    private LocalDateTime created;

    private MxJobLogType processStage;

    private MxLogType logType;

    private String message;

    public DataLoaderProcessLog(Long jobId,
                                MxJobLogType processStage,
                                MxLogType logType,
                                String message) {
        created = LocalDateTime.now();
        this.jobId = jobId;
        this.processStage = processStage;
        this.logType = logType;
        this.message = message;
    }
}
