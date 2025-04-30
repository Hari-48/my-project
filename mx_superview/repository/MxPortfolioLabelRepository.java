package com.finsurge.tmr_portal.mx_superview.repository;

import com.finsurge.tmr_portal.mx_superview.entity.MxPortfolioLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public interface MxPortfolioLabelRepository extends CrudRepository<MxPortfolioLabel, Long> {

    @Query(value = "select t from MxPortfolioLabel t where t.sysDate =:todayDate")
    List<MxPortfolioLabel> getTodaysPortfolio(LocalDate todayDate);

    Page<MxPortfolioLabel> findAllBySysDateOrderByTreeLevelAscPortfolioLabelAsc(LocalDate date, Pageable pageable);

    Page<MxPortfolioLabel> findAllBySysDateAndTreeLevelOrderByPortfolioLabel(LocalDate date, String treeLevel, Pageable pageable);

    Page<MxPortfolioLabel> findAllBySysDateAndTreeLevelAndParentPortFolioOrderByPortfolioLabel(LocalDate date, String treeLevel, String parentPortfolio, Pageable pageable);

    @Query(value = "select count(t) from MxGroupPortfolioRights t where t.reportDate=:requestDate and upper(t.groupLabel)= :groupLabel")
    Long findAllPortfolioRights(String groupLabel, LocalDate requestDate);

}
