package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxDepartments;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MxDepartmentRepository extends CrudRepository<MxDepartments,Long> {

    @Query(value = "select t from MxDepartments t where (:searchValue is null or upper(t.groupDepartment) like concat('%',upper(:searchValue),'%')) order by t.groupDepartment asc")
    List<MxDepartments> getAllDepartments(Sort sort, String searchValue);

    @Query(value = "select t from MxDepartments t where upper(t.groupDepartment)=upper(:groupDepartment)")
    MxDepartments findByGroupDepartment(String groupDepartment);
}
