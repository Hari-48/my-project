package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxSupChgAuditHdr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.Date;

@Repository
@Transactional
public interface MxSupChgAuditHdrRepository extends CrudRepository<MxSupChgAuditHdr,Long> {

    @Modifying
    @Query("DELETE FROM MxSupChgAuditHdr t WHERE t.sysDate <= :created")
    void deleteMxSupChgAuditHdrByCreated(LocalDate created);

    @Query(value = "select distinct t from MxSupChgAuditHdr t where t.reportDate >= :fromDate and t.reportDate <= :requestDate")
    Page<MxSupChgAuditHdr> findByReportDateList(LocalDate requestDate, LocalDate fromDate, Pageable pageable);
}
