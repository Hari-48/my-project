package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_NAVIGATION_TMPL", indexes = {
        @Index(name = "IDX_UAM_NAV_TMPL_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_NAV_TMPL_NAVIGATION_TMPL", columnList = "NAVIGATION_TMPL"),
        @Index(name = "IDX_UAM_NAV_TMPL_DESCRIPTION", columnList = "DESCRIPTION"),
        @Index(name = "IDX_UAM_NAV_TMPL_GROUP_LABEL", columnList = "GROUP_LABEL")
})
public @Data class MxNavigationTmpl {

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

    @Column(name = "NAVIGATION_TMPL")
    private String navigationTmpl;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "GROUP_LABEL")
    private String groupLabel;
}
