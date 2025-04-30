package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
//import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;
import java.util.UUID;

//@Document(indexName = "#{@counterpartyIndexName}", useServerConfiguration = true)
//@Document(indexName = "uam_counterparty")

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class CounterPartyDocument {

    public final static String INDEX_NAME = "uam_counterparty";
    public final static String MAPPING_PATH = "/elastic/mappings/uam_counterparty.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();
    @JsonIgnore
    private String version;  // if mismatch between Elasticsearch json and java object

    private Integer cpId;
    private String counterPartyName;
    private String dspLabel;
    private String fullName;
    private String revision;
    private String code;
    private String nameSpace;
    private String status;
    private String parent;
    private String parentCompany;
    private String legalEntity;
    private String legalEntityIdentifier;
    private String category;
    private String fiscalCategory;
    private String address0;
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String postCode;
    private String country;
    private String tel;
    private String fax;
    private String tlx;
    private String swift;
    private String email;
    private String validityAttribute;
    private String webenabled;
    private String amdDate;
    private String amdTime;
    private String cmmSiPreference;
    private String siMode;
    private String ciMode;
    private String ciEdition;
    private String sector;
    private String sendConfirmation;
    private String paymentNetting;
    private String notifyingFixing;
    private String collateralAgreement;
    private String bank;
    private String broker;
    private String customer;
    private String fictive;
    private String internalParty;
    private String other;
    private String groupLabel;
    private String subsidiary;
    private String branch;
    private String corporate;
    private String state;
    private String clearer;
    private String client;
    private String issuer;
    private String agent;
    private String refEntity;
    private String grantor;
    private String operator;
    private String custodian;
    private String fund;
    private String hedgeFund;
    private String clsEligible;
    private String ccifFlag;
    private String chatsEligible;
    private String customerCode;
    private String sectorName;
    private String legalName;
    private String establishmentDate;
    private String bumiputra;
    private String legalId;
    private String legalIdType;
    private String nationality;
    private String region;
    private String rentasCode;
    private String calculationAgent;
    private String dtcyId;
    private String custody;
    private String expireInMorning;
    private String clearingBroker;
    private String postalCode;
    private String fecl3ptyParent;
    private String rtgsCode;
    private String citadCode;
    private String aaaFa;
    private String aaaBrn;
    private String rmRep;
    private String rmSbu;
    private String lendingUnit;
    private String fxSegregation;
    private String financeRating;
    private String sibsSgcif;
    private String sibsLbcif;
    private String sibsLdcif;
    private String masSector;
    private String bnmSector;
    private String btpCode;
    private String internalSector;
    private String mt202Cov;
    private String dvpFop;
    private String bizsector;
    private String overNightLending;
    private String reutersCode;
    private String reutersIndicator;
    private String dtccLei;
    private String dtccMasked;
    private String dtccNamespace;
    private String dtccSwiftCode;
    private String customerClass;
    private String tinNo;
    private String giinNo;
    private String secondaryId;
    private String passportDoe;
    private String w8ben;
    private String w8benDoe;
    private String giin;
    private String bo1;
    private String bo2;
    private String bo3;
    private String bo4;
    private String fundOwnr;
    private String pob;
    private String si;
    private String poa;
    private String holdMail;
    private String category0;
    private String category1;
    private String category2;
    private String category3;
    private String bnmEntityCode;
    private String bnmInstSecCode;
    private String bnmRaceCode;
    private String msic;
    private String t24Id;
    private String oadId;
    private String oadStatus;
    private String oadCity;
    private String street;
    private String oadPostCode;
    private String oadCountry;
    private String contact1;
    private String contact2;
    private String type;
    private String oadSwift;
    private String channel;
    private String mgrId;
    private String mgrName;
    private String description;
    private String path;
    private String comment1;
    private String comment2;
    private String comment3;
    private String comment4;
    private String comment5;
    private String comment6;
    private String leiLegalName;
    private String legalForm;
    private String leiStatus;
    private String immediateParent;
    private String ultimateParent;
    private String addressId;
    private String lastUpdate;
    private String disabled;
    private String assignment;
    private String irnId;
    private String trnLabel;
    private String countrPart;
    private String filter;
    private String filterDesc;
    private String startDate;
    private String endDate;
    private String documentType;
    private String masterAgreement;
    private String recipient;
    private String address;
    private String addressContact1;
    private String addressContact2;
    private String language;
    private String custominfo;
    private String notification;
    private String negativeAffirmation;
    private String chasing;
    private String customization;
    private String tradersName;
    private String regime;
    private String supBody;
    private String role;
    private String jurDescription;
    private String main;

    private String creationDate;

    private List<String> activeChinesewallTemplate;
    private List<String> inActiveChinesewallTemplate;

    // update duplicate Data  based on mgrId and OadId
    private String counterpartStatus = "static";
    private String duplicateCp = "";

    // private String id;
    private String sysDate;
    private String reportDate;
    private Long jobId;
    private boolean dormantCounterparty = false;

}
