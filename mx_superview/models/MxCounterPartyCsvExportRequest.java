package com.finsurge.tmr_portal.mx_superview.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
@Data
@ToString

public class MxCounterPartyCsvExportRequest {

    @JsonProperty("NO")
    public  Long id;

    @JsonProperty("COUNTERPART LABEL")
    public   String cptyLable;

    @JsonProperty("REF")
    public  Long ref;

    @JsonProperty("SETTLTYPE")
    public  String settlType;

    @JsonProperty("PREVIOUS")
    public Long previous;

    @JsonProperty("NEXT")
    public Long next;

    @JsonProperty("MULTIPLE")
    public String multiple;

    @JsonProperty("AMEND")
    public  Long amend;

    @JsonProperty("NOSTRO VOSTRO")
    public String novo;

    @JsonProperty("CREDIT DEBIT")
    public  String crde;

    @JsonProperty("NATURE")
    public  String nature;
    @JsonProperty("INS DATE")
    public String insDate;
    @JsonProperty("MOD DATE")
    public String modDate;
    @JsonProperty("START DT")
    public  String startDt;
    @JsonProperty("END DT")
    public  String endDt;
    @JsonProperty("INS TIME")
    public String insTime;
    @JsonProperty("MOD TIME")
    public  String modTime;
    @JsonProperty("COUNT PART")
    public  String countPart;
    @JsonProperty("ENTITY")
    public  String entity;
    @JsonProperty("TRADE SECTION")
    public String tradeSection;
    @JsonProperty("FAMILY")
    public String family;
    @JsonProperty("TRN GROUP")
    public String trnGroup;
    @JsonProperty("TYPE")
    public String type;
    @JsonProperty("INSTRUMENT")
    public String instrument;
    @JsonProperty("CURRENCY")
    public String currency;
    @JsonProperty("OD CURRENCY")
    public  String odCurrency;
    @JsonProperty("CODE")
    public  String code;
    @JsonProperty("DISPLAY USER")
    public String displayUser;
    @JsonProperty("STATUS")
    public  String status;
    @JsonProperty("MARKET")
    public  String market;
    @JsonProperty("CLEAR CENTER")
    public  String clearCenter;
    @JsonProperty("SETTLE METHOD")
    public String settleMethod;
    @JsonProperty("COMMENTS")
    public  String comments;
    @JsonProperty("TYPOLOGY")
    public String typology;
    @JsonProperty("SI USAGE")
    public  String usage;
    @JsonProperty("LEGAL ENTITY")
    public String legalEntity;
    @JsonProperty("PROCESS ENTITY")
    public   String processEntity;
    @JsonProperty("FLOW TYPOLOGY0")
    public String flowTypology0;
    @JsonProperty("FLOW TYPOLOGY1")
    public   String flowTypology1;
    @JsonProperty("FLOW TYPOLOGY2")
    public String flowTypology2;
    @JsonProperty("FLOW TYPOLOGY3")
    public String flowTypology3;
    @JsonProperty("FLOW TYPOLOGY4")
    public  String flowTypology4;
    @JsonProperty("CUSTOM INFO")
    public  String customInfo;
    @JsonProperty("PHYSICAL PRODUCT")
    public  String physicalProduct;
    @JsonProperty("LOCATION")
    public  String location;
    @JsonProperty("CLEARER")
    public  String clearer;
    @JsonProperty("PORTFOLIO")
    public String portfolio;
    @JsonProperty("STRATEGY")
    public String strategy;
    @JsonProperty("PROCESSING AREA")
    public  String proccessingArea;
    @JsonProperty("VOSTRO SERVICE")
    public String vostroService;
    @JsonProperty("AGREEMENT")
    public  String agreement;
    @JsonProperty("AGREEMENT TYPE")
    public  String agreementType;
    @JsonProperty("GOVERNING LAW")
    public String governingLaw;

    @JsonProperty("CANCELLED")
    public String cancelled;

    public MxCounterPartyCsvExportRequest(Long id, String cptyLable, Long ref, String settlType, Long previous, Long next, String multiple, Long amend, String novo, String crde, String nature, String insDate, String modDate, String startDt, String endDt, String insTime, String modTime, String countPart, String entity, String tradeSection, String family, String trnGroup, String type, String instrument, String currency, String odCurrency, String code, String displayUser, String status, String market, String clearCenter, String settleMethod, String comments, String typology, String usage, String legalEntity, String processEntity, String flowTypology0, String flowTypology1, String flowTypology2, String flowTypology3, String flowTypology4, String customInfo, String physicalProduct, String location, String clearer, String portfolio, String strategy, String proccessingArea, String vostroService, String agreement, String agreementType, String governingLaw, String cancelled) {
        this.id = id;
        this.cptyLable = cptyLable;
        this.ref = ref;
        this.settlType = settlType;
        this.previous = previous;
        this.next = next;
        this.multiple = multiple;
        this.amend = amend;
        this.novo = novo;
        this.crde = crde;
        this.nature = nature;
        this.insDate = insDate;
        this.modDate = modDate;
        this.startDt = startDt;
        this.endDt = endDt;
        this.insTime = insTime;
        this.modTime = modTime;
        this.countPart = countPart;
        this.entity = entity;
        this.tradeSection = tradeSection;
        this.family = family;
        this.trnGroup = trnGroup;
        this.type = type;
        this.instrument = instrument;
        this.currency = currency;
        this.odCurrency = odCurrency;
        this.code = code;
        this.displayUser = displayUser;
        this.status = status;
        this.market = market;
        this.clearCenter = clearCenter;
        this.settleMethod = settleMethod;
        this.comments = comments;
        this.typology = typology;
        this.usage = usage;
        this.legalEntity = legalEntity;
        this.processEntity = processEntity;
        this.flowTypology0 = flowTypology0;
        this.flowTypology1 = flowTypology1;
        this.flowTypology2 = flowTypology2;
        this.flowTypology3 = flowTypology3;
        this.flowTypology4 = flowTypology4;
        this.customInfo = customInfo;
        this.physicalProduct = physicalProduct;
        this.location = location;
        this.clearer = clearer;
        this.portfolio = portfolio;
        this.strategy = strategy;
        this.proccessingArea = proccessingArea;
        this.vostroService = vostroService;
        this.agreement = agreement;
        this.agreementType = agreementType;
        this.governingLaw = governingLaw;
        this.cancelled = cancelled;
    }

