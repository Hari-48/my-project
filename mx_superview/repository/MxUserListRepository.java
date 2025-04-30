package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxUserListItem;
import com.finsurge.tmr_portal.mx_superview.entity.MxUserPolicy;
import com.finsurge.tmr_portal.mx_superview.models.CombineMxUserList;
import com.finsurge.tmr_portal.mx_superview.models.MxUserList;
import com.finsurge.tmr_portal.mx_superview.models.UserGroupDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Transactional
@Repository
public interface MxUserListRepository extends CrudRepository<MxUserListItem, Long> {

    @Query(value = "select t from MxUserListItem t where t.sysDate =:localDate")
    List<MxUserListItem> getTodaysUserListData(LocalDate localDate);

    Page<MxUserListItem> findAllBySysDate(LocalDate date, Pageable pageable);

    List<MxUserListItem> findAllByReportDate(LocalDate date);

    Page<MxUserListItem> findAllBySysDateAndUserName(LocalDate date, String name, Pageable pageable);

    @Query(value = "select distinct(t.reportDate) from MxUserListItem t ")
    List<LocalDate> findAllSnapshotDates();

    @Query(value = "select m from MxUserPolicy m join MxUserListItem t  on t.mngmntPolicy=m.mName where t.reportDate =:repDate and t.reportDate = m.reportDate and t.userName =:userName")
    List<MxUserPolicy> findAllUserPolicy(String userName, LocalDate repDate);

    @Query(value = "select t.userName from MxUserListItem t where t.reportDate = :repDate")
    List<String> findAllUserName(LocalDate repDate);

    MxUserListItem findTopByReportDate(LocalDate requestDate);

    @Query(value = "select distinct new com.finsurge.tmr_portal.mx_superview.models.MxUserList(t.id,t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, t.userLabel,jt.licenseCatName) " +
            "from MxUserListItem t left join  MxUserLicense jt  on (t.userName = jt.userName and t.reportDate = jt.reportDate) join " +
            "MXUserGroupAccessRgt m on upper(t.userName) = upper(m.userName) and  t.reportDate =m.reportDate where m.reportDate =:requestDate " +
            "and upper(m.groupLabel) = :groupLabel group by t.id")
    Page<MxUserList> findByTemplateAndReportDateList(String groupLabel, LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombineMxUserList(t.id,t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, m.licenseCatName,l.groupLabel,t.userLabel) " +
            "from MxUserListItem t left join MxUserLicense m on" +
            " t.reportDate=m.reportDate and t.userName=m.userName inner join MXUserGroupAccessRgt l on t.reportDate=l.reportDate and t.userName=l.userName where  " +
            "t.reportDate=:requestDate")
    Page<CombineMxUserList> findAllByReportDateGroups(LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombineMxUserList(t.id,t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, m.licenseCatName,l.groupLabel,t.userLabel) " +
            "from MxUserListItem t left join MxUserLicense m on" +
            " t.reportDate=m.reportDate and t.userName=m.userName inner join MXUserGroupAccessRgt l on t.reportDate=l.reportDate and t.userName=l.userName where  " +
            "t.reportDate=:requestDate  and l.groupLabel in :templateValue ")
    Page<CombineMxUserList> findGroupLabelAndRequestDate(List<String> templateValue, LocalDate requestDate, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.UserGroupDetails(t.id,t.userName,t.userDesc,t.grpRoleStr,t.grpDesc) from MXUserGroupAccessRgt t where t.groupLabel = :groupLabel and t.reportDate =:requestDate")
    List<UserGroupDetails> findUserDetailsByGroupLabel(String groupLabel, LocalDate requestDate, Sort sort);

    @Query(value = "select distinct new com.finsurge.tmr_portal.mx_superview.models.MxUserList(t.id,t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy,t.userLabel, jt.licenseCatName) " +
            "from MxUserListItem t left join  MxUserLicense jt  on (t.userName = jt.userName and t.reportDate = jt.reportDate) join " +
            "MXUserGroupAccessRgt m on upper(t.userName) = upper(m.userName) and  t.reportDate =m.reportDate where m.reportDate =:requestDate " +
            "and upper(m.groupLabel) = :groupLabel and (upper(t.userName) like (concat('%', upper(:searchString), '%')) or upper(t.descr) like (concat('%', upper(:searchString), '%')) " +
            "or upper(t.suspended) like (concat('%', upper(:searchString), '%')) or upper(t.locked) like (concat('%', upper(:searchString), '%')) " +
            "or upper(jt.licenseCatName) like (concat('%', upper(:searchString), '%')) or upper(t.code) like (concat('%', upper(:searchString), '%')) or upper(t.mngmntPolicy) like (concat('%', upper(:searchString), '%')) or upper(t.userLabel) like (concat('%', upper(:searchString), '%')))")
    Page<MxUserList> findByTemplateAndReportDateListWithGlobalSearch(String groupLabel, LocalDate requestDate, String searchString, Pageable pageable);


    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombineMxUserList(t.id, t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, m.licenseCatName, l.groupLabel,t.userLabel) " +
            "from MxUserListItem t left join MxUserLicense m on t.reportDate = m.reportDate and t.userName = m.userName inner join MXUserGroupAccessRgt l on t.reportDate = l.reportDate and t.userName = l.userName " +
            "where t.reportDate = :requestDate and (upper(t.userName) like (concat('%', upper(:searchString), '%')) or upper(t.descr) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.suspended) like (concat('%', upper(:searchString), '%')) or upper(t.locked) like (concat('%', upper(:searchString), '%')) or upper(t.code) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.mngmntPolicy) like (concat('%', upper(:searchString), '%')) or upper(m.licenseCatName) like (concat('%', upper(:searchString), '%')) or upper(l.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombineMxUserList> findUserListGlobalCombinedAllGroups(LocalDate requestDate, String searchString, Pageable pageable);

    @Query(value = "select new com.finsurge.tmr_portal.mx_superview.models.CombineMxUserList(t.id, t.userName, t.descr, t.suspended, t.locked, t.code, t.mngmntPolicy, m.licenseCatName, l.groupLabel,t.userLabel) " +
            "from MxUserListItem t left join MxUserLicense m on t.reportDate = m.reportDate and t.userName = m.userName inner join MXUserGroupAccessRgt l on t.reportDate = l.reportDate and t.userName = l.userName " +
            "where t.reportDate = :requestDate and l.groupLabel in :templateValue and (upper(t.userName) like (concat('%', upper(:searchString), '%')) or upper(t.descr) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.suspended) like (concat('%', upper(:searchString), '%')) or upper(t.locked) like (concat('%', upper(:searchString), '%')) or upper(t.code) like (concat('%', upper(:searchString), '%')) or " +
            "upper(t.mngmntPolicy) like (concat('%', upper(:searchString), '%')) or upper(m.licenseCatName) like (concat('%', upper(:searchString), '%')) or upper(l.groupLabel) like (concat('%', upper(:searchString), '%')))")
    Page<CombineMxUserList> findUserListGlobalCombined(List<String> templateValue, LocalDate requestDate, String searchString, Pageable pageable);
}
