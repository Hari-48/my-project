package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxSuperViewLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;


@Repository
@Transactional
public interface MxSuperViewLogRepository extends CrudRepository<MxSuperViewLog , Long> {

  @Query(value = "select t from MxSuperViewLog  t where " +
          "(:logType is null or upper(t.logType) like upper(concat('%', :logType, '%'))) and " +
          "t.rootJobId=:rootJobId order by t.id")
  Page<MxSuperViewLog> findAllByRootJobIdAndLogType(Long rootJobId, String logType, Pageable pageable);

}

