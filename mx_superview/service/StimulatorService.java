package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails;
import com.finsurge.tmr_portal.mx_superview.repository.MxGroupListRepository;
import com.finsurge.tmr_portal.mx_superview.repository.MxUserGroupAccessRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StimulatorService {

    private final MxGroupListRepository mxGroupListRepository;
    private final MxUserGroupAccessRepository mxUserGroupAccessRepository;

    public StimulatorService(MxGroupListRepository mxGroupListRepository, MxUserGroupAccessRepository mxUserGroupAccessRepository) {
        this.mxGroupListRepository = mxGroupListRepository;
        this.mxUserGroupAccessRepository = mxUserGroupAccessRepository;
    }

    public List<String> findUsernameByGroupName(List<String> groupName, LocalDate requestDate) {

        return mxUserGroupAccessRepository.findUsernameByGroupName(groupName, requestDate);
    }

    public List<String> findAllGroupNameByOspTemplateName(String ospTempName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByOspTemplateName(ospTempName, requestDate);
    }

    public List<String> findAllGroupNameByNavTemplateName(String template, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByNavTemplateName(template, requestDate);
    }

    public List<String> findAllGroupNameByCounterTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByCounterTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByConsistencyTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByConsistencyTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByNkeyTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByNkeyTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByLposTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByLposTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByStatTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByStatTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByAccCtrlTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByAccCtrlTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByDistributionTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByDistributionTemplateName(templateName, requestDate);
    }

    public List<String> findAllGroupNameByStpRightTemplateName(String templateName, LocalDate requestDate) {

        return mxGroupListRepository.findAllGroupNameByStpRightTemplateName(templateName, requestDate);
    }

    public List<String> findUsernameByGroup(String groupName, LocalDate requestDate) {

        return mxUserGroupAccessRepository.findUsernameByGroup(groupName, requestDate);
    }

    public List<?> getGroupAndUserValues(LocalDate repDate, String template, String repType, Sort.Order sort, String subTemplate, String searchWhereClause, boolean isGlobalSearch) {
        List<?> object = new ArrayList<>();
        switch (repType) {
            case "OSP_RIGHTS":
                if (searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getOspRightsGroupDetails(repDate, template, Sort.by(sort));
                } else {
                    if (isGlobalSearch) {
                        object = mxGroupListRepository.getOspRightsGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort), searchWhereClause);
                    }
                }
                break;
            case "PORTFOLIO_RIGHTS":
                if(searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getPortfolioRightsGroupDetails(repDate, template, Sort.by(sort));
                }else{
                    if(isGlobalSearch)
                    {
                        object = mxGroupListRepository.getPortfolioRightsGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                    }
                }
                break;
            case "COMBINED_PORTFOLIO":
                if (searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getCombinedPortfolioRightsGroupDetails(repDate, template, Sort.by(sort));
                } else {
                    if (isGlobalSearch) {
                        object = mxGroupListRepository.getCombinedPortfolioRightsGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort), searchWhereClause);
                    }
                }
                break;
            case "CHINESE_WALL_DEFINITIONS":
                if (searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getChineseWallGroupDetails(repDate, template, Sort.by(sort));
                } else {
                    if (isGlobalSearch) {
                        object = mxGroupListRepository.getChineseWallGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort), searchWhereClause);
                    }
                }
                break;
            case "NAVIGATION_RIGHTS":
                if(searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getNavigationGroupDetails(repDate, template, Sort.by(sort));
                }else{
                    if(isGlobalSearch){
                        object = mxGroupListRepository.getNavigationGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                    }
                }
                break;
            case "STP_RIGHTS":
                if(searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getStpRightsGroupDetails(repDate, template, Sort.by(sort));
                }else{
                    if(isGlobalSearch){
                        object = mxGroupListRepository.getStpRightsGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);

                    }
                }
                break;
            case "CONSISTENCY_TEMPLATE":
                if(searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getConsistencyGroupDetails(repDate, template, Sort.by(sort));
                }else{
                    if(isGlobalSearch){
                        object = mxGroupListRepository.getConsistencyGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                    }
                }
                break;
            case "OPERATION_RIGHTS": {
                if (searchWhereClause == null || searchWhereClause.isBlank()) {
                    if (subTemplate.equalsIgnoreCase("nkey")) {
                        object = mxGroupListRepository.getOperationNkeyGroupDetails(repDate, template, Sort.by(sort));
                    } else {
                        object = mxGroupListRepository.getOperationLposGroupDetails(repDate, template, Sort.by(sort));
                    }
                } else {
                    if (isGlobalSearch) {
                        if (subTemplate.equalsIgnoreCase("nkey")) {
                            object = mxGroupListRepository.getOperationNkeyGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort), searchWhereClause);
                        } else {
                            object = mxGroupListRepository.getOperationLposGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort), searchWhereClause);
                        }
                    }
                }
                break;
            }
            case "FINANCE_RIGHTS":
                if(searchWhereClause == null || searchWhereClause.isBlank()){
                if (subTemplate.equalsIgnoreCase("STAT_CATEG_TEMP")) {
                    object = mxGroupListRepository.getFinanceRightsStatGroupDetails(repDate, template, Sort.by(sort));
                } else {
                    object = mxGroupListRepository.getFinanceRightsAcctrlGroupDetails(repDate, template, Sort.by(sort));
                }}else {
                    if(isGlobalSearch){
                        if (subTemplate.equalsIgnoreCase("STAT_CATEG_TEMP")) {
                            object = mxGroupListRepository.getFinanceRightsStatGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                        } else {
                            object = mxGroupListRepository.getFinanceRightsAcctrlGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                        }
                    }
                }
                break;
            case "CONFIGURATION_MANAGEMENT_RIGHTS":
                if(searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getConfigurationGroupDetails(repDate, template, Sort.by(sort));
                }else{
                    if(isGlobalSearch){
                        object = mxGroupListRepository.getConfigurationGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                    }
                }
                break;
            case "ENTERPRISE_RISK_MANAGEMENT":
                if (searchWhereClause == null || searchWhereClause.isBlank()) {
                    object = mxGroupListRepository.getEnterpriseGroupDetails(repDate, template, Sort.by(sort));
                }else{
                    if(isGlobalSearch){
                        object = mxGroupListRepository.getEnterpriseGroupDetailAndGlobalSearch(repDate, template, Sort.by(sort),searchWhereClause);
                    }
                }
                break;
            case "USER_LIST":
                break;
        }
        return object;
    }

}
