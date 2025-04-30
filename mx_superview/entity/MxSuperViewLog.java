package com.finsurge.tmr_portal.mx_superview.entity;


import com.finsurge.tmr_portal.mx_superview.models.MxJobLogType;
import com.finsurge.tmr_portal.mx_superview.models.MxLogType;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "UAM_MX_SUPER_VIEW_LOG", indexes = {
        @Index(name = "IDX_UAM_LOG_ROOT_JOB_ID", columnList = "ROOT_JOB_ID")
})
public @Data class MxSuperViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ROOT_JOB_ID")
    private Long rootJobId;

    @Column(name = "CREATED")
    private LocalDateTime created;

    @Column(name = "STOPPED")
    private LocalDateTime stopped;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "LOG_TYPE")
//    private MxJobLogType jobLogType;

    @Enumerated(EnumType.STRING)
    @Column(name = "MX_LOG_TYPE")
    private MxLogType logType;

    @Lob
    @Column(name = "MX_LOG_MESSAGE")
    private String logMessage;

//    @Lob
//    @Column(name = "LOG_MESSAGE")
//    private String logMessage;

}

