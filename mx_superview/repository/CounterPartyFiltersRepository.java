package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.CounterPartyFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;

@Repository
@Transactional
public interface CounterPartyFiltersRepository extends CrudRepository<CounterPartyFilters,Long> {

    @Query("select m from CounterPartyFilters m ")
    Page<CounterPartyFilters> getCounterPartyData(Pageable pageable);

    @Query("select m from CounterPartyFilters m where  upper(m.status)= upper(:status) ")
    Page<CounterPartyFilters> getCounterPartyDataByStatus(String status, Pageable pageable);

    CounterPartyFilters findTopByLabel(String label);

    Boolean existsByLabel(String label);

    @Query("select m from CounterPartyFilters m where upper(m.label)= upper(:label) and id!=:id")
    CounterPartyFilters checkLabelExists(String label, long id);
}

