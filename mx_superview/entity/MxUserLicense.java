package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name ="UAM_MX_USER_LICENSE", indexes = {
        @Index(name = "IDX_UAM_USR_LIC_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_USR_LIC_USER_NAME", columnList = "USER_NAME"),
        @Index(name = "IDX_UAM_USR_LIC_LICENSE_CAT", columnList = "LICENSE_CAT"),
        @Index(name = "IDX_UAM_USR_USER_NAME_REP_DATE_LICENSE_CAT",columnList = "USER_NAME,REP_DATE,LICENSE_CAT")
})
public @Data
class MxUserLicense {

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

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "LICENSE_CAT")
    private String licenseCatName;
}
