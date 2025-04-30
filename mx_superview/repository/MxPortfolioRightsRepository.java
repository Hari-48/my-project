package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupPortfolioRights;
import com.finsurge.tmr_portal.mx_superview.entity.MxGroupsListItem;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompareFilter;
import com.finsurge.tmr_portal.mx_superview.models.PortfolioRights;
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
public interface MxPortfolioRightsRepository extends CrudRepository<MxGroupPortfolioRights, Long> {

    Page<MxGroupPortfolioRights> findAllByGroupLabelAndSysDate(String groupLabel, LocalDate date, Pageable pageable);

    MxGroupPortfolioRights findTopByGroupLabelAndReportDate(String groupLabel, LocalDate repDate);
    
    @Query(value = "select t1 from MxGroupPortfolioRights t1 where t1.groupLabel = :groupLabel and t1.reportDate = :baseReportDate " +
            "and t1.portfolioLabel in (select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :groupLabel and t2.reportDate = :compareReportDate ) " +
            "order by t1.portfolioLabel")
    Page<MxGroupPortfolioRights> findByMxGroupPortfolioRightsByGroupLabelAndReportDate(String groupLabel,LocalDate baseReportDate, LocalDate compareReportDate,  Pageable pageable);

    @Query(value = "select t1 from MxGroupPortfolioRights t1 where " +
            "t1.groupLabel = :groupLabel and t1.reportDate = :baseReportDate and t1.portfolioLabel not in " +
            "(select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :groupLabel and t2.reportDate= :compareReportDate)")
    Page<MxGroupPortfolioRights> getAdditionalPortfolioRigthsByRepDate(String groupLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);

    @Query(value = "select t1 from MxGroupPortfolioRights t1 where " +
            "t1.groupLabel = :groupLabel and t1.reportDate = :compareReportDate and t1.portfolioLabel not in " +
            "(select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :groupLabel and t2.reportDate= :baseReportDate)")
    Page<MxGroupPortfolioRights> getAdditionalPortfolioRigthsByCompareDate(String groupLabel, LocalDate baseReportDate, LocalDate compareReportDate, Pageable pageable);
    
    List<MxGroupPortfolioRights> findTopByReportDateAndGroupLabelIn(LocalDate reportDate, List<String> groupLabel);

    Page<MxGroupPortfolioRights> findAllByReportDate(LocalDate requestDate,Pageable pageable);

    @Query(value = "select distinct t from MxGroupPortfolioRights t where t.reportDate=:requestDate and t.groupLabel in :templateValue")
    Page<MxGroupPortfolioRights> findByGroupLabelAndReportDate(List<String> templateValue, LocalDate requestDate,Pageable pageable);

