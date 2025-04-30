package com.finsurge.tmr_portal.mx_superview.repository;


import com.finsurge.tmr_portal.mx_superview.entity.MxStpSourceModuleEod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface STPSrcModuleRepository extends CrudRepository<MxStpSourceModuleEod,Long> {

    List<MxStpSourceModuleEod> findDistinctTopBySourceModuleInAndReportDate(List<String> templateValue, LocalDate requestDate);

    Page<MxStpSourceModuleEod> findDistinctByReportDateAndGlobalTemplate(LocalDate requestDate, String templateValue, Pageable pageable);

    MxStpSourceModuleEod findTopByReportDateAndGlobalTemplate(LocalDate requestDate, String templateValue);

    @Query(value = "select distinct t from MxStpSourceModuleEod t where t.globalTemplate=:templateValue and t.reportDate=:requestDate and (UPPER(t.sourceTemp) like concat('%',upper(:searchString),'%') or UPPER(t.sourceModuleAction) like concat('%',upper(:searchString),'%') or UPPER(t.sourceModule) like concat('%',upper(:searchString),'%'))")
    Page<MxStpSourceModuleEod> findDistinctByReportDateAndGlobalTemplateWithGlobalSearch(String templateValue, LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select t.PROCESSING_CENTER,t.PROCESSING_TEMPLATE from UAM_MX_STP_SOURCE_MODULE_EOD t where DATE_FORMAT(REP_DATE,'%Y%m%d')=:requestDate and GLOBAL_TEMPLATE=:templateValue group by " +
            "t.PROCESSING_CENTER, t.PROCESSING_TEMPLATE",nativeQuery = true)
    List<Object[]> findByReportDateAndGlobalTemplate(String templateValue, String requestDate);
}