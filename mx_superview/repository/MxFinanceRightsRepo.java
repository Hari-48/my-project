package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxFinaceAcctrlRights;
import com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate;
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
public interface MxFinanceRightsRepo extends CrudRepository<MxFinaceAcctrlRights, Long> {

    @Query(value = "select distinct t.template from MxFinaceAcctrlRights t where t.sysDate = :date and t.tmplType='STAT_CATEG_TEMP'")
    List<String> findDistinctTempatesByStatTmpl(LocalDate date);

    @Query(value = "select distinct t.template from MxFinaceAcctrlRights t where t.sysDate = :date and t.tmplType='ACC_CTRL_TEMP'")
    List<String> findDistinctTemplatesByCtrlTemp(LocalDate date);

    Page<MxFinaceAcctrlRights> findAllByTemplateAndSysDate(String template, LocalDate sysDate, Pageable pageable);

    MxFinaceAcctrlRights findTopByTemplateAndReportDate(String template, LocalDate requestDate);

    List<MxFinaceAcctrlRights> findTopByTemplateInAndReportDate(List<String> templateValue, LocalDate requestDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id,t.tmplType,t.template,t.description,t.filter,t.filDesc,g.groupLabel) from MxFinaceAcctrlRights t " +
            "inner join MxGroupsListItem g on t.template=g.accCtrl where t.reportDate=:repDate and g.reportDate=:repDate and" +
            " t.tmplType =:subTemplate")
    Page<CombinedFinanceRightsTemplate> findAllByReportDateAndTmplTypeAccCtrl(LocalDate repDate, String subTemplate, Pageable page);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id,t.tmplType,t.template,t.description,t.filter,t.filDesc,g.groupLabel) from MxFinaceAcctrlRights t " +
            "inner join MxGroupsListItem g on t.template=g.statTmpl where t.reportDate=:repDate and g.reportDate=:repDate" +
            " and t.tmplType=:subTemplate")
    Page<CombinedFinanceRightsTemplate> findAllByReportDateAndTmplTypeStatTmpl(LocalDate repDate,String subTemplate,Pageable page);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id,t.tmplType,t.template,t.description,t.filter,t.filDesc,g.groupLabel) from MxFinaceAcctrlRights t " +
            "inner join MxGroupsListItem g on t.template=g.accCtrl where t.reportDate=:repDate and g.reportDate=:repDate and t.template in :template" +
            " and g.accCtrl in :template and t.tmplType=:subTemplate and g.groupLabel in :groupLabel")
    Page<CombinedFinanceRightsTemplate> findByGoupLabelAndReportDateAndAccCtrl(List<String> template,List<String> groupLabel,String subTemplate,LocalDate repDate,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id,t.tmplType,t.template,t.description,t.filter,t.filDesc,g.groupLabel) from MxFinaceAcctrlRights t " +
            "inner join MxGroupsListItem g on t.template=g.statTmpl where t.reportDate=:repDate and g.reportDate=:repDate and t.template in :template" +
            " and g.statTmpl in :template and t.tmplType=:subTemplate and g.groupLabel in :groupLabel")
    Page<CombinedFinanceRightsTemplate> findByGoupLabelAndReportDateAndStatTmpl(List<String> template,List<String> groupLabel,String subTemplate,LocalDate repDate,Pageable pageable);

    @Query(value = "select distinct t from MxFinaceAcctrlRights t where  t.template = :template and t.tmplType=:subTemplate and t.reportDate = :baseReportDate  order by t.template,t.tmplType ")
    List<MxFinaceAcctrlRights> findByMxFinanceRightsByGroupLabelAndReportDate(String template, String subTemplate, LocalDate baseReportDate);

    //    @Query(value = "select  t from MxFinaceAcctrlRights t where t.reportDate=:requestDate and t.template=:templateValue and t.tmplType=:subTemplateValue order by t.template,t.tmplType ")
    Page<MxFinaceAcctrlRights> findAllByTemplateAndTmplTypeAndReportDateOrderByDescriptionAscFilterAsc(String templateValue, String subTemplateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id, t.tmplType, t.template, t.description, t.filter, t.filDesc, g.groupLabel) from MxFinaceAcctrlRights t inner join MxGroupsListItem g " +
            "on t.template = g.accCtrl where t.reportDate = :requestDate and g.reportDate = :requestDate and t.tmplType = :subTemplateValue and " +
            "(upper(t.description) like (concat('%', upper(:searchString), '%')) or upper(t.filter) like (concat('%', upper(:searchString), '%')) or upper(t.filDesc) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.tmplType) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedFinanceRightsTemplate> findFinanceRightsTmplTypeAccCtrlGlobalCombinedAllGroups(LocalDate requestDate, String subTemplateValue, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id, t.tmplType, t.template, t.description, t.filter, t.filDesc, g.groupLabel) from MxFinaceAcctrlRights t inner join MxGroupsListItem g " +
            "on t.template = g.accCtrl where t.reportDate = :requestDate and g.reportDate = :requestDate and t.template in :templateValue and g.accCtrl in :templateValue and t.tmplType = :subTemplateValue and g.groupLabel in :groupValue and " +
            "(upper(t.description) like (concat('%', upper(:searchString), '%')) or upper(t.filter) like (concat('%', upper(:searchString), '%')) or upper(t.filDesc) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.tmplType) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedFinanceRightsTemplate> findFinanceRightsTmplTypeAccCtrlGlobalCombined(List<String> templateValue, List<String> groupValue, String subTemplateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id, t.tmplType, t.template, t.description, t.filter, t.filDesc, g.groupLabel) from MxFinaceAcctrlRights t inner join MxGroupsListItem g " +
            "on t.template = g.statTmpl where t.reportDate = :requestDate and g.reportDate = :requestDate and t.tmplType = :subTemplateValue and " +
            "(upper(t.description) like (concat('%', upper(:searchString), '%')) or upper(t.filter) like (concat('%', upper(:searchString), '%')) or upper(t.filDesc) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.tmplType) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedFinanceRightsTemplate> findFinanceRightsTmplTypeStatTmplGlobalCombinedAllGroups(LocalDate requestDate, String subTemplateValue, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombinedFinanceRightsTemplate(t.id, t.tmplType, t.template, t.description, t.filter, t.filDesc, g.groupLabel) from MxFinaceAcctrlRights t inner join MxGroupsListItem g " +
            "on t.template = g.statTmpl where t.reportDate = :requestDate and g.reportDate = :requestDate and t.template in :templateValue and g.statTmpl in :templateValue and t.tmplType = :subTemplateValue and g.groupLabel in :groupValue and " +
            "(upper(t.description) like (concat('%', upper(:searchString), '%')) or upper(t.filter) like (concat('%', upper(:searchString), '%')) or upper(t.filDesc) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.template) like (concat('%', upper(:searchString), '%')) or upper(t.tmplType) like (concat('%', upper(:searchString), '%')) or upper(g.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombinedFinanceRightsTemplate> findFinanceRightsTmplTypeStatTmplGlobalCombined(List<String> templateValue, List<String> groupValue, String subTemplateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select  t from MxFinaceAcctrlRights t where t.reportDate=:requestDate and t.template=:templateValue and t.tmplType=:subTemplateValue and (upper(t.filDesc) like concat('%',upper(:searchString),'%') or upper(t.description) like concat('%',upper(:searchString),'%')" +
            " or upper(t.filter) like concat('%',upper(:searchString),'%') or upper(t.tmplType) like concat('%', upper(:searchString), '%'))")
    Page<MxFinaceAcctrlRights> findByTemplateAndReportDateListWithGlobalSearch(String templateValue, String subTemplateValue, LocalDate requestDate, String searchString,Pageable pageable);

    @Query(value = "select  t from MxFinaceAcctrlRights t where t.reportDate=:requestDate and t.template=:templateValue and t.tmplType=:subTemplateValue and (upper(t.filDesc) like concat('%',upper(:searchString),'%') or upper(t.description) like concat('%',upper(:searchString),'%')" +
            " or upper(t.filter) like concat('%',upper(:searchString),'%') or upper(t.tmplType) like concat('%', upper(:searchString), '%'))")
    Page<MxFinaceAcctrlRights> findAllBySearchStringAndTemplate(String templateValue, String subTemplateValue, LocalDate requestDate, String searchString,Pageable pageable);
}
