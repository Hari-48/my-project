package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxCwtConfigMgtRight;
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
public interface MxCwtConfigMgtRightRepository extends CrudRepository<MxCwtConfigMgtRight , Long> {

    @Query(value = "select distinct t.irsLabel from MxCwtConfigMgtRight  t where t.sysDate=:date")
    List<String> findDistinctIrsLabelBySysDate(LocalDate date);

    @Query(value = "select distinct t.ldLabel from MxCwtConfigMgtRight  t where t.sysDate=:date")
    List<String> findDistinctLdLabelBySysDate(LocalDate date);

    @Query(value = "select distinct t.cdLabel from MxCwtConfigMgtRight  t where t.sysDate=:date")
    List<String> findDistinctCdLabelBySysDate(LocalDate date);

    @Query(value = "select distinct t.rtgaLabel from MxCwtConfigMgtRight  t where t.sysDate=:date")
    List<String> findDistinctRtLabelBySysDate(LocalDate date);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.sysDate=:date and t.irsLabel=:irsLabel")
    Page<MxCwtConfigMgtRight> findAllByIrsLabelAndSysDate(String irsLabel, LocalDate date, Pageable pageable);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.sysDate=:date and t.rtgaLabel=:rtgaLabel")
    Page<MxCwtConfigMgtRight> findAllByRtgaLabelAndSysDate(String rtgaLabel, LocalDate date, Pageable pageable);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.sysDate=:date and t.cdLabel=:cdLabel")
    Page<MxCwtConfigMgtRight> findAllByCdLabelAndSysDate(String cdLabel, LocalDate date, Pageable pageable);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.sysDate=:date and t.ldLabel=:ldLabel")
    Page<MxCwtConfigMgtRight> findAllByLdLabelAndSysDate(String ldLabel, LocalDate date, Pageable pageable);

    @Query(value = "select t.groupLabel from MxCwtConfigMgtRight t where t.sysDate=:date")
    List<String> findDistinctGroupLabel(LocalDate date);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.sysDate=:date and t.groupLabel=:groupLabel")
    Page<MxCwtConfigMgtRight> findAllCwtRight(String groupLabel, LocalDate date, Pageable pageable);

    MxCwtConfigMgtRight findTopByGroupLabelAndReportDate(String groupLabel, LocalDate requestDate);

    List<MxCwtConfigMgtRight> findTopByReportDateAndGroupLabelIn(LocalDate repDate,List<String> groupLabel);

    Page<MxCwtConfigMgtRight> findAllByReportDate(LocalDate repDate,Pageable pageable);

    @Query(value = "select distinct t from MxCwtConfigMgtRight t where t.reportDate=:requestDate and t.groupLabel in :templateValue")
    Page<MxCwtConfigMgtRight> findByGroupLabelAndReportDate(List<String> templateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select distinct t from MxCwtConfigMgtRight t where t.groupLabel = :template  and t.reportDate = :baseReportDate  order by t.groupLabel ")
    List<MxCwtConfigMgtRight> findConfigByGroupLabelAndReportDate(String template, LocalDate baseReportDate);
    Page<MxCwtConfigMgtRight> findByGroupLabelAndReportDate(String template, LocalDate repDate, Pageable pageable);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.reportDate = :requestDate and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%')) or upper(t.irsLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.ldLabel) like (concat('%', upper(:searchString), '%')) or upper(t.cdLabel) like (concat('%', upper(:searchString), '%')) or upper(t.rtgaLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.hierarchyTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.joinRightTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.export) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.imports) like (concat('%', upper(:searchString), '%')) or upper(t.edit) like (concat('%', upper(:searchString), '%')) or upper(t.purge) like (concat('%', upper(:searchString), '%')))")
    Page<MxCwtConfigMgtRight> findConfigMgtGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.reportDate = :requestDate and t.groupLabel in :templateValue and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%')) or upper(t.irsLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.ldLabel) like (concat('%', upper(:searchString), '%')) or upper(t.cdLabel) like (concat('%', upper(:searchString), '%')) or upper(t.rtgaLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.hierarchyTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.joinRightTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.export) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.imports) like (concat('%', upper(:searchString), '%')) or upper(t.edit) like (concat('%', upper(:searchString), '%')) or upper(t.purge) like (concat('%', upper(:searchString), '%')))")
    Page<MxCwtConfigMgtRight> findConfigMgtGlobalCombined(List<String> templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxCwtConfigMgtRight t where t.reportDate=:requestDate and t.groupLabel=:template and (upper(t.groupLabel) like (concat('%', upper(:searchString), '%')) or upper(t.irsLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.ldLabel) like (concat('%', upper(:searchString), '%')) or upper(t.cdLabel) like (concat('%', upper(:searchString), '%')) or upper(t.rtgaLabel) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.hierarchyTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.joinRightTmpl) like (concat('%', upper(:searchString), '%')) or upper(t.export) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.imports) like (concat('%', upper(:searchString), '%')) or upper(t.edit) like (concat('%', upper(:searchString), '%')) or upper(t.purge) like (concat('%', upper(:searchString), '%')))")
    Page<MxCwtConfigMgtRight> findByGroupLabelAndReportDateWithGlobalSearch(String template, LocalDate requestDate,String searchString, Pageable pageable);


}
