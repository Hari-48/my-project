package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxSupChgAuditBdy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;

@Repository
@Transactional
public interface MxSupChgAuditBdyRepository extends CrudRepository<MxSupChgAuditBdy,Long> {

    @Modifying
    @Query("DELETE FROM MxSupChgAuditBdy t WHERE t.sysDate <= :created")
    void deleteMxSupChgAuditBdyByCreated(LocalDate created);

    MxSupChgAuditBdy  findTopByReportDate(LocalDate reportDate);

    @Query(value = "select distinct t from MxSupChgAuditBdy t where t.reportDate=:requestDate and t.auditId=:auditId")
    Page<MxSupChgAuditBdy> findByReportDateAndAuditIdList(Long auditId,LocalDate requestDate, Pageable pageable);

    @Query(value = "select distinct t from MxSupChgAuditBdy t where t.reportDate=:requestDate and t.auditId=:auditId and (UPPER(t.auditId) like concat('%',upper(:searchString),'%') or upper(t.fieldLabel) like concat('%',upper(:searchString),'%') or upper(t.newValue) like concat('%',upper(:searchString),'%') or upper(t.oldValue) like concat('%',upper(:searchString),'%')" +
            " or upper(t.type) like concat('%',upper(:searchString),'%'))")
    Page<MxSupChgAuditBdy> findByReportDateAndAuditIdWithGlobalSearch(Long auditId, LocalDate requestDate, String searchString, Pageable pageable);
}
