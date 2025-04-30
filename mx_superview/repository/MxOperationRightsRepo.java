package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxOperationRights;
import com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate;
import com.finsurge.tmr_portal.mx_superview.models.OperationalRights;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface MxOperationRightsRepo extends CrudRepository<MxOperationRights,Long> {

    @Query(value = "select distinct t.template from MxOperationRights t where t.sysDate = :date and t.operRgts='NKEY'")
    List<String> findDistinctTemplatesByNkey(LocalDate date);

    @Query(value = "select distinct t.template from MxOperationRights t where t.sysDate = :date and t.operRgts='LPOS'")
    List<String> findDistinctTemplatesByLPOS(LocalDate date);

    Page<MxOperationRights> findAllByTemplateAndSysDate(String template,LocalDate sysDate, Pageable pageable);

    MxOperationRights findTopByTemplateAndReportDate(String template, LocalDate requestDate);


    @Query(value = "select distinct t from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :template and m.reportDate = :compareReportDate " +
            " and t.operRgts =m.operRgts and t.key=m.key) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :baseReportDate  order by t.template,t.operRgts,t.key ")
    Page<MxOperationRights> findByMxOperationRightsByGroupLabelAndReportDate(String template, String subTemplate, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value = "select distinct t from MxOperationRights t where not exists (select m.id from MxOperationRights m where m.template = :template " +
            " and t.operRgts =m.operRgts and t.key=m.key  and m.reportDate = :compareReportDate) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :baseReportDate  order by t.template,t.operRgts,t.key ")
    Page<MxOperationRights> findByAdditionalOperationRightsByGroupLabelAndReportDate(String template, String subTemplate, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);


    @Query(value = "select distinct t from MxOperationRights t where not exists (select m.id from MxOperationRights m where m.template = :template and m.reportDate = :baseReportDate " +
            " and t.operRgts =m.operRgts and t.key=m.key) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :compareReportDate  order by t.template,t.operRgts,t.key ")
    Page<MxOperationRights> findByAdditionalOperationRightsByGroupLabelAndCompareReportDate(String template, String subTemplate, LocalDate baseReportDate, LocalDate compareReportDate,Pageable pageable);


    //combined operation Rights Count
    List<MxOperationRights> findTopByTemplateInAndReportDate(List<String> template, LocalDate requestDate);

    //Combined operation Rights SubTemplateValue as LPOS
    @Query(value = "select new  com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id,t.operRgts,t.template,t.key,t.avp1,t.avp2,t.avp3,t.avp4,t.fifo1,t.fifo2,t.fifo3,t.fifo4,t.realTime,t.eventType,t.evtAccess,t.evtInsert,t.evtModify,t.evtDelete,g.groupLabel) " +
            "from MxOperationRights t inner join MxGroupsListItem g on t.template=g.lposRight where " +
            "t.reportDate=:requestDate and t.template in :templateValue and t.operRgts=:subTemplateValue and g.reportDate=:requestDate and g.groupLabel in :groupValue and g.lposRight in :templateValue")
    Page<CombinedOperationRightsTemplate> findByGoupLabelAndReportDateAndLpos(List<String> templateValue, List<String> groupValue, String subTemplateValue, LocalDate requestDate, Pageable pageable);

    //Combined operation Rights SubTemplateValue as Nkey
    @Query(value = "select new  com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id,t.operRgts,t.template,t.key,t.avp1,t.avp2,t.avp3,t.avp4,t.fifo1,t.fifo2,t.fifo3,t.fifo4,t.realTime,t.eventType,t.evtAccess,t.evtInsert,t.evtModify,t.evtDelete,g.groupLabel) " +
            "from MxOperationRights t inner join MxGroupsListItem g on t.template=g.nkeyTmpl where " +
            "t.reportDate=:requestDate and t.template in :templateValue and t.operRgts=:subTemplateValue and g.reportDate=:requestDate and g.groupLabel in :groupValue and g.nkeyTmpl in :templateValue")
    Page<CombinedOperationRightsTemplate> findByGoupLabelAndReportDateAndNkey(List<String> templateValue,List<String> groupValue, String subTemplateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select new  com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id,t.operRgts,t.template,t.key,t.avp1,t.avp2,t.avp3,t.avp4,t.fifo1,t.fifo2,t.fifo3,t.fifo4,t.realTime,t.eventType,t.evtAccess,t.evtInsert,t.evtModify,t.evtDelete,g.groupLabel) " +
            "from MxOperationRights t inner join MxGroupsListItem g" +
            " on t.template=g.lposRight where t.reportDate=:repDate and t.operRgts=:subTempValue and g.reportDate=:repDate")
    Page<CombinedOperationRightsTemplate> findAllByReportDateLpos(LocalDate repDate , String subTempValue,Pageable pageable);

    @Query(value = "select new  com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id,t.operRgts,t.template,t.key,t.avp1,t.avp2,t.avp3,t.avp4,t.fifo1,t.fifo2,t.fifo3,t.fifo4,t.realTime,t.eventType,t.evtAccess,t.evtInsert,t.evtModify,t.evtDelete,g.groupLabel) from MxOperationRights t inner join MxGroupsListItem g" +
            " on t.template=g.nkeyTmpl where t.reportDate=:repDate and t.operRgts=:subTempValue and g.reportDate=:repDate")
    Page<CombinedOperationRightsTemplate> findAllByReportDateNkey(LocalDate repDate , String subTempValue,Pageable pageable);

    @Query(value = "select distinct t from MxOperationRights t where t.reportDate=:requestDate and upper(t.template)=:templateValue and upper(t.operRgts)=:subTemplateValue")
    Page<MxOperationRights> findByTemplateAndReportDateList(String templateValue,String subTemplateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select  t from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key )) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate and " +
            "(coalesce(:#{#filter.keyList}) is null or t.key in :#{#filter.keyList})   order by t.template,t.operRgts,t.key ")
    Page<MxOperationRights> findAllGroupCompareOperRightsMatched(LocalDate repDate, String template, String compareTemplate, String subTemplate, OperationalRights filter, Pageable pageable);

    @Query(value = "select  t from MxOperationRights t where not exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and t.key=m.key ) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate and " +
            "(coalesce(:#{#filter.keyList}) is null or t.key in :#{#filter.keyList})   ")
    Page<MxOperationRights> findAllGroupCompareOperRightsAdditional(LocalDate repDate, String template, String compareTemplate, String subTemplate,OperationalRights filter, Pageable pageable);

    @Query(value="select  t from MxOperationRights t  where t.template = :compareTemplate and t.operRgts=:subTemplate and (t.key in (:key) or t.key is null ) and t.reportDate = :repDate ")
    List<MxOperationRights> findMatchedOperationByTree(LocalDate repDate, String compareTemplate,String subTemplate, List<String> key);

    @Query(value = "select  new  com.finsurge.tmr_portal.mx_superview.models.OperationalRights(t.key) from MxOperationRights t where  exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and t.key=m.key ) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate ")
    List<OperationalRights> findGeneralPropertyListForOperationAll(String template, String compareTemplate, String subTemplate, LocalDate repDate);

    @Query(value = "select  new  com.finsurge.tmr_portal.mx_superview.models.OperationalRights(t.key)  from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key ) and  (( coalesce(m.avp1,'null') <> coalesce(t.avp1,'null')) or "+
            "(coalesce(m.avp2,'null') <> coalesce(t.avp2,'null')) or (coalesce(m.avp3,'null') <> coalesce(t.avp3,'null')) or  (coalesce(m.avp4,'null') <> coalesce(t.avp4,'null')) or " +
            " (coalesce(m.fifo2,'null') <> coalesce(t.fifo2,'null')) or (coalesce(m.fifo2,'null') <> coalesce(t.fifo2,'null')) or (coalesce(m.fifo3,'null') <> coalesce(t.fifo3,'null')) or "+
            " (coalesce(m.fifo4,'null') <> coalesce(t.fifo4,'null')) or (coalesce(m.realTime,'null') <> coalesce(t.realTime,'null')) or (coalesce(m.eventType,'null') <> coalesce(t.eventType,'null')) or "+
            " (coalesce(m.evtAccess,'null') <> coalesce(t.evtAccess,'null')) or (coalesce(m.evtInsert,'null') <> coalesce(t.evtInsert,'null')) or (coalesce(m.evtModify,'null') <> coalesce(t.evtModify,'null')) or "+
            "(coalesce(m.evtDelete,'null') <> coalesce(t.evtDelete,'null')))) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate ")
    List<OperationalRights> findGeneralPropertyListForOperationUnMatched(String template, String compareTemplate, String subTemplate, LocalDate repDate);

    @Query(value = "select  new  com.finsurge.tmr_portal.mx_superview.models.OperationalRights(t.key)  from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key) and (m.avp1 = t.avp1 or (m.avp1 is null and t.avp1 is null)) and  " +
            "(m.avp2 = t.avp2 or (m.avp2 is null and t.avp2 is null)) and (m.avp3 = t.avp3 or (m.avp3 is null and t.avp3 is null)) and (m.avp4 = t.avp4 or (m.avp4 is null and t.avp4 is null)) and  " +
            "(m.fifo1 = t.fifo1 or (m.fifo1 is null and t.fifo1 is null)) and (m.fifo2 = t.fifo2 or (m.fifo2 is null and t.fifo2 is null)) and (m.fifo3 = t.fifo3 or (m.fifo3 is null and t.fifo3 is null)) and  " +
            "(m.fifo4 = t.fifo4 or (m.fifo4 is null and t.fifo4 is null)) and   (m.realTime = t.realTime or (m.realTime is null and t.realTime is null)) and  " +
            "(m.eventType = t.eventType or (m.eventType is null and t.eventType is null)) and (m.evtAccess = t.evtAccess or (m.evtAccess is null and t.evtAccess is null)) " +
            "and (m.evtInsert = t.evtInsert or (m.evtInsert is null and t.evtInsert is null)) and (m.evtModify = t.evtModify or (m.evtModify is null and t.evtModify is null)) and  " +
            "(m.evtDelete = t.evtDelete or (m.evtDelete is null and t.evtDelete is null)) ) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate  ")
    List<OperationalRights> findGeneralPropertyListForOperationMatched(String template, String compareTemplate, String subTemplate, LocalDate repDate);
    @Query(value = "select  new  com.finsurge.tmr_portal.mx_superview.models.OperationalRights(t.key) from MxOperationRights t where not exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and t.key=m.key ) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate ")
    List<OperationalRights> findGeneralPropertyListForOperationAdditional(String template, String compareTemplate, String subTemplate, LocalDate repDate);

    @Query(value = "select  t from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key ) and  (( coalesce(m.avp1,'null') <> coalesce(t.avp1,'null')) or "+
           "(coalesce(m.avp2,'null') <> coalesce(t.avp2,'null')) or (coalesce(m.avp3,'null') <> coalesce(t.avp3,'null')) or  (coalesce(m.avp4,'null') <> coalesce(t.avp4,'null')) or " +
           " (coalesce(m.fifo2,'null') <> coalesce(t.fifo2,'null')) or (coalesce(m.fifo2,'null') <> coalesce(t.fifo2,'null')) or (coalesce(m.fifo3,'null') <> coalesce(t.fifo3,'null')) or "+
           " (coalesce(m.fifo4,'null') <> coalesce(t.fifo4,'null')) or (coalesce(m.realTime,'null') <> coalesce(t.realTime,'null')) or (coalesce(m.eventType,'null') <> coalesce(t.eventType,'null')) or "+
           " (coalesce(m.evtAccess,'null') <> coalesce(t.evtAccess,'null')) or (coalesce(m.evtInsert,'null') <> coalesce(t.evtInsert,'null')) or (coalesce(m.evtModify,'null') <> coalesce(t.evtModify,'null')) or "+
            "(coalesce(m.evtDelete,'null') <> coalesce(t.evtDelete,'null')))) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate  " +
           "and  (coalesce(:#{#filter.keyList}) is null or t.key in :#{#filter.keyList}) order by t.template,t.operRgts,t.key ")
    Page<MxOperationRights> findAllGroupCompareOperUnMatched(LocalDate repDate, String template, String compareTemplate, String subTemplate,OperationalRights filter, Pageable pageable);

    @Query(value = "select  t from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key) and (m.avp1 = t.avp1 or (m.avp1 is null and t.avp1 is null)) and  " +
            "(m.avp2 = t.avp2 or (m.avp2 is null and t.avp2 is null)) and (m.avp3 = t.avp3 or (m.avp3 is null and t.avp3 is null)) and (m.avp4 = t.avp4 or (m.avp4 is null and t.avp4 is null)) and  " +
            "(m.fifo1 = t.fifo1 or (m.fifo1 is null and t.fifo1 is null)) and (m.fifo2 = t.fifo2 or (m.fifo2 is null and t.fifo2 is null)) and (m.fifo3 = t.fifo3 or (m.fifo3 is null and t.fifo3 is null)) and  " +
            "(m.fifo4 = t.fifo4 or (m.fifo4 is null and t.fifo4 is null)) and   (m.realTime = t.realTime or (m.realTime is null and t.realTime is null)) and  " +
            "(m.eventType = t.eventType or (m.eventType is null and t.eventType is null)) and (m.evtAccess = t.evtAccess or (m.evtAccess is null and t.evtAccess is null)) " +
            "and (m.evtInsert = t.evtInsert or (m.evtInsert is null and t.evtInsert is null)) and (m.evtModify = t.evtModify or (m.evtModify is null and t.evtModify is null)) and  " +
            "(m.evtDelete = t.evtDelete or (m.evtDelete is null and t.evtDelete is null)) ) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate  and" +
            "(coalesce(:#{#filter.keyList}) is null or t.key in :#{#filter.keyList}) order by t.template,t.operRgts,t.key ")
    Page<MxOperationRights> findAllGroupCompareOperMatchedList(LocalDate repDate, String template, String compareTemplate, String subTemplate,OperationalRights filter, Pageable pageable);

    @Query(value = "select  t from MxOperationRights t where not exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key )) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate order by t.template,t.operRgts,t.key")
    Page<MxOperationRights> findGroupCompareOperRightsAdditionalValue(LocalDate repDate, String template, String compareTemplate, String subTemplate, Pageable pageable);

    @Query(value = "select  t from MxOperationRights t where exists (select m.id from MxOperationRights m where m.template = :compareTemplate and m.reportDate = :repDate " +
            " and m.operRgts=:subTemplate and (t.key=m.key )) and  t.template = :template and t.operRgts=:subTemplate and t.reportDate = :repDate order by t.template,t.operRgts,t.key")
    Page<MxOperationRights> findGroupCompareOperRightsMatchedValue(LocalDate repDate, String template, String compareTemplate, String subTemplate, Pageable pageable);

    @Query(value = "select distinct t from MxOperationRights t where t.reportDate=:requestDate and upper(t.template)=:templateValue and upper(t.operRgts)=:subTemplateValue and (upper(t.realTime) like concat('%',upper(:searchString),'%') or upper(t.key) like concat('%',upper(:searchString),'%')" +
            " or upper(t.fifo4) like concat('%',upper(:searchString),'%') or upper(t.fifo3) like concat('%',upper(:searchString),'%') or upper(t.fifo2) like concat('%',upper(:searchString),'%') or upper(t.fifo1) like concat('%',upper(:searchString),'%') or " +
            "upper(t.evtModify) like concat('%',upper(:searchString),'%') or upper(t.evtInsert) like concat('%',upper(:searchString),'%') or upper(t.evtDelete) like concat('%',upper(:searchString),'%') or upper(t.evtAccess) like concat('%',upper(:searchString),'%') or upper(t.eventType) like concat('%',upper(:searchString),'%') or" +
            " upper(t.avp4) like concat('%',upper(:searchString),'%') or upper(t.avp3) like concat('%',upper(:searchString),'%') or upper(t.avp2) like concat('%',upper(:searchString),'%') or upper(t.avp1) like concat('%',upper(:searchString),'%'))")
    Page<MxOperationRights> findByTemplateAndReportDateListWithGlobalSearch(String templateValue,String subTemplateValue, LocalDate requestDate,String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id, t.operRgts, t.template, t.key, t.avp1, t.avp2, t.avp3, t.avp4, t.fifo1, t.fifo2, t.fifo3, " +
            "t.fifo4, t.realTime, t.eventType, t.evtAccess, t.evtInsert, t.evtModify, t.evtDelete, g.groupLabel) from MxOperationRights t inner join MxGroupsListItem g on t.template = g.lposRight " +
            "where t.reportDate = :requestDate and t.operRgts = :subTemplateValue and g.reportDate=:requestDate and (upper(t.template) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.avp1) like (concat('%', upper(:searchString), '%')) or upper(t.avp2) like (concat('%', upper(:searchString), '%')) or upper(t.avp3) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.avp4) like (concat('%', upper(:searchString), '%')) or upper(t.fifo1) like (concat('%', upper(:searchString), '%')) or upper(t.fifo2) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.fifo3) like (concat('%', upper(:searchString), '%')) or upper(t.realTime) like (concat('%', upper(:searchString), '%')) or upper(t.eventType) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.evtAccess) like (concat('%', upper(:searchString), '%')) or upper(t.evtInsert) like (concat('%', upper(:searchString), '%')) or upper(t.evtModify) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.evtDelete) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedOperationRightsTemplate> findOperationRightsTmplTypeLposGlobalCombinedAllGroups(LocalDate requestDate, String subTemplateValue, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id, t.operRgts, t.template, t.key, t.avp1, t.avp2, t.avp3, t.avp4, t.fifo1, t.fifo2, t.fifo3, " +
            "t.fifo4, t.realTime, t.eventType, t.evtAccess, t.evtInsert, t.evtModify, t.evtDelete, g.groupLabel) from MxOperationRights t inner join MxGroupsListItem g on t.template = g.lposRight " +
            "where t.reportDate = :requestDate and g.reportDate = :requestDate and t.template in :templateValue and t.operRgts = :subTemplateValue and g.groupLabel in :groupValue and g.lposRight in :templateValue and " +
            "(upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.avp1) like (concat('%', upper(:searchString), '%')) or upper(t.avp2) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.avp3) like (concat('%', upper(:searchString), '%')) or upper(t.avp4) like (concat('%', upper(:searchString), '%')) or upper(t.fifo1) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.fifo2) like (concat('%', upper(:searchString), '%')) or upper(t.fifo3) like (concat('%', upper(:searchString), '%')) or upper(t.realTime) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.eventType) like (concat('%', upper(:searchString), '%')) or upper(t.evtAccess) like (concat('%', upper(:searchString), '%')) or upper(t.evtInsert) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.evtModify) like (concat('%', upper(:searchString), '%')) or upper(t.evtDelete) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedOperationRightsTemplate> findOperationRightsTmplTypeLposGlobalCombined(List<String> templateValue, List<String> groupValue, String subTemplateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id, t.operRgts, t.template, t.key, t.avp1, t.avp2, t.avp3, t.avp4, t.fifo1, t.fifo2, t.fifo3, " +
            "t.fifo4, t.realTime, t.eventType, t.evtAccess, t.evtInsert, t.evtModify, t.evtDelete, g.groupLabel) from MxOperationRights t inner join MxGroupsListItem g on t.template = g.nkeyTmpl " +
            "where t.reportDate = :requestDate and t.operRgts = :subTemplateValue and g.reportDate = :requestDate and (upper(t.template) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.key) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedOperationRightsTemplate> findOperationRightsTmplTypeNkeyGlobalCombinedAllGroups(LocalDate requestDate, String subTemplateValue, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedOperationRightsTemplate(t.id, t.operRgts, t.template, t.key, t.avp1, t.avp2, t.avp3, t.avp4, t.fifo1, t.fifo2, t.fifo3, " +
            "t.fifo4, t.realTime, t.eventType, t.evtAccess, t.evtInsert, t.evtModify, t.evtDelete, g.groupLabel) from MxOperationRights t inner join MxGroupsListItem g on t.template = g.nkeyTmpl " +
            "where t.reportDate = :requestDate and g.reportDate=:requestDate and t.template in :templateValue and t.operRgts = :subTemplateValue and g.groupLabel in :groupValue and g.nkeyTmpl in :templateValue and " +
            "(upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.key) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedOperationRightsTemplate> findOperationRightsTmplTypeNkeyGlobalCombined(List<String> templateValue, List<String> groupValue, String subTemplateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select distinct t from MxOperationRights t where t.reportDate=:requestDate and upper(t.template)=:templateValue and upper(t.operRgts)=:subTemplateValue and (upper(t.realTime) like concat('%',upper(:searchString),'%') or upper(t.template) like concat('%',upper(:searchString),'%')" +
            " or upper(t.fifo3) like concat('%',upper(:searchString),'%') or upper(t.fifo2) like concat('%',upper(:searchString),'%') or upper(t.fifo1) like concat('%',upper(:searchString),'%') or " +
            "upper(t.evtModify) like concat('%',upper(:searchString),'%') or upper(t.evtInsert) like concat('%',upper(:searchString),'%') or upper(t.evtDelete) like concat('%',upper(:searchString),'%') or upper(t.evtAccess) like concat('%',upper(:searchString),'%') or upper(t.eventType) like concat('%',upper(:searchString),'%') or" +
            " upper(t.avp4) like concat('%',upper(:searchString),'%') or upper(t.avp3) like concat('%',upper(:searchString),'%') or upper(t.avp2) like concat('%',upper(:searchString),'%') or upper(t.avp1) like concat('%',upper(:searchString),'%'))")
    Page<MxOperationRights> findByTemplateAndReportDateAndLposListWithGlobalSearch(String templateValue,String subTemplateValue, LocalDate requestDate,String searchString, Pageable pageable);

    @Query(value = "select distinct t from MxOperationRights t where t.reportDate=:requestDate and upper(t.template)=:templateValue and upper(t.operRgts)=:subTemplateValue and (upper(t.key) like concat('%',upper(:searchString),'%') or upper(t.template) like concat('%',upper(:searchString),'%'))")
    Page<MxOperationRights> findByTemplateAndReportDateAndNkeyListWithGlobalSearch(String templateValue,String subTemplateValue, LocalDate requestDate,String searchString, Pageable pageable);
}
