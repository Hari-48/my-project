package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxStpRightsTypologyEod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;

public interface STPRightsTypologyRepository extends CrudRepository<MxStpRightsTypologyEod,Long> {

    Page<MxStpRightsTypologyEod> findDistinctByTypologyGroupAndReportDate(String typologyGroup, LocalDate repDate, Pageable pageable);

    MxStpRightsTypologyEod findTopByTypologyGroupAndReportDate(String typologyGroup,LocalDate repDate);
}
