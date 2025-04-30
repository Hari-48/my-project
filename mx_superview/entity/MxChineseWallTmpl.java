package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_CHINESE_WALL_TMPL",indexes = {
        @Index(name = "IDX_UAM_REP_DATE",columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_TEMPLATE_LABEL",columnList = "TEMPLATE_LABEL"),
        @Index(name = "IDX_UAM_COUNTERPART_LABEL",columnList = "COUNTERPART_LABEL"),
        @Index(name = "IDX_UAM_COUNTERPART_DESCRIPTION",columnList = "COUNTERPART_DESCRIPTION"),
        @Index(name = "IDX_UAM_CHINESEWALL_TEMPLATE_LABEL_REP_DATE",columnList = "TEMPLATE_LABEL,REP_DATE"),
        @Index(name = "IDX_UAM_CHINESEWALL_PRIMARY",columnList = "TEMPLATE_LABEL,REP_DATE,COUNTERPART_LABEL"),
        @Index(name = "IDX_UAM_CHINESEWALL_PRIMARY_MATCHED",columnList = "TEMPLATE_LABEL,REP_DATE,COUNTERPART_LABEL,COUNTERPART_DESCRIPTION")

})
public @Data class MxChineseWallTmpl {

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

    @Column(name = "TEMPLATE_LABEL")
    private String templateLabel;

    @Column(name = "COUNTERPART_LABEL")
    public String counterpartLabel;

    @Column(name = "COUNTERPART_DESCRIPTION")
    public String counterpartDescription;

    @Column(name = "iDay")
    private int iDay;

    @Column(name = "iMonth")
    private int iMonth;

    @Column(name = "iYear")
    private int iYear;


    @Transient
    @Lob
    private String diff;


}
