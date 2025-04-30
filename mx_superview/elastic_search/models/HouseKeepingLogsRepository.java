package com.finsurge.tmr_portal.mx_superview.elastic_search.models;

import com.finsurge.tmr_portal.mx_superview.elastic_search.entity.HouseKeepingLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface HouseKeepingLogsRepository extends CrudRepository<HouseKeepingLogs, Long> {

    @Query(value = "select h from HouseKeepingLogs h where " +
            "(:message is null or upper(h.message) like upper(concat('%', :message, '%')) )" +
            "and (:status is null or h.status = :status)")
    Page<HouseKeepingLogs> findAllByFilters( String message,
            String status, Pageable pageable);
}

