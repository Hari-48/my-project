package com.finsurge.tmr_portal.mx_superview.service;

import com.finsurge.tmr_portal.mx_superview.entity.SearchHistory;
import com.finsurge.tmr_portal.mx_superview.repository.SearchHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class SearchHistoryService {
    private static final Logger log = LoggerFactory.getLogger(SearchHistoryService.class);

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    public Page<SearchHistory> getSearchPredicates(String userName, String reportType, int page, int pageSize, String search)
    {
        return searchHistoryRepository.findByUserNameAndReportType(userName, reportType, search, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "systemTimestamp")));
    }

    public void deleteSearchHistory(String userName,String reportType ,Long historyId)
    {
        if(historyId!=null ) {
            searchHistoryRepository.deleteByUserNameAndAndReportTypeByAndSearchString(userName, reportType,historyId);

        }
        else{
            searchHistoryRepository.deleteByUserNameAndAndReportType(userName, reportType);

        }

    }
}
