package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_USER_GROUP_ACCESS_RIGHT", indexes = {
        @Index(name = "IDX_UAM_USR_GRP_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_USR_GRP_USER_NAME", columnList = "USER_NAME"),
        @Index(name = "IDX_UAM_USR_GRP_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_USR_USER_NAME_REP_DATE_GROUP_LABEL",columnList = "USER_NAME,REP_DATE,GROUP_LABEL")
})
public @Data class MXUserGroupAccessRgt {

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

    @Column(name = "USER_DESC")
    private String userDesc;

    @Column(name = "GROUP_LABEL")
    private String groupLabel;

    @Column(name = "GRP_ROLE_STR")
    private String grpRoleStr;

    @Column(name = "GRP_TYPE_STR")
    private String grpTypeStr;

    @Column(name = "GRP_DESC")
    private String grpDesc;
}
