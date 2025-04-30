package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_USER_POLICY", indexes = {
        @Index(name = "IDX_UAM_USR_POL_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_USR_POL_M_NAME", columnList = "M_NAME")
})
public @Data class MxUserPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SYS_DATE")
    private LocalDate sysDate;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

    @Column(name = "M_REFERENCE")
    private int mReference;

    @Column(name = "M_NAME")
    private String mName;

    @Column(name = "PWD_USEBY")
    private String pwdUserBy;

    @Column(name = "FRC_PWD_CHG")
    private String frcPwdChg;

    @Column(name = "PRT_PWD_USE")
    private String prtPwdUse;

    @Column(name = "REUSE_OPWD")
    private String reuseOPWD;

    @Column(name = "PRT_PWD_CHG")
    private String prtPwdChg;

    @Column(name = "DISP_LASTLOG")
    private String dispLastLog;

    @Column(name = "AUTO_SUSP")
    private String autoSUSP;

    @Column(name = "AUTO_LOCK")
    private String autoLock;

    @Column(name = "MAX_PWD")
    private String maxPwd;

    @Column(name = "LOCK_UACC")
    private String lockUacc;

    @Column(name = "FAIL_LOCK")
    private String failLock;

    @Column(name = "DEFINE_PWD")
    private String definePwd;

    @Column(name = "DIFF_NAME")
    private String diffName;

    @Column(name = "REVRSE_NAME")
    private String revrseName;

    @Column(name = "MIN_CHAR")
    private String minChar;

    @Column(name = "MAX_CHAR")
    private String maxChar;

    @Column(name = "DIFF_CHAR")
    private String diffChar;

    @Column(name = "ALPHBT_CHAR")
    private String alphbtChar;

    @Column(name = "NALPHBT_CHAR")
    private String nalphbtChar;

    @Column(name = "PCI_CHAR")
    private String pciChar;

    @Column(name = "MIN_UCHAR")
    private String minUchar;

    @Column(name = "MIN_LCHAR")
    private String minLchar;

    @Column(name = "MIN_SCHAR")
    private String minSchar;

    @Column(name = "MIN_NCHAR")
    private String minNchar;

}
