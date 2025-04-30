package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxCwtConfigMgtRight;
import com.finsurge.tmr_portal.mx_superview.entity.MxOspRightsMatrix;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;

@Repository
@Transactional
public interface GroupCompareRepository extends JpaRepository<MxOspRightsMatrix,Long> {
@Query(value = "select t from MxOspRightsMatrix t where t.reportDate =:requestDate and t.ospRightTemplate=:template order by t.validationRightTemplate,t.category,t.subCategory,t.queue")
    Page<MxOspRightsMatrix> findAllOspRightsByTemplate(String template, LocalDate requestDate, Pageable pageable);

@Query(value ="select t from MxCwtConfigMgtRight t where t.reportDate=:requestDate and t.groupLabel=:groupLabel ")
   Page<MxCwtConfigMgtRight> findAllConfigurationManagementByGroupLabel(String groupLabel, LocalDate requestDate, Pageable pageable);
}
