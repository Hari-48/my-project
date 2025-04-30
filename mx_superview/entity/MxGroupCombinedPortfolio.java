package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "UAM_MX_GROUP_COMBINED_PORTFOLIO", indexes = {
        @Index(name = "IDX_UAM_COMB_PORT_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_COMB_PORT_USRGROUP", columnList = "USRGROUP"),
        @Index(name = "IDX_UAM_COMB_PORT_COMPF_LBL", columnList = "COMPF_LBL"),
        @Index(name = "IDX_UAM_COMB_PORT_UNIT", columnList = "UNIT"),
        @Index(name = "IDX_UAM_COMB_PORT_UNIT_TYPE", columnList = "UNIT_TYPE"),
        @Index(name = "IDX_UAM_COMB_USRGROUP_REP_DATE",columnList = "USRGROUP,REP_DATE"),
        @Index(name = "IDX_UAM_COMB_PRIMARY_KEY",columnList = "REP_DATE,USRGROUP,COMPF_LBL,UNIT")
})
public @Data class MxGroupCombinedPortfolio {

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

    @Column(name = "USRGROUP")
    private String usrGroup;

    @Column(name = "COMPF_LBL")
    public String compfLbl;

    @Column(name = "UNIT")
    public String unit;

    @Column(name = "UNIT_TYPE")
    public String unitType;

    @Transient
    @Lob
    private String diff;


}
