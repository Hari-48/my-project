package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxOspRightsMatrix;
import com.finsurge.tmr_portal.mx_superview.models.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface MxOSPRightsTemplateRepository extends CrudRepository<MxOspRightsMatrix, Long> {

    @Query(value = "select distinct t.ospRightTemplate from MxOspRightsMatrix t where t.sysDate = :date order by t.ospRightTemplate")
    List<String> findAllDistinctTemplateNamesForSysDate(LocalDate date);

    Page<MxOspRightsMatrix> findAllByOspRightTemplateAndSysDateOrderByCategoryAscSubCategoryAsc(String templateLabel, LocalDate date, Pageable pageable);

    MxOspRightsMatrix findTopByOspRightTemplateAndReportDate(String ospRightTemplate, LocalDate requestDate);

    // for compare
    @Query(value="select distinct t1 from MxOspRightsMatrix t1 where exists (select t2.validationRightTemplate from MxOspRightsMatrix t2 where t2.ospRightTemplate=:templateLabel"+
            " and t2.reportDate=:compareReportDate and t2.validationRightTemplate=t1.validationRightTemplate " +
            "and t2.category=t1.category " +
            "and (t2.subCategory =t1.subCategory or (t2.subCategory is null and t1.subCategory is null) )" +
            "and (t2.queue =t1.queue or (t2.queue is null and t1.queue is null) )) " +
            "and t1.ospRightTemplate=:templateLabel and  t1.reportDate=:baseReportDate order by t1.validationRightTemplate, t1.category, t1.subCategory, t1.queue")
    Page<MxOspRightsMatrix> findByOSPRightsByGroupLabelAndReportDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value="select distinct t1 from MxOspRightsMatrix t1 where not exists (select t2.validationRightTemplate from MxOspRightsMatrix t2 where t2.ospRightTemplate=:templateLabel"+
            " and t2.reportDate=:compareReportDate and t2.validationRightTemplate=t1.validationRightTemplate " +
            "and t2.category=t1.category " +
            "and (t2.subCategory =t1.subCategory or (t2.subCategory is null and t1.subCategory is null) )" +
            "and (t2.queue =t1.queue or (t2.queue is null and t1.queue is null) )) " +
            "and t1.ospRightTemplate=:templateLabel and  t1.reportDate=:baseReportDate order by t1.validationRightTemplate, t1.category, t1.subCategory, t1.queue")
    Page<MxOspRightsMatrix> findByAdditionalOsprightsByGroupLabelAndReportDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value="select distinct t1 from MxOspRightsMatrix t1 where not exists (select t2.validationRightTemplate from MxOspRightsMatrix t2 where t2.ospRightTemplate=:templateLabel"+
            " and t2.reportDate=:baseReportDate and t2.validationRightTemplate=t1.validationRightTemplate " +
            "and t2.category=t1.category " +
            "and (t2.subCategory =t1.subCategory or (t2.subCategory is null and t1.subCategory is null) )" +
            "and (t2.queue =t1.queue or (t2.queue is null and t1.queue is null)) ) " +
            "and t1.ospRightTemplate=:templateLabel and  t1.reportDate=:compareReportDate order by t1.validationRightTemplate, t1.category, t1.subCategory, t1.queue")
    Page<MxOspRightsMatrix> findByAdditionalOSPRightsByGroupLabelAndCompareReportDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    List<MxOspRightsMatrix> findTopByReportDateAndOspRightTemplateIn(LocalDate reportDate,List<String> ospRightTemplate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOspRightsTemplate(t.id,t.ospRightTemplate,t.validationRightTemplate,t.category,t.subCategory,t.queue,t.queueRight," +
            " t.bulkValidationEnabled,t.nonModifiableAutoSelection,t.filterOnData,t.dataFilterShared,t.action" +
            ",t.userActionEnabled,t.filterOnAction,t.actionFilterShared,t.technical,g.groupLabel) from MxOspRightsMatrix t inner join MxGroupsListItem g " +
            "on t.reportDate=g.reportDate where t.reportDate=:repDate and t.ospRightTemplate=g.ospRightTemplate")
    Page<CombinedOspRightsTemplate> findAllByReportDate(LocalDate repDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOspRightsTemplate(t.id,t.ospRightTemplate,t.validationRightTemplate,t.category,t.subCategory,t.queue,t.queueRight," +
            "  t.bulkValidationEnabled,t.nonModifiableAutoSelection,t.filterOnData,t.dataFilterShared,t.action" +
            ",t.userActionEnabled,t.filterOnAction,t.actionFilterShared,t.technical,g.groupLabel) from " +
            " MxOspRightsMatrix t  inner join MxGroupsListItem g" +
            " on t.ospRightTemplate=g.ospRightTemplate where t.reportDate = :requestDate and g.reportDate = :requestDate and g.groupLabel in :groupValue" +
            " and t.ospRightTemplate in :templateValue and g.ospRightTemplate in :templateValue")
    Page<CombinedOspRightsTemplate> findByGroupLabelAndReportDate(List<String> templateValue, List<String> groupValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select distinct t from MxOspRightsMatrix t where t.reportDate=:requestDate and t.ospRightTemplate=:templateValue")
    Page<MxOspRightsMatrix> findByTemplateAndReportDateList(String templateValue, LocalDate requestDate, Pageable pageable);

    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix( t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            "exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) )) " +
            " and (coalesce(:#{#filter.validationRightTmplList}) is null or t.validationRightTemplate in :#{#filter.validationRightTmplList}) and " +
            "(coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            "(coalesce(:#{#filter.subCategoryList}) is null or t.subCategory in :#{#filter.subCategoryList}) and " +
            "(coalesce(:#{#filter.queueList}) is null or t.queue in :#{#filter.queueList}) and " +
            "(coalesce(:#{#filter.queueRight}) is null or t.queueRight in :#{#filter.queueRight}) and " +
            "(coalesce(:#{#filter.bulkValidationEnabled}) is null or t.bulkValidationEnabled in :#{#filter.bulkValidationEnabled}) and " +
            "(coalesce(:#{#filter.nonModifiableAutoSelection}) is null or t.nonModifiableAutoSelection in :#{#filter.nonModifiableAutoSelection}) and " +
            "(coalesce(:#{#filter.filterOnData}) is null or t.filterOnData in :#{#filter.filterOnData}) and " +
            "(coalesce(:#{#filter.dataFilterShared}) is null or t.dataFilterShared in :#{#filter.dataFilterShared}) and " +
            "(coalesce(:#{#filter.action}) is null or t.action in :#{#filter.action}) and " +
            "(coalesce(:#{#filter.userActionEnabled}) is null or t.userActionEnabled in :#{#filter.userActionEnabled}) and " +
            "(coalesce(:#{#filter.filterOnAction}) is null or t.filterOnAction in :#{#filter.filterOnAction}) and " +
            "(coalesce(:#{#filter.actionFilterShared}) is null or t.actionFilterShared in :#{#filter.actionFilterShared}) and "+
            "(coalesce(:#{#filter.technical}) is null or t.technical in :#{#filter.technical}) and "+
            "  t.reportDate=:repDate and t.ospRightTemplate = :groupName order by t.validationRightTemplate, t.category, t.subCategory, t.queue")
    Page<OspRightsMatrix> findGroupCompareTreeMap(LocalDate repDate, String groupName, String compareGroupName,OspRightsMatrix filter, Pageable pageable);

    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix( t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            " not exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) ) and (p.action =t.action or (p.action is null and t.action is null) ))" +
            " and (coalesce(:#{#filter.validationRightTmplList}) is null or t.validationRightTemplate in :#{#filter.validationRightTmplList}) and " +
            "(coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            "(coalesce(:#{#filter.subCategoryList}) is null or t.subCategory in :#{#filter.subCategoryList}) and " +
            "(coalesce(:#{#filter.queueList}) is null or t.queue in :#{#filter.queueList}) and " +
            "(coalesce(:#{#filter.queueRight}) is null or t.queueRight in :#{#filter.queueRight}) and " +
            "(coalesce(:#{#filter.bulkValidationEnabled}) is null or t.bulkValidationEnabled in :#{#filter.bulkValidationEnabled}) and " +
            "(coalesce(:#{#filter.nonModifiableAutoSelection}) is null or t.nonModifiableAutoSelection in :#{#filter.nonModifiableAutoSelection}) and " +
            "(coalesce(:#{#filter.filterOnData}) is null or t.filterOnData in :#{#filter.filterOnData}) and " +
            "(coalesce(:#{#filter.dataFilterShared}) is null or t.dataFilterShared in :#{#filter.dataFilterShared}) and " +
            "(coalesce(:#{#filter.action}) is null or t.action in :#{#filter.action}) and " +
            "(coalesce(:#{#filter.userActionEnabled}) is null or t.userActionEnabled in :#{#filter.userActionEnabled}) and " +
            "(coalesce(:#{#filter.filterOnAction}) is null or t.filterOnAction in :#{#filter.filterOnAction}) and " +
            "(coalesce(:#{#filter.actionFilterShared}) is null or t.actionFilterShared in :#{#filter.actionFilterShared}) and "+
            "(coalesce(:#{#filter.technical}) is null or t.technical in :#{#filter.technical}) "+
            " and  t.reportDate=:repDate and t.ospRightTemplate = :groupName  order by t.validationRightTemplate, t.category, t.subCategory, t.queue")
    Page<OspRightsMatrix> findGroupCompareTreeMapAdditional(LocalDate repDate, String groupName, String compareGroupName,OspRightsMatrix filter, Pageable pageable);

    @Query("select t from MxOspRightsMatrix t where" +
            "  exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) )  and (p.action =t.action or (p.action is null and t.action is null) ) ) and " +
            " (coalesce(:#{#filter.validationRightTmplList}) is null or t.validationRightTemplate in :#{#filter.validationRightTmplList}) and " +
            "(coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            "(coalesce(:#{#filter.subCategoryList}) is null or t.subCategory in :#{#filter.subCategoryList}) and " +
            "(coalesce(:#{#filter.queueList}) is null or t.queue in :#{#filter.queueList}) and " +
            "(coalesce(:#{#filter.queueRightList}) is null or t.queueRight in :#{#filter.queueRightList}) and " +
            "(coalesce(:#{#filter.bulkValidationEnabledList}) is null or t.bulkValidationEnabled in :#{#filter.bulkValidationEnabledList}) and " +
            "(coalesce(:#{#filter.nonModifiableAutoSelectionList}) is null or t.nonModifiableAutoSelection in :#{#filter.nonModifiableAutoSelectionList}) and " +
            "(coalesce(:#{#filter.filterOnDataList}) is null or t.filterOnData in :#{#filter.filterOnDataList}) and " +
            "(coalesce(:#{#filter.dataFilterSharedList}) is null or t.dataFilterShared in :#{#filter.dataFilterSharedList}) and " +
            "(coalesce(:#{#filter.actionList}) is null or t.action in :#{#filter.actionList}) and " +
            "(coalesce(:#{#filter.userActionEnabledList}) is null or t.userActionEnabled in :#{#filter.userActionEnabledList}) and " +
            "(coalesce(:#{#filter.filterOnActionList}) is null or t.filterOnAction in :#{#filter.filterOnActionList}) and " +
            "(coalesce(:#{#filter.actionFilterSharedList}) is null or t.actionFilterShared in :#{#filter.actionFilterSharedList}) and "+
            "(coalesce(:#{#filter.technicalList}) is null or t.technical in :#{#filter.technicalList}) and t.ospRightTemplate =:groupName" +
            "  and  t.reportDate=:repDate  order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action")
    Page<MxOspRightsMatrix> findAllGroupCompareMatched(LocalDate repDate, String groupName,String compareGroupName,OspRightsMatrix filter, Pageable pageable);

//    @Query("select t from MxOspRightsMatrix t where" +
//            " not exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
//            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
//            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
//            " and (p.queue =t.queue or (p.queue is null and t.queue is null) )   and (p.action =t.action or (p.action is null and t.action is null) ) )and " +
//            " (coalesce(:#{#filter.validationRightTmplList}) is null or t.validationRightTemplate in :#{#filter.validationRightTmplList}) and " +
//            "(coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
//            "(coalesce(:#{#filter.subCategoryList}) is null or t.subCategory in :#{#filter.subCategoryList}) and " +
//            "(coalesce(:#{#filter.queueList}) is null or t.queue in :#{#filter.queueList}) and " +
//            "(coalesce(:#{#filter.queueRightList}) is null or t.queueRight in :#{#filter.queueRightList}) and " +
//            "(coalesce(:#{#filter.bulkValidationEnabledList}) is null or t.bulkValidationEnabled in :#{#filter.bulkValidationEnabledList}) and " +
//            "(coalesce(:#{#filter.nonModifiableAutoSelectionList}) is null or t.nonModifiableAutoSelection in :#{#filter.nonModifiableAutoSelectionList}) and " +
//            "(coalesce(:#{#filter.filterOnDataList}) is null or t.filterOnData in :#{#filter.filterOnDataList}) and " +
//            "(coalesce(:#{#filter.dataFilterSharedList}) is null or t.dataFilterShared in :#{#filter.dataFilterSharedList}) and " +
//            "(coalesce(:#{#filter.actionList}) is null or t.action in :#{#filter.actionList}) and " +
//            "(coalesce(:#{#filter.userActionEnabledList}) is null or t.userActionEnabled in :#{#filter.userActionEnabledList}) and " +
//            "(coalesce(:#{#filter.filterOnActionList}) is null or t.filterOnAction in :#{#filter.filterOnActionList}) and " +
//            "(coalesce(:#{#filter.actionFilterSharedList}) is null or t.actionFilterShared in :#{#filter.actionFilterSharedList}) and "+
//            "(coalesce(:#{#filter.technicalList}) is null or t.technical in :#{#filter.technicalList}) and t.ospRightTemplate =:groupName" +
//            "  and  t.reportDate=:repDate  order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action")
//    Page<MxOspRightsMatrix> findAllGroupCompareAdditional(LocalDate repDate, String groupName,String compareGroupName,OspRightsMatrix filter, Pageable pageable);

    @Query("select t from MxOspRightsMatrix t where" +
            " not exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) )   and (p.action =t.action or (p.action is null and t.action is null) ) )and " +
            " (coalesce(:#{#filter.validationRightTmplList}) is null or t.validationRightTemplate in :#{#filter.validationRightTmplList}) and " +
            "(coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            "(coalesce(:#{#filter.subCategoryList}) is null or t.subCategory in :#{#filter.subCategoryList}) and " +
            "(coalesce(:#{#filter.queueList}) is null or t.queue in :#{#filter.queueList}) and t.ospRightTemplate =:groupName" +
            "  and  t.reportDate=:repDate ")
    Page<MxOspRightsMatrix> findAllGroupCompareAdditional(LocalDate repDate, String groupName,String compareGroupName,OspRightsMatrix filter, Pageable pageable);

    @Query(value=" select t from MxOspRightsMatrix t where t.reportDate =:reportDate and  ( t.validationRightTemplate in (:validation)  or t.validationRightTemplate is null )" +
            "and (t.category in (:categoryList) or t.category is null)  and ( t.subCategory in (:subCategoryList) or t.subCategory is null ) and (t.queue in (:queueList) or t.queue is null )" +
            "and ( t.action in (:actionList) or t.action is null )  and t.ospRightTemplate=:compareGroupName order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action")
    List<MxOspRightsMatrix> findMatchedDataByTree(LocalDate reportDate,String compareGroupName, List<String> validation, List<String>  categoryList, List<String>  subCategoryList, List<String>  queueList,List<String> actionList);

    @Query(value= "select t from MxOspRightsMatrix t where " +
            "exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) ) and (p.action =t.action or (p.action is null and t.action is null) )  ) and  t.reportDate=:repDate and t.ospRightTemplate = :groupName order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action ")
    Page<MxOspRightsMatrix> findAllGroupCompareMatchedExport(LocalDate repDate, String groupName,String compareGroupName, Pageable pageable);

    @Query(value= "select t from MxOspRightsMatrix t where " +
            "not exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareGroupName  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) ) and (p.action =t.action or (p.action is null and t.action is null) )  ) and  t.reportDate=:repDate and t.ospRightTemplate = :groupName order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action")
    Page<MxOspRightsMatrix> findAllGroupCompareAdditionalExport(LocalDate repDate, String groupName,String compareGroupName, Pageable pageable);

    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix(t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            "exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareTemplate  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) ) and (p.action =t.action or (p.action is null and t.action is null))  ) and  t.reportDate=:repDate and t.ospRightTemplate = :template order by t.validationRightTemplate, t.category, t.subCategory, t.queue ")
    List<OspRightsMatrix> findGeneralPropertyListForOspMatched( String template, String compareTemplate, LocalDate repDate);


    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix(t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            " not exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareTemplate  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) ) and (p.action =t.action or (p.action is null and t.action is null))  ) and  t.reportDate=:repDate and t.ospRightTemplate = :template ")
    List<OspRightsMatrix> findGeneralPropertyListForOspAdditional( String template, String compareTemplate, LocalDate repDate);

    @Query(value= "select t from MxOspRightsMatrix t where exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareTemplate  and" +
            " p.reportDate=:reportDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null)) and (p.action =t.action or (p.action is null and t.action is null))  and (coalesce(t.queueRight,'null')<>(coalesce(p.queueRight,'null')) " +
            " or (coalesce(t.bulkValidationEnabled,'null')<>coalesce(p.bulkValidationEnabled ,'null')) or (coalesce(t.nonModifiableAutoSelection,'null')<>coalesce(p.nonModifiableAutoSelection,'null') )" +
            " or (coalesce(t.filterOnData,'null')<>coalesce(p.filterOnData ,'null')) or (coalesce(t.dataFilterShared,'null')<>coalesce(p.dataFilterShared ,'null')) " +
            " or (coalesce(t.userActionEnabled,'null')<>coalesce(p.userActionEnabled ,'null')) or (coalesce(t.filterOnAction,'null')<>coalesce(p.filterOnAction ,'null'))" +
            " or (coalesce(t.actionFilterShared,'null')<>coalesce(p.actionFilterShared ,'null'))" +
            " or (coalesce(t.technical,'null')<>coalesce(p.technical ,'null')))) and  t.reportDate=:reportDate and t.ospRightTemplate = :template order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action ")
    Page<MxOspRightsMatrix> findAllGroupCompareUnMatched(LocalDate reportDate, String template, String compareTemplate, Pageable pageable);

    @Query(value= "select t from MxOspRightsMatrix t where  exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:compareTemplate  and" +
            " p.reportDate=:reportDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null)) and (p.action =t.action or (p.action is null and t.action is null))  and (t.queueRight=p.queueRight or (t.queueRight is null and p.queueRight is null)) " +
            "and (t.bulkValidationEnabled =p.bulkValidationEnabled or (t.bulkValidationEnabled is null and p.bulkValidationEnabled is null)) and (t.nonModifiableAutoSelection=p.nonModifiableAutoSelection or (t.nonModifiableAutoSelection is null and p.nonModifiableAutoSelection is null)) and " +
            "  (t.filterOnData = p.filterOnData or (t.filterOnData is null and p.filterOnData is null)) and (t.dataFilterShared = p.dataFilterShared or (t.dataFilterShared is null and p.dataFilterShared is null)) " +
            " and (t.userActionEnabled = p.userActionEnabled   or (t.userActionEnabled is null and p.userActionEnabled is null)) and (t.filterOnAction = p.filterOnAction or (t.filterOnAction is null and p.filterOnAction is null)) " +
            " and (t.actionFilterShared=p.actionFilterShared or (t.actionFilterShared is null and p.actionFilterShared is null))" +
            " and  (t.technical=p.technical or (t.technical is null and p.technical is null))) and  t.reportDate=:reportDate and t.ospRightTemplate = :template order by t.validationRightTemplate, t.category, t.subCategory, t.queue,t.action ")
    Page<MxOspRightsMatrix> findAllGroupCompareMatchedList(LocalDate reportDate, String template, String compareTemplate, Pageable pageable);

    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix(t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            "exists  (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:#{#filter.compareTemplate}  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null)) and (p.action =t.action or (p.action is null and t.action is null))  and (coalesce(t.queueRight,'null')<>(coalesce(p.queueRight,'null')) " +
            " or (coalesce(t.bulkValidationEnabled,'null')<>coalesce(p.bulkValidationEnabled ,'null')) or (coalesce(t.nonModifiableAutoSelection,'null')<>coalesce(p.nonModifiableAutoSelection,'null') )" +
            " or (coalesce(t.filterOnData,'null')<>coalesce(p.filterOnData ,'null')) or (coalesce(t.dataFilterShared,'null')<>coalesce(p.dataFilterShared ,'null')) " +
            " or (coalesce(t.userActionEnabled,'null')<>coalesce(p.userActionEnabled ,'null')) or (coalesce(t.filterOnAction,'null')<>coalesce(p.filterOnAction ,'null'))" +
            " or (coalesce(t.actionFilterShared,'null')<>coalesce(p.actionFilterShared ,'null'))" +
            " or (coalesce(t.technical,'null')<>coalesce(p.technical ,'null')))) and " +
            " t.reportDate=:repDate and t.ospRightTemplate = :#{#filter.template} ")
    List<OspRightsMatrix> findGeneralPropertyListForOspUnMatched( GroupCompareFilter filter,  LocalDate repDate);

    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix(t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            "exists  (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:#{#filter.compareTemplate}   and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null)) and (p.action =t.action or (p.action is null and t.action is null))  and (t.queueRight=p.queueRight or (t.queueRight is null and p.queueRight is null)) " +
            "and (t.bulkValidationEnabled =p.bulkValidationEnabled or (t.bulkValidationEnabled is null and p.bulkValidationEnabled is null)) and (t.nonModifiableAutoSelection=p.nonModifiableAutoSelection or (t.nonModifiableAutoSelection is null and p.nonModifiableAutoSelection is null)) and " +
            "  (t.filterOnData = p.filterOnData or (t.filterOnData is null and p.filterOnData is null)) and (t.dataFilterShared = p.dataFilterShared or (t.dataFilterShared is null and p.dataFilterShared is null)) " +
            " and (t.userActionEnabled = p.userActionEnabled   or (t.userActionEnabled is null and p.userActionEnabled is null)) and (t.filterOnAction = p.filterOnAction or (t.filterOnAction is null and p.filterOnAction is null)) " +
            " and (t.actionFilterShared=p.actionFilterShared or (t.actionFilterShared is null and p.actionFilterShared is null))" +
            " and  (t.technical=p.technical or (t.technical is null and p.technical is null))) and " +
            " t.reportDate=:repDate and t.ospRightTemplate = :#{#filter.template}" )
    List<OspRightsMatrix> findGeneralPropertyListForOspMatched(  GroupCompareFilter filter,  LocalDate repDate);

    @Query(value= "select new com.finsurge.tmr_portal.mx_superview.models.OspRightsMatrix(t.validationRightTemplate,t.category, t.subCategory, t.queue) from MxOspRightsMatrix t where " +
            "exists (select p.id from MxOspRightsMatrix p where p.ospRightTemplate =:#{#filter.compareTemplate}  and" +
            " p.reportDate=:repDate and (p.validationRightTemplate=t.validationRightTemplate or (p.validationRightTemplate is null and t.validationRightTemplate is null))" +
            " and (p.category=t.category or (p.category is null and t.category is null))   and (p.subCategory =t.subCategory or (p.subCategory is null and t.subCategory is null) )" +
            " and (p.queue =t.queue or (p.queue is null and t.queue is null) ) and (p.action =t.action or (p.action is null and t.action is null))  ) and  t.reportDate=:repDate " +
            "and t.ospRightTemplate = :#{#filter.template} ")
    List<OspRightsMatrix> findGeneralPropertyListForOspAll (GroupCompareFilter filter, LocalDate repDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOspRightsTemplate(t.id, t.ospRightTemplate, t.validationRightTemplate, t.category, t.subCategory, t.queue, t.queueRight, " +
            "t.bulkValidationEnabled, t.nonModifiableAutoSelection, t.filterOnData, t.dataFilterShared, t.action, t.userActionEnabled, t.filterOnAction, t.actionFilterShared, t.technical, g.groupLabel) " +
            "from MxOspRightsMatrix t inner join MxGroupsListItem g on t.reportDate = g.reportDate where t.reportDate = :requestDate and t.ospRightTemplate = g.ospRightTemplate and (upper(t.validationRightTemplate) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.category) like (concat('%', upper(:searchString), '%')) or upper(t.subCategory) like (concat('%', upper(:searchString), '%')) or upper(t.queue) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.queueRight) like (concat('%', upper(:searchString), '%')) or upper(t.bulkValidationEnabled) like (concat('%', upper(:searchString), '%')) or upper(t.nonModifiableAutoSelection) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.filterOnData) like (concat('%', upper(:searchString), '%')) or upper(t.dataFilterShared) like (concat('%', upper(:searchString), '%')) or upper(t.action) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.userActionEnabled) like (concat('%', upper(:searchString), '%')) or upper(t.filterOnAction) like (concat('%', upper(:searchString), '%')) or upper(t.actionFilterShared) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.technical) like (concat('%', upper(:searchString), '%')) or upper(t.ospRightTemplate) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedOspRightsTemplate> findOspRightsGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOspRightsTemplate(t.id, t.ospRightTemplate, t.validationRightTemplate, t.category, t.subCategory, t.queue, t.queueRight, " +
            "t.bulkValidationEnabled, t.nonModifiableAutoSelection, t.filterOnData, t.dataFilterShared, t.action, t.userActionEnabled, t.filterOnAction, t.actionFilterShared, t.technical, g.groupLabel) " +
            "from MxOspRightsMatrix t inner join MxGroupsListItem g on t.ospRightTemplate = g.ospRightTemplate where t.reportDate = :requestDate and g.reportDate = :requestDate and g.groupLabel in :groupValue " +
            "and t.ospRightTemplate in :templateValue and g.ospRightTemplate in :templateValue and (upper(t.validationRightTemplate) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.category) like (concat('%', upper(:searchString), '%')) or upper(t.subCategory) like (concat('%', upper(:searchString), '%')) or upper(t.queue) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.queueRight) like (concat('%', upper(:searchString), '%')) or upper(t.bulkValidationEnabled) like (concat('%', upper(:searchString), '%')) or upper(t.nonModifiableAutoSelection) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.filterOnData) like (concat('%', upper(:searchString), '%')) or upper(t.dataFilterShared) like (concat('%', upper(:searchString), '%')) or upper(t.action) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.userActionEnabled) like (concat('%', upper(:searchString), '%')) or upper(t.filterOnAction) like (concat('%', upper(:searchString), '%')) or upper(t.actionFilterShared) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.technical) like (concat('%', upper(:searchString), '%')) or upper(t.ospRightTemplate) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedOspRightsTemplate> findOspRightsGlobalCombined(List<String> templateValue, List<String> groupValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select distinct t from MxOspRightsMatrix t where t.reportDate=:requestDate and t.ospRightTemplate=:templateValue and (upper(t.category) like (concat('%', upper(:searchString), '%')) or" +
            " upper(t.action) like (concat('%', upper(:searchString), '%')) or upper(t.actionFilterShared) like (concat('%', upper(:searchString), '%')) or upper(t.bulkValidationEnabled) like " +
            "(concat('%', upper(:searchString), '%')) or upper(t.dataFilterShared) like (concat('%', upper(:searchString), '%')) or upper(t.filterOnAction) like (concat('%', upper(:searchString), '%'))" +
            " or upper(t.filterOnData) like (concat('%', upper(:searchString), '%')) or upper(t.nonModifiableAutoSelection) like (concat('%', upper(:searchString), '%')) " +
            "or upper(t.queue) like (concat('%', upper(:searchString), '%')) or upper(t.queueRight) like (concat('%', upper(:searchString), '%')) or upper(t.subCategory) like (concat('%', upper(:searchString), '%')) " +
            "or upper(t.technical) like (concat('%', upper(:searchString), '%')) or upper(t.userActionEnabled) like (concat('%', upper(:searchString), '%')) or upper(t.validationRightTemplate) like " +
            "(concat('%', upper(:searchString), '%')))")
    Page<MxOspRightsMatrix> findByTemplateAndReportDateListWithGlobalSearch(String templateValue, LocalDate requestDate,String searchString, Pageable pageable);

}

