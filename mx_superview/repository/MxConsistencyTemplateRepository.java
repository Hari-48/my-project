package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxConsistencyTmpl;
import com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate;
import com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompareFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Repository
@Transactional
public interface MxConsistencyTemplateRepository extends CrudRepository<MxConsistencyTmpl, Long> {

    @Query(value = "select distinct t.consistencyTmpl from MxConsistencyTmpl t where t.sysDate = :date order by t.consistencyTmpl")
    List<String> findAllDistinctTemplateNamesForSysDate(LocalDate date);

    Page<MxConsistencyTmpl> findAllByConsistencyTmplAndSysDateOrderByCategoryAscItemAsc(String templateLabel, LocalDate date, Pageable pageable);

    MxConsistencyTmpl findTopByConsistencyTmplAndReportDate(String consistencyTmpl, LocalDate requestDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate(t.id,t.consistencyTmpl,t.category, t.item,t.accessRight,t.insertRight,t.modifyRight,t.deleteRight,t.mandatoryRight,t.accountingRight,t.paymentRight,g.groupLabel) from" +
            " MxConsistencyTmpl t inner join MxGroupsListItem g" +
            "  on t.reportDate=g.reportDate where t.reportDate=:requestDate and t.consistencyTmpl=g.consitencyTmpl")
    Page<CombinedConsistencyTemplate> findAllByReportDate(LocalDate requestDate, Pageable pageable);
    
//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate(t.id,t.consistencyTmpl,t.category, t.item,t.accessRight,t.insertRight,t.modifyRight,t.deleteRight,t.mandatoryRight,t.accountingRight,t.paymentRight,g.groupLabel) from" +
//        " MxConsistencyTmpl t inner join MxGroupsListItem g" +
//        "  on t.consistencyTmpl=g.consitencyTmpl where t.reportDate=:requestDate and g.reportDate=:requestDate")
//    Page<CombinedConsistencyTemplate> findAllByReportDate(LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate(t.id,t.consistencyTmpl,t.category, t.item,t.accessRight,t.insertRight,t.modifyRight,t.deleteRight,t.mandatoryRight,t.accountingRight,t.paymentRight,g.groupLabel) from " +
            " MxConsistencyTmpl t inner join MxGroupsListItem g" +
            " on t.consistencyTmpl=g.consitencyTmpl where g.consitencyTmpl in :templateValue and t.consistencyTmpl in :templateValue and t.reportDate=:requestDate and g.groupLabel in :groupValue" +
            " and g.reportDate=:requestDate")
    Page<CombinedConsistencyTemplate> findByGroupLabelAndReportDate(List<String> templateValue, List<String> groupValue, LocalDate requestDate, Pageable pageable);


    List<MxConsistencyTmpl> findTopByConsistencyTmplInAndReportDate(List<String> consistencyTmpl, LocalDate requestDate);

    @Query(value = "select distinct t1 from MxConsistencyTmpl t1 where exists (select t2.id from MxConsistencyTmpl t2 where t2.consistencyTmpl = :template and t2.reportDate = :compareReportDate " +
            " and t1.category =t2.category and t1.item=t2.item) and  t1.consistencyTmpl = :template and t1.reportDate = :baseReportDate  order by t1.category,t1.item ")
    Page<MxConsistencyTmpl> findByMxConsistencyByGroupLabelAndReportDate(String template, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value = "select distinct t1 from MxConsistencyTmpl t1 where  not exists (select t2.id from MxConsistencyTmpl t2 where t2.consistencyTmpl = :template and t2.reportDate = :compareReportDate " +
            " and t1.category =t2.category and t1.item=t2.item) and  t1.consistencyTmpl = :template and t1.reportDate = :baseReportDate  order by t1.category,t1.item ")
    Page<MxConsistencyTmpl> findByAdditionalConsistencyByGroupLabelAndReportDate(String template, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query("select distinct t from MxConsistencyTmpl t where t.reportDate = :requestDate and upper(t.consistencyTmpl) in :templateValue ")
    Page<MxConsistencyTmpl> findByTemplateAndReportDateList(String templateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.category is null and p.category is null)) ) and  t.consistencyTmpl = :template and t.reportDate = :repDate " +
            "and (:category is null or t.category=:category ) and (:item is null or t.item=:item)  order by t.category,t.item ")
    Page<ConsistencyTemplateRights> findGroupCompareConsistencyTreeMap(LocalDate repDate, String template, String compareTemplate,String category,String item, Pageable pageable);

    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where not exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.category is null and p.category is null))  ) and  t.consistencyTmpl = :template and t.reportDate = :repDate " +
            "and (:category is null or t.category=:category ) and (:item is null or t.item=:item)  order by t.category,t.item ")
    Page<ConsistencyTemplateRights> findGroupCompareConsistencyTreeMapAdditional(LocalDate repDate, String template, String compareTemplate,String category,String item, Pageable pageable);

    @Query(value = "select  t from MxConsistencyTmpl t where exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) ) and  t.consistencyTmpl = :template and t.reportDate = :repDate " +
            " and (coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            "(coalesce(:#{#filter.itemList}) is null or t.item in :#{#filter.itemList})  order by t.category,t.item ")
    Page<MxConsistencyTmpl> findAllGroupCompareConsistencyMatched(LocalDate repDate, String template, String compareTemplate,ConsistencyTemplateRights filter, Pageable pageable);

    @Query(value = "select  t from MxConsistencyTmpl t where not exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) ) and  t.consistencyTmpl = :template and t.reportDate = :repDate " +
            " and (coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            "(coalesce(:#{#filter.itemList}) is null or t.item in :#{#filter.itemList})")
    Page<MxConsistencyTmpl> findAllGroupCompareConsistencyAdditional(LocalDate repDate, String template, String compareTemplate,ConsistencyTemplateRights filter, Pageable pageable);

    @Query(value=" select t from MxConsistencyTmpl t where t.reportDate =:repDate and  ( t.category in (:categoryList)  or t.category is null ) " +
            " and (t.item in (:itemList) or t.item is null)  and  t.consistencyTmpl=:compareTemplate order by t.category,t.item ")
    List<MxConsistencyTmpl> findMatchedDataByConsistencyTree(LocalDate repDate, String compareTemplate, List<String> categoryList, List<String> itemList);
    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where  " +
            "exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :#{#filter.compareTemplate} and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.category is null and p.category is null)) ) " +
            "and  t.consistencyTmpl = :#{#filter.template}  and t.reportDate = :repDate  ")
    List<ConsistencyTemplateRights> findGeneralPropertyListForConsistencyMatched(GroupCompareFilter filter, LocalDate repDate);

    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where  " +
            "not exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.category is null and p.category is null)) ) " +
            "and  t.consistencyTmpl = :template  and t.reportDate = :repDate ")
    List<ConsistencyTemplateRights> findGeneralPropertyListForConsistencyAdditional(String template, String compareTemplate, LocalDate repDate);
    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where exists" +
            " (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :#{#filter.compareTemplate}  and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) " +
            "and ( (coalesce(t.accessRight,'null') <>coalesce(p.accessRight,'null'))  or  (coalesce(t.insertRight,'null') <>coalesce(p.insertRight,'null'))  or " +
            " (coalesce(t.modifyRight,'null') <>coalesce(p.modifyRight,'null'))  or  (coalesce(t.deleteRight,'null') <>coalesce(p.deleteRight,'null')) " +
            "or  (coalesce(t.mandatoryRight,'null') <>coalesce(p.mandatoryRight,'null')) or  (coalesce(t.accountingRight,'null') <>coalesce(p.accountingRight,'null')) " +
            "or  (coalesce(t.paymentRight,'null') <>coalesce(p.paymentRight,'null')))) and " +
            " t.consistencyTmpl = :#{#filter.template} and t.reportDate = :repDate")
    List<ConsistencyTemplateRights> findGeneralPropertyListForConsistencyUnMatched(LocalDate repDate,GroupCompareFilter filter);
    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where exists" +
            " (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :#{#filter.compareTemplate}  and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) " +
            "and ( (coalesce(t.accessRight,'null') <>coalesce(p.accessRight,'null'))  or  (coalesce(t.insertRight,'null') <>coalesce(p.insertRight,'null'))  or " +
            " (coalesce(t.modifyRight,'null') <>coalesce(p.modifyRight,'null'))  or  (coalesce(t.deleteRight,'null') <>coalesce(p.deleteRight,'null')) " +
            "or  (coalesce(t.mandatoryRight,'null') <>coalesce(p.mandatoryRight,'null')) or  (coalesce(t.accountingRight,'null') <>coalesce(p.accountingRight,'null')) " +
            "or  (coalesce(t.paymentRight,'null') <>coalesce(p.paymentRight,'null')))) and " +
            " t.consistencyTmpl = :#{#filter.template} and t.reportDate = :repDate and  (:#{#filter.search} is null or upper(t.category) like concat('%',concat(upper(:#{#filter.search}),'%'))) and " +
            "(:#{#filter.search1} is null or upper(t.item) like concat('%',concat(upper(:#{#filter.search1}),'%'))) order by t.category,t.item  ")
    Page<ConsistencyTemplateRights> findGeneralPropertyListForConsistencyUnMatched(LocalDate repDate,GroupCompareFilter filter,Pageable pageable);
    @Query(value = "select  new com.finsurge.tmr_portal.mx_superview.models.ConsistencyTemplateRights(t.category,t.item) from MxConsistencyTmpl t where exists " +
            "(select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :#{#filter.compareTemplate} and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) and" +
            " (t.accessRight=p.accessRight or (t.accessRight is null and p.accessRight is null)) and (t.insertRight=p.insertRight or (t.insertRight is null and p.insertRight is null)) and " +
            " (t.modifyRight=p.modifyRight or (t.modifyRight is null and p.modifyRight is null)) and (t.deleteRight=p.deleteRight or (t.deleteRight is null and p.deleteRight is null)) and " +
            " (t.mandatoryRight=p.mandatoryRight or (t.mandatoryRight is null and p.mandatoryRight is null)) and (t.accountingRight=p.accountingRight or (t.accountingRight is null and p.accountingRight is null)) and " +
            " (t.paymentRight=p.paymentRight or (t.paymentRight is null and p.paymentRight is null))) and " +
            " t.consistencyTmpl = :#{#filter.template} and t.reportDate = :repDate ")
    List<ConsistencyTemplateRights> findGeneralPropertyListForConsistencyMatchedList(LocalDate repDate,GroupCompareFilter filter);

    @Query(value = "select  t from MxConsistencyTmpl t where exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) " +
            "and ( (coalesce(t.accessRight,'null') <>coalesce(p.accessRight,'null'))  or  (coalesce(t.insertRight,'null') <>coalesce(p.insertRight,'null'))  or " +
            " (coalesce(t.modifyRight,'null') <>coalesce(p.modifyRight,'null'))  or  (coalesce(t.deleteRight,'null') <>coalesce(p.deleteRight,'null')) " +
            "or  (coalesce(t.mandatoryRight,'null') <>coalesce(p.mandatoryRight,'null')) or  (coalesce(t.accountingRight,'null') <>coalesce(p.accountingRight,'null')) " +
            "or  (coalesce(t.paymentRight,'null') <>coalesce(p.paymentRight,'null')))) and " +
            " t.consistencyTmpl = :template and t.reportDate = :repDate and  (coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            " (coalesce(:#{#filter.itemList}) is null or t.item in :#{#filter.itemList}) order by t.category,t.item ")
    Page<MxConsistencyTmpl> findAllGroupCompareConsistencyUnMatched(LocalDate repDate, String template, String compareTemplate,ConsistencyTemplateRights filter,Pageable pageable);

    @Query(value = "select  t from MxConsistencyTmpl t where exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.item is null and p.item is null)) and" +
            " (t.accessRight=p.accessRight or (t.accessRight is null and p.accessRight is null)) and (t.insertRight=p.insertRight or (t.insertRight is null and p.insertRight is null)) and " +
            " (t.modifyRight=p.modifyRight or (t.modifyRight is null and p.modifyRight is null)) and (t.deleteRight=p.deleteRight or (t.deleteRight is null and p.deleteRight is null)) and " +
            " (t.mandatoryRight=p.mandatoryRight or (t.mandatoryRight is null and p.mandatoryRight is null)) and (t.accountingRight=p.accountingRight or (t.accountingRight is null and p.accountingRight is null)) and " +
            " (t.paymentRight=p.paymentRight or (t.paymentRight is null and p.paymentRight is null))) and " +
            " t.consistencyTmpl = :template and t.reportDate = :repDate and  (coalesce(:#{#filter.categoryList}) is null or t.category in :#{#filter.categoryList}) and " +
            " (coalesce(:#{#filter.itemList}) is null or t.item in :#{#filter.itemList}) order by t.category,t.item ")
    Page<MxConsistencyTmpl> findAllGrpCompareMatchedListforConsistency(LocalDate repDate, String template, String compareTemplate,ConsistencyTemplateRights filter,Pageable pageable);

    @Query(value = "select  t from MxConsistencyTmpl t where exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.category is null and p.category is null)) ) and  t.consistencyTmpl = :template and t.reportDate = :repDate " +
            "order by t.category,t.item ")
    Page<MxConsistencyTmpl> findAllGroupCompareConsistencyMatchedList(LocalDate repDate, String template, String compareTemplate,Pageable pageable);

    @Query(value = "select  t from MxConsistencyTmpl t where  not exists (select p.id from MxConsistencyTmpl p where p.consistencyTmpl = :compareTemplate and p.reportDate = :repDate " +
            " and (t.category =p.category or (t.category is null and p.category is null)) and (t.item=p.item or (t.category is null and p.category is null))  ) " +
            "and  t.consistencyTmpl = :template and t.reportDate = :repDate " +
            "order by t.category,t.item ")
    Page<MxConsistencyTmpl> findAllGroupCompareConsistencyAdditionalList(LocalDate repDate, String template, String compareTemplate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate(t.id, t.consistencyTmpl, t.category, t.item, t.accessRight, t.insertRight, t.modifyRight, t.deleteRight, t.mandatoryRight, t.accountingRight, t.paymentRight, g.groupLabel) " +
            "from MxConsistencyTmpl t inner join MxGroupsListItem g on t.reportDate = g.reportDate where t.reportDate = :requestDate and t.consistencyTmpl = g.consitencyTmpl and " +
            "(upper(t.consistencyTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.category) like (concat('%', upper(:searchString), '%')) or upper(t.item) like (concat('%', upper(:searchString), '%')) or upper(t.accessRight) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.insertRight) like (concat('%', upper(:searchString), '%')) or upper(t.modifyRight) like (concat('%', upper(:searchString), '%')) or upper(t.deleteRight) like (concat('%', upper(:searchString), '%')) or upper(t.mandatoryRight) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.accountingRight) like (concat('%', upper(:searchString), '%')) or upper(t.paymentRight) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedConsistencyTemplate> findConsistencyTmplGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedConsistencyTemplate(t.id, t.consistencyTmpl, t.category, t.item, t.accessRight, t.insertRight, t.modifyRight, t.deleteRight, t.mandatoryRight, t.accountingRight, t.paymentRight, g.groupLabel) " +
            "from MxConsistencyTmpl t inner join MxGroupsListItem g on t.consistencyTmpl = g.consitencyTmpl where t.reportDate = :requestDate and g.reportDate = :requestDate and g.consitencyTmpl in :templateValue and t.consistencyTmpl in :templateValue and g.groupLabel in :groupValue and " +
            "(upper(t.consistencyTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.category) like (concat('%', upper(:searchString), '%')) or upper(t.item) like (concat('%', upper(:searchString), '%')) or upper(t.accessRight) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.insertRight) like (concat('%', upper(:searchString), '%')) or upper(t.modifyRight) like (concat('%', upper(:searchString), '%')) or upper(t.deleteRight) like (concat('%', upper(:searchString), '%')) or upper(t.mandatoryRight) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.accountingRight) like (concat('%', upper(:searchString), '%')) or upper(t.paymentRight) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedConsistencyTemplate> findConsistencyTmplGlobalCombined(List<String> templateValue, List<String> groupValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query("select distinct t from MxConsistencyTmpl t where t.reportDate = :requestDate and upper(t.consistencyTmpl) = :templateValue and ((upper(t.category) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.item) like concat('%',upper(:searchString),'%') ) or (upper(t.accessRight) like concat('%',upper(:searchString),'%') ) or (upper(t.insertRight) like concat('%',upper(:searchString),'%') ) or (upper(t.modifyRight) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.deleteRight) like concat('%',upper(:searchString),'%') ) or (upper(t.mandatoryRight) like concat('%',upper(:searchString),'%') ) or (upper(t.item) like concat('%',upper(:searchString),'%') ) or (upper(t.accountingRight) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.paymentRight) like concat('%',upper(:searchString),'%') ))")
    Page<MxConsistencyTmpl> findByGroupLabelAndReportDateListWithSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);
}

