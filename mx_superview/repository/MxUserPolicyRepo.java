package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxFinaceAcctrlRights;
import com.finsurge.tmr_portal.mx_superview.entity.MxUserPolicy;
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
public interface MxUserPolicyRepo extends CrudRepository<MxUserPolicy, Long> {

    @Query(value = "select distinct t.mName from MxUserPolicy t where t.sysDate = :date")
    List<String> findDistinctPolicyBySysDate(LocalDate date);

    @Query(value = "select t from MxUserPolicy t where t.reportDate = :date and t.mName = :mName")
    Page<MxUserPolicy> findAllUserPolicy(String mName, LocalDate date,Pageable pageable);


    @Query(value = "select t from MxUserPolicy t where t.reportDate =:requestDate and t.mName =:userPolicy")
    List<MxUserPolicy> findUserPolicy(String userPolicy, LocalDate requestDate);

}
