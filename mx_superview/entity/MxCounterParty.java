package com.finsurge.tmr_portal.mx_superview.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "UAM_MX_COUNTER_PARTY", indexes = {
        @Index(name = "IDX_UAM_COUNTER_REP_DATE", columnList = "REP_DATE"),
        @Index(name = "IDX_UAM_COUNTER_COUNTERPARTY_NAME", columnList = "COUNTERPARTY_NAME"),
        @Index(name = "IDX_UAM_COUNTER_DSP_LABEL", columnList = "DSP_LABEL"),
        @Index(name = "IDX_UAM_COUNTER_FULLNAME", columnList = "FULLNAME"),
        @Index(name = "IDX_UAM_COUNTER_REVISION", columnList = "REVISION"),
        @Index(name = "IDX_UAM_COUNTER_CODE", columnList = "CODE"),
        @Index(name = "IDX_UAM_COUNTER_NAMESPACE", columnList = "NAMESPACE"),
        @Index(name = "IDX_UAM_COUNTER_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_UAM_COUNTER_PARENT", columnList = "PARENT"),
        @Index(name = "IDX_UAM_COUNTER_PARENT_COMPANY", columnList = "PARENT_COMPANY"),
        @Index(name = "IDX_UAM_COUNTER_LEGAL_ENTITY", columnList = "LEGAL_ENTITY"),
        @Index(name = "IDX_UAM_COUNTER_LEGAL_ENTITY_IDENTIFIER", columnList = "LEGAL_ENTITY_IDENTIFIER"),
        @Index(name = "IDX_UAM_COUNTER_CATEGORY", columnList = "CATEGORY"),
        @Index(name = "IDX_UAM_COUNTER_FISCAL_CATEGORY", columnList = "FISCAL_CATEGORY"),
        @Index(name = "IDX_UAM_COUNTER_ADDRESS0", columnList = "ADDRESS0"),
        @Index(name = "IDX_UAM_COUNTER_ADDRESS1", columnList = "ADDRESS1"),
        @Index(name = "IDX_UAM_COUNTER_ADDRESS2", columnList = "ADDRESS2"),
        @Index(name = "IDX_UAM_COUNTER_ADDRESS3", columnList = "ADDRESS3"),
        @Index(name = "IDX_UAM_COUNTER_CITY", columnList = "CITY"),
        @Index(name = "IDX_UAM_COUNTER_POSTCODE", columnList = "POSTCODE"),
        @Index(name = "IDX_UAM_COUNTER_COUNTRY", columnList = "COUNTRY"),
        @Index(name = "IDX_UAM_COUNTER_TEL", columnList = "TEL"),
        @Index(name = "IDX_UAM_COUNTER_FAX", columnList = "FAX"),
        @Index(name = "IDX_UAM_COUNTER_TLX", columnList = "TLX"),
        @Index(name = "IDX_UAM_COUNTER_SWIFT", columnList = "SWIFT"),
        @Index(name = "IDX_UAM_COUNTER_EMAIL", columnList = "EMAIL"),
        @Index(name = "IDX_UAM_COUNTER_VALIDITY_ATTRIBUTE", columnList = "VALIDITY_ATTRIBUTE"),
        @Index(name = "IDX_UAM_COUNTER_WEBENABLED", columnList = "WEBENABLED"),
        @Index(name = "IDX_UAM_COUNTER_AMD_DATE", columnList = "AMD_DATE"),
        @Index(name = "IDX_UAM_COUNTER_AMD_TIME", columnList = "AMD_TIME"),
        @Index(name = "IDX_UAM_COUNTER_CMM_SI_PREFERENCE", columnList = "CMM_SI_PREFERENCE"),
        @Index(name = "IDX_UAM_COUNTER_SI_MODE", columnList = "SI_MODE"),
        @Index(name = "IDX_UAM_COUNTER_CI_MODE", columnList = "CI_MODE"),
        @Index(name = "IDX_UAM_COUNTER_CI_EDITION", columnList = "CI_EDITION"),
        @Index(name = "IDX_UAM_COUNTER_SECTOR", columnList = "SECTOR"),
        @Index(name = "IDX_UAM_COUNTER_SEND_CONFIRMATION", columnList = "SEND_CONFIRMATION"),
        @Index(name = "IDX_UAM_COUNTER_PAYMENT_NETTING", columnList = "PAYMENT_NETTING"),
        @Index(name = "IDX_UAM_COUNTER_NOTIFYING_FIXING", columnList = "NOTIFYING_FIXING"),
        @Index(name = "IDX_UAM_COUNTER_COLLATERAL_AGREEMENT", columnList = "COLLATERAL_AGREEMENT"),
        @Index(name = "IDX_UAM_COUNTER_BANK", columnList = "BANK"),
        @Index(name = "IDX_UAM_COUNTER_BROKER", columnList = "BROKER"),
        @Index(name = "IDX_UAM_COUNTER_CUSTOMER", columnList = "CUSTOMER"),
        @Index(name = "IDX_UAM_COUNTER_FICTIVE", columnList = "FICTIVE"),
        @Index(name = "IDX_UAM_COUNTER_INTERNAL_PARTY", columnList = "INTERNAL_PARTY"),
        @Index(name = "IDX_UAM_COUNTER_OTHER", columnList = "OTHER"),
        @Index(name = "IDX_UAM_COUNTER_GROUP_LABEL", columnList = "GROUP_LABEL"),
        @Index(name = "IDX_UAM_COUNTER_SUBSIDIARY", columnList = "SUBSIDIARY"),
        @Index(name = "IDX_UAM_COUNTER_BRANCH", columnList = "BRANCH"),
        @Index(name = "IDX_UAM_COUNTER_CORPORATE", columnList = "CORPORATE"),
        @Index(name = "IDX_UAM_COUNTER_STATE", columnList = "STATE"),
        @Index(name = "IDX_UAM_COUNTER_CLEARER", columnList = "CLEARER"),
        @Index(name = "IDX_UAM_COUNTER_CLIENT", columnList = "CLIENT"),
        @Index(name = "IDX_UAM_COUNTER_ISSUER", columnList = "ISSUER"),
        @Index(name = "IDX_UAM_COUNTER_AGENT", columnList = "AGENT")
//        @Index(name = "IDX_UAM_COUNTER_LEGALENTITY", columnList = "LEGALENTITY"),
//        @Index(name = "IDX_UAM_COUNTER_REFENTITY", columnList = "REFENTITY"),
//        @Index(name = "IDX_UAM_COUNTER_GARANTOR", columnList = "GARANTOR"),
//        @Index(name = "IDX_UAM_COUNTER_OPERATOR", columnList = "OPERATOR"),
//        @Index(name = "IDX_UAM_COUNTER_CUSTODIAN", columnList = "CUSTODIAN"),
//        @Index(name = "IDX_UAM_COUNTER_FUND", columnList = "FUND"),
//        @Index(name = "IDX_UAM_COUNTER_HEDGEFUND", columnList = "HEDGEFUND"),
//        @Index(name = "IDX_UAM_COUNTER_CLS_ELIGIBLE", columnList = "CLS_ELIGIBLE"),
//        @Index(name = "IDX_UAM_COUNTER_CCIF_FLAG", columnList = "CCIF_FLAG"),
//        @Index(name = "IDX_UAM_COUNTER_CHATS_ELIGIBLE", columnList = "CHATS_ELIGIBLE"),
//        @Index(name = "IDX_UAM_COUNTER_CUSTOMER_CODE", columnList = "CUSTOMER_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_SECTORNAME", columnList = "SECTORNAME"),
//        @Index(name = "IDX_UAM_COUNTER_LEGAL_NAME", columnList = "LEGAL_NAME"),
//        @Index(name = "IDX_UAM_COUNTER_ESTABLISHMENT_DATE", columnList = "ESTABLISHMENT_DATE"),
//        @Index(name = "IDX_UAM_COUNTER_BUMIPUTRA", columnList = "BUMIPUTRA"),
//        @Index(name = "IDX_UAM_COUNTER_LEGAL_ID", columnList = "LEGAL_ID"),
//        @Index(name = "IDX_UAM_COUNTER_LEGALID_TYPE", columnList = "LEGALID_TYPE"),
//        @Index(name = "IDX_UAM_COUNTER_NATIONALITY", columnList = "NATIONALITY"),
//        @Index(name = "IDX_UAM_COUNTER_REGION", columnList = "REGION"),
//        @Index(name = "IDX_UAM_COUNTER_RENTAS_CODE", columnList = "RENTAS_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_CALCULATION_AGENT", columnList = "CALCULATION_AGENT"),
//        @Index(name = "IDX_UAM_COUNTER_DTCY_ID", columnList = "DTCY_ID"),
//        @Index(name = "IDX_UAM_COUNTER_CUSTODY", columnList = "CUSTODY"),
//        @Index(name = "IDX_UAM_COUNTER_EXPIREIN_MORNING", columnList = "EXPIREIN_MORNING"),
//        @Index(name = "IDX_UAM_COUNTER_CLEARING_BROKER", columnList = "CLEARING_BROKER"),
//        @Index(name = "IDX_UAM_COUNTER_POSTAL_CODE", columnList = "POSTAL_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_FECL3PTY_PARENT", columnList = "FECL3PTY_PARENT"),
//        @Index(name = "IDX_UAM_COUNTER_RTGS_CODE", columnList = "RTGS_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_CITAD_CODE", columnList = "CITAD_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_AAA_FA", columnList = "AAA_FA"),
//        @Index(name = "IDX_UAM_COUNTER_AAA_BRN", columnList = "AAA_BRN"),
//        @Index(name = "IDX_UAM_COUNTER_RM_REP", columnList = "RM_REP"),
//        @Index(name = "IDX_UAM_COUNTER_RM_SBU", columnList = "RM_SBU"),
//        @Index(name = "IDX_UAM_COUNTER_LENDING_UNIT", columnList = "LENDING_UNIT"),
//        @Index(name = "IDX_UAM_COUNTER_FX_SEGREGATION", columnList = "FX_SEGREGATION"),
//        @Index(name = "IDX_UAM_COUNTER_FINANCE_RATING", columnList = "FINANCE_RATING"),
//        @Index(name = "IDX_UAM_COUNTER_SIBS_SGCIF", columnList = "SIBS_SGCIF"),
//        @Index(name = "IDX_UAM_COUNTER_SIBS_LBCIF", columnList = "SIBS_LBCIF"),
//        @Index(name = "IDX_UAM_COUNTER_SIBS_LDCIF", columnList = "SIBS_LDCIF"),
//        @Index(name = "IDX_UAM_COUNTER_MAS_SECTOR", columnList = "MAS_SECTOR"),
//        @Index(name = "IDX_UAM_COUNTER_BNM_SECTOR", columnList = "BNM_SECTOR"),
//        @Index(name = "IDX_UAM_COUNTER_BTP_CODE", columnList = "BTP_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_INTERNAL_SECTOR", columnList = "INTERNAL_SECTOR"),
//        @Index(name = "IDX_UAM_COUNTER_MT202_COV", columnList = "MT202_COV"),
//        @Index(name = "IDX_UAM_COUNTER_DVP_FOP", columnList = "DVP_FOP"),
//        @Index(name = "IDX_UAM_COUNTER_BIZSECTOR", columnList = "BIZSECTOR"),
//        @Index(name = "IDX_UAM_COUNTER_OVERNIGHT_LENDING", columnList = "OVERNIGHT_LENDING"),
//        @Index(name = "IDX_UAM_COUNTER_REUTERS_CODE", columnList = "REUTERS_CODE"),
//        @Index(name = "IDX_UAM_COUNTER_REUTERS_INDICATOR", columnList = "REUTERS_INDICATOR"),
//        @Index(name = "IDX_UAM_COUNTER_DTCC_LEI", columnList = "DTCC_LEI"),
//        @Index(name = "IDX_UAM_COUNTER_DTCC_MASKED", columnList = "DTCC_MASKED"),
//        @Index(name = "IDX_UAM_COUNTER_DTCC_NAMESPACE", columnList = "DTCC_NAMESPACE"),
//        @Index(name = "IDX_UAM_COUNTER_DTCC_SWIFTCODE", columnList = "DTCC_SWIFTCODE"),
//        @Index(name = "IDX_UAM_COUNTER_CUSTOMER_CLASS", columnList = "CUSTOMER_CLASS"),
//        @Index(name = "IDX_UAM_COUNTER_TIN_NO", columnList = "TIN_NO"),
//        @Index(name = "IDX_UAM_COUNTER_GIIN_NO", columnList = "GIIN_NO"),
//        @Index(name = "IDX_UAM_COUNTER_SECONDARY_ID", columnList = "SECONDARY_ID"),
//        @Index(name = "IDX_UAM_COUNTER_PASSPORT_DOE", columnList = "PASSPORT_DOE"),
//        @Index(name = "IDX_UAM_COUNTER_W8BEN", columnList = "W8BEN"),
//        @Index(name = "IDX_UAM_COUNTER_W8BEN_DOE", columnList = "W8BEN_DOE"),
//        @Index(name = "IDX_UAM_COUNTER_GIIN", columnList = "GIIN"),
//        @Index(name = "IDX_UAM_COUNTER_BO1", columnList = "BO1"),
//        @Index(name = "IDX_UAM_COUNTER_BO2", columnList = "BO2"),
//        @Index(name = "IDX_UAM_COUNTER_BO3", columnList = "BO3"),
//        @Index(name = "IDX_UAM_COUNTER_BO4", columnList = "BO4"),
//        @Index(name = "IDX_UAM_COUNTER_FUND_OWNR", columnList = "FUND_OWNR"),
//        @Index(name = "IDX_UAM_COUNTER_POB", columnList = "POB"),
//        @Index(name = "IDX_UAM_COUNTER_SI", columnList = "SI"),
//        @Index(name = "IDX_UAM_COUNTER_POA", columnList = "POA"),
//        @Index(name = "IDX_UAM_COUNTER_HOLD_MAIL", columnList = "HOLD_MAIL"),
//        @Index(name = "IDX_UAM_COUNTER_CATEGORY0", columnList = "CATEGORY0"),
//        @Index(name = "IDX_UAM_COUNTER_CATEGORY1", columnList = "CATEGORY1"),
//        @Index(name = "IDX_UAM_COUNTER_CATEGORY2", columnList = "CATEGORY2"),
//        @Index(name = "IDX_UAM_COUNTER_CATEGORY3", columnList = "CATEGORY3"),
//        @Index(name = "IDX_UAM_COUNTER_BNM_ENTITYCODE", columnList = "BNM_ENTITYCODE"),
//        @Index(name = "IDX_UAM_COUNTER_BNM_INST_SECCODE", columnList = "BNM_INST_SECCODE"),
//        @Index(name = "IDX_UAM_COUNTER_BNM_RACECODE", columnList = "BNM_RACECODE"),
//        @Index(name = "IDX_UAM_COUNTER_MSIC", columnList = "MSIC"),
//        @Index(name = "IDX_UAM_COUNTER_T24_ID", columnList = "T24_ID"),
//        @Index(name = "IDX_UAM_COUNTER_OAD_ID", columnList = "OAD_ID"),
//        @Index(name = "IDX_UAM_COUNTER_OAD_STATUS", columnList = "OAD_STATUS"),
//        @Index(name = "IDX_UAM_COUNTER_OAD_CITY", columnList = "OAD_CITY"),
//        @Index(name = "IDX_UAM_COUNTER_STREET", columnList = "STREET"),
//        @Index(name = "IDX_UAM_COUNTER_OAD_POSTCODE", columnList = "OAD_POSTCODE"),
//        @Index(name = "IDX_UAM_COUNTER_OAD_COUNTRY", columnList = "OAD_COUNTRY"),
//        @Index(name = "IDX_UAM_COUNTER_CONTACT1", columnList = "CONTACT1"),
//        @Index(name = "IDX_UAM_COUNTER_CONTACT2", columnList = "CONTACT2"),
//        @Index(name = "IDX_UAM_COUNTER_TYPE", columnList = "TYPE"),
//        @Index(name = "IDX_UAM_COUNTER_OAD_SWIFT", columnList = "OAD_SWIFT"),
//        @Index(name = "IDX_UAM_COUNTER_CHANNEL", columnList = "CHANNEL"),
//        @Index(name = "IDX_UAM_COUNTER_MGR_ID", columnList = "MGR_ID"),
//        @Index(name = "IDX_UAM_COUNTER_MGR_NAME", columnList = "MGR_NAME"),
//        @Index(name = "IDX_UAM_COUNTER_DESCRIPTION", columnList = "DESCRIPTION"),
//        @Index(name = "IDX_UAM_COUNTER_PATH", columnList = "PATH"),
//        @Index(name = "IDX_UAM_COUNTER_COMMENT1", columnList = "COMMENT1"),
//        @Index(name = "IDX_UAM_COUNTER_COMMENT2", columnList = "COMMENT2"),
//        @Index(name = "IDX_UAM_COUNTER_COMMENT3", columnList = "COMMENT3"),
//        @Index(name = "IDX_UAM_COUNTER_COMMENT4", columnList = "COMMENT4"),
//        @Index(name = "IDX_UAM_COUNTER_COMMENT5", columnList = "COMMENT5"),
//        @Index(name = "IDX_UAM_COUNTER_COMMENT6", columnList = "COMMENT6"),
//        @Index(name = "IDX_UAM_COUNTER_LEI_LEGALNAME", columnList = "LEI_LEGALNAME"),
//        @Index(name = "IDX_UAM_COUNTER_LEGAL_FORM", columnList = "LEGAL_FORM"),
//        @Index(name = "IDX_UAM_COUNTER_LEI_STATUS", columnList = "LEI_STATUS"),
//        @Index(name = "IDX_UAM_COUNTER_IMMEDIATE_PARENT", columnList = "IMMEDIATE_PARENT"),
//        @Index(name = "IDX_UAM_COUNTER_ULTIMATE_PARENT", columnList = "ULTIMATE_PARENT"),
//        @Index(name = "IDX_UAM_COUNTER_ADDRESS_ID", columnList = "ADDRESS_ID"),
//        @Index(name = "IDX_UAM_COUNTER_LAST_UPDATE", columnList = "LAST_UPDATE"),
//        @Index(name = "IDX_UAM_COUNTER_DISABLED", columnList = "DISABLED"),
//        @Index(name = "IDX_UAM_COUNTER_ASSIGNMENT", columnList = "ASSIGNMENT"),
//        @Index(name = "IDX_UAM_COUNTER_IRN_ID", columnList = "IRN_ID"),
//        @Index(name = "IDX_UAM_COUNTER_TRN_LABEL", columnList = "TRN_LABEL"),
//        @Index(name = "IDX_UAM_COUNTER_COUNTRPART", columnList = "COUNTRPART"),
//        @Index(name = "IDX_UAM_COUNTER_FILTER", columnList = "FILTER"),
//        @Index(name = "IDX_UAM_COUNTER_FILTER_DESC", columnList = "FILTER_DESC"),
//        @Index(name = "IDX_UAM_COUNTER_STARTDATE", columnList = "STARTDATE"),
//        @Index(name = "IDX_UAM_COUNTER_ENDDATE", columnList = "ENDDATE"),
//        @Index(name = "IDX_UAM_COUNTER_DOCUMENT_TYPE", columnList = "DOCUMENT_TYPE"),
//        @Index(name = "IDX_UAM_COUNTER_MASTER_AGREEMENT", columnList = "MASTER_AGREEMENT"),
//        @Index(name = "IDX_UAM_COUNTER_RECIPIENT", columnList = "RECIPIENT"),
//        @Index(name = "IDX_UAM_COUNTER_ADDRESS", columnList = "ADDRESS"),
//        @Index(name = "IDX_UAM_COUNTER_ADDDRESSCONTACT1", columnList = "ADDDRESSCONTACT1"),
//        @Index(name = "IDX_UAM_COUNTER_ADDDRESSCONTACT2", columnList = "ADDDRESSCONTACT2"),
//        @Index(name = "IDX_UAM_COUNTER_LANGUAGE", columnList = "LANGUAGE"),
//        @Index(name = "IDX_UAM_COUNTER_CUSTOMINFO", columnList = "CUSTOMINFO"),
//        @Index(name = "IDX_UAM_COUNTER_NOTIFICATION", columnList = "NOTIFICATION"),
//        @Index(name = "IDX_UAM_COUNTER_NEGATIVE_AFFIRMATION", columnList = "NEGATIVE_AFFIRMATION"),
//        @Index(name = "IDX_UAM_COUNTER_CHASING", columnList = "CHASING"),
//        @Index(name = "IDX_UAM_COUNTER_CUSTOMIZATIONS", columnList = "CUSTOMIZATIONS"),
//        @Index(name = "IDX_UAM_COUNTER_TRADERS_NAME", columnList = "TRADERS_NAME"),
//        @Index(name = "IDX_UAM_COUNTER_REGIME", columnList = "REGIME"),
//        @Index(name = "IDX_UAM_COUNTER_SUP_BODY", columnList = "SUP_BODY"),
//        @Index(name = "IDX_UAM_COUNTER_ROLE", columnList = "ROLE"),
//        @Index(name = "IDX_UAM_COUNTER_JUR_DESCRIPTION", columnList = "JUR_DESCRIPTION"),
//        @Index(name = "IDX_UAM_COUNTER_MAIN", columnList = "MAIN")
})
public @Data class MxCounterParty {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "COUNTERPARTY_NAME")
    private String counterPartyName;

    @Column(name = "DSP_LABEL")
    private String dspLabel;

    @Column(name = "FULLNAME")
    private String fullName;

    @Column(name = "REVISION", length = 10)
    private String revision;

    @Column(name = "CODE", length = 12)
    private String code;

    @Column(name = "NAMESPACE", length = 15)
    private String nameSpace;

    @Column(name = "STATUS", length = 10)
    private String status;

    @Column(name = "PARENT", length = 35)
    private String parent;

    @Column(name = "PARENT_COMPANY", length = 35)
    private String parentCompany;

    @Column(name = "LEGAL_ENTITY", length = 25)
    private String legalEntity;

    @Column(name = "LEGAL_ENTITY_IDENTIFIER", length = 25)
    private String legalEntityIdentifier;

    @Column(name = "CATEGORY", length = 20)
    private String category;

    @Column(name = "FISCAL_CATEGORY")
    private String fiscalCategory;

    @Column(name = "ADDRESS0", length = 80)
    private String address0;

    @Column(name = "ADDRESS1", length = 100)
    private String address1;

    @Column(name = "ADDRESS2", length = 100)
    private String address2;

    @Column(name = "ADDRESS3", length = 100)
    private String address3;

    @Column(name = "CITY", length = 80)
    private String city;

    @Column(name = "POSTCODE", length = 20)
    private String postCode;

    @Column(name = "COUNTRY", length = 100)
    private String country;

    @Column(name = "TEL", length = 35)
    private String tel;

    @Column(name = "FAX", length = 35)
    private String fax;

    @Column(name = "TLX", length = 35)
    private String tlx;

    @Column(name = "SWIFT", length = 15)
    private String swift;

    @Column(name = "EMAIL", length = 200)
    private String email;

    @Column(name = "VALIDITY_ATTRIBUTE", length = 25)
    private String validityAttribute;

    @Column(name = "WEBENABLED", length = 10)
    private String webenabled;
    
    @Column(name = "AMD_DATE")
    private LocalDate amdDate;

    @Column(name = "AMD_TIME",length = 20)
    private String amdTime;

    @Column(name = "CMM_SI_PREFERENCE", length = 50)
    private String cmmSiPreference;

    @Column(name = "SI_MODE", length = 70)
    private String siMode;

    @Column(name = "CI_MODE", length = 50)
    private String ciMode;

    @Column(name = "CI_EDITION", length = 6)
    private String ciEdition;

    @Column(name = "SECTOR", length = 60)
    private String sector;

    @Column(name = "SEND_CONFIRMATION", length = 6)
    private String sendConfirmation;

    @Column(name = "PAYMENT_NETTING", length = 6)
    private String paymentNetting;

    @Column(name = "NOTIFYING_FIXING", length = 6)
    private String notifyingFixing;

    @Column(name = "COLLATERAL_AGREEMENT", length = 6)
    private String collateralAgreement;

    @Column(name = "BANK", length = 5)
    private String bank;

    @Column(name = "BROKER", length = 4)
    private String broker;

    @Column(name = "CUSTOMER", length = 5)
    private String customer;

    @Column(name = "FICTIVE", length = 5)
    private String fictive;

    @Column(name = "INTERNAL_PARTY", length = 5)
    private String internalParty;

    @Column(name = "OTHER", length = 5)
    private String other;

    @Column(name = "GROUP_LABEL", length = 5)
    private String groupLabel;

    @Column(name = "SUBSIDIARY", length = 5)
    private String subsidiary;

    @Column(name = "BRANCH", length = 4)
    private String branch;

    @Column(name = "CORPORATE", length = 4)
    private String corporate;

    @Column(name = "STATE", length = 5)
    private String state;

    @Column(name = "CLEARER", length = 5)
    private String clearer;

    @Column(name = "CLIENT", length = 5)
    private String client;

    @Column(name = "ISSUER", length = 5)
    private String issuer;

    @Column(name = "AGENT", length = 5)
    private String agent;

    @Column(name = "LEGALENTITY", length = 25)
    private String legalentity;

    @Column(name = "REFENTITY", length = 5)
    private String refEntity;

    @Column(name = "GARANTOR", length = 5)
    private String grantor;

    @Column(name = "OPERATOR", length = 5)
    private String operator;

    @Column(name = "CUSTODIAN", length = 5)
    private String custodian;

    @Column(name = "FUND", length = 5)
    private String fund;

    @Column(name = "HEDGEFUND", length = 5)
    private String hedgeFund;

    @Column(name = "CLS_ELIGIBLE", length = 5)
    private String clsEligible;
    @Column(name = "CCIF_FLAG", length = 5)
    private String ccifFlag;

    @Column(name = "CHATS_ELIGIBLE", length = 5)
    private String chatsEligible;

    @Column(name = "CUSTOMER_CODE", length = 55)
    private String customerCode;

    @Column(name = "SECTORNAME", length = 55)
    private String sectorName;

    @Column(name = "LEGAL_NAME", length = 140)
    private String legalName;

    @Column(name = "ESTABLISHMENT_DATE")
    private String establishmentDate;

    @Column(name = "BUMIPUTRA", length = 10)
    private String bumiputra;

    @Column(name = "LEGAL_ID", length = 150)
    private String legalId;

    @Column(name = "LEGALID_TYPE")
    private String legalIdType;

    @Column(name = "NATIONALITY", length = 80)
    private String nationality;

    @Column(name = "REGION", length = 60)
    private String region;

    @Column(name = "RENTAS_CODE", length = 15)
    private String rentasCode;

    @Column(name = "CALCULATION_AGENT", length = 6)
    private String calculationAgent;

    @Column(name = "DTCY_ID", length = 20)
    private String dtcyId;

    @Column(name = "CUSTODY", length = 5)
    private String custody;

    @Column(name = "EXPIREIN_MORNING", length = 6)
    private String expireInMorning;

    @Column(name = "CLEARING_BROKER", length = 20)
    private String clearingBroker;

    @Column(name = "POSTAL_CODE", length = 15)
    private String postalCode;

    @Column(name = "FECL3PTY_PARENT", length = 20)
    private String fecl3ptyParent;

    @Column(name = "RTGS_CODE", length = 17)
    private String rtgsCode;

    @Column(name = "CITAD_CODE", length = 15)
    private String citadCode;

    @Column(name = "AAA_FA", length = 70)
    private String aaaFa;

    @Column(name = "AAA_BRN", length = 5)
    private String aaaBrn;

    @Column(name = "RM_REP", length = 105)
    private String rmRep;

    @Column(name = "RM_SBU", length = 105)
    private String rmSbu;

    @Column(name = "LENDING_UNIT", length = 30)
    private String lendingUnit;

    @Column(name = "FX_SEGREGATION", length = 55)
    private String fxSegregation;

    @Column(name = "FINANCE_RATING", length = 15)
    private String financeRating;

    @Column(name = "SIBS_SGCIF", length = 20)
    private String sibsSgcif;

    @Column(name = "SIBS_LBCIF", length = 35)
    private String sibsLbcif;

    @Column(name = "SIBS_LDCIF", length = 35)
    private String sibsLdcif;

    @Column(name = "MAS_SECTOR", length = 10)
    private String masSector;

    @Column(name = "BNM_SECTOR", length = 10)
    private String bnmSector;

    @Column(name = "BTP_CODE", length = 8)
    private String btpCode;

    @Column(name = "INTERNAL_SECTOR", length = 10)
    private String internalSector;

    @Column(name = "MT202_COV", length = 5)
    private String mt202Cov;

    @Column(name = "DVP_FOP", length = 5)
    private String dvpFop;

    @Column(name = "BIZSECTOR", length = 55)
    private String bizsector;

    @Column(name = "OVERNIGHT_LENDING", length = 10)
    private String overNightLending;

    @Column(name = "REUTERS_CODE", length = 65)
    private String reutersCode;

    @Column(name = "REUTERS_INDICATOR", length = 105)
    private String reutersIndicator;

    @Column(name = "DTCC_LEI", length = 25)
    private String dtccLei;

    @Column(name = "DTCC_MASKED", length = 5)
    private String dtccMasked;

    @Column(name = "DTCC_NAMESPACE", length = 15)
    private String dtccNamespace;

    @Column(name = "DTCC_SWIFTCODE", length = 15)
    private String dtccSwiftCode;

    @Column(name = "CUSTOMER_CLASS", length = 55)
    private String customerClass;

    @Column(name = "TIN_NO", length = 15)
    private String tinNo;

    @Column(name = "GIIN_NO", length = 30)
    private String giinNo;

    @Column(name = "SECONDARY_ID", length = 55)
    private String secondaryId;

    @Column(name = "PASSPORT_DOE", length = 10)
    private String passportDoe;

    @Column(name = "W8BEN", length = 5)
    private String w8ben;

    @Column(name = "W8BEN_DOE", length = 10)
    private String w8benDoe;

    @Column(name = "GIIN", length = 5)
    private String giin;

    @Column(name = "BO1", length = 80)
    private String bo1;

    @Column(name = "BO2", length = 80)
    private String bo2;

    @Column(name = "BO3", length = 80)
    private String bo3;

    @Column(name = "BO4", length = 80)
    private String bo4;

    @Column(name = "FUND_OWNR", length = 60)
    private String fundOwnr;

    @Column(name = "POB", length = 25)
    private String pob;

    @Column(name = "SI", length = 60)
    private String si;

    @Column(name = "POA", length = 100)
    private String poa;

    @Column(name = "HOLD_MAIL", length = 100)
    private String holdMail;

    @Column(name = "CATEGORY0", length = 80)
    private String category0;

    @Column(name = "CATEGORY1", length = 80)
    private String category1;

    @Column(name = "CATEGORY2", length = 80)
    private String category2;

    @Column(name = "CATEGORY3", length = 80)
    private String category3;

    @Column(name = "BNM_ENTITYCODE",length = 10)
    private String bnmEntityCode;

    @Column(name = "BNM_INST_SECCODE", length = 5)
    private String bnmInstSecCode;

    @Column(name = "BNM_RACECODE", length = 5)
    private String bnmRaceCode;

    @Column(name = "MSIC", length = 10)
    private String misc;

    @Column(name = "T24_ID", length = 15)
    private String t24Id;

    @Column(name = "OAD_ID", length = 15)
    private String oadId;

    @Column(name = "OAD_STATUS", length = 25)
    private String oadStatus;

    @Column(name = "OAD_CITY", length = 75)
    private String oadCity;

    @Column(name = "STREET", length = 75)
    private String street;

    @Column(name = "OAD_POSTCODE", length = 25)
    private String oadPostCode;

    @Column(name = "OAD_COUNTRY", length = 40)
    private String oadCountry;

    @Column(name = "CONTACT1", length = 55)
    private String contact1;

    @Column(name = "CONTACT2", length = 55)
    private String contact2;

    @Column(name = "TYPE", length = 15)
    private String type;

    @Column(name = "OAD_SWIFT", length = 15)
    private String oadSwift;

    @Column(name = "CHANNEL", length = 25)
    private String channel;

    @Column(name = "MGR_ID", length = 80)
    private String mgrId;

    @Column(name = "MGR_NAME", length = 60)
    private String mgrName;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "PATH", length = 255)
    private String path;

    @Column(name = "COMMENT1", length = 60)
    private String comment1;

    @Column(name = "COMMENT2", length = 60)
    private String comment2;

    @Column(name = "COMMENT3", length = 60)
    private String comment3;

    @Column(name = "COMMENT4", length = 60)
    private String comment4;

    @Column(name = "COMMENT5", length = 60)
    private String comment5;

    @Column(name = "COMMENT6", length = 60)
    private String comment6;

    @Column(name = "LEI_LEGALNAME", length = 255)
    private String leiLegalName;

    @Column(name = "LEGAL_FORM", length = 255)
    private String legalForm;

    @Column(name = "LEI_STATUS", length = 50)
    private String leiStatus;

    @Column(name = "IMMEDIATE_PARENT", length = 25)
    private String immediateParent;

    @Column(name = "ULTIMATE_PARENT", length = 25)
    private String ultimateParent;

    @Column(name = "ADDRESS_ID", length = 50)
    private String addressId;

    @Column(name = "LAST_UPDATE", length = 150)
    private String lastUpdate;

    @Column(name = "DISABLED", length = 150)
    private String disabled;

    @Column(name = "ASSIGNMENT", length = 160)
    private String assignment;

    @Column(name = "IRN_ID", length = 15)
    private String irnId;

    @Column(name = "TRN_LABEL", length = 40)
    private String trnLabel;

    @Column(name = "COUNTRPART", length = 15)
    private String countrPart;

    @Column(name = "FILTER", length = 25)
    private String filter;

    @Column(name = "FILTER_DESC", length = 150)
    private String filterDesc;

    @Column(name = "STARTDATE", length = 15)
    private String startDate;

    @Column(name = "ENDDATE", length = 15)
    private String endDate;

    @Column(name = "DOCUMENT_TYPE", length = 15)
    private String documentType;

    @Column(name = "MASTER_AGREEMENT", length = 15)
    private String masterAgreement;

    @Column(name = "RECIPIENT", length = 20)
    private String recipient;

    @Column(name = "ADDRESS", length = 50)
    private String address;

    @Column(name = "ADDDRESSCONTACT1", length = 70)
    private String addressContact1;

    @Column(name = "ADDDRESSCONTACT2", length = 70)
    private String addressContact2;

    @Column(name = "LANGUAGE", length = 20)
    private String language;

    @Column(name = "CUSTOMINFO")
    private String custominfo;

    @Column(name = "NOTIFICATION", length = 5)
    private String notification;

    @Column(name = "NEGATIVE_AFFIRMATION", length = 5)
    private String negativeAffirmation;

    @Column(name = "CHASING", length = 5)
    private String chasing;

    @Column(name = "CUSTOMIZATIONS", length = 10)
    private String customization;

    @Column(name = "TRADERS_NAME", length = 25)
    private String tradersName;

    @Column(name = "REGIME", length = 15)
    private String regmi;

    @Column(name = "SUP_BODY", length = 15)
    private String supBody;

    @Column(name = "ROLE", length = 15)
    private String role;

    @Column(name = "JUR_DESCRIPTION", length = 35)
    private String jurDescription;

    @Column(name = "MAIN", length = 10)
    private String main;

    @Column(name = "REP_DATE")
    private LocalDate reportDate;

    @Column(name = "SYS_DATE")
    private LocalDate sysDate;

    @Column(name = "JOB_ID")
    private Long jobID;


}

