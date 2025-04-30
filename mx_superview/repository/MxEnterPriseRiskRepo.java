package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxEnterpriseRisk;
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
public interface MxEnterPriseRiskRepo extends CrudRepository<MxEnterpriseRisk, Long> {

    @Query(value = "select t.label from MxEnterpriseRisk t where t.sysDate=:sysDate")
    List<String> findDistinctlabelBySysDate(LocalDate sysDate);

    @Query(value = "select t from MxEnterpriseRisk t where t.sysDate=:date and t.label=:label")
    Page<MxEnterpriseRisk> findAllByLabelAndSysDate(String label, LocalDate date, Pageable pageable);

    MxEnterpriseRisk findTopByLabelAndReportDate(String label, LocalDate requestDate);

    @Query(value = "select t from MxEnterpriseRisk t where  t.label = :template and t.reportDate = :baseReportDate order by t.label,t.modPst,t.modRsk,t.upload ")
    List<MxEnterpriseRisk> findEnterpriseByGroupLabelAndReportDate(String template, LocalDate baseReportDate);

    @Query(value = "select distinct t from MxEnterpriseRisk t where t.reportDate=:requestDate and t.label in :templateValue")
    Page<MxEnterpriseRisk> findByGroupLabelAndReportDate(List<String> templateValue, LocalDate requestDate, Pageable pageable);

    Page<MxEnterpriseRisk> findAllByReportDate(LocalDate requestDate, Pageable pageable);

    List<MxEnterpriseRisk> findTopByReportDateAndLabelIn(LocalDate reportDate, List<String> groupLabel);

    @Query(value = "select distinct t from MxEnterpriseRisk t where t.reportDate=:requestDate and t.label = :templateValue")
    Page<MxEnterpriseRisk> findByGroupLabelAndReportDate(String templateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select  t from MxEnterpriseRisk t where t.reportDate=:requestDate and t.label = :template order by t.label,t.modPst,t.modRsk,t.upload")
    Page<MxEnterpriseRisk> findEnterpriseByGroupLabelsAndReportDate(String template, LocalDate requestDate, Pageable pageable);


    @Query(value = "select t from MxEnterpriseRisk t where t.reportDate = :requestDate and (upper(t.label) like (concat('%', upper(:searchString), '%')) or upper(t.modRsk) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.modPst) like (concat('%', upper(:searchString), '%')) or upper(t.upload) like (concat('%', upper(:searchString), '%')) )")
    Page<MxEnterpriseRisk> findEnterpriseRiskGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t from MxEnterpriseRisk t where t.reportDate = :requestDate and t.label in :templateValue and (upper(t.label) like (concat('%', upper(:searchString), '%')) or upper(t.modRsk) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.modPst) like (concat('%', upper(:searchString), '%')) or upper(t.upload) like (concat('%', upper(:searchString), '%')))")
    Page<MxEnterpriseRisk> findEnterpriseRiskGlobalCombined(List<String> templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select  t from MxEnterpriseRisk t where t.reportDate = :requestDate and t.label = :templateValue and (upper(t.label) like (concat('%', upper(:searchString), '%')) or upper(t.modRsk) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.upload) like (concat('%', upper(:searchString), '%')) or upper(t.modPst) like (concat('%', upper(:searchString), '%')) )  order by t.label,t.modPst,t.modRsk,t.upload")
    Page<MxEnterpriseRisk> findByGroupLabelAndReportDateWithGlobalSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select  t from MxEnterpriseRisk t where t.reportDate = :requestDate and t.label = :templateValue and (upper(t.label) like (concat('%', upper(:searchString), '%')) or upper(t.modRsk) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.upload) like (concat('%', upper(:searchString), '%')) or upper(t.modPst) like (concat('%', upper(:searchString), '%')) )")
    Page<MxEnterpriseRisk> findByEnterpriseRiskGlobalSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);

}

