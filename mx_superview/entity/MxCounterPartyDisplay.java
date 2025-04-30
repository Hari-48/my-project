package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "UAM_MX_COUNTER_PARTY_DISPLAY",indexes = {
        @Index(name = "IDX_UAM_DIS_CPTY_LABEL", columnList = "COUNTERPART_LABEL"),
        @Index(name = "IDX_UAM_DIS_REF", columnList = "REF"),
        @Index(name = "IDX_UAM_DIS_SETTL_TYPE", columnList = "SETTL_TYPE"),
        @Index(name = "IDX_UAM_DIS_PREVIOUS", columnList = "PREVIOUS"),
        @Index(name = "IDX_UAM_DIS_NEXT", columnList = "NEXT"),
        @Index(name = "IDX_UAM_DIS_MULTIPLE", columnList = "MULTIPLE"),
        @Index(name = "IDX_UAM_DIS_AMEND", columnList = "AMEND"),
        @Index(name = "IDX_UAM_DIS_NOVO", columnList = "NOSTRO_VOSTRO"),
        @Index(name = "IDX_UAM_DIS_CRDE", columnList = "CREDIT_DEBIT"),
        @Index(name = "IDX_UAM_DIS_NATURE", columnList = "NATURE"),
        @Index(name = "IDX_UAM_DIS_INS_DATE", columnList = "INS_DATE"),
        @Index(name = "IDX_UAM_DIS_MOD_DATE", columnList = "MOD_DATE"),
        @Index(name = "IDX_UAM_DIS_START_DT", columnList = "START_DT"),
        @Index(name = "IDX_UAM_DIS_END_DT", columnList = "END_DT"),
        @Index(name = "IDX_UAM_DIS_INS_TIME", columnList = "INS_TIME"),
        @Index(name = "IDX_UAM_DIS_MOD_TIME", columnList = "MOD_TIME"),
        @Index(name = "IDX_UAM_DIS_COUNTPART", columnList = "COUNTPART"),
        @Index(name = "IDX_UAM_DIS_ENTITY", columnList = "ENTITY"),
        @Index(name = "IDX_UAM_DIS_TRADE_SECTION", columnList = "TRADE_SECTION"),
        @Index(name = "IDX_UAM_DIS_FAMILY", columnList = "FAMILY"),
        @Index(name = "IDX_UAM_DIS_TRN_GROUP", columnList = "TRN_GROUP"),
        @Index(name = "IDX_UAM_DIS_TYPE", columnList = "TYPE"),
        @Index(name = "IDX_UAM_DIS_INSTRUMENT", columnList = "INSTRUMENT"),
        @Index(name = "IDX_UAM_DIS_CURRENCY", columnList = "CURRENCY"),
        @Index(name = "IDX_UAM_DIS_OD_CURRENCY", columnList = "OD_CURRENCY"),
        @Index(name = "IDX_UAM_DIS_CODE", columnList = "CODE"),
        @Index(name = "IDX_UAM_DIS_DISPLAYUSER", columnList = "DISPLAYUSER"),
        @Index(name = "IDX_UAM_DIS_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_UAM_DIS_MARKET", columnList = "MARKET"),
        @Index(name = "IDX_UAM_DIS_CLEAR_CENTER", columnList = "CLEAR_CENTER"),
        @Index(name = "IDX_UAM_DIS_SETTLE_METHOD", columnList = "SETTLE_METHOD"),
        @Index(name = "IDX_UAM_DIS_COMMENTS", columnList = "COMMENTS"),
        @Index(name = "IDX_UAM_DIS_TYPOLOGY", columnList = "TYPOLOGY"),
        @Index(name = "IDX_UAM_DIS_USAGE", columnList = "SI_USAGE"),
        @Index(name = "IDX_UAM_DIS_LEGAL_ENTITY", columnList = "LEGAL_ENTITY"),
        @Index(name = "IDX_UAM_DIS_PROCESS_ENTITY", columnList = "PROCESS_ENTITY"),
        @Index(name = "IDX_UAM_DIS_FLOW_TYPOLOGY0", columnList = "FLOW_TYPOLOGY0"),
        @Index(name = "IDX_UAM_DIS_FLOW_TYPOLOGY1", columnList = "FLOW_TYPOLOGY1"),
        @Index(name = "IDX_UAM_DIS_FLOW_TYPOLOGY2", columnList = "FLOW_TYPOLOGY2"),
        @Index(name = "IDX_UAM_DIS_FLOW_TYPOLOGY3", columnList = "FLOW_TYPOLOGY3"),
        @Index(name = "IDX_UAM_DIS_FLOW_TYPOLOGY4", columnList = "FLOW_TYPOLOGY4"),
        @Index(name = "IDX_UAM_DIS_CUSTOM_INFO", columnList = "CUSTOM_INFO"),
        @Index(name = "IDX_UAM_DIS_PHYSCIAL_PRODUCT", columnList = "PHYSCIAL_PRODUCT"),
        @Index(name = "IDX_UAM_DIS_LOCATION", columnList = "LOCATION"),
        @Index(name = "IDX_UAM_DIS_CLEARER", columnList = "CLEARER"),
        @Index(name = "IDX_UAM_DIS_PORTFOLIO", columnList = "PORTFOLIO"),
        @Index(name = "IDX_UAM_DIS_STRATEGY", columnList = "STRATEGY"),
        @Index(name = "IDX_UAM_DIS_PROCCESSING_AREA", columnList = "PROCCESSING_AREA"),
        @Index(name = "IDX_UAM_DIS_VOSTRO_SERVICE", columnList = "VOSTRO_SERVICE"),
        @Index(name = "IDX_UAM_DIS_AGREEMENT", columnList = "AGREEMENT"),
        @Index(name = "IDX_UAM_DIS_AGREEMENT_TYPE", columnList = "AGREEMENT_TYPE"),
        @Index(name = "IDX_UAM_DIS_GOVERNING_LAW", columnList = "GOVERNING_LAW"),
        @Index(name = "IDX_UAM_DIS_CANCELLED", columnList = "CANCELLED"),
        @Index(name = "IDX_UAM_DIS_REP_DATE", columnList = "REP_DATE")
})
public @Data class MxCounterPartyDisplay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "COUNTERPART_LABEL")
    private String cptyLable;

    @Column(name = "REF")
    private Long ref;

    @Column(name = "SETTL_TYPE")
    private String settlType;

    @Column(name = "PREVIOUS")
    private Long previous;

    @Column(name = "NEXT")
    private Long next;

    @Column(name = "MULTIPLE")
    private String multiple;

    @Column(name = "AMEND")
    private Long amend;

    @Column(name = "NOSTRO_VOSTRO")
    private String novo;

    @Column(name = "CREDIT_DEBIT")
    private String crde;

    @Column(name = "NATURE")
    private String nature;

    @Column(name = "INS_DATE")
    private LocalDate insDate;

    @Column(name = "MOD_DATE")
    private LocalDate modDate;

    @Column(name = "START_DT")
    private LocalDate startDt;

    @Column(name = "END_DT")
    private LocalDate endDt;

    @Column(name = "INS_TIME",length = 20)
    private String insTime;

    @Column(name = "MOD_TIME",length = 20)
    private String modTime;

    @Column(name = "COUNTPART")
    private String countPart;

    @Column(name = "ENTITY")
    private String entity;

    @Column(name = "TRADE_SECTION")
    private String tradeSection;

    @Column(name = "FAMILY")
    private String family;

    @Column(name = "TRN_GROUP")
    private String trnGroup;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "INSTRUMENT")
    private String instrument;

    @Column(name = "CURRENCY")
    private String currency;

    @Column(name = "OD_CURRENCY")
    private String odCurrency;

    @Column(name = "CODE")
    private String code;

    @Column(name = "DISPLAYUSER")
    private String displayUser;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "MARKET")
    private String market;

    @Column(name = "CLEAR_CENTER")
    private String clearCenter;

    @Column(name = "SETTLE_METHOD")
    private String settleMethod;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "TYPOLOGY")
    private String typology;

    @Column(name = "SI_USAGE")
    private String usage;

    @Column(name = "LEGAL_ENTITY")
    private String legalEntity;

    @Column(name = "PROCESS_ENTITY")
    private String processEntity;

    @Column(name = "FLOW_TYPOLOGY0")
    private String flowTypology0;

    @Column(name = "FLOW_TYPOLOGY1")
    private String flowTypology1;

    @Column(name = "FLOW_TYPOLOGY2")
    private String flowTypology2;

    @Column(name = "FLOW_TYPOLOGY3")
    private String flowTypology3;

    @Column(name = "FLOW_TYPOLOGY4")
    private String flowTypology4;

    @Column(name = "CUSTOM_INFO")
    private String customInfo;

    @Column(name = "PHYSCIAL_PRODUCT")
    private String physicalProduct;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "CLEARER")
    private String clearer;

    @Column(name = "PORTFOLIO")
    private String portfolio;

    @Column(name = "STRATEGY")
    private String strategy;

    @Column(name = "PROCCESSING_AREA")
    private String proccessingArea;

    @Column(name = "VOSTRO_SERVICE")
    private String vostroService;

    @Column(name = "AGREEMENT")
    private String agreement;

    @Column(name = "AGREEMENT_TYPE")
    private String agreementType;

    @Column(name = "GOVERNING_LAW")
    private String governingLaw;

    @Column(name = "CANCELLED")
    private String cancelled;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

    @Column(name = "SYS_DATE")
    private LocalDate sysDate;

    @Column(name = "JOB_ID")
    private Long jobID;

}

