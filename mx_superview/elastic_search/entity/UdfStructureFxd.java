package com.finsurge.tmr_portal.mx_superview.elastic_search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UdfStructureFxd {
    public final static String INDEX_NAME = "uam_udf_structure_fxd";
    public final static String MAPPING_PATH = "elastic/mappings/uam_udf_structure_fxd.json";

    @JsonIgnore
    private String _class;  // added for copy object error in elastic global search
    private String id = UUID.randomUUID().toString();

    // Fields
    private String branch;
    private String purpose;
    private String subPurpose;
    private String itepsPurposeCode;
    private String itepsSubPurposeCode;
    private String boComment;
    private String mopType;
    private Integer mopCreator;
    private String branchCode;
    private String settlement;
    private String regionCode;
    private String anticipatedContract;
    private String exportCode;
    private String expDateOfShipment;
    private String callEnd;
    private String callStart;
    private String l1DciRedemptionCond;
    private String l1ClientType;
    private String l1Dealtype;
    private String l1Setltype;
    private String l1IaMethod;
    private Integer l1Amount;
    private String l1CashTransfer;
    private String l1TranType;
    private String l1BizType;
    private String l1SetlPurpose;
    private String l1TranCode;
    private String l1Trader;
    private String l1IslCode;
    private String l1TradabilityStatus;
    private String l1TradabilityMessage;
    private String l1BankNegaraApprovalCode;
    private String l1BankNegaraApprovalDate;
    private String l1AooClientTrade;
    private String l1OffsettingLeg;
    private String l1ExternalImpact;
    private String l1BundleMigration;
    private Integer l1FwdConfAck;
    private String l1FwdConfAckDate;
    private String l1ProdCat;
    private String l1AmendComment;
    private String l1DtccUtiPrefix;
    private String l1DtccUtiValue;
    private String l1DtccPriorUtiVal;
    private String l1DtccPriorUtiPfx;
    private String l1DtccUpdateDate;
    private String l1DtccUsiUpdated;
    private String l1OrigType;
    private String l1CategoryCode;
    private String l1BnmApprovalCode;
    private String l1BnmApprovalDate;
    private Double l1UtilisedAmount;
    private Integer l1DtccNexusFlag;
    private String l1CmpoCallDetail;
    private Integer l1CallAck;
    private String l1CallAckDate;
    private String l1Nfri;
    private String l1AfterHour;
    private String l1AccdPurposeCode;
    private String l1OffPremises;
    private String l1Pds;
    private String l1ResidentStatus;
    private String l1EndBeneficiary;
    private String l1OptionDeltaAmount;
    private String l1BimTypology;
    private Double l1BimNotional;
    private String l1BimNotionalOvrw;
    private String l1CBimPackageType;
    private String l1BimTradeDate;
    private String l2BtPcfx;
    private String l2BtPbFa;
    private String l2BtBrn;
    private String l2BtSetlTf;
    private String l2ExtTrdId;
    private String l2BtBktrade;

    private String sysDate;
    private String reportDate;
    private Long jobId;
}