package com.finsurge.tmr_portal.mx_superview.configs.elastic_data;

import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.*;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.ElasticSaveDataService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.GroupPortfolioService;
import com.finsurge.tmr_portal.mx_superview.elastic_search.service.UserGroupAccessRightsService;
import com.finsurge.tmr_portal.mx_superview.repository.DataImportJobRepository;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Component
public class ElasticSearchItemWriter<T> implements ItemWriter<T> {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchItemWriter.class);

    public String reportDate;
    public Long jobId;
    @Autowired
    private final Environment environment;


    @Autowired
    private ElasticSaveDataService elasticSaveDataService;

    @Autowired
    private GroupPortfolioService groupPortFolioService;

    @Autowired
    private DataImportJobRepository dataImportJobRepository;


    @Autowired
    private UserGroupAccessRightsService userGroupAccessRightsService;

    @Override
    public void write(@NotNull List<? extends T> items) throws Exception {

        log.info("Data Loading is in progress...");
        String tableName = "";

        LocalDate date = LocalDate.parse(reportDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
        String repDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            for (Object listItem : items) {
                if (listItem instanceof CounterPartyDocument) {
                    tableName = "UAM_MX_COUNTER_PARTY";
                } else if (listItem instanceof DormantCounterparty) {
                    tableName = "UAM_MX_DORMANT_COUNTERPARTY";
                } else if (listItem instanceof ClosingEntity) {
                    tableName = "UAM_MX_CLOSING_ENTITY";
                } else if (listItem instanceof CounterpartyCreation) {
                    tableName = "UAM_MX_COUNTERPARTY_CREATION";
                } else if (listItem instanceof UserGroupAccessRights) {
                    tableName = "UAM_USER_GROUP_ACCESS_RIGHT";
                } else if (listItem instanceof UserList) {
                    tableName = "UAM_MX_USER_LIST";
                } else if (listItem instanceof UserGroupList) {
                    tableName = "UAM_MX_GROUP_LIST";
                } else if (listItem instanceof UserLicense) {
                    tableName = "UAM_MX_USER_LICENSE";
                } else if (listItem instanceof UserPolicy) {
                    tableName = "UAM_MX_USER_POLICY";
                } else if (listItem instanceof GroupPortfolioRights) {
                    tableName = "UAM_MX_GROUP_PORTFOLIO_RIGHTS";
                } else if (listItem instanceof ChineseWall) {
                    tableName = "UAM_MX_CHINESE_WALL_TMPL";
                } else if (listItem instanceof GroupNavigationRights) {
                    tableName = "UAM_MX_GROUP_NAV_RIGHTS";
                } else if (listItem instanceof GroupComboPortfolio) {
                    tableName = "UAM_MX_GROUP_COMBINED_PORTFOLIO";
                } else if (listItem instanceof OperationRights) {
                    tableName = "UAM_MX_OPERATION_RIGHTS";
                } else if (listItem instanceof OspRightsMatrix) {
                    tableName = "UAM_MX_OSP_RIGHTS_MATRIX";
                } else if (listItem instanceof EnterpriseRisk) {
                    tableName = "UAM_MX_ENTERPRISE_RISK";
                } else if (listItem instanceof FinanceRights) {
                    tableName = "UAM_MX_FINANCE_ACCTRL_RIGHTS";
                } else if (listItem instanceof ConsistencyTmpl) {
                    tableName = "UAM_MX_CONSISTENCY_TMPL";
                } else if (listItem instanceof ConfigMgtRight) {
                    tableName = "UAM_MX_CWT_CONFIG_MGT_RIGHT";
                } else if (listItem instanceof SupChgAudit) {
                    tableName = "UAM_MX_SUPCHG_AUDIT";
                } else if (listItem instanceof DisplaySI) {
                    tableName = "UAM_MX_COUNTER_PARTY_DISPLAY";
                } else if (listItem instanceof UdfStructureIrd) {
                    tableName = "UAM_UDF_STRUCTURE_IRD";
                } else if (listItem instanceof UdfStructureComm) {
                    tableName = "UAM_UDF_STRUCTURE_COMM";
                } else if (listItem instanceof UdfStructureFxd) {
                    tableName = "UAM_UDF_STRUCTURE_FXD";
                } else if (listItem instanceof UserLoginAudit) {
                    tableName = "UAM_USER_LOGIN_AUDIT";
                } else if (listItem instanceof StpRightsSrcMod) {
                    tableName = "UAM_STP_RIGHTS_SRC_MOD";
                } else if (listItem instanceof StpRightsTypology) {
                    tableName = "UAM_STP_RIGHTS_TYPOLOGY";
                } else if (listItem instanceof StpRightsMatrix) {
                    tableName = "UAM_STP_RIGHTS_MATRIX";
                }

                else {
                    log.info("Error in identifying instance name when writing data in elastic");
                }
            }


        switch (tableName) {
            case "UAM_MX_COUNTER_PARTY":
                List<CounterPartyDocument> counterPartyDocumentList = (List<CounterPartyDocument>) items;
                for (CounterPartyDocument counterPartyDocument : counterPartyDocumentList) {
                    counterPartyDocument.setReportDate(repDate);
                    counterPartyDocument.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    counterPartyDocument.setJobId(jobId);
                }
                String counterpartyIndex = CounterPartyDocument.INDEX_NAME;
                elasticSaveDataService.saveReport(counterPartyDocumentList, counterpartyIndex);
                break;

            case "UAM_MX_DORMANT_COUNTERPARTY":
                List<DormantCounterparty> dormantCounterparties = (List<DormantCounterparty>) items;
                for (DormantCounterparty dormantCounterparty : dormantCounterparties) {
                    dormantCounterparty.setReportDate(repDate);
                    dormantCounterparty.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    dormantCounterparty.setJobId(jobId);
                }
                //  dormantCounterpartyRepo.saveAll(dormantCounterparties);
                String dormantIndex = DormantCounterparty.INDEX_NAME;
                elasticSaveDataService.saveReport(dormantCounterparties, dormantIndex);
                break;

            case "UAM_MX_CLOSING_ENTITY":
                List<ClosingEntity> closingEntities = (List<ClosingEntity>) items;
                for (ClosingEntity closingEntity : closingEntities) {
                    closingEntity.setReportDate(repDate);
                    closingEntity.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    closingEntity.setJobId(jobId);
                }
                String closingEntityIndex = ClosingEntity.INDEX_NAME;
                elasticSaveDataService.saveReport(closingEntities, closingEntityIndex);
                break;

            case "UAM_MX_COUNTERPARTY_CREATION":
                List<CounterpartyCreation> counterpartyCreations = (List<CounterpartyCreation>) items;
                for (CounterpartyCreation counterpartyCreation : counterpartyCreations) {
                    counterpartyCreation.setReportDate(repDate);
                    counterpartyCreation.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    counterpartyCreation.setJobId(jobId);
                }
                String counterpartyCreationIndex = CounterpartyCreation.INDEX_NAME;
                elasticSaveDataService.saveReport(counterpartyCreations, counterpartyCreationIndex);
                break;

            case "UAM_USER_GROUP_ACCESS_RIGHT":
                List<UserGroupAccessRights> userGroupAccessRights = (List<UserGroupAccessRights>) items;
                for (UserGroupAccessRights userGroupAccessRight : userGroupAccessRights) {
                    userGroupAccessRight.setReportDate(repDate);
                    userGroupAccessRight.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    userGroupAccessRight.setJobId(jobId);
                }
                String userGroupAccessRightIndex = UserGroupAccessRights.INDEX_NAME;
                elasticSaveDataService.saveReport(userGroupAccessRights, userGroupAccessRightIndex);
                break;

            case "UAM_MX_USER_LIST":
                List<UserList> userLists = (List<UserList>) items;
                for (UserList userList : userLists) {
                    userList.setReportDate(repDate);
                    userList.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    userList.setJobId(jobId);
                }
                String userListIndex = UserList.INDEX_NAME;
                elasticSaveDataService.saveReport(userLists, userListIndex);
                break;

            case "UAM_MX_GROUP_LIST":
                List<UserGroupList> userGroupLists = (List<UserGroupList>) items;
                for (UserGroupList userGroupList : userGroupLists) {
                    userGroupList.setReportDate(repDate);
                    userGroupList.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    userGroupList.setJobId(jobId);
                }
                String groupListIndex = UserGroupList.INDEX_NAME;
                elasticSaveDataService.saveReport(userGroupLists, groupListIndex);
                break;

            case "UAM_MX_USER_LICENSE":
                List<UserLicense> userLicenses = (List<UserLicense>) items;
                for (UserLicense userLicense : userLicenses) {
                    userLicense.setReportDate(repDate);
                    userLicense.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    userLicense.setJobId(jobId);
                }
                break;

            case "UAM_MX_USER_POLICY":
                List<UserPolicy> userPolicies = (List<UserPolicy>) items;
                for (UserPolicy userPolicy : userPolicies) {
                    userPolicy.setReportDate(repDate);
                    userPolicy.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    userPolicy.setJobId(jobId);
                }
                String userPolicyIndex = UserPolicy.INDEX_NAME;
                elasticSaveDataService.saveReport(userPolicies, userPolicyIndex);
                break;

            case "UAM_MX_GROUP_PORTFOLIO_RIGHTS":
                List<GroupPortfolioRights> groupPortFolioRights = (List<GroupPortfolioRights>) items;
                for (GroupPortfolioRights groupPortFolioRight : groupPortFolioRights) {
                    groupPortFolioRight.setReportDate(repDate);
                    groupPortFolioRight.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    groupPortFolioRight.setJobId(jobId);
                }
                String groupPortFolioRightIndex =GroupPortfolioRights.INDEX_NAME;
                elasticSaveDataService.saveReport(groupPortFolioRights, groupPortFolioRightIndex);
                break;

            case "UAM_MX_CHINESE_WALL_TMPL":
                List<ChineseWall> chineseWalls = (List<ChineseWall>) items;
                for (ChineseWall chineseWall : chineseWalls) {
                    chineseWall.setReportDate(repDate);
                    chineseWall.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    chineseWall.setJobId(jobId);
                }
                String chineseWallIndex = ChineseWall.INDEX_NAME;
                elasticSaveDataService.saveReport(chineseWalls, chineseWallIndex);
                break;
            case "UAM_MX_GROUP_NAV_RIGHTS":
                List<GroupNavigationRights> groupNavigationRights = (List<GroupNavigationRights>) items;
                for (GroupNavigationRights groupNavigationRight : groupNavigationRights) {
                    groupNavigationRight.setReportDate(repDate);
                    groupNavigationRight.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    groupNavigationRight.setJobId(jobId);
                }
                String groupNavigationRightsIndex = GroupNavigationRights.INDEX_NAME;
                elasticSaveDataService.saveReport(groupNavigationRights, groupNavigationRightsIndex);
                break;

            case "UAM_MX_GROUP_COMBINED_PORTFOLIO":
                List<GroupComboPortfolio> groupComboPortfolios = (List<GroupComboPortfolio>) items;
                for (GroupComboPortfolio groupComboPortfolio : groupComboPortfolios) {
                    groupComboPortfolio.setReportDate(repDate);
                    groupComboPortfolio.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    groupComboPortfolio.setJobId(jobId);
                }
                String groupComboPortfolioIndex = GroupComboPortfolio.INDEX_NAME;
                elasticSaveDataService.saveReport(groupComboPortfolios, groupComboPortfolioIndex);
                break;

            case "UAM_MX_OPERATION_RIGHTS":
                List<OperationRights> operationRights= (List<OperationRights>) items;
                for (OperationRights operationRight : operationRights) {
                    operationRight.setReportDate(repDate);
                    operationRight.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    operationRight.setJobId(jobId);
                }
                String operationRightsIndex = OperationRights.INDEX_NAME;
                elasticSaveDataService.saveReport(operationRights, operationRightsIndex);
                break;

            case "UAM_MX_OSP_RIGHTS_MATRIX":
                List<OspRightsMatrix> ospRightsMatrices = (List<OspRightsMatrix>) items;
                for (OspRightsMatrix ospRightsMatrix : ospRightsMatrices) {
                    ospRightsMatrix.setReportDate(repDate);
                    ospRightsMatrix.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    ospRightsMatrix.setJobId(jobId);
                }
                String ospRightsMatrixIndex = OspRightsMatrix.INDEX_NAME;
                elasticSaveDataService.saveReport(ospRightsMatrices, ospRightsMatrixIndex);
                break;

            case "UAM_MX_ENTERPRISE_RISK":
                List<EnterpriseRisk> enterpriseRisks = (List<EnterpriseRisk>) items;
                for (EnterpriseRisk enterpriseRisk : enterpriseRisks) {
                    enterpriseRisk.setReportDate(repDate);
                    enterpriseRisk.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    enterpriseRisk.setJobId(jobId);
                }
                String enterpriseRiskIndex = EnterpriseRisk.INDEX_NAME;
                elasticSaveDataService.saveReport(enterpriseRisks, enterpriseRiskIndex);
                break;

            case "UAM_MX_FINANCE_ACCTRL_RIGHTS":
                List<FinanceRights> financeRights = (List<FinanceRights>) items;
                for (FinanceRights financeRight : financeRights) {
                    financeRight.setReportDate(repDate);
                    financeRight.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    financeRight.setJobId(jobId);
                }
                String financeRightsIndex = FinanceRights.INDEX_NAME;
                elasticSaveDataService.saveReport(financeRights, financeRightsIndex);
                break;

            case "UAM_MX_CONSISTENCY_TMPL":
                List<ConsistencyTmpl> consistencyTmpls = (List<ConsistencyTmpl>) items;
                for (ConsistencyTmpl consistencyTmpl : consistencyTmpls) {
                    consistencyTmpl.setReportDate(repDate);
                    consistencyTmpl.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    consistencyTmpl.setJobId(jobId);
                }
                String consistencyTmplIndex = ConsistencyTmpl.INDEX_NAME;
                elasticSaveDataService.saveReport(consistencyTmpls, consistencyTmplIndex);
                break;

            case "UAM_MX_CWT_CONFIG_MGT_RIGHT":
                List<ConfigMgtRight> configMgtRights = (List<ConfigMgtRight>) items;
                for (ConfigMgtRight configMgtRight : configMgtRights) {
                    configMgtRight.setReportDate(repDate);
                    configMgtRight.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    configMgtRight.setJobId(jobId);
                }
                String configMgtRightIndex = ConfigMgtRight.INDEX_NAME;
                elasticSaveDataService.saveReport(configMgtRights, configMgtRightIndex);
                break;

            case "UAM_MX_SUPCHG_AUDIT":
                List<SupChgAudit> supchgaudits = (List<SupChgAudit>) items;
                for (SupChgAudit supchgaudit : supchgaudits) {
                    supchgaudit.setReportDate(repDate);
                    supchgaudit.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    supchgaudit.setJobId(jobId);
                }
                String supchgauditIndex = SupChgAudit.INDEX_NAME;
                elasticSaveDataService.saveReport(supchgaudits, supchgauditIndex);
                break;

            case "UAM_MX_COUNTER_PARTY_DISPLAY":
                List<DisplaySI> displaySi = (List<DisplaySI>) items;
                for (DisplaySI newDisplaySI : displaySi) {
                    newDisplaySI.setReportDate(repDate);
                    newDisplaySI.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    newDisplaySI.setJobId(jobId);
                }
                String displaySiIndex = DisplaySI.INDEX_NAME;
                elasticSaveDataService.saveReport(displaySi, displaySiIndex);
                break;

            case "UAM_UDF_STRUCTURE_IRD":
                List<UdfStructureIrd> udfStructureIrds = (List<UdfStructureIrd>) items;
                for (UdfStructureIrd udfStructureIrd : udfStructureIrds) {
                    udfStructureIrd.setReportDate(repDate);
                    udfStructureIrd.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    udfStructureIrd.setJobId(jobId);
                }
                String udfStructureIrdIndex = UdfStructureIrd.INDEX_NAME;
                elasticSaveDataService.saveReport(udfStructureIrds, udfStructureIrdIndex);
                break;

            case "UAM_UDF_STRUCTURE_COMM":
                List<UdfStructureComm> udfStructureComms = (List<UdfStructureComm>) items;
                for (UdfStructureComm udfStructureComm : udfStructureComms) {
                    udfStructureComm.setReportDate(repDate);
                    udfStructureComm.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    udfStructureComm.setJobId(jobId);
                }
                String udfStructureCommIndex = UdfStructureComm.INDEX_NAME;
                elasticSaveDataService.saveReport(udfStructureComms, udfStructureCommIndex);
                break;

            case "UAM_UDF_STRUCTURE_FXD":
                List<UdfStructureFxd> udfStructureFxds = (List<UdfStructureFxd>) items;
                for (UdfStructureFxd udfStructureFxd : udfStructureFxds) {
                    udfStructureFxd.setReportDate(repDate);
                    udfStructureFxd.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    udfStructureFxd.setJobId(jobId);
                }
                String udfStructureFxdIndex = UdfStructureFxd.INDEX_NAME;
                elasticSaveDataService.saveReport(udfStructureFxds, udfStructureFxdIndex);
                break;

            case "UAM_USER_LOGIN_AUDIT":
                List<UserLoginAudit> userLoginAudits = (List<UserLoginAudit>) items;
                for (UserLoginAudit userLoginAudit : userLoginAudits) {
                    userLoginAudit.setReportDate(repDate);
                    userLoginAudit.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    userLoginAudit.setJobId(jobId);
                }
                String userLoginAuditIndex = UserLoginAudit.INDEX_NAME;
                elasticSaveDataService.saveReport(userLoginAudits, userLoginAuditIndex);
                break;

            case "UAM_STP_RIGHTS_SRC_MOD":
                List<StpRightsSrcMod> stpRightsSrcMods = (List<StpRightsSrcMod>) items;
                for (StpRightsSrcMod stpRightsSrcMod : stpRightsSrcMods) {
                    stpRightsSrcMod.setReportDate(repDate);
                    stpRightsSrcMod.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    stpRightsSrcMod.setJobId(jobId);
                }
                String stpRightsSrcModIndex = StpRightsSrcMod.INDEX_NAME;
                elasticSaveDataService.saveReport(stpRightsSrcMods, stpRightsSrcModIndex);
                break;

            case "UAM_STP_RIGHTS_TYPOLOGY":
                List<StpRightsTypology> stpRightsTypologies = (List<StpRightsTypology>) items;
                for (StpRightsTypology stpRightsTypology : stpRightsTypologies) {
                    stpRightsTypology.setReportDate(repDate);
                    stpRightsTypology.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    stpRightsTypology.setJobId(jobId);
                }
                String stpRightsTypologyIndex = StpRightsTypology.INDEX_NAME;
                elasticSaveDataService.saveReport(stpRightsTypologies, stpRightsTypologyIndex);
                break;

            case "UAM_STP_RIGHTS_MATRIX":
                List<StpRightsMatrix> stpRightsMatrices = (List<StpRightsMatrix>) items;
                for (StpRightsMatrix stpRightsMatrix : stpRightsMatrices) {
                    stpRightsMatrix.setReportDate(repDate);
                    stpRightsMatrix.setSysDate(String.valueOf(java.time.Clock.systemUTC().instant()));
                    stpRightsMatrix.setJobId(jobId);
                }
                String stpRightsMatrixIndex = StpRightsMatrix.INDEX_NAME;
                elasticSaveDataService.saveReport(stpRightsMatrices, stpRightsMatrixIndex);
                break;


        }
    }

    public void
    setReportDateAndJobId(String reportDate, Long jobId) {
        this.reportDate = reportDate;
        this.jobId = jobId;
    }


}
