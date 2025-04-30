package com.finsurge.tmr_portal.mx_superview.entity;

import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.ReportType;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "UAM_DATA_IMPORT_JOBS",
        indexes = {
                @Index(name = "IDX_UAM_DATA_JOBS_FILE_NAME", columnList = "FILE_NAME"),
                @Index(name = "IDX_UAM_DATA_JOBS_DATA_TYPE", columnList = "DATA_TYPE"),
                @Index(name = "IDX_UAM_DATA_JOBS_REP_DATE", columnList = "REP_DATE")
        })
public @Data class DataImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "FILE_LAST_MODIFIED")
    private Long fileLastModifed;

    @Column(name = "RECORDS_SOURCE")
    private Long recordsInSource;

    @Column(name = "RECORDS_IMPORTED")
    private Long recordsImported;

    @Column(name = "DATA_TYPE")
    private String dataType;

    @Column(name = "IMPORT_TS")
    private LocalDateTime importTimestamp;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "JOB_STATUS")
    private MxJobLogType jobStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPORT_TYPE")
    private ReportType reportType;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

//    @Column(name = "ARCHIVED")
     @Column(name = "PURGED")
    private Character purged = 'N';

    @Column(name = "TABLE_NAME")
    private String tableName;

    @Column(name = "AUTO_LOAD")
    private Character autoLoad = 'Y';

    public boolean getPurged() {
        return purged != null && purged.equals('Y');
    }
    public void setPurged(boolean purged) {
        this.purged = purged ? 'Y' : 'N';
    }

    public boolean getAutoLoad() {
        return autoLoad != null && autoLoad == 'Y';
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad ? 'Y' : 'N';
    }
}

