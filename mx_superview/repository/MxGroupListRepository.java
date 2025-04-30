package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupsListItem;
import com.finsurge.tmr_portal.mx_superview.models.GroupDetails;
import com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList;
import com.finsurge.tmr_portal.mx_superview.models.GroupList;
import com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

@Repository
@Transactional
public interface MxGroupListRepository extends CrudRepository<MxGroupsListItem, Long> {

    @Query(value = "select t from MxGroupsListItem t where t.sysDate =:todayDate")
    List<MxGroupsListItem> getTodaysGroupListData(LocalDate todayDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.sysDate = :date group by t.groupLabel having count(t) > 1")
    HashSet<String> findAllDuplicatedGroupLabelsByDate(LocalDate date);

    Page<MxGroupsListItem> findAllBySysDate(LocalDate date, Pageable pageable);

    List<MxGroupsListItem> findAllByGroupLabelAndSysDate(String groupLabel, LocalDate date);

    @Query(value = "SELECT g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.GRP_DESC, g.STP_RGT_TMPL, g.CONSITENCY_TMPL, g.NAVIGATION_TMPL, g.CHINESE_WALL, g.OSP_RIGHT_TEMPLATE, g.FOD, g.HELP_MONIT, g.RISK_LIMCHECK, " +
            "g.REP_TMPL, g.DST_PROF, g.RFQ_ROLE, g.VIEW_EDIT, g.LAYOUT_EDIT, g.SFV_ADMIN, g.RQWHERE_E, g.SQLRGT_TPL, g.EDIT, g.TRDSQL_QRY, g.QUERY_FILTER, g.MREPORT_OPD, g.DATE_MODE, g.IRS_LABEL, g.LD_LABEL, g.CD_LABEL, " +
            "g.RTGA_LABEL, g.ACC_MODE, g.LPOS_RIGHT, g.NKEY_TMPL, g.STAT_TMPL, g.ACC_CTRL, g.CREATE_DATE, g.MOD_DATETIME, COUNT(t.USER_NAME) " +
            "FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g on t.GROUP_LABEL = g.GROUP_LABEL and t.REP_DATE = g.REP_DATE " +
            "WHERE g.ID = :id AND g.REP_DATE = :date GROUP BY g.ACC_CTRL, g.ACC_MODE, g.CD_LABEL, g.CHINESE_WALL, g.CONSITENCY_TMPL, g.TRDSQL_QRY, g.VIEW_EDIT, " +
            "g.CREATE_DATE, g.DATE_MODE, g.DST_PROF, g.EDIT, g.FOD, g.GROUP_LABEL, g.GRP_DESC, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.HELP_MONIT, g.IRS_LABEL, g.LAYOUT_EDIT, " +
            "g.LD_LABEL, g.LPOS_RIGHT, g.MREPORT_OPD, g.MOD_DATETIME, g.NAVIGATION_TMPL, g.NKEY_TMPL, g.OSP_RIGHT_TEMPLATE, g.QUERY_FILTER, g.REP_TMPL, g.RFQ_ROLE, " +
            "g.RISK_LIMCHECK, g.RQWHERE_E, g.RTGA_LABEL, g.SFV_ADMIN, g.SQLRGT_TPL, g.STAT_TMPL, g.STP_RGT_TMPL", nativeQuery = true)
    Object[] findByIdAndReportDate(Long id, String date);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.ospRightTemplate=:templateName")
    List<String> findAllGroupNameByOspTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.navigationTmpl=:templateName")
    List<String> findAllGroupNameByNavTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.chineseWall=:templateName")
    List<String> findAllGroupNameByCounterTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.consitencyTmpl=:templateName")
    List<String> findAllGroupNameByConsistencyTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.nkeyTmpl=:templateName")
    List<String> findAllGroupNameByNkeyTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.lposRight=:templateName")
    List<String> findAllGroupNameByLposTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.statTmpl=:templateName")
    List<String> findAllGroupNameByStatTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.accCtrl =:templateName")
    List<String> findAllGroupNameByAccCtrlTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.dstProf =:templateName")
    List<String> findAllGroupNameByDistributionTemplateName(String templateName, LocalDate requestDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate = :requestDate and t.stpRgtTmpl =:templateName")
    List<String> findAllGroupNameByStpRightTemplateName(String templateName, LocalDate requestDate);

//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupList(t.id,t.groupLabel,t.grpRoleStr)  from MxGroupsListItem t where t.reportDate = :requestDate and t.groupLabel is not null group by t.id,t.groupLabel,t.grpRoleStr order by t.groupLabel asc ")
//    List<GroupList> findAllUniqueGroupList(LocalDate requestDate);

    @Query(value = "SELECT g.ID, g.GROUP_LABEL, g.GRP_ROLE_STR, count(t.USER_NAME) FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g on t.GROUP_LABEL = g.GROUP_LABEL and t.REP_DATE = g.REP_DATE " +
            "WHERE g.GROUP_LABEL IN (SELECT distinct GROUP_LABEL FROM UAM_MX_GROUP_LIST n WHERE n.REP_DATE = :requestDate) and g.REP_DATE = :requestDate " +
            "GROUP BY g.ID, g.GROUP_LABEL, g.GRP_ROLE_STR order by g.GROUP_LABEL asc", nativeQuery = true)
    List<Object[]> findAllUniqueGroupList(String requestDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupList(t.id,t.groupLabel,t.grpRoleStr)  from MxGroupsListItem t where t.reportDate = :repDate and t.groupLabel =:groupLabel ")
    List<GroupList> findTopByGroupLabelAndReportDate(String groupLabel, LocalDate repDate);

    @Query(value = "select t from MxGroupsListItem  t where t.reportDate = :repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    Page<MxGroupsListItem> findByReportDate(LocalDate repDate, String groupLabel, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetails(t.groupLabel,t.grpRoleStr, t.grpTypeStr,t.grpDesc,t.stpRgtTmpl,t.consitencyTmpl, t.navigationTmpl, t.chineseWall, t.ospRightTemplate, t.fod, t.helpMonit, t.riskLimCheck, t.repTmpl, t.dstProf, t.rfqRole, t.viewEdit, t.layoutEdit, t.sfvAdmin, t.rqWhereE, t.sqlRgtTpl,t.edit, t.trdSqlQry," +
            "t.queryFilter,t.mReportOpd,t.dateMode,t.irsLabel, t.ldLabel, t.cdLabel, t.rtgaLabel, t.accMode, t.lposRight, t.nkeyTmpl,t.statTmpl, t.accCtrl, t.createDate, t.modDateTime) from " +
            "MxGroupsListItem t where t.groupLabel in :groupLabels and t.reportDate = :requestDate order by t.groupLabel")
    List<GroupDetails> findByGroupLabel(List<String> groupLabels, LocalDate requestDate);

    @Query(value = "SELECT g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.GRP_DESC, g.STP_RGT_TMPL, g.CONSITENCY_TMPL, g.NAVIGATION_TMPL, g.CHINESE_WALL, g.OSP_RIGHT_TEMPLATE, g.FOD, g.HELP_MONIT, g.RISK_LIMCHECK, " +
            "g.REP_TMPL, g.DST_PROF, g.RFQ_ROLE, g.VIEW_EDIT, g.LAYOUT_EDIT, g.SFV_ADMIN, g.RQWHERE_E, g.SQLRGT_TPL, g.EDIT, g.TRDSQL_QRY, g.QUERY_FILTER, g.MREPORT_OPD, g.DATE_MODE, g.IRS_LABEL, g.LD_LABEL, g.CD_LABEL, " +
            "g.RTGA_LABEL, g.ACC_MODE, g.LPOS_RIGHT, g.NKEY_TMPL, g.STAT_TMPL, g.ACC_CTRL, g.CREATE_DATE, g.MOD_DATETIME, COUNT(t.USER_NAME) " +
            "FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g on t.GROUP_LABEL = g.GROUP_LABEL and t.REP_DATE = g.REP_DATE " +
            "WHERE g.REP_DATE = :requestDate GROUP BY g.ACC_CTRL, g.ACC_MODE, g.CD_LABEL, g.CHINESE_WALL, g.CONSITENCY_TMPL, g.TRDSQL_QRY, g.VIEW_EDIT, " +
            "g.CREATE_DATE, g.DATE_MODE, g.DST_PROF, g.EDIT, g.FOD, g.GROUP_LABEL, g.GRP_DESC, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.HELP_MONIT, g.IRS_LABEL, g.LAYOUT_EDIT, " +
            "g.LD_LABEL, g.LPOS_RIGHT, g.MREPORT_OPD, g.MOD_DATETIME, g.NAVIGATION_TMPL, g.NKEY_TMPL, g.OSP_RIGHT_TEMPLATE, g.QUERY_FILTER, g.REP_TMPL, g.RFQ_ROLE, " +
            "g.RISK_LIMCHECK, g.RQWHERE_E, g.RTGA_LABEL, g.SFV_ADMIN, g.SQLRGT_TPL, g.STAT_TMPL, g.STP_RGT_TMPL ORDER BY g.GROUP_LABEL", nativeQuery = true)
    List<Object[]> findDistinctByReportDate(String requestDate);


    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetails(t.groupLabel,t.grpRoleStr, t.grpTypeStr,t.grpDesc,t.stpRgtTmpl,t.consitencyTmpl, t.navigationTmpl, t.chineseWall, t.ospRightTemplate, t.fod, t.helpMonit, t.riskLimCheck, t.repTmpl, t.dstProf, t.rfqRole, t.viewEdit, t.layoutEdit, t.sfvAdmin, t.rqWhereE, t.sqlRgtTpl,t.edit, t.trdSqlQry," +
            "t.queryFilter,t.mReportOpd,t.dateMode,t.irsLabel, t.ldLabel, t.cdLabel, t.rtgaLabel, t.accMode, t.lposRight, t.nkeyTmpl,t.statTmpl, t.accCtrl, t.createDate, t.modDateTime) from " +
            "MxGroupsListItem t where t.reportDate=:requestDate and t.groupLabel in :groupLabel and t.grpRoleStr in :groupRole ORDER BY FIELD(t.groupLabel,:groupLabel)")
    List<GroupDetails> findGroupListByRole(LocalDate requestDate, List<String> groupLabel, List<String> groupRole);

//field() function supports only for mysql
//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetails(t.groupLabel,t.grpRoleStr, t.grpTypeStr,t.grpDesc,t.stpRgtTmpl,t.consitencyTmpl, t.navigationTmpl, t.chineseWall, t.ospRightTemplate, t.fod, t.helpMonit, t.riskLimCheck, t.repTmpl, t.dstProf, t.rfqRole, t.viewEdit, t.layoutEdit, t.sfvAdmin, t.rqWhereE, t.sqlRgtTpl,t.edit, t.trdSqlQry," +
//            "t.queryFilter,t.mReportOpd,t.dateMode,t.irsLabel, t.ldLabel, t.cdLabel, t.rtgaLabel, t.accMode, t.lposRight, t.nkeyTmpl,t.statTmpl, t.accCtrl, t.createDate, t.modDateTime) from " +
//            "MxGroupsListItem t where t.reportDate=:requestDate and t.groupLabel in :groupLabel and t.grpRoleStr in :groupRole ORDER BY FIELD(t.groupLabel,:groupLabel)")
//    List<GroupDetails> findGroupListByRole(LocalDate requestDate, List<String> groupLabel, List<String> groupRole);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate=:reportDate and t.consitencyTmpl=:template")
    List<String> findAllConsistencyGroupNameByTemplate(LocalDate reportDate, String template);

    @Query(value = "select distinct t.chineseWall from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueChineseWallTemplates(LocalDate reportDate);

    @Query(value = "select distinct t.consitencyTmpl from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueConsistencyTemplates(LocalDate reportDate);

    @Query(value = "select distinct t.navigationTmpl from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueNavigationTemplates(LocalDate reportDate);

    @Query(value = "select distinct t.ospRightTemplate from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueOspRightsMatrixTemplates(LocalDate reportDate);

    @Query(value = "select distinct t.stpRgtTmpl from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueStpRightsTemplates(LocalDate reportDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueGroupLabels(LocalDate reportDate);

    @Query(value = "select distinct t.accCtrl from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueFinanceTemplateByAccCtrl(LocalDate reportDate);

    @Query(value = "select distinct t.statTmpl from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueFinanceTemplateByStatTmpl(LocalDate reportDate);

    @Query(value = "select distinct t.nkeyTmpl from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueOperationRightsTemplateByNkey(LocalDate reportDate);

    @Query(value = "select distinct t.lposRight from MxGroupsListItem t where t.reportDate=:reportDate")
    List<String> findUniqueOperationRightsTemplateByLpos(LocalDate reportDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.reportDate=:reportDate and t.chineseWall=:template")
    List<String> findGroupsByChineseWallTemplates(LocalDate reportDate, String template);

//    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetails(t.groupLabel,t.grpRoleStr, t.grpTypeStr,t.grpDesc,t.stpRgtTmpl,t.consitencyTmpl, t.navigationTmpl, t.chineseWall, t.ospRightTemplate, t.fod, t.helpMonit, t.riskLimCheck, t.repTmpl, t.dstProf, t.rfqRole, t.viewEdit, t.layoutEdit, t.sfvAdmin, t.rqWhereE, t.sqlRgtTpl,t.edit, t.trdSqlQry," +
//            "t.queryFilter,t.mReportOpd,t.dateMode,t.irsLabel, t.ldLabel, t.cdLabel, t.rtgaLabel, t.accMode, t.lposRight, t.nkeyTmpl,t.statTmpl, t.accCtrl, t.createDate, t.modDateTime) from " +
//            "MxGroupsListItem t where t.reportDate=:reportDate and t.groupLabel=:groupLabel order by t.groupLabel")
//    List<GroupDetails> findByGroupLabelAndReportDate(String groupLabel, LocalDate reportDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetails(t.groupLabel,t.grpRoleStr, t.grpTypeStr,t.grpDesc,t.stpRgtTmpl,t.consitencyTmpl, t.navigationTmpl, t.chineseWall, t.ospRightTemplate, t.fod, t.helpMonit, t.riskLimCheck, t.repTmpl, t.dstProf, t.rfqRole, t.viewEdit, t.layoutEdit, t.sfvAdmin, t.rqWhereE, t.sqlRgtTpl,t.edit, t.trdSqlQry," +
            "t.queryFilter,t.mReportOpd,t.dateMode,t.irsLabel, t.ldLabel, t.cdLabel, t.rtgaLabel, t.accMode, t.lposRight, t.nkeyTmpl,t.statTmpl, t.accCtrl, t.createDate, t.modDateTime) from " +
            "MxGroupsListItem t where t.id in :id and t.reportDate = :requestDate ORDER BY FIELD(t.id,:id) ")
    List<GroupDetails> findByGroupIdsIn(List<Long> id, LocalDate requestDate);
    @Query(value = "SELECT g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.GRP_DESC, g.STP_RGT_TMPL, g.CONSITENCY_TMPL, g.NAVIGATION_TMPL, g.CHINESE_WALL, g.OSP_RIGHT_TEMPLATE, g.FOD, g.HELP_MONIT, g.RISK_LIMCHECK, " +
            "g.REP_TMPL, g.DST_PROF, g.RFQ_ROLE, g.VIEW_EDIT, g.LAYOUT_EDIT, g.SFV_ADMIN, g.RQWHERE_E, g.SQLRGT_TPL, g.EDIT, g.TRDSQL_QRY, g.QUERY_FILTER, g.MREPORT_OPD, g.DATE_MODE, g.IRS_LABEL, g.LD_LABEL, g.CD_LABEL, " +
            "g.RTGA_LABEL, g.ACC_MODE, g.LPOS_RIGHT, g.NKEY_TMPL, g.STAT_TMPL, g.ACC_CTRL, g.CREATE_DATE, g.MOD_DATETIME, COUNT(t.USER_NAME) " +
            "FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g on t.GROUP_LABEL = g.GROUP_LABEL and t.REP_DATE = g.REP_DATE " +
            "WHERE g.GROUP_LABEL = :groupLabel AND g.REP_DATE = :reportDate GROUP BY g.ACC_CTRL, g.ACC_MODE, g.CD_LABEL, g.CHINESE_WALL, g.CONSITENCY_TMPL, g.TRDSQL_QRY, g.VIEW_EDIT, " +
            "g.CREATE_DATE, g.DATE_MODE, g.DST_PROF, g.EDIT, g.FOD, g.GROUP_LABEL, g.GRP_DESC, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.HELP_MONIT, g.IRS_LABEL, g.LAYOUT_EDIT, " +
            "g.LD_LABEL, g.LPOS_RIGHT, g.MREPORT_OPD, g.MOD_DATETIME, g.NAVIGATION_TMPL, g.NKEY_TMPL, g.OSP_RIGHT_TEMPLATE, g.QUERY_FILTER, g.REP_TMPL, g.RFQ_ROLE, " +
            "g.RISK_LIMCHECK, g.RQWHERE_E, g.RTGA_LABEL, g.SFV_ADMIN, g.SQLRGT_TPL, g.STAT_TMPL, g.STP_RGT_TMPL ORDER BY g.GROUP_LABEL", nativeQuery = true)
    List<Object[]> findByGroupLabelAndReportDate(String groupLabel, String reportDate);

    @Query(value = "SELECT g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.GRP_DESC, g.STP_RGT_TMPL, g.CONSITENCY_TMPL, g.NAVIGATION_TMPL, g.CHINESE_WALL, g.OSP_RIGHT_TEMPLATE, g.FOD, g.HELP_MONIT, g.RISK_LIMCHECK, " +
            "g.REP_TMPL, g.DST_PROF, g.RFQ_ROLE, g.VIEW_EDIT, g.LAYOUT_EDIT, g.SFV_ADMIN, g.RQWHERE_E, g.SQLRGT_TPL, g.EDIT, g.TRDSQL_QRY, g.QUERY_FILTER, g.MREPORT_OPD, g.DATE_MODE, g.IRS_LABEL, g.LD_LABEL, g.CD_LABEL, " +
            "g.RTGA_LABEL, g.ACC_MODE, g.LPOS_RIGHT, g.NKEY_TMPL, g.STAT_TMPL, g.ACC_CTRL, g.CREATE_DATE, g.MOD_DATETIME, COUNT(t.USER_NAME) " +
            "FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g on t.GROUP_LABEL = g.GROUP_LABEL and t.REP_DATE = g.REP_DATE " +
            "WHERE g.ID IN :id AND g.REP_DATE = :requestDate GROUP BY g.ACC_CTRL, g.ACC_MODE, g.CD_LABEL, g.CHINESE_WALL, g.CONSITENCY_TMPL, g.TRDSQL_QRY, g.VIEW_EDIT, " +
            "g.CREATE_DATE, g.DATE_MODE, g.DST_PROF, g.EDIT, g.FOD, g.GROUP_LABEL, g.GRP_DESC, g.GRP_ROLE_STR, g.GRP_TYPE_STR, g.HELP_MONIT, g.IRS_LABEL, g.LAYOUT_EDIT, " +
            "g.LD_LABEL, g.LPOS_RIGHT, g.MREPORT_OPD, g.MOD_DATETIME, g.NAVIGATION_TMPL, g.NKEY_TMPL, g.OSP_RIGHT_TEMPLATE, g.QUERY_FILTER, g.REP_TMPL, g.RFQ_ROLE, " +
            "g.RISK_LIMCHECK, g.RQWHERE_E, g.RTGA_LABEL, g.SFV_ADMIN, g.SQLRGT_TPL, g.STAT_TMPL, g.STP_RGT_TMPL ORDER BY g.GROUP_LABEL", nativeQuery = true)
    List<Object[]> findByGroupIdsIn(List<Long> id, String requestDate);

    @Query(value = "select t from MxGroupsListItem  t where t.reportDate = :requestDate and t.id in :groupList")
    Page<MxGroupsListItem> findByReportDateGroupList(LocalDate requestDate, List<Long> groupList, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.chineseWall) from MxGroupsListItem t where t.chineseWall = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getChineseWallTemplateDetails(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.navigationTmpl) from MxGroupsListItem t where t.navigationTmpl = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getNavigationTemplate(String templateValue, LocalDate repDate, Sort sort,String groupLabel);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.accCtrl) from MxGroupsListItem t where t.accCtrl = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getFinanceAcctrlTemplateDetails(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.statTmpl) from MxGroupsListItem t " +
            "where t.statTmpl = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getFinanceStatCategTemplateDetails(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value="select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.consitencyTmpl) from MxGroupsListItem t where t.consitencyTmpl=:templateValue and t.reportDate=:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getConsistencyTemplateDetails(String templateValue, LocalDate repDate, Sort sort, String groupLabel);


    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id, t.groupLabel, t.grpRoleStr, t.grpDesc, t.ospRightTemplate) from MxGroupsListItem t where t.ospRightTemplate = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel, '%')))")
    List<GroupDetailsList> getOspRightsTemplate(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id, t.groupLabel, t.grpRoleStr, t.grpDesc, t.stpRgtTmpl) from MxGroupsListItem t where t.stpRgtTmpl = :templateValue and t.reportDate = :repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel, '%')))")
    List<GroupDetailsList> getStpRightsTemplate(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.CHINESE_WALL,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabel and g.REP_DATE = :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.CHINESE_WALL order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getChineseWallTemplateDetailsNative(List<?> groupLabel, String repDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.chineseWall=:templateValue and t.reportDate=:repDate")
    List<String> findGroupLabelByChinesewallTemplate(String templateValue, LocalDate repDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.ospRightTemplate = :templateValue and t.reportDate = :repDate")
    List<String> findGroupLabelByOspRightsTemplate(String templateValue, LocalDate repDate);

    @Query(value = "SELECT g.ID, g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_DESC, g.OSP_RIGHT_TEMPLATE, COUNT(t.USER_NAME) FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g ON t.GROUP_LABEL = g.GROUP_LABEL AND t.REP_DATE = g.REP_DATE " +
            "WHERE g.GROUP_LABEL IN :groupLabels AND g.REP_DATE = :repDate GROUP BY g.ID, g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_DESC, g.OSP_RIGHT_TEMPLATE ORDER BY g.GROUP_LABEL ASC", nativeQuery = true)
    List<Object[]> getOspRightsTemplateDetailsNative(List<?> groupLabels, String repDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.stpRgtTmpl = :templateValue and t.reportDate = :repDate")
    List<String> findGroupLabelByStpRightsTemplate(String templateValue, LocalDate repDate);

    @Query(value = "SELECT g.ID, g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_DESC, g.STP_RGT_TMPL, COUNT(t.USER_NAME) FROM UAM_USER_GROUP_ACCESS_RIGHT t RIGHT JOIN UAM_MX_GROUP_LIST g ON t.GROUP_LABEL = g.GROUP_LABEL AND t.REP_DATE = g.REP_DATE " +
            "WHERE g.GROUP_LABEL IN :groupLabels AND g.REP_DATE = :repDate GROUP BY g.ID, g.GROUP_LABEL, g.GRP_ROLE_STR, g.GRP_DESC, g.STP_RGT_TMPL ORDER BY g.GROUP_LABEL ASC", nativeQuery = true)
    List<Object[]> getStpRightsTemplateDetailsNative(List<?> groupLabels, String repDate);

    @Query(value = "select distinct t.groupLabel from MxGroupsListItem t where t.consitencyTmpl=:templateValue and t.reportDate=:repDate")
    List<String> findGroupLabelByConsistencyTemplate(String templateValue, LocalDate repDate);


    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.CONSITENCY_TMPL,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabel and g.REP_DATE= :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.CONSITENCY_TMPL order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getConsistencyTemplateDetailsNative(List<?> groupLabel, String repDate);

    @Query(value="select distinct t.groupLabel from MxGroupsListItem t where t.accCtrl=:templateValue and t.reportDate=:repDate")
    List<String> findGroupLabelByFinanaceRightAcctrl(String templateValue, LocalDate repDate);

    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.ACC_CTRL,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabels and g.REP_DATE= :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.ACC_CTRL order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getFinanaceRightDetailsNativeAcctrl(List<?> groupLabels, String repDate);

    @Query(value="select distinct t.groupLabel from MxGroupsListItem t where t.statTmpl=:templateValue and t.reportDate=:repDate")
    List<String> findGroupLabelByFinanaceRightStatCateg(String templateValue, LocalDate repDate);

    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.STAT_TMPL,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabels and g.REP_DATE= :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.STAT_TMPL order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getFinanceRightDetailsNativeStatCateg(List<?> groupLabels, String repDate);

    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.NAVIGATION_TMPL,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabels and g.REP_DATE= :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.NAVIGATION_TMPL order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getNavigationRightsDetailsNative(List<?> groupLabels, String repDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.nkeyTmpl) from MxGroupsListItem t where t.nkeyTmpl = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getOperationNkeyTemplateDetails(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.lposRight) from MxGroupsListItem t where t.lposRight = :templateValue and t.reportDate =:repDate and (:groupLabel is null or upper(t.groupLabel) like upper(concat('%', :groupLabel,'%')))")
    List<GroupDetailsList> getOperationLposTemplateDetails(String templateValue, LocalDate repDate, Sort sort, String groupLabel);

    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.NKEY_TMPL,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabels and g.REP_DATE= :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.NKEY_TMPL order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getOperationNkeyDetailsNative(List<?> groupLabels, String repDate);

    @Query(value ="select g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.LPOS_RIGHT,count(t.USER_NAME) from UAM_USER_GROUP_ACCESS_RIGHT t right join UAM_MX_GROUP_LIST g on t.GROUP_LABEL=g.GROUP_LABEL and t.REP_DATE=g.REP_DATE  " +
            " where g.GROUP_LABEL IN :groupLabels and g.REP_DATE= :repDate GROUP BY g.ID,g.GROUP_LABEL,g.GRP_ROLE_STR,g.GRP_DESC,g.LPOS_RIGHT order by g.GROUP_LABEL asc",nativeQuery = true)
    List<Object[]> getOperationLposDetailsNative(List<?> groupLabels, String repDate);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl) from MxGroupsListItem t where t.stpRgtTmpl in (select globalTemplate from MxStpRightMatrixEod where reportDate=:repDate and rightProfile=:rightsProfile) and t.reportDate=:repDate")
    Page<GroupDetailsList> getGroupDetailsFromRightsProfile(String rightsProfile,LocalDate repDate,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupDetailsList(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl) from MxGroupsListItem t where t.stpRgtTmpl in (select globalTemplate from MxStpRightMatrixEod where reportDate=:repDate and rightProfile=:rightsProfile) and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%'))" +
            " or upper(t.grpRoleStr) like (concat('%', upper(:searchString), '%')))")
    Page<GroupDetailsList> getGroupDetailsFromRightsProfileWithGlobalSearch(String rightsProfile,LocalDate repDate,String searchString,Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.ospRightTemplate,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.ospRightTemplate=:ospTempName and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.ospRightTemplate,g.userName")
    List<GroupUserDetails> getOspRightsGroupDetails(LocalDate repDate, String ospTempName, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.ospRightTemplate,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.ospRightTemplate=:ospTempName and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%')) or" +
            " upper(t.grpRoleStr) like (concat('%', upper(:searchString), '%')) or upper(t.ospRightTemplate) like (concat('%', upper(:searchString), '%')) or upper(g.userName) like (concat('%',upper(:searchString),'%')))" +
            "group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.ospRightTemplate,g.userName")
    List<GroupUserDetails> getOspRightsGroupDetailAndGlobalSearch(LocalDate repDate, String ospTempName, Sort sort, String searchString);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getPortfolioRightsGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.nkeyTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.nkeyTmpl =:template  and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.nkeyTmpl,g.userName")
    List<GroupUserDetails> getOperationNkeyGroupDetails(LocalDate repDate, String template, Sort sort);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.lposRight,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.lposRight =:template  and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.lposRight,g.userName")
    List<GroupUserDetails> getOperationLposGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.nkeyTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.nkeyTmpl=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.nkeyTmpl) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%'))) group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.nkeyTmpl,g.userName ")
    List<GroupUserDetails> getOperationNkeyGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.lposRight,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.lposRight=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.lposRight) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%'))) group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.lposRight,g.userName ")
    List<GroupUserDetails> getOperationLposGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.navigationTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.navigationTmpl=:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.navigationTmpl,g.userName")
    List<GroupUserDetails> getNavigationGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getCombinedPortfolioRightsGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getEnterpriseGroupDetails(LocalDate repDate, String template, Sort sort);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getConfigurationGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.statTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.statTmpl =:template  and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.statTmpl,g.userName")
    List<GroupUserDetails> getFinanceRightsStatGroupDetails(LocalDate repDate, String template, Sort sort);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.accCtrl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.accCtrl =:template  and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.accCtrl,g.userName")
    List<GroupUserDetails> getFinanceRightsAcctrlGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.chineseWall,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.chineseWall=:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.chineseWall,g.userName")
    List<GroupUserDetails> getChineseWallGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.consitencyTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.consitencyTmpl=:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.consitencyTmpl,g.userName")
    List<GroupUserDetails> getConsistencyGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.stpRgtTmpl=:template and t.reportDate=:repDate group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl,g.userName")
    List<GroupUserDetails> getStpRightsGroupDetails(LocalDate repDate, String template, Sort sort);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.grpRoleStr) like (concat('%', upper(:searchString), '%')) or upper(t.grpDesc) like (concat('%', upper(:searchString), '%')) or upper(g.userName) like (concat('%',upper(:searchString),'%'))) " +
            "group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getCombinedPortfolioRightsGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort by, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.chineseWall,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.chineseWall=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.chineseWall) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%'))) group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.chineseWall,g.userName ")
    List<GroupUserDetails> getChineseWallGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort by, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%' )) or " +
            "upper(t.grpRoleStr) like (concat('%', upper(:searchString), '%')) or upper(t.grpDesc) like (concat('%', upper(:searchString), '%')) or upper(g.userName) like (concat('%',upper(:searchString),'%')))  " +
            "group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getPortfolioRightsGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.navigationTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.navigationTmpl=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.navigationTmpl) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%'))) group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.navigationTmpl,g.userName ")
    List<GroupUserDetails> getNavigationGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.consitencyTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.consitencyTmpl=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.consitencyTmpl) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%')))group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.consitencyTmpl,g.userName ")
    List<GroupUserDetails> getConsistencyGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%' )) or " +
            "upper(t.grpRoleStr) like (concat('%', upper(:searchString), '%')) or upper(t.grpDesc) like (concat('%', upper(:searchString), '%')) or upper(g.userName) like (concat('%',upper(:searchString),'%')))  " +
            "group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getConfigurationGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.groupLabel =:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%' )) or " +
            "upper(t.grpRoleStr) like (concat('%', upper(:searchString), '%')) or upper(t.grpDesc) like (concat('%', upper(:searchString), '%')) or upper(g.userName) like (concat('%',upper(:searchString),'%')))  " +
            "group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,g.userName")
    List<GroupUserDetails> getEnterpriseGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.statTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.statTmpl=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.statTmpl) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%')))group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.statTmpl,g.userName ")
    List<GroupUserDetails> getFinanceRightsStatGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort by, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.accCtrl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.accCtrl=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.accCtrl) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%'))) group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.accCtrl,g.userName ")
    List<GroupUserDetails> getFinanceRightsAcctrlGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort by, String searchString);
    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.GroupUserDetails(t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl,g.userName) from MxGroupsListItem t left join MXUserGroupAccessRgt" +
            " g on t.groupLabel=g.groupLabel and t.reportDate=g.reportDate where t.stpRgtTmpl=:template and t.reportDate=:repDate and (upper(t.groupLabel) like (concat('%', upper(:searchString),'%')) or " +
            "upper(t.grpRoleStr) like (concat('%',upper(:searchString),'%')) or upper(t.grpDesc) like (concat('%',upper(:searchString),'%')) or upper(t.stpRgtTmpl) like (concat('%',upper(:searchString),'%')) or" +
            " upper(g.userName) like (concat('%',upper(:searchString),'%'))) group by t.id,t.groupLabel,t.grpRoleStr,t.grpDesc,t.stpRgtTmpl,g.userName ")
    List<GroupUserDetails> getStpRightsGroupDetailAndGlobalSearch(LocalDate repDate, String template, Sort sort, String searchString);
}
