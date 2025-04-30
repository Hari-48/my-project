package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxSupChgAuditBdy;
import com.finsurge.tmr_portal.mx_superview.entity.MxSupChgAuditHdr;
import com.finsurge.tmr_portal.mx_superview.models.AuditFields;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;

@Repository
@Transactional
public interface MxAuditRepo extends CrudRepository<MxSupChgAuditHdr, Long> {

    @Query(value = "select count(t) from MxSupChgAuditHdr t where t.reportDate=:requestDate ")
    Long  findAllByRepDate(LocalDate requestDate);

    @Query(value = "select distinct t from MxSupChgAuditHdr t where t.reportDate >= :fromDate and t.reportDate <= :requestDate")
    Page<MxSupChgAuditHdr> findByReportDateList(LocalDate requestDate,LocalDate fromDate, Pageable pageable);

    @Query(value = "select distinct t from MxSupChgAuditHdr t where t.reportDate >= :fromDate and t.reportDate <= :requestDate and (upper(t.type) like concat('%',upper(:searchString),'%') or upper(t.action) like concat('%',upper(:searchString),'%') or" +
            " upper(t.userName) like concat('%',upper(:searchString),'%') or upper(t.auditId) like concat('%',upper(:searchString),'%') or upper(t.compDate) like concat('%',upper(:searchString),'%') or upper(t.compTime) like concat('%',upper(:searchString),'%')" +
            " or upper(t.refObject) like concat('%',upper(:searchString),'%') or upper(t.systemDate) like concat('%',upper(:searchString),'%') or upper(t.userDesk) like concat('%',upper(:searchString),'%') or upper(t.userGroup) like concat('%',upper(:searchString),'%'))")
    Page<MxSupChgAuditHdr> findByReportDateListWithGlobalSearch(LocalDate requestDate, LocalDate fromDate, String searchString, Pageable pageable);
}
