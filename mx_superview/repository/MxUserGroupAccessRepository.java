package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MXUserGroupAccessRgt;
import com.finsurge.tmr_portal.mx_superview.entity.MxUserListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Transactional
@Repository
public interface MxUserGroupAccessRepository extends CrudRepository<MXUserGroupAccessRgt,Long> {

    @Query(value = "select t from MXUserGroupAccessRgt t where t.sysDate =:todayDate")
    List<MXUserGroupAccessRgt> getTodaysUserGroupAccessData(LocalDate todayDate);

    Page<MXUserGroupAccessRgt> findAllByUserNameAndSysDate(String username, LocalDate date, Pageable pageable);

    List<MXUserGroupAccessRgt> findAllByUserNameAndGroupLabelAndSysDate(String username, String groupLabel, LocalDate date);

    @Query(value = "select distinct t.reportDate from MXUserGroupAccessRgt t")
    List<LocalDate> findAllRepDate();

    @Query(value = "select t.groupLabel from MXUserGroupAccessRgt t where t.reportDate =:date and t.userName =:username")
    List<String> findAllByUserNameAndRepDate(String username, LocalDate date);

    @Query(value = "select distinct t.userName from MXUserGroupAccessRgt t where t.groupLabel in :groupName and t.reportDate=:requestDate")
    List<String> findUsernameByGroupName(List<String> groupName, LocalDate requestDate);

    @Query(value = "select distinct t.userName from MXUserGroupAccessRgt t where t.groupLabel=:groupName and t.reportDate=:requestDate")
    List<String> findUsernameByGroup(String groupName, LocalDate requestDate);

    @Query(value = "select distinct t.userName from MXUserGroupAccessRgt t where"+"(:requestDate is null or t.reportDate = :requestDate) and"
            +"(:searchGroupName is null or upper(t.groupLabel) like upper(concat('%', :searchGroupName,'%')))"+"order by t.userName asc")
    List<String> findUsername(String searchGroupName, LocalDate requestDate);

    @Query(value = "select distinct t from MxUserListItem t where t.reportDate=:requestDate and t.userName in ( select distinct g.userName from MXUserGroupAccessRgt g " +
            "where g.reportDate=:requestDate and g.groupLabel in :groupLabel)")
    List<MxUserListItem> findUserDetails(LocalDate requestDate, List<String> groupLabel);
}
