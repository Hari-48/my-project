package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxCounterPartyDisplay;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;

@Repository
@Transactional
public interface MxCounterPartyDisplayRepository extends CrudRepository<MxCounterPartyDisplay, Long> {

    MxCounterPartyDisplay findTopByReportDate(LocalDate requestDate);

    @Query(value = "select t from MxCounterPartyDisplay t where t.reportDate=:requestDate")
    Page<MxCounterPartyDisplay> findByTemplateAndReportDateList(LocalDate requestDate, Pageable pageable);

    @Query(value = "select distinct t from MxCounterPartyDisplay t where t.reportDate=:requestDate and ( upper(t.agreement) like concat('%',upper(:searchString),'%') or upper(t.agreementType) like concat('%',upper(:searchString),'%') or" +
            " upper(t.amend) like concat('%',upper(:searchString),'%') or upper(t.cancelled) like concat('%',upper(:searchString),'%') or upper(t.clearCenter) like concat('%',upper(:searchString),'%') or upper(t.clearer) like concat('%',upper(:searchString),'%')" +
            " or upper(t.code) like concat('%',upper(:searchString),'%') or upper(t.comments) like concat('%',upper(:searchString),'%') or upper(t.countPart) like concat('%',upper(:searchString),'%') or upper(t.cptyLable) like concat('%',upper(:searchString),'%')" +
            " or upper(t.crde) like concat('%',upper(:searchString),'%') or upper(t.currency) like concat('%',upper(:searchString),'%') or upper(t.customInfo) like concat('%',upper(:searchString),'%') or upper(t.displayUser) like concat('%',upper(:searchString),'%')" +
            " or upper(t.endDt) like concat('%',upper(:searchString),'%') or upper(t.entity) like concat('%',upper(:searchString),'%') or upper(t.family) like concat('%',upper(:searchString),'%') or upper(t.flowTypology0) like concat('%',upper(:searchString),'%')" +
            " or upper(t.flowTypology1) like concat('%',upper(:searchString),'%') or upper(t.flowTypology2) like concat('%',upper(:searchString),'%') or upper(t.flowTypology3) like concat('%',upper(:searchString),'%') or upper(t.flowTypology4) like concat('%',upper(:searchString),'%')" +
            " or upper(t.governingLaw) like concat('%',upper(:searchString),'%') or upper(t.instrument) like concat('%',upper(:searchString),'%') or upper(t.legalEntity) like concat('%',upper(:searchString),'%') or upper(t.location) like concat('%',upper(:searchString),'%')" +
            " or upper(t.market) like concat('%',upper(:searchString),'%') or upper(t.modDate) like concat('%',upper(:searchString),'%') or upper(t.modTime) like concat('%',upper(:searchString),'%') or upper(t.multiple) like concat('%',upper(:searchString),'%')" +
            " or upper(t.nature) like concat('%',upper(:searchString),'%') or upper(t.next) like concat('%',upper(:searchString),'%') or upper(t.novo) like concat('%',upper(:searchString),'%') or upper(t.odCurrency) like concat('%',upper(:searchString),'%') or " +
            "upper(t.physicalProduct) like concat('%',upper(:searchString),'%') or upper(t.portfolio) like concat('%',upper(:searchString),'%') or upper(t.previous) like concat('%',upper(:searchString),'%') or upper(t.proccessingArea) like concat('%',upper(:searchString),'%')" +
            " or upper(t.processEntity) like concat('%',upper(:searchString),'%') or upper(t.ref) like concat('%',upper(:searchString),'%') or upper(t.settlType) like concat('%',upper(:searchString),'%') or upper(t.settleMethod) like concat('%',upper(:searchString),'%')" +
            " or upper(t.startDt) like concat('%',upper(:searchString),'%') or upper(t.status) like concat('%',upper(:searchString),'%') or upper(t.strategy) like concat('%',upper(:searchString),'%') or upper(t.tradeSection) like concat('%',upper(:searchString),'%')" +
            " or upper(t.trnGroup) like concat('%',upper(:searchString),'%') or upper(t.type) like concat('%',upper(:searchString),'%') or upper(t.typology) like concat('%',upper(:searchString),'%') or upper(t.usage) like concat('%',upper(:searchString),'%')" +
            " or upper(t.vostroService) like concat('%',upper(:searchString),'%'))")
    Page<MxCounterPartyDisplay> findByTemplateAndReportDateListWithGlobalSearch(LocalDate requestDate, String searchString, Pageable pageable);
}
