package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxChineseWallTmpl;
import com.finsurge.tmr_portal.mx_superview.models.ChineseWall;
import com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface MxChineseWallTemplateRepository extends CrudRepository<MxChineseWallTmpl, Long> {

    @Query(value = "select distinct t.templateLabel from MxChineseWallTmpl t where t.sysDate = :date order by t.templateLabel")
    List<String> findAllDistinctTemplateNamesForSysDate(LocalDate date);

    Page<MxChineseWallTmpl> findAllByTemplateLabelAndSysDateOrderByCounterpartLabel(String templateLabel, LocalDate date, Pageable pageable);

    MxChineseWallTmpl findTopByReportDateAndTemplateLabel(LocalDate requestDate,String templateValue);

    MxChineseWallTmpl  findTopByReportDate(LocalDate requestDate);
    
    @Query(value = "select t1 from MxChineseWallTmpl t1 where t1.templateLabel = :templateLabel and t1.reportDate = :baseReportDate " +
            "and t1.counterpartLabel in (select t2.counterpartLabel from MxChineseWallTmpl t2 where t2.templateLabel = :templateLabel and t2.reportDate = :compareReportDate ) " +
            "order by t1.counterpartLabel")
    Page<MxChineseWallTmpl> findByChineseWallByGroupLabelAndReportDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value = "select t1 from MxChineseWallTmpl t1 where " +
            "t1.templateLabel = :templateLabel and t1.reportDate = :baseReportDate and t1.counterpartLabel not in " +
            "(select t2.counterpartLabel from MxChineseWallTmpl t2 where t2.templateLabel = :templateLabel and t2.reportDate= :compareReportDate)")
    Page<MxChineseWallTmpl> getAdditionalDataByRepDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable) ;

    @Query(value = "select t1 from MxChineseWallTmpl t1 where " +
            "t1.templateLabel = :templateLabel and t1.reportDate = :compareReportDate and t1.counterpartLabel not in " +
            "(select t2.counterpartLabel from MxChineseWallTmpl t2 where t2.templateLabel = :templateLabel and t2.reportDate= :baseReportDate)")
    Page<MxChineseWallTmpl> getAdditionalDataByCompareDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable) ;

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate(t.id,t.templateLabel, t.counterpartLabel,t.counterpartDescription,g.groupLabel) from " +
            " MxChineseWallTmpl t inner join MxGroupsListItem g" +
            " on t.templateLabel=g.chineseWall where g.chineseWall in :templateValue and t.templateLabel in :templateValue " +
            "and t.reportDate=:requestDate and g.groupLabel in :groupValue" +
            " and g.reportDate=:requestDate ")
    Page<CombinedChineseWallTemplate> findByGroupLabelAndReportDate(List<String> templateValue, List<String> groupValue, LocalDate requestDate, Pageable pageable);

    List<MxChineseWallTmpl> findTopByReportDateAndTemplateLabelIn(LocalDate requestDate, List<String> templateValue);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate(t.id,t.templateLabel, t.counterpartLabel,t.counterpartDescription,g.groupLabel)" +
            " from MxChineseWallTmpl t inner join MxGroupsListItem g on t.templateLabel=g.chineseWall where g.reportDate=:requestDate and t.reportDate=:requestDate")
    Page<CombinedChineseWallTemplate> findAllByReportDate(LocalDate requestDate,Pageable pageable);

    @Query(value = "select distinct t from MxChineseWallTmpl t where t.reportDate=:requestDate and t.templateLabel = :templateValue and (upper(t.counterpartLabel) like concat('%',upper(:searchString),'%')" +
            " or (upper(t.counterpartDescription) like concat('%',upper(:searchString),'%') ))" )
    Page<MxChineseWallTmpl> findByTemplateAndReportDateWithGlobalSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select distinct t from MxChineseWallTmpl t where t.reportDate=:requestDate and t.templateLabel = :templateValue" )
    Page<MxChineseWallTmpl> findByTemplateAndReportDate(String templateValue, LocalDate requestDate,Pageable pageable);


    @Query(value = "select t from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate and p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel ) " +
            "and (coalesce(:#{#filter.counterPartLabelList}) is null or t.counterpartLabel in :#{#filter.counterPartLabelList}) order by t.counterpartLabel")
    Page<MxChineseWallTmpl> findAllGroupCompareChineseMatched(LocalDate repDate, String template, String compareTemplate, ChineseWall filter, Pageable pageable);

    @Query(value = "select t from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and not  exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate and p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel) " +
            "and (coalesce(:#{#filter.counterPartLabelList}) is null or t.counterpartLabel in :#{#filter.counterPartLabelList})")
    Page<MxChineseWallTmpl> findAllGroupCompareChineseAdditional(LocalDate repDate, String template, String compareTemplate, ChineseWall filter,Pageable pageable);

    @Query(value = "select t from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate and p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel ) " +
            "order by t.counterpartLabel")
    Page<MxChineseWallTmpl> findAllGroupCompareChineseMatchedExport(LocalDate repDate, String template, String compareTemplate, Pageable pageable);

    @Query(value = "select t from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and not  exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate and p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel) " +
            "order by t.counterpartLabel")
    Page<MxChineseWallTmpl> findAllGroupCompareChineseAdditionalExport(LocalDate repDate, String template, String compareTemplate, Pageable pageable);

    @Query(value = "select t from MxChineseWallTmpl t where t.templateLabel = :compareTemplate and t.reportDate = :repDate and  (t.counterpartLabel in :label or t.counterpartLabel is null) order by t.counterpartLabel")
    List<MxChineseWallTmpl> findMatchedChineaseWallByTree(LocalDate repDate, String compareTemplate, List<String> label);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.ChineseWall(t.counterpartLabel) from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and (:search is null or upper(t.counterpartLabel) like  upper(concat('%',:search,'%'))) and exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate and" +
            " p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel ) " +
            "order by t.counterpartLabel")
    List<ChineseWall> findGeneralPropertyListForChineseAll(String template, String compareTemplate, LocalDate repDate,String search,Pageable pageable);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.ChineseWall(t.counterpartLabel) from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and (:search is null or upper(t.counterpartLabel) like  upper(concat('%',:search,'%'))) and exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate" +
            " and p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel and  t.counterpartDescription = p.counterpartDescription) order by t.counterpartLabel")
    List<ChineseWall> findGeneralPropertyListForChineseMatched(String template, String compareTemplate, LocalDate repDate,String search,Pageable pageable);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.ChineseWall(t.counterpartLabel) from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and (:search is null or upper(t.counterpartLabel) like  upper(concat('%',:search,'%'))) and exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate " +
            "and p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel and  t.counterpartDescription <> p.counterpartDescription ) order by t.counterpartLabel")
    List<ChineseWall> findGeneralPropertyListForChineseUnMatched(String template, String compareTemplate, LocalDate repDate,String search,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.ChineseWall(t.counterpartLabel) from MxChineseWallTmpl t where t.templateLabel = :template and t.reportDate = :repDate " +
            "and (:search is null or upper(t.counterpartLabel) like  upper(concat('%',:search,'%'))) and not exists (select p.id  from MxChineseWallTmpl p where p.templateLabel = :compareTemplate a" +
            "nd p.reportDate = :repDate and t.counterpartLabel=p.counterpartLabel ) order by t.counterpartLabel")
    List<ChineseWall> findGeneralPropertyListForChineseAdditional(String template, String compareTemplate, LocalDate repDate,String search,Pageable pageable);


    @Query(value = "select t from MxChineseWallTmpl t  " +
            " inner join MxChineseWallTmpl p on t.counterpartLabel=p.counterpartLabel and p.reportDate = t.reportDate  " +
            "where t.templateLabel = :template and t.reportDate = :repDate and  p.templateLabel = :compareTemplate " +
            "and p.counterpartDescription = p.counterpartDescription")
    Page<MxChineseWallTmpl> findGroupCompareMatchedRows(LocalDate repDate, String template, String compareTemplate, Pageable pageable);

    @Query(value = "select t from MxChineseWallTmpl t  " +
            " where exists (select p from  MxChineseWallTmpl p where  t.counterpartLabel=p.counterpartLabel and p.reportDate = :repDate and  p.templateLabel = :compareTemplate and " +
            " coalesce( p.counterpartDescription,'null') <> coalesce(t.counterpartDescription,'null') ) and t.templateLabel = :template and t.reportDate = :repDate " +
            "and (coalesce(:#{#filter.counterPartLabelList}) is null or t.counterpartLabel in :#{#filter.counterPartLabelList}) order by t.counterpartLabel")
    Page<MxChineseWallTmpl> findGroupCompareUnMatchedRows(LocalDate repDate, String template, String compareTemplate,ChineseWall filter, Pageable pageable);

    @Query(value = "select t from MxChineseWallTmpl t  " +
            " where exists (select p from  MxChineseWallTmpl p where  t.counterpartLabel=p.counterpartLabel and p.reportDate = :repDate and  p.templateLabel = :compareTemplate and " +
            "(p.counterpartDescription = t.counterpartDescription or (t.counterpartDescription  is null and p.counterpartDescription is null )))" +
            " and t.templateLabel = :template and t.reportDate = :repDate and (coalesce(:#{#filter.counterPartLabelList}) is null or t.counterpartLabel in :#{#filter.counterPartLabelList}) order by t.counterpartLabel")
    Page<MxChineseWallTmpl> findGroupCompareMatchedList(LocalDate repDate, String template, String compareTemplate, ChineseWall filter,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate(t.id,t.templateLabel, t.counterpartLabel,t.counterpartDescription,g.groupLabel) from " +
            " MxChineseWallTmpl t inner join MxGroupsListItem g" +
            " on t.templateLabel=g.chineseWall where g.chineseWall in :templateValue and t.templateLabel in :templateValue " +
            "and t.reportDate=:requestDate and g.groupLabel in :groupValue" +
            " and g.reportDate=:requestDate and (upper(t.counterpartLabel) like concat('%',upper(:searchString),'%')" +
            " or upper(t.counterpartDescription) like concat('%',upper(:searchString),'%') or upper(t.templateLabel) like concat('%',upper(:searchString),'%') or upper(g.groupLabel) like concat('%',upper(:searchString),'%'))")
    Page<CombinedChineseWallTemplate> findByGroupLabelAndReportDateWithGlobalSearch(List<String> templateValue, List<String> groupValue, LocalDate requestDate,String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedChineseWallTemplate(t.id,t.templateLabel, t.counterpartLabel,t.counterpartDescription,g.groupLabel)" +
            " from MxChineseWallTmpl t inner join MxGroupsListItem g on t.templateLabel=g.chineseWall where g.reportDate=:requestDate and t.reportDate=:requestDate and (upper(t.counterpartLabel) like concat('%',upper(:searchString),'%')" +
            " or upper(t.counterpartDescription) like concat('%',upper(:searchString),'%') or upper(t.templateLabel) like concat('%',upper(:searchString),'%') or upper(g.groupLabel) like concat('%',upper(:searchString),'%'))")
    Page<CombinedChineseWallTemplate> findAllByReportDateWithGlobalSearch(LocalDate requestDate,String searchString,Pageable pageable);
}
