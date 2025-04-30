package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Clob;
import java.util.List;

@Transactional
@Repository
public interface SearchHistoryRepository extends CrudRepository<SearchHistory,Long> {

    @Query(value="select  h from SearchHistory h where " +
            "(:searchQuery is null or upper(h.searchQuery) like upper(concat('%',:searchQuery,'%'))) and h.userName = :userName and h.reportType = :reportType  and h.searchQuery is not null ")
    Page<SearchHistory> findByUserNameAndReportType(String userName, String reportType,String searchQuery ,Pageable pageable);

    @Modifying
    @Query("DELETE FROM  SearchHistory h where h.userName = :userName and h.reportType = :reportType")
    void deleteByUserNameAndAndReportType(String userName,String reportType);

    @Modifying
    @Query("DELETE FROM  SearchHistory h where h.userName = :userName and h.reportType = :reportType and h.id= :id")
    void deleteByUserNameAndAndReportTypeByAndSearchString(String userName, String reportType ,Long id);

    @Modifying
    @Query(value = "SET sql_mode=(SELECT REPLACE(@@sql_mode,'ONLY_FULL_GROUP_BY',''))",nativeQuery = true)
    Integer sqlMode();
    
    SearchHistory getTopByUserNameAndReportTypeAndSearchQuery(String userName,String reportType,String searchQuery);

}


