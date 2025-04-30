package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupCombinedPortfolio;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompPortfolio;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompareFilter;
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
public interface MxGroupCombinedPortfolioRepo extends CrudRepository<MxGroupCombinedPortfolio, Long> {

    MxGroupCombinedPortfolio findTopByUsrGroupAndReportDate(String usrGroup, LocalDate requestDate);

    @Query(value="select distinct t1 from MxGroupCombinedPortfolio t1 where exists (select t2.compfLbl from MxGroupCombinedPortfolio t2 where t2.usrGroup=:templateLabel"+
            " and t2.reportDate=:compareReportDate and t2.compfLbl=t1.compfLbl and (t2.unit =t1.unit or t2.unit is null and t1.unit is null )) and t1.usrGroup=:templateLabel and  t1.reportDate=:baseReportDate order by t1.compfLbl,t1.unit")
    Page<MxGroupCombinedPortfolio> findByCombinedPortfolioByGroupLabelAndReportDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value="select distinct t1 from MxGroupCombinedPortfolio t1 where  not exists (select t2.compfLbl from MxGroupCombinedPortfolio t2 where t2.usrGroup=:templateLabel"+
            " and t2.reportDate=:compareReportDate and t2.compfLbl=t1.compfLbl and (t2.unit =t1.unit or t2.unit is null and t1.unit is null )) and t1.usrGroup=:templateLabel and  t1.reportDate=:baseReportDate order by t1.compfLbl,t1.unit")
    Page<MxGroupCombinedPortfolio> findByAdditionalCombinedPortfolioByGroupLabelAndRepDate(String templateLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value = "select distinct t from MxGroupCombinedPortfolio t where t.reportDate=:requestDate and upper(t.usrGroup)=:templateValue")
    Page<MxGroupCombinedPortfolio> findByGroupLabelAndReportDateList(String templateValue, LocalDate requestDate, Pageable pageable);
    @Query(value = "select distinct t from MxGroupCombinedPortfolio t where t.reportDate=:requestDate and t.usrGroup=:templateValue and ( t.compfLbl like (concat('%',upper(:searchString),'%')) or "+
           "t.unitType like (concat('%',upper(:searchString),'%')) or  t.unit like (concat('%',upper(:searchString),'%')))")
    Page<MxGroupCombinedPortfolio> findByGroupLabelAndReportDateListwithSearch(String templateValue, LocalDate requestDate,String searchString, Pageable pageable);
    List<MxGroupCombinedPortfolio> findTopByUsrGroupInAndReportDate(List<String> templateValue, LocalDate requestDate);
    @Query(value = "select t from MxGroupCombinedPortfolio t where t.reportDate =:requestDate and t.usrGroup in :templateValue")

    Page<MxGroupCombinedPortfolio> findByGroupLabelAndReportDate(List<String> templateValue, LocalDate requestDate, Pageable pageable);

    Page<MxGroupCombinedPortfolio> findAllByReportDate(LocalDate requestDate,Pageable pageable);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.GroupCompPortfolio(t.compfLbl,t.unit) from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))  ) and t.usrGroup=:template and  t.reportDate=:repDate" +
            " and (:comfLabel is null or t.compfLbl=:comfLabel) and (:unit is null or t.unit=:unit)  order by t.compfLbl,t.unit")
    Page<GroupCompPortfolio> findGroupCompareCombPortfolioTreeMap(LocalDate repDate, String template, String compareTemplate,String comfLabel,String unit, Pageable pageable);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.GroupCompPortfolio(t.compfLbl,t.unit) from MxGroupCombinedPortfolio t where not exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))  ) and t.usrGroup=:template and  t.reportDate=:repDate" +
            " and (:comfLabel is null or t.compfLbl=:comfLabel) and (:unit is null or t.unit=:unit)  order by t.compfLbl,t.unit")
    Page<GroupCompPortfolio> findGroupCompareCombPortfolioTreeMapAdditional(LocalDate repDate, String template, String compareTemplate,String comfLabel,String unit, Pageable pageable);

    @Query(value="select t from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))) " +
            "and (coalesce(:#{#filter.compfLblList}) is null or t.compfLbl in :#{#filter.compfLblList}) and " +
            " (coalesce(:#{#filter.unitList}) is null or t.unit in :#{#filter.unitList}) and t.usrGroup=:template and  t.reportDate=:repDate order by t.compfLbl,t.unit")
    Page<MxGroupCombinedPortfolio> findAllGroupCompareCombPortfolioMatched(LocalDate repDate, String compareTemplate, String template,GroupCompPortfolio filter, Pageable pageable);

    @Query(value="select t from MxGroupCombinedPortfolio t where not exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))) " +
            "and (coalesce(:#{#filter.compfLblList}) is null or t.compfLbl in :#{#filter.compfLblList}) and " +
            " (coalesce(:#{#filter.unitList}) is null or t.unit in :#{#filter.unitList}) and t.usrGroup=:template and  t.reportDate=:repDate")
    Page<MxGroupCombinedPortfolio> findAllGroupCompareCombPortfolioAdditional(LocalDate repDate, String template, String compareTemplate,GroupCompPortfolio filter, Pageable pageable);

    @Query(value="select t from MxGroupCombinedPortfolio t where  (t.compfLbl in :compList or t.compfLbl is null) and (t.unit in :unitList or t.unit is null ) and  t.usrGroup=:compareTemplate and  t.reportDate=:repDate" +
            "   order by t.compfLbl,t.unit")
    List<MxGroupCombinedPortfolio> findMatchedDataByCompTree(LocalDate repDate, String compareTemplate, List<String> compList, List<String> unitList);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.GroupCompPortfolio(t.compfLbl,t.unit) from MxGroupCombinedPortfolio t where  not exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))  ) and t.usrGroup=:template and  t.reportDate=:repDate")
    List<GroupCompPortfolio> findGeneralPropertyListForCombPortAdditional(String template, String compareTemplate,LocalDate repDate);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.GroupCompPortfolio(t.compfLbl,t.unit) from MxGroupCombinedPortfolio t where exists " +
            "(select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:#{#filter.compareTemplate}"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))  )" +
            " and t.usrGroup=:#{#filter.template} and  t.reportDate=:repDate")
    List<GroupCompPortfolio> findGeneralPropertyListForCombPortAll(GroupCompareFilter filter, LocalDate repDate);
    @Query(value="select t from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:#{#filter.compareTemplate}"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null)) and (coalesce(t.unitType,'null') <> coalesce( p.unitType,'null') ) ) " +
            "and   t.usrGroup=:#{#filter.template} and  t.reportDate=:repDate ")
    List<GroupCompPortfolio> findGeneralPropertyListForCombPortMatched(GroupCompareFilter filter, LocalDate repDate);

    @Query(value="select t from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:#{#filter.compareTemplate}"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null)) and (t.unitType = p.unitType or (p.unitType is null and t.unitType is null))) " +
            "and  t.usrGroup=:#{#filter.template} and  t.reportDate=:repDate ")
    List<GroupCompPortfolio> findGeneralPropertyListForCombPorUnMatched(GroupCompareFilter filter, LocalDate repDate);

    @Query(value="select t from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))) and t.usrGroup=:template and  t.reportDate=:repDate order by t.compfLbl,t.unit")
    Page<MxGroupCombinedPortfolio> findAllGroupCompareCombPortfolioMatchedValues(LocalDate repDate, String template, String compareTemplate,Pageable pageable);

    @Query(value="select t from MxGroupCombinedPortfolio t where not exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null))) and t.usrGroup=:template and  t.reportDate=:repDate order by t.compfLbl,t.unit")
    Page<MxGroupCombinedPortfolio> findAllGroupCompareCombPortfolioAdditionalValues(LocalDate repDate, String template, String compareTemplate, Pageable pageable);

    @Query(value="select t from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null)) and (coalesce(t.unitType,'null') <> coalesce( p.unitType,'null') ) ) " +
            "and (coalesce(:#{#filter.compfLblList}) is null or t.compfLbl in :#{#filter.compfLblList}) and " +
            " (coalesce(:#{#filter.unitList}) is null or t.unit in :#{#filter.unitList}) and t.usrGroup=:template and  t.reportDate=:repDate order by t.compfLbl,t.unit")
    Page<MxGroupCombinedPortfolio> findAllGroupCompareCompPortfolioUnMatched(LocalDate repDate, String template, String compareTemplate,GroupCompPortfolio filter,Pageable pageable);

    @Query(value="select t from MxGroupCombinedPortfolio t where exists (select p.id from MxGroupCombinedPortfolio p where p.usrGroup=:compareTemplate"+
            " and p.reportDate=:repDate and (p.compfLbl=t.compfLbl or (p.compfLbl is null and t.compfLbl is null)) and (p.unit =t.unit or (p.unit is null and t.unit is null)) and (t.unitType = p.unitType or (p.unitType is null and t.unitType is null))) " +
            "and (coalesce(:#{#filter.compfLblList}) is null or t.compfLbl in :#{#filter.compfLblList}) and " +
            " (coalesce(:#{#filter.unitList}) is null or t.unit in :#{#filter.unitList}) and t.usrGroup=:template and  t.reportDate=:repDate order by t.compfLbl,t.unit")
    Page<MxGroupCombinedPortfolio> findAllGroupCompareCompPortfolioMatchedList(LocalDate repDate, String template, String compareTemplate,GroupCompPortfolio filter,Pageable pageable);

    @Query(value = "select t from MxGroupCombinedPortfolio t where t.reportDate = :requestDate and t.usrGroup in :templateValue and (upper(t.compfLbl) like (concat('%', upper(:searchString), '%')) or "+
            "upper(t.unitType) like (concat('%', upper(:searchString), '%')) or upper(t.unit) like (concat('%', upper(:searchString), '%')) or upper(t.usrGroup) like (concat('%', upper(:searchString), '%')))")
    Page<MxGroupCombinedPortfolio> findByGroupLabelAndReportDateListwithSearch(List<String> templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxGroupCombinedPortfolio t where t.reportDate = :requestDate and (upper(t.compfLbl) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.unitType) like (concat('%', upper(:searchString), '%')) or upper(t.unit) like (concat('%', upper(:searchString), '%')) or upper(t.usrGroup) like (concat('%', upper(:searchString), '%')))")
    Page<MxGroupCombinedPortfolio> findCombinedPortfolioGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);
}


