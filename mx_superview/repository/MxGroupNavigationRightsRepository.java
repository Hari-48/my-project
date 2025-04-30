package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupNavigationRight;
import com.finsurge.tmr_portal.mx_superview.models.GroupCompareFilter;
import com.finsurge.tmr_portal.mx_superview.models.MxNavigationTemplateProjection;
import com.finsurge.tmr_portal.mx_superview.models.NavigationRights;
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
public interface MxGroupNavigationRightsRepository extends CrudRepository<MxGroupNavigationRight, Long> {

    @Query(value = "select distinct t.template as template, t.comments as comments, t.menu as menu, t.path as path, t.pathLabel as pathLabel, t.pathRest as pathRest, t.rights as rights, t.submenu1 as submenu1, t.submenu2 as submenu2, t.submenu3 as submenu3, t.submenu4 as submenu4, t.submenu5 as submenu5 " +
            "from MxGroupNavigationRight t where t.template = :template and t.sysDate = :date")
    Page<MxNavigationTemplateProjection> findAllByTemplateAndSysDate(String template, LocalDate date, Pageable pageable);

    MxGroupNavigationRight findTopByTemplateAndReportDate(String template, LocalDate requestDate);

    @Query(value="select distinct t1 from MxGroupNavigationRight t1 where exists (select t2.id from MxGroupNavigationRight t2 where t2.groupLabel=:groupLabel  and t2.template=:templateLabel"+
            " and t2.reportDate=:compareReportDate and t2.groupLabel=t1.groupLabel and t2.template=t1.template and (t2.comments =t1.comments or t2.comments is null and t1.comments is null )" +
            "and (t2.rights =t1.rights or t2.rights is null and t1.rights is null ) and t2.menu =t1.menu and t2.path=t1.path) and t1.groupLabel=:groupLabel and t1.template=:templateLabel and " +
            " t1.reportDate=:baseReportDate order by t1.groupLabel, t1.template,t1.comments,t1.rights,t1.menu,t1.path")
    Page<MxGroupNavigationRight> findByNavigationByGroupLabelAndReportDate(String templateLabel,String groupLabel, LocalDate baseReportDate, LocalDate compareReportDate,  Pageable pageable);

    @Query(value="select distinct t1 from MxGroupNavigationRight t1 where not exists  (select t2.id from MxGroupNavigationRight t2 where t2.groupLabel=:groupLabel and t2.template=:templateLabel"+
            " and t2.reportDate=:compareReportDate and t2.groupLabel=t1.groupLabel and t2.template=t1.template and (t2.comments =t1.comments or t2.comments is null and t1.comments is null )" +
            "and (t2.rights =t1.rights or t2.rights is null and t1.rights is null ) and t2.menu =t1.menu and t2.path=t1.path) and t1.groupLabel=:groupLabel and t1.template=:templateLabel and  " +
            "t1.reportDate=:baseReportDate order by t1.groupLabel, t1.template,t1.comments,t1.rights,t1.menu,t1.path")
    Page<MxGroupNavigationRight> findByAdditionalNavigationByGroupLabelAndRepDate(String templateLabel,String groupLabel, LocalDate baseReportDate, LocalDate compareReportDate,  Pageable pageable);

    List<MxGroupNavigationRight>  findTopByTemplateInAndReportDate(List<String> navigationRights, LocalDate requestDate);


    @Query(value = "select distinct t from MxGroupNavigationRight t where t.reportDate=:requestDate and upper(t.template) in :templateValue and upper(t.groupLabel) in :groups ")
    Page<MxGroupNavigationRight> findByGroupLabelAndReportDate(List<String> templateValue,List<String> groups,LocalDate requestDate, Pageable pageable);

    Page<MxGroupNavigationRight> findAllByReportDate(LocalDate requestDate, Pageable pageable);

