package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxCounterParty;
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
public interface MXCounterpartyRepository extends CrudRepository<MxCounterParty, Long> {

    MxCounterParty findTopByReportDate(LocalDate requestDate);

    @Query(value = "select distinct t from MxCounterParty t where t.reportDate=:requestDate")
    Page<MxCounterParty> findByTemplateAndReportDateList(LocalDate requestDate, Pageable page);

}
