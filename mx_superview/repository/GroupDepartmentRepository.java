package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxGroupsDepartment;
import com.finsurge.tmr_portal.mx_superview.models.DepartmentGroupRole;
import com.finsurge.tmr_portal.mx_superview.models.GroupDepartment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface GroupDepartmentRepository extends CrudRepository<MxGroupsDepartment,Long> {

    @Query("select new com.finsurge.tmr_portal.mx_superview.models.DepartmentGroupRole(t.groupLabel,t.groupRoleStr) from MxGroupsDepartment t")
    List<DepartmentGroupRole> getGroupDepartment();

//    @Query(value = "SELECT GROUP_LABEL,GRP_ROLE_STR FROM ( SELECT  GROUP_LABEL,GRP_ROLE_STR FROM UAM_MX_GROUP_LIST where to_char(REP_DATE,'YYYYMMDD')=:reportDate UNION ALL SELECT GROUP_LABEL,GRP_ROLE_STR FROM UAM_MX_GROUPS_DEPARTMENT) DEPARTMENT_TABLE GROUP BY  GROUP_LABEL,GRP_ROLE_STR HAVING count(*) = 1 ORDER BY GROUP_LABEL",nativeQuery = true)
//    List<Object[]> getNewGroupDepartment(String reportDate);

    @Query(value = "SELECT GROUP_LABEL,GRP_ROLE_STR FROM UAM_MX_GROUP_LIST where to_char(REP_DATE,'YYYYMMDD')=:reportDate and GROUP_LABEL not in (SELECT GROUP_LABEL FROM UAM_MX_GROUPS_DEPARTMENT)",nativeQuery = true)
    List<Object[]> getNewGroupDepartment(String reportDate);

    MxGroupsDepartment findByGroupLabel(String groupLabel);

    @Query("select t from MxGroupsDepartment t")
    List<MxGroupsDepartment> getDepartment();

    @Query("select new com.finsurge.tmr_portal.mx_superview.models.GroupDepartment(t.id,t.groupLabel,t.groupRoleStr) from MxGroupsDepartment t")
    List<GroupDepartment> getAllGroupDepartment();
}
