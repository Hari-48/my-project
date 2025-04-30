package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxNavigationTmpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Repository
@Transactional
public interface MxNavigationTemplateRepository extends CrudRepository<MxNavigationTmpl, Long> {

    Page<MxNavigationTmpl> findAllBySysDate(LocalDate date, Pageable pageable);

}