    @Query(value = "select distinct t from MxGroupNavigationRight t where t.reportDate=:requestDate and upper(t.template)=:templateValue and upper(t.groupLabel)=:group ")
    Page<MxGroupNavigationRight> findByTemplateAndReportDateList(String templateValue,String group, LocalDate requestDate, Pageable pageable);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null) )" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) ) and t.groupLabel=:groupLabel and t.template=:template and  " +
            "t.reportDate=:repDate and (:comments is null or t.comments=:comments) and (:rights is null or t.rights=:rights) and (:menu is null or t.menu=:menu)" +
            "and (:path is null or t.path=:path) order by t.comments,t.rights,t.menu,t.path")
    Page<NavigationRights> findGroupCompareNavigationTreeMap(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel,String comments,String rights,String menu,String path, Pageable pageable);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where not exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null) )" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null))) and t.groupLabel=:groupLabel and t.template=:template and  " +
            "t.reportDate=:repDate and (:comments is null or t.comments=:comments) and (:rights is null or t.rights=:rights) and (:menu is null or t.menu=:menu)" +
            "and (:path is null or t.path=:path) order by t.comments,t.rights,t.menu,t.path")
    Page<NavigationRights> findGroupCompareNavigationTreeMapAdditional(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel,String comments,String rights,String menu,String path, Pageable pageable);

    @Query(value="select  new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where  " +
            " (coalesce(:#{#filter.rightsList}) is null or t.rights in :#{#filter.rightsList}) and " +
            "(coalesce(:#{#filter.menuList}) is null or t.menu in :#{#filter.menuList}) and " +
            "(coalesce(:#{#filter.pathList}) is null or t.path in :#{#filter.pathList}) and " +
//            "(coalesce(:#{#filter.pathLabelList}) is null or t.pathLabel in :#{#filter.pathLabelList}) and " +
//            "(coalesce(:#{#filter.pathRestList}) is null or t.pathRest in :#{#filter.pathRestList}) and " +
//            "(coalesce(:#{#filter.submenuList1}) is null or t.submenu1 in :#{#filter.submenuList1}) and " +
//            "(coalesce(:#{#filter.submenuList2}) is null or t.submenu2 in :#{#filter.submenuList2}) and " +
//            "(coalesce(:#{#filter.submenuList3}) is null or t.submenu3 in :#{#filter.submenuList3}) and " +
//            "(coalesce(:#{#filter.submenuList4}) is null or t.submenu4 in :#{#filter.submenuList4}) and " +
//            "(coalesce(:#{#filter.submenuList5}) is null or t.submenu5 in :#{#filter.submenuList5}) and " +
            "(coalesce(:#{#filter.commentsList}) is null or t.comments in :#{#filter.commentsList}) and  t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null ))" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) )  order by t.groupLabel, t.template,t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findAllGroupCompareNavMatchedTreeMap(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel,NavigationRights filter, Pageable pageable);

    @Query(value="select  t from MxGroupNavigationRight t where  " +
            " (coalesce(:#{#filter.rightsList}) is null or t.rights in :#{#filter.rightsList}) and " +
            "(coalesce(:#{#filter.menuList}) is null or t.menu in :#{#filter.menuList}) and " +
            "(coalesce(:#{#filter.pathList}) is null or t.path in :#{#filter.pathList}) and " +
//            "(coalesce(:#{#filter.pathLabelList}) is null or t.pathLabel in :#{#filter.pathLabelList}) and " +
//            "(coalesce(:#{#filter.pathRestList}) is null or t.pathRest in :#{#filter.pathRestList}) and " +
//            "(coalesce(:#{#filter.submenuList1}) is null or t.submenu1 in :#{#filter.submenuList1}) and " +
//            "(coalesce(:#{#filter.submenuList2}) is null or t.submenu2 in :#{#filter.submenuList2}) and " +
//            "(coalesce(:#{#filter.submenuList3}) is null or t.submenu3 in :#{#filter.submenuList3}) and " +
//            "(coalesce(:#{#filter.submenuList4}) is null or t.submenu4 in :#{#filter.submenuList4}) and " +
//            "(coalesce(:#{#filter.submenuList5}) is null or t.submenu5 in :#{#filter.submenuList5}) and " +
            "(coalesce(:#{#filter.commentsList}) is null or t.comments in :#{#filter.commentsList})  and " +
            " t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null ))" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) )  order by t.groupLabel, t.template,t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findAllGroupCompareNavMatched(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel,NavigationRights filter, Pageable pageable);

    @Query(value="select  t from MxGroupNavigationRight t where  " +
           " t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null ))" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null))) ")
    Page<MxGroupNavigationRight> findAllGroupCompareNavigationMatched(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel, Pageable pageable);

    @Query(value="select  t from MxGroupNavigationRight t where  " +
            " (coalesce(:#{#filter.rightsList}) is null or t.rights in :#{#filter.rightsList}) and " +
            "(coalesce(:#{#filter.menuList}) is null or t.menu in :#{#filter.menuList}) and " +
            "(coalesce(:#{#filter.pathList}) is null or t.path in :#{#filter.pathList}) and " +
            "(coalesce(:#{#filter.commentsList}) is null or t.comments in :#{#filter.commentsList}) and  t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and not exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null ))" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) ) ")
    Page<MxGroupNavigationRight> findAllGroupCompareNavAdditional(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel,NavigationRights filter, Pageable pageable);


    @Query(value="select   new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where  " +
            " (coalesce(:#{#filter.rightsList}) is null or t.rights in :#{#filter.rightsList}) and " +
            "(coalesce(:#{#filter.menuList}) is null or t.menu in :#{#filter.menuList}) and " +
            "(coalesce(:#{#filter.pathList}) is null or t.path in :#{#filter.pathList}) and " +
            "(coalesce(:#{#filter.pathLabelList}) is null or t.pathLabel in :#{#filter.pathLabelList}) and " +
            "(coalesce(:#{#filter.pathRestList}) is null or t.pathRest in :#{#filter.pathRestList}) and " +
            "(coalesce(:#{#filter.submenuList1}) is null or t.submenu1 in :#{#filter.submenuList1}) and " +
            "(coalesce(:#{#filter.submenuList2}) is null or t.submenu2 in :#{#filter.submenuList2}) and " +
            "(coalesce(:#{#filter.submenuList3}) is null or t.submenu3 in :#{#filter.submenuList3}) and " +
            "(coalesce(:#{#filter.submenuList4}) is null or t.submenu4 in :#{#filter.submenuList4}) and " +
            "(coalesce(:#{#filter.submenuList5}) is null or t.submenu5 in :#{#filter.submenuList5}) and " +
            "(coalesce(:#{#filter.commentsList}) is null or t.comments in :#{#filter.commentsList}) and  t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and not exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null ))" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) )  order by t.groupLabel, t.template,t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findAllGroupCompareNavAdditionalTreeMap(LocalDate repDate, String template, String compareTemplate, String groupLabel, String compareGroupLabel,NavigationRights filter, Pageable pageable);

    @Query(value=" select t from MxGroupNavigationRight t where t.reportDate =:repDate and  ( t.comments in (:commentsList)  or t.comments is null ) " +
            " and (t.rights in (:rightsList) or t.rights is null)  and ( t.menu in (:menuList) or t.menu is null ) and (t.path in (:pathList) or t.path is null ) " +
            " and t.template=:compareTemplate and t.groupLabel=:compareGroupLabel")
    List<MxGroupNavigationRight> findMatchedDataByTree(LocalDate repDate, String compareTemplate,String compareGroupLabel, List<String> commentsList, List<String> rightsList, List<String> menuList, List<String> pathList);

