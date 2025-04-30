package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_GROUP_NAV_RIGHTS", indexes = {
        @Index(name = "IDX_UAM_NAV_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_NAV_RIGHTS", columnList = "RIGHTS"),
        @Index(name = "IDX_UAM_NAV_PATH", columnList = "PATH"),
        @Index(name = "IDX_UAM_NAV_TEMPLATE", columnList = "TEMPLATE"),
        @Index(name = "IDX_UAM_NAV_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_NAV_MENU", columnList = "MENU"),
        @Index(name = "IDX_UAM_NAV_PATH_LABEL", columnList = "PATH_LABEL"),
        @Index(name = "IDX_UAM_NAV_PATH_REST", columnList = "PATH_REST"),
        @Index(name = "IDX_UAM_NAV_SUBMENU_1", columnList = "SUBMENU_1"),
        @Index(name = "IDX_UAM_NAV_SUBMENU_2", columnList = "SUBMENU_2"),
        @Index(name = "IDX_UAM_NAV_SUBMENU_3", columnList = "SUBMENU_3"),
        @Index(name = "IDX_UAM_NAV_SUBMENU_4", columnList = "SUBMENU_4"),
        @Index(name = "IDX_UAM_NAV_SUBMENU_5", columnList = "SUBMENU_5"),
        @Index(name = "IDX_UAM_NAV_COMMENTS", columnList = "COMMENTS"),
        @Index(name = "IDX_UAM_NAV_GROUP_LABEL_REP_DATE_TEMPLATE",columnList = "GROUP_LABEL,REP_DATE,TEMPLATE")
//        @Index(name = "IDX_UAM_NAV_PRIMARY_KEYS",columnList = "REP_DATE,GROUP_LABEL,TEMPLATE,COMMENTS,RIGHTS,MENU,PATH")

})
public @Data class MxGroupNavigationRight {

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

    @Column(name = "GROUP_LABEL")
    private String groupLabel;

    @Column(name = "RIGHTS")
    public String rights;

    @Column(name = "MENU")
    public String menu;

    @Column(name = "PATH")
    public String path;

    @Column(name = "PATH_LABEL")
    public String pathLabel;

    @Column(name = "PATH_REST")
    public String pathRest;

    @Column(name = "SUBMENU_1")
    public String submenu1;

    @Column(name = "SUBMENU_2")
    public String submenu2;

    @Column(name = "SUBMENU_3")
    public String submenu3;

    @Column(name = "SUBMENU_4")
    public String submenu4;

    @Column(name = "SUBMENU_5")
    public String submenu5;

    @Column(name = "TEMPLATE")
    public String template;

    @Column(name = "COMMENTS")
    public String comments;

    @Transient
    @Lob
    private String diff;


}
