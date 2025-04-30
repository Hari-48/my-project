package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_OPERATION_RIGHTS", indexes = {
        @Index(name = "IDX_UAM_OPER_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_OPER_EVENT_TYPE", columnList = "EVENT_TYPE"),
        @Index(name = "IDX_UAM_OPER_OPER_RGTS", columnList = "OPER_RGTS"),
        @Index(name = "IDX_UAM_OPER_TEMPLATE", columnList = "TEMPLATE"),
        @Index(name = "IDX_UAM_OPER_KEY_LABEL", columnList = "KEY_LABEL"),
        @Index(name = "IDX_UAM_OPER_AVP1", columnList = "AVP1"),
        @Index(name = "IDX_UAM_OPER_AVP2", columnList = "AVP2"),
        @Index(name = "IDX_UAM_OPER_AVP3", columnList = "AVP3"),
        @Index(name = "IDX_UAM_OPER_AVP4", columnList = "AVP4"),
        @Index(name = "IDX_UAM_OPER_FIFO1", columnList = "FIFO1"),
        @Index(name = "IDX_UAM_OPER_FIFO2", columnList = "FIFO2"),
        @Index(name = "IDX_UAM_OPER_FIFO3", columnList = "FIFO3"),
        @Index(name = "IDX_UAM_OPER_FIFO4", columnList = "FIFO4"),
        @Index(name = "IDX_UAM_OPER_REAL_TIME", columnList = "REAL_TIME"),
        @Index(name = "IDX_UAM_OPER_EVT_ACCESS", columnList = "EVT_ACCESS"),
        @Index(name = "IDX_UAM_OPER_EVT_INSERT", columnList = "EVT_INSERT"),
        @Index(name = "IDX_UAM_OPER_EVT_MODIFY", columnList = "EVT_MODIFY"),
        @Index(name = "IDX_UAM_OPER_EVT_DELETE", columnList = "EVT_DELETE"),
        @Index(name = "IDX_UAM_OPER_TEMPLATE_DATE_OPER_RGTS",columnList = "TEMPLATE,REP_DATE,OPER_RGTS"),
        @Index(name = "IDX_UAM_OPER_TEMPLATE_PRIMARY_KEY",columnList = "TEMPLATE,REP_DATE,OPER_RGTS,KEY_LABEL"),

})

public @Data class MxOperationRights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SYS_DATE")
    private LocalDate sysDate;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

    @Column(name = "OPER_RGTS")
    private String operRgts;

    @Column(name = "TEMPLATE")
    private String template;

    @Column(name = "KEY_LABEL")
    public String key;

    @Column(name = "AVP1")
    public String avp1;

    @Column(name = "AVP2")
    public String avp2;

    @Column(name = "AVP3")
    public String avp3;

    @Column(name = "AVP4")
    public String avp4;

    @Column(name = "FIFO1")
    public String fifo1;

    @Column(name = "FIFO2")
    public String fifo2;

    @Column(name = "FIFO3")
    public String fifo3;

    @Column(name = "FIFO4")
    public String fifo4;

    @Column(name = "REAL_TIME")
    public String realTime;

    @Column(name = "EVENT_TYPE")
    public String eventType;

    @Column(name = "EVT_ACCESS")
    public String evtAccess;

    @Column(name = "EVT_INSERT")
    public String evtInsert;

    @Column(name = "EVT_MODIFY")
    public String evtModify;

    @Column(name = "EVT_DELETE")
    public String evtDelete;

    @Transient
    @Lob
    private String diff;
}

