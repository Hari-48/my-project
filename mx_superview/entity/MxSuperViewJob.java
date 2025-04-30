package com.finsurge.tmr_portal.mx_superview.entity;

import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="UAM_MX_SUPER_VIEW_JOB")
public @Data class MxSuperViewJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "CREATED")
    private LocalDateTime created;

    @Column(name = "STOPPED")
    private LocalDateTime stopped;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOG_TYPE")
    private MxJobLogType logType;

    @Column(name = "LOG_MESSAGE")
    private String logMessage;

}