    @Query(value = "select  t from MxGroupPortfolioRights t where t.groupLabel = :templateValue and t.reportDate=:requestDate")
    Page<MxGroupPortfolioRights> findByGroupLabelAndReportDateList(String templateValue, LocalDate requestDate, Pageable pageable);

//    @Query(value = "select t from MxGroupPortfolioRights t where " +
//            "(coalesce(:#{#filter.getRightType()}) is null or t.rightType in :#{#filter.getRightType()}) and " +
//            "(coalesce(:#{#filter.portfolioLabel}) is null or t.portfolioLabel in :#{#filter.portfolioLabel}) and " +
//            "(coalesce(:#{#filter.rights}) is null or t.rights in :#{#filter.rights}) and " +
//            "(coalesce(:#{#filter.treeLevel}) is null or t.treeLevel in :#{#filter.treeLevel}) and " +
//            "(coalesce(:#{#filter.portfolioType}) is null or t.portfolioType in :#{#filter.portfolioType}) and " +
//            "(coalesce(:#{#filter.branch}) is null or t.branch in :#{#filter.branch}) and " +
//            "(coalesce(:#{#filter.department}) is null or t.department in :#{#filter.department}) and " +
//            "(coalesce(:#{#filter.ENTITY}) is null or t.ENTITY in :#{#filter.ENTITY}) and " +
//            "(coalesce(:#{#filter.prodtype}) is null or t.prodtype in :#{#filter.prodtype}) and " +
//            "(coalesce(:#{#filter.autoRoll}) is null or t.autoRoll in :#{#filter.autoRoll}) and " +
//            "(coalesce(:#{#filter.manualRoll}) is null or t.manualRoll in :#{#filter.manualRoll}) and " +
//            "(coalesce(:#{#filter.autoSweep}) is null or t.autoSweep in :#{#filter.autoSweep}) and " +
//            "(coalesce(:#{#filter.accSection}) is null or t.accSection in :#{#filter.accSection}) and " +
//            "(coalesce(:#{#filter.accCur}) is null or t.accCur in :#{#filter.accCur}) and " +
//            "(coalesce(:#{#filter.trdSection}) is null or t.trdSection in :#{#filter.trdSection}) and " +
//            "(coalesce(:#{#filter.closingEntity}) is null or t.closingEntity in :#{#filter.closingEntity}) and " +
//            "(coalesce(:#{#filter.procArea}) is null or t.procArea in :#{#filter.procArea}) and " +
//            "(coalesce(:#{#filter.legalEntity}) is null or t.legalEntity in :#{#filter.legalEntity}) and " +
//            "(coalesce(:#{#filter.grpDesc}) is null or t.grpDesc in :#{#filter.grpDesc}) and " +
//            "(coalesce(:#{#filter.level0}) is null or t.level0 in :#{#filter.level0}) and " +
//            "(coalesce(:#{#filter.level1}) is null or t.level1 in :#{#filter.level1}) and " +
//            "(coalesce(:#{#filter.level2}) is null or t.level2 in :#{#filter.level2}) and " +
//            "(coalesce(:#{#filter.level3}) is null or t.level3 in :#{#filter.level3}) and " +
//            "(coalesce(:#{#filter.level4}) is null or t.level4 in :#{#filter.level4}) and " +
//            "(coalesce(:#{#filter.level5}) is null or t.level5 in :#{#filter.level5}) and " +
//            "(coalesce(:#{#filter.level6}) is null or t.level6 in :#{#filter.level6}) and " +
//            "(coalesce(:#{#filter.description}) is null or t.description in :#{#filter.description}) and " +
//            "(coalesce(:#{#filter.comment1}) is null or t.comment1 in :#{#filter.comment1}) and " +
//            "(coalesce(:#{#filter.comment2}) is null or t.comment2 in :#{#filter.comment2}) and " +
//            "(coalesce(:#{#filter.comment3}) is null or t.comment3 in :#{#filter.comment3}) and " +
//            "(coalesce(:#{#filter.comment4}) is null or t.comment4 in :#{#filter.comment4}) and " +
//            "(coalesce(:#{#filter.comment5}) is null or t.comment5 in :#{#filter.comment5}) and " +
//            "(coalesce(:#{#filter.pastCashProceed}) is null or t.pastCashProceed in :#{#filter.pastCashProceed}) and "+
//            "t.groupLabel = :templateValue and t.reportDate=:requestDate")
//    Page<MxGroupPortfolioRights> findByGroupLabelAndReportDateList(String templateValue,  LocalDate requestDate, Pageable pageable);

    @Query(value = "select t1 from MxGroupPortfolioRights t1 where t1.groupLabel = :groupLabel and t1.reportDate = :reportDate " +
            "and t1.portfolioLabel in (select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :comparedgroupLabel and t2.reportDate = :reportDate ) " +
            "order by t1.portfolioLabel")
    Page<MxGroupPortfolioRights> findAllPortfolioGroupCompareExport(LocalDate reportDate, String groupLabel, String comparedgroupLabel, Pageable pageable );

    @Query(value = "select t1 from MxGroupPortfolioRights t1 where " +
            "t1.groupLabel = :groupLabel and t1.reportDate = :reportDate and t1.portfolioLabel not in " +
            "(select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :comparedGroupLabel and t2.reportDate= :reportDate)")
    Page<MxGroupPortfolioRights> findAllPortfolioGroupCompareAdditionalExport(LocalDate reportDate, String groupLabel, String comparedGroupLabel, Pageable pageable );

    @Query(value = "select t1 from MxGroupPortfolioRights t1 where t1.groupLabel = :groupLabel and t1.reportDate = :reportDate " +
            "and t1.portfolioLabel in (select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :comparedGroupLabel and t2.reportDate = :reportDate ) " +
            "order by t1.portfolioLabel")
    List<MxGroupPortfolioRights> findAllPortfolioGroupCompare(LocalDate reportDate, String groupLabel, String comparedGroupLabel );