    public Long getId() {
        return id;
    }

    public String getCptyLable() {
        return  cptyLable==null?"Nil":cptyLable;
    }

    public String getRef() {
        return ref==null? "Nil" : String.valueOf(ref);
    }

    public String getSettlType() {
        return settlType==null?"Nil":settlType;
    }

    public String getPrevious() {
        return previous==null?"Nil": String.valueOf(previous);
    }

    public String getNext() {
        return  next==null?"Nil": String.valueOf(next);
    }

    public String getMultiple() {
        return  multiple==null?"Nil": multiple;
    }

    public String getAmend() {
        return  amend==null?"Nil": String.valueOf(amend);
    }

    public String getNovo() {
        return  novo==null?"Nil": novo;
    }

    public String getCrde() {
        return  crde==null?"Nil": crde;
    }

    public String getNature() {
        return  nature==null?"Nil": nature;
    }

    public String getInsDate() {
        return  insDate==null?"Nil": insDate;
    }

    public String getModDate() {
        return  modDate==null?"Nil": modDate;
    }

    public String getStartDt() {
        return  startDt==null?"Nil": startDt;
    }

    public String getEndDt() {
        return  endDt==null?"Nil": endDt;
    }

    public String getInsTime() {
        return  insTime==null?"Nil": insTime;
    }

    public String getModTime() {
        return  modTime==null?"Nil": modTime;
    }

    public String getCountPart() {
        return  countPart==null?"Nil": countPart;
    }

    public String getEntity() {
        return  entity==null?"Nil": entity;
    }

    public String getTradeSection() {
        return tradeSection ==null?"Nil": tradeSection;
    }

    public String getFamily() {
        return  family==null?"Nil": family;
    }

    public String getTrnGroup() {
        return trnGroup ==null?"Nil": trnGroup;
    }

    public String getType() {
        return  type==null?"Nil": type;
    }

    public String getInstrument() {
        return instrument ==null?"Nil": instrument;
    }

    public String getCurrency() {
        return  currency==null?"Nil": currency;
    }

    public String getOdCurrency() {
        return odCurrency ==null?"Nil": odCurrency;
    }

    public String getCode() {
        return  code==null?"Nil": code;
    }

    public String getDisplayUser() {
        return displayUser ==null?"Nil": displayUser;
    }

    public String getStatus() {
        return  status==null?"Nil": status;
    }

    public String getMarket() {
        return  market==null?"Nil": market;
    }

    public String getClearCenter() {
        return  clearCenter==null?"Nil": clearCenter;
    }

    public String getSettleMethod() {
        return settleMethod==null?"Nil": settleMethod;
    }

    public String getComments() {
        return comments==null?"Nil": comments;
    }

    public String getTypology() {
        return typology ==null?"Nil": typology;
    }

    public String getUsage() {
        return usage ==null?"Nil": usage;
    }

    public String getLegalEntity() {
        return legalEntity ==null?"Nil": legalEntity;
    }

    public String getProcessEntity() {
        return processEntity==null?"Nil": processEntity;
    }

    public String getFlowTypology0() {
        return flowTypology0 ==null?"Nil": flowTypology0;
    }

    public String getFlowTypology1() {
        return flowTypology1 ==null?"Nil": flowTypology1;
    }

    public String getFlowTypology2() {
        return flowTypology2 ==null?"Nil": flowTypology2;
    }

    public String getFlowTypology3() {
        return flowTypology3 ==null?"Nil": flowTypology3;
    }

    public String getFlowTypology4() {
        return flowTypology4 ==null?"Nil": flowTypology4;
    }

    public String getCustomInfo() {
        return customInfo ==null?"Nil": customInfo;
    }

    public String getPhysicalProduct() {
        return physicalProduct ==null?"Nil": physicalProduct;
    }

    public String getLocation() {
        return location ==null?"Nil": location;
    }

    public String getClearer() {
        return clearer ==null?"Nil": clearer;
    }

    public String getPortfolio() {
        return portfolio ==null?"Nil": portfolio;
    }

    public String getStrategy() {
        return strategy ==null?"Nil": strategy;
    }

    public String getProccessingArea() {
        return proccessingArea ==null?"Nil": proccessingArea;
    }

    public String getVostroService() {
        return vostroService ==null?"Nil": vostroService;
    }

    public String getAgreement() {
        return agreement ==null?"Nil": agreement;
    }

    public String getAgreementType() {
        return agreementType ==null?"Nil": agreementType;
    }

    public String getGoverningLaw() {
        return governingLaw ==null?"Nil": governingLaw;
    }

    public String getCancelled() {
        return cancelled ==null?"Nil": cancelled;
    }
}