//    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where  exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
//            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null) )" +
//            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null))) and t.groupLabel=:groupLabel and t.template=:template and  " +
//            "t.reportDate=:repDate  order by t.comments,t.rights,t.menu,t.path")
//    List<NavigationRights> findGeneralPropertyListForNavMatched(String template, String compareTemplate, String groupLabel, String compareGroupLabel, LocalDate repDate);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where not exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null) )" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null))) and t.groupLabel=:groupLabel and t.template=:template and  " +
            "t.reportDate=:repDate ")
    List<NavigationRights> findGeneralPropertyListForNavAdditional(String template, String compareTemplate, String groupLabel, String compareGroupLabel, LocalDate repDate);

    @Query(value = "select  t from MxGroupNavigationRight t where not exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupName and p.template=:compareTemplateValue" +
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null ))" + "and (p.rights=t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or(p.path is null and t.path is null)))and t.groupLabel=:groupName and t.template=:templateValue and  " +
            "t.reportDate=:repDate   order by t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findAllNavigationGroupCompareAdditionalExport(LocalDate repDate, String groupName, String compareGroupName, String templateValue, String compareTemplateValue, Pageable pageable);

    @Query(value = "select  t from MxGroupNavigationRight t where exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupName and p.template=:compareTemplateValue" +
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null )) " +
            "and (p.rights=t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null))" +
            " and (p.path=t.path or(p.path is null and t.path is null)))and t.groupLabel=:groupName and t.template=:templateValue and  " +
            "t.reportDate=:repDate   order by t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findNavigationByGroupLabelsAndReportDate(String groupName, String compareGroupName, String templateValue, LocalDate repDate, Pageable pageable, String compareTemplateValue);

    @Query(value="select  t from MxGroupNavigationRight t where  " +
            "t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null )) and (p.rights =t.rights or (p.rights is null and t.rights is null ))" +
            " and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) and (t.pathLabel = p.pathLabel or (p.pathLabel is null and t.pathLabel is null)) and" +
            " (t.pathRest=p.pathRest or (p.pathRest is null and t.pathRest is null)) and (t.submenu1=p.submenu1 or (p.submenu1 is null and t.submenu1 is null)) and (t.submenu2=p.submenu2 or (p.submenu2 is null and t.submenu2 is null)) and " +
            "(t.submenu3=p.submenu3 or (p.submenu3 is null and t.submenu3 is null)) and (t.submenu4=p.submenu4 or (p.submenu4 is null and t.submenu4 is null)) and (t.submenu5=p.submenu5 or " +
            "(p.submenu5 is null and t.submenu5 is null)))  order by t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findAllGroupCompareNavMatchedList(LocalDate repDate, String template, String compareTemplate,String groupLabel,String compareGroupLabel, Pageable pageable);

    @Query(value="select  t from MxGroupNavigationRight t where  " +
            "t.groupLabel=:groupLabel and t.template=:template and  t.reportDate=:repDate" +
            " and exists  (select p.id from MxGroupNavigationRight p where p.groupLabel=:compareGroupLabel and p.template=:compareTemplate"+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null )) and (p.rights =t.rights or (p.rights is null and t.rights is null ))" +
            " and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) and ((coalesce(t.pathLabel,'') <> coalesce(p.pathLabel,'')) or (coalesce(t.pathRest,'')<>coalesce(p.pathRest,''))  or " +
            " (coalesce(t.submenu1,'')<>coalesce(p.submenu1,'')) or (coalesce(t.submenu2,'')<>coalesce(p.submenu2,'')) or (coalesce(t.submenu3,'')<>coalesce(p.submenu3,'')) or (coalesce(t.submenu4,'')<>coalesce(p.submenu4,'')) " +
            " or (coalesce(t.submenu5,'')<>coalesce(p.submenu5,''))))  order by t.comments,t.rights,t.menu,t.path")
    Page<MxGroupNavigationRight> findAllGroupCompareNavUnMatched(LocalDate repDate, String template, String compareTemplate,String groupLabel,String compareGroupLabel, Pageable pageable);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where  exists  " +
            "(select p.id from MxGroupNavigationRight p where p.groupLabel=:#{#filter.compareGroupLabel}  and p.template=:#{#filter.compareTemplate} "+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null) )" +
            "and (p.rights =t.rights or (p.rights is null and t.rights is null )) and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null))) " +
            "and t.groupLabel=:#{#filter.groupLabel}  and t.template=:#{#filter.template} and t.reportDate=:repDate ")
    List<NavigationRights> findGeneralPropertyListForNavAll(GroupCompareFilter filter, LocalDate repDate);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where  exists  " +
            "(select p.id from MxGroupNavigationRight p where p.groupLabel=:#{#filter.compareGroupLabel} and p.template=:#{#filter.compareTemplate} "+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null )) and (p.rights =t.rights or (p.rights is null and t.rights is null ))" +
            " and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) and ((coalesce(t.pathLabel,'') <> coalesce(p.pathLabel,'')) or (coalesce(t.pathRest,'')<>coalesce(p.pathRest,''))  or " +
            " (coalesce(t.submenu1,'')<>coalesce(p.submenu1,'')) or (coalesce(t.submenu2,'')<>coalesce(p.submenu2,'')) or (coalesce(t.submenu3,'')<>coalesce(p.submenu3,'')) or (coalesce(t.submenu4,'')<>coalesce(p.submenu4,'')) " +
            " or (coalesce(t.submenu5,'')<>coalesce(p.submenu5,'')))) and t.groupLabel=:#{#filter.groupLabel}  and t.template=:#{#filter.template} and t.reportDate=:repDate ")
    List<NavigationRights> findGeneralPropertyListForNavUnMatched(GroupCompareFilter filter, LocalDate repDate);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.NavigationRights(t.comments,t.rights,t.menu,t.path) from MxGroupNavigationRight t where  exists  " +
            "(select p.id from MxGroupNavigationRight p where p.groupLabel=:#{#filter.compareGroupLabel}  and p.template=:#{#filter.compareTemplate} "+
            " and p.reportDate=:repDate and (p.comments =t.comments or (p.comments is null and t.comments is null )) and (p.rights =t.rights or (p.rights is null and t.rights is null ))" +
            " and (p.menu =t.menu or (p.menu is null and t.menu is null)) and (p.path=t.path or (p.path is null and t.path is null)) and (t.pathLabel = p.pathLabel or (p.pathLabel is null and t.pathLabel is null)) and" +
            " (t.pathRest=p.pathRest or (p.pathRest is null and t.pathRest is null)) and (t.submenu1=p.submenu1 or (p.submenu1 is null and t.submenu1 is null)) and (t.submenu2<>p.submenu2 or (p.submenu2 is null and t.submenu2 is null)) and " +
            "(t.submenu3=p.submenu3 or (p.submenu3 is null and t.submenu3 is null)) and (t.submenu4=p.submenu4 or (p.submenu4 is null and t.submenu4 is null)) and (t.submenu5<>p.submenu5 or " +
            "(p.submenu5 is null and t.submenu5 is null))) and t.groupLabel=:#{#filter.groupLabel}  and t.template=:#{#filter.template} and t.reportDate=:repDate ")
    List<NavigationRights> findGeneralPropertyListForNavMatched(GroupCompareFilter filter, LocalDate repDate);

    @Query(value = "select t from MxGroupNavigationRight t where t.reportDate = :requestDate and (upper(t.rights) like (concat('%', upper(:searchString), '%')) or upper(t.menu) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.path) like (concat('%', upper(:searchString), '%')) or upper(t.pathLabel) like (concat('%', upper(:searchString), '%')) or upper(t.pathRest) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.submenu1) like (concat('%', upper(:searchString), '%')) or upper(t.submenu2) like (concat('%', upper(:searchString), '%')) or upper(t.submenu3) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.submenu4) like (concat('%', upper(:searchString), '%')) or upper(t.submenu5) like (concat('%', upper(:searchString), '%')) or upper(t.template) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.comments) like (concat('%', upper(:searchString), '%')) or upper(t.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<MxGroupNavigationRight> findNavigationRightsGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxGroupNavigationRight t where t.reportDate = :requestDate and t.template in :templateValue and t.groupLabel in :groupValue and (upper(t.rights) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.menu) like (concat('%', upper(:searchString), '%')) or upper(t.path) like (concat('%', upper(:searchString), '%')) or upper(t.pathLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.pathRest) like (concat('%', upper(:searchString), '%')) or upper(t.submenu1) like (concat('%', upper(:searchString), '%')) or upper(t.submenu2) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.submenu3) like (concat('%', upper(:searchString), '%')) or upper(t.submenu4) like (concat('%', upper(:searchString), '%')) or upper(t.submenu5) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.comments) like (concat('%', upper(:searchString), '%')) or upper(t.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<MxGroupNavigationRight> findNavigationRightsGlobalCombined(List<String> templateValue, List<String> groupValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select distinct t from MxGroupNavigationRight t where t.reportDate=:requestDate and upper(t.template)=:templateValue and upper(t.groupLabel)=:group and (upper(t.comments) like (concat('%', upper(:searchString), '%')) or upper(t.menu) like (concat('%', upper(:searchString), '%')) or upper(t.path) like (concat('%', upper(:searchString), '%')) or " +
            " upper(t.pathLabel) like (concat('%', upper(:searchString), '%')) or upper(t.pathRest) like (concat('%', upper(:searchString), '%')) or upper(t.rights) like (concat('%', upper(:searchString), '%')) or upper(t.submenu1) like (concat('%', upper(:searchString), '%')) or upper(t.submenu2) like (concat('%', upper(:searchString), '%')) or upper(t.submenu3) like (concat('%', upper(:searchString), '%')) or upper(t.submenu4) like (concat('%', upper(:searchString), '%')) or upper(t.submenu5) like (concat('%', upper(:searchString), '%')))")
    Page<MxGroupNavigationRight> findByGroupLabelAndReportDateListWithSearch(String templateValue,String group, LocalDate requestDate,String searchString, Pageable pageable);

    @Query(value = "select t from MxGroupNavigationRight t where " +
            "(coalesce(:#{#rights}) is null or t.rights in :rights) and " +
            "(coalesce(:#{#menu}) is null or t.menu in :menu ) and " +
            "(coalesce(:#{#path}) is null or t.path in :path ) and " +
            "(coalesce(:#{#pathLabel}) is null or t.pathLabel in :pathLabel ) and " +
            "(coalesce(:#{#pathRest}) is null or t.pathRest in :pathRest ) and " +
            "(coalesce(:#{#submenu1}) is null or t.submenu1 in :submenu1 ) and " +
            "(coalesce(:#{#submenu2}) is null or t.submenu2 in :submenu2 ) and " +
            "(coalesce(:#{#submenu3}) is null or t.submenu3 in :submenu3 ) and " +
            "(coalesce(:#{#submenu4}) is null or t.submenu4 in :submenu4 ) and " +
            "(coalesce(:#{#submenu5}) is null or t.submenu5 in :submenu5 ) and " +
            "(coalesce(:#{#comments}) is null or t.comments in :comments ) and " +
            "t.groupLabel in :groupLabel and t.template in :template and t.reportDate = :reportDate")
    Page<MxGroupNavigationRight> findFilteredNavigationRights(LocalDate reportDate, List<String> groupLabel, List<String> template, List<String> rights, List<String> menu, List<String> path,
                                                              List<String> pathLabel, List<String> pathRest, List<String> submenu1, List<String> submenu2, List<String> submenu3, List<String> submenu4,
                                                              List<String> submenu5, List<String> comments, Pageable pageable);
}