    @Query(value = "select t1 from MxGroupPortfolioRights t1 where " +
            "t1.groupLabel = :groupLabel and t1.reportDate = :reportDate and t1.portfolioLabel not in " +
            "(select t2.portfolioLabel from MxGroupPortfolioRights t2 where t2.groupLabel = :comparedGroupLabel and t2.reportDate= :reportDate)")
    List<MxGroupPortfolioRights> findAllPortfolioGroupCompareAdditional(LocalDate reportDate, String groupLabel, String comparedGroupLabel );

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :reportDate and   exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :reportDate and t.portfolioLabel=p.portfolioLabel )" +
            "and  (coalesce(:#{#filter.portfolioLabelList}) is null or t.portfolioLabel in :#{#filter.portfolioLabelList}) order by t.portfolioLabel")
    Page<MxGroupPortfolioRights> findAllPortfolioGroupCompare(LocalDate reportDate, String groupLabel, String compareGroupLabel, PortfolioRights filter,Pageable pageable );

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :reportDate and not  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :reportDate and t.portfolioLabel=p.portfolioLabel )" +
            "and  (coalesce(:#{#filter.portfolioLabelList}) is null or t.portfolioLabel in :#{#filter.portfolioLabelList}) ")
    Page<MxGroupPortfolioRights> findAllPortfolioGroupCompareAdditional(LocalDate reportDate, String groupLabel, String compareGroupLabel, PortfolioRights filter, Pageable pageable );

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :compareTemplate and t.reportDate = :reportDate and   " +
            "(t.portfolioLabel in :portfolioLabel or t.portfolioLabel is null) order by t.portfolioLabel")
    List<MxGroupPortfolioRights> findMatchedDataByPortfolioTree(LocalDate reportDate, String compareTemplate, List<String> portfolioLabel);

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :compareTemplate and t.reportDate = :reportDate and   " +
            "(t.portfolioLabel in :portfolioLabel or t.portfolioLabel is null) order by FIELD(t.portfolioLabel,:portfolioLabel)")
    List<MxGroupPortfolioRights> findMatchedDataByPortfolioTreeList(LocalDate reportDate, String compareTemplate, List<String> portfolioLabel);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.PortfolioRights(t.portfolioLabel)  from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel" +
            " and (p.grpDesc = t.grpDesc or (p.grpDesc is null or t.grpDesc is null) ) and (p.rightType= t.rightType or (p.rightType is null or t.rightType is null)) and" +
            " (p.rights= t.rights or (p.rights is null or t.rights is null)) and (p.treeLevel = t.treeLevel or (p.treeLevel is null or t.treeLevel is null)) and " +
            "(p.portfolioType = t.portfolioType or (p.portfolioType is null or t.portfolioType is null)) and (p.description = t.description or (p.description is null or t.description is null)) " +
            "and (p.pastCashProceed = t.pastCashProceed or (p.pastCashProceed is null or t.pastCashProceed is null)) and (p.level6 = t.level6 or (p.level6 is null or t.level6 is null)) " +
            "and (p.level5 = t.level5 or (p.level5 is null or t.level5 is null)) and (p.level4 = t.level4 or (p.level4 is null or t.level4 is null)) and " +
            "(p.level3 = t.level3 or (p.level3 is null or t.level3 is null)) and (p.level2 = t.level2 or (p.level2 is null or t.level2 is null)) " +
            " and (p.level1 = t.level1 or (p.level1 is null or t.level1 is null)) and (p.level0 = t.level0 or (p.level0 is null or t.level0 is null)) and" +
            " (p.branch = t.branch or (p.branch is null or t.branch is null)) and (p.department = t.department or (p.department is null or t.department is null)) " +
            "and (p.ENTITY = t.ENTITY or (p.ENTITY is null or t.ENTITY is null)) and (p.prodtype = t.prodtype or (p.prodtype is null or t.prodtype is null)) and " +
            "(p.autoRoll = t.autoRoll or (p.autoRoll is null or t.autoRoll is null)) and (p.manualRoll = t.manualRoll or (p.manualRoll is null or t.manualRoll is null)) " +
            " and (p.autoSweep = t.autoSweep or (p.autoSweep is null or t.autoSweep is null)) and (p.accSection = t.accSection or (p.accSection is null or t.accSection is null)) " +
            "and (p.accCur = t.accCur or (p.accCur is null or t.accCur is null)) and (p.trdSection = t.trdSection or (p.trdSection is null or t.trdSection is null)) and " +
            "(p.closingEntity = t.closingEntity or (p.closingEntity is null or t.closingEntity is null)) and " +
            " (p.procArea = t.procArea or (p.procArea is null or t.procArea is null)) and (p.legalEntity = t.legalEntity or (p.legalEntity is null or t.legalEntity is null)) and " +
            "(p.comment1 = t.comment1 or (p.comment1 is null or t.comment1 is null)) and (p.comment2 = t.comment2 or (p.comment2 is null or t.comment2 is null)) and " +
            "(p.comment3 = t.comment3 or (p.comment3 is null or t.comment3 is null)) and (p.comment4 = t.comment4 or (p.comment4 is null or t.comment4 is null)) and " +
            "(p.comment5 = t.comment5 or (p.comment5 is null or t.comment5 is null))  and (p.comment6 = t.comment6 or (p.comment6 is null or t.comment6 is null)))" +
            "  and (:search is null or upper(t.portfolioLabel) like  upper(concat('%',:search,'%')))   order by t.portfolioLabel ")
    List<PortfolioRights> findGeneralPropertyListForPortfolioMatched(String groupLabel, String compareGroupLabel,LocalDate repDate,String search,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.PortfolioRights(t.portfolioLabel)  from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel" +
            " and ((coalesce( p.grpDesc ,'null') <> coalesce(t.grpDesc,'null')) or (coalesce(p.rightType,'null') <> coalesce(t.rightType ,'null')) or (coalesce( p.rights,'null') <> coalesce( t.rights,'null'))  or "+
            " (coalesce( p.treeLevel ,'null') <> coalesce(t.treeLevel ,'null')) or (coalesce( p.portfolioType ,'null') <> coalesce( t.portfolioType,'null')) " +
            " or (coalesce( p.description ,'null') <> coalesce( t.description ,'null')) or (coalesce( p.pastCashProceed ,'null') <> coalesce( t.pastCashProceed ,'null')) or "+
            "(coalesce( p.level6 ,'null') <> coalesce( t.level6 ,'null')) or (coalesce( p.level5 ,'null') <> coalesce( t.level5 ,'null')) or (coalesce( p.level4 ,'null') <> coalesce( t.level4 ,'null')) or "+
            "(coalesce( p.level3 ,'null') <> coalesce( t.level3 ,'null'))  or (coalesce( p.level2 ,'null') <> coalesce( t.level2,'null'))" +
            " or (coalesce( p.level1 ,'null') <> coalesce( t.level1 ,'null')) or (coalesce( p.level0 ,'null') <> coalesce( t.level0 ,'null')) or (coalesce( p.branch ,'null') <> coalesce( t.branch,'null')) "+
            "or (coalesce(p.department ,'null') <> coalesce( t.department,'null')) or (coalesce( p.ENTITY ,'null') <> coalesce( t.ENTITY,'null')) or (coalesce( p.prodtype ,'null') <> coalesce( t.prodtype,'null')) or "+
            "(coalesce( p.autoRoll ,'null') <> coalesce( t.autoRoll ,'null')) or (coalesce(p.manualRoll ,'null') <> coalesce( t.manualRoll ,'null')) or (coalesce( p.autoSweep ,'null') <> coalesce( t.autoSweep ,'null')) or (coalesce( p.accSection ,'null') <> coalesce( t.accSection,'null')) "+
            "or (coalesce( p.accCur ,'null') <> coalesce( t.accCur ,'null')) or (coalesce( p.trdSection ,'null') <> coalesce( t.trdSection,'null'))  or (coalesce( p.closingEntity ,'null') <> coalesce( t.closingEntity,'null')) "+
            "or (coalesce(  p.procArea ,'null') <> coalesce( t.procArea ,'null')) or (coalesce( p.legalEntity ,'null') <> coalesce( t.legalEntity ,'null')) or (coalesce( p.comment1 ,'null') <> coalesce( t.comment1 ,'null')) "+
            "or (coalesce( p.comment2 ,'null') <> coalesce( t.comment2  ,'null')) or (coalesce( p.comment3 ,'null') <> coalesce( t.comment3,'null')) or (coalesce( p.comment4 ,'null') <> coalesce( t.comment4  ,'null')) "+
            "or (coalesce( p.comment5 ,'null') <> coalesce( t.comment5  ,'null'))  or (coalesce( p.comment6 ,'null') <> coalesce( t.comment6,'null')))) " +
            "  and (:search is null or upper(t.portfolioLabel) like  upper(concat('%',:search,'%')))  order by t.portfolioLabel")
    List<PortfolioRights> findGeneralPropertyListForPortfolioUnMatched(String groupLabel, String compareGroupLabel, LocalDate repDate,String search,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.PortfolioRights(t.portfolioLabel) from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and   exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel )" +
            " and (:search is null or upper(t.portfolioLabel) like  upper(concat('%',:search,'%'))) order by t.portfolioLabel")
    List<PortfolioRights> findGeneralPropertyListForPortfolioAll(String groupLabel, String compareGroupLabel, LocalDate repDate,String search,Pageable pageable);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.PortfolioRights(t.portfolioLabel) from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and not  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel ) " +
            " and (:search is null or upper(t.portfolioLabel) like  upper(concat('%',:search,'%'))) order by t.portfolioLabel")
    List<PortfolioRights> findGeneralPropertyListForPortfolioAdditional(String groupLabel, String compareGroupLabel, LocalDate repDate,String search,Pageable pageable);

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel" +
            " and ((coalesce( p.grpDesc ,'null') <> coalesce(t.grpDesc,'null')) or (coalesce(p.rightType,'null') <> coalesce(t.rightType ,'null')) or (coalesce( p.rights,'null') <> coalesce( t.rights,'null'))  or "+
            " (coalesce( p.treeLevel ,'null') <> coalesce(t.treeLevel ,'null')) or (coalesce( p.portfolioType ,'null') <> coalesce( t.portfolioType,'null')) " +
            " or (coalesce( p.description ,'null') <> coalesce( t.description ,'null')) or (coalesce( p.pastCashProceed ,'null') <> coalesce( t.pastCashProceed ,'null')) or "+
            "(coalesce( p.level6 ,'null') <> coalesce( t.level6 ,'null')) or (coalesce( p.level5 ,'null') <> coalesce( t.level5 ,'null')) or (coalesce( p.level4 ,'null') <> coalesce( t.level4 ,'null')) or "+
            "(coalesce( p.level3 ,'null') <> coalesce( t.level3 ,'null'))  or (coalesce( p.level2 ,'null') <> coalesce( t.level2,'null'))" +
            " or (coalesce( p.level1 ,'null') <> coalesce( t.level1 ,'null')) or (coalesce( p.level0 ,'null') <> coalesce( t.level0 ,'null')) or (coalesce( p.branch ,'null') <> coalesce( t.branch,'null')) "+
            "or (coalesce(p.department ,'null') <> coalesce( t.department,'null')) or (coalesce( p.ENTITY ,'null') <> coalesce( t.ENTITY,'null')) or (coalesce( p.prodtype ,'null') <> coalesce( t.prodtype,'null')) or "+
            "(coalesce( p.autoRoll ,'null') <> coalesce( t.autoRoll ,'null')) or (coalesce(p.manualRoll ,'null') <> coalesce( t.manualRoll ,'null')) or (coalesce( p.autoSweep ,'null') <> coalesce( t.autoSweep ,'null')) or (coalesce( p.accSection ,'null') <> coalesce( t.accSection,'null')) "+
            "or (coalesce( p.accCur ,'null') <> coalesce( t.accCur ,'null')) or (coalesce( p.trdSection ,'null') <> coalesce( t.trdSection,'null'))  or (coalesce( p.closingEntity ,'null') <> coalesce( t.closingEntity,'null')) "+
            "or (coalesce(  p.procArea ,'null') <> coalesce( t.procArea ,'null')) or (coalesce( p.legalEntity ,'null') <> coalesce( t.legalEntity ,'null')) or (coalesce( p.comment1 ,'null') <> coalesce( t.comment1 ,'null')) "+
            "or (coalesce( p.comment2 ,'null') <> coalesce( t.comment2  ,'null')) or (coalesce( p.comment3 ,'null') <> coalesce( t.comment3,'null')) or (coalesce( p.comment4 ,'null') <> coalesce( t.comment4  ,'null')) "+
            "or (coalesce( p.comment5 ,'null') <> coalesce( t.comment5  ,'null'))  or (coalesce( p.comment6 ,'null') <> coalesce( t.comment6,'null'))))" +
            " and  (coalesce(:#{#filter.portfolioLabelList}) is null or t.portfolioLabel in :#{#filter.portfolioLabelList})  order by t.portfolioLabel")
    Page<MxGroupPortfolioRights> findAllPortfolioGroupCompareUnMatched(LocalDate repDate, String groupLabel, String compareGroupLabel,  PortfolioRights filter, Pageable pageable);

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel" +
            " and (p.grpDesc = t.grpDesc or (p.grpDesc is null or t.grpDesc is null) ) and (p.rightType= t.rightType or (p.rightType is null or t.rightType is null)) and" +
            " (p.rights= t.rights or (p.rights is null or t.rights is null)) and (p.treeLevel = t.treeLevel or (p.treeLevel is null or t.treeLevel is null)) and " +
            "(p.portfolioType = t.portfolioType or (p.portfolioType is null or t.portfolioType is null)) and (p.description = t.description or (p.description is null or t.description is null)) " +
            "and (p.pastCashProceed = t.pastCashProceed or (p.pastCashProceed is null or t.pastCashProceed is null)) and (p.level6 = t.level6 or (p.level6 is null or t.level6 is null)) " +
            "and (p.level5 = t.level5 or (p.level5 is null or t.level5 is null)) and (p.level4 = t.level4 or (p.level4 is null or t.level4 is null)) and " +
            "(p.level3 = t.level3 or (p.level3 is null or t.level3 is null)) and (p.level2 = t.level2 or (p.level2 is null or t.level2 is null)) " +
            " and (p.level1 = t.level1 or (p.level1 is null or t.level1 is null)) and (p.level0 = t.level0 or (p.level0 is null or t.level0 is null)) and" +
            " (p.branch = t.branch or (p.branch is null or t.branch is null)) and (p.department = t.department or (p.department is null or t.department is null)) " +
            "and (p.ENTITY = t.ENTITY or (p.ENTITY is null or t.ENTITY is null)) and (p.prodtype = t.prodtype or (p.prodtype is null or t.prodtype is null)) and " +
            "(p.autoRoll = t.autoRoll or (p.autoRoll is null or t.autoRoll is null)) and (p.manualRoll = t.manualRoll or (p.manualRoll is null or t.manualRoll is null)) " +
            " and (p.autoSweep = t.autoSweep or (p.autoSweep is null or t.autoSweep is null)) and (p.accSection = t.accSection or (p.accSection is null or t.accSection is null)) " +
            "and (p.accCur = t.accCur or (p.accCur is null or t.accCur is null)) and (p.trdSection = t.trdSection or (p.trdSection is null or t.trdSection is null)) and " +
            "(p.closingEntity = t.closingEntity or (p.closingEntity is null or t.closingEntity is null)) and " +
            " (p.procArea = t.procArea or (p.procArea is null or t.procArea is null)) and (p.legalEntity = t.legalEntity or (p.legalEntity is null or t.legalEntity is null)) and " +
            "(p.comment1 = t.comment1 or (p.comment1 is null or t.comment1 is null)) and (p.comment2 = t.comment2 or (p.comment2 is null or t.comment2 is null)) and " +
            "(p.comment3 = t.comment3 or (p.comment3 is null or t.comment3 is null)) and (p.comment4 = t.comment4 or (p.comment4 is null or t.comment4 is null)) and " +
            "(p.comment5 = t.comment5 or (p.comment5 is null or t.comment5 is null))  and (p.comment6 = t.comment6 or (p.comment6 is null or t.comment6 is null)))" +
            " and  (coalesce(:#{#filter.portfolioLabelList}) is null or t.portfolioLabel in :#{#filter.portfolioLabelList})  order by t.portfolioLabel")
    Page<MxGroupPortfolioRights> findAllPortfolioGroupCompareMatchedList(LocalDate repDate, String groupLabel, String compareGroupLabel, PortfolioRights filter,  Pageable pageable);

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel = :templateValue and t.reportDate = :requestDate and ((upper(t.grpDesc) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.rightType) like concat('%',upper(:searchString),'%') ) or (upper(t.portfolioLabel) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.rights) like concat('%',upper(:searchString),'%') ) or (upper(t.treeLevel) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.portfolioType) like concat('%',upper(:searchString),'%') ) or (upper(t.description) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.pastCashProceed) like concat('%',upper(:searchString),'%') ) or (upper(t.level6) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level5) like concat('%',upper(:searchString),'%') ) or (upper(t.level4) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level3) like concat('%',upper(:searchString),'%') ) or (upper(t.level2) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level1) like concat('%',upper(:searchString),'%') ) or (upper(t.level0) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.branch) like concat('%',upper(:searchString),'%') ) or (upper(t.department) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.ENTITY) like concat('%',upper(:searchString),'%') ) or (upper(t.prodtype) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.autoRoll) like concat('%',upper(:searchString),'%') ) or (upper(t.comment3) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.manualRoll) like concat('%',upper(:searchString),'%') ) or (upper(t.autoSweep) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.trdSection) like concat('%',upper(:searchString),'%') ) or (upper(t.closingEntity) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.procArea) like concat('%',upper(:searchString),'%') ) or (upper(t.legalEntity) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment1) like concat('%',upper(:searchString),'%') ) or (upper(t.comment2) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment4) like concat('%',upper(:searchString),'%') ) or (upper(t.comment5) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment6) like concat('%',upper(:searchString),'%') ) or (upper(t.accCur) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.accSection) like concat('%',upper(:searchString),'%') ) )")
    Page<MxGroupPortfolioRights> findByGroupLabelAndReportDateListWithSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxGroupPortfolioRights t where t.groupLabel in :templateValue and t.reportDate = :requestDate and ((upper(t.grpDesc) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.rightType) like concat('%',upper(:searchString),'%') ) or (upper(t.portfolioLabel) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.rights) like concat('%',upper(:searchString),'%') ) or (upper(t.treeLevel) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.portfolioType) like concat('%',upper(:searchString),'%') ) or (upper(t.description) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.pastCashProceed) like concat('%',upper(:searchString),'%') ) or (upper(t.level6) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level5) like concat('%',upper(:searchString),'%') ) or (upper(t.level4) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level3) like concat('%',upper(:searchString),'%') ) or (upper(t.level2) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level1) like concat('%',upper(:searchString),'%') ) or (upper(t.level0) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.branch) like concat('%',upper(:searchString),'%') ) or (upper(t.department) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.ENTITY) like concat('%',upper(:searchString),'%') ) or (upper(t.prodtype) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.autoRoll) like concat('%',upper(:searchString),'%') ) or (upper(t.comment3) like concat('%',upper(:searchString),'%') ) " +
            " or (upper(t.manualRoll) like concat('%',upper(:searchString),'%') ) or (upper(t.autoSweep) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.trdSection) like concat('%',upper(:searchString),'%') ) or (upper(t.closingEntity) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.procArea) like concat('%',upper(:searchString),'%') ) or (upper(t.legalEntity) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment1) like concat('%',upper(:searchString),'%') ) or (upper(t.comment2) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment4) like concat('%',upper(:searchString),'%') ) or (upper(t.comment5) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment6) like concat('%',upper(:searchString),'%') ) or (upper(t.accCur) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.accSection) like concat('%',upper(:searchString),'%') ) or (upper(t.groupLabel) like concat('%',upper(:searchString),'%') ) )")
    Page<MxGroupPortfolioRights> findByGroupLabelAndReportDateListWithSearchCombined(List<String> templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxGroupPortfolioRights t where t.reportDate = :requestDate and ((upper(t.grpDesc) like concat('%', upper(:searchString), '%' ) )" +
            " or (upper(t.rightType) like concat('%',upper(:searchString),'%') ) or (upper(t.portfolioLabel) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.rights) like concat('%',upper(:searchString),'%') ) or (upper(t.treeLevel) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.portfolioType) like concat('%',upper(:searchString),'%') ) or (upper(t.description) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.pastCashProceed) like concat('%',upper(:searchString),'%') ) or (upper(t.level6) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level5) like concat('%',upper(:searchString),'%') ) or (upper(t.level4) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level3) like concat('%',upper(:searchString),'%') ) or (upper(t.level2) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.level1) like concat('%',upper(:searchString),'%') ) or (upper(t.level0) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.branch) like concat('%',upper(:searchString),'%') ) or (upper(t.department) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.ENTITY) like concat('%',upper(:searchString),'%') ) or (upper(t.prodtype) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.autoRoll) like concat('%',upper(:searchString),'%') ) or (upper(t.comment3) like concat('%',upper(:searchString),'%') ) " +
            " or (upper(t.manualRoll) like concat('%',upper(:searchString),'%') ) or (upper(t.autoSweep) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.trdSection) like concat('%',upper(:searchString),'%') ) or (upper(t.closingEntity) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.procArea) like concat('%',upper(:searchString),'%') ) or (upper(t.legalEntity) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment1) like concat('%',upper(:searchString),'%') ) or (upper(t.comment2) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment4) like concat('%',upper(:searchString),'%') ) or (upper(t.comment5) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.comment6) like concat('%',upper(:searchString),'%') ) or (upper(t.accCur) like concat('%',upper(:searchString),'%') )" +
            " or (upper(t.accSection) like concat('%',upper(:searchString),'%') ) or (upper(t.groupLabel) like concat('%',upper(:searchString),'%') ) )")
    Page<MxGroupPortfolioRights> findByReportDateWithSearchCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select count(t) from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel" +
            " and (p.grpDesc = t.grpDesc or (p.grpDesc is null or t.grpDesc is null) ) and (p.rightType= t.rightType or (p.rightType is null or t.rightType is null)) and" +
            " (p.rights= t.rights or (p.rights is null or t.rights is null)) and (p.treeLevel = t.treeLevel or (p.treeLevel is null or t.treeLevel is null)) and " +
            "(p.portfolioType = t.portfolioType or (p.portfolioType is null or t.portfolioType is null)) and (p.description = t.description or (p.description is null or t.description is null)) " +
            "and (p.pastCashProceed = t.pastCashProceed or (p.pastCashProceed is null or t.pastCashProceed is null)) and (p.level6 = t.level6 or (p.level6 is null or t.level6 is null)) " +
            "and (p.level5 = t.level5 or (p.level5 is null or t.level5 is null)) and (p.level4 = t.level4 or (p.level4 is null or t.level4 is null)) and " +
            "(p.level3 = t.level3 or (p.level3 is null or t.level3 is null)) and (p.level2 = t.level2 or (p.level2 is null or t.level2 is null)) " +
            " and (p.level1 = t.level1 or (p.level1 is null or t.level1 is null)) and (p.level0 = t.level0 or (p.level0 is null or t.level0 is null)) and" +
            " (p.branch = t.branch or (p.branch is null or t.branch is null)) and (p.department = t.department or (p.department is null or t.department is null)) " +
            "and (p.ENTITY = t.ENTITY or (p.ENTITY is null or t.ENTITY is null)) and (p.prodtype = t.prodtype or (p.prodtype is null or t.prodtype is null)) and " +
            "(p.autoRoll = t.autoRoll or (p.autoRoll is null or t.autoRoll is null)) and (p.manualRoll = t.manualRoll or (p.manualRoll is null or t.manualRoll is null)) " +
            " and (p.autoSweep = t.autoSweep or (p.autoSweep is null or t.autoSweep is null)) and (p.accSection = t.accSection or (p.accSection is null or t.accSection is null)) " +
            "and (p.accCur = t.accCur or (p.accCur is null or t.accCur is null)) and (p.trdSection = t.trdSection or (p.trdSection is null or t.trdSection is null)) and " +
            "(p.closingEntity = t.closingEntity or (p.closingEntity is null or t.closingEntity is null)) and " +
            " (p.procArea = t.procArea or (p.procArea is null or t.procArea is null)) and (p.legalEntity = t.legalEntity or (p.legalEntity is null or t.legalEntity is null)) and " +
            "(p.comment1 = t.comment1 or (p.comment1 is null or t.comment1 is null)) and (p.comment2 = t.comment2 or (p.comment2 is null or t.comment2 is null)) and " +
            "(p.comment3 = t.comment3 or (p.comment3 is null or t.comment3 is null)) and (p.comment4 = t.comment4 or (p.comment4 is null or t.comment4 is null)) and " +
            "(p.comment5 = t.comment5 or (p.comment5 is null or t.comment5 is null))  and (p.comment6 = t.comment6 or (p.comment6 is null or t.comment6 is null)))" )
    Long findAllPortfolioGroupCompareMatchedCount(LocalDate repDate, String groupLabel, String compareGroupLabel);


    @Query(value = "select count(t) from MxGroupPortfolioRights t where t.groupLabel = :groupLabel and t.reportDate = :repDate and  exists  " +
            "(select p.id from MxGroupPortfolioRights p where p.groupLabel = :compareGroupLabel and p.reportDate= :repDate and t.portfolioLabel=p.portfolioLabel" +
            " and ((coalesce( p.grpDesc ,'null') <> coalesce(t.grpDesc,'null')) or (coalesce(p.rightType,'null') <> coalesce(t.rightType ,'null')) or (coalesce( p.rights,'null') <> coalesce( t.rights,'null'))  or "+
            " (coalesce( p.treeLevel ,'null') <> coalesce(t.treeLevel ,'null')) or (coalesce( p.portfolioType ,'null') <> coalesce( t.portfolioType,'null')) " +
            " or (coalesce( p.description ,'null') <> coalesce( t.description ,'null')) or (coalesce( p.pastCashProceed ,'null') <> coalesce( t.pastCashProceed ,'null')) or "+
            "(coalesce( p.level6 ,'null') <> coalesce( t.level6 ,'null')) or (coalesce( p.level5 ,'null') <> coalesce( t.level5 ,'null')) or (coalesce( p.level4 ,'null') <> coalesce( t.level4 ,'null')) or "+
            "(coalesce( p.level3 ,'null') <> coalesce( t.level3 ,'null'))  or (coalesce( p.level2 ,'null') <> coalesce( t.level2,'null'))" +
            " or (coalesce( p.level1 ,'null') <> coalesce( t.level1 ,'null')) or (coalesce( p.level0 ,'null') <> coalesce( t.level0 ,'null')) or (coalesce( p.branch ,'null') <> coalesce( t.branch,'null')) "+
            "or (coalesce(p.department ,'null') <> coalesce( t.department,'null')) or (coalesce( p.ENTITY ,'null') <> coalesce( t.ENTITY,'null')) or (coalesce( p.prodtype ,'null') <> coalesce( t.prodtype,'null')) or "+
            "(coalesce( p.autoRoll ,'null') <> coalesce( t.autoRoll ,'null')) or (coalesce(p.manualRoll ,'null') <> coalesce( t.manualRoll ,'null')) or (coalesce( p.autoSweep ,'null') <> coalesce( t.autoSweep ,'null')) or (coalesce( p.accSection ,'null') <> coalesce( t.accSection,'null')) "+
            "or (coalesce( p.accCur ,'null') <> coalesce( t.accCur ,'null')) or (coalesce( p.trdSection ,'null') <> coalesce( t.trdSection,'null'))  or (coalesce( p.closingEntity ,'null') <> coalesce( t.closingEntity,'null')) "+
            "or (coalesce(  p.procArea ,'null') <> coalesce( t.procArea ,'null')) or (coalesce( p.legalEntity ,'null') <> coalesce( t.legalEntity ,'null')) or (coalesce( p.comment1 ,'null') <> coalesce( t.comment1 ,'null')) "+
            "or (coalesce( p.comment2 ,'null') <> coalesce( t.comment2  ,'null')) or (coalesce( p.comment3 ,'null') <> coalesce( t.comment3,'null')) or (coalesce( p.comment4 ,'null') <> coalesce( t.comment4  ,'null')) "+
            "or (coalesce( p.comment5 ,'null') <> coalesce( t.comment5  ,'null'))  or (coalesce( p.comment6 ,'null') <> coalesce( t.comment6,'null'))))" )
    Long findAllPortfolioGroupCompareUnMatchedCount(LocalDate repDate, String groupLabel, String compareGroupLabel);
}
