package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxDistribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface MxDistributionRepo extends CrudRepository<MxDistribution, Long> {


    @Query(value = "select t.dstPrf from MxDistribution t where t.sysDate=:date")
    List<String> findDistinctByDSRFLabel(LocalDate date);

    @Query(value = "select t from MxDistribution t where t.sysDate=:date and t.dstPrf=:dsrfLabel")
    Page<MxDistribution> findAllByDsrfLabelAndSysDate(String dsrfLabel, LocalDate date, Pageable pageable);

}
