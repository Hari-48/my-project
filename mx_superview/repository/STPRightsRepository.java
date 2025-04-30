package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxStpRightMatrixEod;


import com.finsurge.tmr_portal.mx_superview.models.CombinedStpRightsTemplate;
import com.finsurge.tmr_portal.mx_superview.models.StpMatrixModel;
import org.springframework.data.domain.Page;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface STPRightsRepository extends CrudRepository<MxStpRightMatrixEod,Long> {

    @Query(value = "select t from MxStpRightMatrixEod t where t.globalTemplate=:globalTemplate and t.reportDate=:reportDate")
    Page<MxStpRightMatrixEod> findDistinctByReportDateAndGlobalTemplate(LocalDate reportDate,String globalTemplate, Pageable pageable);

    MxStpRightMatrixEod findTopByReportDateAndGlobalTemplate(LocalDate requestDate,String globalTemplate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.StpMatrixModel(t.boTemplate, t.boType, t.globalTemplate, t.groupingTemplate, g.groupLabel) from MxStpRightMatrixEod t inner join MxGroupsListItem g " +
            "on t.globalTemplate = g.stpRgtTmpl where t.reportDate = :requestDate and g.reportDate = :requestDate and t.globalTemplate = :templateValue and t.boTemplate = :boTemplate and t.boType = :boType " +
            "group by t.boTemplate, t.boType, t.globalTemplate, t.groupingTemplate, g.groupLabel")
    Page<StpMatrixModel> findStpMatrixFilterByRepDateAndTemplate(LocalDate requestDate, String templateValue, String boTemplate, String boType, Pageable pageable);


    @Query(value = "select distinct t.BO_TEMPLATE,t.BO_TYPE,t.GLOBAL_TEMPLATE,t.GROUPING_TEMPLATE,g.GROUP_LABEL from UAM_MX_STP_RIGHTS_MATRIX_EOD t inner join UAM_MX_GROUP_LIST g  on t.GLOBAL_TEMPLATE=g.STP_RGT_TMPL where t.REP_DATE=:requestDate and g.REP_DATE=:requestDate" +
            " and t.GLOBAL_TEMPLATE=:templateValue and t.BO_TYPE=:boType and t.BO_TEMPLATE=:boTemplate group by t.BO_TEMPLATE,t.BO_TYPE,t.GLOBAL_TEMPLATE,t.GROUPING_TEMPLATE,g.GROUP_LABEL",nativeQuery = true)
    List<Object[]> findStpMatrixFilterByRepDateAndTemplateNative(String requestDate, String templateValue, String boTemplate, String boType);

    @Query(value = "select distinct t from MxStpRightMatrixEod t where t.reportDate=:requestDate and t.globalTemplate=:templateValue and (upper(t.globalTemplate) like (concat('%', upper(:searchString), '%')) " +
            "or upper(t.srcModule) like (concat('%', upper(:searchString), '%')) or upper(t.boType) like (concat('%', upper(:searchString), '%')) or upper(t.boTemplate) like (concat('%', upper(:searchString), '%'))" +
            " or upper(t.groupingTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.typologyGroup) like (concat('%', upper(:searchString), '%')) or upper(t.typology) like (concat('%', upper(:searchString), '%')) " +
            "or upper(t.rightProfile) like (concat('%', upper(:searchString), '%')) or upper(t.actionEvent) like (concat('%', upper(:searchString), '%')) or upper(t.status) like (concat('%', upper(:searchString), '%'))" +
            " or upper(t.view) like (concat('%', upper(:searchString), '%')))")
    Page<MxStpRightMatrixEod> findByTemplateAndReportDateListWithGlobalSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    MxStpRightMatrixEod findTopByRightProfileAndReportDate(String rightsProfile,LocalDate repDate);

    List<MxStpRightMatrixEod> findDistinctTopByGlobalTemplateInAndReportDate(List<String> templateValue,LocalDate repDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedStpRightsTemplate(t.id,t.globalTemplate, t.boType, t.srcModule, t.typology, t.actionEvent, " +
            " t.status, t.view, t.boTemplate, t.groupingTemplate, t.typologyGroup, t.rightProfile,g.groupLabel) from " +
            " MxStpRightMatrixEod t inner join MxGroupsListItem g " +
            " on t.globalTemplate=g.stpRgtTmpl where g.stpRgtTmpl in :templateValue and t.globalTemplate in :templateValue " +
            " and t.reportDate=:requestDate and g.groupLabel in :groupValue" +
            " and g.reportDate=:requestDate")
    Page<CombinedStpRightsTemplate> findSelectedGroupDetails(List<String> templateValue, List<String> groupValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedStpRightsTemplate(t.id,t.globalTemplate, t.boType, t.srcModule, t.typology, t.actionEvent, " +
            " t.status, t.view, t.boTemplate, t.groupingTemplate, t.typologyGroup, t.rightProfile,g.groupLabel) from " +
            " MxStpRightMatrixEod t inner join MxGroupsListItem g " +
            " on t.globalTemplate=g.stpRgtTmpl where t.reportDate=:requestDate and g.reportDate=:requestDate")
    Page<CombinedStpRightsTemplate> findAllGroupDetailsUsingReportDate(LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedStpRightsTemplate(t.id,t.globalTemplate, t.boType, t.srcModule, t.typology, t.actionEvent, " +
            " t.status, t.view, t.boTemplate, t.groupingTemplate, t.typologyGroup, t.rightProfile,g.groupLabel) from " +
            " MxStpRightMatrixEod t inner join MxGroupsListItem g " +
            " on t.globalTemplate=g.stpRgtTmpl where t.reportDate=:requestDate and g.reportDate=:requestDate and (upper(t.globalTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.boType) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.srcModule) like (concat('%', upper(:searchString), '%')) or upper(t.typology) like (concat('%', upper(:searchString), '%')) or upper(t.actionEvent) like (concat('%', upper(:searchString), '%')) or upper(t.status) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.view) like (concat('%', upper(:searchString), '%')) or upper(t.boTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.groupingTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.typologyGroup) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.rightProfile) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedStpRightsTemplate> findAllGroupDetailsGlobalSearch(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedStpRightsTemplate(t.id,t.globalTemplate, t.boType, t.srcModule, t.typology, t.actionEvent, " +
            " t.status, t.view, t.boTemplate, t.groupingTemplate, t.typologyGroup, t.rightProfile,g.groupLabel) from " +
            " MxStpRightMatrixEod t inner join MxGroupsListItem g " +
            " on t.globalTemplate=g.stpRgtTmpl where g.groupLabel in :groupValue and g.stpRgtTmpl in :templateValue and t.globalTemplate in :templateValue and t.reportDate=:requestDate and g.reportDate=:requestDate and (upper(t.globalTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.boType) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.srcModule) like (concat('%', upper(:searchString), '%')) or upper(t.typology) like (concat('%', upper(:searchString), '%')) or upper(t.actionEvent) like (concat('%', upper(:searchString), '%')) or upper(t.status) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.view) like (concat('%', upper(:searchString), '%')) or upper(t.boTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.groupingTemplate) like (concat('%', upper(:searchString), '%')) or upper(t.typologyGroup) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.rightProfile) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedStpRightsTemplate> findSelectedGroupDetailsByGlobalSearch(List<String> templateValue, List<String> groupValue, LocalDate requestDate, String searchString, Pageable pageable);


//    @Query("select t from STPRightsMatrix t where t.reportDate=:reportDate and t.globalTem=:globelTem")
//    Page<STPRightsMatrix> findDistinctByReportDateAndGlobalTem(LocalDate reportDate, String globelTem, Pageable pageable);
//
//    STPRightsMatrix findTopByGlobalTemAndReportDate(String globalTem, LocalDate requestDate);
//
//
//
//    @Query(value = "select t from STPRightsMatrix t  where t.globalTem=:template " +
//            " and t.reportDate=:baseReportDate  and exists (select p.id from STPRightsMatrix p where " +
//            "p.boType=t.boType and p.typology=t.typology and (p.action=t.action or (p.action is null and t.action is null))" +
//            "and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:template " +
//            " and p.reportDate=:compareReportDate ) order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findByStpByGroupLabelAndReportDate(String template, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where t.globalTem=:template " +
//            " and t.reportDate=:baseReportDate  and not exists (select p.id from STPRightsMatrix p where " +
//            "p.boType=t.boType and p.typology=t.typology and (p.action=t.action or (p.action is null and t.action is null))" +
//            "and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:template " +
//            " and p.reportDate=:compareReportDate ) order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> getAdditionalStpMatrixByRepDate(String template, LocalDate baseReportDate, LocalDate compareReportDate,Pageable pageable);
//
//
//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.STPModel(t.boType,t.typology,t.action,t.status,t.stpView) from STPRightsMatrix t  where exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and (p.status=t.status or (p.status is null and t.status is null)) and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " and (:boType is null or t.boType=:boType ) and (:typology is null or t.typology=:typology) and (:action is null or t.action=:action ) and (:status is null or t.status=:status)" +
//            "and (:stpView is null or t.stpView=:stpView) order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPModel> findGroupCompareStpTreeMap(LocalDate repDate, String template, String compareTemplate,String boType,String typology,String action,String status,String stpView, Pageable pageable);
//
//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.STPModel(t.boType,t.typology,t.action,t.status,t.stpView) from STPRightsMatrix t  where not exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and (p.status=t.status or (p.status is null and t.status is null)) and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " and (:boType is null or t.boType=:boType ) and (:typology is null or t.typology=:typology) and (:action is null or t.action=:action ) and (:status is null or t.status=:status)" +
//            "and (:stpView is null or t.stpView=:stpView) order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPModel> findGroupCompareStpTreeMapAdditional(LocalDate repDate, String template, String compareTemplate,String boType,String typology,String action,String status,String stpView, Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and (p.status=t.status or (p.status is null and t.status is null)) and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " and (coalesce(:#{#filter.boTypeList}) is null or t.boType in :#{#filter.boTypeList}) and " +
//            "(coalesce(:#{#filter.typologyList}) is null or t.typology in :#{#filter.typologyList}) and " +
//            "(coalesce(:#{#filter.sourceModList}) is null or t.sourceMod in :#{#filter.sourceModList}) and " +
//            "(coalesce(:#{#filter.actionList}) is null or t.action in :#{#filter.actionList}) and " +
//            "(coalesce(:#{#filter.statusList}) is null or t.status in :#{#filter.statusList}) and " +
//            "(coalesce(:#{#filter.stpViewList}) is null or t.stpView in :#{#filter.stpViewList}) and " +
//            "(coalesce(:#{#filter.checkedList}) is null or t.checked in :#{#filter.checkedList})  " +
//            " order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findAllGroupCompareStpMatched(LocalDate repDate, String template, String compareTemplate,STPModel filter, Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where  not exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and (p.status=t.status or (p.status is null and t.status is null)) and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " and (coalesce(:#{#filter.boTypeList}) is null or t.boType in :#{#filter.boTypeList}) and " +
//            "(coalesce(:#{#filter.typologyList}) is null or t.typology in :#{#filter.typologyList}) and " +
//            "(coalesce(:#{#filter.sourceModList}) is null or t.sourceMod in :#{#filter.sourceModList}) and " +
//            "(coalesce(:#{#filter.actionList}) is null or t.action in :#{#filter.actionList}) and " +
//            "(coalesce(:#{#filter.statusList}) is null or t.status in :#{#filter.statusList}) and " +
//            "(coalesce(:#{#filter.stpViewList}) is null or t.stpView in :#{#filter.stpViewList}) and " +
//            "(coalesce(:#{#filter.checkedList}) is null or t.checked in :#{#filter.checkedList})  " +
//            " order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findAllGroupCompareStpAdditional(LocalDate repDate, String template, String compareTemplate,STPModel filter, Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where  exists (select p.id from STPRightsMatrix p where " +
//            "p.boType=t.boType and p.typology=t.typology and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " and (:boType is null or t.boType=:boType ) and (:typology is null or t.typology=:typology) and (:action is null or t.action=:action ) and (:status is null or t.status=:status)" +
//            " and (:stpView is null or t.stpView=:stpView) order by t.boType,t.typology,t.action,t.status,t.stpView")
//    List<String> findAllMatchedBotype(String template, String compareTemplate, LocalDate repDate);
//
//    @Query(value=" select t from STPRightsMatrix t where t.reportDate =:repDate and (t.boType in (:boTypeList) or t.boType is null )   " +
//            " and (t.typology in (:typologyList) or t.typology is null ) and  (t.action in (:actionList) or t.action is null)  and " +
//            " (t.status in (:statusList) or t.status is null)   and (t.stpView in (:viewList) or t.stpView is null)  " +
//            " and t.globalTem=:compareTemplate order by t.boType,t.typology,t.action,t.status,t.stpView")
//    List<STPRightsMatrix> findMatchedDataByStpTree(LocalDate repDate, String compareTemplate, List<String> boTypeList, List<String> typologyList, List<String> actionList, List<String> statusList, List<String> viewList);
//
//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.STPModel(t.boType,t.typology,t.action,t.status,t.stpView) from STPRightsMatrix t  where  exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and (p.status=t.status or (p.status is null and t.status is null)) and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            "  order by t.boType,t.typology,t.action,t.status,t.stpView")
//    List<STPModel> findGeneralPropertyListForStpMatched(String template, String compareTemplate, LocalDate repDate);
//
//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.STPModel(t.boType,t.typology,t.action,t.status,t.stpView) from STPRightsMatrix t  where not exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and (p.status=t.status or (p.status is null and t.status is null)) and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            "  order by t.boType,t.typology,t.action,t.status,t.stpView")
//    List<STPModel> findGeneralPropertyListForStpAdditional(String template, String compareTemplate, LocalDate repDate);
//
//    @Query(value = "select t from STPRightsMatrix t  where  exists (select p.id from STPRightsMatrix p where " +
//            "(p.boType=t.boType or (p.boType is null and t.boType is null)) and (p.typology=t.typology or (p.typology is null and t.typology is null)) and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null)) and (p.sourceMod=t.sourceMod or (p.sourceMod is null and t.sourceMod is null)) and " +
//            "(p.checked=t.checked or (p.checked is null and t.checked is null)) " +
//            "and p.globalTem=:compareTemplate and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findAllGroupComparStpMatchedList(LocalDate repDate, String template, String compareTemplate,  Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where  exists (select p.id from STPRightsMatrix p where " +
//            "p.boType=t.boType and p.typology=t.typology and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null)) and ((coalesce(p.sourceMod,'null')<>coalesce(t.sourceMod,'null')) or " +
//            " (coalesce(p.checked,'null')<>coalesce(t.checked,'null'))) and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findAllGroupComparStpUnMatched(LocalDate repDate, String template, String compareTemplate,  Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where  exists (select p.id from STPRightsMatrix p where " +
//            "p.boType=t.boType and p.typology=t.typology and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findAllMatchedStp(String template, String compareTemplate, LocalDate repDate,Pageable pageable);
//
//    @Query(value = "select t from STPRightsMatrix t  where not exists (select p.id from STPRightsMatrix p where " +
//            "p.boType=t.boType and p.typology=t.typology and (p.action=t.action or (p.action is null and t.action is null))" +
//            " and p.status=t.status and  (p.stpView=t.stpView or (p.stpView is null and t.stpView is null))  and p.globalTem=:compareTemplate " +
//            " and p.reportDate=:repDate ) and t.globalTem=:template and t.reportDate=:repDate" +
//            " order by t.boType,t.typology,t.action,t.status,t.stpView")
//    Page<STPRightsMatrix> findAllAdditionalStp(String template, String compareTemplate, LocalDate repDate,Pageable pageable);

}
