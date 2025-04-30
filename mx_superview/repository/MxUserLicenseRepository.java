package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxUserLicense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface MxUserLicenseRepository extends CrudRepository<MxUserLicense, Long> {

    @Query(value = "select distinct t.licenseCatName from MxUserLicense t where t.sysDate = :date order by t.licenseCatName")
    List<String> findAllDistinctLicenseCatNamesForSysDate(LocalDate date);

    Page<MxUserLicense> findAllByLicenseCatNameAndSysDateOrderByLicenseCatNameAscUserNameAsc(String licenseCatName, LocalDate date, Pageable pageable);

    @Query(value = "select distinct t.licenseCatName from MxUserLicense t where t.reportDate = :date and  t.userName = :username")
    String findMxUserLicenseByUserNameAndReportDate(String username,LocalDate date);

}
